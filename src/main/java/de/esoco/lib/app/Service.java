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
import org.obrel.space.SynchronizedObjectSpace;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTION;
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
 * services that can be controlled via REST interface over a network connection.
 * The network interface is implemented by means of the {@link Server} class. If
 * the service implements the {@link AuthenticationService} interface it will be
 * used to authenticate all requests to the REST interface and reject all
 * unauthenticated requests.
 *
 * <p>There are two ways to run a service, controlled by the boolean parameter
 * of the constructor. Either as a REST service implemented by the HTTP control
 * server or as an explicit service functionality that must be implemented in
 * the {@link #runService()} method.</p>
 *
 * <p>If used as a REST service or to extend the control server functionality a
 * subclass can modify the {@link ObjectSpace} that provides the REST server
 * functionality in the method {@link #buildRestServerSpace()}. By default the
 * REST server contains several sub-spaces under the following relation
 * types:</p>
 *
 * <ul>
 *   <li>{@link #API}: the server API which maps all values from and to JSON.
 *     This contains the following sub-spaces:
 *
 *     <ul>
 *       <li>{@link #STATUS}: a read-only space that provides informations about
 *         the current service status.</li>
 *       <li>{@link #CONTROL}: a writable space that allows to control the
 *         server at runtime. The most notable element in that space is the
 *         {@link #RUN} flag which can be set to FALSE to stop the service.</li>
 *     </ul>
 *   </li>
 *   <li>{@link #WEBAPI}: a HTML representation of {@link #API}.</li>
 * </ul>
 *
 * <p>The REST server of a service by default always runs with TLS encryption.
 * By setting the {@link #OPTION_NO_ENCRYPTION no encryption option} on the
 * command line this can be disabled for testing purposes.</p>
 *
 * @author eso
 */
public abstract class Service extends Application implements Stoppable
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * The command line option to disable TLS encryption for the REST server
	 * (which is active by default).
	 */
	public static final String OPTION_NO_ENCRYPTION = "no-encryption";

	/** The run flag in the REST server that controls the service execution. */
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

	private boolean bIsRestService;

	private Thread			    aRestServerThread;
	private Server			    aRestServer;
	private ObjectSpace<Object> aRestServerSpace;

	private HttpRequestMethodHandler rRequestMethodHandler;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param bIsRestService TRUE if this instance should be run as a single
	 *                       REST service implemented by the REST server; FALSE
	 *                       to have separate service functionality in the main
	 *                       thread (by implementing {@link #runService()} and
	 *                       the REST server in a separate thread
	 */
	public Service(boolean bIsRestService)
	{
		this.bIsRestService = bIsRestService;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Builds the {@link ObjectSpace} for the REST server. The REST server uses
	 * this to perform control requests and to lookup status responses.
	 *
	 * @return The new REST object space
	 */
	protected ObjectSpace<Object> buildRestServerSpace()
	{
		RelationSpace<Object> aRoot = new RelationSpace<>(true);

		Date aNow = new Date();

		ObjectSpace<Object> aStatusSpace  = new RelationSpace<>();
		ObjectSpace<Object> aApiSpace     = new RelationSpace<>();
		ObjectSpace<Object> aControlSpace = new RelationSpace<>(true);

		aControlSpace = new SynchronizedObjectSpace<>(aControlSpace);

		aRoot.set(API, new MappedSpace<>(aApiSpace, JsonBuilder.convertJson()));
		aRoot.set(WEBAPI,
				  new HtmlSpace(aApiSpace, "webapi").with(NAME,
														  getServiceName()));
		aApiSpace.set(STATUS, aStatusSpace);
		aApiSpace.set(CONTROL, aControlSpace);

		aControlSpace.set(RUN).onChange(bRun -> stopRequest(null));

		aStatusSpace.init(UPTIME);
		aStatusSpace.set(START_DATE, aNow)
					.viewAs(INFO, aRoot, this::getServiceInfo);

		return aRoot;
	}

	/***************************************
	 * Must be implemented to create new instances of {@link RequestHandler} for
	 * the REST server of this service. If the subclass implements the {@link
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
		return new ObjectSpaceHttpMethodHandler(aRestServerSpace, "info");
	}

	/***************************************
	 * Creates a new REST server that returns status information and allows to
	 * control this service.
	 *
	 * @return The new server instance, initialized but not started
	 */
	protected Server createRestServer()
	{
		RequestHandlerFactory rRequestHandlerFactory =
			getRestRequestHandlerFactory();

		Server aServer =
			new Server(rRequestHandlerFactory).with(NAME, getServiceName())
											  .with(PORT, getRestServerPort());

		if (!getCommandLine().hasOption(OPTION_NO_ENCRYPTION))
		{
			aServer.set(ENCRYPTION);
		}

		if (this instanceof AuthenticationService)
		{
			aServer.set(AUTHENTICATION_SERVICE, (AuthenticationService) this);
		}

		return aServer;
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
	 * Will be invoked to query the {@link RequestHandlerFactory} to be used for
	 * the REST server of this service. The default implementation creates a
	 * factory that invokes {@link #createRequestHandler(Relatable)}.
	 *
	 * @return The REST request handler factory
	 */
	protected RequestHandlerFactory getRestRequestHandlerFactory()
	{
		return rContext -> createRequestHandler(rContext);
	}

	/***************************************
	 * Will be invoked to query the REST server port. The default implementation
	 * looks for the command line option 'port' and tries to convert it to a
	 * integer value. If this is not possible or the option is missing an
	 * exception will be thrown.
	 *
	 * @return The REST server port
	 */
	protected int getRestServerPort()
	{
		Object rPort = getCommandLine().getOption("port");

		if (rPort instanceof Number)
		{
			return ((Number) rPort).intValue();
		}
		else
		{
			throw new IllegalArgumentException("REST server port not set " +
											   "(Option 'port'): " + rPort);
		}
	}

	/***************************************
	 * Returns the REST object space of this service.
	 *
	 * @return The REST object space
	 */
	protected final ObjectSpace<Object> getRestSpace()
	{
		return aRestServerSpace;
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
	 * Overridden to stop the REST server.
	 *
	 * @see Application#handleApplicationError(Exception)
	 */
	@Override
	protected void handleApplicationError(Exception e)
	{
		if (aRestServer != null)
		{
			aRestServer.stop();
		}

		super.handleApplicationError(e);
	}

	/***************************************
	 * Overridden to run the service and the associated REST server.
	 *
	 * @see Application#runApp()
	 */
	@Override
	protected final void runApp() throws Exception
	{
		aRestServerSpace = buildRestServerSpace();
		aRestServer		 = startRestServer();

		runService();

		if (!bIsRestService)
		{
			// stop the REST server if this is not a REST service (where the
			// REST server is the actual service)
			aRestServer.stop();
		}
	}

	/***************************************
	 * Will be invoked to run the actual service after the application has been
	 * initialized and configured. This method needs to be implemented if the
	 * service has it's own functionality to be run on the main thread. If the
	 * service is only using the REST server as a REST service implementation
	 * this method can remain empty.
	 *
	 * @throws Exception If an error occurs during execution
	 */
	protected void runService() throws Exception
	{
	}

	/***************************************
	 * Sets a status value in the status section of the REST server object
	 * space.
	 *
	 * @param rType  The status relation type
	 * @param rValue The status value
	 */
	protected <T> void setStatus(RelationType<T> rType, T rValue)
	{
		aRestServerSpace.get(STATUS).set(rType, rValue);
	}

	/***************************************
	 * Creates and starts the REST server for this service.
	 *
	 * @return The REST server instance
	 *
	 * @throws Exception If starting the REST server fails
	 */
	@SuppressWarnings("boxing")
	protected Server startRestServer() throws Exception
	{
		Server aServer = createRestServer();

		// this will stop the server on service shutdown
		manageResource(aServer);

		aRestServerThread = new Thread(aServer);
		aRestServerThread.setUncaughtExceptionHandler((t, e) -> stopRequest(e));
		aRestServerThread.start();

		Log.infof("%s running, listening on %sport %d",
				  getServiceName(),
				  aServer.get(ENCRYPTION) ? "TLS " : "",
				  getRestServerPort());

		return aServer;
	}

	/***************************************
	 * Internal method to handle a request from the REST server to stop the
	 * service.
	 *
	 * @param e An optional exception to indicate a REST server error or NULL
	 *          for a regular request to shutdown this service
	 */
	private void stopRequest(Throwable e)
	{
		if (e != null)
		{
			Log.errorf(e,
					   "%s error, shutting down",
					   bIsRestService ? "Server" : "Control server");
		}
		else
		{
			Log.infof("Stop requested, shutting down");
		}

		stop();
	}
}
