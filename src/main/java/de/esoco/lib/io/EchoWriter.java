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
package de.esoco.lib.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;


/********************************************************************
 * A {@link Writer} that echos all characters that are written to a wrapped
 * writer to another writer.
 *
 * @author eso
 */
public class EchoWriter extends FilterWriter
{
	//~ Instance fields --------------------------------------------------------

	private final Writer rEchoWriter;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rWrappedWriter The wrapped writer
	 * @param rEchoWriter    The writer to echo the output to
	 */
	public EchoWriter(Writer rWrappedWriter, Writer rEchoWriter)
	{
		super(rWrappedWriter);

		this.rEchoWriter = rEchoWriter;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void flush() throws IOException
	{
		super.flush();
		rEchoWriter.flush();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void write(int nByte) throws IOException
	{
		super.write(nByte);

		rEchoWriter.write(nByte);
	}
}
