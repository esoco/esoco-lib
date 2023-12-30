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

import de.esoco.lib.logging.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A transaction class. Transaction instances are typically managed on a
 * per-thread basis by the {@link TransactionManager} class unless they are
 * created through {@link TransactionManager#beginUnmanagedTransaction()}.
 *
 * <p>A transaction basically is a queue of {@link Transactional} elements
 * which can be committed or rolled back together with the methods in this
 * class. The queue structure means that the elements that have been added first
 * will also be processed first when the transaction is finished (the FIFO
 * principle).</p>
 *
 * <p>If a transaction element implements the {@link Releasable} interface it
 * will be automatically released when a transaction is finished in either way.
 * The number of calls to the {@link Releasable#release() }method will be equal
 * to the number of times the element has been added to the transaction with the
 * {@link #addElement(Transactional)} method. Applications therefore do not need
 * to release such elements by themselves after handing them over to the
 * transaction manager.</p>
 *
 * @author eso
 */
public class Transaction implements Transactional {

	private static int nextTransactionId = 1;

	private final int id = nextTransactionId++;

	private Queue<Transactional> transactionElements =
		new LinkedList<Transactional>();

	private Queue<Releasable> releasableElements =
		new LinkedList<Releasable>();

	private int level = 1;

	private boolean committing = false;

	/**
	 * Package-internal constructor.
	 */
	Transaction() {
	}

	/**
	 * Adds a certain transactional element to this transaction. If the element
	 * is already part of this transaction it will be ignored.
	 *
	 * @param element The element to add
	 * @throws IllegalStateException If this transaction is no longer active
	 */
	public final void addElement(Transactional element) {
		checkActive();

		if (element == null) {
			throw new IllegalArgumentException("Element must not be NULL");
		}

		if (!transactionElements.contains(element)) {
			transactionElements.add(element);
		}

		if (element instanceof Releasable) {
			releasableElements.add((Releasable) element);
		}
	}

	/**
	 * Performs a commit by invoking {@link Transactional#commit()} on all
	 * elements of this transaction if the topmost transaction level has been
	 * reached.
	 *
	 * @throws IllegalStateException If this transaction is no longer active
	 * @throws TransactionException  If the rollback of a certain transaction
	 *                               element fails
	 */
	@Override
	public final void commit() throws TransactionException {
		assert level >= 0 : "Invalid transaction level: " + level;

		// committing will safeguard against re-commits caused by transactions
		// that are started from transaction elements that are currently
		// committed by the method endTransaction()
		if (--level == 0 && !committing) {
			committing = true;
			endTransaction(true);
		}
	}

	/**
	 * Returns a readonly collection of the transactional elements in this
	 * transaction. The returned collection will contain all transaction
	 * elements that have been added to this transaction so far. If this
	 * transaction is new or inactive (see {@link #hasActiveElements()}) the
	 * returned list will be empty.
	 *
	 * @return The list of transaction elements
	 */
	public final Collection<Transactional> getElements() {
		if (transactionElements != null) {
			return Collections.unmodifiableCollection(transactionElements);
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Returns the current transaction level. The starting level of a
	 * transaction is 1. A level of zero means that the transaction has been
	 * committed or rolled back.
	 *
	 * @return The current transaction level
	 */
	public final int getLevel() {
		return level;
	}

	/**
	 * Checks whether this transaction has been finished completely.
	 *
	 * @return TRUE if this transaction has been finished
	 */
	public final boolean isFinished() {
		return level == 0 && !hasActiveElements();
	}

	/**
	 * Removes a certain element from the this transaction. If the element
	 * isn't
	 * currently part of this transaction the method call won't have any
	 * effect.
	 * The given element will neither be committed nor rolled back by this
	 * method. It is the responsibility of the calling context to handle the
	 * removed element as necessary.
	 *
	 * @param element The element to remove from the thread's current
	 *                transaction
	 * @throws IllegalStateException If this transaction is no longer active
	 */
	public final void removeElement(Transactional element) {
		checkActive();

		if (element == null) {
			throw new IllegalArgumentException("Element must not be NULL");
		}

		transactionElements.remove(element);
	}

	/**
	 * Performs a rollback by invoking {@link Transactional#rollback()} on all
	 * elements of this transaction.
	 *
	 * @throws IllegalStateException If this transaction is no longer active
	 * @throws TransactionException  If the rollback of a certain transaction
	 *                               element fails
	 */
	@Override
	public final void rollback() throws TransactionException {
		assert level >= 0 : "Invalid transaction level: " + level;

		if (level == 0) {
			throw new IllegalStateException(
				"Transaction already rolled back completely");
		}

		level -= 1;

		if (hasActiveElements()) {
			endTransaction(false);
		}
	}

	/**
	 * Returns a string representation of this transaction.
	 *
	 * @return A string describing this transaction
	 */
	@Override
	@SuppressWarnings("boxing")
	public String toString() {
		return String.format("Transaction-%d[%d, %s]", id, level,
			transactionElements);
	}

	/**
	 * Checks whether this transaction is still active. Returns FALSE after
	 * this
	 * transaction has been committed or rolled back completely.
	 *
	 * @return The active state of this transaction
	 */
	final boolean hasActiveElements() {
		return transactionElements != null;
	}

	/**
	 * Package-internal method to increment the transaction level.
	 */
	final void incrementLevel() {
		checkActive();
		level += 1;
	}

	/**
	 * Internal method to check the active state of this transaction and to
	 * throw an exception if it is no longer active.
	 *
	 * @throws IllegalStateException If this transaction is no longer active
	 */
	private void checkActive() {
		if (!hasActiveElements()) {
			throw new IllegalStateException("Transaction not active");
		}
	}

	/**
	 * Internal method to finish the current thread's transaction either by
	 * committing or by performing a rollback for all transaction elements.
	 *
	 * @param commit TRUE to commit, FALSE to rollback
	 * @throws IllegalStateException If this transaction is no longer active
	 * @throws TransactionException  If finishing a certain transaction element
	 *                               fails
	 */
	private void endTransaction(boolean commit) throws TransactionException {
		checkActive();

		try {
			while (!transactionElements.isEmpty()) {
				Transactional element = transactionElements.poll();

				if (commit) {
					element.commit();
				} else {
					element.rollback();
				}
			}
		} catch (Exception e) {
			rollbackRemainingElements();

			throw new TransactionException(
				(commit ? "Commit" : "Rollback") + " failed", e);
		} finally {
			releaseElements();

			// mark this transaction as inactive
			transactionElements = null;
		}
	}

	/**
	 * Releases all transaction elements that implement the {@link Releasable}
	 * interface.
	 */
	private void releaseElements() {
		while (!releasableElements.isEmpty()) {
			Releasable element = releasableElements.poll();

			try {
				element.release();
			} catch (Exception e) {
				Log.warn("Error on release of " + element, e);
			}
		}

		releasableElements = null;
	}

	/**
	 * This method performs a rollback of all remaining transaction elements.
	 * Any exceptions will be logged as an error but otherwise they will be
	 * ignored. Releasable elements will be released.
	 */
	private void rollbackRemainingElements() {
		if (transactionElements != null) {
			while (!transactionElements.isEmpty()) {
				Transactional element = transactionElements.poll();

				try {
					element.rollback();
				} catch (Exception e) {
					Log.error("Error on cleanup rollback of " + element, e);
				}
			}
		}
	}
}
