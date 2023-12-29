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
package de.esoco.lib.manage;

import de.esoco.lib.logging.Log;

/**
 * A thread-based transaction manager implementation. New transactions will be
 * associated with the current thread and all subsequent invocations of the
 * static manager methods will operate on the transaction for the current
 * thread. The transaction that is associated with the current thread can be
 * queried with the method {@link #getTransaction()}. The static methods in this
 * class are only shortcuts to the transaction methods which may be invoked
 * alternatively.
 *
 * <p>A new transaction for the current thread can be started by invoking the
 * method {@link #begin()}. Afterwards, transaction elements which implement the
 * interface {@link Transactional} can be added to the thread's transaction by
 * means of the method {@link #addTransactionElement(Transactional)}. To
 * complete a transaction it can either be committed (i.e. made persistent) by
 * calling the method {@link #commit()} or rolled back (canceled) by using
 * {@link #rollback()}.</p>
 *
 * <p>If a call to {@link #commit()} or {@link #rollback()} fails the
 * respective method will throw a {@link TransactionException}. All transaction
 * elements after the element that caused the failure will be rolled back before
 * the call returns. All elements that had been committed before the failure
 * will remain so because currently there's no way to revert the commit.
 * Therefore it is recommended to handle modifications in different contexts in
 * different transactions to prevent inconsistencies between the context.</p>
 *
 * <p>Transactions can be nested. Any subsequent call to the {@link #begin()}
 * method will increase the transaction level by 1. The current level can be
 * queried with the method {@link Transaction#getLevel()}. To prevent
 * inconsistencies of stored data a commit will only be executed if no more
 * nested transactions exist (i.e. the transaction level reaches zero) while a
 * rollback will be executed immediately.</p>
 *
 * <p>Either way, a thread's transaction will remain available (but inactive
 * after the first commit/rollback) until a distinct commit or rollback has been
 * executed for each call to the method {@link #begin()}. Any try to invoke a
 * method that doesn't match the current state (e.g. trying to begin a new
 * transaction on one that has been rolled back) will cause an exception.
 * Therefore it is paramount to implement transactional code correctly to avoid
 * errors by matching call to begin and commit/rollback. Typical transaction
 * code should look like the following on each level that starts a
 * transaction:</p>
 *
 * <pre>
 * TransactionManager.begin();
 * try
 * {  // add the transaction elements
 * Transactional aElement = ...;
 * TransactionManager.addTransactionElement(aElement);
 * ...
 * }
 * catch (Exception e)
 * {  // rollback on any error, even runtime exceptions
 * TransactionManager.rollback();
 * // re-throw exceptions (wrap if necessary) to signal the rollback to the
 * // invoking code unless this is the top-level error handling code; in the
 * // latter case the commit must only be performed if no rollback occurred
 * // (e.g. by checking TransactionManager.isInTransaction()
 * throw e;
 * }
 * // commit the successfully created transaction elements
 * TransactionManager.commit();
 *
 * </pre>
 *
 * @author eso
 */
public class TransactionManager {

	private static ThreadLocal<Transaction> aThreadTransaction;

	static {
		aThreadTransaction = new ThreadLocal<Transaction>();
	}

	/**
	 * Private, only static use.
	 */
	private TransactionManager() {
	}

	/**
	 * Adds a certain transactional element to the current transaction. If the
	 * element is already part of the transaction it will be ignored.
	 *
	 * @param rElement The element to add
	 * @throws IllegalStateException If there's no active transaction for the
	 *                               current thread
	 */
	public static void addTransactionElement(Transactional rElement) {
		getTransaction().addElement(rElement);
	}

	/**
	 * Begins a new transaction for the current thread.
	 *
	 * @return Returns the new transaction instance
	 */
	public static Transaction begin() {
		Transaction aTransaction = aThreadTransaction.get();

		if (aTransaction == null) {
			aTransaction = new Transaction();
			aThreadTransaction.set(aTransaction);
		} else {
			aTransaction.incrementLevel();
		}

		Log.debugf("Begin transaction %s", aTransaction);

		return aTransaction;
	}

