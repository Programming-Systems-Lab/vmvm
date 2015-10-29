package edu.columbia.cs.psl.vmvm.runtime.inst;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReflectionFixingCV extends ClassVisitor {
	public ReflectionFixingCV(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}

	private boolean fixLdcClass = false;
	private String className;

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
		this.className = name;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		return new ReflectionFixingMV(mv, fixLdcClass, className);
	}
}
