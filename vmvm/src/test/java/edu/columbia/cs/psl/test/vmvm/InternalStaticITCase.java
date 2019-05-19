package edu.columbia.cs.psl.test.vmvm;

import static org.junit.Assert.assertSame;

import java.util.Locale;

import org.junit.Test;

import java.edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class InternalStaticITCase {
	@Test 
	public void testDefaultLocale() throws Exception {
		Locale def = Locale.getDefault();
		Locale.setDefault(Locale.GERMAN);
		assertSame(Locale.GERMAN,Locale.getDefault());
		Reinitializer.markAllClassesForReinit();
		assertSame(def,Locale.getDefault());
	}
}
