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

import de.esoco.lib.net.NetUtil;


/********************************************************************
 * An enumeration of the standard HTTP 1.1 status codes as defined in the HTTP
 * RFC 2616 {@link https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html}.
 */
public enum HttpStatusCode
{
	CONTINUE(100, "Continue"), SWITCHING_PROTOCOLS(101, "Switching Protocols"),
	OK(200, "OK"), CREATED(201, "Created"), ACCEPTED(202, "Accepted"),
	NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
	NO_CONTENT(204, "No Content"), RESET_CONTENT(205, "Reset Content"),
	PARTIAL_CONTENT(206, "Partial Content"),
	MULTIPLE_CHOICES(300, "Multiple Choices"),
	MOVED_PERMANENTLY(301, "Moved Permanently"), FOUND(302, "Found"),
	SEE_OTHER(303, "See Other"), NOT_MODIFIED(304, "Not Modified"),
	USE_PROXY(305, "Use Proxy"), TEMPORARY_REDIRECT(307, "Temporary Redirect"),
	BAD_REQUEST(400, "Bad Request"), UNAUTHORIZED(401, "Unauthorized"),
	PAYMENT_REQUIRED(402, "Payment Required"), FORBIDDEN(403, "Forbidden"),
	NOT_FOUND(404, "Not Found"), METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
	NOT_ACCEPTABLE(406, "Not Acceptable"),
	PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
	REQUEST_TIME_OUT(408, "Request Time-out"), CONFLICT(409, "Conflict"),
	GONE(410, "Gone"), LENGTH_REQUIRED(411, "Length Required"),
	PRECONDITION_FAILED(412, "Precondition Failed"),
	REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
	REQUEST_URI_TOO_LARGE(414, "Request-URI Too Large"),
	UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
	REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable"),
	EXPECTATION_FAILED(417, "Expectation Failed"),
	INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
	NOT_IMPLEMENTED(501, "Not Implemented"), BAD_GATEWAY(502, "Bad Gateway"),
	SERVICE_UNAVAILABLE(503, "Service Unavailable"),
	GATEWAY_TIME_OUT(504, "Gateway Time-out"),
	HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version not supported");

	//~ Instance fields --------------------------------------------------------

	private final int    nStatusCode;
	private final String sReasonPhrase;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param nCode   The integer status code
	 * @param sReason The reason phrase describing the status code
	 */
	private HttpStatusCode(int nCode, String sReason)
	{
		nStatusCode   = nCode;
		sReasonPhrase = sReason;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns the status code instance for a certain integer code value.
	 *
	 * @param  nStatusCode The integer status code
	 *
	 * @return The matching status code instance
	 *
	 * @throws IllegalArgumentException If no instance for the given code exists
	 */
	public static HttpStatusCode valueOf(int nStatusCode)
	{
		for (HttpStatusCode eStatusCode : values())
		{
			if (eStatusCode.nStatusCode == nStatusCode)
			{
				return eStatusCode;
			}
		}

		throw new IllegalArgumentException("No " +
										   HttpStatusCode.class.getSimpleName() +
										   " instance with code " +
										   nStatusCode);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the integer status code.
	 *
	 * @return The integer status code
	 */
	public final int getCode()
	{
		return nStatusCode;
	}

	/***************************************
	 * Returns the reason phrase.
	 *
	 * @return The reason phrase string
	 */
	public final String getReasonPhrase()
	{
		return sReasonPhrase;
	}

	/***************************************
	 * Returns the full HTTP response status line for this status code,
	 * including CRLF at the end of the line.
	 *
	 * @return The status line
	 */
	public final String getStatusLine()
	{
		StringBuilder aStatusLine = new StringBuilder("HTTP/1.1 ");

		aStatusLine.append(nStatusCode);
		aStatusLine.append(' ');
		aStatusLine.append(sReasonPhrase);
		aStatusLine.append(NetUtil.CRLF);

		return aStatusLine.toString();
	}
}
