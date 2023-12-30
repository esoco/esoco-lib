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

/**
 * A dynamic array of integers that can grow and shrink to hold an arbitrary
 * number of int values.
 *
 * @author eso
 */
public class IntArray {

	/**
	 * The default capacity for new array instances
	 */
	public static final int DEFAULT_CAPACITY = 10;

	/**
	 * Contains the array data
	 */
	private int[] data;

	/**
	 * The number of integers stored in the data array
	 */
	private int size = 0;

	private int capacityIncrement;

	/**
	 * Default constructor, creates a new instance with an initial capacity of
	 * DEFAULT_CAPACITY integers.
	 */
	public IntArray() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * Creates a new instance from an int array.
	 *
	 * @param data The data to be copied into this instance
	 */
	public IntArray(int[] data) {
		int l = data.length;

		data = new int[l];
		System.arraycopy(data, 0, data, 0, last());
	}

	/**
	 * Creates a new instance with the given capacity. The capacity increment
	 * size will be set to the initial capacity.
	 *
	 * @param capacity The initial capacity of the array
	 * @throws IllegalArgumentException If the capacity is negative
	 */
	public IntArray(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Invalid capacity: " + capacity);
		}

		data = new int[capacity];
		capacityIncrement = capacity > 0 ? capacity : DEFAULT_CAPACITY;
	}

	/**
	 * Adds an integer value to the end of the array.
	 *
	 * @param value The value to add
	 */
	public void add(int value) {
		checkCapacity(1);
		data[size++] = value;
	}

	/**
	 * Removes all entries from the array. The array's size will be 0
	 * afterwards, it's capacity will not be changed.
	 */
	public void clear() {
		size = 0;
	}

	/**
	 * Copies the contents of this array to an simple integer array. The
	 * destination array must have a length of at least getSize() + offset,
	 * otherwise an IndexOutOfBoundsException will be thrown.
	 *
	 * @param dst    The destination array to copy the array data into
	 * @param offset The start position in the destination array
	 * @throws IndexOutOfBoundsException If the destination array is to small
	 */
	public void copyTo(int[] dst, int offset) {
		System.arraycopy(data, 0, dst, offset, size);
	}

	/**
	 * Checks if the current capacity fits a certain minimum and to increase
	 * the
	 * capacity it if necessary.
	 *
	 * @param minCapacity The minimum capacity required by the operation that
	 *                    invoked this method
	 */
	public void ensureCapacity(int minCapacity) {
		if (minCapacity > data.length) {
			setCapacity(minCapacity);
		}
	}

	/**
	 * Returns the integer value at a particular position in the array.
	 *
	 * @param index The position in the array
	 * @return The integer value at the given position
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public int get(int index) {
		checkIndex(index);

		return data[index];
	}

	/**
	 * Returns the current capacity (i.e. the maximum number of integer values
	 * in the array).
	 *
	 * @return The current capacity of the array
	 */
	public int getCapacity() {
		return data.length;
	}

	/**
	 * Return the amount by which the array capacity will be incremented when
	 * the size exceeds the current capacity.
	 *
	 * @return The capacity increment
	 */
	public int getCapacityIncrement() {
		return capacityIncrement;
	}

	/**
	 * Returns the current size of the array, i.e. the number of integer values
	 * stored in it.
	 *
	 * @return The number of integer values in the array
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Searches for a specific value and returns the position of it's first
	 * occurence in the array from a certain position.
	 *
	 * @param value The value to search for
	 * @param pos   The position to start at
	 * @return The first position of the value in the array or -1 if not found
	 */
	public int indexOf(int value, int pos) {
		while ((pos < size) && (value != data[pos])) {
			pos++;
		}

		return ((pos < size) ? pos : (-1));
	}

	/**
	 * Inserts an integer value at an arbitrary position of the array. All
	 * values at and behind this position will be shifted one position to the
	 * end of the array. If the array capacity is exceeded the array will be
	 * resized.
	 *
	 * @param value The value to add
	 * @param index The index of the position where the value shall be inserted
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void insert(int value, int index) {
		checkIndex(index);
		checkCapacity(1);
		System.arraycopy(data, index, data, index + 1, size - index);
		data[index] = value;
		size++;
	}

	/**
	 * Inserts a value into the array at a position corresponding to it's
	 * ascending numerical order. All values that are greater than the new
	 * value
	 * will be shifted one position to the end of the array. If the current
	 * array capacity is not sufficient the array will be resized.
	 *
	 * <p>If the parameter start is greater than 0, the values at the array
	 * positions before start will be ignored by the insertion. This allows to
	 * keep a set of unordered values at the beginning of the array.</p>
	 *
	 * @param value The value to insert into the array
	 * @param start The position to start comparing values
	 * @return The position at which the new value has been inserted
	 */
	public int insertAscending(int value, int start) {
		int pos = start;

		checkCapacity(1);

		while ((pos < size) && (value >= data[pos])) {
			pos++;
		}

		for (int i = size; i > pos; i--) {
			data[i] = data[i - 1];
		}

		data[pos] = value;
		size++;

		return pos;
	}

	/**
	 * Returns the last integer value (stored at size -1) in the array.
	 *
	 * @return The integer value stored at the end of the array
	 */
	public int last() {
		return data[size - 1];
	}

	/**
	 * Converts this instance into a new array defined by the given predicate
	 * and mapping function.
	 *
	 * @param include A predicate that returns TRUE if a value should be
	 *                included in the new array
	 * @param map     The mapping function
	 * @return The new array
	 */
	public IntArray map(Predicate<Integer> include,
		Function<Integer, Integer> map) {
		IntArray result = new IntArray(size);

		for (int i = 0; i < size; i++) {
			int value = data[i];

			if (include.test(value)) {
				result.add(value);
			}
		}

		return result;
	}

	/**
	 * Returns the last integer value and removes it from the array.
	 *
	 * @return The last integer value in the array
	 * @throws ArrayIndexOutOfBoundsException If the array is empty
	 */
	public int pop() {
		if (size > 0) {
			return data[--size];
		} else {
			throw new ArrayIndexOutOfBoundsException(
				"IntArray.pop(): array is empty");
		}
	}

	/**
	 * Adds an integer value to the end of the array (stack version, calls
	 * <code>add()</code>).
	 *
	 * @param value The value to add
	 */
	public void push(int value) {
		add(value);
	}

	/**
	 * Removes a value from a certain index position in the array. All values
	 * behind that index will be shifted one position to the beginning of the
	 * array.
	 *
	 * @param index The position at which a value shall be removed
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void remove(int index) {
		checkIndex(index);
		size--;
		System.arraycopy(data, index + 1, data, index, size - index);
	}

	/**
	 * Returns a new array in which all occurrences of a certain value have
	 * been
	 * replaced with another.
	 *
	 * @param value       The value to replace
	 * @param replacement The replacement value
	 * @return The new array
	 */
	public IntArray replaceAll(int value, int replacement) {
		return map(Predicates.alwaysTrue(),
			b -> b == value ? replacement : value);
	}

	/**
	 * Sets a new integer value at an arbitrary position of the array.
	 *
	 * @param value The new value to set
	 * @param index The index of the position where the value shall be set
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void set(int value, int index) {
		checkIndex(index);
		data[index] = value;
	}

	/**
	 * Sets the amount by which the array capacity will be incremented when the
	 * size exceeds the current capacity.
	 *
	 * @param increment The capacity increment
	 */
	public void setCapacityIncrement(int increment) {
		capacityIncrement = increment;
	}

	/**
	 * Sets the size, i.e. the number of integer values that the array
	 * contains.
	 * If the new size is greater than the current size, new integers with the
	 * value 0 will be added to the end of the array. If the new size extends
	 * the current array capacity the capacity will be increased accordingly.
	 *
	 * @param newSize The new number of integer values in the array
	 */
	public void setSize(int newSize) {
		ensureCapacity(newSize);
		size = newSize;
	}

	/**
	 * Converts this array into an integer array containing all values. The
	 * length of the new array will be exactly the same as the size (not the
	 * capacity) of the IntArray.
	 *
	 * @return int[]
	 */
	public int[] toIntArray() {
		int[] result = new int[size];

		copyTo(result, 0);

		return result;
	}

	/**
	 * Returns a string representation of the array in the form
	 * "IntArray[value1,...,valueN]".
	 *
	 * @return A String representing the array
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("IntArray[");

		for (int i = 0; i < size; i++) {
			if (i > 0) {
				result.append(',');
			}

			result.append(data[i]);
		}

		result.append("]");

		return result.toString();
	}

	/**
	 * Shrinks the array capacity to it's current size.
	 */
	public void trimToSize() {
		if (size < data.length) {
			setCapacity(size);
		}
	}

	/**
	 * Lets the array capacity grow for at least a certain size increment.
	 *
	 * @param increment The minimum increment for which the capacity shall grow
	 */
	protected final void checkCapacity(int increment) {
		int newSize = size + increment;

		if (newSize > data.length) {
			if (increment <= 2) {
				newSize += (DEFAULT_CAPACITY - increment);
			}

			setCapacity(newSize);
		}
	}

	/**
	 * Checks if a certain index position is valid for the current array size.
	 *
	 * @param index The index value to check
	 * @throws ArrayIndexOutOfBoundsException If the given index value is not
	 *                                        valid for the current array size
	 */
	protected final void checkIndex(int index)
		throws ArrayIndexOutOfBoundsException {
		if ((index < 0) || (index >= size)) {
			throw new ArrayIndexOutOfBoundsException("Illegal index: " + index);
		}
	}

	/**
	 * Resizes the array to a certain capacity. The new capacity must always be
	 * greater or equal to the current array size.
	 *
	 * @param newCapacity The new array capacity
	 */
	protected final void setCapacity(int newCapacity) {
		int[] newData = new int[newCapacity];

		System.arraycopy(data, 0, newData, 0, size);
		data = newData;
	}
}
