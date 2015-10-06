package edu.columbia.cs.psl.vmvmtest;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;
import edu.columbia.cs.psl.vmvm.testsupport.FieldGetter;
import edu.columbia.cs.psl.vmvmtest.classes.ClassWithOneSField;
import edu.columbia.cs.psl.vmvmtest.classes.IFace;
import edu.columbia.cs.psl.vmvmtest.classes.SubClassWithAnotherSField;
import edu.columbia.cs.psl.vmvmtest.classes.TryCatchInInitClass;

public class ReinitializerITCase {
	public static int foo = 5;

	@Test
	public void testReinitDirectIface() throws Exception {
		ClassWithOneSField inst = IFace.inst;
		Reinitializer.markAllClassesForReinit();
		inst = ClassWithOneSField.inst;
		assertSame(IFace.inst, inst);
	}
	@Test
	public void testTryCatchInClinit() throws Exception {
		TryCatchInInitClass c = new TryCatchInInitClass();
		Reinitializer.markAllClassesForReinit();
		c = new TryCatchInInitClass();
	}
	@Test
	public void testDirectInit() throws Exception {
		ClassWithOneSField f = new  ClassWithOneSField();
		assertEquals(5, FieldGetter.getFooWithoutInit());
		ClassWithOneSField.foo = 3;
		Reinitializer.markAllClassesForReinit();
		f = new  ClassWithOneSField();
		assertEquals(5, FieldGetter.getFooWithoutInit());
		ClassWithOneSField.foo = 3;
		Reinitializer.markAllClassesForReinit();
		f = new  ClassWithOneSField(4);
		assertEquals(5, FieldGetter.getFooWithoutInit());
	}
	
	@Test
	public void testSubClassInit() throws Exception{
		SubClassWithAnotherSField f = new  SubClassWithAnotherSField();
		assertEquals(5, FieldGetter.getFooWithoutInit());
		assertEquals(4, FieldGetter.getBarWithoutInit());
		SubClassWithAnotherSField.bar = 3;
		ClassWithOneSField.foo = 3;
		Reinitializer.markAllClassesForReinit();
		f = new  SubClassWithAnotherSField();
		assertEquals(4, FieldGetter.getBarWithoutInit());
		assertEquals(5, FieldGetter.getFooWithoutInit());
		ClassWithOneSField.foo = 3;

		Reinitializer.markAllClassesForReinit();
		f = new  SubClassWithAnotherSField(5);
		assertEquals(5, FieldGetter.getFooWithoutInit());
	}	
	
	@Test
	public void testReinitCalledThroughMethods() throws Exception {
		foo = 3;
		foo();
		assertEquals(3, foo());
		Reinitializer.markAllClassesForReinit();
		assertEquals(5, foo());
		foo = 4;
		assertEquals(4, foo());
	}

	int getOtherClassFoo() {
		return OtherClass.foo;
	}

	@Test
	public void testReinitCalledThroughFields() throws Exception {
		OtherClass.foo = 3;
		Reinitializer.markAllClassesForReinit();
		assertEquals(5, getOtherClassFoo());
		OtherClass.foo = 4;
		assertEquals(4, getOtherClassFoo());
	}

	@After
	public void ensureThisClassIsReinited() {
		foo();
	}

	@Test
	public void testInterface() throws Exception {
		EvilInterface.baz.put("bar", "baz");
		assertEquals(1, EvilInterface.baz.size());
		Reinitializer.markAllClassesForReinit();
		assertEquals(0, EvilInterface.baz.size());
	}

	static int foo() {
		return foo;
	}

}
