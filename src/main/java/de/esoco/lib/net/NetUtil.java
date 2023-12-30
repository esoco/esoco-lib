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
package de.esoco.lib.net;

import de.esoco.lib.io.StreamUtil;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Utility class containing static network helper methods.
 *
 * @author eso
 */
public class NetUtil {

	/**
	 * An enumeration of socket connection types.
	 */
	public enum SocketType {PLAIN, SSL, SELF_SIGNED_SSL}

	/**
	 * Constant for the default wake-on-LAN port
	 */
	public static final int WAKEONLAN_DEFAULT_PORT = 9;

	/**
	 * A constant for the \r\n string that is used as a separator in HTTP
	 * requests.
	 */
	public static final String CRLF = "\r\n";

	private static final String TUNNELING_CHARSET = "ASCII7";

	private static final String JAVA_VERSION =
		"Java/" + System.getProperty("java.version");

	/**
	 * A user agent string like the string used by internal Java classes.
	 */
	public static final String JAVA_USER_AGENT =
		System.getProperty("http.agent") == null ?
		JAVA_VERSION :
		System.getProperty("http.agent") + " " + JAVA_VERSION;

	/**
	 * The standard encoding for URL elements (UTF-8).
	 */
	public static String URL_ENCODING = StandardCharsets.UTF_8.name();

	/**
	 * Private, only static use.
	 */
	private NetUtil() {
	}

	/**
	 * Appends a path element to an URL string and adds a separating '/' if
	 * necessary.
	 *
	 * @param baseUrl rUrlBuilder The string build containing the base URL
	 * @param path    The URL path to append
	 * @return The resulting URL string
	 */
	public static String appendUrlPath(String baseUrl, String path) {
		return appendUrlPath(new StringBuilder(baseUrl), path).toString();
	}

	/**
	 * Appends a path element to an URL string builder and adds a separating
	 * '/'
	 * if necessary.
	 *
	 * @param urlBuilder The string build containing the base URL
	 * @param path       The URL path to append
	 * @return The input URL builder to allow concatenation
	 */
	public static StringBuilder appendUrlPath(StringBuilder urlBuilder,
		String path) {
		if (path != null && path.length() > 0) {
			if (urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
				if (path.charAt(0) != '/') {
					urlBuilder.append('/');
				}
			} else if (path.charAt(0) == '/') {
				path = path.substring(1);
			}

			urlBuilder.append(path);
		}

		return urlBuilder;
	}

	/**
	 * Creates a new socket for the connection to a certain host and port. This
	 * method takes into account any system properties for a connection proxy.
	 *
	 * @param host       The host to connect the socket to
	 * @param port       The port to connect the socket to
	 * @param socketType The type of socket connection to create
	 * @return A new socket that is connected to the given host and port
	 * @throws IOException If creating the socket fails
	 */
	public static Socket createSocket(String host, int port,
		SocketType socketType) throws IOException {
		boolean sSL = (socketType != SocketType.PLAIN);
		String proxyHost =
			System.getProperty(sSL ? "https.proxyHost" : "http.proxyHost");

		Socket socket;
		int proxyPort = 0;

		if (proxyHost != null) {
			proxyPort = Integer.parseInt(
				System.getProperty(sSL ? "https.proxyPort" : "http.proxyPort"
				));

			String nonProxyHosts = System.getProperty("http.nonProxyHosts");

			if (nonProxyHosts != null) {
				nonProxyHosts = nonProxyHosts.replaceAll("\\.", "\\.");
				nonProxyHosts = nonProxyHosts.replaceAll("\\*", ".*");

				if (Pattern.matches(nonProxyHosts, host)) {
					proxyHost = null;
					proxyPort = 0;
				}
			}
		}

		if (sSL) {
			SSLSocketFactory factory;

			if (socketType == SocketType.SSL) {
				factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			} else {
				factory = getTrustingSocketFactory();
			}

			if (proxyHost != null) {
				socket =
					createTunnelingSslSocket(factory, host, port, proxyHost,
						proxyPort);
			} else {
				socket = factory.createSocket(host, port);
			}
		} else {
			if (proxyHost != null) {
				Proxy proxy = new Proxy(Proxy.Type.HTTP,
					new InetSocketAddress(proxyHost, proxyPort));

				socket = new Socket(proxy);

				socket.connect(new InetSocketAddress(host, port));
			} else {
				socket = SocketFactory.getDefault().createSocket(host, port);
			}
		}

		return socket;
	}

