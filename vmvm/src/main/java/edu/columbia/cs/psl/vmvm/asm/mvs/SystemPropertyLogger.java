package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Scanner;

import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Type;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.commons.GeneratorAdapter;

public class SystemPropertyLogger extends GeneratorAdapter {

	
	public SystemPropertyLogger(final int api, final MethodVisitor mv,
            final int access, final String name, final String desc) {
		super(api,mv,access,name,desc);
	}
	public static void main(String[] args)
	{
		for(String clazz : internalStatics.keySet())
			{
			InternalStaticClass c = internalStatics.get(clazz);
				for(String s : c.setMethods.keySet())
				{
					String fullSetMethod = clazz.replace("/", ".")+"."+s;
					String castType = "";
					try {
						Class cl = Class.forName(clazz.replace("/", "."));
						for(Method m : cl.getMethods())
						{
							if(m.getName().equals(s) && m.getParameterTypes().length == 1)
							{
								castType = m.getParameterTypes()[0].getCanonicalName();
							}
						}
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("if(logsUsed["+c.setMethods.get(s)+"]){try{");
					System.out.println(fullSetMethod+"(("+castType+")loggedValues["+c.setMethods.get(s)+"]);}catch(Exception ex){ex.printStackTrace();}logsUsed["+c.setMethods.get(s)+"]=false;loggedValues["+c.setMethods.get(s)+"]=null;");
					System.out.println("}");
				}
				
//				for(String s : c.addMethods.keySet())
//				{
//					String fullSetMethod = clazz.replace("/", ".")+"."+s.replace("add", "remove");
//					String castType = "";
//					try {
//						Class cl = Class.forName(clazz.replace("/", "."));
//						for(Method m : cl.getMethods())
//						{
//							if(m.getName().equals(s) && m.getParameterTypes().length == 1)
//							{
//								castType = m.getParameterTypes()[0].getCanonicalName();
//							}
//						}
//					} catch (ClassNotFoundException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					System.out.println("if(logsUsed["+c.addMethods.get(s)+"]){");
//					System.out.println("for(WeakReference<Object> o : (LinkedList<WeakReference<Object>>) loggedValues["+c.addMethods.get(s)+"])");
//					System.out.println("if(!o.isEnqueued()) "+fullSetMethod+"(("+castType+")o.get());");
//					System.out.println("loggedValues["+c.addMethods.get(s)+"]=null;logsUsed["+c.addMethods.get(s)+"]=false;}");
//				}
//				
//				
//				for(String s : c.removeMethods.keySet())
//				{
//					String fullSetMethod = clazz.replace("/", ".")+"."+s.replace("remove", "add");
//					String castType = "";
//					try {
//						Class cl = Class.forName(clazz.replace("/", "."));
//						for(Method m : cl.getMethods())
//						{
//							if(m.getName().equals(s) && m.getParameterTypes().length == 1)
//							{
//								castType = m.getParameterTypes()[0].getCanonicalName();
//							}
//						}
//					} catch (ClassNotFoundException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					System.out.println("if(logsUsed["+c.removeMethods.get(s)+"]){");
//					System.out.println("for(Object o : (LinkedList<Object>) loggedValues["+c.removeMethods.get(s)+"])");
//					System.out.println(fullSetMethod+"(("+castType+")o);");
//					System.out.println("loggedValues["+c.removeMethods.get(s)+"]=null;logsUsed["+c.removeMethods.get(s)+"]=false;}");
//				}
			}
	}

	public static HashMap<String, InternalStaticClass> internalStatics = new HashMap<>();
	static {
		int n = 0;
		Scanner s = new Scanner(VirtualRuntime.class.getResourceAsStream("internal-statics"));
		while (s.hasNextLine()) {
			String l = s.nextLine();
			String[] d = l.split("\t");
			String name = d[0];
			InternalStaticClass c = new InternalStaticClass();
			for (String z : d[1].split(",")) {
				if (z.length() > 0)
					c.addMethods.put(z, ++n);
			}
			for (String z : d[2].split(","))
				if (z.length() > 0)
					c.removeMethods.put(z, ++n);
			for (String z : d[3].split(","))
				if (z.length() > 0)
					c.setMethods.put(z, ++n);
			for (String z : d[4].split(","))
				if (z.length() > 0)
					c.getMethods.put(z, -1);
			internalStatics.put(name, c);
		}
	}
	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc, boolean itfc) {
		if(owner.equals(Type.getInternalName(Runtime.class)) && name.equals("addShutdownHook"))
		{
			owner = Type.getInternalName(VirtualRuntime.class);
			desc = "("+Type.getDescriptor(Runtime.class)+Type.getDescriptor(Thread.class)+")V";
			opcode = Opcodes.INVOKESTATIC;
		}
		if((owner.equals("java/lang/System") && name.equals("setProperty") && desc.equals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
				|| (owner.equals("java/lang/System") && name.equals("setProperties") && desc.equals("(Ljava/util/Properties;)V")))
		{
			owner = Type.getInternalName(VirtualRuntime.class);
			name = "logAndSetProperty";
		}
		else if(internalStatics.containsKey(owner))
		{
			Type[] args =Type.getArgumentTypes(desc);
			InternalStaticClass clazz = internalStatics.get(owner);
			if(clazz.setMethods.keySet().contains(name) && args.length == 1)
			{
				//insert a fake get
				String newName = name.replace("set", "get");
				if(owner.equals("javax/swing/JDialog") || owner.equals("javax/swing/JFrame"))
					newName = name.replace("set", "is");
				if(owner.equals("java/lang/System") && (name.equals("setOut") || name.equals("setErr") || name.equals("setIn")))
				{				
					super.visitFieldInsn(Opcodes.GETSTATIC, owner, name.replace("set", "").toLowerCase(), args[0].getDescriptor());
				}
				else if(owner.equals("java/net/Authenticator") && name.equals("setDefault"))
				{
					super.visitInsn(Opcodes.ACONST_NULL);
				}
				else
				{
					super.visitMethodInsn(opcode, owner, newName, Type.getMethodDescriptor(args[0]), false);
					box(args[0]);
				}
				//Do the log
				//box if necessary
				super.visitIntInsn(Opcodes.BIPUSH, clazz.setMethods.get(name));

				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "logStaticInternal", "(Ljava/lang/Object;I)V", false);
			}
			else if(clazz.addMethods.containsKey(name))
			{
				super.visitInsn(Opcodes.DUP);
				//Do the log
				//box if necessary
				box(args[0]);
				super.visitIntInsn(Opcodes.BIPUSH, clazz.addMethods.get(name));

				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "logStaticInternalAdd", "(Ljava/lang/Object;I)V", false);
			}
			else if(clazz.removeMethods.containsKey(name))
			{
				super.visitInsn(Opcodes.DUP);
				//Do the log
				//box if necessary
				box(args[0]);
				super.visitIntInsn(Opcodes.BIPUSH, clazz.removeMethods.get(name));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "logStaticInternalRemove", "(Ljava/lang/Object;I)V", false);
			}
		}
		super.visitMethodInsn(opcode, owner, name, desc, itfc);
	}

	public static class InternalStaticClass {
		public HashMap<String, Integer> getMethods = new HashMap<>();
		public HashMap<String, Integer> setMethods = new HashMap<>();
		public HashMap<String, Integer> addMethods = new HashMap<>();
		public HashMap<String, Integer> removeMethods = new HashMap<>();

		@Override
		public String toString() {
			return "InternalStaticClass [getMethods=" + getMethods + ", setMethods=" + setMethods + ", addMethods=" + addMethods + ", removeMethods=" + removeMethods + "]";
		}

	}
}
