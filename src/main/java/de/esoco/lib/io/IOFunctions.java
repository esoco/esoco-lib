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


/********************************************************************
 * Contains factory methods for I/O-related functions.
 */

public class IOFunctions
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Private, only static use.
	 */
	private IOFunctions()
	{
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns an implementation of {@link Predicate} and {@link FilenameFilter}
	 * that matches filenames against a simple filename pattern. The simple
	 * pattern can contain the standard wildcard characters '*' and '?'. '*'
	 * matches arbitrary character sequences, '?' matches a single character.
	 * The pattern will be converted into a regular expression by means of the
	 * method {@link TextUtil#simplePatternToRegEx(String)}.
	 *
	 * @param  sPattern The simple filename pattern
	 *
	 * @return The filename filter predicate
	 */
	public static PatternFilenameFilter ifFilenameLike(String sPattern)
	{
		Pattern aPattern =
			Pattern.compile(TextUtil.simplePatternToRegEx(sPattern));

		return new PatternFilenameFilter(aPattern);
	}

	/***************************************
	 * Returns an implementation of {@link Predicate} and {@link FilenameFilter}
	 * that matches filenames against a regular expression.
	 *
	 * @param  sRegex The regular expression
	 *
	 * @return The filename filter predicate
	 */
	public static PatternFilenameFilter ifFilenameMatches(String sRegex)
	{
		return new PatternFilenameFilter(Pattern.compile(sRegex));
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * Implementation of {@link FilenameFilter} that uses a regular expression
	 * pattern for the filename selection. Also implements the {@link Predicate}
	 * interface for {@link File} input values.
	 *
	 * @author eso
	 */
	public static class PatternFilenameFilter implements Predicate<File>,
														 FilenameFilter
	{
		//~ Instance fields ----------------------------------------------------

		private Pattern rPattern;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new FileFilter object.
		 *
		 * @param rPattern The file pattern
		 */
		private PatternFilenameFilter(Pattern rPattern)
		{
			this.rPattern = rPattern;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public boolean accept(File rPath, String sName)
		{
			return rPattern.matcher(sName).matches();
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("boxing")
		public Boolean evaluate(File rFile)
		{
			return accept(rFile.getParentFile(), rFile.getName());
		}
	}
}
