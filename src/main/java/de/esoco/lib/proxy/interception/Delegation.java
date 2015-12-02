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

import de.esoco.lib.mapping.MethodMappingDefinition;

import java.lang.reflect.Method;


/********************************************************************
 * An interception implementation that delegates intercepted method calls to the
 * target object of a {@link InterceptionProxy} instance. The default
 * implementation uses a 1:1 mapping and invokes equally named and parameterized
 * methods of the target object. It is possible so define another mapping by
 * creating a new instance with a specific MethodMappingDefinition instance.
 */
public class Delegation implements Interception
{
	//~ Instance fields --------------------------------------------------------

	MethodMappingDefinition rMapping;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Default constructor that uses a 1:1 mapping.
	 */
	public Delegation()
	{
		this(new MethodMappingDefinition());
	}

	/***************************************
	 * Creates a new delegation with a specific mapping of methods.
	 *
	 * @param rMapping The mapping definition
	 */
	public Delegation(MethodMappingDefinition rMapping)
	{
		this.rMapping = rMapping;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * @see Interception#invoke(Object, Method, Object, Object[])
	 */
	@Override
	public Object invoke(Object   rProxy,
						 Method   rOriginalMethod,
						 Object   rTarget,
						 Object[] rArgs) throws Throwable
	{
		return rMapping.invoke(rOriginalMethod, rTarget, rArgs);
	}
}
