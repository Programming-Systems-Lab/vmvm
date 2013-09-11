package edu.columbia.cs.psl.vmvm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldReflectionWrapper {

	public static Field tryToInit(Field f, Object obj)
	{
		if (Modifier.isStatic(f.getModifiers()))
			ReflectionWrapper.tryToInit(f.getDeclaringClass());
		return f;
	}
	
	public static Field tryToInit(Field f) throws IllegalArgumentException, IllegalAccessException {
		if (Modifier.isStatic(f.getModifiers()))
			ReflectionWrapper.tryToInit(f.getDeclaringClass());
		return f;
	}
}
