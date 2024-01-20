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

import org.obrel.core.FluentRelatable;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;

import static org.obrel.core.RelationTypes.newType;

/**
 * A related object that contains the values for a single email message.
 *
 * @author eso
 */
public class Email extends RelatedObject implements FluentRelatable<Email> {

	/**
	 * The name of the email sender (FROM).
	 */
	public static final RelationType<String> SENDER_NAME = newType();

	/**
	 * The email address of the sender (FROM).
	 */
	public static final RelationType<String> SENDER_ADDRESS = newType();

	/**
	 * The name of the email recipient (TO).
	 */
	public static final RelationType<String> RECIPIENT_NAME = newType();

	/**
	 * The email address of the recipient (TO).
	 */
	public static final RelationType<String> RECIPIENT_ADDRESS = newType();

	/**
	 * The subject line of the email.
	 */
	public static final RelationType<String> SUBJECT = newType();

	/**
	 * The message text of the email.
	 */
	public static final RelationType<String> MESSAGE = newType();

	/**
	 * A factory method that creates a new instance.
	 *
	 * @return The new instance
	 */
	public static Email email() {
		return new Email();
	}

	/**
	 * Sets the sender address and an empty sender name.
	 *
	 * @param address The receiver address
	 * @return This instance for fluent invocation
	 */
	public Email from(String address) {
		return from("", address);
	}

	/**
	 * Sets the receiver address.
	 *
	 * @param name    The receiver name
	 * @param address The receiver address
	 * @return This instance for fluent invocation
	 */
	public Email from(String name, String address) {
		return with(SENDER_NAME, name).with(SENDER_ADDRESS, address);
	}

	/**
	 * Sets the email's message text.
	 *
	 * @param message subject The message text
	 * @return This instance for fluent invocation
	 */
	public Email message(String message) {
		return with(MESSAGE, message);
	}

	/**
	 * Sets the email's subject line.
	 *
	 * @param subject The subject line
	 * @return This instance for fluent invocation
	 */
	public Email subject(String subject) {
		return with(SUBJECT, subject);
	}

	/**
	 * Sets the recipient address and an empty recipient name.
	 *
	 * @param address The recipient address
	 * @return This instance for fluent invocation
	 */
	public Email to(String address) {
		return to("", address);
	}

	/**
	 * Sets the receiver address.
	 *
	 * @param name    The receiver name
	 * @param address The receiver address
	 * @return This instance for fluent invocation
	 */
	public Email to(String name, String address) {
		return with(RECIPIENT_NAME, name).with(RECIPIENT_ADDRESS, address);
	}
}
