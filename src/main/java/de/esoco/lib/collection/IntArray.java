//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.expression.Predicates;

import java.util.function.Function;
import java.util.function.Predicate;


/********************************************************************
 * A dynamic array of integers that can grow and shrink to hold an arbitrary
 * number of int values.
 *
 * @author eso
 */
public class IntArray
{
	//~ Static fields/initializers ---------------------------------------------

	/** The default capacity for new array instances */
	public static final int DEFAULT_CAPACITY = 10;

	//~ Instance fields --------------------------------------------------------

	/** Contains the array data */
	private int[] aData;

	/** The number of integers stored in the data array */
	private int nSize = 0;

	private int nCapacityIncrement;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Default constructor, creates a new instance with an initial capacity of
	 * DEFAULT_CAPACITY integers.
	 */
	public IntArray()
	{
		this(DEFAULT_CAPACITY);
	}

	/***************************************
	 * Creates a new instance from an int array.
	 *
	 * @param rData The data to be copied into this instance
	 */
	public IntArray(int[] rData)
	{
		int l = rData.length;

		aData = new int[l];
		System.arraycopy(rData, 0, aData, 0, last());
	}

	/***************************************
	 * Creates a new instance with the given capacity. The capacity increment
	 * size will be set to the initial capacity.
	 *
	 * @param  nCapacity The initial capacity of the array
	 *
	 * @throws IllegalArgumentException If the capacity is negative
	 */
	public IntArray(int nCapacity)
	{
		if (nCapacity < 0)
		{
			throw new IllegalArgumentException(
				"Invalid capacity: " +
				nCapacity);
		}

		aData			   = new int[nCapacity];
		nCapacityIncrement = nCapacity > 0 ? nCapacity : DEFAULT_CAPACITY;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Adds an integer value to the end of the array.
	 *
	 * @param nValue The value to add
	 */
	public void add(int nValue)
	{
		checkCapacity(1);
		aData[nSize++] = nValue;
	}

	/***************************************
	 * Removes all entries from the array. The array's size will be 0
	 * afterwards, it's capacity will not be changed.
	 */
	public void clear()
	{
		nSize = 0;
	}

	/***************************************
	 * Copies the contents of this array to an simple integer array. The
	 * destination array must have a length of at least getSize() + nOffset,
	 * otherwise an IndexOutOfBoundsException will be thrown.
	 *
	 * @param  rDst    The destination array to copy the array data into
	 * @param  nOffset The start position in the destination array
	 *
	 * @throws IndexOutOfBoundsException If the destination array is to small
	 */
	public void copyTo(int[] rDst, int nOffset)
	{
		System.arraycopy(aData, 0, rDst, nOffset, nSize);
	}

	/***************************************
	 * Checks if the current capacity fits a certain minimum and to increase the
	 * capacity it if necessary.
	 *
	 * @param nMinCapacity The minimum capacity required by the operation that
	 *                     invoked this method
	 */
	public void ensureCapacity(int nMinCapacity)
	{
		if (nMinCapacity > aData.length)
		{
			setCapacity(nMinCapacity);
		}
	}

	/***************************************
	 * Returns the integer value at a particular position in the array.
	 *
	 * @param  nIndex The position in the array
	 *
	 * @return The integer value at the given position
	 *
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public int get(int nIndex)
	{
		checkIndex(nIndex);

		return aData[nIndex];
	}

	/***************************************
	 * Returns the current capacity (i.e. the maximum number of integer values
	 * in the array).
	 *
	 * @return The current capacity of the array
	 */
	public int getCapacity()
	{
		return aData.length;
	}

	/***************************************
	 * Return the amount by which the array capacity will be incremented when
	 * the size exceeds the current capacity.
	 *
	 * @return The capacity increment
	 */
	public int getCapacityIncrement()
	{
		return nCapacityIncrement;
	}

	/***************************************
	 * Returns the current size of the array, i.e. the number of integer values
	 * stored in it.
	 *
	 * @return The number of integer values in the array
	 */
	public int getSize()
	{
		return nSize;
	}

	/***************************************
	 * Searches for a specific value and returns the position of it's first
	 * occurence in the array from a certain position.
	 *
	 * @param  nValue The value to search for
	 * @param  nPos   The position to start at
	 *
	 * @return The first position of the value in the array or -1 if not found
	 */
	public int indexOf(int nValue, int nPos)
	{
		while ((nPos < nSize) && (nValue != aData[nPos]))
		{
			nPos++;
		}

		return ((nPos < nSize) ? nPos : (-1));
	}

	/***************************************
	 * Inserts an integer value at an arbitrary position of the array. All
	 * values at and behind this position will be shifted one position to the
	 * end of the array. If the array capacity is exceeded the array will be
	 * resized.
	 *
	 * @param  nValue The value to add
	 * @param  nIndex The index of the position where the value shall be
	 *                inserted
	 *
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void insert(int nValue, int nIndex)
	{
		checkIndex(nIndex);
		checkCapacity(1);
		System.arraycopy(aData, nIndex, aData, nIndex + 1, nSize - nIndex);
		aData[nIndex] = nValue;
		nSize++;
	}

	/***************************************
	 * Inserts a value into the array at a position corresponding to it's
	 * ascending numerical order. All values that are greater than the new value
	 * will be shifted one position to the end of the array. If the current
	 * array capacity is not sufficient the array will be resized.
	 *
	 * <p>If the parameter nStart is greater than 0, the values at the array
	 * positions before nStart will be ignored by the insertion. This allows to
	 * keep a set of unordered values at the beginning of the array.</p>
	 *
	 * @param  nValue The value to insert into the array
	 * @param  nStart The position to start comparing values
	 *
	 * @return The position at which the new value has been inserted
	 */
	public int insertAscending(int nValue, int nStart)
	{
		int nPos = nStart;

		checkCapacity(1);

		while ((nPos < nSize) && (nValue >= aData[nPos]))
		{
			nPos++;
		}

		for (int i = nSize; i > nPos; i--)
		{
			aData[i] = aData[i - 1];
		}

		aData[nPos] = nValue;
		nSize++;

		return nPos;
	}

	/***************************************
	 * Returns the last integer value (stored at size -1) in the array.
	 *
	 * @return The integer value stored at the end of the array
	 */
	public int last()
	{
		return aData[nSize - 1];
	}

	/***************************************
	 * Converts this instance into a new array defined by the given predicate
	 * and mapping function.
	 *
	 * @param  pInclude A predicate that returns TRUE if a value should be
	 *                  included in the new array
	 * @param  fMap     The mapping function
	 *
	 * @return The new array
	 */
	public IntArray map(
		Predicate<Integer>		   pInclude,
		Function<Integer, Integer> fMap)
	{
		IntArray aResult = new IntArray(nSize);

		for (int i = 0; i < nSize; i++)
		{
			int nValue = aData[i];

			if (pInclude.test(nValue))
			{
				aResult.add(nValue);
			}
		}

		return aResult;
	}

	/***************************************
	 * Returns the last integer value and removes it from the array.
	 *
	 * @return The last integer value in the array
	 *
	 * @throws ArrayIndexOutOfBoundsException If the array is empty
	 */
	public int pop()
	{
		if (nSize > 0)
		{
			return aData[--nSize];
		}
		else
		{
			throw new ArrayIndexOutOfBoundsException(
				"IntArray.pop(): array is empty");
		}
	}

	/***************************************
	 * Adds an integer value to the end of the array (stack version, calls
	 * <code>add()</code>).
	 *
	 * @param nValue The value to add
	 */
	public void push(int nValue)
	{
		add(nValue);
	}

	/***************************************
	 * Removes a value from a certain index position in the array. All values
	 * behind that index will be shifted one position to the beginning of the
	 * array.
	 *
	 * @param  nIndex The position at which a value shall be removed
	 *
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void remove(int nIndex)
	{
		checkIndex(nIndex);
		nSize--;
		System.arraycopy(aData, nIndex + 1, aData, nIndex, nSize - nIndex);
	}

	/***************************************
	 * Returns a new array in which all occurrences of a certain value have been
	 * replaced with another.
	 *
	 * @param  nValue       The value to replace
	 * @param  nReplacement The replacement value
	 *
	 * @return The new array
	 */
	public IntArray replaceAll(int nValue, int nReplacement)
	{
		return map(
			Predicates.alwaysTrue(),
			b -> b == nValue ? nReplacement : nValue);
	}

	/***************************************
	 * Sets a new integer value at an arbitrary position of the array.
	 *
	 * @param  nValue The new value to set
	 * @param  nIndex The index of the position where the value shall be set
	 *
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void set(int nValue, int nIndex)
	{
		checkIndex(nIndex);
		aData[nIndex] = nValue;
	}

	/***************************************
	 * Sets the amount by which the array capacity will be incremented when the
	 * size exceeds the current capacity.
	 *
	 * @param nIncrement The capacity increment
	 */
	public void setCapacityIncrement(int nIncrement)
	{
		nCapacityIncrement = nIncrement;
	}

	/***************************************
	 * Sets the size, i.e. the number of integer values that the array contains.
	 * If the new size is greater than the current size, new integers with the
	 * value 0 will be added to the end of the array. If the new size extends
	 * the current array capacity the capacity will be increased accordingly.
	 *
	 * @param nNewSize The new number of integer values in the array
	 */
	public void setSize(int nNewSize)
	{
		ensureCapacity(nNewSize);
		nSize = nNewSize;
	}

	/***************************************
	 * Converts this array into an integer array containing all values. The
	 * length of the new array will be exactly the same as the size (not the
	 * capacity) of the IntArray.
	 *
	 * @return int[]
	 */
	public int[] toIntArray()
	{
		int[] aResult = new int[nSize];

		copyTo(aResult, 0);

		return aResult;
	}

	/***************************************
	 * Returns a string representation of the array in the form
	 * "IntArray[value1,...,valueN]".
	 *
	 * @return A String representing the array
	 */
	@Override
	public String toString()
	{
		StringBuilder sResult = new StringBuilder("IntArray[");

		for (int i = 0; i < nSize; i++)
		{
			if (i > 0)
			{
				sResult.append(',');
			}

			sResult.append(aData[i]);
		}

		sResult.append("]");

		return sResult.toString();
	}

	/***************************************
	 * Shrinks the array capacity to it's current size.
	 */
	public void trimToSize()
	{
		if (nSize < aData.length)
		{
			setCapacity(nSize);
		}
	}

	/***************************************
	 * Lets the array capacity grow for at least a certain size increment.
	 *
	 * @param nIncrement The minimum increment for which the capacity shall grow
	 */
	protected final void checkCapacity(int nIncrement)
	{
		int nNewSize = nSize + nIncrement;

		if (nNewSize > aData.length)
		{
			if (nIncrement <= 2)
			{
				nNewSize += (DEFAULT_CAPACITY - nIncrement);
			}

			setCapacity(nNewSize);
		}
	}

	/***************************************
	 * Checks if a certain index position is valid for the current array size.
	 *
	 * @param  nIndex The index value to check
	 *
	 * @throws ArrayIndexOutOfBoundsException If the given index value is not
	 *                                        valid for the current array size
	 */
	protected final void checkIndex(int nIndex)
		throws ArrayIndexOutOfBoundsException
	{
		if ((nIndex < 0) || (nIndex >= nSize))
		{
			throw new ArrayIndexOutOfBoundsException(
				"Illegal index: " +
				nIndex);
		}
	}

	/***************************************
	 * Resizes the array to a certain capacity. The new capacity must always be
	 * greater or equal to the current array size.
	 *
	 * @param nNewCapacity The new array capacity
	 */
	protected final void setCapacity(int nNewCapacity)
	{
		int[] aNewData = new int[nNewCapacity];

		System.arraycopy(aData, 0, aNewData, 0, nSize);
		aData = aNewData;
	}
}
