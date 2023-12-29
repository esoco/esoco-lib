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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An input stream that echos all bytes that are read from a wrapped input
 * stream to an output stream.
 *
 * @author eso
 */
public class EchoInputStream extends FilterInputStream {

	private final OutputStream rEchoStream;

	/**
	 * Creates a new instance.
	 *
	 * @param rWrappedStream The wrapped input stream
	 * @param rEchoStream    The stream to echo the input to
	 */
	public EchoInputStream(InputStream rWrappedStream,
		OutputStream rEchoStream) {
		super(rWrappedStream);

		this.rEchoStream = rEchoStream;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		int nByte = super.read();

		if (nByte >= 0) {
			rEchoStream.write(nByte);
		}

		return nByte;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] rBuffer, int nOffset, int nLength)
		throws IOException {
		int nRead = super.read(rBuffer, nOffset, nLength);

		if (nRead >= 0) {
			rEchoStream.write(rBuffer, nOffset, nRead);
		}

		return nRead;
	}
}
