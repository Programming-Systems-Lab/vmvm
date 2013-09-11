package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.util.HashSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import edu.columbia.cs.psl.vmvm.CloningUtils;

public class CloningAdapter extends InstructionAdapter implements Opcodes {

	public CloningAdapter(MethodVisitor mv) {
		super(mv);
	}


	private static final HashSet<String> immutableClasses = new HashSet<String>();
	static {
		immutableClasses.add("Ljava/lang/Integer;");
		immutableClasses.add("Ljava/lang/Long;");
		immutableClasses.add("Ljava/lang/Short;");
		immutableClasses.add("Ljava/lang/Float;");
		immutableClasses.add("Ljava/lang/String;");
		immutableClasses.add("Ljava/lang/Char;");
		immutableClasses.add("Ljava/lang/Byte;");
		immutableClasses.add("Ljava/lang/Integer;");
		immutableClasses.add("Ljava/lang/Long;");
		immutableClasses.add("Ljava/lang/Short;");
		immutableClasses.add("Ljava/lang/Float;");
		immutableClasses.add("Ljava/lang/String;");
		immutableClasses.add("Ljava/lang/Char;");
		immutableClasses.add("Ljava/lang/Byte;");
		immutableClasses.add("Ljava/sql/ResultSet;");
		immutableClasses.add("Ljava/lang/Class;");
		immutableClasses.add("Z");
		immutableClasses.add("B");
		immutableClasses.add("C");
		immutableClasses.add("S");
		immutableClasses.add("I");
		immutableClasses.add("J");
		immutableClasses.add("F");
		immutableClasses.add("L");

	}

	protected void cloneValAtTopOfStack(String typeOfField) {
		_generateClone(typeOfField, "", false);
	}

	protected void cloneValAtTopOfStack(String typeOfField, String debug, boolean secondElHasArrayLen) {
		_generateClone(typeOfField, debug, secondElHasArrayLen);
	}

	public void println(String toPrint) {
		visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		visitLdcInsn(toPrint + " : ");
		super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V");

		visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;");
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;");
		super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
	}

	private void _generateClone(String typeOfField, String debug, boolean secondElHasArrayLen) {
		Type fieldType = Type.getType(typeOfField);
//		println("Generate clone: "+ debug);
		if (
		// fieldType.getSort() == Type.ARRAY &&
		// fieldType.getElementType().getSort()
		// ||
		fieldType.getSort() == Type.VOID || (fieldType.getSort() != Type.ARRAY && (fieldType.getSort() != Type.OBJECT || immutableClasses.contains(typeOfField)))) {
			//			 println("reference> " + debug);
			return;
		}
		if (fieldType.getSort() == Type.ARRAY) {
			if (fieldType.getElementType().getSort() != Type.OBJECT || immutableClasses.contains(fieldType.getElementType().getDescriptor())) {
				//				 println("array> " + debug);

				// Just need to duplicate the array
				dup();
				Label nullContinue = new Label();
				visitJumpInsn(IFNULL, nullContinue);
				if (secondElHasArrayLen) {
					swap();
				} else {
					dup();
					visitInsn(ARRAYLENGTH);
				}
				dup();
				newarray(Type.getType(fieldType.getDescriptor().substring(1)));
				dupX2();
				swap();

				iconst(0);
				dupX2();
				swap();
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
				Label noNeedToPop = new Label();
				if (secondElHasArrayLen) {
					visitJumpInsn(GOTO, noNeedToPop);
					visitLabel(nullContinue);
					swap();
					pop();
				} else {
					visitLabel(nullContinue);
				}

				visitLabel(noNeedToPop);

			} else {
				// println("heavy> " + debug);
				// Just use the reflective cloner
				visitLdcInsn(debug);
//				getSandboxFlag();
				invokestatic(Type.getType(CloningUtils.class).getInternalName(), "clone", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
				//				invokeStatic(Type.getType(CloningUtils.class), Method.getMethod("Object clone(Object, String, int)"));
				checkcast(fieldType);
			}
		} else if (fieldType.getClassName().contains("InputStream") || fieldType.getClassName().contains("OutputStream") || fieldType.getClassName().contains("Socket")) {
			// Do nothing
		} else {
			// println("heavy> " + debug);
			visitLdcInsn(debug);
//			getSandboxFlag();
			visitMethodInsn(INVOKESTATIC,Type.getType(CloningUtils.class).getInternalName(), "clone", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
			//			invokeStatic(Type.getType(CloningUtils.class), Method.getMethod("Object clone(Object, String, int)"));
			checkcast(fieldType);

		}
//		println("Complete clone: " + debug);
	}
}
