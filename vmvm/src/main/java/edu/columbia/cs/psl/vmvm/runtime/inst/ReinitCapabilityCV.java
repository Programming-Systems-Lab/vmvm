package edu.columbia.cs.psl.vmvm.runtime.inst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.sound.midi.Instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.Textifier;

import com.sun.org.apache.bcel.internal.util.ClassStack;

import edu.columbia.cs.psl.vmvm.runtime.ClassState;
import edu.columbia.cs.psl.vmvm.runtime.Instrumenter;
import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;
import edu.columbia.cs.psl.vmvm.runtime.PreMain;
import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;
import edu.columbia.cs.psl.vmvm.runtime.VMVMClassFileTransformer;

public class ReinitCapabilityCV extends ClassVisitor {

	public ReinitCapabilityCV(ClassVisitor parent) {
		super(Opcodes.ASM5, parent);
	}

	private boolean isInterface;
	private String className;
	private boolean skipFrames;
	private LinkedList<FieldNode> mutabilizedFields = new LinkedList<FieldNode>();
	private boolean fixLdcClass;
	private boolean isEnum;
	
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
		if ((access & Opcodes.ACC_STATIC) != 0 && (fieldType.getSort() == Type.OBJECT || fieldType.getSort() == Type.ARRAY)) {
			mutabilizedFields.add(new FieldNode(access, name, desc, signature, value));
			value = null;
			desc = MutableInstance.DESC;
			signature = MutableInstance.DESC;
		}
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
		mv = new StaticFinalMutibleizer(mv, ownersOfStaticFields, skipFrames);
		mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
		mv = new ReflectionHackMV(mv, fixLdcClass, className);
		return mv;
	}

	private MethodNode reClinitMethod;


	@Override
	public void visitEnd() {
		String classNameWField = className;
		if (isInterface)
			classNameWField = className + "$$VMVMRESETTER";

		super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, Constants.VMVM_NEEDS_RESET, ClassState.DESC, null, null);

		if (!isInterface) {
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);
			for (String s : ownersOfStaticFields) {
				super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, s.replace("/", "_") + Constants.TOTAL_STATIC_CLASSES_CHECKED, "Z", null, 1);
			}
		}

		//Create a new <clinit>
		MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		mv.visitTypeInsn(Opcodes.NEW, ClassState.INTERNAL_NAME);
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ClassState.INTERNAL_NAME, "<init>", "()V", false);
		if (isInterface) {
			mv.visitInsn(Opcodes.DUP);
			mv.visitInsn(Opcodes.ICONST_1);
			mv.visitFieldInsn(Opcodes.PUTFIELD, ClassState.INTERNAL_NAME, "isInterface", "Z");
		}
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className, Constants.VMVM_NEEDS_RESET, ClassState.DESC);
//		if (isInterface) {
//			mv.visitLdcInsn(classNameWField.replace("/", "."));
////			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "lookupInterfaceClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
//		} else {
			if (fixLdcClass) {
				mv.visitLdcInsn(className.replace("/", "."));
				mv.visitInsn(Opcodes.ICONST_0);
				mv.visitLdcInsn(className.replace("/", "."));
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
			} else
				mv.visitLdcInsn(Type.getObjectType(className));
