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

import de.esoco.lib.collection.CollectionUtil;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.esoco.lib.datatype.Pair.t;


/********************************************************************
 * A generic implementation for iterable ranges of comparable numeric values.
 * Ranges consist of a start and an end value with a certain step size to
 * proceed for the increment or decrement of the current value during iteration.
 * Both the start and end value are inclusive and therefore a range always
 * contains at least one value.
 *
 * @author eso
 */
public class Range<T extends Comparable<T>> implements Iterable<T>
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long END_EXCLUSIVE = Long.MIN_VALUE + 1;

	private static final Map<Class<?>, Comparable<?>> DEFAULT_STEPS =
		CollectionUtil.fixedMapOf(t(Long.class, Long.valueOf(1)),
								  t(Integer.class, Integer.valueOf(1)),
								  t(Short.class, Short.valueOf((short) 1)),
								  t(Byte.class, Byte.valueOf((byte) 1)),
								  t(BigInteger.class, BigInteger.ONE),
								  t(BigDecimal.class, BigDecimal.ONE),
								  t(Double.class, Double.valueOf(1)),
								  t(Float.class, Float.valueOf(1)),
								  t(Character.class,
									Character.valueOf('\u0001')));

	private static final Map<Class<?>, Comparable<?>> ZERO_VALUES =
		CollectionUtil.fixedMapOf(t(Long.class, Long.valueOf(0)),
								  t(Integer.class, Integer.valueOf(0)),
								  t(Short.class, Short.valueOf((short) 0)),
								  t(Byte.class, Byte.valueOf((byte) 0)),
								  t(BigInteger.class, BigInteger.ZERO),
								  t(BigDecimal.class, BigDecimal.ZERO),
								  t(Double.class, Double.valueOf(0)),
								  t(Float.class, Float.valueOf(0)),
								  t(Character.class,
									Character.valueOf('\u0000')));

	//~ Instance fields --------------------------------------------------------

	private final T aStart;
	private T	    aEnd	   = null;
	private T	    aStep	   = null;
	private long    nSize	   = Long.MIN_VALUE;
	private boolean bAscending;

	private BiFunction<T, T, T> fGetNextValue;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Internal constructor to creates a new instance. Use the factory method
	 * {@link #from(Comparable)} to create new ranges.
	 *
	 * @param rStart The starting value of this range (inclusive)
	 */
	private Range(T rStart)
	{
		this.aStart = rStart;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Creates a new range with a certain start value.
	 *
	 * @param  rStart The start of the range
	 *
	 * @return The new range
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> Range<T> from(T rStart)
	{
		Objects.requireNonNull(rStart, "Start value must not be NULL");

		Class<T> rRangeType = (Class<T>) rStart.getClass();
		Range<T> aRange     = new Range<T>(rStart);

		if (rRangeType == Long.class)
		{
			aRange.fGetNextValue =
				(rCurrent, rStep) ->
					(T) Long.valueOf(((Long) rCurrent).longValue() +
									 ((Long) rStep).longValue());
		}
		else if (rRangeType == Integer.class)
		{
			aRange.fGetNextValue =
				(rCurrent, rStep) ->
					(T) Integer.valueOf(((Integer) rCurrent).intValue() +
										((Integer) rStep).intValue());
		}
		else if (rRangeType == Short.class)
		{
			aRange.fGetNextValue =
				(rCurrent, rStep) ->
					(T) Short.valueOf((short) (((Short) rCurrent).shortValue() +
											   ((Short) rStep).shortValue()));
		}
		else if (rRangeType == Byte.class)
		{
			aRange.fGetNextValue =
				(rCurrent, rStep) ->
					(T) Byte.valueOf((byte) (((Byte) rCurrent).byteValue() +
											 ((Byte) rStep).byteValue()));
		}
		else if (rRangeType == BigInteger.class)
		{
			aRange.fGetNextValue =
				(rCurrent, rStep) ->
					(T) ((BigInteger) rCurrent).add((BigInteger) rStep);
		}
		else if (rRangeType == BigDecimal.class)
		{
			aRange.fGetNextValue =
				(rCurrent, rStep) ->
					(T) ((BigDecimal) rCurrent).add((BigDecimal) rStep);
		}
		else if (rRangeType == Double.class)
		{
			aRange.fGetNextValue =
				(rCurrent, rStep) ->
					(T) Double.valueOf(((Double) rCurrent).doubleValue() +
									   ((Double) rStep).doubleValue());
		}
		else if (rRangeType == Float.class)
		{
			aRange.fGetNextValue =
				(rCurrent, rStep) ->
					(T) Float.valueOf(((Float) rCurrent).floatValue() +
									  ((Float) rStep).floatValue());
		}
		else if (rRangeType == Character.class)
		{
			aRange.fGetNextValue =
				(rCurrent, rStep) ->
				{
					int nStep = ((Character) rStep).charValue();

					return (T) Character.valueOf((char) (((Character) rCurrent)
														 .charValue() +
														 (aRange.bAscending
														  ? nStep : -nStep)));
				};
		}

		return aRange;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Checks whether a certain value is contained in this range.
	 *
	 * @param  rValue The value to check
	 *
	 * @return TRUE if the value is contained in this range
	 */
	@SuppressWarnings("unchecked")
	public boolean contains(T rValue)
	{
		checkInitialized();

		return bAscending
			   ? rValue.compareTo(aStart) >= 0 && rValue.compareTo(aEnd) <= 0
			   : rValue.compareTo(aEnd) >= 0 && rValue.compareTo(aStart) <= 0;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object rObj)
	{
		checkInitialized();

		if (this == rObj)
		{
			return true;
		}

		if (rObj == null || getClass() != rObj.getClass())
		{
			return false;
		}

		Range<?> rOther = (Range<?>) rObj;

		rOther.checkInitialized();

		return aStart.equals(rOther.aStart) &&
			   Objects.equals(aEnd, rOther.aEnd) &&
			   Objects.equals(aStep, rOther.aStep);
	}

	/***************************************
	 * Returns the last value of this range. If the range has been defined with
	 * {@link #until(Comparable)} the value returned by this method will be last
	 * value before the exclusive end based on the step size.
	 *
	 * @return The last value
	 */
	public T getEnd()
	{
		checkInitialized();

		return aEnd;
	}

	/***************************************
	 * Returns the start value of this range.
	 *
	 * @return The first value
	 */
	public T getStart()
	{
		return aStart;
	}

	/***************************************
	 * Returns the step size for iterating over this range. The sign of the
	 * returned value will reflect the iteration direction (positive if
	 * iterating from lower to higher values, negative for the the other way).
	 *
	 * @return The step size
	 */
	public T getStep()
	{
		checkInitialized();

		return aStep;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		checkInitialized();

		final int nPrime = 31;
		int		  nHash  = 1;

		nHash = nPrime * nHash + aStart.hashCode();
		nHash = nPrime * nHash + aEnd.hashCode();
		nHash = nPrime * nHash + aStep.hashCode();

		return nHash;
	}

	/***************************************
	 * Checks the range direction.
	 *
	 * @return TRUE if the range values are ordered ascending, FALSE if ordered
	 *         descending
	 */
	public boolean isAscending()
	{
		return bAscending;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<T> iterator()
	{
		checkInitialized();

		return new RangeIterator();
	}

	/***************************************
	 * Returns the size of this range.
	 *
	 * @return The range size
	 */
	public long size()
	{
		checkInitialized();

		return nSize;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public Spliterator<T> spliterator()
	{
		return Spliterators.spliterator(iterator(),
										size(),
										Spliterator.DISTINCT |
										Spliterator.IMMUTABLE |
										Spliterator.NONNULL |
										Spliterator.ORDERED |
										Spliterator.SORTED | Spliterator.SIZED |
										Spliterator.SUBSIZED);
	}

	/***************************************
	 * Sets the step size for iteration through this range. The step must always
	 * be a positive value. If the range end is lower than the start the step
	 * will be automatically applied by subtraction.
	 *
	 * <p>This method can only be invoked once and afterwards this range is
	 * effectively immutable. If no explicit value is given a default step size
	 * of 1 (one) will be used.</p>
	 *
	 * @param  rStep The step size
	 *
	 * @return This instance for fluent invocations
	 *
	 * @throws IllegalArgumentException If the step value is invalid or has
	 *                                  already been set
	 */
	@SuppressWarnings("unchecked")
	public Range<T> step(T rStep)
	{
		Objects.requireNonNull(rStep, "Range step must not be NULL");

		if (this.aStep != null)
		{
			throw new IllegalArgumentException("Range step already set to " +
											   this.aStep);
		}

		if (rStep.compareTo((T) ZERO_VALUES.get(aStart.getClass())) <= 0)
		{
			throw new IllegalArgumentException("Step must be a positive number");
		}

		this.aStep = rStep;

		return this;
	}

	/***************************************
	 * Returns a stream of the values in this range.
	 *
	 * @return The stream of range values
	 */
	public Stream<T> stream()
	{
		return StreamSupport.stream(spliterator(), false);
	}

	/***************************************
	 * Sets the inclusive end value of this range. This method can only be
	 * invoked once and afterwards this range is effectively immutable.
	 *
	 * @param  rEnd The end value (inclusive)
	 *
	 * @return This instance for fluent invocations
	 *
	 * @throws IllegalArgumentException If the end value has already been set
	 */
	public Range<T> to(T rEnd)
	{
		Objects.requireNonNull(rEnd, "End value must not be NULL");

		if (this.aEnd != null)
		{
			throw new IllegalArgumentException("Range end already set to " +
											   this.aEnd);
		}

		this.aEnd = rEnd;

		bAscending = aStart.compareTo(aEnd) <= 0;

		return this;
	}

	/***************************************
	 * Returns a list that contains all elements of this range.
	 *
	 * @return A new list containing the range elements
	 */
	public List<T> toList()
	{
		return stream().collect(Collectors.toList());
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		checkInitialized();

		StringBuilder aResult = new StringBuilder(aStart.toString());

		aResult.append("..");
		aResult.append(aEnd);

		if (aStep != DEFAULT_STEPS.get(aStart.getClass()))
		{
			aResult.append(" step ").append(aStep);
		}

		return aResult.toString();
	}

	/***************************************
	 * Sets the exclusive end value of this range. This method can only be
	 * invoked once and afterwards this range is effectively immutable. The
	 * value returned by {@link #getEnd()} will be the argument of this method
	 * incremented or decremented by the range step depending on the range
	 * direction.
	 *
	 * @param  rBefore The end value (exclusive)
	 *
	 * @return This instance for fluent invocations
	 *
	 * @throws IllegalArgumentException If the end value has already been set
	 */
	public Range<T> until(T rBefore)
	{
		Range<T> rThis = to(rBefore);

		// internal signal for an exclusive end value
		nSize = END_EXCLUSIVE;

		return rThis;
	}

	/***************************************
	 * Must be implemented to return the next value in the range.
	 *
	 * @param  rCurrent The current value to calculate the next value from
	 * @param  rStep    The step size to calculate the next value with
	 *
	 * @return The next value
	 */
	protected T getNextValue(T rCurrent, T rStep)
	{
		return fGetNextValue.apply(rCurrent, rStep);
	}

	/***************************************
	 * Internal method to calculate the size of this range.
	 */
	private void calcSize()
	{
		if (aEnd != null && aStep != null && nSize <= END_EXCLUSIVE)
		{
			if (!bAscending)
			{
				aStep = negate(aStep);
			}

			if (nSize == END_EXCLUSIVE)
			{
				// unsigned types like Character calculate next value based on
				// the direction, therefore toggle bUpwards temporarily
				bAscending = !bAscending;
				aEnd	   = fGetNextValue.apply(aEnd, negate(aStep));
				bAscending = !bAscending;
			}

			@SuppressWarnings("unchecked")
			Class<T> rRangeType = (Class<T>) aEnd.getClass();

			if (rRangeType == BigDecimal.class)
			{
				nSize =
					((BigDecimal) aEnd).subtract((BigDecimal) aStart)
									   .add((BigDecimal) aStep)
									   .divide((BigDecimal) aStep)
									   .longValue();
			}
			else if (rRangeType == BigInteger.class)
			{
				nSize =
					((BigInteger) aEnd).subtract((BigInteger) aStart)
									   .add((BigInteger) aStep)
									   .divide((BigInteger) aStep)
									   .longValue();
			}
			else if (Number.class.isAssignableFrom(rRangeType))
			{
				if (rRangeType == Double.class || rRangeType == Float.class)
				{
					double fStep = ((Number) aStep).doubleValue();

					nSize =
						(long) ((((Number) aEnd).doubleValue() -
								 ((Number) aStart).doubleValue() + fStep) /
								fStep);
				}
				else
				{
					long nStep = ((Number) aStep).longValue();

					nSize =
						(((Number) aEnd).longValue() -
						 ((Number) aStart).longValue() + nStep) / nStep;
				}
			}
			else if (rRangeType == Character.class)
			{
				int nStep = ((Character) aStep).charValue();

				if (!bAscending)
				{
					nStep = -nStep;
				}

				nSize =
					(((Character) aEnd).charValue() -
					 ((Character) aStart).charValue() + nStep) / nStep;
			}
		}
	}

	/***************************************
	 * Checks whether this instance has been fully initialized or else throws an
	 * {@link IllegalStateException}.
	 */
	@SuppressWarnings("unchecked")
	private void checkInitialized()
	{
		if (aEnd == null)
		{
			throw new IllegalStateException("Range end has not been set");
		}

		if (aStep == null)
		{
			step((T) DEFAULT_STEPS.get(aStart.getClass()));

			if (aStep == null)
			{
				throw new IllegalArgumentException("No range mapping for type " +
												   aStart.getClass());
			}
		}

		calcSize();
	}

	/***************************************
	 * Returns the negated value of the argument.
	 *
	 * @param  rValue The value to negate
	 *
	 * @return The negated value
	 */
	@SuppressWarnings("unchecked")
	private T negate(T rValue)
	{
		Class<T> rValueType = (Class<T>) rValue.getClass();

		if (rValueType == Integer.class)
		{
			rValue = (T) Integer.valueOf(-((Integer) rValue).intValue());
		}
		else if (rValueType == Long.class)
		{
			rValue = (T) Long.valueOf(-((Long) rValue).longValue());
		}
		else if (rValueType == Short.class)
		{
			rValue = (T) Short.valueOf((short) -((Short) rValue).shortValue());
		}
		else if (rValueType == Byte.class)
		{
			rValue = (T) Byte.valueOf((byte) -((Byte) rValue).byteValue());
		}
		else if (rValueType == BigDecimal.class)
		{
			rValue = (T) ((BigDecimal) rValue).negate();
		}
		else if (rValueType == BigInteger.class)
		{
			rValue = (T) ((BigInteger) rValue).negate();
		}
		else if (rValueType == Double.class)
		{
			rValue = (T) Double.valueOf(-((Double) rValue).doubleValue());
		}
		else if (rValueType == Float.class)
		{
			rValue = (T) Float.valueOf(-((Float) rValue).floatValue());
		}
		else if (rValueType == Character.class)
		{
			// characters are unsigned, therefore this must be handled by
			// the next value function based on the upwards flag
		}

		return rValue;
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * The iterator implementation for ranges.
	 *
	 * @author eso
	 */
	class RangeIterator implements Iterator<T>
	{
		//~ Instance fields ----------------------------------------------------

		private T rNext;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 */
		public RangeIterator()
		{
			rNext = aStart;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext()
		{
			return rNext != null;
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public T next()
		{
			T rCurrent = rNext;

			rNext = getNextValue(rCurrent, aStep);

			int nNextCompared = rNext.compareTo(aEnd);

			if (bAscending && nNextCompared > 0 ||
				!bAscending && nNextCompared < 0)
			{
				rNext = null;
			}

			return rCurrent;
		}
	}
}
