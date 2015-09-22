package edu.columbia.cs.psl.vmvmtest;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class ReoptimizeITCase {
	@Test
	public void testReoptimize() throws Exception {
		for (int i = 0; i < 10; i++) {
			foo();
			Reinitializer.markAllClassesForReinit();
			foo();
		}
	}

	private static void foo() throws IllegalArgumentException, IllegalAccessException {
		OtherClass.foo = 5;
	}
}
