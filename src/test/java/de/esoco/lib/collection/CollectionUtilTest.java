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
package de.esoco.lib.collection;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/********************************************************************
 * Test of CollectionUtil class.
 *
 * @author eso
 */
public class CollectionUtilTest extends TestCase
{
	//~ Instance fields --------------------------------------------------------

	private String[] aTestData =
		new String[]
		{
			"null:null", "'STRINGKEY1':\"TESTSTRING1\"",
			"STRINGKEY2:TESTSTRING2", "STRINGKEY3:TESTSTRING 3",
			"   STRINGKEY4\t\t: \t TESTSTRING 4 \t ", "'NUMKEY1':12345",
			"NUMKEY2:-12345", "3:'NUMKEY_TESTSTRING'", "4:-42",
			"'NULLTEST':null"
		};

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test of method parseMap().
	 */
	public final void testParseMap()
	{
		Map<Object, Object> aMap = CollectionUtil.parseMap(aTestData);

		assertMapContents(aMap);
	}

	/***************************************
	 * Test of method parseMapEntries().
	 */
	public final void testParseMapEntries()
	{
		Map<Object, Object> aMap = new HashMap<Object, Object>();

		CollectionUtil.parseMapEntries(aMap,
									   Object.class,
									   Object.class,
									   aTestData);
		assertMapContents(aMap);
	}

	/***************************************
	 * Test of method parseMapEntry().
	 */
	public final void testParseMapEntry()
	{
		Map<Object, Object> aMap = new HashMap<Object, Object>();

		for (int i = 0; i < aTestData.length; i++)
		{
			CollectionUtil.addMapEntry(aMap,
									   aTestData[i],
									   Object.class,
									   Object.class);
		}

		assertMapContents(aMap);
	}

	/***************************************
	 * Tests {@link CollectionUtil#toString(Iterable, String)}.
	 */
	public void testToStringCollection()
	{
		List<?> aList = CollectionUtil.listOf("A", "B", "C");

		assertEquals("A,B,C", CollectionUtil.toString(aList, ","));
	}

	/***************************************
	 * Tests {@link CollectionUtil#toString(Map, String, String)}
	 */
	public void testToStringMap()
	{
		Map<?, ?> aMap = CollectionUtil.parseMap("A=1,B=2,C=12", ',');

		assertEquals("A:1|B:2|C:12", CollectionUtil.toString(aMap, ":", "|"));
	}

	/***************************************
	 * Checks the map argument to contain the expected test data.
	 *
	 * @param aMap The map to check
	 */
	private void assertMapContents(Map<Object, Object> aMap)
	{
		assertEquals(null, aMap.get(null));
		assertEquals("TESTSTRING1", aMap.get("STRINGKEY1"));
		assertEquals("TESTSTRING2", aMap.get("STRINGKEY2"));
		assertEquals("TESTSTRING 3", aMap.get("STRINGKEY3"));
		assertEquals("TESTSTRING 4", aMap.get("STRINGKEY4"));
		assertEquals(new Integer(12345), aMap.get("NUMKEY1"));
		assertEquals(new Integer(-12345), aMap.get("NUMKEY2"));
		assertEquals("NUMKEY_TESTSTRING", aMap.get(new Integer(3)));
		assertEquals(new Integer(-42), aMap.get(new Integer(4)));
		assertEquals(null, aMap.get("NULLTEST"));
	}
}
