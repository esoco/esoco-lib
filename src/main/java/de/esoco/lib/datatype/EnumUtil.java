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
	 * @param current The current value to return the next value of
	 * @param values  The list of values
	 * @param wrap    TRUE to return the first value if current is the last
	 * @return The next value as defined by the list order
	 * @throws IllegalArgumentException If the current value cannot be found in
	 *                                  the list of values
	 */
	public static <E extends Enum<E>> E next(E current, E[] values,
		boolean wrap) {
		int max = values.length - 1;

		for (int i = 0; i <= max; i++) {
			if (values[i] == current) {
				if (i < max) {
					return values[i + 1];
				} else if (wrap) {
					return values[0];
				}
			}
		}

		throw new IllegalArgumentException("Value not found: " + current);
	}

	/**
	 * Returns the previous value from a list of enums.
	 *
	 * @param current The current value to return the previous value of
	 * @param values  The list of values
	 * @param wrap    TRUE to return the last value if current is the first
	 * @return The previous value as defined by the list order
	 * @throws IllegalArgumentException If the current value cannot be found in
	 *                                  the list of values
	 */
	public static <E extends Enum<E>> E previous(E current, E[] values,
		boolean wrap) {
		int max = values.length - 1;

		for (int i = max; i >= 0; i--) {
			if (values[i] == current) {
				if (i > 0) {
					return values[i - 1];
				} else if (wrap) {
					return values[max];
				}
			}
		}

		throw new IllegalArgumentException("Value not found: " + current);
	}
}
