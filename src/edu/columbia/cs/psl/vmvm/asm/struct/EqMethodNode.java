package edu.columbia.cs.psl.vmvm.asm.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.MethodNode;

public class EqMethodNode extends MethodNode implements Serializable {

	public transient HashSet<EqMethodNode> methodsICall = new HashSet<>();
	public transient HashSet<EqMethodNode> methodsThatCallMe = new HashSet<>();
	public HashSet<String> typesThatIInit = new HashSet<>();
	public HashSet<Integer> ignoredInitCalls = new HashSet<>();
	public HashSet<String> typesToIgnoreInit;
	public String owner;
	public boolean hasSideEffects;
	public boolean sideEffectsExplored;
	/**
	 * 
	 */
	private static final long serialVersionUID = -3345910259321149535L;

	public EqMethodNode(int access, String name, String desc, String owner, String signature, String[] exceptions) {
		super(Opcodes.ASM5, access, name, desc, signature, exceptions);
		this.owner = owner;
		// TODO Auto-generated constructor stub
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		// default serialization 
		oos.defaultWriteObject();
		oos.writeObject(this.name);
		oos.writeObject(this.desc);
		oos.writeInt(this.access);
		oos.writeObject(this.owner);
		oos.writeBoolean(this.hasSideEffects);
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		// default deserialization
		ois.defaultReadObject();
		this.name = (String) ois.readObject();
		this.desc = (String) ois.readObject();
		this.access = ois.readInt();
		this.owner = (String) ois.readObject();
		this.hasSideEffects = ois.readBoolean();
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
		EqMethodNode other = (EqMethodNode) obj;

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

	@Override
	public String toString() {
		return "EqMethodNode [name=" + owner + "." + name + ", desc=" + desc + "]";
	}

	private int originatesOnlyFromClinit = -1;
	private boolean exploreInProgress = false;

	public boolean originatesOnlyFromClInit() {
		if (originatesOnlyFromClinit > -1)
			return originatesOnlyFromClinit == 1;
		if (this.name.equals("<clinit>")) {
			originatesOnlyFromClinit = 1;
			return true;
		} else {
			boolean ok = true;
			exploreInProgress = true;
			for (EqMethodNode mn : methodsThatCallMe) {
				if (mn.exploreInProgress)
					return true;
				ok = ok && mn.originatesOnlyFromClInit();
				if (!ok)
					break;
			}
			if (ok)
				this.originatesOnlyFromClinit = 1;
			else
				this.originatesOnlyFromClinit = 0;
		}
		return this.originatesOnlyFromClinit == 1;
	}

}
