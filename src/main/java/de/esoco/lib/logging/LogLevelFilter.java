//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.collection.CollectionUtil;
import de.esoco.lib.expression.Predicate;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static de.esoco.lib.logging.LogLevel.FATAL;

/**
 * A predicate implementation that filters log levels. New instances are created
 * through the static factory methods.
 *
 * @author eso
 */
public class LogLevelFilter implements Predicate<LogRecord> {

	private final Set<LogLevel> aLogLevels;

	/**
	 * Private, use factory methods to create instances.
	 *
	 * @param rLevels The log levels that may pass through this instance
	 */
	private LogLevelFilter(Set<LogLevel> rLevels) {
		aLogLevels = rLevels;

		// always add FATAL log level
		aLogLevels.add(FATAL);
	}

	/**
	 * Returns a log level filter instance for a certain collection of log
	 * levels. Independent from the argument the {@link LogLevel#FATAL} log
	 * level will always be part of the resulting filter.
	 *
	 * @param rLevels A collection containing the log levels to return a filter
	 *                for
	 * @return The log level filter for the given log levels
	 */
	public static LogLevelFilter isLevel(Collection<LogLevel> rLevels) {
		return new LogLevelFilter(EnumSet.copyOf(rLevels));
	}

	/**
	 * Returns a log level filter instance for a certain set of log levels.
	 * Independent from the argument the {@link LogLevel#FATAL} log level will
	 * always be part of the resulting filter.
	 *
	 * @param rLevels The log levels to return a filter for
	 * @return The log level filter for the given log levels
	 */
	public static LogLevelFilter isLevel(LogLevel... rLevels) {
		return isLevel(Arrays.asList(rLevels));
	}

	/**
	 * Returns a log level filter instance with a certain minimum log level.
	 * Only messages with that level or greater will be logged. The order of
	 * log
	 * levels from lower to higher severity is TRACE, DEBUG, INFO, WARN, ERROR,
	 * FATAL. The ending log level is always the level FATAL.
	 *
	 * @param rLevel The starting (minimum) log level to return a filter for
	 * @return The log level filter for the resulting log level range
	 */
	public static LogLevelFilter startingAt(LogLevel rLevel) {
		return new LogLevelFilter(EnumSet.range(rLevel, FATAL));
	}

	/**
	 * Returns TRUE if this filter contains the log level stored in the given
	 * log record.
	 *
	 * @see Predicate#evaluate(Object)
	 */
	@Override
	@SuppressWarnings("boxing")
	public Boolean evaluate(LogRecord rRecord) {
		return isLevelEnabled(rRecord.getLevel());
	}

	/**
	 * Returns the minimum log level this filter is configured for.
	 *
	 * @return The minimum level
	 */
	public LogLevel getMinimumLevel() {
		return CollectionUtil.firstElementOf(aLogLevels);
	}

	/**
	 * Checks if the argument log level is enabled for logging.
	 *
	 * @param rLevel The log level to check
	 * @return TRUE if the level is enabled for logging
	 */
	public final boolean isLevelEnabled(LogLevel rLevel) {
		return aLogLevels.contains(rLevel);
	}
}
