package edu.columbia.cs.psl.vmvm.runtime.inst;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class ReinitCheckForceCV extends ClassVisitor {
	private String className;
	private boolean isInterface;
	private boolean fixLdcClass;
	private boolean skipFrames;
	
	private boolean doOpt;
	
	public ReinitCheckForceCV(ClassVisitor cv, boolean doOpt) {
		super(Opcodes.ASM5, cv);
		this.doOpt = doOpt;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		this.className = name;
		this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
		skipFrames = false;
		if (version >= 100 || version <= 50)
			skipFrames = true;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
	
		if (!isInterface) {
			AnalyzerAdapter an = new AnalyzerAdapter(className, access, name, desc, mv);
			mv = new ReinitCheckForceMV(an, an, className, name, (access & Opcodes.ACC_STATIC) != 0, fixLdcClass, skipFrames, doOpt);
			LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, mv);
			((ReinitCheckForceMV)mv).setLVS(lvs);
			mv = lvs;
		}
		return mv;
	}

}
