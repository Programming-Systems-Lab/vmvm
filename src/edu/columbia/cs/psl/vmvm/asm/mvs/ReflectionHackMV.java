package edu.columbia.cs.psl.vmvm.asm.mvs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;


import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.FieldReflectionWrapper;
import edu.columbia.cs.psl.vmvm.ReflectionWrapper;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;
import edu.columbia.cs.psl.vmvm.asm.VMVMClassVisitor;

public class ReflectionHackMV extends InstructionAdapter implements Opcodes {

	private VMVMClassVisitor cv;
    public ReflectionHackMV(int api, MethodVisitor mv, VMVMClassVisitor cv) {
        super(api, mv);
        this.cv = cv;
    }
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {

        if(Type.getInternalName(Class.class).equals(owner) && name.equals("getConstructors"))
        {
            super.visitMethodInsn(opcode, owner, name, desc);
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(VirtualRuntime.class), "hideVMVMConstructors", "([Ljava/lang/reflect/Constructor;)[Ljava/lang/reflect/Constructor;");
        }
        else if(Type.getInternalName(Class.class).equals(owner) && name.equals("newInstance"))
        {
//        	owner = Type.getInternalName(ReflectionWrapper.class);
//        	opcode = INVOKESTATIC;
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), "preNewInstance", "(Ljava/lang/Class;)Ljava/lang/Class;");
            super.visitMethodInsn(opcode, owner, name, desc);
        }
        else if((Type.getInternalName(Class.class).equals(owner) && name.equals("forName")))        		
        {
        	owner = Type.getInternalName(ReflectionWrapper.class);
        	if(desc.contains("ClassLoader"))
        		super.visitMethodInsn(opcode, owner, name, desc);
        	else
        	{
        		if (cv.getVersion() > 48 && cv.getVersion() < 1000)// java 5+
        			super.visitLdcInsn(Type.getType("L" + cv.getClassName() + ";"));
        		else {
        			super.visitLdcInsn(cv.getClassName().replace("/", "."));
        			super.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        		}
        	    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");
        		super.visitMethodInsn(opcode, owner, name, desc.substring(0,desc.indexOf(")")) + Type.getDescriptor(ClassLoader.class)+ desc.substring(desc.indexOf(")")));
        	}
        }
        else if((Type.getInternalName(Class.class).equals(owner) && (name.equals("getDeclaredFields") || name.equals("getDeclaredMethods") || name.equals("getFields") || name.equals("getMethods"))))        		
        {
        	desc = "("+Type.getDescriptor(Class.class)+")"+Type.getReturnType(desc).getDescriptor();
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReflectionWrapper.class), name,desc);
        }
        else if((Type.getInternalName(Method.class).equals(owner) && name.equals("invoke")))        		
        {
        	owner = Type.getInternalName(ReflectionWrapper.class);
        	opcode = INVOKESTATIC;

            super.visitMethodInsn(opcode, owner, name, "(Ljava/lang/reflect/Method;"+desc.substring(1));
        }
        else if((Type.getInternalName(Method.class).equals(owner) && name.equals("getModifiers")))        		
        {
        	owner = Type.getInternalName(ReflectionWrapper.class);
        	opcode = INVOKESTATIC;

            super.visitMethodInsn(opcode, owner, name, "(Ljava/lang/reflect/Method;"+desc.substring(1));
        }
        else if((Type.getInternalName(Class.class).equals(owner) && name.equals("getModifiers")))        		
        {
        	owner = Type.getInternalName(ReflectionWrapper.class);
        	opcode = INVOKESTATIC;

            super.visitMethodInsn(opcode, owner, name, "(Ljava/lang/Class;"+desc.substring(1));
        }
        else if((Type.getInternalName(Field.class).equals(owner) && name.equals("getModifiers")))        		
        {
        	owner = Type.getInternalName(ReflectionWrapper.class);
        	opcode = INVOKESTATIC;

            super.visitMethodInsn(opcode, owner, name, "(Ljava/lang/reflect/Field;"+desc.substring(1));
        }
        else if((Type.getInternalName(Field.class).equals(owner) && (
        		name.equals("getBoolean") || 
        		name.equals("getByte") || 
        		name.equals("getChar") || 
        		name.equals("getDouble") || 
        		name.equals("getFloat") || 
        		name.equals("getInt") || 
        		name.equals("getLong") || 
        		name.equals("get")
        		)))
        {
        	super.visitInsn(DUP_X1);
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(FieldReflectionWrapper.class), "tryToInit", "(Ljava/lang/reflect/Field;Ljava/lang/Object;)Ljava/lang/reflect/Field;");
            super.visitInsn(SWAP);
            super.visitMethodInsn(opcode, owner, name, desc);
        }
        else if(Type.getInternalName(Field.class).equals(owner) && (
        		name.equals("setBoolean") || 
        		name.equals("setByte") || 
        		name.equals("setChar") || 
        		name.equals("setDouble") || 
        		name.equals("setFloat") || 
        		name.equals("setInt") || 
        		name.equals("setLong") || 
        		name.equals("set") ))
        {
        	Type[] args = Type.getArgumentTypes(desc);
        	String d2 = "(";
        	for(Type t : args)
        	{
        		if(t.getSize()<2)
        			d2 +=t.getDescriptor();
        	}
        	if(args.length == 2 && args[1].getSize() ==2)
        	{
        		super.visitInsn(DUP2_X2);
            	super.visitInsn(POP2);
            	super.visitInsn(SWAP);
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(FieldReflectionWrapper.class), "tryToInit", "(Ljava/lang/reflect/Field;)Ljava/lang/reflect/Field;");
                super.visitInsn(SWAP);
                super.visitInsn(DUP2_X2);
                super.visitInsn(POP2);
                super.visitMethodInsn(opcode, owner, name, desc);
        	}
        	else
        	{
            	super.visitInsn(DUP2_X1);
            	super.visitInsn(POP2);
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(FieldReflectionWrapper.class), "tryToInit", "(Ljava/lang/reflect/Field;)Ljava/lang/reflect/Field;");
                super.visitInsn(DUP_X2);
                super.visitInsn(POP);
                super.visitMethodInsn(opcode, owner, name, desc);

        	}    
        }
        else
        {
            super.visitMethodInsn(opcode, owner, name, desc);
        }
        //XXX TODO also need to handle invokestatic, getstatic, putstatic
    }
}
