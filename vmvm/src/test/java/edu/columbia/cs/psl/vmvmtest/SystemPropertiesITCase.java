package edu.columbia.cs.psl.vmvmtest;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class SystemPropertiesITCase {
	@Test
	public void testSystemProps() throws Exception {
		System.setProperty("foo", "bar");
		Reinitializer.markAllClassesForReinit();
		assertNull(System.getProperty("foo"));
		System.out.println(System.getProperty("foo"));
	}
}
