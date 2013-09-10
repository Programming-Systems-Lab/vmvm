package edu.columbia.cs.psl.vmvm.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.InstructionAdapter;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;

/**
 * This method visitor will make sure that all sandbox flags are propogated
 * 
 * @author jon
 * 
 */
public class SandboxPropogatingMV extends InstructionAdapter implements Opcodes {

	private InvivoAdapter invivoAdapter;
	private String className;
	private String name;
	private String desc;

	protected SandboxPropogatingMV(int api, MethodVisitor mv, int access, String name, String desc, String className, InvivoAdapter lvs) {
		//		super(api,mv,access,name,desc);
		super(mv);
		this.invivoAdapter = lvs;
		this.className = className;
		this.name = name;
		this.desc = desc;
	}

	@Override
	public void visitEnd() {
		visitLabel(end);
		if (sandboxVar >= 0)
			super.visitLocalVariable("vmvmSandboxIndx", "I", null, start, end, sandboxVar);
		super.visitEnd();
	}

	private int sandboxVar = -1;
	private Label start = new Label();
	private Label end = new Label();

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		if (index >= invivoAdapter.getFirstLocal() - 1 && invivoAdapter.isStaticMethod() && !invivoAdapter.isMain() && !name.equals("<clinit>"))
			super.visitLocalVariable(name, desc, signature, start, end, index + 1);
		else
			super.visitLocalVariable(name, desc, signature, start, end, index);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		System.out.println("ZZZ" + name + var);
		if (var >= invivoAdapter.getFirstLocal() - 1 && invivoAdapter.isStaticMethod() && !invivoAdapter.isMain() && !name.equals("<clinit>"))
			super.visitVarInsn(opcode, var + 1);
		else
			super.visitVarInsn(opcode, var);
	}

	@Override
	public void visitCode() {
		super.visitCode();
		if (!name.equals("<init>"))
			onMethodEnter();
	}

	protected void onMethodEnter() {
		visitLabel(start);
		if (invivoAdapter.isStaticMethod()) {
			if (invivoAdapter.isMain() || name.equals("<clinit>")) {
				sandboxVar = invivoAdapter.newLocal(Type.INT_TYPE);
				visitIntInsn(BIPUSH, 0);
				invivoAdapter.setSandboxFlag();
			}
		} else {
			if (name.equals("<init>")) {
				visitVarInsn(ILOAD, invivoAdapter.getFirstLocal() - 1);
				invivoAdapter.setSandboxFlag();
			}
		}
	}

	private boolean superInit = false;

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		if (Instrumenter.instrumentedClasses.containsKey(owner)) {
			if (name.equals("<init>") || opcode == INVOKESTATIC) {
				//Need to call the modified method - which takes a short arg; the current sandbox state
				Type[] args = Type.getArgumentTypes(desc);
				Type[] descTypes = new Type[args.length + 1];
				System.arraycopy(args, 0, descTypes, 0, args.length);
				descTypes[args.length] = Type.INT_TYPE;
				desc = Type.getMethodDescriptor(Type.getReturnType(desc), descTypes);
				invivoAdapter.getSandboxFlag();
			}
		}
		super.visitMethodInsn(opcode, owner, name, desc);

		if (this.name.equals("<init>") && !superInit && opcode == INVOKESPECIAL)
			onMethodEnter();
	}
}
