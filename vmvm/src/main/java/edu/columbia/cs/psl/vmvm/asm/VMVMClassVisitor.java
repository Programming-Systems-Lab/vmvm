package edu.columbia.cs.psl.vmvm.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.VMState;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.asm.mvs.ChrootMethodVisitor;
import edu.columbia.cs.psl.vmvm.asm.mvs.StaticFieldIsolatorMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.StaticFinalMutibleizer;
import edu.columbia.cs.psl.vmvm.asm.mvs.UnconditionalChrootMethodVisitor;
import edu.columbia.cs.psl.vmvm.asm.struct.EqFieldInsnNode;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodInsnNode;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodNode;
import edu.columbia.cs.psl.vmvm.chroot.ChrootUtils;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Label;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.GeneratorAdapter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.AnnotationNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.FieldInsnNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.FieldNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.vmvm.struct.MutableInstance;

public abstract class VMVMClassVisitor extends ClassVisitor implements Opcodes, Constants  {
	protected String className;
	private boolean useVMState;
	public VMVMClassVisitor(int api, ClassVisitor cv, boolean useVMState) {
		super(api, cv);
		this.useVMState = useVMState;
	}
	
	private boolean isClass;
	private int version;
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if((access & Opcodes.ACC_PRIVATE) != 0)
		{
//			System.out.println(access);
//			System.out.println(name);
			access = access & ~Opcodes.ACC_PRIVATE;
			access = access + Opcodes.ACC_PUBLIC;
//			System.out.println(access);
		}
		super.visit(version, access, name, signature, superName, interfaces);
		this.version = version;
		isClass = (access & Opcodes.ACC_INTERFACE) == 0;
		isEnum = (access & Opcodes.ACC_ENUM) != 0;

	}
	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if((access & Opcodes.ACC_PRIVATE) != 0)
		{
//			System.out.println(access);
//			System.out.println(name);
			access = access & ~Opcodes.ACC_PRIVATE;
			access = access + Opcodes.ACC_PUBLIC;
//			System.out.println(access);
		}
		super.visitInnerClass(name, outerName, innerName, access);
	}
	public int getVersion() {
		return version;
	}
	protected HashMap<EqMethodNode, MethodNode> renamedMethodsToCover = new HashMap<>();

	private HashSet<EqMethodInsnNode> chrootMethodsToGen = new HashSet<>();

	public void addChrootMethodToGen(EqMethodInsnNode mi) {
		chrootMethodsToGen.add(mi);
	}

	public void addStaticCloneWrapper(String cloneMethodName, HashSet<EqFieldInsnNode> fieldsToClone) {
		staticCloneMethodsToGen.put(cloneMethodName, fieldsToClone);
	}
	private HashMap<String, HashSet<EqFieldInsnNode>> staticCloneMethodsToGen = new HashMap<>();

