//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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


/********************************************************************
 * Describes a method to communicate with an endpoint in the communication
 * framework.
 *
 * @author eso
 */
public abstract class CommunicationMethod<I, O>
	extends AbstractBinaryFunction<I, Connection, O>
{
	//~ Instance fields --------------------------------------------------------

	private final I rDefaultInput;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param sMethodName   The name of this method
	 * @param rDefaultInput The default input for this method
	 */
	public CommunicationMethod(String sMethodName, I rDefaultInput)
	{
		super(null, sMethodName);

		this.rDefaultInput = rDefaultInput;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Must be implemented by subclasses to perform the actual communication
	 * over a connection to execute this method. This method must always be
	 * invoked with explicit parameters. An implicit default input value will
	 * only be considered by the standard method {@link #evaluate(Object)}.
	 *
	 * @param  rConnection The connection to communicate over
	 * @param  rInput      The input value for this communication method
	 *
	 * @return The output value according to this method's definition
	 */
	public abstract O doOn(Connection rConnection, I rInput);

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public final O evaluate(I rInput, Connection rConnection)
	{
		return doOn(rConnection, rInput != null ? rInput : rDefaultInput);
	}

	/***************************************
	 * Overloaded variant of {@link Function#from(Function)} that returns an
	 * instance of {@link EndpointChain}.
	 *
	 * @param  rEndpoint The endpoint to evaluat e for the connection
	 *
	 * @return A new function chain that evaluates this method at the given
	 *         endpoint
	 */
	public EndpointChain<I, O> from(Endpoint rEndpoint)
	{
		return rEndpoint.then(this);
	}

	/***************************************
	 * Returns the default input value of this method.
	 *
	 * @return The default input value
	 */
	public final I getDefaultInput()
	{
		return rDefaultInput;
	}

	/***************************************
	 * A synonym for {@link #doOn(Connection, Object)} that can be used to
	 * indicate that a value is retrieved from an endpoint connection.
	 *
	 * @see #doOn(Connection, Object)
	 */
	public O getFrom(Connection rConnection, I rInput)
	{
		return doOn(rConnection, rInput);
	}

	/***************************************
	 * A synonym for {@link #doOn(Connection, Object)} that can be used to
	 * indicate that a value is sent over an endpoint connection.
	 *
	 * @see #doOn(Connection, Object)
	 */
	public O sendTo(Connection rConnection, I rInput)
	{
		return doOn(rConnection, rInput);
	}

	/***************************************
	 * Overloaded to return a special type of {@link CommunicationMethod} that
	 * can perform automatic resource handling for chained methods.
	 *
	 * @see AbstractBinaryFunction#then(Function)
	 */
	@Override
	public <T> CommunicationMethod<I, T> then(Function<? super O, T> fOther)
	{
		return new CommunicationChain<>(this, fOther);
	}
}
