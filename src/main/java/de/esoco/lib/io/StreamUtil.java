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

import de.esoco.lib.collection.ByteArray;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Arrays;


/********************************************************************
 * Class containing tools for access to IO streams.
 *
 * @author eso
 */
public final class StreamUtil
{
	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Seeks to the position after the next occurrence of a certain token in an
	 * input stream. This method does no extra buffering, so if necessary a
	 * buffered stream should be used for optimal performance. If this method
	 * returns TRUE the token had been found in the stream and the stream will
	 * be positioned directly behind the last byte in the token.
	 *
	 * <p>If the handler argument is not NULL, this instance of the inner class
	 * ByteHandler will be invoked for each byte that is read from the stream
	 * (including the search token).</p>
	 *
	 * @param  rIn      The input stream to read from
	 * @param  rToken   The token to search for; tokens of type byte[] are used
	 *                  directly
	 * @param  nMax     The maximum number of bytes to read
	 * @param  rHandler A handler to be invoked for values read or NULL for none
	 *
	 * @return TRUE if the token has been found, FALSE if the end of the stream
	 *         has been reached
	 *
	 * @throws IOException              If reading from the stream fails
	 * @throws IllegalArgumentException If the search token is empty
	 * @throws NullPointerException     If either stream or token is NULL
	 */
	public static boolean find(InputStream rIn,
							   byte[]	   rToken,
							   int		   nMax,
							   ReadHandler rHandler) throws IOException
	{
		int nTokenLength = rToken.length;
		int nRead		 = 0;
		int nPos		 = 0;

		if (nTokenLength == 0)
		{
			throw new IllegalArgumentException("Invalid search token");
		}

		while (nRead != -1 && nMax-- > 0)
		{
			nRead = rIn.read();

			if (nRead != -1)
			{
				byte nByte = (byte) nRead;

				if (rHandler != null)
				{
					rHandler.valueRead(nByte);
				}

				if (nByte != rToken[nPos++])
				{
					nPos = 0;
				}
				else if (nPos == nTokenLength)
				{
					return true;
				}
			}
		}

		return false;
	}

	/***************************************
	 * Searches a character input stream for a certain token. If this method
	 * returns TRUE the input stream reader will be positioned directly after
	 * the found token.
	 *
	 * @param  rInput      The input stream reader to read from
	 * @param  sToken      The token to search
	 * @param  nMax        The maximum number of bytes that should be read
	 * @param  bIgnoreCase TRUE if the case of the token should be ignored
	 * @param  rHandler    A handler to be invoked for values read or NULL for
	 *                     none
	 *
	 * @return TRUE if the token has been found, FALSE if not
	 *
	 * @throws IOException If accessing the stream fails
	 */
	public static boolean find(Reader	   rInput,
							   String	   sToken,
							   int		   nMax,
							   boolean	   bIgnoreCase,
							   ReadHandler rHandler) throws IOException
	{
		if (bIgnoreCase)
		{
			sToken = sToken.toLowerCase();
		}

		int nTokenLength = sToken.length();
		int nRead		 = 0;
		int nPos		 = 0;

		if (nTokenLength == 0)
		{
			throw new IllegalArgumentException("Invalid search token");
		}

		while (nRead != -1 && nMax-- > 0)
		{
			nRead = rInput.read();

			if (nRead != -1)
			{
				char cSearch = sToken.charAt(nPos++);

				if (rHandler != null)
				{
					rHandler.valueRead(nRead);
				}

				if (bIgnoreCase)
				{
					nRead = Character.toLowerCase((char) nRead);
				}

				if (nRead != cSearch)
				{
					nPos = 0;
				}
				else if (nPos == nTokenLength)
				{
					return true;
				}
			}
		}

		return false;
	}

