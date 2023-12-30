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

import de.esoco.lib.event.Event;
import de.esoco.lib.event.EventHandler;

/**
 * An event handler implementation that invokes a method on a target object to
 * handle an event. Even non-public methods can be used, the implementation will
 * try to invoke setAccessible() on them. In environments where that is not
 * allowed an exception will be thrown. Therefore applications that are built
 * for environments that have limited accessibility (like applets) should only
 * use public methods as event listeners.
 *
 * @author eso
 */
public class EventMethod<E extends Event<?>> extends MethodDispatcher<Object>
	implements EventHandler<E> {

	/**
	 * Creates a new event method object that will invoke a certain method on
	 * the target object in case of an event. It will first look for a method
	 * with the given name and a parameter with a type of the EWT EWTEvent
	 * interface. If such is not found it will look for a method with no
	 * parameters. If that also doesn't exist an IllegalArgumentException will
	 * be thrown.
	 *
	 * <p>The implementation first looks for a method with the given name and a
	 * parameter with the type of the EWT EWTEvent interface. If such is not
	 * found it will look for a method with no parameters. If such also doesn't
	 * exist an IllegalArgumentException will be thrown.</p>
	 *
	 * <p>The given method may be non-public. It will then be tried to set it
	 * as accessible for this purpose. In environments where that is not
	 * possible an exception will be thrown. Therefore applications that are
	 * built for environments that have limited accessibility (like applets)
	 * should only use public methods as event listeners.</p>
	 *
	 * @param target     The target on which the method shall be invoked
	 * @param method     The name of the method to invoke
	 * @param eventClass The class of the event to dispatch
	 * @throws IllegalArgumentException If no matching method could be found
	 */
	public EventMethod(Object target, String method, Class<E> eventClass) {
		super(target, method, true, eventClass);
	}

	/**
	 * Dispatches the event to the actual listener method.
	 *
	 * @param event The event to dispatch
	 */
	@Override
	public final void handleEvent(E event) {
		dispatch(event);
	}
}
