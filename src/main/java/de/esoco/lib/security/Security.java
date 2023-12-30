//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
import de.esoco.lib.text.TextUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
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
import java.util.UUID;

/**
 * Contains static helper methods for typical security tasks like the creation
 * of cryptographic certificates and keys and configuring Java security key
 * stores and SSL contexts. Relies on the security relation types defined in
 * {@link SecurityRelationTypes}.
 *
 * @author eso
 */
public class Security {

	/**
	 * A standard {@link KeyStore} alias for a server certificate.
	 */
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

	/**
	 * Private, only static use.
	 */
	private Security() {
	}

	/**
	 * Creates a Java security key store containing a certain certificate chain
	 * and it's private key.
	 *
	 * @param alias      The alias name for the certificate and key
	 * @param password   The password for the private key
	 * @param privateKey The private key of the first certificate in the chain
	 * @param certChain  The certificate chain which must contain at least one
	 *                   certificate
	 * @return A new key store with the certificate chain and key
	 * @throws IllegalStateException If the certificate generation fails
	 * because
	 *                               of unavailable algorithms
	 */
	public static KeyStore createKeyStore(String alias, String password,
		PrivateKey privateKey, X509Certificate[] certChain) {
		try {
			KeyStore keyStore = KeyStore.getInstance("JKS");

			keyStore.load(null);
			keyStore.setKeyEntry(alias, privateKey, password.toCharArray(),
				certChain);

			return keyStore;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * decodes data that is encoded in base64 format, optionally in PEM format.
	 * The latter wraps the base64 data in uppercase begin and end tokens on
	 * separate lines describing the contained data enclosed in five dash
	 * characters on each side (e.g. '-----BEGIN CERTIFICATE-----' and
	 * '-----END
	 * CERTIFICATE-----') according to <a
	 * href="https://tools.ietf.org/html/rfc7468">RFC 7468</a>.
	 *
	 * @param base64 A string containing the Base64 encoded data
	 * @return The decoded binary data
	 */
	public static byte[] decodeBase64(String base64) {
		base64 =
			base64.replaceAll("-----.*\n", "").replaceAll("\n", "").trim();

		return Base64
			.getDecoder()
			.decode(base64.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Reads an X509 certificate from an input stream.
	 *
	 * @param certData The input stream containing the certificate data
	 * @return The X509 certificate
	 * @throws IllegalArgumentException If the certificate creation fails
	 */
	public static X509Certificate decodeCertificate(byte[] certData) {
		try {
			ByteArrayInputStream data = new ByteArrayInputStream(certData);

			return (X509Certificate) CertificateFactory
				.getInstance("X.509")
				.generateCertificate(data);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Reads the binary data of a private key in PKCS8 format.
	 *
	 * @param pkcs8Key An input stream providing the PKCS8 key data
	 * @return A new private key instance
	 * @throws IllegalArgumentException If the key cannot be created from the
	 *                                  input
	 */
	public static PrivateKey decodePrivateKey(byte[] pkcs8Key) {
		try {
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Key);

			return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Decrypts a string value with the given passphrase by using the AES
	 * algorithm. The passphrase will be converted into a 128 bit AES key by
	 * applying the method {@link #deriveKey(String, String, int)}.
	 *
	 * @param data       The data to decrypt
	 * @param passphrase The passphrase
	 * @return The decrypted data
	 */
	public static String decrypt(byte[] data, String passphrase) {
		try {
			Cipher cipher = Cipher.getInstance("AES");
			Key key = deriveKey(passphrase, "AES", 128);

			cipher.init(Cipher.DECRYPT_MODE, key);

			return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Derives an cryptographic key with a certain length from a passphrase.
	 * The
	 * key will be generated as a SHA-256 hash of the passphrase which will
	 * then
	 * be reduced to the requested bit length.
	 *
	 * @param passphrase The passphrase
	 * @param algorithm  The algorithm of the returned key
	 * @param bitLength  The bit length of the returned key
	 * @return The resulting key
	 * @throws SecurityException If deriving the key fails
	 */
	public static Key deriveKey(String passphrase, String algorithm,
		int bitLength) {
		try {
			byte[] keyHash = Arrays.copyOf(MessageDigest
					.getInstance("SHA-256")
					.digest(passphrase.getBytes(StandardCharsets.UTF_8)),
				bitLength / 8);

			return new SecretKeySpec(keyHash, algorithm);
		} catch (Exception e) {
			throw new SecurityException(e);
		}
	}

	/**
	 * This method will enable java cryptographic extensions on the application
	 * level if possible.
	 *
	 * <p><b>Attention</b>: this performs a rather dirty hack by using
	 * reflection to set a field in the class 'javax.crypto.JceSecurity' to
	 * FALSE. That is necessary because otherwise some JCE security algorithms
	 * are not availabe. Whether or not this actually works depends on the
	 * platform that the application is deployed to and its JRE. Keep an eye
	 * out
	 * for logged errors that might occur at application startup/initialization
	 * of this endpoint.</p>
	 */
	public static void enableJavaCryptographicExtensions() {
		try {
			Field field = Class
				.forName("javax.crypto.JceSecurity")
				.getDeclaredField("isRestricted");

			field.setAccessible(true);

			Field modifiersField = Field.class.getDeclaredField("modifiers");

			modifiersField.setAccessible(true);
			modifiersField.setInt(field,
				field.getModifiers() & ~Modifier.FINAL);

			field.set(null, Boolean.FALSE);
		} catch (Exception e) {
			Log.error("Unable to enable Java Cryptographic Extensions. " +
				"Some ciphers may be unavailable in the current JRE.", e);
		}
	}

	/**
	 * Encodes binary cryptographic data into base64 in PEM format according to
	 * <a href="https://tools.ietf.org/html/rfc7468">RFC 7468</a>.
	 *
	 * @param data  The binary data to encode
	 * @param token The token for the PEM delimiters describing the encoded
	 *                 data
	 *              (will be converted to uppercase)
	 * @return An ASCII string containing the encoded data
	 */
	public static String encodeBase64(byte[] data, String token) {
		String base64 = Base64.getEncoder().encodeToString(data);
		StringBuilder encoded = new StringBuilder("-----BEGIN ");
		int max = base64.length();

		token = token.toUpperCase();
		encoded.append(token).append("-----\n");

		for (int i = 0; i < max; i += 64) {
			int l = max - i;

			encoded.append(base64, i, i + (l > 64 ? 64 : l));
			encoded.append('\n');
		}

		encoded.append("-----END ").append(token).append("-----\n");

		return encoded.toString();
	}

	/**
	 * Encrypts a string value with the given passphrase by using the AES
	 * algorithm. The passphrase will be converted into a 128 bit AES key by
	 * applying the method {@link #deriveKey(String, String, int)}.
	 *
	 * @param value      The value to encrypt
	 * @param passphrase The passphrase
	 * @return The encrypted data
	 */
	public static byte[] encrypt(String value, String passphrase) {
		try {
			Cipher cipher = Cipher.getInstance("AES");
			Key key = deriveKey(passphrase, "AES", 128);

			cipher.init(Cipher.ENCRYPT_MODE, key);

			return cipher.doFinal(value.getBytes());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates a random ID based by applying a certain hash algorithm.
	 * This is
	 * done by hashing a random UUID (see {@link UUID#randomUUID()}) and
	 * returning the resulting hash as an uppercase hexadecimal string with the
	 * length of the algorithms hash size (e.g. 32 bytes = 64 characters for
	 * the
	 * SHA-256 algorithm).
	 *
	 * @param algorithm The name of the hash algorithm to apply
	 * @return The random hash ID
	 * @throws IllegalArgumentException If the hash algorithm cannot be found
	 */
	public static String generateHashId(String algorithm) {
		return hash(algorithm, UUID.randomUUID().toString().getBytes());
	}

	/**
	 * Generates a key pair with the given algorithm and key size.
	 *
	 * @param algorithm The algorithm name as defined in the Java cryptography
	 *                  packages
	 * @param keySize   The number of bits in the key
	 * @return The generated key pair
	 * @throws IllegalArgumentException If the given algorithm could not be
	 *                                  found (mapped from
	 *                                  {@link NoSuchAlgorithmException})
	 */
	public static KeyPair generateKeyPair(String algorithm, int keySize) {
		try {
			SecureRandom random = new SecureRandom();
			KeyPairGenerator generator =
				KeyPairGenerator.getInstance(algorithm);

			generator.initialize(keySize, random);

			return generator.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Generates a random ID based on the SHA-256 hash algorithm. This is done
	 * by hashing a random UUID (see {@link UUID#randomUUID()}) and returning
	 * the resulting hash as a hexadecimal string with 64 characters (= 32
	 * bytes).
	 *
	 * @return The random SHA-256 ID
	 */
	public static String generateSha256Id() {
		return generateHashId("SHA-256");
	}

	/**
	 * Initializes an SSL context from a key store so that it will use the
	 * certificates from the key store for SSL/TLS connections.
	 *
	 * @param keyStore         The key store to initialize the context from
	 * @param keyStorePassword The key store password
	 * @return A new SSL context initialized to use the given key store
	 * @throws IllegalArgumentException If the parameters are invalid or refer
	 *                                  to unavailable security algorithms
	 */
	public static SSLContext getSslContext(KeyStore keyStore,
		String keyStorePassword) {
		try {
			KeyManagerFactory keyManagerFactory =
				KeyManagerFactory.getInstance("SunX509");
			TrustManagerFactory trustManagerFactory =
				TrustManagerFactory.getInstance("SunX509");

			keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
			trustManagerFactory.init(keyStore);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			TrustManager[] trustManagers =
				trustManagerFactory.getTrustManagers();

			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers,
				null);

			return sslContext;
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Creates the hash of an input value with a certain hash algorithm. The
	 * returned value is an uppercase hexadecimal string of the resulting hash
	 * value.
	 *
	 * @param algorithm The name of the hash algorithm to apply
	 * @param data      The data to hash
	 * @return The hexadecimal hash value (uppercase)
	 * @throws IllegalArgumentException If the hash algorithm cannot be found
	 */
	public static String hash(String algorithm, byte[] data) {
		try {
			return TextUtil.hexString(
				MessageDigest.getInstance(algorithm).digest(data), "");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(
				"Unkown hash algorithm: " + algorithm, e);
		}
	}
}
