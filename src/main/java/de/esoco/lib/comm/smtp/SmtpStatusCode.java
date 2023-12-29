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
package de.esoco.lib.comm.smtp;

import de.esoco.lib.text.TextConvert;

/**
 * Enumeration of the SMTP status codes (incomplete).
 */
public enum SmtpStatusCode {
	READY("220"), CLOSING("221"), AUTH_SUCCESS("235"), OK("250"),
	START_MAIL("354");

	private final String sCode;

	/**
	 * Creates a new instance.
	 *
	 * @param sCode The status code (three-digit number as a decimal string)
	 */
	SmtpStatusCode(String sCode) {
		this.sCode = sCode;
	}

	/**
	 * Returns the actual three-digit status code as a decimal string.
	 *
	 * @return The status code
	 */
	public String getCode() {
		return sCode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("%s (%s)", sCode,
			TextConvert.capitalize(name(), " "));
	}
}
