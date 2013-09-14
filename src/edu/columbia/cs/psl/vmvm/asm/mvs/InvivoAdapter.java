package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.LocalVariableNode;

import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.VMState;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodNode;
import edu.columbia.cs.psl.vmvm.asm.struct.MethodListClassNode;

public class InvivoAdapter extends CloningAdapter implements Opcodes {
	protected int access;
	protected String name;
	protected String desc;
	protected String className;
	private TypeRememberingLocalVariableSorter lvs;
	private int firstLocal;
	private int sandboxVar;
//	private static Logger log = Logger.getLogger(InvivoAdapter.class);
	public InvivoAdapter(int api, MethodVisitor mv, int access, String name, String desc, String className, boolean generateReverseStub) {
		//		super(api, mv, access, name, desc);
		super(mv);
		this.access = access;
		this.name = name;
		this.desc = desc;
		this.className = className;

		Type[] args = Type.getArgumentTypes(desc);
		firstLocal = (Opcodes.ACC_STATIC & access) == 0 ? 1 : 0;
		for (int i = 0; i < args.length; i++) {
			firstLocal += args[i].getSize();
		}
		sandboxVar = isMain() || isClinit() ? getFirstLocal() : getFirstLocal() - 1;
		
		if(generateReverseStub)
		{
			GeneratorAdapter gv = new GeneratorAdapter(mv, access, name, desc);
			gv.visitCode();
			gv.loadThis();
			for(int i = 0; i < args.length-1; i++)
			{
				gv.loadArg(i);
			}
			Type[] args2 = new Type[args.length-1];
			System.arraycopy(args, 0, args2, 0, args.length-1);
			gv.visitMethodInsn(INVOKEVIRTUAL, className, name.substring(1), Type.getMethodDescriptor(Type.getReturnType(desc), args2));
//			switch(Type.getReturnType(desc).getSort())
//			{
//			case Type.ARRAY:
//			case Type.OBJECT:
//				gv.visitLdcInsn(null);
//			break;
//			case Type.VOID:
//				break;
//			default:
//				gv.push(0);
//			}
			gv.returnValue();
			gv.visitMaxs(0, 0);
//			mv.visitEnd();
		}
	}

	public boolean isMain() {
		return name.equals("main") && desc.equals("([Ljava/lang/String;)V");
	}

	public boolean needToGenerateSandboxVar() {
		return isStaticMethod() && (isMain() || name.equals("<clinit>"));
	}

	public boolean isStaticMethod() {
		return (access & Opcodes.ACC_STATIC) != 0;
	}

	public int getFirstLocal() {
		return firstLocal;
	}

	@Override
	public void visitCode() {
		visitLabel(start);
		super.visitCode();
	}

	private Label start = new Label();
	private Label end = new Label();
	private boolean sandboxVarVisited = false;

	public boolean isRegularClass()
	{
		return (ACC_ABSTRACT & this.access) == 0; 
	}
	@Override
	public void visitEnd() {
		try{
		super.visitEnd();
		}
		catch(Exception ex)
		{
//			log.error("Unable to calculate frame for method " + this.className+"."+this.name+this.desc,ex);
		}
    }
	
	private void visitVMVMLocalVariable()
	{
        if (sandboxVar >= 0 && !sandboxVarVisited && isRegularClass()) {
            try {
                sandboxVarVisited = true;
                super.visitLocalVariable("secretVMVMD4t4z", Type.getDescriptor(VMState.class),
                        null, start, end, sandboxVar);
            } catch (NullPointerException ex) {
                System.err.println("Unable to visit local variable vmvmSandboxIndx (num "
                        + sandboxVar + ") [labels=" + start + "; " + end + "]" + "[method = "
                        + this.className + "." + this.name + this.desc);
                ex.printStackTrace();
            }
        }
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
        visitLabel(end);

        if(sandboxVar == 0)
            visitVMVMLocalVariable();
        while (newLVs.size() > 0) {
            LocalVariableNode lv = newLVs.pop();
            if (lvDescs.get(lv.index) != null)
                lv.desc = lvDescs.get(lv.index);
            try {
                super.visitLocalVariable(lv.name, lv.desc, lv.signature, this.start, this.end,
                        lv.index);
                if(lv.index == sandboxVar - 1)
                    visitVMVMLocalVariable();
            } catch (NullPointerException ex) {
                System.err.println("Unable to visit local variable " + lv.name + " " + lv.desc
                        + " (num " + lv.index + ")");
                ex.printStackTrace();
            }
        }
        
	    super.visitMaxs(maxStack, maxLocals);
	}
	@Override
	public void visitIincInsn(int var, int increment) {
		if (var >= firstLocal - 1 && !isMain() && !name.equals("<clinit>")) {
			super.visitIincInsn(var + 1, increment);
		} else {
			super.visitIincInsn(var, increment);
		}

	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		if (var >= firstLocal - 1 && !isMain() && !name.equals("<clinit>")) {
			super.visitVarInsn(opcode, var + 1);
		} else {
			super.visitVarInsn(opcode, var);
		}
	}

