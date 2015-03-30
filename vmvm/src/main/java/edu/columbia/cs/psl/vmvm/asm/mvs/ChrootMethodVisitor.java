package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;


import edu.columbia.cs.psl.vmvm.VMState;
import edu.columbia.cs.psl.vmvm.asm.VMVMClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodInsnNode;
import edu.columbia.cs.psl.vmvm.chroot.ChrootUtils;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Label;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.InstructionAdapter;

public class ChrootMethodVisitor extends InstructionAdapter {
	public static HashMap<String, String> fsMethods = new HashMap<>();
	static {
		try {
			Scanner s = new Scanner(new File("fs-methods.txt"));
			while (s.hasNextLine()) {
				String[] d = s.nextLine().split("\t");
				fsMethods.put(d[0], d[1]);
			}
			s.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	private String className;
	private VMVMClassVisitor icv;
	private InvivoAdapter invivoAdapter;
	public ChrootMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className, InvivoAdapter invivoAdapter, VMVMClassVisitor icv) {
		super(Opcodes.ASM5,mv);
		this.className = className;
		this.icv = icv;
		this.invivoAdapter = invivoAdapter;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		if (fsMethods.containsKey(owner + "." + name + desc) && opcode != Opcodes.INVOKESPECIAL) {
//			System.out.println("Chroot over " + owner + " " + name + " " + desc);
			Label sandboxed = new Label();
			Label end = new Label();
			EqMethodInsnNode mi = new EqMethodInsnNode(opcode, owner, name, desc, itfc);
			Type[] args = Type.getArgumentTypes(desc);
			
			if (opcode != Opcodes.INVOKESTATIC && !name.equals("<init>")) {
				Type[] descTypes2 = new Type[args.length + 1];
				System.arraycopy(args, 0, descTypes2, 1, args.length);
				descTypes2[0] = Type.getType("L" + owner + ";");
				desc = Type.getMethodDescriptor(Type.getReturnType(desc), descTypes2);
			}
			else if(name.equals("<init>") && opcode != Opcodes.INVOKESPECIAL)
			{
				desc = Type.getMethodDescriptor(Type.getType("L"+owner+";"), args);
			}
			args = Type.getArgumentTypes(desc);
			Type[] descTypes2 = new Type[args.length + 1];
			System.arraycopy(args, 0, descTypes2, 0, args.length);
			descTypes2[args.length] = Type.getType(VMState.class);
			desc = Type.getMethodDescriptor(Type.getReturnType(desc), descTypes2);
			
			invivoAdapter.branchIfSandboxed(sandboxed);
			
			super.visitMethodInsn(mi.getOpcode(), mi.owner, mi.name, mi.desc, itfc);
			super.visitJumpInsn(Opcodes.GOTO, end);
			
			super.visitLabel(sandboxed);
			invivoAdapter.getSandboxFlagState();
			
			super.visitMethodInsn(Opcodes.INVOKESTATIC, className,ChrootUtils.getCaptureMethodName(mi), desc, itfc);
//			mi.desc= desc;
			icv.addChrootMethodToGen(mi);

			if(name.equals("<init>") && opcode != Opcodes.INVOKESPECIAL)
			{
				super.visitInsn(Opcodes.SWAP);
				super.visitInsn(Opcodes.POP);
				super.visitInsn(Opcodes.SWAP);
				super.visitInsn(Opcodes.POP);
			}
			super.visitLabel(end);
			
		} else
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
	}
}
