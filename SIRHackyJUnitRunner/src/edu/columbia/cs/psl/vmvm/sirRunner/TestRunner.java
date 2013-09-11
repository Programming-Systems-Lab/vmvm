package edu.columbia.cs.psl.vmvm.sirRunner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.jmeter.util.JMeterUtils;

import sun.awt.windows.ThemeReader;

import edu.columbia.cs.psl.vmvm.ReflectionWrapper;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

public class TestRunner {
	static int globalFailures;
	static int globalErrors;
	
	static Connection getConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			return DriverManager.getConnection("jdbc:mysql://127.0.0.1/foss?user=foss&password=f055");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	public static void main(String[] args) {
		Connection db = getConnection();
		long starTime = System.currentTimeMillis();
		String clazz = args[0];
		int id = Integer.valueOf(args[1]);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		junit.textui.TestRunner runner = new junit.textui.TestRunner(new PrintStream(bos));
		int nCases = 0;
		System.err.println(args[0]);
		int nFailures= 0;
		int nErrors = 0;
		try {
			if(args.length == 4)
			{
				JMeterRunner.initializeManager(new String[]{"",args[2],args[3]});
				ReflectionWrapper.forName(org.apache.jmeter.config.Arguments.class.getName(), TestRunner.class.getClassLoader());
				ReflectionWrapper.forName(org.apache.jmeter.testelement.TestPlan.class.getName(), TestRunner.class.getClassLoader());
				Class<?> c= ReflectionWrapper.forName(clazz,TestRunner.class.getClassLoader());

				ReflectionWrapper.forName(org.apache.jmeter.config.Arguments.class.getName(), TestRunner.class.getClassLoader());
				ReflectionWrapper.forName(org.apache.jmeter.testelement.TestPlan.class.getName(), TestRunner.class.getClassLoader());

				ReflectionWrapper.forName(org.apache.jmeter.util.JMeterUtils.class.getName(), TestRunner.class.getClassLoader());

				System.err.println("Reinited: " + org.apache.jmeter.config.Arguments.class);
			}
			Class<?> c= ReflectionWrapper.forName(clazz,TestRunner.class.getClassLoader());
            if(c.getName().startsWith("org.apache.xml.security"))
            {
            	System.err.println("Initing!!");
                org.apache.xml.security.Init.init();
            }
			if(args.length == 2 || args.length == 4)
			{
				TestSuite suite = new TestSuite(c);
				nCases = suite.countTestCases();
				TestResult r = runner.doRun(suite,false);
				nFailures = r.failureCount();
				nErrors = r.errorCount();

			}
			else
			{
				TestSuite theSuite = (TestSuite) c.getMethod("suite").invoke(null);
				nCases = theSuite.countTestCases();
				TestResult r=  runner.doRun(theSuite, false);
				
//				theSuite.run(r);
				nFailures = r.failureCount();
				nErrors = r.errorCount();
//				junit.textui.TestRunner.run(theSuite);
			}
			if(nErrors> 0 || nFailures >0)
			{
				System.err.println("!!!!!!!!!!!!! nErrors=" + nErrors+", nFailures=" + nFailures);
				System.out.println(bos.toString());
			}
			System.err.println("Finished, inserting");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();

		try{
			if(db == null)
				db =getConnection();
		PreparedStatement ps = db.prepareStatement("INSERT INTO test_result_test (test_execution_id,test,time,output,success,nTestMethods,start,end,nFailures,nErrors) VALUES (?,?,?,?,?,?,?,?,?,?)");
		ps.setInt(1, id);
		ps.setString(2, args[0]);
		ps.setLong(3, endTime -starTime);
		ps.setString(4, "Stdout:\n"+bos.toString());
		ps.setInt(5, (nFailures == 0 && nErrors == 0) ? 1:0);
		ps.setInt(6, nCases);
		ps.setLong(7, starTime);
		ps.setLong(8,endTime);

		ps.setInt(9,nFailures);
		ps.setInt(10,nErrors);
		ps.executeUpdate();
		globalFailures+=nFailures;
		globalErrors += nErrors;
				
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
		}
		System.err.println("Done");
	}
}
