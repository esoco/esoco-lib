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


/********************************************************************
 * Extended delegation interception that implements a mapping of methods by
 * name. The actual mapping is performed by a MethodMappingDefinition instance.
 *
 * @author eso
 */
public class MethodMapping implements Interception
{
	//~ Instance fields --------------------------------------------------------

	private final MethodMappingDefinition rMapping;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new method mapping instance.
	 *
	 * @param rMapping The mapping definition
	 */
	public MethodMapping(MethodMappingDefinition rMapping)
	{
		this.rMapping = rMapping;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * @see de.esoco.lib.proxy.interception.Interception#invoke(Object,
	 *      java.lang.reflect.Method, java.lang.Object, java.lang.Object[])
	 */
	@Override
	public Object invoke(Object   rProxy,
						 Method   rOriginalMethod,
						 Object   rTarget,
						 Object[] rArgs) throws Exception
	{
		return rMapping.invoke(rOriginalMethod, rTarget, rArgs);
	}
}
