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

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

/**
 * Host-based Authenticator implementation that returns passwords based on the
 * address of the requesting host.
 *
 * @author eso
 */
public class HostAuthenticator extends Authenticator {

	private static final Map<String, PasswordAuthentication> authentications =
		new HashMap<String, PasswordAuthentication>();

	/**
	 * Adds a password authentication for a certain host.
	 *
	 * @param host The host to set the authentication for
	 * @param auth The password authentication object
	 */
	public static void addAuthentication(String host,
		PasswordAuthentication auth) {
		authentications.put(host, auth);
	}

	/**
	 * Adds an authentication for a certain host. For higher security please
	 * use
	 * the {@link #addAuthentication(String, PasswordAuthentication)} method.
	 *
	 * @param host     The host to set the authentication for
	 * @param user     The user name
	 * @param password The password
	 */
	public static void addAuthentication(String host, String user,
		String password) {
		addAuthentication(host,
			new PasswordAuthentication(user, password.toCharArray()));
	}

	/**
	 * Enables host-based authentication by registering an instance of this
	 * class by means of the {@link Authenticator#setDefault(Authenticator)}
	 * method.
	 */
	public static void enable() {
		Authenticator.setDefault(new HostAuthenticator());
	}

	/**
	 * Returns the password authentication for the current requesting host.
	 *
	 * @return The password authentication
	 */
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return authentications.get(getRequestingHost());
	}
}
