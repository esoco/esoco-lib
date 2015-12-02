//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTED_CONNECTION;
import static de.esoco.lib.comm.CommunicationRelationTypes.TRUST_SELF_SIGNED_CERTIFICATES;

import static org.obrel.core.RelationTypeModifier.PRIVATE;
import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * An endpoint to an HTTP address.
 *
 * @author eso
 */
public class SocketEndpoint extends Endpoint
{
	//~ Static fields/initializers ---------------------------------------------

	/** An internal relation type to store a connection socket. */
	private static final RelationType<Socket> ENDPOINT_SOCKET =
		newType(PRIVATE);

	/**
	 * An internal relation type to store a reader for a socket input stream.
	 */
	private static final RelationType<Reader> ENDPOINT_SOCKET_READER =
		newType(PRIVATE);

	/**
	 * An internal relation type to store a writer for a socket input stream.
	 */
	private static final RelationType<PrintWriter> ENDPOINT_SOCKET_WRITER =
		newType(PRIVATE);

	static
	{
		RelationTypes.init(SocketEndpoint.class);
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Factory method that creates a new socket request for binary
	 * communication.
	 *
	 * @param  rDefaultCommand  The default command string to send to the
	 *                          endpoint
	 * @param  fGetResponseSize A function that determines the response size to
	 *                          be read from the endpoint socket after sending
	 *                          the command string or NULL if no response needs
	 *                          to be read
	 *
	 * @return The command function
	 */
	public static BinaryRequest binaryRequest(
		byte[]						   rDefaultCommand,
		Function<InputStream, Integer> fGetResponseSize)
	{
		return new BinaryRequest("BinaryRequest(%s)",
								 rDefaultCommand,
								 fGetResponseSize);
	}

	/***************************************
	 * Factory method that creates a new socket request for text-based
	 * communication.
	 *
	 * @param  sDefaultCommand  The default command string to send to the
	 *                          endpoint
	 * @param  fGetResponseSize A function that determines the response size to
	 *                          be read from the endpoint socket after sending
	 *                          the command string or NULL if no response needs
	 *                          to be read
	 *
	 * @return The command function
	 */
	public static TextRequest textRequest(
		String					  sDefaultCommand,
		Function<Reader, Integer> fGetResponseSize)
	{
		return new TextRequest("TextRequest(%s)",
							   sDefaultCommand,
							   fGetResponseSize);
	}

	/***************************************
	 * Builds a socket endpoint URL from the given parameters.
	 *
	 * @param  sHost      The host name or address
	 * @param  nPort      The port to connect to
	 * @param  bEncrypted TRUE for an encrypted connection
	 *
	 * @return The resulting endpoint URL
	 */
	@SuppressWarnings("boxing")
	public static String url(String sHost, int nPort, boolean bEncrypted)
	{
		return String.format("%s://%s:%d",
							 bEncrypted ? "sockets" : "socket",
							 sHost,
							 nPort);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection rConnection) throws IOException
	{
		Socket rSocket = rConnection.get(ENDPOINT_SOCKET);

		if (rSocket != null && !rSocket.isClosed())
		{
			rSocket.close();
		}
	}

	/***************************************
	 * Returns the endpoint socket of a certain connection.
	 *
	 * @param  rConnection The connection
	 *
	 * @return The endpoint socket
	 */
	protected Socket getEndpointSocket(Connection rConnection)
	{
		return rConnection.get(ENDPOINT_SOCKET);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void initConnection(Connection rConnection) throws IOException
	{
		URI		   rUri		   = rConnection.getUri();
		SocketType eSocketType =
			hasFlag(ENCRYPTED_CONNECTION) ? SocketType.SSL : SocketType.PLAIN;

		if (eSocketType == SocketType.SSL &&
			rConnection.hasFlag(TRUST_SELF_SIGNED_CERTIFICATES))
		{
			eSocketType = SocketType.SELF_SIGNED_SSL;
		}

		Socket aSocket =
			NetUtil.createSocket(rUri.getHost(), rUri.getPort(), eSocketType);

		rConnection.set(ENDPOINT_SOCKET, aSocket);

		if (eSocketType != SocketType.PLAIN &&
			!hasRelation(ENCRYPTED_CONNECTION))
		{
			rConnection.set(ENCRYPTED_CONNECTION);
		}
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A generic base class for socket communication method that sends data to
	 * an endpoint socket and optionally receives a response.
	 *
	 * @author eso
	 */
	public static abstract class SocketRequest<I, O>
		extends CommunicationMethod<I, O>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sMethodName     The name of this method
		 * @param rDefaultRequest rDefaultCommand The default command string to
		 *                        send
		 */
		protected SocketRequest(String sMethodName, I rDefaultRequest)
		{
			super(sMethodName, rDefaultRequest);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public O doOn(Connection rConnection, I rRequest)
		{
			try
			{
				Socket		 aSocket	   = rConnection.get(ENDPOINT_SOCKET);
				OutputStream rOutputStream = aSocket.getOutputStream();
				InputStream  rInputStream  = aSocket.getInputStream();

				writeRequest(rConnection, rOutputStream, rRequest);
				rOutputStream.flush();

				return readResponse(rConnection, rInputStream);
			}
			catch (Exception e)
			{
				throw new CommunicationException(e);
			}
		}

		/***************************************
		 * Must be implemented by subclasses to write a request to the given
		 * output stream of the current socket.
		 *
		 * @param  rConnection  The connection to read the response from
		 * @param  rInputStream The socket input stream to read from
		 *
		 * @return The response value
		 *
		 * @throws Exception Any exception may be thrown to indicate errors
		 */
		protected abstract O readResponse(
			Connection  rConnection,
			InputStream rInputStream) throws Exception;

		/***************************************
		 * Must be implemented by subclasses to write a request to the given
		 * output stream of the current socket. The implementation doesn't need
		 * to flush the output stream, this will be handled by the base
		 * implementation. But it may be necessary to flush stream wrappers like
		 * instances of {@link Writer}.
		 *
		 * @param  rConnection   The connection to write the request to
		 * @param  rOutputStream The socket output stream to write to
		 * @param  rRequest      The request to write
		 *
		 * @throws Exception Any exception may be thrown to indicate errors
		 */
		protected abstract void writeRequest(Connection   rConnection,
											 OutputStream rOutputStream,
											 I			  rRequest)
			throws Exception;
	}

	/********************************************************************
	 * A socket communication method implementation that sends binary data to an
	 * endpoint socket and optionally receives a response.
	 *
	 * @author eso
	 */
	public static class BinaryRequest extends SocketRequest<byte[], byte[]>
	{
		//~ Instance fields ----------------------------------------------------

		private Function<InputStream, Integer> fGetResponseSize;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sMethodName      The name of this method
		 * @param rDefaultRequest  The default request bytes to send
		 * @param fGetResponseSize A function that determines the response size
		 *                         to be read from the socket input stream after
		 *                         sending the command string or NULL if no
		 *                         response needs to be read
		 */
		protected BinaryRequest(String						   sMethodName,
								byte[]						   rDefaultRequest,
								Function<InputStream, Integer> fGetResponseSize)
		{
			super(sMethodName, rDefaultRequest);

			this.fGetResponseSize = fGetResponseSize;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected byte[] readResponse(
			Connection  rConnection,
			InputStream rInputStream) throws Exception
		{
			byte[] rResult = null;

			if (fGetResponseSize != null)
			{
				@SuppressWarnings("boxing")
				int nResponseSize = fGetResponseSize.evaluate(rInputStream);

				rResult = StreamUtil.readAll(rInputStream, 1024, nResponseSize);
			}

			return rResult;
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected void writeRequest(Connection   rConnection,
									OutputStream rOutputStream,
									byte[]		 rRequest) throws Exception
		{
			rOutputStream.write(rRequest);
		}
	}

	/********************************************************************
	 * A socket communication method implementation that sends text data to an
	 * endpoint socket and optionally receives a response.
	 *
	 * @author eso
	 */
	public static class TextRequest extends SocketRequest<String, String>
	{
		//~ Instance fields ----------------------------------------------------

		private Function<Reader, Integer> fGetResponseSize;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sMethodName      The name of this method
		 * @param sDefaultRequest  The default request string to send
		 * @param fGetResponseSize A function that determines the response size
		 *                         to be read from the socket input stream after
		 *                         sending the command string or NULL if no
		 *                         response needs to be read
		 */
		public TextRequest(String					 sMethodName,
						   String					 sDefaultRequest,
						   Function<Reader, Integer> fGetResponseSize)
		{
			super(sMethodName, sDefaultRequest);

			this.fGetResponseSize = fGetResponseSize;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("boxing")
		protected String readResponse(
			Connection  rConnection,
			InputStream rInputStream) throws Exception
		{
			String sResult = null;

			if (fGetResponseSize != null)
			{
				Reader aReader = rConnection.get(ENDPOINT_SOCKET_READER);

				if (aReader == null)
				{
					aReader = new InputStreamReader(rInputStream);
					rConnection.set(ENDPOINT_SOCKET_READER, aReader);
				}

				int nResponseSize = fGetResponseSize.evaluate(aReader);

				sResult = StreamUtil.readAll(aReader, 1024, nResponseSize);
			}

			return sResult;
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected void writeRequest(Connection   rConnection,
									OutputStream rOutputStream,
									String		 sRequest) throws Exception
		{
			PrintWriter aWriter = rConnection.get(ENDPOINT_SOCKET_WRITER);

			if (aWriter == null)
			{
				aWriter = new PrintWriter(rOutputStream);
				rConnection.set(ENDPOINT_SOCKET_WRITER, aWriter);
			}

			aWriter.println(sRequest);
			aWriter.flush();
		}
	}
}
