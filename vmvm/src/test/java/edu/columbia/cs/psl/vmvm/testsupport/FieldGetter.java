package edu.columbia.cs.psl.vmvm.testsupport;

import java.lang.reflect.Field;

import edu.columbia.cs.psl.test.vmvm.classes.ClassWithOneSField;
import edu.columbia.cs.psl.test.vmvm.classes.IFace;
import edu.columbia.cs.psl.test.vmvm.classes.SubClassWithAnotherSField;
import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;

public class FieldGetter {
	public static int getFooWithoutInit() {
		try {
			Field f = ClassWithOneSField.class.getDeclaredField("foo");
			return (Integer) ((MutableInstance) f.get(null)).get();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return -1;
	}

	public static int getBarWithoutInit() {
	       try {
	            Field f = SubClassWithAnotherSField.class.getDeclaredField("bar");
	            return (Integer) ((MutableInstance) f.get(null)).get();
	        } catch (Throwable t) {
	            t.printStackTrace();
	        }
	        return -1;
	}

	public static ClassWithOneSField getInstWithoutInit() {
		try {
			Field f = IFace.class.getDeclaredField("inst");
			return (ClassWithOneSField) ((MutableInstance) f.get(null)).get();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
}
