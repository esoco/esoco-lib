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

import de.esoco.lib.collection.ReferenceCacheMap.MappedReference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * A specialized cache map that manages references to the contained values with
 * a {@link ReferenceQueue}.
 *
 * @author eso
 */
public class ReferenceCacheMap<K, V>
	extends CacheMap<K, MappedReference<K, V>> {

	private static final long serialVersionUID = 1L;

	private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

	private final boolean softReferences;

	/**
	 * Creates a new instance.
	 *
	 * @param soft TRUE for soft and FALSE for weak references
	 * @see CacheMap#CacheMap(int)
	 */
	public ReferenceCacheMap(int capacity, boolean soft) {
		super(capacity);

		softReferences = soft;
	}

	/**
	 * Overridden to empty the reference queue.
	 *
	 * @see CacheMap#clear()
	 */
	@Override
	public void clear() {
		while (queue.poll() != null) {
		}

		super.clear();
	}

	/**
	 * Overridden to process the reference queue before returning the value.
	 *
	 * @see CacheMap#get(Object)
	 */
	@Override
	public MappedReference<K, V> get(Object key) {
		cleanup();

		return super.get(key);
	}

	/**
	 * Returns the value for a certain key.
	 *
	 * @param key The key
	 * @return The value
	 */
	public V getValue(Object key) {
		MappedReference<K, V> reference = get(key);

		return reference != null ? reference.get() : null;
	}

	/**
	 * Overridden to throw an exception. Applications must use the method
	 * {@link #put(Object, MappedReference)} instead.
	 *
	 * @see #put(Object, MappedReference)
	 */
	@Override
	public MappedReference<K, V> put(K key, MappedReference<K, V> reference) {
		throw new UnsupportedOperationException(
			"Use putValue(key, value) instead");
	}

	/**
	 * Adds a new reference mapping into this map.
	 *
	 * @param key   The key that identifies the value
	 * @param value The value to reference
	 */
	public void putValue(K key, V value) {
		cleanup();

		MappedReference<K, V> ref = softReferences ?
		                            new MappedSoftReference<K, V>(key, value,
			                            queue) :
		                            new MappedWeakReference<K, V>(key, value,
			                            queue);

		super.put(key, ref);
	}

	/**
	 * @see CacheMap#remove(Object)
	 */
	@Override
	public MappedReference<K, V> remove(Object key) {
		cleanup();

		return super.remove(key);
	}

	/**
	 * @see CacheMap#size()
	 */
	@Override
	public int size() {
		cleanup();

		return super.size();
	}

	/**
	 * Performs a cleanup of expired references in the cache.
	 */
	@SuppressWarnings("unchecked")
	private void cleanup() {
		MappedReference<K, V> reference;

		while ((reference = (MappedReference<K, V>) queue.poll()) != null) {
			super.remove(reference.getKey());
		}
	}

	/**
	 * The common interface for mapped references.
	 *
	 * @author eso
	 */
	public interface MappedReference<K, V> {

		/**
		 * @see Reference#get()
		 */
		V get();

		/**
		 * Returns the map key for the value that is referenced by this
		 * instance.
		 *
		 * @return The key value
		 */
		K getKey();
	}

	/**
	 * A {@link SoftReference} subclass for mappings that also contains the key
	 * of the mapped value.
	 *
	 * @author eso
	 */
	public static class MappedSoftReference<K, V> extends SoftReference<V>
		implements MappedReference<K, V> {

		private final K key;

		/**
		 * Creates a new instance.
		 *
		 * @param key             The key of the mapping
		 * @param referencedValue The mapped value that is referenced by this
		 *                        instance
		 * @param queue           The reference queue
		 */
		public MappedSoftReference(K key, V referencedValue,
			ReferenceQueue<V> queue) {
			super(referencedValue, queue);

			this.key = key;
		}

		/**
		 * Returns the map key for the value that is referenced by this
		 * instance.
		 *
		 * @return The key value
		 */
		@Override
		public final K getKey() {
			return key;
		}
	}

	/**
	 * A {@link WeakReference} subclass for mappings that also contains the key
	 * of the mapped value.
	 *
	 * @author eso
	 */
	public static class MappedWeakReference<K, V> extends WeakReference<V>
		implements MappedReference<K, V> {

		private final K key;

		/**
		 * Creates a new instance.
		 *
		 * @param key             The key of the mapping
		 * @param referencedValue The mapped value that is referenced by this
		 *                        instance
		 * @param queue           The reference queue
		 */
		public MappedWeakReference(K key, V referencedValue,
			ReferenceQueue<V> queue) {
			super(referencedValue, queue);

			this.key = key;
		}

		/**
		 * Returns the map key for the value that is referenced by this
		 * instance.
		 *
		 * @return The key value
		 */
		@Override
		public final K getKey() {
			return key;
		}
	}
}
