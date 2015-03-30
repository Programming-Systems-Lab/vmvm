package edu.columbia.cs.psl.vmvm.asm;

import java.util.LinkedList;

import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.VMState;
import edu.columbia.cs.psl.vmvm.VMVMInstrumented;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.asm.mvs.ChrootMethodVisitor;
import edu.columbia.cs.psl.vmvm.asm.mvs.ClassDefineInterceptMethodVisitor;
import edu.columbia.cs.psl.vmvm.asm.mvs.InvivoAdapter;
import edu.columbia.cs.psl.vmvm.asm.mvs.ReflectionHackMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.SandboxPropogatingMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.StaticFieldIsolatorMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.TypeRememberingLocalVariableSorter;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.AnnotationVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.ClassWriter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.FieldVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.GeneratorAdapter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.JSRInlinerAdapter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.AnnotationNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.ClassNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.MethodNode;

public class InterceptingClassVisitor extends VMVMClassVisitor {

	//	private static Logger logger = Logger.getLogger(InterceptingClassVisitor.class);

	private boolean isAClass = true;

	private boolean runIMV = true;

	private boolean willRewrite = false;
	private ClassNode thisClassInfo;
	private boolean generateVMVMInterface = false;

	public InterceptingClassVisitor(ClassVisitor cv, ClassNode thisClassInfo, boolean generateVMVMInterface) {
		super(Opcodes.ASM5, cv,true);
		this.thisClassInfo = thisClassInfo;
		this.generateVMVMInterface = generateVMVMInterface;
		if (generateVMVMInterface && (thisClassInfo.access & Opcodes.ACC_INTERFACE) != 0) {
			intfcCw = new ClassWriter(0);
			intfcCV = new InterceptingClassVisitor(intfcCw, null, false);
		}
	}

	private ClassWriter intfcCw;
	private InterceptingClassVisitor intfcCV;
	private boolean skipClass = false;

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (generateVMVMInterface) {
			for (int i = 0; i < interfaces.length; i++) {
				interfaces[i] = Instrumenter.remapInterface(interfaces[i]);
			}
		}
		super.visit(version, access, name, signature, superName, interfaces);

		//		if(signature != null && signature.startsWith("Ljava/lang/Enum"))
		//		{
		//			addAllClassMethodsToCoverList("java/lang/Enum");
		//		}
		//		if(!Instrumenter.instrumentedClasses.containsKey(superName))
		//		{
		//			addAllClassMethodsToCoverList(superName);
		//		}
		//		for(String s : interfaces)
		//		{
		//			if(!Instrumenter.instrumentedClasses.containsKey(s))
		//				addAllClassMethodsToCoverList(s);
		//		}
		this.className = name;
		skipClass = shouldIgnoreClass(name);
		if ((access & Opcodes.ACC_INTERFACE) != 0)
			isAClass = false;

		if (!skipClass && !isAClass && generateVMVMInterface) {
			super.visitInnerClass(Instrumenter.getInstrumentedInterfaceName(name), name, "vmvm", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT + ACC_INTERFACE);
			intfcCV.visit(version, ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT + ACC_INTERFACE, Instrumenter.getInstrumentedInterfaceName(name), signature, "java/lang/Object", new String[] { name });
			intfcCV.visitInnerClass(Instrumenter.getInstrumentedInterfaceName(name), name, "vmvm", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT + ACC_INTERFACE);
			intfcCV.visitOuterClass(name, null, null);
		}

