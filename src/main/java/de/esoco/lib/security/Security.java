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

import de.esoco.lib.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.nio.charset.StandardCharsets;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.obrel.core.ObjectRelations;
import org.obrel.core.Relatable;
import org.obrel.type.StandardTypes;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import static de.esoco.lib.security.SecurityRelationTypes.CERTIFICATE_ALGORITHM;
import static de.esoco.lib.security.SecurityRelationTypes.CERTIFICATE_VALIDITY;
import static de.esoco.lib.security.SecurityRelationTypes.COMMON_NAME;
import static de.esoco.lib.security.SecurityRelationTypes.COUNTRY;
import static de.esoco.lib.security.SecurityRelationTypes.KEY_ALGORITHM;
import static de.esoco.lib.security.SecurityRelationTypes.KEY_PASSWORD;
import static de.esoco.lib.security.SecurityRelationTypes.KEY_SIZE;
import static de.esoco.lib.security.SecurityRelationTypes.LOCALITY;
import static de.esoco.lib.security.SecurityRelationTypes.ORGANIZATION;
import static de.esoco.lib.security.SecurityRelationTypes.ORGANIZATION_UNIT;
import static de.esoco.lib.security.SecurityRelationTypes.SIGNING_CERTIFICATE;
import static de.esoco.lib.security.SecurityRelationTypes.STATE_PROVINCE_REGION;

import static org.obrel.type.StandardTypes.START_DATE;

import static sun.security.x509.CertificateVersion.V3;


/********************************************************************
 * Contains static helper methods for typical security tasks like the creation
 * of cryptographic certificates and keys and configuring Java security key
 * stores and SSL contexts. Relies on the security relation types defined in
 * {@link SecurityRelationTypes}.
 *
 * @author eso
 */
public class Security
{
	//~ Static fields/initializers ---------------------------------------------

	/** A standard {@link KeyStore} alias for a server certificate. */
	public static final String ALIAS_SERVER_CERT = "_ServerCert";

	/**
	 * A standard {@link KeyStore} alias for the signing certificate in a
	 * certificate creation request.
	 */
	public static final String ALIAS_SIGNING_CERT = "_SigningCert";

