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
package de.esoco.lib.comm;

/********************************************************************
 * The base class for all exceptions that can occur in the communications
 * framework. It is derived from {@link RuntimeException} because errors can
 * occur during all communication phases and should therefore always be
 * expected.
 *
 * @author eso
 */
public class CommunicationException extends RuntimeException
{
	//~ Static fields/initializers ---------------------------------------------

	private static final long serialVersionUID = 1L;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * @see RuntimeException#RuntimeException()
	 */
	public CommunicationException()
	{
	}

	/***************************************
	 * @see RuntimeException#RuntimeException(String)
	 */
	public CommunicationException(String sMessage)
	{
		super(sMessage);
	}

	/***************************************
	 * @see RuntimeException#RuntimeException(Throwable)
	 */
	public CommunicationException(Exception eCause)
	{
		super(eCause);
	}

	/***************************************
	 * @see RuntimeException#RuntimeException(String, Throwable)
	 */
	public CommunicationException(String sMessage, Exception eCause)
	{
		super(sMessage, eCause);
	}
}
