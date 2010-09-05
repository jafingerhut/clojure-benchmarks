import java.text.DecimalFormat;



public class ParallelTest {

    static int CacheBuster1_data[];

    public static class IntTest implements Runnable {
	long jobSize;
	int result;
	
	public IntTest(long jobSize) {
	    this.jobSize = jobSize;
	}
	
	public void run() {
	    result = 0;
	    for (long i = 0L; i < jobSize; i++) {
		result = result + 1;
	    }
	    System.out.println(result);
	}
    }

    public static class LongTest implements Runnable {
	long jobSize;
	long result;
	
	public LongTest(long jobSize) {
	    this.jobSize = jobSize;
	}
	
	public void run() {
	    result = 0;
	    for (long i = 0L; i < jobSize; i++) {
		result = result + 1;
	    }
	    System.out.println(result);
	}
    }
    
    public static class DoubleTest implements Runnable {
	long jobSize;
	double result;
	
	public DoubleTest(long jobSize) {
	    this.jobSize = jobSize;
	}
	
	public void run() {
	    result = 0.0;
	    for (long i = 0L; i < jobSize; i++) {
		result = result + 1.0;
	    }
	    System.out.println(result);
	}
    }
    
    public static class NewDoubleTestA implements Runnable {
	long jobSize;
	Double result;
	
	public NewDoubleTestA(long jobSize) {
	    this.jobSize = jobSize;
	}
	
	public void run() {
	    result = new Double(0.0);
	    for (long i = 0L; i < jobSize; i++) {
		result = new Double(result.doubleValue() + 1.0);
	    }
	    System.out.println(result);
	}
    }
    
    public static class NewDoubleTestB implements Runnable {
	long jobSize;
	Double result;
	
	public NewDoubleTestB(long jobSize) {
	    this.jobSize = jobSize;
	}
	
	public void run() {
	    result = new Double(0.0);
	    for (long i = 0L; i < jobSize; i++) {
		double old = result.doubleValue() + 1.0;
		result = new Double(old);
	    }
	    System.out.println(result);
	}
    }

    static String typeList() {
	return ("int"
		+ "," + "long"
		+ "," + "double"
		+ "," + "newdoubleA"
		+ "," + "newdoubleB"
		+ "," + "cachebuster1"
		);
    }

    static boolean legalType(String s) {
	if (s.equals("int")) return true;
	else if (s.equals("long")) return true;
	else if (s.equals("double")) return true;
	else if (s.equals("newdoubleA")) return true;
	else if (s.equals("newdoubleB")) return true;
	else if (s.equals("cachebuster1")) return true;
	return false;
    }

    static Thread newThreadWithType (String s, int threadNum, long jobSize) {
	CacheBuster1 c;
	if (s.equals("int"))
	    return new Thread(new IntTest(jobSize));
	else if (s.equals("long"))
	    return new Thread(new LongTest(jobSize));
	else if (s.equals("double"))
	    return new Thread(new DoubleTest(jobSize));
	else if (s.equals("newdoubleA"))
	    return new Thread(new NewDoubleTestA(jobSize));
	else if (s.equals("newdoubleB"))
	    return new Thread(new NewDoubleTestB(jobSize));
	else if (s.equals("cachebuster1")) {
	    c = new CacheBuster1(threadNum, jobSize);
	    if (threadNum == 0) {
		CacheBuster1_data = c.initReadData();
	    }
	    c.setData(CacheBuster1_data);
	    return new Thread(c);
	} else
	    usage(1);
	return null;
    }

    static void doTest(String taskFnSpecifier, int numJobs,
		       long jobSize, boolean parallel) {
	try {
	    Thread[] tarray = new Thread[numJobs];
	    for (int i = 0; i < numJobs; i++) {
		tarray[i] = newThreadWithType(taskFnSpecifier, i, jobSize);
	    }
	    
	    long startTime 	= System.nanoTime();
	    long stopTime	= 0;
	    long runTime	= 0;
	    if (parallel) {
		for (int i = 0; i < numJobs; i++)
		    tarray[i].start();
		for (int i = 0; i < numJobs; i++)
		    tarray[i].join();
	    } else {
		for (int i = 0; i < numJobs; i++) {
		    tarray[i].start();
		    tarray[i].join();
		}
	    }
	    
	    stopTime = System.nanoTime();
	    runTime  = stopTime - startTime;
	    System.out.println();
	    System.out.println(taskFnSpecifier+" ALL RUNS FINISHED for JOB.");
	    System.out.println(taskFnSpecifier+" OVERALL Time:  "+
			       new DecimalFormat("0.0000").format((double)runTime/1000000)
			       +" ms");
	    System.out.println();
	    System.out.println("Number of Runs: "+numJobs);
	}
	catch (Exception e) { }
    }

    static long defaultRepetitions = 1000000000L;
    
    static void usage(int exitCode) {
	String programName = "ParallelTest";
	System.out.println("usage: "+programName+" type num-jobs job-size {parallel|sequential}");
	System.out.println("    type must be one of: "+typeList());
  	System.out.println("    all other arguments must be integers >= 0");
  	System.out.println("    num-jobs must be >= 1, and is the number of jobs in the list to perform");
  	System.out.println("    job-size is the number of steps in each job");
  	System.out.println("        0 means to use the default number of steps: "
			   +defaultRepetitions);
  	System.out.println("    parallel|sequential is whether to run the jobs sequentially, or in parallel");
	System.exit(exitCode);
    }

    static boolean decimalLongString (String s) {
	try {
	    Long i = Long.valueOf(s);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

    public static void main(String[] args) {
	try {
	    if (args.length != 4) {
		System.out.println("Expected 4 args but found "+args.length);
		usage(1);
	    }
	    String taskFnSpecifier = args[0];
	    if (!legalType(taskFnSpecifier)) {
		System.out.println("Type specified was "+taskFnSpecifier+
				   " but must be one of: "+typeList());
	    }

	    String arg = args[1];
	    int numJobs = 0;
	    try {
		numJobs = Integer.valueOf(arg).intValue();
	    } catch (Exception e) {
		System.out.println("num-jobs specified was "+arg+
				   " but must be an integer");
		usage(1);
	    }
	    if (numJobs < 1) usage(1);

	    arg = args[2];
	    long jobSize = 0;
	    try {
		jobSize = Long.valueOf(arg).longValue();
	    } catch (Exception e) {
		System.out.println("job-size specified was "+arg+
				   " but must be an integer");
		usage(1);
	    }
	    if (jobSize == 0) {
		jobSize = defaultRepetitions;
	    }

	    arg = args[3];
	    if (!((arg.equals("parallel") || arg.equals("sequential")))) {
		usage(1);
	    }
	    boolean parallel = arg.equals("parallel");


	    System.out.println("Number of Runs: "+numJobs);
	    doTest(taskFnSpecifier, numJobs, jobSize, parallel);
	    System.exit(0);
	}
	catch(Exception e) {}
    }
}
