//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//		 http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.lib.comm;

import de.esoco.lib.expression.Function;
import de.esoco.lib.expression.function.AbstractFunction;
import de.esoco.lib.text.TextConvert;

import java.io.InputStream;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.Map;

import org.obrel.core.ObjectRelations;
import org.obrel.core.Relatable;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTED_CONNECTION;
import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_ADDRESS;

import static org.obrel.core.RelationTypeModifier.PRIVATE;
import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * Describes an endpoint of a connection in the communication framework. New
 * endpoint types must be registered for the URI schemes they can handle by
 * invoking {@link #registerEndpointType(Class, String, String...)}. All
 * implementations must provide a no-arguments constructor which it is used by
 * the factory method {@link #at(String)} to create new endpoint instances.
 *
 * <p>Some endpoint implementations may need access to global configuration
 * values. These can be injected by invoking the static method {@link
 * #setGlobalConfiguration(Relatable)}. The provided {@link Relatable} instance
 * must then contain relations with the respective configuration values.</p>
 *
 * @author eso
 */
public abstract class Endpoint extends AbstractFunction<Relatable, Connection>
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * A token that describes an endpoint type (used by the function base
	 * class).
	 */
	protected static final RelationType<String> ENDPOINT_TOKEN = newType();

	/** An internal relation type to store an active endpoint connection. */
	protected static final RelationType<Connection> ENDPOINT_CONNECTION =
		newType(PRIVATE);

	/**
	 * An internal relation type to store an output stream used by an endpoint.
	 */
	protected static final RelationType<OutputStream> ENDPOINT_OUTPUT_STREAM =
		newType(PRIVATE);

	/**
	 * An internal relation type to store an input stream used by an endpoint.
	 */
	protected static final RelationType<InputStream> ENDPOINT_INPUT_STREAM =
		newType(PRIVATE);

	private static Map<String, Class<? extends Endpoint>> aEndpointRegistry =
		new HashMap<>();

	/**
	 * A relatable object that provides global configuration data for all
	 * endpoints. Must be initialized by invoking {@link
	 * #setGlobalConfiguration(Relatable)}.
	 */
	private static Relatable rGlobalConfig = null;

	static
	{
		// init global relation types here too because Endpoint is the top level
		// class in the communications framework
		RelationTypes.init(CommunicationRelationTypes.class);
		RelationTypes.init(Endpoint.class);
	}

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * The standard constructor. All subclasses must provide a no-arguments
	 * constructor because it is used by the factory method {@link #at(String)}
	 * to create new endpoint instances.
	 */
	public Endpoint()
	{
		super("");
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns an endpoint instance for a certain URI.
	 *
	 * @param  sUri The URI to create the endpoint for
	 *
	 * @return The endpoint for the given URI
	 */
	public static Endpoint at(String sUri)
	{
		int		 nColonPos  = sUri.indexOf(':');
		String   sScheme    = sUri.substring(0, nColonPos).toUpperCase();
		Boolean  bEncrypted = Boolean.valueOf(sScheme.endsWith("S"));
		Endpoint aEndpoint;

		Class<? extends Endpoint> aEndpointClass =
			aEndpointRegistry.get(sScheme);

		if (aEndpointClass == null)
		{
			aEndpointClass = tryRegisterDefaultEndpoint(sScheme);
		}

		if (aEndpointClass != null)
		{
			try
			{
				aEndpoint = aEndpointClass.newInstance();
				aEndpoint.set(ENDPOINT_TOKEN, sScheme);
				aEndpoint.set(ENDPOINT_ADDRESS, sUri);
				aEndpoint.init();

				if (!aEndpoint.hasRelation(ENCRYPTED_CONNECTION))
				{
					aEndpoint.set(ENCRYPTED_CONNECTION, bEncrypted);
				}
			}
			catch (Exception e)
			{
				throw new CommunicationException("Could not create endpoint for scheme " +
												 sScheme,
												 e);
			}
		}
		else
		{
			throw new CommunicationException("No endpoint registered for scheme " +
											 sScheme);
		}

		return aEndpoint;
	}

	/***************************************
	 * Registers a new endpoint type for one or more URI schemes. Additional
	 * schemes typically are SSL/TLS-encrypted variants of the base scheme or
	 * vice versa.
	 *
	 * @param rEndpointClass     The endpoint type to register
	 * @param sPrimaryScheme     The primary URI scheme to associate the type
	 *                           with
	 * @param sAdditionalSchemes Optional additional URI schemes
	 */
	public static void registerEndpointType(
		Class<? extends Endpoint> rEndpointClass,
		String					  sPrimaryScheme,
		String... 				  sAdditionalSchemes)
	{
		RelationTypes.init(rEndpointClass);
		aEndpointRegistry.put(sPrimaryScheme.toUpperCase(), rEndpointClass);

		for (String sScheme : sAdditionalSchemes)
		{
			aEndpointRegistry.put(sScheme.toUpperCase(), rEndpointClass);
		}
	}

	/***************************************
	 * Sets the global endpoint configuration. The configuration is an arbitrary
	 * {@link Relatable} object that must contain the relations with the
	 * configuration values for the endpoints needed by an application.
	 *
	 * @param rConfiguration The global configuration object
	 */
	public static void setGlobalConfiguration(Relatable rConfiguration)
	{
		if (rGlobalConfig != null)
		{
			rGlobalConfig = rConfiguration;
		}
	}

	/***************************************
	 * Returns a configuration value from the global endpoint configuration.
	 *
	 * @param  rConfigType   The configuration relation type
	 * @param  rDefaultValue The default value to return if no configuration
	 *                       value is available
	 *
	 * @return The configuration value or the default if no config available
	 */
	protected static <T> T getConfigValue(
		RelationType<T> rConfigType,
		T				rDefaultValue)
	{
		T rConfigValue = rDefaultValue;

		if (rGlobalConfig != null && rGlobalConfig.hasRelation(rConfigType))
		{
			rConfigValue = rGlobalConfig.get(rConfigType);
		}

		return rConfigValue;
	}

	/***************************************
	 * Tries to lookup an endpoint implementation based on an URL scheme and if
	 * found registers it for plain and encrypted protocol variants.
	 *
	 * @param  sScheme The scheme to lookup an endpoint class for
	 *
	 * @return The class of the endpoint implementation
	 *
	 * @throws CommunicationException If no endpoint implementation for the
	 *                                given scheme could be found
	 */
	@SuppressWarnings("unchecked")
	private static Class<? extends Endpoint> tryRegisterDefaultEndpoint(
		String sScheme) throws CommunicationException
	{
		Class<? extends Endpoint> aEndpointClass;

		try
		{
			String sPrefix  = sScheme.toUpperCase();
			String sPackage = Endpoint.class.getPackage().getName();

			if (sPrefix.endsWith("S"))
			{
				sPrefix = sPrefix.substring(0, sPrefix.length() - 1);
			}

			String sDefaultName =
				TextConvert.capitalizedIdentifier(sPrefix) +
				Endpoint.class.getSimpleName();

			aEndpointClass =
				(Class<? extends Endpoint>) Class.forName(sPackage + "." +
														  sDefaultName);

			registerEndpointType(aEndpointClass, sPrefix, sPrefix + "s");
		}
		catch (ClassNotFoundException e)
		{
			throw new CommunicationException("No endpoint for scheme " +
											 sScheme);
		}

		return aEndpointClass;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * A no-argument connect method that invokes {@link #connect(Relatable)}
	 * with a NULL parameter.
	 *
	 * @return A connection initialized for this endpoint
	 */
	public Connection connect()
	{
		return connect(null);
	}

	/***************************************
	 * Returns a connection that is initialized for communicating with this
	 * endpoint. It is also the implementation of and a synonym for the standard
	 * {@link Function} method {@link #evaluate(Relatable)}.
	 *
	 * @param  rParams Optional connection parameters or NULL for none
	 *
	 * @return The new connection
	 */
	public Connection connect(Relatable rParams)
	{
		Connection aConnection = new Connection(this);

		ObjectRelations.copyRelations(this, aConnection, true);

		if (rParams != null)
		{
			ObjectRelations.copyRelations(rParams, aConnection, true);
		}

		try
		{
			initConnection(aConnection);
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

		return aConnection;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public Connection evaluate(Relatable rParams)
	{
		return connect(rParams);
	}

	/***************************************
	 * Overridden to return an endpoint-specific function token.
	 *
	 * @see AbstractFunction#getToken()
	 */
	@Override
	public String getToken()
	{
		return get(ENDPOINT_TOKEN);
	}

	/***************************************
	 * Overridden to create a communication chain.
	 *
	 * @see AbstractFunction#then(Function)
	 */
	public <I, O> EndpointChain<I, O> then(CommunicationMethod<I, O> fMethod)
	{
		return new EndpointChain<>(this, fMethod);
	}

	/***************************************
	 * Closes any resources that have been acquired on initializing the given
	 * connection. Any exception thrown by this method will be converted into a
	 * {@link CommunicationException}.
	 *
	 * @param  rConnection The connection to close
	 *
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected abstract void closeConnection(Connection rConnection)
		throws Exception;

	/***************************************
	 * Must be implemented to initialize any resources that are needed to use
	 * the given connection. Any exception thrown by this method will be
	 * converted into a {@link CommunicationException}.
	 *
	 * @param  rConnection The connection to initialize
	 *
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected abstract void initConnection(Connection rConnection)
		throws Exception;

	/***************************************
	 * Can be implemented by subclasses to perform additional initialization
	 * from the connection URI and/or the global configuration.
	 */
	protected void init()
	{
	}
}
