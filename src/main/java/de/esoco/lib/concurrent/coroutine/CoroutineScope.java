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
package de.esoco.lib.concurrent.coroutine;

import de.esoco.lib.concurrent.RunLock;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.obrel.core.RelatedObject;


/********************************************************************
 * A scope that manages running coroutines and blocks the invoking thread until
 * all contained routines have finished execution. This allows to apply
 * "structured concurrency" by wrapping a set of coroutine executions in a
 * scope.
 *
 * @author eso
 */
public class CoroutineScope extends RelatedObject
{
	//~ Instance fields --------------------------------------------------------

	private CoroutineContext rContext;

	private boolean			 bCancelled		    = false;
	private final AtomicLong nRunningCoroutines = new AtomicLong();
	private final RunLock    aCoroutineLock     = new RunLock();
	private CountDownLatch   aFinishedSignal    = new CountDownLatch(1);

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rContext The context to run the scope's coroutines in
	 */
	CoroutineScope(CoroutineContext rContext)
	{
		this.rContext =
			rContext != null ? rContext : Coroutines.getDefaultContext();
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Creates a new scope for the launching of coroutine executions.
	 *
	 * @param  rBuilder The builder to invoke the launching methods
	 *
	 * @return The finished scope
	 */
	public static CoroutineScope launch(Builder rBuilder)
	{
		return launch(null, rBuilder);
	}

	/***************************************
	 * Creates a new scope for the launching of coroutine executions in a
	 * certain context.
	 *
	 * @param  rContext The coroutine context for the scope
	 * @param  rBuilder The builder to invoke the launching methods
	 *
	 * @return The finished scope
	 */
	public static CoroutineScope launch(
		CoroutineContext rContext,
		Builder			 rBuilder)
	{
		CoroutineScope rScope = new CoroutineScope(rContext);

		rScope.getContext().scopeLaunched(rScope);

		rBuilder.build(rScope);
		rScope.await();

		rScope.getContext().scopeFinished(rScope);

		return rScope;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Starts the asynchronous execution of a coroutine in this scope with an
	 * input value of NULL. This is typically used to start coroutines with a
	 * void input type.
	 *
	 * @param  rCoroutine The coroutine
	 *
	 * @return The continuation of the executing coroutine
	 */
	public <I, O> Continuation<O> async(Coroutine<I, O> rCoroutine)
	{
		return rCoroutine.runAsync(this, null);
	}

	/***************************************
	 * Starts the asynchronous execution of a coroutine in this scope.
	 *
	 * @param  rCoroutine The coroutine
	 * @param  rInput     The coroutine input
	 *
	 * @return The continuation of the executing coroutine
	 */
	public <I, O> Continuation<O> async(Coroutine<I, O> rCoroutine, I rInput)
	{
		return rCoroutine.runAsync(this, rInput);
	}

	/***************************************
	 * Executes a coroutine in this scope with an input value of NULL and blocks
	 * the current thread until it is finished. This is typically used to start
	 * coroutines with a void input type.
	 *
	 * @param  rCoroutine The coroutine
	 *
	 * @return The continuation of the completed coroutine
	 */
	public <I, O> Continuation<O> blocking(Coroutine<I, O> rCoroutine)
	{
		return rCoroutine.runBlocking(this, null);
	}

	/***************************************
	 * Executes a coroutine in this scope and blocks the current thread until it
	 * is finished.
	 *
	 * @param  rCoroutine The coroutine
	 * @param  rInput     The coroutine input
	 *
	 * @return The continuation of the completed coroutine
	 */
	public <I, O> Continuation<O> blocking(Coroutine<I, O> rCoroutine, I rInput)
	{
		return rCoroutine.runBlocking(this, rInput);
	}

	/***************************************
	 * Cancels the execution of all coroutines that are currently running in
	 * this scope.
	 */
	public void cancel()
	{
		bCancelled = true;
	}

	/***************************************
	 * Returns the context in which coroutines of this scope are executed.
	 *
	 * @return The coroutine context
	 */
	public CoroutineContext getContext()
	{
		return rContext;
	}

	/***************************************
	 * Returns the number of currently running coroutines. This will only be a
	 * momentary value as the execution of the coroutines happens asynchronously
	 * and coroutines may finish while querying this count.
	 *
	 * @return The number of running coroutines
	 */
	public long getCoroutineCount()
	{
		return nRunningCoroutines.get();
	}

	/***************************************
	 * Checks whether this scope has been cancelled.
	 *
	 * @return The canceled
	 */
	public boolean isCancelled()
	{
		return bCancelled;
	}

	/***************************************
	 * Blocks until the coroutines of all {@link CoroutineScope scopes} in this
	 * context have finished execution. If no coroutines are running or all have
	 * finished execution already this method returns immediately.
	 */
	void await()
	{
		if (aFinishedSignal != null)
		{
			try
			{
				aFinishedSignal.await();
			}
			catch (InterruptedException e)
			{
				throw new CompletionException(e);
			}
		}
	}

	/***************************************
	 * Notifies this context that a coroutine execution has been finished
	 * (either regularly or by canceling).
	 *
	 * @param rContinuation The continuation of the execution
	 */
	void coroutineFinished(Continuation<?> rContinuation)
	{
		if (nRunningCoroutines.decrementAndGet() == 0)
		{
			aCoroutineLock.runLocked(() -> aFinishedSignal.countDown());
		}
	}

	/***************************************
	 * Notifies this context that a coroutine has been started in it.
	 *
	 * @param rContinuation The continuation of the execution
	 */
	void coroutineStarted(Continuation<?> rContinuation)
	{
		if (nRunningCoroutines.incrementAndGet() == 1)
		{
			aCoroutineLock.runLocked(
				() ->
			{
				if (aFinishedSignal.getCount() == 0)
				{
					aFinishedSignal = new CountDownLatch(1);
				}
			});
		}
	}

	//~ Inner Interfaces -------------------------------------------------------

	/********************************************************************
	 * TODO: DOCUMENT ME!
	 *
	 * @author eso
	 */
	@FunctionalInterface
	public interface Builder
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * TODO: DOCUMENT ME!
		 *
		 * @param rScope TODO: DOCUMENT ME!
		 */
		public void build(CoroutineScope rScope);
	}
}
