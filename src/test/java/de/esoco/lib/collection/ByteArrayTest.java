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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/********************************************************************
 * Test suite for the ByteArray class.
 *
 * @author eso
 */
public class ByteArrayTest
{
	//~ Instance fields --------------------------------------------------------

	private ByteArray aTestArray;

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Set up test data.
	 */
	@Before
	public void setUp()
	{
		aTestArray = new ByteArray(10);

		for (byte b = 0; b < 10; b++)
		{
			aTestArray.push(b);
		}
	}

	/***************************************
	 * Test array access.
	 */
	@Test
	public void testArrayAccess()
	{
		assertEquals(5, aTestArray.get(5));

		aTestArray.push((byte) 123);

		assertEquals(11, aTestArray.getSize());
		assertEquals(123, aTestArray.pop());
		assertEquals(10, aTestArray.getSize());
		assertEquals(20, aTestArray.getCapacity());

		aTestArray.set((byte) 45, 9);
		assertEquals(45, aTestArray.get(9));

		aTestArray.insert((byte) 122, 2);
		assertEquals(11, aTestArray.getSize());
		assertEquals(122, aTestArray.get(2));
		assertEquals(2, aTestArray.get(3));

		aTestArray.remove(2);
		assertEquals(2, aTestArray.get(2));
		assertEquals(10, aTestArray.getSize());

		aTestArray.insertAscending((byte) 5, 0);
		assertEquals(5, aTestArray.get(6));
		assertEquals(11, aTestArray.getSize());

		aTestArray.insertAscending((byte) 1, 8);
		assertEquals(1, aTestArray.get(8));

		aTestArray.setSize(20);
		assertEquals(20, aTestArray.getSize());
		assertEquals(0, aTestArray.get(19));
		assertEquals(20, aTestArray.getCapacity());

		aTestArray.add((byte) 42);
		assertEquals(30, aTestArray.getCapacity());
	}

	/***************************************
	 * Test {@link ByteArray#fromJson(String)}
	 */
	@Test
	public void testFromJson()
	{
		assertEquals(new ByteArray(), new ByteArray().fromJson("\"0x\""));

		assertEquals(aTestArray,
					 new ByteArray().fromJson("\"0x00010203040506070809\""));

		aTestArray.add((byte) 128);
		aTestArray.add((byte) 255);
		assertEquals(aTestArray,
					 new ByteArray().fromJson("\"0x0001020304050607080980FF\""));
	}

	/***************************************
	 * Test {@link ByteArray#toJson()}.
	 */
	@Test
	public void testToJson()
	{
		assertEquals("\"0x\"", new ByteArray().toJson());

		assertEquals("\"0x00010203040506070809\"", aTestArray.toJson());

		aTestArray.add((byte) 10);
		aTestArray.add((byte) 15);
		assertEquals("\"0x000102030405060708090A0F\"", aTestArray.toJson());

		aTestArray.add((byte) 128);
		aTestArray.add((byte) 255);
		assertEquals("\"0x000102030405060708090A0F80FF\"", aTestArray.toJson());
	}
}
