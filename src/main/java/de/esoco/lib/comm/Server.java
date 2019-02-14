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

import de.esoco.lib.io.LimitedInputStream;
import de.esoco.lib.io.LimitedOutputStream;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogLevel;
import de.esoco.lib.manage.Releasable;
import de.esoco.lib.manage.RunCheck;
import de.esoco.lib.manage.Stoppable;
import de.esoco.lib.security.Security;
import de.esoco.lib.security.SecurityRelationTypes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.security.KeyStore;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ServerSocketFactory;

import org.obrel.core.ObjectRelations;
import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationBuilder;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.type.StandardTypes;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTION;
import static de.esoco.lib.comm.CommunicationRelationTypes.LAST_REQUEST;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_CONNECTIONS;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_REQUEST_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_RESPONSE_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.REQUEST_HANDLING_TIME;
import static de.esoco.lib.comm.CommunicationRelationTypes.REQUEST_HISTORY;
import static de.esoco.lib.security.SecurityRelationTypes.CERTIFICATE;
import static de.esoco.lib.security.SecurityRelationTypes.KEY_PASSWORD;

import static org.obrel.core.RelationTypes.newType;
import static org.obrel.type.MetaTypes.IMMUTABLE;
import static org.obrel.type.StandardTypes.IP_ADDRESS;
import static org.obrel.type.StandardTypes.NAME;
import static org.obrel.type.StandardTypes.PORT;
import static org.obrel.type.StandardTypes.TIMER;


/********************************************************************
 * A server that listens on a socket for requests. To create a new instance the
 * constructor expects an instance of {@link RequestHandlerFactory}. This
 * factory must then return new instances of the {@link RequestHandler}
 * interface which will be invoked to perform the client request by analyzing
 * the request data from an input stream and writing the response data to an
 * output stream.
 *
 * <p>A server is started by invoking the {@link #run()} method. A running
 * server can be stopped by invoking {@link #stop()} and it's current state can
 * be queried with {@link #isRunning()}. The configuration of a server is done
 * by settings relations on it before it is started. The only mandatory relation
 * is {@link StandardTypes#PORT} containing the port on which the server will
 * listen. Optional configuration parameters are:</p>
 *
 * <ul>
 *   <li>{@link StandardTypes#NAME}: the name of the server. Will be used as
 *     identifier in log output (default: simple class name).</li>
 *   <li>{@link CommunicationRelationTypes#MAX_REQUEST_SIZE}: the maximum size
 *     of client requests. Requests exceeding this size will be rejected.</li>
 *   <li>{@link CommunicationRelationTypes#MAX_RESPONSE_SIZE}: the maximum size
 *     of a response to a request.</li>
 *   <li>{@link CommunicationRelationTypes#ENCRYPTION}: if set to TRUE the
 *     server will only accept encrypted connections. In that case the following
 *     additional configuration parameters may be set:
 *
 *     <ul>
 *       <li>{@link SecurityRelationTypes#KEY_PASSWORD}: the password for
 *         private keys in any of the key stores mentioned below (default: empty
 *         string).</li>
 *       <li>{@link SecurityRelationTypes#CERTIFICATE}: a Java {@link KeyStore}
 *         containing the server certificate and the corresponding private key,
 *         stored under the alias {@link Security#ALIAS_SERVER_CERT}. If not
 *         provided a certificate will be generated automatically under
 *         consideration of the following optional parameters.</li>
 *       <li>{@link SecurityRelationTypes#SIGNING_CERTIFICATE}: used to sign an
 *         automatically generated certificate (typically some kind of
 *         self-signed certificate authority). If not set a self-signed
 *         certificate will be generated.</li>
 *       <li>{@link StandardTypes#HOST}: the host name of the server. Will be
 *         used as the common name of a generated certificate (default:
 *         'localhost').</li>
 *       <li>{@link SecurityRelationTypes#KEY_SIZE}: the bit size of the private
 *         key for the TLS certificate (default: 2048).</li>
 *       <li>{@link SecurityRelationTypes#CERTIFICATE_VALIDITY}: the validity
 *         period of a generated certificate in days (default: 30).</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author eso
 */
