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
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogExtent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Describes a method to communicate with an endpoint in the communication
 * framework.
 *
 * @author eso
 */
public abstract class CommunicationMethod<I, O>
	extends AbstractBinaryFunction<I, Connection, O> {

	private final I defaultInput;

	/**
	 * Creates a new instance.
	 *
	 * @param methodName   The name of this method
	 * @param defaultInput The default input for this method
	 */
	public CommunicationMethod(String methodName, I defaultInput) {
		super(null, methodName);

		this.defaultInput = defaultInput;
	}

	/**
	 * Converts a communication method with argument and return value into a
	 * method that ignores input values and only returns a remote value (analog
	 * to the {@link Supplier} interface). This can be used to wrap
	 * communication methods that don't have different input values but should
	 * always be invoked with their default input.
	 *
	 * @param request The communication method that performs the actual request
	 *                to receive data from the remote endpoint
	 * @return A new communication method with a void input
	 */
	public static <T> CommunicationMethod<Void, T> doReceive(
		CommunicationMethod<?, T> request) {
		return new CommunicationMethod<Void, T>(request.getToken(), null) {
			@Override
			public T doOn(Connection connection, Void input) {
				return request.evaluate(null, connection);
			}
		};
	}

	/**
	 * Converts a communication method with argument and return value into a
	 * method that ignores return values and only transfers input values to a
	 * remote service (analog to the {@link Consumer} interface). This can be
	 * used to wrap communication methods that don't have a meaningful return
	 * value and should only be invoked to send values to an endpoint.
	 *
	 * @param request The communication method that performs the actual request
	 *                to send data to the remote endpoint
	 * @return A new communication method with a void return value
	 */
	public static <T> CommunicationMethod<T, Void> doSend(
		CommunicationMethod<T, ?> request) {
		return new CommunicationMethod<T, Void>(request.getToken(), null) {
			@Override
			public Void doOn(Connection connection, T input) {
				request.evaluate(input, connection);

				return null;
			}
		};
	}

	/**
	 * Must be implemented by subclasses to perform the actual communication
	 * over a connection to execute this method. This method must always be
	 * invoked with explicit parameters. An implicit default input value will
	 * only be considered by the standard method {@link #evaluate(Object)}.
	 *
	 * @param connection The connection to communicate over
	 * @param input      The input value for this communication method
	 * @return The output value according to this method's definition
	 * @throws Exception Any kind of exception may be thrown to signal errors
	 */
	public abstract O doOn(Connection connection, I input) throws Exception;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final O evaluate(I input, Connection connection) {
		LogExtent logExtent = connection.get(Log.LOG_EXTENT);

		try {
			if (input == null) {
				input = defaultInput;
			}

			O result = doOn(connection, input);

			if (logExtent.logs(LogExtent.SUCCESS)) {
				Log.info(getLogMessage(connection, input, null));
			}

			return result;
		} catch (Exception e) {
			if (logExtent.logs(LogExtent.ERRORS)) {
				Log.error(getLogMessage(connection, input, e), e);
			}

			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			} else {
				throw new CommunicationException(e);
			}
		}
	}

	/**
	 * Overloaded variant of {@link Function#from(Function)} that returns an
	 * instance of {@link EndpointFunction}.
	 *
	 * @param endpoint The endpoint to return the chain for
	 * @return A new endpoint chain that evaluates this method at the given
	 * endpoint
	 */
	public EndpointFunction<I, O> from(Endpoint endpoint) {
		return endpoint.then(this);
	}

	/**
	 * Returns the default input value of this method.
	 *
	 * @return The default input value
	 */
	public final I getDefaultInput() {
		return defaultInput;
	}

	/**
	 * A synonym for {@link #evaluate(Object, Connection)} that can be used to
	 * indicate that a value is retrieved from an endpoint connection.
	 *
	 * @see #evaluate(Object, Connection)
	 */
	public O getFrom(Connection connection, I input) {
		return evaluate(input, connection);
	}

	/**
	 * Semantic variant of {@link #from(Endpoint)} that indicates that a
	 * communication method is executed at a certain endpoint.
	 *
	 * @param endpoint The endpoint
	 * @return The endpoint chain of this method with the given endpoint
	 */
	public EndpointFunction<I, O> on(Endpoint endpoint) {
		return from(endpoint);
	}

	/**
	 * A synonym for {@link #evaluate(Object, Connection)} that can be used to
	 * indicate that a value is sent over an endpoint connection.
	 *
	 * @see #evaluate(Object, Connection)
	 */
	public O sendTo(Connection connection, I input) {
		return evaluate(input, connection);
	}

	/**
	 * Overloaded to return a special type of {@link CommunicationMethod} that
	 * can perform automatic resource handling for chained methods.
	 *
	 * @see AbstractBinaryFunction#then(Function)
	 */
	@Override
	public <T> CommunicationMethod<I, T> then(Function<? super O, T> other) {
		return new CommunicationChain<>(this, other);
	}

	/**
	 * Generates a message for this method that will be used if logging is
	 * enabled for the connection that this method is invoked upon. Subclasses
	 * can override this method to generate more specific log messages.
	 * Alternatively they may override
	 * {@link #getMethodDescription(Connection, Object)} which will be invoked
	 * to get a description of this method for the log message.
	 *
	 * @param connection The current connection
	 * @param input      The input value for the method invocation
	 * @param exception  In the case of an error logging the exception that
	 *                   occurred or NULL for the (info) logging of a
	 *                   successful
	 *                   request
	 * @return The log message
	 */
	protected String getLogMessage(Connection connection, I input,
		Exception exception) {
		String methodDescription = getMethodDescription(connection, input);
		String message;

		if (exception != null) {
			message = String.format("Failure: %s [%s]", methodDescription,
				exception.getMessage());
		} else {
			message = String.format("Success: %s", methodDescription);
		}

		return message;
	}

	/**
	 * Returns a description of this instance. this method be invoked by
	 * {@link #getLogMessage(Connection, Object, Exception)} to get a
	 * description for log messages. It may be overridden to provide a more
	 * specific description.
	 *
	 * @param connection The current connection
	 * @param input      The input value for the method invocation
	 * @return The method description
	 */
	protected String getMethodDescription(Connection connection, I input) {
		return String.format("%s(%s)", getToken(), input);
	}
}
