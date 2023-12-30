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

/**
 * A wrapper for {@link Reader} instances that limits the number of characters
 * that can be read from it. It limits all (and only) characters read from the
 * stream, even if the {@link #mark(int)} and {@link #reset()} methods are used.
 * Characters ignored by invoking the {@link #skip(long)} method will not count
 * against the limit.
 *
 * <p>If the limit is exceeded any further attempt at reading will throw a
 * {@link StreamLimitException}. The remaining limit can be queried with the
 * {@link #getRemainingLimit()} method.</p>
 *
 * @author eso
 */
public class LimitedReader extends FilterReader {

	private int remainingLimit;

	/**
	 * Creates a new instance.
	 *
	 * @param wrappedReader The reader wrapped by this instance
	 * @param max           The maximum number of characters that can be read
	 *                      from this instance
	 */
	public LimitedReader(Reader wrappedReader, int max) {
		super(wrappedReader);

		remainingLimit = max;
	}

	/**
	 * Returns the remaining limit that can be read.
	 *
	 * @return The remaining limit
	 */
	public int getRemainingLimit() {
		return remainingLimit;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		checkLimit();
		remainingLimit--;

		return super.read();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(char[] buffer, int offset, int length) throws IOException {
		checkLimit();

		if (remainingLimit < length) {
			length = remainingLimit;
		}

		int read = super.read(buffer, offset, length);

		remainingLimit -= read;

		return read;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("boxing")
	public String toString() {
		return String.format("%s(%d, %s)", getClass().getSimpleName(),
			remainingLimit, in);
	}

	/**
	 * Checks whether the limit has been reached.
	 *
	 * @throws StreamLimitException If the limit has been reached
	 */
	protected void checkLimit() throws StreamLimitException {
		if (remainingLimit == 0) {
			throw new StreamLimitException("Input limit reached", true);
		}
	}
}
