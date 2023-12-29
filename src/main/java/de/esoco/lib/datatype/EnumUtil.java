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

/**
 * Static utility functions for Java Enums.
 *
 * @author eso
 */
public class EnumUtil {

	/**
	 * Private, only static use.
	 */
	private EnumUtil() {
	}

	/**
	 * Returns the next value from a list of enums.
	 *
	 * @param rCurrent The current value to return the next value of
	 * @param rValues  The list of values
	 * @param bWrap    TRUE to return the first value if rCurrent is the last
	 * @return The next value as defined by the list order
	 * @throws IllegalArgumentException If the current value cannot be found in
	 *                                  the list of values
	 */
	public static <E extends Enum<E>> E next(E rCurrent, E[] rValues,
		boolean bWrap) {
		int nMax = rValues.length - 1;

		for (int i = 0; i <= nMax; i++) {
			if (rValues[i] == rCurrent) {
				if (i < nMax) {
					return rValues[i + 1];
				} else if (bWrap) {
					return rValues[0];
				}
			}
		}

		throw new IllegalArgumentException("Value not found: " + rCurrent);
	}

	/**
	 * Returns the previous value from a list of enums.
	 *
	 * @param rCurrent The current value to return the previous value of
	 * @param rValues  The list of values
	 * @param bWrap    TRUE to return the last value if rCurrent is the first
	 * @return The previous value as defined by the list order
	 * @throws IllegalArgumentException If the current value cannot be found in
	 *                                  the list of values
	 */
	public static <E extends Enum<E>> E previous(E rCurrent, E[] rValues,
		boolean bWrap) {
		int nMax = rValues.length - 1;

		for (int i = nMax; i >= 0; i--) {
			if (rValues[i] == rCurrent) {
				if (i > 0) {
					return rValues[i - 1];
				} else if (bWrap) {
					return rValues[nMax];
				}
			}
		}

		throw new IllegalArgumentException("Value not found: " + rCurrent);
	}
}
