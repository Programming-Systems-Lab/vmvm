package edu.columbia.cs.psl.vmvm.junit.driver;


import java.lang.reflect.Field;
import java.util.ArrayList;

import junit.framework.TestResult;
import junit.framework.TestSuite;


import edu.columbia.cs.psl.vmvm.VirtualRuntime;

public class IvyDriver {
	

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
    	ArrayList<Class> tests = new ArrayList<>();
    	
//    	tests.add(com.continuent.bristlecone.benchmark.test.BenchmarkTest.class);
//    	tests.add(org.apache.ivy.IvyTest.class);
//    	tests.add(org.apache.ivy.MainTest.class);
//
//    	tests.add(org.apache.ivy.ant.AntBuildTriggerTest.class);
//    	tests.add(org.apache.ivy.ant.AntCallTriggerTest.class);
//    	tests.add(org.apache.ivy.ant.FixDepsTaskTest.class);
//    	tests.add(org.apache.ivy.ant.IvyAntSettingsBuildFileTest.class);
//    	tests.add(org.apache.ivy.ant.IvyConfigureTest.class);
//    	tests.add(org.apache.ivy.ant.IvyRetrieveBuildFileTest.class);
//    	tests.add(org.apache.ivy.osgi.repo.BundleRepoTest.class);
//
//    	
//    	tests.add(org.apache.ivy.osgi.updatesite.UpdateSiteLoaderTest.class);
    	tests.add(org.apache.ivy.IvyTest.class);
    	tests.add(org.apache.ivy.MainTest.class);
////
//    	tests.add(org.apache.ivy.ant.AntBuildTriggerTest.class);
//    	tests.add(org.apache.ivy.ant.AntCallTriggerTest.class);
//    	tests.add(org.apache.ivy.ant.FixDepsTaskTest.class);
    	tests.add(org.apache.ivy.ant.IvyAntSettingsBuildFileTest.class);
//    	tests.add(org.apache.ivy.ant.IvyArtifactPropertyTest.class);
//    	tests.add(org.apache.ivy.ant.IvyArtifactReportTest.class);
//    	tests.add(org.apache.ivy.ant.IvyBuildListTest.class);
//    	tests.add(org.apache.ivy.ant.IvyBuildNumberTest.class);
//    	tests.add(org.apache.ivy.ant.IvyCacheFilesetTest.class);
//    	tests.add(org.apache.ivy.ant.IvyCachePathTest.class);
//    	tests.add(org.apache.ivy.ant.IvyCleanCacheTest.class);
//
//    	tests.add(org.apache.ivy.ant.IvyConfigureTest.class);
//    	tests.add(org.apache.ivy.ant.IvyRetrieveBuildFileTest.class);
//    	tests.add(org.apache.ivy.core.resolve.ResolveTest.class);
//    	tests.add(org.apache.ivy.osgi.repo.BundleRepoTest.class);
////

//    	tests.add(org.apache.ivy.plugins.conflict.LatestCompatibleConflictManagerTest.class);
//    	tests.add(org.apache.ivy.plugins.conflict.RegexpConflictManagerTest.class);
//    	tests.add(org.apache.ivy.plugins.conflict.StrictConflictManagerTest.class);
//    	tests.add(org.apache.ivy.plugins.trigger.LogTriggerTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.ChainResolverTest.class);
//
//    	tests.add(org.apache.ivy.plugins.report.XmlReportWriterTest.class);
//
//    	tests.add(org.apache.ivy.plugins.lock.ArtifactLockStrategyTest.class);
//    	tests.add(org.apache.ivy.IvyTest.class);
//    	tests.add(org.apache.ivy.MainTest.class);
//    	tests.add(org.apache.ivy.ant.AntBuildTriggerTest.class);
//    	tests.add(org.apache.ivy.ant.AntCallTriggerTest.class);
//    	tests.add(org.apache.ivy.ant.FixDepsTaskTest.class);


//    	tests.add(org.apache.ivy.ant.IvyConvertPomTest.class);
//    	tests.add(org.apache.ivy.ant.IvyDeliverTest.class);
//    	tests.add(org.apache.ivy.ant.IvyDependencyTreeTest.class);
//    	tests.add(org.apache.ivy.ant.IvyDependencyUpdateCheckerTest.class);
//    	tests.add(org.apache.ivy.ant.IvyFindRevisionTest.class);
//    	tests.add(org.apache.ivy.ant.IvyInfoRepositoryTest.class);
//    	tests.add(org.apache.ivy.ant.IvyInfoTest.class);
//    	tests.add(org.apache.ivy.ant.IvyInstallTest.class);
//    	tests.add(org.apache.ivy.ant.IvyListModulesTest.class);
//    	tests.add(org.apache.ivy.ant.IvyPostResolveTaskTest.class);
//    	tests.add(org.apache.ivy.ant.IvyPublishTest.class);
//    	tests.add(org.apache.ivy.ant.IvyReportTest.class);
//    	tests.add(org.apache.ivy.ant.IvyRepositoryReportTest.class);
//    	tests.add(org.apache.ivy.ant.IvyResolveTest.class);
//    	tests.add(org.apache.ivy.ant.IvyResourcesTest.class);

//    	tests.add(org.apache.ivy.ant.IvyRetrieveTest.class);
//    	tests.add(org.apache.ivy.ant.IvyTaskTest.class);
//    	tests.add(org.apache.ivy.ant.IvyVarTest.class);
//    	tests.add(org.apache.ivy.core.NormalRelativeUrlResolverTest.class);
//    	tests.add(org.apache.ivy.core.cache.DefaultRepositoryCacheManagerTest.class);
//    	tests.add(org.apache.ivy.core.cache.ModuleDescriptorMemoryCacheTest.class);
//    	tests.add(org.apache.ivy.core.deliver.DeliverTest.class);
//    	tests.add(org.apache.ivy.core.event.IvyEventFilterTest.class);
//    	tests.add(org.apache.ivy.core.install.InstallTest.class);
//    	tests.add(org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptorTest.class);
//    	tests.add(org.apache.ivy.core.module.id.ModuleIdTest.class);
//    	tests.add(org.apache.ivy.core.module.id.ModuleRevisionIdTest.class);
//    	tests.add(org.apache.ivy.core.module.id.ModuleRulesTest.class);
//    	tests.add(org.apache.ivy.core.publish.PublishEngineTest.class);
//    	tests.add(org.apache.ivy.core.publish.PublishEventsTest.class);
//    	tests.add(org.apache.ivy.core.report.ResolveReportTest.class);
//    	tests.add(org.apache.ivy.core.repository.RepositoryManagementEngineTest.class);
//    	tests.add(org.apache.ivy.core.resolve.ResolveEngineTest.class);
//    	tests.add(org.apache.ivy.core.resolve.ResolveTest.class);
//    	tests.add(org.apache.ivy.core.retrieve.RetrieveTest.class);
//    	tests.add(org.apache.ivy.core.search.SearchTest.class);
//    	tests.add(org.apache.ivy.core.settings.ConfigureTest.class);
//    	tests.add(org.apache.ivy.core.settings.IvySettingsTest.class);
//    	tests.add(org.apache.ivy.core.settings.OnlineXmlSettingsParserTest.class);
//    	tests.add(org.apache.ivy.core.settings.XmlSettingsParserTest.class);
//    	tests.add(org.apache.ivy.core.sort.SortTest.class);
//    	tests.add(org.apache.ivy.osgi.core.ExecutionEnvironmentProfileLoaderTest.class);
//    	tests.add(org.apache.ivy.osgi.core.ManifestHeaderTest.class);
//    	tests.add(org.apache.ivy.osgi.core.ManifestParserTest.class);
//    	tests.add(org.apache.ivy.osgi.core.OSGiManifestParserTest.class);
//    	tests.add(org.apache.ivy.osgi.core.OsgiLatestStrategyTest.class);
//    	tests.add(org.apache.ivy.osgi.obr.OBRParserTest.class);
//    	tests.add(org.apache.ivy.osgi.obr.OBRResolverTest.class);
//    	tests.add(org.apache.ivy.osgi.obr.RequirementFilterTest.class);
//    	tests.add(org.apache.ivy.osgi.p2.P2DescriptorTest.class);

//    	tests.add(org.apache.ivy.osgi.updatesite.UpdateSiteAndIbiblioResolverTest.class);
//    	tests.add(org.apache.ivy.osgi.updatesite.UpdateSiteLoaderTest.class);
//    	tests.add(org.apache.ivy.osgi.updatesite.UpdateSiteResolverTest.class);
//    	tests.add(org.apache.ivy.osgi.util.ParseUtilTest.class);
//    	tests.add(org.apache.ivy.osgi.util.VersionRangeTest.class);
//    	tests.add(org.apache.ivy.osgi.util.VersionTest.class);
//    	tests.add(org.apache.ivy.plugins.circular.IgnoreCircularDependencyStrategyTest.class);
    	tests.add(org.apache.ivy.plugins.circular.WarnCircularDependencyStrategyTest.class);

