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

import de.esoco.lib.expression.BinaryFunction;
import de.esoco.lib.expression.BinaryPredicate;
import de.esoco.lib.expression.Function;
import de.esoco.lib.expression.FunctionException;
import de.esoco.lib.expression.function.ThrowingBinaryFunction;
import de.esoco.lib.expression.function.ThrowingFunction;
import de.esoco.lib.expression.monad.Option;
import de.esoco.lib.expression.predicate.ThrowingBinaryPredicate;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;


/********************************************************************
 * Provides access to stream and I/O related function implementations. All
 * functions map occurring IO exceptions to a {@link FunctionException} runtime
 * exception.
 *
 * @author eso
 */
public class StreamFunctions
{
	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Private, only static use.
	 */
	private StreamFunctions()
	{
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns a new binary predicate that invokes {@link
	 * StreamUtil#find(Reader, String, int, boolean,
	 * de.esoco.lib.io.StreamUtil.ReadHandler)}.
	 *
	 * @param  rDefaultToken The token to search
	 * @param  nMax          The maximum number of characters to read
	 *
	 * @return A new binary predicate instance
	 */
	public static BinaryPredicate<InputStream, byte[]> find(
		final byte[] rDefaultToken,
		final int    nMax)
	{
		return ThrowingBinaryPredicate.of(
			(stream, token) ->
				StreamUtil.find(
					stream,
					Option.of(token).orUse(rDefaultToken),
					nMax,
					null));
	}

	/***************************************
	 * Returns a new binary predicate that invokes {@link
	 * StreamUtil#find(Reader, String, int, boolean,
	 * de.esoco.lib.io.StreamUtil.ReadHandler)}.
	 *
	 * @param  sDefaultToken The token to search
	 * @param  nMax          The maximum number of characters to read
	 * @param  bIgnoreCase   TRUE if the case of the token should be ignored
	 *
	 * @return A new binary predicate instance
	 */
	public static BinaryPredicate<Reader, String> find(
		final String  sDefaultToken,
		final int	  nMax,
		final boolean bIgnoreCase)
	{
		return ThrowingBinaryPredicate.of(
			(stream, token) ->
				StreamUtil.find(
					stream,
					Option.of(token).orUse(sDefaultToken),
					nMax,
					bIgnoreCase,
					null));
	}

	/***************************************
	 * Returns a new function that invokes reads all available data from an
	 * input stream by invoking {@link StreamUtil#readAll(InputStream, int,
	 * int)}.
	 *
	 * @param  nBufferSize The buffer size to use
	 * @param  nMaxLength  The maximum length to read
	 *
	 * @return A new function instance
	 */
	public static Function<InputStream, byte[]> readAll(
		int nBufferSize,
		int nMaxLength)
	{
		return ThrowingFunction.of(
			rInput -> StreamUtil.readAll(rInput, nBufferSize, nMaxLength));
	}

	/***************************************
	 * Returns a new binary function that invokes the method {@link
	 * StreamUtil#readUntil(Reader, Writer, String, int, boolean)} and returns
	 * either the string found or NULL if the given token didn't occur in the
	 * data that has been read up to the maximum..
	 *
	 * @param  sDefaultToken sToken The token to search
	 * @param  nMax          The maximum number of characters to read
	 * @param  bIgnoreCase   TRUE if the case of the token should be ignored
	 *
	 * @return A new binary function instance
	 */
	public static BinaryFunction<Reader, String, String> readUntil(
		final String  sDefaultToken,
		final int	  nMax,
		final boolean bIgnoreCase)
	{
		return ThrowingBinaryFunction.of(
			(reader, token) ->
			{
				Writer aOutput = new StringWriter();
				String sToken  = Option.of(token).orUse(sDefaultToken);
				String sResult = null;

				if (StreamUtil.readUntil(
						reader,
						aOutput,
						sToken,
						nMax,
						bIgnoreCase))
				{
					sResult = aOutput.toString();
					sResult =
						sResult.substring(0, sResult.length() -
							sToken.length());
				}

				return sResult;
			});
	}
}
