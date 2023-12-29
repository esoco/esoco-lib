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
package de.esoco.lib.template;

import org.obrel.type.StandardTypes;

/**
 * A concrete template implementation that can create instances of the described
 * type from the type's class.
 *
 * @author eso
 */
public class ClassTemplate<T> extends Template<T> {

	private final Class<? extends T> rClass;

	/**
	 * Creates a new instance for a certain type class.
	 *
	 * @param rClass       The class of the template's class
	 * @param sDescription The template description
	 */
	public ClassTemplate(Class<? extends T> rClass, String sDescription) {
		this.rClass = rClass;

		set(StandardTypes.DESCRIPTION, sDescription);
	}

	/**
	 * Creates a new instance of the type class described by this template by
	 * invoking an empty constructor through reflection. Throws a
	 * RuntimeException if the constructor invocation fails.
	 *
	 * @return A new instance of the template's type
	 */
	@Override
	protected T create() {
		try {
			return rClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
