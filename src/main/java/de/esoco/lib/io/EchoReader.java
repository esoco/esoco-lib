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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * A {@link Reader} that echos all characters that are read from a wrapped
 * reader to a {@link Writer}.
 *
 * @author eso
 */
public class EchoReader extends FilterReader {

	private final Writer echoWriter;

	/**
	 * Creates a new instance.
	 *
	 * @param wrappedReader The wrapped reader
	 * @param echoWriter    The writer to echo the input to
	 */
	public EchoReader(Reader wrappedReader, Writer echoWriter) {
		super(wrappedReader);

		this.echoWriter = echoWriter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		int ch = super.read();

		if (ch >= 0) {
			echoWriter.write(ch);
		}

		return ch;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(char[] buffer, int offset, int length) throws IOException {
		int read = super.read(buffer, offset, length);

		if (read >= 0) {
			echoWriter.write(buffer, offset, read);
		}

		return read;
	}
}
