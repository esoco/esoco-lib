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
package de.esoco.lib.logging;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * A log aspect that writes log data to streams. The {@link Writer} to write the
 * log records to must be provided by a {@link Supplier} function that is handed
 * to the constructor {@link StreamLogging}.
 *
 * @author eso
 */
public class StreamLogging extends LogAspect<String> {

	private final Supplier<Writer> getWriter;

	/**
	 * Creates a new instance that get's the target writer from a function. If
	 * the logging target is a stream the supplier function could wrap it into
	 * an {@link OutputStreamWriter}, for example. The writer returned by the
	 * function will be closed after each access and therefore needs to be
	 * re-created (or re-opened) by each call to the supplier. This should be
	 * taken into account when creating the actual writer, e.g. by opening a
	 * {@link FileWriter} or {@link FileOutputStream} in append mode.
	 *
	 * @param getWriter A function that returns the target writer
	 */
	public StreamLogging(Supplier<Writer> getWriter) {
		this.getWriter = getWriter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String createLogObject(LogRecord logRecord) {
		return logRecord.format(get(MIN_STACK_LOG_LEVEL));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processLogObjects(Collection<String> logs) throws Exception {
		try (Writer output = getWriter.get()) {
			for (String log : logs) {
				output.write(log);
				output.write('\n');
			}

			output.flush();
		}
	}
}
