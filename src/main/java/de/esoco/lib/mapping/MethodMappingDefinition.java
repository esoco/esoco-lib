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


/********************************************************************
 * This class encapsulates a mapping description for methods and method
 * parameters. This allows to implement mappings from certain method calls to
 * others for several purposes, e.g. in proxy classes.
 *
 * @author eso
 */
public class MethodMappingDefinition
{
	//~ Static fields/initializers ---------------------------------------------

	/** Target method name constant for a method call that shall be ignored */
	public static final String IGNORE = "IGNORE";

	/** Special mapping handler instance that ignores a method call */
	private static final MethodMappingHandler IGNORED_CALL_HANDLER =
		new MethodMappingHandler()
		{
			@Override
			protected Object invoke(Method   rOriginalMethod,
									Object   rTarget,
									Object[] rArgs) throws Exception
			{
				return null;
			}
		};

	//~ Instance fields --------------------------------------------------------

	/** The default handler that is used if no other handler has been set */

	private MethodMappingHandler aDefaultHandler =
		new MethodMappingHandler(this);

	private List<MethodDatatypeMapper>		  aDatatypeMapping =
		new ArrayList<MethodDatatypeMapper>();
	private Map<String, MethodMappingHandler> aMethodMapping   =
		new HashMap<String, MethodMappingHandler>();

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new default mapping definition (for a 1:1 mapping).
	 */
	public MethodMappingDefinition()
	{
	}

	/***************************************
	 * Creates a new definition from a map containing method mappings. A mapping
	 * consists of the original method name as the key and either the mapped
	 * method name or a MethodMappingHandler instance as the value.
	 *
	 * @param  rMappingTable The map containing the method mappings (may be
	 *                       NULL)
	 *
	 * @throws IllegalArgumentException If the map contains an invalid entry
	 */
	public MethodMappingDefinition(Map<String, ?> rMappingTable)
	{
		if (rMappingTable != null)
		{
			for (String sMethod : rMappingTable.keySet())
			{
				Object				 rValue   = rMappingTable.get(sMethod);
				MethodMappingHandler rHandler;

				if (rValue instanceof String)
				{
					if (IGNORE.equals(rValue))
					{
						rHandler = IGNORED_CALL_HANDLER;
					}
					else
					{
						rHandler = new MethodMappingHandler((String) rValue);
					}
				}
				else if (rValue instanceof MethodMappingHandler)
				{
					rHandler = (MethodMappingHandler) rValue;
				}
				else
				{
					throw new IllegalArgumentException("Invalid map entry type: " +
													   rValue.getClass());
				}

				rHandler.setMappingDefinition(this);
				aMethodMapping.put(sMethod, rHandler);
			}
		}
	}

	/***************************************
	 * Creates a new definition that contains a 1:1 method mapping and a list of
	 * datatype mappers that will be used for all methods.The datatype mappers
	 * are searched in the order they appear in the list. Therefore, if an
	 * implementation needs to distinguish between specific and more generic
	 * mappings it must make sure that the more specific mappers come first in
	 * the list.
	 *
	 * @param  rDatatypeMapping A list of datatype mappers (may be NULL)
	 *
	 * @throws IllegalArgumentException If the map contains an invalid entry
	 */
	public MethodMappingDefinition(List<MethodDatatypeMapper> rDatatypeMapping)
	{
		this(null, rDatatypeMapping);
	}

	/***************************************
	 * Creates a new definition from a map containing method mappings and a list
	 * of datatype mappers that will be used for all methods. A mapping consists
	 * of the original method name as the key and either the mapped method name
	 * or a MethodMappingHandler instance as the value.
	 *
	 * <p>The datatype mappers are searched in the order they appear in the
	 * list. Therefore, if an implementation needs to distinguish between
	 * specific and more generic mappings it must make sure that the more
	 * specific mappers come first in the list.</p>
	 *
	 * @param  rMappingTable    A map containing the method mappings (may be
	 *                          NULL)
	 * @param  rDatatypeMapping A list of datatype mappers (may be NULL)
	 *
	 * @throws IllegalArgumentException If the map contains an invalid entry
	 */
	public MethodMappingDefinition(
		Map<String, ?>			   rMappingTable,
		List<MethodDatatypeMapper> rDatatypeMapping)
	{
		this(rMappingTable);

		if (rDatatypeMapping != null)
		{
			aDatatypeMapping.addAll(rDatatypeMapping);
		}
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Checks the global set of datatype mappers if a certain datatype needs
	 * conversion and returns the corresponding converter or NULL.
	 *
	 * @param  rMethod   The method for which the datatype shall be mapped
	 * @param  rDatatype The datatype to check for mapping
	 *
	 * @return The matching datatype mapper or NULL if no mapping is necessary
	 */
	public MethodDatatypeMapper getDatatypeMapper(
		Method   rMethod,
		Class<?> rDatatype)
	{
		for (MethodDatatypeMapper rMapper : aDatatypeMapping)
		{
			if (rMapper.appliesTo(rMethod, rDatatype))
			{
				return rMapper;
			}
		}

		return null;
	}

	/***************************************
	 * Returns the mapping handler for a particular method.
	 *
	 * @param  rMethod The name of the method to map.
	 *
	 * @return The mapping handler for the method name
	 */
	public MethodMappingHandler getMappingHandler(Method rMethod)
	{
		String				 sName    = rMethod.getName();
		MethodMappingHandler rHandler = aMethodMapping.get(sName);

		if (rHandler == null)
		{
			rHandler = aDefaultHandler;
			aMethodMapping.put(sName, rHandler);
		}

		return rHandler;
	}

	/***************************************
	 * Performs the method invocation or an equivalent action on the target
	 * object and returns the corresponding result. The result's type must match
	 * the return type of the method and must be NULL for void methods.
	 *
	 * <p>The default implementation will first invoke mapMethod() to obtain the
	 * target method. Then it maps the method arguments by calling
	 * mapArguments(). After invoking the mapped method with the converted
	 * arguments through reflection it maps the return value by invoking
	 * mapReturnValue() and returns it.</p>
	 *
	 * <p>The implementation can signal errors by throwing any kind of
	 * exception.</p>
	 *
	 * @param  rOriginalMethod The unmapped method that has been invoked
	 * @param  rTarget         The target object of the method mapping proxy
	 * @param  rArgs           The arguments of the method call
	 *
	 * @return The return value of the method call; this must match the return
	 *         type of the method (NULL for void methods)
	 *
	 * @throws Exception Any exception may be thrown if either the method
	 *                   invocation or the mapping of arguments or the return
	 *                   value fails
	 */
	public Object invoke(Method rOriginalMethod, Object rTarget, Object[] rArgs)
		throws Exception
	{
		MethodMappingHandler rHandler = getMappingHandler(rOriginalMethod);

		return rHandler.invoke(rOriginalMethod, rTarget, rArgs);
	}

	/***************************************
	 * Merges the method and datatype mappings of another mapping definition
	 * into this one. Only mappings that don't exist already in this instance
	 * will me added, existing mappings won't be changed.
	 *
	 * @param rOther The other mapping to merge into this
	 */
	public void merge(MethodMappingDefinition rOther)
	{
		for (MethodDatatypeMapper rMapper : rOther.aDatatypeMapping)
		{
			if (!aDatatypeMapping.contains(rMapper))
			{
				aDatatypeMapping.add(rMapper);
			}
		}

		for (String s : rOther.aMethodMapping.keySet())
		{
			if (!aMethodMapping.containsKey(s))
			{
				aMethodMapping.put(s, rOther.aMethodMapping.get(s));
			}
		}
	}
}
