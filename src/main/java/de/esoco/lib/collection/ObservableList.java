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
import de.esoco.lib.event.EventSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import static de.esoco.lib.event.ElementEvent.EventType.ADD;
import static de.esoco.lib.event.ElementEvent.EventType.REMOVE;
import static de.esoco.lib.event.ElementEvent.EventType.UPDATE;

/**
 * A list implementation that can be observed for changes.
 *
 * @author eso
 */
public class ObservableList<E>
	extends AbstractObservableCollection<E, List<E>, ListEvent<E>>
	implements List<E>, EventSource<ListEvent<E>>, Serializable, RandomAccess {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance that observes an {@link ArrayList} instance.
	 */
	public ObservableList() {
		this(new ArrayList<E>());
	}

	/**
	 * Creates a new instance that observes a certain list instance.
	 *
	 * @param observedList The list to be observed
	 */
	public ObservableList(List<E> observedList) {
		super(observedList);
	}

	/**
	 * @see List#add(Object)
	 */
	@Override
	public boolean add(E element) {
		add(size(), element);

		return true;
	}

	/**
	 * @see List#add(int, Object)
	 */
	@Override
	public void add(int index, E element) {
		notifyListeners(ADD, element, null, index);
		getObservedCollection().add(index, element);
	}

	/**
	 * @see List#addAll(int, Collection)
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> collection) {
		for (E object : collection) {
			add(index++, object);
		}

		return !collection.isEmpty();
	}

	/**
	 * @see List#get(int)
	 */
	@Override
	public E get(int index) {
		return getObservedCollection().get(index);
	}

	/**
	 * Overridden to return the class of {@link ListEvent}.
	 *
	 * @see AbstractObservableCollection#getEventClass()
	 */
	@Override
	public Class<? super ListEvent<E>> getEventClass() {
		return ListEvent.class;
	}

	/**
	 * @see List#indexOf(Object)
	 */
	@Override
	public int indexOf(Object object) {
		return getObservedCollection().indexOf(object);
	}

	/**
	 * Overridden to return a list iterator instead of the default iterator for
	 * observable collections.
	 *
	 * @see List#iterator()
	 */
	@Override
	public Iterator<E> iterator() {
		return listIterator();
	}

	/**
	 * @see List#lastIndexOf(Object)
	 */
	@Override
	public int lastIndexOf(Object object) {
		return getObservedCollection().lastIndexOf(object);
	}

	/**
	 * @see List#listIterator()
	 */
	@Override
	public ListIterator<E> listIterator() {
		return new ObservableListIterator(
			getObservedCollection().listIterator());
	}

	/**
	 * @see List#listIterator(int)
	 */
	@Override
	public ListIterator<E> listIterator(int index) {
		return new ObservableListIterator(
			getObservedCollection().listIterator(index));
	}

	/**
	 * @see List#remove(int)
	 */
	@Override
	public E remove(int index) {
		notifyListeners(REMOVE, get(index), null, index);

		return getObservedCollection().remove(index);
	}

	/**
	 * @see List#set(int, Object)
	 */
	@Override
	public E set(int index, E element) {
		notifyListeners(UPDATE, get(index), element, index);

		return getObservedCollection().set(index, element);
	}

	/**
	 * @see List#subList(int, int)
	 */
	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		List<E> subList = getObservedCollection().subList(fromIndex, toIndex);

		return new ObservableSubList(subList, fromIndex);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ListEvent<E> createEvent(EventType type, E element,
		E updateValue,
		int index) {
		return new ListEvent<E>(type, this, element, updateValue, index);
	}

	/**
	 * A list iterator subclass of the observable collection iterator.
	 *
	 * @author eso
	 */
	class ObservableListIterator
		extends ObservableCollectionIterator<ListIterator<E>>
		implements ListIterator<E> {

		boolean forward = true;

		private int current = -1;

		/**
		 * Creates a new instance that wraps an iterator of the observed
		 * collection.
		 *
		 * @param iterator The iterator to wrap
		 */
		ObservableListIterator(ListIterator<E> iterator) {
			super(iterator);
		}

		/**
		 * @see ListIterator#add(Object)
		 */
		@Override
		public void add(E object) {
			notifyListeners(ADD, object, null, forward ? current + 1 :
			                                   current);

			getIterator().add(object);
			current++;
			// setCurrent() is not necessary because by specification the add
			// method inserts between current and next element
		}

		/**
		 * @see ListIterator#hasPrevious()
		 */
		@Override
		public boolean hasPrevious() {
			return getIterator().hasPrevious();
		}

		/**
		 * Overridden to increment the current index.
		 *
		 * @return The next element
		 */
		@Override
		public E next() {
			E next = super.next();

			if (forward) {
				current++;
			} else {
				forward = true;
			}

			return next;
		}

		/**
		 * @see ListIterator#nextIndex()
		 */
		@Override
		public int nextIndex() {
			return getIterator().nextIndex();
		}

		/**
		 * @see ListIterator#previous()
		 */
		@Override
		public E previous() {
			E previous = getIterator().previous();

			setCurrentElement(previous);

			if (forward) {
				forward = false;
			} else {
				current--;
			}

			return previous;
		}

		/**
		 * @see ListIterator#previousIndex()
		 */
		@Override
		public int previousIndex() {
			return getIterator().previousIndex();
		}

		/**
		 * @see Iterator#remove()
		 */
		@Override
		public void remove() {
			notifyListeners(REMOVE, getCurrentElement(), null, current);
			getIterator().remove();

			if (forward) {
				current--;
			}
		}

		/**
		 * @see ListIterator#set(Object)
		 */
		@Override
		public void set(E object) {
			notifyListeners(UPDATE, getCurrentElement(), object, current);
			getIterator().set(object);
			setCurrentElement(object);
		}
	}

	/**
	 * A list implementation for sub-lists of observable lists.
	 *
	 * @author eso
	 */
	class ObservableSubList extends ObservableList<E> {

		private static final long serialVersionUID = 1L;

		private final int start;

		/**
		 * Creates a new instance that wraps a certain sub-list of the observed
		 * collection.
		 *
		 * @param subList The sub-list to wrap
		 * @param start   The starting index of the sub-list
		 */
		public ObservableSubList(List<E> subList, int start) {
			super(subList);
			this.start = start;
		}

		/**
		 * Overridden to additionally notify the listeners of the enclosing
		 * class. The given sub-list index value will be transformed into the
		 * corresponding index of the full list.
		 *
		 * @see AbstractObservableCollection#notifyListeners(EventType, Object,
		 * Object, int)
		 */
		@Override
		protected void notifyListeners(EventType type, E element,
			E updateValue,
			int index) {
			super.notifyListeners(type, element, updateValue, index);
			ObservableList.this.notifyListeners(type, element, updateValue,
				index + start);
		}
	}
}
