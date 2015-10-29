package edu.columbia.cs.psl.test.vmvm;

import java.util.HashMap;

public interface EvilInterface {
	public static final HashMap<Object, Object> baz = new HashMap<Object, Object>();
	public void getBaz();
}
