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

/**
 * A wrapper for {@link Writer} instances that limits the number of characters
 * that can be written to it. If the limit is exceeded any further attempt at
 * writing will throw a {@link StreamLimitException}. The remaining limit can be
 * queried with the {@link #getRemainingLimit()} method.
 *
 * @author eso
 */
public class LimitedWriter extends FilterWriter {

	private int remainingLimit;

	/**
	 * Creates a new instance.
	 *
	 * @param wrappedWriter The writer wrapped by this instance
	 * @param max           The maximum number of characters that can be
	 *                         written
	 *                      to this instance
	 */
	public LimitedWriter(Writer wrappedWriter, int max) {
		super(wrappedWriter);

		remainingLimit = max;
	}

	/**
	 * Returns the remaining limit that can be written.
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
	@SuppressWarnings("boxing")
	public String toString() {
		return String.format("%s(%d, %s)", getClass().getSimpleName(),
			remainingLimit, out);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(int b) throws IOException {
		if (remainingLimit-- > 0) {
			super.write(b);
		} else {
			throw new StreamLimitException("Output limit reached", false);
		}
	}
}
