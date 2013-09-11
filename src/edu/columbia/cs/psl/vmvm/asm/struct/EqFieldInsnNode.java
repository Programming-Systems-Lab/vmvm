package edu.columbia.cs.psl.vmvm.asm.struct;

import org.objectweb.asm.tree.FieldInsnNode;

public class EqFieldInsnNode extends FieldInsnNode {

	private boolean isFinal;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((desc == null) ? 0 : desc.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EqFieldInsnNode other = (EqFieldInsnNode) obj;
		if (desc == null) {
			if (other.desc != null)
				return false;
		} else if (!desc.equals(other.desc))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public EqFieldInsnNode(int opcode, String owner, String name, String desc) {
		super(opcode, owner, name, desc);
	}

	public EqFieldInsnNode(int opcode, String owner, String name, String desc,
			boolean isFinal) {
		super(opcode, owner, name, desc);
		this.isFinal = isFinal;
	}

	@Override
	public String toString() {
		return "EqFieldInsnNode [name=" + name + ", owner=" + owner + ", desc="
				+ desc + "]";
	}

}
