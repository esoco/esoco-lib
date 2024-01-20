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
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.net.NetUtil;
import de.esoco.lib.net.NetUtil.SocketType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.net.URI;

import static de.esoco.lib.comm.CommunicationRelationTypes.CONNECTION_TIMEOUT;
import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTION;
import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_SOCKET;
import static de.esoco.lib.comm.CommunicationRelationTypes.SOCKET_INPUT_STREAM;
import static de.esoco.lib.comm.CommunicationRelationTypes.SOCKET_OUTPUT_STREAM;
import static de.esoco.lib.comm.CommunicationRelationTypes.SOCKET_READER;
import static de.esoco.lib.comm.CommunicationRelationTypes.SOCKET_WRITER;
import static de.esoco.lib.comm.CommunicationRelationTypes.TRUST_SELF_SIGNED_CERTIFICATES;

/**
 * An endpoint to an HTTP address.
 *
 * @author eso
 */
public class SocketEndpoint extends Endpoint {

	/**
	 * The URL scheme name for socket connections.
	 */
	public static final String SOCKET_URL_SCHEME = "socket";

	/**
	 * The URL scheme name for (SSL) encrypted socket connections.
	 */
	public static final String ENCRYPTED_SOCKET_URL_SCHEME = "sockets";

	/**
	 * Factory method that creates a new socket request for binary
	 * communication.
	 *
	 * @param defaultCommand  The default command string to send to the
	 *                        endpoint
	 * @param getResponseSize A function that determines the response size
	 *                           to be
	 *                        read from the endpoint socket after sending the
	 *                        command string or NULL if no response needs to be
	 *                        read
	 * @return The command function
	 */
	public static BinaryRequest binaryRequest(byte[] defaultCommand,
		Function<InputStream, Integer> getResponseSize) {
		return new BinaryRequest("BinaryRequest(%s)", defaultCommand,
			getResponseSize);
	}

	/**
	 * Factory method that creates a new socket request for text-based
	 * communication.
	 *
	 * @param defaultCommand  The default command string to send to the
	 *                        endpoint
	 * @param getResponseSize A function that determines the response size
	 *                           to be
	 *                        read from the endpoint socket after sending the
	 *                        command string or NULL if no response needs to be
	 *                        read
	 * @return The command function
	 */
	public static TextRequest textRequest(String defaultCommand,
		Function<Reader, Integer> getResponseSize) {
		return new TextRequest("TextRequest(%s)", defaultCommand,
			getResponseSize);
	}

	/**
	 * Builds a socket endpoint URL from the given parameters.
	 *
	 * @param host      The host name or address
	 * @param port      The port to connect to
	 * @param encrypted TRUE for an encrypted connection
	 * @return The resulting endpoint URL
	 */
	@SuppressWarnings("boxing")
	public static String url(String host, int port, boolean encrypted) {
		String url;

		String scheme =
			encrypted ? ENCRYPTED_SOCKET_URL_SCHEME : SOCKET_URL_SCHEME;

		if (port > 0) {
			url = String.format("%s://%s:%d", scheme, host, port);
		} else {
			url = String.format("%s://%s", scheme, host);
		}

		return url;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection connection) throws IOException {
		Socket socket = connection.get(ENDPOINT_SOCKET);

		if (socket != null) {
			if (!socket.isClosed()) {
				socket.close();
			}

			connection.set(ENDPOINT_SOCKET, null);
		}
	}

