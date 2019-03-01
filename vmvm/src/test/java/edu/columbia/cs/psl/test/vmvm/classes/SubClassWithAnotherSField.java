package edu.columbia.cs.psl.test.vmvm.classes;

public class SubClassWithAnotherSField extends ClassWithOneSField {
	static{
		System.out.println("SubClassWithAnotherSField init");
	}
	public static int bar = 4;

	public SubClassWithAnotherSField() {
	}

	public SubClassWithAnotherSField(int baz) {

	}
}
