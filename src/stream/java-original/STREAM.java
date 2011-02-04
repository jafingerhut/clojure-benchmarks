/*-----------------------------------------------------------------------*/
/* Program: STREAM.java                                                  */
/* Based on the following revision of stream.c                           */
/* Revision: $Id: stream.c,v 5.9 2009/04/11 16:35:00 mccalpin Exp $ */
/* Original code developed by John D. McCalpin                           */
/* Programmers: John D. McCalpin                                         */
/*              Joe R. Zagar                                             */
/*                                                                       */
/* This program measures memory transfer rates in MB/s for simple        */
/* computational kernels coded in C.                                     */
/*-----------------------------------------------------------------------*/
/* Copyright 1991-2005: John D. McCalpin                                 */
/*-----------------------------------------------------------------------*/
/* License:                                                              */
/*  1. You are free to use this program and/or to redistribute           */
/*     this program.                                                     */
/*  2. You are free to modify this program for your own use,             */
/*     including commercial use, subject to the publication              */
/*     restrictions in item 3.                                           */
/*  3. You are free to publish results obtained from running this        */
/*     program, or from works that you derive from this program,         */
/*     with the following limitations:                                   */
/*     3a. In order to be referred to as "STREAM benchmark results",     */
/*         published results must be in conformance to the STREAM        */
/*         Run Rules, (briefly reviewed below) published at              */
/*         http://www.cs.virginia.edu/stream/ref.html                    */
/*         and incorporated herein by reference.                         */
/*         As the copyright holder, John McCalpin retains the            */
/*         right to determine conformity with the Run Rules.             */
/*     3b. Results based on modified source code or on runs not in       */
/*         accordance with the STREAM Run Rules must be clearly          */
/*         labelled whenever they are published.  Examples of            */
/*         proper labelling include:                                     */
/*         "tuned STREAM benchmark results"                              */
/*         "based on a variant of the STREAM benchmark code"             */
/*         Other comparable, clear and reasonable labelling is           */
/*         acceptable.                                                   */
/*     3c. Submission of results to the STREAM benchmark web site        */
/*         is encouraged, but not required.                              */
/*  4. Use of this program or creation of derived works based on this    */
/*     program constitutes acceptance of these licensing restrictions.   */
/*  5. Absolutely no warranty is expressed or implied.                   */
/*-----------------------------------------------------------------------*/
import java.lang.Double;

public class STREAM {

 static Object lock = new Object();

 static Object lock2 = new Object();
 static volatile int _barrier;
 static void reach_barrier() { synchronized(lock2) { _barrier++; } }

 static double total_bandwidth[] = new double[4];

 static String[] label = { "Copy:      ", "Scale:     ",
			   "Add:       ", "Triad:     " };

 int  N;
 int  M;
 int  NTIMES;
 int  OFFSET;
 String  HLINE ;
 int precision;

 double[] a ;
 double[] b;
 double[] c;

 double[]    avgtime;
 double[]    maxtime;
 double[]    mintime;
 double[]    bytes;
 Double tmp;
 double FLT_MAX;


 long pr(long ticks, int pid, String msg, boolean do_print) {
   long t = System.currentTimeMillis();
   if (do_print) {
       System.out.println("PID "+pid+msg+" ("+((t-ticks)/1000)+" secs since last report)");
   }
   return t;
 }

