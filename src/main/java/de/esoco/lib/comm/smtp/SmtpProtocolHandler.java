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


/********************************************************************
 * A helper class that wraps output and input streams and perform SMTP requests
 * on them.
 *
 * @author eso
 */
public class SmtpProtocolHandler
{
	//~ Instance fields --------------------------------------------------------

	private final String		   sClient;
	private final DataOutputStream rOutput;
	private final BufferedReader   rInput;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance from output and input streams. The streams will
	 * not be closed by this instance, this needs to be handled by the invoking
	 * code.
	 *
	 * @param sClient       sOrigin The client name or address
	 * @param rOutputStream The output stream
	 * @param rInputStream  The input stream
	 */
	public SmtpProtocolHandler(String		sClient,
							   OutputStream rOutputStream,
							   InputStream  rInputStream)
	{
		this.sClient = sClient;
		this.rOutput = new DataOutputStream(rOutputStream);
		this.rInput  = new BufferedReader(new InputStreamReader(rInputStream));
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Performs the SMTP connection handshake.
	 *
	 * @param sUser     The user for authentication
	 * @param sPassword The user's password
	 */
	public void connect(String sUser, String sPassword)
	{
		checkResponse(READY);

		if (sUser != null)
		{
			send("EHLO " + sClient).skipResponses(OK);

			String sAuthPlain =
				String.format("%1$s\u0000%1$s\u0000%2$s", sUser, sPassword);

			sAuthPlain =
				"AUTH PLAIN " +
				Base64.getEncoder().encodeToString(sAuthPlain.getBytes());

			send(sAuthPlain).checkResponse(OK, AUTH_SUCCESS);
		}
		else
		{
			send("HELO " + sClient).checkOk();
		}
	}

	/***************************************
	 * Disconnects from the SMTP server.
	 */
	public void disconnect()
	{
		send("QUIT").skipResponses(OK, CLOSING);
	}

	/***************************************
	 * Sends an email after connecting (see {@link #connect(String, String)}).
	 *
	 * @param rEmail The email data
	 */
	public void send(Email rEmail)
	{
		String sSenderName		 = rEmail.get(SENDER_NAME);
		String sSenderAddress    = rEmail.get(SENDER_ADDRESS);
		String sRecipientName    = rEmail.get(RECIPIENT_NAME);
		String sRecipientAddress = rEmail.get(RECIPIENT_ADDRESS);

		send("MAIL FROM:<%s>", sSenderAddress).checkOk();
		send("RCPT TO:<%s>", sRecipientAddress).checkOk();
		send("DATA").checkResponse(OK, START_MAIL);
		send("Date: %s",
			 DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()));
		sendAddress("From", sSenderName, sSenderAddress);
		sendAddress("To", sRecipientName, sRecipientAddress);
		send("Subject: %s", rEmail.get(SUBJECT));
		send("Mime-Version: 1.0");
		send("Content-Type: text/plain; charset=\"utf-8\"");
		send("Content-Transfer-Encoding: quoted-printable");
		send("");
		send(rEmail.get(MESSAGE));
		send(".").checkOk();
	}

	/***************************************
	 * Shortcut for checking the status code {@link SmtpStatusCode#OK OK} with
	 * {@link #checkResponse(SmtpStatusCode)}.
	 *
	 * @return The received response if status code is OK
	 */
	String checkOk()
	{
		return checkResponse(OK);
	}

	/***************************************
	 * Reads a response string from the socket and throws an exception if it
	 * doesn't start with an expected status code.
	 *
	 * @param  eExpectedStatusCodes One or more expected status codes of the
	 *                              response
	 *
	 * @return The received response
	 *
	 * @throws CommunicationException If the response does not begin with one of
	 *                                the given status codes
	 */
	String checkResponse(SmtpStatusCode... eExpectedStatusCodes)
	{
		try
		{
			String sResponse = rInput.readLine();

			for (SmtpStatusCode eStatus : eExpectedStatusCodes)
			{
				if (sResponse.startsWith(eStatus.getCode()))
				{
					return sResponse;
				}
			}

			throw new CommunicationException("Expected one of [%s] but response was %s",
											 Arrays.asList(eExpectedStatusCodes),
											 sResponse);
		}
		catch (IOException e)
		{
			throw new CommunicationException(e);
		}
	}

	/***************************************
	 * Sends a data string to the SMTP server.
	 *
	 * @param  sDataFormat A format string for data to send
	 * @param  rFormatArgs Format arguments to be inserted into the data
	 *
	 * @return This instance for fluent invocation
	 */
	SmtpProtocolHandler send(String sDataFormat, Object... rFormatArgs)
	{
		try
		{
			rOutput.writeBytes(String.format(sDataFormat + "\n", rFormatArgs));
		}
		catch (IOException e)
		{
			throw new CommunicationException(e);
		}

		return this;
	}

	/***************************************
	 * Sends an email address field string to the server.
	 *
	 * @param sField   The field name
	 * @param sName    The name of address
	 * @param sAddress The email address
	 */
	void sendAddress(String sField, String sName, String sAddress)
	{
		if (sName != null)
		{
			send("%s: %s <%s>", sField, sName, sAddress);
		}
		else
		{
			send("%s: %s", sField, sAddress);
		}
	}

	/***************************************
	 * Skips one or more status codes responses.
	 *
	 * @param eStatusCodes The status codes to skip
	 */
	void skipResponses(SmtpStatusCode... eStatusCodes)
	{
		try
		{
			while (rInput.ready())
			{
				checkResponse(eStatusCodes);
			}
		}
		catch (IOException e)
		{
			throw new CommunicationException(e);
		}
	}
}
