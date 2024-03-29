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

import java.util.concurrent.TimeUnit;

/**
 * Contains helper functions for the handing of threads.
 *
 * @author eso
 */
public class Threads {

	/**
	 * Private, only static use.
	 */
	private Threads() {
	}

	/**
	 * Invokes {@link Thread#sleep(long)} and ignores any occurring
	 * {@link InterruptedException}.
	 *
	 * @param millis The milliseconds to wait
	 */
	public static void sleep(long millis) {
		sleep(millis, TimeUnit.MILLISECONDS);
	}

	/**
	 * A variant of {@link #sleep(long)} that accepts a {@link TimeUnit}.
	 *
	 * @param time The time to sleep
	 * @param unit The time unit
	 */
	public static void sleep(long time, TimeUnit unit) {
		try {
			unit.sleep(time);
		} catch (InterruptedException e) {
			// just continue
		}
	}
}
