package edu.columbia.cs.psl.test.vmvm;

import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class SystemPropertiesITCase {
	@Test
	public void testSystemProps() throws Exception {
		System.setProperty("foo", "bar");
		Reinitializer.markAllClassesForReinit();
		assertNull(System.getProperty("foo"));
		System.out.println(System.getProperty("foo"));
	}
}
