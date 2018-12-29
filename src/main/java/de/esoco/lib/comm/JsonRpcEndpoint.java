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

import de.esoco.lib.expression.Functions;
import de.esoco.lib.json.Json;
import de.esoco.lib.json.JsonObject;
import de.esoco.lib.json.JsonParser;

import java.net.URI;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	/** A generic relation type to store a JSON RPC batch call. */
	public static final RelationType<JsonRpcBatchCall> RPC_BATCH_CALL =
		newType();

	private static final RelationType<Connection> RPC_SERVER_CONNECTION =
		newType(PRIVATE);

	private static final RelationType<CommunicationMethod<String, String>> RPC_SERVER_METHOD =
		newType(PRIVATE);

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Creates a new JSON RPC batch call that can invoke multiple RPC methods at
	 * once. New methods must be added to the batch call object by invoking one
	 * of it's methods to add a request.
	 *
	 * @return The new JSON RPC batch request object
	 */
	public static JsonRpcBatchCall batchCall()
	{
		return new JsonRpcBatchCall();
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
	public static JsonRpcMethod<Object, Object> call(String sMethod)
	{
		return call(sMethod, null, sJson -> Json.parse(sJson));
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
	public static <R> JsonRpcMethod<Object, R> call(
		String   sMethod,
		Class<R> rResultType)
	{
		return call(sMethod, null, rResultType);
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
	public static <P, R> JsonRpcMethod<P, R> call(String   sMethod,
												  P		   rDefaultParams,
												  Class<R> rResultType)
	{
		return new JsonRpcMethod<P, R>(sMethod, rDefaultParams, rResultType);
	}

	/***************************************
	 * Returns a new {@link JsonRpcMethod} with specific datatypes for the
	 * method parameters and a parse function for the response.
	 *
	 * @param  sMethod        The name of the method to invoke
	 * @param  rDefaultParams Default parameters for the call (can be NULL)
	 * @param  fParseResponse A function that parses the JSON response string
	 *                        into the result type
	 *
	 * @return The new method
	 */
	public static <P, R> JsonRpcMethod<P, R> call(
		String				sMethod,
		P					rDefaultParams,
		Function<String, R> fParseResponse)
	{
		return new JsonRpcMethod<P, R>(sMethod, rDefaultParams, fParseResponse);
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
		URI    rUri		  = rConnection.getUri();
		String sTargetUrl = rUri.getSchemeSpecificPart();

		Endpoint						    rTransportEndpoint;
		CommunicationMethod<String, String> rTransportMethod;

		if (sTargetUrl.startsWith("http"))
		{
			URL aUrl = new URL(sTargetUrl);

			// create endpoint from URL without path
			rTransportEndpoint =
				Endpoint.at(
					HttpEndpoint.url(
						aUrl.getHost(),
						aUrl.getPort(),
						aUrl.getProtocol().endsWith("s")));
			rTransportMethod   = HttpEndpoint.httpPost(aUrl.getPath(), null);
		}
		else if (sTargetUrl.startsWith("pipe"))
		{
			rTransportEndpoint = Endpoint.at(sTargetUrl);
			rTransportMethod   = PipeEndpoint.textRequest(null);
		}
		else
		{
			throw new CommunicationException(
				"Unsupported JSON RPC transport: " +
				sTargetUrl);
		}

		Connection rTransportConnection =
			rTransportEndpoint.connect(rConnection);

		rConnection.set(RPC_SERVER_CONNECTION, rTransportConnection);
		rConnection.set(RPC_SERVER_METHOD, rTransportMethod);

		rTransportConnection.get(HTTP_REQUEST_HEADERS)
							.put(
								"Content-Type",
								Arrays.asList("application/json"));
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * The base class for for JSON RPC requests that execute batch calls.
	 *
	 * @author eso
	 */
	public static abstract class JsonRpcBatch<P, R>
		extends JsonRpcRequest<List<P>, List<R>>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sName          The request name
		 * @param rDefaultParams The default parameters
		 */
		public JsonRpcBatch(String sName, Collection<P> rDefaultParams)
		{
			super(sName, new ArrayList<>(rDefaultParams));
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Resets this batch by removing all default input values so that new
		 * inputs can be added and the batch can be re-executed.
		 */
		public void reset()
		{
			getDefaultInput().clear();
		}

		/***************************************
		 * Returns the number of requests that have been added to this batch
		 * call.
		 *
		 * @return The number of batch requests
		 */
		public int size()
		{
			return getDefaultInput().size();
		}

		/***************************************
		 * Parses the response for a single method invocation. Must be
		 * implemented by subclasses.
		 *
		 * @param  aResponse The JSON RPC response object for the method
		 * @param  rInputs   The method inputs
		 *
		 * @return The parsed method response
		 */
		protected abstract R parseMethodResponse(
			JsonObject aResponse,
			List<P>    rInputs);

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected List<R> parseRawResponse(String  sRawResponse,
										   List<P> rInputs)
		{
			return Json.parseArray(sRawResponse, 1)
					   .stream()
					   .map(o -> Json.parseObject(o.toString(), 1))
					   .sorted(
		   				(j1, j2) ->
		   					j1.getInt("id", 0) - j2.getInt("id", 0))
					   .map(
		   				aResponse ->
		   					parseMethodResponse(aResponse, rInputs))
					   .collect(Collectors.toList());
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected List<R> parseResult(String sRawResponse)
		{
			// not used for batch calls
			return null;
		}
	}

	/********************************************************************
	 * The base class for communication methods that call a JSON RPC method over
	 * the actual transport endpoint.
	 *
	 * @author eso
	 */
	public static abstract class JsonRpcRequest<P, R>
		extends CommunicationMethod<P, R>
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
		 * @param  nId    The request ID
		 *
		 * @return The JSON JPC request object
		 */
		public abstract Object buildRequest(P rInput, int nId);

		/***************************************
		 * Builds the request for the default input of this instance. Used for
		 * batch invocation.
		 *
		 * @param  nId The request ID
		 *
		 * @return The JSON RPC request object
		 */
		public Object buildDefaultRequest(int nId)
		{
			return buildRequest(getDefaultInput(), nId);
		}

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

			Object aRequest = buildRequest(rInput, 1);

			String sRawResponse =
				rTransportMethod.evaluate(
					Json.toCompactJson(aRequest),
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

				throw new CommunicationException(
					String.format(
						"JSON RPC Error %s: %s",
						aError.get("code"),
						aError.get("message")));
			}
		}
	}

	/********************************************************************
	 * A batch invocation of JSON RPC methods that dispatches the responses to
	 * different consumers for each method call. A batch invocation will also
	 * yield a result of type List &lt;Object&gt; containing the results of the
	 * single method calls in the order in which they have been added through
	 * the add method.
	 *
	 * @author eso
	 */
	public static class JsonRpcBatchCall extends JsonRpcBatch<Call<?>, Object>
	{
		//~ Instance fields ----------------------------------------------------

		private int nFirstId;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance without default calls. The calls must either
		 * be provided on evaluation or by adding default calls through
		 * invocation of the add() method.
		 */
		public JsonRpcBatchCall()
		{
			super(
				JsonRpcBatchCall.class.getSimpleName(),
				Collections.emptyList());
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public Object buildRequest(List<Call<?>> rCalls, int nId)
		{
			nFirstId = nId;

			List<Object> aCallRequests = new ArrayList<>(rCalls.size());

			for (Call<?> rCall : rCalls)
			{
				Object aRequest = rCall.rRequest.buildDefaultRequest(nId);

				if (aRequest instanceof JsonObject)
				{
					aCallRequests.add(aRequest);
					nId += 1;
				}
				else if (aRequest instanceof Collection)
				{
					Collection<?> rBatchRequests = (Collection<?>) aRequest;

					aCallRequests.addAll(rBatchRequests);
					nId += rBatchRequests.size();
				}
				else
				{
					throw new IllegalArgumentException(
						"Unsupported batch " +
						"request data from " + rCall.rRequest);
				}
			}

			return aCallRequests;
		}

		/***************************************
		 * Adds a new batch call to this instance that invokes a function to
		 * process the result upon receiving. The return value of the function
		 * will be available in the result of the batch call evaluation.
		 *
		 * @param  rRequest         The JSON RPC request to be invoked
		 * @param  fResponseHandler A function that handles the response
		 *                          received by the call
		 *
		 * @return This instance for call concatenation
		 */
		public <R> JsonRpcBatchCall call(
			JsonRpcRequest<?, R> rRequest,
			Consumer<? super R>  fResponseHandler)
		{
			Call<R> aCall = new Call<>(rRequest, fResponseHandler);

			getDefaultInput().add(aCall);

			return this;
		}

		/***************************************
		 * Checks if this batch contains no calls.
		 *
		 * @return TRUE if no calls have been added
		 */
		public boolean isEmpty()
		{
			return getDefaultInput().isEmpty();
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public String toString()
		{
			StringBuilder aResult =
				new StringBuilder(getClass().getSimpleName());

			int nId = 1;

			for (Call<?> rCall : getDefaultInput())
			{
				aResult.append("\n");
				aResult.append(rCall.rRequest.buildDefaultRequest(nId++));
			}

			return aResult.toString();
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected Object parseMethodResponse(
			JsonObject    aResponse,
			List<Call<?>> rCalls)
		{
			int nId = aResponse.getInt("id", 0) - nFirstId;

			if (nId >= 0 && nId < rCalls.size())
			{
				Call<?> rCall = rCalls.get(nId);

				return rCall.processResponse(aResponse);
			}
			else
			{
				throw new CommunicationException(
					"No method to parse response ID" +
					nId);
			}
		}
	}

	/********************************************************************
	 * A JSON RPC batch call that invokes a single method multiple times with
	 * different parameters.
	 *
	 * @author eso
	 */
	public static class JsonRpcBatchMethod<P, R> extends JsonRpcBatch<P, R>
	{
		//~ Instance fields ----------------------------------------------------

		private JsonRpcMethod<P, ? extends R> rRpcMethod;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rRpcMethod     The methods to call
		 * @param rDefaultInputs The default method input
		 */
		public JsonRpcBatchMethod(
			JsonRpcMethod<P, R> rRpcMethod,
			Collection<P>		rDefaultInputs)
		{
			super(JsonRpcBatchMethod.class.getSimpleName(), rDefaultInputs);

			this.rRpcMethod = rRpcMethod;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Adds a new method parameter to be requested by this instance.
		 *
		 * @param rCallParam rRequest The JSON RPC request
		 */
		public void add(P rCallParam)
		{
			getDefaultInput().add(rCallParam);
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public Collection<JsonObject> buildRequest(List<P> rInputs, int nId)
		{
			List<JsonObject> aMethodRequests = new ArrayList<>(rInputs.size());

			for (P rParams : rInputs)
			{
				aMethodRequests.add(rRpcMethod.buildRequest(rParams, nId++));
			}

			return aMethodRequests;
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public String toString()
		{
			return getClass().getSimpleName() + getDefaultInput();
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected R parseMethodResponse(JsonObject aResponse, List<P> rInput)
		{
			return rRpcMethod.parseResponse(aResponse);
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

		private final String				 sMethod;
		private final Function<String, R>    fParseResponse;
		private final Function<? super P, ?> fConvertInput;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance that uses input values directly as RPC call
		 * parameters.
		 *
		 * @param sMethod        The name of the RPC method to invoke
		 * @param rDefaultParams Default parameters for the method invocation
		 * @param rResponseType  The target datatype to parse the response with
		 */
		public JsonRpcMethod(String   sMethod,
							 P		  rDefaultParams,
							 Class<R> rResponseType)
		{
			this(
				sMethod,
				rDefaultParams,
				Functions.identity(),
				JsonParser.parseJson(rResponseType));
		}

		/***************************************
		 * Creates a new instance that uses input values directly as RPC call
		 * parameters.
		 *
		 * @param sMethod        The name of the RPC method to invoke
		 * @param rDefaultParams Default parameters for the method invocation
		 * @param fParseResponse A function that parses the JSON response string
		 *                       into the result type
		 */
		public JsonRpcMethod(String				 sMethod,
							 P					 rDefaultParams,
							 Function<String, R> fParseResponse)
		{
			this(sMethod, rDefaultParams, Functions.identity(), fParseResponse);
		}

		/***************************************
		 * Creates a new instance that converts input values into the actual
		 * input value before using them as RPC call parameters.
		 *
		 * @param sMethod        The name of the RPC method to invoke
		 * @param rDefaultParams Default parameters for the method invocation
		 * @param fConvertInput  A function that converts input parameters to
		 *                       the actual value to be used in the request
		 * @param fParseResponse A function that parses the JSON response string
		 *                       into the result type
		 */
		public JsonRpcMethod(String					sMethod,
							 P						rDefaultParams,
							 Function<? super P, ?> fConvertInput,
							 Function<String, R>    fParseResponse)
		{
			super(sMethod, rDefaultParams);

			this.sMethod	    = sMethod;
			this.fParseResponse = fParseResponse;
			this.fConvertInput  = fConvertInput;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public JsonObject buildRequest(P rInput, int nId)
		{
			JsonObject aRequest = new JsonObject();

			aRequest.set("jsonrpc", "2.0");
			aRequest.set("id", nId);
			aRequest.set("method", sMethod);

			Object rRequestParams = getRequestParams(rInput);

			if (rRequestParams != null)
			{
				aRequest.set("params", rRequestParams);
			}

			return aRequest;
		}

		/***************************************
		 * Returns the function that is used to convert input values. If not set
		 * otherwise the default is an identity function that returns the input
		 * value unchanged.
		 *
		 * @return The input conversion function
		 */
		public Function<? super P, ?> getInputConversion()
		{
			return fConvertInput;
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
			return fConvertInput.apply(rInput);
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected R parseResult(String sJsonResult)
		{
			return fParseResponse.apply(sJsonResult);
		}
	}

	/********************************************************************
	 * Contains the data for a single method call in a JSON RPC batch.
	 *
	 * @author eso
	 */
	static class Call<R>
	{
		//~ Instance fields ----------------------------------------------------

		private JsonRpcRequest<?, R> rRequest;
		private Consumer<? super R>  fResponseHandler;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param rRequest         The request for the method to be called
		 * @param fResponseHandler A function that handles the response received
		 *                         by the call
		 */
		Call(
			JsonRpcRequest<?, R> rRequest,
			Consumer<? super R>  fResponseHandler)
		{
			this.rRequest		  = rRequest;
			this.fResponseHandler = fResponseHandler;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Lets the JSON RPC request parse the response and invokes the response
		 * handler if available.
		 *
		 * @param  rResponse The response to parse
		 *
		 * @return The parsed result
		 */
		public R processResponse(JsonObject rResponse)
		{
			R aResponse = rRequest.parseResponse(rResponse);

			if (fResponseHandler != null)
			{
				fResponseHandler.accept(aResponse);
			}

			return aResponse;
		}

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public String toString()
		{
			return String.format("Call(%s)", rRequest);
		}
	}
}
