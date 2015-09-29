package edu.columbia.cs.psl.vmvm.testsupport;

import java.lang.reflect.Field;

import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;
import edu.columbia.cs.psl.vmvmtest.classes.ClassWithOneSField;
import edu.columbia.cs.psl.vmvmtest.classes.IFace;
import edu.columbia.cs.psl.vmvmtest.classes.SubClassWithAnotherSField;

public class FieldGetter {
	public static int getFooWithoutInit()
	{
		return ClassWithOneSField.foo;
	}
	public static int getBarWithoutInit()
	{
		return SubClassWithAnotherSField.bar;
	}
	public static ClassWithOneSField getInstWithoutInit() {
		try{
		Field f = IFace.class.getDeclaredField("inst");
		return (ClassWithOneSField) ((MutableInstance) f.get(null)).get();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		return null;
	}
}
