package edu.columbia.cs.psl.vmvm.sirRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.jmeter.gui.tree.JMeterTreeNode;
//import org.apache.log.Hierarchy;
//import org.apache.log.Logger;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.reflect.ClassFinder;
import org.apache.jorphan.test.UnitTestManager;

import junit.framework.TestSuite;

public class JMeterRunner {
	static String[] _args;

	public static void reinit() {
		//		initializeLogging(_args);
		initializeManager(_args);
	}

	public static void main(String[] args) {
		_args = new String[args.length - 2];
		System.arraycopy(args, 0, _args, 0, args.length - 2);
		reinit();
		//		TestSuite suite = suite(args[0]);
		ArrayList<String> tests = new ArrayList<>();

		if(args[args.length - 2].equals("v0")){
			tests.add("org.apache.jmeter.gui.action.Load$Test");
			tests.add("org.apache.jmeter.gui.action.Save$Test");
			tests.add("org.apache.jmeter.engine.TreeCloner$Test");
			tests.add("org.apache.jmeter.util.ListedHashTree$Test");
			tests.add("org.apache.jmeter.util.SearchByClass$Test");
			tests.add("org.apache.jmeter.util.ClassFinder$Test");
			tests.add("org.apache.jmeter.functions.CompoundFunction$Test");
			tests.add("org.apache.jmeter.threads.TestCompiler$Test");
			tests.add("org.apache.jmeter.junit.JMeterTest");
			tests.add("org.apache.jmeter.save.SaveService$Test");
			tests.add("org.apache.jmeter.config.gui.ArgumentsPanel$Test");
			tests.add("org.apache.jmeter.control.OnceOnlyController$Test");
			tests.add("org.apache.jmeter.control.GenericController$Test");
			tests.add("org.apache.jmeter.control.InterleaveControl$Test");
			tests.add("org.apache.jmeter.control.LoopController$Test");
			tests.add("org.apache.jmeter.functions.RegexFunction$Test");
			tests.add("org.apache.jmeter.junit.protocol.http.config.UrlConfigTest");
			tests.add("org.apache.jmeter.junit.protocol.http.parser.HtmlParserTester");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSampler$Test");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSamplerFull$Test");
			tests.add("org.apache.jmeter.protocol.http.modifier.URLRewritingModifier$Test");
			tests.add("org.apache.jmeter.protocol.http.parser.HtmlParser$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.ProxyControl$Test");
			tests.add("org.apache.jmeter.protocol.http.util.HTTPArgument$Test");
		}else if(args[args.length - 2].equals("v1")){
			tests.add("org.apache.jmeter.gui.action.Load$Test");
			tests.add("org.apache.jmeter.gui.action.Save$Test");
			tests.add("org.apache.jmeter.engine.TreeCloner$Test");
			tests.add("org.apache.jmeter.functions.ValueReplacer$Test");
			tests.add("org.apache.jmeter.functions.CompoundFunction$Test");
			tests.add("org.apache.jmeter.threads.TestCompiler$Test");
			tests.add("org.apache.jmeter.junit.JMeterTest");
			tests.add("org.apache.jmeter.save.SaveService$Test");
			tests.add("org.apache.jmeter.util.StringUtilities$Test");
			tests.add("org.apache.jmeter.config.gui.ArgumentsPanel$Test");
			tests.add("org.apache.jmeter.control.OnceOnlyController$Test");
			tests.add("org.apache.jmeter.control.GenericController$Test");
			tests.add("org.apache.jmeter.control.InterleaveControl$Test");
			tests.add("org.apache.jmeter.control.LoopController$Test");
			tests.add("org.apache.jmeter.functions.RegexFunction$Test");
			tests.add("org.apache.jmeter.junit.protocol.http.config.UrlConfigTest");
			tests.add("org.apache.jmeter.junit.protocol.http.parser.HtmlParserTester");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSampler$Test");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSamplerFull$Test");
			tests.add("org.apache.jmeter.protocol.http.modifier.URLRewritingModifier$Test");
			tests.add("org.apache.jmeter.protocol.http.parser.HtmlParser$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.ProxyControl$Test");
			tests.add("org.apache.jmeter.protocol.http.util.HTTPArgument$Test");
		}else if(args[args.length - 2].equals("v2")){
			tests.add("org.apache.jmeter.gui.action.Load$Test");
			tests.add("org.apache.jmeter.gui.action.Save$Test");
			tests.add("org.apache.jmeter.engine.TreeCloner$Test");
			tests.add("org.apache.jmeter.functions.ValueReplacer$Test");
			tests.add("org.apache.jmeter.functions.CompoundFunction$Test");
			tests.add("org.apache.jmeter.threads.TestCompiler$Test");
			tests.add("org.apache.jmeter.junit.JMeterTest");
			tests.add("org.apache.jmeter.save.SaveService$Test");
			tests.add("org.apache.jmeter.util.StringUtilities$Test");
			tests.add("org.apache.jmeter.config.gui.ArgumentsPanel$Test");
			tests.add("org.apache.jmeter.control.OnceOnlyController$Test");
			tests.add("org.apache.jmeter.control.GenericController$Test");
			tests.add("org.apache.jmeter.control.InterleaveControl$Test");
			tests.add("org.apache.jmeter.control.LoopController$Test");
			tests.add("org.apache.jmeter.functions.RegexFunction$Test");
			tests.add("org.apache.jmeter.junit.protocol.http.config.UrlConfigTest");
			tests.add("org.apache.jmeter.junit.protocol.http.parser.HtmlParserTester");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSampler$Test");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSamplerFull$Test");
			tests.add("org.apache.jmeter.protocol.http.modifier.URLRewritingModifier$Test");
			tests.add("org.apache.jmeter.protocol.http.parser.HtmlParser$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.ProxyControl$Test");
			tests.add("org.apache.jmeter.protocol.http.util.HTTPArgument$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.HttpRequestHdr$Test");
			tests.add("org.apache.jmeter.visualizers.StatVisualizerModel$Test");
		}else if(args[args.length - 2].equals("v3")){
			tests.add("org.apache.jmeter.gui.action.Load$Test");
			tests.add("org.apache.jmeter.gui.action.Save$Test");
			tests.add("org.apache.jmeter.engine.TreeCloner$Test");
			tests.add("org.apache.jmeter.threads.TestCompiler$Test");
			tests.add("org.apache.jmeter.junit.JMeterTest");
			tests.add("org.apache.jmeter.save.SaveService$Test");
			tests.add("org.apache.jmeter.util.StringUtilities$Test");
			tests.add("org.apache.jmeter.config.gui.ArgumentsPanel$Test");
			tests.add("org.apache.jmeter.control.OnceOnlyController$Test");
			tests.add("org.apache.jmeter.control.GenericController$Test");
			tests.add("org.apache.jmeter.control.InterleaveControl$Test");
			tests.add("org.apache.jmeter.control.LoopController$Test");
			tests.add("org.apache.jmeter.junit.protocol.http.config.UrlConfigTest");
			tests.add("org.apache.jmeter.junit.protocol.http.parser.HtmlParserTester");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSampler$Test");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSamplerFull$Test");
			tests.add("org.apache.jmeter.protocol.http.modifier.URLRewritingModifier$Test");
			tests.add("org.apache.jmeter.protocol.http.parser.HtmlParser$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.ProxyControl$Test");
			tests.add("org.apache.jmeter.protocol.http.util.HTTPArgument$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.HttpRequestHdr$Test");
			tests.add("org.apache.jmeter.visualizers.StatVisualizerModel$Test");
			tests.add("org.apache.jmeter.engine.util.ValueReplacer$Test");
			tests.add("org.apache.jmeter.testelement.property.CollectionProperty$Test");
			tests.add("org.apache.jmeter.extractor.RegexExtractor$Test");
			tests.add("org.apache.jmeter.protocol.http.control.CookieManager$Test");
			tests.add("org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui$Test");
			tests.add("org.apache.jmeter.protocol.http.modifier.AnchorModifier$Test");
		}else if(args[args.length - 2].equals("v4")){
			tests.add("org.apache.jmeter.gui.action.Load$Test");
			tests.add("org.apache.jmeter.gui.action.Save$Test");
			tests.add("org.apache.jmeter.engine.TreeCloner$Test");
			tests.add("org.apache.jmeter.threads.TestCompiler$Test");
			tests.add("org.apache.jmeter.junit.JMeterTest");
			tests.add("org.apache.jmeter.save.SaveService$Test");
			tests.add("org.apache.jmeter.util.StringUtilities$Test");
			tests.add("org.apache.jmeter.config.gui.ArgumentsPanel$Test");
			tests.add("org.apache.jmeter.control.OnceOnlyController$Test");
			tests.add("org.apache.jmeter.control.GenericController$Test");
			tests.add("org.apache.jmeter.control.InterleaveControl$Test");
			tests.add("org.apache.jmeter.control.LoopController$Test");
			tests.add("org.apache.jmeter.junit.protocol.http.config.UrlConfigTest");
			tests.add("org.apache.jmeter.junit.protocol.http.parser.HtmlParserTester");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSampler$Test");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSamplerFull$Test");
			tests.add("org.apache.jmeter.protocol.http.modifier.URLRewritingModifier$Test");
			tests.add("org.apache.jmeter.protocol.http.parser.HtmlParser$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.ProxyControl$Test");
			tests.add("org.apache.jmeter.protocol.http.util.HTTPArgument$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.HttpRequestHdr$Test");
			tests.add("org.apache.jmeter.visualizers.StatVisualizerModel$Test");
			tests.add("org.apache.jmeter.engine.util.ValueReplacer$Test");
			tests.add("org.apache.jmeter.testelement.property.CollectionProperty$Test");
			tests.add("org.apache.jmeter.extractor.RegexExtractor$Test");
			tests.add("org.apache.jmeter.protocol.http.control.CookieManager$Test");
			tests.add("org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui$Test");
			tests.add("org.apache.jmeter.protocol.http.modifier.AnchorModifier$Test");
		}else if(args[args.length - 2].equals("v5")){
			tests.add("org.apache.jmeter.gui.action.Load$Test");
			tests.add("org.apache.jmeter.gui.action.Save$Test");
			tests.add("org.apache.jmeter.engine.TreeCloner$Test");
			tests.add("org.apache.jmeter.threads.TestCompiler$Test");
			tests.add("org.apache.jmeter.junit.JMeterTest");
			tests.add("org.apache.jmeter.save.SaveService$Test");
			tests.add("org.apache.jmeter.util.StringUtilities$Test");
			tests.add("org.apache.jmeter.config.gui.ArgumentsPanel$Test");
			tests.add("org.apache.jmeter.control.OnceOnlyController$Test");
			tests.add("org.apache.jmeter.control.GenericController$Test");
			tests.add("org.apache.jmeter.control.InterleaveControl$Test");
			tests.add("org.apache.jmeter.control.LoopController$Test");
			tests.add("org.apache.jmeter.junit.protocol.http.config.UrlConfigTest");
			tests.add("org.apache.jmeter.junit.protocol.http.parser.HtmlParserTester");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSampler$Test");
			tests.add("org.apache.jmeter.protocol.http.sampler.HTTPSamplerFull$Test");
			tests.add("org.apache.jmeter.protocol.http.modifier.URLRewritingModifier$Test");
			tests.add("org.apache.jmeter.protocol.http.parser.HtmlParser$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.ProxyControl$Test");
			tests.add("org.apache.jmeter.protocol.http.util.HTTPArgument$Test");
			tests.add("org.apache.jmeter.protocol.http.proxy.HttpRequestHdr$Test");
			tests.add("org.apache.jmeter.visualizers.StatVisualizerModel$Test");
			tests.add("org.apache.jmeter.engine.util.ValueReplacer$Test");
			tests.add("org.apache.jmeter.testelement.property.CollectionProperty$Test");
			tests.add("org.apache.jmeter.extractor.RegexExtractor$Test");
			tests.add("org.apache.jmeter.protocol.http.control.CookieManager$Test");
			tests.add("org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui$Test");
			tests.add("org.apache.jmeter.protocol.http.modifier.AnchorModifier$Test");
			tests.add("org.apache.jmeter.testelement.property.PackageTest");
			tests.add("org.apache.jmeter.testelement.PackageTest");
			tests.add("org.apache.jmeter.engine.util.PackageTest");
			tests.add("org.apache.jmeter.protocol.http.sampler.PackageTest");
		}


		synchronized (JMeterTreeNode.class) {
			System.out.println("JMetertreenode ok");
		}
		synchronized (org.apache.jmeter.config.Arguments.class) {
			System.out.println("Arguments OK");
		}
		SuiteWrapper.runSuite(tests, "JMeter " + args[args.length - 2], args[args.length - 1], false, args[1] + " " + args[2]);
	}

	private static TestSuite suite(String searchPaths) {
		TestSuite suite = new TestSuite();
		try {
			Iterator classes = ClassFinder.findClassesThatExtend(searchPaths.split(","), new Class[] { TestCase.class }, true).iterator();
			while (classes.hasNext()) {
				String name = (String) classes.next();
				try {
					suite.addTest(new TestSuite(Class.forName(name)));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return suite;
	}

	/**
	 * An overridable method that initializes the logging for the unit test run,
	 * using the properties file passed in as the second argument.
	 * 
	 * @param args
	 */
	protected static void initializeLogging(String[] args) {
		if (args.length >= 2) {
			Properties props = new Properties();
			try {
				System.out.println("setting up logging props using file: " + args[1]);
				props.load(new FileInputStream(args[1]));
				LoggingManager.initializeLogging(props);
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
	}

	/**
	 * An overridable method that that instantiates a UnitTestManager (if one
	 * was specified in the command-line arguments), and hands it the name of
	 * the properties file to use to configure the system.
	 * 
	 * @param args
	 */
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

}
