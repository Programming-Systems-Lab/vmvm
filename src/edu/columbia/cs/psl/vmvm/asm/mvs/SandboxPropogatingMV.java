package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.sun.media.jai.rmi.HashSetState;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.asm.InterceptingClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.VMVMClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.struct.EqFieldInsnNode;
import edu.columbia.cs.psl.vmvm.asm.struct.MethodListClassNode;
import edu.columbia.cs.psl.vmvm.Virtualizer;
import edu.columbia.cs.psl.vmvm.VMState;

/**
 * This method visitor will make sure that all sandbox flags are propogated
 * 
 * @author jon
 * 
 */
public class SandboxPropogatingMV extends InstructionAdapter implements Opcodes {

	private InvivoAdapter invivoAdapter;
	private String className;
	private String name;
	private String desc;
	private VMVMClassVisitor icv;
	private boolean methodTracksVMState = false;
	public static int concurrentVMs = 0;
	private MethodNode coveringMN;
//	private static Logger logger = Logger.getLogger(SandboxPropogatingMV.class);
	public static HashMap<Integer, HashSet<EqFieldInsnNode>> staticFieldsToClone = new HashMap<>();
	public SandboxPropogatingMV(int api, MethodVisitor mv, int access, String name, String desc, String className, InvivoAdapter lvs, VMVMClassVisitor interceptingClassVisitor, MethodNode newM) {
		//		super(api,mv,access,name,desc);
		super(mv);
		this.invivoAdapter = lvs;
		this.className = className;
		this.name = name;
		this.desc = desc;
		this.icv = interceptingClassVisitor;
		this.coveringMN = newM;
		numVMsThisMethod = 0;
		methodTracksVMState = name.startsWith("_") && desc.contains(Type.getDescriptor(VMState.class));
	}

	@Override
	public void visitInsn(int opcode) {
		if (opcode == RETURN || opcode == ARETURN || opcode == IRETURN || opcode == LRETURN || opcode == FRETURN) {
			if (concurrentVMs > 0) {
				exitVM();
			}
		}
		super.visitInsn(opcode);
	}

	private int sandboxVar = -1;

	//	@Override
	//	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
	//		if (index >= invivoAdapter.getFirstLocal() - 1 && invivoAdapter.isStaticMethod() && !invivoAdapter.isMain() && !name.equals("<clinit>"))
	//			super.visitLocalVariable(name, desc, signature, start, end, index + 1);
	//		else
	//			super.visitLocalVariable(name, desc, signature, start, end, index);
	//	}
	
	@Override
	public void visitCode() {
		super.visitCode();

		if (!name.equals("<init>"))
			onMethodEnter();
	}

