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

/**
 * Implementation of a multi-level cache based on {@link Reference}
 * implementations. A cache will store key-value pairs up to the capacities that
 * have been set in the constructor. The first level (if not disabled by setting
 * it's capacity to zero) is a permanent cache that holds strong references to
 * it's elements. The second level uses {@link SoftReference soft references} to
 * store it' values and the third level keeps values with
 * {@link WeakReference weak references}.
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
public class MultiLevelCache<K, V> {

	private final CacheMap<K, V> firstLevelMap;

	private final ReferenceCacheMap<K, V> secondLevelMap;

	private final ReferenceCacheMap<K, V> thirdLevelMap;

	/**
	 * Creates a new cache with certain capacities.
	 *
	 * @param firstLevelCapacity  The maximum number of entries that will be
	 *                            kept permanently in this cache (zero to
	 *                            disable the permanent cache)
	 * @param secondLevelCapacity The maximum number of entries this cache will
	 *                            store as soft references in the first level
	 * @param thirdLevelCapacity  The maximum number of entries that will be
	 *                            stored as weak references after they have
	 *                            been
	 *                            removed from the first level
	 */
	public MultiLevelCache(int firstLevelCapacity, int secondLevelCapacity,
		int thirdLevelCapacity) {
		firstLevelMap = new CacheMap<K, V>(firstLevelCapacity);
		secondLevelMap = new ReferenceCacheMap<K, V>(secondLevelCapacity,
			true);
		thirdLevelMap = new ReferenceCacheMap<K, V>(thirdLevelCapacity, false);
	}

	/**
	 * Clears both first and second level cache.
	 */
	public void clear() {
		thirdLevelMap.clear();
		secondLevelMap.clear();
		firstLevelMap.clear();
	}

	/**
	 * Checks whether this cache contains a mapping with a certain key.
	 *
	 * @param key The key to check
	 * @return TRUE if this instance contains the given key
	 */
	public boolean contains(K key) {
		return firstLevelMap.containsKey(key) ||
			secondLevelMap.containsKey(key) || thirdLevelMap.containsKey(key);
	}

	/**
	 * Returns a certain value from this cache.
	 *
	 * @param key The key to return the value for
	 * @return The value associated with the key or NULL if no such value
	 * exists
	 * or if it has expired
	 */
	public final synchronized V get(K key) {
		V value = null;

		if (firstLevelMap.containsKey(key)) {
			value = firstLevelMap.get(key);
		} else if (secondLevelMap.containsKey(key)) {
			value = getReferenceValue(key, secondLevelMap);
		} else if (thirdLevelMap.containsKey(key)) {
			value = getReferenceValue(key, thirdLevelMap);
		}

		return value;
	}

	/**
	 * Returns the capacity of the three levels of this cache.
	 *
	 * @return A three-element integer array containing the capacities of the
	 * cache levels
	 */
	public int[] getCapacity() {
		return new int[] { firstLevelMap.getCapacity(),
			secondLevelMap.getCapacity(), thirdLevelMap.getCapacity() };
	}

	/**
	 * Returns a string that summarizes the current cache utilization.
	 *
	 * @return The utilization string
	 */
	@SuppressWarnings("boxing")
	public String getUsage() {
		return String.format("%d/%d, %d/%d, %s/%d", firstLevelMap.size(),
			firstLevelMap.getCapacity(), secondLevelMap.size(),
			secondLevelMap.getCapacity(), thirdLevelMap.size(),
			thirdLevelMap.getCapacity());
	}

	/**
	 * Adds an entry to this cache. If the capacity of the first and/or second
	 * cache level is exceeded the oldest entry will be moved to the next cache
	 * level.
	 *
	 * @param key   The key to associate the value with
	 * @param value The value of the entry
	 */
	public final synchronized void put(K key, V value) {
		// make sure that the value doesn't exist in multiple cache levels
		remove(key);

		int firstLevelCapacity = firstLevelMap.getCapacity();

		if (firstLevelCapacity > 0) {
			if (firstLevelMap.size() == firstLevelCapacity) {
				saveEldest();
			}

			firstLevelMap.put(key, value);
		} else {
			putReference(key, value);
		}
	}

	/**
	 * Removes a certain value from this cache.
	 *
	 * @param key The key associated with the value to remove
	 * @return The value associated with the key or NULL for none
	 */
	public final synchronized V remove(K key) {
		V oldValue = firstLevelMap.remove(key);

		oldValue = checkRemoveReference(key, oldValue, secondLevelMap);
		oldValue = checkRemoveReference(key, oldValue, thirdLevelMap);

		return oldValue;
	}

	/**
	 * Sets the capacities of the different cache levels.
	 *
	 * @param firstLevel  The maximum number of entries that will be kept
	 *                    permanently in this cache (zero to disable the
	 *                    permanent cache)
	 * @param secondLevel The maximum number of entries this cache will
	 *                       store as
	 *                    soft references in the first level
	 * @param thirdLevel  The maximum number of entries that will be stored as
	 *                    weak references after they have been removed from the
	 *                    first level
	 */
	public void setCapacity(int firstLevel, int secondLevel, int thirdLevel) {
		// set increased secondary level capacities first to store any overflow
		// from first level if necessary
		if (secondLevel > secondLevelMap.getCapacity()) {
			secondLevelMap.setCapacity(secondLevel);
		}

		if (thirdLevel > thirdLevelMap.getCapacity()) {
			thirdLevelMap.setCapacity(thirdLevel);
		}

		// try to overflow from first into the secondary levels
		while (firstLevelMap.size() > firstLevel) {
			saveEldest();
		}

		// overflow from second to third level
		if (thirdLevel > 0) {
			while (secondLevelMap.size() > secondLevel) {
				Entry<K, MappedReference<K, V>> eldest =
					secondLevelMap.removeEldest();

				if (eldest != null) {
					V value = eldest.getValue().get();

					if (value != null) {
						thirdLevelMap.putValue(eldest.getKey(), value);
					}
				}
			}
		}

		firstLevelMap.setCapacity(firstLevel);
		secondLevelMap.setCapacity(secondLevel);
		thirdLevelMap.setCapacity(thirdLevel);
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "Cache[" + getUsage() + ']';
	}

	/**
	 * Checks whether a value needs to be removed from a reference map
	 *
	 * @param key          The key to check
	 * @param oldValue     The old value to return if it is not NULL
	 * @param referenceMap The reference map
	 * @return The value that has been removed
	 */
	private V checkRemoveReference(K key, V oldValue,
		ReferenceCacheMap<K, V> referenceMap) {
		MappedReference<K, V> reference = referenceMap.remove(key);

		if (oldValue == null && reference != null) {
			oldValue = reference.get();
		}

		return oldValue;
	}

	/**
	 * Retrieves a certain value from a reference map and marks it as the
	 * most-recently used value by putting it again in this cache. This will
	 * move the value to the first active cache level.
	 *
	 * @param key          The key to return the value for
	 * @param referenceMap The reference map to lookup
	 * @return The value for the given key or NULL if it is expired or not
	 * available from this cache
	 */
	private V getReferenceValue(K key, ReferenceCacheMap<K, V> referenceMap) {
		MappedReference<K, V> reference = referenceMap.remove(key);
		V value = null;

		if (reference != null) {
			value = reference.get();

			if (value != null) {
				// move to first cache level
				put(key, value);
			}
		}

		return value;
	}

	/**
	 * Adds an entry to this cache's reference levels. If the cache capacity is
	 * exceeded by this new entry the oldest entry will be moved to the second
	 * cache level.
	 *
	 * @param key   The key to associate the value with
	 * @param value The value of the entry
	 */
	private void putReference(K key, V value) {
		int secondLevelCapacity = secondLevelMap.getCapacity();
		int thirdLevelCapacity = thirdLevelMap.getCapacity();

		if (secondLevelMap.size() == secondLevelCapacity) {
			Entry<K, MappedReference<K, V>> eldest =
				secondLevelMap.removeEldest();

			if (eldest != null && thirdLevelCapacity > 0) {
				V eldestValue = eldest.getValue().get();

				if (eldestValue != null) {
					if (thirdLevelMap.size() == thirdLevelCapacity) {
						thirdLevelMap.removeEldest();
					}

					thirdLevelMap.putValue(eldest.getKey(), eldestValue);
				}
			}
		}

		if (secondLevelMap.getCapacity() > 0) {
			secondLevelMap.putValue(key, value);
		} else if (thirdLevelCapacity > 0) {
			thirdLevelMap.putValue(key, value);
		}
	}

	/**
	 * Saves the eldest entry in the first level cache in one of the secondary
	 * levels.
	 */
	private void saveEldest() {
		Entry<K, V> eldest = firstLevelMap.removeEldest();

		if (eldest != null) {
			putReference(eldest.getKey(), eldest.getValue());
		}
	}
}
