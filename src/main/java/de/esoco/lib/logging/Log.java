//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.lib.logging;

import de.esoco.lib.expression.function.Group;
import de.esoco.lib.reflect.ReflectUtil;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static de.esoco.lib.expression.Functions.asConsumer;
import static de.esoco.lib.expression.Functions.doIf;
import static de.esoco.lib.expression.Functions.doIfElse;
import static de.esoco.lib.expression.Functions.println;
import static de.esoco.lib.logging.LogLevel.DEBUG;
import static de.esoco.lib.logging.LogLevel.ERROR;
import static de.esoco.lib.logging.LogLevel.FATAL;
import static de.esoco.lib.logging.LogLevel.INFO;
import static de.esoco.lib.logging.LogLevel.TRACE;
import static de.esoco.lib.logging.LogLevel.WARN;
import static de.esoco.lib.logging.LogLevelFilter.isLevel;
import static org.obrel.core.RelationTypes.newInitialValueType;

/**
 * Main class of a logging framework that is as simple to use as possible. No
 * static log instance is necessary, only the invocation of a static method like
 * {@link #warn(String) Log.warn(String)}. Log levels can be enabled or disabled
 * globally, independent of the underlying logging system. This can be done
 * either for single log levels with {@link Log#setGlobalLogLevels(LogLevel[])}
 * or for a range of levels with {@link Log#setGlobalMinimumLogLevel(LogLevel)}
 * (ordered by level severity).
 *
 * <p>There is also a set of log methods that accept a format string and a list
 * of format arguments (like {@link #warnf(Throwable, String, Object...)}).
 * These method also have the advantage that the message formatting only takes
 * place on actual output. If the formatting of message objects is expensive
 * these methods should be used to prevent unnecessary string conversions.</p>
 *
 * <p>The logging can be controlled by several system properties which can be
 * set on the command line of a VM with the '-D' switch. The supported logging
 * properties are:</p>
 *
 * <ul>
 *   <li>esoco.log.file=[filename]: the name and path of the log file (default
 *     logging is on the console)</li>
 *   <li>esoco.log.level=[LogLevel]: the global minimum log level</li>
 *   <li>esoco.log.plevels=[list of package log levels]: allows to constrain the
 *     minimum log level for certain packages or classes. The package level must
 *     be higher than the global level or else this property will be ignored.
 *     The property must be set to at least one entry of the form [package/class
 *     name]=[LogLevel], multiple entries must be separated by commas. If a
 *     package name is given the given log level applies to all classes in the
 *     package and it's sub-packages.</li>
 * </ul>
 *
 * @author eso
 */
public final class Log {

	/**
	 * A relation type that contains a {@link LogExtent}. Defaults to
	 * {@link LogExtent#ERRORS}.
	 */
	public static final RelationType<LogExtent> LOG_EXTENT =
		newInitialValueType(LogExtent.NOTHING);

	/**
	 * A relation type that contains a {@link LogLevel}. Defaults to
	 * {@link LogLevel#ERROR}.
	 */
	public static final RelationType<LogLevel> LOG_LEVEL =
		newInitialValueType(LogLevel.ERROR);

	/**
	 * The default log string transformation for log output
	 */
	public static final LogRecordFormat DEFAULT_FORMAT = new LogRecordFormat(
		"[{level:F%-5s}]{t:Dyyyy.MM.dd-HH:mm:ss}: {message}  [{package}" +
			".{class}.{method}() [{line}]]");

	/**
	 * A log string transformation for the full exception stacktrace, including
	 * _all_ causing exceptions! See {@link LogRecord#getCauseStackTrace()} for
	 * details.
	 */
	public static final LogRecordFormat CAUSE_TRACE =
		new LogRecordFormat("{getCauseStackTrace():F%s\n}");

	private static final Object[] NO_ARGS = null;

	private static final Map<String, Consumer<? super LogRecord>>
		logHandlerRegistry = new HashMap<>();

	private static final Map<String, Consumer<? super LogRecord>>
		logHandlerCache = new HashMap<>();

	private static Consumer<LogRecord> standardLogHandler;

	private static Group<LogRecord> defaultLogHandlers;

	private static LogLevelFilter globalLevelFilter =
		LogLevelFilter.startingAt(ERROR);

	@SuppressWarnings("rawtypes")
	private static Map<Class<? extends LogAspect>, LogAspect<?>> logAspects;

	static {
		RelationTypes.init(Log.class);
		setupStandardLogHandler();
		setupPackageLogHandlers();
	}

	/**
	 * Private, only static use.
	 */
	private Log() {
	}

