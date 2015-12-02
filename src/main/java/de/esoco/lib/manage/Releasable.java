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
package de.esoco.lib.manage;

/********************************************************************
 * Interface for manageable elements that can be released. An example are
 * objects that are retrieved from a cache and can be put back into the cache by
 * releasing them. How a releasable object is acquired depends on the
 * implementation. It can be through a constructor or from a factory. Typical
 * implementations allow to acquire the same object several times (e.g. from
 * several levels of a call hierarchy) and then also require to call the
 * interface method {@link #release()} equally often to finally free the
 * object's allocated resources.
 *
 * @author eso
 */
public interface Releasable
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Releases this objects.
	 */
	public void release();
}
