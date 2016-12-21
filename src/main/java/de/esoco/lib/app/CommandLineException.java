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

/********************************************************************
 * An exception that indicates an invalid command line option in a {@link
 * CommandLine}. Invalid option are either missing completely or have an illegal
 * option value.
 *
 * @author eso
 */
public class CommandLineException extends Exception
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long serialVersionUID = 1L;

	//~ Instance fields --------------------------------------------------------

	private final String sInvalidOption;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance for a certain invalid command line option. If the
	 * error message contains a %s placeholder it will be replaced with the name
	 * of the invalid option
	 *
	 * @param sMessage       The error message
	 * @param sInvalidOption The name of the invalid command line option
	 */
	public CommandLineException(String sMessage, String sInvalidOption)
	{
		super(createErrorMessage(sMessage, sInvalidOption));

		this.sInvalidOption = sInvalidOption;
	}

	/***************************************
	 * Creates a new instance for a certain invalid command line option. If the
	 * error message contains a %s placeholder it will be replaced with the name
	 * of the invalid option
	 *
	 * @param sMessage       The error message
	 * @param sInvalidOption The name of the invalid command line option
	 * @param eCause         The causing exception
	 */
	public CommandLineException(String    sMessage,
								String    sInvalidOption,
								Throwable eCause)
	{
		super(createErrorMessage(sMessage, sInvalidOption), eCause);

		this.sInvalidOption = sInvalidOption;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Creates the error message for the superclass by formatting it with the
	 * invalid option name if necessary.
	 *
	 * @param  sMessage
	 * @param  sInvalidOption
	 *
	 * @return
	 */
	private static String createErrorMessage(
		String sMessage,
		String sInvalidOption)
	{
		if (sMessage.contains("%s"))
		{
			sMessage = String.format(sMessage, sInvalidOption);
		}

		return sMessage;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the invalid command line option that this instance refers to.
	 *
	 * @return The invalid command line option
	 */
	public final String getInvalidOption()
	{
		return sInvalidOption;
	}
}
