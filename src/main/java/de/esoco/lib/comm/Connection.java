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

import de.esoco.lib.manage.Closeable;

import java.net.URI;
import java.net.URISyntaxException;

import org.obrel.core.RelatedObject;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_ADDRESS;
import static de.esoco.lib.comm.CommunicationRelationTypes.PASSWORD;
import static de.esoco.lib.comm.CommunicationRelationTypes.USER_NAME;

import static org.obrel.type.MetaTypes.CLOSED;


/********************************************************************
 * The base class for all kinds of connections handled by the communication
 * framework.
 *
 * @author eso
 */
public class Connection extends RelatedObject implements Closeable
{
	//~ Instance fields --------------------------------------------------------

	private final Endpoint rEndpoint;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a connection to a certain endpoint. Private because only to be
	 * used by factory methods.
	 *
	 * @param rEndpoint The connection endpoint
	 */
	Connection(Endpoint rEndpoint)
	{
		this.rEndpoint = rEndpoint;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void close()
	{
		try
		{
			rEndpoint.closeConnection(this);
		}
		catch (Exception e)
		{
			if (e instanceof CommunicationException)
			{
				throw (CommunicationException) e;
			}
			else
			{
				throw new CommunicationException(e);
			}
		}

		set(CLOSED);
	}

	/***************************************
	 * Returns the endpoint of this instance.
	 *
	 * @return The connection endpoint
	 */
	public final Endpoint getEndpoint()
	{
		return rEndpoint;
	}

	/***************************************
	 * Returns the current password for this connection, either from the
	 * relations or from the URI.
	 *
	 * @return The password
	 */
	public String getPassword()
	{
		return get(PASSWORD);
	}

	/***************************************
	 * Returns an {@link URI} instance for the final endpoint of this
	 * connection.
	 *
	 * @return The URI object for this connection
	 */
	public URI getUri()
	{
		try
		{
			return new URI(getEndpoint().get(ENDPOINT_ADDRESS));
		}
		catch (URISyntaxException e)
		{
			throw new CommunicationException(e);
		}
	}

	/***************************************
	 * Returns the current user name for this connection, either from the
	 * relations or from the URI.
	 *
	 * @return The user name
	 */
	public String getUserName()
	{
		return get(USER_NAME);
	}
}
