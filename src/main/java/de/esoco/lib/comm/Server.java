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

import de.esoco.lib.io.LimitedInputStream;
import de.esoco.lib.io.LimitedOutputStream;
import de.esoco.lib.logging.Log;
import de.esoco.lib.manage.RunCheck;
import de.esoco.lib.manage.Stoppable;
import de.esoco.lib.security.Security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.nio.file.Files;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ServerSocketFactory;

import org.obrel.core.ObjectRelations;
import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.type.StandardTypes;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTED_CONNECTION;
import static de.esoco.lib.comm.CommunicationRelationTypes.LAST_REQUEST;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_CONNECTIONS;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_REQUEST_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_RESPONSE_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.REQUEST_HANDLING_TIME;
import static de.esoco.lib.security.SecurityRelationTypes.CERTIFICATE_VALIDITY;
import static de.esoco.lib.security.SecurityRelationTypes.COMMON_NAME;
import static de.esoco.lib.security.SecurityRelationTypes.KEY_PASSWORD;
import static de.esoco.lib.security.SecurityRelationTypes.KEY_SIZE;
import static de.esoco.lib.security.SecurityRelationTypes.KEY_STORE;

import static org.obrel.core.RelationTypes.newType;
import static org.obrel.type.MetaTypes.IMMUTABLE;
import static org.obrel.type.StandardTypes.NAME;
import static org.obrel.type.StandardTypes.PORT;
import static org.obrel.type.StandardTypes.TIMER;


/********************************************************************
 * A simple server class that listens on a socket for requests.
 *
 * @author eso
 */
public class Server extends RelatedObject implements Runnable, RunCheck,
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

	private ServerSocket	   aServerSocket;
	private ThreadPoolExecutor aThreadPool;
	private boolean			   bRunning;

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
	 * @throws IOException If a communication error occurs
	 */
	@Override
	public void run()
	{
		if (aThreadPool != null)
		{
			throw new IllegalStateException(getServerName() +
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
		if (aThreadPool != null)
		{
			aThreadPool.shutdown();
			aThreadPool = null;
			bRunning    = false;

			Log.infof("%s stopped", getServerName());
		}
	}

	/***************************************
	 * A builder-style method to set a certain boolean relation.
	 *
	 * @see #with(RelationType, Object)
	 */
	public <T> Server with(RelationType<Boolean> rFlagType)
	{
		return with(rFlagType, Boolean.TRUE);
	}

	/***************************************
	 * A builder-style method to set a certain relation and then return this
	 * instance for concatenation.
	 *
	 * @param  rType  The type of the relation to set
	 * @param  rValue The relation value
	 *
	 * @return This instance for method concatenation
	 */
	public <T> Server with(RelationType<T> rType, T rValue)
	{
		set(rType, rValue);

		return this;
	}

	/***************************************
	 * A variant of {@link #with(RelationType, Object)} that performs the boxing
	 * of integer values.
	 *
	 * @param  rType  The integer relation type to set
	 * @param  nValue The integer value
	 *
	 * @return This instance for method concatenation
	 */
	@SuppressWarnings("boxing")
	public Server with(RelationType<Integer> rType, int nValue)
	{
		return with(rType, (Integer) nValue);
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

		if (hasFlag(ENCRYPTED_CONNECTION))
		{
			RelatedObject aCertParams = new RelatedObject();

			aCertParams.set(NAME, getServerName());
			aCertParams.set(COMMON_NAME, "localhost");
			aCertParams.set(KEY_SIZE, 2048);
			aCertParams.set(KEY_PASSWORD, "");
			aCertParams.set(CERTIFICATE_VALIDITY, 30);

			byte[] aCertData =
				Files.readAllBytes(new File("S:/test/ca/rootCA.pem").toPath());
			byte[] aKeyData  =
				Files.readAllBytes(new File("S:/test/ca/rootCA.pkcs8")
								   .toPath());

			X509Certificate aCACert = Security.decodeCertificate(aCertData);
			PrivateKey	    aCAKey  = Security.decodePrivateKey(aKeyData);

			if (aCACert != null)
			{
				aCertParams.set(KEY_STORE,
								Security.createKeyStore(Security.SIGNING_CERTIFICATE,
														"",
														aCAKey,
														new X509Certificate[]
														{
															aCACert
														}));
			}

			KeyStore rKeyStore = Security.createCertificate(aCertParams);

			aServerSocketFactory =
				Security.getSslContext(rKeyStore, "").getServerSocketFactory();
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
	 * @throws IOException If a communication error occurs
	 */
	@SuppressWarnings("boxing")
	protected void handleClientRequest(Socket    rClientSocket,
									   Relatable rContext)
	{
		Log.infof("%s: handling request from %s",
				  getServerName(),
				  rClientSocket.getInetAddress());

		try
		{
			RequestHandler aRequestHandler =
				get(REQUEST_HANDLER_FACTORY).getRequestHandler(rContext);

			aRequestHandler.init(TIMER);

			InputStream  rInput  =
				new LimitedInputStream(rClientSocket.getInputStream(),
									   get(MAX_REQUEST_SIZE));
			OutputStream rOutput =
				new LimitedOutputStream(rClientSocket.getOutputStream(),
										get(MAX_RESPONSE_SIZE));

			String sRequest = aRequestHandler.handleRequest(rInput, rOutput);

			sRequest = sRequest.replaceAll("(\r\n|\r|\n)", "¶");

			if (aServerLock.tryLock())
			{
				Log.debugf("Request: %s", sRequest);
				set(LAST_REQUEST, sRequest);
				set(REQUEST_HANDLING_TIME, aRequestHandler.get(TIMER));
			}

			if (!bRunning)
			{
				aServerSocket.close();
			}
		}
		catch (Exception e)
		{
			Log.error("Client request handling failed", e);
		}
		finally
		{
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
		aServerSocket = createServerSocket(get(PORT));

		Integer nMaxConnections = get(MAX_CONNECTIONS);

		aThreadPool =
			new ThreadPoolExecutor(0,
								   nMaxConnections,
								   60L,
								   TimeUnit.SECONDS,
								   new ArrayBlockingQueue<>(nMaxConnections *
															10));

		bRunning = true;

		Relatable aRequestContext = createRequestContext();

		while (bRunning)
		{
			try
			{
				Socket rClientSocket = aServerSocket.accept();

				aThreadPool.execute(() ->
									handleClientRequest(rClientSocket,
														aRequestContext));
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
	 * each request a new instance will be created. All request-specific data
	 * will be set into relations of the handler object before it's method
	 * {@link #handleRequest(InputStream, OutputStream)} will be invoked.
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
	 * instances of {@link RequestHandler}.
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
