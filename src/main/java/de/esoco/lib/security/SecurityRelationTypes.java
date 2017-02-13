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
package de.esoco.lib.security;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;

import org.obrel.core.RelationType;
import org.obrel.core.RelationTypes;

import static org.obrel.core.RelationTypes.newInitialValueType;
import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * Contains security-related relation types.
 *
 * @author eso
 */
public class SecurityRelationTypes
{
	//~ Static fields/initializers ---------------------------------------------

	/** A login name for authentication purposes. */
	public static final RelationType<String> LOGIN_NAME = newType();

	/** A string token describing the authentication method to apply. */
	public static final RelationType<String> AUTHENTICATION_METHOD = newType();

	/** An authentication password. */
	public static final RelationType<String> PASSWORD = newType();

	/**
	 * An authentication credential in text form (e.g. the text representation
	 * of a certificate).
	 */
	public static final RelationType<String> CREDENTIAL = newType();

	/** An alias name for a cryptographic element. */
	public static final RelationType<String> ALIAS = newType();

	/**
	 * An authentication credential in binary form (e.g. the binary
	 * representation of a certificate).
	 */
	public static final RelationType<byte[]> BINARY_CREDENTIAL = newType();

	/**
	 * The certificate of a certificate authority. For typical usage the data
	 * should be in a format that can be processed by {@link
	 * CertificateFactory#generateCertificate(java.io.InputStream)}.
	 */
	public static final RelationType<byte[]> CA_CERTIFICATE = newType();

	/**
	 * The private key for the certificate of a certificate authority. For
	 * typical usage the data should be in PKCS8 format that can be handed to
	 * {@link PKCS8EncodedKeySpec}.
	 */
	public static final RelationType<byte[]> CA_PRIVATE_KEY = newType();

	/** A reference to an {@link AuthenticationService}. */
	public static final RelationType<AuthenticationService> AUTHENTICATION_SERVICE =
		RelationTypes.newType();

	/**
	 * A Java security key store that holds cryptographic keys and certificates.
	 */
	public static final RelationType<KeyStore> KEY_STORE = newType();

	/** A pair of public and private key for asymmetric cryptography. */
	public static final RelationType<KeyPair> KEY_PAIR = newType();

	/** The size of a cryptographic key. Defaults to 2048 bit. */
	@SuppressWarnings("boxing")
	public static final RelationType<Integer> KEY_SIZE =
		newInitialValueType(2048);

	/** The password for a cryptographic key. */
	public static final RelationType<String> KEY_PASSWORD = newType();

	/**
	 * The algorithm for the creation of a cryptographic key. Defaults to 'RSA'.
	 */
	public static final RelationType<String> KEY_ALGORITHM =
		newInitialValueType("RSA");

	/**
	 * The algorithm for the creation of a cryptographic certificate. Defaults
	 * to 'SHA256withRSA'.
	 */
	public static final RelationType<String> CERTIFICATE_ALGORITHM =
		newInitialValueType("SHA256withRSA");

	/**
	 * The issuer of a cryptographic certificate in standard string form (like
	 * 'CN=&lt;CommonName&gt;, O=&lt;Organization&gt;,...'.
	 */
	public static final RelationType<String> CERTIFICATE_ISSUER = newType();

	/** The validity of a cryptographic certificate in days. */
	public static final RelationType<Integer> CERTIFICATE_VALIDITY = newType();

	/** The common name of a cryptographic certificate. */
	public static final RelationType<String> COMMON_NAME = newType();

	/**
	 * The organization name of a cryptographic certificate. Defaults to an
	 * empty string.
	 */
	public static final RelationType<String> ORGANIZATION =
		newInitialValueType("");

	/**
	 * The organization unit of a cryptographic certificate. Defaults to an
	 * empty string.
	 */
	public static final RelationType<String> ORGANIZATION_UNIT =
		newInitialValueType("");

	/**
	 * The locality name (typically a city name) of a cryptographic certificate.
	 * Defaults to an empty string.
	 */
	public static final RelationType<String> LOCALITY = newInitialValueType("");

	/**
	 * The state, province, or region name of a cryptographic certificate.
	 * Defaults to an empty string.
	 */
	public static final RelationType<String> STATE_PROVINCE_REGION =
		newInitialValueType("");

	/**
	 * The country name of a cryptographic certificate. Defaults to an empty
	 * string.
	 */
	public static final RelationType<String> COUNTRY = newInitialValueType("");

	static
	{
		RelationTypes.init(SecurityRelationTypes.class);
	}

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Private, only static use.
	 */
	private SecurityRelationTypes()
	{
	}
}
