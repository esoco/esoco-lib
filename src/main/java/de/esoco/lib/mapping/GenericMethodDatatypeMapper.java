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
package de.esoco.lib.mapping;

import de.esoco.lib.reflect.ReflectUtil;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Generic implementation of the MethodDatatypeMapper interface that is
 * based on a mapping table of datatype classes. It is abstract because the
 * mapping of values must be implemented by subclasses. The mapping methods will
 * be invoked through reflection. So, to provide a certain mapping a subclass
 * needs to do the following:</p>
 *
 * <ol>
 *   <li>Register the supported datatype classes with one of the methods
 *     addMapping() or addOneWayMapping(), preferrably in the constructor</li>
 *   <li>For each registered datatype, provide two methods (only one for one-way
 *     mappings) for the datatype mappings with a name and signature as
 *     described in the comment of the mapValue() method</li>
 * </ol>
 *
 * <p>This implementation also takes into account datatype inheritance
 * hierarchies. That means that a mapping will be detected even if the classes
 * added with the addMapping() method are not the same as the types that are
 * used as the runtime arguments to the MethodDatatypeMapper methods. The
 * implementation will then search the superclasses of the argument types until
 * it finds a mapping for one of these classes. Therefore it would be possible
 * to provide a default mapping handler by adding a mapping for class Object and
 * providing a mapObject() method.</p>
 *
 * @author eso
 * @see #mapValue(Object, Method, Object)
 */
public abstract class GenericMethodDatatypeMapper
	implements MethodDatatypeMapper {

	private final Map<Class<?>, MappedTypes> mapping =
		new HashMap<Class<?>, MappedTypes>();

	/**
	 * @see de.esoco.lib.mapping.MethodDatatypeMapper#appliesTo(Method, Class)
	 */
	@Override
	public boolean appliesTo(Method method, Class<?> datatype) {
		if (!mapping.containsKey(datatype)) {
			initMappedType(datatype);
		}

		return mapping.get(datatype) != null;
	}

	/**
	 * Returns the mapping for a certain datatype class or one of it's
	 * superclasses.
	 *
	 * @see de.esoco.lib.mapping.MethodDatatypeMapper#mapType(Method, Class)
	 */
	@Override
	public Class<?> mapType(Method method, Class<?> datatype) {
		return mapping.get(datatype).targetType;
	}

	/**
	 * Implements a dispatcher that invokes a public method of a subclass to
	 * convert the value. The signature of the method that will be invoked must
	 * be of the following type:
	 *
	 * <pre>
	 * public [MappedType] map[OriginalTypeName](Object, Method, [OriginalType])
	 * </pre>
	 *
	 * <p>Example: the method for the mapping from a.b.OldType to x.y.NewType
	 * would be</p>
	 *
	 * <pre>public x.y.NewType mapOldType(Object target, Method method,
	 * a.b.OldType)</pre>
	 *
	 * <p>The return value of the method may also be of another type, e.g. if
	 * the implementation distinguishes between different methods that map to
	 * the same datatype in the source but different ones in the target
	 * implementations.</p>
	 *
	 * <p>The method may also be static, e.g. if the application needs to
	 * invoke the mapping methods directly and doesn't want to create a
	 * separate
	 * instance.</p>
	 *
	 * @throws IllegalArgumentException If the target method for the
	 * dispatching
	 *                                  could not be found
	 * @see de.esoco.lib.mapping.MethodDatatypeMapper#mapValue(Object, Method,
	 * Object)
	 */
	@Override
	public Object mapValue(Object target, Method method, Object value) {
		Class<?> sourceType = mapping.get(value.getClass()).sourceType;

		String methodName = "map" + sourceType.getSimpleName();

		return ReflectUtil.invokePublic(this, methodName,
			new Object[] { target, method, value },
			new Class[] { Object.class, Method.class, sourceType });
	}

	/**
	 * Adds a two-way mapping to the internal mapping table.
	 *
	 * @param first  The first datatype to map
	 * @param second The second datatype to map
	 */
	protected final void addMapping(Class<?> first, Class<?> second) {
		addOneWayMapping(first, second);
		addOneWayMapping(second, first);
	}

	/**
	 * Adds a one-way mapping to the internal mapping table. This will only map
	 * the source datatype to the target datatype, but not vice versa.
	 *
	 * @param source The source datatype
	 * @param target The target datatype
	 */
	protected final void addOneWayMapping(Class<?> source, Class<?> target) {
		assert source != null;
		assert target != null;
		mapping.put(source, new MappedTypes(source, target));
	}

	/**
	 * Internal method that initializes the mapping for a certain datatype
	 * class
	 * that has not been defined explicitly. If a mapping exist for either a
	 * superclass, an interface, or a super-interface of the datatype class
	 * that
	 * mapping will be duplicated for the datatype class. If no such mapping
	 * could be found a mapping to NULL will be added to signal internally that
	 * no mapping exists.
	 *
	 * @param datatype The datatype to initialize the mapping of
	 */
	void initMappedType(Class<?> datatype) {
		Class<?> searchType = datatype;
		boolean search = true;

		while (search && searchType != null &&
			!mapping.containsKey(searchType)) {
			// if type unknown check all interfaces
			for (Class<?> interfaceType : searchType.getInterfaces()) {
				// check for NULL (not containsKey!) because NULL values will
				// be registered below to signal undefined mappings
				if (mapping.get(interfaceType) != null) {
					searchType = interfaceType;
					search = false;

					break;
				}
			}

			if (search) {
				// if unsuccessful continue with superclass
				searchType = searchType.getSuperclass();
			}
		}

		if (searchType != null) {
			Class<?> target = mapping.get(searchType).targetType;

			mapping.put(datatype, new MappedTypes(searchType, target));
		} else {
			// map to NULL to indicate that no mapping exists
			mapping.put(datatype, null);
		}
	}

	/**
	 * Internal structure to hold source and target type classes of a mapping.
	 *
	 * @author eso
	 */
	private static class MappedTypes {

		Class<?> sourceType;

		Class<?> targetType;

		/**
		 * Default constructor.
		 */
		public MappedTypes(Class<?> sourceType, Class<?> targetType) {
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return "Mapping: " + sourceType + " <=> " + targetType;
		}
	}
}
