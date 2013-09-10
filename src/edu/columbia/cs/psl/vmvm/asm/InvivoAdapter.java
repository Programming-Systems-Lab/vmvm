package edu.columbia.cs.psl.vmvm.asm;

import java.util.HashSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;

import edu.columbia.cs.psl.vmvm.CloningUtils;
import edu.columbia.cs.psl.vmvm.Constants;

public class InvivoAdapter extends AdviceAdapter {
	protected int access;
	protected String name;
	protected String desc;
	protected String className;
	private LocalVariablesSorter lvs;

	protected InvivoAdapter(int api, MethodVisitor mv, int access, String name, String desc, String className, LocalVariablesSorter lvs) {
		super(api, mv, access, name, desc);
		this.access = access;
		this.name = name;
		this.desc = desc;
		this.className = className;
		this.lvs = lvs;
	}
	public boolean isMain() {
		return name.equals("main") && desc.equals("([Ljava/lang/String;)V");
	}
	public boolean needToGenerateSandboxVar()
	{
		return isStaticMethod() && (isMain() || name.equals("<clinit>"));
	}
	public boolean isStaticMethod() {
		return (access & Opcodes.ACC_STATIC) != 0;
	}

	public int getFirstLocal() {
		return firstLocal;
	}

	@Override
	public int newLocal(Type type) {
		return lvs.newLocal(type);
	}
	public void branchIfSandboxed(Label lblForSandboxed) {
		getSandboxFlag();
		visitJumpInsn(IFGT, lblForSandboxed);
	}

	public void getSandboxFlag() {
		if (!isStaticMethod()) {
			loadThis();
			visitFieldInsn(GETFIELD, className, Constants.CHILD_FIELD, Type.INT_TYPE.getDescriptor());
		} else
			visitVarInsn(ILOAD, getFirstLocal() - (isMain() || isClinit() ? 0 : 1));
	}
	private boolean isClinit() {
		return name.equals("<clinit>");
	}
	public void setSandboxFlag() {
		if (!isStaticMethod())
		{
			loadThis();
			swap();
			visitFieldInsn(PUTFIELD, className, Constants.CHILD_FIELD, Type.INT_TYPE.getDescriptor());
		}
		else
			visitVarInsn(ISTORE, getFirstLocal() - (isMain() || isClinit() ? 0 : 1));
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
				ifNull(nullContinue);
				if (secondElHasArrayLen) {
					swap();
				} else {
					dup();
					visitInsn(ARRAYLENGTH);
				}
				dup();
				newArray(Type.getType(fieldType.getDescriptor().substring(1)));
				dupX2();
				swap();
				push(0);
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
				getSandboxFlag();
				invokeStatic(Type.getType(CloningUtils.class), Method.getMethod("Object clone(Object, String, int)"));
				checkCast(fieldType);
			}
		} else if (fieldType.getClassName().contains("InputStream") || fieldType.getClassName().contains("OutputStream") || fieldType.getClassName().contains("Socket")) {
			// Do nothing
		} else {
			// println("heavy> " + debug);
			visitLdcInsn(debug);
			getSandboxFlag();
			invokeStatic(Type.getType(CloningUtils.class), Method.getMethod("Object clone(Object, String, int)"));
			checkCast(fieldType);

		}
	}
}
