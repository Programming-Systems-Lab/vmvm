package edu.columbia.cs.psl.vmvm.runtime;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class JUnitInterceptingClassVisitor extends ClassVisitor {
	/*
	org.junit.internal.requests.ClassRequest(Class, boolean) <-- here
	 */
	public JUnitInterceptingClassVisitor(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (name.equals("<init>") && desc.equals("(Ljava/lang/Class;Z)V")) {
			mv = new MethodVisitor(Opcodes.ASM5, mv) {
				boolean intercepted = false;

				@Override
				public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
					super.visitMethodInsn(opcode, owner, name, desc, itf);

					if (!intercepted && name.equals("<init>")) {
						intercepted = true;
						super.visitVarInsn(Opcodes.ALOAD, 1);
						super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Reinitializer.class), "newTestClassHit", "(Ljava/lang/Class;)V", false);
					}
				}
			};
		}
		return mv;
	}
}
