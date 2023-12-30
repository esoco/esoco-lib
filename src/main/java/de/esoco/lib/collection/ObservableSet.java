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

import de.esoco.lib.event.ElementEvent.EventType;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A set implementation that can be used to observe arbitrary set instances for
 * changes.
 *
 * @author eso
 */
public class ObservableSet<E>
	extends AbstractObservableCollection<E, Set<E>, CollectionEvent<E, Set<E>>>
	implements Set<E>, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance that is backed by a {@link HashSet} instance.
	 */
	public ObservableSet() {
		this(new HashSet<E>());
	}

	/**
	 * Creates a new instance that observes a certain set instance for changes.
	 *
	 * @param observedSet The set to observe for changes
	 */
	public ObservableSet(Set<E> observedSet) {
		super(observedSet);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected CollectionEvent<E, Set<E>> createEvent(EventType type, E element,
		E updateValue, int index) {
		return new CollectionEvent<E, Set<E>>(type, this, element,
			updateValue);
	}
}
