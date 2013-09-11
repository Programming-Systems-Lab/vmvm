package edu.columbia.cs.psl.vmvm.sirRunner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import edu.columbia.cs.psl.vmvm.ReflectionWrapper;
import edu.columbia.cs.psl.vmvm.VirtualRuntime;

import junit.framework.Test;
import junit.framework.TestSuite;

public class SuiteWrapper {
	static Connection db = getConnection();

	static Connection getConnection() {
		try {
			if(db == null || db.isClosed())
			{
			Class.forName("com.mysql.jdbc.Driver");
			db = DriverManager.getConnection("jdbc:mysql://127.0.0.1/foss?user=foss&password=f055");
			}
			return db;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}


	public static void runSuite(TestSuite s, String name, String mode) {
		try {
			System.err.println("Running suite: " + name + " in mode " + mode);
			PreparedStatement ps = getConnection().prepareStatement("INSERT INTO test_execution(type,mode,manual_name) VALUES (?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setString(1, "SIR");
			ps.setString(2, mode);
			ps.setString(3, name);
			ps.executeUpdate();	
			ResultSet rs = ps.getGeneratedKeys();
			int testID=-1;
			if (rs != null && rs.next()) {
			    testID= rs.getInt(1);
			}
			if(mode.startsWith("fork"))
			{
				Enumeration<Test> e = s.tests();
				while(e.hasMoreElements())
				{
					TestSuite t = (TestSuite) e.nextElement();
//					TestRunner.main(new String[]{t.testAt(0).getClass().getName(),""+testID});
					
					RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
				    String cp = mx.getClassPath();

				    List<String> listArgs = new ArrayList<String>();
				    listArgs.add("/usr/java/jdk1.7.0_10/bin/java");
				    listArgs.add("-Dlog4j.configuration=mylogging.properties");
				    listArgs.add("-cp");
				    listArgs.add(cp);
				    listArgs.add("edu.columbia.cs.psl.vmvm.sirRunner.TestRunner");
				    listArgs.add(t.testAt(0).getClass().getName());
				    listArgs.add(""+testID);
				    System.err.println(listArgs);
				    String[] res = new String[listArgs.size()];
				    Process process = Runtime.getRuntime().exec(listArgs.toArray(res));
				    process.waitFor();
//				    InputStream stderr = process.getErrorStream ();
//				    InputStream stdout = process.getInputStream ();
//				    String line;
//				    BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
//				    while ((line = reader.readLine ()) != null) {
//				    	System.out.println ("Stdout: " + line);
//				    	}
//				     reader = new BufferedReader (new InputStreamReader(stderr));
//				    while ((line = reader.readLine ()) != null) {
//				    	System.out.println ("stderr: " + line);
//				    	}

				}
				System.exit(0);
			}
			else if(mode.startsWith("normal"))
			{
//				junit.textui.TestRunner runner = new junit.textui.TestRunner(System.out);
//				runner.run(s);

				Enumeration<Test> e = s.tests();
				while(e.hasMoreElements())
				{
					
					TestSuite t = (TestSuite) e.nextElement();
					System.err.println(t.testAt(0).getClass().getName());
					TestRunner.main(new String[]{t.testAt(0).getClass().getName(),""+testID});
				}
				System.exit(0);
			}
			else if(mode.startsWith("vmvm"))
			{
				Enumeration<Test> e = s.tests();
				while(e.hasMoreElements())
				{
					
					TestSuite t = (TestSuite) e.nextElement();
					System.err.println(t.testAt(0).getClass().getName());
					ReflectionWrapper.forName(t.testAt(0).getClass().getName(), SuiteWrapper.class.getClassLoader());
					TestRunner.main(new String[]{t.testAt(0).getClass().getName(),""+testID});
					VirtualRuntime.resetStatics();
				}
				System.exit(0);
			}
			db.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	public static void runSuite(ArrayList<String> s, String name, String mode,boolean hack,String extraArgs) {
		try {
			System.err.println("Running suite: " + name + " in mode " + mode);
			PreparedStatement ps = getConnection().prepareStatement("INSERT INTO test_execution(type,mode,manual_name) VALUES (?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setString(1, "SIR");
			ps.setString(2, mode);
			ps.setString(3, name);
			ps.executeUpdate();	
			ResultSet rs = ps.getGeneratedKeys();
			int testID=-1;
			if (rs != null && rs.next()) {
			    testID= rs.getInt(1);
			}
			if(mode.startsWith("fork"))
			{
				for(String t : s)
				{
//					TestRunner.main(new String[]{t.testAt(0).getClass().getName(),""+testID});
					
					RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
				    String cp = mx.getClassPath();

				    List<String> listArgs = new ArrayList<String>();
				    listArgs.add("/usr/java/jdk1.7.0_10/bin/java");
				    listArgs.add("-Dlog4j.configuration=mylogging.properties");

				    listArgs.add("-cp");
				    listArgs.add(cp);
				    listArgs.add("edu.columbia.cs.psl.vmvm.sirRunner.TestRunner");
				    listArgs.add(t);
				    listArgs.add(""+testID);
				    if(hack)
				    listArgs.add("hack");
				    if(extraArgs.length()>0)
				    {
				    	for(String str : extraArgs.split(" "))
				    		listArgs.add(str);
				    }
				    System.err.println("Forking: " + t);
//				    System.out.println(listArgs);
				    String[] res = new String[listArgs.size()];
				    Process process = Runtime.getRuntime().exec(listArgs.toArray(res));
				    process.waitFor();
				    InputStream stderr = process.getErrorStream ();
				    InputStream stdout = process.getInputStream ();
				    String line;
				    BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
				    while ((line = reader.readLine ()) != null) {
				    	System.out.println ("Stdout: " + line);
				    	}
				    reader.close();
				     reader = new BufferedReader (new InputStreamReader(stderr));
				    while ((line = reader.readLine ()) != null) {
				    	System.out.println ("stderr: " + line);
				    	}
				    reader.close();
//				    System.err.println(listArgs);
				}
				System.exit(0);
			}
			else if(mode.startsWith("normal"))
			{
//				junit.textui.TestRunner runner = new junit.textui.TestRunner(System.out);
//				runner.run(s);

				for(String t : s){
					if(hack)
						TestRunner.main(new String[]{t,""+testID,"hack"});
					else
						TestRunner.main(new String[]{t,""+testID});
				}
				System.exit(0);
			}
			else if(mode.startsWith("vmvm"))
			{
				for(String t : s){
					ReflectionWrapper.forName(t, SuiteWrapper.class.getClassLoader());

					if(hack)
						TestRunner.main(new String[]{t,""+testID,"hack"});
					else if(extraArgs.length()>0)
						TestRunner.main(new String[]{t,""+testID,extraArgs.split(" ")[0],extraArgs.split(" ")[1]});
					else
						TestRunner.main(new String[]{t,""+testID});
					VirtualRuntime.resetStatics();
				}
				System.exit(0);
			}
			db.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
