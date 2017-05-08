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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;


/********************************************************************
 * A subclass of linked hash map that implements a cache of a fixed size. It
 * uses the constructor {@link LinkedHashMap#LinkedHashMap(int, float, boolean)}
 * to create an access-ordered map. The maximum capacity of the cache is
 * enforced by overriding the method {@link #removeEldestEntry(Entry)}. If the
 * capacity is exceeded while adding a new entry the eldest (i.e. least recently
 * accessed) entry will be removed from the cache.
 *
 * @author eso
 */
public class CacheMap<K, V> extends LinkedHashMap<K, V>
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long serialVersionUID = 1L;

	//~ Instance fields --------------------------------------------------------

	private int nCapacity;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance with a certain capacity that will not be exceeded.
	 *
	 * @param nCapacity The capacity of this cache map
	 */
	public CacheMap(int nCapacity)
	{
		super(nCapacity, 0.75f, true);
		this.nCapacity = nCapacity;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the capacity of this cache map. If a new entry is added to the
	 * cache and the capacity is exceeded the oldest entry will be removed from
	 * the cache.
	 *
	 * @return The capacity
	 */
	public int getCapacity()
	{
		return nCapacity;
	}

	/***************************************
	 * Removes the eldest entry from this cache map and returns it.
	 *
	 * @return The removed entry or NULL if this map is empty
	 */
	public Entry<K, V> removeEldest()
	{
		Iterator<Entry<K, V>> aIterator = entrySet().iterator();
		Entry<K, V>			  rEldest   = null;

		if (aIterator.hasNext())
		{
			rEldest = aIterator.next();
			aIterator.remove();
		}

		return rEldest;
	}

	/***************************************
	 * Sets the capacity of this instance. If the capacity is decreased below
	 * the current size of this instance the oldest entries will be removed.
	 *
	 * @param nNewCapacity The new capacity
	 */
	public void setCapacity(int nNewCapacity)
	{
		while (size() > nNewCapacity)
		{
			removeEldest();
		}

		nCapacity = nNewCapacity;
	}

	/***************************************
	 * Ensures that the cache does not exceeds it's capacity.
	 *
	 * @see LinkedHashMap#removeEldestEntry(Entry)
	 */
	@Override
	protected boolean removeEldestEntry(Entry<K, V> rEldest)
	{
		return size() > nCapacity;
	}
}
