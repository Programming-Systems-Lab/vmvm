package edu.columbia.cs.psl.vmvm;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.rits.cloning.Cloner;

/**
 * 
 * @author jon
 * 
 */
public class COWAInterceptor implements Constants {
	private static Logger logger = Logger.getLogger(COWAInterceptor.class);
	private static Cloner deepCloner = new Cloner();

	public static Object readAndCOAIfNecessary(Object theOwner, Object theFieldValue, Object callee) {
		Object r = doCopy(AbstractLazyCloningInterceptor.getRootCallee(), theFieldValue);
		int childNum = AbstractInterceptor.getThreadChildId();
		logger.info(childNum + "Copying on " +r);
		synchronized (pointsTo) {
			if(!pointsTo.containsKey(childNum))
				pointsTo.put(childNum, new IdentityHashMap<Object, Object>());
		}
		IdentityHashMap<Object, Object> myPointsTo = pointsTo.get(childNum);
		
		final List<Field> fields = allFields(r.getClass());
		for (final Field field : fields) {
			final int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers)) {
				try {
					final Object fieldObject = field.get(r);
					synchronized (myPointsTo) {
						if(myPointsTo.containsKey(fieldObject))
							field.set(r, myPointsTo.get(fieldObject));						
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		myPointsTo.put(theFieldValue, r);
		return r;
	}

	public static void setIsAClonedObject(Object obj) {
		try {
			obj.getClass().getField(BEEN_CLONED_FIELD).setBoolean(obj, true);
			obj.getClass().getField(CHILD_FIELD).setInt(obj, AbstractLazyCloningInterceptor.getThreadChildId());
		} catch (Exception ex) {
//			logger.error("Unable to set cloned on " + obj, ex);
		}
	}
	private static void setFieldCloned(Object obj, Field f)
	{
		try {
			obj.getClass().getField(f.getName()+BEEN_CLONED_FIELD).setBoolean(obj, true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static boolean isAClonedObject(Object obj) {
		try {
			return obj.getClass().getField(BEEN_CLONED_FIELD).getBoolean(obj);
		} catch (Exception ex) {
			return false;
		}
	}

	public static void setAndCOWIfNecessary(Object theOwner, Object value, Object callee, String owner, String name, String desc) {

	}

	/**
	 * Will perform a minimal deep clone of the object graph started at root
	 * such that leaf is cloned.
	 * 
	 * @param root
	 * @param leaf
	 */
	public static Object doCopy(Object root, Object leaf) {
		Object newLeaf = deepCloner.shallowClone(leaf);
		setIsAClonedObject(leaf);
		traverseGraphAndReplace(root, leaf, newLeaf);
		return newLeaf;
	}

	private static List<Field> allFields(final Class<?> c) {
		List<Field> l = fieldsCache.get(c);
		if (l == null) {
			l = new LinkedList<Field>();
			final Field[] fields = c.getDeclaredFields();
			addAll(l, fields);
			Class<?> sc = c;
			while ((sc = sc.getSuperclass()) != Object.class && sc != null) {
				addAll(l, sc.getDeclaredFields());
			}
			fieldsCache.putIfAbsent(c, l);
		}
		return l;
	}
	private final static ConcurrentHashMap<Integer,IdentityHashMap<Object, Object>> pointsTo = new ConcurrentHashMap<Integer, IdentityHashMap<Object,Object>>();
	private final static ConcurrentHashMap<Class<?>, List<Field>> fieldsCache = new ConcurrentHashMap<Class<?>, List<Field>>();

	private static void addAll(final List<Field> l, final Field[] fields) {
		for (final Field field : fields) {
			if (!field.isAccessible())
				field.setAccessible(true);
			l.add(field);
		}
	}

	private static void traverseGraphAndReplace(Object root, Object leaf, Object newLeaf) {
//		logger.info(root);
//		logger.info(leaf);
//		System.out.println("Searching for obj " + leaf + " on " + root + (isAClonedObject(root) ? " (cloned) " : " not cloned"));
		if (root == null)
			return;
		if (!isAClonedObject(root))
			return;
		Class<?> clz = root.getClass();
//		logger.info(clz);
		if (clz.isArray()) {
			final int length = Array.getLength(root);
			for (int i = 0; i < length; i++) {
				final Object v = Array.get(root, i);
				if (isAClonedObject(v)) {
					if (v == leaf)
						Array.set(root, i, newLeaf);
					else
						traverseGraphAndReplace(v, leaf, newLeaf);
				}
			}
		}
		final List<Field> fields = allFields(clz);
		for (final Field field : fields) {
			final int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers)) {
				try {
					final Object fieldObject = field.get(root);
						if (fieldObject == leaf)
						{
//							System.out.println("Found obj " + fieldObject + " on " + root);
							setFieldCloned(root, field);
							field.set(root, newLeaf);
						}
						else if(isAClonedObject(fieldObject))
							traverseGraphAndReplace(fieldObject, leaf, newLeaf);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}
