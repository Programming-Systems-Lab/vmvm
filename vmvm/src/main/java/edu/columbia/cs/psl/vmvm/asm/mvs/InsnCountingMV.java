package edu.columbia.cs.psl.vmvm.asm.mvs;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Handle;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Label;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;

public class InsnCountingMV extends MethodVisitor {

	int count;
	
	public int getCount() {
		return count-1;
	}
	public InsnCountingMV(int api, MethodVisitor mv) {
		super(api, mv);
	}
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		count++;
		super.visitFieldInsn(opcode, owner, name, desc);
	}
	@Override
	public void visitInsn(int opcode) {
		count++;
		super.visitInsn(opcode);
	}
	@Override
	public void visitIincInsn(int var, int increment) {
		count++;
		super.visitIincInsn(var, increment);
	}
	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		count++;
		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}
	@Override
	public void visitIntInsn(int opcode, int operand) {
		count++;
		super.visitIntInsn(opcode, operand);
	}
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		count++;
		super.visitJumpInsn(opcode, label);
	}
	@Override
	public void visitLdcInsn(Object cst) {
		count++;
		super.visitLdcInsn(cst);
	}
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		count++;
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		count++;
		super.visitMethodInsn(opcode, owner, name, desc, itfc);
	}
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		count++;
		super.visitMultiANewArrayInsn(desc, dims);
	}
	@Override
	public void visitTypeInsn(int opcode, String type) {
		count++;
		super.visitTypeInsn(opcode, type);
	}
	@Override
	public void visitVarInsn(int opcode, int var) {
		count++;
		super.visitVarInsn(opcode, var);
	}
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		count++;
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}
	@Override
	public void visitLabel(Label label) {
		count++;
		super.visitLabel(label);
	}
	@Override
	public void visitLineNumber(int line, Label start) {
		count++;
		super.visitLineNumber(line, start);
	}
}
