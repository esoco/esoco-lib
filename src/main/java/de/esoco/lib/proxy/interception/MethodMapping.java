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
 * Extended delegation interception that implements a mapping of methods by
 * name. The actual mapping is performed by a MethodMappingDefinition instance.
 *
 * @author eso
 */
public class MethodMapping implements Interception {

	private final MethodMappingDefinition mapping;

	/**
	 * Creates a new method mapping instance.
	 *
	 * @param mapping The mapping definition
	 */
	public MethodMapping(MethodMappingDefinition mapping) {
		this.mapping = mapping;
	}

	/**
	 * @see de.esoco.lib.proxy.interception.Interception#invoke(Object,
	 * java.lang.reflect.Method, java.lang.Object, java.lang.Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method originalMethod, Object target,
		Object[] args) throws Exception {
		return mapping.invoke(originalMethod, target, args);
	}
}
