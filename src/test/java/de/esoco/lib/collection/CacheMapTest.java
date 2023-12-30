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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link CacheMap}.
 *
 * @author eso
 */
public class CacheMapTest {

	private final CacheMap<Integer, String> testMap =
		new CacheMap<Integer, String>(5);

	/**
	 * Test setup.
	 */
	@BeforeEach
	@SuppressWarnings("boxing")
	public void setUp() {
		testMap.put(1, "Test1");
		testMap.put(2, "Test2");
		testMap.put(3, "Test3");
		testMap.put(4, "Test4");
		testMap.put(5, "Test5");
	}

	/**
	 * Test of {@link CacheMap#get(Object)}.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testAutoRemove() {
		testMap.put(6, "Test6");
		assertFalse(testMap.containsKey(1));
		assertOrder(2, 3, 4, 5, 6);
	}

	/**
	 * Test of {@link CacheMap#getCapacity()}.
	 */
	@Test
	public void testGetCapacity() {
		assertEquals(5, testMap.getCapacity());
	}

	/**
	 * Test correct ordering of map contents.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testOrder() {
		assertOrder(1, 2, 3, 4, 5);
		testMap.get(1);
		assertOrder(2, 3, 4, 5, 1);
		testMap.get(3);
		assertOrder(2, 4, 5, 1, 3);
	}

	/**
	 * Test of {@link CacheMap#put(Object, Object)}.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testPut() {
		testMap.put(6, "Test6");
		assertFalse(testMap.containsKey(1));
	}

	/**
	 * Test of {@link CacheMap#removeEldest()}.
	 */
	@Test
	public void testRemoveEldest() {
		assertEquals("Test1", testMap.removeEldest().getValue());
		assertEquals("Test2", testMap.removeEldest().getValue());
		assertEquals("Test3", testMap.removeEldest().getValue());
		assertEquals("Test4", testMap.removeEldest().getValue());
		assertEquals("Test5", testMap.removeEldest().getValue());
		assertTrue(testMap.isEmpty());
	}

	/**
	 * Assert a certain order of the map entries.
	 *
	 * @param order The keys for the cache order, from oldest to newest
	 */
	private void assertOrder(int... order) {
		int i = 0;

		for (Integer key : testMap.keySet()) {
			assertEquals(Integer.valueOf(order[i++]), key);
		}
	}
}
