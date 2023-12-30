//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
import org.obrel.core.RelationType;

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

import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_REQUEST_HEADERS;
import static org.obrel.core.RelationTypeModifier.PRIVATE;
import static org.obrel.core.RelationTypes.newType;

/**
 * An endpoint that communicates via JSON RPC with a wrapped, transport-specific
 * endpoint. In the current implementation only the HTTP(S) transport protocol
 * is supported.
 *
 * @author eso
 */
public class JsonRpcEndpoint extends Endpoint {

	/**
	 * A generic relation type to store a JSON RPC batch call.
	 */
	public static final RelationType<JsonRpcBatchCall> RPC_BATCH_CALL =
		newType();

	private static final RelationType<Connection> RPC_SERVER_CONNECTION =
		newType(PRIVATE);

	private static final RelationType<CommunicationMethod<String, String>>
		RPC_SERVER_METHOD = newType(PRIVATE);

	/**
	 * Creates a new JSON RPC batch call that can invoke multiple RPC
	 * methods at
	 * once. New methods must be added to the batch call object by invoking one
	 * of it's methods to add a request.
	 *
	 * @return The new JSON RPC batch request object
	 */
	public static JsonRpcBatchCall batchCall() {
		return new JsonRpcBatchCall();
	}

	/**
	 * Creates a new JSON RPC batch call that invokes an RPC method multiple
	 * times with different call parameters.
	 *
	 * @param method        The RPC methods to call
	 * @param defaultParams The default call parameters
	 * @return The new JSON RPC batch request object
	 */
	@SafeVarargs
	public static <P, R> JsonRpcBatchMethod<P, R> batchCall(
		JsonRpcMethod<P, R> method, P... defaultParams) {
		return new JsonRpcBatchMethod<>(method, Arrays.asList(defaultParams));
	}

	/**
	 * Returns a new {@link JsonRpcMethod} for a generic RPC invocation.
	 *
	 * @param method The name of the method to invoke
	 * @return The new method
	 */
	public static JsonRpcMethod<Object, Object> call(String method) {
		return call(method, null, json -> Json.parse(json));
	}

	/**
	 * Returns a new {@link JsonRpcMethod} with a generic parameters argument
	 * and a specific result datatype.
	 *
	 * @param method     The name of the method to invoke
	 * @param resultType The datatype to parse the result field of responses
	 *                   with
	 * @return The new method
	 */
	public static <R> JsonRpcMethod<Object, R> call(String method,
		Class<R> resultType) {
		return call(method, null, resultType);
	}

	/**
	 * Returns a new {@link JsonRpcMethod} with specific datatypes for the
	 * method parameters and the call result.
	 *
	 * @param method        The name of the method to invoke
	 * @param defaultParams Default parameters for the call (can be NULL)
	 * @param resultType    The datatype to parse the result field of responses
	 *                      with
	 * @return The new method
	 */
	public static <P, R> JsonRpcMethod<P, R> call(String method,
		P defaultParams, Class<R> resultType) {
		return new JsonRpcMethod<P, R>(method, defaultParams, resultType);
	}

