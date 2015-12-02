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
package de.esoco.lib.manage;

import de.esoco.lib.manage.TransactionTest.TestTransactionElement.State;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/********************************************************************
 * JUnit test case for the {@link TransactionManager} class.
 *
 * @author eso
 */
public class TransactionTest
{
	//~ Enums ------------------------------------------------------------------

	/********************************************************************
	 * Transaction test modes
	 */
	enum Mode { COMMIT, ROLLBACK, MULTI_LEVEL }

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Performs initial checks before a single test is run.
	 */
	@Before
	public void before()
	{
		assertFalse(TransactionManager.isInTransaction());
	}

	/***************************************
	 * Tests committing of transactions.
	 *
	 * @throws TransactionException
	 */
	@Test
	public void testCommit() throws TransactionException
	{
		performTransaction(true, false, 0);
	}

	/***************************************
	 * Tests failing commit of transactions.
	 *
	 * @throws TransactionException
	 */
	@Test
	public void testFailingCommit() throws TransactionException
	{
		performTransaction(true, true, 0);
	}

	/***************************************
	 * Tests failing rollback of transactions.
	 *
	 * @throws TransactionException
	 */
	@Test
	public void testFailingRollback() throws TransactionException
	{
		performTransaction(false, true, 0);
	}

	/***************************************
	 * Tests a commit of a multi-level transaction.
	 *
	 * @throws TransactionException
	 */
	@Test
	public void testMultiLevelCommit() throws TransactionException
	{
		performTransaction(true, false, 2);
	}

	/***************************************
	 * Tests a commit of a multi-level transaction that fails.
	 *
	 * @throws TransactionException
	 */
	@Test
	public void testMultiLevelFailedCommit() throws TransactionException
	{
		performTransaction(true, true, 2);
	}

	/***************************************
	 * Tests a rollback of a multi-level transaction that fails.
	 *
	 * @throws TransactionException
	 */
	@Test
	public void testMultiLevelFailedRollback() throws TransactionException
	{
		performTransaction(false, true, 2);
	}

	/***************************************
	 * Tests a rollback of a multi-level transaction.
	 *
	 * @throws TransactionException
	 */
	@Test
	public void testMultiLevelRollback() throws TransactionException
	{
		performTransaction(false, false, 2);
	}

	/***************************************
	 * Tests committing of transactions.
	 *
	 * @throws TransactionException
	 * @throws InterruptedException
	 */
	@Test
	public void testMultithreadCommit() throws InterruptedException
	{
		runThreadTest(true);
	}

	/***************************************
	 * Tests committing of transactions.
	 *
	 * @throws TransactionException
	 * @throws InterruptedException
	 */
	@Test
	public void testMultithreadRollback() throws InterruptedException
	{
		runThreadTest(false);
	}

	/***************************************
	 * Tests rollback of transactions.
	 *
	 * @throws TransactionException
	 */
	@Test
	public void testRollback() throws TransactionException
	{
		performTransaction(false, false, 0);
	}

