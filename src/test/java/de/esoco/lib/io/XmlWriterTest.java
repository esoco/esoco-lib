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
package de.esoco.lib.io;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import static org.junit.Assert.assertEquals;


/********************************************************************
 * Unit test for the {@link XmlWriter} class.
 *
 * @author eso
 */
public class XmlWriterTest
{
	//~ Instance fields --------------------------------------------------------

	private XmlWriter	    aXmlWriter;
	private Writer		    aWriter;
	private DocumentBuilder aDocumentBuilder;

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Test class setup.
	 */
	@BeforeClass
	public static void setUpBeforeClass()
	{
	}

	/***************************************
	 * Test class tear down.
	 */
	@AfterClass
	public static void tearDownAfterClass()
	{
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test set up.
	 *
	 * @throws Exception If setup fails
	 */
	@Before
	public void setUp() throws Exception
	{
		aWriter    = new StringWriter();
		aXmlWriter = new XmlWriter(aWriter, "1.0", "UTF-8", Boolean.TRUE);

		aDocumentBuilder =
			DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	/***************************************
	 * Test tear down.
	 */
	@After
	public void tearDown()
	{
		aXmlWriter = null;
	}

	/***************************************
	 * Tests the creation of an empty XML file.
	 *
	 * @throws Exception
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testEmptyFile() throws Exception
	{
		aXmlWriter.startElement("test");
		aXmlWriter.endElement();

		Document aDocument = createResultDocument();

		assertEquals("1.0", aDocument.getXmlVersion());
		assertEquals("UTF-8", aDocument.getXmlEncoding());
		assertEquals(true, aDocument.getXmlStandalone());

		NodeList rTestElement = aDocument.getElementsByTagName("test");

		assertEquals(1, rTestElement.getLength());
		assertEquals("test", rTestElement.item(0).getNodeName());
	}

	/***************************************
	 * Tests the creation of a single element XML file.
	 *
	 * @throws Exception
	 */
	@Test
	public void testNestedElements() throws Exception
	{
		aXmlWriter.startElement("test");
		aXmlWriter.writeAttribute("attr1", "value1");
		aXmlWriter.writeAttribute("attr2", "value2");
		aXmlWriter.startElement("test-c1");
		aXmlWriter.writeAttribute("c1-attr1", "c1-value1");
		aXmlWriter.startElement("test-c1-1");
		aXmlWriter.writeAttribute("c1-1-attr1", "c1-1-value1");
		aXmlWriter.writeText("c1-1-test-text");
		aXmlWriter.endElement();
		aXmlWriter.endElement();
		aXmlWriter.startElement("test-c2");
		aXmlWriter.writeElement("test-c2-1",
								"c2-1-attr1",
								"c2-1-value1",
								"c2-1-test-text");
		aXmlWriter.writeElement("test-c2-2", "c2-2-attr1", "c2-2-value1", null);
		aXmlWriter.endElement();
		aXmlWriter.endElement();

		System.out.printf("-------RESULT-------\n%s\n", aWriter);

		Document aDocument = createResultDocument();

		NamedNodeMap rAttributes =
			aDocument.getElementsByTagName("test").item(0).getAttributes();

		assertEquals(2, rAttributes.getLength());
		assertEquals("value1",
					 rAttributes.getNamedItem("attr1").getNodeValue());
		assertEquals("value2",
					 rAttributes.getNamedItem("attr2").getNodeValue());
	}

	/***************************************
	 * Tests the creation of a single element XML file.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSingleElement() throws Exception
	{
		aXmlWriter.startElement("test");
		aXmlWriter.writeAttribute("attr1", "value1");
		aXmlWriter.writeAttribute("attr2", "value2");
		aXmlWriter.writeText("test-text");
		aXmlWriter.endElement();

		Document aDocument = createResultDocument();

		NamedNodeMap rAttributes =
			aDocument.getElementsByTagName("test").item(0).getAttributes();

		assertEquals(2, rAttributes.getLength());
		assertEquals("value1",
					 rAttributes.getNamedItem("attr1").getNodeValue());
		assertEquals("value2",
					 rAttributes.getNamedItem("attr2").getNodeValue());
	}

	/***************************************
	 * Parses the result string and returns a {@link Document} object for it.
	 *
	 * @return The result document
	 *
	 * @throws Exception
	 */
	private Document createResultDocument() throws Exception
	{
		aXmlWriter.close();

		return aDocumentBuilder.parse(new InputSource(new StringReader(aWriter
																	   .toString())));
	}
}
