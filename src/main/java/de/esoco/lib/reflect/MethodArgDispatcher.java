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
package de.esoco.lib.reflect;

/********************************************************************
 * MethodDispatcher subclass that can be created and invoked with predefined
 * arguments. That makes it easier to use MethodDispatcher instances in
 * environments where it is not possible to provide the arguments to the method
 * {@link MethodDispatcher#dispatch(Object[]) dispatch()} at invocation time
 * (e.g. when using it as a Runnable instance in threads).
 *
 * @author eso
 */
public class MethodArgDispatcher<T> extends MethodDispatcher<T>
{
	//~ Instance fields --------------------------------------------------------

	private final Object[] rArguments;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new method dispatcher for a method with predefined parameters.
	 * The target object must contain a method with parameters of exactly the
	 * type of the elements in the given arguments array, else an
	 * IllegalArgumentException will be thrown.
	 *
	 * @param rTarget The target on which the method shall be invoked
	 * @param sMethod The name of the method to invoke
	 * @param rArgs   The arguments to be used on invocation
	 */
	public MethodArgDispatcher(Object rTarget, String sMethod, Object... rArgs)
	{
		super(rTarget, sMethod, ReflectUtil.getArgumentTypes(rArgs));

		rArguments = rArgs;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Overridden empty dispatch() method that invokes the underlying method
	 * with the arguments that have been set through the constructor.
	 *
	 * @return The value returned by the invoked method (NULL for void methods)
	 */
	@Override
	public T dispatch()
	{
		return dispatch(rArguments);
	}
}
