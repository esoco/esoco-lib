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

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


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
	 * Test of character ranges
	 */
	@Test
	public void testCharRange()
	{
		assertEquals(Arrays.asList('A', 'B', 'C', 'D'),
					 Range.from('A').to('D').toList());
		assertEquals(Arrays.asList('9', '8', '7', '6'),
					 Range.from('9').to('6').toList());
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
	 * Test of {@link BigDecimal} ranges
	 */
	@Test
	public void testDecimalRange()
	{
		assertEquals(Arrays.asList(new BigDecimal(1),
								   new BigDecimal(2),
								   new BigDecimal(3),
								   new BigDecimal(4),
								   new BigDecimal(5)),
					 Range.from(new BigDecimal(1))
					 .to(new BigDecimal(5))
					 .toList());
		assertEquals(Arrays.asList(new BigDecimal(10),
								   new BigDecimal(8),
								   new BigDecimal(6),
								   new BigDecimal(4),
								   new BigDecimal(2),
								   BigDecimal.ZERO),
					 Range.from(new BigDecimal(10))
					 .to(new BigDecimal(0))
					 .step(new BigDecimal(2))
					 .toList());
		assertEquals(Arrays.asList(new BigDecimal(1),
								   new BigDecimal("1.1"),
								   new BigDecimal("1.2"),
								   new BigDecimal("1.3"),
								   new BigDecimal("1.4")),
					 Range.from(BigDecimal.ONE)
					 .to(new BigDecimal("1.4"))
					 .step(new BigDecimal("0.1"))
					 .toList());
	}

	/***************************************
	 * Test of {@link Range#equals(Object)} and {@link Range#hashCode()}
	 */
	@Test
	public void testEqualsAndHashCode()
	{
		Range<Integer> r1 = Range.from(1).to(5);
		Range<Integer> r2 = Range.from(1).to(5);
		Range<Integer> r3 = Range.from(0).to(4);

		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
		assertNotEquals(r1, r3);
		assertNotEquals(r1.hashCode(), r3.hashCode());
	}

	/***************************************
	 * Test of error conditions
	 */
	@Test
	public void testErrors()
	{
		try
		{
			Range.from(null);
			fail();
		}
		catch (Exception e)
		{
			// expected
		}

		try
		{
			Range.from(1).size();
			fail();
		}
		catch (Exception e)
		{
			// expected
		}

		try
		{
			Range.from(1).to(null);
			fail();
		}
		catch (Exception e)
		{
			// expected
		}

		try
		{
			Range.from(1).to(2).to(3);
			fail();
		}
		catch (Exception e)
		{
			// expected
		}

		try
		{
			Range.from(1).to(2).step(null);
			fail();
		}
		catch (Exception e)
		{
			// expected
		}

		try
		{
			Range.from(1).to(2).step(-1);
			fail();
		}
		catch (Exception e)
		{
			// expected
		}
	}

	/***************************************
	 * Test of float value ranges
	 */
	@Test
	public void testFloatRange()
	{
		assertEquals(Arrays.asList(1.0, 1.25, 1.50, 1.75, 2.0),
					 Range.from(1.0).to(2.0).step(0.25).toList());
	}

	/***************************************
	 * Test of {@link Range#forEach(java.util.function.Consumer)}
	 */
	@Test
	public void testForEach()
	{
		checkRangeForEach(0, 5, 1);
		checkRangeForEach(2, 5, 1);
		checkRangeForEach(5, 0, 1);
		checkRangeForEach(5, 2, 1);
		checkRangeForEach(-5, 0, 1);
		checkRangeForEach(5, -5, 1);
	}

	/***************************************
	 * Test of {@link Range#stream()}
	 */
	@Test
	public void testStream()
	{
		List<Integer> l =
			Range.from(1).to(10).stream().collect(Collectors.toList());

		assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), l);

		l = Range.from(1)
				 .to(10)
				 .stream()
				 .filter(i -> i % 2 == 0)
				 .collect(Collectors.toList());

		assertEquals(Arrays.asList(2, 4, 6, 8, 10), l);

		l = Range.from(-2).to(2).stream().collect(Collectors.toList());
		assertEquals(Arrays.asList(-2, -1, 0, 1, 2), l);

		l = Range.from(2).to(-2).stream().collect(Collectors.toList());
		assertEquals(Arrays.asList(2, 1, 0, -1, -2), l);
	}

	/***************************************
	 * Checks the contents of integer ranges.
	 *
	 * @param nStart
	 * @param nEnd
	 */
	private void checkRangeContains(int nStart, int nEnd)
	{
		Range<Integer> r = Range.from(nStart).to(nEnd);

		for (int i = nStart; i <= nEnd; i++)
		{
			assertTrue(r.contains(i));
		}

		assertFalse(r.contains(nStart - r.getStep()));
		assertFalse(r.contains(nEnd + r.getStep()));
	}

	/***************************************
	 * Checks {@link Range#forEach(java.util.function.Consumer)}.
	 *
	 * @param nStart
	 * @param nEnd
	 * @param nStep
	 */
	private void checkRangeForEach(int nStart, int nEnd, int nStep)
	{
		nDiff = 0;

		Range<Integer> r = Range.from(nStart).to(nEnd);

		r.forEach(i ->
	  			{
	  				assertEquals(i.intValue(), nStart + nDiff);
	  				nDiff += r.getStep();
				  });
	}
}
