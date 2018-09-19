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
package de.esoco.lib.concurrent.coroutine.step;

import de.esoco.lib.concurrent.coroutine.Continuation;
import de.esoco.lib.concurrent.coroutine.Step;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;


/********************************************************************
 * A step that implements suspendable iteration over an {@link Iterable} input
 * value. Each value returned by the iterator will be processed with a separate
 * execution, allowing steps from other coroutines to run in parallel..
 *
 * @author eso
 */
public class Iteration<T, I extends Iterable<T>> extends Step<I, Void>
{
	//~ Instance fields --------------------------------------------------------

	private final Step<T, ?> rProcessingStep;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rProcessingStep The step to be applied to each value returned by
	 *                        the iterator
	 */
	public Iteration(Step<T, ?> rProcessingStep)
	{
		this.rProcessingStep = rProcessingStep;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Iterates over each element in the {@link Iterator} of an {@link Iterable}
	 * input value. If invoked asynchronously each iteration step will be
	 * invoked as a separate suspension, but sequentially for each value
	 * returned by the iterator. After the iteration has completed the coroutine
	 * continues with the next step.
	 *
	 * @param  rProcessor The step to process each value
	 *
	 * @return A new step instance
	 */
	public static <T, I extends Iterable<T>> Iteration<T, I> forEach(
		Step<T, ?> rProcessor)
	{
		return new Iteration<>(rProcessor);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<I> fPreviousExecution,
						 Step<Void, ?>		  rNextStep,
						 Continuation<?>	  rContinuation)
	{
		fPreviousExecution.thenAcceptAsync(
			i -> iterateAsync(i.iterator(), rNextStep, rContinuation));
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected Void execute(I rInput, Continuation<?> rContinuation)
	{
		for (T rValue : rInput)
		{
			rProcessingStep.runBlocking(rValue, rContinuation);
		}

		return null;
	}

	/***************************************
	 * Performs the asynchronous iteration over all values in an iterator.
	 *
	 * @param rIterator     The iterator
	 * @param rNextStep     The step to execute when the iteration is finished
	 * @param rContinuation The current continuation
	 */
	private void iterateAsync(Iterator<T>	  rIterator,
							  Step<Void, ?>   rNextStep,
							  Continuation<?> rContinuation)
	{
		if (rIterator.hasNext())
		{
			CompletableFuture<T> fGetNextValue =
				CompletableFuture.supplyAsync(
					() -> rIterator.next(),
					rContinuation);

			fGetNextValue.thenAccept(
				v ->
				{
					rProcessingStep.restart(v, rContinuation)
					.thenRun(
						() -> iterateAsync(rIterator, rNextStep, rContinuation));
				});
		}
		else
		{
			rNextStep.suspend(rContinuation).resume();
		}
	}
}
