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

	private static final Set<String> aRecursiceLookupTlds =
		new HashSet<String>(Arrays.asList("com", "net"));

	private static final Map<String, String> aWhoisParamsMap =
		new HashMap<String, String>();

	private final Map<String, String> aWhoisServerMap =
		new HashMap<String, String>();

	/**
	 * Creates a new instance.
	 *
	 * @param rWhoisServerListStream An input stream
	 * @throws IllegalStateException If reading the configuration file fails
	 */
	public Whois(InputStream rWhoisServerListStream) {
		try {
			BufferedReader aServerList = new BufferedReader(
				new InputStreamReader(rWhoisServerListStream));
			String sLine;

			while ((sLine = aServerList.readLine()) != null) {
				if (!sLine.startsWith("#")) {
					String[] aWhoisServerRecord = sLine.split("\\|");

					if (aWhoisServerRecord.length > 1) {
						String sTld = aWhoisServerRecord[0];
						String sServer = aWhoisServerRecord[1];

						if (sServer.length() > 0 && !sServer.equals("NONE")) {
							aWhoisServerMap.put(sTld, sServer);

							if (aWhoisServerRecord.length > 2) {
								String sParams = aWhoisServerRecord[2];

								aWhoisParamsMap.put(sTld, sParams);
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
	 * @param rArgs The list of domains to query
	 */
	public static void main(String[] rArgs) {
		if (rArgs.length > 0) {
			try {
				Whois aWhois =
					new Whois(new FileInputStream("whois-server-list"));

				for (String sDomain : rArgs) {
					System.out.printf("--- Domain: %s ---\n", sDomain);
					System.out.printf("%s", aWhois.query(sDomain));
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
	 * @param sDomain The domain to query
	 * @return The WHOIS record or null if none could be found
	 * @throws IOException If the network connection fails
	 */
	public WhoisRecord query(String sDomain) throws IOException {
		String sTld = sDomain.substring(sDomain.indexOf('.') + 1);
		String sServer = aWhoisServerMap.get(sTld);
		String sParams = aWhoisParamsMap.get(sTld);

		if (sServer == null) {
			sServer = "whois.geektools.com";
		} else if (sParams != null) {
			sDomain = sParams + " " + sDomain;
		}

		WhoisRecord aWhoisRecord =
			new WhoisRecord(queryWhoisServer(sServer, sDomain));

		if (aRecursiceLookupTlds.contains(sTld)) {
			sServer = aWhoisRecord.findValue("Whois Server:");

			if (sServer == null) {
				aWhoisRecord.getLines().add(0,
					"+++++++++++++++++++++++++++++");
				aWhoisRecord
					.getLines()
					.add(1, "ERROR: No Detail WHOIS " + "server");
				aWhoisRecord
					.getLines()
					.add(2, "       found in master " + "record");
				aWhoisRecord.getLines().add(3,
					"+++++++++++++++++++++++++++++");
			} else {
				aWhoisRecord.getLines().add("");
				aWhoisRecord
					.getLines()
					.add("---------- Detail WHOIS from " + sServer +
						" -----------");
				aWhoisRecord.getLines().add("");
				aWhoisRecord
					.getLines()
					.addAll(queryWhoisServer(sServer, sDomain));
			}
		}

		return aWhoisRecord;
	}

	/**
	 * Queries a WHOIS server for the data of a certain domain.
	 *
	 * @param sServer The name of the server to query
	 * @param sDomain The name of the domain to query the data for
	 * @return A list of strings containing the lines returned by the query
	 * @throws IOException If accessing the server fails
	 */
	private List<String> queryWhoisServer(String sServer, String sDomain)
		throws IOException {
		Log.info("Using WHOIS server " + sServer);

		Socket aSocket = new Socket(sServer, 43);
		List<String> aResult = new ArrayList<String>();

		try {
			PrintStream aOut = new PrintStream(aSocket.getOutputStream());

			BufferedReader aIn = new BufferedReader(
				new InputStreamReader(aSocket.getInputStream()));

			String sLine;

			aOut.println(sDomain);

			while ((sLine = aIn.readLine()) != null) {
				aResult.add(sLine);
			}

			aIn.close();
		} finally {
			aSocket.close();
		}

		return aResult;
	}

	/**
	 * An inner class that describes a whois record.
	 *
	 * @author eso
	 */
	public static class WhoisRecord {

		private final List<String> rLines;

		/**
		 * Creates a new instance.
		 *
		 * @param rLines The text line of the record
		 */
		public WhoisRecord(List<String> rLines) {
			this.rLines = rLines;
		}

		/**
		 * Searches for the value of a certain key in this record. This method
		 * searches for a line that starts with the given key and returns the
		 * remaining part of that line. The key string must therefore also
		 * contain any characters that separate the key from the value in the
		 * record (e.g. ':' or '=').
		 *
		 * @param sKey The key to search for
		 * @return The associated value or NULL if the key doesn't exist in
		 * this
		 * record
		 */
		public String findValue(String sKey) {
			String sValue = null;

			for (String sLine : rLines) {
				int nKeyPosition = sLine.indexOf(sKey);

				if (nKeyPosition >= 0) {
					sValue =
						sLine.substring(nKeyPosition + sKey.length()).trim();
				}
			}

			return sValue;
		}

		/**
		 * Returns the line strings of this record.
		 *
		 * @return A list containing the line strings
		 */
		public final List<String> getLines() {
			return rLines;
		}

		/**
		 * @see Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder aResult = new StringBuilder();

			for (String sLine : rLines) {
				aResult.append(sLine);
				aResult.append('\n');
			}

			return aResult.toString();
		}
	}
}
