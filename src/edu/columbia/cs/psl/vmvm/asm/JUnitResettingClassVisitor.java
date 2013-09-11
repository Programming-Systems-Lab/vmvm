package edu.columbia.cs.psl.vmvm.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.VMState;
import edu.columbia.cs.psl.vmvm.VMVMInstrumented;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.asm.mvs.ChrootMethodVisitor;
import edu.columbia.cs.psl.vmvm.asm.mvs.ClassDefineInterceptMethodVisitor;
import edu.columbia.cs.psl.vmvm.asm.mvs.InsnCountingMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.InvivoAdapter;
import edu.columbia.cs.psl.vmvm.asm.mvs.JUnitRunnerMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.ReflectionHackMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.SandboxPropogatingMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.StaticFieldIsolatorMV;
import edu.columbia.cs.psl.vmvm.asm.mvs.StaticFinalMutibleizer;
import edu.columbia.cs.psl.vmvm.asm.mvs.SystemPropertyLogger;
import edu.columbia.cs.psl.vmvm.asm.mvs.TypeRememberingLocalVariableSorter;
import edu.columbia.cs.psl.vmvm.asm.mvs.UnconditionalChrootMethodVisitor;
import edu.columbia.cs.psl.vmvm.asm.struct.EqFieldInsnNode;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodInsnNode;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodNode;
import edu.columbia.cs.psl.vmvm.chroot.ChrootUtils;
import edu.columbia.cs.psl.vmvm.struct.MutableInstance;

public class JUnitResettingClassVisitor extends VMVMClassVisitor {

	//	private static Logger logger = Logger.getLogger(InterceptingClassVisitor.class);

	private boolean isAClass = true;

	private boolean runIMV = true;

	private boolean willRewrite = false;
	private ClassNode thisClassInfo;
	private boolean generateVMVMInterface = false;

	public JUnitResettingClassVisitor(ClassVisitor cv, ClassNode thisClassInfo, boolean generateVMVMInterface) {
		super(Opcodes.ASM4, cv,false);
		this.thisClassInfo = thisClassInfo;
	}

	private boolean skipClass = false;

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (generateVMVMInterface) {
			for (int i = 0; i < interfaces.length; i++) {
				interfaces[i] = Instrumenter.remapInterface(interfaces[i]);
			}
		}
		if((access & Opcodes.ACC_PUBLIC) == 0)
		{
			access = access & ~Opcodes.ACC_PROTECTED;
			access = access & ~Opcodes.ACC_PRIVATE;
			access = access | Opcodes.ACC_PUBLIC; 
		}
		super.visit(version, access, name, signature, superName, interfaces);

