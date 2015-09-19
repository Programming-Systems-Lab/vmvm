package edu.columbia.cs.psl.vmvm;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class MvnVMVMListener extends RunListener {

	String lastTestClass = null;

	@Override
	public void testStarted(Description description) throws Exception {
		if (lastTestClass != null && !lastTestClass.equals(description.getClassName())) {
			Reinitializer.markAllClassesForReinit();
		}
		lastTestClass = description.getClassName();
	}
}
