package edu.columbia.cs.psl.vmvmtest.classes;

public enum EEnum {
	A("a"), B("b"), C("c");
	private final String name;
	EEnum(String name)
	{
		this.name= name;
	}
	public String getName() {
		return name;
	}
}
