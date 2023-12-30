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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test of observable collection functionality.
 *
 * @author eso
 */
public class ObservableCollectionTest
	implements EventHandler<CollectionEvent<String, ?>> {

	private ObservableSet<String> observableCollection;

	private EventType expectedEventType = EventType.ADD;

	private String testValue;

	/**
	 * Handles collection events.
	 *
	 * @param event The event
	 */
	@Override
	public void handleEvent(CollectionEvent<String, ?> event) {
		EventType type = event.getType();

		assertEquals(expectedEventType, type);

		switch (event.getType()) {
			case ADD:
			case REMOVE:
			case REMOVE_ALL:
				testValue = event.getElement();
				break;

			case UPDATE:
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
		observableCollection =
			new ObservableSet<String>(new LinkedHashSet<String>());

		observableCollection.addListener(this);
	}

	/**
	 * Tests adding of elements.
	 */
	@Test
	public void testAdd() {
		assertTrue(observableCollection.isEmpty());

		observableCollection.add("T1");
		assertEquals("T1", testValue);
		assertEquals(1, observableCollection.size());

		observableCollection.add("T2");
		assertEquals("T2", testValue);
		assertEquals(2, observableCollection.size());

		observableCollection.addAll(Arrays.asList("T3", "T4"));
		assertEquals("T4", testValue);
		assertEquals(4, observableCollection.size());

		Collections.addAll(observableCollection, "T5", "T6");
		assertEquals("T6", testValue);
		assertEquals(6, observableCollection.size());
	}

	/**
	 * Tests clearing the collection.
	 */
	@Test
	public void testClear() {
		initCollection(EventType.REMOVE_ALL);

		observableCollection.clear();
		assertNull(testValue);
		assertTrue(observableCollection.isEmpty());
	}

	/**
	 * Tests iterating.
	 */
	@Test
	public void testIterator() {
		initCollection(EventType.REMOVE);

		int i = 0;

		for (String test : observableCollection) {
			assertEquals("T" + ++i, test);
		}

		assertEquals(3, i);

		Iterator<String> iterator = observableCollection.iterator();

		i = 0;

		while (iterator.hasNext()) {
			String test = "T" + ++i;

			assertEquals(test, iterator.next());
			iterator.remove();
			assertEquals(test, testValue);
		}

		assertTrue(observableCollection.isEmpty());
	}

	/**
	 * Tests removing of elements.
	 */
	@Test
	public void testRemove() {
		initCollection(EventType.REMOVE);

		observableCollection.remove("T2");

		assertEquals("T2", testValue);
		assertEquals(2, observableCollection.size());
		assertFalse(observableCollection.contains("T2"));
	}

	/**
	 * Initializes the test collection.
	 *
	 * @param eventType The expected event type for subsequent changes
	 */
	void initCollection(EventType eventType) {
		assertTrue(observableCollection.isEmpty());

		observableCollection.add("T1");
		observableCollection.add("T2");
		observableCollection.add("T3");

		expectedEventType = eventType;
	}
}
