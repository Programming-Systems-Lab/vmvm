package edu.columbia.cs.psl.vmvm;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import java.edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class MvnVMVMListener extends RunListener {

	@Override
	public void testStarted(Description description) throws Exception {
		Reinitializer.newTestClassHit(description.getClassName());
	}


}
