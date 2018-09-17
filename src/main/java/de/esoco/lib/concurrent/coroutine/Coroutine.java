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

import de.esoco.lib.concurrent.coroutine.step.ChannelReceive;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.obrel.core.FluentRelatable;
import org.obrel.core.ObjectRelations;
import org.obrel.core.RelatedObject;

import static org.obrel.type.StandardTypes.NAME;


/********************************************************************
 * A pure Java implementation of cooperative concurrency, also known as
 * coroutines.
 *
 * @author eso
 */
public class Coroutine<I, O> extends RelatedObject
	implements FluentRelatable<Coroutine<I, O>>
{
	//~ Instance fields --------------------------------------------------------

	private StepChain<I, ?, O> aCode;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance that starts execution with a certain step.
	 *
	 * @param rFirstStep The first step to execute
	 */
	public Coroutine(Step<I, O> rFirstStep)
	{
		init(new StepChain<>(rFirstStep, new FinishStep<>()));
	}

	/***************************************
	 * Internal constructor to create a new instance that is an extension of
	 * another coroutine.
	 *
	 * @param rOther    The other coroutine
	 * @param rNextStep The code to execute after that of the other coroutine
	 */
	private <T> Coroutine(Coroutine<I, T> rOther, Step<T, O> rNextStep)
	{
		init(rOther.aCode.then(rNextStep));

		ObjectRelations.copyRelations(rOther, this, true);
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * A factory method that creates a new coroutine which starts with the
	 * execution of a certain code function.
	 *
	 * @param  rStep fCode The function containing the starting code of the
	 *               coroutine
	 *
	 * @return A new coroutine instance
	 */
	public static <I, O> Coroutine<I, O> first(Step<I, O> rStep)
	{
		Objects.requireNonNull(rStep);

		return new Coroutine<>(rStep);
	}

	/***************************************
	 * A factory method that creates a new coroutine which starts execution with
	 * receiving from a certain channel. If the channel doesn't exist in the
	 * context when the coroutine is run it will be created with a capacity of 1
	 * (see {@link CoroutineContext#getChannel(ChannelId)}). To change the
	 * channel capacity the channel must be created in advance in an explicit
	 * context which is then handed to {@link #runAsync(CoroutineContext,
	 * Object)}.
	 *
	 * @param  rId fCode The function containing the starting code of the
	 *             coroutine
	 *
	 * @return A new coroutine instance
	 */
	public static <T> Coroutine<Void, T> receive(ChannelId<T> rId)
	{
		Objects.requireNonNull(rId);

		return new Coroutine<Void, T>(new ChannelReceive<>(rId));
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * A convenience method to asynchronously run a coroutine with a NULL input
	 * value. This is typically used to start coroutines with a Void input type.
	 *
	 * @return A {@link Continuation} that provides access to the execution
	 *         result
	 *
	 * @see    #runAsync(Object)
	 */
	public Continuation<O> runAsync()
	{
		return runAsync(null);
	}

	/***************************************
	 * Runs this coroutine asynchronously in a default context. This method can
	 * be used if no explicit context is needed for an execution. A default
	 * context will be created automatically. The returned {@link Continuation}
	 * provides access to the context and can then be used to execute additional
	 * coroutines in it if necessary (e.g. for channel-based communication
	 * between coroutines).
	 *
	 * <p>If multiple coroutines need to communicate through {@link Channel
	 * channels} they must run in the same context because channels are managed
	 * by the context based on the channel ID. Therefore any further coroutines
	 * of these must be started with {@link #runAsync(CoroutineContext, Object)}
	 * with the context from the returned continuation.</p>
	 *
	 * @param  rInput The input value for the execution
	 *
	 * @return A {@link Continuation} that provides access to the execution
	 *         result
	 *
	 * @see    #runAsync(CoroutineContext, Object)
	 */
	public Continuation<O> runAsync(I rInput)
	{
		return runAsync(null, rInput);
	}

	/***************************************
	 * Runs this coroutine asynchronously in a certain context. This method
	 * returns a {@link Continuation} that contains the execution state and
	 * provides access to the coroutine result AFTER it finishes. Because the
	 * execution happens asynchronously (i.e. in another thread) the receiving
	 * code must always use the corresponding continuation methods to check for
	 * completion before accessing the continuation state.
	 *
	 * <p>If multiple coroutines need to communicate through {@link Channel
	 * channels} they must run in the same context because channels are managed
	 * by the context based on the channel ID. If a coroutine is started with
	 * {@link #runAsync(Object)} it's automatically created context can be
	 * queried from the returned {@link Continuation} and used for subsequent
	 * coroutine executions with this method.</p>
	 *
	 * @param  rContext The context to run this coroutine in
	 * @param  rInput   The input value
	 *
	 * @return A {@link Continuation} that provides access to the execution
	 *         result
	 */
	@SuppressWarnings("unchecked")
	public Continuation<O> runAsync(CoroutineContext rContext, I rInput)
	{
		Continuation<O> aContinuation = new Continuation<>(rContext, this);

		CompletableFuture<I> fExecution =
			CompletableFuture.supplyAsync(() -> rInput, aContinuation);

		aCode.runAsync(fExecution, null, aContinuation);

		return aContinuation;
	}

	/***************************************
	 * Runs this coroutine on the current thread in a default context and
	 * returns after the execution finishes.
	 *
	 * @param  rInput The input value
	 *
	 * @return The result of the execution
	 *
	 * @see    #runBlocking(CoroutineContext, Object)
	 */
	public Continuation<O> runBlocking(I rInput)
	{
		return runBlocking(null, rInput);
	}

	/***************************************
	 * Runs this coroutine on the current thread in a certain context and
	 * returns after the execution finishes. The returned {@link Continuation}
	 * will already be finished when this method returns and provides access to
	 * the result. If multiple coroutines should be run in parallel by using a
	 * blocking run method the caller needs to create multiple threads. If these
	 * threaded coroutines then need to communicated through {@link Channel
	 * channels} they must also run in the same context (see {@link
	 * #runAsync(CoroutineContext, Object)} for details).
	 *
	 * @param  rContext The context to run this coroutine in
	 * @param  rInput   The input value
	 *
	 * @return The result of the execution
	 */
	@SuppressWarnings("unchecked")
	public Continuation<O> runBlocking(CoroutineContext rContext, I rInput)
	{
		Continuation<O> aContinuation = new Continuation<>(rContext, this);

		aCode.runBlocking(rInput, aContinuation);

		return aContinuation;
	}

	/***************************************
	 * Returns a new coroutine that executes additional code after that of this
	 * instance. This and the related methods serve as builders for complex
	 * coroutines. The initial coroutine is created with the first step, either
	 * from the static factory methods like {@link #first(Function)} or with one
	 * of the public constructors. Invoking a builder method creates a new
	 * coroutine with the combined code while the original coroutine remains
	 * unchanged (or is discarded in the case of a builder chain).
	 *
	 * <p>Each invocation of a builder method creates a coroutine suspension
	 * point at which the execution will be interrupted to allow other code to
	 * run on the current thread (e.g. another coroutine). Some steps like
	 * {@link #thenReceive(Channel)} will suspend the execution until values
	 * from another thread become available.</p>
	 *
	 * <p>An extended coroutine re-uses the original code of the coroutine it is
	 * derived from. Therefore it is necessary to ensure that the code in shared
	 * (base) coroutines contains no dependencies to external state. The best
	 * way to achieve this is by using correctly defined closures for the steps.
	 * If steps need to share information during execution that can be achieved
	 * by setting relations on the {@link Continuation} which is local to the
	 * respective execution. Access to the continuation instance is available
	 * through the method {@link #then(BiFunction)}.</p>
	 *
	 * @param  rStep The step to execute
	 *
	 * @return The new coroutine
	 */
	public <T> Coroutine<I, T> then(Step<O, T> rStep)
	{
		Objects.requireNonNull(rStep);

		return new Coroutine<>(this, rStep);
	}

	/***************************************
	 * A variant of {@link #then(Step)} that also sets a step label. Labeling
	 * steps is used for branching and can help during the debugging of
	 * Coroutines.
	 *
	 * @param  sLabel A label that identifies this step in this coroutine
	 * @param  rStep  The step to execute
	 *
	 * @return The new coroutine
	 *
	 * @see    #then(Step)
	 */
	public <T> Coroutine<I, T> then(String sStepLabel, Step<O, T> rStep)
	{
		rStep.sLabel = sStepLabel;

		return then(rStep);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return String.format("%s[%s]", get(NAME), aCode);
	}

	/***************************************
	 * Initializes a new instance. Invoked from the constructors.
	 *
	 * @param rCode The code to be executed
	 */
	private void init(StepChain<I, ?, O> rCode)
	{
		aCode = rCode;

		set(NAME, getClass().getSimpleName());
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * The base class of the single execution steps in a coroutine.
	 *
	 * @author eso
	 */
	public static abstract class Step<I, O>
	{
		//~ Instance fields ----------------------------------------------------

		private String sLabel;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 */
		protected Step()
		{
			sLabel = getClass().getSimpleName();
		}

		/***************************************
		 * Creates a new instance with a certain name.
		 *
		 * @param sLabel A label that identifies this step in it's coroutine
		 */
		protected Step(String sLabel)
		{
			this.sLabel = sLabel;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Suspends this step for later invocation and returns an instance of
		 * {@link Suspension} that contains the state necessary for resuming the
		 * execution. Other than {@link #suspend(Object, Continuation)} this
		 * suspension will not contain an explicit input value. Such suspensions
		 * are used if the input will only become available when the suspension
		 * ends (e.g. when receiving data asynchronously).
		 *
		 * @param  rContinuation The continuation of the suspended execution
		 *
		 * @return A new suspension object
		 */
		public Suspension<I> suspend(Continuation<?> rContinuation)
		{
			return suspend(null, rContinuation);
		}

		/***************************************
		 * Suspends this step for later invocation and returns an instance of
		 * {@link Suspension} that contains the state necessary for resuming the
		 * execution. If the input value is not known before the suspension ends
		 * the method {@link #suspend(Continuation)} can be used instead.
		 *
		 * @param  rInput        The input value for the execution
		 * @param  rContinuation The continuation of the suspended execution
		 *
		 * @return A new suspension object
		 */
		public Suspension<I> suspend(I rInput, Continuation<?> rContinuation)
		{
			return new Suspension<>(rInput, this, rContinuation);
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public String toString()
		{
			return sLabel;
		}

		/***************************************
		 * This method must be implemented by subclasses to provide the actual
		 * functionality of this step.
		 *
		 * @param  rInput        The input value
		 * @param  rContinuation The continuation of the execution
		 *
		 * @return The result of the execution
		 */
		protected abstract O execute(I rInput, Continuation<?> rContinuation);

		/***************************************
		 * Runs this execution step asynchronously as a continuation of a
		 * previous code execution in a {@link CompletableFuture} and proceeds
		 * to the next step afterwards.
		 *
		 * <p>Subclasses that need to suspend the invocation of the next step
		 * until some condition is met (e.g. sending or receiving data has
		 * finished) need to override this method and call {@link
		 * #resume(Object, Continuation)} on the next step if the suspension
		 * ends.</p>
		 *
		 * @param fPreviousExecution The future of the previous code execution
		 * @param rNextStep          The next step to execute
		 * @param rContinuation      The continuation of the execution
		 */
		protected void runAsync(CompletableFuture<I> fPreviousExecution,
								Step<O, ?>			 rNextStep,
								Continuation<?>		 rContinuation)
		{
			CompletableFuture<O> fExecution =
				fPreviousExecution.thenApplyAsync(
					i -> execute(i, rContinuation),
					rContinuation);

			if (rNextStep != null)
			{
				// the next step is either a StepChain which contains it's own
				// next step or the final step in a coroutine and therefore the
				// rNextStep argument can be NULL
				rNextStep.runAsync(fExecution, null, rContinuation);
			}
		}

		/***************************************
		 * Runs this execution immediately, blocking the current thread until
		 * the execution finishes.
		 *
		 * @param  rInput        The input value
		 * @param  rContinuation The continuation of the execution
		 *
		 * @return The execution result
		 */
		protected O runBlocking(I rInput, Continuation<?> rContinuation)
		{
			return execute(rInput, rContinuation);
		}
	}

	/********************************************************************
	 * The final step of a coroutine execution that updates the state of the
	 * corresponding {@link Continuation}.
	 *
	 * @param  rResult       The result of the execution
	 * @param  rContinuation The continuation of the execution
	 *
	 * @author eso
	 */
	static class FinishStep<T> extends Step<T, T>
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("unchecked")
		protected T execute(T rResult, Continuation<?> rContinuation)
		{
			((Continuation<T>) rContinuation).finish(rResult);

			return rResult;
		}
	}

	/********************************************************************
	 * A chain of an execution step and it's successor which may also be a step
	 * chain.
	 *
	 * @author eso
	 */
	static class StepChain<I, T, O> extends Step<I, O>
	{
		//~ Instance fields ----------------------------------------------------

		Step<I, T> rFirstStep;
		Step<T, O> rNextStep;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rCode The first execution
		 * @param rNext The second execution
		 */
		private StepChain(Step<I, T> rCode, Step<T, O> rNext)
		{
			this.rFirstStep = rCode;
			this.rNextStep  = rNext;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public String toString()
		{
			return rFirstStep + " -> " + rNextStep;
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected O execute(I rInput, Continuation<?> rContinuation)
		{
			if (rContinuation.isCancelled())
			{
				rContinuation.finish(null);

				return null;
			}
			else
			{
				return rNextStep.execute(
					rFirstStep.execute(rInput, rContinuation),
					rContinuation);
			}
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected void runAsync(CompletableFuture<I> fPreviousExecution,
								Step<O, ?>			 rIgnored,
								Continuation<?>		 rContinuation)
		{
			if (rContinuation.isCancelled())
			{
				rContinuation.finish(null);
			}
			else
			{
				// a step chain will always be the second step in the preceding
				// chain step and therefore the ignored step argument will
				// always be null
				rFirstStep.runAsync(
					fPreviousExecution,
					rNextStep,
					rContinuation);
			}
		}

		/***************************************
		 * Returns a new {@link StepChain} that invokes a certain step as the
		 * last step of the execution chain.
		 *
		 * @param  rStep rStep The next step to invoke
		 *
		 * @return The new invocation
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		<R> StepChain<I, T, R> then(Step<O, R> rStep)
		{
			StepChain<I, T, R> aChainedInvocation =
				new StepChain<>(rFirstStep, null);

			if (rNextStep instanceof StepChain)
			{
				// Chains need to be accessed as raw types because the
				// intermediate type of the chain in rNextStep is unknown
				aChainedInvocation.rNextStep =
					((StepChain) rNextStep).then(rStep);
			}
			else
			{
				// rNextStep is either another StepChain (see above) or else the
				// FinishStep which must be invoked last. Raw type is necessary
				// because the type of THIS is actually <I,O,O> as FinishStep is
				// an identity step, but this type info is not available here.
				aChainedInvocation.rNextStep = new StepChain(rStep, rNextStep);
			}

			return aChainedInvocation;
		}
	}
}
