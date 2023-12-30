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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test of observable collection functionality.
 *
 * @author eso
 */
public class ObservableMapTest
	implements EventHandler<MapEvent<String, String>> {

	EventType expectedEventType;

	String testKey;

	String testValue;

	private ObservableMap<String, String> observableMap;

	/**
	 * Handles collection events.
	 *
	 * @param event The event
	 */
	@Override
	public void handleEvent(MapEvent<String, String> event) {
		EventType type = event.getType();

		assertEquals(expectedEventType, type);

		switch (event.getType()) {
			case ADD:
			case REMOVE:
			case REMOVE_ALL:
			case UPDATE:
				testKey = event.getElement();
				testValue = event.getUpdateValue();

				break;

			default:
				fail("Unknown event type: " + type);
		}
	}

	/**
	 * Test setup.
	 */
	@BeforeEach
	public void setup() {
		// use a linked map to make entry order predictable
		observableMap = new ObservableMap<String, String>(
			new LinkedHashMap<String, String>());

		observableMap.addListener(this);
	}

	/**
	 * Tests clearing the map.
	 */
	@Test
	public void testClear() {
		initMap(EventType.REMOVE_ALL);

		observableMap.clear();
		assertTestValues(null, null);
		assertTrue(observableMap.isEmpty());
	}

	/**
	 * Tests removing through an entry iterator.
	 */
	@Test
	public void testEntryIteratorRemove() {
		initMap(EventType.REMOVE);

		Iterator<Entry<String, String>> iterator =
			observableMap.entrySet().iterator();

		int i = 1;

		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
			assertTestValues("K" + i, "V" + i);
			i++;
		}
	}

	/**
	 * Tests removing through an entry iterator.
	 */
	@Test
	public void testEntryUpdate() {
		initMap(EventType.UPDATE);

		Iterator<Entry<String, String>> iterator =
			observableMap.entrySet().iterator();

		int i = 1;

		while (iterator.hasNext()) {
			iterator.next().setValue("U" + i);
			assertTestValues("K" + i, "U" + i);
			i++;
		}
	}

	/**
	 * Tests removing through a key iterator.
	 */
	@Test
	public void testKeyIteratorRemove() {
		initMap(EventType.REMOVE);

		Iterator<String> iterator = observableMap.keySet().iterator();

		int i = 1;

		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
			assertTestValues("K" + i, "V" + i);
			i++;
		}
	}

	/**
	 * Tests adding elements with put.
	 */
	@Test
	public void testPut() {
		initMap(EventType.ADD);

		Map<String, String> putMap = new HashMap<String, String>();

		putMap.put("K4", "V4");
		putMap.put("K5", "V5");

		observableMap.putAll(putMap);
		assertTestValues("K5", "V5");
	}

	/**
	 * Tests removing elements.
	 */
	@Test
	public void testRemove() {
		initMap(EventType.REMOVE);

		observableMap.remove("K1");
		assertTestValues("K1", "V1");
		observableMap.remove("K3");
		assertTestValues("K3", "V3");
		observableMap.remove("K2");
		assertTestValues("K2", "V2");

		assertTrue(observableMap.isEmpty());
	}

	/**
	 * Tests updating elements.
	 */
	@Test
	public void testUpdate() {
		initMap(EventType.UPDATE);

		observableMap.put("K1", "V1A");
		assertTestValues("K1", "V1A");
		observableMap.put("K2", "V2A");
		assertTestValues("K2", "V2A");
		observableMap.put("K3", "V3A");
		assertTestValues("K3", "V3A");
	}

	/**
	 * Tests removing through a value iterator.
	 */
	@Test
	public void testValueIteratorRemove() {
		initMap(EventType.REMOVE);

		Iterator<String> iterator = observableMap.values().iterator();

		int i = 1;

		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
			assertTestValues("K" + i, "V" + i);
			i++;
		}
	}

	/**
	 * Asserts the equality of the test values with the arguments.
	 *
	 * @param key   The expected test key
	 * @param value The expected test value
	 */
	void assertTestValues(String key, String value) {
		assertEquals(key, testKey);
		assertEquals(value, testValue);
	}

	/**
	 * Initializes the test map.
	 *
	 * @param eventType The expected event type for subsequent changes
	 */
	void initMap(EventType eventType) {
		assertTrue(observableMap.isEmpty());

		expectedEventType = EventType.ADD;
		observableMap.put("K1", "V1");
		assertTestValues("K1", "V1");
		observableMap.put("K2", "V2");
		assertTestValues("K2", "V2");
		observableMap.put("K3", "V3");
		assertTestValues("K3", "V3");

		expectedEventType = eventType;
	}
}
