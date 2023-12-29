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

import de.esoco.lib.expression.BinaryPredicate;
import de.esoco.lib.expression.Function;
import de.esoco.lib.net.NetUtil;
import org.obrel.type.MetaTypes;

import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import static de.esoco.lib.comm.HttpEndpoint.httpGet;
import static de.esoco.lib.comm.HttpEndpoint.httpPost;
import static de.esoco.lib.comm.SocketEndpoint.textRequest;
import static de.esoco.lib.expression.Functions.doIfElse;
import static de.esoco.lib.expression.Functions.value;
import static de.esoco.lib.expression.MathFunctions.parseInteger;
import static de.esoco.lib.expression.StringFunctions.find;
import static de.esoco.lib.io.StreamFunctions.find;
import static de.esoco.lib.io.StreamFunctions.readUntil;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.obrel.type.MetaTypes.CLOSED;

/**
 * Unit test for the communication framework.
 *
 * @author eso
 */
public class EndpointTest {

	private static final String HTTP_GET_TEST_SERVER = "example.org";

	private static final String HTTP_POST_TEST_SERVER =
		"www.posttestserver.com";

	private static final String HTML_GET_URL = "index.html";
//	private static final String HTML_POST_URL = "post";

	private static final String HTTP_GET_INDEX =
		"GET /index.html HTTP/1.1\r\nHost: " + HTTP_GET_TEST_SERVER +
			"\r\n\r\n";

	private static final String HTML_BODY_PATTERN = "(?s)<body>(.*)</body>";

	/**
	 * Test of {@link HttpEndpoint}.
	 */
//	@Test
	public void testHttpDynamic() {
		testHttpGetDynamic("http://" + HTTP_GET_TEST_SERVER);
	}

	/**
	 * Test of {@link HttpEndpoint}.
	 */
//	@Test
	public void testHttpDynamicSsl() {
		testHttpGetDynamic("https://" + HTTP_GET_TEST_SERVER);
	}

	/**
	 * Test of {@link HttpEndpoint}.
	 */
//	@Test
	public void testHttpGetStatic() {
		testHttpGetStatic("http://" + HTTP_GET_TEST_SERVER);
	}

	/**
	 * Test of {@link HttpEndpoint}.
	 */
//	@Test
	public void testHttpGetStaticSsl() {
		testHttpGetStatic("https://" + HTTP_GET_TEST_SERVER);
	}

	/**
	 * Test of {@link HttpEndpoint}.
	 */
// disabled, currently no reliable test server for POST requests
//	@Test
	public void testHttpPostStatic() throws UnsupportedEncodingException {
		testHttpPostStatic("http://" + HTTP_POST_TEST_SERVER);
	}

	/**
	 * Test {@link SocketEndpoint}.
	 */
//	@Test
	public void testSocketPlainTextRequest() {
		testSocketRequest("socket://" + HTTP_GET_TEST_SERVER + ":80");
	}

	/**
	 * Test {@link SocketEndpoint}. No longer working on example.org
	 */
//	@Test
	public void testSocketSslTextRequest() {
		testSocketRequest("sockets://" + HTTP_GET_TEST_SERVER + ":443");
	}

	/**
	 * Implementation of an HTTP GET request to a certain endpoint address that
	 * is defined dynamically at runtime by providing an URL argument.
	 *
	 * @param sEndpointAddress The endpoint address
	 */
	@SuppressWarnings("resource")
	void testHttpGetDynamic(String sEndpointAddress) {
		Endpoint aEndpoint = Endpoint.at(sEndpointAddress);
		Connection rAssertConnection;

		try (Connection rConnection = aEndpoint.connect()) {
			rAssertConnection = rConnection;

			CommunicationMethod<String, String> fBody =
				httpGet().then(find(HTML_BODY_PATTERN));

			assertTrue(Pattern.matches(HTML_BODY_PATTERN,
				fBody.getFrom(rConnection, HTML_GET_URL)));

			assertEquals(Boolean.FALSE, rConnection.get(MetaTypes.CLOSED));
			assertTrue(Pattern.matches(HTML_BODY_PATTERN,
				fBody.getFrom(rConnection, HTML_GET_URL)));
		}

		assertEquals(Boolean.TRUE, rAssertConnection.get(MetaTypes.CLOSED));
	}

	/**
	 * Implementation of a static HTTP GET request to a certain endpoint
	 * address.
	 *
	 * @param sEndpointAddress The endpoint address
	 */
	void testHttpGetStatic(String sEndpointAddress) {
		Endpoint aEndpoint = Endpoint.at(sEndpointAddress);

		EndpointFunction<String, String> fGetBody =
			httpGet(HTML_GET_URL).from(aEndpoint).then(find(HTML_BODY_PATTERN));

		assertTrue(Pattern.matches(HTML_BODY_PATTERN, fGetBody.receive()));
	}

	/**
	 * Implementation of a static HTTP POST request to a certain endpoint
	 * address.
	 *
	 * @param sEndpointAddress The endpoint address
	 */
	void testHttpPostStatic(String sEndpointAddress)
		throws UnsupportedEncodingException {
		Endpoint aEndpoint = Endpoint.at(sEndpointAddress);

		EndpointFunction<String, String> fPostParam = httpPost("post.php",
			NetUtil.encodeUrlParameter("test_param", "test_value")).from(
			aEndpoint);

		System.out.printf("POST: %s\n", fPostParam.send());
//		assertTrue(Pattern.matches(HTML_BODY_PATTERN, fPostParam.result()));
	}

	/**
	 * Implementation of a socket request to a certain endpoint address.
	 *
	 * @param sEndpointAddress The endpoint address
	 */
	void testSocketRequest(String sEndpointAddress) {
		Endpoint aEndpoint = Endpoint.at(sEndpointAddress);

		BinaryPredicate<Reader, String> pFindLength =
			find("Content-Length: ", Short.MAX_VALUE, false);

		Function<Reader, String> fReadLength =
			r -> readUntil("\r\n", 1000, false).apply(r, null);

		Function<Reader, Integer> fReadContentLength =
			doIfElse(pFindLength, fReadLength, value("-1")).then(
				parseInteger());

		EndpointFunction<String, String> fGetBody =
			textRequest(HTTP_GET_INDEX, fReadContentLength)
				.from(aEndpoint)
				.then(find(HTML_BODY_PATTERN));

		assertTrue(Pattern.matches(HTML_BODY_PATTERN, fGetBody.receive()));
		assertTrue(fGetBody.get(Endpoint.ENDPOINT_CONNECTION).hasFlag(CLOSED));
	}
}
