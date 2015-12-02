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

import de.esoco.lib.expression.Action;
import de.esoco.lib.expression.function.AbstractAction;
import de.esoco.lib.expression.function.AbstractFunction;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import static org.obrel.core.RelationTypes.newDefaultValueType;
import static org.obrel.core.RelationTypes.newIntType;
import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * This is a base class for the implementation of specific aspects of logging,
 * like logging to a database or external systems. The common property of
 * aspects implemented with this class is that they need to synchronize access
 * to the logging back-end through a shared resource like a socket or a database
 * connection.
 *
 * <p>Log aspects can or sometimes must be configured by setting relation types
 * on the corresponding instance. Some standard configuration types are already
 * defined in this base class:</p>
 *
 * <ul>
 *   <li>{@link #MIN_LOG_LEVEL}</li>
 *   <li>{@link #MIN_STACK_LOG_LEVEL}</li>
 *   <li>{@link #MAX_LOGGING_ERRORS}</li>
 * </ul>
 *
 * <p>{@link #MIN_STACK_LOG_LEVEL} must be evaluated by subclasses, the other
 * configurations are handled by this base class. It is recommended that
 * subclasses always contain a no-argument constructor and perform their
 * initialization in the {@link #init()} method from the configuration
 * relations. They may also offer additional parameterized constructors.</p>
 *
 * <p>The generic parameter defines the type of log message object that is used
 * by an implementation.</p>
 *
 * @author eso
 */
public abstract class LogAspect<T> extends RelatedObject
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * The minimum log level to be logged by this aspect. Will be initialized
	 * with {@link Log#getGlobalMinimumLogLevel()} if not set explicitly.
	 */
	public static final RelationType<LogLevel> MIN_LOG_LEVEL =
		newType(new AbstractFunction<Object, LogLevel>("GetGlobalMinimumLogLevel")
			{
				@Override
				public LogLevel evaluate(Object rIgnored)
				{
					return Log.getGlobalMinimumLogLevel();
				}
			});

	/**
	 * The minimum log level to be logged by this aspect that shall include the
	 * execution stack (default: {@link LogLevel#ERROR}).
	 */
	public static final RelationType<LogLevel> MIN_STACK_LOG_LEVEL =
		newDefaultValueType(LogLevel.ERROR);

	/**
	 * A configuration value that defines the maximum number of errors that may
	 * occur before this log aspect is disabled and an error is logged with the
	 * logging framework. Default value is 0 (zero), i.e. logging will be
	 * shutdown on the first error.
	 */
	public static final RelationType<Integer> MAX_LOGGING_ERRORS = newIntType();

	static
	{
		RelationTypes.init(LogAspect.class);
	}

	//~ Instance fields --------------------------------------------------------

	private boolean bLoggingInitialized = false;
	private int     nErrorCount;

	private Lock     aQueueAccessLock;
	private Queue<T> aLogQueue;

	private Action<LogRecord> fLogFunction;

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Initializes the logging of this aspect. Multiple invocations of this
	 * method will be ignored.
	 */
	public final synchronized void initLogging()
	{
		if (!bLoggingInitialized)
		{
			nErrorCount		 = 0;
			aQueueAccessLock = new ReentrantLock();
			aLogQueue		 = new ConcurrentLinkedQueue<T>();
			fLogFunction     =
				new AbstractAction<LogRecord>(getClass().getSimpleName())
				{
					@Override
					public void execute(LogRecord rLogRecord)
					{
						processLogRecord(rLogRecord);
					}
				};

			init();

			Log.infof("Log aspect %s initialized, starting logging", this);
			Log.addDefaultLogHandler(fLogFunction);

			bLoggingInitialized = true;
		}
	}

	/***************************************
	 * Stops the logging of this aspect. Multiple invocations of this method
	 * will be ignored.
	 */
	public final synchronized void shutdownLogging()
	{
		if (bLoggingInitialized)
		{
			Log.removeDefaultLogHandler(fLogFunction);
			shutdown();
			Log.infof("Log aspect %s has been shut down", this);
			bLoggingInitialized = false;
		}
	}

	/***************************************
	 * Returns the simple name of this aspect's class.
	 *
	 * @return The class name
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

	/***************************************
	 * Must be implemented by subclasses to create an implementation-specific
	 * log data object from a {@link LogRecord} instance.
	 *
	 * @param  rLogRecord The log record to convert
	 *
	 * @return The implementation log object
	 */
	protected abstract T createLogObject(LogRecord rLogRecord);

	/***************************************
	 * Must be implemented by subclasses to process a collection of log data
	 * objects in an implementation-specific way, e.g. by sending them to
	 * another system or store them in a database. The given collection will
	 * always contain at least one object. Multiple objects should always be
	 * process in the iteration order of the collection and the collection must
	 * not be modified by the implementation. The objects in the collection have
	 * all been created by {@link #createLogObject(LogRecord)}.
	 *
	 * <p>Implementations may throw any kind of exception. The log aspect base
	 * implementation will perform a generic error handling by shutting down the
	 * aspect logging and logging a FATAL message. The implementation must still
	 * perform any cleanup of acquired resources if necessary.</p>
	 *
	 * @param  rLogObjects The log objects to process
	 *
	 * @throws Exception If the processing fails
	 */
	protected abstract void processLogObjects(Collection<T> rLogObjects)
		throws Exception;

	/***************************************
	 * Will be invoked from {@link #initLogging()}. Can be implemented by
	 * subclasses to initialize the internal state that is needed for the
	 * operation of this aspect. The default implementation does nothing.
	 */
	protected void init()
	{
	}

	/***************************************
	 * Will be invoked from {@link #shutdownLogging()}. Can be implemented by
	 * subclasses to perform a cleanup of resources that had been acquired in
	 * {@link #init()} or during logging. The default implementation does
	 * nothing.
	 */
	protected void shutdown()
	{
	}

	/***************************************
	 * Processes and empties the log object queue.
	 */
	@SuppressWarnings("boxing")
	private void processLogQueue()
	{
		try
		{
			if (!aLogQueue.isEmpty())
			{
				processLogObjects(aLogQueue);
			}

			aLogQueue.clear();
		}
		catch (Exception e)
		{
			if (++nErrorCount >= get(MAX_LOGGING_ERRORS))
			{
				shutdownLogging();
				Log.fatalf(e,
						   "Log aspect %s failed, stopped after %d errors",
						   this.getClass().getSimpleName(),
						   nErrorCount);
			}
		}
	}

	/***************************************
	 * Stores a new log entry based on a log record.
	 *
	 * @param rLogRecord The log record
	 */
	private void processLogRecord(LogRecord rLogRecord)
	{
		if (rLogRecord.getLevel().compareTo(get(MIN_LOG_LEVEL)) >= 0)
		{
			T aLogObject = createLogObject(rLogRecord);

			if (aLogObject != null)
			{
				aLogQueue.add(aLogObject);

				// only send queue if processing is not currently in progress to
				// prevent opening multiple target connections
				if (aQueueAccessLock.tryLock())
				{
					try
					{
						processLogQueue();
					}
					finally
					{
						aQueueAccessLock.unlock();
					}
				}
			}
		}
	}
}
