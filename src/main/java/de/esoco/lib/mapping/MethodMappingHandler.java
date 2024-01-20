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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Default implementation of a mapping of methods and their arguments and return
 * values. Subclasses can override some or all of the methods to provide
 * specific mappings. The central method is invoke(), it will be invoked from
 * the similar method of MethodMappingDefinition to perform a method call.
 * invoke() will call the mapping methods to perform the mapping of method
 * names, argument values, etc.
 *
 * @see #invoke(Method, Object, Object[])
 */
public class MethodMappingHandler {

	private MethodMappingDefinition mapping = null;

	private String targetMethod = null;

	/**
	 * Creates a new default MethodMappingHandler object that maps to a method
	 * with the same name if the mapping methods are not overridden.
	 */
	public MethodMappingHandler() {
	}

	/**
	 * Internal constructor to create a new MethodMappingHandler object that
	 * maps to a method with another name.
	 *
	 * @param targetMethod The target method to be invoked (may be NULL)
	 */
	public MethodMappingHandler(String targetMethod) {
		this.targetMethod = targetMethod;
	}

	/**
	 * Internal constructor for a child handler of a certain mapping
	 * definition.
	 *
	 * @param mapping The parent mapping definition
	 */
	public MethodMappingHandler(MethodMappingDefinition mapping) {
		setMappingDefinition(mapping);
	}

	/**
	 * Returns a string representation of this mapping handler.
	 *
	 * @return A descriptive string
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + targetMethod + "]";
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
	 * @param target         The target object of the invocation
	 * @param args           The arguments of the method call
	 * @return The return value of the method call; this must match the return
	 * type of the method (NULL for void methods)
	 * @throws Exception Any exception may be thrown if either the method
	 *                   invocation or the mapping of arguments or the return
	 *                   value fails
	 * @see #mapTarget(Method, Object, Object[])
	 * @see #mapMethod(Method, String, Object)
	 * @see #mapArguments(Method, Object, Method, Object[])
	 * @see #mapReturnValue(Method, Object, Method, Object)
	 */
	protected Object invoke(Method originalMethod, Object target,
		Object[] args)
		throws Exception {
		try {
			target = mapTarget(originalMethod, target, args);

			Method method = mapMethod(originalMethod, targetMethod, target);
			args = mapArguments(originalMethod, target, method, args);
			Object value = invokeMethod(target, method, args);
			return mapReturnValue(originalMethod, target, method, value);
		} catch (InvocationTargetException tE) {
			Throwable t = tE.getTargetException();

			if (t instanceof Exception) {
				throw (Exception) t;
			} else {
				throw (Error) t;
			}
		}
	}

	/**
	 * Invokes a method on the target object. This method is invoked internally
	 * after the method and the argument have been mapped. It may be overloaded
	 * by subclasses to redirect the actual invocation. The returned value will
	 * then be mapped with mapReturnValue() before it is returned to the
	 * caller.
	 *
	 * @param target       The target object to invoke the method on
	 * @param targetMethod The method to be invoked
	 * @param args         The arguments of the method call
	 * @return The result of the method call
	 * @throws Exception Any exception may be caused by the invocation
	 */
	protected Object invokeMethod(Object target, Method targetMethod,
		Object[] args) throws Exception {
		return targetMethod.invoke(target, args);
	}

	/**
	 * Maps the argument types of a method to the count and type required by a
	 * different target method. The default implementation checks for each
	 * argument type if a global datatype converter is defined in the mapping.
	 * If so, it converts the datatype with it, else it returns the orginal
	 * datatype.
	 *
	 * @param originalMethod The original method from wich the types shall be
	 *                       mapped
	 * @param argTypes       The original argument types
	 * @param target         The target object that will be invoked
	 * @return An array of Class instances containing the (optionally mapped)
	 * argument types
	 */
	protected Class<?>[] mapArgumentTypes(Method originalMethod,
		Class<?>[] argTypes, Object target) {
		for (int i = 0; i < argTypes.length; i++) {
			MethodDatatypeMapper mapper =
				mapping.getDatatypeMapper(originalMethod, argTypes[i]);

			if (mapper != null) {
				argTypes[i] = mapper.mapType(originalMethod, argTypes[i]);
			}
		}

		return argTypes;
	}