 public void run(int pid, boolean print_normal_msgs, boolean verbose) {
   long ticks = pr(System.currentTimeMillis(),pid," begins to allocate",verbose);
   N = 20000000;
   M = 20;
   NTIMES=10;
   OFFSET=0;
   HLINE= "-------------------------------------------------------------";
   precision = 8; // java uses 8bytes per DOUBLE PRECISION word

   tmp=  new Double(0);
   FLT_MAX =tmp.longBitsToDouble(0x7fefffffffffffffL);


   a  = new double[N+OFFSET];
   b  = new double[N+OFFSET];
   c  = new double[N+OFFSET];

   avgtime = new double[4];
   maxtime = new double[4];
   mintime  = new double[4];

   mintime[0] = FLT_MAX;
   mintime[1] = FLT_MAX;
   mintime[2] = FLT_MAX;
   mintime[3] = FLT_MAX;

   bytes= new double[4];
   bytes[0] =  2 * precision * N;
   bytes[1] =  2 * precision * N;
   bytes[2] =  3 * precision * N;
   bytes[3] =  3 * precision * N;

   int            BytesPerWord;
   int    j, k;
   double        scalar;
   double[][] times = new double[4][NTIMES];

   /* --- SETUP --- determine precision and check timing --- */

   BytesPerWord = precision;
   if (print_normal_msgs) {
       System.out.println(HLINE);
       System.out.println("This system uses " + BytesPerWord+" bytes per DOUBLE PRECISION word." );
       System.out.println(HLINE);
       System.out.println("Array size = "+ N+", Offset = "+OFFSET);
       System.out.format("Total memory required = %.1f MB.\n",
			 ((3.0 * BytesPerWord) * ( (double) N / 1048576.0)));
       System.out.println("Each test is run "+NTIMES+" times, but only");
       System.out.println("the *best* time for each is used.");
   }

   /* Get initial value for system clock. */

   for (j=0; j<N; j++) {
     a[j] = 1.0;
     b[j] = 2.0;
     c[j] = 0.0;
   }

   ticks = pr(ticks,pid," checks time quanta",verbose);

   int temp = checktick();
   if (temp >= 1) {
       if (print_normal_msgs) {
	   System.out.println("Your clock granularity/precision appears to be "
			      + temp + " microseconds.");
       }
   } else {
       if (print_normal_msgs) {
	   System.out.println("Your clock granularity appears to be "
			      + "less than one microsecond.");
       }
       temp = 1;
   }
   final int quantum = temp;

   ticks = pr(ticks,pid," begins to init array",verbose);
   double t = mysecond();
   for (j = 0; j < N; j++)
     a[j] = 2.0E0 * a[j];
   t = 1.0E6 * (mysecond() - t);

   if (print_normal_msgs) {
       System.out.println("Each test below will take on the order of "+(int) t+" microseconds.");
       System.out.println("   (= "+(int) (t/quantum)+" clock ticks)" );
       System.out.println("Increase the size of the arrays if this shows that");
       System.out.println("you are not getting at least 20 clock ticks per test.");
       System.out.println(HLINE);
   }

   reach_barrier();
   ticks = pr(ticks,pid," waits on lock",verbose);
   synchronized(lock) {}       // block till all ready
   ticks = pr(ticks,pid," starts main loop",verbose);

   /*    --- MAIN LOOP --- repeat test cases NTIMES times --- */

   scalar = 3.0;
   for (k=0; k<NTIMES; k++) {

     times[0][k] = mysecond();
     for (j=0; j<N; j++)
       c[j] = a[j];
     times[0][k] = mysecond() - times[0][k];

     times[1][k] = mysecond();
     for (j=0; j<N; j++)
       b[j] = scalar*c[j];
     times[1][k] = mysecond() - times[1][k];

     times[2][k] = mysecond();
     for (j=0; j<N; j++)
       c[j] = a[j]+b[j];
     times[2][k] = mysecond() - times[2][k];

     times[3][k] = mysecond();
     for (j=0; j<N; j++)
       a[j] = b[j]+scalar*c[j];
     times[3][k] = mysecond() - times[3][k];
   }

   /*    --- SUMMARY --- */

   for (k=1; k<NTIMES; k++) { /* note -- skip first iteration */
     for (j=0; j<4; j++) {
       avgtime[j] =     avgtime[j]+ times[j][k];
       mintime[j] = MIN(mintime[j], times[j][k]);
       maxtime[j] = MAX(maxtime[j], times[j][k]);
     }
   }

   System.out.println("Function      Rate (MB/s)   Avg time     Min time     Max time");
   synchronized(lock2) {
     System.out.println("PID: "+pid);
     for (j=0; j<4; j++) {
       avgtime[j] = avgtime[j]/(double)(NTIMES-1);
       double bandwidth = 1.0E-06 * bytes[j]/mintime[j];
       System.out.print(label[j]);
       System.out.format("%11.4f", bandwidth);
       System.out.format("  %11.4f", avgtime[j]);
       System.out.format("  %11.4f", mintime[j]);
       System.out.format("  %11.4f", maxtime[j]);
       System.out.println();
       total_bandwidth[j] += bandwidth;
     }
     System.out.println();
   }

   /* --- Check Results --- */
   checkSTREAMresults();
   ticks = pr(ticks,pid," exits",verbose);
   if (print_normal_msgs) {
       System.out.println(HLINE);
   }
 }

