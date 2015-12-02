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


/********************************************************************
 * A list implementation that can be observed for changes.
 *
 * @author eso
 */
public class ObservableList<E>
	extends AbstractObservableCollection<E, List<E>, ListEvent<E>>
	implements List<E>, EventSource<ListEvent<E>>, Serializable, RandomAccess
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long serialVersionUID = 1L;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance that observes an {@link ArrayList} instance.
	 */
	public ObservableList()
	{
		this(new ArrayList<E>());
	}

	/***************************************
	 * Creates a new instance that observes a certain list instance.
	 *
	 * @param rObservedList The list to be observed
	 */
	public ObservableList(List<E> rObservedList)
	{
		super(rObservedList);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * @see List#add(Object)
	 */
	@Override
	public boolean add(E rElement)
	{
		add(size(), rElement);

		return true;
	}

	/***************************************
	 * @see List#add(int, Object)
	 */
	@Override
	public void add(int nIndex, E rElement)
	{
		notifyListeners(ADD, rElement, null, nIndex);
		getObservedCollection().add(nIndex, rElement);
	}

	/***************************************
	 * @see List#addAll(int, Collection)
	 */
	@Override
	public boolean addAll(int nIndex, Collection<? extends E> rCollection)
	{
		for (E rObject : rCollection)
		{
			add(nIndex++, rObject);
		}

		return !rCollection.isEmpty();
	}

	/***************************************
	 * @see List#get(int)
	 */
	@Override
	public E get(int rIndex)
	{
		return getObservedCollection().get(rIndex);
	}

	/***************************************
	 * Overridden to return the class of {@link ListEvent}.
	 *
	 * @see AbstractObservableCollection#getEventClass()
	 */
	@Override
	public Class<? super ListEvent<E>> getEventClass()
	{
		return ListEvent.class;
	}

	/***************************************
	 * @see List#indexOf(Object)
	 */
	@Override
	public int indexOf(Object rObject)
	{
		return getObservedCollection().indexOf(rObject);
	}

	/***************************************
	 * Overridden to return a list iterator instead of the default iterator for
	 * observable collections.
	 *
	 * @see List#iterator()
	 */
	@Override
	public Iterator<E> iterator()
	{
		return listIterator();
	}

	/***************************************
	 * @see List#lastIndexOf(Object)
	 */
	@Override
	public int lastIndexOf(Object rObject)
	{
		return getObservedCollection().lastIndexOf(rObject);
	}

	/***************************************
	 * @see List#listIterator()
	 */
	@Override
	public ListIterator<E> listIterator()
	{
		return new ObservableListIterator(getObservedCollection()
										  .listIterator());
	}

	/***************************************
	 * @see List#listIterator(int)
	 */
	@Override
	public ListIterator<E> listIterator(int nIndex)
	{
		return new ObservableListIterator(getObservedCollection().listIterator(nIndex));
	}

	/***************************************
	 * @see List#remove(int)
	 */
	@Override
	public E remove(int nIndex)
	{
		notifyListeners(REMOVE, get(nIndex), null, nIndex);

		return getObservedCollection().remove(nIndex);
	}

	/***************************************
	 * @see List#set(int, Object)
	 */
	@Override
	public E set(int nIndex, E rElement)
	{
		notifyListeners(UPDATE, get(nIndex), rElement, nIndex);

		return getObservedCollection().set(nIndex, rElement);
	}

	/***************************************
	 * @see List#subList(int, int)
	 */
	@Override
	public List<E> subList(int nFromIndex, int nToIndex)
	{
		List<E> rSubList =
			getObservedCollection().subList(nFromIndex, nToIndex);

		return new ObservableSubList(rSubList, nFromIndex);
	}

	/***************************************
	 * @see AbstractObservableCollection#createEvent(ElementEvent.EventType,Object,
	 *      Object, int)
	 */
	@Override
	protected ListEvent<E> createEvent(EventType rType,
									   E		 rElement,
									   E		 rUpdateValue,
									   int		 nIndex)
	{
		return new ListEvent<E>(rType, this, rElement, rUpdateValue, nIndex);
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A list iterator subclass of the observable collection iterator.
	 *
	 * @author eso
	 */
	class ObservableListIterator
		extends ObservableCollectionIterator<ListIterator<E>>
		implements ListIterator<E>
	{
		//~ Instance fields ----------------------------------------------------

		private int nCurrent = -1;
		boolean     bForward = true;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * @see ObservableCollectionIterator#ObservableCollectionIterator(Iterator)
		 */
		ObservableListIterator(ListIterator<E> rIterator)
		{
			super(rIterator);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * @see ListIterator#add(Object)
		 */
		@Override
		public void add(E rObject)
		{
			notifyListeners(ADD,
							rObject,
							null,
							bForward ? nCurrent + 1 : nCurrent);

			getIterator().add(rObject);
			nCurrent++;
			// setCurrent() is not necessary because by specification the add
			// method inserts between current and next element
		}

		/***************************************
		 * @see ListIterator#hasPrevious()
		 */
		@Override
		public boolean hasPrevious()
		{
			return getIterator().hasPrevious();
		}

		/***************************************
		 * Overridden to increment the current index.
		 *
		 * @see ObservableCollectionIterator#next()
		 */
		@Override
		public E next()
		{
			E rNext = super.next();

			if (bForward)
			{
				nCurrent++;
			}
			else
			{
				bForward = true;
			}

			return rNext;
		}

		/***************************************
		 * @see ListIterator#nextIndex()
		 */
		@Override
		public int nextIndex()
		{
			return getIterator().nextIndex();
		}

		/***************************************
		 * @see ListIterator#previous()
		 */
		@Override
		public E previous()
		{
			E rPrevious = getIterator().previous();

			setCurrentElement(rPrevious);

			if (bForward)
			{
				bForward = false;
			}
			else
			{
				nCurrent--;
			}

			return rPrevious;
		}

		/***************************************
		 * @see ListIterator#previousIndex()
		 */
		@Override
		public int previousIndex()
		{
			return getIterator().previousIndex();
		}

		/***************************************
		 * @see Iterator#remove()
		 */
		@Override
		public void remove()
		{
			notifyListeners(REMOVE, getCurrentElement(), null, nCurrent);
			getIterator().remove();

			if (bForward)
			{
				nCurrent--;
			}
		}

		/***************************************
		 * @see ListIterator#set(Object)
		 */
		@Override
		public void set(E rObject)
		{
			notifyListeners(UPDATE, getCurrentElement(), rObject, nCurrent);
			getIterator().set(rObject);
			setCurrentElement(rObject);
		}
	}

	/********************************************************************
	 * A list implementation for sub-lists of observable lists.
	 *
	 * @author eso
	 */
	class ObservableSubList extends ObservableList<E>
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		//~ Instance fields ----------------------------------------------------

		private final int nStart;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance that wraps a certain sub-list of the observed
		 * collection.
		 *
		 * @param rSubList The sub-list to wrap
		 * @param nStart   The starting index of the sub-list
		 */
		public ObservableSubList(List<E> rSubList, int nStart)
		{
			super(rSubList);
			this.nStart = nStart;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Overridden to additionally notify the listeners of the enclosing
		 * class. The given sub-list index value will be transformed into the
		 * corresponding index of the full list.
		 *
		 * @see AbstractObservableCollection#notifyListeners(EventType, Object,
		 *      Object, int)
		 */
		@Override
		protected void notifyListeners(EventType rType,
									   E		 rElement,
									   E		 rUpdateValue,
									   int		 nIndex)
		{
			super.notifyListeners(rType, rElement, rUpdateValue, nIndex);
			ObservableList.this.notifyListeners(rType,
												rElement,
												rUpdateValue,
												nIndex + nStart);
		}
	}
}
