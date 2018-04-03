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
package de.esoco.lib.collection;

import de.esoco.lib.json.Json;
import de.esoco.lib.json.JsonBuilder;
import de.esoco.lib.json.JsonSerializable;
import de.esoco.lib.text.TextConvert;


/********************************************************************
 * A dynamic array of bytes that can grow and shrink to hold an arbitrary number
 * of byte values. It also can be converted to and from a JSON string which will
 * have the prefix "0x" followed by a joined sequence of two-char hexadecimal
 * values for each byte. If the byte array is empty, the JSON value is only
 * "0x".
 *
 * @author eso
 */
public class ByteArray implements JsonSerializable<ByteArray>
{
	//~ Static fields/initializers ---------------------------------------------

	/** The default capacity for new array instances */
	public static final int DEFAULT_CAPACITY = 10;

	//~ Instance fields --------------------------------------------------------

	/** Contains the array data */
	private byte[] aData;

	/** The number of bytes stored in the data array */
	private int nSize = 0;

	private int nCapacityIncrement = DEFAULT_CAPACITY;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Default constructor, creates an array with an initial capacity of
	 * DEFAULT_CAPACITY bytes.
	 */
	public ByteArray()
	{
		this(DEFAULT_CAPACITY);
	}

	/***************************************
	 * Constructor that creates an array with the given capacity. The capacity
	 * increment size will be set to the initial capacity.
	 *
	 * @param  nCapacity The initial capacity of the array
	 *
	 * @throws IllegalArgumentException If the capacity is negative
	 */
	public ByteArray(int nCapacity)
	{
		if (nCapacity < 0)
		{
			throw new IllegalArgumentException("Invalid capacity: " +
											   nCapacity);
		}

		aData			   = new byte[nCapacity];
		nCapacityIncrement = nCapacity > 0 ? nCapacity : DEFAULT_CAPACITY;
	}

