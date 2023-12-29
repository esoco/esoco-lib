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

	Map<Object, ReferenceCount> aResourceMap =
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
	 * @param rID     The ID to register the object with
	 * @param rObject The instance to add
	 */
	public void add(Object rID, Object rObject) {
		synchronized (aResourceMap) {
			ReferenceCount rc = aResourceMap.get(rID);

			if (rc == null) {
				rc = new ReferenceCount(rObject);
				aResourceMap.put(rID, rc);
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
		synchronized (aResourceMap) {
			Iterator<ReferenceCount> i = aResourceMap.values().iterator();

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
	 * @param rID The ID of the managed object
	 * @return The object associated ID or NULL if no such object exists
	 */
	public Object get(Object rID) {
		ReferenceCount rc = aResourceMap.get(rID);

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
	 * @param rID The ID of the instance to be released from the manager
	 */
	public void release(Object rID) {
		synchronized (aResourceMap) {
			ReferenceCount rc = aResourceMap.get(rID);

			if (rc != null && rc.decrement()) {
				Object rObject = aResourceMap.remove(rID).getObject();

				// disposing must me done AFTER removing because dispose may
				// change the results of the hashCode() and equals() methods in
				// some cases (e.g. SWT)
				disposeObject(rObject);
			}
		}
	}

	/**
	 * Removes an object from the resource manager completely. The reference
	 * count of the object will be ignored and the referenced object will be
	 * disposed and removed from the internal cache.
	 *
	 * @param rID The ID of the instance to be removed from the manager
	 */
	public void remove(Object rID) {
		synchronized (aResourceMap) {
			if (aResourceMap.containsKey(rID)) {
				Object rObject = aResourceMap.remove(rID).getObject();

				// disposing must me done AFTER removing because dispose may
				// change the results of the hashCode() and equals() methods in
				// some cases (e.g. SWT)
				disposeObject(rObject);
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
	 * @param rObject The object to dispose
	 * @throws RuntimeException If disposing the object fails
	 */
	private void disposeObject(Object rObject) {
		if (rObject instanceof Closeable) {
			((Closeable) rObject).close();
		}

		if (rObject instanceof Disposable) {
			((Disposable) rObject).dispose();
		} else {
			try {
				Method dispose = rObject.getClass().getMethod("dispose");

				dispose.invoke(rObject);
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

		private final Object rObject;

		private int nCount = 0;

		/**
		 * Creates a new instance for a particular object and sets the
		 * reference
		 * count to 1.
		 *
		 * @param rObject The object the reference count is managed for
		 */
		public ReferenceCount(Object rObject) {
			this.rObject = rObject;
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
			return (--nCount == 0);
		}

		/**
		 * Returns the object this reference count is managing.
		 *
		 * @return The managed object
		 */
		public final Object getObject() {
			return rObject;
		}

		/**
		 * Increments the reference count by one.
		 */
		public final void increment() {
			nCount++;
		}

		/**
		 * Returns a string representation of this reference count.
		 *
		 * @return A string
		 */
		@Override
		public String toString() {
			return "ReferenceCount[" + nCount + "]";
		}
	}
}
