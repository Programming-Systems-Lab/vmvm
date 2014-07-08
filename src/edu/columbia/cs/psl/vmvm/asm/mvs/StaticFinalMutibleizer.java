package edu.columbia.cs.psl.vmvm.asm.mvs;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.asm.JUnitResettingClassVisitor;
import edu.columbia.cs.psl.vmvm.struct.MutableInstance;

public class StaticFinalMutibleizer extends GeneratorAdapter implements Opcodes {
	private String mName;
	private String className;

	public StaticFinalMutibleizer(MethodVisitor mv, int access, String className, String name, String desc) {
		super(mv, access, name, desc);
		this.mName = name;
		this.className = className;
	}
	private boolean isFinalField(String owner, String name)
	{
		if(name.equals(Constants.VMVM_NEEDS_RESET))
			return false;
		if(name.equals(Constants.VMVM_RESET_IN_PROGRESS))
			return false;
		if(JUnitResettingClassVisitor.shouldIgnoreClass(owner))
			return false;
		if(Instrumenter.mutablizedFields.containsKey(owner + "." + name))
			return true;
		ClassNode owningNode = Instrumenter.getClassNodeWithField(owner,name);
		if(owningNode == null)
			return false;
		else if((owningNode.access & Opcodes.ACC_INTERFACE) != 0){
			FieldNode fn = Instrumenter.getFieldNode(owner, name);
			if(fn.value == null)
				return true;
			return false;
		}
		return false;
	}
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if ((opcode == GETSTATIC || opcode == PUTSTATIC) && isFinalField(owner,name)) {
			Type originalType = Type.getType(desc);
			if (opcode == GETSTATIC) {
				super.visitFieldInsn(opcode, owner, name, Type.getDescriptor(MutableInstance.class));
				super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(MutableInstance.class), "get", "()Ljava/lang/Object;");
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
					unbox(originalType);
				}
			} else {
				if (!(originalType.getSort() == Type.OBJECT || originalType.getSort() == Type.ARRAY)) {
					super.box(originalType);
				}
				super.visitFieldInsn(GETSTATIC, owner, name, Type.getDescriptor(MutableInstance.class));
				super.visitInsn(SWAP);
				super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(MutableInstance.class), "put", "(Ljava/lang/Object;)V");
			}
		} else
			super.visitFieldInsn(opcode, owner, name, desc);
	}
}
