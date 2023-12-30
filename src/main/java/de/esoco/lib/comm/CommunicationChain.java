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

/**
 * Binary function that chains a communication method with another function and
 * still can be used as a communication method so that it can be invoked with a
 * connection as the second evaluation argument.
 *
 * @author eso
 */
public class CommunicationChain<I, V, O> extends CommunicationMethod<I, O> {

	private final CommunicationMethod<I, V> communicationMethod;

	private final Function<? super V, O> processValue;

	/**
	 * Creates a new instance.
	 *
	 * @param communicationMethod The communication method to evaluate
	 * @param processValue        The function to evaluate the communication
	 *                            result with
	 */
	CommunicationChain(CommunicationMethod<I, V> communicationMethod,
		Function<? super V, O> processValue) {
		super(processValue.getToken() + "(" + communicationMethod.getToken() +
			")", null);

		this.communicationMethod = communicationMethod;
		this.processValue = processValue;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public O doOn(Connection connection, I input) {
		V value = communicationMethod.evaluate(input, connection);

		return processValue.evaluate(value);
	}

	/**
	 * Returns the communication method of this instance.
	 *
	 * @return The communication method
	 */
	public final CommunicationMethod<I, V> getCommunicationMethod() {
		return communicationMethod;
	}

	/**
	 * Returns the value function of this instance.
	 *
	 * @return The value function
	 */
	public final Function<? super V, O> getValueFunction() {
		return processValue;
	}
}