//		}
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "clinitCalled", "(Ljava/lang/Class;)V", false);

		for (FieldNode fn : mutabilizedFields) {
			mv.visitTypeInsn(Opcodes.NEW, MutableInstance.INTERNAL_NAME);
			mv.visitInsn(Opcodes.DUP);
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
			if (fn.value != null) {
				mv.visitLdcInsn(fn.value);
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, MutableInstance.INTERNAL_NAME, "<init>", "(Ljava/lang/Class;Ljava/lang/Object;)V", false);
			} else
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, MutableInstance.INTERNAL_NAME, "<init>", "(Ljava/lang/Class;)V", false);
			mv.visitFieldInsn(Opcodes.PUTSTATIC, className, fn.name, MutableInstance.DESC);
		}

		if (clinitMethod != null) {
			if (clinitMethod.maxStack < 4)
				clinitMethod.maxStack = 4;
			clinitMethod.accept(mv);
		} else {
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(4, 0);
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
		
		if(clinitMethod != null)
		{
			if(clinitMethod.tryCatchBlocks != null)
			{
				for(Object o :clinitMethod.tryCatchBlocks)
				{
					((TryCatchBlockNode)o).accept(reLabeler);
				}
			}
		}
		//		println(mv, "Reclinit " + classNameWField);
		Label allDone = new Label();

		Label continu = new Label();
		if(className.equals("org/apache/juli/logging/DirectJDKLog"))
		{
			println(mv, "DirectJDKLog reinit check start");
		}
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
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		} else
			mv.visitLdcInsn(Type.getObjectType(className));


		mv.visitInsn(Opcodes.DUP);
		if(className.equals("org/apache/juli/logging/DirectJDKLog"))
		{
			println(mv, "DirectJDKLog reinit barrier start");
		}
		mv.visitInsn(Opcodes.MONITORENTER);
	
		mv.visitFieldInsn(Opcodes.GETSTATIC, classNameWField, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		Label notInInit = new Label();
		mv.visitJumpInsn(Opcodes.IFNULL, notInInit);
	
		mv.visitFieldInsn(Opcodes.GETSTATIC, classNameWField, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		mv.visitJumpInsn(Opcodes.IF_ACMPEQ, continu);
		//If the Class object for C indicates that initialization is in progress for C by some other thread, then release LC and block the current thread until informed that the in-progress initialization has completed, at which time repeat this procedure.
		//If the Class object for C indicates that initialization is in progress for C by the current thread, then this must be a recursive request for initialization. Release LC and complete normally
		if(className.equals("org/apache/juli/logging/DirectJDKLog"))
		{
			println(mv, "DirectJDKLog reinit wait");
		}
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

		if (fixLdcClass) {
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitLdcInsn(className.replace("/", "."));
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
		} else
			mv.visitLdcInsn(Type.getObjectType(className));


		for (String s : ownersOfStaticFields) {
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitFieldInsn(Opcodes.PUTSTATIC, classNameWField, s.replace("/", "_") + Constants.TOTAL_STATIC_CLASSES_CHECKED, "Z");
		}
		for (FieldNode fn : mutabilizedFields) {
			if (fn.value != null) {
				mv.visitFieldInsn(Opcodes.GETSTATIC, className, fn.name, MutableInstance.DESC);
				mv.visitLdcInsn(fn.value);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MutableInstance.INTERNAL_NAME, "put", "(Ljava/lang/Object;)V", false);
			}
		}
		if(className.equals("org/apache/juli/logging/DirectJDKLog"))
		{
			println(mv, "DirectJDKLog reinit call parent");
		}
		if(!VMVMClassFileTransformer.isIgnoredClass(superName))
		{
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, superName, "__vmvmReClinit", "()V", false);
		}
		if(className.equals("org/apache/juli/logging/DirectJDKLog"))
		{
			println(mv, "DirectJDKLog reinit called parent");
		}
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "reinitCalled", "(Ljava/lang/Class;)V", false);
		if(className.equals("org/apache/juli/logging/DirectJDKLog"))
		{
			println(mv, "DirectJDKLog reinit logged");
		}
		if(isEnum)
		{
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
		if(className.equals("org/apache/juli/logging/DirectJDKLog"))
		{
			println(mv, "DirectJDKLog reinit running clinit code");
		}
		if (clinitMethod != null) {
			mv.visitInsn(Opcodes.POP);
			maxStack = (clinitMethod.maxStack > 3 ? clinitMethod.maxStack : 0);
			maxLocals = (clinitMethod.maxLocals > 2 ? clinitMethod.maxLocals : 0);
			clinitMethod.instructions.accept(reLabeler);
			mv.visitLabel(finishedClinitCode);
			if (!skipFrames)
				mv.visitFrame(Opcodes.F_NEW, 0, new Object[] {}, 0, new Object[0]);
			if (fixLdcClass) {
				mv.visitLdcInsn(className.replace("/", "."));
				mv.visitInsn(Opcodes.ICONST_0);
				mv.visitLdcInsn(className.replace("/", "."));
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false);
			} else
				mv.visitLdcInsn(Type.getObjectType(className));
		}
		if(className.equals("org/apache/juli/logging/DirectJDKLog"))
		{
			println(mv, "DirectJDKLog exit notify");
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
		if(className.equals("org/apache/juli/logging/DirectJDKLog"))
		{
			println(mv, "DirectJDKLog reinit return");
		}
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
