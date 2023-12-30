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
import de.esoco.lib.event.ElementEvent.EventType;
import de.esoco.lib.event.EventHandler;
import de.esoco.lib.event.EventSource;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A base class for collection implementations that can be observed for changes.
 * This is done by wrapping an arbitrary collection and sending events about
 * pending changes to the wrapped collection to registered listeners. Listeners
 * must implement the {@link EventHandler} interface and can be registered
 * through the method {@link #addListener(EventHandler)}. This class supports
 * all element event types as defined in the {@link ElementEvent} class.
 *
 * <p>The generic parameters allow subclasses to define the element type (T),
 * the type of the wrapped collection (C), and the type of collection event
 * thrown by the implementation (E).</p>
 *
 * @author eso
 */
public abstract class AbstractObservableCollection<T, C extends Collection<T>
	, E extends CollectionEvent<T, C>> extends AbstractCollection<T>
	implements EventSource<E>, Serializable {

	private static final long serialVersionUID = 1L;

	private final C observedCollection;

	private List<EventHandler<? super E>> listeners;

	/**
	 * Creates a new instance that observes a certain collection instance.
	 *
	 * @param observedCollection The collection to be observed
	 */
	public AbstractObservableCollection(C observedCollection) {
		this.observedCollection = observedCollection;
	}

	/**
	 * @see Collection#add(Object)
	 */
	@Override
	public boolean add(T element) {
		notifyListeners(EventType.ADD, element, null, -1);

		return observedCollection.add(element);
	}

	/**
	 * @see EventSource#addListener(EventHandler)
	 */
	@Override
	public void addListener(EventHandler<? super E> listener) {
		if (listeners == null) {
			listeners = new ArrayList<EventHandler<? super E>>(1);
		}

		listeners.add(listener);
	}

	/**
	 * @see Collection#clear()
	 */
	@Override
	public void clear() {
		notifyListeners(EventType.REMOVE_ALL, null, null, -1);
		observedCollection.clear();
	}

	/**
	 * This implementation always returns the class of {@link CollectionEvent}.
	 * Subclasses that use a more specific event type must override this
	 * method.
	 *
	 * @see EventSource#getEventClass()
	 */
	@Override
	public Class<? super E> getEventClass() {
		return CollectionEvent.class;
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
	 * @see Collection#iterator()
	 */
	@Override
	public Iterator<T> iterator() {
		return new ObservableCollectionIterator<Iterator<T>>(
			observedCollection.iterator());
	}

	/**
	 * @see EventSource#removeListener(EventHandler)
	 */
	@Override
	public void removeListener(EventHandler<? super E> listener) {
		listeners.remove(listener);

		if (listeners.size() == 0) {
			listeners = null;
		}
	}

	/**
	 * @see Collection#size()
	 */
	@Override
	public int size() {
		return observedCollection.size();
	}

	/**
	 * Must be implemented by subclasses to create a new event object. This
	 * method will only be invoked if listeners have been registered on this
	 * observable collection instance, so implementations need not test this.
	 * See the method
	 * {@link #notifyListeners(ElementEvent.EventType, Object, Object, int)}
	 * for
	 * more details.
	 *
	 * @param type        The type of event
	 * @param element     The element affected by the event
	 * @param updateValue The new element value in case of an update event
	 * @param index       The position index for random access collections
	 * @return A new event object
	 */
	protected abstract E createEvent(EventType type, T element, T updateValue,
		int index);

	/**
	 * Method for subclasses to access the observed collection.
	 *
	 * @return The observed collection
	 */
	protected final C getObservedCollection() {
		return observedCollection;
	}

	/**
	 * Notifies all registered listeners of a collection event. The method
	 * {@link #createEvent(ElementEvent.EventType, Object, Object, int)}
	 * will be
	 * invoked to create the event object.
	 *
	 * <p>The last parameter is an index value for random access collections
	 * like lists. It has been included to ease the implementation of different
	 * subclasses, other collection implementations should ignore it. The
	 * methods in this class (like {@link #add(Object)} invoke this method with
	 * an index value of -1. Subclasses should either interpret this
	 * accordingly
	 * (e.g. as the last element) or override these methods to determine the
	 * appropriate index by themselves.</p>
	 *
	 * @param type        The type of event
	 * @param element     The element affected by the event
	 * @param updateValue The new element value in case of an update event
	 * @param index       The position index for random access collections
	 */
	protected void notifyListeners(EventType type, T element, T updateValue,
		int index) {
		if (hasListeners()) {
			E event = createEvent(type, element, updateValue, index);

			for (EventHandler<? super E> listener : listeners) {
				listener.handleEvent(event);
			}
		}
	}

	/**
	 * An iterator implementation that wraps iterators of the observed
	 * collection to detect remove events.
	 *
	 * @author eso
	 */
	class ObservableCollectionIterator<I extends Iterator<T>>
		implements Iterator<T> {

		private final I iterator;

		private T current;

		/**
		 * Creates a new instance that wraps an iterator of the observed
		 * collection.
		 *
		 * @param iterator The iterator to wrap
		 */
		ObservableCollectionIterator(I iterator) {
			this.iterator = iterator;
		}

		/**
		 * @see Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		/**
		 * @see Iterator#next()
		 */
		@Override
		public T next() {
			setCurrentElement(iterator.next());

			return current;
		}

		/**
		 * @see Iterator#remove()
		 */
		@Override
		public void remove() {
			notifyListeners(EventType.REMOVE, current, null, -1);
			setCurrentElement(null);
			iterator.remove();
		}

		/**
		 * Returns the current element this iterator is positioned at.
		 *
		 * @return The current element (NULL if iterator hasn't been used yet)
		 */
		final T getCurrentElement() {
			return current;
		}

		/**
		 * Returns the iterator.
		 *
		 * @return The iterator
		 */
		final I getIterator() {
			return iterator;
		}

		/**
		 * Sets the current element of this iterator. This method can be
		 * overridden by subclasses that need to wrap elements before they are
		 * returned to the calling party. Subclasses that reposition the
		 * iterator must invoke this method to update the current element
		 * accordingly.
		 *
		 * @param element The new current element or NULL if no current element
		 *                exists (e.g. after it has been removed)
		 */
		void setCurrentElement(T element) {
			current = element;
		}
	}
}
