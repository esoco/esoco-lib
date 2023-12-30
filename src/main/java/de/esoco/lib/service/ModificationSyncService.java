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
package de.esoco.lib.service;

import de.esoco.lib.app.RestService;
import de.esoco.lib.app.Service;
import de.esoco.lib.comm.CommunicationRelationTypes;
import de.esoco.lib.comm.Server;
import de.esoco.lib.comm.http.HttpRequestHandler;
import de.esoco.lib.comm.http.HttpStatusCode;
import de.esoco.lib.comm.http.HttpStatusException;
import de.esoco.lib.expression.monad.Option;
import de.esoco.lib.json.JsonObject;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogLevel;
import de.esoco.lib.security.AuthenticationService;
import org.obrel.core.Relatable;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.space.ObjectSpace;
import org.obrel.space.RelationSpace;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.obrel.core.RelationTypes.newType;
import static org.obrel.type.StandardTypes.IP_ADDRESS;
import static org.obrel.type.StandardTypes.NAME;

/**
 * A service that implements the monitoring and synchronization of data
 * modifications across multiple applications.
 *
 * @author eso
 */
public class ModificationSyncService extends RestService
	implements AuthenticationService {

	/**
	 * The name of the JSON attribute with the request client.
	 */
	public static final String JSON_REQUEST_CLIENT = "client";

	/**
	 * The name of the JSON attribute with the request context.
	 */
	public static final String JSON_REQUEST_CONTEXT = "context";

	/**
	 * The name of the JSON attribute with the target ID.
	 */
	public static final String JSON_REQUEST_TARGET_ID = "target";

	/**
	 * The name of the JSON attribute with a flag to force a certain request.
	 */
	public static final String JSON_REQUEST_FORCE_FLAG = "force";

	/**
	 * The part of the API providing access to server control.
	 */
	public static final RelationType<ObjectSpace<Object>> SYNC = newType();

	private static final RelationType<JsonObject> CHECK_LOCK = newType();

	private static final RelationType<JsonObject> REQUEST_LOCK = newType();

	private static final RelationType<JsonObject> RELEASE_LOCK = newType();

	private static final RelationType<Map<String, Map<String, LockData>>>
		CURRENT_LOCKS = newType();

	static {
		RelationTypes.init(ModificationSyncService.class);
	}

	private final Map<String, Map<String, LockData>> contextLocks =
		new LinkedHashMap<>();

	/**
	 * Returns the IP address of the client that is performing the current
	 * request.
	 *
	 * @return The client IP address
	 */
	static String getClientAddress() {
		return HttpRequestHandler
			.getThreadLocalRequest()
			.get(IP_ADDRESS)
			.getHostAddress();
	}

	/**
	 * Runs this service.
	 *
	 * @param args The invocation arguments
	 */
	public static void main(String[] args) {
		new ModificationSyncService().run(args);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean authenticate(Relatable authData) {
		return true;
	}

	/**
	 * Overridden to add the sync service endpoints to the REST server object
	 * space.
	 *
	 * @see Service#buildRestServerSpace()
	 */
	@Override
	protected ObjectSpace<Object> buildRestServerSpace() {
		ObjectSpace<Object> rootSpace = super.buildRestServerSpace();

		ObjectSpace<String> apiSpace = rootSpace.get(API);
		ObjectSpace<Object> syncSpace = new RelationSpace<>(true);

		apiSpace.set(SYNC, syncSpace);
		apiSpace.get(STATUS).set(CURRENT_LOCKS, contextLocks);

		syncSpace.set(NAME, getServiceName() + " Sync API");

		syncSpace.init(CHECK_LOCK).onUpdate(this::checkLock);
		syncSpace.init(REQUEST_LOCK).onUpdate(this::requestLock);
		syncSpace.init(RELEASE_LOCK).onUpdate(this::releaseLock);
		syncSpace.set(CURRENT_LOCKS, contextLocks).onUpdate(this::updateLocks);

		return rootSpace;
	}

	/**
	 * @see RestService#createRestServer()
	 */
	@Override
	protected Server createRestServer() {
		Server restServer = super.createRestServer();

		restServer.set(CommunicationRelationTypes.MAX_CONNECTIONS, 20);

		return restServer;
	}

	/**
	 * Tries to acquire a lock for a certain request.
	 *
	 * @param request The lock request
	 */
	private void checkLock(JsonObject request) {
		processSyncRequest(request, this::handleCheckLock);
	}

	/**
	 * Handles a request to check for a lock.
	 *
	 * @param clientId     The requesting client (ignored)
	 * @param context      The lock context
	 * @param targetId     The target ID
	 * @param forceRequest Ignored in this context
	 */
	private void handleCheckLock(String clientId, String context,
		String targetId, boolean forceRequest) {
		boolean hasLock = contextLocks.containsKey(context) &&
			contextLocks.get(context).containsKey(targetId);

		throw new HttpStatusException(HttpStatusCode.OK,
			Boolean.toString(hasLock));
	}

	/**
	 * Handles a request to release an entity lock.
	 *
	 * @param client       The ID of the requesting client
	 * @param context      The lock context
	 * @param targetId     The target ID
	 * @param forceRequest TRUE to force the release even if the lock has been
	 *                     acquired by a different client
	 */
	private void handleReleaseLock(String client, String context,
		String targetId, boolean forceRequest) {
		Map<String, LockData> locks = contextLocks.get(context);
		LockData currentLock = null;

		if (locks != null) {
			currentLock = locks.get(targetId);
		} else {
			respond(HttpStatusCode.NOT_FOUND, "Unknown context " + context);
		}

		if (currentLock != null) {
			boolean lockedByClient = currentLock.isHeldBy(client);

			if (lockedByClient || forceRequest) {
				if (forceRequest && !lockedByClient) {
					Log.warnf("Locked by %s, release forced by %s",
						currentLock.getClientInfo(), client);
				}

				locks.remove(targetId);

				if (Log.isLevelEnabled(LogLevel.DEBUG)) {
					Log.debugf("Current locks: %s", contextLocks);
				}
			} else {
				respond(HttpStatusCode.CONFLICT, client);
			}
		} else {
			respond(HttpStatusCode.NOT_FOUND, context + ":" + targetId);
		}
	}

	/**
	 * Handles a request to set a lock.
	 *
	 * @param client       The ID of the requesting client
	 * @param context      The lock context
	 * @param targetId     The target ID
	 * @param forceRequest TRUE to force the lock even if the same lock has
	 *                     already been acquired by a different client
	 */
	private void handleRequestLock(String client, String context,
		String targetId, boolean forceRequest) {
		Map<String, LockData> locks = contextLocks.get(context);

		if (locks == null) {
			locks = new LinkedHashMap<>();
			contextLocks.put(context, locks);
		}

		LockData currentLock = locks.get(targetId);

		if (currentLock == null || forceRequest) {
			LockData newLock = new LockData(client);

			if (forceRequest && currentLock != null) {
				Log.warnf("Locked by %s, forcing lock to %s",
					currentLock.getClientInfo(), newLock);
			}

			locks.put(targetId, newLock);

			Log.debug("Current locks: " + contextLocks);
		} else if (currentLock.isHeldBy(client)) {
			respond(HttpStatusCode.ALREADY_REPORTED, "");
		} else {
			respond(HttpStatusCode.LOCKED, client);
		}
	}

	/**
	 * The main method to process requests. It delegates the actual request
	 * handling to methods that implement the {@link SyncRequestHandler}
	 * interface.
	 *
	 * @param request        The sync request
	 * @param requestHandler The request handler
	 */
	private void processSyncRequest(JsonObject request,
		SyncRequestHandler requestHandler) {
		try {
			Option<?> clientId = request.getProperty(JSON_REQUEST_CLIENT);
			Option<?> context = request.getProperty(JSON_REQUEST_CONTEXT);
			Option<?> globalId = request.getProperty(JSON_REQUEST_TARGET_ID);
			Option<?> force = request.getProperty(JSON_REQUEST_FORCE_FLAG);

			if (!clientId.is(String.class) || !context.is(String.class) ||
				!globalId.is(String.class) || !force.is(Boolean.class)) {
				respond(HttpStatusCode.BAD_REQUEST, request.toString());
			}

			// orFail() won't occur as the is(...) tests above ensures
			// existence
			requestHandler.handleRequest(
				clientId.map(Object::toString).orFail(),
				context.map(Object::toString).orFail(),
				globalId.map(Object::toString).orFail(),
				force.map(Boolean.class::cast).orFail());
		} catch (HttpStatusException e) {
			// just re-throw as this has already been handled
			throw e;
		} catch (Exception e) {
			respond(HttpStatusCode.BAD_REQUEST, request.toString());
		}
	}

	/**
	 * Releases a lock in a certain context.
	 *
	 * @param request The lock release request
	 */
	private void releaseLock(JsonObject request) {
		processSyncRequest(request, this::handleReleaseLock);
	}

	/**
	 * Tries to acquire a lock on a target in a certain context.
	 *
	 * @param request The lock request
	 */
	private void requestLock(JsonObject request) {
		processSyncRequest(request, this::handleRequestLock);
	}

	/**
	 * Signals a failed request by throwing a {@link HttpStatusException}.
	 *
	 * @param status  The status code for the failure
	 * @param message The failure message
	 * @throws HttpStatusException Always throws this exception with the given
	 *                             parameters
	 */
	private void respond(HttpStatusCode status, String message) {
		throw new HttpStatusException(status, message);
	}

	/**
	 * Invoked upon tries to set all locks by writing to
	 * {@link #CURRENT_LOCKS}.
	 * Currently not supported, therefore throws an exception.
	 *
	 * @param newLocks The new lock mapping
	 */
	private void updateLocks(Map<?, ?> newLocks) {
		respond(HttpStatusCode.METHOD_NOT_ALLOWED,
			"Setting all locks is not supported");
	}

	/**
	 * A functional interface that is used internally to delegate request
	 * handling to methods.
	 *
	 * @author eso
	 */
	@FunctionalInterface
	private interface SyncRequestHandler {

		/**
		 * Handles a synchronization request.
		 *
		 * @param client       An identifier of the client making the request
		 * @param context      The target context of the request
		 * @param targetId     The unique ID of the request target
		 * @param forceRequest TRUE to force the request execution
		 */
		void handleRequest(String client, String context, String targetId,
			boolean forceRequest);
	}

	/**
	 * A data object that contains client informations about a lock.
	 *
	 * @author eso
	 */
	private static class LockData {

		Date lockTime = new Date();

		String clientId;

		String clientAddress;

		/**
		 * Creates a new instance.
		 *
		 * @param clientId The ID received from the client or NULL for none
		 */
		LockData(String clientId) {
			this.clientId = clientId;
			this.clientAddress = getClientAddress();
		}

		/**
		 * Gets a string describing the client that holds the lock.
		 *
		 * @return The client info
		 */
		public String getClientInfo() {
			return String.format("%s[%s]", clientId, clientAddress);
		}

		/**
		 * Checks whether this lock is currently held by the given client .
		 *
		 * @param client The ID of the client to check against this lock
		 * @return TRUE if the given client currently holds the lock
		 */
		public boolean isHeldBy(String client) {
			return clientId.equals(client);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return String.format("%s[%s] (%3$tF %3$tT.%3$tL)", clientId,
				clientAddress, lockTime);
		}
	}
}
