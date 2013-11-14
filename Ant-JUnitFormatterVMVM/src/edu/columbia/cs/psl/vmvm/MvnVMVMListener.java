package edu.columbia.cs.psl.vmvm;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

public class MvnVMVMListener extends RunListener{

	String lastTestClass = null;
	@Override
	public void testStarted(Description description) throws Exception {
		if(lastTestClass != null && ! lastTestClass.equals(description.getClassName()))
		{
			VirtualRuntime.resetStatics();
		}
		lastTestClass=description.getClassName();
	}
}
