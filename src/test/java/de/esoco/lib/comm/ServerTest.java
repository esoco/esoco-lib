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
import de.esoco.lib.expression.Functions;
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogLevel;

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
			new FileSystemSpace<>("src/test/html/testsite",
								  "index.html",
								  Functions.tryWith(f -> new FileReader(f),
													r -> StreamUtil.readAll(r,
																			8192,
																			Short.MAX_VALUE)));

		HttpRequestMethodHandler aMethodHandler =
			new ObjectSpaceHttpMethodHandler(aFileSpace, "");

		RequestHandlerFactory aFactory =
			rConfig -> new HttpRequestHandler(rConfig, aMethodHandler);

		Server aServer =
			new Server(aFactory).with(NAME, "TestServer").with(PORT, 8008);

		aServer.run();
	}
}
