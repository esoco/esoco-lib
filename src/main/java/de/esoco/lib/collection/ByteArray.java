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
import de.esoco.lib.json.Json;
import de.esoco.lib.json.JsonBuilder;
import de.esoco.lib.json.JsonSerializable;
import de.esoco.lib.text.TextConvert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A dynamic array of bytes that can grow and shrink to hold an arbitrary number
 * of byte values. It also can be converted to and from a JSON string which will
 * have the prefix "0x" followed by a joined sequence of two-char hexadecimal
 * values for each byte. If the byte array is empty, the JSON value is only
 * "0x".
 *
 * @author eso
 */
public class ByteArray implements JsonSerializable<ByteArray> {

	/**
	 * The default capacity for new array instances
	 */
	public static final int DEFAULT_CAPACITY = 10;

	/**
	 * Contains the array data
	 */
	private byte[] data;

	/**
	 * The number of bytes stored in the data array
	 */
	private int size = 0;

	private int capacityIncrement = DEFAULT_CAPACITY;

	/**
	 * Default constructor, creates an array with an initial capacity of
	 * DEFAULT_CAPACITY bytes.
	 */
	public ByteArray() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * Constructor that creates an array with the given capacity. The capacity
	 * increment size will be set to the initial capacity.
	 *
	 * @param capacity The initial capacity of the array
	 * @throws IllegalArgumentException If the capacity is negative
	 */
	public ByteArray(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Invalid capacity: " + capacity);
		}

