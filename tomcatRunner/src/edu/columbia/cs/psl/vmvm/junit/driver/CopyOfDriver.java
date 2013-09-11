package edu.columbia.cs.psl.vmvm.junit.driver;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

import org.apache.jorphan.test.UnitTestManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;




import edu.columbia.cs.psl.vmvm.CloningUtils;
import edu.columbia.cs.psl.vmvm.ReflectionWrapper;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.sirRunner.SuiteWrapper;

public class CopyOfDriver {
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
    	ArrayList<String> tests = new ArrayList<>();

//        tests.add("org.apache.xml.security.test.c14n.helper.C14nHelperTest");
//        tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315Test");
//        tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315ExclusiveTest");
//        tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.XalanBug1425Test");
//        tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.AttributeAncestorOrSelf");
//        tests.add("org.apache.xml.security.test.signature.XMLSignatureInputTest");
//        tests.add("org.apache.xml.security.test.transforms.implementations.TransformBase64DecodeTest");
//        tests.add("org.apache.xml.security.test.utils.resolver.ResourceResolverSpiTest");
//        tests.add("org.apache.xml.security.test.utils.Base64Test");
//        
//        tests.add("org.apache.xml.security.test.interop.BaltimoreTest");
//        tests.add("org.apache.xml.security.test.interop.IAIKTest");
//        tests.add("org.apache.xml.security.test.interop.RSASecurityTest");
//
//        tests.add("org.apache.xml.security.test.interop.RSASecurityTest");
    	
//        tests.add("org.apache.xml.security.test.c14n.implementations.ExclusiveC14NInterop");
//        tests.add("org.apache.xml.security.test.c14n.implementations.ExclusiveC14NInterop");
//    	tests.add("org.apache.jmeter.gui.action.Load$Test");
//		tests.add("org.apache.jmeter.gui.action.Save$Test");
//		tests.add("org.apache.jmeter.engine.TreeCloner$Test");
//		tests.add("org.apache.jmeter.threads.TestCompiler$Test");
//		tests.add("org.apache.jmeter.junit.JMeterTest");
//		tests.add("org.apache.jmeter.save.SaveService$Test");
//		tests.add("org.apache.jmeter.util.StringUtilities$Test");
//		tests.add("org.apache.jmeter.config.gui.ArgumentsPanel$Test");
//		tests.add("org.apache.jmeter.control.OnceOnlyController$Test");
//		tests.add("org.apache.jmeter.control.GenericController$Test");
//		tests.add("org.apache.jmeter.control.InterleaveControl$Test");
//		tests.add("org.apache.jmeter.control.LoopController$Test");
//		tests.add("org.apache.jmeter.junit.protocol.http.config.UrlConfigTest");
//		tests.add("org.apache.jmeter.junit.protocol.http.parser.HtmlParserTester");
//		tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSampler$Test");
//		tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSamplerFull$Test");
//		tests.add("org.apache.jmeter.protocol.http.modifier.URLRewritingModifier$Test");
//		tests.add("org.apache.jmeter.protocol.http.parser.HtmlParser$Test");
//		tests.add("org.apache.jmeter.protocol.http.proxy.ProxyControl$Test");
//		tests.add("org.apache.jmeter.protocol.http.util.HTTPArgument$Test");
//		tests.add("org.apache.jmeter.protocol.http.proxy.HttpRequestHdr$Test");
//		tests.add("org.apache.jmeter.visualizers.StatVisualizerModel$Test");
//		tests.add("org.apache.jmeter.engine.util.ValueReplacer$Test");
//		tests.add("org.apache.jmeter.testelement.property.CollectionProperty$Test");
//		tests.add("org.apache.jmeter.extractor.RegexExtractor$Test");
//		tests.add("org.apache.jmeter.protocol.http.control.CookieManager$Test");
//		tests.add("org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui$Test");
//		tests.add("org.apache.jmeter.protocol.http.modifier.AnchorModifier$Test");
    	tests.add("com.continuent.bristlecone.benchmark.test.BenchmarkTest");
//    	
//		tests.add("org.apache.jmeter.gui.action.Load$Test");
//		tests.add("org.apache.jmeter.gui.action.Save$Test");
//		tests.add("org.apache.jmeter.save.SaveService$Test");
//		tests.add("org.apache.jmeter.engine.TreeCloner$Test");
//		tests.add("org.apache.jmeter.functions.CompoundFunction$Test");
//		tests.add("org.apache.jmeter.threads.TestCompiler$Test");
//		tests.add("org.apache.jmeter.junit.JMeterTest");
//		tests.add("org.apache.jmeter.save.SaveService$Test");
//		tests.add("org.apache.jmeter.config.gui.ArgumentsPanel$Test");
//		tests.add("org.apache.jmeter.control.OnceOnlyController$Test");
//		tests.add("org.apache.jmeter.control.GenericController$Test");
//		tests.add("org.apache.jmeter.control.InterleaveControl$Test");
//		tests.add("org.apache.jmeter.control.LoopController$Test");
//		tests.add("org.apache.jmeter.functions.RegexFunction$Test");
//		tests.add("org.apache.jmeter.junit.protocol.http.config.UrlConfigTest");
//		tests.add("org.apache.jmeter.junit.protocol.http.parser.HtmlParserTester");
//		tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSampler$Test");
//		tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSamplerFull$Test");
//		tests.add("org.apache.jmeter.protocol.http.modifier.URLRewritingModifier$Test");
//		tests.add("org.apache.jmeter.protocol.http.parser.HtmlParser$Test");
//		tests.add("org.apache.jmeter.protocol.http.proxy.ProxyControl$Test");
//		tests.add("org.apache.jmeter.protocol.http.util.HTTPArgument$Test");
		
//		tests.add("org.apache.jmeter.gui.action.Load$Test");
//		tests.add("org.apache.jmeter.gui.action.Save$Test");
//		tests.add("org.apache.jmeter.engine.TreeCloner$Test");
//		tests.add("org.apache.jmeter.functions.CompoundFunction$Test");
//		tests.add("org.apache.jmeter.threads.TestCompiler$Test");
//		tests.add("org.apache.jmeter.junit.JMeterTest");
//		initializeManager(new String[]{"","./jmeter.properties","org.apache.jmeter.util.JMeterUtils"});
//
//        for(String t : tests)
//        	runTest(t);
		
