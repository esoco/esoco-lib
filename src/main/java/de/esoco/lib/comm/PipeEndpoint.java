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
package de.esoco.lib.comm;

import de.esoco.lib.collection.ByteArray;
import de.esoco.lib.io.LimitedOutputStream;
import de.esoco.lib.io.RandomAccessFileOutputStream;
import org.obrel.core.RelationType;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.function.Function;

import static de.esoco.lib.comm.CommunicationRelationTypes.BUFFER_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_REQUEST_SIZE;
import static org.obrel.core.RelationTypes.newType;

/**
 * An endpoint that uses a named pipe for communication.
 *
 * @author eso
 */
public class PipeEndpoint extends Endpoint {

	/**
	 * The random access file used by a pipe endpoint.
	 */
	public static final RelationType<RandomAccessFile> PIPE_FILE = newType();

	/**
	 * Factory method that creates a new pipe request for text data request.
	 *
	 * @param defaultRequest The default request string
	 * @return The new request
	 */
	public static PipeRequest<String, String> textRequest(
		String defaultRequest) {
		return new PipeRequest<String, String>("PipeRequest(%s)",
			defaultRequest, s -> s.getBytes(), d -> new String(d));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection connection) throws Exception {
		RandomAccessFile pipeFile = connection.get(PIPE_FILE);

		if (pipeFile != null) {
			pipeFile.close();
			connection.set(PIPE_FILE, null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initConnection(Connection connection) throws Exception {
		String pipeName = connection.getUri().getSchemeSpecificPart();

		RandomAccessFile pipeFile = new RandomAccessFile(pipeName, "rw");

		connection.set(PIPE_FILE, pipeFile);
	}

	/**
	 * A request method to be executed on a {@link PipeEndpoint}. This class
	 * can
	 * either be used directly by providing functions for input conversion and
	 * response processing or it can be subclassed by overriding the methods
	 * {@link #convertInput(Object)} and {@link #processResponse(byte[])}. In
	 * the latter case the functions handed to the constructor should be set to
	 * NULL.
	 *
	 * @author eso
	 */
	public static class PipeRequest<I, O> extends CommunicationMethod<I, O> {

		private final Function<I, byte[]> convertInput;

		private final Function<byte[], O> processResponse;

		/**
		 * Creates a new instance.
		 *
		 * @param requestName     The request name
		 * @param defaultInput    The default input value
		 * @param convertInput    Will be invoked to convert input values to a
		 *                        byte arrays that will be written to the pipe
		 * @param processResponse Will be invoked to process the byte data
		 *                        received from the pipe
		 */
		public PipeRequest(String requestName, I defaultInput,
			Function<I, byte[]> convertInput,
			Function<byte[], O> processResponse) {
			super(requestName, defaultInput);

			this.convertInput = convertInput;
			this.processResponse = processResponse;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public O doOn(Connection connection, I input) throws IOException {
			RandomAccessFile pipeFile = connection.get(PIPE_FILE);

			try (LimitedOutputStream output = new LimitedOutputStream(
				new RandomAccessFileOutputStream(pipeFile),
				connection.get(MAX_REQUEST_SIZE))) {
				output.write(convertInput(input));
			}

			ByteArray rawResponse = new ByteArray(connection.get(BUFFER_SIZE));

			int b;

			while ((b = pipeFile.read()) != -1 && pipeFile.length() > 0) {
				rawResponse.add((byte) b);
			}

			return processResponse(rawResponse.toByteArray());
		}

		/**
		 * Returns the byte data to be sent for a request. The default
		 * implementation invokes the input conversion function.
		 *
		 * @param input The input value
		 * @return The request bytes
		 */
		protected byte[] convertInput(I input) {
			return convertInput.apply(input);
		}

		/**
		 * Returns the response value for a request. The default implementation
		 * invokes the response processing function.
		 *
		 * @param rawResponse The raw response as received from the pipe
		 * @return The processed response
		 */
		protected O processResponse(byte[] rawResponse) {
			return processResponse.apply(rawResponse);
		}
	}
}
