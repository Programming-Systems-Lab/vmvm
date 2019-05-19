package edu.columbia.cs.psl.test.vmvm;

import static org.junit.Assert.assertSame;

import org.junit.Test;

import edu.columbia.cs.psl.test.vmvm.classes.EEnum;
import java.edu.columbia.cs.psl.vmvm.runtime.Reinitializer;

public class EnumITCase {
	@Test
	public void testValueOf() throws Exception {
		EEnum a = EEnum.A;
		Reinitializer.markAllClassesForReinit();
		EEnum b = EEnum.A;
		System.out.println("Now assertsame");
		assertSame(b,EEnum.valueOf("A"));
	}
	@Test
	public void testValues() throws Exception {
		EEnum a = EEnum.A;
		Reinitializer.markAllClassesForReinit();
		EEnum b = EEnum.A;
		for(EEnum z : EEnum.values())
		{
			if(z.getName().equals("A"))
				assertSame(b,z);
		}
	}
}
