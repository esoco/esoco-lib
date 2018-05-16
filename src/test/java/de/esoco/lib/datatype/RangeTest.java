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
package de.esoco.lib.datatype;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


/********************************************************************
 * Tests for {@link Range} implementations.
 *
 * @author eso
 */
public class RangeTest
{
	//~ Instance fields --------------------------------------------------------

	private int nDiff = 0;

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test of {@link CharRange}
	 */
	@Test
	public void testCharRange()
	{
		assertEquals(Arrays.asList('A', 'B', 'C', 'D'),
					 Range.of('A', 'D').toList());
		assertEquals(Arrays.asList('9', '8', '7', '6'),
					 Range.of('9', '6').toList());
	}

	/***************************************
	 * Test of {@link Range#contains(Comparable)}
	 */
	@Test
	public void testContains()
	{
		checkRangeContains(0, 5);
		checkRangeContains(2, 5);
		checkRangeContains(5, 0);
		checkRangeContains(5, 2);
		checkRangeContains(-5, 0);
		checkRangeContains(-5, 5);
		checkRangeContains(5, -5);
	}

	/***************************************
	 * Test of {@link Range#equals(Object)} and {@link Range#hashCode()}
	 */
	@Test
	public void testEqualsAndHashCode()
	{
		IntRange r1 = Range.of(1, 5);
		IntRange r2 = Range.of(1, 5);
		IntRange r3 = Range.of(0, 4);

		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
		assertNotEquals(r1, r3);
		assertNotEquals(r1.hashCode(), r3.hashCode());
	}

	/***************************************
	 * Test of {@link Range#forEach(java.util.function.Consumer)}
	 */
	@Test
	public void testForEach()
	{
		checkRangeForEach(0, 5);
		checkRangeForEach(2, 5);
		checkRangeForEach(5, 0);
		checkRangeForEach(5, 2);
		checkRangeForEach(-5, 0);
		checkRangeForEach(5, -5);
	}

	/***************************************
	 * Test of {@link Range#stream()}
	 */
	@Test
	public void testStream()
	{
		List<Integer> l = Range.of(1, 10).stream().collect(Collectors.toList());

		assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), l);

		l = Range.of(1, 10)
				 .stream()
				 .filter(i -> i % 2 == 0)
				 .collect(Collectors.toList());

		assertEquals(Arrays.asList(2, 4, 6, 8, 10), l);

		l = Range.of(-2, 2).stream().collect(Collectors.toList());
		assertEquals(Arrays.asList(-2, -1, 0, 1, 2), l);

		l = Range.of(2, -2).stream().collect(Collectors.toList());
		assertEquals(Arrays.asList(2, 1, 0, -1, -2), l);
	}

	/***************************************
	 * Checks the bounds of integer ranges.
	 *
	 * @param nFirst
	 * @param nLast
	 */
	private void checkRangeContains(int nFirst, int nLast)
	{
		IntRange r = Range.of(nFirst, nLast);

		for (int i = nFirst; i <= nLast; i++)
		{
			assertTrue(r.contains(i));
		}

		assertFalse(r.contains(nFirst - (int) r.getStep()));
		assertFalse(r.contains(nLast + (int) r.getStep()));
	}

	/***************************************
	 * Checks {@link Range#forEach(java.util.function.Consumer)}.
	 *
	 * @param nFirst
	 * @param nLast
	 */
	private void checkRangeForEach(int nFirst, int nLast)
	{
		nDiff = 0;

		IntRange r = Range.of(nFirst, nLast);

		r.forEach(i ->
	  			{
	  				assertEquals(i.intValue(), nFirst + nDiff);
	  				nDiff += r.getStep();
				  });
	}
}
