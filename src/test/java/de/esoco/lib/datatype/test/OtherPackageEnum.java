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
package de.esoco.lib.datatype.test;

import de.esoco.lib.datatype.GenericEnum;

/**
 * Test enum subclass in another package.
 *
 * @author eso
 */
public class OtherPackageEnum extends GenericEnum<OtherPackageEnum> {

	/**
	 * Test enum instance
	 */
	public static final OtherPackageEnum PACKAGE_ENUM =
		new OtherPackageEnum("PACKAGE_ENUM");

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	OtherPackageEnum(String rName) {
		super(rName);
	}
}
