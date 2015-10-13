package edu.columbia.cs.psl.vmvmtest;

import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class ReoptimizeITCase {
	@Test
	public void testReoptimize() throws Exception {
		for (int i = 0; i < 10; i++) {
			foo();
			foo();
			foo();
			foo();
			foo();
			foo();
			foo();
			foo();
			foo();
			foo();
			foo();
			foo();
			Reinitializer.markAllClassesForReinit();
			foo();
			foo();
			foo();
			foo();
			foo();
			foo();
			foo();
		}
	}

	private static void foo() throws IllegalArgumentException, IllegalAccessException {
		OtherClass.foo = 5;
	}
}
