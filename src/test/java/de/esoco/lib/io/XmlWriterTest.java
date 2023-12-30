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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for the {@link XmlWriter} class.
 *
 * @author eso
 */
public class XmlWriterTest {

	private XmlWriter xmlWriter;

	private Writer writer;

	private DocumentBuilder documentBuilder;

	/**
	 * Test set up.
	 *
	 * @throws Exception If setup fails
	 */
	@BeforeEach
	public void setUp() throws Exception {
		writer = new StringWriter();
		xmlWriter = new XmlWriter(writer, "1.0", "UTF-8", Boolean.TRUE);

		documentBuilder =
			DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	/**
	 * Tests the creation of an empty XML file.
	 */
	@SuppressWarnings("boxing")
	@Test
	public void testEmptyFile() throws Exception {
		xmlWriter.startElement("test");
		xmlWriter.endElement();

		Document document = createResultDocument();

		assertEquals("1.0", document.getXmlVersion());
		assertEquals("UTF-8", document.getXmlEncoding());
		assertTrue(document.getXmlStandalone());

		NodeList testElement = document.getElementsByTagName("test");

		assertEquals(1, testElement.getLength());
		assertEquals("test", testElement.item(0).getNodeName());
	}

	/**
	 * Tests the creation of a single element XML file.
	 */
	@Test
	public void testNestedElements() throws Exception {
		xmlWriter.startElement("test");
		xmlWriter.writeAttribute("attr1", "value1");
		xmlWriter.writeAttribute("attr2", "value2");
		xmlWriter.startElement("test-c1");
		xmlWriter.writeAttribute("c1-attr1", "c1-value1");
		xmlWriter.startElement("test-c1-1");
		xmlWriter.writeAttribute("c1-1-attr1", "c1-1-value1");
		xmlWriter.writeText("c1-1-test-text");
		xmlWriter.endElement();
		xmlWriter.endElement();
		xmlWriter.startElement("test-c2");
		xmlWriter.writeElement("test-c2-1", "c2-1-attr1", "c2-1-value1",
			"c2-1-test-text");
		xmlWriter.writeElement("test-c2-2", "c2-2-attr1", "c2-2-value1", null);
		xmlWriter.endElement();
		xmlWriter.endElement();

		System.out.printf("-------RESULT-------\n%s\n", writer);

		Document document = createResultDocument();

		NamedNodeMap attributes =
			document.getElementsByTagName("test").item(0).getAttributes();

		assertEquals(2, attributes.getLength());
		assertEquals("value1",
			attributes.getNamedItem("attr1").getNodeValue());
		assertEquals("value2",
			attributes.getNamedItem("attr2").getNodeValue());
	}

	/**
	 * Tests the creation of a single element XML file.
	 */
	@Test
	public void testSingleElement() throws Exception {
		xmlWriter.startElement("test");
		xmlWriter.writeAttribute("attr1", "value1");
		xmlWriter.writeAttribute("attr2", "value2");
		xmlWriter.writeText("test-text");
		xmlWriter.endElement();

		Document document = createResultDocument();

		NamedNodeMap attributes =
			document.getElementsByTagName("test").item(0).getAttributes();

		assertEquals(2, attributes.getLength());
		assertEquals("value1",
			attributes.getNamedItem("attr1").getNodeValue());
		assertEquals("value2",
			attributes.getNamedItem("attr2").getNodeValue());
	}

	/**
	 * Parses the result string and returns a {@link Document} object for it.
	 *
	 * @return The result document
	 */
	private Document createResultDocument() throws Exception {
		xmlWriter.close();

		return documentBuilder.parse(
			new InputSource(new StringReader(writer.toString())));
	}
}
