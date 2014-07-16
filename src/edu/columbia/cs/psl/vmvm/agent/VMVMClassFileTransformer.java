package edu.columbia.cs.psl.vmvm.agent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;
import edu.columbia.cs.psl.vmvm.Instrumenter.InstrumentResult;
import edu.columbia.cs.psl.vmvm.VMVMInstrumented;
import edu.columbia.cs.psl.vmvm.asm.InterceptingClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.JUnitResettingClassVisitor;
import edu.columbia.cs.psl.vmvm.asm.struct.MethodListClassNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.ClassReader;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.AnnotationNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.ClassNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.FieldNode;

@SuppressWarnings("unchecked")
public class VMVMClassFileTransformer extends SecureClassLoader implements ClassFileTransformer, Opcodes{
//	private static final Logger logger = Logger.getLogger(VMVMClassFileTransformer.class);
	public static final boolean NO_RUNTIME_INST= true;
	static{
		try
		{
//			ClassNode cn = new ClassNode();
			ObjectInputStream ois = new ObjectInputStream(VMVMClassFileTransformer.getSystemResourceAsStream("vmvm-runtimecheat"));
			Instrumenter.instrumentedClasses = (HashMap<String, MethodListClassNode>) ois.readObject();
			Instrumenter.remappedInterfaces = (HashSet<String>) ois.readObject();
			Instrumenter.finalClasses = (HashSet<String>)ois.readObject();
			Instrumenter.finalMethods= (HashSet<String>)ois.readObject();
			Instrumenter.finalFields= (HashSet<String>)ois.readObject();

//			if(Instrumenter.instrumentedClasses.containsKey("os$py"))
//			{
////				Instrumenter.instrumentedClasses.remove("os$py");
////				System.out.println(Instrumenter.instrumentedClasses.get("os$py"));
//				System.exit(-1);
//			}
			ois.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		return handleTransform(classfileBuffer);
	}
	private static byte[] handleTransform(byte[] classfileBuffer)
	{
		if(NO_RUNTIME_INST)
		return classfileBuffer;
		
		if(classfileBuffer == null)
			return null;
		
		ClassReader cr = new ClassReader(classfileBuffer);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		
		if(JUnitResettingClassVisitor.shouldIgnoreClass(cn.name) || (cn.access & Opcodes.ACC_ANNOTATION) != 0)
		{
//			System.out.println("Ignoring " + cn.name);
			return classfileBuffer;
		}
		
		if(cn.fields != null)
			for(Object o : cn.fields)
			{
				FieldNode an = (FieldNode) o;
				if(an.name.equals(Constants.VMVM_NEEDS_RESET))
				{
					return classfileBuffer;
				}
			}
//		if(Instrumenter.instrumentedClasses.containsKey(cn.name))
//			System.out.println("Runtime inst: " + cn.name);

		Instrumenter.analyzeClass(cn.name,new ClassReader(classfileBuffer), Instrumenter.instrumentedClasses);
		InstrumentResult out = Instrumenter.instrumentClass(cn.name,new ByteArrayInputStream(classfileBuffer),false);
		try{
			File debugFolder =new File("debug");
			debugFolder.mkdir();
			FileOutputStream fos = new FileOutputStream("debug/"+cn.name.replace("/", ".")+".class");
			fos.write(out.clazz);
			fos.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		if(out == null)
		{
			return null;
		}
		return out.clazz; //Don't support lazy interface duplication here
	}
	public VMVMClassFileTransformer()
	{
		
	}
	public VMVMClassFileTransformer(ClassLoader parent)
	{
		super(parent);
	}
	private static HashMap<ClassLoader, VMVMClassFileTransformer> instances =  new HashMap<>();
	public static Class<?> defineClass(ClassLoader l, byte[] b, int off, int len)
	{
		if(!instances.containsKey(l))
			instances.put(l, new VMVMClassFileTransformer(l));
		return instances.get(l).defineClass(b, off, len,true);
	}
	private Class<?> defineClass(byte[] b, int off, int len, boolean fake)
	{
		return super.defineClass(b, off, len);
	}
	
	public static Class<?> defineClass(ClassLoader l, String name, byte[] b, int off, int len)
	{
		if(!instances.containsKey(l))
			instances.put(l, new VMVMClassFileTransformer(l));
		return instances.get(l).defineClass( name, b, off, len,true);
	}
	private Class<?> defineClass(String name, byte[] b, int off, int len, boolean fake)
	{
		return super.defineClass(name, b, off, len);
	}
	public static Class<?> defineClass(ClassLoader l, String name, byte[] b, int off, int len, ProtectionDomain domain)
	{
		if(!instances.containsKey(l))
			instances.put(l, new VMVMClassFileTransformer(l));

		return instances.get(l).defineClass( name, b, off, len,domain, true);
	}
	private Class<?> defineClass(String name, byte[] b, int off, int len, ProtectionDomain domain, boolean fake)
	{
//		System.out.println("Got the fake load");
		if(len != b.length)
		{
			byte[] c = new byte[len];
			System.arraycopy(b, off, c, 0, len);
			b = handleTransform(c);
		}
		else
			b = handleTransform(b);
		return super.defineClass(name, b, off, b.length, domain);
		
	}
	public static Class<?> defineClass(ClassLoader l,String name, ByteBuffer b, ProtectionDomain domain)
	{
		if(!instances.containsKey(l))
			instances.put(l, new VMVMClassFileTransformer(l));
		return instances.get(l).defineClass( name, b,domain, true);
	}
	private Class<?> defineClass(String name, ByteBuffer b, ProtectionDomain domain, boolean fake)
	{
		return super.defineClass(name, b, domain);
	}
	
	//(Ljava/lang/ClassLoader;Ljava/lang/String;[BIILjava/security/CodeSource;)Ljava/lang/Class;
	public static Class<?> defineClass(ClassLoader l, String name, byte[] b, int off, int len, CodeSource codesource)
	{
		if(!instances.containsKey(l))
			instances.put(l, new VMVMClassFileTransformer(l));
		return instances.get(l).defineClass( name, b, off, len,codesource, true);
	}
	private Class<?> defineClass(String name, byte[] b, int off, int len, CodeSource codesource, boolean fake) {
		return super.defineClass(name, b, off, len, codesource);
	}
	public static void ensureInit() {
		// TODO Auto-generated method stub
		
	}
}
