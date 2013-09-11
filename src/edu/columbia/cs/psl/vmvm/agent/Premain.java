package edu.columbia.cs.psl.vmvm.agent;

import java.lang.instrument.Instrumentation;

public class Premain {
	private static Instrumentation instrumentation;
	 /**
     * JVM hook to statically load the javaagent at startup.
     * 
     * After the Java Virtual Machine (JVM) has initialized, the premain method
     * will be called. Then the real application main method will be called.
     * 
     * @param args
     * @param inst
     * @throws Exception
     */
    public static void premain(String args, Instrumentation inst) throws Exception {
        instrumentation = inst;
        instrumentation.addTransformer(new ClassLoaderBasedTransformer(),true);
    }
}
