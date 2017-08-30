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
package de.esoco.lib.app;

/********************************************************************
 * A service subclass that is only based on the REST service implementation of
 * the {@link Service} class.
 *
 * @author eso
 */
public abstract class RestService extends Service
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 */
	public RestService()
	{
		super(true);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Overridden to to nothing as stopping a REST service is done through it's
	 * control API.
	 */
	@Override
	public void stop()
	{
	}

	/***************************************
	 * Overridden to to nothing as the REST service is already running.
	 */
	@Override
	protected void runService()
	{
	}
}
