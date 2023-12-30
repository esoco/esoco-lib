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

		private final Period period;

		private final int fieldAddition;

		/**
		 * Creates a new instance.
		 *
		 * @param period        The period of this standard date range
		 * @param fieldAddition The value to add to the calendar field
		 */
		StandardDateRange(Period period, int fieldAddition) {
			this.period = period;
			this.fieldAddition = fieldAddition;
		}

		/**
		 * Returns the value that will be added to the corresponding calendar
		 * field for this standard range.
		 *
		 * @return The calendar field addition
		 */
		public final int getFieldAddition() {
			return fieldAddition;
		}

		/**
		 * Returns the period of this standard range.
		 *
		 * @return The period
		 */
		public final Period getPeriod() {
			return period;
		}
	}

	static final long serialVersionUID = 3730180974383009738L;

	private final long start;

	private final long end;

	// Lazily initialized variable; volatile to ensure thread safety
	private volatile int hashCode = 0;

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
	 * @param start The start date of the range (inclusive)
	 * @param end   The end date of the range (inclusive) or NULL for no end
	 */
	public DateRange(Date start, Date end) {
		this(start.getTime(), (end != null) ? end.getTime() : Long.MAX_VALUE);
	}

	/**
	 * Creates a new DateRange object that is valid from a certain start date
	 * until a certain end date, defined as java.util.Calendar instances. If
	 * the
	 * end date is NULL the range will be open, i.e. the method isValid() will
	 * not check against an end date.
	 *
	 * @param start A Calendar instance containing the start date of the range
	 *              (inclusive)
	 * @param end   A Calendar instance containing the end date of the range
	 *              (inclusive) or NULL for no end
	 * @see #DateRange(Date, Date)
	 */
	public DateRange(Calendar start, Calendar end) {
		this(start.getTimeInMillis(),
			(end != null) ? end.getTimeInMillis() : Long.MAX_VALUE);
	}

	/**
	 * Creates a new instance from {@link Instant Instants}.
	 *
	 * @param start The start instant
	 * @param end   The end instant (exclusive)
	 */
	public DateRange(Instant start, Instant end) {
		this(start.toEpochMilli(),
			(end != null) ? end.toEpochMilli() : Long.MAX_VALUE);
	}

	/**
	 * Creates a new DateRange object from the milliseconds of it's start and
	 * end dates. The milliseconds are counted from the start date January 1,
	 * 1970, 00:00:00 GMT.
	 *
	 * @param startMillis The milliseconds of the start time
	 * @param endMillis   The milliseconds of the end time
	 * @throws IllegalArgumentException If start &gt; end
	 */
	public DateRange(long startMillis, long endMillis) {
		if (startMillis > endMillis) {
			throw new IllegalArgumentException(
				String.format("Start > End: %s > %s", new Date(startMillis),
					new Date(endMillis)));
		}

		start = startMillis;
		end = endMillis;
	}

	/**
	 * Calculates a date range for a standard date range relative to the
	 * current
	 * date.
	 *
	 * @param standardRange The standard date range definition
	 * @return The resulting date range
	 */
	public static DateRange calculateFor(StandardDateRange standardRange) {
		return calculateFor(new Date(), standardRange);
	}

	/**
	 * Calculates a date range for a standard date range relative to a given
	 * date.
	 *
	 * @param date          The date to calculate the standard date range for
	 * @param standardRange The standard date range definition
	 * @return The resulting date range
	 */
	public static DateRange calculateFor(Date date,
		StandardDateRange standardRange) {
		if (standardRange.fieldAddition != 0) {
			Calendar rangeDate = Calendar.getInstance();

			rangeDate.setTime(date);
			rangeDate.add(standardRange.period.getUnit().getCalendarField(),
				standardRange.fieldAddition);
			date = rangeDate.getTime();
		}

		return calculateFor(date, standardRange.period);
	}

	/**
	 * Calculates a date range for a certain period relative to a given date.
	 *
	 * @param date   The date to place the date range around
	 * @param period The period to calculate
	 * @return The resulting date range
	 */
	public static DateRange calculateFor(Date date, Period period) {
		Calendar start = Calendar.getInstance();
		Calendar end = Calendar.getInstance();
		int field = period.getUnit().getCalendarField();
		int rangeSize = period.getCount();

		start.setTime(date);

		int rangeStart = start.get(field);

		if (rangeSize > 1) {
			boolean zeroBased = CalendarFunctions.isZeroBased(field);

			if (!zeroBased) {
				rangeStart -= 1;
			}

			rangeStart = rangeStart / rangeSize * rangeSize;

			if (!zeroBased) {
				rangeStart += 1;
			}
		}

		start.set(field, rangeStart);

		if (field == Calendar.WEEK_OF_YEAR) {
			CalendarFunctions.resetBelow(Calendar.DAY_OF_MONTH, start, false);
			start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		} else {
			CalendarFunctions.resetBelow(field, start, false);
		}

		end.setTime(start.getTime());
		end.add(field, rangeSize);

		return new DateRange(start, end);
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
	 * @param other The other DateRange object to compare with
	 * @see Comparable#compareTo(Object)
	 */
	@Override
	public int compareTo(DateRange other) {
		if (end != other.end) {
			return (end < other.end) ? -1 : 1;
		}

		if (start != other.start) {
			return (start < other.start) ? -1 : 1;
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
	 * @param date The date to test against this range
	 * @return TRUE, if start &lt;= date &lt;= end
	 */
	public boolean contains(Date date) {
		long t = date.getTime();

		return start <= t && t < end;
	}

	/**
	 * To check if this date range completely contains another range. Returns
	 * TRUE if the arguments start and end dates lie between this range's start
	 * and end dates. The end date is exclusive, i.e. it will be 1 millisecond
	 * AFTER the end of the date range and on the first millisecond of the
	 * adjacent range.
	 *
	 * @param range The range to test against this range
	 * @return TRUE, if start &lt;= range.start and end &gt;= range.end
	 */
	public boolean contains(DateRange range) {
		return range.start >= start && range.end <= end;
	}

	/**
	 * Test for equality with another object. Returns true if the argument is
	 * also a DateRange object and compareTo(obj) returns 0.
	 *
	 * @param obj The object to compare with for equality
	 * @return TRUE if the objects are equal
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof DateRange) {
			return compareTo((DateRange) obj) == 0;
		}

		return false;
	}

	/**
	 * Returns the end date of the date range.
	 *
	 * @return A new instance of java.util.Date containing the end date
	 */
	public Date getEnd() {
		return new Date(end);
	}

	/**
	 * Returns the start date of the date range.
	 *
	 * @return A new instance of java.util.Date containing the start date
	 */
	public Date getStart() {
		return new Date(start);
	}

	/**
	 * Calculates and returns the hash code for this range.
	 *
	 * @return A hash code based on the internal start and end date values
	 */
	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = (int) (((37L + start) * 37L) + end);
		}

		return hashCode;
	}

	/**
	 * To check if this date range overlaps with another range. Returns TRUE if
	 * at least either the arguments start or end date lies between this
	 * range's
	 * start and end dates. The end date is exclusive, i.e. it will be 1
	 * millisecond AFTER the end of the date range and on the first millisecond
	 * of the adjacent range.
	 *
	 * @param other The range to test against this range
	 * @return TRUE, if NOT (range.end &lt; start OR range.start &gt; end)
	 * @see #contains(DateRange)
	 */
	public boolean overlaps(DateRange other) {
		return other.end > start && other.start < end;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("DateRange[%s - %s]", getStart(), getEnd());
	}
}
