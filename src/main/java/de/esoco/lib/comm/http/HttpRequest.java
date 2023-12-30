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

import de.esoco.lib.comm.http.HttpHeaderTypes.HttpHeaderField;
import de.esoco.lib.comm.http.HttpStatusException.EmptyRequestException;
import de.esoco.lib.expression.Conversions;
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.logging.Log;
import de.esoco.lib.net.NetUtil;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.esoco.lib.comm.http.HttpHeaderTypes.CONTENT_LENGTH;
import static de.esoco.lib.comm.http.HttpStatusCode.badRequest;

/**
 * A class that collects the data of an HTTP request and additional information
 * (like headers) in it's relations.
 *
 * @author eso
 */
public class HttpRequest extends RelatedObject {

	private final Reader requestReader;

	private final HttpRequestMethod requestMethod;

	private final String requestPath;

	private final Map<String, List<String>> requestHeaders;

	private int maxLineLength;

	/**
	 * Reads the incoming request and throws an exception if it doesn't match
	 * the requirements.
	 *
	 * @param input         inputReader The reader to read the request from
	 * @param maxLineLength The maximum length a request header line
	 *                         (terminated
	 *                      with CRLF) is allowed to have
	 * @throws IOException         If reading from the input fails
	 * @throws HttpStatusException The corresponding HTTP status if the request
	 *                             violates requirements
	 */
	public HttpRequest(InputStream input, int maxLineLength)
		throws IOException, HttpStatusException {
		this.maxLineLength = maxLineLength;
		requestReader = new BufferedReader(
			new InputStreamReader(input, StandardCharsets.UTF_8));

		String requestLine = readLine(requestReader);

		if (requestLine == null) {
			badRequest("Unterminated request");
		} else if (requestLine.isEmpty()) {
			badRequest("Empty request line");
		}

		HttpRequestMethod method = HttpRequestMethod.GET;
		String[] requestParts = requestLine.split(" ");

		if (requestParts.length != 3 || !requestParts[2].startsWith("HTTP/")) {
			badRequest("Malformed request line: " + requestLine);
		}

		try {
			method = HttpRequestMethod.valueOf(requestParts[0]);
		} catch (Exception e) {
			badRequest("Unknown request method: " + requestParts[0]);
		}

		requestMethod = method;
		requestPath = requestParts[1];
		requestHeaders = readHeaders(requestReader);

		Log.debugf("Request: %s %s", requestLine, requestHeaders);
	}

	/**
	 * Creates a new instance.
	 *
	 * @param requestMethod     The request method
	 * @param requestPath       The request path
	 * @param requestHeaders    The request headers
	 * @param requestBodyReader The reader that given access to the request
	 *                             body
	 *                          (if applicable)
	 */
	public HttpRequest(HttpRequestMethod requestMethod, String requestPath,
		Map<String, List<String>> requestHeaders, Reader requestBodyReader) {
		this.requestMethod = requestMethod;
		this.requestPath = requestPath;
		this.requestHeaders = requestHeaders;
		requestReader = requestBodyReader;
	}

	/**
	 * Returns the complete body of this request by reading it from the reader
	 * returned by {@link #getBodyReader()}. The maximum length to be read will
	 * be taken from the {@link HttpHeaderTypes#CONTENT_LENGTH} relation type
	 * which will be automatically set if the request is read from the input
	 * stream.
	 *
	 * @return A string containing the full body text
	 * @throws HttpStatusException {@link HttpStatusCode#LENGTH_REQUIRED} if no
	 *                             content length is provided
	 * @throws IOException         If reading the body content fails
	 */
	public final String getBody() throws IOException {
		Integer length = get(CONTENT_LENGTH);

		if (length == null) {
			throw new HttpStatusException(HttpStatusCode.LENGTH_REQUIRED,
				"Content-Length header missing");
		}

		return StreamUtil.readAll(requestReader, 8 * 1024, length.intValue());
	}

	/**
	 * Returns a reader that provides the body of the request. Will be yield no
	 * data (but will never be NULL) if the request has no body. The relation
	 * {@link HttpHeaderTypes#CONTENT_LENGTH} will contain the length of the
	 * body data.
	 *
	 * @return A reader that provides the body data
	 */
	public final Reader getBodyReader() {
		return requestReader;
	}

