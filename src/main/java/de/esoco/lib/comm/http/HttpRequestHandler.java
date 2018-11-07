//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
import de.esoco.lib.comm.Server;
import de.esoco.lib.comm.Server.RequestHandler;
import de.esoco.lib.comm.http.HttpHeaderTypes.HttpHeaderField;
import de.esoco.lib.comm.http.HttpStatusException.EmptyRequestException;
import de.esoco.lib.datatype.Pair;
import de.esoco.lib.io.EchoInputStream;
import de.esoco.lib.logging.Log;
import de.esoco.lib.security.AuthenticationService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;

import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_MAX_HEADER_LINE_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_RESPONSE_HEADERS;
import static de.esoco.lib.comm.http.HttpStatusCode.badRequest;
import static de.esoco.lib.security.SecurityRelationTypes.AUTHENTICATION_METHOD;
import static de.esoco.lib.security.SecurityRelationTypes.AUTHENTICATION_SERVICE;
import static de.esoco.lib.security.SecurityRelationTypes.LOGIN_NAME;
import static de.esoco.lib.security.SecurityRelationTypes.PASSWORD;

import static org.obrel.type.StandardTypes.EXCEPTION;
import static org.obrel.type.StandardTypes.IP_ADDRESS;
import static org.obrel.type.StandardTypes.NAME;


/********************************************************************
 * A {@link Server} request handler implementation for HTTP requests.
 *
 * @author eso
 */
public class HttpRequestHandler extends RelatedObject implements RequestHandler
{
	//~ Static fields/initializers ---------------------------------------------

	private static final Set<String> SUPPORTED_AUTH_METHODS =
		CollectionUtil.setOf("Basic", "BCrypt");

