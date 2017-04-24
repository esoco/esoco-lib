//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.expression.Function;
import de.esoco.lib.expression.Functions;
import de.esoco.lib.expression.function.FunctionGroup;
import de.esoco.lib.reflect.ReflectUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

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


/********************************************************************
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
public final class Log
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * A relation type that contains a {@link LogExtent}. Defaults to {@link
	 * LogExtent#ERRORS}.
	 */
	public static final RelationType<LogExtent> LOG_EXTENT =
		newInitialValueType(LogExtent.ERRORS);

	/**
	 * A relation type that contains a {@link LogLevel}. Defaults to {@link
	 * LogLevel#ERROR}.
	 */
	public static final RelationType<LogLevel> LOG_LEVEL =
		newInitialValueType(LogLevel.ERROR);

	/** The default log string transformation for log output */
	public static final LogRecordFormat DEFAULT_FORMAT =
		new LogRecordFormat("[{level:F%-5s}]{t:Dyyyy.MM.dd-HH:mm:ss}: {message}  [{package}.{class}.{method}() [{line}]]");

	/**
	 * A log string transformation for the full exception stacktrace, including
	 * _all_ causing exceptions! See {@link LogRecord#getCauseStackTrace()} for
	 * details.
	 */
	public static final LogRecordFormat CAUSE_TRACE =
		new LogRecordFormat("{getCauseStackTrace():F%s\n}");

	private static final Object[] NO_ARGS = null;

	private static Map<String, Function<? super LogRecord, ?>> aLogHandlerRegistry =
		new HashMap<String, Function<? super LogRecord, ?>>();
	private static Map<String, Function<? super LogRecord, ?>> aLogHandlerCache    =
		new HashMap<String, Function<? super LogRecord, ?>>();

	private static Function<LogRecord, ?>   aStandardLogHandler;
	private static FunctionGroup<LogRecord> aDefaultLogHandlers;

	private static LogLevelFilter aGlobalLevelFilter =
		LogLevelFilter.startingAt(ERROR);

	@SuppressWarnings("rawtypes")
	private static Map<Class<? extends LogAspect>, LogAspect<?>> aLogAspects;

	static
	{
		RelationTypes.init(Log.class);
		setupDefaultLogHandler();
		setupPackageLogHandlers();
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Adds an additional default log handler to the handlers that will be used
	 * if no special handler has been registered for the package or class that a
	 * log call originates from.
	 *
	 * @param  rHandler An additional default log handler
	 *
	 * @throws NullPointerException If the argument is NULL
	 */
	public static void addDefaultLogHandler(
		Function<? super LogRecord, ?> rHandler)
	{
		Objects.requireNonNull(rHandler);

		synchronized (aLogHandlerCache)
		{
			List<Function<? super LogRecord, ?>> rFunctions =
				aDefaultLogHandlers.getFunctions();

			rFunctions.add(rHandler);
			aDefaultLogHandlers = new FunctionGroup<>(rFunctions);

			aLogHandlerCache.clear();
		}
	}

	/***************************************
	 * Adds a certain log aspect. The log aspect will be registered and
	 * initialized to start logging. If a log aspect with the same class already
	 * exists it will be shut down and removed first.
	 *
	 * @param rLogAspect The log aspect to register
	 */
	public static void addLogAspect(LogAspect<?> rLogAspect)
	{
		@SuppressWarnings("unchecked")
		Class<? extends LogAspect<?>> rLogAspectType =
			(Class<? extends LogAspect<?>>) rLogAspect.getClass();

		if (aLogAspects == null)
		{
			aLogAspects = new HashMap<>();
		}
		else
		{
			removeLogAspect(rLogAspectType);
		}

		aLogAspects.put(rLogAspectType, rLogAspect);
		rLogAspect.initLogging();
	}

	/***************************************
	 * Logs a message at debug log level.
	 *
	 * @param sMessage The log message
	 */
	public static void debug(String sMessage)
	{
		logImpl(DEBUG, null, sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a message and an exception at debug log level.
	 *
	 * @param sMessage The log message
	 * @param rCause   The causing exception
	 */
	public static void debug(String sMessage, Throwable rCause)
	{
		logImpl(DEBUG, rCause, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a formatted message at debug log level.
	 *
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void debugf(String sFormat, Object... rArgs)
	{
		logImpl(DEBUG, null, sFormat, rArgs);
	}

	/***************************************
	 * Logs a formatted message and an exception at debug log level.
	 *
	 * @param rCause  The causing exception (may be NULL)
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void debugf(Throwable rCause, String sFormat, Object... rArgs)
	{
		logImpl(DEBUG, rCause, sFormat, rArgs);
	}

	/***************************************
	 * Logs a message at error log level.
	 *
	 * @param sMessage The log message
	 */
	public static void error(String sMessage)
	{
		logImpl(ERROR, null, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a message and an exception at error log level.
	 *
	 * @param sMessage The log message
	 * @param rCause   The causing exception
	 */
	public static void error(String sMessage, Throwable rCause)
	{
		logImpl(ERROR, rCause, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a formatted message at error log level.
	 *
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void errorf(String sFormat, Object... rArgs)
	{
		logImpl(ERROR, null, sFormat, rArgs);
	}

	/***************************************
	 * Logs a formatted message and an exception at error log level.
	 *
	 * @param rCause  The causing exception (may be NULL)
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void errorf(Throwable rCause, String sFormat, Object... rArgs)
	{
		logImpl(ERROR, rCause, sFormat, rArgs);
	}

	/***************************************
	 * Logs a message at fatal log level.
	 *
	 * @param sMessage The log message
	 */
	public static void fatal(String sMessage)
	{
		logImpl(FATAL, null, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a message and an exception at fatal log level.
	 *
	 * @param sMessage The log message
	 * @param rCause   The causing exception
	 */
	public static void fatal(String sMessage, Throwable rCause)
	{
		logImpl(FATAL, rCause, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a formatted message at fatal log level.
	 *
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void fatalf(String sFormat, Object... rArgs)
	{
		logImpl(FATAL, null, sFormat, rArgs);
	}

	/***************************************
	 * Logs a formatted message and an exception at fatal log level.
	 *
	 * @param rCause  The causing exception (may be NULL)
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void fatalf(Throwable rCause, String sFormat, Object... rArgs)
	{
		logImpl(FATAL, rCause, sFormat, rArgs);
	}

	/***************************************
	 * Returns the global minimum log level that is currently set.
	 *
	 * @return The global minimum log level
	 */
	public static LogLevel getGlobalMinimumLogLevel()
	{
		return aGlobalLevelFilter.getMinimumLevel();
	}

	/***************************************
	 * Returns the log handler that has been registered for a certain package or
	 * class.
	 *
	 * @param  sPackageOrClass The name of the package or class to lookup the
	 *                         log handler for
	 *
	 * @return The log handler for the given name or NULL for none
	 */
	public static Function<? super LogRecord, ?> getRegisteredLogHandler(
		String sPackageOrClass)
	{
		return aLogHandlerRegistry.get(sPackageOrClass);
	}

	/***************************************
	 * Returns the standard log handler that performs output to System.out. To
	 * disabled this handler it can be removed by invoking the method {@link
	 * #removeDefaultLogHandler(Function)} with the result of this method.
	 *
	 * @return The standard log handler
	 */
	public static Function<LogRecord, ?> getStandardLogHandler()
	{
		return aStandardLogHandler;
	}

	/***************************************
	 * Logs a message at info log level.
	 *
	 * @param sMessage The log message
	 */
	public static void info(String sMessage)
	{
		logImpl(INFO, null, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a message and an exception at info log level.
	 *
	 * @param sMessage The log message
	 * @param rCause   The causing exception
	 */
	public static void info(String sMessage, Throwable rCause)
	{
		logImpl(INFO, rCause, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a formatted message at info log level.
	 *
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void infof(String sFormat, Object... rArgs)
	{
		logImpl(INFO, null, sFormat, rArgs);
	}

	/***************************************
	 * Logs a formatted message and an exception at info log level.
	 *
	 * @param rCause  The causing exception (may be NULL)
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void infof(Throwable rCause, String sFormat, Object... rArgs)
	{
		logImpl(INFO, rCause, sFormat, rArgs);
	}

	/***************************************
	 * Checks if the argument log level is enabled globally for logging.
	 *
	 * @param  rLevel The log level to check
	 *
	 * @return TRUE if the level is enabled globally for logging
	 *
	 * @see    #setGlobalLogLevels(LogLevel[])
	 * @see    #setGlobalMinimumLogLevel(LogLevel)
	 */
	public static boolean isLevelEnabled(LogLevel rLevel)
	{
		return aGlobalLevelFilter.isLevelEnabled(rLevel);
	}

	/***************************************
	 * Generic method to log a message at a certain log level.
	 *
	 * @param rLevel   The log level
	 * @param sMessage The message to log
	 */
	public static void log(LogLevel rLevel, String sMessage)
	{
		logImpl(rLevel, null, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Generic method to log a message at a certain log level with a causing
	 * exception.
	 *
	 * @param rLevel   The log level
	 * @param sMessage The message to log
	 * @param rCause   The throwable that caused the logging (may be NULL)
	 */
	public static void log(LogLevel rLevel, String sMessage, Throwable rCause)
	{
		logImpl(rLevel, rCause, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Generic method to log a formatted message at a certain log level.
	 *
	 * @param rLevel  The log level
	 * @param sFormat The format string of the message to log
	 * @param rArgs   The optional arguments to add to the message
	 */
	public static void logf(LogLevel rLevel, String sFormat, Object... rArgs)
	{
		logImpl(rLevel, null, sFormat, rArgs);
	}

	/***************************************
	 * Generic method to log a formatted message at a certain log level.
	 *
	 * @param rLevel  The log level
	 * @param rCause  The throwable that caused the logging (may be NULL)
	 * @param sFormat The format string of the message to log
	 * @param rArgs   The optional arguments to add to the message
	 */
	public static void logf(LogLevel  rLevel,
							Throwable rCause,
							String    sFormat,
							Object... rArgs)
	{
		logImpl(rLevel, rCause, sFormat, rArgs);
	}

	/***************************************
	 * Registers a log handler to be used for the logging of events from a
	 * certain class. All logging calls from that class will be logged through
	 * that handler.
	 *
	 * @param rClass      The root package to register the handler for
	 * @param rNewHandler The log handler to register or NULL to remove the log
	 *                    handler for the given class
	 */
	public static void registerLogHandler(
		Class<?>					   rClass,
		Function<? super LogRecord, ?> rNewHandler)
	{
		registerLogHandler(rClass.getName(), rNewHandler);
	}

	/***************************************
	 * Registers a log handler to be used for the logging of events from a
	 * certain package. All logging calls from classes in and below that package
	 * will be logged through that handler.
	 *
	 * @param rPackage    The root package to register the handler for
	 * @param rNewHandler The log handler to register or NULL to remove the log
	 *                    handler for the given package
	 */
	public static void registerLogHandler(
		Package						   rPackage,
		Function<? super LogRecord, ?> rNewHandler)
	{
		registerLogHandler(rPackage.getName(), rNewHandler);
	}

	/***************************************
	 * Removes a log handler from the default log handlers.
	 *
	 * @param rHandler The default log handler to remove
	 */
	public static void removeDefaultLogHandler(
		Function<? super LogRecord, ?> rHandler)
	{
		synchronized (aLogHandlerCache)
		{
			List<Function<? super LogRecord, ?>> rFunctions =
				aDefaultLogHandlers.getFunctions();

			rFunctions.remove(rHandler);
			aDefaultLogHandlers = new FunctionGroup<>(rFunctions);
			aLogHandlerCache.clear();
		}
	}

	/***************************************
	 * Removes a log aspect with a certain type. The log aspect will be shut
	 * down and then removed from the log aspect registry. If no log aspect with
	 * the given type has been registered the call will be ignored.
	 *
	 * @param rLogAspectType The class of the log aspect to remove
	 */
	public static void removeLogAspect(
		Class<? extends LogAspect<?>> rLogAspectType)
	{
		if (aLogAspects != null)
		{
			LogAspect<?> rLogAspect = aLogAspects.remove(rLogAspectType);

			if (rLogAspect != null)
			{
				rLogAspect.shutdownLogging();
			}
		}
	}

	/***************************************
	 * Enables certain global log levels. Independent of the log handler for a
	 * certain class only messages with one of these levels will be logged, all
	 * other levels will be disabled. The ordering of log levels is ignored by
	 * this method. If no log levels are provided (i.e. the argument list is
	 * empty) all levels except FATAL will be disabled. The log level FATAL
	 * cannot be disabled globally.
	 *
	 * @param rLevels The minimum level to log
	 */
	public static void setGlobalLogLevels(LogLevel... rLevels)
	{
		aGlobalLevelFilter = LogLevelFilter.isLevel(EnumSet.of(FATAL, rLevels));
	}

	/***************************************
	 * Sets the minimum global log level (= severity). Independent of the log
	 * handler for a certain class only messages with this level or greater will
	 * be logged. The order of log levels from lower to higher severity is
	 * TRACE, DEBUG, INFO, WARN, ERROR, FATAL. The log level FATAL cannot be
	 * disabled globally.
	 *
	 * @param rLevel The minimum level to log
	 */
	public static void setGlobalMinimumLogLevel(LogLevel rLevel)
	{
		aGlobalLevelFilter = LogLevelFilter.startingAt(rLevel);
	}

	/***************************************
	 * Sets the minimum log level for a certain class or package. This will only
	 * have an effect if the global log level is lower than the given package
	 * level. This allows to reduce the log output for certain classes or
	 * (parent) packages.
	 *
	 * @param sPackageOrClass The name of the class or package to set the log
	 *                        level for
	 * @param eLevel          The minimum log level for the given package or
	 *                        NULL to remove any special log handling for the
	 *                        given name
	 */
	public static void setLogLevel(String sPackageOrClass, LogLevel eLevel)
	{
		// first clear any previous handler
		registerLogHandler(sPackageOrClass, null);

		if (eLevel != null)
		{
			registerLogHandler(sPackageOrClass,
							   Functions.doIf(LogLevelFilter.startingAt(eLevel),
											  findLogHandler(sPackageOrClass)));
		}
	}

	/***************************************
	 * Logs a message at trace log level.
	 *
	 * @param sMessage The log message
	 */
	public static void trace(String sMessage)
	{
		logImpl(TRACE, null, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a message and an exception at trace log level.
	 *
	 * @param sMessage The log message
	 * @param rCause   The causing exception
	 */
	public static void trace(String sMessage, Throwable rCause)
	{
		logImpl(TRACE, rCause, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a formatted message and an exception at trace log level.
	 *
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void tracef(String sFormat, Object... rArgs)
	{
		logImpl(TRACE, null, sFormat, rArgs);
	}

	/***************************************
	 * Logs a formatted message at trace log level.
	 *
	 * @param rCause  The causing exception (may be NULL)
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void tracef(Throwable rCause, String sFormat, Object... rArgs)
	{
		logImpl(TRACE, rCause, sFormat, rArgs);
	}

	/***************************************
	 * Logs a message at warn log level.
	 *
	 * @param sMessage The message
	 */
	public static void warn(String sMessage)
	{
		logImpl(WARN, null, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a message and an exception at warn log level.
	 *
	 * @param sMessage The log message
	 * @param rCause   The causing exception
	 */
	public static void warn(String sMessage, Throwable rCause)
	{
		logImpl(WARN, rCause, "%s", sMessage, NO_ARGS);
	}

	/***************************************
	 * Logs a formatted message at warn log level.
	 *
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void warnf(String sFormat, Object... rArgs)
	{
		logImpl(WARN, null, sFormat, rArgs);
	}

	/***************************************
	 * Logs a formatted message and an exception at warn log level.
	 *
	 * @param rCause  The causing exception (may be NULL)
	 * @param sFormat The format of the log message
	 * @param rArgs   The optional arguments to format as the log message
	 */
	public static void warnf(Throwable rCause, String sFormat, Object... rArgs)
	{
		logImpl(WARN, rCause, sFormat, rArgs);
	}

	/***************************************
	 * Looks up the log handler that has been registered to perform logging
	 * calls from a certain package. If no direct handler can be found this
	 * method recursively searches for handlers registered for parent packages.
	 * If no handler has been registered at all the default handler will be
	 * returned.
	 *
	 * @param  sPackageOrClass The name of the package to lookup the log handler
	 *                         for
	 *
	 * @return The handler to be used to perform logging from the given package
	 */
	private static Function<? super LogRecord, ?> findLogHandler(
		String sPackageOrClass)
	{
		Function<? super LogRecord, ?> rHandler = aDefaultLogHandlers;

		if (sPackageOrClass != null)
		{
			rHandler = aLogHandlerRegistry.get(sPackageOrClass);

			if (rHandler == null)
			{
				sPackageOrClass = ReflectUtil.getNamespace(sPackageOrClass);
				rHandler	    = findLogHandler(sPackageOrClass);
			}
		}

		return rHandler;
	}

	/***************************************
	 * Internal method to return a log handler for a certain log record. If no
	 * matching handler exists a new one will be created. Performs thread
	 * synchronization.
	 *
	 * @param  rRecord The log record to return the handler for
	 *
	 * @return The log handler
	 */
	private static Function<? super LogRecord, ?> getLogHandler(
		LogRecord rRecord)
	{
		String sClassName = rRecord.getLogClass().getName();

		Function<? super LogRecord, ?> rLogHandler = null;

		synchronized (aLogHandlerCache)
		{
			rLogHandler = aLogHandlerCache.get(sClassName);

			if (rLogHandler == null)
			{
				rLogHandler = findLogHandler(sClassName);
				aLogHandlerCache.put(sClassName, rLogHandler);
			}
		}

		return rLogHandler;
	}

	/***************************************
	 * Internal method to log a message at a certain log level with a causing
	 * exception. This method must always be invoked directly by all public log
	 * methods to ensure the same stack overhead in all log records.
	 *
	 * @param eLevel         The log level
	 * @param rCause         The throwable that caused the logging (may be NULL)
	 * @param sMessageFormat The format string for the log message
	 * @param rMessageValues The log message values to be inserted into the
	 *                       format string
	 */
	private static void logImpl(LogLevel  eLevel,
								Throwable rCause,
								String    sMessageFormat,
								Object... rMessageValues)
	{
		LogRecord aLogRecord = null;

		if (aGlobalLevelFilter.isLevelEnabled(eLevel))
		{
			aLogRecord =
				new LogRecord(eLevel, rCause, sMessageFormat, rMessageValues);

			Function<? super LogRecord, ?> rLogHandler =
				getLogHandler(aLogRecord);

			if (rLogHandler != null)
			{
				// synchronize on log handler to process requests sequentially
				synchronized (rLogHandler)
				{
					rLogHandler.evaluate(aLogRecord);
				}
			}
		}
	}

	/***************************************
	 * Registers a log handler to be used for the logging of events from a
	 * certain package (including sub-packages) or a single class. If a package
	 * is registered all logging calls from classes in and below that package
	 * will be logged through the registered handler.
	 *
	 * @param sPackageOrClass The name of the package or class to register the
	 *                        handler for
	 * @param rNewHandler     The log handler to register or NULL to unregister
	 *                        the current log handler for the given name
	 */
	private static void registerLogHandler(
		String						   sPackageOrClass,
		Function<? super LogRecord, ?> rNewHandler)
	{
		synchronized (aLogHandlerCache)
		{
			if (rNewHandler != null)
			{
				aLogHandlerRegistry.put(sPackageOrClass, rNewHandler);
			}
			else
			{
				aLogHandlerRegistry.remove(sPackageOrClass);
			}

			aLogHandlerCache.clear();
		}
	}

	/***************************************
	 * Performs the static setup of the default and standard log handlers.
	 */
	private static void setupDefaultLogHandler()
	{
		String	    sLevel = System.getProperty("esoco.log.level");
		String	    sFile  = System.getProperty("esoco.log.file");
		PrintWriter rOut   = new PrintWriter(System.out);

		if (sFile != null)
		{
			try
			{
				rOut = new PrintWriter(new FileWriter(sFile, true));
			}
			catch (IOException e)
			{
				System.err.println("Log file not found, reverting to System.out");
				e.printStackTrace();
			}
		}

		LogRecordFormat aStackTop = new LogRecordFormat("  at {stacktop}");

		Function<LogRecord, Object> aStandardLog =
			println(rOut, "%s").from(DEFAULT_FORMAT);

		Function<LogRecord, Object> aTraceLog =
			doIf(isLevel(TRACE), println(rOut, "%s").from(aStackTop));

		Function<LogRecord, ?> aCauseLog =
			doIfElse(LogRecord.HAS_CAUSE,
					 println(rOut, "%s").from(CAUSE_TRACE),
					 aTraceLog);

		aStandardLogHandler = Functions.doAll(aStandardLog, aCauseLog);
		aDefaultLogHandlers = FunctionGroup.of(aStandardLogHandler);

		if (sLevel != null)
		{
			try
			{
				setGlobalMinimumLogLevel(LogLevel.valueOf(sLevel));
			}
			catch (Exception e)
			{
				Log.error("Invalid log level system property: " + sLevel, e);
			}
		}
	}

	/***************************************
	 * Evaluates the system property 'esoco.log.plevels' and registers the
	 * package-specific log level handlers if such exist.
	 */
	private static void setupPackageLogHandlers()
	{
		String sPackageLevels = System.getProperty("esoco.log.plevels");

		if (sPackageLevels != null)
		{
			String[] aPackageLevels = sPackageLevels.split(",");

			for (String sPackageLevel : aPackageLevels)
			{
				String[] aPackageLevel = sPackageLevel.split("=");

				if (aPackageLevel.length != 2)
				{
					throw new IllegalArgumentException("Invalid package log level: " +
													   sPackageLevel);
				}

				String   sPackageOrClass = aPackageLevel[0];
				LogLevel eLevel			 = LogLevel.valueOf(aPackageLevel[1]);

				if (sPackageOrClass == null || eLevel == null)
				{
					throw new IllegalArgumentException(String.format("Invalid log package definition: %s",
																	 sPackageLevel));
				}

				setLogLevel(sPackageOrClass, eLevel);
			}
		}
	}
}