	private HashMap<Integer, String> lvDescs = new HashMap<>();

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		if (oldLVtoSandboxLV != null && oldLVtoSandboxLV.containsKey(index))
			lvDescs.put(getSboxLVMap(index), desc);
		//		if (sandboxVar == index && !sandboxVarVisited) {
		//			sandboxVarVisited = true;
		//			visitLabel(end);
		//			super.visitLocalVariable("vmvmSandboxIndx", Type.getDescriptor(VMState.class), null, this.start, this.end, sandboxVar);
		//		}
		while (newLVs.size() > 0 && newLVs.peek().index < index) {
			LocalVariableNode lv = newLVs.pop();
			if (lvDescs.get(lv.index) != null)
				lv.desc = lvDescs.get(lv.index);
			super.visitLocalVariable(lv.name, lv.desc, lv.signature, this.start, this.end, lv.index);
		}
		if (index >= firstLocal - 1 && !isMain() && !name.equals("<clinit>")) {

			super.visitLocalVariable(name, desc, signature, start, end, index + 1);
		} else
		{
			super.visitLocalVariable(name, desc, signature, start, end, index);
            if(index == sandboxVar - 1)
                visitVMVMLocalVariable();

		}
	}

	public int newLocal(Type type) {
		return lvs.newLocal(type);
	}

	public void branchIfSandboxed(Label lblForSandboxed) {
		getSandboxFlag();

		visitJumpInsn(IFGT, lblForSandboxed);
	}

	public void setSandboxVar(int sandboxVar) {
		this.sandboxVar = sandboxVar;
	}

	public void getSandboxFlagState() {
		super.visitVarInsn(ALOAD, sandboxVar);
	}

	public void getSandboxFlag() {
		getSandboxFlagState();
		super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(VMState.class), "getState", "()I");
	}

	private boolean isClinit() {
		return name.equals("<clinit>");
	}

	public void setSandboxFlag() {
		super.visitVarInsn(ASTORE, sandboxVar);
	}

	

	public void setLocalVariableSorter(TypeRememberingLocalVariableSorter lvs2) {
		this.lvs = lvs2;
	}

	public int getSandboxVar() {
		return sandboxVar;
	}

	public int getSboxLVMap(int oldLV) {
		if (!oldLVtoSandboxLV.containsKey(oldLV))
			throw new NoSuchElementException("Size: " + oldLVtoSandboxLV.size() + "; No element " + oldLV + " in LV map for " + className + "." + name + desc);
		return oldLVtoSandboxLV.get(oldLV);
	}

	private LinkedList<LocalVariableNode> newLVs = new LinkedList<>();
	private HashMap<Integer, Integer> oldLVtoSandboxLV;

	private List<?> getLocals() {
		MethodListClassNode cn = Instrumenter.instrumentedClasses.get(className);
		for (EqMethodNode mn : cn.methodsHashSet) {
			System.out.println(this.name.substring(1));
			if ((mn.name.equals(this.name)  || mn.name.equals(this.name.substring(1)) ) && methodDescsCloseEnough(this.desc, mn.desc)) {
				return mn.localVariables;
			}
		}

		return null;
	}
	public static boolean methodDescsCloseEnough(String mightHaveVMState, String desc)
	{
		Type[] d1 = Type.getArgumentTypes(mightHaveVMState);
		Type[] d2 = Type.getArgumentTypes(desc);
		if(Arrays.deepEquals(d1, d2))
			return true;
		if(d1[d1.length-1].getInternalName().equals(Type.getInternalName(VMState.class)))
		{

			if(d1.length -1 != d2.length)
				return false;
			for(int i = 0; i < d1.length -1;i++)
				if(!d1[i].equals(d2[i]))
					return false;
			return true;
		}
		return false;
	}
	public void cloneLocals(int whereToStop) {
		oldLVtoSandboxLV = new HashMap<>();
		List<?> oldLocals = getLocals();
		for (int i = 0; i < whereToStop; i++) {
			Type t = lvs.getLocalTypes().get(i);
			if (i == 0 && t == null && (access & ACC_STATIC) == 0)
				t = Type.getType("L" + className + ";");
			if (i < firstLocal && t == null)
			{
				if((Opcodes.ACC_STATIC & access) == 0)
					t = Type.getMethodType(this.desc).getArgumentTypes()[i-1];
				else
					t = Type.getMethodType(this.desc).getArgumentTypes()[i];
			}
			if (t.getSort() == Type.OBJECT && t.getInternalName().equals("java/lang/Object")) {
				//Default to the type of what we cloened from
				for (Object o : oldLocals) {
					LocalVariableNode lv = (LocalVariableNode) o;
					if (lv.index + 1 == i) {
						t = Type.getType(lv.desc);
						break;
					}
				}
			}

			int idx = lvs.newLocal(t);
			oldLVtoSandboxLV.put(i, idx);
			newLVs.add(new LocalVariableNode("clone_" + i, t.getDescriptor(), null, null, null, idx));

			load(i, t);
			if (t.getSort() == Type.ARRAY || t.getSort() == Type.OBJECT)
				cloneValAtTopOfStack(t.getDescriptor());
			store(idx, t);
		}
	}

}
