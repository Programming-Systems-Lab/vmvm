package edu.columbia.cs.psl.vmvmtest.classes;

public class ClassWithOneSField implements IFace {
	public static int foo = 5;
	public int val = 5;
	public ClassWithOneSField()
	{
		
	}
	public ClassWithOneSField(int baz)
	{
		
	}
	static int getFoo()
	{
		return foo;
	}
}
