package edu.mayo.bmi.medtagger.ml.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamPump implements Runnable {
	    private BufferedInputStream in;
	    private BufferedOutputStream out;
	    private IOException exception;
	    public StreamPump(InputStream in, OutputStream out) {
	      this.in = new BufferedInputStream(in);
	      this.out = new BufferedOutputStream(out);
	    }
	    public void run() {
	      try {
	        int b;
	        while ((b = in.read()) != -1) {
	          out.write(b);
	        }
	        in.close();
	        out.close();
	      }
	      catch (IOException ex) {
	        exception = ex;
	      }
	    }
	    public IOException getException() {
	      return exception;
	    }
}

