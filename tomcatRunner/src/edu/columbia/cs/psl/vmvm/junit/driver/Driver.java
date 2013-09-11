package edu.columbia.cs.psl.vmvm.junit.driver;


import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Scanner;


import org.apache.catalina.loader.TestVirtualContext;
import org.apache.tomcat.util.http.TestCookiesAllowEquals;
import org.apache.tomcat.util.http.TestCookiesAllowHttpSeps;
import org.apache.tomcat.util.http.TestCookiesAllowNameOnly;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.BlockJUnit4ClassRunner;

import edu.columbia.cs.psl.vmvm.CloningUtils;
import edu.columbia.cs.psl.vmvm.ReflectionWrapper;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;

public class Driver {
    @Test
    public static void main(String[] args) {
//    	runTest(TestOutputBuffer.class);
//    	runTest(TestAsyncContextImpl.class);
//    	runTest(TestStandardContext.class);
//    	runTest(TestRemoteIpFilter.class);
//    	runTest(TestVirtualContext.class);
//    	runTest(TestCookiesAllowEquals.class);
//    	runTest(TestCookiesAllowNameOnly.class);
//    	runTest(TestCookiesAllowHttpSeps.class);
//    	runTest(TestAbstractHttp11Processor.class);
//    	runTest(TestAbstractHttp11Processor.class);
//    	runTest(TestFormAuthenticator.class);
//    	runTest(TestNonLoginAndBasicAuthenticator.class);
//    	runTest(TestSSOnonLoginAndBasicAuthenticator.class);
//    	runTest(TestSSOnonLoginAndDigestAuthenticator.class);
//    	runTest(TestCometProcessor.class);
//    	runTest(TestConnector.class);
//    	runTest(TestCoyoteAdapter.class);
//    	runTest(TestInputBuffer.class);
//    	runTest(TestKeepAliveCount.class);
//    	runTest(TestMaxConnections.class);
//    	runTest(TestOutputBuffer.class);
//    	runTest(AnnotationUtilsTest.class);
//    	runTest(ObjectUtilsTest.class);
//    	runTest(SystemUtilsTest.class);
//    	runTest(RandomStringUtilsTest.class);
//    	runTest(ToStringBuilderTest.class);
//    	runTest(ConstructorUtilsTest.class);
//    	runTest(ClassUtilsTest.class);
//    	runTest(MethodUtilsTest.class);
//    	runTest(TestCompositeELResolver.class);
//    	runTest(TestCompositeELResolver.class);
//    	runTest(org.apache.xml.security.test.c14n.helper.C14nHelperTest.class);
//    	runTest(org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315Test.class);
//    	runTest(org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315ExclusiveTest.class);
//    	runTest(org.apache.catalina.core.TestStandardWrapper.class);
//    	runTest(org.apache.catalina.core.TestSwallowAbortedUploads.class);
//    	runTest(org.apache.catalina.core.TestStandardWrapper.class);
//    	runTest(org.apache.catalina.loader.TestWebappClassLoaderThreadLocalMemoryLeak.class); //still not fixed
//    	runTest(org.apache.catalina.startup.TestTomcatClassLoader.class);
    	runTest(org.apache.catalina.tribes.group.TestGroupChannelSenderConnections.class);
    	runTest(org.apache.catalina.tribes.group.interceptors.TestNonBlockingCoordinator.class);
    	runTest(org.apache.catalina.tribes.group.interceptors.TestOrderInterceptor.class);
    	runTest(org.apache.catalina.tribes.group.interceptors.TestTcpFailureDetector.class);
//    	runTest(org.apache.catalina.core.TestStandardWrapper.class);
//    	runTest(org.apache.catalina.core.TestStandardWrapper.class);
//    	runTest(org.apache.catalina.core.TestSwallowAbortedUploads.class);
//    	runTest(org.apache.catalina.core.TestStandardWrapper.class);
//    	runTest(org.apache.catalina.loader.TestWebappClassLoaderThreadLocalMemoryLeak.class); //still not fixed
//    	runTest(org.apache.catalina.startup.TestTomcatClassLoader.class);
    	runTest(org.apache.catalina.tribes.group.TestGroupChannelSenderConnections.class);
    	runTest(org.apache.catalina.tribes.group.interceptors.TestNonBlockingCoordinator.class);
    	runTest(org.apache.catalina.tribes.group.interceptors.TestOrderInterceptor.class);
    	runTest(org.apache.catalina.tribes.group.interceptors.TestTcpFailureDetector.class);
    	runTest(org.apache.catalina.core.TestStandardWrapper.class);
    }
    static void runTest(Class<?> testClass)
    {
    	System.out.println("Running testclass: " + testClass.getName());
        ClassLoader ctxLdr = Thread.currentThread().getContextClassLoader();
        System.setProperty(
        		"tomcat.test.protocol", "org.apache.coyote.http11.Http11Protocol");
    	 try{

    		 ReflectionWrapper.forName(testClass.getName(), true, testClass.getClassLoader());
    		 Runner r = new BlockJUnit4ClassRunner(testClass);
         	JUnitCore c = new JUnitCore();
         	
         	c.addListener(new RunListener(){
         		@Override
         		public void testFailure(Failure failure) throws Exception {
         		System.out.println("FAIL: " + failure.getMessage());
         		failure.getException().printStackTrace();
         		}
         		@Override
         		public void testFinished(Description description) throws Exception {
         			System.out.println("DONE: " + description.getDisplayName());
//         	        VirtualRuntime.resetStatics();

         		}
         		@Override
         		public void testStarted(Description description) throws Exception {
         			System.out.println("Starting: " + description.getDisplayName());
         		}
         	});
         	
         	c.run(Request.runner(r));

             Thread.currentThread().setContextClassLoader(ctxLdr);

 	        VirtualRuntime.resetStatics();
         }
         catch(Exception ex)
         {
         	ex.printStackTrace();
         }
         System.out.println("---Done: " + testClass.getName());
//         Thread.currentThread().setContextClassLoader(ctxLdr);
    }
}
