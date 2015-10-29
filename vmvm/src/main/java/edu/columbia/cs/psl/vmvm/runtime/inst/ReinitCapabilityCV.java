package edu.columbia.cs.psl.vmvm.runtime.inst;

import java.util.HashSet;
import java.util.LinkedList;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.cs.psl.vmvm.runtime.ClassState;
import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;
import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;
import edu.columbia.cs.psl.vmvm.runtime.VMVMClassFileTransformer;

public class ReinitCapabilityCV extends ClassVisitor {

	public ReinitCapabilityCV(ClassVisitor parent) {
		super(Opcodes.ASM5, new CheckClassAdapter(parent,false));
	}

	private boolean isInterface;
	private String className;
	private boolean skipFrames;
	private LinkedList<FieldNode> mutabilizedFields = new LinkedList<FieldNode>();
	private boolean fixLdcClass;
	private boolean isEnum;

	public LinkedList<FieldNode> getMutabilizedFields() {
		return mutabilizedFields;
	}
	public MethodNode getReClinitMethod() {
		return reClinitMethod;
	}

	public int version;

	private HashSet<String> ownersOfStaticFields = new HashSet<String>();

	public HashSet<String> getOwnersOfStaticFields() {
		return ownersOfStaticFields;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		/*
		 * So, we will need to wrap all static final fields of interfaces like
		 * this, because all fields of interfaces must be static final. But we
		 * don't need to do it for static fields on classes. However, to make
		 * instrumentation simpler, we do it for ALL static fields - otherwise
		 * we would need to know at a field access site whether it's accessing a
		 * field of an interface or not, and that is super cumbersome (but what
		 * we did with the original VMVM).
		 */
		Type fieldType = Type.getType(desc);
		if ((access & Opcodes.ACC_STATIC) != 0) {
			access = access & ~Opcodes.ACC_PRIVATE;
			access = access & ~Opcodes.ACC_PROTECTED;
			access = access | Opcodes.ACC_PUBLIC;
			mutabilizedFields.add(new FieldNode(access, name, desc, signature, value));
			value = null;
			desc = MutableInstance.DESC;
			signature = MutableInstance.DESC;
		}
		return super.visitField(access, name, desc, signature, value);
	}

