//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.expression.Predicate;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Package-internal data object for log records.
 *
 * @author eso
 */
public final class LogRecord {

	/**
	 * Filter that returns TRUE if the evaluated record has the cause field set
	 */
	public static final Predicate<LogRecord> HAS_CAUSE =
		r -> r.getCause() != null;

	private static final int MAX_CAUSE_STACK_SIZE = 50;

	// stack frames to be omitted from returned stacks
	private static int stackOverhead = -1;

	private final LogLevel level;

	private final String messageFormat;

	private final Object[] messageValues;

	private final Throwable cause;

	private final long time;

	private final Thread logThread;

	private final StackTraceElement[] logStack;

	/**
	 * Creates a new instance for a certain log level, message, and an error
	 * object of type Throwable. The log time is initialized to the current
	 * system time.
	 *
	 * @param level         The log level this record has been logged for
	 * @param cause         The log cause (may be NULL)
	 * @param messageFormat The format string for the log message
	 * @param messageValues The log message values to be inserted into the
	 *                      format string or NULL if no formatting is necessary
	 */
	public LogRecord(LogLevel level, Throwable cause, String messageFormat,
		Object... messageValues) {
		this.level = level;
		this.cause = cause;
		this.messageFormat = messageFormat;
		this.messageValues = messageValues;
		this.time = System.currentTimeMillis();
		logThread = Thread.currentThread();

		StackTraceElement[] stackTrace = logThread.getStackTrace();

		if (stackOverhead == -1) {
			stackOverhead =
				getStackOverhead(getClass().getPackage(), stackTrace);
		}

		int length = stackTrace.length - stackOverhead;

		logStack = new StackTraceElement[length];
		System.arraycopy(stackTrace, stackOverhead, logStack, 0, length);
	}

	/**
	 * Returns the index of the first entry on top of a stack trace that
	 * doesn't
	 * refer to a certain package. Any elements on to of the stack that
	 * refer to
	 * different packages will also be skipped. That means the the given
	 * package
	 * must occur at least once in the stack trace or else the returned value
	 * will equal the stack size.
	 *
	 * @param pkg        The package to search for
	 * @param stackTrace The stack trace to analyze
	 * @return The index of the first stack trace element after the last entry
	 * with the given package
	 */
	public static int getStackOverhead(Package pkg,
		StackTraceElement[] stackTrace) {
		String packageName = pkg.getName();
		int max = stackTrace.length - 1;
		int overhead = 0;

		while (overhead < max &&
			!stackTrace[overhead].getClassName().startsWith(packageName)) {
			overhead++;
		}

		while (overhead < max &&
			stackTrace[overhead].getClassName().startsWith(packageName)) {
			overhead++;
		}

		return overhead;
	}

	/**
	 * Converts this record into a string with the same format as the standard
	 * log handler.
	 *
	 * @param minStackLevel The minimum log level for which to add a stack
	 *                      trace
	 * @return The formatted log record string
	 */
	public String format(LogLevel minStackLevel) {
		String log = Log.DEFAULT_FORMAT.apply(this);

		if (level.compareTo(minStackLevel) >= 0) {
			log += "\n" + Log.CAUSE_TRACE.evaluate(this);
		}

		return log;
	}

	/**
	 * Returns the error that caused the log record to be created.
	 *
	 * @return The error causing the log record (may be NULL)
	 */
	public Throwable getCause() {
		return cause;
	}

	/**
	 * Returns the full stack trace of the exception that caused the log entry.
	 * This will include the stack traces of all causing exceptions.
	 *
	 * @return An array of stack trace elements
	 */
	public List<String> getCauseStackTrace() {
		List<String> fullStackTrace = new ArrayList<String>();
		Throwable e = cause;
		String prefix = "\t| ";

		while (e != null) {
			int stackElements = 0;

			fullStackTrace.add("   Caused by " + e);

			for (StackTraceElement stackTraceElement : e.getStackTrace()) {
				fullStackTrace.add(prefix + stackTraceElement.toString());

				if (++stackElements >= MAX_CAUSE_STACK_SIZE) {
					break;
				}
			}

			e = e.getCause();
		}

		return fullStackTrace;
	}

	/**
	 * Returns the log level this record is logged at.
	 *
	 * @return The log level
	 */
	public LogLevel getLevel() {
		return level;
	}

	/**
	 * Returns the line number in the code that caused the log entry.
	 *
	 * @return The line number
	 */
	public int getLineNumber() {
		return getLogLocation().getLineNumber();
	}

	/**
	 * Returns the class which caused the log entry.
	 *
	 * @return The log class name
	 */
	public Class<?> getLogClass() {
		try {
			return Class.forName(getLogLocation().getClassName());
		} catch (ClassNotFoundException e) {
			// should never happen because it's a valid stack trace
			throw new AssertionError();
		}
	}

	/**
	 * Returns the name (without package) of the class which caused the log
	 * entry.
	 *
	 * @return The log class name
	 */
	public String getLogClassName() {
		String name = getLogLocation().getClassName();

		return name.substring(name.lastIndexOf('.') + 1);
	}

	/**
	 * Returns the name of the class which caused the log entry.
	 *
	 * @return The Log Class
	 */
	public StackTraceElement getLogLocation() {
		return logStack[0];
	}

	/**
	 * Returns the name of the method which caused the log entry.
	 *
	 * @return The log method name
	 */
	public String getLogMethod() {
		return getLogLocation().getMethodName();
	}

	/**
	 * Returns the name of the package from which the log entry originated.
	 *
	 * @return The package name
	 */
	public String getLogPackage() {
		String className = getLogLocation().getClassName();
		int pos = className.lastIndexOf('.');

		if (pos > 0) {
			return className.substring(0, pos);
		} else {
			return "";
		}
	}

	/**
	 * Returns the stack trace of the code that caused the log entry. This
	 * returns the internal array of stack trace elements and must therefore
	 * not
	 * be modified.
	 *
	 * @return An array of stack trace elements
	 */
	public StackTraceElement[] getLogStackTrace() {
		return logStack;
	}

	/**
	 * Returns the thread from which the logging occurred.
	 *
	 * @return The log thread
	 */
	public Thread getLogThread() {
		return logThread;
	}

	/**
	 * Returns the formatted log message.
	 *
	 * @return The log message
	 */
	public String getMessage() {
		return messageValues != null ?
		       String.format(messageFormat, messageValues) :
		       messageFormat;
	}

	/**
	 * Returns the log message format.
	 *
	 * @return The log message format
	 */
	public String getMessageFormat() {
		return messageFormat;
	}

	/**
	 * Returns the log message values.
	 *
	 * @return The log message values
	 */
	public Object[] getMessageValues() {
		return messageValues;
	}

	/**
	 * Returns the name of the source file containing the code that caused the
	 * log entry.
	 *
	 * @return The source file name
	 */
	public String getSourceFileName() {
		return getLogLocation().getFileName();
	}

	/**
	 * Returns the time of the log record in milliseconds.
	 *
	 * @return The log time in milliseconds
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Returns a string description of this record.
	 *
	 * @return The string description
	 */
	@Override
	public String toString() {

		String sb = "LogRecord[" + level + ',' +
			DateFormat.getInstance().format(new Date(time)) + ',' + "\"" +
			getMessage() + "\"]";

		return sb;
	}
}
