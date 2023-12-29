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

import de.esoco.lib.datatype.Period.Unit;
import de.esoco.lib.expression.function.CalendarFunctions;

import java.io.Serializable;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

/**
 * Immutable datatype that describes date ranges. It is encoded with two dates
 * that contain the start and end of the date range. The end date will be
 * exclusive, i.e. it will be 1 millisecond AFTER the end of the date range and
 * on the first millisecond of the adjacent range.
 *
 * <p>Date ranges are comparable based on their position in time (not their
 * size in milliseconds). A range is considered to be "larger" if it's end date
 * is after that of another range and "smaller" if the end date is before that
 * of the other range. If the end dates are equal the start dates are consider
 * accordingly. That also means that a range that is fully contained in another
 * is always considered smaller. Comparing may not be sufficient for all cases
 * so application code should also use the methods {@link #contains(DateRange)}
 * and {@link #overlaps(DateRange)} for final decisions if necessary.</p>
 *
 * @author eso
 */
public class DateRange implements Comparable<DateRange>, Serializable {

	/**
	 * An enumeration of typical date range types for date calculations and
	 * display in user interfaces. The value {@link #NONE} has only a
	 * declarative purpose (e.g. for UI selection) and must not be used as an
	 * argument to methods like
	 * {@link DateRange#calculateFor(StandardDateRange)}.
	 */
	public enum StandardDateRange {
		NONE(Period.NONE, 0), LAST_HOUR(Period.HOURLY, -1),
		CURRENT_HOUR(Period.HOURLY, 0), BEFORE_YESTERDAY(Period.DAYLY, -2),
		YESTERDAY(Period.DAYLY, -1), TODAY(Period.DAYLY, 0),
		LAST_WEEK(Period.WEEKLY, -1), CURRENT_WEEK(Period.WEEKLY, 0),
		LAST_FORTNIGHT(new Period(2, Unit.WEEK), -1),
		CURRENT_FORTNIGHT(new Period(2, Unit.WEEK), 0),
		LAST_MONTH(Period.MONTHLY, -1), CURRENT_MONTH(Period.MONTHLY, 0),
		LAST_QUARTER(Period.QUARTERLY, -1),
		CURRENT_QUARTER(Period.QUARTERLY, 0),
		LAST_HALF_YEAR(Period.HALF_YEARLY, -1),
		CURRENT_HALF_YEAR(Period.HALF_YEARLY, 0), LAST_YEAR(Period.YEARLY, -1),
		CURRENT_YEAR(Period.YEARLY, 0);

		private final Period rPeriod;

		private final int nFieldAddition;

		/**
		 * Creates a new instance.
		 *
		 * @param rPeriod        The period of this standard date range
		 * @param nFieldAddition The value to add to the calendar field
		 */
		StandardDateRange(Period rPeriod, int nFieldAddition) {
			this.rPeriod = rPeriod;
			this.nFieldAddition = nFieldAddition;
		}

		/**
		 * Returns the value that will be added to the corresponding calendar
		 * field for this standard range.
		 *
		 * @return The calendar field addition
		 */
		public final int getFieldAddition() {
			return nFieldAddition;
		}

		/**
		 * Returns the period of this standard range.
		 *
		 * @return The period
		 */
		public final Period getPeriod() {
			return rPeriod;
		}
	}

	static final long serialVersionUID = 3730180974383009738L;

	private final long nStart;

	private final long nEnd;

	// Lazily initialized variable; volatile to ensure thread safety
	private volatile int nHashCode = 0;

	/**
	 * Creates a new DateRange object that is valid from a certain start date
	 * until a certain end date. If the end date is NULL the range will be
	 * open,
	 * i.e. the method isValid() will not check against an end date.
	 *
	 * <p>This constructor makes defensive copies of the mutable Date arguments
	 * so that later changes to these objects won't affect the internal
	 * state of
	 * this instance.</p>
	 *
	 * @param rStart The start date of the range (inclusive)
	 * @param rEnd   The end date of the range (inclusive) or NULL for no end
	 */
	public DateRange(Date rStart, Date rEnd) {
		this(rStart.getTime(),
			(rEnd != null) ? rEnd.getTime() : Long.MAX_VALUE);
	}

