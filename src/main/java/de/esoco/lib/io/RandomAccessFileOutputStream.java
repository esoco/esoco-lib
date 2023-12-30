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
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * A wrapper that forwards the output stream write methods to the corresponding
 * {@link RandomAccessFile} methods so that stream-based tools and classes can
 * be used on the file. Flushing or closing the stream has no effect, the
 * wrapped file needs to be closed separately.
 *
 * @author eso
 */
public class RandomAccessFileOutputStream extends OutputStream {

	private final RandomAccessFile randomAccessFile;

	/**
	 * Creates a new instance.
	 *
	 * @param file The wrapped file
	 */
	public RandomAccessFileOutputStream(RandomAccessFile file) {
		randomAccessFile = file;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(byte[] bytes) throws IOException {
		randomAccessFile.write(bytes);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(int b) throws IOException {
		randomAccessFile.write(b);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
		randomAccessFile.write(bytes, offset, length);
	}
}
