package edu.columbia.cs.psl.vmvm.asm.struct;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.MethodInsnNode;

public class EqMethodInsnNode extends MethodInsnNode {
	public EqMethodInsnNode(
	        final int opcode,
	        final String owner,
	        final String name,
	        final String desc, boolean itfc)
	    {
	        super(opcode,owner,name,desc, itfc);
	    }
	public EqMethodInsnNode(
	        final int opcode,
	        final String owner,
	        final String name,
	        final String desc, int flag, boolean itfc)
	    {
	        super(opcode,owner,name,desc, itfc);
	        this.flag = flag;
	    }
	public static final int FLAG_SUPER_INVOKE_CHROOT = 1;
	public int flag = 0;
	@Override
	public String toString() {
		return "EqMethodInsnNode [owner=" + owner + ", name=" + name + ", desc=" + desc + ", opcode=" + opcode + "]";
	}

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
		EqMethodInsnNode other = (EqMethodInsnNode) obj;
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
		if(flag != other.flag)
			return false;
		return true;
	}
    
}