	/**
	 * Creates a new DateRange object that is valid from a certain start date
	 * until a certain end date, defined as java.util.Calendar instances. If
	 * the
	 * end date is NULL the range will be open, i.e. the method isValid() will
	 * not check against an end date.
	 *
	 * @param rStart A Calendar instance containing the start date of the range
	 *               (inclusive)
	 * @param rEnd   A Calendar instance containing the end date of the range
	 *               (inclusive) or NULL for no end
	 * @see #DateRange(Date, Date)
	 */
	public DateRange(Calendar rStart, Calendar rEnd) {
		this(rStart.getTimeInMillis(),
			(rEnd != null) ? rEnd.getTimeInMillis() : Long.MAX_VALUE);
	}

	/**
	 * Creates a new instance from {@link Instant Instants}.
	 *
	 * @param rStart The start instant
	 * @param rEnd   The end instant (exclusive)
	 */
	public DateRange(Instant rStart, Instant rEnd) {
		this(rStart.toEpochMilli(),
			(rEnd != null) ? rEnd.toEpochMilli() : Long.MAX_VALUE);
	}

	/**
	 * Creates a new DateRange object from the milliseconds of it's start and
	 * end dates. The milliseconds are counted from the start date January 1,
	 * 1970, 00:00:00 GMT.
	 *
	 * @param nStartMillis The milliseconds of the start time
	 * @param nEndMillis   The milliseconds of the end time
	 * @throws IllegalArgumentException If start &gt; end
	 */
	public DateRange(long nStartMillis, long nEndMillis) {
		if (nStartMillis > nEndMillis) {
			throw new IllegalArgumentException(
				String.format("Start > End: %s > %s", new Date(nStartMillis),
					new Date(nEndMillis)));
		}

		nStart = nStartMillis;
		nEnd = nEndMillis;
	}

	/**
	 * Calculates a date range for a standard date range relative to the
	 * current
	 * date.
	 *
	 * @param eStandardRange The standard date range definition
	 * @return The resulting date range
	 */
	public static DateRange calculateFor(StandardDateRange eStandardRange) {
		return calculateFor(new Date(), eStandardRange);
	}

	/**
	 * Calculates a date range for a standard date range relative to a given
	 * date.
	 *
	 * @param rDate          The date to calculate the standard date range for
	 * @param eStandardRange The standard date range definition
	 * @return The resulting date range
	 */
	public static DateRange calculateFor(Date rDate,
		StandardDateRange eStandardRange) {
		if (eStandardRange.nFieldAddition != 0) {
			Calendar aRangeDate = Calendar.getInstance();

			aRangeDate.setTime(rDate);
			aRangeDate.add(eStandardRange.rPeriod.getUnit().getCalendarField(),
				eStandardRange.nFieldAddition);
			rDate = aRangeDate.getTime();
		}

		return calculateFor(rDate, eStandardRange.rPeriod);
	}

	/**
	 * Calculates a date range for a certain period relative to a given date.
	 *
	 * @param rDate   The date to place the date range around
	 * @param rPeriod The period to calculate
	 * @return The resulting date range
	 */
	public static DateRange calculateFor(Date rDate, Period rPeriod) {
		Calendar aStart = Calendar.getInstance();
		Calendar aEnd = Calendar.getInstance();
		int nField = rPeriod.getUnit().getCalendarField();
		int nRangeSize = rPeriod.getCount();

		aStart.setTime(rDate);

		int nRangeStart = aStart.get(nField);

		if (nRangeSize > 1) {
			boolean bZeroBased = CalendarFunctions.isZeroBased(nField);

			if (!bZeroBased) {
				nRangeStart -= 1;
			}

			nRangeStart = nRangeStart / nRangeSize * nRangeSize;

			if (!bZeroBased) {
				nRangeStart += 1;
			}
		}

		aStart.set(nField, nRangeStart);

		if (nField == Calendar.WEEK_OF_YEAR) {
			CalendarFunctions.resetBelow(Calendar.DAY_OF_MONTH, aStart, false);
			aStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		} else {
			CalendarFunctions.resetBelow(nField, aStart, false);
		}

		aEnd.setTime(aStart.getTime());
		aEnd.add(nField, nRangeSize);

		return new DateRange(aStart, aEnd);
	}

