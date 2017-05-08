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
package de.esoco.lib.datatype;

import junit.framework.TestCase;

import de.esoco.lib.datatype.test.OtherPackageEnum;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/********************************************************************
 * Test case.
 *
 * @author eso
 */
public class EnumTest extends TestCase
{
	//~ Static fields/initializers ---------------------------------------------

	static final EnumConstant TEST_CONSTANT1 =
		new EnumConstant("TEST_CONSTANT1");
	static final EnumConstant TEST_CONSTANT2 =
		new EnumConstant("TEST_CONSTANT2");

	static
	{
		assert GenericEnum.assertEnumInstances(EnumTest.class);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test enum compare.
	 */
	public void testEnumCompare()
	{
		assertNotSame(TestEnum1.ENUM2, TestEnum2.ENUM2);
		assertNotSame(TestEnum1.ENUM1, TestEnum1b.ENUM1);
		assertSame(TestEnum1.ENUM1, TestEnum1a.ENUM1);
		assertSame(TestEnum1.ENUM2, TestEnum1b.ENUM2);
	}

	/***************************************
	 * Test Enum functions.
	 */
	public void testEnumFunction()
	{
		assertEquals(1, FuncEnum.FUNC1.func());
		assertEquals(2, FuncEnum.FUNC2.func());
	}

	/***************************************
	 * Test next() and previous() methods.
	 */
	public void testEnumNextPrevious()
	{
		assertEquals(TestEnumNP.NP1, TestEnumNP.NP2.previous(false));
		assertEquals(TestEnumNP.NP1, TestEnumNP.NP2.previous(true));

		assertEquals(TestEnumNP.NP3, TestEnumNP.NP2.next(false));
		assertEquals(TestEnumNP.NP3, TestEnumNP.NP2.next(true));

		assertEquals(null, TestEnumNP.NP1.previous(false));
		assertEquals(TestEnumNP.NP3, TestEnumNP.NP1.previous(true));

		assertEquals(null, TestEnumNP.NP3.next(false));
		assertEquals(TestEnumNP.NP1, TestEnumNP.NP3.next(true));

		TestEnumNPSub nps = TestEnumNPSub.NPS2;

		nps = nps.previous(false);

		assertEquals(TestEnumNPSub.NPS1, nps);
		assertEquals(TestEnumNPSub.NP2, TestEnumNPSub.NP1.next(false));
	}

	/***************************************
	 * Test enum serialization.
	 *
	 * @throws IOException            IO error
	 * @throws ClassNotFoundException Deserialization
	 */
	public void testEnumSerialization() throws IOException,
											   ClassNotFoundException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream    oos = new ObjectOutputStream(bos);

		oos.writeObject(TestEnum1.ENUM1);
		oos.writeObject(TestEnum1a.ENUM1);
		oos.writeObject(TestEnum1a.ENUM1A);
		oos.writeObject(TestEnum1b.ENUM1);
		oos.writeObject(TestEnum2.ENUM2);
		oos.writeObject(FuncEnum.FUNC1);
		oos.writeObject(FuncEnum.FUNC2);
		oos.writeObject(OtherPackageEnum.PACKAGE_ENUM);
		oos.close();

		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ObjectInputStream    ois = new ObjectInputStream(bis);

		Object enum1 = ois.readObject();
		Object enum2 = ois.readObject();

		assertSame(enum1, TestEnum1.ENUM1);
		assertSame(enum2, TestEnum1a.ENUM1);
		assertSame(enum1, enum2);

		enum1 = ois.readObject();
		assertSame(enum1, TestEnum1a.ENUM1A);

		enum1 = ois.readObject();
		assertSame(enum1, TestEnum1b.ENUM1);

		enum1 = ois.readObject();
		assertSame(enum1, TestEnum2.ENUM2);

		enum1 = ois.readObject();
		assertSame(enum1, FuncEnum.FUNC1);

		enum1 = ois.readObject();
		assertSame(enum1, FuncEnum.FUNC2);

		enum1 = ois.readObject();
		assertSame(enum1, OtherPackageEnum.PACKAGE_ENUM);
	}

