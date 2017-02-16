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
import de.esoco.lib.comm.http.HttpRequestHandler.HttpRequestMethodHandler;
import de.esoco.lib.comm.http.ObjectSpaceHttpMethodHandler;
import de.esoco.lib.json.JsonBuilder;
import de.esoco.lib.logging.Log;
import de.esoco.lib.manage.Stoppable;
import de.esoco.lib.security.AuthenticationService;
import de.esoco.lib.security.SecurityRelationTypes;
import de.esoco.lib.text.TextUtil;

import java.util.Date;

import org.obrel.core.Relatable;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.space.HtmlSpace;
import org.obrel.space.MappedSpace;
import org.obrel.space.ObjectSpace;
import org.obrel.space.RelationSpace;

import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_CONNECTIONS;
import static de.esoco.lib.comm.CommunicationRelationTypes.REQUEST_HISTORY;
import static de.esoco.lib.security.SecurityRelationTypes.AUTHENTICATION_SERVICE;

import static org.obrel.core.RelationTypes.newFlagType;
import static org.obrel.core.RelationTypes.newType;
import static org.obrel.type.StandardTypes.INFO;
import static org.obrel.type.StandardTypes.NAME;
import static org.obrel.type.StandardTypes.PORT;
import static org.obrel.type.StandardTypes.START_DATE;
import static org.obrel.type.StandardTypes.UPTIME;


/********************************************************************
 * An application subclass that is the abstract base for the implementation of
 * services that can be controlled via a network connection. The network
 * interface is implemented by means of the {@link Server} class.
 *
 * @author eso
 */
