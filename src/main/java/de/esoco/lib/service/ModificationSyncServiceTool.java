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
package de.esoco.lib.service;

import de.esoco.lib.app.Application;
import de.esoco.lib.app.CommandLine;
import de.esoco.lib.app.Service;
import de.esoco.lib.collection.CollectionUtil;
import de.esoco.lib.comm.Endpoint;
import de.esoco.lib.comm.EndpointFunction;
import de.esoco.lib.expression.Function;
import de.esoco.lib.json.JsonBuilder;
import de.esoco.lib.json.JsonParser;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogExtent;
import de.esoco.lib.logging.LogLevel;
import de.esoco.lib.security.Security;
import de.esoco.lib.service.ModificationSyncEndpoint.SyncData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_ADDRESS;
import static de.esoco.lib.service.ModificationSyncEndpoint.getCurrentLocks;
import static de.esoco.lib.service.ModificationSyncEndpoint.releaseLock;
import static de.esoco.lib.service.ModificationSyncEndpoint.requestLock;


/********************************************************************
 * Tests the functionality of {@link ModificationSyncEndpoint}.
 *
 * @author eso
 */
public class ModificationSyncServiceTool extends Application
{
	//~ Enums ------------------------------------------------------------------

	/********************************************************************
	 * Enumeration of available sync service commands.
	 */
	enum Command
	{
		LIST("Lists either all locks or only in a given context"),
		LOCK("Locks a target in a context"),
		UNLOCK("Removes a target lock in a context"),
		RESET("Resets either all locks or only in the given context"),
		LOGLEVEL("Queries or updates the log level of the target service");

		//~ Instance fields ----------------------------------------------------

		private final String sHelpText;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param sHelp The help text for the command
		 */
		private Command(String sHelp)
		{
			sHelpText = sHelp;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Returns the command's help text.
		 *
		 * @return The help text
		 */
		public final String getHelpText()
		{
			return sHelpText;
		}
	}

	//~ Instance fields --------------------------------------------------------

	private Endpoint					    aSyncService;
	private EndpointFunction<SyncData, String> fReleaseLock;
	private EndpointFunction<SyncData, String> fRequestLock;

