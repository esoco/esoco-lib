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
package de.esoco.lib.net;

import org.obrel.core.RelatedObject;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Immutable datatype class for network adapter MAC addresses. A MAC address
 * consists of 6 bytes that identify a network adapter.
 *
 * @author eso
 */
public class MACAddress extends RelatedObject implements Serializable {

	private static final long serialVersionUID = 1L;

	// allowed MAC string patterns: xx-xx-xx-xx-xx-xx or xx:xx:xx:xx:xx:xx
	private static final Pattern mACPattern =
		Pattern.compile("(\\p{XDigit}{2}[:-]){5}" + "\\p{XDigit}{2}");

	private byte[] bytes;

	/**
	 * Creates a new instance from a string representation of the MAC address.
	 *
	 * @param mAC A string containing the MAC address
	 * @throws IllegalArgumentException If the format of the argument string is
	 *                                  invalid
	 */
	public MACAddress(String mAC) {
		bytes = parseBytes(mAC);
	}

	/**
	 * Creates a new instance from a byte array. The array must contain exactly
	 * 6 bytes in transmission order.
	 *
	 * @param bytes An array containing the 6 bytes of the MAC address
	 * @throws IllegalArgumentException If the argument array is NULL or does
	 *                                  not contain exactly 6 bytes
	 */
	public MACAddress(byte[] bytes) {
		if (bytes == null || bytes.length != 6) {
			bytes = new byte[6];
		}

		System.arraycopy(bytes, 0, bytes, 0, bytes.length);
	}

	/**
	 * Creates a new instance from 6 byte values in transmission order.
	 *
	 * @param b1 Byte 1
	 * @param b2 Byte 2
	 * @param b3 Byte 3
	 * @param b4 Byte 4
	 * @param b5 Byte 5
	 * @param b6 Byte 6
	 */
	public MACAddress(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6) {
		bytes = new byte[6];

		bytes[0] = b1;
		bytes[1] = b2;
		bytes[2] = b3;
		bytes[3] = b4;
		bytes[4] = b5;
		bytes[5] = b6;
	}

	/**
	 * Parses the bytes of a MAC address string and returns a new array
	 * containing the bytes in transmission order.
	 *
	 * @param mAC The MAC address string to parse
	 * @return A new array containing the MAC address bytes
	 * @throws IllegalArgumentException If the argument string is NULL or
	 * has an
	 *                                  invalid format
	 */
	public static byte[] parseBytes(String mAC)
		throws IllegalArgumentException {
		if (mAC == null || !mACPattern.matcher(mAC).matches()) {
			throw new IllegalArgumentException("Invalid MAC string: " + mAC);
		}

		String[] byteTokens = mAC.split("[:-]");
		byte[] bytes = new byte[6];

		// should be covered by the pattern, but to be sure...
		assert byteTokens.length == 6;

		for (int i = 0; i < 6; i++) {
			bytes[i] = (byte) Integer.parseInt(byteTokens[i], 16);
		}

		return bytes;
	}

	/**
	 * Returns the bytes of this MAC address in a new byte array in
	 * transmission
	 * order.
	 *
	 * @return A new array containing the bytes of the MAC address
	 */
	public byte[] getBytes() {
		byte[] result = new byte[bytes.length];

		System.arraycopy(bytes, 0, result, 0, bytes.length);

		return result;
	}
}
