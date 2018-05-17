//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Converts a communication method with argument and return value into a
	 * method that ignores input values and only returns a remote value (analog
	 * to the {@link Supplier} interface). This can be used to wrap
	 * communication methods that don't have different input values but should
	 * always be invoked with their default input.
	 *
	 * @param  fRequest The communication method that performs the actual
	 *                  request to receive data from the remote endpoint
	 *
	 * @return A new communication method with a void input
	 */
	public static <T> CommunicationMethod<Void, T> doReceive(
		CommunicationMethod<?, T> fRequest)
	{
		return new CommunicationMethod<Void, T>(fRequest.getToken(), null)
		{
			@Override
			public T doOn(Connection rConnection, Void rInput)
			{
				return fRequest.evaluate(null, rConnection);
			}
		};
	}

	/***************************************
	 * Converts a communication method with argument and return value into a
	 * method that ignores return values and only transfers input values to a
	 * remote service (analog to the {@link Consumer} interface). This can be
	 * used to wrap communication methods that don't have a meaningful return
	 * value and should only be invoked to send values to an endpoint.
	 *
	 * @param  fRequest The communication method that performs the actual
	 *                  request to send data to the remote endpoint
	 *
	 * @return A new communication method with a void return value
	 */
	public static <T> CommunicationMethod<T, Void> doSend(
		CommunicationMethod<T, ?> fRequest)
	{
		return new CommunicationMethod<T, Void>(fRequest.getToken(), null)
		{
			@Override
			public Void doOn(Connection rConnection, T rInput)
			{
				fRequest.evaluate(rInput, rConnection);

				return null;
			}
		};
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
	 * Semantic variant of {@link #from(Endpoint)} that indicates that a
	 * communication method is executed at a certain endpoint.
	 *
	 * @param  rEndpoint The endpoint
	 *
	 * @return The endpoint chain of this method with the given endpoint
	 */
	public EndpointFunction<I, O> on(Endpoint rEndpoint)
	{
		return from(rEndpoint);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public final O evaluate(I rInput, Connection rConnection)
	{
		LogExtent eLogExtent = rConnection.get(Log.LOG_EXTENT);

		try
		{
			if (rInput == null)
			{
				rInput = rDefaultInput;
			}

			O rResult = doOn(rConnection, rInput);

			if (eLogExtent.logs(LogExtent.SUCCESS))
			{
				Log.info(getLogMessage(rConnection, rInput, null));
			}

			return rResult;
		}
		catch (RuntimeException e)
		{
			if (eLogExtent.logs(LogExtent.ERRORS))
			{
				Log.error(getLogMessage(rConnection, rInput, e), e);
			}

			throw e;
		}
	}

	/***************************************
	 * Overloaded variant of {@link Function#from(Function)} that returns an
	 * instance of {@link EndpointFunction}.
	 *
	 * @param  rEndpoint The endpoint to return the chain for
	 *
	 * @return A new endpoint chain that evaluates this method at the given
	 *         endpoint
	 */
	public EndpointFunction<I, O> from(Endpoint rEndpoint)
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

	/***************************************
	 * Generates a message for this method that will be used if logging is
	 * enabled for the connection that this method is invoked upon. Subclasses
	 * can override this method to generate more specific log messages.
	 * Alternatively they may override {@link #getMethodDescription(Connection,
	 * Object)} which will be invoked to get a description of this method for
	 * the log message.
	 *
	 * @param  rConnection The current connection
	 * @param  rInput      The input value for the method invocation
	 * @param  rException  In the case of an error logging the exception that
	 *                     occurred or NULL for the (info) logging of a
	 *                     successful request
	 *
	 * @return The log message
	 */
	protected String getLogMessage(Connection rConnection,
								   I		  rInput,
								   Exception  rException)
	{
		String sMethodDescription = getMethodDescription(rConnection, rInput);
		String sMessage;

		if (rException != null)
		{
			sMessage =
				String.format("Failure: %s [%s]",
							  sMethodDescription,
							  rException.getMessage());
		}
		else
		{
			sMessage = String.format("Success: %s", sMethodDescription);
		}

		return sMessage;
	}

	/***************************************
	 * Returns a description of this instance. this method be invoked by {@link
	 * #getLogMessage(Connection, Object, Exception)} to get a description for
	 * log messages. It may be overridden to provide a more specific
	 * description.
	 *
	 * @param  rConnection The current connection
	 * @param  rInput      The input value for the method invocation
	 *
	 * @return The method description
	 */
	protected String getMethodDescription(Connection rConnection, I rInput)
	{
		return String.format("%s(%s)", getToken(), rInput);
	}
}