	/**
	 * Adds an additional default log handler to the handlers that will be used
	 * if no special handler has been registered for the package or class
	 * that a
	 * log call originates from.
	 *
	 * @param handler An additional default log handler
	 * @throws NullPointerException If the argument is NULL
	 */
	public static void addDefaultLogHandler(
		Consumer<? super LogRecord> handler) {
		Objects.requireNonNull(handler);

		synchronized (logHandlerCache) {
			List<Consumer<? super LogRecord>> functions =
				defaultLogHandlers.getMembers();

			functions.add(handler);
			defaultLogHandlers = new Group<>(functions);

			logHandlerCache.clear();
		}
	}

	/**
	 * Adds a certain log aspect. The log aspect will be registered and
	 * initialized to start logging. If a log aspect with the same class
	 * already
	 * exists it will be shut down and removed first.
	 *
	 * @param logAspect The log aspect to register
	 */
	public static void addLogAspect(LogAspect<?> logAspect) {
		@SuppressWarnings("unchecked")
		Class<? extends LogAspect<?>> logAspectType =
			(Class<? extends LogAspect<?>>) logAspect.getClass();

		if (logAspects == null) {
			logAspects = new HashMap<>();
		} else {
			removeLogAspect(logAspectType);
		}

		logAspects.put(logAspectType, logAspect);
		logAspect.initLogging();
	}

	/**
	 * Logs a message at debug log level.
	 *
	 * @param message The log message
	 */
	public static void debug(String message) {
		logImpl(DEBUG, null, message, NO_ARGS);
	}

