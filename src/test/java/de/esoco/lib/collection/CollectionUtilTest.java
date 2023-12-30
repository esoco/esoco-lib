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
package de.esoco.lib.collection;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test of CollectionUtil class.
 *
 * @author eso
 */
public class CollectionUtilTest {

	private final String[] testData =
		new String[] { "null:null", "'STRINGKEY1':\"TESTSTRING1\"",
			"STRINGKEY2:TESTSTRING2", "STRINGKEY3:TESTSTRING 3",
			"   STRINGKEY4\t\t: \t TESTSTRING 4 \t ", "'NUMKEY1':12345",
			"NUMKEY2:-12345", "3:'NUMKEY_TESTSTRING'", "4:-42",
			"'NULLTEST':null" };

	/**
	 * Test of method parseMap().
	 */
	@Test
	public final void testParseMap() {
		Map<Object, Object> map = CollectionUtil.parseMap(testData);

		assertMapContents(map);
	}

	/**
	 * Test of method parseMapEntries().
	 */
	@Test
	public final void testParseMapEntries() {
		Map<Object, Object> map = new HashMap<Object, Object>();

		CollectionUtil.parseMapEntries(map, Object.class, Object.class,
			testData);
		assertMapContents(map);
	}

	/**
	 * Test of method parseMapEntry().
	 */
	@Test
	public final void testParseMapEntry() {
		Map<Object, Object> map = new HashMap<Object, Object>();

		for (int i = 0; i < testData.length; i++) {
			CollectionUtil.addMapEntry(map, testData[i], Object.class,
				Object.class);
		}

		assertMapContents(map);
	}

	/**
	 * Tests {@link CollectionUtil#toString(Iterable, String)}.
	 */
	@Test
	public void testToStringCollection() {
		List<?> list = CollectionUtil.listOf("A", "B", "C");

		assertEquals("A,B,C", CollectionUtil.toString(list, ","));
	}

	/**
	 * Tests {@link CollectionUtil#toString(Map, String, String)}
	 */
	@Test
	public void testToStringMap() {
		Map<?, ?> map = CollectionUtil.parseMap("A=1,B=2,C=12", ',');

		assertEquals("A:1|B:2|C:12", CollectionUtil.toString(map, ":", "|"));
	}

	/**
	 * Checks the map argument to contain the expected test data.
	 *
	 * @param map The map to check
	 */
	private void assertMapContents(Map<Object, Object> map) {
		assertNull(map.get(null));
		assertEquals("TESTSTRING1", map.get("STRINGKEY1"));
		assertEquals("TESTSTRING2", map.get("STRINGKEY2"));
		assertEquals("TESTSTRING 3", map.get("STRINGKEY3"));
		assertEquals("TESTSTRING 4", map.get("STRINGKEY4"));
		assertEquals(Integer.valueOf(12345), map.get("NUMKEY1"));
		assertEquals(Integer.valueOf(-12345), map.get("NUMKEY2"));
		assertEquals("NUMKEY_TESTSTRING", map.get(Integer.valueOf(3)));
		assertEquals(Integer.valueOf(-42), map.get(Integer.valueOf(4)));
		assertNull(map.get("NULLTEST"));
	}
}
