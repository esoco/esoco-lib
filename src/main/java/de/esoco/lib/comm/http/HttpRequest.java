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

import de.esoco.lib.expression.Conversions;
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.net.NetUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;

import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_MAX_HEADER_LINE_SIZE;
import static de.esoco.lib.comm.http.HttpStatusCode.badRequest;


/********************************************************************
 * A class that collects the data of an HTTP request and additional information
 * (like headers) in it's relations.
 *
 * @author eso
 */
public class HttpRequest extends RelatedObject
{
	//~ Instance fields --------------------------------------------------------

	private final HttpRequestMethod eRequestMethod;
	private final String		    sRequestPath;
	private final Reader		    aRequestBodyReader;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Reads the incoming request and throws an exception if it doesn't match
	 * the requirements.
	 *
	 * @param  rInput rInputReader The reader to read the request from
	 *
	 * @return A pair containing the HTTP request method and the
	 *
	 * @throws IOException         If reading from the input fails
	 * @throws HttpStatusException The corresponding HTTP status if the request
	 *                             violates requirements
	 */
	public HttpRequest(InputStream rInput) throws IOException,
												  HttpStatusException
	{
		Reader aHeaderReader =
			new BufferedReader(new InputStreamReader(rInput,
													 StandardCharsets.US_ASCII));

		String sRequestLine = readLine(aHeaderReader);

		if (sRequestLine == null)
		{
			badRequest("Unterminated request");
		}
		else if (sRequestLine.isEmpty())
		{
			badRequest("Empty request line");
		}

		HttpRequestMethod eMethod	    = HttpRequestMethod.GET;
		String[]		  aRequestParts = sRequestLine.split(" ");

		if (aRequestParts.length != 3 || !aRequestParts[2].startsWith("HTTP/"))
		{
			badRequest("Malformed request line: " + sRequestLine);
		}

		try
		{
			eMethod = HttpRequestMethod.valueOf(aRequestParts[0]);
		}
		catch (Exception e)
		{
			badRequest("Unknown request method: " + aRequestParts[0]);
		}

		readHeaders(aHeaderReader);

		eRequestMethod     = eMethod;
		sRequestPath	   = aRequestParts[1];
		aRequestBodyReader =
			new BufferedReader(new InputStreamReader(rInput,
													 StandardCharsets.UTF_8));
	}

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rRequestMethod     The request method
	 * @param rRequestPath       The request path
	 * @param rRequestBodyReader The reader that given access to the request
	 *                           body (if applicable)
	 */
	public HttpRequest(HttpRequestMethod rRequestMethod,
					   String			 rRequestPath,
					   Reader			 rRequestBodyReader)
	{
		eRequestMethod     = rRequestMethod;
		sRequestPath	   = rRequestPath;
		aRequestBodyReader = rRequestBodyReader;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns a reader that provides the body of the request. Will be yield no
	 * data (but will never be NULL) if the request has no body. The relation
	 * {@link HttpHeaderTypes#CONTENT_LENGTH} will contain the length of the
	 * body data.
	 *
	 * @return A reader that provides the body data
	 */
	public final Reader getBody()
	{
		return aRequestBodyReader;
	}

	/***************************************
	 * Returns the request method.
	 *
	 * @return The request method
	 */
	public final HttpRequestMethod getMethod()
	{
		return eRequestMethod;
	}

	/***************************************
	 * Returns the requested path.
	 *
	 * @return The requested path
	 */
	public final String getPath()
	{
		return sRequestPath;
	}

	/***************************************
	 * Tries to set an HTTP request header field as a relation on this instance.
	 * Invokes {@link HttpHeaderTypes#get(String)} with the given header name
	 * and if successfull tries to parse the value with {@link
	 * Conversions#parseValue(String, Class)} with the relation datatype.
	 *
	 * @param  sHeaderName  The name of the header field
	 * @param  sHeaderValue The value of the header field
	 *
	 * @throws HttpStatusException If the field value could not be parsed
	 */
	@SuppressWarnings("unchecked")
	protected void parseRequestHeader(String sHeaderName, String sHeaderValue)
		throws HttpStatusException
	{
		RelationType<?> rHeaderType = HttpHeaderTypes.get(sHeaderName);

		if (rHeaderType != null)
		{
			try
			{
				Object rValue =
					Conversions.parseValue(sHeaderValue,
										   rHeaderType.getTargetType());

				set((RelationType<Object>) rHeaderType, rValue);
			}
			catch (Exception e)
			{
				badRequest(String.format("Invalid value for header '%s': %s",
										 sHeaderName,
										 sHeaderValue));
			}
		}
	}

	/***************************************
	 * Reads the request headers into a map and returns it.
	 *
	 * @param  rInputReader The reader to read the header fields from
	 *
	 * @return A mapping from header field names to field values
	 *
	 * @throws IOException         On stream acces failures
	 * @throws HttpStatusException On malformed requests
	 */
	protected Map<String, List<String>> readHeaders(Reader rInputReader)
		throws IOException, HttpStatusException
	{
		Map<String, List<String>> aHeaders = new LinkedHashMap<>();
		String					  sHeader;

		do
		{
			sHeader = readLine(rInputReader);

			if (sHeader == null)
			{
				badRequest("Request must be terminated with CRLF on empty line");
			}
			else if (!sHeader.isEmpty())
			{
				int nColon = sHeader.indexOf(':');

				if (nColon < 1)
				{
					badRequest("Malformed header: " + sHeader);
				}

				String sHeaderName  = sHeader.substring(0, nColon).trim();
				String sHeaderValue = sHeader.substring(nColon + 1).trim();

				List<String> rHeaderValues = aHeaders.get(sHeaderName);

				if (rHeaderValues == null)
				{
					rHeaderValues = new ArrayList<>();
				}

				rHeaderValues.add(sHeaderValue);
				aHeaders.put(sHeaderName, rHeaderValues);
				parseRequestHeader(sHeaderName, sHeaderValue);
			}
		}
		while (!sHeader.isEmpty());

		return aHeaders;
	}

	/***************************************
	 * Helper method to read a single HTTP request line (terminated with CRLF)
	 * from a {@link Reader}.
	 *
	 * @param  rReader The reader to read the line from
	 *
	 * @return The line string or NULL if no terminating CRLF could be found
	 *
	 * @throws IOException         If reading data fails
	 * @throws HttpStatusException If the line is not terminated correctly
	 */
	protected String readLine(Reader rReader) throws IOException
	{
		@SuppressWarnings("boxing")
		String sLine =
			StreamUtil.readUntil(rReader,
								 NetUtil.CRLF,
								 get(HTTP_MAX_HEADER_LINE_SIZE),
								 false);

		if (sLine == null)
		{
			badRequest("Request line not terminated with CRLF: " + sLine);
		}

		return sLine;
	}
}
