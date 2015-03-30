package edu.columbia.cs.psl.vmvm.chroot;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.Method;

/**
 * @author miriammelnick A class to contain metadata about a method. Includes an
 *         ASM Method, a String className, an integer for access flags, and a
 *         linkedlist of MethodInstances that invoke this method.
 */
public class MethodInstance {

	/**
	 * Encoded access flags for this method. private int access
	 * 
	 * @see Method
	 */
	private int access;

	/**
	 * Fully qualified class name of the class owning this method. private
	 * String clazz
	 */
	private String clazz;

	public LinkedList<String> functionsThatCallMe = new LinkedList<String>();

	public LinkedList<String> functionsThatICall = new LinkedList<String>();

	private boolean isFSTaint;

	/**
	 * ASM method at the core of this MethodInstance object. private Method
	 * method
	 * 
	 * @see Method
	 */
	private Method method;

	public List<Integer> tainted;

	public HashSet<String> fieldsPut = new HashSet<String>();
	public HashSet<String> fieldsGet = new HashSet<String>();
	public MethodInstance(String fullName) {

		String[] pieces = fullName.split("\\.|:");
		this.clazz = pieces[0];
		this.method = new Method(pieces[1], pieces[2]);
	}

	/**
	 * Constructor for MethodInstance - accepts method name, method description,
	 * class name, and access flag.
	 * 
	 * @param name
	 *            String name of method
	 * @param desc
	 *            String method descriptor
	 * @param clazz
	 *            String fully qualified class name
	 * @param access
	 *            int access flags in decimal
	 */
	public MethodInstance(String name, String desc, String clazz, int access) {
		this.method = new Method(name, desc);
		this.clazz = clazz;
		this.access = access;
	}
	
	
	/**
	 * (Override) This function declares two MethodInstances A, B "equal" if and
	 * only if: ((A.getMethod().equals(B.getMethod)) &&
	 * (A.getClazz().equals(B.getClazz())) == true
	 * 
	 * @see Object#equals(Object)
	 * @see MethodInstance#hashCode()
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj.getClass().equals(this.getClass())) {
			MethodInstance other = (MethodInstance) obj;
			if ((other.getClazz().equals(this.getClazz()))
					&& (other.getMethod().getName().equals(this.getMethod().getName()) && other.getMethod().getDescriptor().equals(this.getMethod().getDescriptor())))
				return true;
		}
		return false;
	}

	public int getAccess() {
		return access;
	}

	/**
	 * Get the owner class name.
	 * 
	 * @return String
	 */
	public String getClazz() {
		return clazz;
	}

	public String getFullName() {
		return this.clazz + "." + this.method.getName() + this.method.getDescriptor();
	}

	/**
	 * Get the Method underlying this MethodInstance.
	 * 
	 * @return Method
	 */
	public Method getMethod() {
		return method;
	}

	@Override
	public int hashCode() {
		return this.getClazz().hashCode() * this.getMethod().getName().hashCode() * this.getMethod().getDescriptor().hashCode();
	}


	public boolean isFSTaint() {
		return isFSTaint;
	}

	public void setFSTaint(boolean isFSTaint) {
		this.isFSTaint = isFSTaint;
	}

	@Override
	public String toString() {
		return "MethodInstance [method=" + method + ", class=" + clazz + ", fIC:" + functionsThatCallMe.size() + "]";
	}

}
