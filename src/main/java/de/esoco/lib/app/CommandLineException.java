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
package de.esoco.lib.app;

/**
 * An exception that indicates an invalid command line option in a
 * {@link CommandLine}. Invalid options are either missing completely or have an
 * illegal option value. This is a runtime exception because it may occur
 * anytime during command line processing.
 *
 * @author eso
 */
public class CommandLineException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String invalidOption;

	/**
	 * Creates a new instance for a certain invalid command line option. If the
	 * error message contains a %s placeholder it will be replaced with the
	 * name
	 * of the invalid option
	 *
	 * @param message       The error message
	 * @param invalidOption The name of the invalid command line option
	 */
	public CommandLineException(String message, String invalidOption) {
		super(createErrorMessage(message, invalidOption));

		this.invalidOption = invalidOption;
	}

	/**
	 * Creates a new instance for a certain invalid command line option. If the
	 * error message contains a %s placeholder it will be replaced with the
	 * name
	 * of the invalid option
	 *
	 * @param message       The error message
	 * @param invalidOption The name of the invalid command line option
	 * @param cause         The causing exception
	 */
	public CommandLineException(String message, String invalidOption,
		Throwable cause) {
		super(createErrorMessage(message, invalidOption), cause);

		this.invalidOption = invalidOption;
	}

	/**
	 * Creates the error message for the superclass by formatting it with the
	 * invalid option name if necessary.
	 */
	private static String createErrorMessage(String message,
		String invalidOption) {
		if (message.contains("%s")) {
			message = String.format(message, invalidOption);
		}

		return message;
	}

	/**
	 * Returns the invalid command line option that this instance refers to.
	 *
	 * @return The invalid command line option
	 */
	public final String getInvalidOption() {
		return invalidOption;
	}
}
