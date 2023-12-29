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
package de.esoco.lib.property;

/**
 * Property interface for integer ranges that have a minimum and a maximum
 * integer value.
 *
 * @author eso
 */
public interface IntRangeAttribute {

	/**
	 * Returns the maximum value.
	 *
	 * @return The maximum value
	 */
	int getMaximum();

	/**
	 * Returns the minimum value.
	 *
	 * @return The minimum value
	 */
	int getMinimum();

	/**
	 * Sets the maximum value.
	 *
	 * @param nValue The new maximum value
	 */
	void setMaximum(int nValue);

	/**
	 * Sets the minimum value.
	 *
	 * @param nValue The new minimum value
	 */
	void setMinimum(int nValue);
}
