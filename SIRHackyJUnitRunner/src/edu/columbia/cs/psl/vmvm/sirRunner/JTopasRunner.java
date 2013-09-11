package edu.columbia.cs.psl.vmvm.sirRunner;

import java.util.ArrayList;

import de.susebox.java.util.TestDifficultSituations;
import de.susebox.java.util.TestEmbeddedTokenizer;
import de.susebox.java.util.TestInputStreamTokenizer;
import de.susebox.java.util.TestTokenProperties;
import de.susebox.java.util.TestTokenizerProperties;
import de.susebox.jtopas.TestPluginTokenizer;
import de.susebox.jtopas.TestTokenizerSpeed;
import junit.framework.TestSuite;

public class JTopasRunner {
	public static void main(String[] args) {
		ArrayList<String> tests = new ArrayList<>();
		if(args[args.length - 2].equals("v0")){
			tests.add("de.susebox.java.util.TestTokenizerProperties");
			tests.add("de.susebox.java.util.TestTokenProperties");
			tests.add("de.susebox.java.util.TestInputStreamTokenizer");
			tests.add("de.susebox.java.util.TestDifficultSituations");
			tests.add("de.susebox.java.util.TestEmbeddedTokenizer");
			tests.add("de.susebox.jtopas.TestPluginTokenizer");
			tests.add("de.susebox.jtopas.TestTokenizerSpeed");
		}else if(args[args.length - 2].equals("v1")){
			tests.add("de.susebox.java.util.TestTokenizerProperties");
			tests.add("de.susebox.java.util.TestTokenProperties");
			tests.add("de.susebox.java.util.TestInputStreamTokenizer");
			tests.add("de.susebox.java.util.TestDifficultSituations");
			tests.add("de.susebox.java.util.TestEmbeddedTokenizer");
			tests.add("de.susebox.java.lang.TestExceptionList");
			tests.add("de.susebox.jtopas.TestPluginTokenizer");
			tests.add("de.susebox.jtopas.TestTokenizerSpeed");
			tests.add("de.susebox.jtopas.TestJavaTokenizing");
			tests.add("de.susebox.TestExceptions");
		}else if(args[args.length - 2].equals("v2")){
			tests.add("de.susebox.java.util.TestTokenizerProperties");
			tests.add("de.susebox.java.util.TestTokenProperties");
			tests.add("de.susebox.java.util.TestInputStreamTokenizer");
			tests.add("de.susebox.java.util.TestTextAccess");
			tests.add("de.susebox.java.util.TestDifficultSituations");
			tests.add("de.susebox.java.util.TestEmbeddedTokenizer");
			tests.add("de.susebox.java.lang.TestExceptionList");
			tests.add("de.susebox.jtopas.TestPluginTokenizer");
			tests.add("de.susebox.jtopas.TestTokenizerSpeed");
			tests.add("de.susebox.jtopas.TestJavaTokenizing");
			tests.add("de.susebox.TestExceptions");
		}else if(args[args.length - 2].equals("v3")){
			tests.add("de.susebox.java.util.TestTokenizerProperties");
			tests.add("de.susebox.java.util.TestTokenProperties");
			tests.add("de.susebox.java.util.TestInputStreamTokenizer");
			tests.add("de.susebox.java.util.TestTextAccess");
			tests.add("de.susebox.java.util.TestDifficultSituations");
			tests.add("de.susebox.java.util.TestEmbeddedTokenizer");
			tests.add("de.susebox.java.lang.TestExceptionList");
			tests.add("de.susebox.jtopas.TestPluginTokenizer");
			tests.add("de.susebox.jtopas.TestTokenizerSpeed");
			tests.add("de.susebox.jtopas.TestJavaTokenizing");
			tests.add("de.susebox.jtopas.TestTokenizerProperties");
			tests.add("de.susebox.jtopas.TestMultithreadTokenizerProperties");
			tests.add("de.susebox.jtopas.TestDifficultSituations");
			tests.add("de.susebox.jtopas.TestStandardTokenizer");
			tests.add("de.susebox.jtopas.TestEmbeddedTokenizer");
			tests.add("de.susebox.jtopas.TestTextAccess");
			tests.add("de.susebox.jtopas.TestMultithreadTokenizer");
			tests.add("de.susebox.TestExceptions");
		}
		SuiteWrapper.runSuite(tests, "jtopas " + args[args.length - 2], args[args.length - 1],true,"");
	}
}
