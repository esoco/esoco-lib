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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.obrel.core.Relatable;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.space.ObjectSpace;
import org.obrel.space.RelationSpace;

import static org.obrel.core.RelationTypes.newType;
import static org.obrel.type.StandardTypes.IP_ADDRESS;
import static org.obrel.type.StandardTypes.NAME;


/********************************************************************
 * A service that implements the monitoring and synchronization of data
 * modifications across multiple applications.
 *
 * @author eso
 */
public class ModificationSyncService extends RestService
	implements AuthenticationService
{
	//~ Static fields/initializers ---------------------------------------------

	/** The name of the JSON attribute with the request client. */
	public static final String JSON_REQUEST_CLIENT = "client";

	/** The name of the JSON attribute with the request context. */
	public static final String JSON_REQUEST_CONTEXT = "context";

	/** The name of the JSON attribute with the target ID. */
	public static final String JSON_REQUEST_TARGET_ID = "target";

	/**
	 * The name of the JSON attribute with a flag to force a certain request.
	 */
	public static final String JSON_REQUEST_FORCE_FLAG = "force";

	/** The part of the API providing access to server control. */
	public static final RelationType<ObjectSpace<Object>> SYNC = newType();

	private static final RelationType<JsonObject> CHECK_LOCK   = newType();
	private static final RelationType<JsonObject> REQUEST_LOCK = newType();
	private static final RelationType<JsonObject> RELEASE_LOCK = newType();

	private static final RelationType<Map<String, Map<String, LockData>>> CURRENT_LOCKS =
		newType();

	static
	{
		RelationTypes.init(ModificationSyncService.class);
	}

	//~ Instance fields --------------------------------------------------------

	private Map<String, Map<String, LockData>> aContextLocks =
		new LinkedHashMap<>();

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Runs this service.
	 *
	 * @param rArgs The invocation arguments
	 */
	public static void main(String[] rArgs)
	{
		new ModificationSyncService().run(rArgs);
	}

	/***************************************
	 * Returns the IP address of the client that is performing the current
	 * request.
	 *
	 * @return The client IP address
	 */
	static String getClientAddress()
	{
		return HttpRequestHandler.getThreadLocalRequest()
								 .get(IP_ADDRESS)
								 .getHostAddress();
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public boolean authenticate(Relatable rAuthData)
	{
		return true;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void stop()
	{
	}

	/***************************************
	 * Overridden to add the sync service endpoints to the REST server object
	 * space.
	 *
	 * @see Service#buildRestServerSpace()
	 */
	@Override
	protected ObjectSpace<Object> buildRestServerSpace()
	{
		ObjectSpace<Object> rRootSpace = super.buildRestServerSpace();

		ObjectSpace<String> rApiSpace  = rRootSpace.get(API);
		ObjectSpace<Object> aSyncSpace = new RelationSpace<>(true);

		rApiSpace.set(SYNC, aSyncSpace);
		rApiSpace.get(STATUS).set(CURRENT_LOCKS, aContextLocks);

		aSyncSpace.set(NAME, getServiceName() + " Sync API");

		aSyncSpace.init(CHECK_LOCK).onUpdate(this::checkLock);
		aSyncSpace.init(REQUEST_LOCK).onUpdate(this::requestLock);
		aSyncSpace.init(RELEASE_LOCK).onUpdate(this::releaseLock);
		aSyncSpace.set(CURRENT_LOCKS, aContextLocks)
				  .onUpdate(this::updateLocks);

		return rRootSpace;
	}

	/***************************************
	 * @see RestService#createRestServer()
	 */
	@Override
	protected Server createRestServer()
	{
		Server rRestServer = super.createRestServer();

		rRestServer.set(CommunicationRelationTypes.MAX_CONNECTIONS, 20);

		return rRestServer;
	}

	/***************************************
	 * Tries to acquire a lock for a certain request.
	 *
	 * @param rRequest The lock request
	 */
	private void checkLock(JsonObject rRequest)
	{
		processSyncRequest(rRequest, this::handleCheckLock);
	}

	/***************************************
	 * Handles a request to check for a lock.
	 *
	 * @param sClientId     The requesting client (ignored)
	 * @param sContext      The lock context
	 * @param sTargetId     The target ID
	 * @param bForceRequest Ignored in this context
	 */
	private void handleCheckLock(String  sClientId,
								 String  sContext,
								 String  sTargetId,
								 boolean bForceRequest)
	{
		boolean bHasLock =
			aContextLocks.containsKey(sContext) &&
			aContextLocks.get(sContext).containsKey(sTargetId);

		throw new HttpStatusException(
			HttpStatusCode.OK,
			Boolean.toString(bHasLock));
	}

	/***************************************
	 * Handles a request to release an entity lock.
	 *
	 * @param sClient       The ID of the requesting client
	 * @param sContext      The lock context
	 * @param sTargetId     The target ID
	 * @param bForceRequest TRUE to force the release even if the lock has been
	 *                      acquired by a different client
	 */
	private void handleReleaseLock(String  sClient,
								   String  sContext,
								   String  sTargetId,
								   boolean bForceRequest)
	{
		Map<String, LockData> aLocks	   = aContextLocks.get(sContext);
		LockData			  rCurrentLock = null;

		if (aLocks != null)
		{
			rCurrentLock = aLocks.get(sTargetId);
		}
		else
		{
			respond(HttpStatusCode.NOT_FOUND, "Unknown context " + sContext);
		}

		if (rCurrentLock != null)
		{
			boolean bLockedByClient = rCurrentLock.isHeldBy(sClient);

			if (bLockedByClient || bForceRequest)
			{
				if (bForceRequest && !bLockedByClient)
				{
					Log.warnf(
						"Locked by %s, release forced by %s",
						rCurrentLock.getClientInfo(),
						sClient);
				}

				aLocks.remove(sTargetId);

				if (Log.isLevelEnabled(LogLevel.DEBUG))
				{
					Log.debugf("Current locks: %s", aContextLocks);
				}
			}
			else
			{
				respond(HttpStatusCode.CONFLICT, sClient);
			}
		}
		else
		{
			respond(HttpStatusCode.NOT_FOUND, sContext + ":" + sTargetId);
		}
	}

	/***************************************
	 * Handles a request to set a lock.
	 *
	 * @param sClient       The ID of the requesting client
	 * @param sContext      The lock context
	 * @param sTargetId     The target ID
	 * @param bForceRequest TRUE to force the lock even if the same lock has
	 *                      already been acquired by a different client
	 */
	private void handleRequestLock(String  sClient,
								   String  sContext,
								   String  sTargetId,
								   boolean bForceRequest)
	{
		Map<String, LockData> aLocks = aContextLocks.get(sContext);

		if (aLocks == null)
		{
			aLocks = new LinkedHashMap<>();
			aContextLocks.put(sContext, aLocks);
		}

		LockData rCurrentLock = aLocks.get(sTargetId);

		if (rCurrentLock == null || bForceRequest)
		{
			LockData aNewLock = new LockData(sClient);

			if (bForceRequest && rCurrentLock != null)
			{
				Log.warnf(
					"Locked by %s, forcing lock to %s",
					rCurrentLock.getClientInfo(),
					aNewLock);
			}

			aLocks.put(sTargetId, aNewLock);

			Log.debug("Current locks: " + aContextLocks);
		}
		else if (rCurrentLock.isHeldBy(sClient))
		{
			respond(HttpStatusCode.ALREADY_REPORTED, "");
		}
		else
		{
			respond(HttpStatusCode.LOCKED, sClient);
		}
	}

	/***************************************
	 * The main method to process requests. It delegates the actual request
	 * handling to methods that implement the {@link SyncRequestHandler}
	 * interface.
	 *
	 * @param rRequest        The sync request
	 * @param rRequestHandler The request handler
	 */
	private void processSyncRequest(
		JsonObject		   rRequest,
		SyncRequestHandler rRequestHandler)
	{
		try
		{
			Option<?> oClientId = rRequest.getProperty(JSON_REQUEST_CLIENT);
			Option<?> oContext  = rRequest.getProperty(JSON_REQUEST_CONTEXT);
			Option<?> oGlobalId = rRequest.getProperty(JSON_REQUEST_TARGET_ID);
			Option<?> oForce    = rRequest.getProperty(JSON_REQUEST_FORCE_FLAG);

			if (!oClientId.is(String.class) ||
				!oContext.is(String.class) ||
				!oGlobalId.is(String.class) ||
				!oForce.is(Boolean.class))
			{
				respond(HttpStatusCode.BAD_REQUEST, rRequest.toString());
			}

			// orFail() won't occur as the is(...) tests above ensures existence
			rRequestHandler.handleRequest(
				oClientId.map(Object::toString).orFail(),
				oContext.map(Object::toString).orFail(),
				oGlobalId.map(Object::toString).orFail(),
				oForce.map(Boolean.class::cast).orFail());
		}
		catch (HttpStatusException e)
		{
			// just re-throw as this has already been handled
			throw e;
		}
		catch (Exception e)
		{
			respond(HttpStatusCode.BAD_REQUEST, rRequest.toString());
		}
	}

	/***************************************
	 * Releases a lock in a certain context.
	 *
	 * @param rRequest The lock release request
	 */
	private void releaseLock(JsonObject rRequest)
	{
		processSyncRequest(rRequest, this::handleReleaseLock);
	}

	/***************************************
	 * Tries to acquire a lock on a target in a certain context.
	 *
	 * @param rRequest The lock request
	 */
	private void requestLock(JsonObject rRequest)
	{
		processSyncRequest(rRequest, this::handleRequestLock);
	}

	/***************************************
	 * Signals a failed request by throwing a {@link HttpStatusException}.
	 *
	 * @param  eStatus  The status code for the failure
	 * @param  sMessage The failure message
	 *
	 * @throws HttpStatusException Always throws this exception with the given
	 *                             parameters
	 */
	private void respond(HttpStatusCode eStatus, String sMessage)
	{
		throw new HttpStatusException(eStatus, sMessage);
	}

	/***************************************
	 * Invoked upon tries to set all locks by writing to {@link #CURRENT_LOCKS}.
	 * Currently not supported, therefore throws an exception.
	 *
	 * @param aNewLocks The new lock mapping
	 */
	private void updateLocks(Map<?, ?> aNewLocks)
	{
		respond(
			HttpStatusCode.METHOD_NOT_ALLOWED,
			"Setting all locks is not supported");
	}

	//~ Inner Interfaces -------------------------------------------------------

	/********************************************************************
	 * A functional interface that is used internally to delegate request
	 * handling to methods.
	 *
	 * @author eso
	 */
	@FunctionalInterface
	private static interface SyncRequestHandler
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Handles a synchronization request.
		 *
		 * @param sClient       An identifier of the client making the request
		 * @param sContext      The target context of the request
		 * @param sTargetId     The unique ID of the request target
		 * @param bForceRequest TRUE to force the request execution
		 */
		public void handleRequest(String  sClient,
								  String  sContext,
								  String  sTargetId,
								  boolean bForceRequest);
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A data object that contains client informations about a lock.
	 *
	 * @author eso
	 */
	private static class LockData
	{
		//~ Instance fields ----------------------------------------------------

		Date   aLockTime	  = new Date();
		String sClientId;
		String sClientAddress;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sClientId The ID received from the client or NULL for none
		 */
		LockData(String sClientId)
		{
			this.sClientId	    = sClientId;
			this.sClientAddress = getClientAddress();
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Gets a string describing the client that holds the lock.
		 *
		 * @return The client info
		 */
		public String getClientInfo()
		{
			return String.format("%s[%s]", sClientId, sClientAddress);
		}

		/***************************************
		 * Checks whether this lock is currently held by the given client .
		 *
		 * @param  sClient The ID of the client to check against this lock
		 *
		 * @return TRUE if the given client currently holds the lock
		 */
		public boolean isHeldBy(String sClient)
		{
			return sClientId.equals(sClient);
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public String toString()
		{
			return String.format(
				"%s[%s] (%3$tF %3$tT.%3$tL)",
				sClientId,
				sClientAddress,
				aLockTime);
		}
	}
}
