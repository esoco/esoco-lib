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
package de.esoco.lib.datatype;

import de.esoco.lib.datatype.DateRange.StandardDateRange;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


/********************************************************************
 * Test case for {@link DateRange}.
 *
 * @author eso
 */
public class DateRangeTest
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * test
	 */
	@Test
	public void testCalculateForDatePeriod()
	{
		DateRange aRange =
			DateRange.calculateFor(getDate(10, 1, 2000, 17, 10),
								   Period.MONTHLY);

		assertEquals(getDate(1, 1, 2000, 0, 0), aRange.getStart());
		assertEquals(getDate(1, 2, 2000, 0, 0), aRange.getEnd());

		aRange =
			DateRange.calculateFor(getDate(30, 11, 2000, 22, 44),
								   Period.QUARTERLY);

		assertEquals(getDate(1, 10, 2000, 0, 0), aRange.getStart());
		assertEquals(getDate(1, 1, 2001, 0, 0), aRange.getEnd());

		aRange =
			DateRange.calculateFor(getDate(1, 7, 2000, 1, 1), Period.YEARLY);

		assertEquals(getDate(1, 1, 2000, 0, 0), aRange.getStart());
		assertEquals(getDate(1, 1, 2001, 0, 0), aRange.getEnd());

		aRange =
			DateRange.calculateFor(getDate(1, 7, 2000, 9, 10), Period.HOURLY);

		assertEquals(getDate(1, 7, 2000, 9, 0), aRange.getStart());
		assertEquals(getDate(1, 7, 2000, 10, 0), aRange.getEnd());
	}

	/***************************************
	 * test
	 */
	@Test
	public void testCalculateForDateStandardDateRange()
	{
		DateRange aRange =
			DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
								   StandardDateRange.TODAY);

		assertEquals(getDate(1, 1, 2000, 0, 0), aRange.getStart());
		assertEquals(getDate(2, 1, 2000, 0, 0), aRange.getEnd());

		aRange =
			DateRange.calculateFor(getDate(10, 1, 2000, 17, 10),
								   StandardDateRange.YESTERDAY);

		assertEquals(getDate(9, 1, 2000, 0, 0), aRange.getStart());
		assertEquals(getDate(10, 1, 2000, 0, 0), aRange.getEnd());

		aRange =
			DateRange.calculateFor(getDate(1, 2, 2000, 17, 10),
								   StandardDateRange.CURRENT_QUARTER);

		assertEquals(getDate(1, 1, 2000, 0, 0), aRange.getStart());
		assertEquals(getDate(1, 4, 2000, 0, 0), aRange.getEnd());
	}

	/***************************************
	 * test
	 */
	@Test
	public void testContainsDate()
	{
		DateRange aRange =
			DateRange.calculateFor(getDate(1, 1, 2000, 17, 10), Period.MONTHLY);

		assertTrue(aRange.contains(getDate(15, 1, 2000, 12, 0)));
		assertTrue(aRange.contains(getDate(1, 1, 2000, 0, 0)));
		assertTrue(aRange.contains(getDate(31, 1, 2000, 23, 59)));
		assertFalse(aRange.contains(getDate(1, 2, 2000, 0, 0)));
	}

	/***************************************
	 * test
	 */
	@Test
	public void testContainsDateRange()
	{
		DateRange aRange =
			DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
								   Period.QUARTERLY);

		assertTrue(aRange.contains(DateRange.calculateFor(getDate(1,
																  1,
																  2000,
																  17,
																  10),
														  Period.MONTHLY)));
		assertTrue(aRange.contains(DateRange.calculateFor(getDate(31,
																  3,
																  2000,
																  23,
																  59),
														  Period.MONTHLY)));
		assertTrue(aRange.contains(new DateRange(getDate(31, 3, 2000, 23, 59),
												 getDate(1, 4, 2000, 0, 0))));
		assertFalse(aRange.contains(new DateRange(getDate(31, 3, 2000, 23, 59),
												  getDate(1, 4, 2000, 0, 1))));
		assertFalse(aRange.contains(new DateRange(getDate(31, 12, 1999, 23, 59),
												  getDate(1, 1, 2000, 0, 1))));
	}

	/***************************************
	 * test
	 */
	@Test
	public void testEqualsObject()
	{
		DateRange aRange =
			DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
								   Period.QUARTERLY);

		assertEquals(aRange,
					 DateRange.calculateFor(getDate(1, 1, 2000, 0, 0),
											StandardDateRange.CURRENT_QUARTER));
		assertEquals(aRange,
					 new DateRange(getDate(1, 1, 2000, 0, 0),
								   getDate(1, 4, 2000, 0, 0)));

		assertNotEquals(aRange,
						new DateRange(getDate(1, 1, 2000, 0, 0),
									  getDate(1, 4, 2000, 0, 1)));
	}

	/***************************************
	 * test
	 */
	@Test
	public void testOverlaps()
	{
		DateRange aRange =
			DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
								   Period.QUARTERLY);

		assertTrue(aRange.overlaps(DateRange.calculateFor(getDate(1,
																  1,
																  2000,
																  17,
																  10),
														  Period.MONTHLY)));
		assertTrue(aRange.overlaps(DateRange.calculateFor(getDate(31,
																  3,
																  2000,
																  23,
																  59),
														  Period.MONTHLY)));
		assertTrue(aRange.overlaps(new DateRange(getDate(31, 3, 2000, 23, 59),
												 getDate(1, 4, 2000, 0, 0))));
		assertTrue(aRange.overlaps(new DateRange(getDate(31, 3, 2000, 23, 59),
												 getDate(1, 4, 2000, 0, 1))));
		assertTrue(aRange.overlaps(new DateRange(getDate(31, 12, 1999, 23, 59),
												 getDate(1, 1, 2000, 0, 1))));
		assertFalse(aRange.overlaps(new DateRange(getDate(1, 4, 2000, 0, 0),
												  getDate(1, 4, 2000, 0, 1))));
		assertFalse(aRange.overlaps(new DateRange(getDate(31, 12, 1999, 23, 59),
												  getDate(1, 1, 2000, 0, 0))));
	}

	/***************************************
	 * Returns the test date.
	 *
	 * @param  nDay    The test date
	 * @param  nMonth  The test date
	 * @param  nYear   The test date
	 * @param  nHour   The test date
	 * @param  nMinute The test date
	 *
	 * @return The test date
	 */
	private Date getDate(int nDay,
						 int nMonth,
						 int nYear,
						 int nHour,
						 int nMinute)
	{
		Calendar aCalendar = Calendar.getInstance();

		aCalendar.set(Calendar.YEAR, nYear);
		aCalendar.set(Calendar.MONTH, nMonth - 1);
		aCalendar.set(Calendar.DAY_OF_MONTH, nDay);
		aCalendar.set(Calendar.HOUR_OF_DAY, nHour);
		aCalendar.set(Calendar.MINUTE, nMinute);
		aCalendar.set(Calendar.SECOND, 0);
		aCalendar.set(Calendar.MILLISECOND, 0);

		return aCalendar.getTime();
	}
}
