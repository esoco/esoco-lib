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
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test of observable collection functionality.
 *
 * @author eso
 */
public class ObservableCollectionTest
	implements EventHandler<CollectionEvent<String, ?>> {

	private ObservableSet<String> aObservableCollection;

	private EventType rExpectedEventType = EventType.ADD;

	private String sTestValue;

	/**
	 * Handles collection events.
	 *
	 * @param rEvent The event
	 */
	@Override
	public void handleEvent(CollectionEvent<String, ?> rEvent) {
		EventType rType = rEvent.getType();

		assertEquals(rExpectedEventType, rType);

		switch (rEvent.getType()) {
			case ADD:
			case REMOVE:
			case REMOVE_ALL:
				sTestValue = rEvent.getElement();
				break;

			case UPDATE:
				sTestValue = rEvent.getUpdateValue();
				break;

			default:
				assertTrue("Unknown event type: " + rType, false);
		}
	}

	/**
	 * Test setup.
	 */
	@Before
	public void setup() {
		aObservableCollection =
			new ObservableSet<String>(new LinkedHashSet<String>());

		aObservableCollection.addListener(this);
	}

	/**
	 * Tests adding of elements.
	 */
	@Test
	public void testAdd() {
		assertTrue(aObservableCollection.isEmpty());

		aObservableCollection.add("T1");
		assertEquals("T1", sTestValue);
		assertEquals(1, aObservableCollection.size());

		aObservableCollection.add("T2");
		assertEquals("T2", sTestValue);
		assertEquals(2, aObservableCollection.size());

		aObservableCollection.addAll(Arrays.asList("T3", "T4"));
		assertEquals("T4", sTestValue);
		assertEquals(4, aObservableCollection.size());

		Collections.addAll(aObservableCollection, "T5", "T6");
		assertEquals("T6", sTestValue);
		assertEquals(6, aObservableCollection.size());
	}

	/**
	 * Tests clearing the collection.
	 */
	@Test
	public void testClear() {
		initCollection(EventType.REMOVE_ALL);

		aObservableCollection.clear();
		assertNull(sTestValue);
		assertTrue(aObservableCollection.isEmpty());
	}

	/**
	 * Tests iterating.
	 */
	@Test
	public void testIterator() {
		initCollection(EventType.REMOVE);

		int i = 0;

		for (String sTest : aObservableCollection) {
			assertEquals("T" + ++i, sTest);
		}

		assertEquals(3, i);

		Iterator<String> rIterator = aObservableCollection.iterator();

		i = 0;

		while (rIterator.hasNext()) {
			String sTest = "T" + ++i;

			assertEquals(sTest, rIterator.next());
			rIterator.remove();
			assertEquals(sTest, sTestValue);
		}

		assertTrue(aObservableCollection.isEmpty());
	}

	/**
	 * Tests removing of elements.
	 */
	@Test
	public void testRemove() {
		initCollection(EventType.REMOVE);

		aObservableCollection.remove("T2");

		assertEquals("T2", sTestValue);
		assertEquals(2, aObservableCollection.size());
		assertFalse(aObservableCollection.contains("T2"));
	}

	/**
	 * Initializes the test collection.
	 *
	 * @param rEventType The expected event type for subsequent changes
	 */
	void initCollection(EventType rEventType) {
		assertTrue(aObservableCollection.isEmpty());

		aObservableCollection.add("T1");
		aObservableCollection.add("T2");
		aObservableCollection.add("T3");

		rExpectedEventType = rEventType;
	}
}
