package edu.columbia.cs.psl.vmvm.asm.struct;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;

import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.asm.InterceptingClassVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.ClassNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.InsnList;

public class MethodListClassNode extends ClassNode implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8364049116079289872L;
	public transient InsnList clInitInsns;
	public boolean hasClinit;
	public boolean hasClinitOrSuperHasClinit;
	public boolean superClinitExplored;
	
	public boolean clInitCalculatedNecessary;
	public HashSet<EqMethodNode> methodsHashSet = new HashSet<>();
	public boolean isMutable;
	
	public MethodListClassNode()
	{
		super(Opcodes.ASM5);
	}
	private void writeObject(ObjectOutputStream oos) throws IOException {
		// default serialization 
		oos.defaultWriteObject();
		oos.writeObject(this.name);
		oos.writeInt(this.access);
		oos.writeObject(this.superName);
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		// default deserialization
		ois.defaultReadObject();
		this.name = (String) ois.readObject();
		this.access = ois.readInt();
		this.superName = (String) ois.readObject();
	}
	public boolean containsMethod(String name, String desc)
	{
		return containsMethod(name, desc, 0);
	}
	public EqMethodNode getMethod(String name, String desc)
	{
		return getMethod(name, desc,0);
	}
	public EqMethodNode getMethod(String name, String desc, int n)
	{
		boolean ret = methodsHashSet.contains(new EqMethodNode(0, name, desc,this.name, null, null));
		if(ret)
		for(EqMethodNode e : methodsHashSet)
		{
			if(name.equals(e.name) && desc.equals(e.desc))
				return e;
		}
		MethodListClassNode superNode=  Instrumenter.instrumentedClasses.get(superName);
		if(superNode != null && n <= 20 && !InterceptingClassVisitor.shouldIgnoreClass(superName)){
			return superNode.getMethod(name, desc,n+1);
		}
		return null;
	}
	public boolean containsMethod(String name, String desc, int n)
	{
		if(name.contains("foo"))
			System.out.println(name+desc);
//		boolean ret = methodsHashSet.contains(new EqMethodNode(0, name, desc, null, null));
		EqMethodNode ret = getMethod(name, desc,n);
		if(ret != null && ((ret.access & Opcodes.ACC_ABSTRACT) != Opcodes.ACC_ABSTRACT || (this.access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE))
			return true;
		MethodListClassNode superNode=  Instrumenter.instrumentedClasses.get(superName);
		if(superNode != null && n <= 20 && !InterceptingClassVisitor.shouldIgnoreClass(superName)){
			return superNode.containsMethod(name, desc,n+1);
		}
		return false;
	}
}
