package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.util.HashSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.asm.JUnitResettingClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.VMVMClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.struct.EqFieldInsnNode;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodNode;
import edu.columbia.cs.psl.vmvm.asm.struct.MethodListClassNode;

public class StaticFieldIsolatorMV extends CloningAdapter implements Opcodes {

	private String name;
	private VMVMClassVisitor cv;
	public static final boolean CLINIT_ORDER_DEBUG =false;
	private EqMethodNode thisMN;
	private LocalVariablesSorter lvs;
	private boolean isStaticMethod;
	public StaticFieldIsolatorMV(MethodVisitor mv, int access, String name, String desc, VMVMClassVisitor cv, EqMethodNode thisMN) {
		super(mv);
		this.name = name;
		this.cv = cv;
		this.thisMN = thisMN;
		this.isStaticMethod = (access & Opcodes.ACC_STATIC) != 0;
	}
	public void setLvs(LocalVariablesSorter lvs) {
		this.lvs = lvs;
	}

	Label l0 = new Label();
	Label l1 = new Label();
	Label l2 = new Label();
	@Override
	public void visitCode() {


		if (this.name.equals("<clinit>") || this.name.equals(Constants.VMVM_STATIC_RESET_METHOD)) {
			super.visitCode();

//			super.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
//			super.visitLabel(l0);
			/*
			 * LDC "edu.columbia.cs.psl.test.SerializeTest" INVOKESTATIC
			 * java/lang/Class.forName(Ljava/lang/String;)Ljava/lang/Class;
			 */
			if(cv != null && Instrumenter.getClassNode(cv.getClassName()) != null)
			{
			String superz = Instrumenter.getClassNode(cv.getClassName()).superName;
			if (!superz.equals("java/lang/Object")) {
				MethodListClassNode superN = Instrumenter.instrumentedClasses.get(superz);
				if (superN != null && superN.hasClinit) {
					Label continu = new Label();
					super.visitFieldInsn(GETSTATIC, superz, Constants.VMVM_NEEDS_RESET, "Z");
					super.visitJumpInsn(IFEQ, continu);
					super.visitMethodInsn(INVOKESTATIC, superz, Constants.VMVM_STATIC_RESET_METHOD, "()V");
					super.visitLabel(continu);

				}
			}
			}
			String classToRegister = cv.getClassName();
			if (!cv.isAClass())
				classToRegister = classToRegister + "$vmvmReseter";
			super.visitInsn(ICONST_0);
			super.visitFieldInsn(PUTSTATIC, classToRegister, Constants.VMVM_NEEDS_RESET, "Z");

			if (cv.getVersion() > 48 && cv.getVersion() < 1000)// java 5+
				super.visitLdcInsn(Type.getType("L" + classToRegister + ";"));
			else {
				super.visitLdcInsn(classToRegister.replace("/", "."));
				super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
			}
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "registerClInit", "(Ljava/lang/Class;)V");

			if (CLINIT_ORDER_DEBUG) {
				super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
				super.visitLdcInsn("clinit  rerunning>" + cv.getClassName());
				super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
				if (cv.getClassName().endsWith("ivy/Ivy") || cv.getClassName().endsWith("ivy/util/FileUtil")) {
					super.visitTypeInsn(NEW, "java/lang/Exception");
					super.visitInsn(DUP);
					super.visitMethodInsn(INVOKESPECIAL, "java/lang/Exception", "<init>", "()V");
					super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V");
				}
			}

			String className = cv.getClassName();
			
			for (Object o : Instrumenter.getClassNode(className).fields) {
				FieldNode fn = (FieldNode) o;
				if ((fn.access & Opcodes.ACC_STATIC) == 0 || ((fn.access & Opcodes.ACC_ENUM) != 0 && (fn.access & Opcodes.ACC_FINAL) != 0) || (fn.name.contains("$VALUES") && cv.isEnum()))
					continue;
				if(cv.getClassName().endsWith("DetectorFactoryCollection"))
				{
//					System.err.println(fn.access + (fn.access ));
					System.err.println(fn.name + " is " + fn.access);
					
				}
				if(fn.name.equals("$VRc"))
					continue;
				if (fn.value != null) {
					super.visitLdcInsn(fn.value);
					super.visitFieldInsn(PUTSTATIC, className, fn.name, fn.desc);
				} else {
					switch (Type.getType(fn.desc).getSort()) {
					case Type.OBJECT:
					case Type.ARRAY:
						super.visitInsn(ACONST_NULL);
						break;
					case Type.DOUBLE:
						super.visitInsn(DCONST_0);
						break;
					case Type.LONG:
						super.visitInsn(ICONST_0);
						super.visitInsn(I2L);
						break;
					case Type.FLOAT:
						super.visitInsn(FCONST_0);
						break;
					default:
						super.visitInsn(ICONST_0);
						break;
					}
					super.visitFieldInsn(PUTSTATIC, className, fn.name, fn.desc);
				}
			}

		}
	}

	private HashSet<EqFieldInsnNode> staticFieldsToClone = new HashSet<EqFieldInsnNode>();

	private int getFieldAcc(String name, String owner) {
		ClassNode cn = Instrumenter.instrumentedClasses.get(owner);
		if (cn == null) {
			return -1;
		}

		int acc = -1;
		for (Object o : cn.fields) {
			FieldNode fn = (FieldNode) o;
			if (fn.name.equals(name))
				return fn.access;
		}
		acc = getFieldAcc(name, cn.superName);
		if (acc == -1) {
			for (Object o : cn.interfaces) {
				String s = (String) o;
				acc = getFieldAcc(name, s);
				if (acc != -1) {
					return acc;
				}
			}
		} else {
			return acc;
		}
		// System.out.println("Can't get field acc for " + owner+"."+name);
		return -1;
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		String _type = type.replace("[", "");
		MethodListClassNode cn = Instrumenter.instrumentedClasses.get(_type);
		if (cn != null && cn.hasClinit && opcode == NEW) {
			String classToCheckReset = cn.name;
			if ((cn.access & Opcodes.ACC_INTERFACE) != 0)
				classToCheckReset += "$vmvmReseter";
			checkAndReinit(classToCheckReset);
		}
		super.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		MethodListClassNode cn = Instrumenter.getClassNodeWithMethod(owner, name, desc);
		if (cn == null)
			cn = Instrumenter.instrumentedClasses.get(owner);

		if (cn != null && cn.hasClinit && opcode == INVOKESTATIC) {
			String classToCheckReset = cn.name;
			if ((cn.access & Opcodes.ACC_INTERFACE) != 0)
				classToCheckReset += "$vmvmReseter";
			checkAndReinit(classToCheckReset);
		}
		super.visitMethodInsn(opcode, owner, name, desc);
	}

	public void checkAndReinit(String clazz) {
		if (clazz.equals(cv.getClassName()) && isStaticMethod) //no need to check to init if we are already executing code in this class!
			return;
		if (this.thisMN != null && this.thisMN.ignoredInitCalls != null && this.thisMN.ignoredInitCalls.contains(counter.getCount())) {
//			System.out.println("Ignoring reinit of " + clazz + " in " + this.cv.getClassName() + "." + this.name + " (prev in this method) at " + counter.getCount());
			return;
		}
		if (this.thisMN != null && this.thisMN.typesToIgnoreInit != null && this.thisMN.typesToIgnoreInit.contains(clazz)) {
//			System.out.println("Ignoring reinit of " + clazz + " in " + this.cv.getClassName() + "." + this.name + "(from prev methods in CG) at " + counter.getCount());
//			return;
		}
		if(JUnitResettingClassVisitor.shouldIgnoreClass(clazz))
			return;
		super.visitFieldInsn(GETSTATIC, clazz, Constants.VMVM_NEEDS_RESET, "Z"); //Need to force to make sure that this is initialized before we can get a lock on it.
		super.visitInsn(POP);
		
		Label continu = new Label();
			if(CLINIT_ORDER_DEBUG)
			{
				super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
				super.visitLdcInsn("clinit going to check "+clazz+" > in " + cv.getClassName());
			super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
			}		
		if (cv.getVersion() > 48 && cv.getVersion() < 1000)// java 5+
			super.visitLdcInsn(Type.getType("L" + clazz + ";"));
		else {
			super.visitLdcInsn(clazz.replace("/", "."));
			super.visitInsn(ICONST_0);
			super.visitLdcInsn(cv.getClassName().replace("/", "."));
			super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
			super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");
			super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
		}
		if(CLINIT_ORDER_DEBUG)
		{
			super.visitInsn(DUP);
			super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class), "toString", "()Ljava/lang/String;");
			super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
			super.visitInsn(SWAP);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		}		
		super.visitInsn(DUP);
		super.monitorenter();
		super.visitFieldInsn(GETSTATIC, clazz, Constants.VMVM_NEEDS_RESET, "Z"); //must be on a class, not an interface b/c final bs
		super.visitJumpInsn(IFEQ, continu);
		super.visitInsn(ICONST_0);
		super.visitFieldInsn(PUTSTATIC, clazz, Constants.VMVM_NEEDS_RESET, "Z"); //must be on a class, not an interface b/c final bs
		super.monitorexit();
		Label end = new Label();
		super.visitMethodInsn(INVOKESTATIC, clazz, Constants.VMVM_STATIC_RESET_METHOD, "()V");
		super.visitJumpInsn(GOTO, end);
		super.visitLabel(continu);
		super.monitorexit();
		super.visitLabel(end);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		MethodListClassNode cn = Instrumenter.getClassNodeWithField(owner, name);
		if (cn == null)
			cn = Instrumenter.instrumentedClasses.get(owner);

		if (cn != null && cn.hasClinit && (opcode == GETSTATIC || opcode == PUTSTATIC)) {
			String classToCheckReset = cn.name;
			if ((cn.access & Opcodes.ACC_INTERFACE) != 0)
				classToCheckReset += "$vmvmReseter";
			checkAndReinit(classToCheckReset);
		}
		if (opcode == PUTSTATIC) {
			FieldNode fn = Instrumenter.getFieldNode(owner, name);
			if (fn != null)
				if (fn != null && cn != null && fn.name != null && (fn.access & Opcodes.ACC_ENUM) != 0
						&& ((fn.access & Opcodes.ACC_FINAL) != 0 || (fn.name.endsWith("$VALUES") && (cn.access & Opcodes.ACC_ENUM) != 0))) {
					super.visitFieldInsn(GETSTATIC, owner, name, desc);
					Label continu = new Label();
					Label doLoad = new Label();
					super.visitJumpInsn(IFNULL, doLoad);
					super.visitInsn(POP);
					super.visitJumpInsn(GOTO, continu);
					super.visitLabel(doLoad);
					super.visitFieldInsn(opcode, owner, name, desc);
					super.visitLabel(continu);
					return;
				}
		}
		super.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitInsn(int opcode) {
		if (CLINIT_ORDER_DEBUG && opcode == Opcodes.RETURN && this.name.equals(Constants.VMVM_STATIC_RESET_METHOD) && this.cv.isAClass()) {
			super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
			super.visitLdcInsn("clinit finished rerunning>" + cv.getClassName());
			super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		}
		//		if (opcode == Opcodes.RETURN && this.name.equals("<clinit>") && this.cv.isAClass()) {
		//			for (EqFieldInsnNode fin : Instrumenter.getFieldsEffectedBy(cv.getClassName(), "<clinit>", "()V")) {
		//				ClassNode owner = Instrumenter.getClassNode(fin.owner);
		//				if((owner.access & Opcodes.ACC_ENUM) != 0 || (owner.access & Opcodes.ACC_INTERFACE) != 0)
		//					continue;
		//				int acc = getFieldAcc(fin.name, fin.owner);
		//				acc = acc & ~Opcodes.ACC_FINAL;
		//				staticFieldsToClone.add(new EqFieldInsnNode(fin.getOpcode(), fin.owner, fin.name, fin.desc, (acc & Opcodes.ACC_FINAL) != 0));
		////				super.visitFieldInsn(GETSTATIC, fin.owner, fin.name + Constants.BEEN_CLONED_FIELD, "Z");
		////				Label continu = new Label();
		////				super.visitJumpInsn(IFEQ, continu);
		//
		//				super.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		//				super.visitLdcInsn("static Cloning>" + fin.owner + "." + fin.name + " " + fin.desc);
		//				super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		//				super.visitFieldInsn(GETSTATIC, fin.owner, fin.name, fin.desc);
		//				cloneValAtTopOfStack(fin.desc, fin.owner + "." + fin.name + fin.desc + " at " + cv.getClassName() + "." + this.name, false);
		//				super.visitFieldInsn(PUTSTATIC, fin.owner, fin.name + Constants.SANDBOX_SUFFIX, fin.desc);
		//				super.visitInsn(ICONST_1);
		//				super.visitFieldInsn(PUTSTATIC, fin.owner, fin.name + Constants.BEEN_CLONED_FIELD, "Z");
		////				super.visitLabel(continu);
		//			}
		//		}
		super.visitInsn(opcode);
	}

	@Override
	public void visitEnd() {
//		if (this.name.equals("<clinit>") || this.name.equals(Constants.VMVM_STATIC_RESET_METHOD)) {
//
//		super.visitLabel(l1);
//		Label l3 = new Label();
//		super.visitJumpInsn(GOTO, l3);
//		super.visitLabel(l2);
//		super.visitFrame(Opcodes.F_SAME1, 0,null, 1, new Object[] {"java/lang/Exception"});
//		int n = lvs.newLocal(Type.getType(Exception.class));
//		super.visitVarInsn(ASTORE, n);
//		super.visitVarInsn(ALOAD, n);
//		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V");
//		super.visitLabel(l3);
//		}
		super.visitEnd();
		cv.addStaticFieldsToClone(staticFieldsToClone);
	}

	private InsnCountingMV counter;

	public void setCounter(InsnCountingMV counter) {
		this.counter = counter;
	}
}