	/**
	 * Returns the socket of a connection to a socket endpoint.
	 *
	 * @param connection The connection
	 * @return The endpoint socket
	 */
	protected Socket getSocket(Connection connection) {
		return connection.get(ENDPOINT_SOCKET);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("boxing")
	protected void initConnection(Connection connection) throws IOException {
		URI uri = connection.getUri();
		SocketType socketType =
			hasFlag(ENCRYPTION) ? SocketType.SSL : SocketType.PLAIN;

		if (socketType == SocketType.SSL &&
			connection.hasFlag(TRUST_SELF_SIGNED_CERTIFICATES)) {
			socketType = SocketType.SELF_SIGNED_SSL;
		}

		Socket socket =
			NetUtil.createSocket(uri.getHost(), uri.getPort(), socketType);

		socket.setSoTimeout(connection.get(CONNECTION_TIMEOUT));
		connection.set(ENDPOINT_SOCKET, socket);

		if (socketType != SocketType.PLAIN && !hasRelation(ENCRYPTION)) {
			connection.set(ENCRYPTION);
		}
	}

	/**
	 * A generic base class for socket communication method that sends data to
	 * an endpoint socket and optionally receives a response. Subclasses must
	 * either override and implement the two methods
	 * {@link #writeRequest(Connection, OutputStream, Object)} and
	 * {@link #readResponse(Connection, InputStream)} or perform the complete
	 * request handling in
	 * {@link #sendRequest(Connection, OutputStream, InputStream, Object)}.
	 *
	 * @author eso
	 */
	public static abstract class SocketRequest<I, O>
		extends CommunicationMethod<I, O> {

		/**
		 * Creates a new instance.
		 *
		 * @param methodName     The name of this method
		 * @param defaultRequest defaultCommand The default command string to
		 *                       send
		 */
		protected SocketRequest(String methodName, I defaultRequest) {
			super(methodName, defaultRequest);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public O doOn(Connection connection, I request) throws Exception {
			Socket socket = connection.get(ENDPOINT_SOCKET);
			OutputStream output = socket.getOutputStream();
			InputStream input = socket.getInputStream();

			connection.set(SOCKET_OUTPUT_STREAM, output);
			connection.set(SOCKET_INPUT_STREAM, input);

			return sendRequest(connection, output, input, request);
		}

		/**
		 * Must be implemented by subclasses to read q request response from
		 * the
		 * given input stream of the current socket if not overriding
		 * {@link #sendRequest(Connection, OutputStream, InputStream, Object)}.
		 * The default implementation always returns NULL.
		 *
		 * @param connection  The connection to read the response from
		 * @param inputStream The socket input stream to read from
		 * @return The processed response
		 * @throws Exception Any exception may be thrown to indicate errors
		 */
		protected O readResponse(Connection connection,
			InputStream inputStream)
			throws Exception {
			return null;
		}

		/**
		 * Sends the request through the socket and processes the response. The
		 * standard implementation invokes
		 * {@link #writeRequest(Connection, OutputStream, Object)} and
		 * {@link #readResponse(Connection, InputStream)}. Subclasses may
		 * alternatively override this method, e.g. for more complex requests
		 * that need to perform some kind of handshake.
		 *
		 * @param connection The connection for the current request
		 * @param output     The output stream to send the request through
		 * @param input      The input stream to read the response from
		 * @param data       The request data to send
		 * @return The processed response
		 * @throws Exception Any exception may be thrown to indicate errors
		 */
		protected O sendRequest(Connection connection, OutputStream output,
			InputStream input, I data) throws Exception {
			writeRequest(connection, output, data);
			output.flush();

			return readResponse(connection, input);
		}

		/**
		 * Must be implemented by subclasses to write a request to the given
		 * output stream of the current socket if not overriding
		 * {@link #sendRequest(Connection, OutputStream, InputStream, Object)}.
		 * The implementation doesn't need to flush the output stream, this
		 * will
		 * be handled by the base implementation. But it may be necessary to
		 * flush stream wrappers like instances of {@link Writer}.
		 *
		 * <p>The default implementation does nothing.</p>
		 *
		 * @param connection   The connection to write the request to
		 * @param outputStream The socket output stream to write to
		 * @param request      The request to write
		 * @throws Exception Any exception may be thrown to indicate errors
		 */
		protected void writeRequest(Connection connection,
			OutputStream outputStream, I request) throws Exception {
		}
	}

	/**
	 * A socket communication method implementation that sends binary data
	 * to an
	 * endpoint socket and optionally receives a response.
	 *
	 * @author eso
	 */
	public static class BinaryRequest extends SocketRequest<byte[], byte[]> {

		private final Function<InputStream, Integer> getResponseSize;

		/**
		 * Creates a new instance.
		 *
		 * @param methodName      The name of this method
		 * @param defaultRequest  The default request bytes to send
		 * @param getResponseSize A function that determines the response size
		 *                        to be read from the socket input stream after
		 *                        sending the command string or NULL if no
		 *                        response needs to be read
		 */
		protected BinaryRequest(String methodName, byte[] defaultRequest,
			Function<InputStream, Integer> getResponseSize) {
			super(methodName, defaultRequest);

			this.getResponseSize = getResponseSize;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected byte[] readResponse(Connection connection,
			InputStream inputStream) throws Exception {
			byte[] result = null;

			if (getResponseSize != null) {
				@SuppressWarnings("boxing")
				int responseSize = getResponseSize.evaluate(inputStream);

				result = StreamUtil.readAll(inputStream, 1024, responseSize);
			}

			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void writeRequest(Connection connection,
			OutputStream outputStream, byte[] request) throws Exception {
			outputStream.write(request);
		}
	}

	/**
	 * A socket communication method implementation that sends text data to an
	 * endpoint socket and optionally receives a response.
	 *
	 * @author eso
	 */
	public static class TextRequest extends SocketRequest<String, String> {

		private final Function<Reader, Integer> getResponseSize;

		/**
		 * Creates a new instance.
		 *
		 * @param methodName      The name of this method
		 * @param defaultRequest  The default request string to send
		 * @param getResponseSize A function that determines the response size
		 *                        to be read from the socket input stream after
		 *                        sending the command string or NULL if no
		 *                        response needs to be read
		 */
		public TextRequest(String methodName, String defaultRequest,
			Function<Reader, Integer> getResponseSize) {
			super(methodName, defaultRequest);

			this.getResponseSize = getResponseSize;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("boxing")
		protected String readResponse(Connection connection,
			InputStream inputStream) throws Exception {
			String result = null;

			if (getResponseSize != null) {
				Reader reader = connection.get(SOCKET_READER);

				if (reader == null) {
					reader = new InputStreamReader(inputStream);
					connection.set(SOCKET_READER, reader);
				}

				int responseSize = getResponseSize.evaluate(reader);

				result = StreamUtil.readAll(reader, 1024, responseSize);
			}

			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void writeRequest(Connection connection,
			OutputStream outputStream, String request) throws Exception {
			PrintWriter writer = connection.get(SOCKET_WRITER);

			if (writer == null) {
				writer = new PrintWriter(outputStream);
				connection.set(SOCKET_WRITER, writer);
			}

			writer.println(request);
			writer.flush();
		}
	}
}