//	protected void generateFinalHackMethod()
//	{
//		MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, Constants.VMVM_STATIC_RESET_METHOD, "(L"+className+";)V", null,null);
//		GeneratorAdapter gmv = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC, Constants.VMVM_STATIC_RESET_METHOD, "(L"+className+";)V");
//		
//		for(Object o : Instrumenter.getClassNode(className).fields)
//		{
//			FieldNode fn = (FieldNode) o;
//			if((fn.access &  Opcodes.ACC_STATIC) == 0)
//			{
//				if((fn.access &  Opcodes.ACC_FINAL) == 0)
//				{
//					gmv.loadThis();
//					gmv.loadArg(0);
//					gmv.visitFieldInsn(GETFIELD, className, fn.name, fn.desc);
//					gmv.visitFieldInsn(PUTFIELD, className, fn.name, fn.desc);
//				}
//				else if(fn.desc.length() > 2 && Instrumenter.instrumentedClasses.containsKey(Type.getType(fn.desc).getInternalName()))
//				{
//					gmv.loadThis();
//					gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//					gmv.loadArg(0);
//					gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//					gmv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getType(fn.desc).getInternalName(), Constants.VMVM_STATIC_RESET_METHOD, "("+fn.desc+")V");
//				}
//				else if(fn.desc.startsWith("["))
//				{
//					gmv.loadArg(0);
//					gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//					gmv.push(0);
//					gmv.loadThis();
//					gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//					gmv.push(0);
//					gmv.loadArg(0);
//					gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//					gmv.visitInsn(ARRAYLENGTH);
////					System.arraycopy(src, srcPos, dest, destPos, length)
//					
//					gmv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
//				} else if (fn.desc.length() > 1) {
//					try {
//						Class<?> c = Class.forName(Type.getType(fn.desc)
//								.getClassName());
//						if (Collection.class.isAssignableFrom(c)) {
//							gmv.loadThis();
//							gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//							gmv.visitInsn(DUP);
//							String fieldType = Type.getType(fn.desc).getInternalName();
//							gmv.visitMethodInsn(INVOKEVIRTUAL, fieldType, "clear", "()V");
//							gmv.loadArg(0);
//							gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//							gmv.visitMethodInsn(INVOKEVIRTUAL, fieldType, "addAll", "("+Type.getDescriptor(Collection.class)+")Z");
//							gmv.visitInsn(POP);
//						}
//						else if(Map.class.isAssignableFrom(c))
//						{
//							gmv.loadThis();
//							gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//							gmv.visitInsn(DUP);
//							String fieldType = Type.getType(fn.desc).getInternalName();
//							gmv.visitMethodInsn(INVOKEVIRTUAL, fieldType, "clear", "()V");
//							gmv.loadArg(0);
//							gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//							gmv.visitMethodInsn(INVOKEVIRTUAL, fieldType, "putAll", "("+Type.getDescriptor(Map.class)+")V");
//						}
//						else if(fn.desc.equals("Ljava/lang/ThreadLocal;"))
//						{
//							gmv.loadThis();
//							gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//							gmv.loadArg(0);
//							gmv.visitFieldInsn(Opcodes.GETFIELD, className, fn.name, fn.desc);
//							gmv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;");
//							gmv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "set", "(Ljava/lang/Object;)V");
//						}
//					} catch (Exception ex)
//					{
//						ex.printStackTrace();
//					}
//				}
//			}
//		}
//		gmv.returnValue();
//		gmv.visitMaxs(0, 0);
//		gmv.visitEnd();
//
//	}
	protected void generateClinit(boolean callClinit)
	{
		MethodVisitor mv = super.visitMethod( Opcodes.ACC_STATIC, "<clinit>", "()V", null,null);
		GeneratorAdapter gmv = new GeneratorAdapter(mv,  Opcodes.ACC_STATIC, "<clinit>", "()V");
		StaticFinalMutibleizer fmv = new StaticFinalMutibleizer(gmv,  Opcodes.ACC_STATIC, className, "<clinit>", "()V");
		if(!isAClass())
			for(Object o : Instrumenter.getClassNode(className).fields)
			{
				FieldNode fn = (FieldNode) o;
				if(fn.value != null)
				{
					fmv.visitLdcInsn(fn.value);
					fmv.visitFieldInsn(PUTSTATIC, className, fn.name, fn.desc);
				}
				else
				{
					gmv.visitTypeInsn(NEW, Type.getInternalName(MutableInstance.class));
					gmv.visitInsn(DUP);
					gmv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(MutableInstance.class), "<init>", "()V", false);
					gmv.visitFieldInsn(PUTSTATIC, className, fn.name, Type.getDescriptor(MutableInstance.class));
				}
			}
		String classWithResetMethod = className;
		if(!isAClass())
			classWithResetMethod += "$vmvmReseter";
