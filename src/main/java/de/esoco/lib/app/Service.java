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

import de.esoco.lib.comm.CommunicationMethod;
import de.esoco.lib.comm.HttpEndpoint;
import de.esoco.lib.comm.Server;
import de.esoco.lib.comm.Server.RequestHandler;
import de.esoco.lib.comm.Server.RequestHandlerFactory;
import de.esoco.lib.comm.http.HttpRequestHandler;
import de.esoco.lib.comm.http.HttpRequestHandler.HttpRequestMethodHandler;
import de.esoco.lib.comm.http.HttpStatusCode;
import de.esoco.lib.comm.http.HttpStatusException;
import de.esoco.lib.comm.http.ObjectSpaceHttpMethodHandler;
import de.esoco.lib.json.JsonBuilder.ConvertJson;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogLevel;
import de.esoco.lib.manage.Stoppable;
import de.esoco.lib.security.AuthenticationService;
import de.esoco.lib.security.SecurityRelationTypes;
import de.esoco.lib.text.TextUtil;
import org.obrel.core.Relatable;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.space.HtmlSpace;
import org.obrel.space.MappedSpace;
import org.obrel.space.ObjectSpace;
import org.obrel.space.RelationSpace;
import org.obrel.space.SynchronizedObjectSpace;

import java.util.Date;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTION;
import static de.esoco.lib.security.SecurityRelationTypes.AUTHENTICATION_SERVICE;
import static org.obrel.core.RelationTypes.newFlagType;
import static org.obrel.core.RelationTypes.newType;
import static org.obrel.type.StandardTypes.INFO;
import static org.obrel.type.StandardTypes.NAME;
import static org.obrel.type.StandardTypes.PORT;
import static org.obrel.type.StandardTypes.START_DATE;
import static org.obrel.type.StandardTypes.UPTIME;

/**
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
 * <p>Furthermore the URLs /ping and /healthcheck can be queried for the
 * availability and healthiness of the service. By default both return a boolean
 * with the value TRUE but subclasses may replace the {@link #HEALTHCHECK}
 * relation with their own variant that contains additional JSON data.</p>
 *
 * <p>The REST server of a service by default always runs with TLS encryption.
 * By setting the {@link #OPTION_NO_ENCRYPTION no encryption option} on the
 * command line this can be disabled for testing purposes.</p>
 *
 * @author eso
 */
public abstract class Service extends Application implements Stoppable {

	/**
	 * The command line option to disable TLS encryption for the REST server
	 * (which is active by default).
	 */
	public static final String OPTION_NO_ENCRYPTION = "no-encryption";

	/**
	 * The run flag in the REST server that controls the service execution.
	 */
	public static final RelationType<Boolean> RUN = newFlagType();

	/**
	 * The control space attribute to query and set the log level of a running
	 * service.
	 */
	public static final RelationType<String> LOG_LEVEL = newType();

	/**
	 * The {@link ObjectSpace} containing the server API.
	 */
	public static final RelationType<ObjectSpace<String>> API = newType();

	/**
	 * The part of the API containing the server status.
	 */
	public static final RelationType<ObjectSpace<Object>> STATUS = newType();

	/**
	 * The part of the API providing access to server control.
	 */
	public static final RelationType<ObjectSpace<Object>> CONTROL = newType();

	/**
	 * The {@link HtmlSpace} providing web access to the server API.
	 */
	public static final RelationType<HtmlSpace> WEBAPI = newType();

	/**
	 * Defines the URL endpoint to check whether the service is alive.
	 */
	public static final RelationType<Boolean> PING = newType();

	/**
	 * Defines the URL endpoint to check whether the service is healthy.
	 */
	public static final RelationType<Boolean> HEALTHCHECK = newType();

	/**
	 * A pre-defined communication method that can be invoked on service HTTP
	 * endpoints to check the current run state of a service. The result value
	 * will be the string 'true' if the service is running.
	 */
	public static final CommunicationMethod<Void, String> CHECK_RUNNING =
		CommunicationMethod.doReceive(HttpEndpoint.httpGet("/api/control/run"
		));

