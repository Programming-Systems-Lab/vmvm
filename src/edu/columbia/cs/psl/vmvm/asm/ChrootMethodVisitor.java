package edu.columbia.cs.psl.vmvm.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.InstructionAdapter;

public class ChrootMethodVisitor extends InstructionAdapter {

	protected ChrootMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className) {
//		super(api, mv, access, name, desc, className);
		super(mv);
		
	}

}
