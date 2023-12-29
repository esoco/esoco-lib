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
	 * @param rDefaultCommand  The default command string to send to the
	 *                         endpoint
	 * @param fGetResponseSize A function that determines the response size to
	 *                         be read from the endpoint socket after sending
	 *                         the command string or NULL if no response needs
	 *                         to be read
	 * @return The command function
	 */
	public static BinaryRequest binaryRequest(byte[] rDefaultCommand,
		Function<InputStream, Integer> fGetResponseSize) {
		return new BinaryRequest("BinaryRequest(%s)", rDefaultCommand,
			fGetResponseSize);
	}

	/**
	 * Factory method that creates a new socket request for text-based
	 * communication.
	 *
	 * @param sDefaultCommand  The default command string to send to the
	 *                         endpoint
	 * @param fGetResponseSize A function that determines the response size to
	 *                         be read from the endpoint socket after sending
	 *                         the command string or NULL if no response needs
	 *                         to be read
	 * @return The command function
	 */
	public static TextRequest textRequest(String sDefaultCommand,
		Function<Reader, Integer> fGetResponseSize) {
		return new TextRequest("TextRequest(%s)", sDefaultCommand,
			fGetResponseSize);
	}

	/**
	 * Builds a socket endpoint URL from the given parameters.
	 *
	 * @param sHost      The host name or address
	 * @param nPort      The port to connect to
	 * @param bEncrypted TRUE for an encrypted connection
	 * @return The resulting endpoint URL
	 */
	@SuppressWarnings("boxing")
	public static String url(String sHost, int nPort, boolean bEncrypted) {
		String sUrl;

		String sScheme =
			bEncrypted ? ENCRYPTED_SOCKET_URL_SCHEME : SOCKET_URL_SCHEME;

		if (nPort > 0) {
			sUrl = String.format("%s://%s:%d", sScheme, sHost, nPort);
		} else {
			sUrl = String.format("%s://%s", sScheme, sHost);
		}

		return sUrl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection rConnection) throws IOException {
		Socket rSocket = rConnection.get(ENDPOINT_SOCKET);

		if (rSocket != null) {
			if (!rSocket.isClosed()) {
				rSocket.close();
			}

			rConnection.set(ENDPOINT_SOCKET, null);
		}
	}

	/**
	 * Returns the socket of a connection to a socket endpoint.
	 *
	 * @param rConnection The connection
	 * @return The endpoint socket
	 */
	protected Socket getSocket(Connection rConnection) {
		return rConnection.get(ENDPOINT_SOCKET);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("boxing")
	protected void initConnection(Connection rConnection) throws IOException {
		URI rUri = rConnection.getUri();
		SocketType eSocketType =
			hasFlag(ENCRYPTION) ? SocketType.SSL : SocketType.PLAIN;

		if (eSocketType == SocketType.SSL &&
			rConnection.hasFlag(TRUST_SELF_SIGNED_CERTIFICATES)) {
			eSocketType = SocketType.SELF_SIGNED_SSL;
		}

		Socket aSocket =
			NetUtil.createSocket(rUri.getHost(), rUri.getPort(), eSocketType);

		aSocket.setSoTimeout(rConnection.get(CONNECTION_TIMEOUT));
		rConnection.set(ENDPOINT_SOCKET, aSocket);

		if (eSocketType != SocketType.PLAIN && !hasRelation(ENCRYPTION)) {
			rConnection.set(ENCRYPTION);
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
		 * @param sMethodName     The name of this method
		 * @param rDefaultRequest rDefaultCommand The default command string to
		 *                        send
		 */
		protected SocketRequest(String sMethodName, I rDefaultRequest) {
			super(sMethodName, rDefaultRequest);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public O doOn(Connection rConnection, I rRequest) throws Exception {
			Socket aSocket = rConnection.get(ENDPOINT_SOCKET);
			OutputStream rOutput = aSocket.getOutputStream();
			InputStream rInput = aSocket.getInputStream();

			rConnection.set(SOCKET_OUTPUT_STREAM, rOutput);
			rConnection.set(SOCKET_INPUT_STREAM, rInput);

			return sendRequest(rConnection, rOutput, rInput, rRequest);
		}

		/**
		 * Must be implemented by subclasses to read q request response from
		 * the
		 * given input stream of the current socket if not overriding
		 * {@link #sendRequest(Connection, OutputStream, InputStream, Object)}.
		 * The default implementation always returns NULL.
		 *
		 * @param rConnection  The connection to read the response from
		 * @param rInputStream The socket input stream to read from
		 * @return The processed response
		 * @throws Exception Any exception may be thrown to indicate errors
		 */
		protected O readResponse(Connection rConnection,
			InputStream rInputStream) throws Exception {
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
		 * @param rConnection The connection for the current request
		 * @param rOutput     The output stream to send the request through
		 * @param rInput      The input stream to read the response from
		 * @param rData       The request data to send
		 * @return The processed response
		 * @throws Exception Any exception may be thrown to indicate errors
		 */
		protected O sendRequest(Connection rConnection, OutputStream rOutput,
			InputStream rInput, I rData) throws Exception {
			writeRequest(rConnection, rOutput, rData);
			rOutput.flush();

			return readResponse(rConnection, rInput);
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
		 * @param rConnection   The connection to write the request to
		 * @param rOutputStream The socket output stream to write to
		 * @param rRequest      The request to write
		 * @throws Exception Any exception may be thrown to indicate errors
		 */
		protected void writeRequest(Connection rConnection,
			OutputStream rOutputStream, I rRequest) throws Exception {
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

		private final Function<InputStream, Integer> fGetResponseSize;

		/**
		 * Creates a new instance.
		 *
		 * @param sMethodName      The name of this method
		 * @param rDefaultRequest  The default request bytes to send
		 * @param fGetResponseSize A function that determines the response size
		 *                         to be read from the socket input stream
		 *                         after
		 *                         sending the command string or NULL if no
		 *                         response needs to be read
		 */
		protected BinaryRequest(String sMethodName, byte[] rDefaultRequest,
			Function<InputStream, Integer> fGetResponseSize) {
			super(sMethodName, rDefaultRequest);

			this.fGetResponseSize = fGetResponseSize;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected byte[] readResponse(Connection rConnection,
			InputStream rInputStream) throws Exception {
			byte[] rResult = null;

			if (fGetResponseSize != null) {
				@SuppressWarnings("boxing")
				int nResponseSize = fGetResponseSize.evaluate(rInputStream);

				rResult = StreamUtil.readAll(rInputStream, 1024,
					nResponseSize);
			}

			return rResult;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void writeRequest(Connection rConnection,
			OutputStream rOutputStream, byte[] rRequest) throws Exception {
			rOutputStream.write(rRequest);
		}
	}

	/**
	 * A socket communication method implementation that sends text data to an
	 * endpoint socket and optionally receives a response.
	 *
	 * @author eso
	 */
	public static class TextRequest extends SocketRequest<String, String> {

		private final Function<Reader, Integer> fGetResponseSize;

		/**
		 * Creates a new instance.
		 *
		 * @param sMethodName      The name of this method
		 * @param sDefaultRequest  The default request string to send
		 * @param fGetResponseSize A function that determines the response size
		 *                         to be read from the socket input stream
		 *                         after
		 *                         sending the command string or NULL if no
		 *                         response needs to be read
		 */
		public TextRequest(String sMethodName, String sDefaultRequest,
			Function<Reader, Integer> fGetResponseSize) {
			super(sMethodName, sDefaultRequest);

			this.fGetResponseSize = fGetResponseSize;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("boxing")
		protected String readResponse(Connection rConnection,
			InputStream rInputStream) throws Exception {
			String sResult = null;

			if (fGetResponseSize != null) {
				Reader aReader = rConnection.get(SOCKET_READER);

				if (aReader == null) {
					aReader = new InputStreamReader(rInputStream);
					rConnection.set(SOCKET_READER, aReader);
				}

				int nResponseSize = fGetResponseSize.evaluate(aReader);

				sResult = StreamUtil.readAll(aReader, 1024, nResponseSize);
			}

			return sResult;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void writeRequest(Connection rConnection,
			OutputStream rOutputStream, String sRequest) throws Exception {
			PrintWriter aWriter = rConnection.get(SOCKET_WRITER);

			if (aWriter == null) {
				aWriter = new PrintWriter(rOutputStream);
				rConnection.set(SOCKET_WRITER, aWriter);
			}

			aWriter.println(sRequest);
			aWriter.flush();
		}
	}
}
