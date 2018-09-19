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
package de.esoco.lib.concurrent.coroutine;

import de.esoco.lib.logging.Profiler;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;

import static de.esoco.lib.concurrent.coroutine.step.CodeExecution.run;
import static de.esoco.lib.datatype.Range.from;


/********************************************************************
 * Demo of {@link Coroutine} features.
 *
 * @author eso
 */
public class CoroutineDemo
{
	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Runs coroutines parallel in threads and with asynchronous execution for
	 * comparison.
	 */
	public static void demoParallelExecution()
	{
		CoroutineContext	    ctx = new CoroutineContext();
		Coroutine<Object, Void> cr  =
			Coroutine.first(run(() -> from(1).to(10).forEach(Math::sqrt)));

		int nThreadCount    = 1_000;
		int nCoroutineCount = 1_000;

		Profiler	   p	  = new Profiler("Parallel Coroutine Execution");
		CountDownLatch signal = new CountDownLatch(nThreadCount);

		for (int i = 0; i < nThreadCount; i++)
		{
			new Thread(() ->
				{
					cr.runBlocking(ctx, null);
					signal.countDown();
				}).start();
		}

		try
		{
			signal.await();
		}
		catch (InterruptedException e)
		{
			throw new CompletionException(e);
		}

		p.measure(nThreadCount + " Threads");

		for (int i = 0; i < nCoroutineCount; i++)
		{
			cr.runAsync(ctx);
		}

		ctx.awaitAll();

		p.measure(nCoroutineCount + " Coroutines");
		p.printSummary();
	}

	/***************************************
	 * Main
	 *
	 * @param rArgs
	 */
	public static void main(String[] rArgs)
	{
		demoParallelExecution();
	}
}
