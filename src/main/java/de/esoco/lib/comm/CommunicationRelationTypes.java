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
package de.esoco.lib.comm;

import java.nio.charset.StandardCharsets;

import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import static org.obrel.core.RelationTypeModifier.FINAL;
import static org.obrel.core.RelationTypes.newFlagType;
import static org.obrel.core.RelationTypes.newInitialValueType;
import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * Contains standard relation type definitions for the communications framework.
 *
 * @author eso
 */
public class CommunicationRelationTypes
{
	//~ Static fields/initializers ---------------------------------------------

	/** A reference to a communication endpoint. */
	public static final RelationType<Endpoint> ENDPOINT = newType();

	/**
	 * The address of a communication endpoint (typically some kind of URI).
	 * This type is final so that it cannot be changed after it has been set.
	 */
	public static final RelationType<String> ENDPOINT_ADDRESS = newType(FINAL);

	/**
	 * Defines the character encoding to be used for the communication with an
	 * endpoint. Defaults to the name of {@link StandardCharsets#UTF_8}.
	 */
	public static final RelationType<String> ENDPOINT_ENCODING =
		newInitialValueType(StandardCharsets.UTF_8.name());

	/** A user name for the authentication on a communication endpoint. */
	public static final RelationType<String> USER_NAME = newType();

	/** A password for the authentication on a communication endpoint. */
	public static final RelationType<String> PASSWORD = newType();

	/**
	 * The maximum size that the response of an endpoint communication is
	 * allowed to have. Has a default value of 1 MiB.
	 */
	@SuppressWarnings("boxing")
	public static final RelationType<Integer> MAXIMUM_RESPONSE_SIZE =
		newInitialValueType(1024 * 1024);

	/**
	 * A flag that indicates that a connection performs encrypted communication
	 * like SSL, TLS, or SSH. A final relation that is determined during
	 * initialization.
	 */
	public static final RelationType<Boolean> ENCRYPTED_CONNECTION =
		newFlagType(FINAL);

	/**
	 * A flag to enabled SSL/TLS connections to endpoints that use self-signed
	 * certificates. ATTENTION: this should only be used for test environments,
	 * never for production!
	 */
	public static final RelationType<Boolean> TRUST_SELF_SIGNED_CERTIFICATES =
		newFlagType();

	static
	{
		RelationTypes.init(CommunicationRelationTypes.class);
	}

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 */
	private CommunicationRelationTypes()
	{
	}
}
