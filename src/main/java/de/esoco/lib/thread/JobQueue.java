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
package de.esoco.lib.thread;

import de.esoco.lib.expression.Functions;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


/********************************************************************
 * A class that runs queued jobs (implemented in {@link Runnable} instances) in
 * one or more separate threads. The queue can be {@link #pause() paused} and
 * {@link #resume() resumed} to avoid concurrent processing of non-synchronized
 * resources.
 *
 * @author eso
 */
public class JobQueue
{
	//~ Instance fields --------------------------------------------------------

	private BlockingQueue<Runnable> aJobQueue;
	private ExecutorService		    aJobService;
	private CountDownLatch		    aPauseSignal;

	private int     nMaxJobs;
	private boolean bRun = false;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param nQueueSize       The maximum number of queued jobs
	 * @param nMaxParallelJobs The maximum number of jobs to be executed in
	 *                         parallel
	 */
	public JobQueue(int nQueueSize, int nMaxParallelJobs)
	{
		if (nMaxParallelJobs < 1)
		{
			throw new IllegalArgumentException("Maximum parallel jobs must be >= 1");
		}

		nMaxJobs  = nMaxParallelJobs;
		aJobQueue = new LinkedBlockingQueue<>(nQueueSize);

		resume();
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Adds a job to this queue. Will block until the next job is completed if
	 * the maximum queue size has been reached.
	 *
	 * @param rJob The runnable of the job to execute
	 */
	public void add(Runnable rJob)
	{
		try
		{
			aJobQueue.put(rJob);
		}
		catch (InterruptedException e)
		{
			// forward to invoking thread
			Thread.currentThread().interrupt();
		}
	}

	/***************************************
	 * Returns the current number of jobs that are waiting for processing. This
	 * does not include jobs that are currently processing.
	 *
	 * @return The number of jobs waiting in the queue
	 */
	public int getSize()
	{
		return aJobQueue.size();
	}

	/***************************************
	 * Checks whether the queue is currently empty. This does not take into
	 * account jobs that are currently processing.
	 *
	 * @return The number of jobs waiting in the queue
	 */
	public boolean isEmpty()
	{
		return aJobQueue.isEmpty();
	}

	/***************************************
	 * Pauses the execution of jobs until it is {@link #resume() resumed}. This
	 * method blocks until all job threads have been paused. The job execution
	 * can be started again by invoking {@link #resume()}.
	 *
	 * <p>This method is blocking because it is intended to be used to suspend
	 * job execution while an application needs to process data that would
	 * otherwise be modified concurrently by queued jobs. To shutdown the job
	 * processing without waiting the method {@link #stop()} can be used.</p>
	 */
	public void pause()
	{
		if (bRun)
		{
			bRun = false;

			for (int i = 0; i < aPauseSignal.getCount(); i++)
			{
				// offer empty jobs so that the runners awake and detect the pause
				aJobQueue.offer(Functions.NO_OPERATION);
			}

			try
			{
				aPauseSignal.await();
			}
			catch (InterruptedException e)
			{
				// forward to invoking thread
				Thread.currentThread().interrupt();
			}
		}
	}

	/***************************************
	 * Resumes the processing of jobs after it has been {@link #pause() paused}
	 * or {@link #stop() stopped}.
	 */
	public void resume()
	{
		if (!bRun)
		{
			bRun		 = true;
			aPauseSignal = new CountDownLatch(nMaxJobs);

			if (aJobService == null)
			{
				aJobService = Executors.newFixedThreadPool(nMaxJobs);
			}

			for (int i = 0; i < nMaxJobs; i++)
			{
				aJobService.submit(new JobRunner(aPauseSignal));
			}
		}
	}

	/***************************************
	 * Stops the processing of jobs, interrupting currently executing jobs if
	 * necessary. The job execution can be started again by invoking {@link
	 * #resume()}.
	 *
	 * <p>This method will return immediately. If an application needs to wait
	 * for the orderly shutdown of all jobs (without interruption) it should
	 * invoke the blocking method {@link #pause()} first.</p>
	 */
	public void stop()
	{
		if (aJobService != null)
		{
			bRun = false;
			aJobQueue.clear();
			aJobService.shutdownNow();
			aJobService  = null;
			aPauseSignal = null;
		}
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * Runs jobs from the queue until processing is stopped or interrupted.
	 *
	 * @author eso
	 */
	private class JobRunner implements Runnable
	{
		//~ Instance fields ----------------------------------------------------

		private CountDownLatch rPauseSignal;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rPauseSignal Used to signal termination of this runner
		 */
		public JobRunner(CountDownLatch rPauseSignal)
		{
			this.rPauseSignal = rPauseSignal;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Runs jobs from the queue until the thread is interrupted.
		 */
		@Override
		public void run()
		{
			while (bRun)
			{
				try
				{
					aJobQueue.take().run();
				}
				catch (InterruptedException e)
				{
					bRun = false;
				}
			}

			rPauseSignal.countDown();
		}
	}
}
