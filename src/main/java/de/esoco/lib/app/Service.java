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
package de.esoco.lib.app;

import de.esoco.lib.comm.Server;
import de.esoco.lib.comm.Server.RequestHandler;

import static de.esoco.lib.comm.Server.REQUEST_HANDLER;

import static org.obrel.type.StandardTypes.PORT;


/********************************************************************
 * An application subclass that is the abstract base for the implementation of
 * services that can be controlled via a network connection. The network
 * interface is implemented by means of the {@link Server} class.
 *
 * @author eso
 */
public abstract class Service extends Application
{
	//~ Instance fields --------------------------------------------------------

	private Server aControlServer;
	private Thread aControlServerThread;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 */
	public Service()
	{
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Will be invoked to query the {@link RequestHandler} to be used for the
	 * control server of this service.
	 *
	 * @return The control request handler
	 */
	protected abstract RequestHandler getControlRequestHandler();

	/***************************************
	 * Will be invoked to run the actual service after the application has been
	 * initialized and configured.
	 *
	 * @throws Exception If an error occurs during execution
	 */
	protected abstract void runService() throws Exception;

	/***************************************
	 * Will be invoked to query the control server port. The default
	 * implementation looks for the command line option 'port' and tries to
	 * convert it to a integer value. If this is not possible or the option is
	 * missing an exception will be thrown.
	 *
	 * @return The control server port
	 */
	protected int getControlServerPort()
	{
		Object rPort = getCommandLine().getOption("port");

		if (rPort instanceof Number)
		{
			return ((Number) rPort).intValue();
		}
		else
		{
			throw new IllegalArgumentException("Control server port not set " +
											   "(Option 'port'): " + rPort);
		}
	}

	/***************************************
	 * Overridden to stop the control server.
	 *
	 * @see Application#handleApplicationError(Exception)
	 */
	@Override
	protected void handleApplicationError(Exception e)
	{
		if (aControlServer != null)
		{
			aControlServer.stop();
		}

		super.handleApplicationError(e);
	}

	/***************************************
	 * Overridden to run the service and the associated control server.
	 *
	 * @see Application#runApp()
	 */
	@Override
	protected final void runApp() throws Exception
	{
//		aControlServer = startControlServer();
		runService();
	}

	/***************************************
	 * Creates and starts the control server for this service.
	 *
	 * @return The control server instance
	 *
	 * @throws Exception If starting the control server fails
	 */
	protected Server startControlServer() throws Exception
	{
		Server aServer =
			new Server().with(PORT, getControlServerPort())
						.with(REQUEST_HANDLER, getControlRequestHandler());

		aControlServerThread = new Thread(aServer);

		manageResource(aServer);
		aControlServerThread.start();

		return aServer;
	}

	/***************************************
	 * Overridden to also stop the control server.
	 *
	 * @see Application#stopApp()
	 */
	@Override
	protected void stopApp() throws Exception
	{
		aControlServer.stop();

		super.stopApp();
	}
}
