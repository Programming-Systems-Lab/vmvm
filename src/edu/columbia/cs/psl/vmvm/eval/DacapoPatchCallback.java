package edu.columbia.cs.psl.vmvm.eval;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class DacapoPatchCallback extends Callback {

	  public DacapoPatchCallback(CommandLineArgs args) {
	    super(args);
	  }

	  /* Immediately prior to start of the benchmark */
	  public void start(String benchmark) {
	    System.err.println("my hook starting " + (isWarmup() ? "warmup " : "") + benchmark);
	    System.exit(-1);
	    super.start(benchmark);
	  };

	  /* Immediately after the end of the benchmark */
	  public void stop() {
	    super.stop();
	    System.err.println("my hook stopped " + (isWarmup() ? "warmup" : ""));
	    System.err.flush();
	  };

	  public void complete(String benchmark, boolean valid) {
	    super.complete(benchmark, valid);
	    System.err.println("my hook " + (valid ? "PASSED " : "FAILED ") + (isWarmup() ? "warmup " : "") + benchmark);
	    System.err.flush();
	  };
	}

