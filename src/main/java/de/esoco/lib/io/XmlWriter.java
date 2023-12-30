//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

/**
 * A simple class to write XML files directly to an arbitrary {@link Writer}
 * instance. This class performs no buffering by itself, so if necessary a
 * buffered writer implementation should be used for the output. It also
 * performs no XML validation, only some basic structural checks.
 */
public class XmlWriter {

	private Writer writer;

	private String namespace = "";

	private final String indentation = "\t";

	private Stack<String> elementStack = new Stack<String>();

	private boolean tagOpen = false;

	/**
	 * Creates a new instance for a standalone XML file with a version
	 * number of
	 * 1.0, UTF-8 encoding.
	 *
	 * @param writer The writer instance to write the XML to
	 * @throws IOException If writing the XML header fails
	 */
	public XmlWriter(Writer writer) throws IOException {
		this(writer, "1.0", "UTF-8", Boolean.TRUE);
	}

	/**
	 * Creates a new instance with a certain XML declaration at the
	 * beginning of
	 * the file.
	 *
	 * @param writer     The writer instance to write the XML to
	 * @param version    The XML version used by the file
	 * @param encoding   The character encoding used for the content or NULL
	 *                     for
	 *                   no value
	 * @param standalone TRUE for a file with internal DTD, FALSE for a file
	 *                   with a separate DTD or other external references or
	 *                   NULL for no value
	 * @throws IOException If writing the XML header fails
	 */
	public XmlWriter(Writer writer, String version, String encoding,
		Boolean standalone) throws IOException {
		this.writer = writer;

		writer.write("<?xml version=\"");
		writer.write(version);

		if (encoding != null) {
			writer.write("\" encoding=\"");
			writer.write(encoding);
		}

		if (standalone != null) {
			writer.write("\" standalone=\"");
			writer.write(standalone.booleanValue() ? "yes" : "no");
		}

		writer.write("\"?>\n");
	}

	/**
	 * Closes this instance. This flushes any remaining output in the
	 * underlying
	 * {@link Writer} but doesn't close it. The behavior of an instance after
	 * closing it is undefined and may cause exceptions.
	 *
	 * @throws IOException If writing to the output writer fails or if the
	 *                     internal state of this instance is inconsistent for
	 *                     closing
	 */
	public void close() throws IOException {
		if (!elementStack.isEmpty()) {
			throw new IOException(
				"XML file contains unclosed elements: " + elementStack);
		}

		writer.flush();

		writer = null;
		elementStack = null;
	}

	/**
	 * Finishes the structure of an element by writing the corresponding end
	 * tag
	 * to the output writer. The name of the element is determined from the
	 * previous invocation hierarchy of {@link #startElement(String)} and this
	 * method. As an optimization only an empty-element tag will be written if
	 * no content has been added to the currently open element.
	 *
	 * @return This instance for method concatenation
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter endElement() throws IOException {
		if (elementStack.isEmpty()) {
			throw new IOException("No open element");
		}

		String element = elementStack.pop();

		if (tagOpen) {
			writer.write("/>\n");
			tagOpen = false;
		} else {
			writer.write("</");
			writer.write(element);
			writer.write(">\n");
		}

		indent(elementStack.size() - 1);

		return this;
	}

	/**
	 * Returns the indentation string.
	 *
	 * @return The indentation string
	 */
	public final String getIndentation() {
		return indentation;
	}

	/**
	 * Returns the current namespace of this writer.
	 *
	 * @return The namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Returns the writer that this instance uses for output.
	 *
	 * @return The output writer
	 */
	public Writer getWriter() {
		return writer;
	}

	/**
	 * Sets the indentation string. For each indentation level one copy of this
	 * string will be added to the output of each line. The default value is a
	 * tabulator character.
	 *
	 * @param indentation The indentation string or NULL to disable indentation
	 */
	public final void setIndentation(String indentation) {
		indentation = indentation;
	}

	/**
	 * Sets the namespace for all subsequent element creations.
	 *
	 * @param namespace The namespace
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Starts the writing of a new element in the XML file. This will open a
	 * new
	 * starting tag with the given element name and accept the writing of
	 * element attributes with {@link #writeAttribute(String, String)} or of
	 * element data with one of the corresponding methods. To finish the
	 * element
	 * the method {@link #endElement()} must be invoked.
	 *
	 * <p>Within an element an arbitrary number of child elements can be
	 * created by invoking this method as long as all elements are closed
	 * correctly with {@link #endElement()}. The implementation keeps track of
	 * the added elements and will throw an exception if the element hierarchy
	 * becomes inconsistent.</p>
	 *
	 * @param name The element name that will appear in the start and end tags
	 * @return This instance for method concatenation
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter startElement(String name) throws IOException {
		if (namespace != null && namespace.length() > 0) {
			name = namespace + ":" + name;
		}

		if (tagOpen) {
			writer.write(">\n");
			indent(elementStack.size());
		} else {
			tagOpen = true;
			indent(1);
		}

		writer.write("<");
		writer.write(name);
		elementStack.push(name);

		return this;
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(),
			writer.getClass().getSimpleName());
	}

	/**
	 * Writes an attribute for the current element. Invocations of this method
	 * can only occur directly after an element had been started with the
	 * method
	 * {@link #startElement(String)}. The writing of attributes is ended by
	 * writing element content, child elements, or with {@link #endElement()}.
	 *
	 * <p>Any reserved characters in the attribute value will be replaced with
	 * the corresponding XML character entity.</p>
	 *
	 * @param name  The attribute name
	 * @param value The attribute value
	 * @return This instance for method concatenation
	 * @throws IOException If writing to the output writer fails or if no
	 *                     element tag is currently open
	 */
	public XmlWriter writeAttribute(String name, String value)
		throws IOException {
		if (!tagOpen) {
			throw new IOException("No open element tag");
		}

		writer.write(" ");
		writer.write(name);
		writer.write("=\"");
		writer.write(escapeCharacterEntities(value));
		writer.write("\"");

		return this;
	}

