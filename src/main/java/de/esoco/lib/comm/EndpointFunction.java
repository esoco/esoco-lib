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
package de.esoco.lib.comm;

import de.esoco.lib.expression.Function;
import de.esoco.lib.expression.function.AbstractBinaryFunction;
import de.esoco.lib.expression.function.AbstractFunction;

import org.obrel.core.Relatable;


/********************************************************************
 * A function that applies a {@link CommunicationMethod} to an endpoint and
 * automatically performs the resource handling upon evaluation (i.e. closing
 * the endpoint {@link Connection}).
 *
 * @author eso
 */
public class EndpointFunction<I, O>
	extends AbstractBinaryFunction<I, Relatable, O>
{
	//~ Instance fields --------------------------------------------------------

	private final Endpoint				    rEndpoint;
	private final CommunicationMethod<I, O> fMethod;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rEndpoint fValue The function to evaluate the communication result
	 *                  with
	 * @param fMethod   The communication method to evaluate
	 */
	public EndpointFunction(
		Endpoint				  rEndpoint,
		CommunicationMethod<I, O> fMethod)
	{
		super(null);

		this.rEndpoint = rEndpoint;
		this.fMethod   = fMethod;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public O evaluate(I rInput, Relatable rParams)
	{
		try (Connection rConnection = rEndpoint.connect(rParams))
		{
			set(Endpoint.ENDPOINT_CONNECTION, rConnection);

			return fMethod.evaluate(rInput, rConnection);
		}
	}

	/***************************************
	 * A semantic variant of {@link #result()} that invokes the communication
	 * method with it's default input.
	 *
	 * @return The result of the endpoint request
	 */
	public O send()
	{
		return result();
	}

	/***************************************
	 * A semantic variant of {@link #evaluate(Object)}.
	 *
	 * @param  rInput The input of the endpoint request
	 *
	 * @return The result of the endpoint request
	 */
	public O send(I rInput)
	{
		return evaluate(rInput);
	}

	/***************************************
	 * Overridden to create a new endpoint chain.
	 *
	 * @see AbstractFunction#then(Function)
	 */
	@Override
	public <T> EndpointFunction<I, T> then(Function<? super O, T> fOther)
	{
		return new EndpointFunction<>(rEndpoint,
									  new CommunicationChain<>(fMethod,
															   fOther));
	}
}
