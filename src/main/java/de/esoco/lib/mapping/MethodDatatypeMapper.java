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
package de.esoco.lib.mapping;

import java.lang.reflect.Method;

/**
 * Interface that defines the mapping of certain datatypes of method parameters.
 * Implementations should normally work in both ways, i.e. it should be possible
 * to convert the result of one mapping back to the original type with another
 * call to one of the mapping methods. Accordingly, appliesTo() should return
 * TRUE for both types.
 *
 * <p>Each method call has two arguments, the method that has been invoked and
 * the datatype or value to map. Implementations that provide a global mapping
 * of datatypes can ignore the method parameter and simply map each occurence of
 * a covered datatype. The method argument allows to limit the mapping to
 * certain methods or to map datatypes differently for different methods.</p>
 */
public interface MethodDatatypeMapper {

	/**
	 * Checks if this mapper applies to a particular datatype. This method must
	 * return TRUE for all datatypes that are covered by this mapper.
	 *
	 * @param rOriginalMethod The original method that has been invoked
	 * @param rDatatype       The datatype to check
	 * @return TRUE if this mapper should be used to map parameters of the
	 * given
	 * type
	 */
	boolean appliesTo(Method rOriginalMethod, Class<?> rDatatype);

	/**
	 * Maps the argument datatype into another type. This should normally be
	 * implemented both ways, i.e. the mapped type should also be a valid
	 * argument to this method.
	 *
	 * @param rOriginalMethod The original method that has been invoked
	 * @param rDatatype       The datatype to map
	 * @return The converted type
	 */
	Class<?> mapType(Method rOriginalMethod, Class<?> rDatatype);

	/**
	 * Maps the argument value into another type. The actual value itself
	 * should
	 * normally not be modified (at least not more than necessary for the new
	 * type). This should normally be implemented both ways, i.e. the mapped
	 * value should also be a valid argument to this method.
	 *
	 * @param rTarget         The target object of the method call
	 * @param rOriginalMethod The original method that has been invoked
	 * @param rValue          The value to map the datatype of
	 * @return The converted value
	 */
	Object mapValue(Object rTarget, Method rOriginalMethod, Object rValue);
}