	/***************************************
	 * Test Enum functions.
	 */
	public void testEnumValues()
	{
		GenericEnum<?>[] aEnums =
			new GenericEnum<?>[]
			{
				TestEnum1.ENUM1, TestEnum1.ENUM2, TestEnum1a.ENUM1A
			};

		@SuppressWarnings("unused")
		GenericEnum<?>[] aInitEnums =
			new GenericEnum<?>[]
			{
				TestEnum1b.ENUM1, TestEnum2.ENUM2, FuncEnum.FUNC1,
				FuncEnum.FUNC2, OtherPackageEnum.PACKAGE_ENUM
			};

		assertContainsExactly(GenericEnum.getValues(TestEnum1.class), aEnums);
		assertOrder(GenericEnum.getValues(TestEnum1.class), aEnums);
		assertContainsExactly(GenericEnum.getValues(TestEnum1a.class), aEnums);

		// only TestEnum1b.ENUM1 will be returned by getValues because of
		// overridden method getEnumBaseClass although ENUM2 of TestEnum1 is
		// visible in TestEnum1b too; this is a test related special case and
		// not a designated usage scenario
		assertContainsExactly(GenericEnum.getValues(TestEnum1b.class),
							  TestEnum1b.ENUM1);
		assertContainsExactly(GenericEnum.getValues(TestEnum2.class),
							  TestEnum2.ENUM2);
		assertContainsExactly(GenericEnum.getValues(FuncEnum.class),
							  FuncEnum.FUNC1,
							  FuncEnum.FUNC2);
		assertContainsExactly(GenericEnum.getValues(OtherPackageEnum.class),
							  OtherPackageEnum.PACKAGE_ENUM);
		assertEquals(0, GenericEnum.getValues(TestEnum1c.class).size());
		assertEquals(0, GenericEnum.getValues(TestEnum3.class).size());
	}

	/***************************************
	 * Checks if a collection contains all and only the elements from an Enum
	 * array.
	 *
	 * @param rActual   The collection
	 * @param rExpected The Enum array
	 */
	private void assertContainsExactly(
		Collection<GenericEnum<?>> rActual,
		GenericEnum<?>... 		   rExpected)
	{
		List<GenericEnum<?>> rExpectedList = Arrays.asList(rExpected);

		assertEquals(rExpected.length, rActual.size());

		for (Object o : rActual)
		{
			assertTrue(rExpectedList.contains(o));
		}

		for (Object o : rExpectedList)
		{
			assertTrue(rActual.contains(o));
		}
	}

	/***************************************
	 * Checks if a collection contains enum elements in the same order as an
	 * array.
	 *
	 * @param rActual   The collection
	 * @param rExpected The Enum array
	 */
	private void assertOrder(
		Collection<GenericEnum<?>> rActual,
		GenericEnum<?>... 		   rExpected)
	{
		int i = 0;

		for (Object o : rActual)
		{
			assertEquals(o, rExpected[i++]);
		}
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * Function Enum.
	 */
	@SuppressWarnings("serial")
	static abstract class FuncEnum extends GenericEnum<FuncEnum>
	{
		//~ Static fields/initializers -----------------------------------------

		static final FuncEnum FUNC1 =
			new FuncEnum("FUNC1")
			{
				@Override
				public int func()
				{
					return 1;
				}
			};

		static final FuncEnum FUNC2 =
			new FuncEnum("FUNC2")
			{
				@Override
				public int func()
				{
					return 2;
				}
			};

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor.
		 *
		 * @param rName
		 */
		public FuncEnum(String rName)
		{
			super(rName);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Abstract function method.
		 *
		 * @return int
		 */
		protected abstract int func();

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected Class<FuncEnum> getEnumBaseClass()
		{
			return FuncEnum.class;
		}
	}

	/********************************************************************
	 * Test enum constant for enum assertion test.
	 */
	static class EnumConstant extends GenericEnum<EnumConstant>
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new EnumConstant instance.
		 *
		 * @param sName
		 */
		protected EnumConstant(String sName)
		{
			super(sName);
		}
	}

	/********************************************************************
	 * Test Enum 2.
	 */
	static class TestEnum1 extends GenericEnum<TestEnum1>
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		/** ENUM1 */
		public static final TestEnum1 ENUM1 = new TestEnum1("ENUM1");

		/** ENUM2 */
		public static final TestEnum1 ENUM2 = new TestEnum1("ENUM2");

		//~ Instance fields ----------------------------------------------------

		// instance field to make sure it is not checked by enum verification
		@SuppressWarnings("unused")
		private int nTestField = 0;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor.
		 *
		 * @param rName
		 */
		TestEnum1(String rName)
		{
			super(rName);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Overridden to set base.
		 *
		 * @see GenericEnum#getEnumBaseClass()
		 */
		@Override
		protected Class<? extends GenericEnum<?>> getEnumBaseClass()
		{
			return TestEnum1.class;
		}
	}

	/********************************************************************
	 * Test Enum 1a, derived from Enum 1.
	 */
	static class TestEnum1a extends TestEnum1
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		/** ENUM1A */
		public static final TestEnum1a ENUM1A = new TestEnum1a("ENUM1A");

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor.
		 *
		 * @param rName
		 */
		private TestEnum1a(String rName)
		{
			super(rName);
		}
	}

