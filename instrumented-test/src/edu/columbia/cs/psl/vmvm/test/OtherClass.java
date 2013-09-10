package edu.columbia.cs.psl.vmvm.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class OtherClass {
//	private static int foo = 3;
	static
	{
//		int x = 3;
		inst = new OtherClass();
	}
	private static OtherClass inst = new OtherClass();
	public void go() {
		System.out.println("I did go");
	}

	public static void foo(int a)
	{
		String p = "foo";
	}
	@Override
		public String toString() {

			String r = "[OtherClass ";
			try{
			Field f = OtherClass.class.getDeclaredField("_invivo_cloned");
			if(f !=null)
				r += " sandboxId=" + f.getInt(this);
			else
				r += " sandboxId = <NO SANDBOX ENV>";
			r+="]";
			}
			catch(Exception ex)
			{
				
			}
			return r;
		}
	public static void main(String[] args) {
		OtherClass sc = new OtherClass();
		sc.go();
		
		System.out.println("Scanning fields");
		System.out.println(OtherClass.class.getDeclaredFields().length);
		for(Field f : OtherClass.class.getDeclaredFields())
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
