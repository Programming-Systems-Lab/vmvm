package edu.columbia.cs.psl.vmvm;


public class Virtualizer {
	
	public static void execInVM(){
		throw new RuntimeException("execInVM was called, but the VMVM instrumenter was never run on this code");
	}
	public static void execInVM(int n)
	{
		throw new RuntimeException("execInVM was called, but the VMVM instrumenter was never run on this code");
	}
	public static void exitVM()
	{
		throw new RuntimeException("exitVM was called, but the VMVM instrumenter was never run on this code");
	}
	
	
}