	/**
	 * Logs a message and an exception at debug log level.
	 *
	 * @param message The log message
	 * @param cause   The causing exception
	 */
	public static void debug(String message, Throwable cause) {
		logImpl(DEBUG, cause, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a formatted message at debug log level.
	 *
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void debugf(String format, Object... args) {
		logImpl(DEBUG, null, format, args);
	}

	/**
	 * Logs a formatted message and an exception at debug log level.
	 *
	 * @param cause  The causing exception (may be NULL)
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void debugf(Throwable cause, String format, Object... args) {
		logImpl(DEBUG, cause, format, args);
	}

	/**
	 * Logs a message at error log level.
	 *
	 * @param message The log message
	 */
	public static void error(String message) {
		logImpl(ERROR, null, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a message and an exception at error log level.
	 *
	 * @param message The log message
	 * @param cause   The causing exception
	 */
	public static void error(String message, Throwable cause) {
		logImpl(ERROR, cause, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a formatted message at error log level.
	 *
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void errorf(String format, Object... args) {
		logImpl(ERROR, null, format, args);
	}

	/**
	 * Logs a formatted message and an exception at error log level.
	 *
	 * @param cause  The causing exception (may be NULL)
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void errorf(Throwable cause, String format, Object... args) {
		logImpl(ERROR, cause, format, args);
	}

	/**
	 * Logs a message at fatal log level.
	 *
	 * @param message The log message
	 */
	public static void fatal(String message) {
		logImpl(FATAL, null, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a message and an exception at fatal log level.
	 *
	 * @param message The log message
	 * @param cause   The causing exception
	 */
	public static void fatal(String message, Throwable cause) {
		logImpl(FATAL, cause, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a formatted message at fatal log level.
	 *
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void fatalf(String format, Object... args) {
		logImpl(FATAL, null, format, args);
	}

	/**
	 * Logs a formatted message and an exception at fatal log level.
	 *
	 * @param cause  The causing exception (may be NULL)
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void fatalf(Throwable cause, String format, Object... args) {
		logImpl(FATAL, cause, format, args);
	}

	/**
	 * Looks up the log handler that has been registered to perform logging
	 * calls from a certain package. If no direct handler can be found this
	 * method recursively searches for handlers registered for parent packages.
	 * If no handler has been registered at all the default handler will be
	 * returned.
	 *
	 * @param packageOrClass The name of the package to lookup the log handler
	 *                       for
	 * @return The handler to be used to perform logging from the given package
	 */
	private static Consumer<? super LogRecord> findLogHandler(
		String packageOrClass) {
		Consumer<? super LogRecord> handler;

		if (packageOrClass != null) {
			handler = logHandlerRegistry.get(packageOrClass);

			if (handler == null) {
				packageOrClass = ReflectUtil.getNamespace(packageOrClass);
				handler = findLogHandler(packageOrClass);
			}
		} else {
			handler = asConsumer(defaultLogHandlers);
		}

		return handler;
	}

	/**
	 * Returns the global minimum log level that is currently set.
	 *
	 * @return The global minimum log level
	 */
	public static LogLevel getGlobalMinimumLogLevel() {
		return globalLevelFilter.getMinimumLevel();
	}

	/**
	 * Internal method to return a log handler for a certain log record. If no
	 * matching handler exists a new one will be created. Performs thread
	 * synchronization.
	 *
	 * @param record The log record to return the handler for
	 * @return The log handler
	 */
	private static Consumer<? super LogRecord> getLogHandler(LogRecord record) {
		String className = record.getLogClass().getName();

		Consumer<? super LogRecord> logHandler = null;

		synchronized (logHandlerCache) {
			logHandler = logHandlerCache.get(className);

			if (logHandler == null) {
				logHandler = findLogHandler(className);
				logHandlerCache.put(className, logHandler);
			}
		}

		return logHandler;
	}

	/**
	 * Returns the log handler that has been registered for a certain
	 * package or
	 * class.
	 *
	 * @param packageOrClass The name of the package or class to lookup the log
	 *                       handler for
	 * @return The log handler for the given name or NULL for none
	 */
	public static Consumer<? super LogRecord> getRegisteredLogHandler(
		String packageOrClass) {
		return logHandlerRegistry.get(packageOrClass);
	}

	/**
	 * Returns the standard log handler that performs output to System.out. To
	 * disable this handler it can be removed by invoking the method
	 * {@link #removeStandardLogHandler()}.
	 *
	 * @return The standard log handler
	 */
	public static Consumer<LogRecord> getStandardLogHandler() {
		return standardLogHandler;
	}

	/**
	 * Logs a message at info log level.
	 *
	 * @param message The log message
	 */
	public static void info(String message) {
		logImpl(INFO, null, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a message and an exception at info log level.
	 *
	 * @param message The log message
	 * @param cause   The causing exception
	 */
	public static void info(String message, Throwable cause) {
		logImpl(INFO, cause, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a formatted message at info log level.
	 *
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void infof(String format, Object... args) {
		logImpl(INFO, null, format, args);
	}

	/**
	 * Logs a formatted message and an exception at info log level.
	 *
	 * @param cause  The causing exception (may be NULL)
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void infof(Throwable cause, String format, Object... args) {
		logImpl(INFO, cause, format, args);
	}

	/**
	 * Checks if the argument log level is enabled globally for logging.
	 *
	 * @param level The log level to check
	 * @return TRUE if the level is enabled globally for logging
	 * @see #setGlobalLogLevels(LogLevel[])
	 * @see #setGlobalMinimumLogLevel(LogLevel)
	 */
	public static boolean isLevelEnabled(LogLevel level) {
		return globalLevelFilter.isLevelEnabled(level);
	}

	/**
	 * Generic method to log a message at a certain log level.
	 *
	 * @param level   The log level
	 * @param message The message to log
	 */
	public static void log(LogLevel level, String message) {
		logImpl(level, null, "%s", message, NO_ARGS);
	}

	/**
	 * Generic method to log a message at a certain log level with a causing
	 * exception.
	 *
	 * @param level   The log level
	 * @param message The message to log
	 * @param cause   The throwable that caused the logging (may be NULL)
	 */
	public static void log(LogLevel level, String message, Throwable cause) {
		logImpl(level, cause, "%s", message, NO_ARGS);
	}

	/**
	 * Internal method to log a message at a certain log level with a causing
	 * exception. This method must always be invoked directly by all public log
	 * methods to ensure the same stack overhead in all log records.
	 *
	 * @param level         The log level
	 * @param cause         The throwable that caused the logging (may be NULL)
	 * @param messageFormat The format string for the log message
	 * @param messageValues The log message values to be inserted into the
	 *                      format string
	 */
	private static void logImpl(LogLevel level, Throwable cause,
		String messageFormat, Object... messageValues) {
		LogRecord logRecord = null;

		if (globalLevelFilter.isLevelEnabled(level)) {
			logRecord =
				new LogRecord(level, cause, messageFormat, messageValues);

			Consumer<? super LogRecord> logHandler = getLogHandler(logRecord);

			if (logHandler != null) {
				// synchronize on log handler to process requests sequentially
				synchronized (logHandler) {
					logHandler.accept(logRecord);
				}
			}
		}
	}

	/**
	 * Generic method to log a formatted message at a certain log level.
	 *
	 * @param level  The log level
	 * @param format The format string of the message to log
	 * @param args   The optional arguments to add to the message
	 */
	public static void logf(LogLevel level, String format, Object... args) {
		logImpl(level, null, format, args);
	}

	/**
	 * Generic method to log a formatted message at a certain log level.
	 *
	 * @param level  The log level
	 * @param cause  The throwable that caused the logging (may be NULL)
	 * @param format The format string of the message to log
	 * @param args   The optional arguments to add to the message
	 */
	public static void logf(LogLevel level, Throwable cause, String format,
		Object... args) {
		logImpl(level, cause, format, args);
	}

	/**
	 * Registers a log handler to be used for the logging of events from a
	 * certain class. All logging calls from that class will be logged through
	 * that handler.
	 *
	 * @param type       The root package to register the handler for
	 * @param newHandler The log handler to register or NULL to remove the log
	 *                   handler for the given class
	 */
	public static void registerLogHandler(Class<?> type,
		Consumer<? super LogRecord> newHandler) {
		registerLogHandler(type.getName(), newHandler);
	}

	/**
	 * Registers a log handler to be used for the logging of events from a
	 * certain package. All logging calls from classes in and below that
	 * package
	 * will be logged through that handler.
	 *
	 * @param pkg        The root package to register the handler for
	 * @param newHandler The log handler to register or NULL to remove the log
	 *                   handler for the given package
	 */
	public static void registerLogHandler(Package pkg,
		Consumer<? super LogRecord> newHandler) {
		registerLogHandler(pkg.getName(), newHandler);
	}

	/**
	 * Registers a log handler to be used for the logging of events from a
	 * certain package (including sub-packages) or a single class. If a package
	 * is registered all logging calls from classes in and below that package
	 * will be logged through the registered handler.
	 *
	 * @param packageOrClass The name of the package or class to register the
	 *                       handler for
	 * @param newHandler     The log handler to register or NULL to unregister
	 *                       the current log handler for the given name
	 */
	private static void registerLogHandler(String packageOrClass,
		Consumer<? super LogRecord> newHandler) {
		synchronized (logHandlerCache) {
			if (newHandler != null) {
				logHandlerRegistry.put(packageOrClass, newHandler);
			} else {
				logHandlerRegistry.remove(packageOrClass);
			}

			logHandlerCache.clear();
		}
	}

	/**
	 * Removes a log handler from the default log handlers.
	 *
	 * @param handler The default log handler to remove
	 */
	public static void removeDefaultLogHandler(
		Consumer<? super LogRecord> handler) {
		synchronized (logHandlerCache) {
			List<Consumer<? super LogRecord>> functions =
				defaultLogHandlers.getMembers();

			functions.remove(handler);
			defaultLogHandlers = new Group<>(functions);
			logHandlerCache.clear();
		}
	}

	/**
	 * Removes a log aspect with a certain type. The log aspect will be shut
	 * down and then removed from the log aspect registry. If no log aspect
	 * with
	 * the given type has been registered the call will be ignored.
	 *
	 * @param logAspectType The class of the log aspect to remove
	 */
	public static void removeLogAspect(
		Class<? extends LogAspect<?>> logAspectType) {
		if (logAspects != null) {
			LogAspect<?> logAspect = logAspects.remove(logAspectType);

			if (logAspect != null) {
				logAspect.shutdownLogging();
			}
		}
	}

	/**
	 * Removes the standard log handler (@see
	 * {@link #getStandardLogHandler()}).
	 */
	public static void removeStandardLogHandler() {
		removeDefaultLogHandler(standardLogHandler);
	}

	/**
	 * Enables certain global log levels. Independent of the log handler for a
	 * certain class only messages with one of these levels will be logged, all
	 * other levels will be disabled. The ordering of log levels is ignored by
	 * this method. If no log levels are provided (i.e. the argument list is
	 * empty) all levels except FATAL will be disabled. The log level FATAL
	 * cannot be disabled globally.
	 *
	 * @param levels The minimum level to log
	 */
	public static void setGlobalLogLevels(LogLevel... levels) {
		globalLevelFilter = LogLevelFilter.isLevel(EnumSet.of(FATAL, levels));
	}

	/**
	 * Sets the minimum global log level (= severity). Independent of the log
	 * handler for a certain class only messages with this level or greater
	 * will
	 * be logged. The order of log levels from lower to higher severity is
	 * TRACE, DEBUG, INFO, WARN, ERROR, FATAL. The log level FATAL cannot be
	 * disabled globally.
	 *
	 * @param level The minimum level to log
	 */
	public static void setGlobalMinimumLogLevel(LogLevel level) {
		globalLevelFilter = LogLevelFilter.startingAt(level);
	}

	/**
	 * Sets the minimum log level for a certain class or package. This will
	 * only
	 * have an effect if the global log level is lower than the given package
	 * level. This allows to reduce the log output for certain classes or
	 * (parent) packages.
	 *
	 * @param packageOrClass The name of the class or package to set the log
	 *                       level for
	 * @param level          The minimum log level for the given package or
	 *                          NULL
	 *                       to remove any special log handling for the given
	 *                       name
	 */
	public static void setLogLevel(String packageOrClass, LogLevel level) {
		// first clear any previous handler
		registerLogHandler(packageOrClass, null);

		if (level != null) {
			registerLogHandler(packageOrClass,
				doIf(LogLevelFilter.startingAt(level),
					findLogHandler(packageOrClass)));
		}
	}

	/**
	 * Evaluates the system property 'esoco.log.plevels' and registers the
	 * package-specific log level handlers if such exist.
	 */
	private static void setupPackageLogHandlers() {
		String packageLevels = System.getProperty("esoco.log.plevels");

		if (packageLevels != null) {
			String[] pkgLevels = packageLevels.split(",");

			for (String packageLevel : pkgLevels) {
				String[] pgkLevel = packageLevel.split("=");

				if (pgkLevel.length != 2) {
					throw new IllegalArgumentException(
						"Invalid package log pgkLevel: " + packageLevel);
				}

				String packageOrClass = pgkLevel[0];
				LogLevel logLevel = LogLevel.valueOf(pgkLevel[1]);

				if (packageOrClass == null) {
					throw new IllegalArgumentException(
						String.format("Invalid log package definition: %s",
							packageLevel));
				}

				setLogLevel(packageOrClass, logLevel);
			}
		}
	}

	/**
	 * Performs the static setup of the standard log handler.
	 */
	private static void setupStandardLogHandler() {
		String level = System.getProperty("esoco.log.level");
		String file = System.getProperty("esoco.log.file");
		PrintWriter out = new PrintWriter(System.out);

		if (file != null) {
			try {
				out = new PrintWriter(new FileWriter(file, true));
			} catch (IOException e) {
				System.err.println(
					"Log file not found, reverting to System.out");
				e.printStackTrace();
			}
		}

		LogRecordFormat stackTop = new LogRecordFormat("  at {stacktop}");

		Consumer<LogRecord> standardLog =
			asConsumer(println(out, "%s").from(DEFAULT_FORMAT));

		Consumer<LogRecord> traceLog =
			doIf(isLevel(TRACE),
				asConsumer(println(out, "%s").from(stackTop)));

		Consumer<LogRecord> causeLog = doIfElse(LogRecord.HAS_CAUSE,
			asConsumer(println(out, "%s").from(CAUSE_TRACE)), traceLog);

		standardLogHandler = asConsumer(Group.of(standardLog, causeLog));
		defaultLogHandlers = Group.of(standardLogHandler);

		if (level != null) {
			try {
				setGlobalMinimumLogLevel(LogLevel.valueOf(level));
			} catch (Exception e) {
				Log.error("Invalid log level system property: " + level, e);
			}
		}
	}

	/**
	 * Logs a message at trace log level.
	 *
	 * @param message The log message
	 */
	public static void trace(String message) {
		logImpl(TRACE, null, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a message and an exception at trace log level.
	 *
	 * @param message The log message
	 * @param cause   The causing exception
	 */
	public static void trace(String message, Throwable cause) {
		logImpl(TRACE, cause, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a formatted message and an exception at trace log level.
	 *
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void tracef(String format, Object... args) {
		logImpl(TRACE, null, format, args);
	}

	/**
	 * Logs a formatted message at trace log level.
	 *
	 * @param cause  The causing exception (may be NULL)
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void tracef(Throwable cause, String format, Object... args) {
		logImpl(TRACE, cause, format, args);
	}

	/**
	 * Logs a message at warn log level.
	 *
	 * @param message The message
	 */
	public static void warn(String message) {
		logImpl(WARN, null, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a message and an exception at warn log level.
	 *
	 * @param message The log message
	 * @param cause   The causing exception
	 */
	public static void warn(String message, Throwable cause) {
		logImpl(WARN, cause, "%s", message, NO_ARGS);
	}

	/**
	 * Logs a formatted message at warn log level.
	 *
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void warnf(String format, Object... args) {
		logImpl(WARN, null, format, args);
	}

	/**
	 * Logs a formatted message and an exception at warn log level.
	 *
	 * @param cause  The causing exception (may be NULL)
	 * @param format The format of the log message
	 * @param args   The optional arguments to format as the log message
	 */
	public static void warnf(Throwable cause, String format, Object... args) {
		logImpl(WARN, cause, format, args);
	}
}
