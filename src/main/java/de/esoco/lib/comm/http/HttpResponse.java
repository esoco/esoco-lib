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

import de.esoco.lib.comm.CommunicationRelationTypes;
import de.esoco.lib.comm.http.HttpHeaderTypes.HttpHeaderField;
import de.esoco.lib.io.StreamUtil;
import de.esoco.lib.net.NetUtil;
import org.obrel.core.RelatedObject;
import org.obrel.core.RelationType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_RESPONSE_HEADERS;
import static de.esoco.lib.comm.CommunicationRelationTypes.HTTP_STATUS_CODE;
import static de.esoco.lib.comm.CommunicationRelationTypes.RESPONSE_ENCODING;
import static de.esoco.lib.comm.http.HttpHeaderTypes.CONTENT_LENGTH;
import static de.esoco.lib.comm.http.HttpHeaderTypes.HTTP_HEADER_FIELD;
import static de.esoco.lib.comm.http.HttpHeaderTypes.HTTP_HEADER_TYPES;

/**
 * A class that contains the data of an HTTP response and additional response
 * information (like headers) in it's relations.
 *
 * @author eso
 */
public class HttpResponse extends RelatedObject {

	private final Reader responseBodyReader;

	/**
	 * Creates a new instance for a successful request from a response data
	 * string. The HTTP status code will be be set to
	 * {@link HttpStatusCode#OK}.
	 *
	 * @param responseData The response data string
	 * @see HttpResponse#HttpResponse(HttpStatusCode, Reader, int)
	 */
	public HttpResponse(String responseData) {
		this(HttpStatusCode.OK, responseData);
	}

	/**
	 * Creates a new instance for a successful request with the status code
	 * {@link HttpStatusCode#OK}.
	 *
	 * @param responseData   A stream reader that provides access to the
	 *                          data of
	 *                       the response body
	 * @param responseLength The length of the response data stream
	 * @see HttpResponse#HttpResponse(HttpStatusCode, Reader, int)
	 */
	public HttpResponse(Reader responseData, int responseLength) {
		this(HttpStatusCode.OK, responseData, responseLength);
	}

	/**
	 * Creates a new instance with a certain status code and (short) response
	 * data as a string. For longer response bodies it is recommended to use
	 * the
	 * constructor with a {@link Reader} argument.
	 *
	 * @param status       The response status code
	 * @param responseData The data of the response body
	 * @see HttpResponse#HttpResponse(HttpStatusCode, Reader, int)
	 */
	public HttpResponse(HttpStatusCode status, String responseData) {
		this(status, new StringReader(responseData), responseData.length());
	}

	/**
	 * Creates a new instance with a certain status code. The response data
	 * must
	 * be provided as a {@link Reader} instance. The status code will be set on
	 * this instance as a relation with the relation type
	 * {@link CommunicationRelationTypes#HTTP_STATUS_CODE}.
	 *
	 * @param status         The response status code
	 * @param responseData   A stream reader that provides access to the
	 *                          data of
	 *                       the response body
	 * @param responseLength The length of the response data stream
	 */
	public HttpResponse(HttpStatusCode status, Reader responseData,
		int responseLength) {
		responseBodyReader = responseData;

		init(HTTP_HEADER_TYPES);
		set(HTTP_STATUS_CODE, status);
		set(CONTENT_LENGTH, responseLength);
	}

	/**
	 * Sets a header field of this response to a certain value.
	 *
	 * @param field The header field to set
	 * @param value The field value
	 * @return The previous header value or NULL for none
	 */
	public List<String> setHeader(HttpHeaderField field, String value) {
		return get(HTTP_RESPONSE_HEADERS).put(field.getFieldName(),
			Collections.singletonList(value));
	}

	/**
	 * A builder-pattern variant of {@link #set(RelationType, Object)} which
	 * returns this response instance to allow the concatenation of multiple
	 * method invocations.
	 *
	 * @param type  The type of the relation to set
	 * @param value The relation value
	 * @return This instance for concatenation
	 */
	public <T> HttpResponse with(RelationType<T> type, T value) {
		set(type, value);

		return this;
	}

	/**
	 * Writes this response to the given output stream.
	 *
	 * @param output The target output stream
	 * @throws IOException If writing to the stream fails
	 */
	public void write(OutputStream output) throws IOException {
		Writer responseHeaderWriter = new BufferedWriter(
			new OutputStreamWriter(output, StandardCharsets.US_ASCII));

		// no buffer needed because StreamUtil.send() performs buffering
		Writer responseBodyWriter =
			new OutputStreamWriter(output, get(RESPONSE_ENCODING));

		Collection<RelationType<?>> headerTypes = get(HTTP_HEADER_TYPES);

		for (RelationType<?> header : headerTypes) {
			setHeader(header.get(HTTP_HEADER_FIELD), get(header).toString());
		}

		writeResponseHeader(get(HTTP_STATUS_CODE), get(HTTP_RESPONSE_HEADERS),
			responseHeaderWriter);
		StreamUtil.send(responseBodyReader, responseBodyWriter);
		responseHeaderWriter.flush();
		responseBodyWriter.flush();
		output.flush();
	}

	/**
	 * Writes the header for an HTTP response with a certain status code to a
	 * {@link Writer}.
	 *
	 * @param status The response status
	 * @param out    The output writer
	 * @throws IOException If writing data fails
	 */
	protected void writeResponseHeader(HttpStatusCode status,
		Map<String, List<String>> responseHeaders, Writer out)
		throws IOException {
		out.write(status.toResponseString());

		for (Entry<String, List<String>> responseHeader :
			responseHeaders.entrySet()) {
			out.write(responseHeader.getKey());
			out.write(": ");
			out.write(responseHeader.getValue().get(0));
			out.write(NetUtil.CRLF);
		}

		// terminate with empty line
		out.write(NetUtil.CRLF);
		out.flush();
	}
}
