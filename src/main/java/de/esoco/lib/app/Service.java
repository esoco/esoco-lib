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
package de.esoco.lib.app;

import de.esoco.lib.comm.Server;
import de.esoco.lib.comm.Server.RequestHandler;
import de.esoco.lib.comm.Server.RequestHandlerFactory;
import de.esoco.lib.comm.http.HttpRequestHandler;
import de.esoco.lib.comm.http.ObjectSpaceHttpMethodHandler;
import de.esoco.lib.expression.Functions;

import org.obrel.core.Relatable;
import org.obrel.core.RelationType;
import org.obrel.space.ObjectSpace;
import org.obrel.space.RelatableObjectSpace;

import static org.obrel.core.RelationTypes.newFlagType;
import static org.obrel.core.RelationTypes.newType;
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
	//~ Static fields/initializers ---------------------------------------------

	/** TODO: DOCUMENT ME */
	public static final RelationType<Boolean> RUN = newFlagType();

	/** The {@link ObjectSpace} containing the server status. */
	public static final RelationType<ObjectSpace<Object>> STATUS = newType();

	/** The {@link ObjectSpace} providing access to server control. */
	public static final RelationType<ObjectSpace<Object>> CONTROL = newType();

	//~ Instance fields --------------------------------------------------------

	private Thread			    aControlServerThread;
	private Server			    aControlServer;
	private ObjectSpace<Object> aControlSpace;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 */
	public Service()
	{
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Will be invoked to run the actual service after the application has been
	 * initialized and configured.
	 *
	 * @throws Exception If an error occurs during execution
	 */
	protected abstract void runService() throws Exception;

	/***************************************
	 * Builds the {@link ObjectSpace} for the control server. The control server
	 * uses this to lookup responses for control requests.
	 *
	 * @return The new control object space
	 */
	protected ObjectSpace<Object> buildControlSpace()
	{
		ObjectSpace<Object> aRoot =
			new RelatableObjectSpace<>(Functions.identity());

		aRoot.set(STATUS, new RelatableObjectSpace<>(Functions.identity()));
		aRoot.set(CONTROL, new RelatableObjectSpace<>(Functions.identity()));

		return null;
	}

	/***************************************
	 * Must be implemented to create new instances of {@link RequestHandler} for
	 * the the control server of this service.
	 *
	 * @param  rContext The service context
	 *
	 * @return A new request handler instance
	 */
	protected RequestHandler createRequestHandler(Relatable rContext)
	{
		return new HttpRequestHandler(rContext,
									  new ObjectSpaceHttpMethodHandler(aControlSpace));
	}

	/***************************************
	 * Will be invoked to query the {@link RequestHandlerFactory} to be used for
	 * the control server of this service. The default implementation creates a
	 * factory that invokes {@link #createRequestHandler(Relatable)}.
	 *
	 * @return The control request handler factory
	 */
	protected RequestHandlerFactory getControlRequestHandlerFactory()
	{
		return rContext -> createRequestHandler(rContext);
	}

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
		aControlServer = startControlServer();
		aControlSpace.get(CONTROL).set(RUN);
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
		aControlSpace = buildControlSpace();

		Server aServer =
			new Server(getControlRequestHandlerFactory()).with(PORT,
															   getControlServerPort());

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
