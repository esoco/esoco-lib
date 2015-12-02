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

/********************************************************************
 * Interface for objects that require disposal to end their life-cycle. If an
 * object implements this interface it normally signals that it acquires special
 * resources that need to freed explicitly when the objects is no longer needed
 * and that it is not sufficient to rely on garbage collection for this task.
 * Therefore this method should be invoked by an application as soon as the
 * object is not used anymore.
 *
 * @author eso
 */
public interface Disposable
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * The method that must be invoked to end the object's life-cycle. It is
	 * recommended that implementations handle multiple invocations and problems
	 * that occur while disposing gracefully. In cases where a problem is caused
	 * by inconsistencies in the runtime environment or similar issues (or will
	 * cause them) a documented RuntimeException should be thrown.
	 */
	public void dispose();
}
