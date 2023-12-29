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
import java.util.ListIterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test of observable list functionality.
 *
 * @author eso
 */
public class ObservableListTest implements EventHandler<ListEvent<String>> {

	private ObservableList<String> aObservableList;

	private EventType rExpectedEventType = EventType.ADD;

	private String sTestValue;

	private int nExpectedIndex;

	/**
	 * Handles collection events.
	 *
	 * @param rEvent The event
	 */
	@Override
	public void handleEvent(ListEvent<String> rEvent) {
		EventType rType = rEvent.getType();

		assertEquals(rExpectedEventType, rType);
		assertEquals(nExpectedIndex, rEvent.getIndex());

		switch (rEvent.getType()) {
			case ADD:
				sTestValue = rEvent.getElement();
				nExpectedIndex++;
				break;

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
		aObservableList = new ObservableList<String>();

		aObservableList.addListener(this);
	}

	/**
	 * Tests adding of elements.
	 */
	@Test
	public void testAdd() {
		assertTrue(aObservableList.isEmpty());

		aObservableList.add("T1");
		assertEquals("T1", sTestValue);
		assertEquals(1, aObservableList.size());

		aObservableList.add("T2");
		assertEquals("T2", sTestValue);
		assertEquals(2, aObservableList.size());

		aObservableList.addAll(Arrays.asList("T3", "T4"));
		assertEquals("T4", sTestValue);
		assertEquals(4, aObservableList.size());

		Collections.addAll(aObservableList, "T5", "T6");
		assertEquals("T6", sTestValue);
		assertEquals(6, aObservableList.size());

		assertEquals(6, nExpectedIndex);

		nExpectedIndex = 0;
		aObservableList.add(nExpectedIndex, "T1a");
		assertEquals("T1a", sTestValue);
		assertEquals(7, aObservableList.size());

		nExpectedIndex = 2;
		aObservableList.add(nExpectedIndex, "T3a");
		assertEquals("T3a", sTestValue);
		assertEquals(8, aObservableList.size());

		nExpectedIndex = aObservableList.size();
		aObservableList.add(nExpectedIndex, "TX");
		assertEquals("TX", sTestValue);
		assertEquals(9, aObservableList.size());
	}

	/**
	 * Tests clearing the collection.
	 */
	@Test
	public void testClear() {
		initList(EventType.REMOVE_ALL);

		nExpectedIndex = -1;

		aObservableList.clear();
		assertNull(sTestValue);
		assertTrue(aObservableList.isEmpty());
	}

	/**
	 * Tests iterating.
	 */
	@Test
	public void testIterator() {
		initList(null);

		int i = 0;

		for (String sTest : aObservableList) {
			assertEquals("T" + ++i, sTest);
		}

		assertEquals(3, i);
	}

	/**
	 * Tests removing from an iterator.
	 */
	@Test
	public void testIteratorRemove() {
		initList(EventType.REMOVE);

		Iterator<String> rIterator = aObservableList.iterator();
		int i = 0;

		while (rIterator.hasNext()) {
			String sTest = "T" + ++i;

			assertEquals(sTest, rIterator.next());
			rIterator.remove();
			assertEquals(sTest, sTestValue);
		}

		assertTrue(aObservableList.isEmpty());
	}

	/**
	 * Tests list iterator functions.
	 */
	@Test
	public void testListIterator() {
		initList(EventType.UPDATE);

		ListIterator<String> rIterator = aObservableList.listIterator();

		while (rIterator.hasNext()) {
			String sTest = rIterator.next();

			rIterator.set("U" + sTest);
			nExpectedIndex++;
			assertEquals("UT" + nExpectedIndex, sTestValue);
		}

		while (rIterator.hasPrevious()) {
			String sTest = rIterator.previous();

			nExpectedIndex--;
			assertEquals("UT" + (nExpectedIndex + 1), sTest);
			rIterator.set("R" + sTest);
			assertEquals("RUT" + (nExpectedIndex + 1), sTestValue);
		}

		rExpectedEventType = EventType.ADD;
		rIterator.add("T1");
		assertEquals(4, aObservableList.size());
		assertEquals("RUT1", rIterator.next());
		nExpectedIndex++;
		rIterator.add("T1");
		assertEquals("RUT2", rIterator.next());

		rIterator = aObservableList.listIterator();
		nExpectedIndex = 0;

		rIterator.add("T0");
		assertEquals("T1", rIterator.next());
	}

	/**
	 * Tests list iterator functions.
	 */
	@Test
	public void testListIteratorRemovePrevious() {
		initList(EventType.REMOVE);

		ListIterator<String> rIterator = aObservableList.listIterator();

		while (rIterator.hasNext()) {
			rIterator.next();
		}

		nExpectedIndex = 2;

		while (rIterator.hasPrevious()) {
			rIterator.previous();
			rIterator.remove();

			assertEquals("T" + (nExpectedIndex + 1), sTestValue);
			nExpectedIndex--;
		}
	}

	/**
	 * Tests removing of elements.
	 */
	@Test
	public void testRemove() {
		initList(EventType.REMOVE);

		nExpectedIndex = 1;

		aObservableList.remove("T2");
		assertEquals("T2", sTestValue);
		assertEquals(2, aObservableList.size());
		assertFalse(aObservableList.contains("T2"));

		aObservableList.remove(1);
		assertEquals("T3", sTestValue);
		assertEquals(1, aObservableList.size());
		assertFalse(aObservableList.contains("T3"));
	}

	/**
	 * Tests adding of elements.
	 */
	@Test
	public void testSet() {
		initList(EventType.UPDATE);

		nExpectedIndex = 1;
		aObservableList.set(1, "T4");

		assertEquals("T4", sTestValue);
		assertEquals(3, aObservableList.size());
	}

	/**
	 * Initializes the list with a test dataset.
	 *
	 * @param rEventType The expected event type for subsequent changes
	 */
	void initList(EventType rEventType) {
		assertTrue(aObservableList.isEmpty());

		aObservableList.add("T1");
		aObservableList.add("T2");
		aObservableList.add("T3");

		rExpectedEventType = rEventType;
		nExpectedIndex = 0;
	}
}
