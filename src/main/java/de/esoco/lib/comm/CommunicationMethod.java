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

import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_LOG_EXTENT;


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
		LogExtent eLogExtent = rConnection.get(ENDPOINT_LOG_EXTENT);

		try
		{
			O rResult =
				doOn(rConnection, rInput != null ? rInput : rDefaultInput);

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
				String.format("%s failed: %s",
							  sMethodDescription,
							  rException.getMessage());
		}
		else
		{
			sMessage =
				String.format("%s executed successfully", sMethodDescription);
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
