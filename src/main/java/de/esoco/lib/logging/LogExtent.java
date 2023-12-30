//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.lib.logging;

/**
 * An enumeration of the possible extents to which logging should be performed.
 * This considers a different aspect than the log level which can be evaluated
 * by a log handler. The log extent is not evaluated by the the logging
 * framework (as it cannot detect it from a log record) but must be managed by
 * the application code. It exists in the log package only to provide a basic
 * definition of log extents but applications can use any other means to decide
 * what to log.
 *
 * <p>The log extents build up on each other so that a higher ordinal also
 * includes all lower extents (i.e. a {@link #SUCCESS} extent should also log
 * warnings and errors.</p>
 */
public enum LogExtent {
	NOTHING, ERRORS, WARNINGS, SUCCESS;

	/**
	 * Checks whether a certain log extent is included in this instance.
	 * This is
	 * the case if the other extent is lower or equal than this extent.
	 *
	 * @param extent The other log extent to check against
	 * @return TRUE if this extent includes the other extent
	 */
	public boolean logs(LogExtent extent) {
		return extent.ordinal() <= ordinal();
	}
}