	/**
	 * Returns a new {@link JsonRpcMethod} with specific datatypes for the
	 * method parameters and a parse function for the response.
	 *
	 * @param method        The name of the method to invoke
	 * @param defaultParams Default parameters for the call (can be NULL)
	 * @param parseResponse A function that parses the JSON response string
	 *                         into
	 *                      the result type
	 * @return The new method
	 */
	public static <P, R> JsonRpcMethod<P, R> call(String method,
		P defaultParams, Function<String, R> parseResponse) {
		return new JsonRpcMethod<P, R>(method, defaultParams, parseResponse);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection connection) throws Exception {
		connection.get(RPC_SERVER_CONNECTION).close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initConnection(Connection connection) throws Exception {
		URI uri = connection.getUri();
		String targetUrl = uri.getSchemeSpecificPart();

		Endpoint transportEndpoint;
		CommunicationMethod<String, String> transportMethod;

		if (targetUrl.startsWith("http")) {
			URL url = new URL(targetUrl);

			// create endpoint from URL without path
			transportEndpoint = Endpoint.at(
				HttpEndpoint.url(url.getHost(), url.getPort(),
					url.getProtocol().endsWith("s")));
			transportMethod = HttpEndpoint.httpPost(url.getPath(), null);
		} else if (targetUrl.startsWith("pipe")) {
			transportEndpoint = Endpoint.at(targetUrl);
			transportMethod = PipeEndpoint.textRequest(null);
		} else {
			throw new CommunicationException(
				"Unsupported JSON RPC transport: " + targetUrl);
		}

		Connection transportConnection = transportEndpoint.connect(connection);

		connection.set(RPC_SERVER_CONNECTION, transportConnection);
		connection.set(RPC_SERVER_METHOD, transportMethod);

		transportConnection
			.get(HTTP_REQUEST_HEADERS)
			.put("Content-Type", Collections.singletonList("application/json"
			));
	}

	/**
	 * The base class for for JSON RPC requests that execute batch calls.
	 *
	 * @author eso
	 */
	public static abstract class JsonRpcBatch<P, R>
		extends JsonRpcRequest<List<P>, List<R>> {

		/**
		 * Creates a new instance.
		 *
		 * @param name          The request name
		 * @param defaultParams The default parameters
		 */
		public JsonRpcBatch(String name, Collection<P> defaultParams) {
			super(name, new ArrayList<>(defaultParams));
		}

		/**
		 * Resets this batch by removing all default input values so that new
		 * inputs can be added and the batch can be re-executed.
		 */
		public void reset() {
			getDefaultInput().clear();
		}

		/**
		 * Returns the number of requests that have been added to this batch
		 * call.
		 *
		 * @return The number of batch requests
		 */
		public int size() {
			return getDefaultInput().size();
		}

		/**
		 * Parses the response for a single method invocation. Must be
		 * implemented by subclasses.
		 *
		 * @param response The JSON RPC response object for the method
		 * @param inputs   The method inputs
		 * @return The parsed method response
		 */
		protected abstract R parseMethodResponse(JsonObject response,
			List<P> inputs);

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected List<R> parseRawResponse(String rawResponse,
			List<P> inputs) {
			return Json
				.parseArray(rawResponse, 1)
				.stream()
				.map(o -> Json.parseObject(o.toString(), 1))
				.sorted((j1, j2) -> j1.getInt("id", 0) - j2.getInt("id", 0))
				.map(response -> parseMethodResponse(response, inputs))
				.collect(Collectors.toList());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected List<R> parseResult(String rawResponse) {
			// not used for batch calls
			return null;
		}
	}

	/**
	 * The base class for communication methods that call a JSON RPC method
	 * over
	 * the actual transport endpoint.
	 *
	 * @author eso
	 */
	public static abstract class JsonRpcRequest<P, R>
		extends CommunicationMethod<P, R> {

		/**
		 * Creates a new instance.
		 *
		 * @param name          The name of the RPC method to invoke
		 * @param defaultParams Default parameters for the method invocation
		 */
		public JsonRpcRequest(String name, P defaultParams) {
			super(name, defaultParams);
		}

		/**
		 * Builds the request for the default input of this instance. Used for
		 * batch invocation.
		 *
		 * @param id The request ID
		 * @return The JSON RPC request object
		 */
		public Object buildDefaultRequest(int id) {
			return buildRequest(getDefaultInput(), id);
		}

		/**
		 * Builds an object containing the JSON RPC request properties.
		 *
		 * @param input The input value to create the request parameters from
		 * @param id    The request ID
		 * @return The JSON JPC request object
		 */
		public abstract Object buildRequest(P input, int id);

		/**
		 * {@inheritDoc}
		 */
		@Override
		public R doOn(Connection connection, P input) {
			Connection transportConnection =
				connection.get(RPC_SERVER_CONNECTION);

			CommunicationMethod<String, String> transportMethod =
				connection.get(RPC_SERVER_METHOD);

			Object request = buildRequest(input, 1);

			String rawResponse =
				transportMethod.evaluate(Json.toCompactJson(request),
					transportConnection);

			return parseRawResponse(rawResponse, input);
		}

		/**
		 * Parses the raw JSON response string.
		 *
		 * @param rawResponse The raw JSON response
		 * @param input       The input value for which the response has been
		 *                    returned
		 * @return The parsed result
		 */
		protected R parseRawResponse(String rawResponse, P input) {
			JsonObject response = Json.parseObject(rawResponse, 1);

			return parseResponse(response);
		}

		/**
		 * Parses the response to a method call and returns the raw result
		 * value.
		 *
		 * @param response The JSON response object received from the server
		 * @return The parsed result
		 * @throws CommunicationException If an error response has been
		 *                                received
		 */
		protected R parseResponse(JsonObject response) {
			response.getString("error").ifExists(err -> {
				JsonObject error = Json.parseObject(err);

				throw new CommunicationException(
					String.format("JSON RPC Error %s: %s",
						error.getString("code"), error.getString("message")));
			});

			return parseResult(response.getString("result").orFail());
		}

		/**
		 * Parses the result of a remote method call into the target datatype.
		 * The default implementation invokes
		 * {@link Json#parse(String, Class)}.
		 *
		 * @param jsonResult The raw JSON result field of the request
		 * @return The parsed value
		 */
		protected abstract R parseResult(String jsonResult);
	}

	/**
	 * A batch invocation of JSON RPC methods that dispatches the responses to
	 * different consumers for each method call. A batch invocation will also
	 * yield a result of type List &lt;Object&gt; containing the results of the
	 * single method calls in the order in which they have been added through
	 * the add method.
	 *
	 * @author eso
	 */
	public static class JsonRpcBatchCall extends JsonRpcBatch<Call<?>,
		Object> {

		private int firstId;

		/**
		 * Creates a new instance without default calls. The calls must either
		 * be provided on evaluation or by adding default calls through
		 * invocation of the add() method.
		 */
		public JsonRpcBatchCall() {
			super(JsonRpcBatchCall.class.getSimpleName(),
				Collections.emptyList());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object buildRequest(List<Call<?>> calls, int id) {
			firstId = id;

			List<Object> callRequests = new ArrayList<>(calls.size());

			for (Call<?> call : calls) {
				Object request = call.request.buildDefaultRequest(id);

				if (request instanceof JsonObject) {
					callRequests.add(request);
					id += 1;
				} else if (request instanceof Collection) {
					Collection<?> batchRequests = (Collection<?>) request;

					callRequests.addAll(batchRequests);
					id += batchRequests.size();
				} else {
					throw new IllegalArgumentException(
						"Unsupported batch " + "request data from " +
							call.request);
				}
			}

			return callRequests;
		}

		/**
		 * Adds a new batch call to this instance that invokes a function to
		 * process the result upon receiving. The return value of the function
		 * will be available in the result of the batch call evaluation.
		 *
		 * @param request         The JSON RPC request to be invoked
		 * @param responseHandler A function that handles the response received
		 *                        by the call
		 * @return This instance for call concatenation
		 */
		public <R> JsonRpcBatchCall call(JsonRpcRequest<?, R> request,
			Consumer<? super R> responseHandler) {
			Call<R> call = new Call<>(request, responseHandler);

			getDefaultInput().add(call);

			return this;
		}

		/**
		 * Checks if this batch contains no calls.
		 *
		 * @return TRUE if no calls have been added
		 */
		public boolean isEmpty() {
			return getDefaultInput().isEmpty();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			StringBuilder result =
				new StringBuilder(getClass().getSimpleName());

			int id = 1;

			for (Call<?> call : getDefaultInput()) {
				result.append("\n");
				result.append(call.request.buildDefaultRequest(id++));
			}

			return result.toString();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Object parseMethodResponse(JsonObject response,
			List<Call<?>> calls) {
			int id = response.getInt("id", 0) - firstId;

			if (id >= 0 && id < calls.size()) {
				Call<?> call = calls.get(id);

				return call.processResponse(response);
			} else {
				throw new CommunicationException(
					"No method to parse response ID" + id);
			}
		}
	}

	/**
	 * A JSON RPC batch call that invokes a single method multiple times with
	 * different parameters.
	 *
	 * @author eso
	 */
	public static class JsonRpcBatchMethod<P, R> extends JsonRpcBatch<P, R> {

		private final JsonRpcMethod<P, ? extends R> rpcMethod;

		/**
		 * Creates a new instance.
		 *
		 * @param rpcMethod     The methods to call
		 * @param defaultInputs The default method input
		 */
		public JsonRpcBatchMethod(JsonRpcMethod<P, R> rpcMethod,
			Collection<P> defaultInputs) {
			super(JsonRpcBatchMethod.class.getSimpleName(), defaultInputs);

			this.rpcMethod = rpcMethod;
		}

		/**
		 * Adds a new method parameter to be requested by this instance.
		 *
		 * @param callParam request The JSON RPC request
		 */
		public void add(P callParam) {
			getDefaultInput().add(callParam);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Collection<JsonObject> buildRequest(List<P> inputs, int id) {
			List<JsonObject> methodRequests = new ArrayList<>(inputs.size());

			for (P params : inputs) {
				methodRequests.add(rpcMethod.buildRequest(params, id++));
			}

			return methodRequests;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return getClass().getSimpleName() + getDefaultInput();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected R parseMethodResponse(JsonObject response, List<P> input) {
			return rpcMethod.parseResponse(response);
		}
	}

	/**
	 * A JSON RPC request that calls a certain RPC method.
	 *
	 * @author eso
	 */
	public static class JsonRpcMethod<P, R> extends JsonRpcRequest<P, R> {

		private final String method;

		private final Function<String, R> parseResponse;

		private final Function<? super P, ?> convertInput;

		/**
		 * Creates a new instance that uses input values directly as RPC call
		 * parameters.
		 *
		 * @param method        The name of the RPC method to invoke
		 * @param defaultParams Default parameters for the method invocation
		 * @param responseType  The target datatype to parse the response with
		 */
		public JsonRpcMethod(String method, P defaultParams,
			Class<R> responseType) {
			this(method, defaultParams, Functions.identity(),
				JsonParser.parseJson(responseType));
		}

		/**
		 * Creates a new instance that uses input values directly as RPC call
		 * parameters.
		 *
		 * @param method        The name of the RPC method to invoke
		 * @param defaultParams Default parameters for the method invocation
		 * @param parseResponse A function that parses the JSON response string
		 *                      into the result type
		 */
		public JsonRpcMethod(String method, P defaultParams,
			Function<String, R> parseResponse) {
			this(method, defaultParams, Functions.identity(), parseResponse);
		}

		/**
		 * Creates a new instance that converts input values into the actual
		 * input value before using them as RPC call parameters.
		 *
		 * @param method        The name of the RPC method to invoke
		 * @param defaultParams Default parameters for the method invocation
		 * @param convertInput  A function that converts input parameters to
		 *                           the
		 *                      actual value to be used in the request
		 * @param parseResponse A function that parses the JSON response string
		 *                      into the result type
		 */
		public JsonRpcMethod(String method, P defaultParams,
			Function<? super P, ?> convertInput,
			Function<String, R> parseResponse) {
			super(method, defaultParams);

			this.method = method;
			this.parseResponse = parseResponse;
			this.convertInput = convertInput;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public JsonObject buildRequest(P input, int id) {
			return new JsonRpcRequestData(id, method).withParams(
				getRequestParams(input));
		}

		/**
		 * Returns the function that is used to convert input values. If not
		 * set
		 * otherwise the default is an identity function that returns the input
		 * value unchanged.
		 *
		 * @return The input conversion function
		 */
		public Function<? super P, ?> getInputConversion() {
			return convertInput;
		}

		/**
		 * Returns the value for the "params" property of a request. The
		 * default
		 * implementation just returns the input value. Subclasses can override
		 * this to extend or modify the method input.
		 *
		 * @param input The method input
		 * @return The actual request params
		 */
		protected Object getRequestParams(P input) {
			return convertInput.apply(input);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected R parseResult(String jsonResult) {
			return parseResponse.apply(jsonResult);
		}
	}

	/**
	 * Contains the data for a single method call in a JSON RPC batch.
	 *
	 * @author eso
	 */
	static class Call<R> {

		private final JsonRpcRequest<?, R> request;

		private final Consumer<? super R> responseHandler;

		/**
		 * Creates a new instance.
		 *
		 * @param request         The request for the method to be called
		 * @param responseHandler A function that handles the response received
		 *                        by the call
		 */
		Call(JsonRpcRequest<?, R> request,
			Consumer<? super R> responseHandler) {
			this.request = request;
			this.responseHandler = responseHandler;
		}

		/**
		 * Lets the JSON RPC request parse the response and invokes the
		 * response
		 * handler if available.
		 *
		 * @param response The response to parse
		 * @return The parsed result
		 */
		public R processResponse(JsonObject response) {
			R result = request.parseResponse(response);

			if (responseHandler != null) {
				responseHandler.accept(result);
			}
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return String.format("Call(%s)", request);
		}
	}
}
