package edu.columbia.cs.psl.vmvm.chroot;

public class ClassInstance {
	public String clazz;
	public String parent;
	public String[] interfaces;
	public ClassInstance(String clazz, String parent, String[] interfaces) {
		this.clazz = clazz;
		this.parent = parent;
		this.interfaces = interfaces;
	}
}
