package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.util.HashMap;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.LocalVariablesSorter;

public class TypeRememberingLocalVariableSorter extends LocalVariablesSorter{

	public TypeRememberingLocalVariableSorter(int access, String desc, MethodVisitor mv) {
		super(Opcodes.ASM5, access, desc, mv);
	}
	HashMap<Integer, Type> localTypes = new HashMap<>();
	@Override
	protected void setLocalType(int local, Type type) {
//		System.out.println(local + " -> " + type);
		super.setLocalType(local, type);
		localTypes.put(local, type);
	}

	public HashMap<Integer, Type> getLocalTypes() {
		return localTypes;
	}
}