	private static final ThreadLocal<HttpRequest> aThreadLocalRequest =
		new ThreadLocal<>();

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
	 * {@link #setRequestMethodHandler(HttpRequestMethodHandler)} before any
	 * requests are accepted or else a {@link NullPointerException} will occur.
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

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns the active HTTP request for the current thread.
	 *
	 * @return The current thread's HTTP request or NULL if no request is
	 *         handled by the thread
	 */
	public static final HttpRequest getThreadLocalRequest()
	{
		return aThreadLocalRequest.get();
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
	public String handleRequest(
		InputStream  rRequestStream,
		OutputStream rResponseStream) throws IOException
	{
		ByteArrayOutputStream aRequestCopy = new ByteArrayOutputStream(2048);
		String				  sRequest     = null;

		try
		{
			rRequestStream = new EchoInputStream(rRequestStream, aRequestCopy);

			HttpRequest rRequest = readRequest(rRequestStream);

			rRequest.set(IP_ADDRESS, get(IP_ADDRESS));
			aThreadLocalRequest.set(rRequest);

			checkAuthentication(rRequest);

			sendResponse(createResponse(rRequest), rResponseStream);
		}
		catch (Exception e)
		{
			HttpStatusCode eStatus		 = HttpStatusCode.INTERNAL_SERVER_ERROR;
			boolean		   bEmptyRequest = (e instanceof EmptyRequestException);
			String		   sMessage		 = "";

			Map<HttpHeaderField, String> rResponseHeaders = null;

			set(EXCEPTION, e);

			if (e instanceof HttpStatusException)
			{
				HttpStatusException eStatusException = (HttpStatusException) e;

				sMessage		 = eStatusException.getMessage();
				eStatus			 = eStatusException.getStatusCode();
				rResponseHeaders = eStatusException.getResponseHeaders();

				if (!bEmptyRequest)
				{
					Log.infof(
						"HTTP status exception (%s): %s",
						eStatus,
						sMessage);
				}
			}
			else
			{
				Log.error("HTTP Request failed", e);
			}

			// ignore empty requests; some browsers open connections in advance
			if (!bEmptyRequest)
			{
				HttpResponse aErrorResponse =
					new HttpResponse(eStatus, sMessage);

				if (rResponseHeaders != null)
				{
					for (Entry<HttpHeaderField, String> rHeader :
						 rResponseHeaders.entrySet())
					{
						aErrorResponse.setHeader(
							rHeader.getKey(),
							rHeader.getValue());
					}
				}

				try
				{
					aErrorResponse.write(rResponseStream);
				}
				catch (Exception eResponse)
				{
					Log.info("Response output failed", eResponse);
				}
			}
		}
		finally
		{
			sRequest = aRequestCopy.toString(StandardCharsets.UTF_8.name());
		}

		rResponseStream.flush();

		return sRequest;
	}

	/***************************************
	 * Checks if authentication is needed and if so, whether the request
	 * contains the necessary authentication information.
	 *
	 * @param  rRequest The request to check for authentication if necessary
	 *
	 * @throws HttpStatusException If authentication is required but not
	 *                             provided
	 */
	protected void checkAuthentication(HttpRequest rRequest)
		throws HttpStatusException
	{
		AuthenticationService rAuthService =
			rContext.get(AUTHENTICATION_SERVICE);

		if (rAuthService != null)
		{
			boolean bAuthenticated = false;

			String sAuth = rRequest.get(HttpHeaderTypes.AUTHORIZATION);

			if (sAuth == null)
			{
				throw new HttpStatusException(
					HttpStatusCode.UNAUTHORIZED,
					"Authentication required",
					getAuthErrorHeader());
			}

			String[] aAuthHeader = sAuth.trim().split(" ");

			if (aAuthHeader.length == 2)
			{
				String sMethod = aAuthHeader[0];

				if (SUPPORTED_AUTH_METHODS.contains(sMethod))
				{
					String[] aCredential =
						new String(
							Base64.getDecoder().decode(aAuthHeader[1]),
							StandardCharsets.UTF_8).split(":");

					if (aCredential.length == 2)
					{
						Relatable aAuthData = new RelatedObject();

						aAuthData.set(AUTHENTICATION_METHOD, sMethod);
						aAuthData.set(LOGIN_NAME, aCredential[0]);
						aAuthData.set(PASSWORD, aCredential[1].toCharArray());
						bAuthenticated = rAuthService.authenticate(aAuthData);
					}
				}
			}

			if (!bAuthenticated)
			{
				throw new HttpStatusException(
					HttpStatusCode.UNAUTHORIZED,
					"Authentication invalid",
					getAuthErrorHeader());
			}
		}
	}

	/***************************************
	 * Creates the HTTP response for a certain HTTP request. The default
	 * implementation invokes the method {@link
	 * HttpRequestMethodHandler#handleMethod(HttpRequest)} of the request method
	 * handler.
	 *
	 * @param  rRequest The request to create the response for
	 *
	 * @return The response
	 *
	 * @throws IOException If handling the request method fails
	 */
	protected HttpResponse createResponse(HttpRequest rRequest)
		throws IOException
	{
		return rRequestMethodHandler.handleMethod(rRequest);
	}

	/***************************************
	 * Returns a pair of header field name and value for an authentication
	 * error.
	 *
	 * @return The authentication error header
	 */
	protected Pair<HttpHeaderField, String> getAuthErrorHeader()
	{
		return new Pair<>(
			HttpHeaderField.WWW_AUTHENTICATE,
			String.format("Basic realm=\"%s\"", getContext().get(NAME)));
	}

	/***************************************
	 * Reads the HTTP request from the given input stream. The default
	 * implementation just returns a new instance of {@link HttpRequest}
	 * containing the data from the stream.
	 *
	 * @param  rInput The input stream to read the request from
	 *
	 * @return A new HTTP request
	 *
	 * @throws IOException         If reading from the input fails
	 * @throws HttpStatusException The corresponding HTTP status if the request
	 *                             violates requirements
	 */
	@SuppressWarnings("boxing")
	protected HttpRequest readRequest(InputStream rInput)
		throws IOException, HttpStatusException
	{
		return new HttpRequest(rInput, rContext.get(HTTP_MAX_HEADER_LINE_SIZE));
	}

	/***************************************
	 * Sends the HTTP response for an HTTP request through the given output
	 * stream.
	 *
	 * @param  rResponse The HTTP response to send
	 * @param  rOutput   The output stream to write the response to
	 *
	 * @throws IOException If writing the output fails
	 */
	protected void sendResponse(HttpResponse rResponse, OutputStream rOutput)
		throws IOException
	{
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
	 * a functional interface so only the method {@link #doGet(HttpRequest)}
	 * needs to be implemented for basic GET request functionality. But
	 * implementors can also implement support for other requests methods like
	 * POST, PUT, and DELETE if necessary. The default implementations for this
	 * methods always throw an {@link UnsupportedOperationException}. If other
	 * request methods need to be supported the implementation may also override
	 * the method {@link #handleMethod(HttpRequest)} which switches over the
	 * {@link HttpRequestMethod} and throws a HTTP status code exception on
	 * unsupported request methods.
	 *
	 * <p>The handling of request methods is intended to be stateless so that a
	 * single instance should be able to handle multiple requests. The state
	 * that is associated with the handling of a particular request is stored in
	 * the associated {@link RequestHandler} instance and will be handed to the
	 * request method handler in the {@link HttpRequest} argument of the methods
	 * below.</p>
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
			throw new HttpStatusException(
				HttpStatusCode.NOT_IMPLEMENTED,
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
			throw new HttpStatusException(
				HttpStatusCode.NOT_IMPLEMENTED,
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
			throw new HttpStatusException(
				HttpStatusCode.NOT_IMPLEMENTED,
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
					badRequest(
						"Unsupported request method: " +
						rRequest.getMethod());
			}

			return rResponse;
		}
	}
}
