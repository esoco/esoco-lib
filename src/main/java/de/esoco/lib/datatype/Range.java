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

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/********************************************************************
 * Base class for the implementation of ranges of comparable values. Ranges
 * allow to iterate from a start to and end value. Both values are inclusive and
 * therefore a range always contains at least one value.
 *
 * @author eso
 */
public abstract class Range<T extends Comparable<T>> implements Iterable<T>
{
	//~ Instance fields --------------------------------------------------------

	private final T    rFirst;
	private final T    rLast;
	private final long nStep;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance with a default step of 1 or -1, depending on the
	 * bounds.
	 *
	 * @param rFirst The first bound of this range (inclusive)
	 * @param rLast  The last bound of this range (inclusive)
	 */
	protected Range(T rFirst, T rLast)
	{
		this(rFirst, rLast, 1);
	}

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rFirst The first bound of this range (inclusive)
	 * @param rLast  The last bound of this range (inclusive)
	 * @param nStep  The step size from one value to the next (should always be
	 *               positive)
	 */
	protected Range(T rFirst, T rLast, long nStep)
	{
		if (nStep == 0)
		{
			throw new IllegalArgumentException("Step must not be zero");
		}
		else if (nStep > 0 && rFirst.compareTo(rLast) > 0)
		{
			nStep = -nStep;
		}

		this.rFirst = rFirst;
		this.rLast  = rLast;
		this.nStep  = nStep;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Creates a new range of integer values with a default step of 1.
	 *
	 * @param  nFirst The first value of the range (inclusive)
	 * @param  nLast  The last value of the range (inclusive)
	 *
	 * @return The new range
	 */
	public static IntRange of(int nFirst, int nLast)
	{
		return Range.of(nFirst, nLast, 1);
	}

	/***************************************
	 * Creates a new range of character values with a default step of 1.
	 *
	 * @param  cFirst The first value of the range (inclusive)
	 * @param  cLast  The last value of the range (inclusive)
	 *
	 * @return The new range
	 */
	public static CharRange of(char cFirst, char cLast)
	{
		return Range.of(cFirst, cLast, 1);
	}

	/***************************************
	 * Creates a new range of long values with a default step of 1.
	 *
	 * @param  nFirst The first value of the range (inclusive)
	 * @param  nLast  The last value of the range (inclusive)
	 *
	 * @return The new range
	 */
	public static LongRange of(long nFirst, long nLast)
	{
		return Range.of(nFirst, nLast, 1);
	}

	/***************************************
	 * Creates a new range of integer values.
	 *
	 * @param  nFirst The first value of the range (inclusive)
	 * @param  nLast  The last value of the range (inclusive)
	 * @param  nStep  The step size for the progression between values
	 *
	 * @return The new range
	 */
	public static IntRange of(int nFirst, int nLast, int nStep)
	{
		return new IntRange(nFirst, nLast, nStep);
	}

	/***************************************
	 * Creates a new range of character values.
	 *
	 * @param  cFirst The first value of the range (inclusive)
	 * @param  cLast  The last value of the range (inclusive)
	 * @param  nStep  The step size for the progression between values
	 *
	 * @return The new range
	 */
	public static CharRange of(char cFirst, char cLast, int nStep)
	{
		return new CharRange(cFirst, cLast, nStep);
	}

	/***************************************
	 * Creates a new range of long values.
	 *
	 * @param  nFirst The first value of the range (inclusive)
	 * @param  nLast  The last value of the range (inclusive)
	 * @param  nStep  The step size for the progression between values
	 *
	 * @return The new range
	 */
	public static LongRange of(long nFirst, long nLast, long nStep)
	{
		return new LongRange(nFirst, nLast, nStep);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the size of this range.
	 *
	 * @return The range size
	 */
	public abstract long getRangeSize();

	/***************************************
	 * Checks whether a certain value is contained in this range.
	 *
	 * @param  rValue The value to check
	 *
	 * @return TRUE if the value is contained in this range
	 */
	public boolean contains(T rValue)
	{
		return nStep > 0
			   ? rValue.compareTo(rFirst) >= 0 && rValue.compareTo(rLast) <= 0
			   : rValue.compareTo(rLast) >= 0 && rValue.compareTo(rFirst) <= 0;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object rObj)
	{
		if (this == rObj)
		{
			return true;
		}

		if (rObj == null || getClass() != rObj.getClass())
		{
			return false;
		}

		Range<?> rOther = (Range<?>) rObj;

		return nStep == rOther.nStep && rFirst.equals(rOther.rFirst) &&
			   rLast.equals(rOther.rLast);
	}

	/***************************************
	 * Returns the first value of this range.
	 *
	 * @return The first value
	 */
	public T getFirst()
	{
		return rFirst;
	}

	/***************************************
	 * Returns the last value of this range.
	 *
	 * @return The last value
	 */
	public T getLast()
	{
		return rLast;
	}

	/***************************************
	 * Returns the step size for iterating over this range. The sign of the
	 * returned value will reflect the iteration direction (positive if
	 * iterating from lower to higher values, negative for the the other way).
	 *
	 * @return The step size
	 */
	public long getStep()
	{
		return nStep;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		final int nPrime = 31;
		int		  nHash  = 1;

		nHash = nPrime * nHash + rFirst.hashCode();
		nHash = nPrime * nHash + rLast.hashCode();
		nHash = nPrime * nHash + (int) (nStep ^ (nStep >>> 32));

		return nHash;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<T> iterator()
	{
		return new RangeIterator();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public Spliterator<T> spliterator()
	{
		return Spliterators.spliterator(iterator(),
										getRangeSize(),
										Spliterator.DISTINCT |
										Spliterator.IMMUTABLE |
										Spliterator.NONNULL |
										Spliterator.ORDERED |
										Spliterator.SORTED | Spliterator.SIZED |
										Spliterator.SUBSIZED);
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
		StringBuilder aResult = new StringBuilder(rFirst.toString());

		aResult.append("..");
		aResult.append(rLast);

		if (Math.abs(nStep) != 1)
		{
			aResult.append(" step ").append(nStep);
		}

		return aResult.toString();
	}

	/***************************************
	 * Must be implemented to return the next value in the range.
	 *
	 * @param  rCurrent The current value to calculate the next value from
	 * @param  nStep    The step size to calculate the next value with
	 *
	 * @return The next value
	 */
	protected abstract T getNextValue(T rCurrent, long nStep);

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * Base class for ranges that are based on {@link Number} values.
	 *
	 * @author eso
	 */
	protected static abstract class NumberRange<N extends Number & Comparable<N>>
		extends Range<N>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * @see Range#Range(Comparable, Comparable, int)
		 */
		protected NumberRange(N rFirst, N rLast, long nStep)
		{
			super(rFirst, rLast, nStep);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public long getRangeSize()
		{
			return (getLast().longValue() - getFirst().longValue()) / getStep();
		}
	}

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
			rNext = rFirst;
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

			rNext = getNextValue(rCurrent, nStep);

			int nNextCompared = rNext.compareTo(rLast);

			if (nStep > 0 && nNextCompared > 0 ||
				nStep < 0 && nNextCompared < 0)
			{
				rNext = null;
			}

			return rCurrent;
		}
	}
}
