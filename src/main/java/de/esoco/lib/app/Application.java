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
package de.esoco.lib.app;

import de.esoco.lib.logging.Log;
import de.esoco.lib.manage.Closeable;
import de.esoco.lib.manage.Disposable;
import de.esoco.lib.manage.RunCheck;
import de.esoco.lib.manage.Stoppable;
import de.esoco.lib.text.TextConvert;
import org.obrel.core.RelatedObject;

import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for applications. A subclass must at least implement the method
 * {@link #runApp()} and a main method that invokes the {@link #run(String[])}
 * method on an instance of the application. It may also override any of the
 * other runtime methods (and it should normally also invoke super in them). The
 * runtime methods are invoked in the following order:
 *
 * <ol>
 *   <li>{@link #processArguments(String[])}: evaluates the command line</li>
 *   <li>{@link #initialize(CommandLine)}: initialization of app parameters</li>
 *   <li>{@link #configure(CommandLine)}: configuration of the application</li>
 *   <li>{@link #startApp()}:</li>
 *   <li>{@link #runApp()}:</li>
 *   <li>{@link #stopApp()}: invokes the following two methods:
 *
 *     <ol>
 *       <li>{@link #cleanup()}: performs a cleanup of registered resources</li>
 *       <li>{@link #terminate()}: terminates the application</li>
 *     </ol>
 *   </li>
 * </ol>
 *
 * @author eso
 */
public abstract class Application extends RelatedObject {

	private static final int CLEANUP_TOTAL_WAIT_TIME = 30 * 1000;

	private static final int CLEANUP_RESOURCE_WAIT_TIME = 10 * 1000;

	private static final int CLEANUP_SLEEP_TIME = 100;

	private final AtomicBoolean shutdownRequest = new AtomicBoolean(false);

	private CommandLine commandLine;

	private String appName = null;

	private List<Object> cleanupResources = null;

	private Thread mainThread;

	/**
	 * Default constructor.
	 */
	public Application() {
	}

	/**
	 * Returns the command line of this application.
	 *
	 * @return The command line
	 */
	public final CommandLine getCommandLine() {
		return commandLine;
	}

	/**
	 * Registers a resource that implements the {@link Stoppable} interface to
	 * be managed by this application.
	 *
	 * @param resource The {@link Stoppable} resource
	 */
	public void manageResource(Stoppable resource) {
		addManagedResource(resource);
	}

	/**
	 * Registers a resource that implements the {@link Closeable} interface to
	 * be managed by this application.
	 *
	 * @param resource The {@link Closeable} resource
	 */
	public void manageResource(Closeable resource) {
		addManagedResource(resource);
	}

	/**
	 * Registers a resource that implements the {@link Disposable} interface to
	 * be managed by this application.
	 *
	 * @param resource The {@link Disposable} resource
	 */
	public void manageResource(Disposable resource) {
		addManagedResource(resource);
	}

	/**
	 * This method must be invoked on new instances. It will perform the
	 * default
	 * steps necessary to initialize and run the application.
	 *
	 * @param args The command line arguments of the application
	 */
	public final void run(String[] args) {
		if (appName == null) {
			try {
				commandLine = processArguments(args);

				if (commandLine.hasOption("h")) {
					printHelp(commandLine.getOption("h"));
				} else if (commandLine.hasOption("-help")) {
					printHelp(commandLine.getOption("-help"));
				} else {
					String appName = getClass().getSimpleName();

					Log.debugf("%s initializing...", appName);
					initialize(commandLine);
					Log.debugf("%s configuring...", appName);
					configure(commandLine);
					Log.debugf("%s starting...", appName);
					startApp();

					mainThread = Thread.currentThread();

					Thread shutdownHook = new Thread(this::requestShutdown);

					Runtime.getRuntime().addShutdownHook(shutdownHook);

					try {
						runApp();
					} finally {
						if (!shutdownRequest.get()) {
							Runtime
								.getRuntime()
								.removeShutdownHook(shutdownHook);
						}
					}

					stopApp();
				}
			} catch (CommandLineException e) {
				displayUsageError(e);
			} catch (Exception e) {
				handleApplicationError(e);
			}
		}
	}

	/**
	 * Allows to add a resource to be managed by this application. Such
	 * resources must implement at least one of the management interfaces
	 * {@link Stoppable}, {@link Closeable}, or {@link Disposable}. They may
	 * also implement the {@link RunCheck} interface in which case the cleanup
	 * code will wait for the shutdown of the resource after stopping it. This
	 * is checked by an assertion. <b>Attention:</b> Subclasses must invoke the
	 * super implementation of {@link #cleanup()} method to make this mechanism
	 * work.
	 *
	 * <p>The management interface will be processed in the order in which they
	 * are mentioned above. First all Stoppable objects will be stopped.
	 * Then it
	 * will be waited until all RunCheck instances have terminated (or until an
	 * application specific timeout has been reached). Finally, all closeable
	 * objects will be closed and then all disposable objects will be
	 * disposed.</p>
	 *
	 * <p>On cleanup the registered resources will be released in the reverse
	 * order of registration. That means that the first registered resource
	 * will
	 * be released first and vice versa. This allows to register resources that
	 * have (one-way) dependencies on each other.</p>
	 *
	 * @param resource The resource, implementing some management interfaces
	 */
	protected void addManagedResource(Object resource) {
		if (!(resource instanceof Stoppable || resource instanceof Closeable ||
			resource instanceof Disposable)) {
			throw new IllegalArgumentException(
				"No supported management interface implemented on " + resource);
		}

		if (cleanupResources == null) {
			cleanupResources = new ArrayList<Object>();
		}

		cleanupResources.add(resource);
	}

	/**
	 * Performs the cleanup of all resources that have been registered with
	 * {@link #addManagedResource(Object)}. This method will be invoked by
	 * {@link #stopApp()}.
	 *
	 * <p>Subclasses may override this method to perform additional cleanup of
	 * resources that have been allocated by the application if necessary. But
	 * they should always invoke super.cleanup() in their implementation. The
	 * order of this invocation depends on the subclass. It is recommended to
	 * first release all access to registered resources, then invoke
	 * super.cleanup() and finally free all subclass resources on which
	 * registered resources did depend.</p>
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void cleanup() throws Exception {
		// free resources in reverse order of registration
		if (cleanupResources != null) {
			int remainingWaitTime = CLEANUP_TOTAL_WAIT_TIME;

			for (int i = cleanupResources.size() - 1; i >= 0; i--) {
				Object resource = cleanupResources.get(i);

				if (resource instanceof Stoppable) {
					Log.debug("Stopping " + resource);
					((Stoppable) resource).stop();
				}

				if (resource instanceof Closeable) {
					Log.debug("Closing " + resource);
					((Closeable) resource).close();
				}

				if (resource instanceof Disposable) {
					Log.debug("Disposing " + resource);
					((Disposable) resource).dispose();
				}

				if (resource instanceof RunCheck) {
					int waitTime =
						remainingWaitTime > CLEANUP_RESOURCE_WAIT_TIME ?
						CLEANUP_RESOURCE_WAIT_TIME :
						remainingWaitTime;

					remainingWaitTime -=
						waitForResource((RunCheck) resource, waitTime,
							CLEANUP_SLEEP_TIME);
				}
			}
		}
	}

	/**
	 * May be overridden by subclasses to configure the application. Will be
	 * invoked directly after the method {@link #initialize(CommandLine)}.
	 * Subclasses should always invoke super as the first step in their
	 * implementation.
	 *
	 * @param commandLine The parsed command line of the application
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void configure(CommandLine commandLine) throws Exception {
	}

	/**
	 * Displays information about a usage error of this application. The
	 * default
	 * implementation displays an error message if a command line exception is
	 * provided and then invokes {@link #printUsage(PrintStream)}.
	 *
	 * @param e An optional command line exception that indicates a usage error
	 *          or NULL for none
	 */
	protected void displayUsageError(CommandLineException e) {
		if (e != null) {
			System.err.printf("Error: %s\n", e.getMessage());
		}

		printHelp(System.out);
	}

	/**
	 * Returns a description of this application. If present it will be
	 * displayed together with the usage string that is output by the method
	 * {@link #printUsage(PrintStream)}. The default implementation returns
	 * NULL.
	 *
	 * @return The application description
	 */
	protected String getAppDescription() {
		return null;
	}

	/**
	 * Can be overridden by subclasses to provide a mapping from allowed
	 * command
	 * line options to corresponding help text string. The default
	 * implementation returns an empty map to indicate that arbitrary options
	 * are allowed. See the documentation of class {@link CommandLine} for more
	 * information.
	 *
	 * @return A mapping from allowed command line options to help texts
	 */
	protected Map<String, String> getCommandLineOptions() {
		return Collections.emptyMap();
	}

	/**
	 * Returns the name of the distribution binary of this application
	 * (typically a JAR file).
	 *
	 * @return The name of the application binary if available
	 */
	protected String getNameOfAppBinary() {
		try {
			String appPath = getClass()
				.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.toURI()
				.getPath();

			appName = appPath.substring(appPath.lastIndexOf('/') + 1);

			int index = appName.indexOf('.');

			if (index > 0) {
				appName = appName.substring(0, index);
			}

			if (appName.length() == 0) {
				appName = getClass().getSimpleName();
			}

			return appName;
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * This method can be overridden by applications to handle errors that have
	 * been signaled by exceptions from one of the runtime methods of this
	 * instance. The default implementation only logs the error.
	 *
	 * @param e The exception that signaled the error
	 */
	protected void handleApplicationError(Exception e) {
		Log.errorf(e, "Error executing %s", getNameOfAppBinary());
	}

	/**
	 * May be overridden by subclasses to initialize the application. This is
	 * the first method that will be invoked by the {@link #run(String[])}
	 * method. Subclasses should always invoke super as the first step in their
	 * implementation.
	 *
	 * @param commandLine The parsed command line of the application
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void initialize(CommandLine commandLine) throws Exception {
	}

	/**
	 * Checks whether a shutdown of the application has been requested.
	 * Applications should check this flag in their processing loop and cleanup
	 * and terminate as soon as possible if this method returns TRUE.
	 *
	 * @return TRUE if a shutdown has been requested
	 */
	protected boolean isShutdownRequested() {
		return shutdownRequest.get();
	}

	/**
	 * Prints help for this application or for a single command.
	 *
	 * @param command The optional command
	 */
	protected void printHelp(Object command) {
		Map<String, String> options =
			new LinkedHashMap<>(getCommandLineOptions());

		String helpInfo = "Display this help or help for a single option";

		options.put("h", helpInfo);
		options.put("-help", helpInfo);
		options.put("-args",
			"The name and path of a properties file to read the arguments " +
				"from");

		if (command instanceof String) {
			String cmd = command.toString();

			System.out.printf("Option -%s: %s\n", command, options.get(cmd));
		} else {
			int maxCommandLength = 0;

			printUsage(System.out);

			for (String cmd : options.keySet()) {
				maxCommandLength = Math.max(cmd.length(), maxCommandLength);
			}

			for (Entry<String, String> commandHelp : options.entrySet()) {
				String cmd = TextConvert.padRight(commandHelp.getKey(),
					maxCommandLength + 2, ' ');

				System.out.printf("\t-%s%s\n", cmd, commandHelp.getValue());
			}
		}
	}

	/**
	 * Prints information about how to use this application. Should be
	 * overridden by subclasses that need complex arguments to work.
	 *
	 * @param output The output stream
	 */
	protected void printUsage(PrintStream output) {
		String description = getAppDescription();

		output.printf("Usage: %s [options...]\n", getNameOfAppBinary());

		if (description != null) {
			output.println(description);
		}
	}

	/**
	 * Processes the application's command line arguments and returns them in a
	 * command line object.
	 *
	 * @param args The application arguments
	 * @return A command line instance containing the parsed arguments
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected CommandLine processArguments(String[] args) throws Exception {
		return new CommandLine(args, getCommandLineOptions());
	}

	/**
	 * Removes a managed resource that has previously been added by invoking
	 * {@link #addManagedResource(Object)} without performing the regular
	 * resource cleanup that is done by the {@link #cleanup()} method.
	 *
	 * @param resource old The resource object to be remove
	 */
	protected void removeManagedResource(Object resource) {
		if (cleanupResources != null) {
			cleanupResources.remove(resource);
		}
	}

	/**
	 * Will be invoked from a separate thread if a shutdown of the application
	 * has been requested.
	 */
	protected void requestShutdown() {
		System.out.print("\nShutdown request received, terminating...\n");
		shutdownRequest.set(true);

		// wake up main thread if it is currently inactive
		mainThread.interrupt();

		try {
			// wait for main thread to stop (else app would terminate
			// immediately)
			mainThread.join();
		} catch (InterruptedException e) {
			// just terminate
		}
	}

	/**
	 * This is the main method that must be implemented by all subclasses. It
	 * contains the code the will perform the applications actual work task(s).
	 * The implementation may throw exceptions to signal errors but it must be
	 * aware that this will skip any final application method invocations. If
	 * final cleanup tasks need to be run anyway the implementation should
	 * do so
	 * in the method {@link #handleApplicationError(Exception)}, e.g. by
	 * invoking the {@link #cleanup()} method.
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected abstract void runApp() throws Exception;

	/**
	 * May be overridden by subclasses to perform the tasks necessary to start
	 * the application. This can be used to run code that must be run before or
	 * parallel with the actual application code (e.g. additional threads).
	 * Will
	 * be invoked after the method {@link #configure(CommandLine)} and just
	 * before the method {@link #runApp()}. Subclasses should always invoke
	 * super as the first step in their implementation.
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void startApp() throws Exception {
	}

	/**
	 * May be overridden by subclasses to perform the tasks necessary to
	 * prepare
	 * the termination of the application. An example would be to stop threads
	 * that have been run in parallel with the main application code. Will be
	 * invoked after the method {@link #runApp()} has finished.
	 *
	 * <p>The default implementation invokes the methods {@link #cleanup()} and
	 * {@link #terminate()} in that order. Therefore subclasses should either
	 * invoke the super method to perform the final application cleanup or
	 * implement the corresponding handling by themselves.</p>
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void stopApp() throws Exception {
		String appName = getClass().getSimpleName();

		Log.debugf("%s cleanup...", appName);
		cleanup();
		Log.debugf("%s terminating...", appName);
		terminate();
		Log.debugf("%s stopped", appName);
	}

	/**
	 * May be overridden by subclasses to execute the final termination of the
	 * application if necessary (e.g. invoke System.exit() with a specific
	 * error
	 * code). Will be invoked as the last method in {@link #stopApp()}. The
	 * default implementation does not exit the application. Subclasses should
	 * invoke super.terminate() as the last call in their implementation to
	 * keep
	 * base class functionality like logging but this is not mandatory.
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void terminate() throws Exception {
	}

	/**
	 * Cleanup helper method that lets the current thread wait for a certain
	 * resource to stop execution. The resource must implement the RunCheck
	 * interface to be processed by this method.
	 *
	 * @param resource    The resource to wait for
	 * @param maxWaitTime The maximum time to wait for this resource
	 * @param sleepTime   The time to wait between checks of the resource
	 * @return The time the method has actually waited for the given resource
	 */
	protected int waitForResource(RunCheck resource, int maxWaitTime,
		int sleepTime) {
		int waitTime = 0;

		Log.debug("Waiting for " + resource);

		while (waitTime < maxWaitTime && resource.isRunning()) {
			try {
				Thread.sleep(sleepTime);
				waitTime += sleepTime;
			} catch (InterruptedException e) {
				// if interrupted (should not occur) just try again
			}
		}

		return waitTime;
	}
}
