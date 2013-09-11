package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.net.MalformedURLException;
import java.net.URL;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.ClassNode;

import edu.columbia.cs.psl.vmvm.Instrumenter;

public class ClassDefineInterceptMethodVisitor extends InstructionAdapter implements Opcodes{

	private String name;
	private String desc;
	private String owner;
	private LocalVariablesSorter lvs;
	public ClassDefineInterceptMethodVisitor(MethodVisitor mv,String owner,String name,String desc) {
		super(mv);
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}
	
	@Override
	public void visitCode() {
		super.visitCode();
	}
	private void onMethodEnter()
	{
		if(this.name.equals("<init>") && this.owner.equals("org/apache/catalina/loader/WebappClassLoader")
				|| Instrumenter.atLeastASuperEq(this.owner, "java/net/URLClassLoader", 0))
		{
			superInit = true;

			/*
			 * Label l0 = new Label();
				Label l1 = new Label();
				Label l2 = new Label();
				mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
				mv.visitLabel(l0);
				mv.visitTypeInsn(NEW, "java/net/URL");
				mv.visitLdcInsn("foo");
				mv.visitMethodInsn(INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/lang/String;)V");
				mv.visitLabel(l1);
				Label l3 = new Label();
				mv.visitJumpInsn(GOTO, l3);
				mv.visitLabel(l2);
				mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Exception"});
				mv.visitVarInsn(ASTORE, 2);
				mv.visitLabel(l3);
				mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(ARETURN);
				mv.visitMaxs(2, 3);
			 */
//			Label l0 = new Label();
//			Label l1 = new Label();
//			Label l2 = new Label();
//			
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");

			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, "java/net/URL");
			mv.visitInsn(DUP);
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitLdcInsn("file://");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
			mv.visitLdcInsn("VMVMLib");
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
			mv.visitMethodInsn(INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/lang/String;)V");
			mv.visitMethodInsn(INVOKEVIRTUAL, this.owner, "addURL", "(Ljava/net/URL;)V");
			
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, "java/net/URL");
			mv.visitInsn(DUP);
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitLdcInsn("file://");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
			mv.visitLdcInsn("ASMLib");
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
			mv.visitMethodInsn(INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/lang/String;)V");
			mv.visitMethodInsn(INVOKEVIRTUAL, this.owner, "addURL", "(Ljava/net/URL;)V");

			mv.visitLabel(l1);
			Label l3 = new Label();
			mv.visitJumpInsn(GOTO, l3);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0,null, 1, new Object[] {"java/lang/Exception"});
			int n = lvs.newLocal(Type.getType(Exception.class));
			mv.visitVarInsn(ASTORE, n);
			mv.visitVarInsn(ALOAD, n);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V");
			mv.visitLabel(l3);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

		}
	}
	private boolean superInit = false;
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		//XXX todo this was buggy so now it's off. is that a problem?
//		if(name.equals("defineClass") && !this.owner.equals("java/lang/ClassLoader") && Instrumenter.atLeastASuperEq(owner, "java/lang/ClassLoader",0))
//		{
////			System.out.println("cribbing on " + owner+"."+name+desc);
////			Type[] newArgs = new Type
//			Type[] oldArgs = Type.getArgumentTypes(desc);
//			Type[] newArgs = new Type[oldArgs.length+1];
//			System.arraycopy(oldArgs, 0, newArgs, 1, oldArgs.length);
//			newArgs[0] = Type.getType(ClassLoader.class);
//			desc = Type.getMethodDescriptor(Type.getReturnType(desc), newArgs);
//			owner = "edu/columbia/cs/psl/vmvm/agent/VMVMClassFileTransformer";
//			opcode = INVOKESTATIC;
//		}
//		else
			super.visitMethodInsn(opcode, owner, name, desc);
			
//		if (this.name.equals("<init>") && !superInit && opcode == INVOKESPECIAL)
//			onMethodEnter();
	}

	public void setLocalVariableSorter(TypeRememberingLocalVariableSorter lvs2) {
		this.lvs = lvs2;
	}
}
