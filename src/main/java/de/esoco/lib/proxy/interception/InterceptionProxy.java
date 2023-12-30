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

import de.esoco.lib.reflect.ReflectUtil;
import org.obrel.core.Relatable;
import org.obrel.core.RelatedObject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proxy implementation that intercepts method calls on an interface and
 * forwards them to an application-specific interception handler.
 *
 * @author eso
 */
public class InterceptionProxy<T> extends RelatedObject {

	/**
	 * Standard interception that forwards a method call to the target
	 */
	public static final Interception FORWARD = new Delegation();

	/**
	 * Standard interception instance that simply ignores a method call
	 */
	public static final Interception IGNORE = new Interception() {
		@Override
		public Object invoke(Object proxy, Method originalMethod,
			Object target,
			Object[] args) throws Exception {
			return null;
		}
	};

	private final Class<?>[] proxyInterfaces;

	private final Map<Method, Interception> interceptions =
		new HashMap<Method, Interception>();

	InterceptionAdvice interceptionAdvice = null;

	private Interception defaultInterception = FORWARD;

	/**
	 * Creates a new instance. By default all method calls on created proxies
	 * will be forwarded to methods in target objects with the same name and
	 * parameters.
	 *
	 * @param interfaceType The interface to be implemented by this proxy
	 */
	public InterceptionProxy(Class<T> interfaceType) {
		proxyInterfaces = new Class[] { interfaceType };
	}

	/**
	 * Creates a new instance for proxies that by default forward all method
	 * calls to the same methods in target objects. If the second argument is
	 * TRUE proxies created by this instance will support object relations even
	 * if the target object does not. The given proxy interface must extend the
	 * {@link Relatable} interface to provide access to relations.
	 *
	 * @param interfaceType   The interface to be implemented by this proxy
	 * @param relationSupport TRUE to support object relations
	 */
	public InterceptionProxy(Class<T> interfaceType, boolean relationSupport) {
		this(interfaceType);

		if (relationSupport) {
			assert Relatable.class.isAssignableFrom(interfaceType);

			setInterception(new RelatableInterception());
		}
	}

	/**
	 * Returns the target object of a particular InterceptionProxy instance.
	 *
	 * @param proxyInstance The proxy instance to return the target of
	 * @return The target object of the proxy instance
	 * @throws IllegalArgumentException If the argument is not an instance of
	 *                                  InterceptionProxy
	 */
	public static Object getTarget(Object proxyInstance) {
		InvocationHandler ih = Proxy.getInvocationHandler(proxyInstance);

		if (ih instanceof InterceptionProxy<?>.InterceptionHandler) {
			return ((InterceptionProxy<?>.InterceptionHandler) ih).target;
		} else {
			throw new IllegalArgumentException(
				"Not an interception proxy: " + proxyInstance);
		}
	}

	/**
	 * Adds advice that will be executed around (i.e. before and/or after) the
	 * method calls of other interceptions. If other advice has been added
	 * previously the new advice will be executed around the first one(s) by
	 * successively invoking the advice instances.
	 *
	 * <p>It is safe to add an InterceptionAdvice instance that has been used
	 * on another proxy before. If the argument advice object already
	 * contains a
	 * chained advice it will be copied by a call to it's clone() method. That
	 * implies that all InterceptionAdvice subclasses must implement a valid
	 * clone() method if they contain fields that need to be treated specially
	 * in clone().</p>
	 *
	 * <p>This functionality is modelled after the principles of aspect
	 * orientation and allows to apply certain crosscutting functions like
	 * logging to interceptions, independent of the actual function of the
	 * interception or the method(s) behind it.</p>
	 *
	 * @param advice The advice object to be added
	 */
	public void addAdvice(InterceptionAdvice advice) {
		if (interceptionAdvice != null) {
			InterceptionAdvice next = advice.getNextAdvice();

			if (next != null && next != interceptionAdvice) {
				// prevent breaking other advice chains but allow re-use  of
				// instances; therefore only create a copy if necessary
				advice = (InterceptionAdvice) advice.clone();
			}

			advice.setNextAdvice(interceptionAdvice);
		}

		interceptionAdvice = advice;
	}

	/**
	 * Returns the default interception of this proxy.
	 *
	 * @return The default interception (NULL if not set)
	 */
	public Interception getDefaultInterception() {
		return defaultInterception;
	}

	/**
	 * Returns the interception for a certain method, or NULL if none has been
	 * set.
	 *
	 * @param method The method for which to return the interception
	 * @return The matching (or default) interception or NULL
	 */
	public Interception getInterception(Method method) {
		return interceptions.get(method);
	}

