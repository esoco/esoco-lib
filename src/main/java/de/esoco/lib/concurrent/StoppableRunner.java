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
package de.esoco.lib.concurrent;

import de.esoco.lib.logging.Log;
import de.esoco.lib.manage.Stoppable;

/**
 * Extension of the {@link de.esoco.lib.concurrent.CheckableRunner} class that
 * provides a thread-safe way to request the execution to be stopped. For this
 * purpose it implements the {@link de.esoco.lib.manage.Stoppable} management
 * interface. Subclasses must implement the {@link #execute()} method and
 * therein regularly check the stop request status by invoking the method
 * {@link #stopRequested()}. If it returns TRUE the execute() method should be
 * terminated as soon as possible. Users of subclasses can query the execution
 * state of an instance with the {@link #isRunning()} method.
 *
 * @author eso
 */
public abstract class StoppableRunner extends CheckableRunner
	implements Stoppable {

	private volatile boolean bStopRequest = false;

	/**
	 * Request to stop the execution. If an implementation cannot stop
	 * immediately it should document it. The method {@link #isRunning()} can
	 * then be used to check when the execution has finally stopped.
	 */
	@Override
	public final void stop() {
		bStopRequest = true;
		Log.trace("Stop request: " + this + "[" + Thread.currentThread() +
			"]");
	}

	/**
	 * Checks if it has been requested externally to stop the execution by
	 * means
	 * of the {@link #stop()} method. This method must be queried regularly by
	 * the implementation of the {@link CheckableRunner#execute()} method
	 * implementation and execution should be stopped as soon as possible if it
	 * returns TRUE.
	 *
	 * @return TRUE if the execution should be stopped
	 */
	public final boolean stopRequested() {
		return bStopRequest;
	}
}
