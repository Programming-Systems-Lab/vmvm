package edu.columbia.cs.psl.vmvmtest;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Test;

public class ReflectionHidingITCase {

	static EvilInterface foo;
	static String baz;
	@Test
	public void testFieldsHidden() throws Exception {
		Field[] f = ReflectionHidingITCase.class.getDeclaredFields();
		System.out.println(Arrays.toString(f));
		ReflectionHidingITCase.class.getDeclaredField("baz").set(null, "haha");
		baz = (String) ReflectionHidingITCase.class.getDeclaredField("baz").get(null);
		System.out.println(baz);
	}
}