    	//    	tests.add(org.apache.ivy.plugins.conflict.LatestConflictManagerTest.class);

//    	tests.add(org.apache.ivy.plugins.latest.LatestRevisionStrategyTest.class);

    	//    	tests.add(org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcherTest.class);
//    	tests.add(org.apache.ivy.plugins.matcher.ExactPatternMatcherTest.class);
//    	tests.add(org.apache.ivy.plugins.matcher.GlobPatternMatcherTest.class);
//    	tests.add(org.apache.ivy.plugins.matcher.RegexpPatternMatcherTest.class);
//    	tests.add(org.apache.ivy.plugins.namespace.MRIDTransformationRuleTest.class);
//    	tests.add(org.apache.ivy.plugins.namespace.NameSpaceHelperTest.class);
//    	tests.add(org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistryTest.class);
//    	tests.add(org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParserTest.class);
//    	tests.add(org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriterTest.class);
//    	tests.add(org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParserTest.class);
//    	tests.add(org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriterTest.class);
//    	tests.add(org.apache.ivy.plugins.parser.xml.XmlModuleUpdaterTest.class);
//    	tests.add(org.apache.ivy.plugins.report.XmlReportParserTest.class);
//    	tests.add(org.apache.ivy.plugins.repository.vfs.VfsRepositoryTest.class);
//    	tests.add(org.apache.ivy.plugins.repository.vfs.VfsResourceTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.DualResolverTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.FileSystemResolverTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.IBiblioResolverTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.IvyRepResolverTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.JarResolverTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.Maven2LocalTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.MirroredURLResolverTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.PackagerResolverTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.URLResolverTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.VfsResolverTest.class);
//    	tests.add(org.apache.ivy.plugins.resolver.util.ResolverHelperTest.class);
//    	tests.add(org.apache.ivy.plugins.trigger.LogTriggerTest.class);
    	tests.add(org.apache.ivy.plugins.version.LatestVersionMatcherTest.class);
//    	tests.add(org.apache.ivy.plugins.version.PatternVersionMatcherTest.class);
//    	tests.add(org.apache.ivy.plugins.version.VersionRangeMatcherTest.class);
//    	tests.add(org.apache.ivy.util.ConfiguratorTest.class);
//    	tests.add(org.apache.ivy.util.IvyPatternHelperTest.class);
    	tests.add(org.apache.ivy.util.StringUtilsTest.class);
    	tests.add(org.apache.ivy.osgi.repo.BundleRepoTest.class);
    	tests.add(org.apache.ivy.plugins.circular.WarnCircularDependencyStrategyTest.class);
//    	tests.add(org.apache.ivy.util.url.ApacheURLListerTest.class);
//    	tests.add(org.apache.ivy.util.url.ArtifactoryListingTest.class);
//    	tests.add(org.apache.ivy.util.url.BasicURLHandlerTest.class);
//    	tests.add(org.apache.ivy.util.url.HttpclientURLHandlerTest.class);
    	for(Class c : tests)
    	{
    		runTest(c);
    	}
//		SuiteWrapper.runSuite(tests, "ivy DEBUG", "vmvm", false,"");
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
    
//    static void runTest(String clazz)
//    {
//    	
//    	 try{
//    		 Class<?> testClass = ReflectionWrapper.forName(clazz,IvyDriver.class.getClassLoader());
//    	    	System.err.println(">>>>>Running testclass: " + testClass.getName());
////    	        ClassLoader ctxLdr = Thread.currentThread().getContextClassLoader();
//    	        System.setProperty(
//    	        		"tomcat.test.protocol", "org.apache.coyote.http11.Http11Protocol");
////    	        TestSuite theSuite = (TestSuite) testClass.getMethod("suite").invoke(null);
////    	        theSuite.run(new TestResult());
//    	        
//    	        Class<?> c= ReflectionWrapper.forName(clazz,TestRunner.class.getClassLoader());
//    	        ReflectionWrapper.forName("org.apache.jmeter.config.Arguments", TestRunner.class.getClassLoader());    	        
//				initializeManager(new String[]{"","./jmeter.properties","org.apache.jmeter.util.JMeterUtils"});
////				ReflectionWrapper.forName(org.apache.jmeter.config.Arguments.class.getName(), TestRunner.class.getClassLoader());
//
//				System.err.println(org.apache.jmeter.config.Arguments.class);
////				nCases = new TestSuite(c).countTestCases();
//				junit.textui.TestRunner.run((Class<? extends TestCase>) c);
////    			Class<?> c= ReflectionWrapper.forName(clazz,TestRunner.class.getClassLoader());
//
////    		 TestRunner.run((TestSuite) testClass.getMethod("suite").invoke(null));
////             Thread.currentThread().setContextClassLoader(ctxLdr);
//
// 	         System.err.println("---Done: " + testClass.getName());
// 	         
//         }
//         catch(Exception ex)
//         {
//         	ex.printStackTrace();
//         }
//	        VirtualRuntime.resetStatics();
////         Thread.currentThread().setContextClassLoader(ctxLdr);
//    }
//    public static void initializeManager(String[] args) {
//		System.err.println("JMR: " + args.length);
//		if (args.length >= 3) {
//			try {
//				UnitTestManager um = (UnitTestManager) Class.forName(args[2]).newInstance();
//				um.initializeProperties(args[1]);
//			} catch (Exception e) {
//				System.out.println("Couldn't create: " + args[2]);
//				e.printStackTrace();
//			}
//		}
//	}
//    static void runTest(Test test)
//    {
//    	
//    	 try{
//    		 Class<?> testClass = ReflectionWrapper.forName(((TestSuite) test).testAt(0).getClass().getName(),IvyDriver.class.getClassLoader());
//    	    	System.err.println(">>>>>Running testclass: " + testClass.getName());
////    	        ClassLoader ctxLdr = Thread.currentThread().getContextClassLoader();
//    	        System.setProperty(
//    	        		"tomcat.test.protocol", "org.apache.coyote.http11.Http11Protocol");
////    		 TestRunner.run(testClass);
////             Thread.currentThread().setContextClassLoader(ctxLdr);
//
// 	         System.err.println("---Done: " + testClass.getName());
// 	         
//         }
//         catch(Exception ex)
//         {
//         	ex.printStackTrace();
//         }
//	        VirtualRuntime.resetStatics();
////         Thread.currentThread().setContextClassLoader(ctxLdr);
//    }
    static void runTest(Class<?> testClass)
    {
    	System.out.println("Running testclass: " + testClass.getName());
//        ClassLoader ctxLdr = Thread.currentThread().getContextClassLoader();
    	 try{
    		 TestResult r = new junit.textui.TestRunner().doRun(new TestSuite(testClass));
    		 System.out.println("Finished: " + r.errorCount() + "err "  + r.failureCount() + "fail " + r.runCount() + "tot ");
//         	junit.textui.TestRunner.run(testClass);
//         	JUnitCore c = new JUnitCore();
//         	
//         	c.addListener(new RunListener(){
//         		@Override
//         		public void testFailure(Failure failure) throws Exception {
//         		System.out.println("FAIL: " + failure.getMessage());
//         		failure.getException().printStackTrace();
//         		}
//         		@Override
//         		public void testFinished(Description description) throws Exception {
//         			System.out.println("DONE: " + description.getDisplayName());
////         	        VirtualRuntime.resetStatics();
//
//         		}
//         		@Override
//         		public void testStarted(Description description) throws Exception {
//         			System.out.println("Starting: " + description.getDisplayName());
//         		}
//         	});
//         	
//         	c.run(Request.runner(r));

//             Thread.currentThread().setContextClassLoader(ctxLdr);

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
