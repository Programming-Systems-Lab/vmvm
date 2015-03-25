package edu.columbia.cs.psl.vmvm.asm;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.LocalVariablesSorter;

public interface MethodVisitorFactory {
	public MethodVisitor getMethodVisitor(MethodVisitor parent, int access, String name, String desc, String signature, String[] exceptions);
	public void addLocalVaribleSorter(LocalVariablesSorter lvs);
	public MethodVisitorFactory newInstance();
}
