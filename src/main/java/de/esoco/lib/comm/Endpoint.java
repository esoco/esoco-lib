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
import org.obrel.core.FluentRelatable;
import org.obrel.core.ObjectRelations;
import org.obrel.core.Params;
import org.obrel.core.ProvidesConfiguration;
import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTION;
import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_ADDRESS;
import static de.esoco.lib.comm.CommunicationRelationTypes.PASSWORD;
import static de.esoco.lib.comm.CommunicationRelationTypes.USER_NAME;
import static org.obrel.core.RelationTypeModifier.PRIVATE;
import static org.obrel.core.RelationTypes.newType;

/**
 * Describes an endpoint of a connection in the communication framework. New
 * endpoint types must be registered for the URI schemes they can handle by
 * invoking {@link #registerEndpointType(Class, String, String...)}. All
 * implementations must provide a no-arguments constructor which it is used by
 * the factory method {@link #at(String)} to create new endpoint instances.
 *
 * <p>Some endpoint implementations may need access to global configuration
 * values. These can be injected by invoking the static method
 * {@link #setGlobalConfiguration(ProvidesConfiguration)}. The provided
 * {@link Relatable} instance must then contain relations with the respective
 * configuration values.</p>
 *
 * @author eso
 */
public abstract class Endpoint extends RelatedObject
	implements Function<Relatable, Connection>, FluentRelatable<Endpoint> {

	/**
	 * Enumeration of the user info fields in a URI.
	 */
	public enum UserInfoField {USERNAME, PASSWORD}

	/**
	 * The URI scheme an endpoint has been created from (used as the function
	 * token).
	 */
	protected static final RelationType<String> ENDPOINT_SCHEME = newType();

	/**
	 * An internal relation type to store an active endpoint connection.
	 */
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

	private static final Map<String, Class<? extends Endpoint>>
		endpointRegistry = new HashMap<>();

	/**
	 * The default parameters for all endpoint instances.
	 */
	private static final Relatable defaultParams = new Params();

	/**
	 * A relatable object that provides global configuration data for all
	 * endpoints. Must be initialized by invoking
	 * {@link #setGlobalConfiguration(ProvidesConfiguration)}.
	 */
	private static ProvidesConfiguration globalConfig = new Params();

	static {
		RelationTypes.init(Endpoint.class);
	}

	/**
	 * The standard constructor. All subclasses must provide a no-arguments
	 * constructor because it is used by the factory method {@link #at(String)}
	 * to create new endpoint instances.
	 */
	public Endpoint() {
	}

	/**
	 * Returns an endpoint instance for a certain URI. The endpoint will be
	 * initialized from the URI data. Authentication data will be moved from
	 * the
	 * endpoint address URI to the {@link CommunicationRelationTypes#USER_NAME}
	 * and {@link CommunicationRelationTypes#PASSWORD} relations.
	 *
	 * @param endpointUri The URI to create the endpoint for
	 * @return The endpoint for the given URI
	 */
	public static Endpoint at(String endpointUri) {
		URI uri = createUri(endpointUri);
		String scheme = uri.getScheme().toUpperCase();
		Boolean encrypted = scheme.endsWith("S");
		Endpoint endpoint;

		Class<? extends Endpoint> endpointClass = endpointRegistry.get(scheme);

		if (endpointClass == null) {
			endpointClass = getDefaultEndpoint(scheme);
		}

		if (endpointClass != null) {
			try {
				endpoint = endpointClass.newInstance();
				endpoint.set(ENDPOINT_SCHEME, scheme);
				endpoint.set(USER_NAME,
					getUserInfoField(uri, UserInfoField.USERNAME));
				endpoint.set(PASSWORD,
					getUserInfoField(uri, UserInfoField.PASSWORD));

				String userInfo = uri.getUserInfo();

				if (userInfo != null) {
					// remove authentication data from URI string after storing
					// user and password to prevent it from leaking when the
					// endpoint address is accessed
					endpointUri = endpointUri.replaceAll(userInfo + "@", "");
				}

				endpoint.set(ENDPOINT_ADDRESS, endpointUri);

				if (!endpoint.hasRelation(ENCRYPTION)) {
					endpoint.set(ENCRYPTION, encrypted);
				}

				endpoint.init();
			} catch (Exception e) {
				throw new CommunicationException(
					"Could not create endpoint for scheme " + scheme, e);
			}
		} else {
			throw new CommunicationException(
				"No endpoint registered for scheme " + scheme);
		}

		return endpoint;
	}

	/**
	 * Converts a string into a URI and converts any occurring exception into a
	 * runtime {@link CommunicationException}.
	 *
	 * @param uri The URI string
	 * @return The {@link URI} object
	 */
	private static URI createUri(String uri) {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new CommunicationException(e);
		}
	}

	/**
	 * Returns a configuration value from the global endpoint configuration.
	 *
	 * @param configType   The configuration relation type
	 * @param defaultValue The default value to return if no configuration
	 *                        value
	 *                     is available
	 * @return The configuration value or the default if no config available
	 */
	protected static <T> T getConfigValue(RelationType<T> configType,
		T defaultValue) {
		return globalConfig.getConfigValue(configType, defaultValue);
	}

	/**
	 * Tries to lookup an endpoint implementation based on an URL scheme and if
	 * found registers it for plain and encrypted protocol variants.
	 *
	 * @param scheme The scheme to lookup an endpoint class for
	 * @return The class of the endpoint implementation
	 * @throws CommunicationException If no endpoint implementation for the
	 *                                given scheme could be found
	 */
	@SuppressWarnings("unchecked")
	private static Class<? extends Endpoint> getDefaultEndpoint(String scheme)
		throws CommunicationException {
		Class<? extends Endpoint> endpointClass;

		try {
			String prefix = scheme.toUpperCase().replaceAll("-", "_");
			String pkg = Endpoint.class.getPackage().getName();

			if (prefix.endsWith("S")) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}

			String defaultName = TextConvert.capitalizedIdentifier(prefix) +
				Endpoint.class.getSimpleName();

			endpointClass = (Class<? extends Endpoint>) Class.forName(
				pkg + "." + defaultName);

			registerEndpointType(endpointClass, prefix, prefix + "s");
		} catch (ClassNotFoundException e) {
			throw new CommunicationException(
				"No endpoint for scheme " + scheme);
		}

		return endpointClass;
	}

	/**
	 * Returns the default parameters for all endpoints. These parameters will
	 * only be used if they are not overridden by any endpoint-specific
	 * parameters.
	 *
	 * @return A relatable containing the default parameters for all endpoint
	 * instances
	 */
	public static final Relatable getDefaultParams() {
		return defaultParams;
	}

	/**
	 * Returns the global endpoint configuration which is a {@link Relatable}
	 * object containing the configuration relations. An application-specific
	 * configuration can been set with
	 * {@link #setGlobalConfiguration(ProvidesConfiguration)}.
	 *
	 * @return The global endpoint configuration
	 */
	public static ProvidesConfiguration getGlobalConfiguration() {
		return globalConfig;
	}

	/**
	 * Returns a field from the user info part of a URI.
	 *
	 * @param uri       The URI
	 * @param infoField bPassword TRUE to return the password field, FALSE to
	 *                  return the username field
	 * @return The user info field (NULL for none)
	 */
	private static String getUserInfoField(URI uri, UserInfoField infoField) {
		String userInfo = uri.getUserInfo();
		String field = null;

		if (userInfo != null) {
			int index = userInfo.indexOf(':');

			if (index >= 0) {
				field = infoField == UserInfoField.PASSWORD ?
				        userInfo.substring(index + 1) :
				        userInfo.substring(0, index);
			}
		}

		return field;
	}

	/**
	 * Registers a new endpoint type for one or more URI schemes. Additional
	 * schemes typically are SSL/TLS-encrypted variants of the base scheme or
	 * vice versa.
	 *
	 * @param endpointClass     The endpoint type to register
	 * @param primaryScheme     The primary URI scheme to associate the type
	 *                          with
	 * @param additionalSchemes Optional additional URI schemes
	 */
	public static void registerEndpointType(
		Class<? extends Endpoint> endpointClass, String primaryScheme,
		String... additionalSchemes) {
		RelationTypes.init(endpointClass);
		endpointRegistry.put(primaryScheme.toUpperCase(), endpointClass);

		for (String scheme : additionalSchemes) {
			endpointRegistry.put(scheme.toUpperCase(), endpointClass);
		}
	}

	/**
	 * Sets (and replaces) the default parameters for all endpoints. These
	 * parameters will only be used if they are not overridden by any
	 * endpoint-specific parameters.
	 *
	 * @param defaultParams A relatable containing the default parameters for
	 *                      all endpoint instances (must not be NULL)
	 */
	public static final void setDefaultParams(Relatable defaultParams) {
		Objects.nonNull(defaultParams);

		defaultParams = defaultParams;
	}

	/**
	 * Sets the global endpoint configuration. The configuration is an
	 * arbitrary
	 * {@link Relatable} object that must contain the relations with the
	 * configuration values for the endpoints needed by an application. NULL
	 * values will cause an exception. To clear the configuration an empty
	 * relatable should be set.
	 *
	 * @param configuration The global configuration object (must not be NULL)
	 */
	public static void setGlobalConfiguration(
		ProvidesConfiguration configuration) {
		Objects.nonNull(configuration);

		globalConfig = configuration;

		LogExtent logExtent = globalConfig.getConfigValue(Log.LOG_EXTENT,
			defaultParams.get(Log.LOG_EXTENT));

		defaultParams.set(Log.LOG_EXTENT, logExtent);
	}

	/**
	 * A no-argument connect method that invokes {@link #connect(Relatable)}
	 * with a NULL parameter.
	 *
	 * @return A connection initialized for this endpoint
	 */
	public Connection connect() {
		return connect(null);
	}

	/**
	 * Returns a connection that is initialized for communication with this
	 * endpoint. It is also the implementation of and a synonym for the
	 * standard
	 * {@link Function} method {@link #evaluate(Relatable)}.
	 *
	 * @param params Optional connection parameters or NULL for none
	 * @return The new connection
	 */
	public Connection connect(Relatable params) {
		Connection connection = new Connection(this);

		ObjectRelations.copyRelations(defaultParams, connection, true);
		ObjectRelations.copyRelations(this, connection, true);

		if (params != null) {
			ObjectRelations.copyRelations(params, connection, true);
		}

		try {
			initConnection(connection);
		} catch (Exception e) {
			if (e instanceof CommunicationException) {
				throw (CommunicationException) e;
			} else {
				throw new CommunicationException(e);
			}
		}

		return connection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Connection evaluate(Relatable params) {
		return connect(params);
	}

	/**
	 * Overridden to create a communication chain.
	 *
	 * @see Function#then(Function)
	 */
	public <I, O> EndpointFunction<I, O> then(
		CommunicationMethod<I, O> method) {
		return new EndpointFunction<>(this, method);
	}

	/**
	 * Closes any resources that have been acquired on initializing the given
	 * connection. Any exception thrown by this method will be converted into a
	 * {@link CommunicationException}.
	 *
	 * @param connection The connection to close
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected abstract void closeConnection(Connection connection)
		throws Exception;

	/**
	 * Can be implemented by subclasses to perform additional initialization
	 * from the connection URI and/or the global configuration.
	 */
	protected void init() {
	}

	/**
	 * Must be implemented to initialize any resources that are needed to use
	 * the given connection. Any exception thrown by this method will be
	 * converted into a {@link CommunicationException}.
	 *
	 * @param connection The connection to initialize
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected abstract void initConnection(Connection connection)
		throws Exception;
}
