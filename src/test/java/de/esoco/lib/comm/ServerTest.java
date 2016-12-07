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
package de.esoco.lib.comm;

import de.esoco.lib.comm.Server.RequestHandler;
import de.esoco.lib.expression.Function;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogLevel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.junit.Test;

import static de.esoco.lib.expression.Functions.value;

import static org.obrel.type.StandardTypes.NAME;
import static org.obrel.type.StandardTypes.PORT;


/********************************************************************
 * Test of the {@link Server} class.
 *
 * @author eso
 */
public class ServerTest
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test of {@link Server#handleClientRequest(java.net.Socket)}
	 */
	@Test
	public void testHandleClientRequest()
	{
		Log.setGlobalMinimumLogLevel(LogLevel.INFO);

		Server aServer =
			new Server().with(NAME, "TestServer").with(PORT, 8008)
						.with(Server.REQUEST_HANDLER, new HttpRequestHander());

		new Thread(aServer).start();

		@SuppressWarnings("boxing")
		Function<String, String> fGet =
			SocketEndpoint.textRequest("GET /index.html\r\n\r\n", value(19))
						  .from(Endpoint.at("socket://localhost:8008"));

		System.out.printf("SERVER RESPONSE: %s\n", fGet.result());
		aServer.stop();
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A {@link Server} request handler implementation for HTTP requests.
	 *
	 * @author eso
	 */
	static class HttpRequestHander implements RequestHandler
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public void handleRequest(Server	   rServer,
								  InputStream  rRequest,
								  OutputStream rResponse)
		{
			try
			{
				BufferedReader rIn  =
					new BufferedReader(new InputStreamReader(rRequest));
				PrintWriter    rOut = new PrintWriter(rResponse);

				String sInputLine;

				System.out.printf("---- REQUEST ----\n");

				while ((sInputLine = rIn.readLine()) != null &&
					   !sInputLine.equals("\r\n"))
				{
					System.out.println(sInputLine);

					if (sInputLine.isEmpty())
					{
						break;
					}
				}

				rOut.write("HTTP/1.0 200 OK\r\n");
				rOut.write("\r\n");
				rOut.flush();
			}
			catch (Exception e)
			{
				throw new CommunicationException(e);
			}
		}
	}
}
