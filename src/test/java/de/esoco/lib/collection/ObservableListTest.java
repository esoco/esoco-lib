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
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test of observable list functionality.
 *
 * @author eso
 */
public class ObservableListTest implements EventHandler<ListEvent<String>> {

	private ObservableList<String> observableList;

	private EventType expectedEventType = EventType.ADD;

	private String testValue;

	private int expectedIndex;

	/**
	 * Handles collection events.
	 *
	 * @param event The event
	 */
	@Override
	public void handleEvent(ListEvent<String> event) {
		EventType type = event.getType();

		assertEquals(expectedEventType, type);
		assertEquals(expectedIndex, event.getIndex());

		switch (event.getType()) {
			case ADD:
				testValue = event.getElement();
				expectedIndex++;
				break;

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
		observableList = new ObservableList<String>();

		observableList.addListener(this);
	}

	/**
	 * Tests adding of elements.
	 */
	@Test
	public void testAdd() {
		assertTrue(observableList.isEmpty());

		observableList.add("T1");
		assertEquals("T1", testValue);
		assertEquals(1, observableList.size());

		observableList.add("T2");
		assertEquals("T2", testValue);
		assertEquals(2, observableList.size());

		observableList.addAll(Arrays.asList("T3", "T4"));
		assertEquals("T4", testValue);
		assertEquals(4, observableList.size());

		Collections.addAll(observableList, "T5", "T6");
		assertEquals("T6", testValue);
		assertEquals(6, observableList.size());

		assertEquals(6, expectedIndex);

		expectedIndex = 0;
		observableList.add(expectedIndex, "T1a");
		assertEquals("T1a", testValue);
		assertEquals(7, observableList.size());

		expectedIndex = 2;
		observableList.add(expectedIndex, "T3a");
		assertEquals("T3a", testValue);
		assertEquals(8, observableList.size());

		expectedIndex = observableList.size();
		observableList.add(expectedIndex, "TX");
		assertEquals("TX", testValue);
		assertEquals(9, observableList.size());
	}

	/**
	 * Tests clearing the collection.
	 */
	@Test
	public void testClear() {
		initList(EventType.REMOVE_ALL);

		expectedIndex = -1;

		observableList.clear();
		assertNull(testValue);
		assertTrue(observableList.isEmpty());
	}

	/**
	 * Tests iterating.
	 */
	@Test
	public void testIterator() {
		initList(null);

		int i = 0;

		for (String test : observableList) {
			assertEquals("T" + ++i, test);
		}

		assertEquals(3, i);
	}

	/**
	 * Tests removing from an iterator.
	 */
	@Test
	public void testIteratorRemove() {
		initList(EventType.REMOVE);

		Iterator<String> iterator = observableList.iterator();
		int i = 0;

		while (iterator.hasNext()) {
			String test = "T" + ++i;

			assertEquals(test, iterator.next());
			iterator.remove();
			assertEquals(test, testValue);
		}

		assertTrue(observableList.isEmpty());
	}

	/**
	 * Tests list iterator functions.
	 */
	@Test
	public void testListIterator() {
		initList(EventType.UPDATE);

		ListIterator<String> iterator = observableList.listIterator();

		while (iterator.hasNext()) {
			String test = iterator.next();

			iterator.set("U" + test);
			expectedIndex++;
			assertEquals("UT" + expectedIndex, testValue);
		}

		while (iterator.hasPrevious()) {
			String test = iterator.previous();

			expectedIndex--;
			assertEquals("UT" + (expectedIndex + 1), test);
			iterator.set("R" + test);
			assertEquals("RUT" + (expectedIndex + 1), testValue);
		}

		expectedEventType = EventType.ADD;
		iterator.add("T1");
		assertEquals(4, observableList.size());
		assertEquals("RUT1", iterator.next());
		expectedIndex++;
		iterator.add("T1");
		assertEquals("RUT2", iterator.next());

		iterator = observableList.listIterator();
		expectedIndex = 0;

		iterator.add("T0");
		assertEquals("T1", iterator.next());
	}

	/**
	 * Tests list iterator functions.
	 */
	@Test
	public void testListIteratorRemovePrevious() {
		initList(EventType.REMOVE);

		ListIterator<String> iterator = observableList.listIterator();

		while (iterator.hasNext()) {
			iterator.next();
		}

		expectedIndex = 2;

		while (iterator.hasPrevious()) {
			iterator.previous();
			iterator.remove();

			assertEquals("T" + (expectedIndex + 1), testValue);
			expectedIndex--;
		}
	}

	/**
	 * Tests removing of elements.
	 */
	@Test
	public void testRemove() {
		initList(EventType.REMOVE);

		expectedIndex = 1;

		observableList.remove("T2");
		assertEquals("T2", testValue);
		assertEquals(2, observableList.size());
		assertFalse(observableList.contains("T2"));

		observableList.remove(1);
		assertEquals("T3", testValue);
		assertEquals(1, observableList.size());
		assertFalse(observableList.contains("T3"));
	}

	/**
	 * Tests adding of elements.
	 */
	@Test
	public void testSet() {
		initList(EventType.UPDATE);

		expectedIndex = 1;
		observableList.set(1, "T4");

		assertEquals("T4", testValue);
		assertEquals(3, observableList.size());
	}

	/**
	 * Initializes the list with a test dataset.
	 *
	 * @param eventType The expected event type for subsequent changes
	 */
	void initList(EventType eventType) {
		assertTrue(observableList.isEmpty());

		observableList.add("T1");
		observableList.add("T2");
		observableList.add("T3");

		expectedEventType = eventType;
		expectedIndex = 0;
	}
}
