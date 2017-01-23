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

import de.esoco.lib.event.ElementEvent.EventType;
import de.esoco.lib.expression.Function;
import de.esoco.lib.text.TextConvert;

import java.util.Set;

import org.obrel.core.Annotations.RelationTypeNamespace;
import org.obrel.core.Relatable;
import org.obrel.core.Relation;
import org.obrel.core.RelationEvent;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypeModifier;
import org.obrel.type.SetType;
import org.obrel.type.StandardTypes;

import static org.obrel.core.RelationTypes.newIntType;
import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * Definitions of relation types that represent the names and datatypes of
 * header fields in HTTP requests.
 *
 * @author eso
 */
@RelationTypeNamespace("de.esoco.lib.comm.http")
public class HttpHeaderTypes
{
	//~ Enums ------------------------------------------------------------------

	/********************************************************************
	 * Enumeration of HTTP header field names.
	 */
	public enum HttpHeaderField
	{
		ACCEPT, ACCEPT_CHARSET, CONNECTION, CONTENT_LENGTH, CONTENT_TYPE,
		COOKIE, HOST;

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

	/** The HTTP Accept header. */
	public static final RelationType<String> ACCEPT = newType();

	/** The HTTP Accept-Charset header. */
	public static final RelationType<String> ACCEPT_CHARSET = newType();

	/** The HTTP Content-Length header. */
	public static final RelationType<Integer> CONTENT_LENGTH = newIntType();

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

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A relation type that collects values on relation updates.
	 *
	 * @author eso
	 */
	public static class CollectorType<T> extends SetType<T>
	{
		//~ Static fields/initializers -----------------------------------------

		private static final long serialVersionUID = 1L;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sName          The name of this type
		 * @param rCollectedType The datatype of the collected values
		 * @param rModifiers     The relation type modifiers
		 */
		public CollectorType(String					 sName,
							 Class<T>				 rCollectedType,
							 RelationTypeModifier... rModifiers)
		{
			super(sName, rCollectedType, true, rModifiers);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * @see RelationType#addRelation(Relatable, Relation)
		 */
		@Override
		protected void addRelation(
			Relatable		 rParent,
			Relation<Set<T>> rRelation)
		{
			super.addRelation(rParent, rRelation);

			rParent.get(StandardTypes.RELATION_LISTENERS)
				   .add(this::processEvent);
		}

		/***************************************
		 * Processes a relation event.
		 *
		 * @param rEvent The relation event
		 */
		protected void processEvent(RelationEvent<?> rEvent)
		{
			Function<Relation, T> fCollector = null;

			if (fCollector != null)
			{
				T rValue = fCollector.evaluate(rEvent.getElement());

				if (rValue != null)
				{
					if (rEvent.getType() == EventType.ADD)
					{
						rEvent.getSource().get(this).add(rValue);
					}
					else if (rEvent.getType() == EventType.REMOVE)
					{
						rEvent.getSource().get(this).remove(rValue);
					}
				}
			}
		}
	}
}
