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

import java.lang.reflect.Method;


/********************************************************************
 * Defines a method Interception for the interception proxy. Contains a single
 * method invoke() that must be overridden by implementations to provide the
 * interception functionality.
 */
public interface Interception
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * This method must be implemented to provide the actual method
	 * interception.
	 *
	 * @param  rProxy          The proxy on which the method has been invoked
	 * @param  rOriginalMethod The original method that has been invoked
	 * @param  rTarget         The object on which the method shall be invoked
	 * @param  rArgs           The original method arguments
	 *
	 * @return The result of the method call or of the interception
	 *
	 * @throws Exception Any kind of exception may be thrown
	 */
	public Object invoke(Object   rProxy,
						 Method   rOriginalMethod,
						 Object   rTarget,
						 Object[] rArgs) throws Exception;
}
