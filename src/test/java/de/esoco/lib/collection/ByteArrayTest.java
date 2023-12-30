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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test suite for the ByteArray class.
 *
 * @author eso
 */
public class ByteArrayTest {

	private ByteArray testArray;

	/**
	 * Set up test data.
	 */
	@BeforeEach
	public void setUp() {
		testArray = new ByteArray(10);

		for (byte b = 0; b < 10; b++) {
			testArray.push(b);
		}
	}

	/**
	 * Test array access.
	 */
	@Test
	public void testArrayAccess() {
		assertEquals(5, testArray.get(5));

		testArray.push((byte) 123);

		assertEquals(11, testArray.getSize());
		assertEquals(123, testArray.pop());
		assertEquals(10, testArray.getSize());
		assertEquals(20, testArray.getCapacity());

		testArray.set((byte) 45, 9);
		assertEquals(45, testArray.get(9));

		testArray.insert((byte) 122, 2);
		assertEquals(11, testArray.getSize());
		assertEquals(122, testArray.get(2));
		assertEquals(2, testArray.get(3));

		testArray.remove(2);
		assertEquals(2, testArray.get(2));
		assertEquals(10, testArray.getSize());

		testArray.insertAscending((byte) 5, 0);
		assertEquals(5, testArray.get(6));
		assertEquals(11, testArray.getSize());

		testArray.insertAscending((byte) 1, 8);
		assertEquals(1, testArray.get(8));

		testArray.setSize(20);
		assertEquals(20, testArray.getSize());
		assertEquals(0, testArray.get(19));
		assertEquals(20, testArray.getCapacity());

		testArray.add((byte) 42);
		assertEquals(30, testArray.getCapacity());
	}

	/**
	 * Test {@link ByteArray#fromJson(String)}
	 */
	@Test
	public void testFromJson() {
		assertEquals(new ByteArray(), new ByteArray().fromJson("\"0x\""));

		assertEquals(testArray,
			new ByteArray().fromJson("\"0x00010203040506070809\""));

		testArray.add((byte) 128);
		testArray.add((byte) 255);
		assertEquals(testArray,
			new ByteArray().fromJson("\"0x0001020304050607080980FF\""));
	}

	/**
	 * Test {@link ByteArray#toJson()}.
	 */
	@Test
	public void testToJson() {
		assertEquals("\"0x\"", new ByteArray().toJson());

		assertEquals("\"0x00010203040506070809\"", testArray.toJson());

		testArray.add((byte) 10);
		testArray.add((byte) 15);
		assertEquals("\"0x000102030405060708090A0F\"", testArray.toJson());

		testArray.add((byte) 128);
		testArray.add((byte) 255);
		assertEquals("\"0x000102030405060708090A0F80FF\"", testArray.toJson());
	}
}