//		if(callClinit)
		{
//			if(StaticFieldIsolatorMV.CLINIT_ORDER_DEBUG)
//			{
//				Label l0 = new Label();
//				Label l1 = new Label();
//				Label l2 = new Label();
//				Label l3 = new Label();
//				gmv.visitTryCatchBlock(l0, l1, l2, "java/lang/Throwable");
//				gmv.visitLabel(l0);
//				gmv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//				gmv.visitLdcInsn("called real, actual clinit in " + className);
//				gmv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//				gmv.visitLabel(l1);
//				gmv.visitJumpInsn(GOTO, l3);
//				gmv.visitLabel(l2);
//				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V");
//				gmv.visitLabel(l3);
//			}
			gmv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
			gmv.visitFieldInsn(PUTSTATIC, classWithResetMethod, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
			gmv.visitMethodInsn(INVOKESTATIC, classWithResetMethod, Constants.VMVM_STATIC_RESET_METHOD, "()V", false);			
			gmv.visitInsn(ACONST_NULL);
			gmv.visitFieldInsn(PUTSTATIC, classWithResetMethod, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;");
		}
		gmv.returnValue();
		gmv.visitMaxs(0, 0);
		gmv.visitEnd();
		
		if(!callClinit && isAClass())
		{
			mv = super.visitMethod( Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, Constants.VMVM_STATIC_RESET_METHOD, "()V", null,null);
			gmv = new GeneratorAdapter(mv,  Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, Constants.VMVM_STATIC_RESET_METHOD, "()V");
			StaticFieldIsolatorMV gmvv = new StaticFieldIsolatorMV(gmv, Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, Constants.VMVM_STATIC_RESET_METHOD, "()V", this, null);
			gmvv.visitCode();
			gmvv.visitInsn(RETURN);
			gmv.visitMaxs(0, 0);
			gmvv.visitEnd();

		}

	}
//	protected void generateStaticFieldCloneMethods(boolean callClinit)
//	{
//		MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_STATIC_RESET_METHOD, "()V", null,null);
//		GeneratorAdapter gmv = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_STATIC_RESET_METHOD, "()V");
//		if(callClinit)
//		gmv.visitMethodInsn(INVOKESTATIC, className, "_clinit", "()V");
//		for(EqFieldInsnNode fn : staticFieldsToClone)
//		{
//			if(fn.name.equals("$assertionsDisabled"))
//				continue;
//			gmv.visitFieldInsn(Opcodes.GETSTATIC, fn.owner, fn.name + Constants.BEEN_CLONED_FIELD, "Z");
//			Label continu = new Label();
//			gmv.visitJumpInsn(IFEQ, continu);
//			if(fn.isFinal())
//			{
//				if(fn.desc.startsWith("["))
//				{
//					gmv.visitFieldInsn(Opcodes.GETSTATIC, fn.owner, fn.name+Constants.SANDBOX_SUFFIX, fn.desc);
//					gmv.push(0);
//					gmv.visitFieldInsn(Opcodes.GETSTATIC, fn.owner, fn.name, fn.desc);
//					gmv.push(0);
//					gmv.visitFieldInsn(Opcodes.GETSTATIC, fn.owner, fn.name, fn.desc);
//					gmv.visitInsn(ARRAYLENGTH);
////					System.arraycopy(src, srcPos, dest, destPos, length)
//					
//					gmv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
//				}
//				else if(fn.desc.length() > 2 && Instrumenter.instrumentedClasses.containsKey(Type.getType(fn.desc).getInternalName()))
//				{
//					gmv.visitFieldInsn(Opcodes.GETSTATIC, fn.owner, fn.name, fn.desc);
//					gmv.visitFieldInsn(Opcodes.GETSTATIC, fn.owner, fn.name+Constants.SANDBOX_SUFFIX, fn.desc);
//					gmv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getType(fn.desc).getInternalName(), Constants.VMVM_STATIC_RESET_METHOD, "("+fn.desc+")V");
//				}
//				else if(fn.desc.length() > 2){
//					try {
//						Class<?> c = Class.forName(Type.getType(fn.desc)
//								.getClassName());
//						if (Collection.class.isAssignableFrom(c)) {
//							gmv.visitFieldInsn(Opcodes.GETSTATIC, className, fn.name, fn.desc);
//							gmv.visitInsn(DUP);
//							String fieldType = Type.getType(fn.desc).getInternalName();
//							gmv.visitMethodInsn(INVOKEVIRTUAL, fieldType, "clear", "()V");
//							gmv.visitFieldInsn(Opcodes.GETSTATIC, className, fn.name+Constants.SANDBOX_SUFFIX, fn.desc);
//							gmv.visitMethodInsn(INVOKEVIRTUAL, fieldType, "addAll", "("+Type.getDescriptor(Collection.class)+")Z");
//							gmv.visitInsn(POP);
//						}
//						else if(Map.class.isAssignableFrom(c))
//						{
//							gmv.visitFieldInsn(Opcodes.GETSTATIC, className, fn.name, fn.desc);
//							gmv.visitInsn(DUP);
//							String fieldType = Type.getType(fn.desc).getInternalName();
//							gmv.visitMethodInsn(INVOKEVIRTUAL, fieldType, "clear", "()V");
//							gmv.visitFieldInsn(Opcodes.GETSTATIC, className, fn.name+Constants.SANDBOX_SUFFIX, fn.desc);
//							gmv.visitMethodInsn(INVOKEVIRTUAL, fieldType, "putAll", "("+Type.getDescriptor(Map.class)+")V");
//						}
//						else if(fn.desc.equals("Ljava/lang/ThreadLocal;"))
//						{
//							gmv.visitFieldInsn(Opcodes.GETSTATIC, className, fn.name, fn.desc);
//							gmv.visitFieldInsn(Opcodes.GETSTATIC, className, fn.name+Constants.SANDBOX_SUFFIX, fn.desc);
//							gmv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;");
//							gmv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ThreadLocal", "set", "(Ljava/lang/Object;)V");
//						}
//					} catch (Exception ex)
//					{
//						ex.printStackTrace();
//					}
//				}
//			}
//			else
//			{
//				gmv.visitFieldInsn(Opcodes.GETSTATIC, fn.owner, fn.name+Constants.SANDBOX_SUFFIX, fn.desc);
//				gmv.visitFieldInsn(Opcodes.PUTSTATIC, fn.owner, fn.name, fn.desc);
//			}
//			gmv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//			gmv.visitLdcInsn("reseting>"+fn.owner+ "."+fn.name+" "+fn.desc);
//			gmv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
////			gmv.visitInsn(ICONST_0);
////			gmv.visitFieldInsn(Opcodes.PUTSTATIC, fn.owner, fn.name + Constants.BEEN_CLONED_FIELD, "Z");
//			gmv.visitLabel(continu);
//		}
//		gmv.returnValue();
//		gmv.visitMaxs(0, 0);
//		gmv.visitEnd();
//	}
	
	protected void generateStaticCloneMethods()
	{
		for (String methodName : staticCloneMethodsToGen.keySet()) {
			MethodVisitor mv = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, "(I)V", null, null);
			GeneratorAdapter gmv = new GeneratorAdapter(mv, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, "(I)V");
			//			InvivoAdapter iv = new InvivoAdapter(Opcodes.ASM4, gmv, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, mName, captureDesc, this.className);
			gmv.visitCode();
			if(staticCloneMethodsToGen != null && methodName != null && staticCloneMethodsToGen.get(methodName) != null)
			for (FieldInsnNode fn : staticCloneMethodsToGen.get(methodName)) {
				gmv.visitFieldInsn(Opcodes.GETSTATIC, fn.owner, fn.name, fn.desc);
				gmv.loadArg(0);
				Label[] lbls = new Label[Instrumenter.MAX_SANDBOXES + 1];
				for (int i = 0; i < lbls.length; i++)
					lbls[i] = new Label();
				Label endOfTbl = new Label();
				gmv.visitTableSwitchInsn(0, Instrumenter.MAX_SANDBOXES, lbls[0], lbls);
				for (int i = 0; i < lbls.length; i++) {
					gmv.visitLabel(lbls[i]);
					gmv.visitFieldInsn(Opcodes.PUTSTATIC, fn.owner, fn.name + "_vmvm_" + i, fn.desc);
					gmv.visitJumpInsn(GOTO, endOfTbl);
				}
				gmv.visitLabel(endOfTbl);
			}
			gmv.returnValue();
			gmv.visitMaxs(0, 0);
			gmv.visitEnd();
		}
	}

	protected void generateChrootMethods()
	{
		Object[] o = new Object[4];
		o[1] = null;
		generateRegularChrootMethods();
		generateParentConstructorChrootMethods();
	}
	private void generateParentConstructorChrootMethods() {
		for (EqMethodInsnNode m : chrootMethodsToGen) {
			if(m.flag != EqMethodInsnNode.FLAG_SUPER_INVOKE_CHROOT)
				continue;
			String mName = ChrootUtils.getCaptureInitParentMethodName(m);
			String captureDesc = "(";

			for (Type t : Type.getArgumentTypes(m.desc))
				captureDesc += t.getDescriptor();
			
			if(useVMState)
				captureDesc += Type.getDescriptor(VMState.class);

			captureDesc += ")[Ljava/lang/Object;";

			MethodVisitor mv = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, mName, captureDesc, null, null);
			GeneratorAdapter gmv = new GeneratorAdapter(mv, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, mName, captureDesc);
			gmv.visitCode();

			String data = ChrootMethodVisitor.fsMethods.get(m.owner + "." + m.name + m.desc);

			if (data.length() == 1) {
				gmv.visitIntInsn(BIPUSH, Type.getArgumentTypes(m.desc).length);
				gmv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
				int n = Integer.parseInt(data);
				Type[] args = Type.getArgumentTypes(captureDesc);
				for (int i = 0; i < args.length - (useVMState ? 1 : 0); i++) {
					gmv.visitInsn(DUP);
					gmv.visitIntInsn(BIPUSH, i);
					gmv.loadArg(i);
					if (i == n) {
						if(useVMState)
							gmv.loadArg(args.length - 1);
						gmv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "ensureSafePath", "(Ljava/lang/String;" + (useVMState ? Type.getDescriptor(VMState.class) :"")
								+ ")Ljava/lang/String;", false);
					}
					gmv.visitInsn(AASTORE);
				}
				gmv.returnValue();

			} else if (data.equals("URI")) {
				Type[] args = Type.getArgumentTypes(captureDesc);
				for (int i = 0; i < args.length; i++)
					gmv.loadArg(i);

				gmv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "ensureSafeURIArrayReturn", captureDesc, false);
			} else if (data.equals("URL")) {
				Type[] args = Type.getArgumentTypes(captureDesc);
				for (int i = 0; i < args.length; i++)
					gmv.loadArg(i);

				gmv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "ensureSafeURLArrayReturn", captureDesc, false);

			} else if (data.equals("FS")) {
				System.out.println("Warn: app uses " + m.owner+"."+m.name + m.desc);
			}
			gmv.returnValue();
			gmv.visitMaxs(0, 0);
			gmv.visitEnd();
		}
	}

	private void generateRegularChrootMethods() {
		for (EqMethodInsnNode m : chrootMethodsToGen) {
			if(m.flag == EqMethodInsnNode.FLAG_SUPER_INVOKE_CHROOT)
				continue;
			String mName = ChrootUtils.getCaptureMethodName(m);
			String captureDesc = "(";

			for (Type t : Type.getArgumentTypes(m.desc))
				captureDesc += t.getDescriptor();
			
			if(useVMState)
				captureDesc += Type.getDescriptor(VMState.class);
			if (m.name.equals("<init>"))
				captureDesc += ")" + Type.getReturnType("L" + m.owner + ";").getDescriptor();
			else
				captureDesc += ")" + Type.getReturnType(m.desc).getDescriptor();

			MethodVisitor mv = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, mName, captureDesc, null, null);
			GeneratorAdapter gmv = new GeneratorAdapter(mv, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, mName, captureDesc);
			gmv.visitCode();

			String originalDesc = m.desc;
			if (m.name.equals("<init>")) {
				if (!m.owner.contains("URL") && !m.owner.contains("URI")) {
					gmv.visitTypeInsn(NEW, Type.getReturnType(m.owner).getInternalName());
					gmv.visitInsn(DUP);
				}
				m.desc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(m.desc));
			}
			String data = ChrootMethodVisitor.fsMethods.get(m.owner + "." + m.name + m.desc);

			if (data.length() == 1) {
				int n = Integer.parseInt(data);
				Type[] args = Type.getArgumentTypes(captureDesc);

				for (int i = 0; i < args.length - (useVMState ? 1 : 0); i++) {
					gmv.loadArg(i);
					if (i == n) {
						if(useVMState)
							gmv.loadArg(args.length - 1);
						gmv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "ensureSafePath", "(Ljava/lang/String;" + (useVMState ? Type.getDescriptor(VMState.class) :"")
								+ ")Ljava/lang/String;", false);
					}

				}
				if(UnconditionalChrootMethodVisitor.fsOutputMethods.contains(m.owner + "." + m.name + m.desc))
				{
					UnconditionalChrootMethodVisitor.sandboxCallToFSOutputMethod(gmv, m.getOpcode(), m.owner, m.name, m.desc);
				}
				else
					gmv.visitMethodInsn(m.getOpcode(), m.owner, m.name, m.desc, false);

			} else if (data.equals("URI")) {
				Type[] args = Type.getArgumentTypes(captureDesc);
				for (int i = 0; i < args.length; i++)
					gmv.loadArg(i);

				gmv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "ensureSafeURI", captureDesc, false);
			} else if (data.equals("URL")) {
				Type[] args = Type.getArgumentTypes(captureDesc);
				for (int i = 0; i < args.length; i++)
					gmv.loadArg(i);

				gmv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "ensureSafeURL", captureDesc, false);

			} else if (data.equals("FS")) {

			}
			gmv.returnValue();
			gmv.visitMaxs(0, 0);
			gmv.visitEnd();
		}
	}

	protected void generateCoverMethods()
	{
		for (MethodNode m : renamedMethodsToCover.keySet()) {
			//			if(Instrumenter.getMethodNode(thisClassInfo, m.name, m.desc) != null)
			//			{
			//				continue;
			//			}
			List<String> list = (List<String>) m.exceptions;
			String[] exceptions = new String[list.size()];
			exceptions = list.toArray(exceptions);
			MethodVisitor mv = super.visitMethod(m.access, m.name, m.desc, m.signature, exceptions);
			GeneratorAdapter gmv = new GeneratorAdapter(mv, m.access, m.name, m.desc);
			MethodNode mOrig = renamedMethodsToCover.get(m);
			for (Object o : mOrig.visibleAnnotations) {
				AnnotationNode an = (AnnotationNode) o;
				an.accept(gmv.visitAnnotation(an.desc, true));
			}
			for (Object o : mOrig.invisibleAnnotations) {
				AnnotationNode an = (AnnotationNode) o;
				an.accept(gmv.visitAnnotation(an.desc, false));
			}
			gmv.visitCode();
			if ((m.access & Opcodes.ACC_ABSTRACT) == 0) {
				if ((m.access & Opcodes.ACC_STATIC) == 0) //not-static
				{
					gmv.loadThis();
				}
				Type[] args = Type.getArgumentTypes(m.desc);
				for (int i = 0; i < args.length; i++) {
					gmv.loadArg(i);
				}
				gmv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "getVMState", "()" + Type.getDescriptor(VMState.class), false);
				gmv.visitMethodInsn((m.access & Opcodes.ACC_STATIC) != 0 ? Opcodes.INVOKESTATIC : m.name.equals("<init>") ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, className,
						(!"<init>".equals(renamedMethodsToCover.get(m).name) && !"<clinit>".equals(renamedMethodsToCover.get(m).name) ? "_" : "") + renamedMethodsToCover.get(m).name, renamedMethodsToCover.get(m).desc, false);

				gmv.returnValue();
			}
			gmv.visitMaxs(0, 0);
			gmv.visitEnd();
		}
	}


	public String getClassName() {
		return className;
	}

	public abstract boolean hasExtraInterface();

	public abstract byte[] getExtraInterface();

	private HashSet<EqFieldInsnNode> staticFieldsToClone = new HashSet<>();
	public void addStaticFieldsToClone(
			HashSet<EqFieldInsnNode> staticFieldsToClone) {
		this.staticFieldsToClone.addAll(staticFieldsToClone);
	}
	public boolean isAClass() {
		return isClass;
	}
	private boolean isEnum;
	public boolean isEnum() {
		return isEnum;
	}
}
