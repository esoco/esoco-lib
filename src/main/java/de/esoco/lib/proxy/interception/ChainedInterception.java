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

/**
 * An interception implementation that allows to chain multiple interceptions
 * together.
 *
 * @author eso
 */
public abstract class ChainedInterception implements Interception {

	private Interception next = null;

	/**
	 * Creates a new instance chained to a certain interception. The argument
	 * interception will be invoked after this one.
	 *
	 * @param nextInterception The next interception in the chain
	 */
	public ChainedInterception(Interception nextInterception) {
		if (nextInterception == null) {
			throw new IllegalArgumentException(
				"Interception argument must not be null");
		}

		next = nextInterception;
	}

	/**
	 * Internal constructor to create an instance without a next interception
	 * (used by the InterceptionProxy).
	 */
	ChainedInterception() {
	}

	/**
	 * Returns the next interception in the chain.
	 *
	 * @return The next interception
	 */
	public final Interception getNextInterception() {
		return next;
	}

	/**
	 * Implemented to perform the invocation of the next interception in the
	 * chain.
	 *
	 * @see Interception#invoke(Object, Method, Object, Object[])
	 */
	@Override
	public final Object invoke(Object proxy, Method originalMethod,
		Object target, Object[] args) throws Exception {
		return invokeChain(null, proxy, originalMethod, target, args);
	}

	/**
	 * Internal implementation that performs the invocation of the next
	 * interception in the chain. If this instance's next interception is not
	 * NULL (i.e. this instance is not the last one in a chain that has been
	 * created by the InterceptionProxy class) it will be invoked. Else the
	 * argument interception will be invoked.
	 *
	 * @param interception The interception to be executed after all advice
	 * @see Interception#invoke(Object, Method, Object, Object[])
	 */
	Object invokeChain(Interception interception, Object proxy,
		Method originalMethod, Object target, Object[] args) throws Exception {
		Object result = null;

		if (interception == null) {
			// called without interception from interception proxy? Then invoke
			// the next interception in chain
			result = next.invoke(proxy, originalMethod, target, args);
		} else if (next != null) {
			// invoked from interception proxy, therefore continue down the
			//chain
			result =
				((ChainedInterception) next).invokeChain(interception, proxy,
					originalMethod, target, args);
		} else {
			// finally, when end of chain reached, invoke the argument
			//interception
			result = interception.invoke(proxy, originalMethod, target, args);
		}

		return result;
	}
}