	/**
	 * Maps the original method arguments to another format, value, and/or
	 * count. This will mostly be done in conjunction with the mapping to
	 * another method, therefore the target method as returned by the method
	 * <code>mapMethod()</code> is also given as a convenience argument so that
	 * the subclass implementation doesn't need to recompute the method mapping
	 * or to keep it in temporary variables.
	 *
	 * <p>The implementation may throw a RuntimeException (recommended are
	 * either IllegalArgument or UnsupportedOperation) to signal that an error
	 * occurred during the mapping.</p>
	 *
	 * <p>The default implementation checks for each argument value if a global
	 * datatype converter is defined in the mapping. If so, it converts the
	 * value with it, else it returns the orginal return value.</p>
	 *
	 * @param originalMethod The unmapped method that has been invoked
	 * @param target         The target object to be invoked
	 * @param targetMethod   The target method that will be invoked
	 * @param args           The arguments of the method call
	 * @return An Object array with the (optionally mapped) arguments for the
	 * method call
	 * @see #invoke(Method, Object, Object[])
	 */
	protected Object[] mapArguments(Method originalMethod, Object target,
		Method targetMethod, Object[] args) {
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				Object value = args[i];

				if (value != null) {
					MethodDatatypeMapper mapper =
						mapping.getDatatypeMapper(originalMethod,
							value.getClass());

					if (mapper != null) {
						args[i] =
							mapper.mapValue(target, originalMethod, value);
					}
				}
			}
		}

		return args;
	}

	/**
	 * Returns the mapped method (if a mapping is defined) for the target
	 * object
	 * by looking into the internal mapping table and mapping the method
	 * parameter types my invoking mapParameterTypes(). Subclasses can overload
	 * this method to provide additional mappings.
	 *
	 * <p>If the mapping of a method results in the additional need of mapping
	 * the method arguments this can be done in the separate method <code>
	 * mapArguments()</code> that is invoked by the proxy implementation after
	 * mapMethod() has returned.</p>
	 *
	 * @param originalMethod The unmapped method that has been invoked
	 * @param method         The name of the target method or NULL if the name
	 *                       shall be determined from the original method
	 * @param target         Target object on which the original method should
	 *                       be mapped
	 * @return The mapped method instance
	 * @throws NoSuchMethodException If no method that matches the mapping
	 * could
	 *                               be found
	 * @see #invoke(Method, Object, Object[])
	 * @see #mapArguments(Method, Object, Method, Object[])
	 */
	protected Method mapMethod(Method originalMethod, String method,
		Object target) throws NoSuchMethodException {
		if (method == null) {
			method = originalMethod.getName();
		}

		Class<?>[] paramTypes =
			mapArgumentTypes(originalMethod,
				originalMethod.getParameterTypes(),
				target);

		return target.getClass().getMethod(method, paramTypes);
	}

	/**
	 * Maps the original return value of a method invocation to another value
	 * and/or datatype. The default implementation checks if a global datatype
	 * converter is defined in the mapping. If so, it converts the return value
	 * with it, else it returns the orginal return value.
	 *
	 * <p>The implementation may throw a RuntimeException (recommended are
	 * either IllegalArgument or UnsupportedOperation) to signal that an error
	 * occurred during the mapping.</p>
	 *
	 * @param originalMethod The unmapped method that has been invoked
	 * @param target         The target object
	 * @param targetMethod   The mapped method that returned the value
	 * @param value          The value returned by the target method
	 * @return The (optionally mapped) return value
	 * @see #mapMethod(Method, String, Object)
	 */
	protected Object mapReturnValue(Method originalMethod, Object target,
		Method targetMethod, Object value) {
		if (value != null) {
			MethodDatatypeMapper mapper =
				mapping.getDatatypeMapper(originalMethod, value.getClass());

			if (mapper != null) {
				value = mapper.mapValue(target, originalMethod, value);
			}
		}

		return value;
	}

	/**
	 * Allows subclasses to map the target object of the invocation. This
	 * default implementation simply returns the original target object.
	 *
	 * @param originalMethod The unmapped method that has been invoked
	 * @param target         The target object of the invocation
	 * @param args           The arguments of the method call
	 * @return The mapped target object
	 */
	protected Object mapTarget(Method originalMethod, Object target,
		Object[] args) {
		return target;
	}

	/**
	 * Sets the parent mapping definition that this mapping handler belongs to.
	 *
	 * @param mapping The parent mapping definition
	 */
	void setMappingDefinition(MethodMappingDefinition mapping) {
		this.mapping = mapping;
	}
}
