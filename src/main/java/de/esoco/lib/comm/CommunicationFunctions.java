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
package de.esoco.lib.comm;

import de.esoco.lib.datatype.Pair;
import de.esoco.lib.expression.Function;
import de.esoco.lib.net.NetUtil;

import java.util.Map;

import static de.esoco.lib.collection.CollectionUtil.orderedMapOf;


/********************************************************************
 * Provides factory methods of communication-related functions.
 *
 * @author eso
 */
public class CommunicationFunctions
{
	//~ Static fields/initializers ---------------------------------------------

	private static final String URL_INPUT_PARAM = "!_INPUT_!";

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Private, only static use.
	 */
	private CommunicationFunctions()
	{
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns a function that encodes a mapping of URL parameter names and
	 * their values.
	 *
	 * @param  rParams The string names and values of the URL parameters
	 *
	 * @return A function that
	 *
	 * @see    NetUtil#encodeUrlParameters(java.util.Map)
	 */
	@SafeVarargs
	public static Function<String, String> encodeUrlParameters(
		String					sInputParam,
		Pair<String, String>... rParams)
	{
		return encodeUrlParameters(sInputParam, orderedMapOf(rParams));
	}

	/***************************************
	 * Returns a function that encodes a mapping of URL parameter names and
	 * their values.
	 *
	 * @param  rParams The string names and values of the URL parameters
	 *
	 * @return A function that
	 *
	 * @see    NetUtil#encodeUrlParameters(java.util.Map)
	 */
	public static Function<String, String> encodeUrlParameters(
		String				sInputParam,
		Map<String, String> rParams)
	{
		String sEncodedParams = NetUtil.encodeUrlParameters(rParams);

		if (sInputParam != null)
		{
			sEncodedParams =
				String.format("%s=%s&%s",
							  NetUtil.encodeUrlElement(sInputParam),
							  URL_INPUT_PARAM,
							  sEncodedParams);
		}

		String sUrlParams = sEncodedParams;

		return sParamValue ->
			   sUrlParams.replace(URL_INPUT_PARAM,
								  NetUtil.encodeUrlElement(sParamValue));
	}
}
