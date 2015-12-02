//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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


/********************************************************************
 * Implements the chaining of communication functions with automatic resource
 * management by closing a connection at the end of a chain.
 *
 * @author eso
 */
public class CommunicationChain<I, V, O> extends CommunicationMethod<I, O>
{
	//~ Instance fields --------------------------------------------------------

	private final CommunicationMethod<I, V> fMethod;
	private final Function<? super V, O>    fValue;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param fMethod The communication method to evaluate
	 * @param fValue  The function to evaluate the communication result with
	 */
	CommunicationChain(
		CommunicationMethod<I, V> fMethod,
		Function<? super V, O>    fValue)
	{
		super(fValue.getToken() + "(" + fMethod.getToken() + ")", null);

		this.fMethod = fMethod;
		this.fValue  = fValue;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public O doOn(Connection rConnection, I rInput)
	{
		V rValue  = fMethod.evaluate(rInput, rConnection);
		O rResult = fValue.evaluate(rValue);

		return rResult;
	}

	/***************************************
	 * Returns the communication method of this instance.
	 *
	 * @return The communication method
	 */
	public final CommunicationMethod<I, V> getCommunicationMethod()
	{
		return fMethod;
	}

	/***************************************
	 * Returns the value function of this instance.
	 *
	 * @return The value function
	 */
	public final Function<? super V, O> getValueFunction()
	{
		return fValue;
	}
}
