package edu.columbia.cs.psl.vmvm.runtime.inst;

import java.util.HashMap;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LabelRemappingMV extends MethodVisitor{
	private HashMap<Label, Label> newLabels = new HashMap<Label, Label>();
	private Label finishedClinitCode;

	public LabelRemappingMV(MethodVisitor mv, Label finishedClinitlbl)
	{
		super(Opcodes.ASM5,mv);
		this.finishedClinitCode = finishedClinitlbl;
	}
	private Label remapLabel(Label l) {
		if (newLabels.containsKey(l))
			return newLabels.get(l);
		Label r = new Label();
		newLabels.put(l, r);
		return r;
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		for (int i = 0; i < labels.length; i++)
			labels[i] = remapLabel(labels[i]);
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		super.visitTryCatchBlock(remapLabel(start), remapLabel(end), remapLabel(handler), type);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		super.visitLineNumber(line, remapLabel(start));
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		super.visitLocalVariable(name, desc, signature, remapLabel(start), remapLabel(end), index);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		for (int i = 0; i < labels.length; i++)
			labels[i] = remapLabel(labels[i]);
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		super.visitJumpInsn(opcode, remapLabel(label));
	}

	@Override
	public void visitLabel(Label label) {
		super.visitLabel(remapLabel(label));
	}

	@Override
	public void visitInsn(int opcode) {
		if (opcode == Opcodes.RETURN) {
			super.visitJumpInsn(Opcodes.GOTO, finishedClinitCode);
		} else
			super.visitInsn(opcode);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}
}
