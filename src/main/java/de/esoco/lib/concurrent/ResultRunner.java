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
package de.esoco.lib.concurrent;

import de.esoco.lib.expression.ThrowingSupplier;

import java.util.function.Supplier;

/**
 * Runnable implementation that allows to access the result of the runnable
 * execution after the run() method has terminated. Subclasses must provide an
 * instance of the {@link ThrowingSupplier} interface that will create the
 * result value. After the run method finished, the result can be queried by
 * means of the {@link #result()} method. If the supplying function threw an
 * exception the result() method will throw a RuntimeException that wraps the
 * original exception instead of returning a value.
 *
 * <p>The purpose of the class is to give access to a computation that must be
 * performed in a {@link Runnable}, e.g. because of some API that expects such
 * an instance. In cases where that is no necessary it should be sufficient to
 * use something like the {@link Supplier} interface.</p>
 *
 * <p>This class does not perform any thread synchronization for the execute()
 * method. It is the responsibility of the application to not access the
 * result() method before the {@link #run()} method has finished. Else most
 * probably unpredictable results will occur. To check if the {@link #run()}
 * method has run completely this class provides the method
 * {@link #isFinished()}.</p>
 */
public class ResultRunner<T> implements Runnable {

	private final ThrowingSupplier<T> createResult;

	private T result = null;

	private Throwable exception = null;

	private boolean finished = false;

	/**
	 * Creates a new instance.
	 *
	 * @param createResult The function that generates the result
	 */
	public ResultRunner(ThrowingSupplier<T> createResult) {
		this.createResult = createResult;
	}

	/**
	 * Returns any exception thrown by the supplying function. Will be NULL if
	 * the method terminated regularly.
	 *
	 * @return An exception thrown by execute() or NULL for none
	 */
	public final Throwable getException() {
		return exception;
	}

	/**
	 * Allows to check if the execution has finished. When this method returns
	 * TRUE it is safe to query the {@link #result()} method.
	 *
	 * @return TRUE if the execution has finished
	 */
	public synchronized boolean isFinished() {
		return finished;
	}

	/**
	 * Allows to query the result of the execution of this instance. If the
	 * supplying function returned a value the same value will be returned by
	 * this method. If it threw an exception this method will throw a
	 * RuntimeException that wraps the original exception (runtime exceptions
	 * will be re-thrown directly). The original exception can also be queried
	 * with {@link #getException()}.
	 *
	 * <p>This method does not perform any thread synchronization. The result
	 * will only be valid after the {@link #run()} method has been executed
	 * completely. It is the application's responsibility to check this by
	 * means
	 * of the method {@link #isFinished()} if necessary.</p>
	 *
	 * @return The result returned by the execute() method (may be NULL)
	 * @throws RuntimeException If the execute() method threw an exception,
	 *                          which is set as the cause of the runtime
	 *                          exception
	 */
	public final T result() {
		if (exception != null) {
			if (exception instanceof RuntimeException) {
				throw (RuntimeException) exception;
			} else {
				throw new RuntimeException(exception);
			}
		}

		return result;
	}

	/**
	 * Invokes the supplying function and captures the result or any exceptions
	 * that may occur.
	 */
	@Override
	public final void run() {
		try {
			result = createResult.tryGet();
		} catch (Throwable t) {
			exception = t;
		} finally {
			finish();
		}
	}

	/**
	 * Internal method to set the finished state of this instance to true.
	 */
	private final synchronized void finish() {
		finished = true;
	}
}
