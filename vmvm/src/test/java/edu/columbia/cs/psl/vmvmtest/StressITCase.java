package edu.columbia.cs.psl.vmvmtest;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class StressITCase {
	public static int foo = 5;

//	public void reinitCalledThroughMethods() throws Exception {
//		foo = 3;
//		foo();
//		assertEquals(3, foo());
//		Reinitializer.markAllClassesForReinit();
//		System.out.println("done1");
//		assertEquals(5, foo());
//		foo = 4;
//		assertEquals(4, foo());
//	}
//
//	@Test
//	public void test10Execs() throws Exception {
//		for(int i = 0; i < 100; i++)
//		{
//			reinitCalledThroughMethods();
//		}
//	}

//	@Test
//	public void testInterface() throws Exception {
//		EvilInterface.baz.put("bar", "baz");
//		assertEquals(1, EvilInterface.baz.size());
//		Reinitializer.markAllClassesForReinit();
//		assertEquals(0, EvilInterface.baz.size());
//	}

	static int foo() {
		return foo;
	}
}
