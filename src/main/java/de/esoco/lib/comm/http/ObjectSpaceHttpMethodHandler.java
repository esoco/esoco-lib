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
import de.esoco.lib.logging.Log;

import org.obrel.space.ObjectSpace;
import org.obrel.space.SynchronizedObjectSpace;


/********************************************************************
 * A HTTP request method handler that retrieves the data of it's responses from
 * an {@link ObjectSpace}.
 *
 * @author eso
 */
public class ObjectSpaceHttpMethodHandler implements HttpRequestMethodHandler
{
	//~ Instance fields --------------------------------------------------------

	private ObjectSpace<? super String> rObjectSpace;
	private String					    sDefaultPath;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rObjectSpace The object space to get response data from
	 * @param sDefaultPath The default path to lookup for the GET method if the
	 *                     request path is empty
	 */
	public ObjectSpaceHttpMethodHandler(
		ObjectSpace<? super String> rObjectSpace,
		String						sDefaultPath)
	{
		this.rObjectSpace = new SynchronizedObjectSpace<>(rObjectSpace);
		this.sDefaultPath = sDefaultPath;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse doGet(HttpRequest rRequest) throws HttpStatusException
	{
		String sPath = rRequest.getPath();

		if (sPath.isEmpty() || sPath.equals("/"))
		{
			sPath = sDefaultPath;
		}

		try
		{
			Object rData = rObjectSpace.get(sPath);

			if (rData == null)
			{
				// jump into catch below
				throw new IllegalArgumentException();
			}

			return new HttpResponse(rData.toString());
		}
		catch (RuntimeException e)
		{
			throw new HttpStatusException(HttpStatusCode.NOT_FOUND,
										  "No data at " + sPath);
		}
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse doPost(HttpRequest rRequest) throws HttpStatusException
	{
		return update(rRequest);
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse doPut(HttpRequest rRequest) throws HttpStatusException
	{
		return update(rRequest);
	}

	/***************************************
	 * Performs an update due to a POST or PUT request.
	 *
	 * @param  rRequest The update request
	 *
	 * @return The response
	 *
	 * @throws HttpStatusException If the update is not allowed
	 */
	private HttpResponse update(HttpRequest rRequest) throws HttpStatusException
	{
		String sPath = rRequest.getPath();
		String sData = null;

		try
		{
			sData = rRequest.getBody();

			rObjectSpace.put(sPath, sData);

			return new HttpResponse("");
		}
		catch (HttpStatusException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			Log.errorf(e, "ObjectSpace update failed: %s - %s", sPath, sData);

			if (sData == null)
			{
				throw new HttpStatusException(HttpStatusCode.BAD_REQUEST,
											  "Could not access request data: " +
											  e.getMessage());
			}
			else
			{
				throw new HttpStatusException(HttpStatusCode.NOT_ACCEPTABLE,
											  "Invalid data '" + sData + "': " +
											  e.getMessage());
			}
		}
	}
}
