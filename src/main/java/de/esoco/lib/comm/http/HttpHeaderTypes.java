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

import de.esoco.lib.text.TextConvert;

import java.util.Collection;

import org.obrel.core.Annotations.RelationTypeNamespace;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;
import org.obrel.type.CollectorType;

import static org.obrel.core.RelationTypeModifier.FINAL;
import static org.obrel.core.RelationTypeModifier.READONLY;
import static org.obrel.core.RelationTypes.newEnumType;
import static org.obrel.core.RelationTypes.newStringType;


/********************************************************************
 * Definitions of relation types that represent the names and datatypes of
 * header fields in HTTP requests.
 *
 * @author eso
 */
@RelationTypeNamespace(HttpHeaderTypes.HTTP_HEADER_TYPES_NAMESPACE)
public class HttpHeaderTypes
{
	//~ Enums ------------------------------------------------------------------

	/********************************************************************
	 * Enumeration of HTTP header field names.
	 */
	public enum HttpHeaderField
	{
		ACCEPT, ACCEPT_CHARSET, AUTHORIZATION, CONNECTION, CONTENT_LENGTH,
		CONTENT_TYPE, COOKIE, HOST, USER_AGENT,
		WWW_AUTHENTICATE("WWW-Authenticate");

		//~ Instance fields ----------------------------------------------------

		private final String sFieldName;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance with a field name derived from the instance
		 * name.
		 */
		private HttpHeaderField()
		{
			this.sFieldName = TextConvert.capitalize(name(), "-");
		}

		/***************************************
		 * Creates a new instance with an explicit field name.
		 *
		 * @param sFieldName The field name
		 */
		private HttpHeaderField(String sFieldName)
		{
			this.sFieldName = sFieldName;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Returns the HTTP name of the header field.
		 *
		 * @return The field name
		 */
		public final String getFieldName()
		{
			return sFieldName;
		}
	}

	//~ Static fields/initializers ---------------------------------------------

	/** The namespace for the HTTP header types. */
	public static final String HTTP_HEADER_TYPES_NAMESPACE =
		"de.esoco.lib.comm.http";

	/** The {@link HttpHeaderField} associated with the header type. */
	// initialized with explicit name because it is used as a meta-type for the
	// subsequently declared relation types and must therefore be initialized
	public static final RelationType<HttpHeaderField> HTTP_HEADER_FIELD =
		newEnumType(HTTP_HEADER_TYPES_NAMESPACE + ".HTTP_HEADER_FIELD",
					HttpHeaderField.class,
					FINAL);

	/** The Accept header. */
	public static final RelationType<String> ACCEPT =
		newStringType().annotate(HTTP_HEADER_FIELD, HttpHeaderField.ACCEPT);

	/** The Accept-Charset header. */
	public static final RelationType<String> ACCEPT_CHARSET =
		newStringType().annotate(HTTP_HEADER_FIELD,
								 HttpHeaderField.ACCEPT_CHARSET);

	/** The Authorization header. */
	public static final RelationType<String> AUTHORIZATION =
		newStringType().annotate(HTTP_HEADER_FIELD,
								 HttpHeaderField.AUTHORIZATION);

	/** The Content-Length header. */
	public static final RelationType<Integer> CONTENT_LENGTH =
		RelationTypes.<Integer>newType()
					 .annotate(HTTP_HEADER_FIELD,
							   HttpHeaderField.CONTENT_LENGTH);

	/** The Content-Type header. */
	public static final RelationType<String> CONTENT_TYPE =
		newStringType().annotate(HTTP_HEADER_FIELD,
								 HttpHeaderField.CONTENT_TYPE);

	/** The Cookie header. */
	public static final RelationType<String> COOKIE =
		newStringType().annotate(HTTP_HEADER_FIELD, HttpHeaderField.COOKIE);

	/** The Host header. */
	public static final RelationType<String> HOST =
		newStringType().annotate(HTTP_HEADER_FIELD, HttpHeaderField.HOST);

	/** The User-Agent header. */
	public static final RelationType<String> USER_AGENT =
		newStringType().annotate(HTTP_HEADER_FIELD, HttpHeaderField.USER_AGENT);

	/**
	 * Collects all HTTP header types that have been set on an object. FINAL to
	 * prevent external modification.
	 */
	public static final RelationType<Collection<RelationType<?>>> HTTP_HEADER_TYPES =
		CollectorType.newDistinctCollector(RelationType.class,
										   (r, o) ->
										   r.getType()
										   .getName()
										   .startsWith(HTTP_HEADER_TYPES_NAMESPACE)
										   ? r.getType() : null,
										   READONLY);

	static
	{
		RelationTypes.init(HttpHeaderTypes.class);
	}

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

		return RelationType.valueOf(HttpHeaderTypes.HTTP_HEADER_TYPES_NAMESPACE +
									"." + sHeaderName);
	}
}
