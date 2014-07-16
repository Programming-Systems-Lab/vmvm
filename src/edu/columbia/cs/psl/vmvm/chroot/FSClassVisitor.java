package edu.columbia.cs.psl.vmvm.chroot;

import java.util.HashMap;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.ClassNode;

public class FSClassVisitor extends ClassVisitor {
	
	private String className;
	private HashMap<String, MethodInstance> methodLookupCache;
	private HashMap<String, ClassInstance> classLookupCache;

	public FSClassVisitor(int api, ClassVisitor cv, HashMap<String, MethodInstance> lookupCache, HashMap<String, ClassInstance> classMap) {
		super(api, cv);
		this.methodLookupCache = lookupCache;
		this.classLookupCache = classMap;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		classLookupCache.put(name, new ClassInstance(name, superName, interfaces));
	}

	/**
	 * We are seeing method A.x for the first time. Add it to methodMap.
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		MethodInstance mi = new MethodInstance(name, desc, this.className, access);
		methodLookupCache.put(mi.getFullName(), mi);
		mi = methodLookupCache.get(mi.getFullName());
		if (FSDetector.fsMethods.contains(mi.getFullName()))
			mi.setFSTaint(true);
		
		ClassNode cn = FSDetector.classInfo.get(className);
		if(cn != null)
		{
			if(cn.interfaces != null)
				for(Object o : cn.interfaces)
				{
					if (FSDetector.fsMethods.contains(o.toString()+"."+name+desc))
					{
						mi.setFSTaint(true);		
					}
				}
			String parent = cn.superName;
			while(parent != null && !parent.equals("java/lang/Object"))
			{
				if (FSDetector.fsMethods.contains(parent+"."+name+desc))
				{
					mi.setFSTaint(true);		
				}
				cn = FSDetector.classInfo.get(parent);
				if(cn != null && cn.interfaces != null)
				{
					for(Object o : cn.interfaces)
					{
						if (FSDetector.fsMethods.contains(o.toString()+"."+name+desc))
						{
							mi.setFSTaint(true);		
						}
					}
				}
				if(cn == null)
					parent = null;
				else
					parent = cn.superName;
			}
		}
		return null;
	}

}
