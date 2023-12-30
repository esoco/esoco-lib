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

/**
 * An output stream that echos all bytes that are written to a wrapped output
 * stream to another output stream.
 *
 * @author eso
 */
public class EchoOutputStream extends FilterOutputStream {

	private final OutputStream echoStream;

	/**
	 * Creates a new instance.
	 *
	 * @param wrappedStream The wrapped output stream
	 * @param echoStream    The stream to echo the output to
	 */
	public EchoOutputStream(OutputStream wrappedStream,
		OutputStream echoStream) {
		super(wrappedStream);

		this.echoStream = echoStream;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush() throws IOException {
		super.flush();
		echoStream.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(int b) throws IOException {
		super.write(b);
		echoStream.write(b);
	}
}
