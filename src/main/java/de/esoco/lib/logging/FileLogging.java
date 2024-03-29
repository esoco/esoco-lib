//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'esoco-lib' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import de.esoco.lib.expression.ThrowingSupplier;

import java.io.FileWriter;

/**
 * A log aspect that appends log records to a file.
 *
 * @author eso
 */
public class FileLogging extends StreamLogging {

	private final String fileName;

	/**
	 * Creates a new instance that logs to a certain file.
	 *
	 * @param fileName The name of the log file
	 */
	public FileLogging(String fileName) {
		super(ThrowingSupplier.of(() -> new FileWriter(fileName, true)));

		this.fileName = fileName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getLogInitMessage() {
		return "Starting logging to file " + fileName;
	}
}
