package edu.columbia.cs.psl.vmvm.asm;

import java.util.HashMap;
import java.util.HashSet;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.InstructionAdapter;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;

public class StaticFieldIsolatorMV extends InstructionAdapter implements Opcodes{

	private InvivoAdapter invAdaptor;
	private String name;

	public StaticFieldIsolatorMV(int api, MethodVisitor mv, int access, String name, String desc, InvivoAdapter invAdaptor) {
//		super(api, mv, access, name, desc);
		super(mv);
		this.invAdaptor = invAdaptor;
		this.name = name;
	}

	@Override
	public void visitInsn(int opcode) {
		if (opcode == RETURN && name.equals("<clinit>"))
			doneWithCLInit();
		super.visitInsn(opcode);
	}

	private void doneWithCLInit() {
		if (staticFieldsToClone.size() > 0) {
			for (int i = 1; i <= Instrumenter.MAX_SANDBOXES; i++) {
				visitIntInsn(BIPUSH, i);
				invAdaptor.setSandboxFlag();
				for (String s : staticFieldsToClone.keySet()) {
					super.visitFieldInsn(GETSTATIC, invAdaptor.className, s, staticFieldsToClone.get(s));
					invAdaptor.cloneValAtTopOfStack(staticFieldsToClone.get(s));
					super.visitFieldInsn(PUTSTATIC, invAdaptor.className, s + Constants.SANDBOX_SUFFIX + i, staticFieldsToClone.get(s));
				}
			}
		}
	}

	private HashMap<String, String> staticFieldsToClone = new HashMap<>();

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (this.name.equals("<clinit>") && opcode == PUTSTATIC) {
			if (owner.equals(invAdaptor.className))
				staticFieldsToClone.put(name, desc);
		} else {
			if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {

			}
		}
		super.visitFieldInsn(opcode, owner, name, desc);
	}
}
