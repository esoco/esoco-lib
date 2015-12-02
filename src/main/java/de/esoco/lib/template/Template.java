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

import org.obrel.core.RelatedObject;


/********************************************************************
 * Base class for templates that describe a certain type that can be created
 * from the template at runtime. Instances of the described type can be created
 * by invoking the method {@link #create()}.
 *
 * @author eso
 */
public abstract class Template<T> extends RelatedObject
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Creates a new instance of the type that is described by this template. It
	 * is recommended to use only types that can be created without throwing
	 * exceptions and (if necessary) provide something like an init() method
	 * that can be invoked for complex initialization after creation. But
	 * applications should still be aware of possible runtime exceptions when
	 * invoking this method.
	 *
	 * @return The new instance
	 */
	protected abstract T create();
}
