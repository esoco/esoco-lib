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

import de.esoco.lib.collection.CollectionUtil;
import de.esoco.lib.comm.http.HttpHeaderTypes.HttpHeaderField;
import de.esoco.lib.datatype.Pair;

import java.io.IOException;

import java.util.Collections;
import java.util.Map;


/********************************************************************
 * An IO exception to report HTTP status codes.
 *
 * @author eso
 */
public class HttpStatusException extends IOException
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long serialVersionUID = 1L;

	//~ Instance fields --------------------------------------------------------

	private final HttpStatusCode eStatusCode;

	private Map<HttpHeaderField, String> rResponseHeaders =
		Collections.emptyMap();

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance with a status code and causing exception.
	 *
	 * @param eStatusCode The status code
	 * @param eCause      The causing exception
	 */
	public HttpStatusException(HttpStatusCode eStatusCode, Exception eCause)
	{
		super(eCause);

		this.eStatusCode = eStatusCode;
	}

	/***************************************
	 * Creates a new instance with a status code and message.
	 *
	 * @param eStatusCode      The status code
	 * @param sMessage         The error message
	 * @param rResponseHeaders An optional array of headers to be set on the
	 *                         response
	 */
	@SafeVarargs
	public HttpStatusException(
		HttpStatusCode					 eStatusCode,
		String							 sMessage,
		Pair<HttpHeaderField, String>... rResponseHeaders)
	{
		super(sMessage);

		this.eStatusCode	  = eStatusCode;
		this.rResponseHeaders =
			CollectionUtil.fixedOrderedMapOf(rResponseHeaders);
	}

	/***************************************
	 * Creates a new instance with a status code, message, and causing
	 * exception.
	 *
	 * @param eStatusCode The status code
	 * @param sMessage    The error message
	 * @param eCause      The causing exception
	 */
	public HttpStatusException(HttpStatusCode eStatusCode,
							   String		  sMessage,
							   Exception	  eCause)
	{
		super(sMessage, eCause);

		this.eStatusCode = eStatusCode;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns a map of the optional response headers for this status code
	 * exception.
	 *
	 * @return The response headers map (may be empty but will never be NULL)
	 */
	public final Map<HttpHeaderField, String> getResponseHeaders()
	{
		return rResponseHeaders;
	}

	/***************************************
	 * Returns the HTTP status code.
	 *
	 * @return The status code
	 */
	public final HttpStatusCode getStatusCode()
	{
		return eStatusCode;
	}
}