	/**
	 * Compares this date ranges with another. The comparison is relative to
	 * the
	 * position in time, not to the "size" of the date range. The algorithm
	 * used
	 * is:
	 *
	 * <ol>
	 *   <li>If the end dates are not equal, return the comparison of the end
	 *     dates</li>
	 *   <li>Else return the comparison of the start dates</li>
	 * </ol>
	 *
	 * @param rOther The other DateRange object to compare with
	 * @see Comparable#compareTo(Object)
	 */
	@Override
	public int compareTo(DateRange rOther) {
		if (nEnd != rOther.nEnd) {
			return (nEnd < rOther.nEnd) ? -1 : 1;
		}

		if (nStart != rOther.nStart) {
			return (nStart < rOther.nStart) ? -1 : 1;
		}

		return 0;
	}

	/**
	 * To check if this date range contains a certain date. Returns TRUE if the
	 * argument lies between this range's start and end dates. The end date is
	 * exclusive, i.e. it will be 1 millisecond AFTER the actual end of the
	 * date
	 * range and on the first millisecond of the adjacent range.
	 *
	 * @param rDate The date to test against this range
	 * @return TRUE, if start &lt;= rDate &lt;= end
	 */
	public boolean contains(Date rDate) {
		long t = rDate.getTime();

		return nStart <= t && t < nEnd;
	}

	/**
	 * To check if this date range completely contains another range. Returns
	 * TRUE if the arguments start and end dates lie between this range's start
	 * and end dates. The end date is exclusive, i.e. it will be 1 millisecond
	 * AFTER the end of the date range and on the first millisecond of the
	 * adjacent range.
	 *
	 * @param rRange The range to test against this range
	 * @return TRUE, if start &lt;= rRange.start and end &gt;= rRange.end
	 */
	public boolean contains(DateRange rRange) {
		return rRange.nStart >= nStart && rRange.nEnd <= nEnd;
	}

	/**
	 * Test for equality with another object. Returns true if the argument is
	 * also a DateRange object and compareTo(rObj) returns 0.
	 *
	 * @param rObj The object to compare with for equality
	 * @return TRUE if the objects are equal
	 */
	@Override
	public boolean equals(Object rObj) {
		if (rObj == this) {
			return true;
		}

		if (rObj instanceof DateRange) {
			return compareTo((DateRange) rObj) == 0;
		}

		return false;
	}

	/**
	 * Returns the end date of the date range.
	 *
	 * @return A new instance of java.util.Date containing the end date
	 */
	public Date getEnd() {
		return new Date(nEnd);
	}

	/**
	 * Returns the start date of the date range.
	 *
	 * @return A new instance of java.util.Date containing the start date
	 */
	public Date getStart() {
		return new Date(nStart);
	}

	/**
	 * Calculates and returns the hash code for this range.
	 *
	 * @return A hash code based on the internal start and end date values
	 */
	@Override
	public int hashCode() {
		if (nHashCode == 0) {
			nHashCode = (int) (((37L + nStart) * 37L) + nEnd);
		}

		return nHashCode;
	}

	/**
	 * To check if this date range overlaps with another range. Returns TRUE if
	 * at least either the arguments start or end date lies between this
	 * range's
	 * start and end dates. The end date is exclusive, i.e. it will be 1
	 * millisecond AFTER the end of the date range and on the first millisecond
	 * of the adjacent range.
	 *
	 * @param rOther The range to test against this range
	 * @return TRUE, if NOT (rRange.end &lt; start OR rRange.start &gt; end)
	 * @see #contains(DateRange)
	 */
	public boolean overlaps(DateRange rOther) {
		return rOther.nEnd > nStart && rOther.nStart < nEnd;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("DateRange[%s - %s]", getStart(), getEnd());
	}
}
