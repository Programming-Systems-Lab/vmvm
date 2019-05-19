package edu.columbia.cs.psl.vmvm.runtime.inst;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.edu.columbia.cs.psl.vmvm.runtime.ReflectionWrapper;

public class ReflectionFixingMV extends InstructionAdapter implements Opcodes {

	private boolean oldClassldcHack;
	private String className;

	public ReflectionFixingMV(MethodVisitor mv, boolean oldClassldcHack, String className) {
		super(Opcodes.ASM5, mv);
		this.className = className;
		this.oldClassldcHack = oldClassldcHack;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		if (Type.getInternalName(Class.class).equals(owner) && name.equals("newInstance")) {
			//        	owner = Type.getInternalName(ReflectionWrapper.class);
			//        	opcode = INVOKESTATIC;
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), "preNewInstance", "(Ljava/lang/Class;)Ljava/lang/Class;", false);
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
		} else if (owner.equals("java/lang/reflect/Field") && name.equals("getType")) {
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), name, "(Ljava/lang/reflect/Field;)Ljava/lang/Class;", false);

		} else if ((Type.getInternalName(Class.class).equals(owner) && name.equals("forName"))) {
			owner = Type.getInternalName(ReflectionWrapper.class);
			if (desc.contains("ClassLoader"))
				super.visitMethodInsn(opcode, owner, name, desc, itfc);
			else {
				if (!oldClassldcHack)// java 5+
					super.visitLdcInsn(Type.getType("L" + className + ";"));
				else {
					super.visitLdcInsn(className.replace("/", "."));
					super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
				}
				super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
				super.visitMethodInsn(opcode, owner, name, desc.substring(0, desc.indexOf(")")) + Type.getDescriptor(ClassLoader.class) + desc.substring(desc.indexOf(")")), itfc);
			}
		} else if ((Type.getInternalName(Class.class).equals(owner) && (name.equals("getDeclaredFields")||name.equals("getInterfaces") || name.equals("getDeclaredMethods") || name.equals("getFields") || name.equals("getMethods")))) {
			desc = "(" + Type.getDescriptor(Class.class) + ")" + Type.getReturnType(desc).getDescriptor();
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), name, desc, false);
		} else if ((Type.getInternalName(Method.class).equals(owner) && name.equals("invoke"))) {
			owner = Type.getInternalName(ReflectionWrapper.class);
			opcode = INVOKESTATIC;

			super.visitMethodInsn(opcode, owner, name, "(Ljava/lang/reflect/Method;" + desc.substring(1), false);
		} else if ((Type.getInternalName(Method.class).equals(owner) && name.equals("getModifiers"))) {
			owner = Type.getInternalName(ReflectionWrapper.class);
			opcode = INVOKESTATIC;

			super.visitMethodInsn(opcode, owner, name, "(Ljava/lang/reflect/Method;" + desc.substring(1), false);
		} else if ((Type.getInternalName(Class.class).equals(owner) && name.equals("getModifiers"))) {
			owner = Type.getInternalName(ReflectionWrapper.class);
			opcode = INVOKESTATIC;

			super.visitMethodInsn(opcode, owner, name, "(Ljava/lang/Class;" + desc.substring(1), false);
		} else if ((Type.getInternalName(Field.class).equals(owner) && name.equals("getModifiers"))) {
			owner = Type.getInternalName(ReflectionWrapper.class);
			opcode = INVOKESTATIC;

			super.visitMethodInsn(opcode, owner, name, "(Ljava/lang/reflect/Field;" + desc.substring(1), false);
		} else if ((Type.getInternalName(Field.class).equals(owner) && (name.equals("getBoolean") || name.equals("getByte") || name.equals("getChar") || name.equals("getDouble")
				|| name.equals("getFloat") || name.equals("getInt") || name.equals("getLong") || name.equals("get")))) {
			if (name.equals("get"))
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), "get", "(Ljava/lang/reflect/Field;Ljava/lang/Object;)Ljava/lang/Object;", false);
			else
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), name, "(Ljava/lang/reflect/Field;" + desc.substring(1), false);
		} else if (Type.getInternalName(Field.class).equals(owner)
				&& (name.equals("setBoolean") || name.equals("setByte") || name.equals("setChar") || name.equals("setDouble") || name.equals("setFloat") || name.equals("setInt")
						|| name.equals("setLong") || name.equals("set"))) {

			if (name.equals("set"))
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), "set", "(Ljava/lang/reflect/Field;Ljava/lang/Object;Ljava/lang/Object;)V", false);
			else {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), name, "(Ljava/lang/reflect/Field;" + desc.substring(1), false);
			}

		} else {
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
		}
	}
}
