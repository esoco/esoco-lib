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

/**
 * A management interface for objects that can free allocated resources when
 * being closed. Whether an object can be reused after is has been closed
 * depends on the implementation and should be documented accordingly. A
 * recommendation is to use this interface for the (temporary) closing of
 * reusable objects and to use the {@link de.esoco.lib.manage.Disposable}
 * interface for the permanent removal of instances.
 *
 * @author eso
 */
public interface Closeable extends AutoCloseable {

	/**
	 * Closes the object. It is recommended that implementations handle
	 * multiple
	 * invocations and problems that occur while closing gracefully. In cases
	 * where a problem is caused by inconsistencies in the runtime environment
	 * or similar issues (or will cause them) a documented RuntimeException
	 * should be thrown.
	 */
	@Override
	void close();
}
