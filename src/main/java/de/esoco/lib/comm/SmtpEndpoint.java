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
package de.esoco.lib.comm;

import de.esoco.lib.comm.smtp.Email;
import de.esoco.lib.comm.smtp.SmtpProtocolHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;

import org.obrel.core.RelationType;

import static de.esoco.lib.comm.CommunicationRelationTypes.PASSWORD;
import static de.esoco.lib.comm.CommunicationRelationTypes.USER_NAME;

import static org.obrel.core.RelationTypeModifier.PRIVATE;
import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * A socket-based {@link Endpoint} that allows to send email to an SMTP server.
 * The endpoint supports encrypted connections (SMTPS) and PLAIN authentication.
 * It does not support STARTTLS so the connection needs to be encrypted from the
 * start (typically on port 465). To create an SMTP(S) endpoint it is
 * recommended to invoke the factory method {@link Endpoint#at(String)} with a
 * URL containing the host and port, e.g. <code>
 * Endpoint.at("smtps://mail.example.com:465")</code>.
 *
 * @author eso
 */
public class SmtpEndpoint extends SocketEndpoint
{
	//~ Static fields/initializers ---------------------------------------------

	/**
	 * An internal relation type to the data input stream for the socket input
	 * stream of an EPP endpoint.
	 */
	private static final RelationType<DataInputStream> SMTP_INPUT_STREAM =
		newType(PRIVATE);

	/**
	 * An internal relation type to the data output stream for the socket output
	 * stream of an EPP endpoint.
	 */
	private static final RelationType<DataOutputStream> SMTP_OUTPUT_STREAM =
		newType(PRIVATE);

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Factory method to create an instance of {@link SmtpRequest} without a
	 * default email message.
	 *
	 * @return The new request instance
	 */
	public static SmtpRequest sendMail()
	{
		return new SmtpRequest(null);
	}

	/***************************************
	 * Factory method to create an instance of {@link SmtpRequest}.
	 *
	 * @param  rDefaultEmail The default email to send
	 *
	 * @return The new request instance
	 */
	public static SmtpRequest sendMail(Email rDefaultEmail)
	{
		return new SmtpRequest(rDefaultEmail);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void initConnection(Connection rConnection) throws IOException
	{
		super.initConnection(rConnection);

		Socket rSocket = getEndpointSocket(rConnection);

		DataOutputStream aDataOut =
			new DataOutputStream(rSocket.getOutputStream());
		DataInputStream  aDataIn  =
			new DataInputStream(rSocket.getInputStream());

		rConnection.set(SMTP_OUTPUT_STREAM, aDataOut);
		rConnection.set(SMTP_INPUT_STREAM, aDataIn);
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A SMTP request that sends email from instance of {@link Email}.
	 *
	 * @author eso
	 */
	public static class SmtpRequest extends SocketRequest<Email, Void>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rDefaultEmail The default email to send
		 */
		protected SmtpRequest(Email rDefaultEmail)
		{
			super(SmtpRequest.class.getSimpleName(), rDefaultEmail);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected Void sendRequest(Connection   rConnection,
								   OutputStream rOutput,
								   InputStream  rInput,
								   Email		rEmail) throws Exception
		{
			SmtpProtocolHandler aSmtpHandler =
				new SmtpProtocolHandler("localhost", rOutput, rInput);

			aSmtpHandler.connect(rConnection.get(USER_NAME),
								 rConnection.get(PASSWORD));
			aSmtpHandler.send(rEmail);
			aSmtpHandler.disconnect();

			return null;
		}
	}
}
