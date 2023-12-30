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
package de.esoco.lib.service;

import de.esoco.lib.app.Application;
import de.esoco.lib.app.CommandLine;
import de.esoco.lib.app.Service;
import de.esoco.lib.collection.CollectionUtil;
import de.esoco.lib.comm.Endpoint;
import de.esoco.lib.comm.EndpointFunction;
import de.esoco.lib.expression.monad.Option;
import de.esoco.lib.json.Json;
import de.esoco.lib.json.JsonObject;
import de.esoco.lib.logging.Log;
import de.esoco.lib.logging.LogExtent;
import de.esoco.lib.logging.LogLevel;
import de.esoco.lib.security.Security;
import de.esoco.lib.service.ModificationSyncEndpoint.SyncData;

import java.util.LinkedHashMap;
import java.util.Map;

import static de.esoco.lib.comm.CommunicationRelationTypes.ENDPOINT_ADDRESS;
import static de.esoco.lib.service.ModificationSyncEndpoint.getCurrentLocks;
import static de.esoco.lib.service.ModificationSyncEndpoint.releaseLock;
import static de.esoco.lib.service.ModificationSyncEndpoint.requestLock;

/**
 * Tests the functionality of {@link ModificationSyncEndpoint}.
 *
 * @author eso
 */
public class ModificationSyncServiceTool extends Application {

	/**
	 * Enumeration of available sync service commands.
	 */
	enum Command {
		LIST("Lists either all locks or only in a given context"),
		LOCK("Locks a target in a context"),
		UNLOCK("Removes a target lock in a context"),
		RESET("Resets either all locks or only in the given context"),
		LOGLEVEL("Queries or updates the log level of the target service");

		private final String helpText;

		/**
		 * Creates a new instance.
		 *
		 * @param help The help text for the command
		 */
		Command(String help) {
			helpText = help;
		}

		/**
		 * Returns the command's help text.
		 *
		 * @return The help text
		 */
		public final String getHelpText() {
			return helpText;
		}
	}

	private Endpoint syncService;

	private EndpointFunction<SyncData, String> releaseLock;

	private EndpointFunction<SyncData, String> requestLock;

	private Map<String, String> commandLineOptions = null;

