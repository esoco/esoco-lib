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
package de.esoco.lib.app;

import de.esoco.lib.expression.Predicate;
import de.esoco.lib.text.TextUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/********************************************************************
 * Parses a command line and provides access to the options and the optional
 * option values of the command line. If only the parsing of a command line is
 * needed the static methods of this method can be used.
 *
 * @author eso
 */
public class CommandLine
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * The key that can be used to query the remaining arguments that didn't
	 * match the command line options pattern. The datatype is a list of strings
	 * that is always present, but may be empty.
	 */
	public static final String EXTRA_ARGUMENTS = "__CMDLINE_EXTRA_ARGS";

	/**
	 * The default pattern for option arguments. Allows arbitrary
	 * case-insensitive command line options (if they fulfill the regular
	 * expression pattern "\\w|\?") of the form -&lt;option&gt;[=&lt;value&gt;]
	 * (the prefix '/' can be used instead of of '-').
	 */
	public static final String DEFAULT_OPTION_PATTERN =
		"(?i)-([\\w-_]+)(?:=(.+))?";

	//~ Instance fields --------------------------------------------------------

	private Map<String, Object> aCommandLineOptions;
	private Map<String, String> rAllowedOptions = null;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new command line from an input stream in Java properties file
	 * format. Boolean values will be converted to {@link Boolean} objects, all
	 * other argument values will be stored as strings.
	 *
	 * @param  rArgsStream sFileName The file name (including path) of the
	 *                     properties file
	 *
	 * @throws IllegalArgumentException If accessing the stream fails
	 */
	public CommandLine(InputStream rArgsStream)
	{
		aCommandLineOptions = readArguments(rArgsStream);
	}

	/***************************************
	 * Creates a new command line that allows arbitrary arguments.
	 *
	 * @param rArgs The command line arguments
	 */
	public CommandLine(String[] rArgs)
	{
		this(rArgs, null);
	}

	/***************************************
	 * Creates a new CommandLine object with a standard argument pattern that
	 * allows a certain set of options. If the allowed options are not empty the
	 * corresponding pattern will be created by invoking {@link
	 * #createStandardPattern(String[])}. Otherwise the {@link
	 * #DEFAULT_OPTION_PATTERN} will be used.
	 *
	 * @param  rArgs           The command line arguments
	 * @param  rAllowedOptions A list containing all the options that are
	 *                         allowed on the command line
	 *
	 * @throws IllegalArgumentException If one of the arguments is invalid
	 */
	public CommandLine(String[] rArgs, Map<String, String> rAllowedOptions)
	{
		this(rArgs, Pattern.compile(DEFAULT_OPTION_PATTERN), rAllowedOptions);
	}

	/***************************************
	 * Creates a new command line instance. Parses the command line with the
	 * static {@link #parse(String[], Pattern) parse()} method.
	 *
	 * @param  rArgs           The command line arguments
	 * @param  rArgPattern     The pattern to parse a single command line option
	 *                         or NULL for the default pattern
	 * @param  rAllowedOptions A list containing all the options that are
	 *                         allowed on the command line
	 *
	 * @throws IllegalArgumentException If either the argument pattern or an
	 *                                  argument is invalid
	 */
	public CommandLine(String[]			   rArgs,
					   Pattern			   rArgPattern,
					   Map<String, String> rAllowedOptions)
	{
		this.aCommandLineOptions = parse(rArgs, rArgPattern);
		this.rAllowedOptions     = rAllowedOptions;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Creates a command line pattern from a list of allowed option names. All
	 * parameter values may contain regular expression elements like character
	 * classes as long as they don't conflict with the regular expression
	 * requirements of the {@link #parse(String[], Pattern) parse()} method.
	 * That means especially that they must not contain capturing groups as
	 * these are used to find options and their values.
	 *
	 * <p>To make the assignment of a value to an option mandatory the
	 * assignment token must be appended to it. For example, to enforce that
	 * option '-value' gets a value assigned, use the string 'value=' (if the
	 * assignment token is'=').</p>
	 *
	 * <p>If the list of allowed options is emtpy and a command line is parsed
	 * with the resulting pattern, any option will cause an exception to be
	 * thrown.</p>
	 *
	 * @param  sPrefix         The allowed prefixes for an option
	 * @param  sAssignment     The string or character used to assign a value to
	 *                         an option
	 * @param  rAllowedOptions A list containing all the options that are
	 *                         allowed on the command line
	 *
	 * @return A new pattern based on the method arguments
	 */
	public static Pattern createPattern(String    sPrefix,
										String    sAssignment,
										String... rAllowedOptions)
	{
		StringBuilder aPattern	    = new StringBuilder(sPrefix + "(");
		StringBuilder aValueOptions = new StringBuilder();

		for (String sOption : rAllowedOptions)
		{
			boolean bValueRequired = sOption.endsWith(sAssignment);

			if (bValueRequired)
			{
				int l = sOption.length() - sAssignment.length();

				aValueOptions.append(sOption.substring(0, l)).append('|');
			}
			else
			{
				aPattern.append(sOption).append('|');
			}
		}

		// if prefix is -, assignment is =, sn are options, and pn are
		// options with mandatory parameters, the resulting pattern will be:
		// -(?:(s1|s2|...)(?:=(.+))?|(p1|p2|...)=(.+))
		// group 1 & 2 for simple options, 3 & 4 for parameterized options

		if (aPattern.charAt(aPattern.length() - 1) == '|')
		{
			// remove trailing '|'
			aPattern.setLength(aPattern.length() - 1);
			aPattern.append(")(?:").append(sAssignment).append("(.+))?");

			if (aValueOptions.length() > 0)
			{
				aPattern.append("|(");
			}
		}

		if (aValueOptions.length() > 0)
		{
			// remove trailing '|'
			aValueOptions.setLength(aValueOptions.length() - 1);
			aPattern.append(aValueOptions).append(")");
			aPattern.append(sAssignment).append("(.+)");
		}

		if (rAllowedOptions.length == 0)
		{
			aPattern.append(")");
		}

		return Pattern.compile(aPattern.toString());
	}

	/***************************************
	 * Creates a standard command line pattern from a list of allowed option
	 * names of the form -&lt;option&gt;[=&lt;value&gt;] (the prefix '/' can be
	 * used instead of of '-'). Based on the method {@link
	 * #createPattern(String, String, String[])}.
	 *
	 * @param  rAllowedOptions A list containing all the options that are
	 *                         allowed on the command line
	 *
	 * @return A new pattern based on the method arguments
	 */
	public static Pattern createStandardPattern(String... rAllowedOptions)
	{
		return createPattern("-", "=", rAllowedOptions);
	}

	/***************************************
	 * Tries to read the command line arguments from a Java properties file.
	 *
	 * @param  sFileName The file name (including path) of the properties file
	 *
	 * @return A map containing the arguments as read from the file
	 *
	 * @throws IllegalArgumentException If the file doesn't exist or could not
	 *                                  be read
	 */
	public static Map<String, Object> readArguments(String sFileName)
	{
		try
		{
			return readArguments(new FileInputStream(sFileName));
		}
		catch (FileNotFoundException e)
		{
			throw new IllegalArgumentException("Arguments file not found: " +
											   sFileName);
		}
	}

	/***************************************
	 * Reads the command line arguments from an input stream in Java properties
	 * file format. Boolean values will be converted to {@link Boolean} objects,
	 * all other argument values will be stored as strings.
	 *
	 * @param  rArgsStream The input stream to read the arguments from
	 *
	 * @return A map containing the arguments as read from the stream
	 *
	 * @throws IllegalArgumentException If accessing the stream fails
	 */
	public static Map<String, Object> readArguments(InputStream rArgsStream)
	{
		Properties aProperties = new Properties();

		try
		{
			aProperties.load(rArgsStream);
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("Could not read arguments", e);
		}

		Map<String, Object> aFileArguments = new HashMap<>(aProperties.size());

		for (Entry<Object, Object> rProperty : aProperties.entrySet())
		{
			Object rValue = rProperty.getValue();

			if ("true".equalsIgnoreCase(rValue.toString()))
			{
				rValue = Boolean.TRUE;
			}
			else if ("false".equalsIgnoreCase(rValue.toString()))
			{
				rValue = Boolean.FALSE;
			}

			aFileArguments.put(rProperty.getKey().toString(), rValue);
		}

		return aFileArguments;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the arguments of this command line that are not options. The
	 * returned list contains all arguments that didn't match the options
	 * pattern as they appeared in the original command line. If no such
	 * arguments exist the list will be empty.
	 *
	 * @return The list of unparsed arguments (may be empty but will never be
	 *         NULL)
	 */
	@SuppressWarnings("unchecked")
	public List<String> getArguments()
	{
		return (List<String>) aCommandLineOptions.get(EXTRA_ARGUMENTS);
	}

	/***************************************
	 * Returns the value of a certain option. If the option exists but has no
	 * value (i.e. a switch option) the result will be {@link Boolean#TRUE}. If
	 * the option doesn't exist in this command line NULL will be returned. The
	 * method {@link #hasOption(String)} also allows to check if an option
	 * exists.
	 *
	 * <p>This method is case-sensitive. If an application has created the
	 * command line to be case-insensitive (e.g. by using the default pattern)
	 * it must always invoke this method with lower case option names because
	 * all option names have been converted to lower case on creation.</p>
	 *
	 * @param  sOption The option to query
	 *
	 * @return The option value or NULL for none
	 */
	public Object getOption(String sOption)
	{
		return aCommandLineOptions.get(sOption);
	}

	/***************************************
	 * Convenience method that converts the result of {@link #getOption(String)}
	 * to a string.
	 *
	 * @param  sOption The option name
	 *
	 * @return The option string or NULL for none
	 */
	public String getString(String sOption)
	{
		Object rOption = getOption(sOption);

		return rOption != null ? rOption.toString() : null;
	}

	/***************************************
	 * Checks if a certain option exists in this command line. This method is
	 * case-sensitive. If an application has created the command line to be
	 * case-insensitive (e.g. by using the default pattern) it must always
	 * invoke this method with lower case option names because all option names
	 * have been converted to lower case on creation.
	 *
	 * @param  sOption The option to check
	 *
	 * @return TRUE if the option exists
	 */
	public boolean hasOption(String sOption)
	{
		return aCommandLineOptions.containsKey(sOption);
	}

	/***************************************
	 * Parses a list of command line arguments into a map with the default
	 * argument pattern that allows arbitrary case-insensitive options.
	 *
	 * @param  rArgs The list of command line arguments
	 *
	 * @return A new map containing all command line options together with their
	 *         values; will be empty if the argument list is empty
	 *
	 * @throws IllegalArgumentException If one of the arguments is invalid
	 */
	public Map<String, Object> parse(String[] rArgs)
	{
		return parse(rArgs, Pattern.compile(DEFAULT_OPTION_PATTERN));
	}

	/***************************************
	 * Parses a list of command line arguments into a map. Each element in the
	 * rArgs array will be matched against the given regular expression pattern.
	 * If the pattern matches the first group in the pattern will be considered
	 * to be a command line option and stored in the result map as a key. If the
	 * matched pattern contains another group it's string value will be
	 * converted by invoking {@link TextUtil#parseObject(String)} and the result
	 * will be stored in the map as the value associated with the key (= the
	 * command line option). If the pattern contains only a single group the
	 * value associated with an option will be {@link Boolean#TRUE}.
	 *
	 * <p>To allow correct parsing of the arguments the argument pattern must
	 * contain at least one capturing group that identifies the name of the
	 * option. If options are allowed to have values a second capturing group
	 * must define the option value. It is possible for the pattern to contain
	 * more than two groups, but a maximum of two groups must be valid (i.e. not
	 * NULL). These two groups will then be used as the option and it's value,
	 * respectively.</p>
	 *
	 * <p>The pattern's flag CASE_INSENSITIVE controls if similar options with
	 * different case are to be distinguished. The default pattern allows
	 * arbitrary case-insensitive command line options of the form
	 * -&lt;option&gt;[=&lt;value&gt;] (the prefix '/' can be used instead of of
	 * '-').</p>
	 *
	 * <p>The returned map will contain the options in the same order they
	 * appear in the argument list. If the same option appears multiple times in
	 * the command line the value of the last occurrence will be contained in
	 * the result map.</p>
	 *
	 * <p>If the default option pattern is used any values on the command line
	 * that don't match the option pattern will be placed in a {@link List} of
	 * unparsed arguments that is stored in the returned map under the key
	 * {@link #EXTRA_ARGUMENTS}. This list will always exist but may be
	 * empty.</p>
	 *
	 * @param  rArgs       The original command line arguments
	 * @param  rArgPattern The pattern to parse a single command line option or
	 *                     NULL for the default pattern
	 *
	 * @return A new map containing all command line options together with their
	 *         values; will be empty if the argument list is empty
	 *
	 * @throws CommandLineException If an argument cannot be parsed
	 */
	public Map<String, Object> parse(String[] rArgs, Pattern rArgPattern)
	{
		Map<String, Object> aResult =
			new LinkedHashMap<String, Object>(rArgs.length);

		List<String> aExtraArguments = new ArrayList<>();
		String		 sPrevOption     = null;

		for (String sArg : rArgs)
		{
			Matcher aArgMatcher = rArgPattern.matcher(sArg);
			boolean bIsArg	    = aArgMatcher.matches();

			if (bIsArg)
			{
				if (aArgMatcher.groupCount() > 0)
				{
					int nGroup = TextUtil.nextGroup(aArgMatcher, 1);

					if (nGroup == -1)
					{
						throw new IllegalArgumentException("Invalid option: " +
														   sArg);
					}

					String sOption = aArgMatcher.group(nGroup);
					Object rValue  = Boolean.TRUE;

					nGroup = TextUtil.nextGroup(aArgMatcher, nGroup + 1);

					if (nGroup != -1)
					{
						rValue =
							TextUtil.parseObject(aArgMatcher.group(nGroup));
					}

					if ((rArgPattern.flags() & Pattern.CASE_INSENSITIVE) != 0)
					{
						sOption = sOption.toLowerCase();
					}

					if (sOption.equals("-args"))
					{
						if (rValue instanceof String)
						{
							aResult.putAll(readArguments(rValue.toString()));
						}
						else
						{
							throw new CommandLineException("--args must point " +
														   "to an argument " +
														   "properties file",
														   "--args");
						}
					}
					else if (rAllowedOptions != null)
					{
						if (!rAllowedOptions.containsKey(sOption))
						{
							throw new CommandLineException("Unsupported option: " +
														   sArg,
														   sArg);
						}
					}
					else
					{
						aResult.put(sOption, rValue);
						sPrevOption = sOption;
					}
				}
				else
				{
					throw new CommandLineException("Invalid option: " + sArg,
												   sArg);
				}
			}
			else if (sPrevOption != null)
			{
				aResult.put(sPrevOption, TextUtil.parseObject(sArg));
				sPrevOption = null;
			}
			else
			{
				aExtraArguments.add(sArg);
			}
		}

		aResult.put(EXTRA_ARGUMENTS, aExtraArguments);

		return aResult;
	}

	/***************************************
	 * Returns a mandatory command line option without validation.
	 *
	 * @see #requireOption(String, Predicate)
	 */
	public Object requireOption(String sOption) throws CommandLineException
	{
		return requireOption(sOption, null);
	}

	/***************************************
	 * Returns a mandatory value of a command line option and optionally that it
	 * fulfills certain criteria that are asserted by applying a predicate. If
	 * the option value doesn't exist or the predicate yields FALSE for it an
	 * exception will be thrown.
	 *
	 * @param  sOption        The command line option to return the value of
	 * @param  pIsValidOption An optional predicate to apply to option values or
	 *                        NULL for no validation
	 *
	 * @return The validated command line option
	 *
	 * @throws CommandLineException If the option value doesn't exist or cannot
	 *                              be validated
	 */
	public Object requireOption(
		String			  sOption,
		Predicate<Object> pIsValidOption) throws CommandLineException
	{
		Object rValue = getOption(sOption);

		if (rValue != null)
		{
			if (pIsValidOption == null || pIsValidOption.test(rValue))
			{
				return rValue;
			}
			else
			{
				throw new CommandLineException("Invalid command line option %s",
											   sOption);
			}
		}
		else
		{
			throw new CommandLineException("Missing command line option %s",
										   sOption);
		}
	}

	/***************************************
	 * Returns a mandatory command line option as a string value.
	 *
	 * @see #requireOption(String, Predicate)
	 */
	public String requireString(String sOption) throws CommandLineException
	{
		return requireOption(sOption).toString();
	}

	/***************************************
	 * Returns a string representation of this command line.
	 *
	 * @return A string description
	 */
	@Override
	public String toString()
	{
		return "CommandLine[" + aCommandLineOptions + "]";
	}
}
