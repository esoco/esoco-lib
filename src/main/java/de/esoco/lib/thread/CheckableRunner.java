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
package de.esoco.lib.thread;

import de.esoco.lib.logging.Log;
import de.esoco.lib.manage.RunCheck;


/********************************************************************
 * Base implementation of the {@link java.lang.Runnable} interface that allows
 * to check if the execution of the run() method has finished. For this pupose
 * it implements the management interface {@link de.esoco.lib.manage.RunCheck}.
 * Users of subclasses can query the execution state of an instance with the
 * {@link #isRunning()} method. Subclasses must provide the code of the task to
 * run in the abstract {@link #execute()} method.
 *
 * @author eso
 */
public abstract class CheckableRunner implements Runnable, RunCheck
{
	//~ Instance fields --------------------------------------------------------

	private boolean bRunning = false;

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Allows to check if the task is still running. This method will return
	 * FALSE either if the task has stopped running or if it did not run yet.
	 *
	 * @return TRUE if the task is still running
	 */
	@Override
	public final boolean isRunning()
	{
		return bRunning;
	}

	/***************************************
	 * Implementation of the {@link Runnable} method that will invoke the
	 * abstract {@link #execute()} method that must be implemented by
	 * subclasses.
	 */
	@Override
	public final void run()
	{
		bRunning = true;

		try
		{
			execute();
		}
		finally
		{
			bRunning = false;
			Log.trace("Stopped: " + this + "[" + Thread.currentThread() + "]");
		}
	}

	/***************************************
	 * This method must be implemented by subclasses to provide the actual
	 * execution code of a subclass.
	 */
	protected abstract void execute();
}