public abstract class Service extends Application implements Stoppable
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * The run flag in the control server that controls the service execution.
	 */
	public static final RelationType<Boolean> RUN = newFlagType();

	/** The {@link ObjectSpace} containing the server API. */
	public static final RelationType<ObjectSpace<String>> API = newType();

	/** The part of the API containing the server status. */
	public static final RelationType<ObjectSpace<Object>> STATUS = newType();

	/** The part of the API providing access to server control. */
	public static final RelationType<ObjectSpace<Object>> CONTROL = newType();

	/** The {@link HtmlSpace} providing web access to the server API. */
	public static final RelationType<HtmlSpace> WEBAPI = newType();

	static
	{
		RelationTypes.init(Service.class);
	}

	//~ Instance fields --------------------------------------------------------

	private Thread			    aControlServerThread;
	private Server			    aControlServer;
	private ObjectSpace<Object> aControlSpace;

	private HttpRequestMethodHandler rRequestMethodHandler;

	private RelationSpace<Object> aStatusSpace;

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
	 * uses this to perform control requests and to lookup status responses.
	 *
	 * @return The new control object space
	 */
	protected ObjectSpace<Object> buildControlSpace()
	{
		RelationSpace<Object> aRoot = new RelationSpace<>(true);

		Date aNow = new Date();

		aStatusSpace = new RelationSpace<>();

		ObjectSpace<Object> aApi     = new RelationSpace<>();
		ObjectSpace<Object> aControl = new RelationSpace<>(true);

		aRoot.set(API, new MappedSpace<>(aApi, JsonBuilder.convertJson()));
		aRoot.set(WEBAPI,
				  new HtmlSpace(aApi, "webapi").with(NAME, getServiceName()));
		aApi.set(STATUS, aStatusSpace);
		aApi.set(CONTROL, aControl);

		aControl.set(RUN).onChange(bRun -> stopRequest(null));

		aStatusSpace.init(UPTIME);
		aStatusSpace.set(START_DATE, aNow)
					.viewAs(INFO, aRoot, this::getServiceInfo);

		return aRoot;
	}

	/***************************************
	 * Creates a new REST server that returns status information and allows to
	 * control this service.
	 *
	 * @return The new server instance, initialized but not started
	 */
	protected Server createControlServer()
	{
		RequestHandlerFactory rRequestHandlerFactory =
			getControlRequestHandlerFactory();

		Server aServer =
			new Server(rRequestHandlerFactory).with(NAME, getServiceName())
											  .with(PORT,
													getControlServerPort())
											  .with(MAX_CONNECTIONS, 2);
//											  .with(ENCRYPTION);

		if (this instanceof AuthenticationService)
		{
			aServer.set(AUTHENTICATION_SERVICE, (AuthenticationService) this);
		}

		return aServer;
	}

	/***************************************
	 * Must be implemented to create new instances of {@link RequestHandler} for
	 * the control server of this service. If the subclass implements the {@link
	 * AuthenticationService} interface it will be set on the request handler
	 * with the {@link SecurityRelationTypes#AUTHENTICATION_SERVICE} relation
	 * type to perform request authentications. If authentication is required
	 * but implemented by a different class the subclass must override this
	 * method to set the above relation on the request handler by itself.
	 *
	 * @param  rContext The service context
	 *
	 * @return A new request handler instance
	 */
	protected RequestHandler createRequestHandler(Relatable rContext)
	{
		HttpRequestHandler aRequestHandler =
			new HttpRequestHandler(rContext, getRequestMethodHandler());

		return aRequestHandler;
	}

	/***************************************
	 * Creates a new instance of {@link HttpRequestMethodHandler}. See the
	 * method {@link #getRequestMethodHandler()} for more details. The default
	 * implementation returns an instance of {@link
	 * ObjectSpaceHttpMethodHandler}.
	 *
	 * @return The new request method handler
	 */
	protected HttpRequestMethodHandler createRequestMethodHandler()
	{
		return new ObjectSpaceHttpMethodHandler(aControlSpace, "info");
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
	 * Returns the control object space of this service.
	 *
	 * @return The control object space
	 */
	protected final ObjectSpace<Object> getControlSpace()
	{
		return aControlSpace;
	}

	/***************************************
	 * Will be invoked to return the handler for HTTP request methods (like GET,
	 * PUT, etc.). The default implementation creates only a single handler (by
	 * invoking {@link #createRequestMethodHandler()} which will then be reused
	 * for all requests, expecting the handler to be stateless. If an
	 * application needs stateful method handlers it must override this method
	 * and return a new handler instance on each invocation.
	 *
	 * @return
	 */
	protected HttpRequestMethodHandler getRequestMethodHandler()
	{
		if (rRequestMethodHandler == null)
		{
			rRequestMethodHandler = createRequestMethodHandler();
		}

		return rRequestMethodHandler;
	}

	/***************************************
	 * Returns a string that contains information about this service.
	 *
	 * @param  rStartDate The start date of the service
	 *
	 * @return The service information string
	 */
	protected String getServiceInfo(Date rStartDate)
	{
		return String.format("%1$s service, running since %2$tF %2$tT [Uptime: %3$s]",
							 getServiceName(),
							 rStartDate,
							 TextUtil.formatDuration(System.currentTimeMillis() -
													 rStartDate.getTime(),
													 false));
	}

	/***************************************
	 * Can be overridden to return a description string for this service
	 * instance. The default implementation returns the class name without
	 * package.
	 *
	 * @return The service description string
	 */
	protected String getServiceName()
	{
		return getClass().getSimpleName();
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
	@SuppressWarnings("boxing")
	protected final void runApp() throws Exception
	{
		aControlSpace  = buildControlSpace();
		aControlServer = startControlServer();

		Log.infof("%s running, control server listening on TLS port %d",
				  getServiceName(),
				  getControlServerPort());

		runService();
	}

	/***************************************
	 * Sets a status value in the status section of the control server object
	 * space.
	 *
	 * @param rType  The status relation type
	 * @param rValue The status value
	 */
	protected <T> void setStatus(RelationType<T> rType, T rValue)
	{
		aControlSpace.get(STATUS).set(rType, rValue);
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
		Server aServer = createControlServer();

		aServer.getRelation(REQUEST_HISTORY)
			   .viewAs(REQUEST_HISTORY, aStatusSpace);

		// this will stop the server on service shutdown
		manageResource(aServer);

		aControlServerThread = new Thread(aServer);
		aControlServerThread.setUncaughtExceptionHandler((t, e) ->
														 stopRequest(e));
		aControlServerThread.start();

		return aServer;
	}

	/***************************************
	 * Internal method to handle a request from the control server to stop the
	 * service.
	 *
	 * @param e An optional exception to indicate a control server error or NULL
	 *          for a regular request to shutdown this service
	 */
	private void stopRequest(Throwable e)
	{
		if (e != null)
		{
			Log.error("Control server error, shutting down", e);
		}
		else
		{
			Log.info("Stop requested from control server, shutting down");
		}

		stop();
	}
}