	/**
	 * Main method.
	 *
	 * @param args The arguments
	 */
	public static void main(String[] args) {
		try {
			new ModificationSyncServiceTool().run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getAppDescription() {
		return "Sends a command to a ModificationSyncService running at an " +
			"URL that must be set with -url. Use -h or --help for help.";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Map<String, String> getCommandLineOptions() {
		if (commandLineOptions == null) {
			commandLineOptions = new LinkedHashMap<>();

			String helpInfo =
				"Display this help or informations about a certain command";

			commandLineOptions.put("h", helpInfo);
			commandLineOptions.put("-help", helpInfo);
			commandLineOptions.put("url",
				"The URL of the sync service (mandatory)");
			commandLineOptions.put("context",
				"The context to which to apply a command");
			commandLineOptions.put("target",
				"The target to which to apply a command (in a certain " +
					"context)");

			for (Command command : Command.values()) {
				commandLineOptions.put(command.name().toLowerCase(),
					command.getHelpText());
			}
		}

		return commandLineOptions;
	}

	/**
	 * Handles all commands provided on the command line.
	 *
	 * @param commandLine The command line
	 * @param context     The optional command context
	 * @param target      The optional command target
	 */
	protected void handleCommands(CommandLine commandLine, String context,
		String target) {
		for (Command command : Command.values()) {
			String cmd = command.name().toLowerCase();

			if (commandLine.hasOption(cmd)) {
				System.out.printf("Applying %s to %s\n", command,
					syncService.get(ENDPOINT_ADDRESS));

				switch (command) {
					case LIST:
					case RESET:
						handleListAndReset(command, context);
						break;

					case LOGLEVEL:
						handleGetAndSetLogLevel(commandLine.getString(cmd));
						break;

					case LOCK:
					case UNLOCK:
						handleLockAndUnlock(command, context, target);
						break;

					default:
						assert false : "Unhandled command " + command;
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runApp() throws Exception {
		CommandLine commandLine = getCommandLine();

		syncService = Endpoint.at(commandLine.requireString("url"));
		requestLock = requestLock().from(syncService);
		releaseLock = releaseLock().from(syncService);

		// handle errors on application level
		syncService.set(Log.LOG_EXTENT, LogExtent.NOTHING);

		String context = commandLine.getString("context");
		String target = commandLine.getString("target");

		handleCommands(commandLine, context, target);
	}

	/**
	 * Returns the client ID to be used for identification to the sync service.
	 *
	 * @return The client ID string
	 */
	private String getClientId() {
		return getClass().getSimpleName() +
			Security.generateSha256Id().substring(0, 8);
	}

	/**
	 * Queries the locks from the sync service and returns the result as parsed
	 * JSON data.
	 *
	 * @return A mapping from lock contexts to mappings from target IDs to lock
	 * holders
	 */
	private JsonObject getLocks() {
		return Json.parseObject(getCurrentLocks().from(syncService).receive());
	}

	/**
	 * Handles the querying and setting of the sync service's log level.
	 *
	 * @param newLevel The optional new log level for setting
	 */
	private void handleGetAndSetLogLevel(String newLevel) {
		if (newLevel != null && LogLevel.valueOf(newLevel) != null) {
			Service.SET_LOG_LEVEL.on(syncService).send(Json.toJson(newLevel));
		} else {
			System.out.printf("Current log level: %s\n",
				Service.GET_LOG_LEVEL.from(syncService).receive());
		}
	}

	/**
	 * Handles {@link Command#LIST} and {@link Command#RESET}.
	 *
	 * @param command The command to handle
	 * @param context The context to apply the command to or none for all
	 */
	private void handleListAndReset(Command command, String context) {
		JsonObject locks = getLocks();

		if (locks.isEmpty()) {
			System.out.print("No lock contexts defined\n");
		} else if (context != null) {
			if (command == Command.RESET) {
				unlockAll(context, locks.getObject(context));
			} else {
				printLocks(context, locks.getObject(context));
			}
		} else {
			if (command == Command.RESET) {
				for (String ctx : locks.getProperties().keySet()) {
					unlockAll(ctx, locks.getObject(ctx));
				}
			} else {
				for (String ctx : locks.getPropertyNames()) {
					printLocks(ctx, locks.getObject(ctx));
				}
			}
		}
	}

	/**
	 * Handles {@link Command#LOCK} and {@link Command#UNLOCK}.
	 *
	 * @param command The command to handle
	 * @param context The context to apply the command to
	 * @param target  The target to apply the command to
	 */
	private void handleLockAndUnlock(Command command, String context,
		String target) {
		if (context == null) {
			System.out.print(
				"Sync context must be provided (-context <context>)\n");
		} else if (target == null) {
			System.out.print(
				"Sync target must be provided (-target <target>)\n");
		} else {
			SyncData syncData =
				new SyncData(getClientId(), context, target, true);

			if (command == Command.LOCK) {
				requestLock.send(syncData);
			} else {
				releaseLock.send(syncData);
			}
		}
	}

	/**
	 * Prints out the locks of a certain context.
	 */
	private void printLocks(String context, Option<JsonObject> contextLocks) {
		contextLocks.ifExists(locks -> {
			if (locks.isEmpty()) {
				System.out.printf("No locks in existing context %s\n",
					context);
			} else {
				System.out.printf("Locks for context %s:\n  %s\n", context,
					CollectionUtil.toString(locks.getProperties(), ": ",
						"\n  "));
			}
		});
	}

	/**
	 * Unlocks all targets in a certain modification context.
	 *
	 * @param context      The modification context
	 * @param contextLocks The mapping from target IDs to lock holders
	 */
	private void unlockAll(String context, Option<JsonObject> contextLocks) {
		String clientId = getClientId();

		contextLocks.ifExists(locks -> {
			for (String target : locks.getProperties().keySet()) {
				releaseLock.send(new SyncData(clientId, context, target,
					true));

				System.out.printf(
					"Removed lock on %s from context %s (acquired by %s)\n",
					target, context, locks.getString(target).orUse("unknown"));
			}
		});
	}
}
