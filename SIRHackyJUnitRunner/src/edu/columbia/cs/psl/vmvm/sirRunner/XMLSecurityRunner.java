package edu.columbia.cs.psl.vmvm.sirRunner;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;


public class XMLSecurityRunner {
	public static void main(String[] args) {
		String[] _args = new String[args.length-2];
		System.arraycopy(args, 0, _args, 0, args.length-2);
		try {
			ArrayList<String> tests =new ArrayList<>();
			if(args[args.length - 2].equals("v0")){
				tests.add("org.apache.xml.security.test.c14n.helper.AttrCompareTest");
				tests.add("org.apache.xml.security.test.c14n.helper.C14nHelperTest");
				tests.add("org.apache.xml.security.test.c14n.helper.NamespaceSearcherTest");
				tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315Test");
				tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315WithoutXPathSupportTest");
				tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.XalanBug1425Test");
				tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.AttributeAncestorOrSelf");
				tests.add("org.apache.xml.security.test.signature.XMLSignatureInputTest");
				tests.add("org.apache.xml.security.test.transforms.implementations.TransformBase64DecodeTest");
				tests.add("org.apache.xml.security.test.utils.resolver.ResourceResolverSpiTest");
				tests.add("org.apache.xml.security.test.utils.Base64Test");
				tests.add("org.apache.xml.security.test.algorithms.implementations.KeyWrapTest");
				tests.add("org.apache.xml.security.test.algorithms.implementations.BlockEncryptionTest");
				tests.add("org.apache.xml.security.test.interop.BaltimoreTest");
				tests.add("org.apache.xml.security.test.interop.IAIKTest");
				tests.add("org.apache.xml.security.test.interop.RSASecurityTest");
			}else if(args[args.length - 2].equals("v1")){
				tests.add("org.apache.xml.security.test.c14n.helper.C14nHelperTest");
				tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315Test");
				tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315ExclusiveTest");
				tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.XalanBug1425Test");
				tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.AttributeAncestorOrSelf");
				tests.add("org.apache.xml.security.test.signature.XMLSignatureInputTest");
				tests.add("org.apache.xml.security.test.transforms.implementations.TransformBase64DecodeTest");
				tests.add("org.apache.xml.security.test.utils.resolver.ResourceResolverSpiTest");
				tests.add("org.apache.xml.security.test.utils.Base64Test");
				tests.add("org.apache.xml.security.test.algorithms.implementations.KeyWrapTest");
				tests.add("org.apache.xml.security.test.algorithms.implementations.BlockEncryptionTest");
				tests.add("org.apache.xml.security.test.interop.BaltimoreTest");
				tests.add("org.apache.xml.security.test.interop.IAIKTest");
				tests.add("org.apache.xml.security.test.interop.RSASecurityTest");
				tests.add("org.apache.xml.security.test.c14n.implementations.ExclusiveC14NInterop");
			}else if(args[args.length - 2].equals("v2")){
				tests.add("org.apache.xml.security.test.c14n.helper.C14nHelperTest");
				tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315Test");
				tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315ExclusiveTest");
				tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.XalanBug1425Test");
				tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.AttributeAncestorOrSelf");
				tests.add("org.apache.xml.security.test.signature.XMLSignatureInputTest");
				tests.add("org.apache.xml.security.test.transforms.implementations.TransformBase64DecodeTest");
				tests.add("org.apache.xml.security.test.utils.resolver.ResourceResolverSpiTest");
				tests.add("org.apache.xml.security.test.utils.Base64Test");
				tests.add("org.apache.xml.security.test.algorithms.implementations.KeyWrapTest");
				tests.add("org.apache.xml.security.test.algorithms.implementations.BlockEncryptionTest");
				tests.add("org.apache.xml.security.test.interop.BaltimoreTest");
				tests.add("org.apache.xml.security.test.interop.IAIKTest");
				tests.add("org.apache.xml.security.test.interop.RSASecurityTest");
				tests.add("org.apache.xml.security.test.c14n.implementations.ExclusiveC14NInterop");
			}else if(args[args.length - 2].equals("v3")){
				tests.add("org.apache.xml.security.test.c14n.helper.C14nHelperTest");
				tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315Test");
				tests.add("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315ExclusiveTest");
				tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.XalanBug1425Test");
				tests.add("org.apache.xml.security.test.external.org.apache.xalan.XPathAPI.AttributeAncestorOrSelf");
				tests.add("org.apache.xml.security.test.signature.XMLSignatureInputTest");
				tests.add("org.apache.xml.security.test.transforms.implementations.TransformBase64DecodeTest");
				tests.add("org.apache.xml.security.test.utils.resolver.ResourceResolverSpiTest");
				tests.add("org.apache.xml.security.test.utils.Base64Test");
				tests.add("org.apache.xml.security.test.interop.BaltimoreTest");
				tests.add("org.apache.xml.security.test.interop.IAIKTest");
				tests.add("org.apache.xml.security.test.interop.RSASecurityTest");
				tests.add("org.apache.xml.security.test.c14n.implementations.ExclusiveC14NInterop");
			}

 			SuiteWrapper.runSuite(tests, "xml-security " + args[args.length-2], args[args.length-1],false,"");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}
}
