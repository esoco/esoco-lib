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
 * Management interface that allows to stop an object. Whether an object can be
 * started again (if it also implements {@link de.esoco.lib.manage.Startable})
 * depends on the actual implementation and should be documented accordingly.
 *
 * <p>Implementation that won't stop execution immediately can additionally
 * implement the interface {@link de.esoco.lib.manage.RunCheck} to provide a way
 * to check whether the execution has finally stopped.</p>
 *
 * @author eso
 */
public interface Stoppable
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Stops the object's execution. It is recommended that implementations
	 * handle problems that occur while stopping gracefully. In cases where the
	 * problem is caused by inconsistencies in the runtime environment or
	 * similar issues (or will cause them) a documented RuntimeException should
	 * be thrown.
	 */
	public void stop();
}
