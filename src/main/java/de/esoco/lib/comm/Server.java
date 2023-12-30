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
import org.obrel.core.ObjectRelations;
import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationBuilder;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.type.StandardTypes;

import javax.net.ServerSocketFactory;
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

/**
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
public class Server extends RelatedObject
	implements RelationBuilder<Server>, Runnable, RunCheck, Stoppable {

	/**
	 * The request handler factory of this server.
	 */
	public static final RelationType<RequestHandlerFactory>
		REQUEST_HANDLER_FACTORY = newType();

	static {
		RelationTypes.init(Server.class);
	}

	private final Lock serverLock = new ReentrantLock();

	private ServerSocket serverSocket;

	private boolean running;

	/**
	 * Creates a new instance with a certain type of request handler. The
	 * request handler class must a have a no-argument constructor to allow the
	 * creation of new instances for each request.
	 *
	 * @param requestHandlerFactory The class of the request handler to use for
	 *                              client requests
	 */
	public Server(RequestHandlerFactory requestHandlerFactory) {
		set(REQUEST_HANDLER_FACTORY, requestHandlerFactory);
		init(REQUEST_HISTORY);
	}

	/**
	 * Checks whether this server is currently running.
	 *
	 * @return TRUE if the server is running, FALSE if it has been stopped (or
	 * not started yet)
	 */
	@Override
	public final boolean isRunning() {
		return running;
	}

	/**
	 * Starts this server instance on the port that is stored in the relation
	 * {@link StandardTypes#PORT}. The server will listen to incoming client
	 * requests and process each in a separate thread. The call to this method
	 * will block while the server is running. To control a running server
	 * (e.g.
	 * to stop it through the {@link #stop()} method) an application must start
	 * the server in a separate thread.
	 *
	 * @throws CommunicationException If a communication error occurs
	 */
	@Override
	public void run() {
		ObjectRelations.require(this, PORT);

		if (running) {
			throw new IllegalStateException(
				getServerName() + " already started");
		}

		Log.infof("%s started", getServerName());

		try {
			runServerLoop();
		} catch (Exception e) {
			throw new CommunicationException(e);
		}
	}

	/**
	 * Stops this server after all active client threads have finished. This
	 * method will return immediately after the call, even if client requests
	 * are still processed.
	 */
	@Override
	public void stop() {
		if (running) {
			running = false;
			Log.infof("%s stopped", getServerName());
		}
	}

	/**
	 * Creates a configuration object for the client requests. The default
	 * implementation returns a new {@link Relatable} object with the copied
	 * relations of this server.
	 *
	 * @return The relatable configuration object
	 */
	protected Relatable createRequestContext() {
		Relatable requestConfig = new RelatedObject();

		ObjectRelations.copyRelations(this, requestConfig, true);
		requestConfig.set(IMMUTABLE);

		return requestConfig;
	}

	/**
	 * Creates the server socket to listen on when the server is started.
	 *
	 * @param port The port to listen on
	 * @return The new server port
	 * @throws IOException If the socket could not be created
	 */
	protected ServerSocket createServerSocket(int port) throws IOException {
		ServerSocketFactory serverSocketFactory;

		if (hasFlag(ENCRYPTION)) {
			KeyStore certificate = get(CERTIFICATE);

			if (certificate != null) {
				serverSocketFactory = Security
					.getSslContext(certificate,
						getOption(KEY_PASSWORD).orUse(""))
					.getServerSocketFactory();
			} else {
				throw new IllegalStateException(CERTIFICATE.getSimpleName() +
					" parameter missing to enable SSL");
			}
		} else {
			serverSocketFactory = ServerSocketFactory.getDefault();
		}

		return serverSocketFactory.createServerSocket(port);
	}

	/**
	 * Handles a single client request. This method will be run in a separate
	 * thread and the given socket is initialized for communication with the
	 * client.
	 *
	 * @param clientSocket The socket for the communication with the client
	 * @param context      A relatable containing context data for the request
	 * @throws CommunicationException If a communication error occurs
	 */
	@SuppressWarnings("boxing")
	protected void handleClientRequest(Socket clientSocket,
		Relatable context) {
		RequestHandler requestHandler =
			get(REQUEST_HANDLER_FACTORY).getRequestHandler(context);

		requestHandler.init(TIMER);

		try {
			InputStream clientIn = clientSocket.getInputStream();
			OutputStream clientOut = clientSocket.getOutputStream();
			InetAddress clientAddress = clientSocket.getInetAddress();

			Log.infof("%s: handling request from %s", getServerName(),
				clientAddress.getHostAddress());

			requestHandler.set(IP_ADDRESS, clientAddress);

			InputStream input =
				new LimitedInputStream(clientIn, get(MAX_REQUEST_SIZE));
			OutputStream output =
				new LimitedOutputStream(clientOut, get(MAX_RESPONSE_SIZE));

			String request = requestHandler.handleRequest(input, output);

			request = request.replaceAll("(\r\n|\r|\n)", "¶");

			if (Log.isLevelEnabled(LogLevel.DEBUG)) {
				Log.debugf("Request: %s", request);
			}

			serverLock.lock();

			try {
				set(LAST_REQUEST, request);
				set(REQUEST_HANDLING_TIME,
					requestHandler.get(TIMER).intValue());

				if (!running) {
					serverSocket.close();
				}
			} finally {
				serverLock.unlock();
			}
		} catch (Exception e) {
			Log.error("Client request handling failed", e);
		} finally {
			if (requestHandler instanceof Releasable) {
				((Releasable) requestHandler).release();
			}

			try {
				clientSocket.close();
			} catch (IOException e) {
				Log.error("Socket close failed", e);
			}
		}
	}

	/**
	 * Runs the main server loop that listens for client requests and handles
	 * them with the current request handler.
	 *
	 * @throws IOException If accessing the input or output streams fails
	 */
	@SuppressWarnings("boxing")
	protected void runServerLoop() throws IOException {
		Relatable requestContext = createRequestContext();

		int maxConnections = getOption(MAX_CONNECTIONS).orUse(
			Math.max(4, ForkJoinPool.commonPool().getParallelism()));

		Queue<CompletableFuture<Void>> requestHandlers =
			new ArrayDeque<>(maxConnections);

		serverSocket = createServerSocket(get(PORT));
		running = true;

		while (running) {
			try {
				Socket clientSocket = serverSocket.accept();

				Iterator<CompletableFuture<Void>> handlers =
					requestHandlers.iterator();

				// remove finished request handlers
				while (handlers.hasNext()) {
					if (handlers.next().isDone()) {
						handlers.remove();
					}
				}

				if (requestHandlers.size() < maxConnections) {
					CompletableFuture<Void> requestHandler =
						CompletableFuture.runAsync(
							() -> handleClientRequest(clientSocket,
								requestContext));

					requestHandlers.add(requestHandler);
				} else {
					Log.warn(
						"Maximum connections reached, rejecting connection " +
							"from " + clientSocket.getInetAddress());

					try {
						clientSocket.close();
					} catch (IOException e) {
						Log.error(
							"Closing rejected connection failed, continuing");
					}
				}
			} catch (SocketException e) {
				if (running) {
					// only throw if still running; if server has been
					// terminated due to a client request the "socket closed"
					// exception can be ignored
					throw e;
				}
			}
		}
	}

	/**
	 * Returns the name of this server instance.
	 *
	 * @return The server name
	 */
	private String getServerName() {
		String name = get(NAME);

		if (name == null) {
			name = getClass().getSimpleName();
		}

		return name;
	}

	/**
	 * Defines the interface that needs to be implemented for server request
	 * handlers. A request handler is a stateful object which means that for
	 * each request a new instance will be created (or at least requested, see
	 * {@link RequestHandlerFactory} for details). How exactly a handler
	 * will be
	 * configured depends on the factory which receives an instance of
	 * {@link Relatable} with the server configuration. It may either copy the
	 * relations of the context into the handler or embed them.
	 *
	 * <p>This interface extends {@link Relatable} to allow the server to set
	 * request state information directly on a handler object.</p>
	 *
	 * @author eso
	 */
	public interface RequestHandler extends Relatable {

		/**
		 * Implements the handling of a single server request by reading the
		 * request from an input stream, processing it, and writing an adequate
		 * response to the given output stream. For each request a new instance
		 * is used so it is not necessary
		 *
		 * <p>The implementation doesn't need to perform any kind of resource
		 * management with the given stream parameters. That will be done by
		 * the
		 * server implementation.</p>
		 *
		 * @param request  The request input stream
		 * @param response The response output stream
		 * @return A string description of the handled request (used for
		 * statistical purposes)
		 * @throws Exception Can throw any exception if handling the request
		 *                   fails
		 */
		String handleRequest(InputStream request, OutputStream response)
			throws Exception;
	}

	/**
	 * A functional interface for the implementation of factories that create
	 * instances of {@link RequestHandler}. A {@link Server} will request a new
	 * handler for each client request it receives. If an implementation wants
	 * to re-use request handlers (e.g. in the case of costly initialization)
	 * the returned handlers can implement the {@link Releasable} interface. In
	 * that case the server will call that method after the request handling
	 * has
	 * been completed, even in the case of an error. The implementation is
	 * responsible to reset the handler into a re-usable state for subsequent
	 * invocations, including the handler relations.
	 *
	 * @author eso
	 */
	@FunctionalInterface
	public interface RequestHandlerFactory {

		/**
		 * Returns a request handler instance for the given server. Typically
		 * implementations should return a new request handler instance from
		 * this method.
		 *
		 * @param context A relatable context containing configuration data for
		 *                the request handler
		 * @return The request handler for the given configuration
		 */
		RequestHandler getRequestHandler(Relatable context);
	}
}
