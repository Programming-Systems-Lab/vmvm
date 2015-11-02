package edu.columbia.cs.psl.test.vmvm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.Field;

import org.junit.Test;

import edu.columbia.cs.psl.test.vmvm.classes.ClassWithNoInterfaces;
import edu.columbia.cs.psl.test.vmvm.classes.ClassWithOneSField;
import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;
import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;
import edu.columbia.cs.psl.vmvm.testsupport.FieldGetter;

public class ReflectionITCase {
	@Test
	public void testNumberOfInterfaces() throws Exception {
		assertEquals(1, ClassWithOneSField.class.getInterfaces().length);
		assertEquals(0, ClassWithNoInterfaces.class.getInterfaces().length);
	}
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
		Class.forName("edu.columbia.cs.psl.test.vmvm.classes.ClassWithOneSField", true, ReflectionITCase.class.getClassLoader());
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
		assertEquals(2, ClassWithOneSField.class.getDeclaredFields().length);
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
