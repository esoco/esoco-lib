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

import de.esoco.lib.proxy.interception.InterceptionProxy.InterceptionHandler;
import de.esoco.lib.reflect.ReflectUtil;

import java.lang.reflect.Method;

import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;


/********************************************************************
 * Method interception for methods of the interface {@link Relatable}. These
 * method are redirected to the corresponding InterceptionHandler instance which
 * extends {@link RelatedObject} for this purpose.
 *
 * @author eso
 */
class RelatableInterception extends MethodInterception
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 */
	public RelatableInterception()
	{
		super(Relatable.class);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Overridden to invoke the original method on the interception handler.
	 * Upon invocation of a relation-specific method the handler replaces the
	 * first argument (normally the proxy) with itself.
	 *
	 * @see MethodInterception#invoke(Object, Method, Object, Object[])
	 * @see InterceptionHandler#invoke(Object, Method, Object[])
	 */
	@Override
	public Object invoke(Object   rInterceptionHandler,
						 Method   rOriginalMethod,
						 Object   rTarget,
						 Object[] rArgs) throws Throwable
	{
		return ReflectUtil.invoke(rInterceptionHandler, rOriginalMethod, rArgs);
	}

	/***************************************
	 * Overridden to return the original method instead of mapping it to this
	 * class. The original method will then be invoked on the interception
	 * handler.
	 *
	 * @see MethodInterception#mapMethod(Method)
	 */
	@Override
	protected Method mapMethod(Method rMethod)
	{
		return rMethod;
	}
}
