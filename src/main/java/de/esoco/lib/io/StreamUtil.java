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

import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Class containing tools for access to IO streams.
 *
 * @author eso
 */
public final class StreamUtil {

	/**
	 * Invokes {@link #find(InputStream, byte[], int, ReadHandler)} with no
	 * read
	 * handler.
	 *
	 * @see #find(InputStream, byte[], int, ReadHandler)
	 */
	public static boolean find(InputStream in, byte[] token, int max)
		throws IOException {
		return find(in, token, max, null);
	}

	/**
	 * Invokes {@link #find(Reader, String, int, boolean, ReadHandler)} with no
	 * read handler.
	 *
	 * @see #find(Reader, String, int, boolean, ReadHandler)
	 */
	public static boolean find(Reader reader, String token, int max,
		boolean ignoreCase) throws IOException {
		return find(reader, token, max, ignoreCase, null);
	}

	/**
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
	 * @param in      The input stream to read from
	 * @param token   The token to search for; tokens of type byte[] are used
	 *                directly
	 * @param max     The maximum number of bytes to read
	 * @param handler A handler to be invoked for values read or NULL for none
	 * @return TRUE if the token has been found, FALSE if the end of the stream
	 * has been reached
	 * @throws IOException              If reading from the stream fails
	 * @throws IllegalArgumentException If the search token is empty
	 */
	public static boolean find(InputStream in, byte[] token, int max,
		ReadHandler handler) throws IOException {
		int tokenLength = token.length;
		int read = 0;
		int pos = 0;

		if (tokenLength == 0) {
			throw new IllegalArgumentException("Invalid search token");
		}

		while (read != -1 && max-- > 0) {
			read = in.read();

			if (read != -1) {
				byte b = (byte) read;

				if (handler != null) {
					handler.valueRead(b);
				}

				if (b != token[pos++]) {
					pos = 0;
				} else if (pos == tokenLength) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Searches a character input stream for a certain token. If this method
	 * returns TRUE the input stream reader will be positioned directly after
	 * the found token.
	 *
	 * @param input      The input stream reader to read from
	 * @param token      The token to search
	 * @param max        The maximum number of characters that should be read
	 * @param ignoreCase TRUE if the case of the token should be ignored
	 * @param handler    A handler to be invoked for values read or NULL for
	 *                   none
	 * @return TRUE if the token has been found, FALSE if not
	 * @throws IOException              If accessing the stream fails
	 * @throws IllegalArgumentException If the search token is empty
	 */
	public static boolean find(Reader input, String token, int max,
		boolean ignoreCase, ReadHandler handler) throws IOException {
		if (ignoreCase) {
			token = token.toLowerCase();
		}

		int tokenLength = token.length();
		int read = 0;
		int pos = 0;

		if (tokenLength == 0) {
			throw new IllegalArgumentException("Invalid search token");
		}

		while (read != -1 && max-- > 0) {
			read = input.read();

			if (read != -1) {
				char search = token.charAt(pos++);

				if (handler != null) {
					handler.valueRead(read);
				}

				if (ignoreCase) {
					read = Character.toLowerCase((char) read);
				}

				if (read != search) {
					pos = 0;
				} else if (pos == tokenLength) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Reads all bytes from an input stream and returns a byte array containing
	 * the data. The buffer size argument should be either the exact size that
	 * will be read or a reasonable fraction of the expected size to prevent
	 * unnecessary buffer allocations.
	 *
	 * @param in         The input stream to read from
	 * @param bufferSize The initial buffer size
	 * @param max        The maximum number of bytes to read
	 * @return A byte array containing the bytes read
	 * @throws IOException              In case of IO errors
	 * @throws IllegalArgumentException If the buffer size is invalid
	 */
	public static byte[] readAll(InputStream in, int bufferSize, int max)
		throws IOException, IllegalArgumentException {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException("Buffer size must be > 0");
		}

		ByteArray readBuffer = new ByteArray(bufferSize);
		byte[] bytes = new byte[bufferSize];
		int readMax = Math.min(max, bufferSize);
		int count;

		while (max > 0 && (count = in.read(bytes, 0, readMax)) != -1) {
			readBuffer.add(bytes, 0, count);
			max -= count;
			readMax = Math.min(max, bufferSize);
		}

		return readBuffer.toByteArray();
	}

	/**
	 * Reads all bytes from a {@link Reader} and returns a string containing
	 * the
	 * character data. The buffer size argument should be either the exact size
	 * that will be read or a reasonable fraction of the expected size to
	 * prevent unnecessary buffer allocations.
	 *
	 * @param in         The reader to read from
	 * @param bufferSize The initial buffer size
	 * @param max        The maximum number of bytes to read
	 * @return A byte array containing the bytes read
	 * @throws IOException              In case of IO errors
	 * @throws IllegalArgumentException If the buffer size is invalid
	 */
	public static String readAll(Reader in, int bufferSize, int max)
		throws IOException, IllegalArgumentException {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException("Buffer size must be > 0");
		}

		StringBuilder result = new StringBuilder(bufferSize);
		char[] buffer = new char[bufferSize];
		int readMax = Math.min(max, bufferSize);
		int count;

		while (max > 0 && (count = in.read(buffer, 0, readMax)) != -1) {
			result.append(buffer, 0, count);
			max -= count;
			readMax = Math.min(max, bufferSize);
		}

		return result.toString();
	}

	/**
	 * Reads all bytes from a stream until the next occurrence of a certain
	 * token and and returns a boolean that indicates whether the token has
	 * been
	 * found. The data will be read into the output stream argument. If the
	 * token is found the output will end at it's last byte. Otherwise it
	 * contains all data that had been read until the maximum number of bytes
	 * had been reached.
	 *
	 * <p>Invokes {@link #find(InputStream, byte[], int, ReadHandler)} to
	 * perform the actual search.</p>
	 *
	 * @param in    The input stream to read from
	 * @param out   The output stream to write data to
	 * @param token The token to read up to
	 * @param max   The maximum number of bytes to read
	 * @return A byte array containing the bytes read from the stream up to the
	 * search token or NULL if the token couldn't be found.
	 * @throws EOFException             If the stream ends before the string
	 *                                  token could be found
	 * @throws IOException              If reading from the stream fails
	 * @throws IllegalArgumentException If the search string is empty
	 * @throws IllegalStateException    If writing to the output stream fails
	 *                                  (wraps the IO exception that occurred)
	 */
	public static boolean readUntil(InputStream in, OutputStream out,
		byte[] token, int max) throws IOException {
		return find(in, token, max, new ReadHandler() {
			@Override
			public void valueRead(int b) {
				try {
					out.write(b);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		});
	}

	/**
	 * Reads a string from a character stream until the next occurrence of a
	 * certain token and returns a boolean that indicates whether the token has
	 * been found. The data will be read into the output writer argument. If
	 * the
	 * token is found the output will end at it's last character. Otherwise it
	 * contains all data that had been read until the maximum number of
	 * characters had been reached.
	 *
	 * <p>Invokes {@link #find(Reader, String, int, boolean, ReadHandler)} to
	 * perform the actual search.</p>
	 *
	 * @param in         The reader to read the data from
	 * @param out        A writer to write the characters that have been to
	 * @param token      The token to read up to
	 * @param max        The maximum number of characters to read
	 * @param ignoreCase TRUE if the case of the token should be ignored
	 * @return The string read from the stream up to but excluding the search
	 * token or NULL if the token couldn't be found
	 * @throws IOException              If reading from the stream fails
	 * @throws IllegalArgumentException If the search string is empty
	 * @throws NullPointerException     If either argument is NULL
	 * @throws IllegalStateException    If writing to the output stream fails
	 *                                  (wraps the IO exception that occurred)
	 */
	public static boolean readUntil(Reader in, Writer out, String token,
		int max, boolean ignoreCase) throws IOException {
		return find(in, token, max, ignoreCase, new ReadHandler() {
			@Override
			public void valueRead(int value) {
				try {
					out.write(value);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		});
	}

	/**
	 * Reads bytes from an input stream and saves them into a file.
	 *
	 * @param stream     The input stream to read data from
	 * @param outputFile The name of the file to write the data into
	 * @param max        The maximum number of bytes to read from the
	 *                      stream; if
	 *                   the stream contains less bytes only these will be
	 *                   written
	 * @throws IOException If accessing the stream or the file fails
	 */
	public static void saveStream(InputStream stream, String outputFile,
		int max) throws IOException {
		FileOutputStream fo = new FileOutputStream(outputFile);
		int rd;

		while ((max > 0) && ((rd = stream.read()) >= 0)) {
			fo.write(rd);
			max--;
		}

		fo.close();
	}

	/**
	 * Sends the data from an input stream to an output stream. This method
	 * uses
	 * a buffer of 8K for the transfer so it is not necessary to wrap the
	 * streams in buffered streams.
	 *
	 * @param input  The input stream to read the data to send from
	 * @param output The target output stream
	 * @return The number of bytes sent
	 * @throws IOException If a stream access fails
	 */
	public static long send(InputStream input, OutputStream output)
		throws IOException {
		byte[] buffer = new byte[1024 * 8];
		long count = 0;
		int read = 0;

		while ((read = input.read(buffer)) != -1) {
			output.write(buffer, 0, read);
			count += read;
		}

		return count;
	}

	/**
	 * Sends the data from a reader to a writer. This method uses a buffer
	 * of 8K
	 * for the transfer so it is not necessary to wrap the streams in buffered
	 * streams.
	 *
	 * @param input  The reader to read the data to send from
	 * @param output The target writer
	 * @return The number of characters sent
	 * @throws IOException If a stream access fails
	 */
	public static long send(Reader input, Writer output) throws IOException {
		char[] buffer = new char[1024 * 8];
		long count = 0;
		int read = 0;

		while ((read = input.read(buffer)) != -1) {
			output.write(buffer, 0, read);
			count += read;
		}

		return count;
	}

	/**
	 * Used by StreamUtil methods to process values that have been read from a
	 * stream.
	 */
	public static abstract class ReadHandler {

		/**
		 * This method will be invoked for each single value that has been read
		 * from the stream.
		 *
		 * @param value The value read from the stream
		 */
		public abstract void valueRead(int value);
	}
}
