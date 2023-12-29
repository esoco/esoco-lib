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
package de.esoco.lib.manage;

/**
 * The base interface that must be provided by factory implementations. The
 * generic type parameters define the type of the elements created by this
 * factory (T), the element definition subtype needed to create new objects (D),
 * and the type of exceptions that may be thrown if the creation of new elements
 * fails (E).
 *
 * @author eso
 */
@FunctionalInterface
public interface Factory<T, D extends ElementDefinition<T>,
	E extends Exception> {

	/**
	 * Creates and returns a new element that corresponds to the given element
	 * definition.
	 *
	 * @param rDefinition The definition of the element to create
	 * @return The new element instance
	 * @throws E Any kind of exception may be thrown by factory implementations
	 */
	T create(D rDefinition) throws E;
}
