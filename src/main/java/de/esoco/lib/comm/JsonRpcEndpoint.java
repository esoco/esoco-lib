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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.obrel.core.RelationType;

import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_REQUEST_HEADERS;

import static java.util.stream.Collectors.toList;

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
	 * Creates a new JSON RPC batch call that invokes several RPC methods at
	 * once.
	 *
	 * @param  rMethods The RPC methods to call
	 *
	 * @return The new JSON RPC batch request object
	 */
	public static <M extends JsonRpcMethod<?, R>, R> JsonRpcBatchCall<M, R>
	batchCall(Collection<M> rMethods)
	{
		return new JsonRpcBatchCall<>(rMethods);
	}

	/***************************************
	 * Creates a new JSON RPC batch call that invokes an RPC method multiple
	 * times with different call parameters.
	 *
	 * @param  rMethod        The RPC methods to call
	 * @param  rDefaultParams The default call parameters
	 *
	 * @return The new JSON RPC batch request object
	 */
	@SafeVarargs
	public static <P, R> JsonRpcBatchMethod<P, R> batchCall(
		JsonRpcMethod<P, R> rMethod,
		P... 				rDefaultParams)
	{
		return new JsonRpcBatchMethod<>(rMethod, Arrays.asList(rDefaultParams));
	}

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
	 * A batch invocation of multiple JSON RPC methods.
	 *
	 * @author eso
	 */
	public static class JsonRpcBatchCall<M extends JsonRpcMethod<?, ? extends R>, R>
		extends JsonRpcRequest<List<M>, List<R>>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rDefaultMethods The default methods to call
		 */
		public JsonRpcBatchCall(Collection<M> rDefaultMethods)
		{
			super(JsonRpcBatchCall.class.getSimpleName(),
				  new ArrayList<>(rDefaultMethods));
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public Collection<JsonObject> buildRequest(List<M> rMethods)
		{
			List<JsonObject> aMethodRequests = new ArrayList<>(rMethods.size());

			int nMethodId = 1;

			for (M rMethod : rMethods)
			{
				JsonObject aMethodRequest = rMethod.buildDefaultRequest();

				aMethodRequest.set("id", nMethodId++);
				aMethodRequests.add(aMethodRequest);
			}

			return aMethodRequests;
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected List<R> parseRawResponse(
			String  sRawResponse,
			List<M> rMethods)
		{
			// first parse response array, then sort by ID to match the correct
			// method index
			return Json.parseArray(sRawResponse, 1)
					   .stream()
					   .map(o -> Json.parseObject(o.toString(), 1))
					   .sorted((j1, j2) ->
							   j1.getInt("id", 0) - j2.getInt("id", 0))
					   .map(aResponse -> parseMethodResult(aResponse, rMethods))
					   .collect(toList());
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected List<R> parseResult(String sRawResponse)
		{
			return null; // not used, parsing occurs in parseRawResponse
		}

		/***************************************
		 * Parses a single method result from the batch response.
		 *
		 * @param  aResponse The method response
		 * @param  rMethods  The methods for the lookup by response ID
		 *
		 * @return The parsed method result
		 */
		private R parseMethodResult(JsonObject aResponse, List<M> rMethods)
		{
			int nId = aResponse.getInt("id", 0) - 1;

			if (nId >= 0 && nId < rMethods.size())
			{
				return rMethods.get(nId).parseResponse(aResponse);
			}
			else
			{
				throw new CommunicationException("No method to parse response ID" +
												 nId);
			}
		}
	}

	/********************************************************************
	 * A batch invocation of a single JSON RPC method with multiple call
	 * parameters.
	 *
	 * @author eso
	 */
	public static class JsonRpcBatchMethod<P, R>
		extends JsonRpcRequest<List<P>, List<R>>
	{
		//~ Instance fields ----------------------------------------------------

		private JsonRpcMethod<P, ? extends R> rRpcMethod;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rRpcMethod     The methods to call
		 * @param rDefaultInputs The default method inputs.
		 */
		public JsonRpcBatchMethod(
			JsonRpcMethod<P, R> rRpcMethod,
			List<P>				rDefaultInputs)
		{
			super(JsonRpcBatchMethod.class.getSimpleName(), rDefaultInputs);

			this.rRpcMethod = rRpcMethod;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public Collection<JsonObject> buildRequest(List<P> rInputs)
		{
			List<JsonObject> aMethodRequests = new ArrayList<>(rInputs.size());

			int nRequestId = 1;

			for (P rParams : rInputs)
			{
				JsonObject aMethodRequest = rRpcMethod.buildRequest(rParams);

				aMethodRequest.set("id", nRequestId++);
				aMethodRequests.add(aMethodRequest);
			}

			return aMethodRequests;
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected List<R> parseRawResponse(String sRawResponse, List<P> rInput)
		{
			return Json.parseArray(sRawResponse, 1)
					   .stream()
					   .map(o -> Json.parseObject(o.toString(), 1))
					   .map(aResponse -> rRpcMethod.parseResponse(aResponse))
					   .collect(Collectors.toList());
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected List<R> parseResult(String sRawResponse)
		{
			// not used
			return null;
		}
	}

	/********************************************************************
	 * A JSON RPC request that calls a certain RPC method.
	 *
	 * @author eso
	 */
	public static class JsonRpcMethod<P, R> extends JsonRpcRequest<P, R>
	{
		//~ Instance fields ----------------------------------------------------

		private String   sMethod;
		private Class<R> rResponseType;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sMethod        The name of the RPC method to invoke
		 * @param rDefaultParams Default parameters for the method invocation
		 * @param rResponseType  The target datatype to parse the response with
		 */
		public JsonRpcMethod(String   sMethod,
							 P		  rDefaultParams,
							 Class<R> rResponseType)
		{
			super(sMethod, rDefaultParams);

			this.sMethod	   = sMethod;
			this.rResponseType = rResponseType;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Builds the request for the default input of this instance. Needed for
		 * batch invocation.
		 *
		 * @return The JSON RPC request object
		 *
		 * @see    JsonRpcBatchCall
		 */
		public JsonObject buildDefaultRequest()
		{
			return buildRequest(getDefaultInput());
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public JsonObject buildRequest(P rInput)
		{
			JsonObject aRequest = new JsonObject();

			aRequest.set("jsonrpc", "2.0");
			aRequest.set("id", 1);
			aRequest.set("method", sMethod);

			Object rRequestParams = getRequestParams(rInput);

			if (rRequestParams != null)
			{
				aRequest.set("params", rRequestParams);
			}

			return aRequest;
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

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected R parseResult(String sJsonResult)
		{
			return Json.parse(sJsonResult, rResponseType);
		}
	}

	/********************************************************************
	 * The base class for communication methods that call a JSON RPC method over
	 * the actual transport endpoint.
	 *
	 * @author eso
	 */
	static abstract class JsonRpcRequest<P, R> extends CommunicationMethod<P, R>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sName          The name of the RPC method to invoke
		 * @param rDefaultParams Default parameters for the method invocation
		 */
		public JsonRpcRequest(String sName, P rDefaultParams)
		{
			super(sName, rDefaultParams);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Builds an object containing the JSON RPC request properties.
		 *
		 * @param  rInput The input value to create the request parameters from
		 *
		 * @return The JSON JPC request object
		 */
		public abstract Object buildRequest(P rInput);

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public R doOn(Connection rConnection, P rInput)
		{
			Connection rTransportConnection =
				rConnection.get(RPC_SERVER_CONNECTION);

			CommunicationMethod<String, String> rTransportMethod =
				rConnection.get(RPC_SERVER_METHOD);

			Object aRequest = buildRequest(rInput);

			String sRawResponse =
				rTransportMethod.evaluate(Json.toJson(aRequest),
										  rTransportConnection);

			return parseRawResponse(sRawResponse, rInput);
		}

		/***************************************
		 * Parses the result of a remote method call into the target datatype.
		 * The default implementation invokes {@link Json#parse(String, Class)}.
		 *
		 * @param  sJsonResult The raw JSON result field of the request
		 *
		 * @return The parsed value
		 */
		protected abstract R parseResult(String sJsonResult);

		/***************************************
		 * Parses the raw JSON response string.
		 *
		 * @param  sRawResponse The raw JSON response
		 * @param  rInput       The input value for which the response has been
		 *                      returned
		 *
		 * @return The parsed result
		 */
		protected R parseRawResponse(String sRawResponse, P rInput)
		{
			JsonObject aResponse = Json.parseObject(sRawResponse, 1);

			return parseResponse(aResponse);
		}

		/***************************************
		 * Parses the response to a method call and returns the raw result
		 * value.
		 *
		 * @param  rResponse The JSON response object received from the server
		 *
		 * @return The parsed result
		 *
		 * @throws CommunicationException If an error response has been received
		 */
		protected R parseResponse(JsonObject rResponse)
		{
			Object aRawError = rResponse.get("error");

			if (aRawError == null)
			{
				return parseResult(rResponse.get("result").toString());
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
	}
}
