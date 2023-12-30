//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.logging.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class that performs whois queries.
 *
 * @author eso
 */
public class Whois {

	private static final Set<String> recursiceLookupTlds =
		new HashSet<String>(Arrays.asList("com", "net"));

	private static final Map<String, String> whoisParamsMap =
		new HashMap<String, String>();

	private final Map<String, String> whoisServerMap =
		new HashMap<String, String>();

	/**
	 * Creates a new instance.
	 *
	 * @param whoisServerListStream An input stream
	 * @throws IllegalStateException If reading the configuration file fails
	 */
	public Whois(InputStream whoisServerListStream) {
		try {
			BufferedReader serverList = new BufferedReader(
				new InputStreamReader(whoisServerListStream));
			String line;

			while ((line = serverList.readLine()) != null) {
				if (!line.startsWith("#")) {
					String[] whoisServerRecord = line.split("\\|");

					if (whoisServerRecord.length > 1) {
						String tld = whoisServerRecord[0];
						String server = whoisServerRecord[1];

						if (server.length() > 0 && !server.equals("NONE")) {
							whoisServerMap.put(tld, server);

							if (whoisServerRecord.length > 2) {
								String params = whoisServerRecord[2];

								whoisParamsMap.put(tld, params);
							}
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			Log.warn("No WHOIS server list found, using Geektools", e);
		} catch (IOException e) {
			throw new IllegalStateException(
				"Could not access WHOIS configuration");
		}
	}

	/**
	 * Main method that queries all argument domains.
	 *
	 * @param args The list of domains to query
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			try {
				Whois whois =
					new Whois(new FileInputStream("whois-server-list"));

				for (String domain : args) {
					System.out.printf("--- Domain: %s ---\n", domain);
					System.out.printf("%s", whois.query(domain));
					System.out.print("-----------------------------------\n");
				}
			} catch (Exception e) {
				System.out.println("ERR: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			System.out.print("Usage: java Whois <domain names>\n");
		}
	}

	/**
	 * queries a domain and returns a WHOIS record for it.
	 *
	 * @param domain The domain to query
	 * @return The WHOIS record or null if none could be found
	 * @throws IOException If the network connection fails
	 */
	public WhoisRecord query(String domain) throws IOException {
		String tld = domain.substring(domain.indexOf('.') + 1);
		String server = whoisServerMap.get(tld);
		String params = whoisParamsMap.get(tld);

		if (server == null) {
			server = "whois.geektools.com";
		} else if (params != null) {
			domain = params + " " + domain;
		}

		WhoisRecord whoisRecord =
			new WhoisRecord(queryWhoisServer(server, domain));

		if (recursiceLookupTlds.contains(tld)) {
			server = whoisRecord.findValue("Whois Server:");

			if (server == null) {
				whoisRecord.getLines().add(0, "+++++++++++++++++++++++++++++");
				whoisRecord
					.getLines()
					.add(1, "ERROR: No Detail WHOIS " + "server");
				whoisRecord
					.getLines()
					.add(2, "       found in master " + "record");
				whoisRecord.getLines().add(3, "+++++++++++++++++++++++++++++");
			} else {
				whoisRecord.getLines().add("");
				whoisRecord
					.getLines()
					.add("---------- Detail WHOIS from " + server +
						" -----------");
				whoisRecord.getLines().add("");
				whoisRecord.getLines().addAll(queryWhoisServer(server,
					domain));
			}
		}

		return whoisRecord;
	}

	/**
	 * Queries a WHOIS server for the data of a certain domain.
	 *
	 * @param server The name of the server to query
	 * @param domain The name of the domain to query the data for
	 * @return A list of strings containing the lines returned by the query
	 * @throws IOException If accessing the server fails
	 */
	private List<String> queryWhoisServer(String server, String domain)
		throws IOException {
		Log.info("Using WHOIS server " + server);

		Socket socket = new Socket(server, 43);
		List<String> result = new ArrayList<String>();

		try {
			PrintStream out = new PrintStream(socket.getOutputStream());

			BufferedReader in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));

			String line;

			out.println(domain);

			while ((line = in.readLine()) != null) {
				result.add(line);
			}

			in.close();
		} finally {
			socket.close();
		}

		return result;
	}

	/**
	 * An inner class that describes a whois record.
	 *
	 * @author eso
	 */
	public static class WhoisRecord {

		private final List<String> lines;

		/**
		 * Creates a new instance.
		 *
		 * @param lines The text line of the record
		 */
		public WhoisRecord(List<String> lines) {
			this.lines = lines;
		}

		/**
		 * Searches for the value of a certain key in this record. This method
		 * searches for a line that starts with the given key and returns the
		 * remaining part of that line. The key string must therefore also
		 * contain any characters that separate the key from the value in the
		 * record (e.g. ':' or '=').
		 *
		 * @param key The key to search for
		 * @return The associated value or NULL if the key doesn't exist in
		 * this
		 * record
		 */
		public String findValue(String key) {
			String value = null;

			for (String line : lines) {
				int keyPosition = line.indexOf(key);

				if (keyPosition >= 0) {
					value = line.substring(keyPosition + key.length()).trim();
				}
			}

			return value;
		}

		/**
		 * Returns the line strings of this record.
		 *
		 * @return A list containing the line strings
		 */
		public final List<String> getLines() {
			return lines;
		}

		/**
		 * @see Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			for (String line : lines) {
				result.append(line);
				result.append('\n');
			}

			return result.toString();
		}
	}
}
