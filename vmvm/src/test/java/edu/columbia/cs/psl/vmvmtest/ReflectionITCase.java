package edu.columbia.cs.psl.vmvmtest;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;
import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;
import edu.columbia.cs.psl.vmvm.testsupport.FieldGetter;
import edu.columbia.cs.psl.vmvmtest.classes.ClassWithOneSField;

public class ReflectionITCase {
	@Test
	public void testReinitOnFieldAccess() throws Exception {
		Class<?> clz = ClassWithOneSField.class;
		Field f = clz.getDeclaredField("foo");
		assertEquals(5, FieldGetter.getFooWithoutInit());
		f.setInt(null, 4);
		assertEquals(4, FieldGetter.getFooWithoutInit());
		Reinitializer.markAllClassesForReinit();
		assertEquals(5, f.getInt(null));
	}

	@Test
	public void testReinitOnConstructorAccess() throws Exception {
		Class<?> clz = ClassWithOneSField.class;
		assertEquals(5, FieldGetter.getFooWithoutInit());
		ClassWithOneSField.foo = 2;
		Reinitializer.markAllClassesForReinit();
		clz.getConstructor().newInstance();
		assertEquals(5, FieldGetter.getFooWithoutInit());

	}

	@Test
	public void testReinitOnClassLoad() throws Exception {
		assertEquals(5, FieldGetter.getFooWithoutInit());
		ClassWithOneSField.foo = 2;
		Reinitializer.markAllClassesForReinit();
		Class.forName("edu.columbia.cs.psl.vmvmtest.classes.ClassWithOneSField", true, ReflectionITCase.class.getClassLoader());
		assertEquals(5, FieldGetter.getFooWithoutInit());
	}

	@Test
	public void testReinitOnStaticMethod() throws Exception {
		Class<?> clz = ClassWithOneSField.class;
		assertEquals(5, FieldGetter.getFooWithoutInit());
		ClassWithOneSField.foo = 2;
		Reinitializer.markAllClassesForReinit();
		clz.getDeclaredMethod("getFoo").invoke(null);
		assertEquals(5, FieldGetter.getFooWithoutInit());
	}

	@Test
	public void testCorrectNumberOfFields() throws Exception {
		for(Field f : ClassWithOneSField.class.getDeclaredFields())
		{
			assertNotEquals(f.getType(), MutableInstance.class);
		}
	}
	@Test
	public void testCorrectNumberOfMethods() throws Exception {
		assertEquals(2, ClassWithOneSField.class.getDeclaredMethods().length);
	}
}
