package edu.columbia.cs.psl.vmvm.runtime.inst;

import edu.columbia.cs.psl.vmvm.runtime.InterfaceReinitializer;
import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;
import edu.columbia.cs.psl.vmvm.runtime.VMVMClassFileTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class ClassReinitCV extends ClassVisitor {


	public ClassReinitCV(ClassVisitor parent) {
		super(Opcodes.ASM5, new CheckClassAdapter(parent,false));

	}

	private boolean isInterface;
	private String className;
	private boolean skipFrames;
	private LinkedList<FieldNode> allStaticFields = new LinkedList<FieldNode>();
	private HashSet<String> finalFields = new HashSet<String>();
	private boolean fixLdcClass;
	private boolean isEnum;

	public static ConcurrentHashMap<String, MethodNode> reClinitMethods = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, Integer> reClinitClassHelperVersions = new ConcurrentHashMap<>();

	public HashSet<String> getFinalFields() {
		return finalFields;
	}
	public LinkedList<FieldNode> getAllStaticFields() {
		return allStaticFields;
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
		access = access & ~Opcodes.ACC_PRIVATE;
		access = access & ~Opcodes.ACC_PROTECTED;
		access = access | Opcodes.ACC_PUBLIC;
		if(!isInterface)
			access = access & ~Opcodes.ACC_FINAL;
		if((access & Opcodes.ACC_STATIC) != 0)
			allStaticFields.add(new FieldNode(access, name, desc, signature, value));
		return super.visitField(access, name, desc, signature, value);
	}

	String[] interfaces;
	String superName;

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
		reClinitClassHelperVersions.put(this.className, this.version);
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
		if (name.equals("<clinit>")) {
			clinitMethod = new MethodNode(access, name, desc, signature, exceptions);
			mv = clinitMethod;
		} else
			mv = super.visitMethod(access, name, desc, signature, exceptions);
		mv = new SystemPropertyLogger(mv);
		mv = new ReflectionFixingMV(mv, fixLdcClass, className);
		AnalyzerAdapter an = new AnalyzerAdapter(className, access, name, desc, mv);
		mv = new ReinitCheckForceMV(an, an, className, name, (access & Opcodes.ACC_STATIC) != 0, fixLdcClass, skipFrames);
		mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);

		return mv;
	}

	private MethodNode reClinitMethod;

	@Override
	public void visitEnd() {
		String classNameWField = className;
		if (isInterface)
			classNameWField = className + "$$VMVM_RESETTER";

		if (!isInterface) {
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_NEEDS_RESET, "Z", null, null);
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Constants.VMVM_RESET_FIELD, "L"+ InterfaceReinitializer.INTERNAL_NAME +";", null, null);
		}
		else
		{
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Constants.VMVM_RESET_FIELD, "L"+ InterfaceReinitializer.INTERNAL_NAME +";", null, null);
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Constants.VMVM_NEEDS_RESET, "Z", null, null);
		}

		//Create a new <clinit>
		MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();

		if (fixLdcClass) {
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
		} else
			mv.visitLdcInsn(Type.getObjectType(className));
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "clinitCalled", "(Ljava/lang/Class;)L" + InterfaceReinitializer.INTERNAL_NAME + ";", false);
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className, Constants.VMVM_RESET_FIELD, "L" + InterfaceReinitializer.INTERNAL_NAME + ";");

		if (!VMVMClassFileTransformer.isIgnoredClass(superName))
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, superName, "__vmvmReClinit", "()V", false);
		for(String i : interfaces)
		{
			if(!VMVMClassFileTransformer.isIgnoredClass(i))
			{
				mv.visitFieldInsn(Opcodes.GETSTATIC, i, Constants.VMVM_RESET_FIELD,"L"+InterfaceReinitializer.INTERNAL_NAME+";");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, InterfaceReinitializer.INTERNAL_NAME, "__vmvmReClinit", "()V", false);

			}
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
			reClinitMethod = new MethodNode(Opcodes.ACC_PUBLIC, "__vmvmReClinit", "()V", null, null);

			mv = new PutStaticHelperMV(reClinitMethod, className, fixLdcClass);
		} else
			mv = super.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "__vmvmReClinit", "()V", null, null);

		if(reClinitMethod != null)
		{
			reClinitMethods.put(className, reClinitMethod);
		}
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
		Label allDone = new Label();

		Label continu = new Label();

		mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, "Z");
		mv.visitJumpInsn(Opcodes.IFEQ, allDone);
		if (fixLdcClass) {
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		} else
			mv.visitLdcInsn(Type.getObjectType(className));

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
		mv.visitFieldInsn(Opcodes.GETSTATIC, className, Constants.VMVM_NEEDS_RESET, "Z");
		mv.visitJumpInsn(Opcodes.IFEQ, continu);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		mv.visitFieldInsn(Opcodes.PUTSTATIC, classNameWField, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		mv.visitInsn(Opcodes.ICONST_0);
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className, Constants.VMVM_NEEDS_RESET, "Z");


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

		for (FieldNode fn : allStaticFields) {
			if(isInterface & fn.value != null)
				continue; //constants dont' need to get reset
			if (fn.value != null) {
				mv.visitLdcInsn(fn.value);
				mv.visitFieldInsn(Opcodes.PUTSTATIC, className, fn.name, fn.desc);
			} else {
				switch (Type.getType(fn.desc).getSort()) {
					case Type.OBJECT:
					case Type.ARRAY:
						mv.visitInsn(Opcodes.ACONST_NULL);
						break;
					case Type.DOUBLE:
						mv.visitInsn(Opcodes.DCONST_0);
						break;
					case Type.LONG:
						mv.visitInsn(Opcodes.ICONST_0);
						mv.visitInsn(Opcodes.I2L);
						break;
					case Type.FLOAT:
						mv.visitInsn(Opcodes.FCONST_0);
						break;
					default:
						mv.visitInsn(Opcodes.ICONST_0);
						break;
				}
				mv.visitFieldInsn(Opcodes.PUTSTATIC, className, fn.name, fn.desc);
			}
		}

		if (!VMVMClassFileTransformer.isIgnoredClass(superName))
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, superName, "__vmvmReClinit", "()V", false);

		for(String i : interfaces)
		{
			if(!VMVMClassFileTransformer.isIgnoredClass(i))
			{
				mv.visitFieldInsn(Opcodes.GETSTATIC, i, Constants.VMVM_RESET_FIELD,"L"+InterfaceReinitializer.INTERNAL_NAME+";");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, InterfaceReinitializer.INTERNAL_NAME, "__vmvmReClinit", "()V", false);

			}
		}
		if (fixLdcClass) {
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
		} else
			mv.visitLdcInsn(Type.getObjectType(className));
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "reinitCalled", "(Ljava/lang/Class;)V", false);
		if (isEnum) {
			if (fixLdcClass) {
				mv.visitLdcInsn(className.replace("/", "."));
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
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

		super.visitEnd();
	}

	public void println(MethodVisitor mv, String toPrint) {
		mv.visitLdcInsn(toPrint);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "logMessage", "(Ljava/lang/String;)V", false);
	}
}
