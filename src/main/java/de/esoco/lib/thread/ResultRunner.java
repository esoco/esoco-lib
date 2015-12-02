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
package de.esoco.lib.thread;

/********************************************************************
 * Runnable implementation that allows to access the result of the runnable
 * execution after the run() method has terminated. Subclasses must implement
 * the {@link #execute()} method and return the result from it. After the run()
 * method (which in turn invokes execute()) finished, the result can be queried
 * by means of the {@link #result()} method. If excute() threw an exception the
 * result() method will throw a RuntimeException that wraps the original
 * exception instead of returning a value.
 *
 * <p>This class does not perform any thread synchronization for the execute()
 * method. It is the responsibility of the application to not access the
 * result() method before the {@link #run()} method has finished. Else most
 * probably unpredictable results will occur. To check if the run() method has
 * run completely this class provides the method {@link #isFinished()}.</p>
 *
 * <p>The generic parameter of the class defines the type of result returned by
 * the subclass. If different result types are possible, this must be a common
 * base class of all possible results.</p>
 */
public abstract class ResultRunner<R> implements Runnable
{
	//~ Instance fields --------------------------------------------------------

	private R		  rResult;
	private Throwable rException = null;
	private boolean   bFinished  = false;

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns any exception thrown by the {@link #execute()} method. Will be
	 * NULL if the method terminated regularly.
	 *
	 * @return An exception thrown by execute() or NULL for none
	 */
	public final Throwable getException()
	{
		return rException;
	}

	/***************************************
	 * Allows to check if both the {@link #run()} and {@link #execute()} methods
	 * have run completely. If this method returns TRUE it is safe to query the
	 * {@link #result()} method.
	 *
	 * @return TRUE if the run() method has been executed completely
	 */
	public synchronized boolean isFinished()
	{
		return bFinished;
	}

	/***************************************
	 * Allows to query the result of the execution of this instance. If the
	 * {@link #execute()} method returned a value the same value will be
	 * returned by this method. If it threw an exception this method will throw
	 * a RuntimeException that wraps the original exception.
	 *
	 * <p>This method does not perform any thread synchronization. The result
	 * will only be valid after the {@link #run()} method has been executed
	 * completely. It is the application's responsibility to check this by means
	 * of the method {@link #isFinished()} if necessary.</p>
	 *
	 * @return The result returned by the execute() method (may be NULL)
	 *
	 * @throws RuntimeException If the execute() method threw an exception,
	 *                          which is set as the cause of the
	 *                          RuntimeException
	 */
	public final R result()
	{
		if (rException != null)
		{
			if (rException instanceof RuntimeException)
			{
				throw (RuntimeException) rException;
			}
			else
			{
				throw new RuntimeException(rException);
			}
		}

		return rResult;
	}

	/***************************************
	 * Invokes the {@link #execute()} method and captures the result or any
	 * exceptions that may occur. Final, subclasses must implement the method
	 * {@link #execute()} instead.
	 */
	@Override
	public final void run()
	{
		try
		{
			rResult = execute();
		}
		catch (Throwable t)
		{
			rException = t;
		}
		finally
		{
			finish();
		}
	}

	/***************************************
	 * Must be implemented by subclasses to provide the actual functionality of
	 * the class. The value returned by this method can be queried after
	 * excution through the {@link #result()} method.
	 *
	 * @return The result of the excution (may be NULL)
	 *
	 * @throws Throwable Any kind of exception may be thrown
	 */
	protected abstract R execute() throws Throwable;

	/***************************************
	 * Internal method to set the finished state of this instance to true.
	 */
	private final synchronized void finish()
	{
		bFinished = true;
	}
}