	/**
	 * A pre-defined communication method that can be invoked on service HTTP
	 * endpoints to stop the service.
	 */
	public static final CommunicationMethod<String, Void> REQUEST_STOP =
		CommunicationMethod.doSend(
			HttpEndpoint.httpPost("/api/control/run", "false"));

	/**
	 * A pre-defined communication method that can be invoked on service HTTP
	 * endpoints to query the current log level.
	 */
	public static final CommunicationMethod<String, Void> GET_LOG_LEVEL =
		CommunicationMethod.doSend(
			HttpEndpoint.httpGet("/api/control/log_level"));

	/**
	 * A pre-defined communication method that can be invoked on service HTTP
	 * endpoints to set the current log level.
	 */
	public static final CommunicationMethod<String, Void> SET_LOG_LEVEL =
		CommunicationMethod.doSend(
			HttpEndpoint.httpPost("/api/control/log_level", "ERROR"));

	static {
		RelationTypes.init(Service.class);
	}

	private final boolean isRestService;

	private Thread restServerThread;

	private Server restServer;

	private ObjectSpace<Object> restServerSpace;

	private HttpRequestMethodHandler requestMethodHandler;

	/**
	 * Creates a new instance for a service with a #runApp.
	 */
	public Service() {
		this(false);
	}

	/**
	 * Creates a new instance or a sub-class.
	 *
	 * @param isRestService TRUE if this instance should be run as a single
	 *                         REST
	 *                      service implemented by the REST server; FALSE to
	 *                      have separate service functionality in the main
	 *                      thread (by implementing {@link #runService()} and
	 *                      the REST server in a separate thread
	 */
	protected Service(boolean isRestService) {
		this.isRestService = isRestService;
	}

	/**
	 * Builds the object space that provides the REST API of this service.
	 *
	 * @param statusSpace  The status space to be queried through the API
	 * @param controlSpace The control space to be accessed through the API
	 * @return The control object space
	 */
	protected ObjectSpace<Object> buildApiSpace(ObjectSpace<Object> statusSpace,
		ObjectSpace<Object> controlSpace) {
		ObjectSpace<Object> apiSpace = new RelationSpace<>(true);

		apiSpace.set(STATUS, statusSpace);
		apiSpace.set(CONTROL, controlSpace);

		return apiSpace;
	}

	/**
	 * Builds the object space that allows to control this service. The control
	 *
	 * @param serviceName The service name
	 * @return The control object space
	 */
	protected ObjectSpace<Object> buildControlSpace(String serviceName) {
		ObjectSpace<Object> controlSpace = new RelationSpace<>(true);

		controlSpace.set(NAME, serviceName + " Control");
		controlSpace.set(RUN).onChange(run -> stopRequest(null));
		controlSpace
			.set(LOG_LEVEL, Log.getGlobalMinimumLogLevel().name())
			.onChange(this::setLogLevel);

		return controlSpace;
	}

	/**
	 * Builds the {@link ObjectSpace} for the REST server. The REST server uses
	 * this to perform control requests and to lookup status responses.
	 *
	 * @return The new REST object space
	 */
	protected ObjectSpace<Object> buildRestServerSpace() {
		RelationSpace<Object> root = new RelationSpace<>(true);
		String serviceName = getServiceName();

		ObjectSpace<Object> statusSpace = buildStatusSpace(serviceName);
		ObjectSpace<Object> controlSpace = buildControlSpace(serviceName);
		ObjectSpace<Object> apiSpace = buildApiSpace(statusSpace,
			controlSpace);

		// synchronize access from multiple server threads
		controlSpace = new SynchronizedObjectSpace<>(controlSpace);

		root.set(API, new MappedSpace<>(apiSpace, new ConvertApiValue()));
		root.set(WEBAPI, buildWebApiSpace(serviceName, apiSpace));
		root.set(PING);
		root.set(HEALTHCHECK);

		if (statusSpace != null) {
			statusSpace
				.set(START_DATE, new Date())
				.viewAs(INFO, root, this::getServiceInfo);
		}

		return root;
	}

