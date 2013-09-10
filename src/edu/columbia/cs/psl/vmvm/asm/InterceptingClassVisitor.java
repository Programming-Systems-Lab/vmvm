package edu.columbia.cs.psl.vmvm.asm;

import java.util.List;
import java.util.Map.Entry;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.FieldNode;

import edu.columbia.cs.psl.vmvm.Constants;
import edu.columbia.cs.psl.vmvm.Instrumenter;
public class InterceptingClassVisitor extends ClassVisitor implements Opcodes, Constants {
	
	private String className;

	private boolean isAClass = true;
	
	private boolean runIMV = true;
	
	private boolean willRewrite = false;
	
	
	public InterceptingClassVisitor(ClassVisitor cv) {
		super(Opcodes.ASM4, cv);
	}
	
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
		if((access & Opcodes.ACC_INTERFACE) != 0)
			isAClass = false;
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if(desc.length() > 1)
			super.visitField(Opcodes.ACC_PUBLIC, name+BEEN_CLONED_FIELD, Type.BOOLEAN_TYPE.getDescriptor(), null, false);
		
		if((access & Opcodes.ACC_STATIC) != 0) //Static field
		{
			for(int i = 1; i <= Instrumenter.MAX_SANDBOXES; i++)
				super.visitField(access, name+SANDBOX_SUFFIX+i, desc,signature, value);

		}
		return super.visitField(access, name, desc, signature, value);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

		return super.visitAnnotation(desc, visible);
	}
	
	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc,
			String signature, String[] exceptions) {
		if(name.equals("<init>") || (((acc & Opcodes.ACC_STATIC) != 0) && !name.equals("<clinit>") && !(name.equals("main")  && desc.equals("([Ljava/lang/String;)V"))))
		{
			//Need to call the modified method - which takes a short arg; the current sandbox state
			Type[] args = Type.getArgumentTypes(desc);
			Type[] descTypes = new Type[args.length + 1];
			System.arraycopy(args, 0,descTypes, 0, args.length);
			descTypes[args.length] = Type.INT_TYPE;
			desc = Type.getMethodDescriptor(Type.getReturnType(desc), descTypes);
		}
		
		if(isAClass)//runIMV && 
		{
			MethodVisitor mv = cv.visitMethod(acc, name, desc, signature,
					exceptions);
			LocalVariablesSorter lvs = new LocalVariablesSorter(acc, desc, mv);
			InvivoAdapter invivoAdapter = new InvivoAdapter(Opcodes.ASM4, lvs, acc, name, desc, className,lvs);
			SandboxPropogatingMV sandboxer = new SandboxPropogatingMV(Opcodes.ASM4, invivoAdapter, acc, name, desc, className,invivoAdapter);
			StaticFieldIsolatorMV staticIsolator  = new StaticFieldIsolatorMV(Opcodes.ASM4, sandboxer, acc, name, desc, invivoAdapter);
//			LazyCloneInterceptingMethodVisitor cloningIMV = new LazyCloneInterceptingMethodVisitor(Opcodes.ASM4, mv, acc, name, desc);
//			InterceptingMethodVisitor imv = new InterceptingMethodVisitor(Opcodes.ASM4, cloningIMV, acc, name, desc);
//			imv.setClassName(className);
//			imv.setClassVisitor(this);
			
			return staticIsolator;
		}
		else
			return 	cv.visitMethod(acc, name, desc, signature,
					exceptions);
	}

	
	//Default to true to make it work for all classes
	public void setShouldRewrite()
	{
		willRewrite = true;
	}
	
	@Override
	public void visitEnd() {
		super.visitEnd();
		
		if(isAClass)
		{
			FieldNode fn2 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL,
					CHILD_FIELD,
					Type.INT_TYPE.getDescriptor(), null, null); 
			fn2.accept(cv);
		}
	}

	
	public String getClassName() {
		return this.className;
	}

}
