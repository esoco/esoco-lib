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
package de.esoco.lib.logging;

import java.util.LinkedHashMap;
import java.util.Map;


/********************************************************************
 * A class that collects performance measurements that are added through {@link
 * #measure(String, long)}.
 *
 * @author eso
 */
public class Profiler
{
	//~ Instance fields --------------------------------------------------------

	String sDescription;

	private long nCreationTime = System.currentTimeMillis();
	private long nStartTime    = System.currentTimeMillis();

	private Map<String, Profiler.Measurement> aMeasurements =
		new LinkedHashMap<>();

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param sDescription A description of this instance
	 */
	public Profiler(String sDescription)
	{
		this.sDescription = sDescription;
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Returns a certain result.
	 *
	 * @param  sDescription The description of the result
	 *
	 * @return The result measurement or NULL for none
	 */
	public Profiler.Measurement getResult(String sDescription)
	{
		return aMeasurements.get(sDescription);
	}

	/***************************************
	 * Returns a mapping from descriptions to resulting measurements.
	 *
	 * @return The results
	 */
	public Map<String, Profiler.Measurement> getResults()
	{
		return aMeasurements;
	}

	/***************************************
	 * Measures the time that a certain execution has taken from the last
	 * measurement until now.
	 *
	 * @param  sDescription The description of the measured execution
	 *
	 * @return The current time in milliseconds (for concatenation of
	 *         measurements)
	 */
	public long measure(String sDescription)
	{
		return measure(sDescription, nStartTime);
	}

	/***************************************
	 * Measures the time from a certain starting time until now.
	 *
	 * @param  sDescription The description of the measured execution
	 * @param  nFromTime    The starting time of the measurement in milliseconds
	 *
	 * @return The current time in milliseconds (for concatenation of
	 *         measurements)
	 */
	public long measure(String sDescription, long nFromTime)
	{
		Measurement aMeasurement = aMeasurements.get(sDescription);
		long	    nNow		 = System.currentTimeMillis();
		long	    nDuration    = nNow - nFromTime;

		if (aMeasurement == null)
		{
			aMeasurements.put(sDescription, new Measurement(nDuration));
		}
		else
		{
			aMeasurement.add(nDuration);
		}

		nStartTime = nNow;

		return nNow;
	}

	/***************************************
	 * Prints all measurements to {@link System#out}.
	 *
	 * @param sIndent The indentation to print with
	 */
	public void printResults(String sIndent)
	{
		for (String sDescription : aMeasurements.keySet())
		{
			System.out.printf("%sTotal time for %s: %s\n",
							  sIndent,
							  sDescription,
							  aMeasurements.get(sDescription));
		}
	}

	/***************************************
	 * Prints the description of this instance, the total time since creation in
	 * seconds, and all measurements to {@link System#out}.
	 */
	@SuppressWarnings("boxing")
	public void printSummary()
	{
		long nTime = (System.currentTimeMillis() - nCreationTime) / 1000;

		System.out.printf("=== %s: %dm %02ds ===\n",
						  sDescription,
						  nTime / 60,
						  nTime % 60);

		printResults("  ");
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A data structure for profiling measurements.
	 *
	 * @author eso
	 */
	public static class Measurement
	{
		//~ Instance fields ----------------------------------------------------

		private long nTotalTime;
		private int  nCount;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Creates a new instance.
		 *
		 * @param nTime
		 */
		private Measurement(long nTime)
		{
			nTotalTime = nTime;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * Returns the average measured time in milliseconds (i.e. {@link
		 * #getTotalTime()} / {@link #getCount()}).
		 *
		 * @return The average time
		 */
		public long getAverageTime()
		{
			return nCount > 0 ? nTotalTime / nCount : nTotalTime;
		}

		/***************************************
		 * Returns the number of single measurements taken.
		 *
		 * @return The number of measurements
		 */
		public final int getCount()
		{
			return nCount;
		}

		/***************************************
		 * Returns the total measured time in milliseconds.
		 *
		 * @return The total time
		 */
		public long getTotalTime()
		{
			return nTotalTime;
		}

		/***************************************
		 * Returns a string representation of the measured time.
		 *
		 * @return The string representation of this measurement
		 */
		@Override
		@SuppressWarnings("boxing")
		public String toString()
		{
			if (nTotalTime > 1_200_000L)
			{
				long nSeconds = nTotalTime / 1000;

				return String.format("%dm %02ds", nSeconds / 60, nSeconds % 60);
			}
			else
			{
				return String.format("%d.%03ds",
									 nTotalTime / 1000,
									 nTotalTime % 1000);
			}
		}

		/***************************************
		 * Adds time in milliseconds to this record.
		 *
		 * @param nTime The time to add
		 */
		private void add(long nTime)
		{
			nTotalTime += nTime;
			nCount++;
		}
	}
}
