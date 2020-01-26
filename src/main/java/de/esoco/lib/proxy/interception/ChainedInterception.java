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
 * An interception implementation that allows to chain multiple interceptions
 * together.
 *
 * @author eso
 */
public abstract class ChainedInterception implements Interception
{
	//~ Instance fields --------------------------------------------------------

	private Interception rNext = null;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance chained to a certain interception. The argument
	 * interception will be invoked after this one.
	 *
	 * @param rNextInterception The next interception in the chain
	 */
	public ChainedInterception(Interception rNextInterception)
	{
		if (rNextInterception == null)
		{
			throw new IllegalArgumentException(
				"Interception argument must not be null");
		}

		rNext = rNextInterception;
	}

	/***************************************
	 * Internal constructor to create an instance without a next interception
	 * (used by the InterceptionProxy).
	 */
	ChainedInterception()
	{
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the next interception in the chain.
	 *
	 * @return The next interception
	 */
	public final Interception getNextInterception()
	{
		return rNext;
	}

	/***************************************
	 * Implemented to perform the invocation of the next interception in the
	 * chain.
	 *
	 * @see Interception#invoke(Object, Method, Object, Object[])
	 */
	@Override
	public final Object invoke(Object   rProxy,
							   Method   rOriginalMethod,
							   Object   rTarget,
							   Object[] rArgs) throws Exception
	{
		return invokeChain(null, rProxy, rOriginalMethod, rTarget, rArgs);
	}

	/***************************************
	 * Internal implementation that performs the invocation of the next
	 * interception in the chain. If this instance's next interception is not
	 * NULL (i.e. this instance is not the last one in a chain that has been
	 * created by the InterceptionProxy class) it will be invoked. Else the
	 * argument interception will be invoked.
	 *
	 * @param rInterception The interception to be executed after all advice
	 *
	 * @see   Interception#invoke(Object, Method, Object, Object[])
	 */
	Object invokeChain(Interception rInterception,
					   Object		rProxy,
					   Method		rOriginalMethod,
					   Object		rTarget,
					   Object[]		rArgs) throws Exception
	{
		Object result = null;

		if (rInterception == null)
		{
			// called without interception from interception proxy? Then invoke
			// the next interception in chain
			result = rNext.invoke(rProxy, rOriginalMethod, rTarget, rArgs);
		}
		else if (rNext != null)
		{
			// invoked from interception proxy, therefore continue down the
			//chain
			result =
				((ChainedInterception) rNext).invokeChain(
					rInterception,
					rProxy,
					rOriginalMethod,
					rTarget,
					rArgs);
		}
		else
		{
			// finally, when end of chain reached, invoke the argument
			//interception
			result =
				rInterception.invoke(rProxy, rOriginalMethod, rTarget, rArgs);
		}

		return result;
	}
}
