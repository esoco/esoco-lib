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

import org.junit.Test;

import static de.esoco.lib.concurrent.coroutine.ChannelId.stringChannel;
import static de.esoco.lib.concurrent.coroutine.step.ChannelReceive.receive;
import static de.esoco.lib.concurrent.coroutine.step.ChannelSend.send;
import static de.esoco.lib.concurrent.coroutine.step.CodeExecution.apply;
import static de.esoco.lib.concurrent.coroutine.step.CodeExecution.consume;
import static de.esoco.lib.concurrent.coroutine.step.CodeExecution.run;
import static de.esoco.lib.concurrent.coroutine.step.CodeExecution.supply;

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
	 * Test asynchronous channel communication
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

		System.out.printf("%s\n", cs);
		System.out.printf("%s\n", cr1);
		System.out.printf("%s\n", cr2);

		Continuation<?> r1 = cr1.runAsync(ctx, null);
		Continuation<?> r2 = cr2.runAsync(ctx, null);

		Continuation<?> s1 = cs.runAsync(ctx, "123");
		Continuation<?> s2 = cs.runAsync(ctx, "456");

		assertEquals("123test", s1.getResult());
		assertEquals("456test", s2.getResult());
		assertEquals("123TEST", r1.getResult());
		assertEquals("456test", r2.getResult());
		assertTrue(s1.isDone());
		assertTrue(s2.isDone());
		assertTrue(r1.isDone());
		assertTrue(r2.isDone());
	}

	/***************************************
	 * Tests a coroutine with multiple steps.
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
	 * Test of {@link Coroutine#runAsync()}.
	 */
	public void testSimpleCoroutine()
	{
		Coroutine<String, String> cr =
			Coroutine.first(apply((String s) -> s.toUpperCase()))
					 .then(apply(s -> s.replace('C', '_')))
					 .then(apply(s -> s.replaceAll("[A-Z]", "!")))
					 .then(consume(s ->
		 						System.out.printf("IN: %s\n", s)))
					 .then(run(() -> System.out.printf("RUNNING!\n")))
					 .then(supply(() -> "FINISHED"));

		System.out.printf("BLOCK: %s\n", cr.runBlocking("abcde").getResult());

		Continuation<?> c =
			cr.runAsync("abcde").then(v -> System.out.printf("ASYNC: %s\n", v));
	}

	/***************************************
	 * Tests a coroutine with a single step.
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
}