	/***************************************
	 * Reads all bytes from an input stream and returns a byte array containing
	 * the data. The buffer size argument should be either the exact size that
	 * will be read or a reasonable fraction of the expected size to prevent
	 * unnecessary buffer allocations.
	 *
	 * @param  rIn         The input stream to read from
	 * @param  nBufferSize The initial buffer size
	 * @param  nMax        The maximum number of bytes to read
	 *
	 * @return A byte array containing the bytes read
	 *
	 * @throws IOException              In case of IO errors
	 * @throws IllegalArgumentException If the buffer size is invalid
	 */
	public static byte[] readAll(InputStream rIn, int nBufferSize, int nMax)
		throws IOException, IllegalArgumentException
	{
		if (nBufferSize <= 0)
		{
			throw new IllegalArgumentException("Buffer size must be > 0");
		}

		ByteArray aReadBuffer = new ByteArray(nBufferSize);
		byte[]    aBytes	  = new byte[nBufferSize];
		int		  nCount;

		while (nMax > 0 && (nCount = rIn.read(aBytes)) != -1)
		{
			aReadBuffer.add(aBytes, 0, nCount);
			nMax -= nCount;
		}

		return aReadBuffer.toByteArray();
	}

	/***************************************
	 * Reads all bytes from a {@link Reader} and returns a string containing the
	 * character data. The buffer size argument should be either the exact size
	 * that will be read or a reasonable fraction of the expected size to
	 * prevent unnecessary buffer allocations.
	 *
	 * @param  rIn         The reader to read from
	 * @param  nBufferSize The initial buffer size
	 * @param  nMax        The maximum number of bytes to read
	 *
	 * @return A byte array containing the bytes read
	 *
	 * @throws IOException              In case of IO errors
	 * @throws IllegalArgumentException If the buffer size is invalid
	 */
	public static String readAll(Reader rIn, int nBufferSize, int nMax)
		throws IOException, IllegalArgumentException
	{
		if (nBufferSize <= 0)
		{
			throw new IllegalArgumentException("Buffer size must be > 0");
		}

		StringBuilder aResult = new StringBuilder(nBufferSize);
		char[]		  aBuffer = new char[nBufferSize];
		int			  nCount;

		while (nMax > 0 && (nCount = rIn.read(aBuffer)) != -1)
		{
			aResult.append(aBuffer, 0, nCount);
			nMax -= nCount;
		}

		return aResult.toString();
	}

	/***************************************
	 * Reads all bytes from a stream until the next occurrence of a certain
	 * token and returns the bytes read as a byte array (without the search
	 * token). Invokes {@link #find(InputStream, byte[], int, ReadHandler)} to
	 * perform the actual search.
	 *
	 * @param  rIn    The input stream to read from
	 * @param  rToken The token to read up to
	 * @param  nMax   The maximum number of bytes to read
	 *
	 * @return A byte array containing the bytes read from the stream up to the
	 *         search token or NULL if the token couldn't be found.
	 *
	 * @throws EOFException             If the stream ends before the string
	 *                                  token could be found
	 * @throws IOException              If reading from the stream fails
	 * @throws IllegalArgumentException If the search string is empty
	 * @throws NullPointerException     If either argument is NULL
	 */
	public static byte[] readUntil(InputStream rIn, byte[] rToken, int nMax)
		throws IOException
	{
		final ByteArrayOutputStream aData   = new ByteArrayOutputStream();
		byte[]					    aResult = null;

		boolean bFound =
			find(rIn,
				 rToken,
				 nMax,
				new ReadHandler()
				{
					@Override
					public void valueRead(int nByte)
					{
						aData.write(nByte);
					}
				});

		if (bFound)
		{
			aResult =
				Arrays.copyOf(aData.toByteArray(),
							  aData.size() - rToken.length);
		}

		return aResult;
	}