	/***************************************
	 * Creates a new instance from a byte array.
	 *
	 * @param rData The data to be copied into this instance
	 */
	public ByteArray(byte[] rData)
	{
		int l = rData.length;

		aData = new byte[l];
		System.arraycopy(rData, 0, aData, 0, last());
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Adds an byte value to the end of this array.
	 *
	 * @param nValue The value to add
	 */
	public void add(byte nValue)
	{
		checkCapacity(1);
		aData[nSize++] = nValue;
	}

	/***************************************
	 * Adds byte values from an array to the end of this array.
	 *
	 * @param rValues The array containing the values to add
	 * @param nOffset The offset from which to copy the bytes
	 * @param nCount  The number of bytes to copy
	 */
	public void add(byte[] rValues, int nOffset, int nCount)
	{
		checkCapacity(nCount);
		System.arraycopy(rValues, 0, aData, nSize, nCount);
		nSize += nCount;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void appendTo(JsonBuilder rBuilder)
	{
		rBuilder.appendString("0x" + toString());
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
	 * Copies the contents of this array to an simple byte array. The
	 * destination array must have a length of at least getSize() + nOffset,
	 * otherwise an IndexOutOfBoundsException will be thrown.
	 *
	 * @param  rTarget The target array to copy the array data into
	 * @param  nOffset The start position in the destination array
	 *
	 * @return The target array
	 *
	 * @throws IndexOutOfBoundsException If the destination array is to small
	 */
	public byte[] copyTo(byte[] rTarget, int nOffset)
	{
		System.arraycopy(aData, 0, rTarget, nOffset, nSize);

		return rTarget;
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
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object rObject)
	{
		if (this == rObject)
		{
			return true;
		}

		if (rObject == null || getClass() != rObject.getClass())
		{
			return false;
		}

		ByteArray rOther = (ByteArray) rObject;

		if (nSize != rOther.nSize)
		{
			return false;
		}

		for (int i = 0; i < nSize; i++)
		{
			if (aData[i] != rOther.aData[i])
			{
				return false;
			}
		}

		return true;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public ByteArray fromJson(String sJson)
	{
		String sData = Json.parse(sJson, String.class);

		if (!sData.startsWith("0x"))
		{
			throw new IllegalArgumentException("Missing 0x prefix: " + sData);
		}
		else if (sData.length() % 2 == 1)
		{
			throw new IllegalArgumentException("Invalid byte array data: " +
											   sData);
		}
		else
		{
			sData = sData.substring(2);

			int nChars = sData.length();

			clear();
			ensureCapacity(nChars / 2);

			for (int i = 0; i < nChars; i += 2)
			{
				aData[nSize++] =
					(byte) Short.parseShort(sData.substring(i, i + 2), 16);
			}
		}

		return this;
	}

	/***************************************
	 * Returns the byte value at a particular position in the array.
	 *
	 * @param  nIndex The position in the array
	 *
	 * @return The byte value at the given position
	 *
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public byte get(int nIndex)
	{
		checkIndex(nIndex);

		return aData[nIndex];
	}

	/***************************************
	 * Returns the current capacity (i.e. the maximum number of byte values in
	 * the array).
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
	 * Returns the current size of the array, i.e. the number of byte values
	 * stored in it.
	 *
	 * @return The number of byte values in the array
	 */
	public int getSize()
	{
		return nSize;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		int nHash = nSize;

		for (int i = 0; i < nSize; i++)
		{
			nHash = 31 * nHash + aData[i];
		}

		return nHash;
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
	public int indexOf(byte nValue, int nPos)
	{
		while ((nPos < nSize) && (nValue != aData[nPos]))
		{
			nPos++;
		}

		return ((nPos < nSize) ? nPos : (-1));
	}

	/***************************************
	 * Inserts an byte value at an arbitrary position of the array. All values
	 * at and behind this position will be shifted one position to the end of
	 * the array. If the array capacity is exceeded the array will be resized.
	 *
	 * @param  nValue The value to add
	 * @param  nIndex The index of the position where the value shall be
	 *                inserted
	 *
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void insert(byte nValue, int nIndex)
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
	public int insertAscending(byte nValue, int nStart)
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
	 * Checks whether this array is empty.
	 *
	 * @return TRUE if empty
	 */
	public boolean isEmpty()
	{
		return nSize == 0;
	}

	/***************************************
	 * Returns the last byte value (stored at size -1) in the array.
	 *
	 * @return The byte value stored at the end of the array
	 */
	public byte last()
	{
		return aData[nSize - 1];
	}

	/***************************************
	 * Returns the last byte value and removes it from the array.
	 *
	 * @return The last byte value in the array
	 *
	 * @throws ArrayIndexOutOfBoundsException If the array is empty
	 */
	public byte pop()
	{
		if (nSize > 0)
		{
			return aData[--nSize];
		}
		else
		{
			throw new ArrayIndexOutOfBoundsException("ByteArray.pop(): array is empty");
		}
	}

	/***************************************
	 * Adds an byte value to the end of the array (stack version, calls <code>
	 * add()</code>).
	 *
	 * @param nValue The value to add
	 */
	public void push(byte nValue)
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
	 * Sets a new byte value at an arbitrary position of the array.
	 *
	 * @param  nValue The new value to set
	 * @param  nIndex The index of the position where the value shall be set
	 *
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void set(byte nValue, int nIndex)
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
	 * Sets the size, i.e. the number of byte values that the array contains. If
	 * the new size is greater than the current size, new bytes with the value 0
	 * will be added to the end of the array. If the new size extends the
	 * current array capacity the capacity will be increased accordingly.
	 *
	 * @param nNewSize The new number of byte values in the array
	 */
	public void setSize(int nNewSize)
	{
		ensureCapacity(nNewSize);
		nSize = nNewSize;
	}

	/***************************************
	 * Copies the bytes of this array into a byte array. The length of the new
	 * array will be exactly the same as the current size (not the capacity) of
	 * the ByteArray.
	 *
	 * @return A new byte array containing all elements of this array
	 */
	public byte[] toByteArray()
	{
		return copyTo(new byte[nSize], 0);
	}

	/***************************************
	 * Returns a string representation of this array with two hexadecimal digits
	 * for each byte without gaps between bytes. For example, a three-byte array
	 * with the values 127, 0, 32 would be returned as "7F0020".
	 *
	 * @return A hexadecimal string representation
	 */
	@Override
	public String toString()
	{
		return TextConvert.hexString(aData, 0, nSize, "");
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
			nNewSize =
				((nNewSize / nCapacityIncrement) + 1) * nCapacityIncrement;

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
			throw new ArrayIndexOutOfBoundsException("Illegal index: " +
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
		byte[] aNewData = new byte[nNewCapacity];

		System.arraycopy(aData, 0, aNewData, 0, nSize);
		aData = aNewData;
	}
}
