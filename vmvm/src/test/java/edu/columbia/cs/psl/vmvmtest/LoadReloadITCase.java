package edu.columbia.cs.psl.vmvmtest;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class LoadReloadITCase {
	void foo()
	{
		int x = OtherOtherClass.baz();
		x = OtherOtherClass.baz();
		x = OtherOtherClass.baz();
		x = OtherOtherClass.baz();
		x = OtherOtherClass.baz();
		System.out.println(x);

	}
	@Test
	public void testLoadReloadReload() throws Exception {
		foo();
		OtherClass.foo =15;
		Reinitializer.markAllClassesForReinit();
		foo();
		OtherClass.foo =15;
		Reinitializer.markAllClassesForReinit();
		OtherClass.foo =15;
		Reinitializer.markAllClassesForReinit();
		Reinitializer.markAllClassesForReinit();
		foo();
		
	}
}
