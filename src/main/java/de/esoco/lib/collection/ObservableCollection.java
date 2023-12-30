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

import de.esoco.lib.event.ElementEvent.EventType;

import java.io.Serializable;
import java.util.Collection;

/**
 * A collection implementation that can be used to observe arbitrary collections
 * for changes.
 *
 * @author eso
 */
public class ObservableCollection<E> extends
	AbstractObservableCollection<E, Collection<E>, CollectionEvent<E,
		Collection<E>>>
	implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance that observes a certain collection.
	 *
	 * @param observedCollection The collection to be observed
	 */
	public ObservableCollection(Collection<E> observedCollection) {
		super(observedCollection);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected CollectionEvent<E, Collection<E>> createEvent(EventType type,
		E element, E updateValue, int index) {
		return new CollectionEvent<E, Collection<E>>(type, this, element,
			updateValue);
	}
}
