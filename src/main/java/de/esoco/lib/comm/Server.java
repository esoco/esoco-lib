//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.logging.Log;
import de.esoco.lib.manage.Startable;
import de.esoco.lib.manage.Stoppable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;
import org.obrel.type.StandardTypes;

import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * A simple server class that listens on a socket for requests.
 *
 * @author eso
 */
public class Server extends RelatedObject implements Startable, Stoppable
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * A flag that indicates if this server is running and which can be set to
	 * FALSE to stop a running server.
	 */
	public static final RelationType<Boolean> RUNNING = newType();

	//~ Instance fields --------------------------------------------------------

	private ExecutorService aThreadPool;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 */
	public Server()
	{
	}

	/***************************************
	 * Creates a new instance that listens on a certain port.
	 *
	 * @param nPort The server port
	 */
	@SuppressWarnings("boxing")
	public Server(int nPort)
	{
		set(StandardTypes.PORT, nPort);
	}

	//~ Methods ----------------------------------------------------------------

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
	public void start() throws IOException
	{
		if (aThreadPool != null)
		{
			throw new IllegalStateException("Sever already started");
		}

		@SuppressWarnings("boxing")
		int nPort = get(StandardTypes.PORT);

		aThreadPool = Executors.newCachedThreadPool();

		try (ServerSocket aServerSocket = createServerSocket(nPort))
		{
			while (hasFlag(RUNNING))
			{
				final Socket rClientSocket = aServerSocket.accept();

				aThreadPool.execute(new Runnable()
					{
						@Override
						public void run()
						{
							handleClientRequest(rClientSocket);
						}
					});
			}
		}
	}

	/***************************************
	 * Stops this server after all active client threads have finished. This
	 * method will return immediately after the call, even if client requests
	 * are still processed.
	 */
	@Override
	@SuppressWarnings("boxing")
	public void stop()
	{
		if (aThreadPool == null)
		{
			throw new IllegalStateException("Server not started");
		}

		set(RUNNING, false);
		aThreadPool.shutdown();
		aThreadPool = null;
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

		if (hasFlag(CommunicationRelationTypes.ENCRYPTED_CONNECTION))
		{
			aServerSocketFactory = SSLServerSocketFactory.getDefault();
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
	 *
	 * @throws IOException If a communication error occurs
	 */
	protected void handleClientRequest(Socket rClientSocket)
	{
		try
		{
			BufferedReader rInput =
				new BufferedReader(new InputStreamReader(rClientSocket
														 .getInputStream()));
			PrintWriter    rOut   =
				new PrintWriter(rClientSocket.getOutputStream());

			String sInputLine;

			while ((sInputLine = rInput.readLine()) != null)
			{
				System.out.println(sInputLine);

				if (sInputLine.isEmpty())
				{
					break;
				}
			}

			rOut.write("HTTP/1.0 200 OK\r\n");
			rOut.write("\r\n");
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
}
