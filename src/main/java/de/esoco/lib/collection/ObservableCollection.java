//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2017 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.event.ElementEvent.EventType;

import java.io.Serializable;

import java.util.Collection;


/********************************************************************
 * A collection implementation that can be used to observe arbitrary collections
 * for changes.
 *
 * @author eso
 */
public class ObservableCollection<E>
	extends AbstractObservableCollection<E, Collection<E>,
										 CollectionEvent<E, Collection<E>>>
	implements Serializable
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long serialVersionUID = 1L;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance that observes a certain collection.
	 *
	 * @param rObservedCollection The collection to be observed
	 */
	public ObservableCollection(Collection<E> rObservedCollection)
	{
		super(rObservedCollection);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns a new instance of {@link CollectionEvent}.
	 *
	 * @see AbstractObservableCollection#createEvent(EventType, Object, Object,
	 *      int)
	 */
	@Override
	protected CollectionEvent<E, Collection<E>> createEvent(
		EventType rType,
		E		  rElement,
		E		  rUpdateValue,
		int		  nIndex)
	{
		return new CollectionEvent<E, Collection<E>>(rType,
													 this,
													 rElement,
													 rUpdateValue);
	}
}
