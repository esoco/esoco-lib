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

import java.io.EOFException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/********************************************************************
 * An output stream wrapper that limits the number of bytes that can be written
 * to the stream. If the limit is exceeded the {@link #write()} method will
 * throw an {@link EOFException}.
 *
 * @author eso
 */
public class LimitedOutputStream extends FilterOutputStream
{
	//~ Instance fields --------------------------------------------------------

	private int nRemainingBytes;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param rWrappedStream The stream wrapped by this instance
	 * @param nMaxBytes      The maximum number of bytes that can be written to
	 *                       this stream
	 */
	public LimitedOutputStream(OutputStream rWrappedStream, int nMaxBytes)
	{
		super(rWrappedStream);

		nRemainingBytes = nMaxBytes;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the remaining bytes that can be written to this stream.
	 *
	 * @return The remaining bytes
	 */
	public int getRemainingBytes()
	{
		return nRemainingBytes;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("boxing")
	public String toString()
	{
		return String.format("%s(%d -> %s)",
							 getClass().getSimpleName(),
							 nRemainingBytes,
							 out);
	}

	/***************************************
	 * Overwritten to throw an {@link EOFException} if the write limit is
	 * exceeded.
	 *
	 * @see java.io.FilterOutputStream#write(int)
	 */
	@Override
	public void write(int nByte) throws IOException
	{
		if (nRemainingBytes-- > 0)
		{
			super.write(nByte);
		}
		else
		{
			throw new EOFException("Stream output limit reached");
		}
	}
}
