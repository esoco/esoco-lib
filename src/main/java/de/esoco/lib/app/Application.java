//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2015 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import java.util.ArrayList;
import java.util.List;

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
	 * Allows to register a resource for cleanup. Such resources must implement
	 * at least one of the management interfaces {@link Stoppable}, {@link
	 * RunCheck}, {@link Closeable}, or {@link Disposable}. This is checked by
	 * an assertion. <b>Attention:</b> Subclasses must invoke the
	 * super.cleanup() method to make this mechanism work.
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
	public void registerCleanupResource(Object rResource)
	{
		assert rResource instanceof Stoppable ||
			   rResource instanceof RunCheck ||
			   rResource instanceof Closeable ||
			   rResource instanceof Disposable : "No management interface implemented";

		if (aCleanupResources == null)
		{
			aCleanupResources = new ArrayList<Object>();
		}

		aCleanupResources.add(rResource);
	}

	/***************************************
	 * Allows to replace (or remove) a resource in the cleanup registry. If the
	 * reference to the new resource is NULL, the old object will simply be
	 * removed. Else the new object will replace the old one at exactly the same
	 * position in the cleanup registry. That means that during cleanup it will
	 * be released at the same time at which the old resource would have been
	 * released.
	 *
	 * @param rOld The resource object to be replaced
	 * @param rNew The new resource or NULL to remove the old one
	 */
	public void replaceCleanupResource(Object rOld, Object rNew)
	{
		if (aCleanupResources != null)
		{
			if (rNew == null)
			{
				aCleanupResources.remove(rOld);
			}
			else
			{
				int nPos = aCleanupResources.indexOf(rOld);

				if (nPos >= 0)
				{
					aCleanupResources.set(nPos, rNew);
				}
				else
				{
					aCleanupResources.add(rNew);
				}
			}
		}
	}

	/***************************************
	 * This method must be invoked on new instances. It will perform the default
	 * steps necessary to initialize and run the application.
	 *
	 * @param rArgs The command line arguments of the application
	 */
	public final void run(String[] rArgs)
	{
		try
		{
			CommandLine aCommandLine = processArguments(rArgs);

			Log.debug("Initializing...");
			initialize(aCommandLine);
			Log.debug("Configuring...");
			configure(aCommandLine);
			Log.debug("Starting...");
			startApp();
			runApp();
			stopApp();
		}
		catch (Exception e)
		{
			handleApplicationError(e);
		}
	}

	/***************************************
	 * This is the main method that must be implemented by all subclasses. It
	 * contains the code the will perform the applications actual work task(s).
	 * The implementation may throw exceptions to signal errors but it must be
	 * aware that this will skip any final application method invocations. If
	 * final cleanup tasks need to be run anyway the implementation should do so
	 * in the method {@link #handleApplicationError(Exception)}.
	 *
	 * @throws Exception Subclasses may throw exceptions to signal errors
	 */
	protected abstract void runApp() throws Exception;

	/***************************************
	 * Performs the cleanup of all resources that have been registered with
	 * {@link #registerCleanupResource(Object)}. This method will be invoked by
	 * {@link #stopApp()}.
	 *
	 * <p>Subclasses may override this method to perform additional cleanup of
	 * resources that have been allocated by the application if necessary. But
	 * they should always invoke super.cleanup in their implementation. The
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
	 * Can be overridden by subclasses to provide the list of allowed command
	 * line switches. The default implementation returns an empty array which
	 * means that no switches are allowed (an exception will be thrown). To
	 * allow any kind of switches a subclass can return NULL. See the
	 * documentation of class {@link CommandLine} for more information.
	 *
	 * @return A string array containing the allowed switches or NULL to allow
	 *         any kind of switch
	 */
	protected String[] getCommandLineSwitches()
	{
		return new String[0];
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
		Log.error("Application execution error", e);
		e.printStackTrace();
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
		String[] rSwitches = getCommandLineSwitches();

		if (rSwitches == null)
		{
			return new CommandLine(rArgs);
		}
		else
		{
			return new CommandLine(rArgs, rSwitches);
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
		Log.debug("Cleanup...");
		cleanup();
		Log.debug("Terminating...");
		terminate();
		Log.debug("Application stopped");
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
