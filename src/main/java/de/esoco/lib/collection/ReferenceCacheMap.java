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

import de.esoco.lib.collection.ReferenceCacheMap.MappedReference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;


/********************************************************************
 * A specialized cache map that manages references to the contained values with
 * a {@link ReferenceQueue}.
 *
 * @author eso
 */
public class ReferenceCacheMap<K, V> extends CacheMap<K, MappedReference<K, V>>
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long serialVersionUID = 1L;

	//~ Instance fields --------------------------------------------------------

	private boolean bSoftReferences;

	private final ReferenceQueue<V> aQueue = new ReferenceQueue<V>();

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param bSoft TRUE for soft and FALSE for weak references
	 *
	 * @see   CacheMap#CacheMap(int)
	 */
	public ReferenceCacheMap(int nCapacity, boolean bSoft)
	{
		super(nCapacity);

		bSoftReferences = bSoft;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Overridden to empty the reference queue.
	 *
	 * @see CacheMap#clear()
	 */
	@Override
	public void clear()
	{
		while (aQueue.poll() != null)
		{
		}

		super.clear();
	}

	/***************************************
	 * Overridden to process the reference queue before returning the value.
	 *
	 * @see CacheMap#get(Object)
	 */
	@Override
	public MappedReference<K, V> get(Object rKey)
	{
		cleanup();

		return super.get(rKey);
	}

	/***************************************
	 * Returns the value for a certain key.
	 *
	 * @param  rKey The key
	 *
	 * @return The value
	 */
	public V getValue(Object rKey)
	{
		MappedReference<K, V> rReference = get(rKey);

		return rReference != null ? rReference.get() : null;
	}

	/***************************************
	 * Overridden to throw an exception. Applications must use the method {@link
	 * #put(Object, Object)} instead.
	 *
	 * @see #put(Object, Object)
	 */
	@Override
	public MappedReference<K, V> put(K rKey, MappedReference<K, V> rReference)
	{
		throw new UnsupportedOperationException("Use putValue(key, value) instead");
	}

	/***************************************
	 * Adds a new reference mapping into this map.
	 *
	 * @param rKey   The key that identifies the value
	 * @param rValue The value to reference
	 */
	public void putValue(K rKey, V rValue)
	{
		cleanup();

		MappedReference<K, V> aRef =
			bSoftReferences
			? new MappedSoftReference<K, V>(rKey, rValue, aQueue)
			: new MappedWeakReference<K, V>(rKey, rValue, aQueue);

		super.put(rKey, aRef);
	}

	/***************************************
	 * @see CacheMap#remove(Object)
	 */
	@Override
	public MappedReference<K, V> remove(Object rKey)
	{
		cleanup();

		return super.remove(rKey);
	}

	/***************************************
	 * @see CacheMap#size()
	 */
	@Override
	public int size()
	{
		cleanup();

		return super.size();
	}

	/***************************************
	 * Performs a cleanup of expired references in the cache.
	 */
	@SuppressWarnings("unchecked")
	private void cleanup()
	{
		MappedReference<K, V> rReference;

		while ((rReference = (MappedReference<K, V>) aQueue.poll()) != null)
		{
			super.remove(rReference.getKey());
		}
	}

	//~ Inner Interfaces -------------------------------------------------------

	/********************************************************************
	 * The common interface for mapped references.
	 *
	 * @author eso
	 */
	public static interface MappedReference<K, V>
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * @see Reference#get()
		 */
		public abstract V get();

		/***************************************
		 * Returns the map key for the value that is referenced by this
		 * instance.
		 *
		 * @return The key value
		 */
		public abstract K getKey();
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A {@link SoftReference} subclass for mappings that also contains the key
	 * of the mapped value.
	 *
	 * @author eso
	 */
	public static class MappedSoftReference<K, V> extends SoftReference<V>
		implements MappedReference<K, V>
	{
		//~ Instance fields ----------------------------------------------------

		private final K rKey;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rKey             The key of the mapping
		 * @param rReferencedValue The mapped value that is referenced by this
		 *                         instance
		 * @param rQueue           The reference queue
		 */
		public MappedSoftReference(K				 rKey,
								   V				 rReferencedValue,
								   ReferenceQueue<V> rQueue)
		{
			super(rReferencedValue, rQueue);

			this.rKey = rKey;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Returns the map key for the value that is referenced by this
		 * instance.
		 *
		 * @return The key value
		 */
		@Override
		public final K getKey()
		{
			return rKey;
		}
	}

	/********************************************************************
	 * A {@link WeakReference} subclass for mappings that also contains the key
	 * of the mapped value.
	 *
	 * @author eso
	 */
	public static class MappedWeakReference<K, V> extends WeakReference<V>
		implements MappedReference<K, V>
	{
		//~ Instance fields ----------------------------------------------------

		private final K rKey;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rKey             The key of the mapping
		 * @param rReferencedValue The mapped value that is referenced by this
		 *                         instance
		 * @param rQueue           The reference queue
		 */
		public MappedWeakReference(K				 rKey,
								   V				 rReferencedValue,
								   ReferenceQueue<V> rQueue)
		{
			super(rReferencedValue, rQueue);

			this.rKey = rKey;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Returns the map key for the value that is referenced by this
		 * instance.
		 *
		 * @return The key value
		 */
		@Override
		public final K getKey()
		{
			return rKey;
		}
	}
}
