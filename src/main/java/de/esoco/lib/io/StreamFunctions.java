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

import de.esoco.lib.expression.BinaryFunction;
import de.esoco.lib.expression.BinaryPredicate;
import de.esoco.lib.expression.Function;
import de.esoco.lib.expression.FunctionException;
import de.esoco.lib.expression.function.AbstractFunction;
import de.esoco.lib.expression.function.ExceptionMappingBinaryFunction;
import de.esoco.lib.expression.predicate.ExceptionMappingBinaryPredicate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
	 * Returns a new function that creates a new {@link InputStreamReader} from
	 * an {@link InputStream} input value. The reader will be stored in a field
	 * of the function instance to prevent it from being closed by garbage
	 * collection which would cause the underlying stream to be closed too.
	 *
	 * @return A new function instance
	 */
	public static Function<InputStream, Reader> createReader()
	{
		return new AbstractFunction<InputStream, Reader>("createReader")
		{
			InputStreamReader rReader;

			@Override
			public Reader evaluate(InputStream rInputStream)
			{
				rReader = new InputStreamReader(rInputStream);

				return rReader;
			}
		};
	}

	/***************************************
	 * Returns a new binary predicate that invokes {@link
	 * StreamUtil#find(Reader, String, int, boolean,
	 * de.esoco.lib.io.StreamUtil.ReadHandler)}.
	 *
	 * @param  rToken The token to search
	 * @param  nMax   The maximum number of characters to read
	 *
	 * @return A new binary predicate instance
	 */
	public static BinaryPredicate<InputStream, byte[]> find(
		final byte[] rToken,
		final int    nMax)
	{
		return new ExceptionMappingBinaryPredicate<InputStream, byte[]>(rToken,
																		"InputStreamFind")
		{
			@Override
			@SuppressWarnings("boxing")
			public Boolean evaluateWithException(
				InputStream rInput,
				byte[]		rToken) throws IOException
			{
				return StreamUtil.find(rInput, rToken, nMax, null);
			}
		};
	}

	/***************************************
	 * Returns a new binary predicate that invokes {@link
	 * StreamUtil#find(Reader, String, int, boolean,
	 * de.esoco.lib.io.StreamUtil.ReadHandler)}.
	 *
	 * @param  sToken      The token to search
	 * @param  nMax        The maximum number of characters to read
	 * @param  bIgnoreCase TRUE if the case of the token should be ignored
	 *
	 * @return A new binary predicate instance
	 */
	public static BinaryPredicate<Reader, String> find(
		final String  sToken,
		final int	  nMax,
		final boolean bIgnoreCase)
	{
		return new ExceptionMappingBinaryPredicate<Reader, String>(sToken,
																   "ReaderFind")
		{
			@Override
			@SuppressWarnings("boxing")
			public Boolean evaluateWithException(Reader rInput, String sToken)
				throws IOException
			{
				return StreamUtil.find(rInput, sToken, nMax, bIgnoreCase, null);
			}
		};
	}

	/***************************************
	 * Returns a new binary function that invokes the method {@link
	 * StreamUtil#readUntil(Reader, StringBuilder, String, int, boolean)} and
	 * returns either the string found or NULL if the given token didn't occur
	 * in the data that has been read up to the maximum..
	 *
	 * @param  sToken      The token to search
	 * @param  nMax        The maximum number of characters to read
	 * @param  bIgnoreCase TRUE if the case of the token should be ignored
	 *
	 * @return A new binary function instance
	 */
	public static BinaryFunction<Reader, String, String> readUntil(
		final String  sToken,
		final int	  nMax,
		final boolean bIgnoreCase)
	{
		return new ExceptionMappingBinaryFunction<Reader, String, String>(sToken,
																		  "ReadUntil")
		{
			@Override
			protected String evaluateWithException(
				Reader rReader,
				String sToken) throws Exception
			{
				Writer aOutput = new StringWriter();
				String sResult = null;

				if (StreamUtil.readUntil(rReader,
										 aOutput,
										 sToken,
										 nMax,
										 bIgnoreCase))
				{
					sResult = aOutput.toString();
					sResult =
						sResult.substring(0,
										  sResult.length() - sToken.length());
				}

				return sResult;
			}
		};
	}
}
