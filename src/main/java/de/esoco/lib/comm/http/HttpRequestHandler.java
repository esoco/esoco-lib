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

import de.esoco.lib.comm.CommunicationRelationTypes;
import de.esoco.lib.comm.Server;
import de.esoco.lib.comm.Server.RequestHandler;
import de.esoco.lib.datatype.Pair;
import de.esoco.lib.expression.Conversions;
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.net.NetUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;

import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_REQUEST_HEADERS;
import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_RESPONSE_HEADERS;
import static de.esoco.lib.comm.CommunicationRelationTypes.RESPONSE_ENCODING;
import static de.esoco.lib.comm.http.HttpStatusCode.badRequest;


/********************************************************************
 * A {@link Server} request handler implementation for HTTP requests.
 *
 * @author eso
 */
public class HttpRequestHandler extends RelatedObject implements RequestHandler
{
	//~ Instance fields --------------------------------------------------------

	private HttpRequestMethodHandler rRequestMethodHandler = null;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance with a certain request method handler.
	 *
	 * @param rHandler The handler for the HTTP request methods
	 */
	public HttpRequestHandler(HttpRequestMethodHandler rHandler)
	{
		rRequestMethodHandler = rHandler;
	}

	/***************************************
	 * Subclass constructor. If the subclass implements the HTTP request method
	 * handler interface {@link HttpRequestMethodHandler} it will be set as the
	 * request method handler directly. Otherwise the handler must be set with
	 * {@link #setRequestMethodHandler(HttpRequestMethodHandler)} before any
	 * requests are accepted or else a {@link NullPointerException} will occur.
	 */
	protected HttpRequestHandler()
	{
		if (this instanceof HttpRequestMethodHandler)
		{
			rRequestMethodHandler = (HttpRequestMethodHandler) this;
		}
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the handler for the HTTP request methods.
	 *
	 * @return The request method handler
	 */
	public final HttpRequestMethodHandler getRequestMethodHandler()
	{
		return rRequestMethodHandler;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void handleRequest(Relatable    rServerConfig,
							  InputStream  rRequest,
							  OutputStream rResponse) throws IOException
	{
		Writer aResponseHeaderWriter =
			new BufferedWriter(new OutputStreamWriter(rResponse,
													  StandardCharsets.US_ASCII));

		try
		{
			// copy default response headers if available
			get(HTTP_RESPONSE_HEADERS).putAll(rServerConfig.get(HTTP_RESPONSE_HEADERS));

			Reader aRequestHeaderReader =
				new BufferedReader(new InputStreamReader(rRequest,
														 StandardCharsets.US_ASCII));

			Pair<HttpRequestMethod, String> aRequestData =
				readRequestHeader(aRequestHeaderReader);

			Reader aRequestBodyReader =
				new BufferedReader(new InputStreamReader(rRequest,
														 StandardCharsets.US_ASCII));

			Reader rResponseData =
				rRequestMethodHandler.handleMethod(aRequestData.first(),
												   aRequestData.second(),
												   aRequestBodyReader);

			Writer aResponseBodyWriter =
				new OutputStreamWriter(rResponse,
									   rServerConfig.get(RESPONSE_ENCODING));

			writeResponseHeader(HttpStatusCode.OK, aResponseHeaderWriter);
			StreamUtil.send(rResponseData, aResponseBodyWriter);
		}
		catch (HttpStatusException e)
		{
			writeResponseHeader(e.getStatusCode(), aResponseHeaderWriter);
		}
		catch (Exception e)
		{
			writeResponseHeader(HttpStatusCode.INTERNAL_SERVER_ERROR,
								aResponseHeaderWriter);
		}

		rResponse.flush();
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
	 * Helder method to read a single HTTP request line (terminated with CRLF)
	 * from a {@link Reader}.
	 *
	 * @param  rReader The reader to read the line from
	 *
	 * @return The line string or NULL if no terminating CRLF could be found
	 *
	 * @throws IOException If reading data fails
	 */
	protected String readLine(Reader rReader) throws IOException
	{
		String sLine =
			StreamUtil.readUntil(rReader, NetUtil.CRLF, Short.MAX_VALUE, false);

		if (!sLine.endsWith(NetUtil.CRLF))
		{
			badRequest("Request line not terminated with CRLF: " + sLine);
		}

		return sLine.substring(0, sLine.length() - 2);
	}

	/***************************************
	 * Reads the incoming request and throws an exception if it doesn't match
	 * the requirements.
	 *
	 * @param  rInputReader The reader to read the request from
	 *
	 * @return A pair containing the HTTP request method and the
	 *
	 * @throws IOException         If reading from the input fails
	 * @throws HttpStatusException The corresponding HTTP status if the request
	 *                             violates the requirements
	 */
	protected Pair<HttpRequestMethod, String> readRequestHeader(
		Reader rInputReader) throws IOException, HttpStatusException
	{
		String sRequestLine = readLine(rInputReader);

		if (sRequestLine == null)
		{
			badRequest("Unterminated request");
		}
		else if (sRequestLine.isEmpty())
		{
			badRequest("Empty request line");
		}

		readRequestHeaders(rInputReader);

		HttpRequestMethod eRequestMethod = HttpRequestMethod.GET;
		String[]		  aRequestParts  = sRequestLine.split(" ");

		if (aRequestParts.length != 3 || !aRequestParts[2].startsWith("HTTP/"))
		{
			badRequest("Malformed request line: " + sRequestLine);
		}

		try
		{
			eRequestMethod = HttpRequestMethod.valueOf(aRequestParts[0]);
		}
		catch (Exception e)
		{
			badRequest("Unknown request method: " + aRequestParts[0]);
		}

		return new Pair<HttpRequestMethod, String>(eRequestMethod,
												   aRequestParts[1]);
	}

	/***************************************
	 * Reads the request headers into the map relation {@link
	 * CommunicationRelationTypes#HTTP_REQUEST_HEADERS} and also maps them to
	 * direct relations with {@link HttpHeaderTypes#get(String)} if possible.
	 *
	 * @param  rInputReader
	 *
	 * @throws IOException
	 * @throws HttpStatusException
	 */
	protected void readRequestHeaders(Reader rInputReader)
		throws IOException, HttpStatusException
	{
		Map<String, List<String>> aHeaders = get(HTTP_REQUEST_HEADERS);
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
	}

	/***************************************
	 * Sets the handler for the HTTP request methods.
	 *
	 * @param rHandler The new request method handler
	 */
	protected final void setRequestMethodHandler(
		HttpRequestMethodHandler rHandler)
	{
		rRequestMethodHandler = rHandler;
	}

	/***************************************
	 * Writes the header for an HTTP response with a certain status code to a
	 * {@link Writer}.
	 *
	 * @param  eStatus The response status
	 * @param  rOut    The output writer
	 *
	 * @throws IOException If writing data fails
	 */
	protected void writeResponseHeader(HttpStatusCode eStatus, Writer rOut)
		throws IOException
	{
		rOut.write(eStatus.toResponseString());

		for (Entry<String, List<String>> rResponseHeader :
			 get(HTTP_RESPONSE_HEADERS).entrySet())
		{
			rOut.write(rResponseHeader.getKey());
			rOut.write(": ");
			rOut.write(rResponseHeader.getValue().get(0));
			rOut.write("\r\n");
		}

		rOut.write("\r\n");
	}

	//~ Inner Interfaces -------------------------------------------------------

	/********************************************************************
	 * An interface for the handling of the distinct HTTP request methods. It is
	 * a function interface so only the method {@link #doGet(String)} needs to
	 * be implemented for basic GET request functionality. But implementors can
	 * also implement support for other requests methods like POST, PUT, and
	 * DELETE if necessary. The default implementations for this methods always
	 * throw an {@link UnsupportedOperationException}. If other request methods
	 * need to be supported the implementation may also override the method
	 * {@link #handleMethod(HttpRequestMethod, String, Reader)} which switches
	 * over the {@link HttpRequestMethod} and throws a bad request exception on
	 * unsupported exceptions.
	 *
	 * @author eso
	 */
	@FunctionalInterface
	public static interface HttpRequestMethodHandler
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * This method must be always be implemented to provide the basic
		 * functionality in the form of GET requests.
		 *
		 * @param  sPath The requested path
		 *
		 * @return A reader providing access to the data of the response body
		 */
		public Reader doGet(String sPath);

		/***************************************
		 * Must be overridden to support the DELETE request method. The default
		 * implementation just throws an {@link UnsupportedOperationException}.
		 *
		 * @param  rRequestPath The path at which to execute the request
		 *
		 * @return A reader that gives access to the response data
		 *
		 * @throws HttpStatusException If executing the request fails
		 */
		default Reader doDelete(String rRequestPath) throws HttpStatusException
		{
			throw new UnsupportedOperationException("PUT not supported");
		}

		/***************************************
		 * Must be overridden to support the POST request method. The default
		 * implementation just throws an {@link UnsupportedOperationException}.
		 *
		 * @param  rRequestPath       The path at which to execute the request
		 * @param  rRequestBodyReader A reader which provides access to the
		 *                            request body
		 *
		 * @return A reader that gives access to the response data
		 *
		 * @throws HttpStatusException If executing the request fails
		 */
		default Reader doPost(String rRequestPath, Reader rRequestBodyReader)
			throws HttpStatusException
		{
			throw new UnsupportedOperationException("PUT not supported");
		}

		/***************************************
		 * Must be overridden to support the PUT request method. The default
		 * implementation just throws an {@link UnsupportedOperationException}.
		 *
		 * @param  rRequestPath       The path at which to execute the request
		 * @param  rRequestBodyReader A reader which provides access to the
		 *                            request body
		 *
		 * @return A reader that gives access to the response data
		 *
		 * @throws HttpStatusException If executing the request fails
		 */
		default Reader doPut(String rRequestPath, Reader rRequestBodyReader)
			throws HttpStatusException
		{
			throw new UnsupportedOperationException("PUT not supported");
		}

		/***************************************
		 * Creates the response for the current request and returns a {@link
		 * Reader} that provides the data of the response body.
		 *
		 * @param  eRequestMethod     The request method to create the response
		 *                            for
		 * @param  sRequestPath       The requested path
		 * @param  rRequestBodyReader A reader that provides access to the
		 *                            request body (if available)
		 *
		 * @return A reader instance that provides the data of the response body
		 *
		 * @throws IOException If reading or writing data fails
		 */
		default Reader handleMethod(HttpRequestMethod eRequestMethod,
									String			  sRequestPath,
									Reader			  rRequestBodyReader)
			throws IOException
		{
			Reader rResponse = null;

			switch (eRequestMethod)
			{
				case GET:
					rResponse = doGet(sRequestPath);
					break;

				case DELETE:
					rResponse = doDelete(sRequestPath);
					break;

				case POST:
					rResponse = doPost(sRequestPath, rRequestBodyReader);
					break;

				case PUT:
					rResponse = doPut(sRequestPath, rRequestBodyReader);
					break;

				default:
					badRequest("Unsupported request method: " + eRequestMethod);
			}

			return rResponse;
		}
	}
}
