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

import de.esoco.lib.collection.CollectionUtil;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a command line and provides access to the options and the optional
 * option values of the command line. If only the parsing of a command line is
 * needed the static methods of this method can be used.
 *
 * @author eso
 */
public class CommandLine {

	/**
	 * The key that can be used to query the remaining arguments that didn't
	 * match the command line options pattern. The datatype is a list of
	 * strings
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

	private static final Set<String> STANDARD_OPTIONS =
		CollectionUtil.fixedSetOf("h", "-help");

	private final Map<String, Object> commandLineOptions;

	private Map<String, String> allowedOptions = null;

	/**
	 * Creates a new command line from an input stream in Java properties file
	 * format. Boolean values will be converted to {@link Boolean} objects, all
	 * other argument values will be stored as strings.
	 *
	 * @param argsStream sFileName The file name (including path) of the
	 *                   properties file
	 * @throws IllegalArgumentException If accessing the stream fails
	 */
	public CommandLine(InputStream argsStream) {
		commandLineOptions = readArguments(argsStream);
	}

	/**
	 * Creates a new command line that allows arbitrary arguments.
	 *
	 * @param args The command line arguments
	 */
	public CommandLine(String[] args) {
		this(args, null);
	}

	/**
	 * Creates a new CommandLine object with a standard argument pattern that
	 * allows a certain set of options. If the allowed options are not empty
	 * the
	 * corresponding pattern will be created by invoking
	 * {@link #createStandardPattern(String[])}. Otherwise the
	 * {@link #DEFAULT_OPTION_PATTERN} will be used.
	 *
	 * @param args           The command line arguments
	 * @param allowedOptions A list containing all the options that are allowed
	 *                       on the command line
	 * @throws IllegalArgumentException If one of the arguments is invalid
	 */
	public CommandLine(String[] args, Map<String, String> allowedOptions) {
		this(args, Pattern.compile(DEFAULT_OPTION_PATTERN), allowedOptions);
	}

	/**
	 * Creates a new command line instance. Parses the command line with the
	 * static {@link #parse(String[], Pattern) parse()} method.
	 *
	 * @param args           The command line arguments
	 * @param argPattern     The pattern to parse a single command line option
	 *                       or NULL for the default pattern
	 * @param allowedOptions A list containing all the options that are allowed
	 *                       on the command line
	 * @throws IllegalArgumentException If either the argument pattern or an
	 *                                  argument is invalid
	 */
	public CommandLine(String[] args, Pattern argPattern,
		Map<String, String> allowedOptions) {
		this.allowedOptions = allowedOptions;

		commandLineOptions = parse(args, argPattern);
	}

	/**
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
	 * @param prefix         The allowed prefixes for an option
	 * @param assignment     The string or character used to assign a value to
	 *                       an option
	 * @param allowedOptions A list containing all the options that are allowed
	 *                       on the command line
	 * @return A new pattern based on the method arguments
	 */
	public static Pattern createPattern(String prefix, String assignment,
		String... allowedOptions) {
		StringBuilder pattern = new StringBuilder(prefix + "(");
		StringBuilder valueOptions = new StringBuilder();

		for (String option : allowedOptions) {
			boolean valueRequired = option.endsWith(assignment);

			if (valueRequired) {
				int l = option.length() - assignment.length();

				valueOptions.append(option, 0, l).append('|');
			} else {
				pattern.append(option).append('|');
			}
		}

		// if prefix is -, assignment is =, sn are options, and pn are
		// options with mandatory parameters, the resulting pattern will be:
		// -(?:(s1|s2|...)(?:=(.+))?|(p1|p2|...)=(.+))
		// group 1 & 2 for simple options, 3 & 4 for parameterized options

		if (pattern.charAt(pattern.length() - 1) == '|') {
			// remove trailing '|'
			pattern.setLength(pattern.length() - 1);
			pattern.append(")(?:").append(assignment).append("(.+))?");

			if (valueOptions.length() > 0) {
				pattern.append("|(");
			}
		}

		if (valueOptions.length() > 0) {
			// remove trailing '|'
			valueOptions.setLength(valueOptions.length() - 1);
			pattern.append(valueOptions).append(")");
			pattern.append(assignment).append("(.+)");
		}

		if (allowedOptions.length == 0) {
			pattern.append(")");
		}

		return Pattern.compile(pattern.toString());
	}

	/**
	 * Creates a standard command line pattern from a list of allowed option
	 * names of the form -&lt;option&gt;[=&lt;value&gt;] (the prefix '/' can be
	 * used instead of of '-'). Based on the method
	 * {@link #createPattern(String, String, String[])}.
	 *
	 * @param allowedOptions A list containing all the options that are allowed
	 *                       on the command line
	 * @return A new pattern based on the method arguments
	 */
	public static Pattern createStandardPattern(String... allowedOptions) {
		return createPattern("-", "=", allowedOptions);
	}

