//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2020 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import org.junit.jupiter.api.Test;
import org.obrel.core.Relatable;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.obrel.core.RelationTypes.newListType;

/**
 * Test of interception proxies.
 *
 * @author eso
 */
class InterceptionProxyTest {

	private static final RelationType<List<String>> TEST_STRINGS =
		newListType();

	static {
		RelationTypes.init(InterceptionProxyTest.class);
	}

	int logCount = 0;

	int beforeCount = 0;

	int returnCount = 0;

	int throwCount = 0;

	/**
	 * Method testNewProxyInstance.
	 */
	@Test
	final void testNewProxyInstance() {
		InterceptionProxy<TestInterface> ip =
			new InterceptionProxy<TestInterface>(TestInterface.class);

		Object target = new TestClass();

		ip.setDefaultInterception(InterceptionProxy.FORWARD);

		ip.addAdvice(new InterceptionAdvice() {
			@Override
			public void before(Object invoked, Method method, Object[] args)
				throws Exception {
				beforeCount++;
			}
		});

		ip.addAdvice(new InterceptionAdvice() {
			@Override
			public void afterReturn(Object toReturn, Object invoked,
				Method method, Object[] args) throws Exception {
				returnCount++;
			}

			@Override
			public void afterThrow(Throwable thrown, Object invoked,
				Method method, Object[] args) throws Exception {
				throwCount++;
			}
		});

		ip.addAdvice(new InterceptionAdvice() {
			@Override
			public void after(Object proxy, Method method, Object[] args)
				throws Exception {
				logCount++;
			}
		});

		TestInterface ti = ip.newProxyInstance(target);

		ti.open();
		assertEquals(1, beforeCount);
		assertEquals(1, returnCount);
		assertEquals(0, throwCount);
		assertEquals(1, logCount);
		ti.other();
		assertEquals(2, beforeCount);
		assertEquals(2, returnCount);
		assertEquals(0, throwCount);
		assertEquals(2, logCount);

		try {
			ti.close();
			fail();
		} catch (Exception e) {
			// expected
		}

		assertEquals(3, beforeCount);
		assertEquals(2, returnCount);
		assertEquals(1, throwCount);
		assertEquals(3, logCount);
	}

	/**
	 * Test proxies with explicit relation support.
	 */
	@Test
	void testRelationsExplicit() {
		// DirectRelationInterface extends RelationEnabled
		InterceptionProxy<RelationInterface> ip =
			new InterceptionProxy<RelationInterface>(RelationInterface.class,
				true);

		Object target = new TestClass();
		Relatable ti = ip.newProxyInstance(target);

		ti.get(TEST_STRINGS).add("TEST1");
		ti.get(TEST_STRINGS).add("TEST2");
		ti.get(TEST_STRINGS).add("TEST3");

		assertEquals(3, ti.get(TEST_STRINGS).size());

		// try again without relation support enabled
		ip = new InterceptionProxy<RelationInterface>(RelationInterface.class,
			false);
		ti = ip.newProxyInstance(target);

		try {
			ti.get(TEST_STRINGS).add("TEST1");
			fail();
		} catch (Exception e) {
			// this is the correct execution path
		}
	}

	/**
	 * Test interface for object relations that directly implements
	 * RelationEnabled.
	 *
	 * @author eso
	 */
	public interface RelationInterface extends TestInterface, Relatable {
	}

	/**
	 * Interface InterceptorIF.
	 */
	public interface TestInterface {

		/**
		 * Method close.
		 */
		void close();

		/**
		 * Method open.
		 */
		void open();

		/**
		 * Method other.
		 */
		void other();
	}

	/**
	 * Class TestClass.
	 */
	public class TestClass {

		/**
		 * Method close.
		 */
		public void close() throws Exception {
			throw new Exception();
		}

		/**
		 * Method open.
		 */
		public void open() {
		}

		/**
		 * Method other.
		 */
		public void other() {
		}
	}
}
