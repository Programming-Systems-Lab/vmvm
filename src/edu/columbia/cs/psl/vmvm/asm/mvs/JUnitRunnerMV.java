package edu.columbia.cs.psl.vmvm.asm.mvs;

import com.sun.xml.internal.ws.org.objectweb.asm.Type;

import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.AdviceAdapter;

public class JUnitRunnerMV extends AdviceAdapter {
	private String name;
	private String className;

	public JUnitRunnerMV(MethodVisitor mv, int access, String name, String desc, String className) {
		super(Opcodes.ASM5, mv, access, name, desc);
		this.name = name;
		this.className = className;
		if (name.equals("<init>") && className.equals("org/apache/tools/ant/taskdefs/optional/junit/JUnitTestRunner"))
			shouldResetInConstructor = true;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
//		if (owner.equals("org/apache/tools/ant/taskdefs/optional/junit/JUnitTestRunner") && name.equals("launch"))
//		{
//			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "resetStatics", "()V");
//		}
		super.visitMethodInsn(opcode, owner, name, desc, itfc);
	}
	@Override
	public void visitCode() {
//		if (className.equals("org/apache/tools/ant/taskdefs/optional/junit/JUnitTestRunner") && this.name.equals("launch"))
////			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "resetStatics", "()V");
//		if(
////				(this.className.equals("org/junit/runners/ParentRunner") && this.name.equals("run"))||(this.className.equals("junit/framework/TestSuite") && this.name.equals("run"))
//	(this.className.equals("org/junit/framework/Test") && this.name.equals("<init>"))
//		|| (this.className.equals("org/junit/framework/TestSuite") && this.name.equals("<init>"))
//		|| (this.className.equals("junit/framework/JUnit4TestAdapter") && this.name.equals("<init>"))
//				)
//		{
//			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "resetStatics", "()V");
//////			ICONST_M1
//////		    INVOKESTATIC java/lang/System.exit(I)V
//////			super.visitInsn(ICONST_M1);
//////			super.visitMethodInsn(INVOKESTATIC, "java/lang/System", "exit", "(I)V");
//		}
		super.visitCode();

	}

	private boolean shouldResetInConstructor = false;

	@Override
	protected void onMethodExit(int opcode) {
//		if (shouldResetInConstructor)
//			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "resetStatics", "()V");
//		if(this.className.equals("org/apache/tools/ant/taskdefs/optional/junit/JUnitTask") && this.name.equals("executeInVM"))
//		{
//			System.out.println(this.className + "."+this.name);
//			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "resetStatics", "()V");
////			ICONST_M1
////		    INVOKESTATIC java/lang/System.exit(I)V
//			super.visitInsn(ICONST_M1);
//			super.visitMethodInsn(INVOKESTATIC, "java/lang/System", "exit", "(I)V");
//		}
	}
}
