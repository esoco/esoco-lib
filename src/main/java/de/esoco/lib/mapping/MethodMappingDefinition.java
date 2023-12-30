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
package de.esoco.lib.mapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates a mapping description for methods and method
 * parameters. This allows to implement mappings from certain method calls to
 * others for several purposes, e.g. in proxy classes.
 *
 * @author eso
 */
public class MethodMappingDefinition {

	/**
	 * Target method name constant for a method call that shall be ignored
	 */
	public static final String IGNORE = "IGNORE";

	/**
	 * Special mapping handler instance that ignores a method call
	 */
	private static final MethodMappingHandler IGNORED_CALL_HANDLER =
		new MethodMappingHandler() {
			@Override
			protected Object invoke(Method originalMethod, Object target,
				Object[] args) throws Exception {
				return null;
			}
		};

	/**
	 * The default handler that is used if no other handler has been set
	 */

	private final MethodMappingHandler defaultHandler =
		new MethodMappingHandler(this);

	private final List<MethodDatatypeMapper> datatypeMapping =
		new ArrayList<MethodDatatypeMapper>();

	private final Map<String, MethodMappingHandler> methodMapping =
		new HashMap<String, MethodMappingHandler>();

	/**
	 * Creates a new default mapping definition (for a 1:1 mapping).
	 */
	public MethodMappingDefinition() {
	}

	/**
	 * Creates a new definition from a map containing method mappings. A
	 * mapping
	 * consists of the original method name as the key and either the mapped
	 * method name or a MethodMappingHandler instance as the value.
	 *
	 * @param mappingTable The map containing the method mappings (may be NULL)
	 * @throws IllegalArgumentException If the map contains an invalid entry
	 */
	public MethodMappingDefinition(Map<String, ?> mappingTable) {
		if (mappingTable != null) {
			for (String method : mappingTable.keySet()) {
				Object value = mappingTable.get(method);
				MethodMappingHandler handler;

				if (value instanceof String) {
					if (IGNORE.equals(value)) {
						handler = IGNORED_CALL_HANDLER;
					} else {
						handler = new MethodMappingHandler((String) value);
					}
				} else if (value instanceof MethodMappingHandler) {
					handler = (MethodMappingHandler) value;
				} else {
					throw new IllegalArgumentException(
						"Invalid map entry type: " + value.getClass());
				}

				handler.setMappingDefinition(this);
				methodMapping.put(method, handler);
			}
		}
	}

	/**
	 * Creates a new definition that contains a 1:1 method mapping and a
	 * list of
	 * datatype mappers that will be used for all methods.The datatype mappers
	 * are searched in the order they appear in the list. Therefore, if an
	 * implementation needs to distinguish between specific and more generic
	 * mappings it must make sure that the more specific mappers come first in
	 * the list.
	 *
	 * @param datatypeMapping A list of datatype mappers (may be NULL)
	 * @throws IllegalArgumentException If the map contains an invalid entry
	 */
	public MethodMappingDefinition(List<MethodDatatypeMapper> datatypeMapping) {
		this(null, datatypeMapping);
	}

	/**
	 * Creates a new definition from a map containing method mappings and a
	 * list
	 * of datatype mappers that will be used for all methods. A mapping
	 * consists
	 * of the original method name as the key and either the mapped method name
	 * or a MethodMappingHandler instance as the value.
	 *
	 * <p>The datatype mappers are searched in the order they appear in the
	 * list. Therefore, if an implementation needs to distinguish between
	 * specific and more generic mappings it must make sure that the more
	 * specific mappers come first in the list.</p>
	 *
	 * @param mappingTable    A map containing the method mappings (may be
	 *                        NULL)
	 * @param datatypeMapping A list of datatype mappers (may be NULL)
	 * @throws IllegalArgumentException If the map contains an invalid entry
	 */
	public MethodMappingDefinition(Map<String, ?> mappingTable,
		List<MethodDatatypeMapper> datatypeMapping) {
		this(mappingTable);

		if (datatypeMapping != null) {
			datatypeMapping.addAll(datatypeMapping);
		}
	}

	/**
	 * Checks the global set of datatype mappers if a certain datatype needs
	 * conversion and returns the corresponding converter or NULL.
	 *
	 * @param method   The method for which the datatype shall be mapped
	 * @param datatype The datatype to check for mapping
	 * @return The matching datatype mapper or NULL if no mapping is necessary
	 */
	public MethodDatatypeMapper getDatatypeMapper(Method method,
		Class<?> datatype) {
		for (MethodDatatypeMapper mapper : datatypeMapping) {
			if (mapper.appliesTo(method, datatype)) {
				return mapper;
			}
		}

		return null;
	}

	/**
	 * Returns the mapping handler for a particular method.
	 *
	 * @param method The name of the method to map.
	 * @return The mapping handler for the method name
	 */
	public MethodMappingHandler getMappingHandler(Method method) {
		String name = method.getName();
		MethodMappingHandler handler = methodMapping.get(name);

		if (handler == null) {
			handler = defaultHandler;
			methodMapping.put(name, handler);
		}

		return handler;
	}

	/**
	 * Performs the method invocation or an equivalent action on the target
	 * object and returns the corresponding result. The result's type must
	 * match
	 * the return type of the method and must be NULL for void methods.
	 *
	 * <p>The default implementation will first invoke mapMethod() to obtain
	 * the target method. Then it maps the method arguments by calling
	 * mapArguments(). After invoking the mapped method with the converted
	 * arguments through reflection it maps the return value by invoking
	 * mapReturnValue() and returns it.</p>
	 *
	 * <p>The implementation can signal errors by throwing any kind of
	 * exception.</p>
	 *
	 * @param originalMethod The unmapped method that has been invoked
	 * @param target         The target object of the method mapping proxy
	 * @param args           The arguments of the method call
	 * @return The return value of the method call; this must match the return
	 * type of the method (NULL for void methods)
	 * @throws Exception Any exception may be thrown if either the method
	 *                   invocation or the mapping of arguments or the return
	 *                   value fails
	 */
	public Object invoke(Method originalMethod, Object target, Object[] args)
		throws Exception {
		MethodMappingHandler handler = getMappingHandler(originalMethod);

		return handler.invoke(originalMethod, target, args);
	}

	/**
	 * Merges the method and datatype mappings of another mapping definition
	 * into this one. Only mappings that don't exist already in this instance
	 * will me added, existing mappings won't be changed.
	 *
	 * @param other The other mapping to merge into this
	 */
	public void merge(MethodMappingDefinition other) {
		for (MethodDatatypeMapper mapper : other.datatypeMapping) {
			if (!datatypeMapping.contains(mapper)) {
				datatypeMapping.add(mapper);
			}
		}

		for (String s : other.methodMapping.keySet()) {
			if (!methodMapping.containsKey(s)) {
				methodMapping.put(s, other.methodMapping.get(s));
			}
		}
	}
}
