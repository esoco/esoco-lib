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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test of {@link MultiLevelCache}.
 *
 * @author eso
 */
public class MultiLevelCacheTest {

	private MultiLevelCache<Integer, String> cache;

	/**
	 * Creates a new instance.
	 */
	@BeforeEach
	public void setup() {
		cache = new MultiLevelCache<Integer, String>(2, 2, 2);

		putTestEntries(1, 6);
	}

	/**
	 * Test of {@link MultiLevelCache#get(Object)}.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testMultiLevel() {
		putTestEntries(7, 8);
		assertNull(cache.get(1));
		assertNull(cache.get(2));

		for (int i = 3; i <= 8; i++) {
			assertEquals("Test" + i, cache.get(i));
		}
	}

	/**
	 * Test of {@link MultiLevelCache#remove(Object)}.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testRemove() {
		cache.remove(1);
		cache.remove(3);
		cache.remove(5);
		assertNull(cache.get(1));
		assertNull(cache.get(3));
		assertNull(cache.get(5));
		assertEquals("Test2", cache.get(2));
		assertEquals("Test4", cache.get(4));
	}

	/**
	 * Puts test entries into the cache.
	 *
	 * @param start The start index
	 * @param end   The end index
	 */
	@SuppressWarnings("boxing")
	private void putTestEntries(int start, int end) {
		for (int i = start; i <= end; i++) {
			cache.put(i, "Test" + i);
		}
	}
}
