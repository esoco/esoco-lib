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

import java.io.IOException;

/**
 * An IO exception that is thrown if a stream has reached a limit.
 *
 * @author eso
 */
public class StreamLimitException extends IOException {

	private static final long serialVersionUID = 1L;

	private final boolean onInput;

	/**
	 * Creates a new instance.
	 *
	 * @param message An error message
	 * @param onInput TRUE if the limit was reached on an input stream, FALSE
	 *                for an output stream
	 */
	public StreamLimitException(String message, boolean onInput) {
		super(message);

		this.onInput = onInput;
	}

	/**
	 * Checks whether the limit was reached on an input stream or on an output
	 * stream.
	 *
	 * @return TRUE if the limit was reached on an input stream, FALSE for an
	 * output stream
	 */
	public final boolean onInput() {
		return onInput;
	}
}
