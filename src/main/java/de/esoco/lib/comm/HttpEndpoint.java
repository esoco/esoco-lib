//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//		 http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.lib.comm;

import de.esoco.lib.expression.Function;
import de.esoco.lib.expression.Functions;
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.net.NetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static de.esoco.lib.comm.CommunicationRelationTypes.BUFFER_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_ADDRESS;
import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_ENCODING;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAXIMUM_RESPONSE_SIZE;


/********************************************************************
 * An endpoint to an HTTP address.
 *
 * @author eso
 */
public class HttpEndpoint extends Endpoint
{
	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns a new HTTP GET method without a default target URL. The returned
	 * method must always be invoked with an explicit URL argument or else a
	 * NullPointerException will occur.
	 *
	 * @return The new communication method
	 */
	public static CommunicationMethod<String, String> httpGet()
	{
		return httpGet(null);
	}

	/***************************************
	 * Returns a new method instance for an HTTP GET request.
	 *
	 * @param  sTargetUrl The relative URL to be retrieved by the get request
	 *
	 * @return The new communication method
	 */
	public static CommunicationMethod<String, String> httpGet(String sTargetUrl)
	{
		return new HttpGetRequest<String, String>("HttpGet(%s)",
												  sTargetUrl,
												  null,
												  Functions.<String>identity());
	}