 public double MIN(double x, double y) {
   if(x<y)
     return x;
   else
     return y;
 }

 public double MAX(double x, double y) {
   if(x>y)
     return x;
   else
     return y;
 }


 public static void main(String args[]) {
   int num_threads = Integer.parseInt(args[0]);

   System.out.println("=== Warmup 0");
   new STREAM().run(0,false,true);
   System.out.println("=== Warmup 1");
   new STREAM().run(1,false,true);
   System.out.println("=== Warmup 2");
   new STREAM().run(2,false,true);
   System.out.println("=== Warmup 3");
   new STREAM().run(3,false,true);

   _barrier = 0;               // Reset barrier
   for( int i=0; i<4; i++ ) total_bandwidth[i] = 0.0;
   Thread ts[] = new Thread[num_threads];

   synchronized(lock) {
     for( int i=0; i<ts.length; i++ ) {
       final int num = i;
       final boolean print_normal_msgs = (i == 0);
       (ts[i] = new Thread() {
	       public void run() {
		   new STREAM().run(num,print_normal_msgs,false);
	       }
	   }
	   ).start();
     }

     int i=0;
     while( _barrier < num_threads ) {
       System.out.println("=== main waits "+i+" secs, seen "+_barrier+"/"+num_threads+" reach barrier");
       i++;
       try {  Thread.sleep(1000); }
       catch( Exception e ) { }
     }
     System.out.println("=== Go!");
   } // Release lock

   for( int i=0; i<ts.length; i++ ) {
     try { ts[i].join(); }
     catch( Exception e ) { }
   }

   System.out.println("=== Caught the last thread.");

   synchronized(lock2) {
     System.out.println("Average cpu bandwidth:  ");
     for (int j=0; j<4; j++) {
	 System.out.print(label[j]);
	 System.out.format("%11.4f MB/sec/cpu", total_bandwidth[j]/num_threads);
	 System.out.println();
     }

     System.out.println("Total system bandwidth: ");
     for (int j=0; j<4; j++) {
	 System.out.print(label[j]);
	 System.out.format("%11.4f MB/sec", total_bandwidth[j]);
	 System.out.println();
     }
   }

 }


 private int checktick() {
   int        i, minDelta, Delta;
   double    t1, t2;
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
     minDelta =(int) MIN(minDelta, MAX(Delta,0));
   }

   return(minDelta);
 }



 /** A gettimeofday routine to give access to the wall
     clock timer on most UNIX-like systems.  */
 private double mysecond() {
   //need to return microseconds not milliseconds -- big big problem!

   return System.nanoTime()/1e9;
 }

 void checkSTREAMresults ()
 {
     double aj,bj,cj,scalar;
     double asum,bsum,csum;
     double epsilon;
     int	j,k;

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
     aj = aj * (double) (N);
     bj = bj * (double) (N);
     cj = cj * (double) (N);
     
     asum = 0.0;
     bsum = 0.0;
     csum = 0.0;
     for (j=0; j<N; j++) {
	 asum += a[j];
	 bsum += b[j];
	 csum += c[j];
     }
     //#ifdef VERBOSE
     //	printf ("Results Comparison: \n");
     //	printf ("        Expected  : %f %f %f \n",aj,bj,cj);
     //	printf ("        Observed  : %f %f %f \n",asum,bsum,csum);
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
    else {
	System.out.format("Solution Validates\n");
    }
 }


}

