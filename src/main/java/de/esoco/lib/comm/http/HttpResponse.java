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
import java.util.Arrays;
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

	private final Reader rResponseBodyReader;

	/**
	 * Creates a new instance for a successful request from a response data
	 * string. The HTTP status code will be be set to
	 * {@link HttpStatusCode#OK}.
	 *
	 * @param sResponseData The response data string
	 * @see HttpResponse#HttpResponse(HttpStatusCode, Reader, int)
	 */
	public HttpResponse(String sResponseData) {
		this(HttpStatusCode.OK, sResponseData);
	}

	/**
	 * Creates a new instance for a successful request with the status code
	 * {@link HttpStatusCode#OK}.
	 *
	 * @param rResponseData   A stream reader that provides access to the data
	 *                        of the response body
	 * @param nResponseLength The length of the response data stream
	 * @see HttpResponse#HttpResponse(HttpStatusCode, Reader, int)
	 */
	public HttpResponse(Reader rResponseData, int nResponseLength) {
		this(HttpStatusCode.OK, rResponseData, nResponseLength);
	}

	/**
	 * Creates a new instance with a certain status code and (short) response
	 * data as a string. For longer response bodies it is recommended to use
	 * the
	 * constructor with a {@link Reader} argument.
	 *
	 * @param eStatus       The response status code
	 * @param sResponseData The data of the response body
	 * @see HttpResponse#HttpResponse(HttpStatusCode, Reader, int)
	 */
	public HttpResponse(HttpStatusCode eStatus, String sResponseData) {
		this(eStatus, new StringReader(sResponseData), sResponseData.length());
	}

	/**
	 * Creates a new instance with a certain status code. The response data
	 * must
	 * be provided as a {@link Reader} instance. The status code will be set on
	 * this instance as a relation with the relation type
	 * {@link CommunicationRelationTypes#HTTP_STATUS_CODE}.
	 *
	 * @param eStatus         The response status code
	 * @param rResponseData   A stream reader that provides access to the data
	 *                        of the response body
	 * @param nResponseLength The length of the response data stream
	 */
	public HttpResponse(HttpStatusCode eStatus, Reader rResponseData,
		int nResponseLength) {
		rResponseBodyReader = rResponseData;

		init(HTTP_HEADER_TYPES);
		set(HTTP_STATUS_CODE, eStatus);
		set(CONTENT_LENGTH, nResponseLength);
	}

	/**
	 * Sets a header field of this response to a certain value.
	 *
	 * @param eField The header field to set
	 * @param sValue The field value
	 * @return The previous header value or NULL for none
	 */
	public List<String> setHeader(HttpHeaderField eField, String sValue) {
		return get(HTTP_RESPONSE_HEADERS).put(eField.getFieldName(),
			Collections.singletonList(sValue));
	}

	/**
	 * A builder-pattern variant of {@link #set(RelationType, Object)} which
	 * returns this response instance to allow the concatenation of multiple
	 * method invocations.
	 *
	 * @param rType  The type of the relation to set
	 * @param rValue The relation value
	 * @return This instance for concatenation
	 */
	public <T> HttpResponse with(RelationType<T> rType, T rValue) {
		set(rType, rValue);

		return this;
	}

	/**
	 * Writes this response to the given output stream.
	 *
	 * @param rOutput The target output stream
	 * @throws IOException If writing to the stream fails
	 */
	public void write(OutputStream rOutput) throws IOException {
		Writer aResponseHeaderWriter = new BufferedWriter(
			new OutputStreamWriter(rOutput, StandardCharsets.US_ASCII));

		// no buffer needed because StreamUtil.send() performs buffering
		Writer aResponseBodyWriter =
			new OutputStreamWriter(rOutput, get(RESPONSE_ENCODING));

		Collection<RelationType<?>> rHeaderTypes = get(HTTP_HEADER_TYPES);

		for (RelationType<?> rHeader : rHeaderTypes) {
			setHeader(rHeader.get(HTTP_HEADER_FIELD), get(rHeader).toString());
		}

		writeResponseHeader(get(HTTP_STATUS_CODE), get(HTTP_RESPONSE_HEADERS),
			aResponseHeaderWriter);
		StreamUtil.send(rResponseBodyReader, aResponseBodyWriter);
		aResponseHeaderWriter.flush();
		aResponseBodyWriter.flush();
		rOutput.flush();
	}

	/**
	 * Writes the header for an HTTP response with a certain status code to a
	 * {@link Writer}.
	 *
	 * @param eStatus The response status
	 * @param rOut    The output writer
	 * @throws IOException If writing data fails
	 */
	protected void writeResponseHeader(HttpStatusCode eStatus,
		Map<String, List<String>> rResponseHeaders, Writer rOut)
		throws IOException {
		rOut.write(eStatus.toResponseString());

		for (Entry<String, List<String>> rResponseHeader :
			rResponseHeaders.entrySet()) {
			rOut.write(rResponseHeader.getKey());
			rOut.write(": ");
			rOut.write(rResponseHeader.getValue().get(0));
			rOut.write(NetUtil.CRLF);
		}

		// terminate with empty line
		rOut.write(NetUtil.CRLF);
		rOut.flush();
	}
}
