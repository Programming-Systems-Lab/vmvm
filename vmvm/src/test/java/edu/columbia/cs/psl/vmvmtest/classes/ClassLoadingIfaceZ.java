package edu.columbia.cs.psl.vmvmtest.classes;

import java.util.LinkedList;

import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;

public class ClassLoadingIfaceZ {
	public static final LinkedList<Class> foo = new LinkedList<Class>();
	static {
		try {
			foo.add(Class.forName("edu.columbia.cs.psl.vmvmtest.classes.LazyLoadedClass"));
		} catch (ClassNotFoundException e) {
//			foo = null;
			e.printStackTrace();
		}
	}
}
