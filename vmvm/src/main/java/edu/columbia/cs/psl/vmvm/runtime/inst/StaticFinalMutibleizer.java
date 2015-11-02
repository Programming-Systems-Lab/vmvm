package edu.columbia.cs.psl.vmvm.runtime.inst;

import java.util.HashSet;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;
import edu.columbia.cs.psl.vmvm.runtime.VMVMClassFileTransformer;

public class StaticFinalMutibleizer extends InstructionAdapter implements Opcodes {

	private boolean skipFrames;
	private HashSet<String> finalFields;
	public StaticFinalMutibleizer(MethodVisitor mv, HashSet<String> finalFields, boolean skipFrames) {
		super(Opcodes.ASM5, mv);
		this.skipFrames = skipFrames;
		this.finalFields = finalFields;
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		if (!skipFrames)
			super.visitFrame(type, nLocal, local, nStack, stack);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		Type originalType = Type.getType(desc);

		if ((opcode == GETSTATIC || opcode == PUTSTATIC) && !VMVMClassFileTransformer.isIgnoredClass(owner)
				&& !name.equals(Constants.VMVM_NEEDS_RESET) && finalFields.contains(name)) {
			if (opcode == GETSTATIC) {
				super.visitFieldInsn(opcode, owner, name, Type.getDescriptor(MutableInstance.class));
				super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(MutableInstance.class), "get", "()Ljava/lang/Object;", false);
				if (originalType.getSort() == Type.OBJECT || originalType.getSort() == Type.ARRAY) {
					super.visitTypeInsn(CHECKCAST, originalType.getInternalName());
				} else//primitive needs to be cast to the boxed type then unboxed
				{
					String checkCastTo = null;
					switch (originalType.getSort()) {
					case Type.BYTE:
						checkCastTo = Type.getInternalName(Byte.class);
						break;
					case Type.BOOLEAN:
						checkCastTo = Type.getInternalName(Boolean.class);
						break;
					case Type.SHORT:
						checkCastTo = Type.getInternalName(Short.class);
						break;
					case Type.CHAR:
						checkCastTo = Type.getInternalName(Character.class);
						break;
					case Type.INT:
						checkCastTo = Type.getInternalName(Integer.class);
						break;
					case Type.FLOAT:
						checkCastTo = Type.getInternalName(Float.class);
						break;
					case Type.LONG:
						checkCastTo = Type.getInternalName(Long.class);
						break;
					case Type.DOUBLE:
						checkCastTo = Type.getInternalName(Double.class);
						break;
					}
					super.visitTypeInsn(CHECKCAST, checkCastTo);
					Utils.unbox(this,originalType);
				}
			} else {
				if (!(originalType.getSort() == Type.OBJECT || originalType.getSort() == Type.ARRAY)) {
					Utils.box(this,originalType);
				}
				super.visitFieldInsn(GETSTATIC, owner, name, Type.getDescriptor(MutableInstance.class));
				super.visitInsn(SWAP);
				super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(MutableInstance.class), "put", "(Ljava/lang/Object;)V", false);
			}
		} else
			super.visitFieldInsn(opcode, owner, name, desc);
	}
}
