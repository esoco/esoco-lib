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
package de.esoco.lib.comm;

import de.esoco.lib.expression.Function;
import de.esoco.lib.io.StreamUtil;

import java.io.InputStream;

import java.net.InetSocketAddress;
import java.net.URI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.obrel.core.Params;
import org.obrel.core.RelationType;
import org.obrel.core.RelationTypeModifier;
import org.obrel.core.RelationTypes;

import sun.net.ftp.FtpClient;
import sun.net.ftp.FtpDirEntry;

import static de.esoco.lib.comm.CommunicationRelationTypes.BUFFER_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.CONNECTION_TIMEOUT;
import static de.esoco.lib.comm.CommunicationRelationTypes.ENCRYPTION;
import static de.esoco.lib.comm.CommunicationRelationTypes.MAX_RESPONSE_SIZE;
import static de.esoco.lib.comm.CommunicationRelationTypes.PASSWORD;
import static de.esoco.lib.comm.CommunicationRelationTypes.USER_NAME;


/********************************************************************
 * An endpoint for FTP connections.
 *
 * @author eso
 */
public class FtpEndpoint extends Endpoint
{
	//~ Enums ------------------------------------------------------------------

	/********************************************************************
	 * Enumeration of the possible file types.
	 */
	public enum FileType { FILE, DIR, LINK }

	//~ Static fields/initializers ---------------------------------------------

	/** The standard port for FTP connections. */
	public static final int FTP_PORT = 21;

	private static final RelationType<FtpClient> FTP_CLIENT =
		RelationTypes.newType(RelationTypeModifier.PRIVATE);

