package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.util.HashSet;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.asm.JUnitResettingClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.VMVMClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.struct.EqFieldInsnNode;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodNode;
import edu.columbia.cs.psl.vmvm.asm.struct.MethodListClassNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Label;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.LocalVariablesSorter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.ClassNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.FieldNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.util.Printer;

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
//			if(cv.getClassName().endsWith("CallbackInfo") || cv.getClassName().endsWith("CodeGenerationException") || cv.getClassName().endsWith("CodeEmitter"))
//				printStackTrace();
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
//				if(cv.getClassName().contains("ClassImposterizer"))
//				System.err.println(superN.name + " " + (superN.hasClinit ? "T" : "F"));
				if (superN != null && superN.hasClinit && !Instrumenter.ignoredClasses.contains(superz)) {
					checkAndReinit(superz);
//					Label continu = new Label();
//					super.visitFieldInsn(GETSTATIC, superz, Constants.VMVM_NEEDS_RESET, "Z");
//					super.visitJumpInsn(IFEQ, continu);
//					super.visitMethodInsn(INVOKESTATIC, superz, Constants.VMVM_STATIC_RESET_METHOD, "()V");
//					super.visitLabel(continu);

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
				super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			}
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "registerClInit", "(Ljava/lang/Class;)V", false);

			if (CLINIT_ORDER_DEBUG) {
				println("clinit  rerunning>" + cv.getClassName());

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
		if (cn != null && cn.hasClinit && opcode == NEW && !Instrumenter.ignoredClasses.contains(type)) {
			String classToCheckReset = cn.name;
			if ((cn.access & Opcodes.ACC_INTERFACE) != 0)
				classToCheckReset += "$vmvmReseter";
			checkAndReinit(classToCheckReset);
		}
		super.visitTypeInsn(opcode, type);
	}
	public void printStackTrace()
	{
		super.visitTypeInsn(NEW, "java/lang/Exception");
		    super.visitInsn(DUP);
		    super.visitMethodInsn(INVOKESPECIAL, "java/lang/Exception", "<init>", "()V", false);
		    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);
	}
	public void println(String toPrint) {
        super.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        super.visitLdcInsn(toPrint + " : ");
        super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print",
                "(Ljava/lang/String;)V", false);

        super.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread",
                "()Ljava/lang/Thread;", false);
        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
        super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);
    }

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		MethodListClassNode cn = Instrumenter.getClassNodeWithMethod(owner, name, desc);
		if (cn == null)
			cn = Instrumenter.instrumentedClasses.get(owner);

		if (cn != null && cn.hasClinit && opcode == INVOKESTATIC && !Instrumenter.ignoredClasses.contains(owner)) {
			String classToCheckReset = cn.name;
			if ((cn.access & Opcodes.ACC_INTERFACE) != 0)
				classToCheckReset += "$vmvmReseter";
			checkAndReinit(classToCheckReset);
		}
		super.visitMethodInsn(opcode, owner, name, desc, itfc);
	}

	public void checkAndReinit(String clazz) {
		if (clazz.equals(cv.getClassName()))// && isStaticMethod) //no need to check to init if we are already executing code in this class!
			return;
		if (this.thisMN != null && this.thisMN.ignoredInitCalls != null && this.thisMN.ignoredInitCalls.contains(counter.getCount())) {
//			System.out.println("Ignoring reinit of " + clazz + " in " + this.cv.getClassName() + "." + this.name + " (prev in this method) at " + counter.getCount());
			return;
		}
//		if (this.thisMN != null && this.thisMN.typesToIgnoreInit != null && this.thisMN.typesToIgnoreInit.contains(clazz)) {
////			System.out.println("Ignoring reinit of " + clazz + " in " + this.cv.getClassName() + "." + this.name + "(from prev methods in CG) at " + counter.getCount());
////			return;
//		}
		
		Label allDone = new Label();
		if(JUnitResettingClassVisitor.shouldIgnoreClass(clazz))
			return;
		super.visitFieldInsn(GETSTATIC, clazz, Constants.VMVM_NEEDS_RESET, "Z"); //Need to force to make sure that this is initialized before we can get a lock on it.
		super.visitJumpInsn(IFEQ, allDone);
		
		Label continu = new Label();
//			if(CLINIT_ORDER_DEBUG)
//			{
//				super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//				super.visitLdcInsn("clinit going to check "+clazz+" > in " + cv.getClassName());
//			super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//			}		
		if (cv.getVersion() > 48 && cv.getVersion() < 1000)// java 5+
			super.visitLdcInsn(Type.getType("L" + clazz + ";"));
		else {
			super.visitLdcInsn(clazz.replace("/", "."));
			super.visitInsn(ICONST_0);
			super.visitLdcInsn(cv.getClassName().replace("/", "."));
			super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
			super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		}

		super.visitInsn(DUP);
		super.monitorenter();
		
		super.visitFieldInsn(GETSTATIC, clazz, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		Label notInInit = new Label();
		super.visitJumpInsn(IFNULL, notInInit);
		
		super.visitFieldInsn(GETSTATIC, clazz, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitJumpInsn(IF_ACMPEQ, continu);
		//If the Class object for C indicates that initialization is in progress for C by some other thread, then release LC and block the current thread until informed that the in-progress initialization has completed, at which time repeat this procedure.
		//If the Class object for C indicates that initialization is in progress for C by the current thread, then this must be a recursive request for initialization. Release LC and complete normally
		super.visitInsn(DUP);
		if(CLINIT_ORDER_DEBUG)
		{
			println("clinit going to wait on "+clazz+" > in " + cv.getClassName());
		}
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "wait", "()V", false);
		if(CLINIT_ORDER_DEBUG)
		{
			println("clinit done waiting on "+clazz+" > in " + cv.getClassName());
		}
		super.visitLabel(notInInit);
		
		super.visitFieldInsn(GETSTATIC, clazz, Constants.VMVM_NEEDS_RESET, "Z");
		super.visitJumpInsn(IFEQ, continu); 		//If the Class object for C indicates that C has already been initialized, then no further action is required. Release LC and complete normally.
		//Otherwise, record the fact that initialization of the Class object for C is in progress by the current thread, and release LC. Then, initialize each final static field of C with the constant value in its ConstantValue attribute (§4.7.2), in the order the fields appear in the ClassFile structure
		super.visitInsn(ICONST_0);
		super.visitFieldInsn(PUTSTATIC, clazz, Constants.VMVM_NEEDS_RESET, "Z"); 
		super.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		super.visitFieldInsn(PUTSTATIC, clazz, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		super.visitInsn(DUP);
		super.monitorexit();
		if(CLINIT_ORDER_DEBUG)
			println("Rerunning " + clazz +" from " + cv.getClassName());
		if(clazz.endsWith("Enhancer"))
			printStackTrace();
		super.visitMethodInsn(INVOKESTATIC, clazz, Constants.VMVM_STATIC_RESET_METHOD, "()V", false);
		//If the execution of the class or interface initialization method completes normally, then acquire LC, label the Class object for C as fully initialized, notify all waiting threads, release LC, and complete this procedure normally.
		super.visitInsn(DUP);
		super.monitorenter();
		super.visitInsn(ACONST_NULL);
		super.visitFieldInsn(PUTSTATIC, clazz, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		super.visitInsn(DUP);
		super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "notifyAll", "()V", false);
		if(CLINIT_ORDER_DEBUG)
		{
			println("clinit notified waiters on "+clazz+" > in " + cv.getClassName());
		}

		super.visitLabel(continu);
		super.monitorexit();
		super.visitLabel(allDone);
	}
	void magic() throws InterruptedException
	{
		synchronized(l0)
		{
			l0.wait();
//			l0.notifyAll();
		}
	}
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		MethodListClassNode cn = Instrumenter.getClassNodeWithField(owner, name);
		if (cn == null)
			cn = Instrumenter.instrumentedClasses.get(owner);

		if (cn != null && cn.hasClinit && (opcode == GETSTATIC || opcode == PUTSTATIC) && !Instrumenter.ignoredClasses.contains(owner)) {
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
	println("clinit finished rerunning>" + cv.getClassName());
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