	/**
	 * Creates a new SSL {@link Socket} that tunnels it's communication through
	 * a proxy.
	 *
	 * @param factory   The factory to create the socket with
	 * @param host      The host to connect the socket to
	 * @param port      The port to connect the socket to
	 * @param proxyHost The host of the proxy to tunnel through
	 * @param proxyPort The port of the proxy to tunnel through
	 * @return The new tunneling SSL socket, initialized and connected to the
	 * given host and port
	 * @throws IOException If creating or initializing the socket fails
	 */
	public static Socket createTunnelingSslSocket(SSLSocketFactory factory,
		String host, int port, String proxyHost, int proxyPort)
		throws IOException {
		Socket tunnelSocket = new Socket(proxyHost, proxyPort);

		initTunneling(tunnelSocket, host, port);

		SSLSocket socket =
			(SSLSocket) factory.createSocket(tunnelSocket, host, port, true);

		socket.startHandshake();

		return socket;
	}

	/**
	 * Enables HTTP basic authentication for a certain {@link URLConnection}.
	 *
	 * @param urlConnection The URL connection
	 * @param userName      The user name to perform the authentication with
	 * @param password      The password to perform the authentication with
	 */
	public static void enableHttpBasicAuth(URLConnection urlConnection,
		String userName, String password) {
		String auth = userName + ":" + password;

		auth = Base64.getEncoder().encodeToString(auth.getBytes());

		urlConnection.setRequestProperty("Authorization", "Basic " + auth);
	}

