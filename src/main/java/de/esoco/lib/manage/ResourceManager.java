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
package de.esoco.lib.manage;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Thread-safe resource manager that manages reference counts for the contained
 * resource objects and disposes them if the count reaches zero. For the
 * disposal it uses either the interface {@link Disposable} or uses reflection
 * to search for a method of type 'public void dispose()'. If a managed object
 * also implements the {@link de.esoco.lib.manage.Closeable} interface it's
 * close() method will be invoked before the dispose() method.
 *
 * <p>This class implements the Disposable interface itself so that it can be
 * registered for automatic shutdown.</p>
 *
 * @author eso
 */
public class ResourceManager implements Disposable {

	Map<Object, ReferenceCount> resourceMap =
		new HashMap<Object, ReferenceCount>();

	/**
	 * Creates a new ResourceManager instance.
	 */
	public ResourceManager() {
	}

	/**
	 * Adds a single use of an object to this resource manager. If the
	 * object is
	 * not already managed it will be added to the internal cache. Else only
	 * it's reference count will be incremented.
	 *
	 * @param iD     The ID to register the object with
	 * @param object The instance to add
	 */
	public void add(Object iD, Object object) {
		synchronized (resourceMap) {
			ReferenceCount rc = resourceMap.get(iD);

			if (rc == null) {
				rc = new ReferenceCount(object);
				resourceMap.put(iD, rc);
			} else {
				rc.increment();
			}
		}
	}

	/**
	 * Implementation of the {@link Disposable#dispose()} method that removes
	 * all managed resources and disposes all contained objects.
	 */
	@Override
	public void dispose() {
		synchronized (resourceMap) {
			Iterator<ReferenceCount> i = resourceMap.values().iterator();

			while (i.hasNext()) {
				ReferenceCount rc = i.next();

				i.remove();
				disposeObject(rc.getObject());
			}
		}
	}

	/**
	 * Simply returns a resource object for a particular ID from this manager.
	 * Neither the reference count nor the object state are changed by this
	 * method.
	 *
	 * @param iD The ID of the managed object
	 * @return The object associated ID or NULL if no such object exists
	 */
	public Object get(Object iD) {
		ReferenceCount rc = resourceMap.get(iD);

		if (rc != null) {
			return rc.getObject();
		} else {
			return null;
		}
	}

	/**
	 * Releases a single use of an object from the resource manager. The
	 * reference count of the object is decremented and if it reaches zero, the
	 * referenced object will be disposed and removed from the internal cache.
	 *
	 * @param iD The ID of the instance to be released from the manager
	 */
	public void release(Object iD) {
		synchronized (resourceMap) {
			ReferenceCount rc = resourceMap.get(iD);

			if (rc != null && rc.decrement()) {
				Object object = resourceMap.remove(iD).getObject();

				// disposing must me done AFTER removing because dispose may
				// change the results of the hashCode() and equals() methods in
				// some cases (e.g. SWT)
				disposeObject(object);
			}
		}
	}

	/**
	 * Removes an object from the resource manager completely. The reference
	 * count of the object will be ignored and the referenced object will be
	 * disposed and removed from the internal cache.
	 *
	 * @param iD The ID of the instance to be removed from the manager
	 */
	public void remove(Object iD) {
		synchronized (resourceMap) {
			if (resourceMap.containsKey(iD)) {
				Object object = resourceMap.remove(iD).getObject();

				// disposing must me done AFTER removing because dispose may
				// change the results of the hashCode() and equals() methods in
				// some cases (e.g. SWT)
				disposeObject(object);
			}
		}
	}

	/**
	 * Internal method that disposes an object. If the object implements the
	 * {@link Disposable} interface or a dispose() method can be found through
	 * reflection dispose() it will be invoked. If the object implements the
	 * {@link Closeable} interface it's close() method will be invoked before
	 * disposing it.
	 *
	 * @param object The object to dispose
	 * @throws RuntimeException If disposing the object fails
	 */
	private void disposeObject(Object object) {
		if (object instanceof Closeable) {
			((Closeable) object).close();
		}

		if (object instanceof Disposable) {
			((Disposable) object).dispose();
		} else {
			try {
				Method dispose = object.getClass().getMethod("dispose");

				dispose.invoke(object);
			} catch (NoSuchMethodException e) {
				// ok, we don't have a public dispose method, so we
				// ignore the exception
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Inner class that encapsulates the usage count of a single object
	 * reference.
	 */
	private static class ReferenceCount {

		private final Object object;

		private int count = 0;

		/**
		 * Creates a new instance for a particular object and sets the
		 * reference
		 * count to 1.
		 *
		 * @param object The object the reference count is managed for
		 */
		public ReferenceCount(Object object) {
			this.object = object;
			increment();
		}

		/**
		 * Decrements the reference counter by one. The resulting status of the
		 * reference will be returned. If the reference count reaches zero the
		 * reference is considered unused and TRUE is returned.
		 *
		 * @return TRUE if the reference count has reached zero
		 */
		public final boolean decrement() {
			return (--count == 0);
		}

		/**
		 * Returns the object this reference count is managing.
		 *
		 * @return The managed object
		 */
		public final Object getObject() {
			return object;
		}

		/**
		 * Increments the reference count by one.
		 */
		public final void increment() {
			count++;
		}

		/**
		 * Returns a string representation of this reference count.
		 *
		 * @return A string
		 */
		@Override
		public String toString() {
			return "ReferenceCount[" + count + "]";
		}
	}
}