		SuiteWrapper.runSuite(tests, "jmeter v5", "vmvm", false, "./jmeter.properties org.apache.jmeter.util.JMeterUtils");
//    	runTest(org.apache.xml.security.test.c14n.helper.C14nHelperTest.suite());
//    	runTest(org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315Test.suite());
//    	runTest(org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315ExclusiveTest.suite());
//    	runTest(org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.XalanBug1425Test.suite());
//    	runTest(org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.AttributeAncestorOrSelf.suite());
//    	runTest(org.apache.xml.security.test.signature.XMLSignatureInputTest.suite());
//    	runTest(org.apache.xml.security.test.transforms.implementations.TransformBase64DecodeTest.suite());
//    	TestSuite suite = new TestSuite();
//		TestSuite s1 = (TestSuite) org.apache.xml.security.test.ModuleTest.suite();
//		TestSuite s2 = (TestSuite) org.apache.xml.security.test.InteropTest.suite();
//		Enumeration<Test> e = s1.tests();
//		while(e.hasMoreElements())
//		{
//			Test t = e.nextElement();
//			suite.addTest(t);
//		}
//		e = s2.tests();
//		while(e.hasMoreElements())
//		{
//			Test t = e.nextElement();
//			suite.addTest(t);
//		}
//		e=suite.tests();
//		while(e.hasMoreElements())
//		{
//			Test t = e.nextElement();
//			runTest(t);
//		}

    }
    
    static void runTest(String clazz)
    {
    	
    	 try{
    		 Class<?> testClass = ReflectionWrapper.forName(clazz,CopyOfDriver.class.getClassLoader());
    	    	System.err.println(">>>>>Running testclass: " + testClass.getName());
//    	        ClassLoader ctxLdr = Thread.currentThread().getContextClassLoader();
    	        System.setProperty(
    	        		"tomcat.test.protocol", "org.apache.coyote.http11.Http11Protocol");
//    	        TestSuite theSuite = (TestSuite) testClass.getMethod("suite").invoke(null);
//    	        theSuite.run(new TestResult());
    	        
    	        Class<?> c= ReflectionWrapper.forName(clazz,TestRunner.class.getClassLoader());
    	        ReflectionWrapper.forName("org.apache.jmeter.config.Arguments", TestRunner.class.getClassLoader());    	        
				initializeManager(new String[]{"","./jmeter.properties","org.apache.jmeter.util.JMeterUtils"});
//				ReflectionWrapper.forName(org.apache.jmeter.config.Arguments.class.getName(), TestRunner.class.getClassLoader());

				System.err.println(org.apache.jmeter.config.Arguments.class);
//				nCases = new TestSuite(c).countTestCases();
				junit.textui.TestRunner.run((Class<? extends TestCase>) c);
//    			Class<?> c= ReflectionWrapper.forName(clazz,TestRunner.class.getClassLoader());

//    		 TestRunner.run((TestSuite) testClass.getMethod("suite").invoke(null));
//             Thread.currentThread().setContextClassLoader(ctxLdr);

 	         System.err.println("---Done: " + testClass.getName());
 	         
         }
         catch(Exception ex)
         {
         	ex.printStackTrace();
         }
	        VirtualRuntime.resetStatics();
//         Thread.currentThread().setContextClassLoader(ctxLdr);
    }
    public static void initializeManager(String[] args) {
		System.err.println("JMR: " + args.length);
		if (args.length >= 3) {
			try {
				UnitTestManager um = (UnitTestManager) Class.forName(args[2]).newInstance();
				um.initializeProperties(args[1]);
			} catch (Exception e) {
				System.out.println("Couldn't create: " + args[2]);
				e.printStackTrace();
			}
		}
	}
    static void runTest(Test test)
    {
    	
    	 try{
    		 Class<?> testClass = ReflectionWrapper.forName(((TestSuite) test).testAt(0).getClass().getName(),CopyOfDriver.class.getClassLoader());
    	    	System.err.println(">>>>>Running testclass: " + testClass.getName());
//    	        ClassLoader ctxLdr = Thread.currentThread().getContextClassLoader();
    	        System.setProperty(
    	        		"tomcat.test.protocol", "org.apache.coyote.http11.Http11Protocol");
//    		 TestRunner.run(testClass);
//             Thread.currentThread().setContextClassLoader(ctxLdr);

 	         System.err.println("---Done: " + testClass.getName());
 	         
         }
         catch(Exception ex)
         {
         	ex.printStackTrace();
         }
	        VirtualRuntime.resetStatics();
//         Thread.currentThread().setContextClassLoader(ctxLdr);
    }
}
