package edu.columbia.cs.psl.vmvmtest;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

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
