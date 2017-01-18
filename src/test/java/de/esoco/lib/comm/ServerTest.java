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
package de.esoco.lib.comm;

import de.esoco.lib.comm.http.HttpRequestHandler;
import de.esoco.lib.expression.Function;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogLevel;

import java.io.StringReader;

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
			new Server(rServer ->
					   new HttpRequestHandler(sPath -> new StringReader(sPath)))
			.with(NAME, "TestServer").with(PORT, 8008);

		new Thread(aServer).start();

		@SuppressWarnings("boxing")
		Function<String, String> fGet =
			SocketEndpoint.textRequest("GET /index.html\r\n\r\n", value(19))
						  .from(Endpoint.at("socket://localhost:8008"));

		System.out.printf("SERVER RESPONSE: %s\n", fGet.result());
		aServer.stop();
	}
}
