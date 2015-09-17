package edu.columbia.cs.psl.vmvm;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class ReinitializerITCase {
	public static int foo = 5;

	@Test
	public void testReinitCalledThroughMethods() throws Exception {
		foo = 3;
		foo();
		assertEquals(3, foo());
		Reinitializer.markAllClassesForReinit();
		assertEquals(5, foo());
		foo = 4;
		assertEquals(4, foo());
	}

	@Test
	public void testReinitCalledThroughFields() throws Exception {
		foo = 3;
		Reinitializer.markAllClassesForReinit();
		assertEquals(5, foo());
		foo = 4;
		assertEquals(4, foo());
	}

	@Test
	public void testInterface() throws Exception {
		EvilInterface.baz.put("bar", "baz");
		assertEquals(1, EvilInterface.baz.size());
		Reinitializer.markAllClassesForReinit();
		assertEquals(0, EvilInterface.baz.size());
	}

	static int foo() {
		return foo;
	}
}
