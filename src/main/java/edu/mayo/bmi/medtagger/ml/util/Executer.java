package edu.mayo.bmi.medtagger.ml.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class Executer {
	    private int status = -1;
	    private byte[] out;
	    private byte[] err;

	   public void execute(String command) throws IOException {

	      Process process = Runtime.getRuntime().exec(command);
	      ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
	      ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

	      StreamPump outPump = new StreamPump(
	          process.getInputStream(), outBuffer);
	      StreamPump errPump = new StreamPump(
	          process.getErrorStream(), errBuffer);

	      Thread outThread = new Thread(outPump);
	      Thread errThread = new Thread(errPump);
	      outThread.start();
	      errThread.start();


	      // wait untill the process end, and check return status
	       status = -1;
	      try {
	       status = process.waitFor();
	      }
	       catch (InterruptedException ex) {
	       throw new IOException("Interrupted when waiting for external process finished.");
	      }

	      // to make sure that all streams are finished

	      try {
	        outThread.join();
	      }
	      catch (InterruptedException ex) {
	      }
	      try {
	        errThread.join();
	      }
	      catch (InterruptedException ex) {
	      }

	      // check if an exception is raised on writing thread

	      if (outPump.getException() != null) {
	        throw outPump.getException();
	      }
	      if (errPump.getException() != null) {
	        throw errPump.getException();
	      }

	      out = outBuffer.toByteArray();
	      if (out.length == 0) {
	        out = null;
	      }
	      err = errBuffer.toByteArray();
	      if (err.length == 0) {
	        err = null;
	      }
	   }




	    public int getStatus() {
	      return status;
	    }
	    public byte[] getOut() {
	      return out;
	    }
	    public byte[] getErr() {
	      return err;
	    }
	  }


	  

