package edu.columbia.cs.psl.vmvm;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import edu.columbia.cs.psl.vmvm.TestReportingListener.TestResult;

public class MvnReportingListener extends RunListener{
	static final Connection db = getConnection();
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
	Class<?> lastTestClass =null;
	/**
	 *  Called when an atomic test is about to be started.
	 * */
	public void testStarted(Description description) throws java.lang.Exception
	{
		if(description.getTestClass() != lastTestClass)
		{
			//we are doing another test class
			if(res != null)
				finishedClass();
			res = new TestResult(description.getTestClass().getName());
			lastTestClass = description.getTestClass();
		}
		res.nMethods++;
	}
	@Override
	public void testFinished(Description description) throws Exception {
		res.stdout.append(description.getDisplayName() + "Finished\n");
	}
	int nErrors;
	@Override
	public void testRunFinished(Result result) throws Exception {

		nFailures = result.getFailureCount();
		finishedClass();
	}
	private void finishedClass()
	{
		res.finished = System.currentTimeMillis();
		try{
			PreparedStatement ps = db.prepareStatement("INSERT INTO test_result_test (test_execution_id,test,time,output,success,nTestMethods,start,end,nFailures) VALUES (?,?,?,?,?,?,?,?,?)");
			ps.setInt(1, testID);
			ps.setString(2, res.name);
			ps.setLong(3, res.finished-res.startTime);
			ps.setString(4, "Stdout:\n"+res.stdout.toString()+"\n\nStderr:\n"+res.stderr.toString());
			ps.setInt(5, nFailures>0?0:1);
			ps.setInt(6, res.nMethods);
			ps.setLong(7, res.startTime);
			ps.setLong(8,res.finished);
			ps.setInt(9, nFailures);
			ps.executeUpdate();
			}
			catch(SQLException ex)
			{
				ex.printStackTrace();
			}
	}


	/**
	 *  Called when an atomic test fails.
	 * */
	public void testFailure(Failure failure) throws java.lang.Exception
	{
		nFailures++;
		res.failed = true;
		res.stderr.append("Failed on  "+failure.getTestHeader()+": " + failure.getMessage()+Arrays.toString(failure.getException().getStackTrace()));
	}
	TestResult res;
	/**
	 *  Called when a test will not be run, generally because a test method is annotated with Ignore.
	 * */
	public void testIgnored(Description description) throws java.lang.Exception
	{
		System.out.println("Execution of test case ignored : "+ description.getMethodName());
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
