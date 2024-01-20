//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.expression.BinaryPredicate;
import de.esoco.lib.expression.FunctionException;
import de.esoco.lib.expression.ThrowingBinaryFunction;
import de.esoco.lib.expression.ThrowingFunction;
import de.esoco.lib.expression.monad.Option;
import de.esoco.lib.expression.predicate.ThrowingBinaryPredicate;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.function.BiFunction;

/**
 * Provides access to stream and I/O related function implementations. All
 * functions map occurring IO exceptions to a {@link FunctionException} runtime
 * exception.
 *
 * @author eso
 */
public class StreamFunctions {

	/**
	 * Private, only static use.
	 */
	private StreamFunctions() {
	}

	/**
	 * Returns a new binary predicate that invokes
	 * {@link StreamUtil#find(Reader, String, int, boolean,
	 * de.esoco.lib.io.StreamUtil.ReadHandler)}.
	 *
	 * @param defaultToken The token to search
	 * @param max          The maximum number of characters to read
	 * @return A new binary predicate instance
	 */
	public static BinaryPredicate<InputStream, byte[]> find(
		final byte[] defaultToken, final int max) {
		return ThrowingBinaryPredicate.of(
			(stream, token) -> StreamUtil.find(stream,
				Option.of(token).orUse(defaultToken), max, null));
	}

	/**
	 * Returns a new binary predicate that invokes
	 * {@link StreamUtil#find(Reader, String, int, boolean,
	 * de.esoco.lib.io.StreamUtil.ReadHandler)}.
	 *
	 * @param defaultToken The token to search
	 * @param max          The maximum number of characters to read
	 * @param ignoreCase   TRUE if the case of the token should be ignored
	 * @return A new binary predicate instance
	 */
	public static BinaryPredicate<Reader, String> find(
		final String defaultToken, final int max, final boolean ignoreCase) {
		return ThrowingBinaryPredicate.of(
			(stream, token) -> StreamUtil.find(stream,
				Option.of(token).orUse(defaultToken), max, ignoreCase, null));
	}

	/**
	 * Returns a new function that invokes reads all available data from an
	 * input stream by invoking
	 * {@link StreamUtil#readAll(InputStream, int, int)}.
	 *
	 * @param bufferSize The buffer size to use
	 * @param maxLength  The maximum length to read
	 * @return A new function instance
	 */
	public static java.util.function.Function<InputStream, byte[]> readAll(
		int bufferSize, int maxLength) {
		return ThrowingFunction.of(
			input -> StreamUtil.readAll(input, bufferSize, maxLength));
	}

	/**
	 * Returns a new binary function that invokes the method
	 * {@link StreamUtil#readUntil(Reader, Writer, String, int, boolean)} and
	 * returns either the string found or NULL if the given token didn't occur
	 * in the data that has been read up to the maximum..
	 *
	 * @param defaultToken token The token to search
	 * @param max          The maximum number of characters to read
	 * @param ignoreCase   TRUE if the case of the token should be ignored
	 * @return A new binary function instance
	 */
	public static BiFunction<Reader, String, String> readUntil(
		final String defaultToken, final int max, final boolean ignoreCase) {
		return ThrowingBinaryFunction.of((reader, t) -> {
			Writer output = new StringWriter();
			String token = Option.of(t).orUse(defaultToken);
			String result = null;

			if (StreamUtil.readUntil(reader, output, token, max, ignoreCase)) {
				result = output.toString();
				result = result.substring(0, result.length() - token.length());
			}

			return result;
		});
	}
}
