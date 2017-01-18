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
package de.esoco.lib.comm.http;

import java.net.HttpURLConnection;
import java.net.ProtocolException;


/********************************************************************
 * An enumeration of the different HTTP request methods.
 */
public enum HttpRequestMethod
{
	GET(false), HEAD(false), OPTIONS(true), TRACE(false), POST(true), PUT(true),
	DELETE(false), CONNECT(true);

	//~ Instance fields --------------------------------------------------------

	private final boolean bDoesOutput;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param bDoesOutput TRUE if this method requires to send data to the
	 *                    endpoint
	 */
	private HttpRequestMethod(boolean bDoesOutput)
	{
		this.bDoesOutput = bDoesOutput;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Applies this request method to a connection for an HTTP URL.
	 *
	 * @param rConnection The connection to apply this method to
	 */
	public void applyTo(HttpURLConnection rConnection)
	{
		try
		{
			rConnection.setRequestMethod(name());
			rConnection.setDoOutput(bDoesOutput);
		}
		catch (ProtocolException e)
		{
			// this should not be possible
			throw new IllegalStateException(e);
		}
	}

	/***************************************
	 * Check whether this request method also sends data to the communication
	 * endpoint.
	 *
	 * @return TRUE if this method performs output
	 */
	public boolean doesOutput()
	{
		return bDoesOutput;
	}
}