	private Map<String, String> aCommandLineOptions = null;

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Main method.
	 *
	 * @param rArgs The arguments
	 */
	public static void main(String[] rArgs)
	{
		new ModificationSyncServiceTool().run(rArgs);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected String getAppDescription()
	{
		return "Sends a command to a ModificationSyncService running at an URL that must be set with -url";
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected Map<String, String> getCommandLineOptions()
	{
		if (aCommandLineOptions == null)
		{
			aCommandLineOptions = new LinkedHashMap<>();

			String sHelpInfo =
				"Display this help or informations about a certain command";

			aCommandLineOptions.put("h", sHelpInfo);
			aCommandLineOptions.put("-help", sHelpInfo);
			aCommandLineOptions.put("url",
									"The URL of the sync service (mandatory)");
			aCommandLineOptions.put("context",
									"The context to which to apply a command");
			aCommandLineOptions.put("target",
									"The target to which to apply a command (in a certain context)");

			for (Command eCommand : Command.values())
			{
				aCommandLineOptions.put(eCommand.name().toLowerCase(),
										eCommand.getHelpText());
			}
		}

		return aCommandLineOptions;
	}

	/***************************************
	 * Handles all commands provided on the command line.
	 *
	 * @param rCommandLine The command line
	 * @param rContext     The optional command context
	 * @param rTarget      The optional command target
	 */
	protected void handleCommands(CommandLine	   rCommandLine,
								  Optional<String> rContext,
								  Optional<String> rTarget)
	{
		for (Command eCommand : Command.values())
		{
			String sCommand = eCommand.name().toLowerCase();

			if (rCommandLine.hasOption(sCommand))
			{
				System.out.printf("%s result for sync service %s:\n",
								  eCommand,
								  aSyncService.get(ENDPOINT_ADDRESS));

				switch (eCommand)
				{
					case LIST:
					case RESET:
						handleListAndReset(eCommand, rContext);
						break;

					case LOGLEVEL:
						handleGetAndSetLogLevel(rCommandLine.getString(sCommand));
						break;

					case LOCK:
					case UNLOCK:
						handleLockAndUnlock(eCommand, rContext, rTarget);
						break;

					default:
						assert false : "Unhandled command " + eCommand;
				}
			}
		}
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	protected void runApp() throws Exception
	{
		CommandLine rCommandLine = getCommandLine();

		aSyncService = Endpoint.at(rCommandLine.requireString("url"));
		fRequestLock = requestLock().from(aSyncService);
		fReleaseLock = releaseLock().from(aSyncService);

		// handle errors on application level
		aSyncService.set(Log.LOG_EXTENT, LogExtent.NOTHING);

		Optional<String> aContext = rCommandLine.getString("context");
		Optional<String> aTarget  = rCommandLine.getString("target");

		handleCommands(rCommandLine, aContext, aTarget);
	}

	/***************************************
	 * Returns the client ID to be used for identification to the sync service.
	 *
	 * @return The client ID string
	 */
	private String getClientId()
	{
		return getClass().getSimpleName() +
			   Security.generateSha256Id().substring(0, 8);
	}

	/***************************************
	 * Queries the locks from the sync service and returns the result as parsed
	 * JSON data.
	 *
	 * @return A mapping from lock contexts to mappings from target IDs to lock
	 *         holders
	 */
	private Map<String, Map<String, String>> getLocks()
	{
		Function<SyncData, String> fGetLocks =
			getCurrentLocks().from(aSyncService);

		@SuppressWarnings("unchecked")
		Map<String, Map<String, String>> aLocks =
			(Map<String, Map<String, String>>)
			new JsonParser().parse(fGetLocks.result());

		return aLocks;
	}

	/***************************************
	 * Handles the querying and setting of the sync service's log level.
	 *
	 * @param rNewLevel The optional new log level for setting
	 */
	private void handleGetAndSetLogLevel(Optional<String> rNewLevel)
	{
		if (rNewLevel.isPresent() && LogLevel.valueOf(rNewLevel.get()) != null)
		{
			Service.SET_LOG_LEVEL.at(aSyncService)
								 .send(JsonBuilder.toJson(rNewLevel.get()));
		}
		else
		{
			System.out.printf("Current log level: %s\n",
							  Service.GET_LOG_LEVEL.from(aSyncService)
							  .result());
		}
	}

	/***************************************
	 * Handles {@link Command#LIST} and {@link Command#RESET}.
	 *
	 * @param eCommand The command to handle
	 * @param rContext The context to apply the command to or none for all
	 */
	private void handleListAndReset(Command			 eCommand,
									Optional<String> rContext)
	{
		Map<String, Map<String, String>> aLocks = getLocks();

		if (aLocks.isEmpty())
		{
			System.out.printf("No lock contexts defined\n");
		}
		else if (rContext.isPresent())
		{
			String			    sContext	  = rContext.get();
			Map<String, String> rContextLocks = aLocks.get(sContext);

			if (eCommand == Command.RESET)
			{
				unlockAll(sContext, rContextLocks);
			}
			else
			{
				printLocks(sContext, rContextLocks);
			}
		}
		else
		{
			if (eCommand == Command.RESET)
			{
				for (String sContext : aLocks.keySet())
				{
					unlockAll(sContext, aLocks.get(sContext));
				}
			}
			else
			{
				for (String sContext : aLocks.keySet())
				{
					printLocks(sContext, aLocks.get(sContext));
				}
			}
		}
	}

	/***************************************
	 * Handles {@link Command#LOCK} and {@link Command#UNLOCK}.
	 *
	 * @param eCommand The command to handle
	 * @param rContext The context to apply the command to
	 * @param rTarget  The target to apply the command to
	 */
	private void handleLockAndUnlock(Command		  eCommand,
									 Optional<String> rContext,
									 Optional<String> rTarget)
	{
		if (!rContext.isPresent())
		{
			System.out.printf("Sync context must be provided (-context <context>)\n");
		}
		else if (!rTarget.isPresent())
		{
			System.out.printf("Sync target must be provided (-target <target>)\n");
		}
		else
		{
			SyncData aSyncData =
				new SyncData(getClientId(),
							 rContext.get(),
							 rTarget.get(),
							 true);

			if (eCommand == Command.LOCK)
			{
				fRequestLock.send(aSyncData);
			}
			else
			{
				fReleaseLock.send(aSyncData);
			}
		}
	}

	/***************************************
	 * Prints out the locks of a certain context.
	 *
	 * @param sContext
	 * @param rContextLocks
	 */
	private void printLocks(String sContext, Map<String, String> rContextLocks)
	{
		if (rContextLocks.isEmpty())
		{
			System.out.printf("No locks in existing context %s\n", sContext);
		}
		else
		{
			System.out.printf("Locks for context %s:\n  %s\n",
							  sContext,
							  CollectionUtil.toString(rContextLocks,
													  ": ",
													  "\n  "));
		}
	}

	/***************************************
	 * Unlocks all targets in a certain modification context.
	 *
	 * @param sContext The modification context
	 * @param aLocks   The mapping from target IDs to lock holders
	 */
	private void unlockAll(String sContext, Map<String, String> aLocks)
	{
		String sClientId = getClientId();

		for (String sTarget : aLocks.keySet())
		{
			fReleaseLock.send(new SyncData(sClientId, sContext, sTarget, true));

			System.out.printf("Removed lock on %s from context %s (acquired by %s)\n",
							  sTarget,
							  sContext,
							  aLocks.get(sTarget));
		}
	}
}
