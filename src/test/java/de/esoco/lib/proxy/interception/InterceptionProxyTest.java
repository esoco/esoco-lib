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
package de.esoco.lib.proxy.interception;

import junit.framework.TestCase;

import java.lang.reflect.Method;

import java.util.List;

import org.obrel.core.Relatable;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import static org.obrel.core.RelationTypes.newListType;


/********************************************************************
 * Test of interception proxies.
 *
 * @author eso
 */
public class InterceptionProxyTest extends TestCase
{
	//~ Static fields/initializers ---------------------------------------------

	private static final RelationType<List<String>> TEST_STRINGS =
		newListType();

	//~ Instance fields --------------------------------------------------------

	int nLogCount    = 0;
	int nBeforeCount = 0;
	int nReturnCount = 0;
	int nThrowCount  = 0;

	//~ Instance initializers --------------------------------------------------

	{
		RelationTypes.init(InterceptionProxyTest.class);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Method testNewProxyInstance.
	 */
	public final void testNewProxyInstance()
	{
		InterceptionProxy<TestInterface> ip =
			new InterceptionProxy<TestInterface>(TestInterface.class);

		Object aTarget = new TestClass();

		ip.setDefaultInterception(InterceptionProxy.FORWARD);

		ip.addAdvice(new InterceptionAdvice()
			{
				@Override
				public void before(Object   rInvoked,
								   Method   rMethod,
								   Object[] rArgs) throws Throwable
				{
					nBeforeCount++;
				}
			});

		ip.addAdvice(new InterceptionAdvice()
			{
				@Override
				public void afterReturn(Object   rReturn,
										Object   rInvoked,
										Method   rMethod,
										Object[] rArgs) throws Throwable
				{
					nReturnCount++;
				}

				@Override
				public void afterThrow(Throwable rThrown,
									   Object    rInvoked,
									   Method    rMethod,
									   Object[]  rArgs) throws Throwable
				{
					nThrowCount++;
				}
			});

		ip.addAdvice(new InterceptionAdvice()
			{
				@Override
				public void after(Object rProxy, Method rMethod, Object[] rArgs)
					throws Throwable
				{
					nLogCount++;
				}
			});

		TestInterface ti = ip.newProxyInstance(aTarget);

		ti.open();
		assertEquals(1, nBeforeCount);
		assertEquals(1, nReturnCount);
		assertEquals(0, nThrowCount);
		assertEquals(1, nLogCount);
		ti.other();
		assertEquals(2, nBeforeCount);
		assertEquals(2, nReturnCount);
		assertEquals(0, nThrowCount);
		assertEquals(2, nLogCount);

		try
		{
			ti.close();
			assertFalse(true);
		}
		catch (Exception e)
		{
			// expected
		}

		assertEquals(3, nBeforeCount);
		assertEquals(2, nReturnCount);
		assertEquals(1, nThrowCount);
		assertEquals(3, nLogCount);
	}

	/***************************************
	 * Test proxies with explicit relation support.
	 */
	public void testRelationsExplicit()
	{
		// DirectRelationInterface extends RelationEnabled
		InterceptionProxy<RelationInterface> ip =
			new InterceptionProxy<RelationInterface>(RelationInterface.class,
													 true);

		Object    aTarget = new TestClass();
		Relatable ti	  = ip.newProxyInstance(aTarget);

		ti.get(TEST_STRINGS).add("TEST1");
		ti.get(TEST_STRINGS).add("TEST2");
		ti.get(TEST_STRINGS).add("TEST3");

		assertEquals(3, ti.get(TEST_STRINGS).size());

		// try again without relation support enabled
		ip = new InterceptionProxy<RelationInterface>(RelationInterface.class,
													  false);
		ti = ip.newProxyInstance(aTarget);

		try
		{
			ti.get(TEST_STRINGS).add("TEST1");
			assertFalse(true);
		}
		catch (Exception e)
		{
			// this is the correct execution path
		}
	}

	//~ Inner Interfaces -------------------------------------------------------

	/********************************************************************
	 * Test interface for object relations that directly implements
	 * RelationEnabled.
	 *
	 * @author eso
	 */
	public static interface RelationInterface extends TestInterface, Relatable
	{
	}

	/********************************************************************
	 * Interface InterceptorIF.
	 */
	public static interface TestInterface
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Method close.
		 */
		public void close();

		/***************************************
		 * Method open.
		 */
		public void open();

		/***************************************
		 * Method other.
		 */
		public void other();
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * Class TestClass.
	 */
	public class TestClass
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Method close.
		 *
		 * @throws Exception
		 */
		public void close() throws Exception
		{
			throw new Exception();
		}

		/***************************************
		 * Method open.
		 */
		public void open()
		{
		}

		/***************************************
		 * Method other.
		 */
		public void other()
		{
		}
	}
}