	/**
	 * Tries to read the command line arguments from a Java properties file.
	 *
	 * @param fileName The file name (including path) of the properties file
	 * @return A map containing the arguments as read from the file
	 * @throws IllegalArgumentException If the file doesn't exist or could not
	 *                                  be read
	 */
	public static Map<String, Object> readArguments(String fileName) {
		try {
			return readArguments(new FileInputStream(fileName));
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(
				"Arguments file not found: " + fileName);
		}
	}

	/**
	 * Reads the command line arguments from an input stream in Java properties
	 * file format. Boolean values will be converted to {@link Boolean}
	 * objects,
	 * all other argument values will be stored as strings.
	 *
	 * @param argsStream The input stream to read the arguments from
	 * @return A map containing the arguments as read from the stream
	 * @throws IllegalArgumentException If accessing the stream fails
	 */
	public static Map<String, Object> readArguments(InputStream argsStream) {
		Properties properties = new Properties();

		try {
			properties.load(argsStream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not read arguments", e);
		}

		Map<String, Object> fileArguments = new HashMap<>(properties.size());

		for (Entry<Object, Object> property : properties.entrySet()) {
			Object value = property.getValue();

			if ("true".equalsIgnoreCase(value.toString())) {
				value = Boolean.TRUE;
			} else if ("false".equalsIgnoreCase(value.toString())) {
				value = Boolean.FALSE;
			}

			fileArguments.put(property.getKey().toString(), value);
		}

		return fileArguments;
	}

	/**
	 * Returns the arguments of this command line that are not options. The
	 * returned list contains all arguments that didn't match the options
	 * pattern as they appeared in the original command line. If no such
	 * arguments exist the list will be empty.
	 *
	 * @return The list of unparsed arguments (may be empty but will never be
	 * NULL)
	 */
	@SuppressWarnings("unchecked")
	public List<String> getArguments() {
		return (List<String>) commandLineOptions.get(EXTRA_ARGUMENTS);
	}

	/**
	 * Convenience method that converts the result of
	 * {@link #getOption(String)}
	 * to an integer value.
	 *
	 * @param option The option name
	 * @return The integer value or -1 if the option doesn't exists
	 * @throws CommandLineException If the option cannot be parsed as an
	 *                              integer
	 */
	public int getInt(String option) {
		String value = getString(option);

		try {
			return value != null ? Integer.parseInt(value) : -1;
		} catch (NumberFormatException e) {
			throw new CommandLineException("Integer value expected: %s",
				option,
				e);
		}
	}

	/**
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
	 * @param option The option to query
	 * @return The option value or NULL for none
	 */
	public Object getOption(String option) {
		return commandLineOptions.get(option);
	}

	/**
	 * Convenience method that converts the result of
	 * {@link #getOption(String)}
	 * to a string.
	 *
	 * @param option The option name
	 * @return The option string or NULL for none
	 */
	public String getString(String option) {
		Object opt = getOption(option);

		return opt != null ? opt.toString() : null;
	}

	/**
	 * Checks if a certain option exists in this command line. This method is
	 * case-sensitive. If an application has created the command line to be
	 * case-insensitive (e.g. by using the default pattern) it must always
	 * invoke this method with lower case option names because all option names
	 * have been converted to lower case on creation.
	 *
	 * @param option The option to check
	 * @return TRUE if the option exists
	 */
	public boolean hasOption(String option) {
		return commandLineOptions.containsKey(option);
	}

	/**
	 * Parses a list of command line arguments into a map with the default
	 * argument pattern that allows arbitrary case-insensitive options.
	 *
	 * @param args The list of command line arguments
	 * @return A new map containing all command line options together with
	 * their
	 * values; will be empty if the argument list is empty
	 * @throws IllegalArgumentException If one of the arguments is invalid
	 */
	public Map<String, Object> parse(String[] args) {
		return parse(args, Pattern.compile(DEFAULT_OPTION_PATTERN));
	}

	/**
	 * Parses a list of command line arguments into a map. Each element in the
	 * args array will be matched against the given regular expression pattern.
	 * If the pattern matches the first group in the pattern will be considered
	 * to be a command line option and stored in the result map as a key. If
	 * the
	 * matched pattern contains another group it's string value will be
	 * converted by invoking {@link TextUtil#parseObject(String)} and the
	 * result
	 * will be stored in the map as the value associated with the key (= the
	 * command line option). If the pattern contains only a single group the
	 * value associated with an option will be {@link Boolean#TRUE}.
	 *
	 * <p>To allow correct parsing of the arguments the argument pattern must
	 * contain at least one capturing group that identifies the name of the
	 * option. If options are allowed to have values a second capturing group
	 * must define the option value. It is possible for the pattern to contain
	 * more than two groups, but a maximum of two groups must be valid (i.e.
	 * not
	 * NULL). These two groups will then be used as the option and it's value,
	 * respectively.</p>
	 *
	 * <p>The pattern's flag CASE_INSENSITIVE controls if similar options with
	 * different case are to be distinguished. The default pattern allows
	 * arbitrary case-insensitive command line options of the form
	 * -&lt;option&gt;[=&lt;value&gt;] (the prefix '/' can be used instead
	 * of of
	 * '-').</p>
	 *
	 * <p>The returned map will contain the options in the same order they
	 * appear in the argument list. If the same option appears multiple
	 * times in
	 * the command line the value of the last occurrence will be contained in
	 * the result map.</p>
	 *
	 * <p>If the default option pattern is used any values on the command line
	 * that don't match the option pattern will be placed in a {@link List} of
	 * unparsed arguments that is stored in the returned map under the key
	 * {@link #EXTRA_ARGUMENTS}. This list will always exist but may be
	 * empty.</p>
	 *
	 * @param args       The original command line arguments
	 * @param argPattern The pattern to parse a single command line option or
	 *                   NULL for the default pattern
	 * @return A new map containing all command line options together with
	 * their
	 * values; will be empty if the argument list is empty
	 * @throws CommandLineException If an argument cannot be parsed
	 */
	public Map<String, Object> parse(String[] args, Pattern argPattern) {
		Map<String, Object> result =
			new LinkedHashMap<String, Object>(args.length);

		List<String> extraArguments = new ArrayList<>();
		String prevOption = null;

		for (String arg : args) {
			Matcher argMatcher = argPattern.matcher(arg);
			boolean isArg = argMatcher.matches();

			if (isArg) {
				if (argMatcher.groupCount() > 0) {
					int group = TextUtil.nextGroup(argMatcher, 1);

					if (group == -1) {
						throw new IllegalArgumentException(
							"Invalid option: " + arg);
					}

					String option = argMatcher.group(group);
					Object value = Boolean.TRUE;

					group = TextUtil.nextGroup(argMatcher, group + 1);

					if (group != -1) {
						value = TextUtil.parseObject(argMatcher.group(group));
					}

					if ((argPattern.flags() & Pattern.CASE_INSENSITIVE) != 0) {
						option = option.toLowerCase();
					}

					if (option.equals("-args")) {
						if (value instanceof String) {
							result.putAll(readArguments(value.toString()));
						} else {
							throw new CommandLineException(
								"--args must point " + "to an argument " +
									"properties file", "--args");
						}
					} else {
						if (allowedOptions != null &&
							!STANDARD_OPTIONS.contains(option) &&
							!allowedOptions.containsKey(option)) {
							throw new CommandLineException(
								"Unsupported option: " + arg, arg);
						}

						result.put(option, value);
						prevOption = option;
					}
				} else {
					throw new CommandLineException("Invalid option: " + arg,
						arg);
				}
			} else if (prevOption != null) {
				result.put(prevOption, TextUtil.parseObject(arg));
				prevOption = null;
			} else {
				extraArguments.add(arg);
			}
		}

		result.put(EXTRA_ARGUMENTS, extraArguments);

		return result;
	}

	/**
	 * Returns a mandatory command line option without validation.
	 *
	 * @see #requireOption(String, Predicate)
	 */
	public Object requireOption(String option) throws CommandLineException {
		return requireOption(option, null);
	}

	/**
	 * Returns a mandatory value of a command line option and optionally
	 * that it
	 * fulfills certain criteria that are asserted by applying a predicate. If
	 * the option value doesn't exist or the predicate yields FALSE for it an
	 * exception will be thrown.
	 *
	 * @param option        The command line option to return the value of
	 * @param isValidOption An optional predicate to apply to option values or
	 *                      NULL for no validation
	 * @return The validated command line option
	 * @throws CommandLineException If the option value doesn't exist or cannot
	 *                              be validated
	 */
	public Object requireOption(String option, Predicate<Object> isValidOption)
		throws CommandLineException {
		Object value = getOption(option);

		if (value != null) {
			if (isValidOption == null || isValidOption.test(value)) {
				return value;
			} else {
				throw new CommandLineException(
					"Invalid command line option '%s'", option);
			}
		} else {
			throw new CommandLineException("Missing command line option '%s'",
				option);
		}
	}

	/**
	 * Returns a mandatory command line option as a string value.
	 *
	 * @see #requireOption(String, Predicate)
	 */
	public String requireString(String option) throws CommandLineException {
		return requireOption(option).toString();
	}

	/**
	 * Returns a string representation of this command line.
	 *
	 * @return A string description
	 */
	@Override
	public String toString() {
		return "CommandLine[" + commandLineOptions + "]";
	}
}
