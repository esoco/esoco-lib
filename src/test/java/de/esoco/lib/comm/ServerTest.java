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

import de.esoco.lib.comm.Server.RequestHandlerFactory;
import de.esoco.lib.comm.http.HttpRequestHandler;
import de.esoco.lib.comm.http.HttpRequestHandler.HttpRequestMethodHandler;
import de.esoco.lib.comm.http.ObjectSpaceHttpMethodHandler;
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogLevel;

import java.io.File;
import java.io.FileReader;

import org.obrel.space.FileSystemSpace;
import org.obrel.space.ObjectSpace;

import static org.obrel.type.StandardTypes.NAME;
import static org.obrel.type.StandardTypes.PORT;


/********************************************************************
 * Test of the {@link Server} class.
 *
 * @author eso
 */
public class ServerTest
{
	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Runs a simple HTTP server for testing purposes.
	 *
	 * @param rArgs The args
	 */
	public static void main(String[] rArgs)
	{
		Log.setGlobalMinimumLogLevel(LogLevel.INFO);

		ObjectSpace<String> aFileSpace =
			new FileSystemSpace<>("src/test/html/testsite", f -> readFile(f));

		HttpRequestMethodHandler aMethodHandler =
			new ObjectSpaceHttpMethodHandler(aFileSpace, "");

		RequestHandlerFactory aFactory =
			rConfig -> new HttpRequestHandler(rConfig, aMethodHandler);

		Server aServer =
			new Server(aFactory).with(NAME, "TestServer").with(PORT, 8008);

		aServer.run();
//		new Thread(aServer).start();

//		@SuppressWarnings("boxing")
//		Function<String, String> fGet =
//			HttpEndpoint.httpGet("index.html")
//						.from(Endpoint.at("http://localhost:8008"));
//
//		System.out.printf("SERVER RESPONSE: %s\n", fGet.result());
//		aServer.stop();
	}

	/***************************************
	 * Reads a file into a string.
	 *
	 * @param  rFile The file
	 *
	 * @return The file contents as a string
	 */
	private static String readFile(File rFile)
	{
		try (FileReader aIn = new FileReader(rFile))
		{
			return StreamUtil.readAll(aIn, 8 * 1024, Short.MAX_VALUE);
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
}
