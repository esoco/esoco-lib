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
import de.esoco.lib.concurrent.coroutine.Coroutine.Step;
import de.esoco.lib.expression.Functions;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/********************************************************************
 * An coroutine step that executes a code function.
 *
 * @author eso
 */
public class CodeExecution<I, O> extends Step<I, O>
{
	//~ Instance fields --------------------------------------------------------

	private final BiFunction<I, Continuation<?>, O> fCode;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance from a binary function that accepts the
	 * continuation of the execution and the input value.
	 *
	 * @param fCode A binary function containing the code to be executed
	 */
	public CodeExecution(BiFunction<I, Continuation<?>, O> fCode)
	{
		this.fCode = fCode;
	}

	/***************************************
	 * Creates a new instance from a simple function that processes the input
	 * into the output value.
	 *
	 * @param fCode A function containing the code to be executed
	 */
	public CodeExecution(Function<I, O> fCode)
	{
		this.fCode = (i, c) -> fCode.apply(i);
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * A factory method for this class to invoke a {@link Function}. To be used
	 * with static imports for fluent declarations.
	 *
	 * @param  fCode The function to be executed
	 *
	 * @return A new instance of this class
	 */
	public static <I, O> CodeExecution<I, O> apply(Function<I, O> fCode)
	{
		return new CodeExecution<>(fCode);
	}

	/***************************************
	 * A factory method for this class to invoke a {@link BiFunction} that also
	 * receives the current continuation. To be used with static imports for
	 * fluent declarations.
	 *
	 * @param  fCode The binary function to be executed
	 *
	 * @return A new instance of this class
	 */
	public static <I, O> CodeExecution<I, O> apply(
		BiFunction<I, Continuation<?>, O> fCode)
	{
		return new CodeExecution<>(fCode);
	}

	/***************************************
	 * A factory method for this class to invoke a {@link Consumer}. To be used
	 * with static imports for fluent declarations.
	 *
	 * @param  fCode The consumer to be executed
	 *
	 * @return A new instance of this class
	 */
	public static <T> CodeExecution<T, Void> consume(Consumer<T> fCode)
	{
		return new CodeExecution<>(Functions.consume(fCode));
	}

	/***************************************
	 * A factory method for this class to invoke a {@link Runnable}. To be used
	 * with static imports for fluent declarations.
	 *
	 * @param  fCode The runnable to be executed
	 *
	 * @return A new instance of this class
	 */
	public static <T> CodeExecution<T, Void> run(Runnable fCode)
	{
		return new CodeExecution<>(Functions.run(fCode));
	}

	/***************************************
	 * A factory method for this class to invoke a {@link Supplier}. To be used
	 * with static imports for fluent declarations.
	 *
	 * @param  fCode The supplier to be executed
	 *
	 * @return A new instance of this class
	 */
	public static <I, O> CodeExecution<I, O> supply(Supplier<O> fCode)
	{
		return new CodeExecution<>(Functions.supply(fCode));
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected O execute(I rInput, Continuation<?> rContinuation)
	{
		return fCode.apply(rInput, rContinuation);
	}
}
