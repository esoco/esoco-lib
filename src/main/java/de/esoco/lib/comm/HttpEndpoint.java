//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.io.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	 * Sets the http basic auth.
	 *
	 * @param rUrlConnection The new http basic auth
	 * @param sUserName      The new http basic auth
	 * @param sPassword      The new http basic auth
	 */
	public static void enableHttpBasicAuth(URLConnection rUrlConnection,
										   String		 sUserName,
										   String		 sPassword)
	{
		String sAuth = sUserName + ":" + sPassword;

		sAuth = Base64.getEncoder().encodeToString(sAuth.getBytes());

		rUrlConnection.setRequestProperty("Authorization", "Basic " + sAuth);
	}

	/***************************************
	 * Returns a new HTTP GET method without a preset target URL.
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
		return new GenericHttpRequest(sTargetUrl, null, false);
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
		return new GenericHttpRequest(sTargetUrl, rParams, true);
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
	 * An abstract base implementation for communication methods that implement
	 * HTTP requests.
	 *
	 * @author eso
	 */
	public static abstract class AbstractHttpRequest<I, O>
		extends CommunicationMethod<I, O>
	{
		//~ Instance fields ----------------------------------------------------

		private boolean bIsPostRequest;

		private final Map<String, String> aHttpParams =
			new LinkedHashMap<String, String>();

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sMethodName    The name of this method
		 * @param rDefaultInput  The default input value
		 * @param rHttpParams    Optional HTTP parameters or NULL for none
		 * @param bIsPostRequest TRUE for a POST request, FALSE for GET
		 */
		public AbstractHttpRequest(String			   sMethodName,
								   I				   rDefaultInput,
								   Map<String, String> rHttpParams,
								   boolean			   bIsPostRequest)
		{
			super(sMethodName, rDefaultInput);

			this.bIsPostRequest = bIsPostRequest;

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
		public O doOn(Connection rConnection, I rInput)
		{
			String sRawResponse = null;

			try
			{
				String sTargetUrl =
					getTargetUrl(rConnection, rInput, !bIsPostRequest);

				URLConnection aUrlConnection =
					createUrlConnection(rConnection, sTargetUrl);

				String sEncoding = rConnection.get(ENDPOINT_ENCODING);
				String sUserName = rConnection.getUserName();

				if (sUserName != null)
				{
					enableHttpBasicAuth(aUrlConnection,
										sUserName,
										rConnection.getPassword());
				}

				if (bIsPostRequest)
				{
					aUrlConnection.setDoOutput(true);
					aUrlConnection.setRequestProperty("Content-Type",
													  "application/x-www-form-urlencoded;charset=" +
													  sEncoding.toLowerCase());

					try (OutputStream rOutput = aUrlConnection.getOutputStream())
					{
						String sParams = encodeParameters(rConnection);

						if (sParams.length() > 0)
						{
							rOutput.write(sParams.getBytes(sEncoding));
						}
					}
				}

				try (InputStream rInputStream = aUrlConnection.getInputStream())
				{
					Reader aInput = new InputStreamReader(rInputStream);
					int    nMax   =
						rConnection.get(MAXIMUM_RESPONSE_SIZE).intValue();

					sRawResponse = StreamUtil.readAll(aInput, 1024 * 8, nMax);
				}
			}
			catch (Exception e)
			{
				throw new CommunicationException(e);
			}

			return processResponse(rConnection, sRawResponse);
		}

		/***************************************
		 * Returns the map of the HTTP parameters used by this instance when
		 * accessing the given connection.
		 *
		 * @param  rConnection The connection to return the parameters for
		 *
		 * @return A mapping from parameter names to values (may be empty but
		 *         will never be NULL)
		 */
		public final Map<String, String> getParameters(Connection rConnection)
		{
			return aHttpParams;
		}

		/***************************************
		 * Must be implemented by subclasses to process the response received
		 * from the endpoint into an implementation-specific output value.
		 *
		 * @param  rConnection  The connection to process the response for
		 * @param  sRawResponse The raw response string to process
		 *
		 * @return The processed output value
		 */
		protected abstract O processResponse(
			Connection rConnection,
			String	   sRawResponse);

		/***************************************
		 * Helper method that builds the full URL for a certain endpoint
		 * request.
		 *
		 * @param  rConnection   The connection to create the URL for
		 * @param  sBaseUrl      The endpoint-relative base URL to build upon
		 * @param  bAppendParams TRUE to append the connection parameters to the
		 *                       URL, FALSE to leave the target URL unchanged
		 *
		 * @return The URL connection
		 *
		 * @throws IOException If initializing or opening the connection fails
		 */
		protected String createEndpointUrl(Connection rConnection,
										   String	  sBaseUrl,
										   boolean    bAppendParams)
			throws IOException
		{
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

			if (bAppendParams)
			{
				String sParams = encodeParameters(rConnection);

				if (sParams.length() > 0)
				{
					aUrlBuilder.append('?');
					aUrlBuilder.append(sParams);
				}
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
				 getParameters(rConnection).entrySet())
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
		 * Derives the target URL for a certain connection from an input value.
		 * The default implementation invokes rInput.toString() and forwards the
		 * result and the other parameters to {@link
		 * #createEndpointUrl(Connection, String, boolean)}. Subclasses can
		 * override this method to process the input value in different ways.
		 *
		 * @param  rConnection   The connection to return the target URL for
		 * @param  rInput        The input value to derive the URL from
		 * @param  bAppendParams
		 *
		 * @return The target URL for this instance
		 *
		 * @throws IOException
		 */
		protected String getTargetUrl(Connection rConnection,
									  I			 rInput,
									  boolean    bAppendParams)
			throws IOException
		{
			return createEndpointUrl(rConnection,
									 rInput.toString(),
									 bAppendParams);
		}

		/***************************************
		 * A method for subclasses to set an HTTP parameter for this method.
		 *
		 * @param sKey   The parameter key
		 * @param sValue The parameter value
		 */
		protected void setParameter(String sKey, String sValue)
		{
			aHttpParams.put(sKey, sValue);
		}
	}

	/********************************************************************
	 * A generic implementation of HTTP requests that uses strings as input and
	 * output values.
	 *
	 * @author eso
	 */
	public static class GenericHttpRequest
		extends AbstractHttpRequest<String, String>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Subclass constructor that accepts a method name.
		 *
		 * @param sTargetUrl     The connection-relative target URL to retrieve
		 * @param rParams        The request parameters
		 * @param bIsPostRequest sMethodName The name of the method
		 */
		protected GenericHttpRequest(String				 sTargetUrl,
									 Map<String, String> rParams,
									 boolean			 bIsPostRequest)
		{
			super(bIsPostRequest ? "HttpPost(%s)" : "HttpGet(%s)",
				  sTargetUrl,
				  rParams,
				  bIsPostRequest);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Simply return the original response string.
		 *
		 * @see AbstractHttpRequest#processResponse(Connection, String)
		 */
		@Override
		protected String processResponse(
			Connection rConnection,
			String	   sResponse)
		{
			return sResponse;
		}
	}
}