		if (!skipClass)
			this.visitAnnotation(Type.getDescriptor(VMVMInstrumented.class), false);
	}

	public static boolean shouldIgnoreClass(String name) {
		return name.startsWith("$Proxy") || name.startsWith("java/") || name.startsWith("net/sf/cglib") //|| name.startsWith("com/sun") || name.contains("$$EnhancerByCGLIB$$") //
				|| name.startsWith("edu/columbia/cs/psl/vmvm") || name.startsWith("org/objenesis") || name.startsWith("sun/") || name.startsWith("com/rits/cloning")
				|| name.startsWith("org/objectweb/asm") || name.startsWith("org/jruby/ext/posix") || name.startsWith("org/w3c/dom") || name.contains("org/xml/sax")
				|| name.equals("sun/org/mozilla/javascript/internal/Arguments");
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		//		if(desc.length() > 1)
		//			super.visitField(Opcodes.ACC_PUBLIC, name+BEEN_CLONED_FIELD, Type.BOOLEAN_TYPE.getDescriptor(), null, false);

		if ((access & Opcodes.ACC_STATIC) != 0) //Static field
		{
			for (int i = 1; i <= Instrumenter.MAX_SANDBOXES; i++)
				super.visitField(access, name + SANDBOX_SUFFIX + i, desc, signature, value);

		}
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

		return super.visitAnnotation(desc, visible);
	}


	private boolean needToGenerateCLInit;

	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc, String signature, String[] exceptions) {
		MethodNode newM = null;
		boolean ignore = (name.startsWith("_") && desc.contains(Type.getDescriptor(VMState.class))) || (acc & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE;

		if (name.equals("<clinit>")) {
			acc = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;
			name = "_clinit$$vmvmoriginal";
			desc = "("+Type.getDescriptor(VMState.class)+")V";
			ignore = false;
			needToGenerateCLInit = true;
		}
		else if (!name.equals("<clinit>") && !skipClass && !ignore) {
			EqMethodNode original = new EqMethodNode(acc, name, desc,this.className, signature, exceptions);
			//Need to call the modified method - which takes a short arg; the current sandbox state
			Type[] args = Type.getArgumentTypes(desc);
			Type[] descTypes = new Type[args.length + 1];
			System.arraycopy(args, 0, descTypes, 0, args.length);
			descTypes[args.length] = Type.getType(VMState.class);
			String desc2 = Type.getMethodDescriptor(Type.getReturnType(desc), descTypes);
			newM = new MethodNode(acc, name, desc2, signature, exceptions);
			newM.invisibleAnnotations = new LinkedList<>();
			newM.visibleAnnotations = new LinkedList<>();
			String name2 = name;
			if (!"<init>".equals(name) && !"<clinit>".equals(name))
				name2 = "_" + name;
			if (Instrumenter.getMethodNode(thisClassInfo, name2, desc2) == null) {
				renamedMethodsToCover.put(original, newM);
				if (!isAClass && generateVMVMInterface && !name.equals("_clinit$$vmvmoriginal")) {
					MethodVisitor m = intfcCV.visitMethod(acc, name2, desc2, signature, exceptions);
					Instrumenter.getClassNode(Instrumenter.getInstrumentedInterfaceName(this.className)).methodsHashSet.add(new EqMethodNode(acc, name2, desc2, this.className, signature, exceptions));
					EqMethodNode orig = Instrumenter.getMethodNode(this.className, name, desc);
					if (orig.invisibleAnnotations != null) {
						for (Object o : orig.invisibleAnnotations) {
							AnnotationNode an = (AnnotationNode) o;
							AnnotationVisitor av = m.visitAnnotation(an.desc, false);
							an.accept(av);
						}
					}
					if (orig.visibleAnnotations != null) {
						for (Object o : orig.visibleAnnotations) {
							AnnotationNode an = (AnnotationNode) o;
							AnnotationVisitor av = m.visitAnnotation(an.desc, true);
							an.accept(av);
						}
					}
				} else {
					desc = desc2;
					name = name2;
				}
				thisClassInfo.methods.add(new MethodNode(acc, name2, desc2, signature, exceptions));
			} else {
				ignore = true;
			}
		}

		if ((isAClass || name.equals("_clinit$$vmvmoriginal")) && !skipClass && !ignore)//runIMV && 
		{
			//			System.out.println(name+desc);

			boolean generateReverseStub = false;
			if ((Opcodes.ACC_ABSTRACT & acc) != 0) {
				acc = acc & ~Opcodes.ACC_ABSTRACT;
				generateReverseStub = true;
			}

			MethodVisitor fmv = cv.visitMethod(acc, name, desc, signature, exceptions);
			JSRInlinerAdapter mv = new JSRInlinerAdapter(fmv, acc, name, desc, signature, exceptions);
			ClassDefineInterceptMethodVisitor dynMV = new ClassDefineInterceptMethodVisitor(mv, className, name, desc);
			InvivoAdapter invivoAdapter = new InvivoAdapter(Opcodes.ASM5, dynMV, acc, name, desc, className, generateReverseStub);
			SandboxPropogatingMV sandboxer = new SandboxPropogatingMV(Opcodes.ASM5, invivoAdapter, acc, name, desc, className, invivoAdapter, this, newM);
			StaticFieldIsolatorMV staticIsolator = new StaticFieldIsolatorMV(sandboxer, acc, name, desc, this,Instrumenter.getMethodNode(this.className, name, desc));
			ChrootMethodVisitor chrooter = new ChrootMethodVisitor(Opcodes.ASM5, staticIsolator, acc, name, desc, className, invivoAdapter, this);
			ReflectionHackMV reflectionHack = new ReflectionHackMV(Opcodes.ASM5, chrooter,this);
			//			LazyCloneInterceptingMethodVisitor cloningIMV = new LazyCloneInterceptingMethodVisitor(Opcodes.ASM4, mv, acc, name, desc);
			//			InterceptingMethodVisitor imv = new InterceptingMethodVisitor(Opcodes.ASM4, cloningIMV, acc, name, desc);
			//			imv.setClassName(className);
			//			imv.setClassVisitor(this);
			TypeRememberingLocalVariableSorter lvs2 = new TypeRememberingLocalVariableSorter(acc, desc, reflectionHack);
			invivoAdapter.setLocalVariableSorter(lvs2);
			dynMV.setLocalVariableSorter(lvs2);
			//			return staticIsolator;

			return lvs2;
		} else
			return cv.visitMethod(acc, name, desc, signature, exceptions);
	}

	//Default to true to make it work for all classes
	public void setShouldRewrite() {
		willRewrite = true;
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		if (isAClass)
			generateCoverMethods();
		try {
			generateChrootMethods();
		} catch (Exception ex) {
			System.err.println("Unable to generate sandbox method in " + this.className);
			ex.printStackTrace();
		}
		generateStaticCloneMethods();
		
		if (needToGenerateCLInit) {
			MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
			GeneratorAdapter gmv = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "<clinit>", "()V");
			InvivoAdapter ia = new InvivoAdapter(Opcodes.ASM5, gmv, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "<clinit>", "()V", this.className, false);
//			SandboxPropogatingMV smv = new SandboxPropogatingMV(Opcodes.ASM4, ia, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "<clinit>", "()V", className, ia, this, new MethodNode());
			
			TypeRememberingLocalVariableSorter lvs2 = new TypeRememberingLocalVariableSorter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "()V", ia);
			ia.setLocalVariableSorter(lvs2);
			
			ia.visitCode();
			
			for (int i = 0; i <= Instrumenter.MAX_SANDBOXES; i++) {
//				VirtualRuntime.setVMed(i);
				ia.println(className + " clinit " + i);
				gmv.visitIntInsn(Opcodes.SIPUSH, i);
				gmv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "setVMed", "(I)"+Type.getDescriptor(VMState.class), false);
				ia.visitMethodInsn(INVOKESTATIC, className, "_clinit$$vmvmoriginal", "("+Type.getDescriptor(VMState.class)+")V", false);
			}
			ia.visitInsn(Opcodes.RETURN);
			ia.visitMaxs(0, 0);
			ia.visitEnd();
		}
	}

	public String getClassName() {
		return this.className;
	}

	private byte[] extraInterfaceBytes = null;

	public boolean hasExtraInterface() {
		if (intfcCw != null)
			extraInterfaceBytes = intfcCw.toByteArray();
		return extraInterfaceBytes != null && extraInterfaceBytes.length > 0;
	}

	public byte[] getExtraInterface() {
		if (extraInterfaceBytes == null)
			if (intfcCw != null)
				extraInterfaceBytes = intfcCw.toByteArray();
			else
				return null;
		return extraInterfaceBytes;
	}

}
