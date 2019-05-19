package edu.columbia.cs.psl.vmvm.runtime.inst;

import java.edu.columbia.cs.psl.vmvm.runtime.ReflectionWrapper;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class PutStaticHelperMV extends MethodVisitor {
	private String className;
	private boolean fixLdcClass;
	public PutStaticHelperMV(MethodVisitor mv, String className, boolean fixLdcClass) {
		super(Opcodes.ASM5, mv);
		this.className = className;
		this.fixLdcClass = fixLdcClass;
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if(opcode == Opcodes.PUTSTATIC && owner.equals(className))
		{
			if (fixLdcClass) {
				super.visitLdcInsn(owner.replace("/", "."));
				super.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			} else
				super.visitLdcInsn(Type.getObjectType(owner));
			super.visitLdcInsn(name);
			Type t = Type.getType(desc);
			switch(t.getSort()){
				case Type.ARRAY:
				case Type.OBJECT:
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), "putStaticField", "(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;)V",false);
					break;
				default:
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), "putStaticField", "("+t.getDescriptor()+"Ljava/lang/Class;Ljava/lang/String;)V",false);
					break;
			}

		}
		else {
			super.visitFieldInsn(opcode, owner, name, desc);
		}
	}
}
