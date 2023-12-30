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

import de.esoco.lib.reflect.ReflectUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract Interception implementation that maps intercepted methods to
 * equally named methods in a subclass. The default implementation of the
 * {@link #invoke(Object, Method, Object, Object[]) invoke()} method uses
 * reflection to dispatch method calls to subclass methods. Therefore subclasses
 * need to implement the intercepted methods with the same name and parameter
 * list, plus two additional parameters as the first and second argument. These
 * parameters will be the invoked proxy object and the target object,
 * respectively.
 *
 * <p><b>Attention:</b> because of restrictions in the use of reflection the
 * invoked subclass and the methods to be invoked <b>must</b> be publicly
 * accessible, else an IllegalAccessException will be thrown. This means
 * especially that is not possible to create anonymous inner classes (which are
 * implicitly private) as subclasses of MethodInterception which use the
 * dispatch mechanism.</p>
 *
 * <p>The existence of matching methods in the subclass will be checked in the
 * constructor or when additional methods are added through the corresponding
 * methods. In general it is recommended to use these methods only during
 * instance initialisation (i.e. from the subclass constructors). If no matching
 * method can be found an IllegalArgumentException will be thrown. That means
 * that the validity of an interception mapping will be verified as soon as an
 * instance of the subclass is created. If instances are not always created
 * automatically by an application the developer should make sure that an
 * interception implementation is instatiated at least during testing (e.g. in a
 * unit test).</p>
 *
 * <p>The two additional parameters of the subclass interception methods may be
 * of any type but must represent the concrete type (or one of it's supertypes)
 * that will be used at runtime. That means that there is a risk of runtime
 * errors when a type mismatch occurs. If in doubt a developer should use the
 * class Object (or another generic supertype) as the argument type for both
 * parameters. Alternatively an implementation may choose to override the method
 * {@link #convertArgTypes(Method)} to change the argument mapping for some or
 * all methods.</p>
 *
 * <p>If a subclass wants to modify the parameter list or other aspects of all
 * or certain method calls it can override the invoke() method to implement the
 * necessary conversions. If an implementation needs to change the mapping of
 * methods it can do so by overriding the {@link #addMethod(Method)}
 * method.</p>
 *
 * @author eso
 */
public abstract class MethodInterception implements Interception {

	private final Map<Method, Method> methodMap = new HashMap<Method,
		Method>();

	/**
	 * Creates a new instance that intercepts one or more methods. If no
	 * methods
	 * are provided the subclass implementation can set the intercepted methods
	 * by invoking one of the methods {@link #addMethod(Method)} or
	 * {@link #addMethods(Collection, boolean)}.
	 *
	 * <p>An assertion will be thrown at development time if the variable
	 * parameter list is empty or NULL.</p>
	 *
	 * @param methods The methods to intercept
	 * @throws IllegalArgumentException If no matching method for an argument
	 *                                  exception exists in this class
	 */
	public MethodInterception(Method... methods) {
		assert methods != null && methods.length > 0;

		addMethods(Arrays.asList(methods), true);
	}

	/**
	 * Creates a new instance that intercepts all public methods declared by a
	 * particular class. This does not include methods declared by any
	 * superclass of the argument class. Subclasses can add additional public
	 * methods from other classes by means of one of the addMethod() methods.
	 *
	 * @param type The class to intercept the public methods of
	 * @throws IllegalArgumentException If for a certain intercepted method no
	 *                                  matching method exists in this class
	 */
	public MethodInterception(Class<?> type) {
		addMethods(Arrays.asList(type.getDeclaredMethods()), true);
	}

	/**
	 * Creates a new instance that intercepts certain methods declared by a
	 * particular class. For each method name all methods will that name
	 * will be
	 * intercepted. That means, if the intercepted class has overloaded methods
	 * with distinct parameter lists the subclass must implement corresponding
	 * methods for each variant. Else an IllegalArgumentException will be
	 * thrown.
	 *
	 * <p>This constructor will check the existence of the corresponding
	 * methods in the intercepted class as well as in this (sub)class. In the
	 * latter case two additional parameters are expected (for details see the
	 * {@linkplain MethodInterception class documentation}).</p>
	 *
	 * @param type    The class to intercept the public methods of
	 * @param methods The names of the intercepted methods
	 * @throws IllegalArgumentException If for one of the given method names
	 *                                  either no intercepted method exists in
	 *                                  the given class or no matching
	 *                                  method(s)
	 *                                  exists in this class
	 */
	public MethodInterception(Class<?> type, String... methods) {
		// assert that with no string params the class param constructor is
		// invoked
		assert methods.length > 0;

		for (String method : methods) {
			addMethod(type, method);
		}
	}

	/**
	 * Overridden to dispatch the invocation to an equal-named method in this
	 * interception instance, but with two additional first arguments which
	 * contain a reference to the EWT proxy and the implementation target
	 * object, respectively.
	 *
	 * @see Interception#invoke(Object, Method, Object, Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method originalMethod, Object target,
		Object[] args) throws Exception {
		Method method = methodMap.get(originalMethod);
		Object[] extArgs;

		if (args != null) {
			extArgs = new Object[args.length + 2];
			System.arraycopy(args, 0, extArgs, 2, args.length);
		} else {
			extArgs = new Object[2];
		}

		extArgs[0] = proxy;
		extArgs[1] = target;

		return ReflectUtil.invoke(this, method, extArgs);
	}

	/**
	 * Returns a string description of this instance.
	 *
	 * @return A string description
	 */
	@Override
	public String toString() {
		return "MethodInterception" + methodMap.keySet();
	}

	/**
	 * Adds a method mapping by searching for a public method in this class
	 * that
	 * matches the argument method. The target method must have the same name
	 * and parameter lisdt.
	 *
	 * @param method The name of the public method to add
	 * @throws IllegalArgumentException If no target method with the given name
	 *                                  exists
	 */
	protected void addMethod(Method method) {
		Method targetMethod = mapMethod(method);

		if (targetMethod == null) {
			throw new IllegalArgumentException(
				"Missing target method: " + method);
		}

		methodMap.put(method, targetMethod);
	}

	/**
	 * Adds all public methods with a certain name to this interception. If the
	 * intercepted class has overloaded methods with distinct parameter lists
	 * the subclass must implement corresponding methods for each variant. Else
	 * an IllegalArgumentException will be thrown.
	 *
	 * @param type   The class to intercept the public methods of
	 * @param method The name of the intercepted method(s)
	 */
	protected void addMethod(Class<?> type, String method) {
		List<Method> ml = ReflectUtil.getPublicMethods(type, method);

		addMethods(ml, false);
	}

	/**
	 * Adds all public methods declared by a particular class. This does not
	 * include methods declared by any superclass of the argument class. These
	 * classes must be added separately if necessary.
	 *
	 * @param type The class to add the public methods of
	 */
	protected void addMethods(Class<?> type) {
		addMethods(Arrays.asList(type.getDeclaredMethods()), true);
	}

	/**
	 * Adds all methods from an array to this interception.
	 *
	 * @param methods    The array of methods to add
	 * @param publicOnly If TRUE, only public methods will be added; else any
	 *                   method in the array will be added
	 */
	protected void addMethods(Collection<Method> methods, boolean publicOnly) {
		for (Method m : methods) {
			if (!publicOnly || Modifier.isPublic(m.getModifiers())) {
				addMethod(m);
			}
		}
	}

	/**
	 * Converts the list of argument types of a method for the lookup of the
	 * corresponding subclass method. The default implementation adds two
	 * additional first parameters for the proxy and the target object,
	 * respectively.
	 *
	 * @param method The method to convert the argument types of
	 * @return A class array containing the converted type list
	 */
	protected Class<?>[] convertArgTypes(Method method) {
		Class<?>[] argTypes = method.getParameterTypes();
		Class<?>[] extArgTypes = new Class[argTypes.length + 2];

		// leaving position 0 & 1 of extArgTypes at null will allow any type
		System.arraycopy(argTypes, 0, extArgTypes, 2, argTypes.length);

		return extArgTypes;
	}

	/**
	 * Maps a source method to a target method in this class that has two
	 * additional parameters. May be overridden by subclasses to implement
	 * different mappings. Invoked from {@link #addMethod(Method)}.
	 *
	 * @param method The source method to map
	 * @return The mapped target method or NULL if no matching method could be
	 * found
	 */
	protected Method mapMethod(Method method) {
		Class<?>[] extArgTypes = convertArgTypes(method);

		return ReflectUtil.findPublicMethod(getClass(), method.getName(),
			extArgTypes);
	}

	/**
	 * Returns the methods this instance is registered to intercept. A package
	 * internal method that allows the Interception proxy to access the
	 * methods.
	 *
	 * @return A collection containing the intercepted methods
	 */
	Map<Method, Method> getMethodMap() {
		return methodMap;
	}
}
