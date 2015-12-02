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
 * A management interface that provides a way to check if an object's thread or
 * task execution is still running. Where appropriate, this interface should be
 * implemented in conjunction with the other management interfaces {@link
 * de.esoco.lib.manage.Stoppable} and {@link de.esoco.lib.manage.Startable}.
 * Additionally it can be used separately for objects that automatically stop
 * running after some time.
 *
 * @author eso
 */
public interface RunCheck
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns TRUE if the object is currently executing a task. Returns FALSE
	 * either when it has already finished or not yet started execution.
	 *
	 * @return TRUE if the task is still running
	 */
	public boolean isRunning();
}
