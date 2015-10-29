package edu.columbia.cs.psl.test.vmvm.classes;

public class TryCatchInInitClass {
	static Class c;

	static {
		try{
			c = Class.forName("foo");
		}
		catch(Throwable t)
		{
			
		}
	}
	static void foo()
	{
		
	}
}
