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

import de.esoco.lib.expression.Function;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogExtent;
import de.esoco.lib.text.TextConvert;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.obrel.core.FluentRelatable;
import org.obrel.core.ObjectRelations;
import org.obrel.core.Params;
import org.obrel.core.ProvidesConfiguration;
import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTION;
import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_ADDRESS;
import static de.esoco.lib.comm.CommunicationRelationTypes.PASSWORD;
import static de.esoco.lib.comm.CommunicationRelationTypes.USER_NAME;

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
 * #setGlobalConfiguration(ProvidesConfiguration)}. The provided {@link
 * Relatable} instance must then contain relations with the respective
 * configuration values.</p>
 *
 * @author eso
 */
public abstract class Endpoint extends RelatedObject
	implements Function<Relatable, Connection>, FluentRelatable<Endpoint>
{
	//~ Enums ------------------------------------------------------------------

	/********************************************************************
	 * Enumeration of the user info fields in a URI.
	 */
	public enum UserInfoField { USERNAME, PASSWORD }

	//~ Static fields/initializers ---------------------------------------------

	/**
	 * The URI scheme an endpoint has been created from (used as the function
	 * token).
	 */
	protected static final RelationType<String> ENDPOINT_SCHEME = newType();

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
	private static ProvidesConfiguration rGlobalConfig = new Params();

	/** The default parameters for all endpoint instances. */
	private static Relatable aDefaultParams = new Params();

	static
	{
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
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns an endpoint instance for a certain URI. The endpoint will be
	 * initialized from the URI data. Authentication data will be moved from the
	 * endpoint address URI to the {@link CommunicationRelationTypes#USER_NAME}
	 * and {@link CommunicationRelationTypes#PASSWORD} relations.
	 *
	 * @param  sEndpointUri The URI to create the endpoint for
	 *
	 * @return The endpoint for the given URI
	 */
	public static Endpoint at(String sEndpointUri)
	{
		URI		 aUri	    = createUri(sEndpointUri);
		String   sScheme    = aUri.getScheme().toUpperCase();
		Boolean  bEncrypted = Boolean.valueOf(sScheme.endsWith("S"));
		Endpoint aEndpoint;

		Class<? extends Endpoint> aEndpointClass =
			aEndpointRegistry.get(sScheme);

		if (aEndpointClass == null)
		{
			aEndpointClass = getDefaultEndpoint(sScheme);
		}

		if (aEndpointClass != null)
		{
			try
			{
				aEndpoint = aEndpointClass.newInstance();
				aEndpoint.set(ENDPOINT_SCHEME, sScheme);
				aEndpoint.set(
					USER_NAME,
					getUserInfoField(aUri, UserInfoField.USERNAME));
				aEndpoint.set(
					PASSWORD,
					getUserInfoField(aUri, UserInfoField.PASSWORD));

				String sUserInfo = aUri.getUserInfo();

				if (sUserInfo != null)
				{
					// remove authentication data from URI string after storing
					// user and password to prevent it from leaking when the
					// endpoint address is accessed
					sEndpointUri = sEndpointUri.replaceAll(sUserInfo + "@", "");
				}

				aEndpoint.set(ENDPOINT_ADDRESS, sEndpointUri);

				if (!aEndpoint.hasRelation(ENCRYPTION))
				{
					aEndpoint.set(ENCRYPTION, bEncrypted);
				}

				aEndpoint.init();
			}
			catch (Exception e)
			{
				throw new CommunicationException(
					"Could not create endpoint for scheme " +
					sScheme,
					e);
			}
		}
		else
		{
			throw new CommunicationException(
				"No endpoint registered for scheme " +
				sScheme);
		}

		return aEndpoint;
	}

	/***************************************
	 * Returns the default parameters for all endpoints. These parameters will
	 * only be used if they are not overridden by any endpoint-specific
	 * parameters.
	 *
	 * @return A relatable containing the default parameters for all endpoint
	 *         instances
	 */
	public static final Relatable getDefaultParams()
	{
		return aDefaultParams;
	}

	/***************************************
	 * Returns the global endpoint configuration which is a {@link Relatable}
	 * object containing the configuration relations. An application-specific
	 * configuration can been set with {@link
	 * #setGlobalConfiguration(ProvidesConfiguration)}.
	 *
	 * @return The global endpoint configuration
	 */
	public static ProvidesConfiguration getGlobalConfiguration()
	{
		return rGlobalConfig;
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
	 * Sets (and replaces) the default parameters for all endpoints. These
	 * parameters will only be used if they are not overridden by any
	 * endpoint-specific parameters.
	 *
	 * @param rDefaultParams A relatable containing the default parameters for
	 *                       all endpoint instances (must not be NULL)
	 */
	public static final void setDefaultParams(Relatable rDefaultParams)
	{
		Objects.nonNull(rDefaultParams);

		aDefaultParams = rDefaultParams;
	}

	/***************************************
	 * Sets the global endpoint configuration. The configuration is an arbitrary
	 * {@link Relatable} object that must contain the relations with the
	 * configuration values for the endpoints needed by an application. NULL
	 * values will cause an exception. To clear the configuration an empty
	 * relatable should be set.
	 *
	 * @param rConfiguration The global configuration object (must not be NULL)
	 */
	public static void setGlobalConfiguration(
		ProvidesConfiguration rConfiguration)
	{
		Objects.nonNull(rConfiguration);

		rGlobalConfig = rConfiguration;

		LogExtent eLogExtent =
			rGlobalConfig.getConfigValue(
				Log.LOG_EXTENT,
				aDefaultParams.get(Log.LOG_EXTENT));

		aDefaultParams.set(Log.LOG_EXTENT, eLogExtent);
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
		return rGlobalConfig.getConfigValue(rConfigType, rDefaultValue);
	}

	/***************************************
	 * Converts a string into a URI and converts any occurring exception into a
	 * runtime {@link CommunicationException}.
	 *
	 * @param  sUri The URI string
	 *
	 * @return The {@link URI} object
	 */
	private static URI createUri(String sUri)
	{
		try
		{
			return new URI(sUri);
		}
		catch (URISyntaxException e)
		{
			throw new CommunicationException(e);
		}
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
	private static Class<? extends Endpoint> getDefaultEndpoint(String sScheme)
		throws CommunicationException
	{
		Class<? extends Endpoint> aEndpointClass;

		try
		{
			String sPrefix  = sScheme.toUpperCase().replaceAll("-", "_");
			String sPackage = Endpoint.class.getPackage().getName();

			if (sPrefix.endsWith("S"))
			{
				sPrefix = sPrefix.substring(0, sPrefix.length() - 1);
			}

			String sDefaultName =
				TextConvert.capitalizedIdentifier(sPrefix) +
				Endpoint.class.getSimpleName();

			aEndpointClass =
				(Class<? extends Endpoint>) Class.forName(
					sPackage + "." + sDefaultName);

			registerEndpointType(aEndpointClass, sPrefix, sPrefix + "s");
		}
		catch (ClassNotFoundException e)
		{
			throw new CommunicationException(
				"No endpoint for scheme " +
				sScheme);
		}

		return aEndpointClass;
	}

	/***************************************
	 * Returns a field from the user info part of a URI.
	 *
	 * @param  rUri   The URI
	 * @param  eField bPassword TRUE to return the password field, FALSE to
	 *                return the user name field
	 *
	 * @return The user info field (NULL for none)
	 */
	private static String getUserInfoField(URI rUri, UserInfoField eField)
	{
		String sUserInfo = rUri.getUserInfo();
		String sField    = null;

		if (sUserInfo != null)
		{
			int nIndex = sUserInfo.indexOf(':');

			if (nIndex >= 0)
			{
				sField =
					eField == UserInfoField.PASSWORD
					? sUserInfo.substring(nIndex + 1)
					: sUserInfo.substring(0, nIndex);
			}
		}

		return sField;
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
	 * Returns a connection that is initialized for communication with this
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

		ObjectRelations.copyRelations(aDefaultParams, aConnection, true);
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
	 * Overridden to create a communication chain.
	 *
	 * @see Function#then(Function)
	 */
	public <I, O> EndpointFunction<I, O> then(CommunicationMethod<I, O> fMethod)
	{
		return new EndpointFunction<>(this, fMethod);
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
