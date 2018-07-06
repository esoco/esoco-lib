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

import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.function.Function;

import org.obrel.core.RelationType;

import static de.esoco.lib.comm.CommunicationRelationTypes.BUFFER_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_REQUEST_SIZE;

import static org.obrel.core.RelationTypes.newType;


/********************************************************************
 * An endpoint that uses a named pipe for communication.
 *
 * @author eso
 */
public class PipeEndpoint extends Endpoint
{
	//~ Static fields/initializers ---------------------------------------------

	/** The random access file used by a pipe endpoint. */
	public static final RelationType<RandomAccessFile> PIPE_FILE = newType();

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Factory method that creates a new pipe request for text data request.
	 *
	 * @param  sDefaultRequest The default request string
	 *
	 * @return The new request
	 */
	public static PipeRequest<String, String> textRequest(
		String sDefaultRequest)
	{
		return new PipeRequest<String, String>("PipeRequest(%s)",
											   sDefaultRequest,
											   s -> s.getBytes(),
											   d -> new String(d));
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection rConnection) throws Exception
	{
		RandomAccessFile rPipeFile = rConnection.get(PIPE_FILE);

		if (rPipeFile != null)
		{
			rPipeFile.close();
			rConnection.set(PIPE_FILE, null);
		}
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void initConnection(Connection rConnection) throws Exception
	{
		String sPipeName = rConnection.getUri().getSchemeSpecificPart();

		RandomAccessFile aPipeFile = new RandomAccessFile(sPipeName, "rw");

		rConnection.set(PIPE_FILE, aPipeFile);
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A request method to be executed on a {@link PipeEndpoint}. This class can
	 * either be used directly by providing functions for input conversion and
	 * response processing or it can be subclassed by overriding the methods
	 * {@link #convertInput(Object)} and {@link #processResponse(byte[])}. In
	 * the latter case the functions handed to the constructor should be set to
	 * NULL.
	 *
	 * @author eso
	 */
	public static class PipeRequest<I, O> extends CommunicationMethod<I, O>
	{
		//~ Instance fields ----------------------------------------------------

		private Function<I, byte[]> fConvertInput;
		private Function<byte[], O> fProcessResponse;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sRequestName     The request name
		 * @param rDefaultInput    The default input value
		 * @param fConvertInput    Will be invoked to convert input values to a
		 *                         byte arrays that will be written to the pipe
		 * @param fProcessResponse Will be invoked to process the byte data
		 *                         received from the pipe
		 */
		public PipeRequest(String			   sRequestName,
						   I				   rDefaultInput,
						   Function<I, byte[]> fConvertInput,
						   Function<byte[], O> fProcessResponse)
		{
			super(sRequestName, rDefaultInput);

			this.fConvertInput    = fConvertInput;
			this.fProcessResponse = fProcessResponse;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public O doOn(Connection rConnection, I rInput) throws IOException
		{
			RandomAccessFile rPipeFile = rConnection.get(PIPE_FILE);

			try (LimitedOutputStream rOutput =
				 new LimitedOutputStream(new RandomAccessFileOutputStream(rPipeFile),
										 rConnection.get(MAX_REQUEST_SIZE)))
			{
				rOutput.write(convertInput(rInput));
			}

			ByteArray aRawResponse =
				new ByteArray(rConnection.get(BUFFER_SIZE));

			int nByte;

			while ((nByte = rPipeFile.read()) != -1 && rPipeFile.length() > 0)
			{
				aRawResponse.add((byte) nByte);
			}

			return processResponse(aRawResponse.toByteArray());
		}

		/***************************************
		 * Returns the byte data to be sent for a request. The default
		 * implementation invokes the input conversion function.
		 *
		 * @param  rInput The input value
		 *
		 * @return The request bytes
		 */
		protected byte[] convertInput(I rInput)
		{
			return fConvertInput.apply(rInput);
		}

		/***************************************
		 * Returns the response value for a request. The default implementation
		 * invokes the response processing function.
		 *
		 * @param  rRawResponse The raw response as received from the pipe
		 *
		 * @return The processed response
		 */
		protected O processResponse(byte[] rRawResponse)
		{
			return fProcessResponse.apply(rRawResponse);
		}
	}
}
