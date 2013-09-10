package edu.columbia.cs.psl.vmvm;

import java.net.URLClassLoader;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class InstrumenterClassWriter extends ClassWriter {
	private static Logger logger = Logger.getLogger(InstrumenterClassWriter.class);
	private ClassLoader loader;
	public InstrumenterClassWriter(ClassReader classReader, int flags, ClassLoader loader) {
		super(classReader, flags);
		this.loader = loader;
	}
	
	public InstrumenterClassWriter(int flags, URLClassLoader loader) {
		super(flags);
		this.loader=loader;
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		Class<?> c, d;
        try {
            c = Class.forName(type1.replace('/', '.'), false, loader);
            d = Class.forName(type2.replace('/', '.'), false, loader);
        } catch (ClassNotFoundException e) {
        	logger.debug("Error while finding common super class for " + type1 +"; " + type2,e);
        	return "java/lang/Object";
//        	throw new RuntimeException(e);
        }
        if (c.isAssignableFrom(d)) {
            return type1;
        }
        if (d.isAssignableFrom(c)) {
            return type2;
        }
        if (c.isInterface() || d.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                c = c.getSuperclass();
            } while (!c.isAssignableFrom(d));
            return c.getName().replace('.', '/');
        }
	}
}
