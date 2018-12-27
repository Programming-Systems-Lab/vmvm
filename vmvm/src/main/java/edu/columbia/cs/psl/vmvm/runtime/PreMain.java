package edu.columbia.cs.psl.vmvm.runtime;

import edu.columbia.cs.psl.vmvm.runtime.inst.Utils;

import java.lang.instrument.Instrumentation;

public class PreMain {
	public static boolean IS_RUNTIME_INST = true;

	public static void premain(String args, Instrumentation inst) {
		Reinitializer.inst = inst;
		if(args != null)
			Utils.ignorePattern = args;
		inst.addTransformer(new VMVMClassFileTransformer(), true);
	}
}
