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


/********************************************************************
 * An endpoint that connects to an HTTP or HTTPS address and allows to perform
 * {@link HttpRequest HTTP requests}. After an HTTP request has been executed
 * the parameters {@link CommunicationRelationTypes#HTTP_STATUS_CODE} and {@link
 * CommunicationRelationTypes#HTTP_RESPONSE_HEADERS} on the {@link Connection}
 * object will contain the respective values as returned by the endpoint.
 *
 * @author eso
 */
public class HttpEndpoint extends Endpoint
{
	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns a new HTTP GET method without a default target URL. The returned
	 * method must always be invoked with an explicit URL argument or else an
	 * exception will occur.
	 *
	 * @return The new communication method
	 */
	public static CommunicationMethod<String, String> httpGet()
	{
		return httpGet(null);
	}

	/***************************************
	 * Returns a new method instance that performs a GET request by using the
	 * method input as the endpoint-relative target URL of the request.
	 *
	 * @param  sTargetUrl The endpoint-relative default URL to be retrieved by
	 *                    the get request
	 *
	 * @return The new communication method
	 */
	public static HttpRequest<String, String> httpGet(String sTargetUrl)
	{
		return new HttpRequest<String, String>("HttpGet(%s)",
											   sTargetUrl,
											   HttpRequestMethod.GET,
											   "",
											   Functions.identity(),
											   Functions.identity());
	}

	/***************************************
	 * Returns a new method instance that performs a POST request by
	 * transmitting the method input to a certain URL of the target endpoint.
	 *
	 * @param  sTargetUrl The endpoint-relative target URL for the POST request
	 * @param  sPostData  The default data to be transmitted (the method input)
	 *
	 * @return The new communication method
	 */
	public static HttpRequest<String, String> httpPost(
		String sTargetUrl,
		String sPostData)
	{
		HttpRequest<String, String> aPostRequest =
			new HttpRequest<String, String>("HttpPost(%s)",
											sPostData,
											HttpRequestMethod.POST,
											sTargetUrl,
											Functions.identity(),
											Functions.identity());

		return aPostRequest;
	}

