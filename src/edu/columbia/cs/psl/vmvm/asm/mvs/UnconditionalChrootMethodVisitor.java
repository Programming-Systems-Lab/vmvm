package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;
import com.sun.org.apache.xml.internal.utils.UnImplNode;

import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.VMState;
import edu.columbia.cs.psl.vmvm.asm.VMVMClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.struct.EqMethodInsnNode;
import edu.columbia.cs.psl.vmvm.chroot.ChrootUtils;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Label;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.AdviceAdapter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.InstructionAdapter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.MethodInsnNode;

public class UnconditionalChrootMethodVisitor extends AdviceAdapter {
	public static HashMap<String, String> fsInputMethods = new HashMap<>();
	public static HashSet<String> fsOutputMethods = new HashSet<>();
	static {
		try {
			Scanner s = new Scanner(new File("fs-input-methods.txt"));
			while (s.hasNextLine()) {
				String[] d = s.nextLine().split("\t");
				fsInputMethods.put(d[0], d[1]);
			}
			s.close();

			s = new Scanner(new File("fs-output-methods.txt"));
			while (s.hasNextLine()) {
				fsOutputMethods.add(s.nextLine());
			}
			s.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	private String className;
	private VMVMClassVisitor icv;

	public UnconditionalChrootMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className, VMVMClassVisitor icv) {
		super(api, mv, access, name, desc);
		this.className = className;
		this.icv = icv;
	}

	private boolean superInit = false;

	@Override
	protected void onMethodEnter() {
		superInit = true;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		
		if (fsInputMethods.containsKey(owner + "." + name + desc) || fsOutputMethods.contains(owner + "." + name + desc)) {
			if (!superInit) {
				/*
				 * If we are calling the super init, and that super init method
				 * is an fs method, we have this gorgeous hack that passes all
				 * of the args to a capture method that returns the args back!
				 */
				//				EqMethodInsnNode mi = new EqMethodInsnNode(opcode, owner, name, desc, EqMethodInsnNode.FLAG_SUPER_INVOKE_CHROOT);
				//				Type[] args = Type.getArgumentTypes(desc);
				//				String captureDesc = Type.getMethodDescriptor(Type.getType("[Ljava/lang/Object;"), args);
				//
				//				super.visitMethodInsn(Opcodes.INVOKESTATIC, className, ChrootUtils.getCaptureInitParentMethodName(mi), captureDesc);
				//				for (int i = 0; i < args.length; i++) {
				//					visitInsn(DUP);
				//					visitIntInsn(BIPUSH, i);
				//					visitInsn(AALOAD);
				//					checkCast(args[i]);
				//					visitInsn(SWAP);
				//				}
				//				visitInsn(POP);
				//
				//				super.visitMethodInsn(opcode, owner, name, desc);
				//				icv.addChrootMethodToGen(mi);
				throw new UnsupportedOperationException("Not implemented yet");
			} else {
				String captureType = fsInputMethods.get(owner + "." + name + desc);
//				EqMethodInsnNode mi = new EqMethodInsnNode(opcode, owner, name, desc);
				Type[] args = Type.getArgumentTypes(desc);
				boolean swapBack = false;
				Type typeToVirtualize = null;
				if(captureType == null)
				{
					if(args.length > 0)
						captureType = "0";
					else
						captureType = "this";
//					throw new IllegalArgumentException("Can't find capture type for "+owner+"."+name+desc);
				}
				if (captureType.startsWith("0")) {
					typeToVirtualize = args[0];
					if (args.length == 2) {
						visitInsn(SWAP);
						swapBack = true;
					} else if (args.length > 2) {
						throw new IllegalArgumentException("Not coded to process args where we need to swap more than once");
					}
				} else if (captureType.startsWith("this")) {
					typeToVirtualize = Type.getType("L"+owner+";");
					if (args.length == 1) {
						visitInsn(SWAP);
						swapBack = true;
					} else if (args.length > 1) {
						throw new IllegalArgumentException("Not coded to process args where we need to swap more than once");
					}
				}
				else
				{
					throw new IllegalStateException("Can't parse capture type<"+captureType+">");
				}
				boolean isOutput = false;
				if (fsOutputMethods.contains(owner + "." + name + desc)) {
					isOutput = true;
				}
				
				if(isOutput)
					super.visitInsn(ICONST_1);
				else
					super.visitInsn(ICONST_0);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "chrootCapture" + (captureType.endsWith("INV") ? "INV" : ""), "(" + typeToVirtualize.getDescriptor()
						+ "Z)" + typeToVirtualize.getDescriptor(), false);
				if (swapBack)
					visitInsn(SWAP);
				//			mi.desc= desc;
				//				icv.addChrootMethodToGen(mi);

				//				if (name.equals("<init>")) {
				//					if (superInit) {
				//						super.visitInsn(Opcodes.SWAP);
				//						super.visitInsn(Opcodes.POP);
				//					}
				//					super.visitInsn(Opcodes.SWAP);
				//					super.visitInsn(Opcodes.POP);
				//				}
			}
		}
		super.visitMethodInsn(opcode, owner, name, desc, itfc);
	}

	public static void sandboxCallToFSOutputMethod(MethodVisitor mv, int opcode, String owner, String name, String desc) {
		Type[] args = Type.getArgumentTypes(desc);
		String captureArgType = null;
		switch (args.length) {
		case 0:
			captureArgType = "L" + owner + ";";
			mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "logFileWrite", "(" + captureArgType + ")"+captureArgType, false);
//			mv.visitMethodInsn(opcode, owner, name, desc);
			break;
		case 1:
			captureArgType = args[0].getDescriptor();
			mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "logFileWrite", "(" + captureArgType + ")"+captureArgType, false);
//			mv.visitMethodInsn(opcode, owner, name, desc);
			break;
		case 2:
			captureArgType = args[0].getDescriptor();
			mv.visitInsn(SWAP);
			mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ChrootUtils.class), "logFileWrite", "(" + captureArgType + ")"+captureArgType, false);
//			mv.visitMethodInsn(opcode, owner, name, desc);
			mv.visitInsn(SWAP);
			break;
		default:
			break;
		}
		if (captureArgType == null) {
			mv.visitMethodInsn(opcode, Type.getInternalName(ChrootUtils.class), name, desc, false);
		} else {

		}
	}
}
