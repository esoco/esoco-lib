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
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case for {@link DateRange}.
 *
 * @author eso
 */
public class DateRangeTest {

	/**
	 * test
	 */
	@Test
	public void testCalculateForDatePeriod() {
		DateRange range = DateRange.calculateFor(getDate(10, 1, 2000, 17, 10),
			Period.MONTHLY);

		assertEquals(getDate(1, 1, 2000, 0, 0), range.getStart());
		assertEquals(getDate(1, 2, 2000, 0, 0), range.getEnd());

		range = DateRange.calculateFor(getDate(30, 11, 2000, 22, 44),
			Period.QUARTERLY);

		assertEquals(getDate(1, 10, 2000, 0, 0), range.getStart());
		assertEquals(getDate(1, 1, 2001, 0, 0), range.getEnd());

		range =
			DateRange.calculateFor(getDate(1, 7, 2000, 1, 1), Period.YEARLY);

		assertEquals(getDate(1, 1, 2000, 0, 0), range.getStart());
		assertEquals(getDate(1, 1, 2001, 0, 0), range.getEnd());

		range =
			DateRange.calculateFor(getDate(1, 7, 2000, 9, 10), Period.HOURLY);

		assertEquals(getDate(1, 7, 2000, 9, 0), range.getStart());
		assertEquals(getDate(1, 7, 2000, 10, 0), range.getEnd());
	}

	/**
	 * test
	 */
	@Test
	public void testCalculateForDateStandardDateRange() {
		DateRange range = DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
			StandardDateRange.TODAY);

		assertEquals(getDate(1, 1, 2000, 0, 0), range.getStart());
		assertEquals(getDate(2, 1, 2000, 0, 0), range.getEnd());

		range = DateRange.calculateFor(getDate(10, 1, 2000, 17, 10),
			StandardDateRange.YESTERDAY);

		assertEquals(getDate(9, 1, 2000, 0, 0), range.getStart());
		assertEquals(getDate(10, 1, 2000, 0, 0), range.getEnd());

		range = DateRange.calculateFor(getDate(1, 2, 2000, 17, 10),
			StandardDateRange.CURRENT_QUARTER);

		assertEquals(getDate(1, 1, 2000, 0, 0), range.getStart());
		assertEquals(getDate(1, 4, 2000, 0, 0), range.getEnd());
	}

	/**
	 * test
	 */
	@Test
	public void testContainsDate() {
		DateRange range =
			DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
				Period.MONTHLY);

		assertTrue(range.contains(getDate(15, 1, 2000, 12, 0)));
		assertTrue(range.contains(getDate(1, 1, 2000, 0, 0)));
		assertTrue(range.contains(getDate(31, 1, 2000, 23, 59)));
		assertFalse(range.contains(getDate(1, 2, 2000, 0, 0)));
	}

	/**
	 * test
	 */
	@Test
	public void testContainsDateRange() {
		DateRange range = DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
			Period.QUARTERLY);

		assertTrue(range.contains(
			DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
				Period.MONTHLY)));
		assertTrue(range.contains(
			DateRange.calculateFor(getDate(31, 3, 2000, 23, 59),
				Period.MONTHLY)));
		assertTrue(range.contains(new DateRange(getDate(31, 3, 2000, 23, 59),
			getDate(1, 4, 2000, 0, 0))));
		assertFalse(range.contains(new DateRange(getDate(31, 3, 2000, 23, 59),
			getDate(1, 4, 2000, 0, 1))));
		assertFalse(range.contains(new DateRange(getDate(31, 12, 1999, 23, 59),
			getDate(1, 1, 2000, 0, 1))));
	}

	/**
	 * test
	 */
	@Test
	public void testEqualsObject() {
		DateRange range = DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
			Period.QUARTERLY);

		assertEquals(range, DateRange.calculateFor(getDate(1, 1, 2000, 0, 0),
			StandardDateRange.CURRENT_QUARTER));
		assertEquals(range, new DateRange(getDate(1, 1, 2000, 0, 0),
			getDate(1, 4, 2000, 0, 0)));

		assertNotEquals(range, new DateRange(getDate(1, 1, 2000, 0, 0),
			getDate(1, 4, 2000, 0, 1)));
	}

	/**
	 * test
	 */
	@Test
	public void testOverlaps() {
		DateRange range = DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
			Period.QUARTERLY);

		assertTrue(range.overlaps(
			DateRange.calculateFor(getDate(1, 1, 2000, 17, 10),
				Period.MONTHLY)));
		assertTrue(range.overlaps(
			DateRange.calculateFor(getDate(31, 3, 2000, 23, 59),
				Period.MONTHLY)));
		assertTrue(range.overlaps(new DateRange(getDate(31, 3, 2000, 23, 59),
			getDate(1, 4, 2000, 0, 0))));
		assertTrue(range.overlaps(new DateRange(getDate(31, 3, 2000, 23, 59),
			getDate(1, 4, 2000, 0, 1))));
		assertTrue(range.overlaps(new DateRange(getDate(31, 12, 1999, 23, 59),
			getDate(1, 1, 2000, 0, 1))));
		assertFalse(range.overlaps(new DateRange(getDate(1, 4, 2000, 0, 0),
			getDate(1, 4, 2000, 0, 1))));
		assertFalse(range.overlaps(new DateRange(getDate(31, 12, 1999, 23, 59),
			getDate(1, 1, 2000, 0, 0))));
	}

	/**
	 * Returns the test date.
	 *
	 * @param day    The test date
	 * @param month  The test date
	 * @param year   The test date
	 * @param hour   The test date
	 * @param minute The test date
	 * @return The test date
	 */
	private Date getDate(int day, int month, int year, int hour, int minute) {
		Calendar calendar = Calendar.getInstance();

		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month - 1);
		calendar.set(Calendar.DAY_OF_MONTH, day);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return calendar.getTime();
	}
}
