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


/********************************************************************
 * A map implementation that can be used to observe arbitrary map objects for
 * changes.
 *
 * @author eso
 */
public class ObservableMap<K, V> extends AbstractMap<K, V>
	implements EventSource<MapEvent<K, V>>, Serializable
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long serialVersionUID = 1L;

	//~ Instance fields --------------------------------------------------------

	private Map<K, V>								   rObservedMap;
	private List<EventHandler<? super MapEvent<K, V>>> aListeners;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance that is backed by a hash map.
	 */
	public ObservableMap()
	{
		this(new HashMap<K, V>());
	}

	/***************************************
	 * Creates a new instance that observes changes in a particular map.
	 *
	 * @param rObservedMap The map to be observed for changes
	 */
	public ObservableMap(Map<K, V> rObservedMap)
	{
		this.rObservedMap = rObservedMap;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * @see EventSource#addListener(EventHandler)
	 */
	@Override
	public void addListener(EventHandler<? super MapEvent<K, V>> rListener)
	{
		if (aListeners == null)
		{
			aListeners = new ArrayList<EventHandler<? super MapEvent<K, V>>>(1);
		}

		aListeners.add(rListener);
	}

	/***************************************
	 * @see Map#clear()
	 */
	@Override
	public void clear()
	{
		notifyListeners(REMOVE_ALL, null, null);
		rObservedMap.clear();
	}

	/***************************************
	 * @see Map#entrySet()
	 */
	@Override
	public Set<Entry<K, V>> entrySet()
	{
		return new ObservableEntrySet(rObservedMap.entrySet());
	}

	/***************************************
	 * @see EventSource#getEventClass()
	 */
	@Override
	public Class<? super MapEvent<K, V>> getEventClass()
	{
		return MapEvent.class;
	}

	/***************************************
	 * Allows to check whether any listeners have been registered on this
	 * instance.
	 *
	 * @return TRUE if at least one listener has been registered
	 */
	public boolean hasListeners()
	{
		return aListeners != null;
	}

	/***************************************
	 * @see Map#put(Object, Object)
	 */
	@Override
	public V put(K rKey, V rValue)
	{
		notifyListeners(rObservedMap.containsKey(rKey) ? UPDATE : ADD,
						rKey,
						rValue);

		return rObservedMap.put(rKey, rValue);
	}

	/***************************************
	 * @see EventSource#removeListener(EventHandler)
	 */
	@Override
	public void removeListener(EventHandler<? super MapEvent<K, V>> rListener)
	{
		aListeners.remove(rListener);

		if (aListeners.size() == 0)
		{
			aListeners = null;
		}
	}

	/***************************************
	 * Notifies all registered listeners of an element event.
	 *
	 * @param rEventType   The event type
	 * @param rElement     The element affected by the event
	 * @param rUpdateValue The new element value in case of an update event
	 */
	protected void notifyListeners(EventType rEventType,
								   K		 rElement,
								   V		 rUpdateValue)
	{
		if (aListeners != null)
		{
			MapEvent<K, V> rEvent =
				new MapEvent<K, V>(rEventType, this, rElement, rUpdateValue);

			for (EventHandler<? super MapEvent<K, V>> rListener : aListeners)
			{
				rListener.handleEvent(rEvent);
			}
		}
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A map entry implementation that notifies map listeners on changes.
	 *
	 * @author eso
	 */
	class ObservableEntry implements Entry<K, V>
	{
		//~ Instance fields ----------------------------------------------------

		private final Entry<K, V> rObservedEntry;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance with a certain key and value
		 *
		 * @param rObservedEntry rKey The key
		 */
		public ObservableEntry(Entry<K, V> rObservedEntry)
		{
			this.rObservedEntry = rObservedEntry;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Compares this entry with another object for equality.
		 *
		 * @param  rObject The object to compare this entry with
		 *
		 * @return TRUE if the argument is also a map entry and contains an
		 *         equal key and value
		 */
		@Override
		public boolean equals(Object rObject)
		{
			return rObservedEntry.equals(rObject);
		}

		/***************************************
		 * @see Entry#getKey()
		 */
		@Override
		public K getKey()
		{
			return rObservedEntry.getKey();
		}

		/***************************************
		 * @see Entry#getValue()
		 */
		@Override
		public V getValue()
		{
			return rObservedEntry.getValue();
		}

		/***************************************
		 * Returns a hash code generated from this entry's key and value.
		 *
		 * @return The hash code for this entry
		 */
		@Override
		public int hashCode()
		{
			return rObservedEntry.hashCode();
		}

		/***************************************
		 * @see Entry#setValue(Object)
		 */
		@Override
		public V setValue(V rNewValue)
		{
			notifyListeners(UPDATE, rObservedEntry.getKey(), rNewValue);

			return rObservedEntry.setValue(rNewValue);
		}

		/***************************************
		 * Returns a string representation of this entry in the form
		 * 'key'='value'.
		 *
		 * @return A string describing this entry
		 */
		@Override
		public String toString()
		{
			return rObservedEntry.toString();
		}
	}

	/********************************************************************
	 * A set implementation that wraps an entry set and notifies listeners of
	 * entry modifications.
	 *
	 * @author eso
	 */
	class ObservableEntrySet extends ObservableSet<Entry<K, V>>
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rEntrySet The set of entries to wrap
		 */
		public ObservableEntrySet(Set<Entry<K, V>> rEntrySet)
		{
			super(rEntrySet);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Always throws an UnsupportedOperationException.
		 *
		 * @see ObservableSet#add(Object)
		 */
		@Override
		public boolean add(Entry<K, V> rElement)
		{
			throw new UnsupportedOperationException("Use Map.put() to add map entries");
		}

		/***************************************
		 * Overridden to return an {@link ObservableEntrySetIterator} instance
		 * that wraps returned entries in an {@link ObservableEntry} object.
		 *
		 * @see AbstractObservableCollection#iterator()
		 */
		@Override
		public Iterator<Entry<K, V>> iterator()
		{
			return new ObservableEntrySetIterator(getObservedCollection()
												  .iterator());
		}

		//~ Inner Classes ------------------------------------------------------

		/********************************************************************
		 * An iterator subclass that wraps returned entries in an instance of
		 * the class {@link ObservableEntry}.
		 *
		 * @author eso
		 */
		class ObservableEntrySetIterator
			extends ObservableCollectionIterator<Iterator<Entry<K, V>>>
		{
			//~ Constructors ---------------------------------------------------

			/***************************************
			 * Creates a new instance that wraps a certain entry iterator.
			 *
			 * @param rIterator The iterator to wrap
			 */
			ObservableEntrySetIterator(Iterator<Entry<K, V>> rIterator)
			{
				super(rIterator);
			}

			//~ Methods --------------------------------------------------------

			/***************************************
			 * Overridden to remove the current key from the observed map and to
			 * notify the listeners that are registered on the map.
			 */
			@Override
			public void remove()
			{
				Entry<K, V> rCurrent = getCurrentElement();
				K		    rKey     = rCurrent.getKey();
				V		    rValue   = rCurrent.getValue();

				ObservableMap.this.notifyListeners(REMOVE, rKey, rValue);
				getIterator().remove();
				setCurrentElement(null);
			}

			/***************************************
			 * Invokes the superclass after wrapping the new current element in
			 * an instance of {@link ObservableEntry}.
			 *
			 * @param rElement The new current
			 */
			@Override
			void setCurrentElement(Entry<K, V> rElement)
			{
				super.setCurrentElement(new ObservableEntry(rElement));
			}
		}
	}
}
