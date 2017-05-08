//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import java.util.List;


/********************************************************************
 * A collection event subclass that provides access to the index of the list
 * position that is affected by the index.
 *
 * @author eso
 */
public class ListEvent<E> extends CollectionEvent<E, List<E>>
{
	//~ Instance fields --------------------------------------------------------

	private final int nIndex;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @see CollectionEvent#CollectionEvent(de.esoco.lib.event.ElementEvent.EventType,
	 *      java.util.Collection, Object, Object)
	 */
	public ListEvent(EventType rType,
					 List<E>   rSource,
					 E		   rElement,
					 E		   rUpdateValue,
					 int	   nIndex)
	{
		super(rType, rSource, rElement, rUpdateValue);
		this.nIndex = nIndex;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the index of the list position that will be affected by this
	 * event. An index value of -1 means that the last element in the list is
	 * affected (typically used if an element is added to the end of a list).
	 *
	 * @return The index of the affected list position
	 */
	public final int getIndex()
	{
		return nIndex;
	}
}