	private int sandboxNumInThisMethod = 0;;
	protected void onMethodEnter() {
		if (invivoAdapter.isStaticMethod()) {
			if (invivoAdapter.isMain() || name.equals("<clinit>")) {
				sandboxVar = invivoAdapter.newLocal(Type.getType(VMState.class));
				invivoAdapter.setSandboxVar(sandboxVar);
				super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "getVMState", "()" + Type.getDescriptor(VMState.class));
				invivoAdapter.setSandboxFlag();
			}
		} else {
			//			if (name.equals("<init>")) {
			//				visitVarInsn(ILOAD, invivoAdapter.getFirstLocal() - 1);
			//				invivoAdapter.setSandboxFlag();
			//			}
		}
	}

	private boolean superInit = false;
	private int vmStateVar;
	//	private int clonedLocalsStart;

	
	private void exitVM() {
		concurrentVMs--;
		super.visitVarInsn(ALOAD, vmStateVar);
		super.visitInsn(DUP);
		super.invokevirtual(Type.getInternalName(VMState.class), "deVM", "()V");
		invivoAdapter.setSandboxFlag();
		
		int n = numVMsThisMethod - concurrentVMs;
		icv.addStaticCloneWrapper("cloneStatics_"+getCaptureMethodName(this.name, this.desc)+"_"+n, staticFieldsToClone.get(n));
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		if (concurrentVMs > 0 && var < vmStateVar)
			super.visitVarInsn(opcode, invivoAdapter.getSboxLVMap(var));
		else
			super.visitVarInsn(opcode, var);
	}
	public static int numVMsThisMethod = 0;
	public static String getCaptureMethodName(String name, String desc) {
		return "_" + name.replace("<", "_").replace(">", "_") + "_"+Arrays.toString(Type.getArgumentTypes(desc)).replace("[", "_").replace("]","").replace(";", "").replace("/", "_").replace(", ","");
	}
	HashMap<String, HashSet<EqFieldInsnNode>> staticsCache = new HashMap<>();
	HashSet<String> staticsInProgress = new HashSet<>();
	private HashSet<EqFieldInsnNode> collectStaticsForMethod(String owner, String name, String desc)
	{
		HashSet<EqFieldInsnNode> ret = staticsCache.get(owner+"."+name+desc);
		if(ret != null)
			return ret;
//		logger.info(this.name+">>>"+owner + "" + name+desc);

		if(staticsInProgress.contains(owner))
			return new HashSet<>();
		staticsInProgress.add(owner);
		ret = new HashSet<>();

		if(Instrumenter.superToSubClass.containsKey(owner))
		{
			for(String s : Instrumenter.superToSubClass.get(owner))
			{
				ret.addAll(collectStaticsForMethod(s, name, desc));
			}
		}
		MethodNode mn = Instrumenter.getMethodNode(owner, name, desc);
		if(mn != null)
		{
			Iterator<?> iter = mn.instructions.iterator();
			while(iter.hasNext())
			{
				AbstractInsnNode in = (AbstractInsnNode) iter.next();
				if(in.getType() == AbstractInsnNode.METHOD_INSN)
				{
					MethodInsnNode min = (MethodInsnNode) in;
					ret.addAll(collectStaticsForMethod(min.owner,min.name,min.desc));
				}
				else
				{
					FieldInsnNode fin = (FieldInsnNode) in;
					if(Instrumenter.instrumentedClasses.containsKey(fin.owner))
						ret.add(new EqFieldInsnNode(Opcodes.GETSTATIC, fin.owner, fin.name, fin.desc));
				}
			}
		}
		staticsInProgress.remove(owner);
		staticsCache.put(owner+"."+name+desc, ret);
		return ret;
	}
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		owner = Instrumenter.remapInterface(owner);
		super.visitFieldInsn(opcode, owner, name, desc);
	}
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		boolean ignore = name.startsWith("_") && desc.contains(Type.getDescriptor(VMState.class));

		owner = Instrumenter.remapInterface(owner);
		if (owner.equals(Type.getInternalName(Virtualizer.class)) && name.equals("execInVM")) {
			concurrentVMs++;
			numVMsThisMethod++;
//			System.out.println("Seeing execinvm " + concurrentVMs);
			staticFieldsToClone.put(concurrentVMs, new HashSet<EqFieldInsnNode>());
			vmStateVar = invivoAdapter.newLocal(Type.getType(VirtualRuntime.class));
			invivoAdapter.cloneLocals(vmStateVar);


			super.invokestatic(Type.getInternalName(VirtualRuntime.class), "setVMed", "()" + Type.getDescriptor(VMState.class));
			super.visitInsn(DUP);
			super.visitVarInsn(ASTORE, vmStateVar);

			invivoAdapter.setSandboxFlag();

			invivoAdapter.getSandboxFlag();
//			System.out.println("cloneStatics_"+this.name+getCaptureMethodName(this.name, this.desc)+"_"+numVMsThisMethod);
			super.invokestatic(this.className, "cloneStatics_"+this.name.replace("<", "").replace(">", "")+getCaptureMethodName(this.name, this.desc)+"_"+numVMsThisMethod, "(I)V");
			return;
		} else if (owner.equals(Type.getInternalName(Virtualizer.class)) && name.equals("exitVM")) {
			exitVM();
			return;
		} else if (opcode == INVOKESPECIAL && owner.equals(Type.getInternalName(Thread.class)) && name.equals("<init>")) {
			super.visitMethodInsn(opcode, owner, name, desc);
			if(Instrumenter.atLeastASuperEq(this.className, "java/lang/Thread",0) && this.name.equals("<init>")) //It's a super call
			{
				super.visitVarInsn(ALOAD, 0);
			}
			else
			{
				super.visitInsn(DUP);
			}
			super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Thread.class), "getId", "()J");
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "setVMed", "(J)V");

			return;
		}
		//TODO i removed this because i don't think it did anything.
		if(concurrentVMs > 0)
		{
			//If we are currently sandboxed, then collect all of the statics that are accessed by the method we are about to call
			HashSet<EqFieldInsnNode> staticsUsedThisMethod = collectStaticsForMethod(owner, name, desc);
			if(owner.equals(Type.getInternalName(Thread.class)) && name.equals("start"))
				staticsUsedThisMethod.addAll(collectStaticsForMethod(owner, "run", "()V"));
			for(int i = (SandboxPropogatingMV.numVMsThisMethod - SandboxPropogatingMV.concurrentVMs) + 1; i <= SandboxPropogatingMV.numVMsThisMethod;i++)
			{
				SandboxPropogatingMV.staticFieldsToClone.get(i).addAll(staticsUsedThisMethod);
			}
		}
		MethodListClassNode cn = Instrumenter.instrumentedClasses.get(owner);
