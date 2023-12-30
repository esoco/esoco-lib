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

import de.esoco.lib.comm.CommunicationException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;

import static de.esoco.lib.comm.smtp.Email.MESSAGE;
import static de.esoco.lib.comm.smtp.Email.RECIPIENT_ADDRESS;
import static de.esoco.lib.comm.smtp.Email.RECIPIENT_NAME;
import static de.esoco.lib.comm.smtp.Email.SENDER_ADDRESS;
import static de.esoco.lib.comm.smtp.Email.SENDER_NAME;
import static de.esoco.lib.comm.smtp.Email.SUBJECT;
import static de.esoco.lib.comm.smtp.SmtpStatusCode.AUTH_SUCCESS;
import static de.esoco.lib.comm.smtp.SmtpStatusCode.CLOSING;
import static de.esoco.lib.comm.smtp.SmtpStatusCode.OK;
import static de.esoco.lib.comm.smtp.SmtpStatusCode.READY;
import static de.esoco.lib.comm.smtp.SmtpStatusCode.START_MAIL;

/**
 * A helper class that wraps output and input streams and perform SMTP requests
 * on them.
 *
 * @author eso
 */
public class SmtpProtocolHandler {

	private final String client;

	private final DataOutputStream output;

	private final BufferedReader input;

	/**
	 * Creates a new instance from output and input streams. The streams will
	 * not be closed by this instance, this needs to be handled by the invoking
	 * code.
	 *
	 * @param client       origin The client name or address
	 * @param outputStream The output stream
	 * @param inputStream  The input stream
	 */
	public SmtpProtocolHandler(String client, OutputStream outputStream,
		InputStream inputStream) {
		this.client = client;
		this.output = new DataOutputStream(outputStream);
		this.input = new BufferedReader(new InputStreamReader(inputStream));
	}

	/**
	 * Performs the SMTP connection handshake.
	 *
	 * @param user     The user for authentication
	 * @param password The user's password
	 */
	public void connect(String user, String password) {
		checkResponse(READY);

		if (user != null) {
			send("EHLO " + client).skipResponses(OK);

			String authPlain =
				String.format("%1$s\u0000%1$s\u0000%2$s", user, password);

			authPlain = "AUTH PLAIN " +
				Base64.getEncoder().encodeToString(authPlain.getBytes());

			send(authPlain).checkResponse(OK, AUTH_SUCCESS);
		} else {
			send("HELO " + client).checkOk();
		}
	}

	/**
	 * Disconnects from the SMTP server.
	 */
	public void disconnect() {
		send("QUIT").skipResponses(OK, CLOSING);
	}

	/**
	 * Sends an email after connecting (see {@link #connect(String, String)}).
	 *
	 * @param email The email data
	 */
	public void send(Email email) {
		String senderName = email.get(SENDER_NAME);
		String senderAddress = email.get(SENDER_ADDRESS);
		String recipientName = email.get(RECIPIENT_NAME);
		String recipientAddress = email.get(RECIPIENT_ADDRESS);

		send("MAIL FROM:<%s>", senderAddress).checkOk();
		send("RCPT TO:<%s>", recipientAddress).checkOk();
		send("DATA").checkResponse(OK, START_MAIL);
		send("Date: %s",
			DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()));
		sendAddress("From", senderName, senderAddress);
		sendAddress("To", recipientName, recipientAddress);
		send("Subject: %s", email.get(SUBJECT));
		send("Mime-Version: 1.0");
		send("Content-Type: text/plain; charset=\"utf-8\"");
		send("Content-Transfer-Encoding: quoted-printable");
		send("");
		send(email.get(MESSAGE));
		send(".").checkOk();
	}

	/**
	 * Shortcut for checking the status code {@link SmtpStatusCode#OK OK} with
	 * {@link #checkResponse(SmtpStatusCode)}.
	 *
	 * @return The received response if status code is OK
	 */
	String checkOk() {
		return checkResponse(OK);
	}

	/**
	 * Reads a response string from the socket and throws an exception if it
	 * doesn't start with an expected status code.
	 *
	 * @param expectedStatusCodes One or more expected status codes of the
	 *                            response
	 * @return The received response
	 * @throws CommunicationException If the response does not begin with
	 * one of
	 *                                the given status codes
	 */
	String checkResponse(SmtpStatusCode... expectedStatusCodes) {
		try {
			String response = input.readLine();

			for (SmtpStatusCode status : expectedStatusCodes) {
				if (response.startsWith(status.getCode())) {
					return response;
				}
			}

			throw new CommunicationException(
				"Expected one of [%s] but response was %s",
				Arrays.asList(expectedStatusCodes), response);
		} catch (IOException e) {
			throw new CommunicationException(e);
		}
	}

	/**
	 * Sends a data string to the SMTP server.
	 *
	 * @param dataFormat A format string for data to send
	 * @param formatArgs Format arguments to be inserted into the data
	 * @return This instance for fluent invocation
	 */
	SmtpProtocolHandler send(String dataFormat, Object... formatArgs) {
		try {
			output.writeBytes(String.format(dataFormat + "\n", formatArgs));
		} catch (IOException e) {
			throw new CommunicationException(e);
		}

		return this;
	}

	/**
	 * Sends an email address field string to the server.
	 *
	 * @param field   The field name
	 * @param name    The name of address
	 * @param address The email address
	 */
	void sendAddress(String field, String name, String address) {
		if (name != null) {
			send("%s: %s <%s>", field, name, address);
		} else {
			send("%s: %s", field, address);
		}
	}

	/**
	 * Skips one or more status codes responses.
	 *
	 * @param statusCodes The status codes to skip
	 */
	void skipResponses(SmtpStatusCode... statusCodes) {
		try {
			while (input.ready()) {
				checkResponse(statusCodes);
			}
		} catch (IOException e) {
			throw new CommunicationException(e);
		}
	}
}
