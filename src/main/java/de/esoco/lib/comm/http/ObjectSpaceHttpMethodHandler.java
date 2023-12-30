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

/**
 * A HTTP request method handler that retrieves the data of it's responses from
 * an {@link ObjectSpace}.
 *
 * @author eso
 */
public class ObjectSpaceHttpMethodHandler implements HttpRequestMethodHandler {

	private final ObjectSpace<? super String> objectSpace;

	private final String defaultPath;

	/**
	 * Creates a new instance.
	 *
	 * @param objectSpace The object space to get response data from
	 * @param defaultPath The default path to lookup for the GET method if the
	 *                    request path is empty
	 */
	public ObjectSpaceHttpMethodHandler(ObjectSpace<? super String> objectSpace,
		String defaultPath) {
		this.objectSpace = new SynchronizedObjectSpace<>(objectSpace);
		this.defaultPath = defaultPath;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse doGet(HttpRequest request) throws HttpStatusException {
		String path = request.getPath();

		if (path.isEmpty() || path.equals("/")) {
			path = defaultPath;
		}

		try {
			Object data = objectSpace.get(path);

			if (data == null) {
				// jump into catch below
				throw new IllegalArgumentException();
			}

			return new HttpResponse(data.toString());
		} catch (RuntimeException e) {
			throw new HttpStatusException(HttpStatusCode.NOT_FOUND,
				"No data at " + path);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse doPost(HttpRequest request) throws HttpStatusException {
		return update(request);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse doPut(HttpRequest request) throws HttpStatusException {
		return update(request);
	}

	/**
	 * Performs an update due to a POST or PUT request.
	 *
	 * @param request The update request
	 * @return The response
	 * @throws HttpStatusException If the update is not allowed
	 */
	private HttpResponse update(HttpRequest request)
		throws HttpStatusException {
		String path = request.getPath();
		String data = null;

		try {
			data = request.getBody();

			objectSpace.put(path, data);

			return new HttpResponse("");
		} catch (HttpStatusException e) {
			throw e;
		} catch (Exception e) {
			Log.errorf(e, "ObjectSpace update failed: %s - %s", path, data);

			if (data == null) {
				throw new HttpStatusException(HttpStatusCode.BAD_REQUEST,
					"Could not access request data: " + e.getMessage());
			} else {
				throw new HttpStatusException(HttpStatusCode.NOT_ACCEPTABLE,
					"Invalid data '" + data + "': " + e.getMessage());
			}
		}
	}
}
