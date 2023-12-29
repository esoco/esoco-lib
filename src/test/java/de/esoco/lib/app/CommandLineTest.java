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
package de.esoco.lib.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test of the {@link CommandLine} class.
 *
 * @author eso
 */
public class CommandLineTest {

	/**
	 * Test CommandLine(String[])
	 */
	@Test
	public void testCommandLine() {
		String[] args =
			new String[] { "-a", "-b", "-t1=123", "-t2='ok ok'", "-t3",
				"'a value'" };
		CommandLine cl = new CommandLine(args);

		assertTrue(cl.hasOption("a"));
		assertTrue(cl.hasOption("b"));
		assertFalse(cl.hasOption("c"));
		assertEquals(Boolean.TRUE, cl.getOption("a"));
		assertEquals(123, cl.getOption("t1"));
		assertEquals("ok ok", cl.getOption("t2"));
		assertEquals("a value", cl.getOption("t3"));
	}

	/**
	 * Test assignment only CommandLine(String[], String...)
	 */
	@Test
	public void testRequireOption() {
		String[] args = new String[] { "-val=test" };
		CommandLine cl = new CommandLine(args);

		assertEquals("test", cl.getOption("val"));

		try {
			args = new String[] {};
			cl = new CommandLine(args);

			cl.requireOption("val");
			fail("Mandatory option value missing");
		} catch (CommandLineException e) {
			// expected
		}
	}
}
