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
 * Management interface that allows to start an object's execution of a thread,
 * a task, or similar. If appropriate, implementations should also implement the
 * complementary interfaces {@link de.esoco.lib.manage.Stoppable} and/or {@link
 * de.esoco.lib.manage.RunCheck} to provide a way to stop execution and check
 * for the end of execution, respectively.
 *
 * @author eso
 */
public interface Startable
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Start the object's execution.
	 *
	 * @throws Exception Implementations may throw any kind of exception if the
	 *                   start was unsuccessful
	 */
	public void start() throws Exception;
}