	String[] interfaces;
	String superName;
	boolean isInnerClass = false;

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		super.visitOuterClass(owner, name, desc);
		isInnerClass = true;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		this.isEnum = (access & Opcodes.ACC_ENUM) != 0;
		this.className = name;
		this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
		this.version = version;
		this.interfaces = interfaces;
		this.superName = superName;
		skipFrames = false;
		if (version >= 100 || version <= 50)
			skipFrames = true;
		if ((access & Opcodes.ACC_PUBLIC) == 0) {
			access = access & ~Opcodes.ACC_PROTECTED;
			access = access & ~Opcodes.ACC_PRIVATE;
			access = access | Opcodes.ACC_PUBLIC;
		}
		//Add signal interface
		String[] newInterfaces = new String[interfaces.length + 1];
		System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
		newInterfaces[interfaces.length] = "edu/columbia/cs/psl/vmvm/runtime/VMVMInstrumented";
		super.visit(version, access, name, signature, superName, newInterfaces);
	}

	MethodNode clinitMethod;

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv;
		if (isInnerClass || name.equals("<init>")) {
			if ((access & Opcodes.ACC_PUBLIC) == 0) {
				access = access & ~Opcodes.ACC_PROTECTED;
				access = access & ~Opcodes.ACC_PRIVATE;
				access = access | Opcodes.ACC_PUBLIC;
			}
		}
		if (name.equals("<clinit>")) {
			clinitMethod = new MethodNode(access, name, desc, signature, exceptions);
			mv = clinitMethod;
		} else
			mv = super.visitMethod(access, name, desc, signature, exceptions);
		mv = new SystemPropertyLogger(mv);
		if(isInterface)
			mv = new StaticFinalMutibleizer(mv, skipFrames);
		mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
		mv = new ReflectionFixingMV(mv, fixLdcClass, className);
		return mv;
	}

	private MethodNode reClinitMethod;

	@Override
	public void visitEnd() {
		String classNameWField = className;
		if (isInterface)
			classNameWField = className + "$$VMVM_RESETTER";

		super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Constants.VMVM_NEEDS_RESET, ClassState.DESC, null, null);
		if(mutabilizedFields.size() > 0)
		super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Constants.VMVM_RESET_SUFFIX, "L"+className+Constants.VMVM_RESET_SUFFIX+";", null, null);

		if (!isInterface) {
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);
			for (String s : ownersOfStaticFields) {
				super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, s.replace("/", "_") + Constants.TOTAL_STATIC_CLASSES_CHECKED, "Z", null, 1);
			}
		}

		//Create a new <clinit>
		MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		if (mutabilizedFields.size() > 0) {
			mv.visitTypeInsn(Opcodes.NEW, className + Constants.VMVM_RESET_SUFFIX);
			mv.visitInsn(Opcodes.DUP);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className+Constants.VMVM_RESET_SUFFIX, "<init>", "()V", false);
			mv.visitFieldInsn(Opcodes.PUTSTATIC, className, Constants.VMVM_RESET_SUFFIX, "L" + className + Constants.VMVM_RESET_SUFFIX + ";");
		}
		mv.visitTypeInsn(Opcodes.NEW, ClassState.INTERNAL_NAME);
		mv.visitInsn(Opcodes.DUP);
		mv.visitInsn(Opcodes.DUP);
		if (fixLdcClass) {
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		} else
			mv.visitLdcInsn(Type.getObjectType(className));
		mv.visitIntInsn(Opcodes.BIPUSH, interfaces.length);
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
		for (int i = 0; i < interfaces.length; i++) {
			mv.visitInsn(Opcodes.DUP);
			mv.visitIntInsn(Opcodes.BIPUSH, i);
			mv.visitLdcInsn(interfaces[i].replace("/", "."));
			mv.visitInsn(Opcodes.AASTORE);
		}
		mv.visitLdcInsn(superName.replace("/", "."));
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ClassState.INTERNAL_NAME, "<init>", "(Ljava/lang/Class;[Ljava/lang/String;Ljava/lang/String;)V", false);
		if (isInterface) {
			mv.visitInsn(Opcodes.DUP);
			mv.visitInsn(Opcodes.ICONST_1);
			mv.visitFieldInsn(Opcodes.PUTFIELD, ClassState.INTERNAL_NAME, "isInterface", "Z");
		}
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "clinitCalled", "(" + ClassState.DESC + ")V", false);

		if (!VMVMClassFileTransformer.isIgnoredClass(superName))
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, superName, "__vmvmReClinit", "()V", false);

		for (FieldNode fn : mutabilizedFields) {
			mv.visitTypeInsn(Opcodes.NEW, MutableInstance.INTERNAL_NAME);
			mv.visitInsn(Opcodes.DUP);
			if (fn.desc.length() == 1) {
				mv.visitInsn(Opcodes.ACONST_NULL);
			} else {
				if (fixLdcClass) {
					Type t = Type.getType(fn.desc);
					mv.visitLdcInsn(t.getInternalName().replace("/", "."));
					mv.visitInsn(Opcodes.ICONST_0);
					mv.visitLdcInsn(className.replace("/", "."));
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
				} else
					mv.visitLdcInsn(Type.getType(fn.desc));
			}
			if (fn.value != null) {
				mv.visitLdcInsn(fn.value);
				if (fn.desc.length() == 1)
					Utils.box(mv, Type.getType(fn.desc));
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, MutableInstance.INTERNAL_NAME, "<init>", "(Ljava/lang/Class;Ljava/lang/Object;)V", false);
			} else
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, MutableInstance.INTERNAL_NAME, "<init>", "(Ljava/lang/Class;)V", false);
			mv.visitFieldInsn(Opcodes.PUTSTATIC, className, fn.name, MutableInstance.DESC);
		}

		if (clinitMethod != null) {
			if (clinitMethod.maxStack < 6)
				clinitMethod.maxStack = 6;
			clinitMethod.accept(mv);
		} else {
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(6, 0);
			mv.visitEnd();
		}

		if (isInterface) {
			reClinitMethod = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, "__vmvmReClinit", "()V", null, null);
			mv = reClinitMethod;
		} else
			mv = super.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, "__vmvmReClinit", "()V", null, null);
		final Label finishedClinitCode = new Label();
		LabelRemappingMV reLabeler = new LabelRemappingMV(mv, finishedClinitCode);
		mv.visitCode();

		if (clinitMethod != null) {
			if (clinitMethod.tryCatchBlocks != null) {
				for (Object o : clinitMethod.tryCatchBlocks) {
					((TryCatchBlockNode) o).accept(reLabeler);
				}
			}
		}
		//		println(mv, "Reclinit " + classNameWField);
		Label allDone = new Label();

		Label continu = new Label();

		//		if(CLINIT_ORDER_DEBUG)
		//		{
		//			super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
		//			super.visitLdcInsn("clinit going to check "+clazz+" > in " + cv.getClassName());
		//		super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		//		}		
		mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
		mv.visitFieldInsn(Opcodes.GETFIELD, ClassState.INTERNAL_NAME, "needsReinit", "Z");
		mv.visitJumpInsn(Opcodes.IFEQ, allDone);
		if (fixLdcClass) {
			mv.visitLdcInsn(classNameWField.replace("/", "."));
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitLdcInsn(classNameWField.replace("/", "."));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		} else
			mv.visitLdcInsn(Type.getObjectType(classNameWField));

		mv.visitInsn(Opcodes.DUP);
		mv.visitInsn(Opcodes.MONITORENTER);

		mv.visitFieldInsn(Opcodes.GETSTATIC, classNameWField, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		Label notInInit = new Label();
		mv.visitJumpInsn(Opcodes.IFNULL, notInInit);

		mv.visitFieldInsn(Opcodes.GETSTATIC, classNameWField, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		mv.visitJumpInsn(Opcodes.IF_ACMPEQ, continu);
		//If the Class object for C indicates that initialization is in progress for C by some other thread, then release LC and block the current thread until informed that the in-progress initialization has completed, at which time repeat this procedure.
		//If the Class object for C indicates that initialization is in progress for C by the current thread, then this must be a recursive request for initialization. Release LC and complete normally

		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "wait", "()V", false);
		//XXX what?
		mv.visitLabel(notInInit);
		if (!skipFrames)
			mv.visitFrame(Opcodes.F_NEW, 0, new Object[0], 1, new Object[] { "java/lang/Class" });
		mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
		mv.visitFieldInsn(Opcodes.GETFIELD, ClassState.INTERNAL_NAME, "needsReinit", "Z");
		mv.visitJumpInsn(Opcodes.IFEQ, continu);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		mv.visitFieldInsn(Opcodes.PUTSTATIC, classNameWField, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
		mv.visitInsn(Opcodes.ICONST_0);
		mv.visitFieldInsn(Opcodes.PUTFIELD, ClassState.INTERNAL_NAME, "needsReinit", "Z");

		mv.visitInsn(Opcodes.DUP);
		mv.visitInsn(Opcodes.MONITOREXIT);
		//		mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, Constants.VMVM_STATIC_RESET_METHOD, "()V", false);
		//do the init

		//		if (fixLdcClass) {
		//			mv.visitLdcInsn(className.replace("/", "."));
		//			mv.visitInsn(Opcodes.ICONST_0);
		//			mv.visitLdcInsn(className.replace("/", "."));
		//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
		//			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
		//			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		//		} else
		//			mv.visitLdcInsn(Type.getObjectType(className));

		for (String s : ownersOfStaticFields) {
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitFieldInsn(Opcodes.PUTSTATIC, classNameWField, s.replace("/", "_") + Constants.TOTAL_STATIC_CLASSES_CHECKED, "Z");
		}
		for (FieldNode fn : mutabilizedFields) {
			if (fn.value != null) {
				mv.visitFieldInsn(Opcodes.GETSTATIC, className, fn.name, MutableInstance.DESC);
				mv.visitLdcInsn(fn.value);
				if (fn.desc.length() == 1)
					Utils.box(mv, Type.getType(fn.desc));
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MutableInstance.INTERNAL_NAME, "put", "(Ljava/lang/Object;)V", false);
			}
		}

		if (!VMVMClassFileTransformer.isIgnoredClass(superName))
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, superName, "__vmvmReClinit", "()V", false);

		mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "reinitCalled", "(" + ClassState.DESC + ")V", false);
		if (isEnum) {
			if (fixLdcClass) {
				mv.visitLdcInsn(className.replace("/", "."));
				mv.visitInsn(Opcodes.ICONST_0);
				mv.visitLdcInsn(className.replace("/", "."));
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
			} else
				mv.visitLdcInsn(Type.getObjectType(className));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "fixEnum", "(Ljava/lang/Class;)V", false);

		}
		int maxStack = 40;
		int maxLocals = 40;
		if (clinitMethod != null) {
			mv.visitInsn(Opcodes.POP);
			maxStack = (clinitMethod.maxStack > 3 ? clinitMethod.maxStack : 0);
			maxLocals = (clinitMethod.maxLocals > 2 ? clinitMethod.maxLocals : 0);
			clinitMethod.instructions.accept(reLabeler);
			mv.visitLabel(finishedClinitCode);
			if (!skipFrames)
				mv.visitFrame(Opcodes.F_NEW, 0, new Object[] {}, 0, new Object[0]);
			if (fixLdcClass) {
				mv.visitLdcInsn(classNameWField.replace("/", "."));
				mv.visitInsn(Opcodes.ICONST_0);
				mv.visitLdcInsn(classNameWField.replace("/", "."));
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
			} else
				mv.visitLdcInsn(Type.getObjectType(classNameWField));
		}
		//If the execution of the class or interface initialization method completes normally, then acquire LC, label the Class object for C as fully initialized, notify all waiting threads, release LC, and complete this procedure normally.
		mv.visitInsn(Opcodes.DUP);
		mv.visitInsn(Opcodes.MONITORENTER);
		mv.visitInsn(Opcodes.ACONST_NULL);
		mv.visitFieldInsn(Opcodes.PUTSTATIC, classNameWField, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "notifyAll", "()V", false);
		//
		mv.visitLabel(continu);
		if (!skipFrames)
			mv.visitFrame(Opcodes.F_NEW, 0, new Object[] {}, 1, new Object[] { "java/lang/Class" });
		mv.visitInsn(Opcodes.MONITOREXIT);
		mv.visitLabel(allDone);
		if (!skipFrames)
			mv.visitFrame(Opcodes.F_NEW, 0, new Object[] {}, 0, new Object[0]);

		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(maxStack, maxLocals);
		mv.visitEnd();

		if (!isInterface) {
			MethodVisitor fieldInit = super.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, "_vmvmReinitFieldCheck", "(Ljava/lang/String;)Z", null, null);
			fieldInit.visitCode();
			Label localReinit = new Label();
			Label otherReinit = new Label();

			for (FieldNode fn : mutabilizedFields) {
				fieldInit.visitVarInsn(Opcodes.ALOAD, 0);
				fieldInit.visitLdcInsn(fn.name);
				fieldInit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
				fieldInit.visitJumpInsn(Opcodes.IFNE, localReinit);
			}
			fieldInit.visitJumpInsn(Opcodes.GOTO, otherReinit);
			fieldInit.visitLabel(localReinit);
			if (!skipFrames)
				fieldInit.visitFrame(Opcodes.F_NEW, 1, new Object[] { "java/lang/String" }, 0, new Object[0]);

			fieldInit.visitMethodInsn(Opcodes.INVOKESTATIC, className, "__vmvmReClinit", "()V", false);

			fieldInit.visitLdcInsn(Opcodes.ICONST_1);
			fieldInit.visitInsn(Opcodes.IRETURN);

			fieldInit.visitLabel(otherReinit);
			if (!skipFrames)
				fieldInit.visitFrame(Opcodes.F_NEW, 1, new Object[] { "java/lang/String" }, 0, new Object[0]);

			fieldInit.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
			fieldInit.visitVarInsn(Opcodes.ALOAD, 0);
			fieldInit.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "tryToReinitForField", "(" + ClassState.DESC + "Ljava/lang/String;)V", false);
			fieldInit.visitLdcInsn(Opcodes.ICONST_0);
			fieldInit.visitInsn(Opcodes.IRETURN);
			fieldInit.visitEnd();
		}
		super.visitEnd();
	}

	public void println(MethodVisitor mv, String toPrint) {
		mv.visitLdcInsn(toPrint);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "logMessage", "(Ljava/lang/String;)V", false);
	}
}