	/**
	 * Returns the value of a header field in this request.
	 *
	 * @param name The name of the header field
	 * @return The header field value or NULL if the header is not set
	 */
	public final List<String> getHeaderField(String name) {
		return requestHeaders.get(name);
	}

	/**
	 * Returns the value of a header field in this request. Invokes the method
	 * {@link #getHeaderField(String)} with the field name.
	 *
	 * @param field sName The header field
	 * @return The header field value or NULL if the header is not set
	 */
	public final List<String> getHeaderField(HttpHeaderField field) {
		return getHeaderField(field.getFieldName());
	}

	/**
	 * Returns the request method.
	 *
	 * @return The request method
	 */
	public final HttpRequestMethod getMethod() {
		return requestMethod;
	}

	/**
	 * Returns the requested path.
	 *
	 * @return The requested path
	 */
	public final String getPath() {
		return requestPath;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("%s[%s %s]", getClass().getSimpleName(),
			requestMethod, requestPath);
	}

	/**
	 * Tries to set an HTTP request header field as a relation on this
	 * instance.
	 * Invokes {@link HttpHeaderTypes#get(String)} with the given header name
	 * and if successfull tries to parse the value with
	 * {@link Conversions#parseValue(String, Class)} with the relation
	 * datatype.
	 *
	 * @param headerName  The name of the header field
	 * @param headerValue The value of the header field
	 * @throws HttpStatusException If the field value could not be parsed
	 */
	@SuppressWarnings("unchecked")
	protected void parseRequestHeader(String headerName, String headerValue)
		throws HttpStatusException {
		RelationType<?> headerType = HttpHeaderTypes.get(headerName);

		if (headerType != null) {
			try {
				Object value = Conversions.parseValue(headerValue,
					headerType.getTargetType());

				set((RelationType<Object>) headerType, value);
			} catch (Exception e) {
				badRequest(String.format("Invalid value for header '%s': %s",
					headerName, headerValue));
			}
		}
	}

	/**
	 * Reads the request headers into a map and returns it.
	 *
	 * @param inputReader The reader to read the header fields from
	 * @return A mapping from header field names to field values
	 * @throws IOException         On stream acces failures
	 * @throws HttpStatusException On malformed requests
	 */
	protected Map<String, List<String>> readHeaders(Reader inputReader)
		throws IOException, HttpStatusException {
		Map<String, List<String>> headers = new LinkedHashMap<>();
		String header;

		do {
			header = readLine(inputReader);

			if (header == null) {
				badRequest(
					"Request must be terminated with CRLF on empty line");
			} else if (!header.isEmpty()) {
				int colon = header.indexOf(':');

				if (colon < 1) {
					badRequest("Malformed header: " + header);
				}

				String headerName = header.substring(0, colon).trim();
				String headerValue = header.substring(colon + 1).trim();

				List<String> headerValues = headers.get(headerName);

				if (headerValues == null) {
					headerValues = new ArrayList<>();
				}

				headerValues.add(headerValue);
				headers.put(headerName, headerValues);
				parseRequestHeader(headerName, headerValue);
			}
		} while (!header.isEmpty());

		return headers;
	}

	/**
	 * Helper method to read a single HTTP request line (terminated with CRLF)
	 * from a {@link Reader}.
	 *
	 * @param reader The reader to read the line from
	 * @return The line string or NULL if no terminating CRLF could be found
	 * @throws IOException         If reading data fails
	 * @throws HttpStatusException If the line is not terminated correctly
	 */
	protected String readLine(Reader reader) throws IOException {
		StringWriter result = new StringWriter();
		String line = null;

		if (StreamUtil.readUntil(reader, result, NetUtil.CRLF, maxLineLength,
			false)) {
			line = result.toString();
			line = line.substring(0, line.length() - 2);
		} else if (result.getBuffer().length() == 0) {
			throw new EmptyRequestException();
		} else {
			badRequest("Request line not terminated with CRLF");
		}

		return line;
	}
}
