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


/********************************************************************
 * Test suite for the IntArray class.
 *
 * @author eso
 */
public class IntArrayTest extends TestCase
{
	//~ Instance fields --------------------------------------------------------

	IntArray aTestArray;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Default constructor.
	 */
	public IntArrayTest()
	{
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Set up data for all tests.
	 */
	@Override
	public void setUp()
	{
		aTestArray = new IntArray(10);

		for (int i = 0; i < 10; i++)
		{
			aTestArray.push(i);
		}
	}

	/***************************************
	 * Test array access.
	 */
	public void testArrayAccess()
	{
		assertEquals(5, aTestArray.get(5));

		aTestArray.push(123);

		assertEquals(11, aTestArray.getSize());
		assertEquals(123, aTestArray.pop());
		assertEquals(10, aTestArray.getSize());
		assertEquals(20, aTestArray.getCapacity());

		aTestArray.set(45, 9);
		assertEquals(45, aTestArray.get(9));

		aTestArray.insert(122, 2);
		assertEquals(11, aTestArray.getSize());
		assertEquals(122, aTestArray.get(2));
		assertEquals(2, aTestArray.get(3));

		aTestArray.remove(2);
		assertEquals(2, aTestArray.get(2));
		assertEquals(10, aTestArray.getSize());

		aTestArray.insertAscending(5, 0);
		assertEquals(5, aTestArray.get(6));
		assertEquals(11, aTestArray.getSize());

		aTestArray.insertAscending(1, 8);
		assertEquals(1, aTestArray.get(8));

		aTestArray.setSize(20);
		assertEquals(20, aTestArray.getSize());
		assertEquals(0, aTestArray.get(19));
		assertEquals(20, aTestArray.getCapacity());

		aTestArray.add(42);
		assertEquals(30, aTestArray.getCapacity());
	}
}
