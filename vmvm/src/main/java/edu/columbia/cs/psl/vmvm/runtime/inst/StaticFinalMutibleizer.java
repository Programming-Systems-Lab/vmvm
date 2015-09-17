package edu.columbia.cs.psl.vmvm.runtime.inst;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;
import edu.columbia.cs.psl.vmvm.runtime.VMVMClassFileTransformer;

public class StaticFinalMutibleizer extends InstructionAdapter implements
		Opcodes {

	public StaticFinalMutibleizer(MethodVisitor mv) {
		super(Opcodes.ASM5, mv);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		Type originalType = Type.getType(desc);
		if ((opcode == GETSTATIC || opcode == PUTSTATIC)
				&& !VMVMClassFileTransformer.isIgnoredClass(owner)
				&& (originalType.getSort() == Type.ARRAY || originalType
						.getSort() == Type.OBJECT)) {
			if (opcode == GETSTATIC) {
				super.visitFieldInsn(opcode, owner, name,
						Type.getDescriptor(MutableInstance.class));
				super.visitMethodInsn(INVOKEVIRTUAL,
						Type.getInternalName(MutableInstance.class), "get",
						"()Ljava/lang/Object;", false);
				super.visitTypeInsn(CHECKCAST, originalType.getInternalName());
			} else {
				super.visitFieldInsn(GETSTATIC, owner, name,
						Type.getDescriptor(MutableInstance.class));
				super.visitInsn(SWAP);
				super.visitMethodInsn(INVOKEVIRTUAL,
						Type.getInternalName(MutableInstance.class), "put",
						"(Ljava/lang/Object;)V", false);
			}
		} else
			super.visitFieldInsn(opcode, owner, name, desc);
	}
}
