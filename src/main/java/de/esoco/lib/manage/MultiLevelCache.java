//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.lib.manage;

import de.esoco.lib.collection.CacheMap;
import de.esoco.lib.collection.ReferenceCacheMap;
import de.esoco.lib.collection.ReferenceCacheMap.MappedReference;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import java.util.Map.Entry;


/********************************************************************
 * Implementation of a multi-level cache based on {@link Reference}
 * implementations. A cache will store key-value pairs up to the capacities that
 * have been set in the constructor. The first level (if not disabled by setting
 * it's capacity to zero) is a permanent cache that holds strong references to
 * it's elements. The second level uses {@link SoftReference soft references} to
 * store it' values and the third level keeps values with {@link WeakReference
 * weak references}.
 *
 * <p>This means that values in the second and third cache level can be garbage
 * collected by the VM if required. If a discarded reference value is queried
 * from the cache the method {@link #get(Object)} will return NULL. Whether
 * there's a difference between soft and weak references depends on the actual
 * JVM that is used. In a typical server VM it can be expected that weak
 * references are cleared earlier than soft references, but it is not
 * guaranteed.</p>
 *
 * <p>If a new value is added through the method {@link #put(Object, Object)}
 * and the capacity of the first level is exceeded the least recently accessed
 * entry in the cache will be removed from the first cache level and stored in
 * the second level up to it's designated capacity. If the second level's
 * capacity is reached it's least recently used element will be moved to the
 * third level accordingly.</p>
 *
 * <p>The cache access methods are synchronized to allow access from different
 * threads.</p>
 *
 * @author eso
 */
public class MultiLevelCache<K, V>
{
	//~ Instance fields --------------------------------------------------------

