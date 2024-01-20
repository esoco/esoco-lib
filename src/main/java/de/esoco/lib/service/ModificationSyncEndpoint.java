//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.comm.Endpoint;
import de.esoco.lib.comm.HttpEndpoint;
import de.esoco.lib.comm.http.HttpRequestMethod;
import de.esoco.lib.comm.http.HttpStatusCode;
import de.esoco.lib.json.JsonBuilder;

import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

import static de.esoco.lib.expression.Functions.identity;
import static de.esoco.lib.service.ModificationSyncService.JSON_REQUEST_CLIENT;
import static de.esoco.lib.service.ModificationSyncService.JSON_REQUEST_CONTEXT;
import static de.esoco.lib.service.ModificationSyncService.JSON_REQUEST_FORCE_FLAG;
import static de.esoco.lib.service.ModificationSyncService.JSON_REQUEST_TARGET_ID;

/**
 * The HTTP endpoint for interaction with the {@link ModificationSyncService}
 * REST service. The endpoint itself uses the standard HTTP endpoint
 * implementation. An instance can be created by invoking the standard method
 * {@link Endpoint#at(String)} with the sync service URL. This class only serves
 * as a holder for the static REST method definitions that are specific for the
 * sync service.
 *
 * @author eso
 */
public class ModificationSyncEndpoint extends HttpEndpoint {

	/**
	 * Static helper method that creates the data record for a forced
	 * synchronization request. This should only be used by management code for
	 * the handling of invalid locks.
	 *
	 * @param client   A unique identifier of the client making the request
	 * @param context  The name of the synchronization context
	 * @param targetId The unique ID of the target to synchronize
	 * @return The data record for use with {@link SyncRequest}
	 */
	public static SyncData forceSyncRequest(String client, String context,
		String targetId) {
		return new SyncData(client, context, targetId, true);
	}

	/**
	 * Returns a request method that will query the current locks. The
	 * result is
	 * a map that contains entries for the lock contexts. Each context entry
	 * contains another mapping from target IDs to the addresses of the clients
	 * that hold the locks.
	 *
	 * @return The request method
	 */
	public static SyncRequest getCurrentLocks() {
		return new SyncRequest(HttpRequestMethod.GET, "current_locks");
	}

	/**
	 * Returns a request method that will release a lock on an certain target.
	 *
	 * @return The request method
	 */
	public static SyncRequest releaseLock() {
		return new SyncRequest(HttpRequestMethod.POST, "release_lock");
	}

	/**
	 * Returns a request method that will lock a certain target.
	 *
	 * @return The request method
	 */
	public static SyncRequest requestLock() {
		return new SyncRequest(HttpRequestMethod.POST, "request_lock");
	}

	/**
	 * Static helper method that creates the data record for a synchronization
	 * request.
	 *
	 * @param client   A unique identifier of the client making the request
	 * @param context  The name of the synchronization context
	 * @param targetId The unique ID of the target to synchronize
	 * @return The data record for use with {@link SyncRequest}
	 */
	public static SyncData syncRequest(String client, String context,
		String targetId) {
		return new SyncData(client, context, targetId, false);
	}

	/**
	 * A simple data record that contains the data needed for a synchronization
	 * request and a method to convert it into JSON.
	 *
	 * @author eso
	 */
	public static class SyncData {

		private final String client;

		private final String context;

		private final String targetId;

		private final boolean forceRequest;

		/**
		 * Creates a new instance.
		 *
		 * @param client       A unique identifier of the client making the
		 *                     request
		 * @param context      The synchronization context
		 * @param targetId     The unique ID of the target
		 * @param forceRequest TRUE to force the request execution even if the
		 *                     requirements are not met
		 */
		public SyncData(String client, String context, String targetId,
			boolean forceRequest) {
			this.client = client;
			this.context = context;
			this.targetId = targetId;
			this.forceRequest = forceRequest;
		}

		/**
		 * Formats this data record into a JSON request string as required by
		 * the modification sync service.
		 *
		 * @return The JSON request
		 */
		public String toJson() {
			Map<String, Object> requestData = new LinkedHashMap<>(3);

			requestData.put(JSON_REQUEST_CLIENT, client);
			requestData.put(JSON_REQUEST_CONTEXT, context);
			requestData.put(JSON_REQUEST_TARGET_ID, targetId);

			if (forceRequest) {
				requestData.put(JSON_REQUEST_FORCE_FLAG, Boolean.TRUE);
			}

			return new JsonBuilder().appendObject(requestData).toString();
		}
	}

	/**
	 * The base class for request methods to the
	 * {@link ModificationSyncEndpoint}.
	 *
	 * @author eso
	 */
	public static class SyncRequest extends HttpRequest<SyncData, String> {

		/**
		 * Creates a new instance.
		 *
		 * @param method     The request method
		 * @param requestUrl methodName The name of the request method
		 */
		SyncRequest(HttpRequestMethod method, String requestUrl) {
			super(requestUrl, null, method, "/api/sync/" + requestUrl,
				data -> data != null ? data.toJson() : "", identity());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected String handleHttpError(HttpURLConnection urlConnection,
			Exception httpException, HttpStatusCode statusCode) {
			return statusCode.toString();
		}
	}
}
