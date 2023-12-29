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
package de.esoco.lib.collection;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test of {@link CacheMap}.
 *
 * @author eso
 */
public class CacheMapTest {

	private final CacheMap<Integer, String> aTestMap =
		new CacheMap<Integer, String>(5);

	/**
	 * Test setup.
	 */
	@Before
	@SuppressWarnings("boxing")
	public void setUp() {
		aTestMap.put(1, "Test1");
		aTestMap.put(2, "Test2");
		aTestMap.put(3, "Test3");
		aTestMap.put(4, "Test4");
		aTestMap.put(5, "Test5");
	}

	/**
	 * Test of {@link CacheMap#get(Object)}.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testAutoRemove() {
		aTestMap.put(6, "Test6");
		assertFalse(aTestMap.containsKey(1));
		assertOrder(2, 3, 4, 5, 6);
	}

	/**
	 * Test of {@link CacheMap#getCapacity()}.
	 */
	@Test
	public void testGetCapacity() {
		assertEquals(5, aTestMap.getCapacity());
	}

	/**
	 * Test correct ordering of map contents.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testOrder() {
		assertOrder(1, 2, 3, 4, 5);
		aTestMap.get(1);
		assertOrder(2, 3, 4, 5, 1);
		aTestMap.get(3);
		assertOrder(2, 4, 5, 1, 3);
	}

	/**
	 * Test of {@link CacheMap#put(Object, Object)}.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testPut() {
		aTestMap.put(6, "Test6");
		assertFalse(aTestMap.containsKey(1));
	}

	/**
	 * Test of {@link CacheMap#removeEldest()}.
	 */
	@Test
	public void testRemoveEldest() {
		assertEquals("Test1", aTestMap.removeEldest().getValue());
		assertEquals("Test2", aTestMap.removeEldest().getValue());
		assertEquals("Test3", aTestMap.removeEldest().getValue());
		assertEquals("Test4", aTestMap.removeEldest().getValue());
		assertEquals("Test5", aTestMap.removeEldest().getValue());
		assertTrue(aTestMap.isEmpty());
	}

	/**
	 * Assert a certain order of the map entries.
	 *
	 * @param rOrder The keys for the cache order, from oldest to newest
	 */
	private void assertOrder(int... rOrder) {
		int i = 0;

		for (Integer nKey : aTestMap.keySet()) {
			assertEquals(Integer.valueOf(rOrder[i++]), nKey);
		}
	}
}
