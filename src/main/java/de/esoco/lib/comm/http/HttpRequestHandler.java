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
import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;

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

/**
 * A {@link Server} request handler implementation for HTTP requests.
 *
 * @author eso
 */
public class HttpRequestHandler extends RelatedObject
	implements RequestHandler {

	private static final Set<String> SUPPORTED_AUTH_METHODS =
		CollectionUtil.setOf("Basic", "BCrypt");

	private static final ThreadLocal<HttpRequest> threadLocalRequest =
		new ThreadLocal<>();

	private final Relatable context;

	private HttpRequestMethodHandler requestMethodHandler = null;

	/**
	 * Creates a new instance with a certain request method handler.
	 *
	 * @param context The context for this request handler
	 * @param handler The handler for the HTTP request methods
	 */
	public HttpRequestHandler(Relatable context,
		HttpRequestMethodHandler handler) {
		this.context = context;
		requestMethodHandler = handler;
	}

	/**
	 * Subclass constructor. If the subclass implements the HTTP request method
	 * handler interface {@link HttpRequestHandler} it will be set as the
	 * request method handler directly. Otherwise the handler must be set with
	 * {@link #setRequestMethodHandler(HttpRequestMethodHandler)} before any
	 * requests are accepted or else a {@link NullPointerException} will occur.
	 *
	 * @param context The context of this request handler
	 */
	protected HttpRequestHandler(Relatable context) {
		this.context = context;

		if (this instanceof HttpRequestMethodHandler) {
			requestMethodHandler = (HttpRequestMethodHandler) this;
		}
	}

	/**
	 * Returns the active HTTP request for the current thread.
	 *
	 * @return The current thread's HTTP request or NULL if no request is
	 * handled by the thread
	 */
	public static final HttpRequest getThreadLocalRequest() {
		return threadLocalRequest.get();
	}

	/**
	 * Returns the context of this handler. The context is a {@link Relatable}
	 * object that provides access to relations containing configuration data.
	 * It may be readonly and should therefore not be modified by the receiver.
	 *
	 * @return The request handler context
	 */
	public final Relatable getContext() {
		return context;
	}

	/**
	 * Returns the handler for the HTTP request methods.
	 *
	 * @return The request method handler
	 */
	public final HttpRequestMethodHandler getRequestMethodHandler() {
		return requestMethodHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String handleRequest(InputStream requestStream,
		OutputStream responseStream) throws IOException {
		ByteArrayOutputStream requestCopy = new ByteArrayOutputStream(2048);
		String result;

		try {
			requestStream = new EchoInputStream(requestStream, requestCopy);

			HttpRequest httpRequest = readRequest(requestStream);

			httpRequest.set(IP_ADDRESS, get(IP_ADDRESS));
			threadLocalRequest.set(httpRequest);

			checkAuthentication(httpRequest);

			sendResponse(createResponse(httpRequest), responseStream);
		} catch (Exception e) {
			HttpStatusCode status = HttpStatusCode.INTERNAL_SERVER_ERROR;
			boolean emptyRequest = (e instanceof EmptyRequestException);
			String message = "";

			Map<HttpHeaderField, String> responseHeaders = null;

			set(EXCEPTION, e);

			if (e instanceof HttpStatusException) {
				HttpStatusException statusException = (HttpStatusException) e;

				message = statusException.getMessage();
				status = statusException.getStatusCode();
				responseHeaders = statusException.getResponseHeaders();

				if (!emptyRequest) {
					Log.infof("HTTP status exception (%s): %s", status,
						message);
				}
			} else {
				Log.error("HTTP Request failed", e);
			}

			// ignore empty requests; some browsers open connections in advance
			if (!emptyRequest) {
				HttpResponse errorResponse = new HttpResponse(status, message);

				if (responseHeaders != null) {
					for (Entry<HttpHeaderField, String> header :
						responseHeaders.entrySet()) {
						errorResponse.setHeader(header.getKey(),
							header.getValue());
					}
				}

				try {
					errorResponse.write(responseStream);
				} catch (Exception response) {
					Log.info("Response output failed", response);
				}
			}
		} finally {
			result = requestCopy.toString(StandardCharsets.UTF_8.name());
		}

		responseStream.flush();

		return result;
	}

	/**
	 * Checks if authentication is needed and if so, whether the request
	 * contains the necessary authentication information.
	 *
	 * @param request The request to check for authentication if necessary
	 * @throws HttpStatusException If authentication is required but not
	 *                             provided
	 */
	protected void checkAuthentication(HttpRequest request)
		throws HttpStatusException {
		AuthenticationService authService =
			context.get(AUTHENTICATION_SERVICE);

		if (authService != null) {
			boolean authenticated = false;

			String auth = request.get(HttpHeaderTypes.AUTHORIZATION);

			if (auth == null) {
				throw new HttpStatusException(HttpStatusCode.UNAUTHORIZED,
					"Authentication required", getAuthErrorHeader());
			}

			String[] authHeader = auth.trim().split(" ");

			if (authHeader.length == 2) {
				String method = authHeader[0];

				if (SUPPORTED_AUTH_METHODS.contains(method)) {
					String[] credential =
						new String(Base64.getDecoder().decode(authHeader[1]),
							StandardCharsets.UTF_8).split(":");

					if (credential.length == 2) {
						Relatable authData = new RelatedObject();

						authData.set(AUTHENTICATION_METHOD, method);
						authData.set(LOGIN_NAME, credential[0]);
						authData.set(PASSWORD, credential[1].toCharArray());
						authenticated = authService.authenticate(authData);
					}
				}
			}

			if (!authenticated) {
				throw new HttpStatusException(HttpStatusCode.UNAUTHORIZED,
					"Authentication invalid", getAuthErrorHeader());
			}
		}
	}

	/**
	 * Creates the HTTP response for a certain HTTP request. The default
	 * implementation invokes the method
	 * {@link HttpRequestMethodHandler#handleMethod(HttpRequest)} of the 
	 * request
	 * method handler.
	 *
	 * @param request The request to create the response for
	 * @return The response
	 * @throws IOException If handling the request method fails
	 */
	protected HttpResponse createResponse(HttpRequest request)
		throws IOException {
		return requestMethodHandler.handleMethod(request);
	}

	/**
	 * Returns a pair of header field name and value for an authentication
	 * error.
	 *
	 * @return The authentication error header
	 */
	protected Pair<HttpHeaderField, String> getAuthErrorHeader() {
		return new Pair<>(HttpHeaderField.WWW_AUTHENTICATE,
			String.format("Basic realm=\"%s\"", getContext().get(NAME)));
	}

	/**
	 * Reads the HTTP request from the given input stream. The default
	 * implementation just returns a new instance of {@link HttpRequest}
	 * containing the data from the stream.
	 *
	 * @param input The input stream to read the request from
	 * @return A new HTTP request
	 * @throws IOException         If reading from the input fails
	 * @throws HttpStatusException The corresponding HTTP status if the request
	 *                             violates requirements
	 */
	@SuppressWarnings("boxing")
	protected HttpRequest readRequest(InputStream input)
		throws IOException, HttpStatusException {
		return new HttpRequest(input, context.get(HTTP_MAX_HEADER_LINE_SIZE));
	}

	/**
	 * Sends the HTTP response for an HTTP request through the given output
	 * stream.
	 *
	 * @param response The HTTP response to send
	 * @param output   The output stream to write the response to
	 * @throws IOException If writing the output fails
	 */
	protected void sendResponse(HttpResponse response, OutputStream output)
		throws IOException {
		Map<String, List<String>> defaultResponseHeaders =
			context.get(HTTP_RESPONSE_HEADERS);

		Map<String, List<String>> responseHeaders =
			response.get(HTTP_RESPONSE_HEADERS);

		for (Entry<String, List<String>> defaultHeader :
			defaultResponseHeaders.entrySet()) {
			String headerName = defaultHeader.getKey();

			if (!responseHeaders.containsKey(headerName)) {
				responseHeaders.put(headerName, defaultHeader.getValue());
			}
		}

		response.write(output);
	}

	/**
	 * Sets the handler for the HTTP request methods.
	 *
	 * @param handler The new request method handler
	 */
	protected final void setRequestMethodHandler(
		HttpRequestMethodHandler handler) {
		requestMethodHandler = handler;
	}

	/**
	 * An interface for the handling of the distinct HTTP request methods. 
	 * It is
	 * a functional interface so only the method {@link #doGet(HttpRequest)}
	 * needs to be implemented for basic GET request functionality. But
	 * implementors can also implement support for other requests methods like
	 * POST, PUT, and DELETE if necessary. The default implementations for this
	 * methods always throw an {@link UnsupportedOperationException}. If other
	 * request methods need to be supported the implementation may also 
	 * override
	 * the method {@link #handleMethod(HttpRequest)} which switches over the
	 * {@link HttpRequestMethod} and throws a HTTP status code exception on
	 * unsupported request methods.
	 *
	 * <p>The handling of request methods is intended to be stateless so that a
	 * single instance should be able to handle multiple requests. The state
	 * that is associated with the handling of a particular request is 
	 * stored in
	 * the associated {@link RequestHandler} instance and will be handed to the
	 * request method handler in the {@link HttpRequest} argument of the 
	 * methods
	 * below.</p>
	 *
	 * @author eso
	 */
	@FunctionalInterface
	public interface HttpRequestMethodHandler {

		/**
		 * Must be overridden to support the DELETE request method. The default
		 * implementation just throws an {@link HttpStatusException}.
		 *
		 * @param request The request to execute
		 * @return A {@link HttpResponse} object containing relations with the
		 * response data
		 * @throws HttpStatusException If executing the request fails
		 */
		default HttpResponse doDelete(HttpRequest request)
			throws HttpStatusException {
			throw new HttpStatusException(HttpStatusCode.NOT_IMPLEMENTED,
				"DELETE not supported");
		}

		/**
		 * This method must be always be implemented to provide the basic
		 * functionality in the form of GET requests.
		 *
		 * @param request The request to execute
		 * @return A {@link HttpResponse} object containing relations with the
		 * response data
		 * @throws HttpStatusException If executing the request fails
		 */
		HttpResponse doGet(HttpRequest request) throws HttpStatusException;

		/**
		 * Must be overridden to support the POST request method. The default
		 * implementation just throws an {@link HttpStatusException}.
		 *
		 * @param request The request to execute
		 * @return A {@link HttpResponse} object containing relations with the
		 * response data
		 * @throws HttpStatusException If executing the request fails
		 */
		default HttpResponse doPost(HttpRequest request)
			throws HttpStatusException {
			throw new HttpStatusException(HttpStatusCode.NOT_IMPLEMENTED,
				"POST not supported");
		}

		/**
		 * Must be overridden to support the PUT request method. The default
		 * implementation just throws an {@link HttpStatusException}.
		 *
		 * @param request The request to execute
		 * @return A {@link HttpResponse} object containing relations with the
		 * response data
		 * @throws HttpStatusException If executing the request fails
		 */
		default HttpResponse doPut(HttpRequest request)
			throws HttpStatusException {
			throw new HttpStatusException(HttpStatusCode.NOT_IMPLEMENTED,
				"PUT not supported");
		}

		/**
		 * Creates the response for the current request and returns a
		 * {@link Reader} that provides the data of the response body.
		 *
		 * @param request rRequest requestMethod The request method to create
		 *                the response for
		 * @return A {@link HttpResponse} object containing relations with the
		 * response data
		 * @throws IOException If reading or writing data fails
		 */
		default HttpResponse handleMethod(HttpRequest request)
			throws IOException {
			HttpResponse response = null;

			switch (request.getMethod()) {
				case GET:
					response = doGet(request);
					break;

				case DELETE:
					response = doDelete(request);
					break;

				case POST:
					response = doPost(request);
					break;

				case PUT:
					response = doPut(request);
					break;

				default:
					badRequest(
						"Unsupported request method: " + request.getMethod());
			}

			return response;
		}
	}
}
