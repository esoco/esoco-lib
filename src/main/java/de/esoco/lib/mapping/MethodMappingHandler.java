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


/********************************************************************
 * Default implementation of a mapping of methods and their arguments and return
 * values. Subclasses can override some or all of the methods to provide
 * specific mappings. The central method is invoke(), it will be invoked from
 * the similar method of MethodMappingDefinition to perform a method call.
 * invoke() will call the mapping methods to perform the mapping of method
 * names, argument values, etc.
 *
 * @see #invoke(Method, Object, Object[])
 */
public class MethodMappingHandler
{
	//~ Instance fields --------------------------------------------------------

	private MethodMappingDefinition rMapping	  = null;
	private String				    sTargetMethod = null;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new default MethodMappingHandler object that maps to a method
	 * with the same name if the mapping methods are not overridden.
	 */
	public MethodMappingHandler()
	{
	}

	/***************************************
	 * Internal constructor to create a new MethodMappingHandler object that
	 * maps to a method with another name.
	 *
	 * @param sTargetMethod The target method to be invoked (may be NULL)
	 */
	public MethodMappingHandler(String sTargetMethod)
	{
		this.sTargetMethod = sTargetMethod;
	}

	/***************************************
	 * Internal constructor for a child handler of a certain mapping definition.
	 *
	 * @param rMapping The parent mapping definition
	 */
	public MethodMappingHandler(MethodMappingDefinition rMapping)
	{
		setMappingDefinition(rMapping);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns a string representation of this mapping handler.
	 *
	 * @return A descriptive string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + sTargetMethod + "]";
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
	 * @param  rTarget         The target object of the invocation
	 * @param  rArgs           The arguments of the method call
	 *
	 * @return The return value of the method call; this must match the return
	 *         type of the method (NULL for void methods)
	 *
	 * @throws Exception Any exception may be thrown if either the method
	 *                   invocation or the mapping of arguments or the return
	 *                   value fails
	 *
	 * @see    #mapTarget(Method, Object, Object[])
	 * @see    #mapMethod(Method, String, Object)
	 * @see    #mapArguments(Method, Object, Method, Object[])
	 * @see    #mapReturnValue(Method, Object, Method, Object)
	 */
	protected Object invoke(Method   rOriginalMethod,
							Object   rTarget,
							Object[] rArgs) throws Exception
	{
		try
		{
			rTarget = mapTarget(rOriginalMethod, rTarget, rArgs);

			Method rTargetMethod =
				mapMethod(rOriginalMethod, sTargetMethod, rTarget);

			rArgs =
				mapArguments(rOriginalMethod, rTarget, rTargetMethod, rArgs);

			Object rValue = invokeMethod(rTarget, rTargetMethod, rArgs);

			return mapReturnValue(rOriginalMethod,
								  rTarget,
								  rTargetMethod,
								  rValue);
		}
		catch (InvocationTargetException eITE)
		{
			Throwable t = eITE.getTargetException();

			if (t instanceof Exception)
			{
				throw (Exception) t;
			}
			else
			{
				throw (Error) t;
			}
		}
	}

	/***************************************
	 * Invokes a method on the target object. This method is invoked internally
	 * after the method and the argument have been mapped. It may be overloaded
	 * by subclasses to redirect the actual invocation. The returned value will
	 * then be mapped with mapReturnValue() before it is returned to the caller.
	 *
	 * @param  rTarget       The target object to invoke the method on
	 * @param  rTargetMethod The method to be invoked
	 * @param  rArgs         The arguments of the method call
	 *
	 * @return The result of the method call
	 *
	 * @throws Exception Any exception may be caused by the invocation
	 */
	protected Object invokeMethod(Object   rTarget,
								  Method   rTargetMethod,
								  Object[] rArgs) throws Exception
	{
		return rTargetMethod.invoke(rTarget, rArgs);
	}

	/***************************************
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
	 * @param  rOriginalMethod The unmapped method that has been invoked
	 * @param  rTarget         The target object to be invoked
	 * @param  rTargetMethod   The target method that will be invoked
	 * @param  rArgs           The arguments of the method call
	 *
	 * @return An Object array with the (optionally mapped) arguments for the
	 *         method call
	 *
	 * @see    #invoke(Method, Object, Object[])
	 */
	protected Object[] mapArguments(Method   rOriginalMethod,
									Object   rTarget,
									Method   rTargetMethod,
									Object[] rArgs)
	{
		if (rArgs != null)
		{
			for (int i = 0; i < rArgs.length; i++)
			{
				Object rValue = rArgs[i];

				if (rValue != null)
				{
					MethodDatatypeMapper rMapper =
						rMapping.getDatatypeMapper(rOriginalMethod,
												   rValue.getClass());

					if (rMapper != null)
					{
						rArgs[i] =
							rMapper.mapValue(rTarget, rOriginalMethod, rValue);
					}
				}
			}
		}

		return rArgs;
	}

	/***************************************
	 * Maps the argument types of a method to the count and type required by a
	 * different target method. The default implementation checks for each
	 * argument type if a global datatype converter is defined in the mapping.
	 * If so, it converts the datatype with it, else it returns the orginal
	 * datatype.
	 *
	 * @param  rOriginalMethod The original method from wich the types shall be
	 *                         mapped
	 * @param  rArgTypes       The original argument types
	 * @param  rTarget         The target object that will be invoked
	 *
	 * @return An array of Class instances containing the (optionally mapped)
	 *         argument types
	 */
	protected Class<?>[] mapArgumentTypes(Method	 rOriginalMethod,
										  Class<?>[] rArgTypes,
										  Object	 rTarget)
	{
		for (int i = 0; i < rArgTypes.length; i++)
		{
			MethodDatatypeMapper mapper =
				rMapping.getDatatypeMapper(rOriginalMethod, rArgTypes[i]);

			if (mapper != null)
			{
				rArgTypes[i] = mapper.mapType(rOriginalMethod, rArgTypes[i]);
			}
		}

		return rArgTypes;
	}

	/***************************************
	 * Returns the mapped method (if a mapping is defined) for the target object
	 * by looking into the internal mapping table and mapping the method
	 * parameter types my invoking mapParameterTypes(). Subclasses can overload
	 * this method to provide additional mappings.
	 *
	 * <p>If the mapping of a method results in the additional need of mapping
	 * the method arguments this can be done in the separate method <code>
	 * mapArguments()</code> that is invoked by the proxy implementation after
	 * mapMethod() has returned.</p>
	 *
	 * @param  rOriginalMethod The unmapped method that has been invoked
	 * @param  sMethod         The name of the target method or NULL if the name
	 *                         shall be determined from the original method
	 * @param  rTarget         Target object on which the original method should
	 *                         be mapped
	 *
	 * @return The mapped method instance
	 *
	 * @throws NoSuchMethodException If no method that matches the mapping could
	 *                               be found
	 *
	 * @see    #invoke(Method, Object, Object[])
	 * @see    #mapArguments(Method, Object, Method, Object[])
	 */
	protected Method mapMethod(Method rOriginalMethod,
							   String sMethod,
							   Object rTarget) throws NoSuchMethodException
	{
		if (sMethod == null)
		{
			sMethod = rOriginalMethod.getName();
		}

		Class<?>[] rParamTypes =
			mapArgumentTypes(rOriginalMethod,
							 rOriginalMethod.getParameterTypes(),
							 rTarget);

		return rTarget.getClass().getMethod(sMethod, rParamTypes);
	}

	/***************************************
	 * Maps the original return value of a method invocation to another value
	 * and/or datatype. The default implementation checks if a global datatype
	 * converter is defined in the mapping. If so, it converts the return value
	 * with it, else it returns the orginal return value.
	 *
	 * <p>The implementation may throw a RuntimeException (recommended are
	 * either IllegalArgument or UnsupportedOperation) to signal that an error
	 * occurred during the mapping.</p>
	 *
	 * @param  rOriginalMethod The unmapped method that has been invoked
	 * @param  rTarget         The target object
	 * @param  rTargetMethod   The mapped method that returned the value
	 * @param  rValue          The value returned by the target method
	 *
	 * @return The (optionally mapped) return value
	 *
	 * @see    #mapMethod(Method, String, Object)
	 */
	protected Object mapReturnValue(Method rOriginalMethod,
									Object rTarget,
									Method rTargetMethod,
									Object rValue)
	{
		if (rValue != null)
		{
			MethodDatatypeMapper mapper =
				rMapping.getDatatypeMapper(rOriginalMethod, rValue.getClass());

			if (mapper != null)
			{
				rValue = mapper.mapValue(rTarget, rOriginalMethod, rValue);
			}
		}

		return rValue;
	}

	/***************************************
	 * Allows subclasses to map the target object of the invocation. This
	 * default implementation simply returns the original target object.
	 *
	 * @param  rOriginalMethod The unmapped method that has been invoked
	 * @param  rTarget         The target object of the invocation
	 * @param  rArgs           The arguments of the method call
	 *
	 * @return The mapped target object
	 */
	protected Object mapTarget(Method   rOriginalMethod,
							   Object   rTarget,
							   Object[] rArgs)
	{
		return rTarget;
	}

	/***************************************
	 * Sets the parent mapping definition that this mapping handler belongs to.
	 *
	 * @param rMapping The parent mapping definition
	 */
	void setMappingDefinition(MethodMappingDefinition rMapping)
	{
		this.rMapping = rMapping;
	}
}