	/**
	 * Builds the object space that provides information about the current
	 * status of this service.
	 *
	 * @param serviceName The service name
	 * @return The status space
	 */
	protected ObjectSpace<Object> buildStatusSpace(String serviceName) {
		ObjectSpace<Object> statusSpace = new RelationSpace<>();

		statusSpace.set(NAME, serviceName + " Status");
		statusSpace.init(UPTIME);

		return statusSpace;
	}

	/**
	 * Builds the object space that provides the web API of this service.
	 *
	 * @param serviceName The service name
	 * @param apiSpace    The API object space to map to the web API
	 * @return The web API object space
	 */
	protected HtmlSpace buildWebApiSpace(String serviceName,
		ObjectSpace<Object> apiSpace) {
		return new HtmlSpace(apiSpace, "webapi").with(NAME, serviceName);
	}

	/**
	 * Must be implemented to create new instances of {@link RequestHandler}
	 * for
	 * the REST server of this service. If the subclass implements the
	 * {@link AuthenticationService} interface it will be set on the request
	 * handler with the {@link SecurityRelationTypes#AUTHENTICATION_SERVICE}
	 * relation type to perform request authentications. If authentication is
	 * required but implemented by a different class the subclass must override
	 * this method to set the above relation on the request handler by itself.
	 *
	 * @param context The service context
	 * @return A new request handler instance
	 */
	protected RequestHandler createRequestHandler(Relatable context) {
		HttpRequestHandler requestHandler =
			new HttpRequestHandler(context, getRequestMethodHandler());

		return requestHandler;
	}

	/**
	 * Creates a new instance of {@link HttpRequestMethodHandler}. See the
	 * method {@link #getRequestMethodHandler()} for more details. The default
	 * implementation returns an instance of
	 * {@link ObjectSpaceHttpMethodHandler}.
	 *
	 * @return The new request method handler
	 */
	protected HttpRequestMethodHandler createRequestMethodHandler() {
		return new ObjectSpaceHttpMethodHandler(restServerSpace, "info");
	}

	/**
	 * Creates a new REST server that returns status information and allows to
	 * control this service.
	 *
	 * @return The new server instance, initialized but not started
	 */
	protected Server createRestServer() {
		RequestHandlerFactory requestHandlerFactory =
			getRestRequestHandlerFactory();

		Server server = new Server(requestHandlerFactory)
			.with(NAME, getServiceName())
			.with(PORT, getRestServerPort());

		if (!getCommandLine().hasOption(OPTION_NO_ENCRYPTION)) {
			server.set(ENCRYPTION);

			if (this instanceof AuthenticationService) {
				server.set(AUTHENTICATION_SERVICE,
					(AuthenticationService) this);
			}
		}

		return server;
	}

	/**
	 * Will be invoked to return the handler for HTTP request methods (like
	 * GET,
	 * PUT, etc.). The default implementation creates only a single handler (by
	 * invoking {@link #createRequestMethodHandler()} which will then be reused
	 * for all requests, expecting the handler to be stateless. If an
	 * application needs stateful method handlers it must override this method
	 * and return a new handler instance on each invocation.
	 *
	 * @return The request method handler
	 */
	protected HttpRequestMethodHandler getRequestMethodHandler() {
		if (requestMethodHandler == null) {
			requestMethodHandler = createRequestMethodHandler();
		}

		return requestMethodHandler;
	}

	/**
	 * Will be invoked to query the {@link RequestHandlerFactory} to be used
	 * for
	 * the REST server of this service. The default implementation creates a
	 * factory that invokes {@link #createRequestHandler(Relatable)}.
	 *
	 * @return The REST request handler factory
	 */
	protected RequestHandlerFactory getRestRequestHandlerFactory() {
		return context -> createRequestHandler(context);
	}

	/**
	 * Will be invoked to query the REST server port. The default
	 * implementation
	 * looks for the command line option 'port' and tries to convert it to a
	 * integer value. If this is not possible or the option is missing an
	 * exception will be thrown.
	 *
	 * @return The REST server port
	 */
	protected int getRestServerPort() {
		Object port = getCommandLine().requireOption("port");

		if (port instanceof Number) {
			return ((Number) port).intValue();
		} else {
			throw new IllegalArgumentException(
				"Invalid REST server port: " + port);
		}
	}