	private final CacheMap<K, V>		  aFirstLevelMap;
	private final ReferenceCacheMap<K, V> aSecondLevelMap;
	private final ReferenceCacheMap<K, V> aThirdLevelMap;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new cache with certain capacities.
	 *
	 * @param nFirstLevelCapacity  The maximum number of entries that will be
	 *                             kept permanently in this cache (zero to
	 *                             disable the permanent cache)
	 * @param nSecondLevelCapacity The maximum number of entries this cache will
	 *                             store as soft references in the first level
	 * @param nThirdLevelCapacity  The maximum number of entries that will be
	 *                             stored as weak references after they have
	 *                             been removed from the first level
	 */
	public MultiLevelCache(int nFirstLevelCapacity,
						   int nSecondLevelCapacity,
						   int nThirdLevelCapacity)
	{
		aFirstLevelMap  = new CacheMap<K, V>(nFirstLevelCapacity);
		aSecondLevelMap =
			new ReferenceCacheMap<K, V>(nSecondLevelCapacity, true);
		aThirdLevelMap  =
			new ReferenceCacheMap<K, V>(nThirdLevelCapacity, false);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Clears both first and second level cache.
	 */
	public void clear()
	{
		aThirdLevelMap.clear();
		aSecondLevelMap.clear();
		aFirstLevelMap.clear();
	}

	/***************************************
	 * Checks whether this cache contains a mapping with a certain key.
	 *
	 * @param  rKey The key to check
	 *
	 * @return TRUE if this instance contains the given key
	 */
	public boolean contains(K rKey)
	{
		return aFirstLevelMap.containsKey(rKey) ||
			   aSecondLevelMap.containsKey(rKey) ||
			   aThirdLevelMap.containsKey(rKey);
	}

	/***************************************
	 * Returns a certain value from this cache.
	 *
	 * @param  rKey The key to return the value for
	 *
	 * @return The value associated with the key or NULL if no such value exists
	 *         or if it has expired
	 */
	public final synchronized V get(K rKey)
	{
		V rValue = null;

		if (aFirstLevelMap.containsKey(rKey))
		{
			rValue = aFirstLevelMap.get(rKey);
		}
		else if (aSecondLevelMap.containsKey(rKey))
		{
			rValue = getReferenceValue(rKey, aSecondLevelMap);
		}
		else if (aThirdLevelMap.containsKey(rKey))
		{
			rValue = getReferenceValue(rKey, aThirdLevelMap);
		}

		return rValue;
	}

	/***************************************
	 * Returns the capacity of the three levels of this cache.
	 *
	 * @return A three-element integer array containing the capacities of the
	 *         cache levels
	 */
	public int[] getCapacity()
	{
		return new int[]
			   {
				   aFirstLevelMap.getCapacity(), aSecondLevelMap.getCapacity(),
				   aThirdLevelMap.getCapacity()
			   };
	}

	/***************************************
	 * Returns a string that summarizes the current cache utilization.
	 *
	 * @return The utilization string
	 */
	@SuppressWarnings("boxing")
	public String getUsage()
	{
		return String.format(
			"%d/%d, %d/%d, %s/%d",
			aFirstLevelMap.size(),
			aFirstLevelMap.getCapacity(),
			aSecondLevelMap.size(),
			aSecondLevelMap.getCapacity(),
			aThirdLevelMap.size(),
			aThirdLevelMap.getCapacity());
	}

	/***************************************
	 * Adds an entry to this cache. If the capacity of the first and/or second
	 * cache level is exceeded the oldest entry will be moved to the next cache
	 * level.
	 *
	 * @param rKey   The key to associate the value with
	 * @param rValue The value of the entry
	 */
	public final synchronized void put(K rKey, V rValue)
	{
		// make sure that the value doesn't exist in multiple cache levels
		remove(rKey);

		int nFirstLevelCapacity = aFirstLevelMap.getCapacity();

		if (nFirstLevelCapacity > 0)
		{
			if (aFirstLevelMap.size() == nFirstLevelCapacity)
			{
				saveEldest();
			}

			aFirstLevelMap.put(rKey, rValue);
		}
		else
		{
			putReference(rKey, rValue);
		}
	}

	/***************************************
	 * Removes a certain value from this cache.
	 *
	 * @param  rKey The key associated with the value to remove
	 *
	 * @return The value associated with the key or NULL for none
	 */
	public final synchronized V remove(K rKey)
	{
		V rOldValue = aFirstLevelMap.remove(rKey);

		rOldValue = checkRemoveReference(rKey, rOldValue, aSecondLevelMap);
		rOldValue = checkRemoveReference(rKey, rOldValue, aThirdLevelMap);

		return rOldValue;
	}

	/***************************************
	 * Sets the capacities of the different cache levels.
	 *
	 * @param nFirstLevel  The maximum number of entries that will be kept
	 *                     permanently in this cache (zero to disable the
	 *                     permanent cache)
	 * @param nSecondLevel The maximum number of entries this cache will store
	 *                     as soft references in the first level
	 * @param nThirdLevel  The maximum number of entries that will be stored as
	 *                     weak references after they have been removed from the
	 *                     first level
	 */
	public void setCapacity(int nFirstLevel, int nSecondLevel, int nThirdLevel)
	{
		// set increased secondary level capacities first to store any overflow
		// from first level if necessary
		if (nSecondLevel > aSecondLevelMap.getCapacity())
		{
			aSecondLevelMap.setCapacity(nSecondLevel);
		}

		if (nThirdLevel > aThirdLevelMap.getCapacity())
		{
			aThirdLevelMap.setCapacity(nThirdLevel);
		}

		// try to overflow from first into the secondary levels
		while (aFirstLevelMap.size() > nFirstLevel)
		{
			saveEldest();
		}

		// overflow from second to third level
		if (nThirdLevel > 0)
		{
			while (aSecondLevelMap.size() > nSecondLevel)
			{
				Entry<K, MappedReference<K, V>> rEldest =
					aSecondLevelMap.removeEldest();

				if (rEldest != null)
				{
					V rValue = rEldest.getValue().get();

					if (rValue != null)
					{
						aThirdLevelMap.putValue(rEldest.getKey(), rValue);
					}
				}
			}
		}

		aFirstLevelMap.setCapacity(nFirstLevel);
		aSecondLevelMap.setCapacity(nSecondLevel);
		aThirdLevelMap.setCapacity(nThirdLevel);
	}

	/***************************************
	 * @see Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Cache[" + getUsage() + ']';
	}

	/***************************************
	 * Checks whether a value needs to be removed from a reference map
	 *
	 * @param  rKey          The key to check
	 * @param  rOldValue     The old value to return if it is not NULL
	 * @param  rReferenceMap The reference map
	 *
	 * @return The value that has been removed
	 */
	private V checkRemoveReference(K					   rKey,
								   V					   rOldValue,
								   ReferenceCacheMap<K, V> rReferenceMap)
	{
		MappedReference<K, V> rReference = rReferenceMap.remove(rKey);

		if (rOldValue == null && rReference != null)
		{
			rOldValue = rReference.get();
		}

		return rOldValue;
	}

	/***************************************
	 * Retrieves a certain value from a reference map and marks it as the
	 * most-recently used value by putting it again in this cache. This will
	 * move the value to the first active cache level.
	 *
	 * @param  rKey          The key to return the value for
	 * @param  rReferenceMap The reference map to lookup
	 *
	 * @return The value for the given key or NULL if it is expired or not
	 *         available from this cache
	 */
	private V getReferenceValue(K rKey, ReferenceCacheMap<K, V> rReferenceMap)
	{
		MappedReference<K, V> rReference = rReferenceMap.remove(rKey);
		V					  rValue     = null;

		if (rReference != null)
		{
			rValue = rReference.get();

			if (rValue != null)
			{
				// move to first cache level
				put(rKey, rValue);
			}
		}

		return rValue;
	}

	/***************************************
	 * Adds an entry to this cache's reference levels. If the cache capacity is
	 * exceeded by this new entry the oldest entry will be moved to the second
	 * cache level.
	 *
	 * @param rKey   The key to associate the value with
	 * @param rValue The value of the entry
	 */
	private void putReference(K rKey, V rValue)
	{
		int nSecondLevelCapacity = aSecondLevelMap.getCapacity();
		int nThirdLevelCapacity  = aThirdLevelMap.getCapacity();

		if (aSecondLevelMap.size() == nSecondLevelCapacity)
		{
			Entry<K, MappedReference<K, V>> rEldest =
				aSecondLevelMap.removeEldest();

			if (rEldest != null && nThirdLevelCapacity > 0)
			{
				V rEldestValue = rEldest.getValue().get();

				if (rEldestValue != null)
				{
					if (aThirdLevelMap.size() == nThirdLevelCapacity)
					{
						aThirdLevelMap.removeEldest();
					}

					aThirdLevelMap.putValue(rEldest.getKey(), rEldestValue);
				}
			}
		}

		if (aSecondLevelMap.getCapacity() > 0)
		{
			aSecondLevelMap.putValue(rKey, rValue);
		}
		else if (nThirdLevelCapacity > 0)
		{
			aThirdLevelMap.putValue(rKey, rValue);
		}
	}

	/***************************************
	 * Saves the eldest entry in the first level cache in one of the secondary
	 * levels.
	 */
	private void saveEldest()
	{
		Entry<K, V> rEldest = aFirstLevelMap.removeEldest();

		if (rEldest != null)
		{
			putReference(rEldest.getKey(), rEldest.getValue());
		}
	}
}
