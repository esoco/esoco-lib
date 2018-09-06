//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.lib.thread;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/********************************************************************
 * A special map implementation that associates threads with arbitrary values.
 * It contains several methods that will automatically associate the current
 * thread (queried through {@link Thread#currentThread()}) with a value. These
 * methods are {@link #put(Object)}, {@link #get()}, and {@link #remove()}.
 * Access to the internal data structures of a thread map instance is
 * synchronized so that it's methods may be invoked from different threads. The
 * values for terminated threads are automatically removed when accessing the
 * map.
 *
 * <p>This class is intended for cases where code needs access to the managed
 * {@link Thread} objects. If it is only necessary to store a thread-related
 * value it is recommended to use the {@link ThreadLocal} class instead.</p>
 *
 * @author eso
 */
public class ThreadMap<T> extends AbstractMap<Thread, T>
{
	//~ Instance fields --------------------------------------------------------

	private Map<Thread, T> aThreadMap;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new empty thread map.
	 */
	public ThreadMap()
	{
		aThreadMap = new ConcurrentHashMap<Thread, T>();
	}

	/***************************************
	 * Creates a new thread map that contains the entries of another map.
	 *
	 * @param rMap The map to copy the entries of
	 */
	public ThreadMap(Map<? extends Thread, ? extends T> rMap)
	{
		aThreadMap = new ConcurrentHashMap<Thread, T>(rMap);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the entry set of this map.
	 *
	 * @see AbstractMap#entrySet()
	 */
	@Override
	public Set<Entry<Thread, T>> entrySet()
	{
		return aThreadMap.entrySet();
	}

	/***************************************
	 * Returns the value that is associated with the current thread from which
	 * the method has been invoked.
	 *
	 * @return The value associated with the current thread, or NULL if there is
	 *         no entry for the current thread
	 */
	public T get()
	{
		cleanup();

		return get(Thread.currentThread());
	}

	/***************************************
	 * Associates a certain value with the current thread from which the method
	 * has been invoked.
	 *
	 * @param  rValue The value to associate with the current thread
	 *
	 * @return The previous value associated with the current thread, or NULL if
	 *         there was no previous entry
	 */
	public T put(T rValue)
	{
		return put(Thread.currentThread(), rValue);
	}

	/***************************************
	 * Associates a certain value with a particular thread.
	 *
	 * @param  rThread The thread to associate the value with
	 * @param  rValue  The value to associate with the given thread
	 *
	 * @return The previous value associated with the thread, or NULL if there
	 *         was no previous entry
	 */
	@Override
	public T put(Thread rThread, T rValue)
	{
		cleanup();

		return aThreadMap.put(rThread, rValue);
	}

	/***************************************
	 * Removes the mapping for the current thread from which the method has been
	 * invoked.
	 *
	 * @return The value that has been associated with the current thread, or
	 *         NULL if there hasn't been an entry for the current thread
	 */
	public T remove()
	{
		return remove(Thread.currentThread());
	}

	/***************************************
	 * Removes all terminated threads from this map.
	 */
	private void cleanup()
	{
		Iterator<Thread> rIterator = aThreadMap.keySet().iterator();

		while (rIterator.hasNext())
		{
			Thread rThread = rIterator.next();

			if (!rThread.isAlive())
			{
				rIterator.remove();
			}
		}
	}
}
