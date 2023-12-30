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

	InterceptionAdvice nextAdvice = null;

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
		return nextAdvice;
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
	 * @param interception The interception to be advised.
	 * @param proxy        The proxy on which the method has been invoked
	 * @param method       The original method that has been invoked
	 * @param target       The object on which the method shall be invoked
	 * @param args         The original method arguments
	 * @return The result of the interception invocation
	 * @throws Exception Any kind of exception may be thrown
	 */
	public Object invoke(Interception interception, Object proxy,
		Method method,
		Object target, Object[] args) throws Exception {
		if (nextAdvice != null) {
			return nextAdvice.advise(interception, proxy, method, target,
				args);
		} else {
			return interception.invoke(proxy, method, target, args);
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
	 * @param interception The interception to be advised.
	 * @param proxy        The proxy on which the method has been invoked
	 * @param method       The original method that has been invoked
	 * @param target       The object on which the method shall be invoked
	 * @param args         The original method arguments
	 * @return The result of the interception invocation
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected Object advise(Interception interception, Object proxy,
		Method method, Object target, Object[] args) throws Exception {
		Object result;

		before(target, method, args);

		try {
			result = invoke(interception, proxy, method, target, args);
		} catch (Throwable t) {
			afterThrow(t, target, method, args);
			throw t;
		}

		afterReturn(result, target, method, args);

		return result;
	}

	/**
	 * This method can be implemented to perform actions either after an
	 * interception returned normally of after it threw an exception. It
	 * will be
	 * invoked by the default implementations of
	 * {@link #afterReturn(Object, Object, Method, Object[]) afterReturn()} and
	 * {@link #afterThrow(Throwable, Object, Method, Object[]) afterThrow()}.
	 *
	 * @param invoked The object the methpd has been invoked on
	 * @param method  The invoked method
	 * @param args    The method arguments
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected void after(Object invoked, Method method, Object[] args)
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
	 * @param toReturn The return value of the method call
	 * @param invoked  The object on which the method had been invoked
	 * @param method   The original method that has been invoked
	 * @param args     The original method arguments
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected void afterReturn(Object toReturn, Object invoked, Method method,
		Object[] args) throws Exception {
		after(invoked, method, args);
	}

	/**
	 * This method can be implemented to perform actions after an interception
	 * threw an exception. The default implementation just invokes
	 * {@link #after(Object, Method, Object[]) after()} which by default does
	 * nothing.
	 *
	 * @param thrown  The exception thrown
	 * @param invoked The object on which the method had been invoked
	 * @param method  The original method that has been invoked
	 * @param args    The original method arguments
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected void afterThrow(Throwable thrown, Object invoked, Method method,
		Object[] args) throws Exception {
		after(invoked, method, args);
	}

	/**
	 * This method can be implemented to perform actions before an interception
	 * will be exectuted. The default implementation does nothing.
	 *
	 * @param target The object on which the method will been invoked
	 * @param method The original method that has been invoked
	 * @param args   The original method arguments
	 * @throws Exception Any kind of exception may be thrown
	 */
	protected void before(Object target, Method method, Object[] args)
		throws Exception {
	}

	/**
	 * Package internal method that initializes the reference to another advice
	 * object. This method is invoked by the InterceptionProxy to chain
	 * multiple
	 * advice together.
	 *
	 * @param advice The next advice in the chain
	 */
	final void setNextAdvice(InterceptionAdvice advice) {
		nextAdvice = advice;
	}
}
