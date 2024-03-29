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
package de.esoco.lib.datatype;

import de.esoco.lib.collection.CollectionUtil;
import de.esoco.lib.logging.Log;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An abstract base class that provides a generic implementation of the
 * extensible, serializable enumeration pattern as described by Joshua Bloch in
 * "Effective Java", Chapter 5, Item 21. Instead of using integer ordinals, each
 * enum instance in the same derived class or class hierarchy must be identified
 * by a unique string. This base class manages the available instances and
 * provides extensibility and serializability for all subclasses. A simple enum
 * class would look like the following:
 *
 * <pre>
 * public class MyEnum extends GenericEnum
 * {
 * public static final MyEnum ENUM1 = new MyEnum("ENUM1");
 * public static final MyEnum ENUM2 = new MyEnum("ENUM2");
 *
 * // private because only used for internal constant creation
 * private MyEnum(String name)
 * {
 * super(name);
 * }
 * }
 * </pre>
 *
 * <p>A simple enum subclass doesn't need to implement any special methods
 * besides a constructor with a string parameter (because the string is needed
 * to identify the distinct enum instances, especially for serialization). Only
 * if a subclass defines it's own abstract interface that will be implemented by
 * (often anonymous) subclasses, it is recommended to make it override the
 * method getEnumBaseClass() and return the base class from it. This will define
 * the same "namespace" for this class and all of it's subclasses. Otherwise
 * subclass instances could be created with equal names which would not harm
 * (serialization would still work) but which could lead to inconsistencies
 * because of developers using wrong enum instances.</p>
 *
 * <p>The Following code shows such a GenericEnum subclass that uses anonymous
 * inner classes to define the actual enum instances:<br>
 * <br>
 * </p>
 *
 * <pre>
 * public abstract class ExampleEnum extends GenericEnum
 * {
 * public static final ExampleEnum EXAMPLE_1 = new ExampleEnum("EXAMPLE_1")
 * {
 * public void enumSpecificMethod()
 * {   // anonymous class with specific implementation
 * }
 * };
 *
 * public ExampleEnum(String name)
 * {
 * super(name);
 * }
 *
 * public abstract void enumSpecificMethod();
 *
 * // final prevents subclasses from overriding, thus "closes" the enum namespace
 * protected final Class getEnumBaseClass()
 * {
 * return ExampleEnum.class;
 * }
 * }
 * </pre>
 *
 * <p><b>Some additional details to consider:</b></p>
 *
 * <ul>
 *   <li>In enum class hierarchies where the base class overrides
 *     getEnumBaseClass(), each further subclass may either implement their own
 *     getEnumBaseClass() method or not. In the first case enum instances in
 *     different subclasses may have equal names, in the latter case not
 *     (because all instances are relative to the base class). If sharing of the
 *     "namespace" shall be forced, a base class can achieve this by declaring
 *     the getEnumBaseClass() method as final.</li>
 *   <li>It is mandatory to use the name of an enum instance as the string name
 *     that is given to the constructor. This uniqueness of instance names is
 *     verified by an assertion in the constructor of this class. Therefore
 *     developers should always have assertions enabled, at least when running
 *     tests that use enumeration classes.</li>
 *   <li>Other than with the original pattern described by Bloch the order in
 *     which new instances appear in a class or class hierarchy has no influence
 *     on the deserialization of enum instances.</li>
 *   <li>The order of enum instances will be the order in which they are
 *     initialized in the class file (from top to bottom). When iterating
 *     through a Collection of values returned by the method {@link
 *     #getValues(Class) getValues()} the enums will be returned in that order.
 *     But an application should not rely on that order because inserting,
 *     changing, or removing instances in future versions of an enum class will
 *     change that order. If an application needs to switch between values based
 *     on their order it can use the convenience methods {@link #next(boolean)
 *     next()} and {@link #previous(boolean) previous()} to do so in a safe way
 *     (besides iterating over the values itself).</li>
 *   <li>This base implementation is serializable and already implements the
 *     Serializable interface. Therefore any subclass should take care to
 *     fulfill the Serializable contract when implementing additional data
 *     fields and features (see [Bloch] for details). Especially, when
 *     deserializing objects subclasses should always invoke or rely on
 *     super.readResolve() from this class because this method will retrieve the
 *     unique singleton instances of an enum subclass.</li>
 * </ul>
 *
 * @author eso
 */
public abstract class GenericEnum<E extends GenericEnum<E>>
	implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Contains the value maps associated with the enum (base) classes
	 */
	private static final Map<String, Map<String, GenericEnum<?>>> enumRegistry =
		new LinkedHashMap<String, Map<String, GenericEnum<?>>>();

	/**
	 * The name of the enum instance. @serial
	 */
	private final String name;

	/**
	 * Default constructor that creates a new enum instance with a certain
	 * name.
	 * This Constructor must be invoked by any subclass to provide the
	 * identifying name of an instance. These names need to be unique for all
	 * instances of a subclass or for all instances of a class hierarchy where
	 * the base class overrides the method getEnumBaseClass().
	 *
	 * <p>An AssertionError will be thrown if an instance with the same name
	 * already exists to prevent programming errors when creating new
	 * instances.
	 * </p>
	 *
	 * @param name The unique name of the new instance
	 * @see #getEnumBaseClass()
	 */
	protected GenericEnum(String name) {
		assert getEnumBaseClass().isAssignableFrom(getClass()) :
			"Wrong enum class hierarchy: " + getClass() +
				" is not a subclass of " + getEnumBaseClass();

		Map<String, GenericEnum<?>> enums =
			getValueMap(getEnumBaseClass(), true);

		assert enums.get(name) == null :
			"Duplicate enum value: " + name + " in class " +
				getClass().getName() + ((getEnumBaseClass() != getClass()) ?
				                        (" (Base: " +
					                        getEnumBaseClass().getName() +
					                        ")") :
				                        "");
		this.name = name;
		enums.put(name, this);

		assert checkEnumStructure();
	}

	/**
	 * Assertion method that verifies the static enum instances of a certain
	 * class. It will iterate over all GenericEnum fields of the argument class
	 * and assert that the fields are "final static" and that their values have
	 * the same name as the field. If not, an assertion will be thrown (if
	 * assertions are enabled). Also non-public fields will be checked. If this
	 * is not possible because of access restrictions a debug message will be
	 * logged.
	 *
	 * <p>This method uses assertions because it is meant to be used to check
	 * the enum structure at development time. the call to it should be put in
	 * the static initializer of a class that declares GenericEnum instances
	 * after the definition of the actual instances. It returns a boolean value
	 * so that it can be used in an assert statement and therefore will only be
	 * evaluated if assertions are enabled (i.e., during development time).</p>
	 *
	 * @param declaringClass The class to check the GenericEnum fields of
	 * @return Always TRUE (assertions in case of an structure error)
	 */
	public static boolean assertEnumInstances(Class<?> declaringClass) {
		checkEnumInstances(declaringClass);

		return true;
	}

	/**
	 * Actual implementation of the {@link #assertEnumInstances(Class)} method.
	 * It returns the last field that is still NULL (during initialization), a
	 * value that is needed by the {@link #checkEnumStructure()} method.
	 *
	 * @param declaringClass The class to check the GenericEnum fields of
	 * @return The last field that is null or NULL for none
	 */
	private static Field checkEnumInstances(Class<?> declaringClass) {
		Field[] fields = declaringClass.getDeclaredFields();
		Field nullField = null;
		int nullCnt = 0;

		for (Field field : fields) {
			// getFields returns only public fields; intentionally, these must
			// all be static, else the following code will fail
			// is field an enum instance?
			if (GenericEnum.class.isAssignableFrom(field.getType())) {
				String name = field.getName();
				int modifier = field.getModifiers();
				Object value = null;

				assert
					Modifier.isFinal(modifier) && Modifier.isStatic(modifier) :
					"Enum instance not final static: " + name;

				try {
					value = field.get(null);
				} catch (Exception e) {
					// if field is not accessible log warning and continue
					Log.debug("Could not assert enum value " + name, e);

					continue;
				}

				if (value != null) {
					GenericEnum<?> genericEnum = (GenericEnum<?>) value;

					// check if enum string and instance names match
					assert genericEnum.name.equals(name) :
						"Name mismatch of GenericEnum " + name +
							" (wrong name: " + genericEnum + ")";
				} else {
					// if value not created yet return at end if last value
					nullCnt++;
					nullField = field;
				}
			}
		}

		return (nullCnt == 1 ? nullField : null);
	}

	/**
	 * Returns the value map for a certain enum class. If the enum is a
	 * subclass
	 * of an enum hierarchy the superclass that defines the hierarchy will be
	 * searched.
	 *
	 * @param enumClass The class to return the value map for
	 * @return The associated value map
	 * @throws IllegalArgumentException If the argument class is invalid
	 */
	private static Map<String, GenericEnum<?>> getValueMap(Class<?> enumClass) {
		// check if a superclass implements the getEnumBaseClass() method;
		// if so, use this class as the key into the map of value maps
		Class<?> base = enumClass;

		while (base != GenericEnum.class) {
			try { // use getDeclaredMethod to provide access to protected
				// methods
				base.getDeclaredMethod("getEnumBaseClass");

				break; // no exception => method exists => base class found
			} catch (NoSuchMethodException e) {
				base = base.getSuperclass();
			}
		}

		if (base != GenericEnum.class) {
			enumClass = base;
		}

		return getValueMap(enumClass, false);
	}

	/**
	 * Internal method to return (and optionally create) the map containing the
	 * distinct values of a particular enum base class.
	 *
	 * @param enumBaseClass The enum base class to return the value map of
	 * @param create        TRUE to create a new if none exists; FALSE to
	 *                         return
	 *                      NULL in such cases
	 * @return A Map containing the values for the given enum base class
	 */
	private static Map<String, GenericEnum<?>> getValueMap(
		Class<?> enumBaseClass, boolean create) {
		String iD = enumBaseClass.getName();

		Map<String, GenericEnum<?>> valueMap = enumRegistry.get(iD);

		if (valueMap == null && create) {
			valueMap = new LinkedHashMap<String, GenericEnum<?>>();
			enumRegistry.put(iD, valueMap);
		}

		return valueMap;
	}

	/**
	 * Returns a new set containing all the distinct values of a certain
	 * GenericEnum subclass. If there no instances of the subclass exist (e.g.
	 * if it is a base class for other Enums) Collection.EMPTY_SET will be
	 * returned.
	 *
	 * @param enumClass The GenericEnum subclass of which the values shall be
	 *                  returned
	 * @return An unmodifiable collection containing the GenericEnum values
	 * @throws IllegalArgumentException If the argument is not an GenericEnum
	 *                                  subclass
	 */
	public static Collection<GenericEnum<?>> getValues(
		Class<? extends GenericEnum<?>> enumClass) {
		Map<String, GenericEnum<?>> valueMap = getValueMap(enumClass);

		if (valueMap != null) {
			return Collections.unmodifiableCollection(valueMap.values());
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Convenience method for subclasses to create an unmodifiable list of
	 * enumerated values. The same as CollectionUtil.fixedListOf(values).
	 * Intended to be used by subclasses to create static lists of instances.
	 *
	 * @param values The enumerated values to put into the list
	 * @return A new and unmodifiable list containing the values
	 */
	@SafeVarargs
	protected static <T extends GenericEnum<T>> List<T> listOf(T... values) {
		return CollectionUtil.fixedListOf(values);
	}

	/**
	 * Convenience method for subclasses to create an unmodifiable set of
	 * enumerated values. The same as CollectionUtil.fixedSetOf(values).
	 * Intended to be used by subclasses to create static groups of instances.
	 *
	 * @param values The enumerated values to put into the set
	 * @return A new and unmodifiable set containing the values
	 */
	@SafeVarargs
	protected static <T extends GenericEnum<T>> Set<T> setOf(T... values) {
		return CollectionUtil.fixedSetOf(values);
	}

	/**
	 * Returns the enum instance for a certain name. Can be used by subclasses
	 * to provide a type-specific valueOf(String) method.
	 *
	 * @param enumClass The generic enum subclass to return the value of
	 * @param name      The name of the instance to return
	 * @return The matching enum instance
	 * @throws IllegalArgumentException If the argument is not an GenericEnum
	 *                                  subclass or if the given name does not
	 *                                  represent an instance of the given enum
	 *                                  class
	 */
	public static GenericEnum<?> valueOf(
		Class<? extends GenericEnum<?>> enumClass, String name) {
		Map<String, GenericEnum<?>> valueMap = getValueMap(enumClass);
		GenericEnum<?> genericEnum = null;

		if (valueMap != null) {
			genericEnum = valueMap.get(name);
		}

		if (genericEnum == null) {
			throw new IllegalArgumentException(
				name + " is not a valid instance name of " + enumClass);
		}

		return genericEnum;
	}

	/**
	 * Overridden as final to prevent subclasses from modifying the Object
	 * functionality.
	 *
	 * @see Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object other) {
		return super.equals(other);
	}

	/**
	 * Overridden as final to prevent subclasses from modifying the Object
	 * functionality.
	 *
	 * @see Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	/**
	 * Returns the next enum instance in the sequence of all enum values of
	 * this
	 * instance's class. The enum values are returned according to the order in
	 * which they have been created.
	 *
	 * @param wrap If FALSE NULL will be returned as the next instance of the
	 *             last value, else the first enum value will be returned
	 * @return The next enum instance
	 */
	@SuppressWarnings("unchecked")
	public E next(boolean wrap) {
		Class<? extends GenericEnum<?>> baseClass = getEnumBaseClass();

		return (E) CollectionUtil.next(getValueMap(baseClass, false).values(),
			this, wrap);
	}

	/**
	 * Returns the previous enum instance in the sequence of all enum values of
	 * this instance's class. The enum values are returned according to the
	 * order in which they have been created.
	 *
	 * @param wrap If FALSE NULL will be returned as the previous instance of
	 *             the first value, else the last enum value will be returned
	 * @return The next enum instance
	 */
	@SuppressWarnings("unchecked")
	public E previous(boolean wrap) {
		Class<? extends GenericEnum<?>> c = getEnumBaseClass();

		return (E) CollectionUtil.previous(getValueMap(c, false).values(),
			this,
			wrap);
	}

	/**
	 * Returns the identifying name of this instance.
	 *
	 * @return The instance name
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Returns the base class of the enum instance. The default implementation
	 * will return the result of a method call to getClass() which will yield
	 * the name of the GenericEnum subclass that the enum instance is created
	 * from.
	 *
	 * <p>For all standard enums the default implementation is sufficient. Only
	 * if the enum instances are based on an abstract class that defines an
	 * enum
	 * specific API and are therefore instances of (anonymous) subclasses of
	 * the
	 * base this method should be overridden. Overriding should then be done in
	 * the base class and return the base class itself. So, if the base class
	 * would be called MyBaseEnum, the code in the overridden method of this
	 * class should be <code>return MyBaseEnum.class;</code></p>
	 *
	 * <p>It doesn't harm to not override this method in a case like the above,
	 * but then the (anonymous inner class) enum instances would be managed
	 * independently of each other by the implemtation in this class. Despite
	 * being more ineffective this would also allow different instances to
	 * share
	 * the same name which could lead to inconsistencies (although
	 * serialization
	 * and comparisons based on the static instances will still work
	 * correctly).
	 * <br>
	 * If a base class overrides this method it's subclasses are free to either
	 * rely on the base method or override it again by themselves.</p>
	 *
	 * @return The base class to associate enum instances with
	 */
	@SuppressWarnings("unchecked")
	protected Class<? extends GenericEnum<?>> getEnumBaseClass() {
		return (Class<? extends GenericEnum<?>>) getClass();
	}

	/**
	 * Returns the enum instance that corresponds to the enum name that has
	 * been
	 * read by the deserialization.
	 *
	 * @return The resolved enum instance
	 * @throws ObjectStreamException If accessing the object stream fails
	 */
	protected final Object readResolve() throws ObjectStreamException {
		return getValueMap(getEnumBaseClass(), true).get(name);
	}

	/**
	 * Internal assertion mechanism that verifies the current enum structure
	 * each time a new instance is created. It is based on the method
	 * {@link #checkEnumInstances(Class)}. If a problem is detected and
	 * assertions are enabled this method will raise an assertion. It returns a
	 * boolean value so that it can be used in an assert statement and
	 * therefore
	 * will only be evaluated if assertions are enabled (i.e., during
	 * development time).
	 *
	 * @return Always TRUE (assertions in case of an structure error)
	 */
	private boolean checkEnumStructure() {
		Field nullField = checkEnumInstances(getClass());

		// if only one uninitialized field left and it's name differs from the
		// currently initialized instance this is an inconsistency
		assert (nullField == null) || nullField.getName().equals(name) :
			"Name mismatch of instance " + nullField.getName() +
				" (wrong name: " + name + ")";

		return true;
	}
}