	/********************************************************************
	 * Test Enum 1b, derived from Enum 1, with own {@link #getEnumBaseClass()}
	 * method.
	 */
	static class TestEnum1b extends TestEnum1
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		/**
		 * Overrides ENUM1 from base class TestEnum1; ENUM2 from it will also be
		 * visible but not part of the value set of this subclass. This is just
		 * a test case that should normally not be reflected in real
		 * applications
		 */
		public static final TestEnum1b ENUM1 = new TestEnum1b("ENUM1");

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor.
		 *
		 * @param rName
		 */
		private TestEnum1b(String rName)
		{
			super(rName);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Overridden to set base.
		 *
		 * @return TestEnum1.class
		 */
		@Override
		protected Class<? extends GenericEnum<?>> getEnumBaseClass()
		{
			return TestEnum1b.class;
		}
	}

	/********************************************************************
	 * Test Enum 1c, derived from Enum 1, with own getGenericEnumClass() method
	 * and without constants (for getValues test).
	 */
	static class TestEnum1c extends TestEnum1
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor.
		 *
		 * @param rName
		 */
		TestEnum1c(String rName)
		{
			super(rName);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Overridden to set base.
		 *
		 * @return TestEnum1.class
		 */
		@Override
		protected Class<? extends GenericEnum<?>> getEnumBaseClass()
		{
			return TestEnum1c.class;
		}
	}

	/********************************************************************
	 * Test Enum 2.
	 */
	static class TestEnum2 extends GenericEnum<TestEnum2>
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		/** ENUM2 */
		public static final TestEnum2 ENUM2 = new TestEnum2("ENUM2");

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor
		 *
		 * @param rName
		 */
		private TestEnum2(String rName)
		{
			super(rName);
		}
	}

	/********************************************************************
	 * Test Enum 3 without constants (for getValues test).
	 */
	static class TestEnum3 extends GenericEnum<TestEnum3>
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor
		 *
		 * @param rName
		 */
		TestEnum3(String rName)
		{
			super(rName);
		}
	}

	/********************************************************************
	 * Test enum for next()/previous().
	 */
	static class TestEnumNP<E extends TestEnumNP<E>> extends GenericEnum<E>
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		/** NP1 */
		@SuppressWarnings("rawtypes")
		public static final TestEnumNP NP1 = new TestEnumNP("NP1");

		/** NP2 */
		@SuppressWarnings("rawtypes")
		public static final TestEnumNP NP2 = new TestEnumNP("NP2");

		/** NP3 */
		@SuppressWarnings("rawtypes")
		public static final TestEnumNP NP3 = new TestEnumNP("NP3");

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor
		 *
		 * @param rName
		 */
		TestEnumNP(String rName)
		{
			super(rName);
		}
	}

	/********************************************************************
	 * Test enum subclass for next()/previous().
	 */
	static class TestEnumNPSub extends TestEnumNP<TestEnumNPSub>
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		/** NPS1 */
		public static final TestEnumNPSub NPS1 = new TestEnumNPSub("NPS1");

		/** NPS2 */
		public static final TestEnumNPSub NPS2 = new TestEnumNPSub("NPS2");

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor
		 *
		 * @param rName
		 */
		private TestEnumNPSub(String rName)
		{
			super(rName);
		}
	}
}
