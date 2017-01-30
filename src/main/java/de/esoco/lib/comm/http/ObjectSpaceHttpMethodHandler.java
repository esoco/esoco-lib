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
package de.esoco.lib.comm.http;

import de.esoco.lib.comm.http.HttpRequestHandler.HttpRequestMethodHandler;

import org.obrel.space.ObjectSpace;


/********************************************************************
 * A HTTP request method handler that retrieves the data of it's responses from
 * an {@link ObjectSpace}.
 *
 * @author eso
 */
public class ObjectSpaceHttpMethodHandler implements HttpRequestMethodHandler
{
	//~ Instance fields --------------------------------------------------------

	private ObjectSpace<?> rObjectSpace;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rObjectSpace The object space to get response data from
	 */
	public ObjectSpaceHttpMethodHandler(ObjectSpace<?> rObjectSpace)
	{
		this.rObjectSpace = rObjectSpace;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse doGet(HttpRequest rRequest) throws HttpStatusException
	{
		String sPath = rRequest.getPath();

		try
		{
			String rData = rObjectSpace.get(sPath).toString();

			if (rData == null)
			{
				// jump into catch below
				throw new IllegalArgumentException();
			}

			return new HttpResponse(rData);
		}
		catch (Exception e)
		{
			throw new HttpStatusException(HttpStatusCode.NOT_FOUND,
										  "No data at " + sPath);
		}
	}
}
