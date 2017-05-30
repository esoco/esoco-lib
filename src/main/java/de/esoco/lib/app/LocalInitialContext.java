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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;


/********************************************************************
 * A {@link InitialContext} implementation for usage in local applications that
 * don't have access to an application server infrastructure.
 *
 * @author eso
 */
public class LocalInitialContext extends InitialContext
{
	//~ Static fields/initializers ---------------------------------------------

	private static Map<String, Object> aContextRegistry = new HashMap<>();

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	LocalInitialContext(Hashtable<?, ?> rEnvironment) throws NamingException
	{
		super(rEnvironment);
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Registers an object under a certain JDNI name in the global registry for
	 * the local context.
	 *
	 * @param sName   The name to register the object under
	 * @param rObject The resource object
	 */
	public static void registerResource(String sName, Object rObject)
	{
		aContextRegistry.put(sName, rObject);
	}

	/***************************************
	 * Registers this class with the {@link NamingManager} as the default
	 * initial context for JNDI lookups.
	 */
	public static void setupForJndiLookups()
	{
		try
		{
			LocalInitialContextFactoryBuilder aFactoryBuilder =
				new LocalInitialContextFactoryBuilder();

			NamingManager.setInitialContextFactoryBuilder(aFactoryBuilder);
		}
		catch (NamingException e)
		{
			throw new IllegalStateException(e);
		}
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public Object lookup(String sName) throws NamingException
	{
		if (aContextRegistry.containsKey(sName))
		{
			return aContextRegistry.get(sName);
		}

		throw new NamingException("Unable to find object " + sName);
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A builder for factories that create instances of {@link
	 * LocalInitialContext}.
	 *
	 * @author eso
	 */
	public static class LocalInitialContextFactoryBuilder
		implements InitialContextFactoryBuilder
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public InitialContextFactory createInitialContextFactory(
			Hashtable<?, ?> rEnvironment) throws NamingException
		{
			return env -> new LocalInitialContext(env);
		}
	}
}
