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
package de.esoco.lib.collection;

import java.util.Iterator;


/********************************************************************
 * A simple wrapper around an Iterable instance. It provides a way to use an
 * iterable in places where the underlying collection shall not be exposed.
 *
 * @author eso
 */
public class IterableWrapper<T> implements Iterable<T>
{
	//~ Instance fields --------------------------------------------------------

	private final Iterable<T> rIterable;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Wraps a certain iterable instance. This allows to return an Iterable
	 * instance for an arbitrary collection without returning the collection
	 * itself. The original iterable object cannot be accessed externally.
	 *
	 * @param rIterable The iterable instance to wrap
	 */
	public IterableWrapper(Iterable<T> rIterable)
	{
		this.rIterable = rIterable;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the iterator of the wrapped iterable.
	 *
	 * @return The iterator
	 */
	@Override
	public Iterator<T> iterator()
	{
		return rIterable.iterator();
	}
}
