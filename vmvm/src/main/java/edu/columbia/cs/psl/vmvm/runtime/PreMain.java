package edu.columbia.cs.psl.vmvm.runtime;

import edu.columbia.cs.psl.vmvm.runtime.inst.Utils;

import java.edu.columbia.cs.psl.vmvm.runtime.Reinitializer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class PreMain {
	public static boolean IS_RUNTIME_INST = true;

	public static void premain(String args, Instrumentation inst) {
		Reinitializer.inst = inst;
		if(args != null)
			Utils.ignorePattern = args;
		for(Class c : inst.getAllLoadedClasses()){
			VMVMClassFileTransformer.ignoredClasses.add(c.getName().replace('.','/'));
		}
		inst.addTransformer(new VMVMClassFileTransformer(), true);
		for(Class c : inst.getAllLoadedClasses()){
			try {
				inst.retransformClasses(c);
			} catch (UnmodifiableClassException e) {
			} catch(Throwable t){
				t.printStackTrace();
			}
		}
	}
}