		data = new byte[capacity];
		capacityIncrement = capacity > 0 ? capacity : DEFAULT_CAPACITY;
	}

	/**
	 * Creates a new instance from a byte array.
	 *
	 * @param data The data to be copied into this instance
	 */
	public ByteArray(byte[] data) {
		int l = data.length;

		data = new byte[l];
		System.arraycopy(data, 0, data, 0, l);
	}

	/**
	 * Returns an instance containing the bytes from a string of hexadecimal
	 * digits.
	 *
	 * @param hexValue The hexadecimal string to convert
	 * @return The instance
	 * @throws IllegalArgumentException If the argument string is NULL or
	 * has an
	 *                                  uneven length
	 * @throws NumberFormatException    If the string contains characters that
	 *                                  cannot be parsed into bytes
	 * @see TextConvert#toBytes(String)
	 */
	public static ByteArray valueOf(String hexValue) {
		return new ByteArray(TextConvert.toBytes(hexValue));
	}

	/**
	 * Adds an byte value to the end of this array.
	 *
	 * @param value The value to add
	 */
	public void add(byte value) {
		checkCapacity(1);
		data[size++] = value;
	}

	/**
	 * Adds all byte values from an array to the end of this array.
	 *
	 * @param values The array containing the values to add
	 */
	public void add(byte[] values) {
		add(values, 0, values.length);
	}

	/**
	 * Adds certain byte values from an array to the end of this array.
	 *
	 * @param values The array containing the values to add
	 * @param offset The offset from which to copy the bytes
	 * @param count  The number of bytes to copy
	 */
	public void add(byte[] values, int offset, int count) {
		checkCapacity(count);
		System.arraycopy(values, 0, data, size, count);
		size += count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void appendTo(JsonBuilder builder) {
		builder.appendString("0x" + this);
	}

	/**
	 * Removes all entries from the array. The array's size will be 0
	 * afterwards, it's capacity will not be changed.
	 */
	public void clear() {
		size = 0;
	}

	/**
	 * Copies the contents of this array to an simple byte array. The
	 * destination array must have a length of at least getSize() + offset,
	 * otherwise an IndexOutOfBoundsException will be thrown.
	 *
	 * @param target The target array to copy the array data into
	 * @param offset The start position in the destination array
	 * @return The target array
	 * @throws IndexOutOfBoundsException If the destination array is to small
	 */
	public byte[] copyTo(byte[] target, int offset) {
		System.arraycopy(data, 0, target, offset, size);

		return target;
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
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (object == null || getClass() != object.getClass()) {
			return false;
		}

		ByteArray other = (ByteArray) object;

		if (size != other.size) {
			return false;
		}

		for (int i = 0; i < size; i++) {
			if (data[i] != other.data[i]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteArray fromJson(String json) {
		String parsed = Json.parse(json, String.class);

		if (!parsed.startsWith("0x")) {
			throw new IllegalArgumentException("Missing 0x prefix: " + data);
		} else if (parsed.length() % 2 == 1) {
			throw new IllegalArgumentException(
				"Invalid byte array data: " + parsed);
		} else {
			parsed = parsed.substring(2);

			int chars = parsed.length();

			clear();
			ensureCapacity(chars / 2);

			for (int i = 0; i < chars; i += 2) {
				data[size++] =
					(byte) Short.parseShort(parsed.substring(i, i + 2), 16);
			}
		}

		return this;
	}

	/**
	 * Returns the byte value at a particular position in the array.
	 *
	 * @param index The position in the array
	 * @return The byte value at the given position
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public byte get(int index) {
		checkIndex(index);

		return data[index];
	}

	/**
	 * Returns the current capacity (i.e. the maximum number of byte values in
	 * the array).
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
	 * Returns the current size of the array, i.e. the number of byte values
	 * stored in it.
	 *
	 * @return The number of byte values in the array
	 */
	public int getSize() {
		return size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		int hash = size;

		for (int i = 0; i < size; i++) {
			hash = 31 * hash + data[i];
		}

		return hash;
	}

	/**
	 * Searches for a specific value and returns the position of it's first
	 * occurence in the array from a certain position.
	 *
	 * @param value The value to search for
	 * @param pos   The position to start at
	 * @return The first position of the value in the array or -1 if not found
	 */
	public int indexOf(byte value, int pos) {
		while ((pos < size) && (value != data[pos])) {
			pos++;
		}

		return ((pos < size) ? pos : (-1));
	}

	/**
	 * Inserts an byte value at an arbitrary position of the array. All values
	 * at and behind this position will be shifted one position to the end of
	 * the array. If the array capacity is exceeded the array will be resized.
	 *
	 * @param value The value to add
	 * @param index The index of the position where the value shall be inserted
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void insert(byte value, int index) {
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
	public int insertAscending(byte value, int start) {
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
	 * Checks whether this array is empty.
	 *
	 * @return TRUE if empty
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns the last byte value (stored at size -1) in the array.
	 *
	 * @return The byte value stored at the end of the array
	 */
	public byte last() {
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
	public ByteArray map(Predicate<Byte> include, Function<Byte, Byte> map) {
		ByteArray result = new ByteArray(size);

		for (int i = 0; i < size; i++) {
			byte value = data[i];

			if (include.test(value)) {
				result.add(map.apply(value));
			}
		}

		return result;
	}

	/**
	 * Returns the last byte value and removes it from the array.
	 *
	 * @return The last byte value in the array
	 * @throws ArrayIndexOutOfBoundsException If the array is empty
	 */
	public byte pop() {
		if (size > 0) {
			return data[--size];
		} else {
			throw new ArrayIndexOutOfBoundsException(
				"ByteArray.pop(): array is empty");
		}
	}

	/**
	 * Adds an byte value to the end of the array (stack version, calls <code>
	 * add()</code>).
	 *
	 * @param value The value to add
	 */
	public void push(byte value) {
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
	public ByteArray replaceAll(byte value, byte replacement) {
		return map(Predicates.alwaysTrue(),
			b -> b == value ? replacement : value);
	}

	/**
	 * Sets a new byte value at an arbitrary position of the array.
	 *
	 * @param value The new value to set
	 * @param index The index of the position where the value shall be set
	 * @throws ArrayIndexOutOfBoundsException If the index value is invalid
	 */
	public void set(byte value, int index) {
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
	 * Sets the size, i.e. the number of byte values that the array contains .
	 * If the new size is greater than the current size, new bytes with the
	 * value 0 will be added to the end of the array. If the new size extends
	 * the current array capacity the capacity will be increased accordingly.
	 *
	 * @param newSize The new number of byte values in the array
	 */
	public void setSize(int newSize) {
		ensureCapacity(newSize);
		size = newSize;
	}

	/**
	 * Converts the bytes of this instance into an ASCII string.*
	 *
	 * @return The resulting string
	 */
	public String toAscii() {
		return toText(StandardCharsets.US_ASCII);
	}

	/**
	 * Copies the bytes of this array into a byte array. The length of the new
	 * array will be exactly the same as the current size (not the capacity) of
	 * the ByteArray.
	 *
	 * @return A new byte array containing all elements of this array
	 */
	public byte[] toByteArray() {
		return copyTo(new byte[size], 0);
	}

	/**
	 * Returns a string representation of this array with two hexadecimal
	 * digits
	 * for each byte without gaps between bytes. For example, a three-byte
	 * array
	 * with the values 127, 0, 32 would be returned as "7F0020".
	 *
	 * @return A hexadecimal string representation
	 */
	@Override
	public String toString() {
		return TextConvert.hexString(data, 0, size, "");
	}

	/**
	 * Converts the bytes of this instance into a string with the given
	 * charset.
	 *
	 * @param charset The charset
	 * @return The resulting string
	 */
	public String toText(Charset charset) {
		return new String(data, charset);
	}

	/**
	 * Converts the bytes of this instance into an UTF-8 string.
	 *
	 * @return The resulting string
	 */
	public String toUtf8() {
		return toText(StandardCharsets.UTF_8);
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
			newSize = ((newSize / capacityIncrement) + 1) * capacityIncrement;

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
		byte[] newData = new byte[newCapacity];

		System.arraycopy(data, 0, newData, 0, size);
		data = newData;
	}
}
