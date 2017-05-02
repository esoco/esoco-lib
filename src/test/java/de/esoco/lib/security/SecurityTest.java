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

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/********************************************************************
 * Test of {@link Security} methods.
 *
 * @author eso
 */
public class SecurityTest
{
	//~ Static fields/initializers ---------------------------------------------

	private static final String ENCRYPTION_TEST_PASSPHRASE = "EncryptionTest";

	private static final String ENCRYPTION_TEST_TEXT =
		"Lorem ipsum dolor sit amet, consectetuer adipiscing elit";

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test of {@link Security#encrypt(String, String)} and {@link
	 * Security#decrypt(byte[], String)}.
	 */
	@Test
	public void testEncrypt()
	{
		byte[] encrypted =
			Security.encrypt(ENCRYPTION_TEST_TEXT, ENCRYPTION_TEST_PASSPHRASE);

		assertEquals(ENCRYPTION_TEST_TEXT,
					 Security.decrypt(encrypted, ENCRYPTION_TEST_PASSPHRASE));
	}
}
