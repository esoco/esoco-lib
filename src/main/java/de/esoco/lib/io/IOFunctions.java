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
package de.esoco.lib.io;

import de.esoco.lib.expression.Predicate;
import de.esoco.lib.text.TextUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * Contains factory methods for I/O-related functions.
 */

public class IOFunctions {

	/**
	 * Private, only static use.
	 */
	private IOFunctions() {
	}

	/**
	 * Returns an implementation of {@link Predicate} and
	 * {@link FilenameFilter}
	 * that matches filenames against a simple filename pattern. The simple
	 * pattern can contain the standard wildcard characters '*' and '?'. '*'
	 * matches arbitrary character sequences, '?' matches a single character.
	 * The pattern will be converted into a regular expression by means of the
	 * method {@link TextUtil#simplePatternToRegEx(String)}.
	 *
	 * @param regex The simple filename regex
	 * @return The filename filter predicate
	 */
	public static PatternFilenameFilter ifFilenameLike(String regex) {
		Pattern pattern =
			Pattern.compile(TextUtil.simplePatternToRegEx(regex));

		return new PatternFilenameFilter(pattern);
	}

	/**
	 * Returns an implementation of {@link Predicate} and
	 * {@link FilenameFilter}
	 * that matches filenames against a regular expression.
	 *
	 * @param regex The regular expression
	 * @return The filename filter predicate
	 */
	public static PatternFilenameFilter ifFilenameMatches(String regex) {
		return new PatternFilenameFilter(Pattern.compile(regex));
	}

	/**
	 * Implementation of {@link FilenameFilter} that uses a regular expression
	 * pattern for the filename selection. Also implements the
	 * {@link Predicate}
	 * interface for {@link File} input values.
	 *
	 * @author eso
	 */
	public static class PatternFilenameFilter
		implements Predicate<File>, FilenameFilter {

		private final Pattern pattern;

		/**
		 * Creates a new FileFilter object.
		 *
		 * @param pattern The file pattern
		 */
		private PatternFilenameFilter(Pattern pattern) {
			this.pattern = pattern;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean accept(File path, String name) {
			return pattern.matcher(name).matches();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("boxing")
		public Boolean evaluate(File file) {
			return accept(file.getParentFile(), file.getName());
		}
	}
}
