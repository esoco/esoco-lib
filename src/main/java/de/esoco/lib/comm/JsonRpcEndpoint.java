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

import de.esoco.lib.json.Json;
import de.esoco.lib.json.JsonObject;
import de.esoco.lib.json.JsonParser;

import java.net.URI;
import java.net.URL;

import java.util.Arrays;

import org.obrel.core.RelationType;

import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_REQUEST_HEADERS;

import static org.obrel.core.RelationTypeModifier.PRIVATE;
import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * An endpoint that communicates via JSON RPC with a wrapped, transport-specific
 * endpoint. In the current implementation only the HTTP(S) transport protocol
 * is supported.
 *
 * @author eso
 */
public class JsonRpcEndpoint extends Endpoint
{
	//~ Static fields/initializers ---------------------------------------------

	private static final RelationType<Connection> RPC_SERVER_CONNECTION =
		newType(PRIVATE);

	private static final RelationType<CommunicationMethod<String, String>> RPC_SERVER_METHOD =
		newType(PRIVATE);

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 */
	public JsonRpcEndpoint()
	{
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns a new {@link JsonRpcMethod} for a generic RPC invocation.
	 *
	 * @param  sMethod The name of the method to invoke
	 *
	 * @return The new method
	 */
	public static JsonRpcMethod<Object, Object> method(String sMethod)
	{
		return method(sMethod, null, null);
	}

	/***************************************
	 * Returns a new {@link JsonRpcMethod} with a generic parameters argument
	 * and a specific result datatype.
	 *
	 * @param  sMethod     The name of the method to invoke
	 * @param  rResultType The datatype to parse the result field of responses
	 *                     with
	 *
	 * @return The new method
	 */
	public static <R> JsonRpcMethod<Object, R> method(
		String   sMethod,
		Class<R> rResultType)
	{
		return method(sMethod, null, rResultType);
	}

	/***************************************
	 * Returns a new {@link JsonRpcMethod} with specific datatypes for the
	 * method parameters and the call result.
	 *
	 * @param  sMethod        The name of the method to invoke
	 * @param  rDefaultParams Default parameters for the call (can be NULL)
	 * @param  rResultType    The datatype to parse the result field of
	 *                        responses with
	 *
	 * @return The new method
	 */
	public static <P, R> JsonRpcMethod<P, R> method(String   sMethod,
													P		 rDefaultParams,
													Class<R> rResultType)
	{
		return new JsonRpcMethod<P, R>(sMethod, rDefaultParams, rResultType);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection rConnection) throws Exception
	{
		rConnection.get(RPC_SERVER_CONNECTION).close();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void initConnection(Connection rConnection) throws Exception
	{
		URI rUri	   = rConnection.getUri();
		URL aTargetUrl;

		CommunicationMethod<String, String> rTransportMethod;

		if (rUri.getScheme().equals("json-rpc"))
		{
			aTargetUrl = new URL(rUri.getSchemeSpecificPart());
		}
		else
		{
			aTargetUrl = rUri.toURL();
		}

		if (aTargetUrl.getProtocol().startsWith("http"))
		{
			rTransportMethod =
				HttpEndpoint.httpPost(aTargetUrl.getPath(), null);
		}
		else
		{
			throw new CommunicationException("Unsupported JSON RPC transport: " +
											 aTargetUrl);
		}

		Connection rTransportConnection =
			Endpoint.at(HttpEndpoint.url(aTargetUrl.getHost(),
										 aTargetUrl.getPort(),
										 aTargetUrl.getProtocol()
										 .endsWith("s"))).connect(rConnection);

		rConnection.set(RPC_SERVER_CONNECTION, rTransportConnection);
		rConnection.set(RPC_SERVER_METHOD, rTransportMethod);

		rTransportConnection.get(HTTP_REQUEST_HEADERS)
							.put("Content-Type",
								 Arrays.asList("application/json"));
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A communication method that invokes a JSON RPC method over the actual
	 * transport endpoint method.
	 *
	 * @author eso
	 */
	public static class JsonRpcMethod<P, R> extends CommunicationMethod<P, R>
	{
		//~ Instance fields ----------------------------------------------------

		private String   sMethod;
		private Class<R> rResponseType;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sMethod        The name of the RPC method to invoke
		 * @param rDefaultParams The default parameters to invoke the method
		 *                       with
		 * @param rResponseType  The datatype of the response
		 */
		public JsonRpcMethod(String   sMethod,
							 P		  rDefaultParams,
							 Class<R> rResponseType)
		{
			super(JsonRpcMethod.class.getSimpleName(), rDefaultParams);

			this.sMethod	   = sMethod;
			this.rResponseType = rResponseType;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("unchecked")
		public R doOn(Connection rConnection, P rInput)
		{
			Connection rTransportConnection =
				rConnection.get(RPC_SERVER_CONNECTION);

			CommunicationMethod<String, String> rTransportMethod =
				rConnection.get(RPC_SERVER_METHOD);

			JsonObject aRequest = new JsonObject();

			aRequest.set("jsonrpc", "2.0");
			aRequest.set("id", 1);
			aRequest.set("method", sMethod);

			Object rRequestParams = getRequestParams(rInput);

			if (rRequestParams != null)
			{
				aRequest.set("params", rRequestParams);
			}

			String sRawResponse =
				rTransportMethod.evaluate(aRequest.toJson(),
										  rTransportConnection);

			JsonObject aResponse = Json.parseObject(sRawResponse, 1);

			Object aRawError = aResponse.get("error");

			if (aRawError == null)
			{
				String sJsonResult = aResponse.get("result").toString();
				R	   aResult;

				if (rResponseType != null)
				{
					aResult = Json.parse(sJsonResult, rResponseType);
				}
				else
				{
					aResult = (R) Json.parse(sJsonResult);
				}

				return aResult;
			}
			else
			{
				JsonObject aError =
					new JsonParser().parseObject(aRawError.toString());

				throw new CommunicationException(String.format("JSON RPC Error %s: %s",
															   aError.get("code"),
															   aError.get("message")));
			}
		}

		/***************************************
		 * Returns the value for the "params" property of a request. The default
		 * implementation just returns the input value. Subclasses can override
		 * this to extend or modify the method input.
		 *
		 * @param  rInput The method input
		 *
		 * @return The actual request params
		 */
		protected Object getRequestParams(P rInput)
		{
			return rInput;
		}
	}
}
