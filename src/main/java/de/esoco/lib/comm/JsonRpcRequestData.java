//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
package de.esoco.lib.comm;

import de.esoco.lib.json.JsonObject;

/**
 * A JSON object that initializes and contains the data of a JSON RPC request.
 *
 * @author eso
 */
public class JsonRpcRequestData extends JsonObject {

	/**
	 * Creates a new instance.
	 *
	 * @param id     The ID of the request (a string or an integer number)
	 * @param method The RPC method to call
	 */
	public JsonRpcRequestData(Object id, String method) {
		set("jsonrpc", "2.0");
		set("id", id);
		set("method", method);
	}

	/**
	 * Adds or removes parameters to this request.
	 *
	 * @param params The parameters to add or NULL to remove all parameters
	 * @return This instance for fluent invocation
	 */
	public JsonRpcRequestData withParams(Object params) {
		if (params != null) {
			set("params", params);
		} else {
			remove("params");
		}

		return this;
	}
}
