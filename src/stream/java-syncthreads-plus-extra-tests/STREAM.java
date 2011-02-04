import java.lang.Double;

public class STREAM {

 static final int NTESTS = 7;

 static final int N = 40000000;
 static final int M = 20;
 static final int NTIMES = 10;
 static final int OFFSET = 0;
 static final String HLINE =
     "-------------------------------------------------------------";
 static final int precision = 8; // java uses 8bytes per DOUBLE PRECISION word

 static double[] a;
 static double[] b;
 static double[] c;
 static double[] d;

 static double[] avgtime;
 static double[] maxtime;
 static double[] mintime;

 static String[] label = { "Copy:      ", "Scale:     ",
			   "Add:       ", "Triad:     ",
			   "Write:     ", "Read:      ",
			   "Scale1:    " };

 static double[] bytes;

 static double[] partial_sums;


 public static void main(String args[]) {
   int num_threads = Integer.parseInt(args[0]);
   Thread ts[] = new Thread[num_threads];

   int j, k;
   boolean verbose = false;

   a  = new double[N+OFFSET];
   b  = new double[N+OFFSET];
   c  = new double[N+OFFSET];
   d  = new double[N+OFFSET];

   avgtime = new double[NTESTS];
   maxtime = new double[NTESTS];
   mintime = new double[NTESTS];
   for (j=0; j<NTESTS; j++) {
       mintime[j] = Double.MAX_VALUE;
   }

   bytes = new double[NTESTS];
   bytes[0] =  2 * precision * N;
   bytes[1] =  2 * precision * N;
   bytes[2] =  3 * precision * N;
   bytes[3] =  3 * precision * N;
   bytes[4] =  1 * precision * N;
   bytes[5] =  1 * precision * N;
   bytes[6] =  2 * precision * N;

   int BytesPerWord;
   double[][] times = new double[NTESTS][NTIMES];

   /* --- SETUP --- determine precision and check timing --- */

   BytesPerWord = precision;
   System.out.println(HLINE);
   System.out.println("This system uses " + BytesPerWord +
		      " bytes per DOUBLE PRECISION word.");
   System.out.println(HLINE);
   System.out.println("Array size = " + N + ", Offset = " + OFFSET);
   System.out.format("Total memory required = %.1f MB.\n",
		     (4.0 * BytesPerWord) * ( (double) N / 1048576.0));
   System.out.println("Each test is run " + NTIMES + " times, but only");
   System.out.println("the *best* time for each is used.");

   System.out.println(HLINE);
   System.out.println("Number of Threads requested = " + num_threads);
   System.out.println(HLINE);

   /* Get initial value for system clock. */

   // Since we will be running several loops in parallel, and all of
   // them iterate over the loop variable j from 0 to N-1, inclusive,
   // let us here decide once how to split up the work among
   // num_threads parallel threads, and then use that same method each
   // time.
   int start_index[] = new int[num_threads];
   int end_index_plus_1[]   = new int[num_threads];
   int num_indices_per_thread = N / num_threads;
   int num_threads_that_do_1_extra_index = N % num_threads;
   k = 0;
   for (j=0; j<num_threads; j++) {
       start_index[j]      = k;
       if (j < num_threads_that_do_1_extra_index) {
	   k += (num_indices_per_thread + 1);
       } else {
	   k += num_indices_per_thread;
       }
       end_index_plus_1[j] = k;
   }
   if (k != N) {
       System.out.println("Error in code that divides up work between threads.");
       System.out.println("N=" + N);
       System.out.println("num_threads=" + num_threads);
       System.out.println("k=" + k);
       System.exit(1);
   }

   // Note: The code below parallelizes this commented-out loop:
   // for (j=0; j<N; j++) {
   //   a[j] = 1.0;
   //   b[j] = 2.0;
   //   c[j] = 0.0;
   // }
   for (int i=0; i<num_threads; i++) {
       final int start = start_index[i];
       final int end = end_index_plus_1[i];
       ts[i] = new Thread() {
	       public void run() {
		   for (int j=start; j<end; j++) {
		       a[j] = 1.0;
		       b[j] = 2.0;
		       c[j] = 0.0;
		   }
	       }
	   };
   }
   fork_and_join(ts);

   int quantum = checktick();
   if (quantum >= 1) {
       System.out.println("Your clock granularity/precision appears to be "
			  + quantum + " microseconds.");
   } else {
       System.out.println("Your clock granularity appears to be "
			  + "less than one microsecond.");
       quantum = 1;
   }

   // Note: The code below parallelizes this commented-out loop:
   // for (j = 0; j < N; j++)
   //     a[j] = 2.0E0 * a[j];
   for (int i=0; i<num_threads; i++) {
       final int start = start_index[i];
       final int end = end_index_plus_1[i];
       ts[i] = new Thread() {
	       public void run() {
		   for (int j=start; j<end; j++) {
		       a[j] = 2.0E0 * a[j];
		   }
	       }
	   };
   }
   double t = mysecond();
   fork_and_join(ts);
   t = 1.0E6 * (mysecond() - t);

   System.out.println("Each test below will take on the order of "
		      + (int) t + " microseconds.");
   System.out.println("   (= " + (int) (t/quantum) + " clock ticks)");
   System.out.println("Increase the size of the arrays if this shows that");
   System.out.println("you are not getting at least 20 clock ticks per test.");

   System.out.println(HLINE);

   System.out.println("WARNING -- The above is only a rough guideline.");
   System.out.println("For best results, please be sure you know the");
   System.out.println("precision of your system timer.");
   System.out.println(HLINE);

   /*    --- MAIN LOOP --- repeat test cases NTIMES times --- */

   final double scalar = 3.0;
   double total_sum = 0.0;
   partial_sums = new double[num_threads];
   for (k=0; k<NTIMES; k++) {
       // Note: The code below parallelizes this commented-out loop:
       // for (j=0; j<N; j++)
       // 	   c[j] = a[j];
       for (int i=0; i<num_threads; i++) {
	   final int start = start_index[i];
	   final int end = end_index_plus_1[i];
	   ts[i] = new Thread() {
		   public void run() {
		       for (int j=start; j<end; j++) {
			   c[j] = a[j];
		       }
		   }
	       };
       }
       times[0][k] = mysecond();
       fork_and_join(ts);
       times[0][k] = mysecond() - times[0][k];
       
       // Note: The code below parallelizes this commented-out loop:
       // for (j=0; j<N; j++)
       // 	   b[j] = scalar*c[j];
       for (int i=0; i<num_threads; i++) {
	   final int start = start_index[i];
	   final int end = end_index_plus_1[i];
	   ts[i] = new Thread() {
		   public void run() {
		       for (int j=start; j<end; j++) {
			   b[j] = scalar*c[j];
		       }
		   }
	       };
       }
       times[1][k] = mysecond();
       fork_and_join(ts);
       times[1][k] = mysecond() - times[1][k];
       
       // Note: The code below parallelizes this commented-out loop:
       // for (j=0; j<N; j++)
       // 	   c[j] = a[j]+b[j];
       for (int i=0; i<num_threads; i++) {
	   final int start = start_index[i];
	   final int end = end_index_plus_1[i];
	   ts[i] = new Thread() {
		   public void run() {
		       for (int j=start; j<end; j++) {
			   c[j] = a[j]+b[j];
		       }
		   }
	       };
       }
       times[2][k] = mysecond();
       fork_and_join(ts);
       times[2][k] = mysecond() - times[2][k];
       
       // Note: The code below parallelizes this commented-out loop:
       // for (j=0; j<N; j++)
       // 	   a[j] = b[j]+scalar*c[j];
       for (int i=0; i<num_threads; i++) {
	   final int start = start_index[i];
	   final int end = end_index_plus_1[i];
	   ts[i] = new Thread() {
		   public void run() {
		       for (int j=start; j<end; j++) {
			   a[j] = b[j]+scalar*c[j];
		       }
		   }
	       };
       }
       times[3][k] = mysecond();
       fork_and_join(ts);
       times[3][k] = mysecond() - times[3][k];
       
       final double val = (double) k;
       // Note: The code below parallelizes this commented-out loop:
       // for (j=0; j<N; j++)
       // 	   d[j] = val;
       for (int i=0; i<num_threads; i++) {
	   final int start = start_index[i];
	   final int end = end_index_plus_1[i];
	   ts[i] = new Thread() {
		   public void run() {
		       for (int j=start; j<end; j++) {
			   d[j] = val;
		       }
		   }
	       };
       }
       times[4][k] = mysecond();
       fork_and_join(ts);
       times[4][k] = mysecond() - times[4][k];
       
       double sum = 0.0;
       // Note: The code below parallelizes this commented-out loop:
       // for (j=0; j<N; j++)
       // 	   sum += c[j];
       for (int i=0; i<num_threads; i++) {
	   final int start = start_index[i];
	   final int end = end_index_plus_1[i];
	   final int thread_ind = i;
	   ts[i] = new Thread() {
		   public void run() {
		       // Make a method call here instead of having
		       // the code in-line, because I don't know a
		       // good way to save the value of sum so that it
		       // can be retrieved and added up below.
		       do_partial_sum(c, start, end, thread_ind);
		   }
	       };
       }
       times[5][k] = mysecond();
       fork_and_join(ts);
       // Add up the partial sums calculated by each thread in
       // do_partial_sum().
       for (int i=0; i<num_threads; i++) {
	   sum += partial_sums[i];
       }
       times[5][k] = mysecond() - times[5][k];
       total_sum += sum;
       
       // Note: The code below parallelizes this commented-out loop:
       // for (j=0; j<N; j++)
       // 	   d[j] = scalar*d[j];
       for (int i=0; i<num_threads; i++) {
	   final int start = start_index[i];
	   final int end = end_index_plus_1[i];
	   ts[i] = new Thread() {
		   public void run() {
		       for (int j=start; j<end; j++) {
			   d[j] = scalar*d[j];
		       }
		   }
	       };
       }
       times[6][k] = mysecond();
       fork_and_join(ts);
       times[6][k] = mysecond() - times[6][k];
   }

   /*    --- SUMMARY --- */

   for (k=1; k<NTIMES; k++) { /* note -- skip first iteration */
       for (j=0; j<NTESTS; j++) {
	   avgtime[j] = avgtime[j] + times[j][k];
	   mintime[j] = Math.min(mintime[j], times[j][k]);
	   maxtime[j] = Math.max(maxtime[j], times[j][k]);
       }
   }

   // To ensure total_sum and sum are not optimized away.
   System.out.println("total_sum=" + total_sum);

   System.out.println("Function      Rate (MB/s)   Avg time     Min time     Max time");
   for (j=0; j<NTESTS; j++) {
       avgtime[j] = avgtime[j]/(double)(NTIMES-1);
       double bandwidth = 1.0E-06 * bytes[j]/mintime[j];
       System.out.print(label[j]);
       System.out.format("%11.4f", bandwidth);
       System.out.format("  %11.4f", avgtime[j]);
       System.out.format("  %11.4f", mintime[j]);
       System.out.format("  %11.4f", maxtime[j]);
       System.out.println();
   }
   System.out.println(HLINE);

   /* --- Check Results --- */
   checkSTREAMresults();
   System.out.println(HLINE);
 }