	/***************************************
	 * Reads a string from a character stream until the next occurrence of a
	 * certain token and returns the bytes read as a byte array (without the
	 * search token). Invokes {@link #find(Reader, String, int, boolean,
	 * ReadHandler)} to perform the actual search.
	 *
	 * @param  rInput      The input stream to read from
	 * @param  sToken      The token to read up to
	 * @param  nMax        The maximum number of bytes to read
	 * @param  bIgnoreCase TRUE if the case of the token should be ignored
	 *
	 * @return The string read from the stream up to and including the search
	 *         token or NULL if the token couldn't be found
	 *
	 * @throws IOException              If reading from the stream fails
	 * @throws IllegalArgumentException If the search string is empty
	 * @throws NullPointerException     If either argument is NULL
	 */
	public static String readUntil(Reader  rInput,
								   String  sToken,
								   int	   nMax,
								   boolean bIgnoreCase) throws IOException
	{
		final StringWriter aData   = new StringWriter();
		String			   sResult = null;

		boolean bFound =
			find(rInput,
				 sToken,
				 nMax,
				 bIgnoreCase,
				new ReadHandler()
				{
					@Override
					public void valueRead(int nValue)
					{
						aData.write(nValue);
					}
				});

		if (bFound)
		{
			StringBuffer rBuffer = aData.getBuffer();

			sResult = rBuffer.substring(0, rBuffer.length() - sToken.length());
		}

		return sResult;
	}

	/***************************************
	 * Reads bytes from an input stream and saves them into a file.
	 *
	 * @param  rStream     The input stream to read data from
	 * @param  sOutputFile The name of the file to write the data into
	 * @param  nMax        The maximum number of bytes to read from the stream;
	 *                     if the stream contains less bytes only these will be
	 *                     written
	 *
	 * @throws IOException If accessing the stream or the file fails
	 */
	public static void saveStream(InputStream rStream,
								  String	  sOutputFile,
								  int		  nMax) throws IOException
	{
		FileOutputStream fo = new FileOutputStream(sOutputFile);
		int				 rd;

		while ((nMax > 0) && ((rd = rStream.read()) >= 0))
		{
			fo.write(rd);
			nMax--;
		}

		fo.close();
	}

	/***************************************
	 * Sends the data from an input stream to an output stream. This method uses
	 * a buffer of 4K for the transfer so it is not necessary to wrap the
	 * streams in buffered streams.
	 *
	 * @param  rInput  The input stream to read the data to send from
	 * @param  rOutput The target output stream
	 *
	 * @return The number of bytes sent
	 *
	 * @throws IOException If a stream access fails
	 */
	public static long send(InputStream rInput, OutputStream rOutput)
		throws IOException
	{
		byte[] aBuffer = new byte[1024 * 4];
		long   nCount  = 0;
		int    nRead   = 0;

		while ((nRead = rInput.read(aBuffer)) != -1)
		{
			rOutput.write(aBuffer, 0, nRead);
			nCount += nRead;
		}

		return nCount;
	}

	/***************************************
	 * Sends the data from a reader to a writer. This method uses a buffer of 4K
	 * for the transfer so it is not necessary to wrap the streams in buffered
	 * streams.
	 *
	 * @param  rInput  The reader to read the data to send from
	 * @param  rOutput The target writer
	 *
	 * @return The number of characters sent
	 *
	 * @throws IOException If a stream access fails
	 */
	public static long send(Reader rInput, Writer rOutput) throws IOException
	{
		char[] aBuffer = new char[1024 * 4];
		long   nCount  = 0;
		int    nRead   = 0;

		while ((nRead = rInput.read(aBuffer)) != -1)
		{
			rOutput.write(aBuffer, 0, nRead);
			nCount += nRead;
		}

		return nCount;
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * Used by StreamUtil methods to process values that have been read from a
	 * stream.
	 */
	public static abstract class ReadHandler
	{
		//~ Methods ------------------------------------------------------------

		/***************************************
		 * This method will be invoked for each single value that has been read
		 * from the stream.
		 *
		 * @param nValue The value read from the stream
		 */
		public abstract void valueRead(int nValue);
	}
}
