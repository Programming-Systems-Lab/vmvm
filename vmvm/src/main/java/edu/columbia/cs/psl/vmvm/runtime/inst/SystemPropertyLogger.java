package edu.columbia.cs.psl.vmvm.runtime.inst;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Scanner;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class SystemPropertyLogger extends InstructionAdapter {

	
	public SystemPropertyLogger(final MethodVisitor mv) {
		super(Opcodes.ASM5,mv);
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
		Scanner s = new Scanner(Reinitializer.class.getResourceAsStream("internal-statics"));

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
		s.close();
	}
	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc, boolean itfc) {
		if(owner.equals(Type.getInternalName(Runtime.class)) && name.equals("addShutdownHook"))
		{
			owner = Reinitializer.INTERNAL_NAME;
			desc = "("+Type.getDescriptor(Runtime.class)+Type.getDescriptor(Thread.class)+")V";
			opcode = Opcodes.INVOKESTATIC;
		}
		if((owner.equals("java/lang/System") && name.equals("setProperty") && desc.equals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
				|| (owner.equals("java/lang/System") && name.equals("setProperties") && desc.equals("(Ljava/util/Properties;)V")))
		{
			owner = Reinitializer.INTERNAL_NAME;
			name = "logAndSetProperty";
		}
		if((owner.equals("java/lang/System") && name.equals("getProperty")))
		{
			owner = Reinitializer.INTERNAL_NAME;
			name = "logAndGetProperty";
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
					if( name.equals("setIn")) //....
						super.visitFieldInsn(Opcodes.GETSTATIC, owner, "in", args[0].getDescriptor());
					else
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

				super.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "logStaticInternal", "(Ljava/lang/Object;I)V", false);
			}
			else if(clazz.addMethods.containsKey(name))
			{
				super.visitInsn(Opcodes.DUP);
				//Do the log
				//box if necessary
				box(args[0]);
				super.visitIntInsn(Opcodes.BIPUSH, clazz.addMethods.get(name));

				super.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "logStaticInternalAdd", "(Ljava/lang/Object;I)V", false);
			}
			else if(clazz.removeMethods.containsKey(name))
			{
				super.visitInsn(Opcodes.DUP);
				//Do the log
				//box if necessary
				box(args[0]);
				super.visitIntInsn(Opcodes.BIPUSH, clazz.removeMethods.get(name));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Reinitializer.INTERNAL_NAME, "logStaticInternalRemove", "(Ljava/lang/Object;I)V", false);
			}
		}
		super.visitMethodInsn(opcode, owner, name, desc, itfc);
	}
	   public void box(final Type type) {
	        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
	            return;
	        }

	            Type boxed = getBoxedType(type);
	            super.visitTypeInsn(Opcodes.NEW	, boxed.getInternalName());

	            if (type.getSize() == 2) {
	                // Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
	                dupX2();
	                dupX2();
	                pop();
	            } else {
	                // p -> po -> opo -> oop -> o
	                dupX1();
	                swap();
	            }
	            super.visitMethodInsn(Opcodes.INVOKESPECIAL, boxed.getInternalName(), "<init>", "("+type.getDescriptor()+")V", false);
	        
	    }
	    private static Type getBoxedType(final Type type) {
	        switch (type.getSort()) {
	        case Type.BYTE:
	            return BYTE_TYPE;
	        case Type.BOOLEAN:
	            return BOOLEAN_TYPE;
	        case Type.SHORT:
	            return SHORT_TYPE;
	        case Type.CHAR:
	            return CHARACTER_TYPE;
	        case Type.INT:
	            return INTEGER_TYPE;
	        case Type.FLOAT:
	            return FLOAT_TYPE;
	        case Type.LONG:
	            return LONG_TYPE;
	        case Type.DOUBLE:
	            return DOUBLE_TYPE;
	        }
	        return type;
	    }
	    private static final Type BYTE_TYPE = Type.getObjectType("java/lang/Byte");

	    private static final Type BOOLEAN_TYPE = Type
	            .getObjectType("java/lang/Boolean");

	    private static final Type SHORT_TYPE = Type
	            .getObjectType("java/lang/Short");

	    private static final Type CHARACTER_TYPE = Type
	            .getObjectType("java/lang/Character");

	    private static final Type INTEGER_TYPE = Type
	            .getObjectType("java/lang/Integer");

	    private static final Type FLOAT_TYPE = Type
	            .getObjectType("java/lang/Float");

	    private static final Type LONG_TYPE = Type.getObjectType("java/lang/Long");

	    private static final Type DOUBLE_TYPE = Type
	            .getObjectType("java/lang/Double");

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
