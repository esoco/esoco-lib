//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.lib.thread;

import de.esoco.lib.logging.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/********************************************************************
 * This class provides several static thread management functions. It also
 * maintains a thread pool which application can use to run and manage
 * additional threads.
 *
 * @author eso
 */
public class ThreadManager
{
	//~ Static fields/initializers ---------------------------------------------

	private static ExecutorService aThreadPool =
		Executors.newCachedThreadPool();

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Private, only static use.
	 */
	private ThreadManager()
	{
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Executes a certain Runnable instance in a separate thread.
	 *
	 * @param rRunnable The runnable instance to be executed
	 */
	public static void execute(Runnable rRunnable)
	{
		aThreadPool.execute(rRunnable);
	}

	/***************************************
	 * Performs a shutdown of all currently running threads, interrupting
	 * threads with {@link Thread#interrupt()} if necessary.
	 *
	 * @param nTimeout     The timeout in seconds to wait for threads to
	 *                     terminate
	 * @param bImmediately TRUE to shutdown immediately without waiting for
	 *                     running tasks to finish
	 */
	public static void shutdownAll(int nTimeout, boolean bImmediately)
	{
		shutdownAndAwaitTermination(aThreadPool, nTimeout, bImmediately);
	}

	/***************************************
	 * Performs a shutdown of all running threads in a certain executor service.
	 * Based on the example code in the javadoc of {@link ExecutorService}.
	 *
	 * @param rService     The executor service/thread pool
	 * @param nTimeout     The timeout in seconds to wait for threads to
	 *                     terminate
	 * @param bImmediately TRUE to shutdown immediately without waiting for
	 *                     running tasks to finish
	 */
	public static void shutdownAndAwaitTermination(ExecutorService rService,
												   int			   nTimeout,
												   boolean		   bImmediately)
	{
		long nTime = System.currentTimeMillis() / 1000;

		if (!bImmediately)
		{
			rService.shutdown();
		}

		try
		{
			if (bImmediately ||
				!rService.awaitTermination(nTimeout, TimeUnit.SECONDS))
			{
				rService.shutdownNow();
				nTimeout -= (System.currentTimeMillis() / 1000) - nTime;

				if (nTimeout < 0)
				{
					nTimeout = 0;
				}

				if (!rService.awaitTermination(nTimeout, TimeUnit.SECONDS))
				{
					Log.error("Thread shutdown timeout elapsed");
				}
			}
		}
		catch (InterruptedException e)
		{
			// continue shutdown if interrupted, than restore interrupt status
			rService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