public class Server extends RelatedObject implements RelationBuilder<Server>,
													 Runnable, RunCheck,
													 Stoppable
{
	//~ Static fields/initializers ---------------------------------------------

	/** The request handler factory of this server. */
	public static final RelationType<RequestHandlerFactory> REQUEST_HANDLER_FACTORY =
		newType();

	static
	{
		RelationTypes.init(Server.class);
	}

	//~ Instance fields --------------------------------------------------------

	private ServerSocket aServerSocket;
	private boolean		 bRunning;

	private Lock aServerLock = new ReentrantLock();

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance with a certain type of request handler. The
	 * request handler class must a have a no-argument constructor to allow the
	 * creation of new instances for each request.
	 *
	 * @param rRequestHandlerFactory The class of the request handler to use for
	 *                               client requests
	 */
	public Server(RequestHandlerFactory rRequestHandlerFactory)
	{
		set(REQUEST_HANDLER_FACTORY, rRequestHandlerFactory);
		init(REQUEST_HISTORY);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Checks whether this server is currently running.
	 *
	 * @return TRUE if the server is running, FALSE if it has been stopped (or
	 *         not started yet)
	 */
	@Override
	public final boolean isRunning()
	{
		return bRunning;
	}

	/***************************************
	 * Starts this server instance on the port that is stored in the relation
	 * {@link StandardTypes#PORT}. The server will listen to incoming client
	 * requests and process each in a separate thread. The call to this method
	 * will block while the server is running. To control a running server (e.g.
	 * to stop it through the {@link #stop()} method) an application must start
	 * the server in a separate thread.
	 *
	 * @throws CommunicationException If a communication error occurs
	 */
	@Override
	public void run()
	{
		ObjectRelations.require(this, PORT);

		if (bRunning)
		{
			throw new IllegalStateException(
				getServerName() +
				" already started");
		}

		Log.infof("%s started", getServerName());

		try
		{
			runServerLoop();
		}
		catch (Exception e)
		{
			throw new CommunicationException(e);
		}
	}

	/***************************************
	 * Stops this server after all active client threads have finished. This
	 * method will return immediately after the call, even if client requests
	 * are still processed.
	 */
	@Override
	public void stop()
	{
		if (bRunning)
		{
			bRunning = false;
			Log.infof("%s stopped", getServerName());
		}
	}

	/***************************************
	 * Creates a configuration object for the client requests. The default
	 * implementation returns a new {@link Relatable} object with the copied
	 * relations of this server.
	 *
	 * @return The relatable configuration object
	 */
	protected Relatable createRequestContext()
	{
		Relatable aRequestConfig = new RelatedObject();

		ObjectRelations.copyRelations(this, aRequestConfig, true);
		aRequestConfig.set(IMMUTABLE);

		return aRequestConfig;
	}

	/***************************************
	 * Creates the server socket to listen on when the server is started.
	 *
	 * @param  nPort The port to listen on
	 *
	 * @return The new server port
	 *
	 * @throws IOException If the socket could not be created
	 */
	protected ServerSocket createServerSocket(int nPort) throws IOException
	{
		ServerSocketFactory aServerSocketFactory;

		if (hasFlag(ENCRYPTION))
		{
			KeyStore rCertificate = get(CERTIFICATE);

			if (rCertificate != null)
			{
				aServerSocketFactory =
					Security.getSslContext(
								rCertificate,
								getOption(KEY_PASSWORD).orUse(""))
							.getServerSocketFactory();
			}
			else
			{
				throw new IllegalStateException(
					CERTIFICATE.getSimpleName() +
					" parameter missing to enable SSL");
			}
		}
		else
		{
			aServerSocketFactory = ServerSocketFactory.getDefault();
		}

		return aServerSocketFactory.createServerSocket(nPort);
	}

	/***************************************
	 * Handles a single client request. This method will be run in a separate
	 * thread and the given socket is initialized for communication with the
	 * client.
	 *
	 * @param  rClientSocket The socket for the communication with the client
	 * @param  rContext      A relatable containing context data for the request
	 *
	 * @throws CommunicationException If a communication error occurs
	 */
	@SuppressWarnings("boxing")
	protected void handleClientRequest(Socket    rClientSocket,
									   Relatable rContext)
	{
		RequestHandler rRequestHandler =
			get(REQUEST_HANDLER_FACTORY).getRequestHandler(rContext);

		rRequestHandler.init(TIMER);

		try
		{
			InputStream  rClientIn	    = rClientSocket.getInputStream();
			OutputStream rClientOut     = rClientSocket.getOutputStream();
			InetAddress  rClientAddress = rClientSocket.getInetAddress();

			Log.infof(
				"%s: handling request from %s",
				getServerName(),
				rClientAddress.getHostAddress());

			rRequestHandler.set(IP_ADDRESS, rClientAddress);

			InputStream  rInput  =
				new LimitedInputStream(rClientIn, get(MAX_REQUEST_SIZE));
			OutputStream rOutput =
				new LimitedOutputStream(rClientOut, get(MAX_RESPONSE_SIZE));

			String sRequest = rRequestHandler.handleRequest(rInput, rOutput);

			sRequest = sRequest.replaceAll("(\r\n|\r|\n)", "¶");

			if (Log.isLevelEnabled(LogLevel.DEBUG))
			{
				Log.debugf("Request: %s", sRequest);
			}

			aServerLock.lock();

			try
			{
				set(LAST_REQUEST, sRequest);
				set(
					REQUEST_HANDLING_TIME,
					rRequestHandler.get(TIMER).intValue());

				if (!bRunning)
				{
					aServerSocket.close();
				}
			}
			finally
			{
				aServerLock.unlock();
			}
		}
		catch (Exception e)
		{
			Log.error("Client request handling failed", e);
		}
		finally
		{
			if (rRequestHandler instanceof Releasable)
			{
				((Releasable) rRequestHandler).release();
			}

			try
			{
				rClientSocket.close();
			}
			catch (IOException e)
			{
				Log.error("Socket close failed", e);
			}
		}
	}

	/***************************************
	 * Runs the main server loop that listens for client requests and handles
	 * them with the current request handler.
	 *
	 * @throws IOException If accessing the input or output streams fails
	 */
	@SuppressWarnings("boxing")
	protected void runServerLoop() throws IOException
	{
		Relatable aRequestContext = createRequestContext();

		int nMaxConnections =
			getOption(MAX_CONNECTIONS).orUse(
				Math.max(4, ForkJoinPool.commonPool().getParallelism()));

		Queue<CompletableFuture<Void>> aRequestHandlers =
			new ArrayDeque<>(nMaxConnections);

		aServerSocket = createServerSocket(get(PORT));
		bRunning	  = true;

		while (bRunning)
		{
			try
			{
				Socket rClientSocket = aServerSocket.accept();

				Iterator<CompletableFuture<Void>> rHandlers =
					aRequestHandlers.iterator();

				// remove finished request handlers
				while (rHandlers.hasNext())
				{
					if (rHandlers.next().isDone())
					{
						rHandlers.remove();
					}
				}

				if (aRequestHandlers.size() < nMaxConnections)
				{
					CompletableFuture<Void> aRequestHandler =
						CompletableFuture.runAsync(
							() ->
								handleClientRequest(
									rClientSocket,
									aRequestContext));

					aRequestHandlers.add(aRequestHandler);
				}
				else
				{
					Log.warn(
						"Maximum connections reached, rejecting connection from " +
						rClientSocket.getInetAddress());

					try
					{
						rClientSocket.close();
					}
					catch (IOException e)
					{
						Log.error(
							"Closing rejected connection failed, continuing");
					}
				}
			}
			catch (SocketException e)
			{
				if (bRunning)
				{
					// only throw if still running; if server has been
					// terminated due to a client request the "socket closed"
					// exception can be ignored
					throw e;
				}
			}
		}
	}

	/***************************************
	 * Returns the name of this server instance.
	 *
	 * @return The server name
	 */
	private String getServerName()
	{
		String sName = get(NAME);

		if (sName == null)
		{
			sName = getClass().getSimpleName();
		}

		return sName;
	}

	//~ Inner Interfaces -------------------------------------------------------

	/********************************************************************
	 * Defines the interface that needs to be implemented for server request
	 * handlers. A request handler is a stateful object which means that for
	 * each request a new instance will be created (or at least requested, see
	 * {@link RequestHandlerFactory} for details). How exactly a handler will be
	 * configured depends on the factory which receives an instance of {@link
	 * Relatable} with the server configuration. It may either copy the
	 * relations of the context into the handler or embed them.
	 *
	 * <p>This interface extends {@link Relatable} to allow the server to set
	 * request state information directly on a handler object.</p>
	 *
	 * @author eso
	 */
	public static interface RequestHandler extends Relatable
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Implements the handling of a single server request by reading the
		 * request from an input stream, processing it, and writing an adequate
		 * response to the given output stream. For each request a new instance
		 * is used so it is not necessary
		 *
		 * <p>The implementation doesn't need to perform any kind of resource
		 * management with the given stream parameters. That will be done by the
		 * server implementation.</p>
		 *
		 * @param  rRequest  The request input stream
		 * @param  rResponse The response output stream
		 *
		 * @return A string description of the handled request (used for
		 *         statistical purposes)
		 *
		 * @throws Exception Can throw any exception if handling the request
		 *                   fails
		 */
		public String handleRequest(
			InputStream  rRequest,
			OutputStream rResponse) throws Exception;
	}

	/********************************************************************
	 * A functional interface for the implementation of factories that create
	 * instances of {@link RequestHandler}. A {@link Server} will request a new
	 * handler for each client request it receives. If an implementation wants
	 * to re-use request handlers (e.g. in the case of costly initialization)
	 * the returned handlers can implement the {@link Releasable} interface. In
	 * that case the server will call that method after the request handling has
	 * been completed, even in the case of an error. The implementation is
	 * responsible to reset the handler into a re-usable state for subsequent
	 * invocations, including the handler relations.
	 *
	 * @author eso
	 */
	@FunctionalInterface
	public static interface RequestHandlerFactory
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Returns a request handler instance for the given server. Typically
		 * implementations should return a new request handler instance from
		 * this method.
		 *
		 * @param  rContext A relatable context containing configuration data
		 *                  for the request handler
		 *
		 * @return The request handler for the given configuration
		 */
		public RequestHandler getRequestHandler(Relatable rContext);
	}
}
