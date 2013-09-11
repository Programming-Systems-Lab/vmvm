package edu.columbia.cs.psl.vmvm;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;

public class ReflectionWrapper {
	public static Method[] getDeclaredMethods(Class<?> clazz)
	{
		Method[] r = clazz.getDeclaredMethods();
		return r;
	}
	public static Method[] getMethods(Class<?> clazz)
	{
		Method[] r = clazz.getMethods();
		return r;
	}
	public static Field[] getDeclaredFields(Class<?> clazz)
	{
		Field[] r = clazz.getDeclaredFields();
		ArrayList<Field> ret = new ArrayList<>(r.length);
 		for(Field f : r)
		{
			if(!f.getName().startsWith("_vmvm") && !f.getName().endsWith("_vmvm_acc_logged") && !f.getName().endsWith("_vmvm_") && !f.getName().equals("vmvm_needs_reset"))
				ret.add(f);
		}
 		r = new Field[ret.size()];
 		r = ret.toArray(r);
 		return r;
	}
	public static Field[] getFields(Class<?> clazz)
	{
		Field[] r = clazz.getFields();
		ArrayList<Field> ret = new ArrayList<>(r.length);
 		for(Field f : r)
		{
			if(!f.getName().startsWith("_vmvm") && !f.getName().endsWith("_vmvm_acc_logged") && !f.getName().endsWith("_vmvm_") && !f.getName().equals("vmvm_needs_reset"))
				ret.add(f);
		}
 		r = new Field[ret.size()];
 		r = ret.toArray(r);
 		return r;
	}
	public static Class<?> forName(String name, ClassLoader loader) throws ClassNotFoundException {
		Class<?> ret = Class.forName(name, true, loader);
		tryToInit(ret);
		return ret;
	}

	public static Class<?> preNewInstance(Class<?> clazz) throws InstantiationException, IllegalAccessException {
		tryToInit(clazz);
		return clazz;
	}

	public static Class<?> forName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
		Class<?> ret = Class.forName(name, initialize, loader);
		if (initialize) {
			tryToInit(ret);
		}
		return ret;
	}

	public static Object invoke(Method m, Object owner, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (Modifier.isStatic(m.getModifiers()))
			tryToInit(m.getDeclaringClass());
		if(!m.isAccessible())
			m.setAccessible(true);
		return m.invoke(owner, args);
	}

	public static void tryToInit(Class<?> clazz) {

		//			if(inited.contains(clazz))
		//				return;
		//			inited.add(clazz);
		//			synchronized (clazz) {
		try {
			boolean val = clazz.getField(Constants.VMVM_NEEDS_RESET).getBoolean(null);
			if (val) {
				clazz.getMethod(Constants.VMVM_STATIC_RESET_METHOD).invoke(null);
			}
		} catch (Exception ex) {
//								if (!(ex instanceof NoSuchMethodException))
//									ex.printStackTrace();

		}
		//		}
	}
}
