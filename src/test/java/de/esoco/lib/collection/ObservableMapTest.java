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

import de.esoco.lib.event.ElementEvent.EventType;
import de.esoco.lib.event.EventHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/********************************************************************
 * Test of observable collection functionality.
 *
 * @author eso
 */
public class ObservableMapTest implements EventHandler<MapEvent<String, String>>
{
	//~ Instance fields --------------------------------------------------------

	private ObservableMap<String, String> aObservableMap;
	EventType							  rExpectedEventType;
	String								  sTestKey;
	String								  sTestValue;

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Handles collection events.
	 *
	 * @param rEvent The event
	 */
	@Override
	public void handleEvent(MapEvent<String, String> rEvent)
	{
		EventType rType = rEvent.getType();

		assertEquals(rExpectedEventType, rType);

		switch (rEvent.getType())
		{
			case ADD:
			case REMOVE:
			case REMOVE_ALL:
			case UPDATE:
				sTestKey   = rEvent.getElement();
				sTestValue = rEvent.getUpdateValue();

				break;

			default:
				assertTrue("Unknown event type: " + rType, false);
		}
	}

	/***************************************
	 * Test setup.
	 */
	@Before
	public void setup()
	{
		// use a linked map to make entry order predictable
		aObservableMap =
			new ObservableMap<String, String>(new LinkedHashMap<String,
																String>());

		aObservableMap.addListener(this);
	}

	/***************************************
	 * Tests clearing the map.
	 */
	@Test
	public void testClear()
	{
		initMap(EventType.REMOVE_ALL);

		aObservableMap.clear();
		assertTestValues(null, null);
		assertTrue(aObservableMap.isEmpty());
	}

	/***************************************
	 * Tests removing through an entry iterator.
	 */
	@Test
	public void testEntryIteratorRemove()
	{
		initMap(EventType.REMOVE);

		Iterator<Entry<String, String>> rIterator =
			aObservableMap.entrySet().iterator();

		int i = 1;

		while (rIterator.hasNext())
		{
			rIterator.next();
			rIterator.remove();
			assertTestValues("K" + i, "V" + i);
			i++;
		}
	}

	/***************************************
	 * Tests removing through an entry iterator.
	 */
	@Test
	public void testEntryUpdate()
	{
		initMap(EventType.UPDATE);

		Iterator<Entry<String, String>> rIterator =
			aObservableMap.entrySet().iterator();

		int i = 1;

		while (rIterator.hasNext())
		{
			rIterator.next().setValue("U" + i);
			assertTestValues("K" + i, "U" + i);
			i++;
		}
	}

	/***************************************
	 * Tests removing through a key iterator.
	 */
	@Test
	public void testKeyIteratorRemove()
	{
		initMap(EventType.REMOVE);

		Iterator<String> rIterator = aObservableMap.keySet().iterator();

		int i = 1;

		while (rIterator.hasNext())
		{
			rIterator.next();
			rIterator.remove();
			assertTestValues("K" + i, "V" + i);
			i++;
		}
	}

	/***************************************
	 * Tests adding elements with put.
	 */
	@Test
	public void testPut()
	{
		initMap(EventType.ADD);

		Map<String, String> aPutMap = new HashMap<String, String>();

		aPutMap.put("K4", "V4");
		aPutMap.put("K5", "V5");

		aObservableMap.putAll(aPutMap);
		assertTestValues("K5", "V5");
	}

	/***************************************
	 * Tests removing elements.
	 */
	@Test
	public void testRemove()
	{
		initMap(EventType.REMOVE);

		aObservableMap.remove("K1");
		assertTestValues("K1", "V1");
		aObservableMap.remove("K3");
		assertTestValues("K3", "V3");
		aObservableMap.remove("K2");
		assertTestValues("K2", "V2");

		assertTrue(aObservableMap.isEmpty());
	}

	/***************************************
	 * Tests updating elements.
	 */
	@Test
	public void testUpdate()
	{
		initMap(EventType.UPDATE);

		aObservableMap.put("K1", "V1A");
		assertTestValues("K1", "V1A");
		aObservableMap.put("K2", "V2A");
		assertTestValues("K2", "V2A");
		aObservableMap.put("K3", "V3A");
		assertTestValues("K3", "V3A");
	}

	/***************************************
	 * Tests removing through a value iterator.
	 */
	@Test
	public void testValueIteratorRemove()
	{
		initMap(EventType.REMOVE);

		Iterator<String> rIterator = aObservableMap.values().iterator();

		int i = 1;

		while (rIterator.hasNext())
		{
			rIterator.next();
			rIterator.remove();
			assertTestValues("K" + i, "V" + i);
			i++;
		}
	}

	/***************************************
	 * Asserts the equality of the test values with the arguments.
	 *
	 * @param sKey   The expected test key
	 * @param sValue The expected test value
	 */
	void assertTestValues(String sKey, String sValue)
	{
		assertEquals(sKey, sTestKey);
		assertEquals(sValue, sTestValue);
	}

	/***************************************
	 * Initializes the test map.
	 *
	 * @param rEventType The expected event type for subsequent changes
	 */
	void initMap(EventType rEventType)
	{
		assertTrue(aObservableMap.isEmpty());

		rExpectedEventType = EventType.ADD;
		aObservableMap.put("K1", "V1");
		assertTestValues("K1", "V1");
		aObservableMap.put("K2", "V2");
		assertTestValues("K2", "V2");
		aObservableMap.put("K3", "V3");
		assertTestValues("K3", "V3");

		rExpectedEventType = rEventType;
	}
}
