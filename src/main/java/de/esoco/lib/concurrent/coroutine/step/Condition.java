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

import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Predicate;


/********************************************************************
 * TODO: DOCUMENT ME!
 *
 * @author eso
 */
public class Condition<I, O> extends Step<I, O>
{
	//~ Instance fields --------------------------------------------------------

	private final BiPredicate<? super I, Continuation<?>> fCondition;

	private Step<I, O> rRunOnTrue;
	private Step<I, O> rRunOnFalse = null;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param fCondition  The condition to test
	 * @param rRunOnTrue  The step to run if the condition is TRUE
	 * @param rRunOnFalse The step to run if the condition is FALSE
	 */
	public Condition(BiPredicate<? super I, Continuation<?>> fCondition,
					 Step<I, O>								 rRunOnTrue,
					 Step<I, O>								 rRunOnFalse)
	{
		this.fCondition  = fCondition;
		this.rRunOnTrue  = rRunOnTrue;
		this.rRunOnFalse = rRunOnFalse;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * A factory method that creates a new instance with a certain condition and
	 * the code to execute if the condition is TRUE. To create a condition that
	 * also runs a step if the condition is FALSE either the instance method
	 * {@link #orElse(Step)} or the alternative {@link #doIfElse(Predicate,
	 * Step, Step)}can be used. If no 'else' step is set the coroutine will
	 * terminate if the condition is not met.
	 *
	 * <p>This variant expects a unary predicate that only receives the input
	 * value. If the {@link Continuation} needs to be tested too the method
	 * {@link #doIf(BiPredicate, Step)} can be used instead.</p>
	 *
	 * @param  fCondition The condition to test
	 * @param  rRunOnTrue The step to run if the condition is TRUE
	 *
	 * @return The new conditional step
	 */
	public static <I, O> Condition<I, O> doIf(
		Predicate<? super I> fCondition,
		Step<I, O>			 rRunOnTrue)
	{
		return new Condition<>((i, c) -> fCondition.test(i), rRunOnTrue, null);
	}

	/***************************************
	 * A factory method that creates a new instance with a certain condition and
	 * the code to execute if the condition is TRUE. To create a condition from
	 * the returned one with an additional step to be run on FALSE the instance
	 * method {@link #orElse(Step)} can be used. If no 'else' step is set at all
	 * the coroutine will terminate if the condition is not met.
	 *
	 * <p>This variant expects a binary predicate that also receives the current
	 * {@link Continuation}. If a test of the input value is sufficient the
	 * method {@link #doIf(Predicate, Step)} can be used instead.</p>
	 *
	 * @param  fCondition The condition to test
	 * @param  rRunOnTrue The step to run if the condition is TRUE
	 *
	 * @return The new conditional step
	 */
	public static <I, O> Condition<I, O> doIf(
		BiPredicate<? super I, Continuation<?>> fCondition,
		Step<I, O>								rRunOnTrue)
	{
		return new Condition<>(fCondition, rRunOnTrue, null);
	}

	/***************************************
	 * A factory method that creates a new instance with a certain condition and
	 * two steps to execute if the condition is either TRUE or FALSE. A semantic
	 * alternative is the factory method {@link #doIf(BiPredicate, Step)} in
	 * conjunction with the instance method {@link #orElse(Step)}.
	 *
	 * <p>This variant expects a binary predicate that also receives the current
	 * {@link Continuation}. If a test of the input value is sufficient the
	 * method {@link #doIfElse(Predicate, Step, Step)} can be used instead.</p>
	 *
	 * @param  fCondition  The condition to test
	 * @param  rRunOnTrue  The step to run if the condition is TRUE
	 * @param  rRunOnFalse The step to run if the condition is FALSE
	 *
	 * @return The new conditional step
	 */
	public static <I, O> Condition<I, O> doIfElse(
		BiPredicate<? super I, Continuation<?>> fCondition,
		Step<I, O>								rRunOnTrue,
		Step<I, O>								rRunOnFalse)
	{
		return new Condition<>(fCondition, rRunOnTrue, rRunOnFalse);
	}

	/***************************************
	 * A factory method that creates a new instance with a certain condition and
	 * two steps to execute if the condition is either TRUE or FALSE. A semantic
	 * alternative is the factory method {@link #doIf(Predicate, Step)} in
	 * conjunction with the instance method {@link #orElse(Step)}.
	 *
	 * <p>This variant expects a unary predicate that only receives the input
	 * value. If the {@link Continuation} needs to be tested too the method
	 * {@link #doIf(BiPredicate, Step)} can be used instead.</p>
	 *
	 * @param  fCondition  The condition to test
	 * @param  rRunOnTrue  The step to run if the condition is TRUE
	 * @param  rRunOnFalse The step to run if the condition is FALSE
	 *
	 * @return The new conditional step
	 */
	public static <I, O> Condition<I, O> doIfElse(
		Predicate<? super I> fCondition,
		Step<I, O>			 rRunOnTrue,
		Step<I, O>			 rRunOnFalse)
	{
		return new Condition<>(
			(i, c) -> fCondition.test(i),
			rRunOnTrue,
			rRunOnFalse);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public O execute(I rInput, Continuation<?> rContinuation)
	{
		O rResult = null;

		if (fCondition.test(rInput, rContinuation))
		{
			rResult = rRunOnTrue.execute(rInput, rContinuation);
		}
		else if (rRunOnFalse != null)
		{
			rResult = rRunOnFalse.execute(rInput, rContinuation);
		}

		return rResult;
	}

	/***************************************
	 * Returns a new instance with the condition and TRUE step of this and a
	 * certain step to execute if the condition is FALSE. This is just a
	 * semantic alternative to {@link #doIfElse(BiPredicate, Step, Step)}.
	 *
	 * @param  rRunOnFalse The step to run if the condition is FALSE
	 *
	 * @return A new conditional step
	 */
	public Condition<I, O> orElse(Step<I, O> rRunOnFalse)
	{
		return new Condition<>(fCondition, rRunOnTrue, rRunOnFalse);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<I> fPreviousExecution,
						 Step<O, ?>			  rNextStep,
						 Continuation<?>	  rContinuation)
	{
		fPreviousExecution.thenAccept(
			i ->
			{
				Step<I, O> rStep =
					fCondition.test(i, rContinuation) ? rRunOnTrue
													  : rRunOnFalse;

				if (rStep != null)
				{
					rStep.runAsync(fPreviousExecution, rNextStep, rContinuation);
				}
				else
				{
					terminateCoroutine(rContinuation);
				}
			});
	}
}
