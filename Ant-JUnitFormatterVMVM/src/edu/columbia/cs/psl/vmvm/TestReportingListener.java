package edu.columbia.cs.psl.vmvm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;


public class TestReportingListener implements JUnitResultFormatter {

	static Connection db = getConnection();
	static int testID;

	static
	{
		try{
		testID  = Integer.valueOf(System.getProperty("vmvm.study.testID"));
		}
		catch(NumberFormatException ex)
		{
			Scanner s;
			try {
				s = new Scanner(new File("vmvm.study.testID"));
				testID = s.nextInt();
				s.close();
			} catch (FileNotFoundException e) {
				throw new IllegalStateException("No test id set!", e);
			}

		}
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					db.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}));
	}
	static Connection getConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			return DriverManager.getConnection("jdbc:mysql://127.0.0.1/foss?user=foss&password=f055");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	int nFailures;
	@Override
	public void addError(Test arg0, Throwable arg1) {
		nErrors++;
		res.failed = true;
		res.stderr.append("Failed on  "+arg0+": " + arg1.getMessage()+Arrays.toString(arg1.getStackTrace()));
	}
	TestResult res;
	@Override
	public void addFailure(Test arg0, AssertionFailedError arg1) {
		{
			nFailures++;
			res.failed = true;
			res.stderr.append("Failed: " + arg1.getMessage());
		}
	}

	@Override
	public void endTest(Test arg0) {

	}

	@Override
	public void startTest(Test arg0) {
			res.nMethods++;
	}
static int nErrors;
	static boolean done = false;
	@Override
	public void endTestSuite(JUnitTest arg0) throws BuildException {
			res.finished = System.currentTimeMillis();
			if(done)
				return;
			done = true;
			try{
				if(db == null)
					db =getConnection();
			PreparedStatement ps = db.prepareStatement("INSERT INTO test_result_test (test_execution_id,test,time,output,success,nTestMethods,start,end,nFailures,nErrors) VALUES (?,?,?,?,?,?,?,?,?,?)");
			ps.setInt(1, testID);
			ps.setString(2, res.name);
			ps.setLong(3, res.finished-res.startTime);
			ps.setString(4, "Stdout:\n"+res.stdout.toString()+"\n\nStderr:\n"+res.stderr.toString());
			ps.setInt(5, nFailures>0?0:1);
			ps.setInt(6, res.nMethods);
			ps.setLong(7, res.startTime);
			ps.setLong(8,res.finished);
			ps.setInt(9, nFailures);
			ps.setInt(10,nErrors);
			ps.executeUpdate();
			}
			catch(SQLException ex)
			{
				ex.printStackTrace();
			}
	}

	@Override
	public void setOutput(OutputStream arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSystemError(String arg0) {
			res.stderr.append(arg0);
	}

	@Override
	public void setSystemOutput(String arg0) {
			res.stdout.append(arg0);
	}

	@Override
	public void startTestSuite(JUnitTest arg0) throws BuildException {
		done = false;
		nFailures=0;
		nErrors=0;
		res = (new TestResult(arg0.getName()));
	}

	class TestResult
	{
		StringBuffer stdout = new StringBuffer();
		StringBuffer stderr = new StringBuffer();
		int nMethods=0;
		long startTime = System.currentTimeMillis();
		long finished;
		boolean failed;
		String name;
		public TestResult(String name)
		{
			this.name=name;
		}
	}
}