	/***************************************
	 * Creates a new thread that invokes the commit or rollback test method.
	 *
	 * @param  bCommit TRUE for commit, FALSE for rollback
	 *
	 * @return A new thread
	 */
	private Thread createTransactionThread(final boolean bCommit)
	{
		return new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					if (bCommit)
					{
						testCommit();
					}
					else
					{
						testRollback();
					}
				}
				catch (TransactionException e)
				{
					fail("Thread commit failed");
				}
			}
		};
	}

	/***************************************
	 * Performs a transaction.
	 *
	 * @param  bCommit      TRUE for commit, FALSE for rollback
	 * @param  bWithFailure TRUE to add a faulty transaction step
	 * @param  nSubLevels   The number of additional transaction levels
	 *
	 * @throws TransactionException
	 */
	private void performTransaction(boolean bCommit,
									boolean bWithFailure,
									int		nSubLevels)
		throws TransactionException
	{
		TestTransactionElement te1  = new TestTransactionElement(false);
		TestTransactionElement te2  = new TestTransactionElement(false);
		TestTransactionElement fail = new TestTransactionElement(true);

		State	    rState		 = bCommit ? State.COMMITTED : State.ROLLBACK;
		Transaction rTransaction;
		int		    nLevel;

		rTransaction = TransactionManager.begin();
		nLevel		 = rTransaction.getLevel();

		assertTrue(TransactionManager.isInTransaction());

		TransactionManager.addTransactionElement(te1);
		rTransaction.addElement(te2);

		if (bWithFailure && nLevel == 1)
		{
			rTransaction.addElement(fail);
		}

		if (nSubLevels > 0)
		{
			performTransaction(bCommit, bWithFailure, nSubLevels - 1);
		}

		try
		{
			if (bCommit)
			{
				TransactionManager.commit();
			}
			else
			{
				TransactionManager.rollback();
			}

			if (bWithFailure)
			{
				if (bCommit && nLevel == 1 || !bCommit && nSubLevels == 0)
				{
					fail("Exception expected");
				}
			}
		}
		catch (TransactionException e)
		{
			if (!bWithFailure)
			{
				throw e;
			}
		}

		if (bWithFailure && nLevel == 1)
		{
			// check for level 1 because the first failure transaction will
			// cause the exception
			assertEquals(State.FAILURE, fail.aState);
			assertTrue(fail.bReleased);
		}

		if (bCommit && nLevel > 1)
		{
			assertTrue(rTransaction.hasActiveElements());
		}
		else
		{
			assertEquals(rState, te1.aState);
			assertEquals(rState, te2.aState);
			assertTrue(te1.bReleased);
			assertTrue(te2.bReleased);
			assertEquals(nLevel - 1, rTransaction.getLevel());
			assertEquals(0, rTransaction.getElements().size());
			assertFalse(rTransaction.hasActiveElements());

			if (nLevel == 1)
			{
				assertFalse(TransactionManager.isInTransaction());
			}
		}
	}

	/***************************************
	 * Runs a multi-threaded transaction test.
	 *
	 * @param  bCommit TRUE for commit, FALSE for rollback
	 *
	 * @throws InterruptedException
	 */
	private void runThreadTest(boolean bCommit) throws InterruptedException
	{
		Thread aThread1 = createTransactionThread(bCommit);
		Thread aThread2 = createTransactionThread(bCommit);

		aThread1.start();
		aThread2.start();

		aThread1.join();
		aThread2.join();
	}

	//~ Inner Classes ----------------------------------------------------------

	/********************************************************************
	 * A simple transaction implementation for test purposes.
	 */
	static class TestTransactionElement implements Transactional, Releasable
	{
		//~ Enums --------------------------------------------------------------

		/********************************************************************
		 * Transaction states
		 */
		enum State { NEW, COMMITTED, ROLLBACK, FAILURE }

		//~ Instance fields ----------------------------------------------------

		State   aState    = State.NEW;
		boolean bFail     = false;
		boolean bReleased = false;

		//~ Constructors -------------------------------------------------------

		/***************************************
		 * Constructor.
		 *
		 * @param bFail TRUE to make the commit and rollback methods fail with
		 *              an exception
		 */
		public TestTransactionElement(boolean bFail)
		{
			this.bFail = bFail;
		}

		//~ Methods ------------------------------------------------------------

		/***************************************
		 * @see Transactional#commit()
		 */
		@Override
		public void commit() throws Exception
		{
			if (bFail)
			{
				aState = State.FAILURE;
				throw new Exception("TestTransaction forced failure");
			}
			else
			{
				aState = State.COMMITTED;
			}
		}

		/***************************************
		 * @see Releasable#release()
		 */
		@Override
		public void release()
		{
			if (bReleased)
			{
				throw new RuntimeException("Already released");
			}

			bReleased = true;
		}

		/***************************************
		 * @see Transactional#rollback()
		 */
		@Override
		public void rollback() throws Exception
		{
			if (bFail)
			{
				aState = State.FAILURE;
				throw new Exception("TestTransaction forced failure");
			}
			else
			{
				aState = State.ROLLBACK;
			}
		}
	}
}
