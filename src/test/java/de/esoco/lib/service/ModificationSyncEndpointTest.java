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
	private static void lockEntities(String sContext,
		EndpointFunction<SyncData, String> fLock) {
		System.out.printf("Locking context %s\n", sContext);

		for (int i = 0; i < TEST_LOOP_COUNT; i++) {
			String sGlobalId = "E-100" + i;
			String sLock =
				fLock.send(syncRequest(TEST_CLIENT, sContext, sGlobalId));

			if (PRINT_INFO) {
				System.out.printf("LOCK %s:%s: '%s'\n", sContext, sGlobalId,
					sLock);
			}
		}
	}

	/**
	 * Main method.
	 */
	@SuppressWarnings("boxing")
	public static void main(String[] rArgs) {
		Endpoint aSyncService = Endpoint.at("http://localhost:7962");

		Service.SET_LOG_LEVEL.on(aSyncService).send("\"WARN\"");

		EndpointFunction<SyncData, String> fLock =
			requestLock().from(aSyncService);

		EndpointFunction<SyncData, String> fRelease =
			releaseLock().from(aSyncService);

		EndpointFunction<SyncData, String> fGetLocks =
			getCurrentLocks().from(aSyncService);

		System.out.printf("RUNNING: %s\n",
			Service.CHECK_RUNNING.from(aSyncService).receive());

		long t = System.currentTimeMillis();

		for (int i = 1; i <= TEST_CONTEXTS; i++) {
			lockEntities("test" + i, fLock);
		}

		System.out.printf("LOCKS: %s\n", fGetLocks.receive());

		for (int i = 1; i <= TEST_CONTEXTS; i++) {
			releaseEntities("test" + i, fRelease);
		}

		t = System.currentTimeMillis() - t;
		System.out.printf("TIME for %d locks: %d.%03d (:= %d locks/s)\n",
			TEST_CONTEXTS * TEST_LOOP_COUNT, t / 1000, t % 1000,
			TEST_CONTEXTS * TEST_LOOP_COUNT * 1000 / t);

//		System.out.printf("LOCK	  : '%s'\n",
//						  fLock.send(syncRequest(TEST_CLIENT,
//												 "test1",
//												 "E-1000")));
//		System.out.printf("LOCK	  : '%s'\n",
//						  fLock.send(syncRequest(TEST_CLIENT,
//												 "test2",
//												 "E-1005")));
//		System.out.printf("LOCKS: %s\n", fGetLocks.result());

//		Service.REQUEST_STOP.from(aSyncService).result();
	}

	/**
	 * Test release entities
	 */
	private static void releaseEntities(String sContext,
		EndpointFunction<SyncData, String> fRelease) {
		System.out.printf("Releasing context %s\n", sContext);

		for (int i = TEST_LOOP_COUNT - 1; i >= 0; i--) {
			String sGlobalId = "E-100" + i;
			String sLock =
				fRelease.send(syncRequest(TEST_CLIENT, sContext, sGlobalId));

			if (PRINT_INFO) {
				System.out.printf("RELEASE %s:%s: '%s'\n", sContext, sGlobalId,
					sLock);
			}
		}
	}
}
