package edu.columbia.cs.psl.vmvm.runtime.inst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;

import edu.columbia.cs.psl.vmvm.runtime.MutableInstance;
import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class ReinitCapabilityCV extends ClassVisitor {

	public static final String VMVM_RESET_IN_PROGRESS = "VMVM_RESET_IN_PROGRESS";

	public ReinitCapabilityCV(ClassVisitor parent) {
		super(Opcodes.ASM5, parent);
	}

	private boolean isInterface;
	private String className;
	private LinkedList<FieldNode> mutabilizedFields = new LinkedList<FieldNode>();
	private boolean fixLdcClass;

	public MethodNode getReClinitMethod() {
		return reClinitMethod;
	}

	public int version;

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

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		this.className = name;
		this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
		this.version = version;
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
		mv = new StaticFinalMutibleizer(mv);
		return mv;
	}

	private MethodNode reClinitMethod;

	private static final boolean CLINIT_ORDER_DEBUG = false;

	@Override
	public void visitEnd() {

		//Create a new <clinit>
		MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		for (FieldNode fn : mutabilizedFields) {
			mv.visitTypeInsn(Opcodes.NEW, MutableInstance.INTERNAL_NAME);
			mv.visitInsn(Opcodes.DUP);
			if (fn.value != null) {
				mv.visitLdcInsn(fn.value);
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, MutableInstance.INTERNAL_NAME, "<init>", "(Ljava/lang/Object;)V", false);
			} else
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, MutableInstance.INTERNAL_NAME, "<init>", "()V", false);
			mv.visitFieldInsn(Opcodes.PUTSTATIC, className, fn.name, MutableInstance.DESC);
		}
		if(!isInterface)
		{
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);
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
		String classNameWField = className;
		if (isInterface) {
			classNameWField = className+"$$VMVMRESETTER";
			reClinitMethod = new MethodNode(Opcodes.ACC_STATIC, "__vmvmReClinit", "(II)V", null, null);
			mv = reClinitMethod;
		} else
			mv = super.visitMethod(Opcodes.ACC_STATIC, "__vmvmReClinit", "(II)V", null, null);
		mv.visitCode();
//		println(mv, "Reclinit " + classNameWField);
		Label allDone = new Label();

		Label continu = new Label();
		//		if(CLINIT_ORDER_DEBUG)
		//		{
		//			super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
		//			super.visitLdcInsn("clinit going to check "+clazz+" > in " + cv.getClassName());
		//		super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		//		}		
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

		mv.visitFieldInsn(Opcodes.GETSTATIC, classNameWField, VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		Label notInInit = new Label();
		mv.visitJumpInsn(Opcodes.IFNULL, notInInit);

		mv.visitFieldInsn(Opcodes.GETSTATIC, classNameWField, VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		mv.visitJumpInsn(Opcodes.IF_ACMPEQ, continu);
		//If the Class object for C indicates that initialization is in progress for C by some other thread, then release LC and block the current thread until informed that the in-progress initialization has completed, at which time repeat this procedure.
		//If the Class object for C indicates that initialization is in progress for C by the current thread, then this must be a recursive request for initialization. Release LC and complete normally

		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "wait", "()V", false);
		mv.visitLabel(notInInit);
		mv.visitFrame(Opcodes.F_NEW, 0, new Object[0], 1, new Object[]{"java/lang/Class"});

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
		mv.visitFieldInsn(Opcodes.PUTSTATIC, classNameWField, VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
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
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "reinitCalled", "(Ljava/lang/Class;)V", false);
		for (FieldNode fn : mutabilizedFields) {
			if (fn.value != null) {
				mv.visitFieldInsn(Opcodes.GETSTATIC, className, fn.name, MutableInstance.DESC);
				mv.visitLdcInsn(fn.value);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, MutableInstance.INTERNAL_NAME, "put", "(Ljava/lang/Object;)V", false);
			}
		}
		int maxStack = 40;
		int maxLocals = 40;
		if (clinitMethod != null)
		{
			final Label finishedClinitCode = new Label();
			mv.visitInsn(Opcodes.POP);
			maxStack = (clinitMethod.maxStack > 3 ? clinitMethod.maxStack : 0);
			maxLocals = (clinitMethod.maxLocals > 2 ? clinitMethod.maxLocals : 0);
			clinitMethod.instructions.accept(new MethodVisitor(Opcodes.ASM5,mv) {
				HashMap<Label, Label> newLabels = new HashMap<Label, Label>();
				private Label remapLabel(Label l)
				{
					if(newLabels.containsKey(l))
						return newLabels.get(l);
					Label r = new Label();
					newLabels.put(l, r);
					return r;
				}
				@Override
				public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
					for(int i = 0; i < labels.length;i++)
						labels[i] = remapLabel(labels[i]);
					super.visitTableSwitchInsn(min, max, dflt, labels);
				}
				@Override
				public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
					super.visitTryCatchBlock(remapLabel(start), remapLabel(end), remapLabel(handler), type);
				}
				@Override
				public void visitLineNumber(int line, Label start) {
					super.visitLineNumber(line, remapLabel(start));
				}
				@Override
				public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
					super.visitLocalVariable(name, desc, signature, remapLabel(start), remapLabel(end), index);
				}
				@Override
				public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
					for(int i = 0; i < labels.length;i++)
						labels[i] = remapLabel(labels[i]);
					super.visitLookupSwitchInsn(dflt, keys, labels);
				}
				@Override
				public void visitJumpInsn(int opcode, Label label) {
					super.visitJumpInsn(opcode, remapLabel(label));
				}
				@Override
				public void visitLabel(Label label) {
					super.visitLabel(remapLabel(label));
				}
				@Override
				public void visitInsn(int opcode) {
					if(opcode == Opcodes.RETURN)
					{
						super.visitJumpInsn(Opcodes.GOTO, finishedClinitCode);
					}
					else
						super.visitInsn(opcode);
				}
				@Override
				public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
					super.visitMethodInsn(opcode, owner, name, desc, itf);
				}
			});
			mv.visitLabel(finishedClinitCode);
			mv.visitFrame(Opcodes.F_NEW, 0, new Object[]{}, 0, new Object[0]);
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
		
		//If the execution of the class or interface initialization method completes normally, then acquire LC, label the Class object for C as fully initialized, notify all waiting threads, release LC, and complete this procedure normally.
		mv.visitInsn(Opcodes.DUP);
		mv.visitInsn(Opcodes.MONITORENTER);
		mv.visitInsn(Opcodes.ACONST_NULL);
		mv.visitFieldInsn(Opcodes.PUTSTATIC, classNameWField, VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "notifyAll", "()V", false);
//
		mv.visitLabel(continu);
		mv.visitFrame(Opcodes.F_NEW, 0, new Object[]{}, 1, new Object[]{"java/lang/Class"});
		mv.visitInsn(Opcodes.MONITOREXIT);
		mv.visitLabel(allDone);
		mv.visitFrame(Opcodes.F_NEW, 0, new Object[]{}, 0, new Object[0]);

		
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(maxStack, maxLocals);
		mv.visitEnd();

		super.visitEnd();
	}
	public void println(MethodVisitor mv, String toPrint) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(toPrint + " : ");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print",
                "(Ljava/lang/String;)V", false);

        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                "()Ljava/lang/Thread;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);
    }
}
