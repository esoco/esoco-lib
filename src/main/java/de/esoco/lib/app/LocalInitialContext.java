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
package de.esoco.lib.app;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link InitialContext} implementation for usage in local applications that
 * don't have access to an application server infrastructure.
 *
 * @author eso
 */
public class LocalInitialContext extends InitialContext {

	private static final Map<String, Object> aContextRegistry =
		new HashMap<>();

	/**
	 * Creates a new instance.
	 */
	public LocalInitialContext() throws NamingException {
	}

	/**
	 * Registers this class with the {@link NamingManager} as the default
	 * initial context for JNDI lookups.
	 *
	 * @param rContextFactory The factory for local initial contexts
	 */
	public static void registerLocalContext(
		InitialContextFactory rContextFactory) {
		try {
			NamingManager.setInitialContextFactoryBuilder(
				env -> rContextFactory);
		} catch (NamingException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Registers an object under a certain JDNI name in the global registry for
	 * the local context.
	 *
	 * @param sName   The name to register the object under
	 * @param rObject The resource object
	 */
	public static void registerResource(String sName, Object rObject) {
		aContextRegistry.put(sName, rObject);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object lookup(String sName) throws NamingException {
		if (aContextRegistry.containsKey(sName)) {
			return aContextRegistry.get(sName);
		}

		throw new NamingException("Unable to find object " + sName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Context getDefaultInitCtx() {
		// return THIS to prevent endless recursion with a default initial
		// context injected by the environment (e.g. Eclipse injects a Jetty
		// context into the environment)
		return null;
	}
}