	/**
	 * Encodes a string so that it can be used as an element in an HTTP URL by
	 * applying the method {@link URLEncoder#encode(String, String)} with the
	 * recommended default encoding UTF-8.
	 *
	 * @param element sName The name of the URL parameter
	 * @return A string containing the encoded parameter assignment
	 */
	public static String encodeUrlElement(String element) {
		try {
			return URLEncoder.encode(element, URL_ENCODING);
		} catch (UnsupportedEncodingException e) {
			// UTF-8 needs to be available for URL encoding
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Encodes the name and value of an HTTP URL parameter by applying the
	 * method {@link #encodeUrlElement(String)} to each and concatenating them
	 * with '='.
	 *
	 * @param name  The name of the URL parameter
	 * @param value The value of the URL parameter
	 * @return A string containing the encoded parameter assignment
	 */
	public static String encodeUrlParameter(String name, String value) {
		return encodeUrlElement(name) + "=" + encodeUrlElement(value);
	}

	/**
	 * Creates a concatenated string of multiple HTTP URL parameters that have
	 * been encoded with {@link #encodeUrlParameter(String, String)}. The
	 * concatenation character is '&amp;', the encoding UTF-8.
	 *
	 * @param params A mapping from HTTP URL parameter names to values
	 * @return The encoded parameters (may be empty but will never be NULL)
	 */
	public static String encodeUrlParameters(Map<String, String> params) {
		StringBuilder result = new StringBuilder();

		for (Entry<String, String> param : params.entrySet()) {
			result.append(encodeUrlParameter(param.getKey(),
				param.getValue()));
			result.append('&');
		}

		int length = result.length();

		if (length > 0) {
			result.setLength(length - 1);
		}

		return result.toString();
	}

	/**
	 * Returns a SSL socket factory that trusts self-signed certificates.
	 * Attention: this should only be used in test scenarios, not for
	 * production
	 * code!
	 *
	 * @return The trusting socket factory
	 */
	public static final SSLSocketFactory getTrustingSocketFactory() {
		SSLSocketFactory sslSocketFactory;

		try {
			TrustManager[] trustManagers =
				new TrustManager[] { new SelfSignedCertificateTrustManager() };

			SSLContext sslContext = SSLContext.getInstance("SSL");

			sslContext.init(null, trustManagers, new SecureRandom());

			sslSocketFactory = sslContext.getSocketFactory();
		} catch (Exception e) {
			throw new SecurityException(e);
		}

		return sslSocketFactory;
	}

	/**
	 * Initializes the tunneling of communication through a proxy.
	 *
	 * @param proxySocket The socket that is connected to the tunneling proxy
	 * @param host        The host to connect the tunnel to
	 * @param port        The port to connect the tunnel to
	 * @throws IOException If the initialization fails
	 */
	@SuppressWarnings("boxing")
	public static void initTunneling(Socket proxySocket, String host, int port)
		throws IOException {
		String request = String.format(
			"CONNECT %s:%d HTTP/1.0\nUser-Agent: " + JAVA_USER_AGENT +
				"\r\n\r\n", host, port);

		OutputStream outputStream = proxySocket.getOutputStream();
		byte[] requestBytes = request.getBytes(TUNNELING_CHARSET);
		String reply = "";

		outputStream.write(requestBytes);
		outputStream.flush();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] replyBytes = null;

		if (StreamUtil.readUntil(proxySocket.getInputStream(), output,
			"\r\n\r\n".getBytes(TUNNELING_CHARSET), 512)) {
			replyBytes = output.toByteArray();
		}

		if (replyBytes != null) {
			reply = new String(replyBytes, TUNNELING_CHARSET);
		}

		if (!reply.startsWith("HTTP/1.0 200")) {
			throw new IOException(String.format(
				"Cannot tunnel through %s:%d. " + "Proxy response: %s", host,
				port, reply));
		}
	}

	/**
	 * Sends a Wake-On-LAN packet to a particular MAC address over a certain
	 * broadcast IP address on the default port.
	 *
	 * @see #wakeUp(MACAddress, InetAddress, int)
	 */
	public static void wakeUp(MACAddress mACAddress, InetAddress broadcastIP)
		throws IOException {
		wakeUp(mACAddress, broadcastIP, WAKEONLAN_DEFAULT_PORT);
	}

	/**
	 * Sends a Wake-On-LAN packet to a particular MAC address over a certain
	 * broadcast IP address and port. The broadcast IP is typcially an address
	 * that ends with 255 in the same subnet where the machine to be waked up
	 * will appear, like 192.168.0.255. It is not the IP of the target machine!
	 * This is because that machine is probably inactive and therefore doesn't
	 * have an IP address until it has been waked up.
	 *
	 * @param mACAddress  The MAC address of the target network adapter
	 * @param broadcastIP The broadcast IP number to send the packet to
	 * @param port        The port to send the broadcast packet to
	 * @throws IOException If the network access fails
	 */

	public static void wakeUp(MACAddress mACAddress, InetAddress broadcastIP,
		int port) throws IOException {
		DatagramSocket socket = new DatagramSocket();
		byte[] mACBytes = mACAddress.getBytes();
		byte[] datagram = new byte[17 * 6];

		for (int i = 0; i < 6; i++) {
			datagram[i] = (byte) 0xff;
		}

		for (int i = 6; i < datagram.length; i += 6) {
			System.arraycopy(mACBytes, 0, datagram, i, 6);
		}

		DatagramPacket packet =
			new DatagramPacket(datagram, datagram.length, broadcastIP, port);

		socket.send(packet);
		socket.close();
	}

	/**
	 * An implementation of {@link TrustManager} that accepts self-signed
	 * certificates.
	 */
	public static class SelfSignedCertificateTrustManager
		implements X509TrustManager {

		/**
		 * @see X509TrustManager#checkClientTrusted(X509Certificate[], String)
		 */
		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
			// perform no checks to accept any certificate
		}

		/**
		 * @see X509TrustManager#checkServerTrusted(X509Certificate[], String)
		 */
		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
			// perform no checks to accept any certificate
		}

		/**
		 * @see X509TrustManager#getAcceptedIssuers()
		 */
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
}
