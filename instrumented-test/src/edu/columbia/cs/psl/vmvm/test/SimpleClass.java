package edu.columbia.cs.psl.vmvm.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SimpleClass {
//	private static int foo = 3;
	static
	{
//		int x = 3;
		inst = new SimpleClass();
	}
	private static SimpleClass inst = new SimpleClass();
	public void go() {
		System.out.println("I did go");
	}
	OtherClass child = new OtherClass();

	public static void foo(int a)
	{
		String p = "foo";
	}
	@Override
		public String toString() {

			String r = "[SimpleClass ";
			try{
			Field f = SimpleClass.class.getDeclaredField("_invivo_cloned");
			if(f !=null)
				r += " sandboxId=" + f.getInt(this);
			else
				r += " sandboxId = <NO SANDBOX ENV>";
			r += "[child = " + child.toString()+"]";
			r+="]";
			}
			catch(Exception ex)
			{
				
			}
			return r;
		}
	public static void main(String[] args) {
		SimpleClass sc = new SimpleClass();
		sc.go();
		
		System.out.println("Scanning fields");
		System.out.println(SimpleClass.class.getDeclaredFields().length);
		for(Field f : SimpleClass.class.getDeclaredFields())
		{
//			System.out.println(f);
			if(Modifier.isStatic(f.getModifiers()))
			try {
				System.out.println(f.getName() + " -> " + f.get(sc));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