//		if(name.equals("startEntity"))
//		{
//			System.err.println(">>"+owner + "."+name +desc);
//		}
		if (cn != null && !ignore && !InterceptingClassVisitor.shouldIgnoreClass(owner) && cn.containsMethod(name, desc) &&
					(cn.getMethod(name, desc).access & Opcodes.ACC_NATIVE) != Opcodes.ACC_NATIVE) {
//			if(name.equals("startEntity"))
//			{
//				System.err.println("^^^^changing to _");
//			}
			//Need to call the modified method - which takes a short arg; the current sandbox state
			Type[] args = Type.getArgumentTypes(desc);
			Type[] descTypes = new Type[args.length + 1];
			System.arraycopy(args, 0, descTypes, 0, args.length);
			descTypes[args.length] = Type.getType(VMState.class);
			desc = Type.getMethodDescriptor(Type.getReturnType(desc), descTypes);
			invivoAdapter.getSandboxFlagState();
			if(!"<init>".equals(name) && !"<clinit>".equals(name))
				name = "_"+name;
			//				if(sandboxNextMethodInsn)
			//				{
			//					if(opcode != Opcodes.INVOKESTATIC)
			//					{
			//						//Dup the top object, make sure that it has the correct sandbox id
			//						icv.addSandboxMethodToGen(new EqMethodInsnNode(opcode, owner, name, desc));
			//						name = "sandbox_"+owner.replace("/", "_")+"_"+name;
			//						opcode = INVOKESTATIC;
			//
			//						Type[] descTypes2 = new Type[descTypes.length + 1];
			//						System.arraycopy(descTypes, 0, descTypes2, 1, descTypes.length);
			//						descTypes2[0] = Type.getType("L"+owner+";");
			//						desc = Type.getMethodDescriptor(Type.getReturnType(desc), descTypes2);
			//					}
			//				}
		}

		super.visitMethodInsn(opcode, owner, name, desc);

		if (this.name.equals("<init>") && !superInit && opcode == INVOKESPECIAL)
			onMethodEnter();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		final AnnotationNode an = new AnnotationNode(desc);
		if(coveringMN != null)
			if(visible)
				coveringMN.visibleAnnotations.add(an);
			else
				coveringMN.invisibleAnnotations.add(an);
		return new AnnotationVisitor(Opcodes.ASM4) {
			@Override
			public void visit(String name, Object value) {
				an.visit(name, value);
			}
			@Override
			public AnnotationVisitor visitAnnotation(String name, String desc) {
				return an.visitAnnotation(name, desc);
			}
			@Override
			public AnnotationVisitor visitArray(String name) {
				return an.visitArray(name);
			}
			@Override
			public void visitEnd() {
				an.visitEnd();
			}
			@Override
			public void visitEnum(String name, String desc, String value) {
				an.visitEnum(name, desc, value);
			}
		};
	}
}