	static
	{
		RelationTypes.init(FtpEndpoint.class);
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Returns a new FTP request method that downloads a certain file. To
	 * process files that are larger that the default response size defined by
	 * {@link CommunicationRelationTypes#MAX_RESPONSE_SIZE} that relation needs
	 * to be set in the connection parameters to the corresponding value.
	 *
	 * @param  sDefaultFile The default file to download (can be overridden with
	 *                      a specific method argument)
	 *
	 * @return The binary data of the file
	 */
	public static FtpRequest<String, byte[]> download(String sDefaultFile)
	{
		return new DownloadFile(sDefaultFile);
	}

	/***************************************
	 * Returns a new FTP request method that lists all files in a certain
	 * directory. If the directory is prefixed with one of the FTP file types in
	 * the enum {@link FileType} (separated from the directory by a colon ':')
	 * only files of that type will be returned.
	 *
	 * @param  sDefaultDir The default directory to be queried (can be
	 *                     overridden with a specific method argument)
	 *
	 * @return The list of files in the given directory
	 */
	public static FtpRequest<String, List<String>> listFiles(String sDefaultDir)
	{
		return new ListFiles(sDefaultDir);
	}

	/***************************************
	 * Main method for testing.
	 *
	 * @param rArgs
	 */
	public static void main(String[] rArgs)
	{
		Endpoint aTestFtpServer = Endpoint.at("ftp://speedtest.tele2.net/");

		Function<String, List<String>> fListFiles =
			listFiles("DIR:.").from(aTestFtpServer);

		System.out.printf("DIRS : %s\n", fListFiles.result());
		System.out.printf("FILES: %s\n", fListFiles.evaluate("FILE:."));

		EndpointChain<String, byte[]> fDownloadFile =
			download("").from(aTestFtpServer);

		Params aParams = new Params().with(MAX_RESPONSE_SIZE, 2048);

		System.out.printf("FILE SIZE: %s\n",
						  fDownloadFile.evaluate("1KB.zip", aParams).length);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void closeConnection(Connection rConnection) throws Exception
	{
		rConnection.get(FTP_CLIENT).close();
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("boxing")
	protected void initConnection(Connection rConnection) throws Exception
	{
		FtpClient aFtpClient = FtpClient.create();
		URI		  rUri		 = rConnection.getUri();
		String    sUser		 = rConnection.get(USER_NAME);
		String    sPassword  = rConnection.get(PASSWORD);
		String    sHost		 = rUri.getHost();
		int		  nPort		 = rUri.getPort();
		int		  nTimeout   = rConnection.get(CONNECTION_TIMEOUT);

		if (sUser == null)
		{
			sUser = "anonymous";

			if (sPassword == null)
			{
				sPassword = "test@test.com";
			}
		}

		if (nPort < 0)
		{
			nPort = FTP_PORT;
		}

		aFtpClient.connect(new InetSocketAddress(sHost, nPort), nTimeout);
		aFtpClient.login(sUser, sPassword.toCharArray());

		if (hasFlag(ENCRYPTION))
		{
			aFtpClient.startSecureSession();
		}

		rConnection.set(FTP_CLIENT, aFtpClient);
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A generic base class for socket communication method that sends data to
	 * an endpoint socket and optionally receives a response.
	 *
	 * @author eso
	 */
	public static abstract class FtpRequest<I, O>
		extends CommunicationMethod<I, O>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		public FtpRequest(String sMethodName, I rDefaultInput)
		{
			super(sMethodName, rDefaultInput);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		public O doOn(Connection rConnection, I rInput)
		{
			try
			{
				FtpClient rFtpClient = rConnection.get(FTP_CLIENT);

				O rResult = executeRequest(rConnection, rFtpClient, rInput);

				rFtpClient.completePending();

				return rResult;
			}
			catch (Exception e)
			{
				throw new CommunicationException(e);
			}
		}

		/***************************************
		 * Must be implemented to perform the actual FTP command on the client
		 * and produce the method result.
		 *
		 * @param  rConnection The connection of the request
		 * @param  rFtpClient  The FTP client to execute the command on
		 * @param  rInput      The input to the method execution
		 *
		 * @return The method result
		 *
		 * @throws Exception In the case of an error
		 */
		protected abstract O executeRequest(Connection rConnection,
											FtpClient  rFtpClient,
											I		   rInput) throws Exception;
	}

	/********************************************************************
	 * Lists the files in a certain directory. If the directory is prefixed with
	 * one of the FTP file types in the enum {@link FileType} (separated from
	 * the directory by a colon ':') only files of that type will be returned.
	 *
	 * @author eso
	 */
	public static class DownloadFile extends FtpRequest<String, byte[]>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sDefaultFile The default directory
		 */
		public DownloadFile(String sDefaultFile)
		{
			super("DownloadFile", sDefaultFile);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		@SuppressWarnings("boxing")
		protected byte[] executeRequest(Connection rConnection,
										FtpClient  rFtpClient,
										String	   sFile) throws Exception
		{
			InputStream rFileStream = rFtpClient.getFileStream(sFile);

			byte[] aFileData =
				StreamUtil.readAll(rFileStream,
								   rConnection.get(BUFFER_SIZE),
								   rConnection.get(MAX_RESPONSE_SIZE));

			rFileStream.close();

			return aFileData;
		}
	}

	/********************************************************************
	 * Lists the files in a certain directory. If the directory is prefixed with
	 * one of the FTP file types in the enum {@link FileType} (separated from
	 * the directory by a colon ':') only files of that type will be returned.
	 *
	 * @author eso
	 */
	public static class ListFiles extends FtpRequest<String, List<String>>
	{
		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sDefaultDir The default directory
		 */
		public ListFiles(String sDefaultDir)
		{
			super("ListFiles", sDefaultDir);
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * {@inheritDoc}
		 */
		@Override
		protected List<String> executeRequest(Connection rConnection,
											  FtpClient  rFtpClient,
											  String	 sDirectory)
			throws Exception
		{
			List<String>     aFiles		 = new ArrayList<>();
			String[]		 aTypeAndDir = sDirectory.split(":");
			FtpDirEntry.Type eFileType   = null;

			if (aTypeAndDir.length > 1)
			{
				eFileType  = FtpDirEntry.Type.valueOf(aTypeAndDir[0]);
				sDirectory = aTypeAndDir[1];
			}

			Iterator<FtpDirEntry> rEntries = rFtpClient.listFiles(sDirectory);

			while (rEntries.hasNext())
			{
				FtpDirEntry rEntry = rEntries.next();

				if (eFileType == null || rEntry.getType() == eFileType)
				{
					aFiles.add(rEntry.getName());
				}
			}

			return aFiles;
		}
	}
}