	/**
	 * Returns the REST object space of this service.
	 *
	 * @return The REST object space
	 */
	protected final ObjectSpace<Object> getRestSpace() {
		return restServerSpace;
	}

	/**
	 * Returns a string that contains information about this service.
	 *
	 * @param startDate The start date of the service
	 * @return The service information string
	 */
	protected String getServiceInfo(Date startDate) {
		return String.format(
			"%1$s service, running since %2$tF %2$tT [Uptime: %3$s]",
			getServiceName(), startDate, TextUtil.formatLongDuration(
				System.currentTimeMillis() - startDate.getTime(), false));
	}

	/**
	 * Can be overridden to return a description string for this service
	 * instance. The default implementation returns the class name without
	 * package.
	 *
	 * @return The service description string
	 */
	protected String getServiceName() {
		return getClass().getSimpleName();
	}

	/**
	 * Overridden to stop the REST server.
	 *
	 * @see Application#handleApplicationError(Exception)
	 */
	@Override
	protected void handleApplicationError(Exception e) {
		if (restServer != null) {
			restServer.stop();
		}

		super.handleApplicationError(e);
	}

	/**
	 * Overridden to run the service and the associated REST server.
	 *
	 * @see Application#runApp()
	 */
	@Override
	protected final void runApp() throws Exception {
		restServerSpace = buildRestServerSpace();
		restServer = startRestServer();

		runService();

		if (isRestService) {
			restServerThread.join();
		} else {
			// stop the REST server if this is not a REST service (where the
			// REST server is the actual service)
			restServer.stop();
		}
	}

	/**
	 * Will be invoked to run the actual service after the application has been
	 * initialized and configured. This method needs to be implemented if the
	 * service has it's own functionality to be run on the main thread. If the
	 * service is only using the REST server as a REST service implementation
	 * this method can remain empty.
	 *
	 * @throws Exception If an error occurs during execution
	 */
	protected abstract void runService() throws Exception;

	/**
	 * Sets a status value in the status section of the REST server object
	 * space.
	 *
	 * @param type  The status relation type
	 * @param value The status value
	 */
	protected <T> void setStatus(RelationType<T> type, T value) {
		restServerSpace.get(STATUS).set(type, value);
	}

	/**
	 * Creates and starts the REST server for this service.
	 *
	 * @return The REST server instance
	 * @throws Exception If starting the REST server fails
	 */
	@SuppressWarnings("boxing")
	protected Server startRestServer() throws Exception {
		Server server = createRestServer();

		// this will stop the server on service shutdown
		manageResource(server);

		restServerThread = new Thread(server);
		restServerThread.setUncaughtExceptionHandler((t, e) -> stopRequest(e));
		restServerThread.start();

		Log.infof("%s running, listening on %sport %d", getServiceName(),
			server.get(ENCRYPTION) ? "TLS " : "", getRestServerPort());

		return server;
	}

	/**
	 * Service method to set the log level.
	 *
	 * @param level The new log level
	 */
	private void setLogLevel(String level) {
		try {
			Log.setGlobalMinimumLogLevel(LogLevel.valueOf(level.toUpperCase()));
		} catch (Exception e) {
			throw new HttpStatusException(HttpStatusCode.BAD_REQUEST,
				"Undefined log level: " + level);
		}
	}

	/**
	 * Internal method to handle a request from the REST server to stop the
	 * service.
	 *
	 * @param e An optional exception to indicate a REST server error or NULL
	 *          for a regular request to shutdown this service
	 */
	private void stopRequest(Throwable e) {
		if (e != null) {
			Log.errorf(e, "%s error, shutting down",
				isRestService ? "Server" : "Control server");
		} else {
			Log.infof("Stop requested, shutting down");
		}

		restServer.stop();
		stop();
	}

	/**
	 * An JSON conversion for the service API that prevents the conversion of
	 * {@link ObjectSpace} nodes.
	 *
	 * @author eso
	 */
	protected static class ConvertApiValue extends ConvertJson {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String evaluate(Object value) {
			if (value instanceof ObjectSpace) {
				throw new IllegalArgumentException("Not an API endpoint");
			}

			return super.evaluate(value);
		}
	}
}
