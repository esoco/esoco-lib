//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.expression.predicate.AbstractPredicate;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/********************************************************************
 * Package-internal data object for log records.
 *
 * @author eso
 */
public final class LogRecord
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * Filter that returns TRUE if the evaluated record has the cause field set
	 */
	public static final AbstractPredicate<LogRecord> HAS_CAUSE =
		new AbstractPredicate<LogRecord>("LogRecord.Cause not NULL")
		{
			@Override
			@SuppressWarnings("boxing")
			public Boolean evaluate(LogRecord rRecord)
			{
				return (rRecord.getCause() != null);
			}
		};

	// stack frames to be omitted from returned stacks
	private static int nStackOverhead = -1;

	private static final int MAX_CAUSE_STACK_SIZE = 50;

	//~ Instance fields --------------------------------------------------------

	private final LogLevel			  rLevel;
	private final String			  sMessageFormat;
	private final Object[]			  rMessageValues;
	private final Throwable			  rCause;
	private final long				  nTime;
	private final Thread			  rLogThread;
	private final StackTraceElement[] aLogStack;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance for a certain log level, message, and an error
	 * object of type Throwable. The log time is initialized to the current
	 * system time.
	 *
	 * @param rLevel         The log level this record has been logged for
	 * @param rCause         The log cause (may be NULL)
	 * @param sMessageFormat The format string for the log message
	 * @param rMessageValues The log message values to be inserted into the
	 *                       format string or NULL if no formatting is necessary
	 */
	public LogRecord(LogLevel  rLevel,
					 Throwable rCause,
					 String    sMessageFormat,
					 Object... rMessageValues)
	{
		this.rLevel		    = rLevel;
		this.rCause		    = rCause;
		this.sMessageFormat = sMessageFormat;
		this.rMessageValues = rMessageValues;
		this.nTime		    = System.currentTimeMillis();
		rLogThread		    = Thread.currentThread();

		StackTraceElement[] rStackTrace = rLogThread.getStackTrace();

		if (nStackOverhead == -1)
		{
			nStackOverhead =
				getStackOverhead(getClass().getPackage(), rStackTrace);
		}

		int nLength = rStackTrace.length - nStackOverhead;

		aLogStack = new StackTraceElement[nLength];
		System.arraycopy(rStackTrace, nStackOverhead, aLogStack, 0, nLength);
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns the index of the first entry on top of a stack trace that doesn't
	 * refer to a certain package. Any elements on to of the stack that refer to
	 * different packages will also be skipped. That means the the given package
	 * must occur at least once in the stack trace or else the returned value
	 * will equal the stack size.
	 *
	 * @param  rPackage    The package to search for
	 * @param  rStackTrace The stack trace to analyze
	 *
	 * @return The index of the first stack trace element after the last entry
	 *         with the given package
	 */
	public static int getStackOverhead(
		Package				rPackage,
		StackTraceElement[] rStackTrace)
	{
		String sPackage  = rPackage.getName();
		int    nMax		 = rStackTrace.length - 1;
		int    nOverhead = 0;

		while (nOverhead < nMax &&
			   !rStackTrace[nOverhead].getClassName().startsWith(sPackage))
		{
			nOverhead++;
		}

		while (nOverhead < nMax &&
			   rStackTrace[nOverhead].getClassName().startsWith(sPackage))
		{
			nOverhead++;
		}

		return nOverhead;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the error that caused the log record to be created.
	 *
	 * @return The error causing the log record (may be NULL)
	 */
	public final Throwable getCause()
	{
		return rCause;
	}

	/***************************************
	 * Returns the full stack trace of the exception that caused the log entry.
	 * This will include the stack traces of all causing exceptions.
	 *
	 * @return An array of stack trace elements
	 */
	public final List<String> getCauseStackTrace()
	{
		List<String> aFullStackTrace = new ArrayList<String>();
		Throwable    e				 = rCause;
		String		 sPrefix		 = "\t| ";

		while (e != null)
		{
			int nStackElements = 0;

			aFullStackTrace.add("   Caused by " + e);

			for (StackTraceElement rStackTraceElement : e.getStackTrace())
			{
				aFullStackTrace.add(sPrefix + rStackTraceElement.toString());

				if (++nStackElements >= MAX_CAUSE_STACK_SIZE)
				{
					break;
				}
			}

			e = e.getCause();
		}

		return aFullStackTrace;
	}

	/***************************************
	 * Returns the log level this record is logged at.
	 *
	 * @return The log level
	 */
	public final LogLevel getLevel()
	{
		return rLevel;
	}

	/***************************************
	 * Returns the line number in the code that caused the log entry.
	 *
	 * @return The line number
	 */
	public final int getLineNumber()
	{
		return getLogLocation().getLineNumber();
	}

	/***************************************
	 * Returns the class which caused the log entry.
	 *
	 * @return The log class name
	 */
	public final Class<?> getLogClass()
	{
		try
		{
			return Class.forName(getLogLocation().getClassName());
		}
		catch (ClassNotFoundException e)
		{
			// should never happen because it's a valid stack trace
			throw new AssertionError();
		}
	}

	/***************************************
	 * Returns the name (without package) of the class which caused the log
	 * entry.
	 *
	 * @return The log class name
	 */
	public final String getLogClassName()
	{
		String name = getLogLocation().getClassName();

		return name.substring(name.lastIndexOf('.') + 1);
	}

	/***************************************
	 * Returns the name of the class which caused the log entry.
	 *
	 * @return The Log Class
	 */
	public final StackTraceElement getLogLocation()
	{
		return aLogStack[0];
	}

	/***************************************
	 * Returns the name of the method which caused the log entry.
	 *
	 * @return The log method name
	 */
	public final String getLogMethod()
	{
		return getLogLocation().getMethodName();
	}

	/***************************************
	 * Returns the name of the package from which the log entry originated.
	 *
	 * @return The package name
	 */
	public final String getLogPackage()
	{
		String sClass = getLogLocation().getClassName();
		int    nPos   = sClass.lastIndexOf('.');

		if (nPos > 0)
		{
			return sClass.substring(0, nPos);
		}
		else
		{
			return "";
		}
	}

	/***************************************
	 * Returns the stack trace of the code that caused the log entry. This
	 * returns the internal array of stack trace elements and must therefore not
	 * be modified.
	 *
	 * @return An array of stack trace elements
	 */
	public final StackTraceElement[] getLogStackTrace()
	{
		return aLogStack;
	}

	/***************************************
	 * Returns the thread from which the logging occurred.
	 *
	 * @return The log thread
	 */
	public final Thread getLogThread()
	{
		return rLogThread;
	}

	/***************************************
	 * Returns the formatted log message.
	 *
	 * @return The log message
	 */
	public final String getMessage()
	{
		return rMessageValues != null
			   ? String.format(sMessageFormat, rMessageValues) : sMessageFormat;
	}

	/***************************************
	 * Returns the log message format.
	 *
	 * @return The log message format
	 */
	public final String getMessageFormat()
	{
		return sMessageFormat;
	}

	/***************************************
	 * Returns the log message values.
	 *
	 * @return The log message values
	 */
	public final Object[] getMessageValues()
	{
		return rMessageValues;
	}

	/***************************************
	 * Returns the name of the source file containing the code that caused the
	 * log entry.
	 *
	 * @return The source file name
	 */
	public final String getSourceFileName()
	{
		return getLogLocation().getFileName();
	}

	/***************************************
	 * Returns the time of the log record in milliseconds.
	 *
	 * @return The log time in milliseconds
	 */
	public final long getTime()
	{
		return nTime;
	}

	/***************************************
	 * Returns a string description of this record.
	 *
	 * @return The string description
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("LogRecord[");

		sb.append(rLevel).append(',');
		sb.append(DateFormat.getInstance().format(new Date(nTime))).append(',');
		sb.append("\"").append(getMessage()).append("\"]");

		return sb.toString();
	}
}