 static void do_partial_sum(double[] c, int start, int end, int i) {
     double sum = 0.0;
     for (int j=start; j<end; j++) {
	 sum += c[j];
     }
     partial_sums[i] = sum;
 }


 static private int checktick() {
     int      i, minDelta, Delta;
     double   t1, t2;
     double[] timesfound = new double[M];
     
     /*  Collect a sequence of M unique time values from the system. */
     
     for (i = 0; i < M; i++) {
	 t1 = mysecond();
	 while( ((t2=mysecond()) - t1) < 1.0E-6 ) {}
	 timesfound[i] = t1 = t2;
     }
     
     /*
      * Determine the minimum difference between these M values.
      * This result will be our estimate (in microseconds) for the
      * clock granularity.
      */
     
     minDelta = 1000000;
     for (i = 1; i < M; i++) {
	 Delta = (int)( 1.0E6 * (timesfound[i]-timesfound[i-1]));
	 minDelta =(int) Math.min(minDelta, Math.max(Delta,0));
     }
     
     return(minDelta);
 }



 /** A gettimeofday routine to give access to the wall
     clock timer on most UNIX-like systems.  */
 static private double mysecond() {
     //need to return microseconds not milliseconds -- big big problem!

     return System.nanoTime()/1e9;
 }

 static void checkSTREAMresults ()
 {
     double aj,bj,cj,dj,scalar;
     double asum,bsum,csum,dsum;
     double epsilon;
     int    j,k;

     /* reproduce initialization */
     aj = 1.0;
     bj = 2.0;
     cj = 0.0;
     /* a[] is modified during timing check */
     aj = 2.0E0 * aj;
     /* now execute timing loop */
     scalar = 3.0;
     for (k=0; k<NTIMES; k++)
     {
	 cj = aj;
	 bj = scalar*cj;
	 cj = aj+bj;
	 aj = bj+scalar*cj;
     }
     dj = scalar * ((double) (NTIMES-1));
     aj = aj * (double) (N);
     bj = bj * (double) (N);
     cj = cj * (double) (N);
     dj = dj * (double) (N);
     
     asum = 0.0;
     bsum = 0.0;
     csum = 0.0;
     dsum = 0.0;
     for (j=0; j<N; j++) {
	 asum += a[j];
	 bsum += b[j];
	 csum += c[j];
	 dsum += d[j];
     }
     //#ifdef VERBOSE
     //	printf ("Results Comparison: \n");
     //	printf ("        Expected  : %f %f %f %f \n",aj,bj,cj,dj);
     //	printf ("        Observed  : %f %f %f %f \n",asum,bsum,csum,dsum);
     //#endif

    epsilon = 1.0e-8;

    if (Math.abs(aj-asum)/asum > epsilon) {
	System.out.format("Failed Validation on array a[]\n");
	System.out.format("        Expected  : %f \n",aj);
	System.out.format("        Observed  : %f \n",asum);
    }
    else if (Math.abs(bj-bsum)/bsum > epsilon) {
	System.out.format("Failed Validation on array b[]\n");
	System.out.format("        Expected  : %f \n",bj);
	System.out.format("        Observed  : %f \n",bsum);
    }
    else if (Math.abs(cj-csum)/csum > epsilon) {
	System.out.format("Failed Validation on array c[]\n");
	System.out.format("        Expected  : %f \n",cj);
	System.out.format("        Observed  : %f \n",csum);
    }
    else if (Math.abs(dj-dsum)/dsum > epsilon) {
	System.out.format("Failed Validation on array d[]\n");
	System.out.format("        Expected  : %f \n",dj);
	System.out.format("        Observed  : %f \n",dsum);
    }
    else {
	System.out.format("Solution Validates\n");
    }
 }


 static void fork_and_join (Thread[] ts)
 {
     int i;
     int num_threads = ts.length;

     // Start them all
     for (i=0; i<num_threads; i++) {
	 ts[i].start();
     }
     // wait for them all to finish
     for (i=0; i<num_threads; i++) {
	 try {
	     ts[i].join();
	 } catch (InterruptedException e) {
	     System.out.println("Exception thrown from Thread.join() call");
	     System.exit(1);
	 }
     }
 }

}

