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

import de.esoco.lib.comm.http.HttpRequestMethod;
import de.esoco.lib.comm.http.HttpStatusCode;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.type.CollectorType;
import org.obrel.type.StandardTypes;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.obrel.core.RelationTypeModifier.FINAL;
import static org.obrel.core.RelationTypeModifier.READONLY;
import static org.obrel.core.RelationTypes.newFlagType;
import static org.obrel.core.RelationTypes.newInitialValueType;
import static org.obrel.core.RelationTypes.newMapType;
import static org.obrel.core.RelationTypes.newType;

/**
 * Contains standard relation type definitions for the communications
 * framework.
 *
 * @author eso
 */
public class CommunicationRelationTypes {

	/**
	 * A reference to a communication endpoint.
	 */
	public static final RelationType<Endpoint> ENDPOINT = newType();

	/**
	 * The address of a communication endpoint (typically some kind of URI).
	 * This type is final so that it cannot be changed after it has been set.
	 */
	public static final RelationType<String> ENDPOINT_ADDRESS = newType(FINAL);

	/**
	 * A relation type to store an endpoint socket.
	 */
	public static final RelationType<Socket> ENDPOINT_SOCKET = newType();

	/**
	 * A relation type to store the input stream of a socket.
	 */
	public static final RelationType<InputStream> SOCKET_INPUT_STREAM =
		newType();

	/**
	 * A relation type to store the output stream of a socket.
	 */
	public static final RelationType<OutputStream> SOCKET_OUTPUT_STREAM =
		newType();

	/**
	 * A relation type to store a reader for a socket input stream.
	 */
	public static final RelationType<Reader> SOCKET_READER = newType();

	/**
	 * A relation type to store a writer for a socket input stream.
	 */
	public static final RelationType<PrintWriter> SOCKET_WRITER = newType();

	/**
	 * Defines the character encoding of a request. Has a default value of
	 * {@link StandardCharsets#UTF_8}.
	 */
	public static final RelationType<Charset> REQUEST_ENCODING =
		newInitialValueType(StandardCharsets.UTF_8);

	/**
	 * Defines the character encoding of a response. Has a default value of
	 * {@link StandardCharsets#UTF_8}.
	 */
	public static final RelationType<Charset> RESPONSE_ENCODING =
		newInitialValueType(StandardCharsets.UTF_8);

	/**
	 * A user name for the authentication on a communication endpoint.
	 */
	public static final RelationType<String> USER_NAME = newType();

	/**
	 * A password for the authentication on a communication endpoint.
	 */
	public static final RelationType<String> PASSWORD = newType();

	/**
	 * Contains a string description of the most recent request that has been
	 * performed in a network operation.
	 */
	public static final RelationType<String> LAST_REQUEST = newType();

	/**
	 * An automatic relation that collects the values of {@link #LAST_REQUEST}.
	 * Should be annotated with {@link StandardTypes#MAXIMUM} to limit the
	 * history size.
	 */
	public static final RelationType<Collection<String>> REQUEST_HISTORY =
		CollectorType.newCollector(String.class,
			(r, o) -> r.getType() == LAST_REQUEST ? o.toString() : null,
			READONLY);

	/**
	 * Contains the time (in milliseconds) that the handling of a request has
	 * consumed.
	 */
	public static final RelationType<Integer> REQUEST_HANDLING_TIME =
		newType();

	/**
	 * The timeout in milliseconds after which an attempt of a network
	 * connection will be interrupted. Defaults to 60 seconds (i.e. 60,000
	 * milliseconds).
	 */
	@SuppressWarnings("boxing")
	public static final RelationType<Integer> CONNECTION_TIMEOUT =
		newInitialValueType(60 * 1000);

	/**
	 * The maximum number of (concurrent) connections to or from a
	 * communication
	 * component.
	 */
	public static final RelationType<Integer> MAX_CONNECTIONS = newType();

	/**
	 * The maximum size that a request to a server allowed to have. Has a
	 * default value of 64 KiB.
	 */
	@SuppressWarnings("boxing")
	public static final RelationType<Integer> MAX_REQUEST_SIZE =
		newInitialValueType(1024 * 64);

	/**
	 * The maximum size that the response of an endpoint communication is
	 * allowed to have. Has a default value of 1 MiB.
	 */
	@SuppressWarnings("boxing")
	public static final RelationType<Integer> MAX_RESPONSE_SIZE =
		newInitialValueType(1024 * 1024);

	/**
	 * The buffer size to be used when communicating over an endpoint
	 * connection. Default value is 8 KiB.
	 */
	@SuppressWarnings("boxing")
	public static final RelationType<Integer> BUFFER_SIZE =
		newInitialValueType(1024 * 8);

	/**
	 * A flag that indicates that a connection performs encrypted communication
	 * like SSL, TLS, or SSH. Declared as final to prevent changes after
	 * initialization.
	 */
	public static final RelationType<Boolean> ENCRYPTION = newFlagType(FINAL);

	/**
	 * A flag to enabled SSL/TLS connections to endpoints that use self-signed
	 * certificates. ATTENTION: this should only be used for test environments,
	 * not for production systems.
	 */
	public static final RelationType<Boolean> TRUST_SELF_SIGNED_CERTIFICATES =
		newFlagType();

	/**
	 * The status code of an HTTP request. Will be available in a connection
	 * after an HTTP request has been executed.
	 */
	public static final RelationType<HttpStatusCode> HTTP_STATUS_CODE =
		newType();

	/**
	 * The maximum size of a line in an HTTP request header . Has a default
	 * value of 8 KiB.
	 */
	@SuppressWarnings("boxing")
	public static final RelationType<Integer> HTTP_MAX_HEADER_LINE_SIZE =
		newInitialValueType(1024 * 8);

	/**
	 * The method of an HTTP request.
	 */
	public static final RelationType<HttpRequestMethod> HTTP_REQUEST_METHOD =
		newType();

	/**
	 * The target path of an HTTP request.
	 */
	public static final RelationType<HttpRequestMethod> HTTP_REQUEST_PATH =
		newType();

	/**
	 * The headers for an HTTP request. These must be set on a connection or
	 * endpoint instance before a request is executed.
	 */
	public static final RelationType<Map<String, List<String>>>
		HTTP_REQUEST_HEADERS = newMapType(true);

	/**
	 * The headers of the response to an HTTP request. Will be available in a
	 * connection after an HTTP request has been executed. The contents is a
	 * mapping from header names to a list of header values in the order in
	 * which they are returned by {@link HttpURLConnection#getHeaderFields()}.
	 */
	public static final RelationType<Map<String, List<String>>>
		HTTP_RESPONSE_HEADERS = newMapType(true);

	/**
	 * The host name of a proxy server.
	 */
	public static final RelationType<String> PROXY_HOST = newType();

	/**
	 * The port of a proxy server.
	 */
	public static final RelationType<Integer> PROXY_PORT = newType();

	/**
	 * The optional user name for the connection to a proxy server.
	 */
	public static final RelationType<String> PROXY_USER = newType();

	/**
	 * The optional password for the connection to a proxy server.
	 */
	public static final RelationType<String> PROXY_PASSWORD = newType();

	static {
		RelationTypes.init(CommunicationRelationTypes.class);
	}

	/**
	 * Creates a new instance.
	 */
	private CommunicationRelationTypes() {
	}
}
