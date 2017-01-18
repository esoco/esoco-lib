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

import org.obrel.core.Annotations.RelationTypeNamespace;
import org.obrel.core.RelationType;

import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * Definitions of relation types that represent the names and datatypes of
 * header fields in HTTP requests.
 *
 * @author eso
 */
@RelationTypeNamespace("de.esoco.lib.com.http")
public class HttpHeaderTypes
{
	//~ Static fields/initializers ---------------------------------------------

	/** The HTTP Accept header. */
	public static final RelationType<String> ACCEPT = newType();

	/** The HTTP Accept-Charset header. */
	public static final RelationType<String> ACCEPT_CHARSET = newType();

	/** The HTTP Content-Length header. */
	public static final RelationType<Integer> CONTENT_LENGTH = newType();

	/** The HTTP Content-Type header. */
	public static final RelationType<String> CONTENT_TYPE = newType();

	/** The HTTP Cookie header. */
	public static final RelationType<String> COOKIE = newType();

	/** The HTTP Host header. */
	public static final RelationType<String> HOST = newType();

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns the corresponding relation type for a certain HTTP request header
	 * field name.
	 *
	 * @param  sHeaderName The HTTP request header field name
	 *
	 * @return
	 */
	public static RelationType<?> get(String sHeaderName)
	{
		sHeaderName = sHeaderName.replaceAll("-", "_").toUpperCase();

		return RelationType.valueOf("de.esoco.lib.com.http." + sHeaderName);
	}
}
