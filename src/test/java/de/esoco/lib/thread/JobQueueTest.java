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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/********************************************************************
 * Test of {@link JobQueue}.
 *
 * @author eso
 */
public class JobQueueTest
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test method
	 */
	@Test
	public void testSingleThreadJobQueue()
	{
		JobQueue		    jq	    = new JobQueue(10, 1);
		final AtomicInteger counter = new AtomicInteger(0);

		for (int i = 0; i < 10; i++)
		{
			jq.add(() -> counter.incrementAndGet());
		}

		jq.pause();
		jq.stop();
		assertEquals(0, jq.getSize());
	}
}
