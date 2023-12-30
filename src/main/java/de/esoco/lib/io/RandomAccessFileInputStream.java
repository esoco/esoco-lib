//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * A wrapper that forwards the input stream read methods to the corresponding
 * {@link RandomAccessFile} methods so that stream-based tools can be used on
 * the file. It doesn't support the optional available/mark/reset methods.
 * Closing the stream has no effect, the wrapped file needs to be closed
 * separately.
 *
 * @author eso
 */
public class RandomAccessFileInputStream extends InputStream {

	private final RandomAccessFile randomAccessFile;

	/**
	 * Creates a new instance.
	 *
	 * @param file The wrapped file
	 */
	public RandomAccessFileInputStream(RandomAccessFile file) {
		this.randomAccessFile = file;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		return randomAccessFile.read();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] bytes) throws IOException {
		return randomAccessFile.read(bytes);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		return randomAccessFile.read(bytes, offset, length);
	}
}