	/***************************************
	 * Returns a new method instance for an HTTP POST request.
	 *
	 * @param  sTargetUrl The relative URL to be targeted by the post request
	 * @param  rParams    The POST parameters
	 *
	 * @return The new communication method
	 */
	public static CommunicationMethod<String, String> httpPost(
		String				sTargetUrl,
		Map<String, String> rParams)
	{
		return new HttpPostRequest<String, String>("HttpPost(%s)",
												   sTargetUrl,
												   rParams,
												   Functions
												   .<String>identity());
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
	 * Implementation of a communication method that performs a HTTP GET
	 * request. Can be sub-classed for more specific request implementations.
	 *
	 * @author eso
	 */
	public static class HttpGetRequest<I, O> extends CommunicationMethod<I, O>
	{
		//~ Instance fields ----------------------------------------------------

		private Function<String, O> fProcessResponse;

		private final Map<String, String> aHttpParams =
			new LinkedHashMap<String, String>();

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sMethodName      The name of this method
		 * @param rDefaultInput    The default input value
		 * @param rHttpParams      Optional HTTP parameters (NULL or empty for
		 *                         none)
		 * @param fProcessResponse A function to be invoked to process the raw
		 *                         (text) response into the output format of
		 *                         this communication method.
		 */
		public HttpGetRequest(String			  sMethodName,
							  I					  rDefaultInput,
							  Map<String, String> rHttpParams,
							  Function<String, O> fProcessResponse)
		{
			super(sMethodName, rDefaultInput);

			this.fProcessResponse = fProcessResponse;

			if (rHttpParams != null)
			{
				aHttpParams.putAll(rHttpParams);
			}
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("boxing")
		public O doOn(Connection rConnection, I rInput)
		{
			String sRawResponse = null;

			try
			{
				URLConnection aUrlConnection =
					setupUrlConnection(rConnection, rInput);

				try (InputStream rInputStream = aUrlConnection.getInputStream())
				{
					Reader aInput = new InputStreamReader(rInputStream);
					int    nMax   =
						rConnection.get(MAXIMUM_RESPONSE_SIZE).intValue();

					sRawResponse =
						StreamUtil.readAll(aInput,
										   rConnection.get(BUFFER_SIZE),
										   nMax);
				}
			}
			catch (Exception e)
			{
				throw new CommunicationException(e);
			}

			return processResponse(sRawResponse);
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
		 * Creates and initializes the URL connection used to communicate with
		 * the HTTP endpoint.
		 *
		 * @param  rConnection The endpoint connection
		 * @param  rInput      The input value for this communication method
		 *
		 * @return The URL connection
		 *
		 * @throws IOException If the setup fails
		 */
		public URLConnection setupUrlConnection(
			Connection rConnection,
			I		   rInput) throws IOException
		{
			String sTargetUrl = getTargetUrl(rConnection, rInput);

			URLConnection aUrlConnection =
				createUrlConnection(rConnection, sTargetUrl);

			String sUserName = rConnection.getUserName();

			if (sUserName != null)
			{
				NetUtil.enableHttpBasicAuth(aUrlConnection,
											sUserName,
											rConnection.getPassword());
			}

			return aUrlConnection;
		}

		/***************************************
		 * Helper method that builds the full URL for a certain endpoint
		 * request.
		 *
		 * @param  rConnection The connection to create the URL for
		 * @param  sBaseUrl    The endpoint-relative base URL to build upon
		 *
		 * @return The URL connection
		 *
		 * @throws IOException If initializing or opening the connection fails
		 */
		protected String createEndpointUrl(
			Connection rConnection,
			String	   sBaseUrl) throws IOException
		{
			String sParams = getUrlParameters(rConnection);

			StringBuilder aUrlBuilder =
				new StringBuilder(rConnection.getEndpoint()
								  .get(ENDPOINT_ADDRESS)
								  .toString());

			if (aUrlBuilder.charAt(aUrlBuilder.length() - 1) != '/' &&
				!sBaseUrl.startsWith("/"))
			{
				aUrlBuilder.append('/');
			}

			aUrlBuilder.append(sBaseUrl);

			if (sParams.length() > 0)
			{
				aUrlBuilder.append('?');
				aUrlBuilder.append(sParams);
			}

			return aUrlBuilder.toString();
		}

		/***************************************
		 * Creates a {@link URLConnection} to a certain target URL for the given
		 * endpoint connection.
		 *
		 * @param  rConnection The endpoint connection
		 * @param  sTargetUrl  The target URL to open the connection for
		 *
		 * @return The new {@link URLConnection}
		 *
		 * @throws IOException If opening the connection fails
		 */
		protected URLConnection createUrlConnection(
			Connection rConnection,
			String	   sTargetUrl) throws IOException
		{
			URL			  aUrl			 = new URL(sTargetUrl);
			URLConnection aUrlConnection = aUrl.openConnection();

			aUrlConnection.setRequestProperty("Accept-Charset",
											  rConnection.get(ENDPOINT_ENCODING));

			return aUrlConnection;
		}

		/***************************************
		 * Encodes the HTTP parameters of this instance into a string with the
		 * encoding in {@link CommunicationRelationTypes#ENDPOINT_ENCODING} as
		 * it is stored in the connection.
		 *
		 * @param  rConnection The connection for which the parameters shall be
		 *                     encoded
		 *
		 * @return The encoded parameters (may be empty but will never be NULL)
		 *
		 * @throws UnsupportedEncodingException If the encoding fails
		 */
		protected String encodeParameters(Connection rConnection)
			throws UnsupportedEncodingException
		{
			StringBuilder aParams = new StringBuilder();

			for (Entry<String, String> rParam :
				 getHttpParameters(rConnection).entrySet())
			{
				String sEncoding = rConnection.get(ENDPOINT_ENCODING);

				aParams.append(URLEncoder.encode(rParam.getKey(), sEncoding));
				aParams.append('=');
				aParams.append(URLEncoder.encode(rParam.getValue(), sEncoding));
				aParams.append('&');
			}

			int nLength = aParams.length();

			if (nLength > 0)
			{
				aParams.setLength(nLength - 1);
			}

			return aParams.toString();
		}

		/***************************************
		 * Returns the map of the HTTP parameters used by this instance when
		 * accessing the given connection.
		 *
		 * @param  rConnection The connection to return the parameters for or
		 *                     NULL to return the default HTTP parameters
		 *
		 * @return A mapping from parameter names to values (may be empty but
		 *         will never be NULL)
		 */
		protected Map<String, String> getHttpParameters(Connection rConnection)
		{
			return aHttpParams;
		}

		/***************************************
		 * Derives the target URL for a certain connection from an input value.
		 * The default implementation invokes rInput.toString() and forwards the
		 * result and the other parameters to {@link
		 * #createEndpointUrl(Connection, String, boolean)}. Subclasses can
		 * override this method to process the input value in different ways.
		 *
		 * @param  rConnection The connection to return the target URL for
		 * @param  rInput      The input value to derive the URL from
		 *
		 * @return The target URL for this instance
		 *
		 * @throws IOException
		 */
		protected String getTargetUrl(Connection rConnection, I rInput)
			throws IOException
		{
			return createEndpointUrl(rConnection, rInput.toString());
		}

		/***************************************
		 * Returns the encoded parameter string that needs to be appended to the
		 * request URL. Can be overridden by sublcasses to modify the default
		 * behavior.
		 *
		 * @param  rConnection The connection for which to encode the parameters
		 *
		 * @return The encoded string (must be empty for no parameter)
		 *
		 * @throws UnsupportedEncodingException If encoding the parameters fails
		 */
		protected String getUrlParameters(Connection rConnection)
			throws UnsupportedEncodingException
		{
			return encodeParameters(rConnection);
		}

		/***************************************
		 * Invokes the response processing function. Can be overridden by
		 * subclasses to extend or modify the processing.
		 *
		 * @param  sRawResponse The original response received from the endpoint
		 *
		 * @return The processed response
		 */
		protected O processResponse(String sRawResponse)
		{
			return fProcessResponse.evaluate(sRawResponse);
		}
	}

	/********************************************************************
	 * Implementation of a communication method that performs a HTTP POST
	 * request. Can be sub-classed for more specific request implementations.
	 *
	 * @author eso
	 */
	public static class HttpPostRequest<I, O> extends HttpGetRequest<I, O>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		public HttpPostRequest(String			   sMethodName,
							   I				   rDefaultInput,
							   Map<String, String> rHttpParams,
							   Function<String, O> fProcessResponse)
		{
			super(sMethodName, rDefaultInput, rHttpParams, fProcessResponse);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Overridden to initialize the connection for a POST request.
		 *
		 * @see HttpGetRequest#setupUrlConnection(Connection, Object)
		 */
		@Override
		public URLConnection setupUrlConnection(
			Connection rConnection,
			I		   rInput) throws IOException
		{
			URLConnection rUrlConnection =
				super.setupUrlConnection(rConnection, rInput);

			String sEncoding = rConnection.get(ENDPOINT_ENCODING);

			rUrlConnection.setDoOutput(true);
			rUrlConnection.setRequestProperty("Content-Type",
											  "application/x-www-form-urlencoded;charset=" +
											  sEncoding.toLowerCase());

			try (OutputStream rOutput = rUrlConnection.getOutputStream())
			{
				String sParams = encodeParameters(rConnection);

				if (sParams.length() > 0)
				{
					rOutput.write(sParams.getBytes(sEncoding));
				}
			}

			return rUrlConnection;
		}

		/***************************************
		 * Overridden to return an empty string because parameters are sent in
		 * the POST request.
		 *
		 * @see HttpGetRequest#getUrlParameters(Connection)
		 */
		@Override
		protected String getUrlParameters(Connection rConnection)
			throws UnsupportedEncodingException
		{
			return "";
		}
	}
}
