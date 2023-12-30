//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
import de.esoco.lib.comm.http.HttpHeaderTypes.HttpHeaderField;
import de.esoco.lib.datatype.Pair;

import java.util.Collections;
import java.util.Map;

/**
 * An unchecked exception to report exceptional HTTP responses.
 *
 * @author eso
 */
public class HttpStatusException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HttpStatusCode statusCode;

	private Map<HttpHeaderField, String> responseHeaders =
		Collections.emptyMap();

	/**
	 * Creates a new instance with a status code and causing exception.
	 *
	 * @param statusCode The status code
	 * @param cause      The causing exception
	 */
	public HttpStatusException(HttpStatusCode statusCode, Exception cause) {
		super(cause);

		this.statusCode = statusCode;
	}

	/**
	 * Creates a new instance with a status code and message.
	 *
	 * @param statusCode      The status code
	 * @param message         The error message
	 * @param responseHeaders An optional array of headers to be set on the
	 *                        response
	 */
	@SafeVarargs
	public HttpStatusException(HttpStatusCode statusCode, String message,
		Pair<HttpHeaderField, String>... responseHeaders) {
		super(message);

		this.statusCode = statusCode;
		this.responseHeaders =
			CollectionUtil.fixedOrderedMapOf(responseHeaders);
	}

	/**
	 * Creates a new instance with a status code, message, and causing
	 * exception.
	 *
	 * @param statusCode The status code
	 * @param message    The error message
	 * @param cause      The causing exception
	 */
	public HttpStatusException(HttpStatusCode statusCode, String message,
		Exception cause) {
		super(message, cause);

		this.statusCode = statusCode;
	}

	/**
	 * Returns a map of the optional response headers for this status code
	 * exception.
	 *
	 * @return The response headers map (may be empty but will never be NULL)
	 */
	public final Map<HttpHeaderField, String> getResponseHeaders() {
		return responseHeaders;
	}

	/**
	 * Returns the HTTP status code.
	 *
	 * @return The status code
	 */
	public final HttpStatusCode getStatusCode() {
		return statusCode;
	}

	/**
	 * A subclass that indicates a bad request that has no content.
	 *
	 * @author eso
	 */
	public static class EmptyRequestException extends HttpStatusException {

		private static final long serialVersionUID = 1L;

		/**
		 * Creates a new instance.
		 */
		public EmptyRequestException() {
			super(HttpStatusCode.BAD_REQUEST, "Request empty");
		}
	}
}
