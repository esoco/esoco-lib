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
import de.esoco.lib.event.EventHandler;
import de.esoco.lib.event.EventSource;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.esoco.lib.event.ElementEvent.EventType.ADD;
import static de.esoco.lib.event.ElementEvent.EventType.REMOVE;
import static de.esoco.lib.event.ElementEvent.EventType.REMOVE_ALL;
import static de.esoco.lib.event.ElementEvent.EventType.UPDATE;

/**
 * A map implementation that can be used to observe arbitrary map objects for
 * changes.
 *
 * @author eso
 */
public class ObservableMap<K, V> extends AbstractMap<K, V>
	implements EventSource<MapEvent<K, V>>, Serializable {

	private static final long serialVersionUID = 1L;

	private final Map<K, V> observedMap;

	private List<EventHandler<? super MapEvent<K, V>>> listeners;

	/**
	 * Creates a new instance that is backed by a hash map.
	 */
	public ObservableMap() {
		this(new HashMap<K, V>());
	}

	/**
	 * Creates a new instance that observes changes in a particular map.
	 *
	 * @param observedMap The map to be observed for changes
	 */
	public ObservableMap(Map<K, V> observedMap) {
		this.observedMap = observedMap;
	}

	/**
	 * @see EventSource#addListener(EventHandler)
	 */
	@Override
	public void addListener(EventHandler<? super MapEvent<K, V>> listener) {
		if (listeners == null) {
			listeners = new ArrayList<EventHandler<? super MapEvent<K, V>>>(1);
		}

		listeners.add(listener);
	}

	/**
	 * @see Map#clear()
	 */
	@Override
	public void clear() {
		notifyListeners(REMOVE_ALL, null, null);
		observedMap.clear();
	}

	/**
	 * @see Map#entrySet()
	 */
	@Override
	public Set<Entry<K, V>> entrySet() {
		return new ObservableEntrySet(observedMap.entrySet());
	}

	/**
	 * @see EventSource#getEventClass()
	 */
	@Override
	public Class<? super MapEvent<K, V>> getEventClass() {
		return MapEvent.class;
	}

	/**
	 * Allows to check whether any listeners have been registered on this
	 * instance.
	 *
	 * @return TRUE if at least one listener has been registered
	 */
	public boolean hasListeners() {
		return listeners != null;
	}

	/**
	 * @see Map#put(Object, Object)
	 */
	@Override
	public V put(K key, V value) {
		notifyListeners(observedMap.containsKey(key) ? UPDATE : ADD, key,
			value);

		return observedMap.put(key, value);
	}

	/**
	 * @see EventSource#removeListener(EventHandler)
	 */
	@Override
	public void removeListener(EventHandler<? super MapEvent<K, V>> listener) {
		listeners.remove(listener);

		if (listeners.size() == 0) {
			listeners = null;
		}
	}

	/**
	 * Notifies all registered listeners of an element event.
	 *
	 * @param eventType   The event type
	 * @param element     The element affected by the event
	 * @param updateValue The new element value in case of an update event
	 */
	protected void notifyListeners(EventType eventType, K element,
		V updateValue) {
		if (listeners != null) {
			MapEvent<K, V> event =
				new MapEvent<K, V>(eventType, this, element, updateValue);

			for (EventHandler<? super MapEvent<K, V>> listener : listeners) {
				listener.handleEvent(event);
			}
		}
	}

	/**
	 * A map entry implementation that notifies map listeners on changes.
	 *
	 * @author eso
	 */
	class ObservableEntry implements Entry<K, V> {

		private final Entry<K, V> observedEntry;

		/**
		 * Creates a new instance with a certain key and value
		 *
		 * @param observedEntry rKey The key
		 */
		public ObservableEntry(Entry<K, V> observedEntry) {
			this.observedEntry = observedEntry;
		}

		/**
		 * Compares this entry with another object for equality.
		 *
		 * @param object The object to compare this entry with
		 * @return TRUE if the argument is also a map entry and contains an
		 * equal key and value
		 */
		@Override
		public boolean equals(Object object) {
			return observedEntry.equals(object);
		}

		/**
		 * @see java.util.Map.Entry#getKey()
		 */
		@Override
		public K getKey() {
			return observedEntry.getKey();
		}

		/**
		 * @see java.util.Map.Entry#getValue()
		 */
		@Override
		public V getValue() {
			return observedEntry.getValue();
		}

		/**
		 * Returns a hash code generated from this entry's key and value.
		 *
		 * @return The hash code for this entry
		 */
		@Override
		public int hashCode() {
			return observedEntry.hashCode();
		}

		/**
		 * @see java.util.Map.Entry#setValue(Object)
		 */
		@Override
		public V setValue(V newValue) {
			notifyListeners(UPDATE, observedEntry.getKey(), newValue);

			return observedEntry.setValue(newValue);
		}

		/**
		 * Returns a string representation of this entry in the form
		 * 'key'='value'.
		 *
		 * @return A string describing this entry
		 */
		@Override
		public String toString() {
			return observedEntry.toString();
		}
	}

	/**
	 * A set implementation that wraps an entry set and notifies listeners of
	 * entry modifications.
	 *
	 * @author eso
	 */
	class ObservableEntrySet extends ObservableSet<Entry<K, V>> {

		private static final long serialVersionUID = 1L;

		/**
		 * Creates a new instance.
		 *
		 * @param entrySet The set of entries to wrap
		 */
		public ObservableEntrySet(Set<Entry<K, V>> entrySet) {
			super(entrySet);
		}

		/**
		 * Always throws an UnsupportedOperationException.
		 *
		 * @see ObservableSet#add(Object)
		 */
		@Override
		public boolean add(Entry<K, V> element) {
			throw new UnsupportedOperationException(
				"Use Map.put() to add map entries");
		}

		/**
		 * Overridden to return an {@link ObservableEntrySetIterator} instance
		 * that wraps returned entries in an {@link ObservableEntry} object.
		 *
		 * @see AbstractObservableCollection#iterator()
		 */
		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new ObservableEntrySetIterator(
				getObservedCollection().iterator());
		}

		/**
		 * An iterator subclass that wraps returned entries in an instance of
		 * the class {@link ObservableEntry}.
		 *
		 * @author eso
		 */
		class ObservableEntrySetIterator
			extends ObservableCollectionIterator<Iterator<Entry<K, V>>> {

			/**
			 * Creates a new instance that wraps a certain entry iterator.
			 *
			 * @param iterator The iterator to wrap
			 */
			ObservableEntrySetIterator(Iterator<Entry<K, V>> iterator) {
				super(iterator);
			}

			/**
			 * Overridden to remove the current key from the observed map
			 * and to
			 * notify the listeners that are registered on the map.
			 */
			@Override
			public void remove() {
				Entry<K, V> current = getCurrentElement();
				K key = current.getKey();
				V value = current.getValue();

				ObservableMap.this.notifyListeners(REMOVE, key, value);
				getIterator().remove();
				setCurrentElement(null);
			}

			/**
			 * Invokes the superclass after wrapping the new current element in
			 * an instance of {@link ObservableEntry}.
			 *
			 * @param element The new current
			 */
			@Override
			void setCurrentElement(Entry<K, V> element) {
				super.setCurrentElement(new ObservableEntry(element));
			}
		}
	}
}
