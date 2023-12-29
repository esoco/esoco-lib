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
 * Base class for advice on interceptions. It is modelled after the principles
 * of aspect orientation and allows to apply certain crosscutting functionality
 * like logging to interceptions of an interception proxy, independent of the
 * actual function of the interception or the method(s) behind it.
 *
 * <p>There are several ways to use this class. The easiest is to implement one
 * or more of its specific advice methods like before() or after() which have a
 * default implementation that does nothing. Implementing these methods can
 * easily be done in anonymous adapter classes, for example.</p>
 *
 * <p>If the subclass needs to handle the invocation of the underlying
 * interceptions specially it can override the invoke() method which, in the
 * default implementation, calls the Interception.invoke() method. But it also
 * handles the invocation of chained advice (setup by the interception proxy).
 * Therefore a subclass overriding invoke() should always call super.invoke() to
 * perform the actual interception invocation, else the chaining mechanism won't
 * work anymore.</p>
 *
 * <p>Finally, a subclass may also override the advise() method to implement
 * it's own advice handling without the invocation of the specific advice
 * methods. Again, the subclass should always call super.invoke() to perform the
 * interception invocation to keep the advice chain working.</p>
 *
 * @author eso
 */
public abstract class InterceptionAdvice implements Cloneable {

	InterceptionAdvice rNextAdvice = null;

	/**
	 * Creates a copy of this advice object. This method is used by the
	 * interception proxy to duplicate advice instances in case of advice chain
	 * conflicts. A subclass containing fields that need special treatment in a
	 * clone method (like references to mutable objects) must override this
	 * method and handle such fields respectively.
	 *
	 * @return the new instance
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	/**
	 * Package internal method that returns the reference to a chained advice
	 * object that is wrapped by this one.
	 *
	 * @return The next advice object or NULL for none
	 */
	public final InterceptionAdvice getNextAdvice() {
		return rNextAdvice;
	}

	/**
	 * The method that performs the interception invocation. If a next advice
	 * has been set by the interception proxy, the invocation will be forwarded
	 * to the chained advice's advise() method.
	 *
	 * <p>If a subclass chooses to override this method it should at least
	 * always invoke
	 * {@link InterceptionAdvice#invoke(Interception, Object, Method, Object,
	 * Object[]) super.invoke()} to support the chaining of advice.</p>
	 *
	 * @param rInterception The interception to be advised.
	 * @param rProxy        The proxy on which the method has been invoked
	 * @param rMethod       The original method that has been invoked
	 * @param rTarget       The object on which the method shall be invoked
	 * @param rArgs         The original method arguments
	 * @return The result of the interception invocation
	 * @throws Exception Any kind of exception may be thrown
	 */
	public Object invoke(Interception rInterception, Object rProxy,
		Method rMethod, Object rTarget, Object[] rArgs) throws Exception {
		if (rNextAdvice != null) {
			return rNextAdvice.advise(rInterception, rProxy, rMethod, rTarget,
				rArgs);
		} else {
			return rInterception.invoke(rProxy, rMethod, rTarget, rArgs);
		}
	}

	/**
	 * The main method that executes the actual advice on the interception. It
	 * invokes the specialized advice methods in this class (like before() or
	 * after()) which may be overridden by subclasses. The default
	 * implementations simply do nothing.
	 *
	 * <p>If a subclass chooses to override this method it should at least
	 * always invoke the method
	 * {@link #invoke(Interception, Object, Method, Object, Object[]) invoke()}
	 * to support the chaining of advice that may have been setup by the
	 * InterceptionProxy class.</p>
	 *
	 * @param rInterception The interception to be advised.
	 * @param rProxy        The proxy on which the method has been invoked
	 * @param rMethod       The original method that has been invoked
	 * @param rTarget       The object on which the method shall be invoked
	 * @param rArgs         The original method arguments
	 * @return The result of the interception invocation
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected Object advise(Interception rInterception, Object rProxy,
		Method rMethod, Object rTarget, Object[] rArgs) throws Exception {
		Object rResult;

		before(rTarget, rMethod, rArgs);

		try {
			rResult = invoke(rInterception, rProxy, rMethod, rTarget, rArgs);
		} catch (Throwable t) {
			afterThrow(t, rTarget, rMethod, rArgs);
			throw t;
		}

		afterReturn(rResult, rTarget, rMethod, rArgs);

		return rResult;
	}

	/**
	 * This method can be implemented to perform actions either after an
	 * interception returned normally of after it threw an exception. It
	 * will be
	 * invoked by the default implementations of
	 * {@link #afterReturn(Object, Object, Method, Object[]) afterReturn()} and
	 * {@link #afterThrow(Throwable, Object, Method, Object[]) afterThrow()}.
	 *
	 * @param rInvoked The object the methpd has been invoked on
	 * @param rMethod  The invoked method
	 * @param rArgs    The method arguments
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected void after(Object rInvoked, Method rMethod, Object[] rArgs)
		throws Exception {
	}

	/**
	 * This method can be implemented to perform actions after an interception
	 * exectuted successfully (i.e., without throwing an exception). The
	 * default
	 * implementation just invokes
	 * {@link #after(Object, Method, Object[]) after()} which by default does
	 * nothing.
	 *
	 * @param rReturn  The return value of the method call
	 * @param rInvoked The object on which the method had been invoked
	 * @param rMethod  The original method that has been invoked
	 * @param rArgs    The original method arguments
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected void afterReturn(Object rReturn, Object rInvoked, Method rMethod,
		Object[] rArgs) throws Exception {
		after(rInvoked, rMethod, rArgs);
	}

	/**
	 * This method can be implemented to perform actions after an interception
	 * threw an exception. The default implementation just invokes
	 * {@link #after(Object, Method, Object[]) after()} which by default does
	 * nothing.
	 *
	 * @param rThrown  The exception thrown
	 * @param rInvoked The object on which the method had been invoked
	 * @param rMethod  The original method that has been invoked
	 * @param rArgs    The original method arguments
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected void afterThrow(Throwable rThrown, Object rInvoked,
		Method rMethod, Object[] rArgs) throws Exception {
		after(rInvoked, rMethod, rArgs);
	}

	/**
	 * This method can be implemented to perform actions before an interception
	 * will be exectuted. The default implementation does nothing.
	 *
	 * @param rTarget The object on which the method will been invoked
	 * @param rMethod The original method that has been invoked
	 * @param rArgs   The original method arguments
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected void before(Object rTarget, Method rMethod, Object[] rArgs)
		throws Exception {
	}

	/**
	 * Package internal method that initializes the reference to another advice
	 * object. This method is invoked by the InterceptionProxy to chain
	 * multiple
	 * advice together.
	 *
	 * @param rAdvice The next advice in the chain
	 */
	final void setNextAdvice(InterceptionAdvice rAdvice) {
		rNextAdvice = rAdvice;
	}
}
