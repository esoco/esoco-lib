//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2016 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JUnit test case for the {@link TransactionManager} class.
 *
 * @author eso
 */
public class TransactionTest {

	/**
	 * Transaction test modes
	 */
	enum Mode {COMMIT, ROLLBACK, MULTI_LEVEL}

	/**
	 * Performs initial checks before a single test is run.
	 */
	@BeforeEach
	public void before() {
		assertFalse(TransactionManager.isInTransaction());
	}

	/**
	 * Tests committing of transactions.
	 */
	@Test
	public void testCommit() throws TransactionException {
		performTransaction(true, false, 0);
	}

	/**
	 * Tests failing commit of transactions.
	 */
	@Test
	public void testFailingCommit() throws TransactionException {
		performTransaction(true, true, 0);
	}

	/**
	 * Tests failing rollback of transactions.
	 */
	@Test
	public void testFailingRollback() throws TransactionException {
		performTransaction(false, true, 0);
	}

	/**
	 * Tests a commit of a multi-level transaction.
	 */
	@Test
	public void testMultiLevelCommit() throws TransactionException {
		performTransaction(true, false, 2);
	}

	/**
	 * Tests a commit of a multi-level transaction that fails.
	 */
	@Test
	public void testMultiLevelFailedCommit() throws TransactionException {
		performTransaction(true, true, 2);
	}

	/**
	 * Tests a rollback of a multi-level transaction that fails.
	 */
	@Test
	public void testMultiLevelFailedRollback() throws TransactionException {
		performTransaction(false, true, 2);
	}

	/**
	 * Tests a rollback of a multi-level transaction.
	 */
	@Test
	public void testMultiLevelRollback() throws TransactionException {
		performTransaction(false, false, 2);
	}

	/**
	 * Tests committing of transactions.
	 */
	@Test
	public void testMultithreadCommit() throws InterruptedException {
		runThreadTest(true);
	}

	/**
	 * Tests committing of transactions.
	 */
	@Test
	public void testMultithreadRollback() throws InterruptedException {
		runThreadTest(false);
	}

	/**
	 * Tests rollback of transactions.
	 */
	@Test
	public void testRollback() throws TransactionException {
		performTransaction(false, false, 0);
	}

	/**
	 * Creates a new thread that invokes the commit or rollback test method.
	 *
	 * @param commit TRUE for commit, FALSE for rollback
	 * @return A new thread
	 */
	private Thread createTransactionThread(final boolean commit) {
		return new Thread() {
			@Override
			public void run() {
				try {
					if (commit) {
						testCommit();
					} else {
						testRollback();
					}
				} catch (TransactionException e) {
					fail("Thread commit failed");
				}
			}
		};
	}

	/**
	 * Performs a transaction.
	 *
	 * @param commit      TRUE for commit, FALSE for rollback
	 * @param withFailure TRUE to add a faulty transaction step
	 * @param subLevels   The number of additional transaction levels
	 */
	private void performTransaction(boolean commit, boolean withFailure,
		int subLevels) throws TransactionException {
		TestTransactionElement te1 = new TestTransactionElement(false);
		TestTransactionElement te2 = new TestTransactionElement(false);
		TestTransactionElement fail = new TestTransactionElement(true);

		State state = commit ? State.COMMITTED : State.ROLLBACK;
		Transaction transaction;
		int level;

		transaction = TransactionManager.begin();
		level = transaction.getLevel();

		assertTrue(TransactionManager.isInTransaction());

		TransactionManager.addTransactionElement(te1);
		transaction.addElement(te2);

		if (withFailure && level == 1) {
			transaction.addElement(fail);
		}

		if (subLevels > 0) {
			performTransaction(commit, withFailure, subLevels - 1);
		}

		try {
			if (commit) {
				TransactionManager.commit();
			} else {
				TransactionManager.rollback();
			}

			if (withFailure) {
				if (commit && level == 1 || !commit && subLevels == 0) {
					fail("Exception expected");
				}
			}
		} catch (TransactionException e) {
			if (!withFailure) {
				throw e;
			}
		}

		if (withFailure && level == 1) {
			// check for level 1 because the first failure transaction will
			// cause the exception
			assertEquals(State.FAILURE, fail.state);
			assertTrue(fail.released);
		}

		if (commit && level > 1) {
			assertTrue(transaction.hasActiveElements());
		} else {
			assertEquals(state, te1.state);
			assertEquals(state, te2.state);
			assertTrue(te1.released);
			assertTrue(te2.released);
			assertEquals(level - 1, transaction.getLevel());
			assertEquals(0, transaction.getElements().size());
			assertFalse(transaction.hasActiveElements());

			if (level == 1) {
				assertFalse(TransactionManager.isInTransaction());
			}
		}
	}

	/**
	 * Runs a multi-threaded transaction test.
	 *
	 * @param commit TRUE for commit, FALSE for rollback
	 */
	private void runThreadTest(boolean commit) throws InterruptedException {
		Thread thread1 = createTransactionThread(commit);
		Thread thread2 = createTransactionThread(commit);

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();
	}

	/**
	 * A simple transaction implementation for test purposes.
	 */
	static class TestTransactionElement implements Transactional, Releasable {

		/**
		 * Transaction states
		 */
		enum State {NEW, COMMITTED, ROLLBACK, FAILURE}

		State state = State.NEW;

		boolean fail = false;

		boolean released = false;

		/**
		 * Constructor.
		 *
		 * @param fail TRUE to make the commit and rollback methods fail
		 *                   with an
		 *             exception
		 */
		public TestTransactionElement(boolean fail) {
			this.fail = fail;
		}

		/**
		 * @see Transactional#commit()
		 */
		@Override
		public void commit() throws Exception {
			if (fail) {
				state = State.FAILURE;
				throw new Exception("TestTransaction forced failure");
			} else {
				state = State.COMMITTED;
			}
		}

		/**
		 * @see Releasable#release()
		 */
		@Override
		public void release() {
			if (released) {
				throw new RuntimeException("Already released");
			}

			released = true;
		}

		/**
		 * @see Transactional#rollback()
		 */
		@Override
		public void rollback() throws Exception {
			if (fail) {
				state = State.FAILURE;
				throw new Exception("TestTransaction forced failure");
			} else {
				state = State.ROLLBACK;
			}
		}
	}
}
