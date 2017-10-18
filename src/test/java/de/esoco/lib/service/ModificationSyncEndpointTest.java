//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-business' project.
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
package de.esoco.lib.service;

import de.esoco.lib.app.Service;
import de.esoco.lib.comm.Endpoint;
import de.esoco.lib.comm.EndpointChain;
import de.esoco.lib.service.ModificationSyncEndpoint;
import de.esoco.lib.service.ModificationSyncEndpoint.SyncData;

import static de.esoco.lib.service.ModificationSyncEndpoint.getCurrentLocks;
import static de.esoco.lib.service.ModificationSyncEndpoint.releaseLock;
import static de.esoco.lib.service.ModificationSyncEndpoint.requestLock;
import static de.esoco.lib.service.ModificationSyncEndpoint.syncRequest;


/********************************************************************
 * Tests the functionality of {@link ModificationSyncEndpoint}.
 *
 * @author eso
 */
public class ModificationSyncEndpointTest
{
	//~ Static fields/initializers ---------------------------------------------

	private static final int TEST_COUNT = 500;

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Main method.
	 *
	 * @param rArgs
	 */
	public static void main(String[] rArgs)
	{
		Endpoint aSyncService = Endpoint.at("http://localhost:7962");

//		Service.SET_LOG_LEVEL.at(aSyncService).send("\"DEBUG\"");

		EndpointChain<SyncData, String> fLock =
			requestLock().from(aSyncService);

		EndpointChain<SyncData, String> fRelease =
			releaseLock().from(aSyncService);

		EndpointChain<SyncData, String> fGetLocks =
			getCurrentLocks().from(aSyncService);

		System.out.printf("RUNNING: %s\n",
						  Service.CHECK_RUNNING.from(aSyncService).result());

		lockEntities("test1", fLock);
		lockEntities("test2", fLock);
//		System.out.printf("LOCKS: %s\n", fGetLocks.result());
		releaseEntities("test1", fRelease);
		releaseEntities("test2", fRelease);

//		System.out.printf("LOCK	  : '%s'\n",
//						  fLock.send(syncRequest("TESTCLIENT",
//												 "test1",
//												 "E-1000")));
//		System.out.printf("LOCK	  : '%s'\n",
//						  fLock.send(syncRequest("TESTCLIENT",
//												 "test2",
//												 "E-1005")));
//		System.out.printf("LOCKS: %s\n", fGetLocks.result());

//		Service.REQUEST_STOP.from(aSyncService).result();
	}

	/***************************************
	 * Test lock entities
	 *
	 * @param sContext
	 * @param fLock
	 */
	private static void lockEntities(
		String							sContext,
		EndpointChain<SyncData, String> fLock)
	{
		for (int i = 0; i < TEST_COUNT; i++)
		{
			String sGlobalId = "E-100" + i;

			System.out.printf("LOCK %s:%s: '%s'\n",
							  sContext,
							  sGlobalId,
							  fLock.send(syncRequest("TESTCLIENT",
													 sContext,
													 sGlobalId)));
		}
	}

	/***************************************
	 * Test release entities
	 *
	 * @param sContext
	 * @param fRelease
	 */
	private static void releaseEntities(
		String							sContext,
		EndpointChain<SyncData, String> fRelease)
	{
		for (int i = TEST_COUNT - 1; i >= 0; i--)
		{
			String sGlobalId = "E-100" + i;

			System.out.printf("RELEASE %s:%s: '%s'\n",
							  sContext,
							  sGlobalId,
							  fRelease.send(syncRequest("TESTCLIENT",
														sContext,
														sGlobalId)));
		}
	}
}