	/**
	 * A standard {@link KeyStore} alias for the certificate that has been
	 * created in a certificate creation request.
	 */
	public static final String ALIAS_GENERATED_CERT = "_GeneratedCert";

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Private, only static use.
	 */
	private Security()
	{
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Creates a new X509 certificate from the given parameters and returns a
	 * key store containing the certificate and it's private key. The
	 * certificate and key will be stored in the key store under the alias
	 * {@link #ALIAS_GENERATED_CERT}. The following parameters must be provided
	 * or else an exception will be thrown:
	 *
	 * <ul>
	 *   <li>{@link SecurityRelationTypes#COMMON_NAME}: the common name of the
	 *     certificate.</li>
	 *   <li>{@link SecurityRelationTypes#CERTIFICATE_VALIDITY}: the validity of
	 *     the certificate in days.</li>
	 *   <li>{@link SecurityRelationTypes#KEY_PASSWORD}: the password under
	 *     which to store the private key of the certificate in the returned key
	 *     store. Also the password that will be used to access a key store
	 *     containing a signing certificate (see optional parameters
	 *     below).</li>
	 * </ul>
	 *
	 * <p>The following parameters can optionally been set or else the default
	 * values of the relation types will be used. For the text fields of the
	 * certificates the defaults will be empty strings.</p>
	 *
	 * <ul>
	 *   <li>{@link StandardTypes#START_DATE}: the date from which the
	 *     certificate will be valid (defaults to the time of invocation).</li>
	 *   <li>{@link SecurityRelationTypes#KEY_SIZE}: the bit size of the private
	 *     key for the certificate.</li>
	 *   <li>{@link SecurityRelationTypes#KEY_ALGORITHM}: the algorithm for the
	 *     private key generation.</li>
	 *   <li>{@link SecurityRelationTypes#SIGNING_CERTIFICATE}: A {@link
	 *     KeyStore} that contains a certificate and private key to sign the new
	 *     certificate with. The password to access it private key must be set
	 *     in the parameters with {@link SecurityRelationTypes#KEY_PASSWORD} and
	 *     the alias must be {@link #ALIAS_SIGNING_CERT}. If not present the new
	 *     certificate will be self-signed.</li>
	 *   <li>{@link SecurityRelationTypes#CERTIFICATE_ALGORITHM}</li>
	 *   <li>{@link SecurityRelationTypes#ORGANIZATION}</li>
	 *   <li>{@link SecurityRelationTypes#ORGANIZATION_UNIT}</li>
	 *   <li>{@link SecurityRelationTypes#LOCALITY}</li>
	 *   <li>{@link SecurityRelationTypes#STATE_PROVINCE_REGION}</li>
	 *   <li>{@link SecurityRelationTypes#COUNTRY}</li>
	 * </ul>
	 *
	 * @param  rParams The parameters to create the certificate from
	 *
	 * @return A new X509 certificate, signed with the given key or self-signed
	 *
	 * @throws IllegalArgumentException Mapped from exceptions that occur if the
	 *                                  certificate generation or signing fails
	 *                                  for some reason
	 */
	public static KeyStore createCertificate(Relatable rParams)
	{
		ObjectRelations.requireNonNull(rParams,
									   KEY_PASSWORD,
									   CERTIFICATE_VALIDITY);

		KeyStore rSigningKeyStore = rParams.get(SIGNING_CERTIFICATE);
		String   sKeyPassword     = rParams.get(KEY_PASSWORD);
		String   sCertAlgorithm   = rParams.get(CERTIFICATE_ALGORITHM);
		String   sKeyAlgorithm    = rParams.get(KEY_ALGORITHM);
		Date     rStartDate		  = rParams.get(START_DATE);

		int nKeySize  = rParams.get(KEY_SIZE).intValue();
		int nValidity = rParams.get(CERTIFICATE_VALIDITY).intValue();

		X500Name     aSubject  = createX500Name(rParams);
		X500Name     aIssuer   = aSubject;
		X509CertInfo aCertInfo = new X509CertInfo();
		Calendar     aEndDate  = Calendar.getInstance();

		X509CertImpl	  aCertificate;
		X509Certificate[] aCertChain;

		KeyPair aCertKeys;
		KeyPair aSigningKeys;
		String  sIssuer;

		if (rStartDate == null)
		{
			rStartDate = new Date();
		}

		aCertKeys = generateKeyPair(sKeyAlgorithm, nKeySize);

		X509Certificate rSigningCert = null;

		if (rSigningKeyStore != null)
		{
			PrivateKey rSigningKey;

			try
			{
				rSigningCert =
					(X509Certificate) rSigningKeyStore.getCertificate(ALIAS_SIGNING_CERT);

				rSigningKey =
					(PrivateKey) rSigningKeyStore.getKey(ALIAS_SIGNING_CERT,
														 sKeyPassword
														 .toCharArray());
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException(e);
			}

			if (rSigningCert == null || rSigningKey == null)
			{
				throw new IllegalArgumentException("KeyStore doesn't contain " +
												   "valid signing data");
			}

			aSigningKeys = new KeyPair(aCertKeys.getPublic(), rSigningKey);
			sIssuer		 = rSigningCert.getSubjectX500Principal().toString();
			aCertChain   = new X509Certificate[] { null, rSigningCert };
		}
		else // self-signing
		{
			aSigningKeys = aCertKeys;
			sIssuer		 = aSubject.toString();
			aCertChain   = new X509Certificate[1];
		}

		aEndDate.setTime(rStartDate);
		aEndDate.add(Calendar.DAY_OF_YEAR, nValidity);

		CertificateValidity aCertValidity =
			new CertificateValidity(rStartDate, aEndDate.getTime());

		CertificateSerialNumber aSerial =
			new CertificateSerialNumber(new java.util.Random().nextInt() &
										0x7fffffff);

		CertificateX509Key aPublicKey =
			new CertificateX509Key(aSigningKeys.getPublic());

		try
		{
			CertificateAlgorithmId aAlgorithmId =
				new CertificateAlgorithmId(AlgorithmId.get(sCertAlgorithm));

			if (sIssuer != null)
			{
				aIssuer = new X500Name(sIssuer);
			}

			aCertInfo.set(X509CertInfo.SUBJECT, aSubject);
			aCertInfo.set(X509CertInfo.ISSUER, aIssuer);
			aCertInfo.set(X509CertInfo.VALIDITY, aCertValidity);
			aCertInfo.set(X509CertInfo.VERSION, new CertificateVersion(V3));
			aCertInfo.set(X509CertInfo.SERIAL_NUMBER, aSerial);
			aCertInfo.set(X509CertInfo.ALGORITHM_ID, aAlgorithmId);
			aCertInfo.set(X509CertInfo.KEY, aPublicKey);

			aCertificate = new X509CertImpl(aCertInfo);
			aCertificate.sign(aSigningKeys.getPrivate(), sCertAlgorithm);

			aCertChain[0] = aCertificate;

			KeyStore aKeyStore =
				createKeyStore(ALIAS_GENERATED_CERT,
							   sKeyPassword,
							   aCertKeys.getPrivate(),
							   aCertChain);

			return aKeyStore;
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/***************************************
	 * Creates a Java security key store containing a certain certificate chain
	 * and it's private key.
	 *
	 * @param  sAlias      The alias name for the certificate and key
	 * @param  sPassword   The password for the private key
	 * @param  rPrivateKey The private key of the first certificate in the chain
	 * @param  rCertChain  The certificate chain which must contain at least one
	 *                     certificate
	 *
	 * @return A new key store with the certificate chain and key
	 *
	 * @throws IllegalStateException If the certificate generation fails because
	 *                               of unavailable algorithms
	 */
	public static KeyStore createKeyStore(String			sAlias,
										  String			sPassword,
										  PrivateKey		rPrivateKey,
										  X509Certificate[] rCertChain)
	{
		try
		{
			KeyStore aKeyStore = KeyStore.getInstance("JKS");

			aKeyStore.load(null);
			aKeyStore.setKeyEntry(sAlias,
								  rPrivateKey,
								  sPassword.toCharArray(),
								  rCertChain);

			return aKeyStore;
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	/***************************************
	 * decodes data that is encoded in base64 format, optionally in PEM format.
	 * The latter wraps the base64 data in uppercase begin and end tokens on
	 * separate lines describing the contained data enclosed in five dash
	 * characters on each side (e.g. '-----BEGIN CERTIFICATE-----' and '-----END
	 * CERTIFICATE-----') according to <a
	 * href="https://tools.ietf.org/html/rfc7468">RFC 7468</a>.
	 *
	 * @param  sBase64 A string containing the Base64 encoded data
	 *
	 * @return The decoded binary data
	 */
	public static byte[] decodeBase64(String sBase64)
	{
		sBase64 =
			sBase64.replaceAll("-----.*\n", "").replaceAll("\n", "").trim();

		return Base64.getDecoder()
					 .decode(sBase64.getBytes(StandardCharsets.UTF_8));
	}

	/***************************************
	 * Reads an X509 certificate from an input stream.
	 *
	 * @param  rCertData The input stream containing the certificate data
	 *
	 * @return The X509 certificate
	 *
	 * @throws IllegalArgumentException If the certificate creation fails
	 */
	public static X509Certificate decodeCertificate(byte[] rCertData)
	{
		try
		{
			ByteArrayInputStream aData = new ByteArrayInputStream(rCertData);

			return (X509Certificate) CertificateFactory.getInstance("X.509")
													   .generateCertificate(aData);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/***************************************
	 * Reads the binary data of a private key in PKCS8 format.
	 *
	 * @param  rPkcs8Key An input stream providing the PKCS8 key data
	 *
	 * @return A new private key instance
	 *
	 * @throws IllegalArgumentException If the key cannot be created from the
	 *                                  input
	 */
	public static PrivateKey decodePrivateKey(byte[] rPkcs8Key)
	{
		try
		{
			PKCS8EncodedKeySpec aKeySpec = new PKCS8EncodedKeySpec(rPkcs8Key);

			return KeyFactory.getInstance("RSA").generatePrivate(aKeySpec);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/***************************************
	 * Decrypts a string value with the given passphrase by using the AES
	 * algorithm. The passphrase will be converted into a 128 bit AES key by
	 * applying the method {@link #deriveKey(String, String, int)}.
	 *
	 * @param  rData       The data to decrypt
	 * @param  sPassphrase The passphrase
	 *
	 * @return The decrypted data
	 */
	public static String decrypt(byte[] rData, String sPassphrase)
	{
		try
		{
			Cipher aCipher = Cipher.getInstance("AES");
			Key    aKey    = deriveKey(sPassphrase, "AES", 128);

			aCipher.init(Cipher.DECRYPT_MODE, aKey);

			return new String(aCipher.doFinal(rData),
							  StandardCharsets.UTF_8.name());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/***************************************
	 * Derives an cryptographic key with a certain length from a passphrase. The
	 * key will be generated as a SHA-256 hash of the passphrase which will then
	 * be reduced to the requested bit length.
	 *
	 * @param  sPassphrase The passphrase
	 * @param  sAlgorithm  The algorithm of the returned key
	 * @param  nBitLength  The bit length of the returned key
	 *
	 * @return The resulting key
	 *
	 * @throws SecurityException If deriving the key fails
	 */
	public static Key deriveKey(String sPassphrase,
								String sAlgorithm,
								int    nBitLength)
	{
		try
		{
			byte[] aKeyHash =
				Arrays.copyOf(MessageDigest.getInstance("SHA-256")
							  .digest(sPassphrase.getBytes(StandardCharsets
														   .UTF_8.name())),
							  nBitLength / 8);

			return new SecretKeySpec(aKeyHash, sAlgorithm);
		}
		catch (Exception e)
		{
			throw new SecurityException(e);
		}
	}

	/***************************************
	 * This method will enable java cryptographic extensions on the application
	 * level if possible.
	 *
	 * <p><b>Attention</b>: this performs a rather dirty hack by using
	 * reflection to set a field in the class {@link JceSecurity} to FALSE. That
	 * is necessary because otherwise some JCE security algorithms are not
	 * availabe. Whether or not this actually works depends on the platform that
	 * the application is deployed to and its JRE. Keep an eye out for logged
	 * errors that might occur at application startup/initialization of this
	 * endpoint.</p>
	 */
	public static void enableJavaCryptographicExtensions()
	{
		try
		{
			Field rField =
				Class.forName("javax.crypto.JceSecurity")
					 .getDeclaredField("isRestricted");

			rField.setAccessible(true);

			Field rModifiersField = Field.class.getDeclaredField("modifiers");

			rModifiersField.setAccessible(true);
			rModifiersField.setInt(rField,
								   rField.getModifiers() & ~Modifier.FINAL);

			rField.set(null, Boolean.FALSE);
		}
		catch (Exception e)
		{
			Log.error("Unable to enable Java Cryptographic Extensions. " +
					  "Some ciphers may be unavailable in the current JRE.",
					  e);
		}
	}

	/***************************************
	 * Encodes binary cryptographic data into base64 in PEM format according to
	 * <a href="https://tools.ietf.org/html/rfc7468">RFC 7468</a>.
	 *
	 * @param  rData  The binary data to encode
	 * @param  sToken The token for the PEM delimiters describing the encoded
	 *                data (will be converted to uppercase)
	 *
	 * @return An ASCII string containing the encoded data
	 */
	public static String encodeBase64(byte[] rData, String sToken)
	{
		String		  sBase64  = Base64.getEncoder().encodeToString(rData);
		StringBuilder aEncoded = new StringBuilder("-----BEGIN ");
		int			  nMax     = sBase64.length();

		sToken = sToken.toUpperCase();
		aEncoded.append(sToken).append("-----\n");

		for (int i = 0; i < nMax; i += 64)
		{
			int l = nMax - i;

			aEncoded.append(sBase64.substring(i, i + (l > 64 ? 64 : l)));
			aEncoded.append('\n');
		}

		aEncoded.append("-----END ").append(sToken).append("-----\n");

		return aEncoded.toString();
	}

	/***************************************
	 * Encrypts a string value with the given passphrase by using the AES
	 * algorithm. The passphrase will be converted into a 128 bit AES key by
	 * applying the method {@link #deriveKey(String, String, int)}.
	 *
	 * @param  sValue      The value to encrypt
	 * @param  sPassphrase The passphrase
	 *
	 * @return The encrypted data
	 */
	public static byte[] encrypt(String sValue, String sPassphrase)
	{
		try
		{
			Cipher aCipher = Cipher.getInstance("AES");
			Key    aKey    = deriveKey(sPassphrase, "AES", 128);

			aCipher.init(Cipher.ENCRYPT_MODE, aKey);

			return aCipher.doFinal(sValue.getBytes());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/***************************************
	 * Generates a key pair with the given algorithm and key size.
	 *
	 * @param  sAlgorithm The algorithm name as defined in the Java cryptography
	 *                    packages
	 * @param  nKeySize   The number of bits in the key
	 *
	 * @return The generated key pair
	 *
	 * @throws IllegalArgumentException If the given algorithm could not be
	 *                                  found (mapped from {@link
	 *                                  NoSuchAlgorithmException})
	 */
	public static KeyPair generateKeyPair(String sAlgorithm, int nKeySize)
	{
		try
		{
			SecureRandom     aRandom    = new SecureRandom();
			KeyPairGenerator rGenerator =
				KeyPairGenerator.getInstance(sAlgorithm);

			rGenerator.initialize(nKeySize, aRandom);

			return rGenerator.generateKeyPair();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/***************************************
	 * Initializes an SSL context from a key store so that it will use the
	 * certificates from the key store for SSL/TLS connections.
	 *
	 * @param  rKeyStore         The key store to initialize the context from
	 * @param  sKeyStorePassword The key store password
	 *
	 * @return A new SSL context initialized to use the given key store
	 *
	 * @throws IllegalArgumentException If the parameters are invalid or refer
	 *                                  to unavailable security algorithms
	 */
	public static SSLContext getSslContext(
		KeyStore rKeyStore,
		String   sKeyStorePassword)
	{
		try
		{
			KeyManagerFactory   aKeyManagerFactory   =
				KeyManagerFactory.getInstance("SunX509");
			TrustManagerFactory aTrustManagerFactory =
				TrustManagerFactory.getInstance("SunX509");

			aKeyManagerFactory.init(rKeyStore, sKeyStorePassword.toCharArray());
			aTrustManagerFactory.init(rKeyStore);

			SSLContext     aSslContext    = SSLContext.getInstance("TLS");
			TrustManager[] rTrustManagers =
				aTrustManagerFactory.getTrustManagers();

			aSslContext.init(aKeyManagerFactory.getKeyManagers(),
							 rTrustManagers,
							 null);

			return aSslContext;
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/***************************************
	 * Creates a new instance of the sun-internal security class {@link
	 * X500Name} from parameters stored in a relatable object.
	 *
	 * @param  rCertParams The relatable containing the certificate parameters
	 *                     as defined in {@link SecurityRelationTypes}
	 *
	 * @return The new X500 certificate name
	 *
	 * @throws IllegalArgumentException If the parsing of the parameters fails
	 */
	private static X500Name createX500Name(Relatable rCertParams)
	{
		ObjectRelations.requireNonNull(rCertParams, COMMON_NAME);

		try
		{
			return new X500Name(rCertParams.get(COMMON_NAME),
								rCertParams.get(ORGANIZATION_UNIT),
								rCertParams.get(ORGANIZATION),
								rCertParams.get(LOCALITY),
								rCertParams.get(STATE_PROVINCE_REGION),
								rCertParams.get(COUNTRY));
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
}
