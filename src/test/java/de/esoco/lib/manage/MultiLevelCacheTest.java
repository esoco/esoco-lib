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
package de.esoco.lib.manage;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/********************************************************************
 * Test of {@link MultiLevelCache}.
 *
 * @author eso
 */
public class MultiLevelCacheTest
{
	//~ Instance fields --------------------------------------------------------

	private MultiLevelCache<Integer, String> aCache;

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 */
	@Before
	public void setup()
	{
		aCache = new MultiLevelCache<Integer, String>(2, 2, 2);

		putTestEntries(1, 6);
	}

	/***************************************
	 * Test of {@link MultiLevelCache#get(Object)}.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testMultiLevel()
	{
		putTestEntries(7, 8);
		assertEquals(null, aCache.get(1));
		assertEquals(null, aCache.get(2));

		for (int i = 3; i <= 8; i++)
		{
			assertEquals("Test" + i, aCache.get(i));
		}
	}

	/***************************************
	 * Test of {@link MultiLevelCache#remove(Object)}.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testRemove()
	{
		aCache.remove(1);
		aCache.remove(3);
		aCache.remove(5);
		assertEquals(null, aCache.get(1));
		assertEquals(null, aCache.get(3));
		assertEquals(null, aCache.get(5));
		assertEquals("Test2", aCache.get(2));
		assertEquals("Test4", aCache.get(4));
	}

	/***************************************
	 * Puts test entries into the cache.
	 *
	 * @param nStart The start index
	 * @param nEnd   The end index
	 */
	@SuppressWarnings("boxing")
	private void putTestEntries(int nStart, int nEnd)
	{
		for (int i = nStart; i <= nEnd; i++)
		{
			aCache.put(i, "Test" + i);
		}
	}
}
