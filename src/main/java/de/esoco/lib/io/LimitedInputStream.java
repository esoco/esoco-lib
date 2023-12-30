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

/**
 * An input stream wrapper that limits the number of bytes that can be read from
 * the stream. It limits all (and only) bytes read from the stream, even if the
 * {@link #mark(int)} and {@link #reset()} methods are used. Bytes ignored by
 * invoking the {@link #skip(long)} method will not count against the limit.
 *
 * <p>If the limit is exceeded any further attempt at reading from the stream
 * will throw a {@link StreamLimitException}. The remaining limit can be queried
 * with the {@link #getRemainingLimit()} method.</p>
 *
 * @author eso
 */
public class LimitedInputStream extends FilterInputStream {

	private int remainingLength;

	/**
	 * Creates a new instance.
	 *
	 * @param wrappedStream The stream wrapped by this instance
	 * @param max           The maximum number of bytes that can be read from
	 *                      this instance
	 */
	public LimitedInputStream(InputStream wrappedStream, int max) {
		super(wrappedStream);

		remainingLength = max;
	}

	/**
	 * Returns the remaining limit that can be read.
	 *
	 * @return The remaining limit
	 */
	public int getRemainingLimit() {
		return remainingLength;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		checkLimit();
		remainingLength--;

		return super.read();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		checkLimit();

		if (remainingLength < length) {
			length = remainingLength;
		}

		int read = super.read(buffer, offset, length);

		remainingLength -= read;

		return read;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("boxing")
	public String toString() {
		return String.format("%s(%d, %s)", getClass().getSimpleName(),
			remainingLength, in);
	}

	/**
	 * Checks whether the limit has been reached.
	 *
	 * @throws StreamLimitException If the limit has been reached
	 */
	protected void checkLimit() throws StreamLimitException {
		if (remainingLength == 0) {
			throw new StreamLimitException("Input limit reached", true);
		}
	}
}
