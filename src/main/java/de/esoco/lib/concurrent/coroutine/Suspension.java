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

import java.util.concurrent.CompletableFuture;


/********************************************************************
 * Encapsulates the data that represents a suspended {@link Coroutine}. The
 * execution can be resumed by invoking {@link #resume()}.
 *
 * @author eso
 */
public class Suspension<T>
{
	//~ Instance fields --------------------------------------------------------

	private final T rInput;

	private final Step<T, ?>	  rStep;
	private final Continuation<?> rContinuation;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance. The input value may be NULL if it will only
	 * become available when the suspension is resumed (e.g. when receiving
	 * data). The step may also be NULL if the suspension occurs in the last
	 * step of a coroutine. In that case the {@link #resume()} methods do
	 * nothing but this instance can still be used to store the suspended state
	 * until the suspension ends and the associated action is performed (e.g.
	 * sending data).
	 *
	 * @param rInput        The input value to the step or NULL for none
	 * @param rStep         The coroutine step to be invoked when resuming
	 * @param rContinuation The continuation of the execution
	 */
	public Suspension(T rInput, Step<T, ?> rStep, Continuation<?> rContinuation)
	{
		this.rInput		   = rInput;
		this.rStep		   = rStep;
		this.rContinuation = rContinuation;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the continuation of the suspended coroutine.
	 *
	 * @return The continuation
	 */
	public final Continuation<?> continuation()
	{
		return rContinuation;
	}

	/***************************************
	 * Returns the input value of the curr.
	 *
	 * @return The input
	 */
	public final T input()
	{
		return rInput;
	}

	/***************************************
	 * Resumes the execution of the suspended coroutine with the input value
	 * provided to the constructor.
	 *
	 * @see #resume(Object)
	 */
	public final void resume()
	{
		resume(rInput);
	}

	/***************************************
	 * Resumes the execution of the suspended coroutine with an explicit input
	 * value. The default implementation invokes the method {@link
	 * Step#runAsync(CompletableFuture, Step, Continuation)} in a new {@link
	 * CompletableFuture} that is executed in the executor of the continuation.
	 *
	 * @param rStepInput The input value to the step
	 */
	public void resume(T rStepInput)
	{
		if (rStep != null)
		{
			CompletableFuture<T> fResume =
				CompletableFuture.supplyAsync(() -> rStepInput, rContinuation);

			// the resume step is either a StepChain which contains it's own
			// next step or the final step in a coroutine and therefore the
			// rNextStep argument is NULL
			rStep.runAsync(fResume, null, rContinuation);
		}
	}

	/***************************************
	 * Returns the step to be execute when resuming.
	 *
	 * @return The step
	 */
	public final Step<T, ?> step()
	{
		return rStep;
	}
}