	/**
	 * Creates and returns a new transaction that is not managed by the
	 * transaction manager and therefore isn't bound to a specific thread. This
	 * method can be used in special cases where applications need to handle
	 * separate transactions, e.g. to coordinate separate threads by
	 * themselves.
	 * Because transactions implement the {@link Transactional} interface they
	 * can be added to other transactions to perform the final commit or
	 * rollback.
	 *
	 * @return The new unmanaged transaction instance
	 */
	public static Transaction beginUnmanagedTransaction() {
		return new Transaction();
	}

	/**
	 * Commits the transaction of the current thread by committing all
	 * transaction elements in the reverse order in which they have been added.
	 * If an element fails to commit a {@link TransactionException} will be
	 * thrown which contains a reference to the element that failed. The
	 * thread's transaction will then remain in a partially committed state and
	 * the application code should invoke either rollback() or commit() to
	 * finish the remaining transaction elements.
	 *
	 * @throws IllegalStateException If there's no active transaction for the
	 *                               current thread
	 * @throws TransactionException  If committing a transaction element fails
	 */
	public static void commit() throws TransactionException {
		Transaction rTransaction = getTransaction();

		try {
			Log.debugf("Commit of %s", rTransaction);
			rTransaction.commit();
		} finally {
			if (rTransaction.isFinished()) {
				removeTransaction(rTransaction);
			}
		}
	}

	/**
	 * Returns the transaction instance for the current thread. Throws an
	 * exception if there isn't a transaction active for the invoking thread.
	 *
	 * @return The current thread's transaction
	 * @throws IllegalStateException If there is no active transaction for the
	 *                               current thread
	 */
	public static Transaction getTransaction() {
		Transaction rTransaction = aThreadTransaction.get();

		if (rTransaction == null) {
			throw new IllegalStateException("Thread not in transaction");
		}

		return rTransaction;
	}

	/**
	 * Returns the transaction state of the current thread.
	 *
	 * @return TRUE if there's an active transaction for the current thread
	 */
	public static boolean isInTransaction() {
		return aThreadTransaction.get() != null;
	}

	/**
	 * Checks whether a certain transaction element is active in the current
	 * thread's transaction. An element is active if it is part of this
	 * transaction and is not yet committed or rolled back.
	 *
	 * @param rElement The element to check
	 * @return TRUE if the given element is active in the current thread's
	 * transaction, FALSE if not or if there's no active transaction for the
	 * current thread
	 */
	public static boolean isTransactionElement(Transactional rElement) {
		Transaction rTransaction = aThreadTransaction.get();

		return rTransaction != null &&
			rTransaction.getElements().contains(rTransaction);
	}

	/**
	 * Package-internal method to remove a certain transaction. Will be invoked
	 * by a transaction after it has finished completely.
	 *
	 * @param rTransaction The transaction to remove
	 */
	static void removeTransaction(Transaction rTransaction) {
		assert getTransaction() == rTransaction :
			"Not current thread's transaction: " + rTransaction;

		aThreadTransaction.remove();
	}

	/**
	 * Removes a certain element from the current thread's transaction. If the
	 * element isn't currently part of the transaction the method call won't
	 * have any effect. The given element will neither be committed nor rolled
	 * back by this method. It is the responsibility of the calling context to
	 * handle the removed element as necessary.
	 *
	 * @param rElement The element to remove from the thread's current
	 *                 transaction
	 * @throws IllegalStateException If there's no active transaction for the
	 *                               current thread
	 */
	public static void removeTransactionElement(Transactional rElement) {
		getTransaction().removeElement(rElement);
	}

	/**
	 * Performs a rollback of the transaction that is active for the current
	 * thread.
	 *
	 * @throws IllegalStateException If there's no active transaction for the
	 *                               current thread
	 * @throws TransactionException  If the rollback of a transaction element
	 *                               fails
	 */
	public static void rollback() throws TransactionException {
		Transaction rTransaction = getTransaction();

		try {
			Log.debugf("Rollback of %s", rTransaction);
			rTransaction.rollback();
		} finally {
			if (rTransaction.isFinished()) {
				removeTransaction(rTransaction);
			}
		}
	}

	/**
	 * Performs a shutdown of the transaction manager and frees all allocated
	 * resources.
	 */
	public static void shutdown() {
		if (aThreadTransaction.get() != null) {
			aThreadTransaction.get().rollback();
		}

		aThreadTransaction = null;
	}
}
