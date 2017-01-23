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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/********************************************************************
 * An output stream that echos all bytes that are written to a wrapped output
 * stream to another output stream.
 *
 * @author eso
 */
public class EchoOutputStream extends FilterOutputStream
{
	//~ Instance fields --------------------------------------------------------

	private final OutputStream rEchoStream;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rWrappedStream The wrapped output stream
	 * @param rEchoStream    The stream to echo the output to
	 */
	public EchoOutputStream(
		OutputStream rWrappedStream,
		OutputStream rEchoStream)
	{
		super(rWrappedStream);

		this.rEchoStream = rEchoStream;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void flush() throws IOException
	{
		super.flush();
		rEchoStream.flush();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void write(int nByte) throws IOException
	{
		super.write(nByte);

		rEchoStream.write(nByte);
	}
}
