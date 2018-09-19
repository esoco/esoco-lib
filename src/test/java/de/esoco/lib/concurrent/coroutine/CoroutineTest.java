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
package de.esoco.lib.concurrent.coroutine;

import de.esoco.lib.concurrent.coroutine.step.Condition;
import de.esoco.lib.concurrent.coroutine.step.Iteration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import static de.esoco.lib.concurrent.coroutine.ChannelId.stringChannel;
import static de.esoco.lib.concurrent.coroutine.step.ChannelReceive.receive;
import static de.esoco.lib.concurrent.coroutine.step.ChannelSend.send;
import static de.esoco.lib.concurrent.coroutine.step.CodeExecution.apply;
import static de.esoco.lib.concurrent.coroutine.step.CodeExecution.supply;
import static de.esoco.lib.concurrent.coroutine.step.Condition.doIf;
import static de.esoco.lib.concurrent.coroutine.step.Condition.doIfElse;
import static de.esoco.lib.concurrent.coroutine.step.Iteration.forEach;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.obrel.type.StandardTypes.NAME;


/********************************************************************
 * Test of {@link Coroutine}.
 *
 * @author eso
 */
public class CoroutineTest
{
	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * Test of asynchronous channel communication.
	 */
	@Test
	public void testChannel()
	{
		CoroutineContext  ctx = new CoroutineContext();
		ChannelId<String> ch  = stringChannel("TEST");

		Coroutine<String, String> cs =
			Coroutine.first(apply((String s) -> s + "test"))
					 .then(send(ch))
					 .with(NAME, "Send");

		Coroutine<?, String> cr1 =
			Coroutine.first(receive(ch))
					 .with(NAME, "Receive")
					 .then(apply(s -> s.toUpperCase()));
		Coroutine<?, String> cr2 =
			cr1.then(apply((String s) -> s.toLowerCase()));

		Continuation<String> r1 = cr1.runAsync(ctx);
		Continuation<String> r2 = cr2.runAsync(ctx);

		Continuation<?> s1 = cs.runAsync(ctx, "123");
		Continuation<?> s2 = cs.runAsync(ctx, "456");

		assertEquals("123test", s1.getResult());
		assertEquals("456test", s2.getResult());

		String r1v = r1.getResult();
		String r2v = r2.getResult();

		// because of the concurrent execution it is not sure which result
		// r1 and r2 will receive
		assertTrue(
			"123test".equalsIgnoreCase(r1v) || "456test".equalsIgnoreCase(r1v));
		assertTrue(
			"123test".equalsIgnoreCase(r2v) || "456test".equalsIgnoreCase(r2v));
		assertTrue(s1.isDone());
		assertTrue(s2.isDone());
		assertTrue(r1.isDone());
		assertTrue(r2.isDone());
	}

	/***************************************
	 * Test of {@link Condition} step.
	 */
	@Test
	public void testCondition()
	{
		assertBooleanInput(
			"true",
			"false",
			Coroutine.first(
				doIfElse(b -> b, supply(() -> "true"), supply(() -> "false"))));

		assertBooleanInput(
			"true",
			"false",
			Coroutine.first(
				doIf((Boolean b) -> b, supply(() -> "true")).orElse(
					supply(() -> "false"))));

		assertBooleanInput(
			"true",
			"false",
			Coroutine.first(apply((Boolean b) -> b.toString()))
			.then(apply(s -> Boolean.valueOf(s)))
			.then(
				doIfElse(b -> b, supply(() -> "true"), supply(() -> "false"))));

		assertBooleanInput(
			"true",
			"false",
			Coroutine.first(apply((Boolean b) -> b.toString()))
			.then(apply(s -> Boolean.valueOf(s)))
			.then(
				doIf((Boolean b) -> b, supply(() -> "true")).orElse(
					supply(() -> "false"))));

		assertBooleanInput(
			"true",
			null,
			Coroutine.first(doIf(b -> b, supply(() -> "true"))));
	}

	/***************************************
	 * Test of {@link Iteration} step.
	 */
	@Test
	public void testIteration()
	{
		Coroutine<String, List<String>> cr =
			Coroutine.first(apply((String s) ->
		 						Arrays.asList(s.split(","))))
					 .then(
		 				forEach(
		 					apply((String s) -> s.toUpperCase()),
		 					() -> new LinkedList<>()));

		Continuation<?> ca = cr.runAsync("a,b,c,d");
		Continuation<?> cb = cr.runBlocking("a,b,c,d");

		assertEquals(Arrays.asList("A", "B", "C", "D"), ca.getResult());
		assertEquals(Arrays.asList("A", "B", "C", "D"), cb.getResult());
	}

	/***************************************
	 * Test of coroutines with multiple steps.
	 */
	@Test
	public void testMultiStep()
	{
		Coroutine<String, Integer> cr =
			Coroutine.first(apply((String s) -> s + 5))
					 .then(apply(s -> s.replaceAll("\\D", "")))
					 .then(apply(s -> Integer.valueOf(s)));

		Continuation<Integer> ca = cr.runAsync("test1234");
		Continuation<Integer> cb = cr.runBlocking("test1234");

		assertEquals(Integer.valueOf(12345), ca.getResult());
		assertEquals(Integer.valueOf(12345), cb.getResult());
		assertTrue(ca.isDone());
		assertTrue(cb.isDone());
	}

	/***************************************
	 * Test of coroutines with a single step.
	 */
	@Test
	public void testSingleStep()
	{
		Coroutine<String, String> cr =
			Coroutine.first(apply((String s) -> s.toUpperCase()));

		Continuation<String> ca = cr.runAsync("test");
		Continuation<String> cb = cr.runBlocking("test");

		assertEquals("TEST", ca.getResult());
		assertEquals("TEST", cb.getResult());
		assertTrue(ca.isDone());
		assertTrue(cb.isDone());
	}

	/***************************************
	 * Asserts the results of executing a {@link Coroutine} with a boolean
	 * input.
	 *
	 * @param sTrueResult  The expected result for a TRUE input
	 * @param sFalseResult The expected result for a FALSE input
	 * @param cr           The coroutine
	 */
	private void assertBooleanInput(String					   sTrueResult,
									String					   sFalseResult,
									Coroutine<Boolean, String> cr)
	{
		Continuation<String> cat = cr.runAsync(true);
		Continuation<String> caf = cr.runAsync(false);
		Continuation<String> cbt = cr.runBlocking(true);
		Continuation<String> cbf = cr.runBlocking(false);

		assertEquals(sTrueResult, cat.getResult());
		assertEquals(sTrueResult, cbt.getResult());
		assertEquals(sFalseResult, caf.getResult());
		assertEquals(sFalseResult, cbf.getResult());
		assertTrue(cat.isDone());
		assertTrue(caf.isDone());
		assertTrue(cbt.isDone());
		assertTrue(cbf.isDone());
	}
}
