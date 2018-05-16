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

/********************************************************************
 * Implements a range of integer values that can be iterated through.
 *
 * @author eso
 */
public class CharRange extends Range<Character>
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Private, use factory method {@link #of(int, int, int)} to create a new
	 * int range.
	 *
	 * @see Range#Range(Comparable, Comparable, int)
	 */
	CharRange(char cFirst, char cLast, int nStep)
	{
		super(cFirst, cLast, nStep);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public long getRangeSize()
	{
		return (getLast().charValue() - getFirst().charValue()) / getStep();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected Character getNextValue(Character rCurrent, long nStep)
	{
		return (char) (rCurrent.charValue() + nStep);
	}
}