	/***************************************
	 * Builds a HTTP endpoint URL from the given parameters.
	 *
	 * @param  sHost      The host name or address
	 * @param  nPort      The port to connect to
	 * @param  bEncrypted TRUE for an encrypted connection
	 *
	 * @return The resulting endpoint URL
	 */
	@SuppressWarnings("boxing")
	public static String url(String sHost, int nPort, boolean bEncrypted)
	{
		String sScheme = bEncrypted ? "https" : "http";
		String sUrl;

		if (nPort > 0)
		{
			sUrl = String.format("%s://%s:%d", sScheme, sHost, nPort);
		}
		else
		{
			sUrl = String.format("%s://%s", sScheme, sHost);
		}

		return sUrl;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection rConnection)
	{
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void initConnection(Connection rConnection)
	{
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * Implementation of a communication method that performs a HTTP request.
	 * Can be sub-classed for more specific request implementations.
	 *
	 * @author eso
	 */
	public static class HttpRequest<I, O> extends CommunicationMethod<I, O>
	{
		//~ Instance fields ----------------------------------------------------

		private final HttpRequestMethod eRequestMethod;
		private final String		    sBaseUrl;

		private final Function<I, String> fProvideRequestData;
		private final Function<String, O> fProcessResponse;

		private final Map<String, String> aRequestHeaders =
			new LinkedHashMap<String, String>();

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new HTTP request.
		 *
		 * @param sMethodName         The name of this method
		 * @param rDefaultInput       The default input value
		 * @param eRequestMethod      The HTTP request method
		 * @param sBaseUrl            The base URL for this request
		 * @param fProvideRequestData A function that derives the request data
		 *                            to be transferred to the server from the
		 *                            method input
		 * @param fProcessResponse    A function to be invoked to process the
		 *                            raw (text) response into the output format
		 *                            of this communication method
		 */
		public HttpRequest(String			   sMethodName,
						   I				   rDefaultInput,
						   HttpRequestMethod   eRequestMethod,
						   String			   sBaseUrl,
						   Function<I, String> fProvideRequestData,
						   Function<String, O> fProcessResponse)
		{
			super(sMethodName, rDefaultInput);

			this.eRequestMethod		 = eRequestMethod;
			this.sBaseUrl			 = sBaseUrl;
			this.fProvideRequestData = fProvideRequestData;
			this.fProcessResponse    = fProcessResponse;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("boxing")
		public O doOn(Connection rConnection, I rInput)
		{
			HttpURLConnection aUrlConnection =
				setupUrlConnection(rConnection, rInput);

			try
			{
				if (eRequestMethod.doesOutput())
				{
					try (OutputStream rOutStream =
						 new LimitedOutputStream(aUrlConnection
												 .getOutputStream(),
												 rConnection.get(MAX_REQUEST_SIZE)))
					{
						writeRequest(rConnection, rOutStream, rInput);
					}
				}

				try (InputStream rInputStream =
					 new LimitedInputStream(aUrlConnection.getInputStream(),
											rConnection.get(MAX_RESPONSE_SIZE)))
				{
					Reader aInputReader =
						new InputStreamReader(rInputStream,
											  rConnection.get(RESPONSE_ENCODING));

					rConnection.set(HTTP_STATUS_CODE,
									HttpStatusCode.valueOf(aUrlConnection
														   .getResponseCode()));
					rConnection.set(HTTP_RESPONSE_HEADERS,
									aUrlConnection.getHeaderFields());

					return readResponse(rConnection, aInputReader);
				}
			}
			catch (Exception e)
			{
				int nResponseCode;

				try
				{
					nResponseCode = aUrlConnection.getResponseCode();
				}
				catch (IOException e2)
				{
					// continue with original exception
					throw new CommunicationException(e);
				}

				if (nResponseCode != -1)
				{
					return handleHttpError(aUrlConnection,
										   e,
										   HttpStatusCode.valueOf(nResponseCode));
				}
				else
				{
					throw new CommunicationException(e);
				}
			}
		}

		/***************************************
		 * Returns the base URL of this request.
		 *
		 * @return The base URL
		 */
		public final String getBaseUrl()
		{
			return sBaseUrl;
		}

		/***************************************
		 * Returns the function that provides the data to be sent with an HTTP
		 * request.
		 *
		 * @return The request data provider function or NULL for none
		 */
		public final Function<I, String> getRequestDataProvider()
		{
			return fProvideRequestData;
		}

		/***************************************
		 * Returns the HTTP request method of this request.
		 *
		 * @return The HTTP request method
		 */
		public HttpRequestMethod getRequestMethod()
		{
			return eRequestMethod;
		}

		/***************************************
		 * Returns the function that processes server responses.
		 *
		 * @return The response processor function
		 */
		public final Function<String, O> getResponseProcessor()
		{
			return fProcessResponse;
		}

		/***************************************
		 * Applies the request headers of this method and the given connection
		 * to the given URL connection.
		 *
		 * @param rConnection    The connection to apply the headers for
		 * @param rUrlConnection The HTTP URL connection to apply the headers to
		 */
		protected void applyRequestHeaders(
			Connection		  rConnection,
			HttpURLConnection rUrlConnection)
		{
			rUrlConnection.setRequestProperty("Accept-Charset",
											  rConnection.get(REQUEST_ENCODING)
											  .name());

			for (Entry<String, String> rHeader : aRequestHeaders.entrySet())
			{
				rUrlConnection.setRequestProperty(rHeader.getKey(),
												  rHeader.getValue());
			}

			if (rConnection.hasRelation(HTTP_REQUEST_HEADERS))
			{
				for (Entry<String, List<String>> rHeader :
					 rConnection.get(HTTP_REQUEST_HEADERS).entrySet())
				{
					String		 sHeaderName   = rHeader.getKey();
					List<String> rHeaderValues = rHeader.getValue();

					for (String sValue : rHeaderValues)
					{
						rUrlConnection.setRequestProperty(sHeaderName, sValue);
					}
				}
			}
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected String getMethodDescription(Connection rConnection, I rInput)
		{
			StringBuilder aDescription =
				new StringBuilder(String.format("HTTP "));

			aDescription.append(eRequestMethod).append(' ');
			aDescription.append(getTargetUrl(rConnection, rInput));

			Map<String, String> rHeaders = getRequestHeaders(rConnection);

			if (eRequestMethod.doesOutput())
			{
				aDescription.append("\nData: ");
				aDescription.append(getRequestData(rConnection, rInput));
			}

			if (!rHeaders.isEmpty())
			{
				aDescription.append("\nHeaders: ").append(rHeaders);
			}

			return aDescription.toString();
		}

		/***************************************
		 * Retrieves the request data from the request data provider function.
		 *
		 * @param  rConnection The current connection
		 * @param  rInput      The method input to process with the request data
		 *                     provider
		 *
		 * @return The resulting request data
		 */
		protected String getRequestData(Connection rConnection, I rInput)
		{
			return fProvideRequestData.evaluate(rInput);
		}

		/***************************************
		 * Returns an ordered map with all request headers of this instance and
		 * the given connection.
		 *
		 * @param  rConnection The current connection
		 *
		 * @return The request header map
		 */
		protected Map<String, String> getRequestHeaders(Connection rConnection)
		{
			Map<String, String> aHeaders = new LinkedHashMap<>(aRequestHeaders);

			if (rConnection.hasRelation(HTTP_REQUEST_HEADERS))
			{
				for (Entry<String, List<String>> rHeader :
					 rConnection.get(HTTP_REQUEST_HEADERS).entrySet())
				{
					String		 sHeaderName   = rHeader.getKey();
					List<String> rHeaderValues = rHeader.getValue();

					aHeaders.put(sHeaderName,
								 rHeaderValues.size() == 1
								 ? rHeaderValues.get(0)
								 : rHeaderValues.toString());
				}
			}

			return aHeaders;
		}

		/***************************************
		 * Derives the target URL for a certain connection from an input value.
		 * For non-output requests (i.e. GET requests) the default
		 * implementation invokes {@link #getRequestData(Connection, Object)}
		 * with the input value and appends the result to the base URL.
		 * Subclasses can override this method to process the input value in
		 * different ways.
		 *
		 * @param  rConnection The connection to return the target URL for
		 * @param  rInput      The input value to derive the URL from
		 *
		 * @return The target URL for this instance
		 */
		protected String getTargetUrl(Connection rConnection, I rInput)
		{
			String sEndpointAddress =
				rConnection.getEndpoint().get(ENDPOINT_ADDRESS);

			StringBuilder aUrlBuilder = new StringBuilder(sEndpointAddress);

			appendUrlPath(aUrlBuilder, sBaseUrl);

			if (!eRequestMethod.doesOutput())
			{
				String sRequestData = getRequestData(rConnection, rInput);

				appendUrlPath(aUrlBuilder, sRequestData);
			}

			return aUrlBuilder.toString();
		}

		/***************************************
		 * Handles an HTTP error response that has been signaled by the URL
		 * connection with an exception. The default implementation always
		 * throws an {@link HttpStatusException} but subclasses can override
		 * this method for different error handling. If the method returns a
		 * value instead of throwing an exception the value will be returned as
		 * the regular response message of this request.
		 *
		 * @param  rUrlConnection The URL connection that caused the error
		 * @param  eHttpException The exception that occurred
		 * @param  eStatusCode    nResponseThe response status code
		 *
		 * @return The request response if the error should be mapped to a
		 *         regular response; else a runtime exception should be thrown
		 */
		protected O handleHttpError(HttpURLConnection rUrlConnection,
									Exception		  eHttpException,
									HttpStatusCode    eStatusCode)
		{
			throw new HttpStatusException(eStatusCode, eHttpException);
		}

		/***************************************
		 * Invokes the response processing function. Can be overridden by
		 * subclasses to extend or modify the processing.
		 *
		 * @param  rConnection  The connection the response has been send over
		 * @param  sRawResponse The original response received from the endpoint
		 *
		 * @return The processed response
		 */
		protected O processResponse(Connection rConnection, String sRawResponse)
		{
			return fProcessResponse.evaluate(sRawResponse);
		}

		/***************************************
		 * Reads the response from a {@link Reader} on the connection input
		 * stream. The default implementation first reads all data from the
		 * stream (until EOF) and then returns the result of processing the raw
		 * data with the response processing function of this request instance
		 * (see {@link #getResponseProcessor()}). Subclasses can override this
		 * method if they need to handle the reading and/or processing
		 * differently.
		 *
		 * <p>The {@link Reader} argument must not be closed by this method.</p>
		 *
		 * @param  rConnection  The connection the response has been send over
		 * @param  rInputReader The input reader
		 *
		 * @return The processed response
		 *
		 * @throws IOException If reading the response fails
		 */
		protected O readResponse(Connection rConnection, Reader rInputReader)
			throws IOException
		{
			// the maximum response size is already limited by the stream
			@SuppressWarnings("boxing")
			String sRawResponse =
				StreamUtil.readAll(rInputReader,
								   rConnection.get(BUFFER_SIZE),
								   Integer.MAX_VALUE);

			return processResponse(rConnection, sRawResponse);
		}

		/***************************************
		 * Creates and initializes the URL connection used to communicate with
		 * the HTTP endpoint.
		 *
		 * @param  rConnection The endpoint connection
		 * @param  rInput      The input value for this communication method
		 *
		 * @return The URL connection
		 *
		 * @throws CommunicationException If the setup fails
		 */
		protected HttpURLConnection setupUrlConnection(
			Connection rConnection,
			I		   rInput)
		{
			try
			{
				String sTargetUrl = getTargetUrl(rConnection, rInput);

				HttpURLConnection aUrlConnection =
					(HttpURLConnection) new URL(sTargetUrl).openConnection();

				eRequestMethod.applyTo(aUrlConnection);
				applyRequestHeaders(rConnection, aUrlConnection);

				String sUserName = rConnection.getUserName();

				if (sUserName != null)
				{
					NetUtil.enableHttpBasicAuth(aUrlConnection,
												sUserName,
												rConnection.getPassword());
				}

				return aUrlConnection;
			}
			catch (Exception e)
			{
				throw new CommunicationException(e);
			}
		}

		/***************************************
		 * If this HTTP request is configured to send additional request data
		 * this method will be invoked to send the data through the given output
		 * stream.
		 *
		 * @param  rConnection   The current connection
		 * @param  rOutputStream The stream to write the data to
		 * @param  rInput        The method input
		 *
		 * @throws IOException If writing the request data fails
		 */
		protected void writeRequest(Connection   rConnection,
									OutputStream rOutputStream,
									I			 rInput) throws IOException
		{
			String sRequestData = getRequestData(rConnection, rInput);

			if (sRequestData.length() > 0)
			{
				Charset rEncoding = rConnection.get(REQUEST_ENCODING);

				rOutputStream.write(sRequestData.getBytes(rEncoding));
				rOutputStream.flush();
			}
		}
	}
}
