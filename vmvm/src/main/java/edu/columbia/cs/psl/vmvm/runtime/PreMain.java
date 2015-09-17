package edu.columbia.cs.psl.vmvm.runtime;


import java.lang.instrument.Instrumentation;

public class PreMain {
	public static boolean IS_RUNTIME_INST = true;

	public static void premain(String args, Instrumentation inst) {
		inst.addTransformer(new VMVMClassFileTransformer());
	}
}
