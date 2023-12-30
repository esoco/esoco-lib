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

import de.esoco.lib.mapping.MethodMappingDefinition;

import java.lang.reflect.Method;

/**
 * Subclass of the delegation interception that redirects method calls to
 * another object than the proxy's original target. As with delegations it is
 * possible to define a mapping to different methods by providing an instance of
 * MethodMappingDefinition.
 *
 * @author eso
 */
public class Redirection extends Delegation {

	private final Object redirectionTarget;

	/**
	 * Creates a redirection that uses a 1:1 method mapping to invoke the
	 * methods of a different target object.
	 *
	 * @param target The target object to be invoked instead of the proxy's
	 *               original target
	 */
	public Redirection(Object target) {
		redirectionTarget = target;
	}

	/**
	 * Constructor that creates a specific method mapping for the
	 * invocations of
	 * a different target object.
	 *
	 * @param target  The target object to be invoked instead of the proxy's
	 *                original target
	 * @param mapping The mapping definition
	 */
	public Redirection(Object target, MethodMappingDefinition mapping) {
		super(mapping);

		redirectionTarget = target;
	}

	/**
	 * Invokes the superclass' invoke() method, but with this redirection's
	 * target instead of the argument target object.
	 *
	 * @see Delegation#invoke(Object, Method, Object, Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method originalMethod, Object target,
		Object[] args) throws Exception {
		return super.invoke(proxy, originalMethod, redirectionTarget, args);
	}
}
