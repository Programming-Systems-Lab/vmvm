package edu.columbia.cs.psl.vmvm.asm;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;

public class ClinitPrintingCV extends VMVMClassVisitor {

	public ClinitPrintingCV(int api, ClassVisitor cv, boolean useVMState) {
		super(api, cv, useVMState);
	}

	private String className;
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor smv = super.visitMethod(access, name, desc, signature, exceptions);
		if(name.equals("<clinit>"))
		{
			return new MethodVisitor(Opcodes.ASM5,smv) {
				@Override
				public void visitCode() {
					super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
					super.visitLdcInsn("clinit  rerunning>" + className);
					super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
					super.visitCode();
				}
				@Override
				public void visitInsn(int opcode) {
					if (opcode == Opcodes.RETURN) {
						super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
						super.visitLdcInsn("clinit finished rerunning>" + className);
						super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
					}
					super.visitInsn(opcode);
				}
			};
		}
		else
			return smv;
	}
	@Override
	public boolean hasExtraInterface() {
		return false;
	}

	@Override
	public byte[] getExtraInterface() {
		return null;
	}

}
