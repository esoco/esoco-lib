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
package de.esoco.lib.collection;

import de.esoco.lib.event.ElementEvent;

import java.util.Map;


/********************************************************************
 * An element event subclass that contains informations related to map
 * modifications. The element value of a map event always describes the map key
 * that is affected by the event while the update value contains the map value
 * for this key.
 *
 * @author eso
 */
public class MapEvent<K, V> extends ElementEvent<Map<K, V>, K, V>
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rType        The event type
	 * @param rSource      The map that is the source of this event
	 * @param rElement     The map key affected by this event
	 * @param rUpdateValue The new value in case of update events
	 */
	public MapEvent(EventType rType,
					Map<K, V> rSource,
					K		  rElement,
					V		  rUpdateValue)
	{
		super(rType, rSource, rElement, rUpdateValue);
	}
}
