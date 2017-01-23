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
import de.esoco.lib.expression.Conversions;
import de.esoco.lib.io.EchoInputStream;
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.logging.Log;
import de.esoco.lib.net.NetUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;

import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_MAX_HEADER_LINE_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_RESPONSE_HEADERS;
import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_STATUS_CODE;
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
	 * handler interface {@link HttpRequestHandler} it will be set as the
	 * request method handler directly. Otherwise the handler must be set with
	 * {@link #setRequestMethodHandler(HttpRequestHandler)} before any requests
	 * are accepted or else a {@link NullPointerException} will occur.
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
							  InputStream  rRequestStream,
							  OutputStream rResponseStream) throws IOException
	{
		try
		{
			ByteArrayOutputStream aRequestCopy =
				new ByteArrayOutputStream(2048);

			rRequestStream = new EchoInputStream(rRequestStream, aRequestCopy);

			HttpRequest aRequest = readRequest(rRequestStream);

			Log.info("Handling request: " +
					 aRequestCopy.toString(StandardCharsets.US_ASCII.name()));

			sendResponse(rServerConfig, aRequest, rResponseStream);
		}
		catch (Exception e)
		{
			HttpStatusCode eStatus  = HttpStatusCode.INTERNAL_SERVER_ERROR;
			String		   sMessage = "";

			if (e instanceof HttpStatusException)
			{
				eStatus  = ((HttpStatusException) e).getStatusCode();
				sMessage = e.getMessage();
			}

			Log.errorf(e, "HTTP Request failed (%s): %s", eStatus, sMessage);

			HttpResponse rErrorResponse = new HttpResponse(eStatus, sMessage);

			try
			{
				rErrorResponse.write(rResponseStream);
			}
			catch (Exception eResponse)
			{
				Log.error("Response output failed", eResponse);
			}
		}

		rResponseStream.flush();
	}

	/***************************************
	 * Reads the HTTP request from the given input stream. The default
	 * implementation just returns a new instance of {@link HttpRequest}
	 * containing the data from the stream.
	 *
	 * @param  rInput The input stream to read the request from
	 *
	 * @return
	 *
	 * @throws IOException         If reading from the input fails
	 * @throws HttpStatusException The corresponding HTTP status if the request
	 *                             violates requirements
	 */
	protected HttpRequest readRequest(InputStream rInput)
		throws IOException, HttpStatusException
	{
		return new HttpRequest(rInput);
	}

	/***************************************
	 * Sends the HTTP response for an HTTP request through the given output
	 * stream.
	 *
	 * @param  rServerConfig The configuration to read default values from
	 * @param  rRequest      The HTTP request to send the response for
	 * @param  rOutput       The output stream to write the response to
	 *
	 * @throws IOException If writing the output fails
	 */
	protected void sendResponse(Relatable    rServerConfig,
								HttpRequest  rRequest,
								OutputStream rOutput) throws IOException
	{
		HttpResponse rResponse = rRequestMethodHandler.handleMethod(rRequest);

		Map<String, List<String>> rDefaultResponseHeaders =
			rServerConfig.get(HTTP_RESPONSE_HEADERS);

		Map<String, List<String>> rResponseHeaders =
			rResponse.get(HTTP_RESPONSE_HEADERS);

		for (Entry<String, List<String>> rDefaultHeader :
			 rDefaultResponseHeaders.entrySet())
		{
			String sHeaderName = rDefaultHeader.getKey();

			if (!rResponseHeaders.containsKey(sHeaderName))
			{
				rResponseHeaders.put(sHeaderName, rDefaultHeader.getValue());
			}
		}

		rResponse.write(rOutput);
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

	//~ Inner Interfaces -------------------------------------------------------

	/********************************************************************
	 * An interface for the handling of the distinct HTTP request methods. It is
	 * a functional interface so only the method {@link #doGet(String)} needs to
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
		 * @param  rRequest The request to execute
		 *
		 * @return A {@link HttpResponse} object containing relations with the
		 *         response data
		 *
		 * @throws HttpStatusException If executing the request fails
		 */
		public HttpResponse doGet(HttpRequest rRequest)
			throws HttpStatusException;

		/***************************************
		 * Must be overridden to support the DELETE request method. The default
		 * implementation just throws an {@link HttpStatusException}.
		 *
		 * @param  rRequest The request to execute
		 *
		 * @return A {@link HttpResponse} object containing relations with the
		 *         response data
		 *
		 * @throws HttpStatusException If executing the request fails
		 */
		default HttpResponse doDelete(HttpRequest rRequest)
			throws HttpStatusException
		{
			throw new HttpStatusException(HttpStatusCode.NOT_IMPLEMENTED,
										  "DELETE not supported");
		}

		/***************************************
		 * Must be overridden to support the POST request method. The default
		 * implementation just throws an {@link HttpStatusException}.
		 *
		 * @param  rRequest The request to execute
		 *
		 * @return A {@link HttpResponse} object containing relations with the
		 *         response data
		 *
		 * @throws HttpStatusException If executing the request fails
		 */
		default HttpResponse doPost(HttpRequest rRequest)
			throws HttpStatusException
		{
			throw new HttpStatusException(HttpStatusCode.NOT_IMPLEMENTED,
										  "POST not supported");
		}

		/***************************************
		 * Must be overridden to support the PUT request method. The default
		 * implementation just throws an {@link HttpStatusException}.
		 *
		 * @param  rRequest The request to execute
		 *
		 * @return A {@link HttpResponse} object containing relations with the
		 *         response data
		 *
		 * @throws HttpStatusException If executing the request fails
		 */
		default HttpResponse doPut(HttpRequest rRequest)
			throws HttpStatusException
		{
			throw new HttpStatusException(HttpStatusCode.NOT_IMPLEMENTED,
										  "PUT not supported");
		}

		/***************************************
		 * Creates the response for the current request and returns a {@link
		 * Reader} that provides the data of the response body.
		 *
		 * @param  rRequest rRequest eRequestMethod The request method to create
		 *                  the response for
		 *
		 * @return A {@link HttpResponse} object containing relations with the
		 *         response data
		 *
		 * @throws IOException If reading or writing data fails
		 */
		default HttpResponse handleMethod(HttpRequest rRequest)
			throws IOException
		{
			HttpResponse rResponse = null;

			switch (rRequest.getMethod())
			{
				case GET:
					rResponse = doGet(rRequest);
					break;

				case DELETE:
					rResponse = doDelete(rRequest);
					break;

				case POST:
					rResponse = doPost(rRequest);
					break;

				case PUT:
					rResponse = doPut(rRequest);
					break;

				default:
					badRequest("Unsupported request method: " +
							   rRequest.getMethod());
			}

			return rResponse;
		}
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A class that collects the data of an HTTP request and additional
	 * information (like headers) in it's relations.
	 *
	 * @author eso
	 */
	public static class HttpRequest extends RelatedObject
	{
		//~ Instance fields ----------------------------------------------------

		private final HttpRequestMethod eRequestMethod;
		private final String		    sRequestPath;
		private final Reader		    aRequestBodyReader;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Reads the incoming request and throws an exception if it doesn't
		 * match the requirements.
		 *
		 * @param  rInput rInputReader The reader to read the request from
		 *
		 * @return A pair containing the HTTP request method and the
		 *
		 * @throws IOException         If reading from the input fails
		 * @throws HttpStatusException The corresponding HTTP status if the
		 *                             request violates requirements
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

			if (aRequestParts.length != 3 ||
				!aRequestParts[2].startsWith("HTTP/"))
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

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Returns a reader that provides the body of the request. Will be yield
		 * no data (but will never be NULL) if the request has no body. The
		 * relation {@link HttpHeaderTypes#CONTENT_LENGTH} will contain the
		 * length of the body data.
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
		 * Tries to set an HTTP request header field as a relation on this
		 * instance. Invokes {@link HttpHeaderTypes#get(String)} with the given
		 * header name and if successfull tries to parse the value with {@link
		 * Conversions#parseValue(String, Class)} with the relation datatype.
		 *
		 * @param  sHeaderName  The name of the header field
		 * @param  sHeaderValue The value of the header field
		 *
		 * @throws HttpStatusException If the field value could not be parsed
		 */
		@SuppressWarnings("unchecked")
		protected void parseRequestHeader(
			String sHeaderName,
			String sHeaderValue) throws HttpStatusException
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
		 * Helper method to read a single HTTP request line (terminated with
		 * CRLF) from a {@link Reader}.
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

	/********************************************************************
	 * A class that contains the data of an HTTP response and additional
	 * response information (like headers) in it's relations.
	 *
	 * @author eso
	 */
	public static class HttpResponse extends RelatedObject
	{
		//~ Instance fields ----------------------------------------------------

		private final Reader rResponseBodyReader;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance for a successful request with the status code
		 * {@link HttpStatusCode#OK}.
		 *
		 * @param rResponseData A reader that provides access to the data of the
		 *                      response body
		 *
		 * @see   HttpResponse#HttpResponse(HttpStatusCode, Reader)
		 */
		public HttpResponse(Reader rResponseData)
		{
			this(HttpStatusCode.OK, rResponseData);
		}

		/***************************************
		 * Creates a new instance for a successful request from a response data
		 * string. The HTTP status code will be be set to {@link
		 * HttpStatusCode#OK}.
		 *
		 * @param sResponseData The response data string
		 *
		 * @see   HttpResponse#HttpResponse(HttpStatusCode, Reader)
		 */
		public HttpResponse(String sResponseData)
		{
			this(HttpStatusCode.OK, sResponseData);
		}

		/***************************************
		 * Creates a new instance with a certain status code. The response data
		 * must be provided as a {@link Reader} instance. The status code will
		 * be set on this instance as a relation with the relation type {@link
		 * CommunicationRelationTypes#HTTP_STATUS_CODE}.
		 *
		 * @param eStatus       The response status code
		 * @param rResponseData A reader that provides access to the data of the
		 *                      response body
		 */
		public HttpResponse(HttpStatusCode eStatus, Reader rResponseData)
		{
			rResponseBodyReader = rResponseData;

			set(HTTP_STATUS_CODE, eStatus);
		}

		/***************************************
		 * Creates a new instance with a certain status code and (short)
		 * response data as a string. For longer response bodies it is
		 * recommended to use the constructor with a {@link Reader} argument.
		 *
		 * @param eStatus       The response status code
		 * @param rResponseData A reader that provides access to the data of the
		 *                      response body
		 *
		 * @see   HttpResponse#HttpResponse(HttpStatusCode, Reader)
		 */
		public HttpResponse(HttpStatusCode eStatus, String sResponseData)
		{
			this(eStatus, new StringReader(sResponseData));
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Writes this response to the given output stream.
		 *
		 * @param  rOutput The target output stream
		 *
		 * @throws IOException If writing to the stream fails
		 */
		public void write(OutputStream rOutput) throws IOException
		{
			Writer aResponseHeaderWriter =
				new BufferedWriter(new OutputStreamWriter(rOutput,
														  StandardCharsets.US_ASCII));

			Writer aResponseBodyWriter =
				new OutputStreamWriter(rOutput, get(RESPONSE_ENCODING));

			Map<String, List<String>> rResponseHeaders =
				get(HTTP_RESPONSE_HEADERS);

//			rResponseHeaders.put(eField.getFieldName(),
//								 Arrays.asList(rValue.toString()));
//
//			setResponseHeader(HttpHeaderField.CONTENT_TYPE, get(CONTENT_TYPE));
//			setResponseHeader(HttpHeaderField.CONTENT_LENGTH,
//							  get(CONTENT_LENGTH));

			writeResponseHeader(HttpStatusCode.OK, aResponseHeaderWriter);
			StreamUtil.send(rResponseBodyReader, aResponseBodyWriter);
			rOutput.flush();
		}

		/***************************************
		 * Writes the header for an HTTP response with a certain status code to
		 * a {@link Writer}.
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
				rOut.write(NetUtil.CRLF);
			}

			// terminate with empty line
			rOut.write(NetUtil.CRLF);
		}
	}
}
