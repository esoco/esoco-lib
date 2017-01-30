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

import de.esoco.lib.comm.Server;
import de.esoco.lib.comm.Server.RequestHandler;
import de.esoco.lib.io.EchoInputStream;
import de.esoco.lib.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;

import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_RESPONSE_HEADERS;
import static de.esoco.lib.comm.http.HttpStatusCode.badRequest;


/********************************************************************
 * A {@link Server} request handler implementation for HTTP requests.
 *
 * @author eso
 */
public class HttpRequestHandler extends RelatedObject implements RequestHandler
{
	//~ Instance fields --------------------------------------------------------

	private Relatable				 rContext;
	private HttpRequestMethodHandler rRequestMethodHandler = null;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance with a certain request method handler.
	 *
	 * @param rContext The context for this request handler
	 * @param rHandler The handler for the HTTP request methods
	 */
	public HttpRequestHandler(
		Relatable				 rContext,
		HttpRequestMethodHandler rHandler)
	{
		this.rContext		  = rContext;
		rRequestMethodHandler = rHandler;
	}

	/***************************************
	 * Subclass constructor. If the subclass implements the HTTP request method
	 * handler interface {@link HttpRequestHandler} it will be set as the
	 * request method handler directly. Otherwise the handler must be set with
	 * {@link #setRequestMethodHandler(HttpRequestHandler)} before any requests
	 * are accepted or else a {@link NullPointerException} will occur.
	 *
	 * @param rContext The context of this request handler
	 */
	protected HttpRequestHandler(Relatable rContext)
	{
		this.rContext = rContext;

		if (this instanceof HttpRequestMethodHandler)
		{
			rRequestMethodHandler = (HttpRequestMethodHandler) this;
		}
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the context of this handler. The context is a {@link Relatable}
	 * object that provides access to relations containing configuration data.
	 * It may be readonly and should therefore not be modified by the receiver.
	 *
	 * @return The request handler context
	 */
	public final Relatable getContext()
	{
		return rContext;
	}

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
	public void handleRequest(
		InputStream  rRequestStream,
		OutputStream rResponseStream) throws IOException
	{
		try
		{
			ByteArrayOutputStream aRequestCopy =
				new ByteArrayOutputStream(2048);

			rRequestStream = new EchoInputStream(rRequestStream, aRequestCopy);

			HttpRequest aRequest = readRequest(rRequestStream);

			String sRequest =
				aRequestCopy.toString(StandardCharsets.US_ASCII.name())
							.replaceAll("\r\n", "¶");

			Log.info("Handling request: " + sRequest);

			sendResponse(aRequest, rResponseStream);
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
	 * @param  rRequest The HTTP request to send the response for
	 * @param  rOutput  The output stream to write the response to
	 *
	 * @throws IOException If writing the output fails
	 */
	protected void sendResponse(HttpRequest rRequest, OutputStream rOutput)
		throws IOException
	{
		HttpResponse rResponse = rRequestMethodHandler.handleMethod(rRequest);

		Map<String, List<String>> rDefaultResponseHeaders =
			rContext.get(HTTP_RESPONSE_HEADERS);

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
}
