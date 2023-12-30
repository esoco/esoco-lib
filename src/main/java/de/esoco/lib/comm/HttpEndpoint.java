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
package de.esoco.lib.comm;

import de.esoco.lib.comm.http.HttpRequestMethod;
import de.esoco.lib.comm.http.HttpStatusCode;
import de.esoco.lib.comm.http.HttpStatusException;
import de.esoco.lib.expression.Function;
import de.esoco.lib.expression.Functions;
import de.esoco.lib.io.LimitedInputStream;
import de.esoco.lib.io.LimitedOutputStream;
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.net.NetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static de.esoco.lib.comm.CommunicationRelationTypes.BUFFER_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_ADDRESS;
import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_REQUEST_HEADERS;
import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_RESPONSE_HEADERS;
import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_STATUS_CODE;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_REQUEST_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_RESPONSE_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.REQUEST_ENCODING;
import static de.esoco.lib.comm.CommunicationRelationTypes.RESPONSE_ENCODING;
import static de.esoco.lib.net.NetUtil.appendUrlPath;

/**
 * An endpoint that connects to an HTTP or HTTPS address and allows to perform
 * {@link HttpRequest HTTP requests}. After an HTTP request has been executed
 * the parameters {@link CommunicationRelationTypes#HTTP_STATUS_CODE} and
 * {@link CommunicationRelationTypes#HTTP_RESPONSE_HEADERS} on the
 * {@link Connection} object will contain the respective values as returned by
 * the endpoint.
 *
 * @author eso
 */
public class HttpEndpoint extends Endpoint {

	/**
	 * Returns a new HTTP GET method without a default target URL. The returned
	 * method must always be invoked with an explicit URL argument or else an
	 * exception will occur.
	 *
	 * @return The new communication method
	 */
	public static CommunicationMethod<String, String> httpGet() {
		return httpGet(null);
	}

	/**
	 * Returns a new method instance that performs a GET request by using the
	 * method input as the endpoint-relative target URL of the request.
	 *
	 * @param targetUrl The endpoint-relative default URL to be retrieved by
	 *                    the
	 *                  get request
	 * @return The new communication method
	 */
	public static HttpRequest<String, String> httpGet(String targetUrl) {
		return new HttpRequest<String, String>("HttpGet(%s)", targetUrl,
			HttpRequestMethod.GET, "", Functions.identity(),
			Functions.identity());
	}

	/**
	 * Returns a new method instance that performs a POST request by
	 * transmitting the method input to a certain URL of the target endpoint.
	 *
	 * @param targetUrl The endpoint-relative target URL for the POST request
	 * @param postData  The default data to be transmitted (the method input)
	 * @return The new communication method
	 */
	public static HttpRequest<String, String> httpPost(String targetUrl,
		String postData) {
		HttpRequest<String, String> postRequest =
			new HttpRequest<String, String>("HttpPost(%s)", postData,
				HttpRequestMethod.POST, targetUrl, Functions.identity(),
				Functions.identity());

		return postRequest;
	}

	/**
	 * Builds a HTTP endpoint URL from the given parameters.
	 *
	 * @param host      The host name or address
	 * @param port      The port to connect to
	 * @param encrypted TRUE for an encrypted connection
	 * @return The resulting endpoint URL
	 */
	@SuppressWarnings("boxing")
	public static String url(String host, int port, boolean encrypted) {
		String scheme = encrypted ? "https" : "http";
		String url;

		if (port > 0) {
			url = String.format("%s://%s:%d", scheme, host, port);
		} else {
			url = String.format("%s://%s", scheme, host);
		}

		return url;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection connection) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initConnection(Connection connection) {
	}

	/**
	 * Implementation of a communication method that performs a HTTP request.
	 * Can be sub-classed for more specific request implementations.
	 *
	 * @author eso
	 */
	public static class HttpRequest<I, O> extends CommunicationMethod<I, O> {

		private final HttpRequestMethod requestMethod;

		private final String baseUrl;

		private final Function<I, String> provideRequestData;

		private final Function<String, O> processResponse;

		private final Map<String, String> requestHeaders =
			new LinkedHashMap<String, String>();

