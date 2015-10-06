package edu.columbia.cs.psl.vmvm.runtime.inst;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class Utils {
	private static final String CLDESC = "Ljava/lang/Class;";

	private static final Type BYTE_TYPE = Type.getObjectType("java/lang/Byte");

	private static final Type BOOLEAN_TYPE = Type.getObjectType("java/lang/Boolean");

	private static final Type SHORT_TYPE = Type.getObjectType("java/lang/Short");

	private static final Type CHARACTER_TYPE = Type.getObjectType("java/lang/Character");

	private static final Type INTEGER_TYPE = Type.getObjectType("java/lang/Integer");

	private static final Type FLOAT_TYPE = Type.getObjectType("java/lang/Float");

	private static final Type LONG_TYPE = Type.getObjectType("java/lang/Long");

	private static final Type DOUBLE_TYPE = Type.getObjectType("java/lang/Double");

	private static final Type NUMBER_TYPE = Type.getObjectType("java/lang/Number");

	private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");

	private static final Method BOOLEAN_VALUE = Method.getMethod("boolean booleanValue()");

	private static final Method CHAR_VALUE = Method.getMethod("char charValue()");

	private static final Method INT_VALUE = Method.getMethod("int intValue()");

	private static final Method FLOAT_VALUE = Method.getMethod("float floatValue()");

	private static final Method LONG_VALUE = Method.getMethod("long longValue()");

	private static final Method DOUBLE_VALUE = Method.getMethod("double doubleValue()");

	private static Type getBoxedType(final Type type) {
		switch (type.getSort()) {
		case Type.BYTE:
			return BYTE_TYPE;
		case Type.BOOLEAN:
			return BOOLEAN_TYPE;
		case Type.SHORT:
			return SHORT_TYPE;
		case Type.CHAR:
			return CHARACTER_TYPE;
		case Type.INT:
			return INTEGER_TYPE;
		case Type.FLOAT:
			return FLOAT_TYPE;
		case Type.LONG:
			return LONG_TYPE;
		case Type.DOUBLE:
			return DOUBLE_TYPE;
		}
		return type;
	}

	public static void box(MethodVisitor mv, final Type type) {
		if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
			return;
		}

		Type boxed = getBoxedType(type);
		mv.visitTypeInsn(Opcodes.NEW, boxed.getInternalName());
		if (type.getSize() == 2) {
			// Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
			mv.visitInsn(Opcodes.DUP_X2);
			mv.visitInsn(Opcodes.DUP_X2);
			mv.visitInsn(Opcodes.POP);
		} else {
			// p -> po -> opo -> oop -> o
			mv.visitInsn(Opcodes.DUP_X1);
			mv.visitInsn(Opcodes.SWAP);
		}
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, boxed.getInternalName(), "<init>", "(" + type.getDescriptor() + ")V", false);

	}

	public static void unbox(MethodVisitor mv, final Type type) {
		Type t = NUMBER_TYPE;
		Method sig = null;
		switch (type.getSort()) {
		case Type.VOID:
			return;
		case Type.CHAR:
			t = CHARACTER_TYPE;
			sig = CHAR_VALUE;
			break;
		case Type.BOOLEAN:
			t = BOOLEAN_TYPE;
			sig = BOOLEAN_VALUE;
			break;
		case Type.DOUBLE:
			sig = DOUBLE_VALUE;
			break;
		case Type.FLOAT:
			sig = FLOAT_VALUE;
			break;
		case Type.LONG:
			sig = LONG_VALUE;
			break;
		case Type.INT:
		case Type.SHORT:
		case Type.BYTE:
			sig = INT_VALUE;
		}
		if (sig == null) {
			mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
		} else {
			mv.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, t.getInternalName(), sig.getName(), sig.getDescriptor(), false);
		}
	}
}
