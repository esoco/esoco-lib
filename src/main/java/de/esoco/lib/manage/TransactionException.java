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

/********************************************************************
 * A runtime exception that will be thrown if a transaction that is managed by
 * the {@link TransactionManager} fails.
 *
 * @author eso
 */
public class TransactionException extends RuntimeException
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long serialVersionUID = 1L;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * @see Exception#Exception(String)
	 */
	public TransactionException(String sMessage)
	{
		super(sMessage);
	}

	/***************************************
	 * @see Exception#Exception(Throwable)
	 */
	public TransactionException(Throwable eCause)
	{
		super(eCause);
	}

	/***************************************
	 * @see Exception#Exception(Throwable)
	 */
	public TransactionException(String sMessage, Throwable eCause)
	{
		super(sMessage, eCause);
	}
}
