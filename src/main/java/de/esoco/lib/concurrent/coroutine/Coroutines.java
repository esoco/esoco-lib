//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.lib.concurrent.coroutine;

/********************************************************************
 * Contains global {@link Coroutine} management functions.
 *
 * @author eso
 */
public class Coroutines
{
	//~ Static fields/initializers ---------------------------------------------

	private static CoroutineContext rDefaultContext = null;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Private, only static use.
	 */
	private Coroutines()
	{
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns the default {@link CoroutineContext}.
	 *
	 * @return The default context (can be NULL)
	 */
	public static CoroutineContext getDefaultContext()
	{
		return rDefaultContext;
	}

	/***************************************
	 * Sets the default {@link CoroutineContext}. The context will be used for
	 * all coroutines that are started without an explicit context. If set to
	 * NULL (the default) such coroutines are started with an individual (local)
	 * context instead.
	 *
	 * @param rDefaultContext The new default context or NULL for none
	 */
	public static void setDefaultContext(CoroutineContext rDefaultContext)
	{
		Coroutines.rDefaultContext = rDefaultContext;
	}
}
