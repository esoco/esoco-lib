//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.lib.app;

import de.esoco.lib.logging.Log;
import de.esoco.lib.text.TextUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/********************************************************************
 * Parses a command line and provides access to the switches and the optional
 * switch values of the command line. If only the parsing of a command line is
 * needed the static methods of this method can be used.
 *
 * @author eso
 */
public class CommandLine
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * Default argument pattern; allows arbitrary case-insensitive command line
	 * switches (if they fulfill the regex pattern "\\w|\?") of the form
	 * -&lt;switch&gt;[=&lt;value&gt;] (the prefix '/' can be used instead of of
	 * '-').
	 */
	public static final String DEFAULT_ARGUMENT_PATTERN =
		"(?i)[-/]((?:\\w|\\?)+)(?:=(.+))?";

	//~ Instance fields --------------------------------------------------------

	Map<String, Object> aCommandLineSwitches;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new CommandLine object with the default argument pattern that
	 * allows arbitrary case-insensitive switches.
	 *
	 * @param  rArgs The list of command line arguments
	 *
	 * @throws IllegalArgumentException If one of the arguments is invalid
	 */
	public CommandLine(String[] rArgs)
	{
		this(rArgs, Pattern.compile(DEFAULT_ARGUMENT_PATTERN));
	}

	/***************************************
	 * Creates a new CommandLine object with a standard argument pattern that
	 * allows a certain set of switches. The corresponding pattern will be
	 * created by invoking {@link #createStandardPattern(String[])}.
	 *
	 * @param  rArgs            The list of command line arguments
	 * @param  rAllowedSwitches A list containing all the switches that are
	 *                          allowed on the command line
	 *
	 * @throws IllegalArgumentException If one of the arguments is invalid
	 */
	public CommandLine(String[] rArgs, String... rAllowedSwitches)
	{
		this(rArgs, createStandardPattern(rAllowedSwitches));
	}

	/***************************************
	 * Creates a new command line instance. Parses the command line withe the
	 * static {@link #parse(String[], Pattern) parse()} method.
	 *
	 * @param  rArgs       The list of command line arguments
	 * @param  rArgPattern The pattern to parse a single command line switch or
	 *                     NULL for the default pattern
	 *
	 * @throws IllegalArgumentException If either the argument pattern or an
	 *                                  argument is invalid
	 */
	public CommandLine(String[] rArgs, Pattern rArgPattern)
	{
		aCommandLineSwitches = parse(rArgs, rArgPattern);
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Creates a command line pattern from a list of allowed switch values. All
	 * parameter values may contain regular expression elements like character
	 * classes as long as they don't conflict with the regular expression
	 * requirements of the {@link #parse(String[], Pattern) parse()} method.
	 * That means especially that they must not contain capturing groups as
	 * these are used to find switches and switch values.
	 *
	 * <p>To make the assigment of a value to a switch mandatory the assignment
	 * token must be appended to it. For example, to enforce that switch
	 * '-value' gets a value assigned, use the string 'value=' (if the
	 * assignment token is'=').</p>
	 *
	 * <p>If the list of allowed switches is emtpy and a command line is parsed
	 * with the resulting pattern, any switch will cause an exception to be
	 * thrown.</p>
	 *
	 * @param  sPrefix          The prefix(es) for a switch
	 * @param  sAssignment      The string or character used to assign a value
	 *                          to a switch
	 * @param  rAllowedSwitches A list containing all the switches that are
	 *                          allowed on the command line
	 *
	 * @return A new pattern based on the method arguments
	 */
	public static Pattern createPattern(String    sPrefix,
										String    sAssignment,
										String... rAllowedSwitches)
	{
		StringBuilder aPattern		 = new StringBuilder(sPrefix + "(?:(");
		StringBuilder aValueSwitches = new StringBuilder();

		for (String sSwitch : rAllowedSwitches)
		{
			boolean bValueRequired = sSwitch.endsWith(sAssignment);

			if (bValueRequired)
			{
				int l = sSwitch.length() - sAssignment.length();

				aValueSwitches.append(sSwitch.substring(0, l)).append('|');
			}
			else
			{
				aPattern.append(sSwitch).append('|');
			}
		}

		// if prefix is -, assignment is =, sn are switches, and pn are
		// switches with mandatory parameters, the resulting pattern will be:
		// -(?:(s1|s2|...)(?:=(.+))?|(p1|p2|...)=(.+))
		// group 1 & 2 for simple switches, 3 & 4 for parameterized switches

		if (aPattern.charAt(aPattern.length() - 1) == '|')
		{
			// remove trailing '|'
			aPattern.setLength(aPattern.length() - 1);
			aPattern.append(")(?:").append(sAssignment).append("(.+))?");

			if (aValueSwitches.length() > 0)
			{
				aPattern.append("|(");
			}
		}

		if (aValueSwitches.length() > 0)
		{
			// remove trailing '|'
			aValueSwitches.setLength(aValueSwitches.length() - 1);
			aPattern.append(aValueSwitches).append(")");
			aPattern.append(sAssignment).append("(.+)");
		}

		aPattern.append(")");

		if (rAllowedSwitches.length == 0)
		{
			aPattern.append(")");
		}

		return Pattern.compile(aPattern.toString());
	}

	/***************************************
	 * Creates a standard command line pattern from a list of allowed switch
	 * values of the form -&lt;switch&gt;[=&lt;value&gt;] (the prefix '/' can be
	 * used instead of of '-'). Based on the method {@link
	 * #createPattern(String, String, String[])}.
	 *
	 * @param  rAllowedSwitches A list containing all the switches that are
	 *                          allowed on the command line
	 *
	 * @return A new pattern based on the method arguments
	 */
	public static Pattern createStandardPattern(String... rAllowedSwitches)
	{
		return createPattern("[-/]", "=", rAllowedSwitches);
	}

	/***************************************
	 * Parses a list of command line arguments into a map with the default
	 * argument pattern that allows arbitrary case-insensitive switches.
	 *
	 * @param  rArgs The list of command line arguments
	 *
	 * @return A new map containing all command line switches together with
	 *         their values; will be empty if the argument list is empty
	 *
	 * @throws IllegalArgumentException If one of the arguments is invalid
	 */
	public static Map<String, Object> parse(String[] rArgs)
	{
		return parse(rArgs, Pattern.compile(DEFAULT_ARGUMENT_PATTERN));
	}

	/***************************************
	 * Parses a list of command line arguments into a map. Each element in the
	 * rArgs array will be matched against the argument pattern (a default if
	 * the value of the rArgPattern parameter is NULL). If the pattern matches
	 * the first group in the pattern will be considered to be a command line
	 * switch and stored in the result map as a key. If the matched pattern
	 * contains another group it's string value will be converted into an object
	 * by invoking {@link TextUtil#parseObject(String)}. The result will be
	 * stored in the map as the value associated with the command line switch.
	 * If the pattern contains only a single group the value associated with a
	 * switch will be NULL.
	 *
	 * <p>To allow correct parsing of the arguments the argument pattern must
	 * contain at least one capturing group that identifies the name of the
	 * switch. If switches are allowed to have values a second capturing group
	 * must define the switch value. It is possible for the pattern to contain
	 * more than two groups, but a maximum of two groups must be valid (i.e. not
	 * NULL). These two groups will then be used as the switch and it's value,
	 * respectively.</p>
	 *
	 * <p>The pattern's flag CASE_INSENSITIVE controls if similar switches with
	 * different case are to be distinguished. The default pattern allows
	 * arbitrary case-insensitive command line switches of the form
	 * -&lt;switch&gt;[=&lt;value&gt;] (the prefix '/' can be used instead of of
	 * '-').</p>
	 *
	 * <p>The returned map will contain the switches in the same order they
	 * appear in the argument list. If the same switch appears multiple times in
	 * the command line the value of the last occurence will be contained in the
	 * result map.</p>
	 *
	 * @param  rArgs       The list of command line arguments
	 * @param  rArgPattern The pattern to parse a single command line switch or
	 *                     NULL for the default pattern
	 *
	 * @return A new map containing all command line switches together with
	 *         their values; will be empty if the argument list is empty
	 *
	 * @throws IllegalArgumentException If an argument cannot be parsed
	 */
	public static Map<String, Object> parse(String[] rArgs, Pattern rArgPattern)
	{
		Map<String, Object> aSwitches =
			new LinkedHashMap<String, Object>(rArgs.length);

		for (String sArg : rArgs)
		{
			Matcher aArgMatcher = rArgPattern.matcher(sArg);
			boolean bMatch	    = aArgMatcher.matches();

			if (bMatch && aArgMatcher.groupCount() > 0)
			{
				int nGroup = TextUtil.nextGroup(aArgMatcher, 1);

				if (nGroup == -1)
				{
					String sErr = "Invalid switch: " + sArg;

					Log.debug(sErr + " / Pattern: " + rArgPattern.pattern());
					throw new IllegalArgumentException(sErr);
				}

				String sSwitch = aArgMatcher.group(nGroup);
				Object rValue  = null;

				nGroup = TextUtil.nextGroup(aArgMatcher, nGroup + 1);

				if (nGroup != -1)
				{
					rValue = TextUtil.parseObject(aArgMatcher.group(nGroup));
				}

				if ((rArgPattern.flags() & Pattern.CASE_INSENSITIVE) != 0)
				{
					sSwitch = sSwitch.toLowerCase();
				}

				aSwitches.put(sSwitch, rValue);
			}
			else
			{
				String sErr;

				if (bMatch && aArgMatcher.groupCount() == 0)
				{
					sErr = "Invalid switch format: " + sArg;
				}
				else
				{
					sErr = "Invalid switch: " + sArg;
				}

				Log.debug(sErr + " / Pattern: " + rArgPattern.pattern());

				throw new IllegalArgumentException(sErr);
			}
		}

		return aSwitches;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the value of a certain switch. If the switch doesn't exist in
	 * this command line or if no value has been set for it NULL will be
	 * returned. The method {@link #hasSwitch(String)} allows to check if a
	 * switch exists.
	 *
	 * <p>This method is case-sensitive. If an application has created the
	 * command line to be case-insensitive (e.g. by using the default pattern)
	 * it must always invoke this method with lower case switch values because
	 * all switch have been converted to lower case on creation.</p>
	 *
	 * @param  sSwitch The switch to check
	 *
	 * @return TRUE if the switch exists
	 */
	public Object getSwitchValue(String sSwitch)
	{
		return aCommandLineSwitches.get(sSwitch);
	}

	/***************************************
	 * Checks if a certain switch exists in this command line. This method is
	 * case-sensitive. If an application has created the command line to be
	 * case-insensitive (e.g. by using the default pattern) it must always
	 * invoke this method with lower case switch values because all switch have
	 * been converted to lower case on creation.
	 *
	 * @param  sSwitch The switch to check
	 *
	 * @return TRUE if the switch exists
	 */
	public boolean hasSwitch(String sSwitch)
	{
		return aCommandLineSwitches.containsKey(sSwitch);
	}

	/***************************************
	 * Returns a string representation of this command line.
	 *
	 * @return A string description
	 */
	@Override
	public String toString()
	{
		return "CommandLine[" + aCommandLineSwitches + "]";
	}
}
