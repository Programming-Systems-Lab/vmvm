package edu.columbia.cs.psl.vmvm.runtime.inst;

import org.objectweb.asm.ClassWriter;

import java.util.LinkedList;

public class PlaceholderClasswriter extends ClassWriter {

	private int place;
	private int place2;
	private LinkedList<Integer> interfacePlaces = new LinkedList<Integer>();

	public PlaceholderClasswriter(int flags) {
		super(flags);
	}

	public int getPlace() {
		return place;
	}

	public int getPlace2() {
		return place2;
	}

	public LinkedList<Integer> getInterfacePlaces() {
		return interfacePlaces;
	}

	@Override
	public int newClass(String value) {
		return super.newClass(value);
	}

	@Override
	public int newUTF8(String value) {
		int ret = super.newUTF8(value);
		if ("net/jonbell/PlaceHolder".equals(value)) {
			place = ret;
		} else if ("net/jonbell/PlaceHolder2".equals(value)) {
			place2 = ret;
		} else if (value != null && value.startsWith("net/jonbell/IFacePlaceHolder")) {
			interfacePlaces.add(ret);
		}
		return ret;
	}

}