	/**
	 * Creates a new proxy instance that implements the interception proxy's
	 * interface and forwards call on that interface to a certain target
	 * object.
	 * What exactly this forwarding means depends on the interceptions used.
	 * For
	 * example, a value object interception could apply calls to set and get
	 * methods to a generic value container (like a Map).
	 *
	 * <p><b>Attention:</b> the created proxy instance remains connected to the
	 * interception settings in this proxy. That means that changes in the
	 * configuration of this proxy will be reflected in existing proxy
	 * instances. Unless such effects are desired it is recommended that once
	 * proxy instances have been created no more changes to the interception
	 * proxy configuration are made. This will prevent unexpected side
	 * effects.</p>
	 *
	 * @param target The target object of calls on the proxy interface
	 * @return An instance of the proxy interface using a LimitedAccessProxy
	 * instance to forward methods calls to the target
	 */
	@SuppressWarnings("unchecked")
	public T newProxyInstance(Object target) {
		return (T) Proxy.newProxyInstance(proxyInterfaces[0].getClassLoader(),
			proxyInterfaces, new InterceptionHandler(target));
	}

	/**
	 * Sets the default interception of this proxy that will be invoked if no
	 * method-specific interception can be found. The default setting of a new
	 * proxy instance will forward calls to the same method in the target
	 * object
	 * (if available).
	 *
	 * @param defaultInterception The default interception (NULL to disable)
	 */
	public void setDefaultInterception(Interception defaultInterception) {
		this.defaultInterception = defaultInterception;
	}

	/**
	 * Adds a method interception for all methods it is registered for.
	 *
	 * @param interception The method interception to add
	 */
	public void setInterception(MethodInterception interception) {
		for (Method method : interception.getMethodMap().keySet()) {
			setInterception(method, interception);
		}
	}

	/**
	 * Sets an interception for a method of the proxy interface with a certain
	 * name and an arbitrary parameter list. If multiple methods with the same
	 * name exist the interception will be registered for all of them.
	 *
	 * @param method       The name of the method to intercept
	 * @param interception The interception to be invoked in place of the
	 *                     method
	 * @throws IllegalArgumentException If
	 */
	public void setInterception(String method, Interception interception) {
		List<Method> methodList =
			ReflectUtil.getPublicMethods(proxyInterfaces[0], method);

		if (methodList.size() > 0) {
			for (Method m : methodList) {
				setInterception(m, interception);
			}
		} else {
			throw new IllegalArgumentException("No such method: " + method);
		}
	}

	/**
	 * Adds an interception for a particular method to this proxy.
	 *
	 * @param method       The method to intercept
	 * @param interception The interception to be used
	 * @throws IllegalArgumentException If one of the arguments is invalid
	 */
	public void setInterception(Method method, Interception interception) {
		if (method == null ||
			!method.getDeclaringClass().isAssignableFrom(proxyInterfaces[0])) {
			throw new IllegalArgumentException("Invalid method: " + method);
		}

		if (interception == null) {
			throw new IllegalArgumentException(
				"Interception must not be " + "null");
		}

		interceptions.put(method, interception);
	}

	/**
	 * Returns the interception that has been defined for a certain method.
	 * Depending on the internal state this method will return either a
	 * specific
	 * interception for the given method or a default interception. If no
	 * interception matching interception has been defined an
	 * IllegalArgumentException will be thrown.
	 *
	 * @param method The method for which to return the interception
	 * @return The matching interception
	 * @throws IllegalArgumentException If no interception has been defined for
	 *                                  the given method (and no default
	 *                                  interception exists)
	 */
	protected Interception getDefinedInterception(Method method) {
		Interception interception = getInterception(method);

		if (interception == null) {
			interception = defaultInterception;
		}

		if (interception == null) {
			throw new IllegalArgumentException(
				"No interception for method " + method.getName());
		}

		return interception;
	}

	/**
	 * The invocation handler for interception proxies.
	 */
	class InterceptionHandler extends RelatedObject
		implements InvocationHandler {

		final Object target;

		/**
		 * Creates a new instance.
		 *
		 * @param target The target object for method invocations
		 */
		InterceptionHandler(Object target) {
			this.target = target;
		}

		/**
		 * Handles interceptions that may be defined for certain methods. If an
		 * interception exists for the invoked method it's invoke() method will
		 * be called with this interception handler's target object as the
		 * first
		 * argument. If the interception proxy contains advice, it will be
		 * invoked around the actual interception.
		 *
		 * @see InvocationHandler#invoke(java.lang.Object,
		 * java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
			Interception interception = getDefinedInterception(method);

			// if relation support is enabled and a method of the Relatable
			// interface is invoked, forward any method calls to this
			// interception handler instance
			if (interception instanceof RelatableInterception) {
				proxy = this;
			}

			if (interceptionAdvice != null) {
				return interceptionAdvice.advise(interception, proxy, method,
					target, args);
			} else {
				return interception.invoke(proxy, method, target, args);
			}
		}
	}
}
