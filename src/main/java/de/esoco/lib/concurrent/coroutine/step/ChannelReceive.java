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

import de.esoco.lib.concurrent.coroutine.ChannelId;
import de.esoco.lib.concurrent.coroutine.Continuation;
import de.esoco.lib.concurrent.coroutine.Coroutine.Step;

import java.util.concurrent.CompletableFuture;


/********************************************************************
 * A coroutine step that receives a value from a channel. If the channel is
 * empty at the time of this step's invocation the coroutine execution will be
 * suspended until channel data becomes available.
 *
 * <p>A receive step can be chained to arbitrary other execution steps that
 * produce a value of the generic type I but that input value will be
 * ignored.</p>
 *
 * @author eso
 */
public class ChannelReceive<I, O> extends Step<I, O>
{
	//~ Instance fields --------------------------------------------------------

	private ChannelId<O> rChannelId;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rId The ID of the channel to send to
	 */
	public ChannelReceive(ChannelId<O> rId)
	{
		this.rChannelId = rId;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * A factory method for this class to be used with static imports for fluent
	 * declarations.
	 *
	 * @param  rId The ID of the channel to receive from
	 *
	 * @return A new instance of this class
	 */
	public static <I, O> ChannelReceive<I, O> receive(ChannelId<O> rId)
	{
		return new ChannelReceive<>(rId);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected O execute(I rIgnored, Continuation<?> rContinuation)
	{
		return rContinuation.getChannel(rChannelId).receiveBlocking();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void runAsync(CompletableFuture<I> fPreviousExecution,
							Step<O, ?>			 rNextStep,
							Continuation<?>		 rContinuation)
	{
		fPreviousExecution.thenAccept(
			v -> rContinuation.getChannel(rChannelId)
				.receiveSuspending(rNextStep.suspend(rContinuation)));
	}
}
