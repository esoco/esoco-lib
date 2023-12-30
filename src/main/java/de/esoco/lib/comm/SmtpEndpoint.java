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
import org.obrel.core.RelationType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static de.esoco.lib.comm.CommunicationRelationTypes.PASSWORD;
import static de.esoco.lib.comm.CommunicationRelationTypes.USER_NAME;
import static org.obrel.core.RelationTypeModifier.PRIVATE;
import static org.obrel.core.RelationTypes.newType;

/**
 * A socket-based {@link Endpoint} that allows to send email to an SMTP server.
 * The endpoint supports encrypted connections (SMTPS) and PLAIN authentication.
 * It does not support STARTTLS so the connection needs to be encrypted from the
 * start (typically on port 465).
 *
 * <p>To create an SMTP(S) endpoint it is recommended to invoke the factory
 * method {@link Endpoint#at(String)} with a URL containing the host and port,
 * e.g. <code>Endpoint.at("smtps://mail.example.com:465")</code>. Optionally the
 * URL can also contain authentication credentials (i.e. login name and
 * password) before the host name. If not present in the URL the credential must
 * be set on the endpoint instance if the server requires authentication.
 * Credentials set on the instance will take precedence over the URL.</p>
 *
 * <p>Furthermore the URL may contain a query part that provides additional
 * parameters. Currently that can be "to" to set the sender and "from" for the
 * recipient email addresses. Sender and recipient in an {@link Email} object
 * that is handed over to {@link #sendMail(Email)} take precedence. If either
 * sender or recipient are missing (i.e. neither defined in the email nor in the
 * URL) the request will fail.</p>
 *
 * @author eso
 */
public class SmtpEndpoint extends SocketEndpoint {

	/**
	 * An internal relation type to the data input stream for the socket input
	 * stream of an EPP endpoint.
	 */
	private static final RelationType<DataInputStream> SMTP_INPUT_STREAM =
		newType(PRIVATE);

	/**
	 * An internal relation type to the data output stream for the socket
	 * output
	 * stream of an EPP endpoint.
	 */
	private static final RelationType<DataOutputStream> SMTP_OUTPUT_STREAM =
		newType(PRIVATE);

	/**
	 * Factory method to create an instance of {@link SmtpRequest} without a
	 * default email message.
	 *
	 * @return The new request instance
	 */
	public static SmtpRequest sendMail() {
		return new SmtpRequest(null);
	}

	/**
	 * Factory method to create an instance of {@link SmtpRequest}.
	 *
	 * @param defaultEmail The default email to send
	 * @return The new request instance
	 */
	public static SmtpRequest sendMail(Email defaultEmail) {
		return new SmtpRequest(defaultEmail);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initConnection(Connection connection) throws IOException {
		super.initConnection(connection);

		Socket socket = getSocket(connection);

		DataOutputStream dataOut =
			new DataOutputStream(socket.getOutputStream());
		DataInputStream dataIn = new DataInputStream(socket.getInputStream());

		connection.set(SMTP_OUTPUT_STREAM, dataOut);
		connection.set(SMTP_INPUT_STREAM, dataIn);
	}

	/**
	 * A SMTP request that sends email from instance of {@link Email}.
	 *
	 * @author eso
	 */
	public static class SmtpRequest extends SocketRequest<Email, Void> {

		/**
		 * Creates a new instance.
		 *
		 * @param defaultEmail The default email to send
		 */
		protected SmtpRequest(Email defaultEmail) {
			super(SmtpRequest.class.getSimpleName(), defaultEmail);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Void sendRequest(Connection connection, OutputStream output,
			InputStream input, Email email) throws Exception {
			SmtpProtocolHandler smtpHandler =
				new SmtpProtocolHandler("localhost", output, input);

			String from = email.get(Email.SENDER_ADDRESS);
			String to = email.get(Email.RECIPIENT_ADDRESS);

			if (from == null || to == null) {
				String query = connection.getUri().getQuery();
				String[] queryElements = query.split("&");
				Map<String, String> params = new HashMap<>();

				for (String element : queryElements) {
					String[] param = element.split("=");

					if (param.length == 2) {
						params.put(param[0].toLowerCase(), param[1]);
					}
				}

				from = from == null ? params.get("from") : from;
				to = to == null ? params.get("to") : to;

				Objects.requireNonNull(from, "Missing sender address");
				Objects.requireNonNull(to, "Missing recipient address");

				email.set(Email.SENDER_ADDRESS, from);
				email.set(Email.RECIPIENT_ADDRESS, to);
			}

			smtpHandler.connect(connection.get(USER_NAME),
				connection.get(PASSWORD));
			smtpHandler.send(email);
			smtpHandler.disconnect();

			return null;
		}
	}
}
