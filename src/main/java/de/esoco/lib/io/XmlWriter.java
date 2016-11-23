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


/********************************************************************
 * A simple class to write XML files directly to an arbitrary {@link Writer}
 * instance. This class performs no buffering by itself, so if necessary a
 * buffered writer implementation should be used for the output. It also
 * performs no XML validation, only some basic structural checks.
 */
public class XmlWriter
{
	//~ Instance fields --------------------------------------------------------

	private Writer		  rWriter;
	private String		  sNamespace    = "";
	private String		  sIndentation  = "\t";
	private Stack<String> aElementStack = new Stack<String>();
	private boolean		  bTagOpen	    = false;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance for a standalone XML file with a version number of
	 * 1.0, UTF-8 encoding.
	 *
	 * @param  rWriter The writer instance to write the XML to
	 *
	 * @throws IOException If writing the XML header fails
	 */
	public XmlWriter(Writer rWriter) throws IOException
	{
		this(rWriter, "1.0", "UTF-8", Boolean.TRUE);
	}

	/***************************************
	 * Creates a new instance with a certain XML declaration at the beginning of
	 * the file.
	 *
	 * @param  rWriter     The writer instance to write the XML to
	 * @param  sVersion    The XML version used by the file
	 * @param  sEncoding   The character encoding used for the content or NULL
	 *                     for no value
	 * @param  rStandalone TRUE for a file with internal DTD, FALSE for a file
	 *                     with a separate DTD or other external references or
	 *                     NULL for no value
	 *
	 * @throws IOException If writing the XML header fails
	 */
	public XmlWriter(Writer  rWriter,
					 String  sVersion,
					 String  sEncoding,
					 Boolean rStandalone) throws IOException
	{
		this.rWriter = rWriter;

		rWriter.write("<?xml version=\"");
		rWriter.write(sVersion);

		if (sEncoding != null)
		{
			rWriter.write("\" encoding=\"");
			rWriter.write(sEncoding);
		}

		if (rStandalone != null)
		{
			rWriter.write("\" standalone=\"");
			rWriter.write(rStandalone.booleanValue() ? "yes" : "no");
		}

		rWriter.write("\"?>\n");
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Closes this instance. This flushes any remaining output in the underlying
	 * {@link Writer} but doesn't close it. The behavior of an instance after
	 * closing it is undefined and may cause exceptions.
	 *
	 * @throws IOException If writing to the output writer fails or if the
	 *                     internal state of this instance is inconsistent for
	 *                     closing
	 */
	public void close() throws IOException
	{
		if (!aElementStack.isEmpty())
		{
			throw new IOException("XML file contains unclosed elements: " +
								  aElementStack);
		}

		rWriter.flush();

		rWriter		  = null;
		aElementStack = null;
	}

	/***************************************
	 * Finishes the structure of an element by writing the corresponding end tag
	 * to the output writer. The name of the element is determined from the
	 * previous invocation hierarchy of {@link #startElement(String)} and this
	 * method. As an optimization only an empty-element tag will be written if
	 * no content has been added to the currently open element.
	 *
	 * @return This instance for method concatenation
	 *
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter endElement() throws IOException
	{
		if (aElementStack.isEmpty())
		{
			throw new IOException("No open element");
		}

		String sElement = aElementStack.pop();

		if (bTagOpen)
		{
			rWriter.write("/>\n");
			bTagOpen = false;
		}
		else
		{
			rWriter.write("</");
			rWriter.write(sElement);
			rWriter.write(">\n");
		}

		indent(aElementStack.size() - 1);

		return this;
	}

	/***************************************
	 * Returns the indentation string.
	 *
	 * @return The indentation string
	 */
	public final String getIndentation()
	{
		return sIndentation;
	}

	/***************************************
	 * Returns the current namespace of this writer.
	 *
	 * @return The namespace
	 */
	public String getNamespace()
	{
		return sNamespace;
	}

	/***************************************
	 * Returns the writer that this instance uses for output.
	 *
	 * @return The output writer
	 */
	public Writer getWriter()
	{
		return rWriter;
	}

	/***************************************
	 * Sets the indentation string. For each indentation level one copy of this
	 * string will be added to the output of each line. The default value is a
	 * tabulator character.
	 *
	 * @param rIndentation The indentation string or NULL to disable indentation
	 */
	public final void setIndentation(String rIndentation)
	{
		sIndentation = rIndentation;
	}

	/***************************************
	 * Sets the namespace for all subsequent element creations.
	 *
	 * @param sNamespace The namespace
	 */
	public void setNamespace(String sNamespace)
	{
		this.sNamespace = sNamespace;
	}

	/***************************************
	 * Starts the writing of a new element in the XML file. This will open a new
	 * starting tag with the given element name and accept the writing of
	 * element attributes with {@link #writeAttribute(String, String)} or of
	 * element data with one of the corresponding methods. To finish the element
	 * the method {@link #endElement()} must be invoked.
	 *
	 * <p>Within an element an arbitrary number of child elements can be created
	 * by invoking this method as long as all elements are closed correctly with
	 * {@link #endElement()}. The implementation keeps track of the added
	 * elements and will throw an exception if the element hierarchy becomes
	 * inconsistent.</p>
	 *
	 * @param  sName The element name that will appear in the start and end tags
	 *
	 * @return This instance for method concatenation
	 *
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter startElement(String sName) throws IOException
	{
		if (sNamespace != null && sNamespace.length() > 0)
		{
			sName = sNamespace + ":" + sName;
		}

		if (bTagOpen)
		{
			rWriter.write(">\n");
			indent(aElementStack.size());
		}
		else
		{
			bTagOpen = true;
			indent(1);
		}

		rWriter.write("<");
		rWriter.write(sName);
		aElementStack.push(sName);

		return this;
	}

	/***************************************
	 * @see Object#toString()
	 */
	@Override
	public String toString()
	{
		return String.format("%s[%s]",
							 getClass().getSimpleName(),
							 rWriter.getClass().getSimpleName());
	}

	/***************************************
	 * Writes an attribute for the current element. Invocations of this method
	 * can only occur directly after an element had been started with the method
	 * {@link #startElement(String)}. The writing of attributes is ended by
	 * writing element content, child elements, or with {@link #endElement()}.
	 *
	 * <p>Any reserved characters in the attribute value will be replaced with
	 * the corresponding XML character entity.</p>
	 *
	 * @param  sName  The attribute name
	 * @param  sValue The attribute value
	 *
	 * @return This instance for method concatenation
	 *
	 * @throws IOException If writing to the output writer fails or if no
	 *                     element tag is currently open
	 */
	public XmlWriter writeAttribute(String sName, String sValue)
		throws IOException
	{
		if (!bTagOpen)
		{
			throw new IOException("No open element tag");
		}

		rWriter.write(" ");
		rWriter.write(sName);
		rWriter.write("=\"");
		rWriter.write(escapeCharacterEntities(sValue));
		rWriter.write("\"");

		return this;
	}

	/***************************************
	 * Writes a block of unparsed character data as the current element's
	 * content. This will insert an XML CDATA block into the output. If the data
	 * string contains occurrences of the CDATA termination string ']]&gt;' it
	 * will be split into multiple CDATA sections.
	 *
	 * @param  sData The string containing the unparsed character data
	 *
	 * @return This instance for method concatenation
	 *
	 * @throws IOException If writing to the output writer fails or if no
	 *                     element is currently open
	 */
	public XmlWriter writeCharacterData(String sData) throws IOException
	{
		int nCDataTerminator = sData.indexOf("]]>");

		if (nCDataTerminator >= 0)
		{
			nCDataTerminator += 2;
			writeCharacterData(sData.substring(0, nCDataTerminator));
			writeCharacterData(sData.substring(nCDataTerminator));
		}
		else
		{
			writeContent("<![CDATA[" + sData + "]]>", true);
		}

		return this;
	}

	/***************************************
	 * Writes a string into an XML comment tag. Comments can be written
	 * everywhere in an XML file, either outside of or in elements.
	 *
	 * @param  sComment The comment string
	 *
	 * @return This instance for method concatenation
	 *
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter writeComment(String sComment) throws IOException
	{
		writeContent("<!--" + sComment + "-->\n", false);
		indent(aElementStack.size());

		return this;
	}

	/***************************************
	 * A convenience method that writes an element without attributes and closes
	 * it immediately. If the text argument is NULL an empty-element tag will be
	 * written.
	 *
	 * @param  sName The element name
	 * @param  sText The element content or NULL for none
	 *
	 * @return This instance for method concatenation
	 *
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter writeElement(String sName, String sText) throws IOException
	{
		startElement(sName);

		if (sText != null)
		{
			writeText(sText);
		}

		return endElement();
	}

	/***************************************
	 * A convenience method that writes an element with a single attribute and
	 * closes it immediately. If the text argument is NULL an empty-element tag
	 * will be written.
	 *
	 * @param  sName      The element name
	 * @param  sAttribute The attribute name
	 * @param  sValue     The attribute value
	 * @param  sText      The element content or NULL for none
	 *
	 * @return This instance for method concatenation
	 *
	 * @throws IOException If writing to the output writer fails
	 */
	public XmlWriter writeElement(String sName,
								  String sAttribute,
								  String sValue,
								  String sText) throws IOException
	{
		startElement(sName);
		writeAttribute(sAttribute, sValue);

		if (sText != null)
		{
			writeText(sText);
		}

		return endElement();
	}

	/***************************************
	 * Writes a text string as the current element's content.
	 *
	 * @param  sText The text to write
	 *
	 * @return This instance for method concatenation
	 *
	 * @throws IOException If writing to the output writer fails or if no
	 *                     element is currently open
	 */
	public XmlWriter writeText(String sText) throws IOException
	{
		return writeContent(escapeCharacterEntities(sText), true);
	}

	/***************************************
	 * Escapes reserved characters in a text string with XML character entities.
	 *
	 * @param  sText The text to escape
	 *
	 * @return The resulting text
	 */
	private String escapeCharacterEntities(String sText)
	{
		sText = sText.replaceAll("&", "&amp;");
		sText = sText.replaceAll("<", "&lt;");
		sText = sText.replaceAll(">", "&gt;");
		sText = sText.replaceAll("\"", "&quot;");
		sText = sText.replaceAll("'", "&apos;");

		return sText;
	}

	/***************************************
	 * Writes tab characters for the current indentation level.
	 *
	 * @param  nLevel The indentation level
	 *
	 * @throws IOException If writing to the output writer fails
	 */
	private void indent(int nLevel) throws IOException
	{
		if (sIndentation != null)
		{
			for (int i = nLevel; i > 0; i--)
			{
				rWriter.write(sIndentation);
			}
		}
	}

	/***************************************
	 * Internal helper method to write element content.
	 *
	 * @param  sContent     The content to write
	 * @param  bElementOnly TRUE to throw an exception if no element is
	 *                      currently open
	 *
	 * @return This instance for method concatenation
	 *
	 * @throws IOException If writing to the output writer fails or if no
	 *                     element is open and bElementOnly is TRUE
	 */
	private XmlWriter writeContent(String sContent, boolean bElementOnly)
		throws IOException
	{
		if (bElementOnly && aElementStack.isEmpty())
		{
			throw new IOException("No open element");
		}

		if (bTagOpen)
		{
			bTagOpen = false;
			rWriter.write(">");

			if (sContent.length() > 0 && sContent.charAt(0) == '<')
			{
				rWriter.write("\n");
				indent(aElementStack.size());
			}
		}

		rWriter.write(sContent);

		return this;
	}
}
