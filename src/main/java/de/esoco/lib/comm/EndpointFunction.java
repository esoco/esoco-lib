//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.expression.BinaryFunction;
import de.esoco.lib.expression.Function;
import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;

/**
 * A function that applies a {@link CommunicationMethod} to an endpoint and
 * automatically performs the resource handling upon evaluation (i.e. closing
 * the endpoint {@link Connection}).
 *
 * @author eso
 */
public class EndpointFunction<I, O> extends RelatedObject
	implements BinaryFunction<I, Relatable, O> {

	private final Endpoint endpoint;

	private final CommunicationMethod<I, O> method;

	/**
	 * Creates a new instance.
	 *
	 * @param endpoint The endpoint to invoke the method on
	 * @param method   The communication method to invoke
	 */
	public EndpointFunction(Endpoint endpoint,
		CommunicationMethod<I, O> method) {
		this.endpoint = endpoint;
		this.method = method;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public O evaluate(I input, Relatable params) {
		try (Connection connection = endpoint.connect(params)) {
			set(Endpoint.ENDPOINT_CONNECTION, connection);

			return method.evaluate(input, connection);
		}
	}

	/**
	 * A semantic variant of {@link #evaluate(Object)} to request from the
	 * endpoint data with the default input of the invoked method.
	 *
	 * @return The function result for the default method input
	 */
	public O receive() {
		return evaluate(null);
	}

	/**
	 * A semantic variant of {@link #evaluate(Object)} to send data to the
	 * endpoint with the default input of the invoked method.
	 *
	 * @return The function result for the default method input
	 */
	public O send() {
		return evaluate(null);
	}

	/**
	 * A semantic variant of {@link #evaluate(Object)} to send data to the
	 * endpoint.
	 *
	 * @param input The input of the endpoint request
	 * @return The result of the endpoint request
	 */
	public O send(I input) {
		return evaluate(input);
	}

	/**
	 * Overridden to create a new endpoint chain.
	 *
	 * @see Function#then(Function)
	 */
	@Override
	public <T> EndpointFunction<I, T> then(Function<? super O, T> other) {
		return new EndpointFunction<>(endpoint,
			new CommunicationChain<>(method, other));
	}
}
