package edu.columbia.cs.psl.vmvm.asm;

import java.util.List;
import java.util.Map.Entry;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import edu.columbia.cs.psl.vmvm.Constants;


public class InterceptingMethodVisitor extends AdviceAdapter implements Constants {
	private String name;


	private Type[] argumentTypes;

	private int access;

	protected InterceptingMethodVisitor(int api, MethodVisitor mv, int access,
			String name, String desc) {
		super(api, mv, access, name, desc);
		this.name = name;
		this.access = access;
		this.argumentTypes = Type.getArgumentTypes(desc);
	}

	boolean rewrite = false;

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
//		if (desc.equals(InvivoPreMain.config.getAnnotationDescriptor())) {
			classVisitor.setShouldRewrite();
			rewrite = true;
//		}
		return super.visitAnnotation(desc, visible);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		if (!rewrite) {
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
		if (opcode == GETSTATIC
				&& !(owner.startsWith("java") || owner.startsWith("sun"))) {
			visitLdcInsn(owner);
			visitLdcInsn(name);
			super.visitFieldInsn(opcode, owner, name, desc);
			visitLdcInsn(false);
			visitMethodInsn(INVOKESTATIC,
					"edu/columbia/cs/psl/invivo/runtime/StaticWrapper",
					"getValue",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Z)Ljava/lang/Object;");
			if (Type.getType(desc).getSort() != Type.OBJECT)
				unbox(Type.getType(desc));
			else
				checkCast(Type.getType(desc));
		} else if (opcode == PUTSTATIC) {
			// here should be the value we want to set to
			if (Type.getType(desc).getSort() != Type.OBJECT)
				box(Type.getType(desc));
			visitLdcInsn(owner);
			visitLdcInsn(name);
			visitLdcInsn(desc);
			visitMethodInsn(INVOKESTATIC,
					"edu/columbia/cs/psl/invivo/runtime/StaticWrapper",
					"setValue",
					"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		}
		// else do the standard dynamic lookup
		else
			super.visitFieldInsn(opcode, owner, name, desc);
	}

	int refIdForInterceptor;


	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack, maxLocals);
	}


	private String className;

	public void setClassName(String className) {
		this.className = className;
	}

	private InterceptingClassVisitor classVisitor;

	public void setClassVisitor(
			InterceptingClassVisitor interceptingClassVisitor) {
		classVisitor = interceptingClassVisitor;
	}


}
