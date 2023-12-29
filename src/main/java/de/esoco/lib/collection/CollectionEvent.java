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

import java.util.Collection;

/**
 * An element event subclass that contains informations related to collection
 * modifications. Basically all element event types can occur but which types
 * are actually used depends on the type of collection that generated the
 * event.
 *
 * @author eso
 */
public class CollectionEvent<E, C extends Collection<E>>
	extends ElementEvent<C, E, E> {

	/**
	 * Creates a new instance.
	 *
	 * @param rType        The event type
	 * @param rSource      The collection that is the source of this event
	 * @param rElement     The collection element affected by this event
	 * @param rUpdateValue The new element value in case of update events
	 */
	public CollectionEvent(EventType rType, C rSource, E rElement,
		E rUpdateValue) {
		super(rType, rSource, rElement, rUpdateValue);
	}
}
