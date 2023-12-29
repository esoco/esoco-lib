//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.expression.function.TokenStringFormat;

/**
 * Special token string transformation that transforms log records into strings.
 * It defines several additional tokens that refer to the fields of the
 * transformed log record. All tokens support the formatting options of the base
 * class. The following list shows the available tokens (in short|long form
 * where applicable) with the data type of the token in brackets:
 *
 * <ul>
 *   <li>l|level: the description of the log level [LogLevel]</li>
 *   <li>m|message: the log message [String]</li>
 *   <li>t|time: the log time in milliseconds [long]</li>
 *   <li>c|cause: the causing error (may be NULL) [Throwable]</li>
 *   <li>package: the name of the package where the logging call occurred</li>
 *   <li>class: the name without package of the class where the logging call
 *     occurred</li>
 *   <li>method: the name of the method where the logging call occurred</li>
 *   <li>stack: the stack trace of the code where the logging call occurred, in
 *     a multi-line format similar to Throwable.printStacktrace()</li>
 *   <li>stacktop: like stack but only prints the topmost 3 stacktrace
 *     elements</li>
 *   <li>file: the name of the source file in which the logging call
 *     occurred</li>
 *   <li>line: the number of the source code line where the logging call
 *     occurred</li>
 * </ul>
 *
 * @author eso
 */
public class LogRecordFormat extends TokenStringFormat<LogRecord> {

	/**
	 * A token string macro for the source location of a log record
	 */
	public static final String SOURCE_LOCATION =
		"{package}.{class}.{method}({file}:{line})";

	static {
		PropertyToken aToken;

		aToken = new PropertyToken("getLevel");
		registerToken("l", aToken);
		registerToken("level", aToken);

		aToken = new PropertyToken("getMessage");
		registerToken("m", aToken);
		registerToken("message", aToken);

		aToken = new PropertyToken("getTime");
		registerToken("t", aToken);
		registerToken("time", aToken);

		aToken = new PropertyToken("getCause");
		registerToken("c", aToken);
		registerToken("cause", aToken);
		registerToken("package", new PropertyToken("getLogPackage"));
		registerToken("class", new PropertyToken("getLogClassName"));
		registerToken("method", new PropertyToken("getLogMethod"));
		registerToken("file", new PropertyToken("getSourceFileName"));
		registerToken("line", new PropertyToken("getLineNumber"));
		registerToken("stack",
			new PropertyToken("getLogStackTrace", "F\t| %s\n"));
		registerToken("stacktop",
			new PropertyToken("getLogStackTrace", "3F\t| %s\n"));
	}

	/**
	 * Creates a new instance for a certain log format string.
	 *
	 * @param sLogFormat The log format pattern
	 */
	public LogRecordFormat(String sLogFormat) {
		super(sLogFormat);
	}
}
