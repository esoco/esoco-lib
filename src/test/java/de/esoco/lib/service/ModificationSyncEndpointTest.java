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

import de.esoco.lib.app.Service;
import de.esoco.lib.comm.Endpoint;
import de.esoco.lib.comm.EndpointFunction;
import de.esoco.lib.service.ModificationSyncEndpoint.SyncData;

import static de.esoco.lib.service.ModificationSyncEndpoint.getCurrentLocks;
import static de.esoco.lib.service.ModificationSyncEndpoint.releaseLock;
import static de.esoco.lib.service.ModificationSyncEndpoint.requestLock;
import static de.esoco.lib.service.ModificationSyncEndpoint.syncRequest;

/**
 * Tests the functionality of {@link ModificationSyncEndpoint}.
 *
 * @author eso
 */
public class ModificationSyncEndpointTest {

	private static final String TEST_CLIENT = "TESTCLIENT";

	private static final int TEST_CONTEXTS = 1;

	private static final int TEST_LOOP_COUNT = 500;

	private static final boolean PRINT_INFO = false;

	/**
	 * Test lock entities
	 */
	private static void lockEntities(String context,
		EndpointFunction<SyncData, String> lock) {
		System.out.printf("Locking context %s\n", context);

		for (int i = 0; i < TEST_LOOP_COUNT; i++) {
			String globalId = "E-100" + i;
			String lockId =
				lock.send(syncRequest(TEST_CLIENT, context, globalId));

			if (PRINT_INFO) {
				System.out.printf("LOCK %s:%s: '%s'\n", context, globalId,
					lockId);
			}
		}
	}

	/**
	 * Main method.
	 */
	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		Endpoint syncService = Endpoint.at("http://localhost:7962");

		Service.SET_LOG_LEVEL.on(syncService).send("\"WARN\"");

		EndpointFunction<SyncData, String> lock =
			requestLock().from(syncService);

		EndpointFunction<SyncData, String> release =
			releaseLock().from(syncService);

		EndpointFunction<SyncData, String> getLocks =
			getCurrentLocks().from(syncService);

		System.out.printf("RUNNING: %s\n",
			Service.CHECK_RUNNING.from(syncService).receive());

		long t = System.currentTimeMillis();

		for (int i = 1; i <= TEST_CONTEXTS; i++) {
			lockEntities("test" + i, lock);
		}

		System.out.printf("LOCKS: %s\n", getLocks.receive());

		for (int i = 1; i <= TEST_CONTEXTS; i++) {
			releaseEntities("test" + i, release);
		}

		t = System.currentTimeMillis() - t;
		System.out.printf("TIME for %d locks: %d.%03d (:= %d locks/s)\n",
			TEST_CONTEXTS * TEST_LOOP_COUNT, t / 1000, t % 1000,
			TEST_CONTEXTS * TEST_LOOP_COUNT * 1000 / t);

//		System.out.printf("LOCK	  : '%s'\n",
//						  lock.send(syncRequest(TEST_CLIENT,
//												 "test1",
//												 "E-1000")));
//		System.out.printf("LOCK	  : '%s'\n",
//						  lock.send(syncRequest(TEST_CLIENT,
//												 "test2",
//												 "E-1005")));
//		System.out.printf("LOCKS: %s\n", getLocks.result());

//		Service.REQUEST_STOP.from(syncService).result();
	}

	/**
	 * Test release entities
	 */
	private static void releaseEntities(String context,
		EndpointFunction<SyncData, String> release) {
		System.out.printf("Releasing context %s\n", context);

		for (int i = TEST_LOOP_COUNT - 1; i >= 0; i--) {
			String globalId = "E-100" + i;
			String lock =
				release.send(syncRequest(TEST_CLIENT, context, globalId));

			if (PRINT_INFO) {
				System.out.printf("RELEASE %s:%s: '%s'\n", context, globalId,
					lock);
			}
		}
	}
}
