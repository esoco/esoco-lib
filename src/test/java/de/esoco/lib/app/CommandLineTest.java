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
package de.esoco.lib.app;

import junit.framework.TestCase;

import java.util.regex.Pattern;


/********************************************************************
 * Test of CommandLine class.
 *
 * @author eso
 */
public class CommandLineTest extends TestCase
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test assignment only CommandLine(String[], String...)
	 */
	public void testAssignmentCommandLine()
	{
		String[]    args     = new String[] { "-val=test" };
		String[]    switches = new String[] { "val=" };
		CommandLine cl		 = new CommandLine(args, switches);

		assertEquals("test", cl.getSwitchValue("val"));

		try
		{
			args = new String[] { "-val" };
			cl   = new CommandLine(args, switches);

			assertTrue("Mandatory switch value missing", false);
		}
		catch (IllegalArgumentException e)
		{
			// expected
		}
	}

	/***************************************
	 * Test CommandLine(String[])
	 */
	public void testCommandLine()
	{
		String[]    args = "-a_/b_-t1=123_-t2='ok ok'".split("_");
		CommandLine cl   = new CommandLine(args);

		assertSwitches(cl);
	}

	/***************************************
	 * Test CommandLine(String[], Pattern)
	 */
	public void testCommandLineWithPattern()
	{
		String[]    args = "--a_-b_-t1:=123_--t2:='ok ok'".split("_");
		Pattern     p    =
			CommandLine.createPattern("-{1,2}", ":=", "a", "b", "t1:=", "t2:=");
		CommandLine cl   = new CommandLine(args, p);

		assertSwitches(cl);

		try
		{
			args = "--a_-b_-c".split("_");
			cl   = new CommandLine(args, p);

			assertTrue("Command line contains illegal argument c", false);
		}
		catch (IllegalArgumentException e)
		{
			// expected
		}
	}

	/***************************************
	 * Test CommandLine(String[], String...)
	 */
	public void testCommandLineWithSwitches()
	{
		String[]    args     =
			"-a_/b_/t1=replaced_-t1=123_-t2='ok ok'_-T2=xy".split("_");
		String[]    switches = new String[] { "a", "b", "t1=", "t2=", "T2=" };
		CommandLine cl		 = new CommandLine(args, switches);

		assertSwitches(cl);
		assertEquals("xy", cl.getSwitchValue("T2"));

		try
		{
			args = "-a_-b_-c".split("_");
			cl   = new CommandLine(args, switches);

			assertTrue("Command line contains illegal argument c", false);
		}
		catch (IllegalArgumentException e)
		{
			// expected
		}

		try
		{
			args = "-t1=".split("_");
			cl   = new CommandLine(args, switches);

			assertTrue("missing switch value", false);
		}
		catch (IllegalArgumentException e)
		{
			// expected
		}

		try
		{
			args = "-t2".split("_");
			cl   = new CommandLine(args, switches);

			assertTrue("missing switch value", false);
		}
		catch (IllegalArgumentException e)
		{
			// expected
		}
	}

	/***************************************
	 * Test empty CommandLine(String[], String...)
	 */
	public void testEmtpyCommandLine()
	{
		String[] args     = new String[0];
		String[] switches = new String[0];

		new CommandLine(args, switches);

		try
		{
			args = new String[] { "-t1" };

			new CommandLine(args, switches);

			assertTrue("Command line contains illegal switch", false);
		}
		catch (IllegalArgumentException e)
		{
			// expected
		}
	}

	/***************************************
	 * Asserts the contents of a command line.
	 *
	 * @param cl The command line
	 */
	@SuppressWarnings("boxing")
	private void assertSwitches(CommandLine cl)
	{
		assertTrue(cl.hasSwitch("a"));
		assertTrue(cl.hasSwitch("b"));
		assertFalse(cl.hasSwitch("c"));
		assertEquals(null, cl.getSwitchValue("a"));
		assertEquals(123, cl.getSwitchValue("t1"));
		assertEquals("ok ok", cl.getSwitchValue("t2"));
	}
}