		this.className = name;
		skipClass = shouldIgnoreClass(name);
		if ((access & Opcodes.ACC_INTERFACE) != 0)
			isAClass = false;
		if((access & Opcodes.ACC_ANNOTATION) !=0)
		{
			isAClass = false;
			skipClass = true;
		}
//		if (!skipClass)
//			this.visitAnnotation(Type.getDescriptor(VMVMInstrumented.class), false);
	}

	public static boolean shouldIgnoreClass(String name) {
		return name.startsWith("java/") || name.startsWith("net/sf/cglib") //|| name.startsWith("com/sun") || name.contains("$$EnhancerByCGLIB$$") //
				|| name.startsWith("edu/columbia/cs/psl/vmvm") || name.startsWith("org/objenesis") || name.startsWith("sun/") || name.startsWith("com/rits/cloning")
				|| name.startsWith("org/objectweb/asm") 
				|| name.startsWith("org/jruby/ext/posix") || name.startsWith("org/w3c/dom") || name.contains("org/xml/sax")
//				|| name.startsWith("org/apache/tomcat/buildutil")
				|| name.startsWith("org/mockito/cglib")
				|| name.startsWith("org/mockito/asm")
				|| name.startsWith("org/apache/ant")
				|| name.startsWith("org/apache/tools/ant")
				|| name.startsWith("net/sf/antcontrib")
				|| name.startsWith("org/apache/tomcat/util/buf/B2CConverter")
				|| name.startsWith("org/apache/ivy/plugins/lock/DeleteOnExitHook")
				;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if(skipClass)
			return super.visitField(access, name, desc, signature, value);
//		access = access & ~Opcodes.ACC_FINAL;
		if ((access & Opcodes.ACC_STATIC) != 0 && isAClass) //Static field
		{
//			if((access & Opcodes.ACC_PUBLIC) != 0) //pub
			if((access & Opcodes.ACC_ENUM) != 0)
			{
				access = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC;
			}
//			else if((access & Opcodes.ACC_PROTECTED) != 0) //pub
//				sAccess = Opcodes.ACC_PROTECTED + Opcodes.ACC_STATIC;
//			else if((access & Opcodes.ACC_PRIVATE) != 0) //pub
//				sAccess = Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC;
			super.visitField(access, name + SANDBOX_SUFFIX, desc, signature, value);
			super.visitField(access, name + BEEN_CLONED_FIELD, "Z", null, false);
		}
		else if((access & Opcodes.ACC_STATIC) != 0 && value ==null) //static field on an interface - is final!!!
		{
			Instrumenter.mutablizedFields.put(this.className+"."+name,desc);
			desc = Type.getDescriptor(MutableInstance.class);
			value = null;
		}
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

		return super.visitAnnotation(desc, visible);
	}

	private HashSet<EqMethodInsnNode> sandboxMethodsToGen = new HashSet<>();

	public void addSandboxMethodToGen(EqMethodInsnNode m) {
		sandboxMethodsToGen.add(m);
	}

	private boolean hasClinit;
	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc, String signature, String[] exceptions) {
		boolean ignore = (acc & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE;

		if (!skipClass && !ignore)//runIMV && 
		{
			if(isAClass && name.equals("<clinit>"))
			{
				acc = Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC;
				name = Constants.VMVM_STATIC_RESET_METHOD;
				hasClinit = true;
			}
			else if(!isAClass && name.equals("<clinit>"))
			{
				hasClinit = true;
				return null;
			}
			MethodVisitor fmv = cv.visitMethod(acc, name, desc, signature, exceptions);
			fmv = new JSRInlinerAdapter(fmv, acc, name, desc, signature, exceptions);

			ClassDefineInterceptMethodVisitor dynMV = new ClassDefineInterceptMethodVisitor(fmv, className, name, desc);
//			UnconditionalChrootMethodVisitor chrooter = new UnconditionalChrootMethodVisitor(Opcodes.ASM4, dynMV, acc, name, desc, className , this);
			SystemPropertyLogger propLogger = new SystemPropertyLogger(Opcodes.ASM4,dynMV,acc,name,desc);
			StaticFinalMutibleizer mutableizer = new StaticFinalMutibleizer(propLogger, acc, className, name, desc);
			StaticFieldIsolatorMV staticMV = new StaticFieldIsolatorMV(mutableizer, acc, name, desc, this, Instrumenter.getMethodNode(this.className, name, desc));
			ReflectionHackMV reflectionHack = new ReflectionHackMV(Opcodes.ASM4, staticMV,this);

			JUnitRunnerMV jumv = new JUnitRunnerMV(reflectionHack, acc, name, desc, this.className);
			//			LazyCloneInterceptingMethodVisitor cloningIMV = new LazyCloneInterceptingMethodVisitor(Opcodes.ASM4, mv, acc, name, desc);
			//			InterceptingMethodVisitor imv = new InterceptingMethodVisitor(Opcodes.ASM4, cloningIMV, acc, name, desc);
			//			imv.setClassName(className);
			//			imv.setClassVisitor(this);
			TypeRememberingLocalVariableSorter lvs2 = new TypeRememberingLocalVariableSorter(acc, desc, jumv);
			staticMV.setLvs(lvs2);
			dynMV.setLocalVariableSorter(lvs2);
			InsnCountingMV counter = new InsnCountingMV(Opcodes.ASM4, lvs2);
			staticMV.setCounter(counter);
			return counter;
		} else
			return cv.visitMethod(acc, name, desc, signature, exceptions);
	}

	//Default to true to make it work for all classes
	public void setShouldRewrite() {
		willRewrite = true;
	}

	@Override
	public void visitEnd() {
		if(skipClass)
			return;
		if(isAClass)
		{
			visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_NEEDS_RESET, "Z", null, true);
			visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.VMVM_RESET_IN_PROGRESS, "Ljava/lang/Thread;", null, null);

		}
		super.visitEnd();
		generateClinit(hasClinit);
		if (isAClass)
			generateCoverMethods();
		if(isAClass)
			generateReInitCheckMethod();
		try {
			generateChrootMethods();
		} catch (Exception ex) {
			System.err.println("Unable to generate sandbox method in " + this.className);
			ex.printStackTrace();
		}
		generateStaticCloneMethods();
		if(isAClass)
		{
//			generateStaticFieldCloneMethods(hasClinit);
//			generateFinalHackMethod();
		}
		
	}

	private void generateReInitCheckMethod() {
		MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, Constants.VMVM_STATIC_RESET_METHOD_WITH_CHECK, "()V", null, null);
		StaticFieldIsolatorMV smv = new StaticFieldIsolatorMV(mv, Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, Constants.VMVM_STATIC_RESET_METHOD_WITH_CHECK, "()V", this, new EqMethodNode(Opcodes.ASM4, Constants.VMVM_STATIC_RESET_METHOD_WITH_CHECK, "()V", this.className, null, null));
		smv.visitCode();
		smv.checkAndReinit(this.className);
		smv.visitInsn(RETURN);
		smv.visitMaxs(0, 0);
		smv.visitEnd();
		
	}

	public String getClassName() {
		return this.className;
	}

	
	public boolean hasExtraInterface() {
		return false;
	}

	public byte[] getExtraInterface() {
		return null;
	}

}
