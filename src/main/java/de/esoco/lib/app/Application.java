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
package de.esoco.lib.app;

import de.esoco.lib.logging.Log;
import de.esoco.lib.manage.Closeable;
import de.esoco.lib.manage.Disposable;
import de.esoco.lib.manage.RunCheck;
import de.esoco.lib.manage.Stoppable;
import de.esoco.lib.thread.ThreadManager;

import java.io.PrintStream;

import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.obrel.core.RelatedObject;


/********************************************************************
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
public abstract class Application extends RelatedObject
{
	//~ Static fields/initializers ---------------------------------------------

	private static final int CLEANUP_TOTAL_WAIT_TIME    = 30 * 1000;
	private static final int CLEANUP_RESOURCE_WAIT_TIME = 10 * 1000;
	private static final int CLEANUP_SLEEP_TIME		    = 100;

	//~ Instance fields --------------------------------------------------------

	private CommandLine aCommandLine;

	private String		 sAppName		   = null;
	private List<Object> aCleanupResources = null;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Default constructor.
	 */
	public Application()
	{
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns the command line of this application.
	 *
	 * @return The command line
	 */
	public final CommandLine getCommandLine()
	{
		return aCommandLine;
	}

	/***************************************
	 * Registers a resource that implements the {@link Stoppable} interface to
	 * be managed by this application.
	 *
	 * @param rResource The {@link Stoppable} resource
	 */
	public void manageResource(Stoppable rResource)
	{
		addManagedResource(rResource);
	}

	/***************************************
	 * Registers a resource that implements the {@link Closeable} interface to
	 * be managed by this application.
	 *
	 * @param rResource The {@link Closeable} resource
	 */
	public void manageResource(Closeable rResource)
	{
		addManagedResource(rResource);
	}

	/***************************************
	 * Registers a resource that implements the {@link Disposable} interface to
	 * be managed by this application.
	 *
	 * @param rResource The {@link Disposable} resource
	 */
	public void manageResource(Disposable rResource)
	{
		addManagedResource(rResource);
	}

	/***************************************
	 * This method must be invoked on new instances. It will perform the default
	 * steps necessary to initialize and run the application.
	 *
	 * @param rArgs The command line arguments of the application
	 */
	public final void run(String[] rArgs)
	{
		if (sAppName == null)
		{
			try
			{
				aCommandLine = processArguments(rArgs);

				String sAppName = getClass().getSimpleName();

				Log.debugf("%s initializing...", sAppName);
				initialize(aCommandLine);
				Log.debugf("%s configuring...", sAppName);
				configure(aCommandLine);
				Log.debugf("%s starting...", sAppName);
				startApp();
				runApp();
				stopApp();
			}
			catch (CommandLineException e)
			{
				displayUsageError(e);
			}
			catch (Exception e)
			{
				handleApplicationError(e);
			}
		}
	}

	/***************************************
	 * This is the main method that must be implemented by all subclasses. It
	 * contains the code the will perform the applications actual work task(s).
	 * The implementation may throw exceptions to signal errors but it must be
	 * aware that this will skip any final application method invocations. If
	 * final cleanup tasks need to be run anyway the implementation should do so
	 * in the method {@link #handleApplicationError(Exception)}, e.g. by
	 * invoking the {@link #cleanup()} method.
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected abstract void runApp() throws Exception;

	/***************************************
	 * Allows to add a resource to be managed by this application. Such
	 * resources must implement at least one of the management interfaces {@link
	 * Stoppable}, {@link Closeable}, or {@link Disposable}. They may also
	 * implement the {@link RunCheck} interface in which case the cleanup code
	 * will wait for the shutdown of the resource after stopping it. This is
	 * checked by an assertion. <b>Attention:</b> Subclasses must invoke the
	 * super implementation of {@link #cleanup()} method to make this mechanism
	 * work.
	 *
	 * <p>The management interface will be processed in the order in which they
	 * are mentioned above. First all Stoppable objects will be stopped. Then it
	 * will be waited until all RunCheck instances have terminated (or until an
	 * application specific timeout has been reached). Finally, all closeable
	 * objects will be closed and then all disposable objects will be
	 * disposed.</p>
	 *
	 * <p>On cleanup the registered resources will be released in the reverse
	 * order of registration. That means that the first registered resource will
	 * be released first and vice versa. This allows to register resources that
	 * have (one-way) dependencies on each other.</p>
	 *
	 * @param rResource The resource, implementing some management interfaces
	 */
	protected void addManagedResource(Object rResource)
	{
		if (!(rResource instanceof Stoppable ||
			  rResource instanceof Closeable ||
			  rResource instanceof Disposable))
		{
			throw new IllegalArgumentException("No supported management interface implemented on " +
											   rResource);
		}

		if (aCleanupResources == null)
		{
			aCleanupResources = new ArrayList<Object>();
		}

		aCleanupResources.add(rResource);
	}

	/***************************************
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
	protected void cleanup() throws Exception
	{
		// free resources in reverse order of registration
		if (aCleanupResources != null)
		{
			int nRemainingWaitTime = CLEANUP_TOTAL_WAIT_TIME;

			for (int i = aCleanupResources.size() - 1; i >= 0; i--)
			{
				Object rResource = aCleanupResources.get(i);

				if (rResource instanceof Stoppable)
				{
					Log.debug("Stopping " + rResource);
					((Stoppable) rResource).stop();
				}

				if (rResource instanceof Closeable)
				{
					Log.debug("Closing " + rResource);
					((Closeable) rResource).close();
				}

				if (rResource instanceof Disposable)
				{
					Log.debug("Disposing " + rResource);
					((Disposable) rResource).dispose();
				}

				if (rResource instanceof RunCheck)
				{
					int nWaitTime =
						nRemainingWaitTime > CLEANUP_RESOURCE_WAIT_TIME
						? CLEANUP_RESOURCE_WAIT_TIME : nRemainingWaitTime;

					nRemainingWaitTime -=
						waitForResource((RunCheck) rResource,
										nWaitTime,
										CLEANUP_SLEEP_TIME);
				}
			}
		}

		ThreadManager.shutdownAll(60, true);
	}

	/***************************************
	 * May be overridden by subclasses to configure the application. Will be
	 * invoked directly after the method {@link #initialize(CommandLine)}.
	 * Subclasses should always invoke super as the first step in their
	 * implementation.
	 *
	 * @param  rCommandLine The parsed command line of the application
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void configure(CommandLine rCommandLine) throws Exception
	{
	}

	/***************************************
	 * Displays information about a usage error of this application. The default
	 * implementation displays an error message if a command line exception is
	 * provided and then invokes {@link #printUsage(PrintStream)}.
	 *
	 * @param e An optional command line exception that indicates a usage error
	 *          or NULL for none
	 */
	protected void displayUsageError(CommandLineException e)
	{
		if (e != null)
		{
			System.out.printf("Error: %s\n", e.getMessage());
		}

		printUsage(System.out);
	}

	/***************************************
	 * Can be overridden by subclasses to provide a mapping from allowed command
	 * line options to corresponding help text string. The default
	 * implementation returns an empty map to indicate that arbitrary options
	 * are allowed. See the documentation of class {@link CommandLine} for more
	 * information.
	 *
	 * @return A mapping from allowed command line options to help texts
	 */
	protected Map<String, String> getCommandLineOptions()
	{
		return Collections.emptyMap();
	}

	/***************************************
	 * Returns the name of the distribution binary of this application
	 * (typically a JAR file).
	 *
	 * @return The name of the application binary if available
	 */
	protected String getNameOfAppBinary()
	{
		try
		{
			String sAppPath =
				getClass().getProtectionDomain()
						  .getCodeSource()
						  .getLocation()
						  .toURI()
						  .getPath();

			sAppName = sAppPath.substring(sAppPath.lastIndexOf('/') + 1);

			int nIndex = sAppName.indexOf('.');

			if (nIndex > 0)
			{
				sAppName = sAppName.substring(0, nIndex);
			}

			if (sAppName.length() == 0)
			{
				sAppName = getClass().getSimpleName();
			}

			return sAppName;
		}
		catch (URISyntaxException e)
		{
			throw new IllegalStateException(e);
		}
	}

	/***************************************
	 * This method can be overridden by applications to handle errors that have
	 * been signaled by exceptions from one of the runtime methods of this
	 * instance. The default implementation only logs the error.
	 *
	 * @param e The exception that signaled the error
	 */
	protected void handleApplicationError(Exception e)
	{
		System.err.printf("Error executing %s: %s",
						  getNameOfAppBinary(),
						  e.getMessage());

		if (getCommandLine().hasOption("verbose"))
		{
			e.printStackTrace();
		}
	}

	/***************************************
	 * May be overridden by subclasses to initialize the application. This is
	 * the first method that will be invoked by the {@link #run(String[])}
	 * method. Subclasses should always invoke super as the first step in their
	 * implementation.
	 *
	 * @param  rCommandLine The parsed command line of the application
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void initialize(CommandLine rCommandLine) throws Exception
	{
	}

	/***************************************
	 * Prints information about how to use this application. Should be
	 * overridden by subclasses that need complex arguments to work.
	 *
	 * @param rOutput The output stream
	 */
	protected void printUsage(PrintStream rOutput)
	{
		rOutput.printf("Usage: %s <arguments>\n", getNameOfAppBinary());
	}

	/***************************************
	 * Processes the application's command line arguments and returns them in a
	 * command line object.
	 *
	 * @param  rArgs The application arguments
	 *
	 * @return A command line instance containing the parsed arguments
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected CommandLine processArguments(String[] rArgs) throws Exception
	{
		return new CommandLine(rArgs, getCommandLineOptions());
	}

	/***************************************
	 * Removes a managed resource that has previously been added by invoking
	 * {@link #addManagedResource(Object)} without performing the regular
	 * resource cleanup that is done by the {@link #cleanup()} method.
	 *
	 * @param rResource rOld The resource object to be remove
	 */
	protected void removeManagedResource(Object rResource)
	{
		if (aCleanupResources != null)
		{
			aCleanupResources.remove(rResource);
		}
	}

	/***************************************
	 * May be overridden by subclasses to perform the tasks necessary to start
	 * the application. This can be used to run code that must be run before or
	 * parallel with the actual application code (e.g. additional threads). Will
	 * be invoked after the method {@link #configure(CommandLine)} and just
	 * before the method {@link #runApp()}. Subclasses should always invoke
	 * super as the first step in their implementation.
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void startApp() throws Exception
	{
	}

	/***************************************
	 * May be overridden by subclasses to perform the tasks necessary to prepare
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
	protected void stopApp() throws Exception
	{
		String sAppName = getClass().getSimpleName();

		Log.debugf("%s cleanup...", sAppName);
		cleanup();
		Log.debugf("%s terminating...", sAppName);
		terminate();
		Log.debugf("%s stopped", sAppName);
	}

	/***************************************
	 * May be overridden by subclasses to execute the final termination of the
	 * application if necessary (e.g. invoke System.exit() with a specific error
	 * code). Will be invoked as the last method in {@link #stopApp()}. The
	 * default implementation does not exit the application. Subclasses should
	 * invoke super.terminate() as the last call in their implementation to keep
	 * base class functionality like logging but this is not mandatory.
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected void terminate() throws Exception
	{
	}

	/***************************************
	 * Cleanup helper method that lets the current thread wait for a certain
	 * resource to stop execution. The resource must implement the RunCheck
	 * interface to be processed by this method.
	 *
	 * @param  rResource    The resource to wait for
	 * @param  nMaxWaitTime The maximum time to wait for this resource
	 * @param  nSleepTime   The time to wait between checks of the resource
	 *
	 * @return The time the method has actually waited for the given resource
	 */
	protected int waitForResource(RunCheck rResource,
								  int	   nMaxWaitTime,
								  int	   nSleepTime)
	{
		int nWaitTime = 0;

		Log.debug("Waiting for " + rResource);

		while (nWaitTime < nMaxWaitTime && rResource.isRunning())
		{
			try
			{
				Thread.sleep(nSleepTime);
				nWaitTime += nSleepTime;
			}
			catch (InterruptedException e)
			{
				// if interrupted (should not occur) just try again
			}
		}

		return nWaitTime;
	}
}