	/**
	 * Writes a block of unparsed character data as the current element's
	 * content. This will insert an XML CDATA block into the output. If the
	 * data
	 * string contains occurrences of the CDATA termination string ']]&gt;' it
	 * will be split into multiple CDATA sections.
	 *
	 * @param data The string containing the unparsed character data
	 * @return This instance for method concatenation
	 * @throws IOException If writing to the output writer fails or if no
	 *                     element is currently open
	 */
	public XmlWriter writeCharacterData(String data) throws IOException {
		int cDataTerminator = data.indexOf("]]>");

		if (cDataTerminator >= 0) {
			cDataTerminator += 2;
			writeCharacterData(data.substring(0, cDataTerminator));
			writeCharacterData(data.substring(cDataTerminator));
		} else {
			writeContent("<![CDATA[" + data + "]]>", true);
		}

		return this;
	}

	/**
	 * Writes a string into an XML comment tag. Comments can be written
	 * everywhere in an XML file, either outside of or in elements.
	 *
	 * @param comment The comment string
	 * @return This instance for method concatenation
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter writeComment(String comment) throws IOException {
		writeContent("<!--" + comment + "-->\n", false);
		indent(elementStack.size());

		return this;
	}

	/**
	 * A convenience method that writes an element without attributes and
	 * closes
	 * it immediately. If the text argument is NULL an empty-element tag
	 * will be
	 * written.
	 *
	 * @param name The element name
	 * @param text The element content or NULL for none
	 * @return This instance for method concatenation
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter writeElement(String name, String text) throws IOException {
		startElement(name);

		if (text != null) {
			writeText(text);
		}

		return endElement();
	}

	/**
	 * A convenience method that writes an element with a single attribute and
	 * closes it immediately. If the text argument is NULL an empty-element tag
	 * will be written.
	 *
	 * @param name      The element name
	 * @param attribute The attribute name
	 * @param value     The attribute value
	 * @param text      The element content or NULL for none
	 * @return This instance for method concatenation
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter writeElement(String name, String attribute, String value,
		String text) throws IOException {
		startElement(name);
		writeAttribute(attribute, value);

		if (text != null) {
			writeText(text);
		}

		return endElement();
	}

	/**
	 * Writes a text string as the current element's content.
	 *
	 * @param text The text to write
	 * @return This instance for method concatenation
	 * @throws IOException If writing to the output writer fails or if no
	 *                     element is currently open
	 */
	public XmlWriter writeText(String text) throws IOException {
		return writeContent(escapeCharacterEntities(text), true);
	}

	/**
	 * Escapes reserved characters in a text string with XML character
	 * entities.
	 *
	 * @param text The text to escape
	 * @return The resulting text
	 */
	private String escapeCharacterEntities(String text) {
		text = text.replaceAll("&", "&amp;");
		text = text.replaceAll("<", "&lt;");
		text = text.replaceAll(">", "&gt;");
		text = text.replaceAll("\"", "&quot;");
		text = text.replaceAll("'", "&apos;");

		return text;
	}

	/**
	 * Writes tab characters for the current indentation level.
	 *
	 * @param level The indentation level
	 * @throws IOException If writing to the output writer fails
	 */
	private void indent(int level) throws IOException {
		if (indentation != null) {
			for (int i = level; i > 0; i--) {
				writer.write(indentation);
			}
		}
	}

	/**
	 * Internal helper method to write element content.
	 *
	 * @param content     The content to write
	 * @param elementOnly TRUE to throw an exception if no element is currently
	 *                    open
	 * @return This instance for method concatenation
	 * @throws IOException If writing to the output writer fails or if no
	 *                     element is open and elementOnly is TRUE
	 */
	private XmlWriter writeContent(String content, boolean elementOnly)
		throws IOException {
		if (elementOnly && elementStack.isEmpty()) {
			throw new IOException("No open element");
		}

		if (tagOpen) {
			tagOpen = false;
			writer.write(">");

			if (content.length() > 0 && content.charAt(0) == '<') {
				writer.write("\n");
				indent(elementStack.size());
			}
		}

		writer.write(content);

		return this;
	}
}