		/**
		 * Creates a new HTTP request.
		 *
		 * @param methodName         The name of this method
		 * @param defaultInput       The default input value
		 * @param requestMethod      The HTTP request method
		 * @param baseUrl            The base URL for this request
		 * @param provideRequestData A function that derives the request
		 *                                 data to
		 *                           be transferred to the server from the
		 *                           method input
		 * @param processResponse    A function to be invoked to process the
		 *                                raw
		 *                           (text) response into the output format of
		 *                           this communication method
		 */
		public HttpRequest(String methodName, I defaultInput,
			HttpRequestMethod requestMethod, String baseUrl,
			Function<I, String> provideRequestData,
			Function<String, O> processResponse) {
			super(methodName, defaultInput);

			this.requestMethod = requestMethod;
			this.baseUrl = baseUrl;
			this.provideRequestData = provideRequestData;
			this.processResponse = processResponse;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("boxing")
		public O doOn(Connection connection, I input) {
			HttpURLConnection urlConnection =
				setupUrlConnection(connection, input);

			try {
				if (requestMethod.doesOutput()) {
					try (OutputStream outStream = new LimitedOutputStream(
						urlConnection.getOutputStream(),
						connection.get(MAX_REQUEST_SIZE))) {
						writeRequest(connection, outStream, input);
					}
				}

				try (InputStream inputStream = new LimitedInputStream(
					urlConnection.getInputStream(),
					connection.get(MAX_RESPONSE_SIZE))) {
					Reader inputReader = new InputStreamReader(inputStream,
						connection.get(RESPONSE_ENCODING));

					connection.set(HTTP_STATUS_CODE, HttpStatusCode.valueOf(
						urlConnection.getResponseCode()));
					connection.set(HTTP_RESPONSE_HEADERS,
						urlConnection.getHeaderFields());

					return readResponse(connection, inputReader);
				}
			} catch (Exception e) {
				int responseCode;

				try {
					responseCode = urlConnection.getResponseCode();
				} catch (IOException e2) {
					// continue with original exception
					throw new CommunicationException(e);
				}

				if (responseCode != -1) {
					return handleHttpError(urlConnection, e,
						HttpStatusCode.valueOf(responseCode));
				} else {
					throw new CommunicationException(e);
				}
			}
		}

		/**
		 * Returns the base URL of this request.
		 *
		 * @return The base URL
		 */
		public final String getBaseUrl() {
			return baseUrl;
		}

		/**
		 * Returns the function that provides the data to be sent with an HTTP
		 * request.
		 *
		 * @return The request data provider function or NULL for none
		 */
		public final Function<I, String> getRequestDataProvider() {
			return provideRequestData;
		}

		/**
		 * Returns the HTTP request method of this request.
		 *
		 * @return The HTTP request method
		 */
		public HttpRequestMethod getRequestMethod() {
			return requestMethod;
		}

		/**
		 * Returns the function that processes server responses.
		 *
		 * @return The response processor function
		 */
		public final Function<String, O> getResponseProcessor() {
			return processResponse;
		}

		/**
		 * Applies the request headers of this method and the given connection
		 * to the given URL connection.
		 *
		 * @param connection    The connection to apply the headers for
		 * @param urlConnection The HTTP URL connection to apply the headers to
		 */
		protected void applyRequestHeaders(Connection connection,
			HttpURLConnection urlConnection) {
			urlConnection.setRequestProperty("Accept-Charset",
				connection.get(REQUEST_ENCODING).name());

			for (Entry<String, String> header : requestHeaders.entrySet()) {
				urlConnection.setRequestProperty(header.getKey(),
					header.getValue());
			}

			if (connection.hasRelation(HTTP_REQUEST_HEADERS)) {
				for (Entry<String, List<String>> header : connection
					.get(HTTP_REQUEST_HEADERS)
					.entrySet()) {
					String headerName = header.getKey();
					List<String> headerValues = header.getValue();

					for (String value : headerValues) {
						urlConnection.setRequestProperty(headerName, value);
					}
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected String getMethodDescription(Connection connection, I input) {
			StringBuilder description = new StringBuilder("HTTP ");

			description.append(requestMethod).append(' ');
			description.append(getTargetUrl(connection, input));

			Map<String, String> headers = getRequestHeaders(connection);

			if (requestMethod.doesOutput()) {
				description.append("\nData: ");
				description.append(getRequestData(connection, input));
			}

			if (!headers.isEmpty()) {
				description.append("\nHeaders: ").append(headers);
			}

			return description.toString();
		}

		/**
		 * Retrieves the request data from the request data provider function.
		 *
		 * @param connection The current connection
		 * @param input      The method input to process with the request data
		 *                   provider
		 * @return The resulting request data
		 */
		protected String getRequestData(Connection connection, I input) {
			return provideRequestData.evaluate(input);
		}

		/**
		 * Returns an ordered map with all request headers of this instance and
		 * the given connection.
		 *
		 * @param connection The current connection
		 * @return The request header map
		 */
		protected Map<String, String> getRequestHeaders(Connection connection) {
			Map<String, String> headers = new LinkedHashMap<>(requestHeaders);

			if (connection.hasRelation(HTTP_REQUEST_HEADERS)) {
				for (Entry<String, List<String>> header : connection
					.get(HTTP_REQUEST_HEADERS)
					.entrySet()) {
					String headerName = header.getKey();
					List<String> headerValues = header.getValue();

					headers.put(headerName, headerValues.size() == 1 ?
					                        headerValues.get(0) :
					                        headerValues.toString());
				}
			}

			return headers;
		}

		/**
		 * Derives the target URL for a certain connection from an input value.
		 * For non-output requests (i.e. GET requests) the default
		 * implementation invokes {@link #getRequestData(Connection, Object)}
		 * with the input value and appends the result to the base URL.
		 * Subclasses can override this method to process the input value in
		 * different ways.
		 *
		 * @param connection The connection to return the target URL for
		 * @param input      The input value to derive the URL from
		 * @return The target URL for this instance
		 */
		protected String getTargetUrl(Connection connection, I input) {
			String endpointAddress =
				connection.getEndpoint().get(ENDPOINT_ADDRESS);

			StringBuilder urlBuilder = new StringBuilder(endpointAddress);

			appendUrlPath(urlBuilder, baseUrl);

			if (!requestMethod.doesOutput()) {
				String requestData = getRequestData(connection, input);

				appendUrlPath(urlBuilder, requestData);
			}

			return urlBuilder.toString();
		}

		/**
		 * Handles an HTTP error response that has been signaled by the URL
		 * connection with an exception. The default implementation always
		 * throws an {@link HttpStatusException} but subclasses can override
		 * this method for different error handling. If the method returns a
		 * value instead of throwing an exception the value will be returned as
		 * the regular response message of this request.
		 *
		 * @param urlConnection The URL connection that caused the error
		 * @param httpException The exception that occurred
		 * @param statusCode    responseThe response status code
		 * @return The request response if the error should be mapped to a
		 * regular response; else a runtime exception should be thrown
		 */
		protected O handleHttpError(HttpURLConnection urlConnection,
			Exception httpException, HttpStatusCode statusCode) {
			throw new HttpStatusException(statusCode, httpException);
		}

		/**
		 * Invokes the response processing function. Can be overridden by
		 * subclasses to extend or modify the processing.
		 *
		 * @param connection  The connection the response has been send over
		 * @param rawResponse The original response received from the endpoint
		 * @return The processed response
		 */
		protected O processResponse(Connection connection,
			String rawResponse) {
			return processResponse.evaluate(rawResponse);
		}

		/**
		 * Reads the response from a {@link Reader} on the connection input
		 * stream. The default implementation first reads all data from the
		 * stream (until EOF) and then returns the result of processing the raw
		 * data with the response processing function of this request instance
		 * (see {@link #getResponseProcessor()}). Subclasses can override this
		 * method if they need to handle the reading and/or processing
		 * differently.
		 *
		 * <p>The {@link Reader} argument must not be closed by this
		 * method.</p>
		 *
		 * @param connection  The connection the response has been send over
		 * @param inputReader The input reader
		 * @return The processed response
		 * @throws IOException If reading the response fails
		 */
		protected O readResponse(Connection connection, Reader inputReader)
			throws IOException {
			// the maximum response size is already limited by the stream
			@SuppressWarnings("boxing")
			String rawResponse =
				StreamUtil.readAll(inputReader, connection.get(BUFFER_SIZE),
					Integer.MAX_VALUE);

			return processResponse(connection, rawResponse);
		}

		/**
		 * Creates and initializes the URL connection used to communicate with
		 * the HTTP endpoint.
		 *
		 * @param connection The endpoint connection
		 * @param input      The input value for this communication method
		 * @return The URL connection
		 * @throws CommunicationException If the setup fails
		 */
		protected HttpURLConnection setupUrlConnection(Connection connection,
			I input) {
			try {
				String targetUrl = getTargetUrl(connection, input);

				HttpURLConnection urlConnection =
					(HttpURLConnection) new URL(targetUrl).openConnection();

				requestMethod.applyTo(urlConnection);
				applyRequestHeaders(connection, urlConnection);

				String userName = connection.getUserName();

				if (userName != null) {
					NetUtil.enableHttpBasicAuth(urlConnection, userName,
						connection.getPassword());
				}

				return urlConnection;
			} catch (Exception e) {
				throw new CommunicationException(e);
			}
		}

		/**
		 * If this HTTP request is configured to send additional request data
		 * this method will be invoked to send the data through the given
		 * output
		 * stream.
		 *
		 * @param connection   The current connection
		 * @param outputStream The stream to write the data to
		 * @param input        The method input
		 * @throws IOException If writing the request data fails
		 */
		protected void writeRequest(Connection connection,
			OutputStream outputStream, I input) throws IOException {
			String requestData = getRequestData(connection, input);

			if (requestData.length() > 0) {
				Charset encoding = connection.get(REQUEST_ENCODING);

				outputStream.write(requestData.getBytes(encoding));
				outputStream.flush();
			}
		}
	}
}
