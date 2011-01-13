import java.text.DecimalFormat;



public class ParallelTest {

    static int CacheBuster1_data[];

    public static class IntTest implements Runnable {
	long jobSizePerThread;
	int result;
	
	public IntTest(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    result = 0;
	    for (long i = 0L; i < jobSizePerThread; i++) {
		result = result + 1;
	    }
	    System.out.println(result);
	}
    }

    public static class LongTest implements Runnable {
	long jobSizePerThread;
	long result;
	
	public LongTest(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    result = 0;
	    for (long i = 0L; i < jobSizePerThread; i++) {
		result = result + 1;
	    }
	    System.out.println(result);
	}
    }
    
    public static class DoubleTest implements Runnable {
	long jobSizePerThread;
	double result;
	
	public DoubleTest(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    result = 0.0;
	    for (long i = 0L; i < jobSizePerThread; i++) {
		result = result + 1.0;
	    }
	    System.out.println(result);
	}
    }
    
    public static class NewDoubleTestA implements Runnable {
	long jobSizePerThread;
	Double result;
	
	public NewDoubleTestA(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    result = new Double(0.0);
	    for (long i = 0L; i < jobSizePerThread; i++) {
		result = new Double(result.doubleValue() + 1.0);
	    }
	    System.out.println(result);
	}
    }
    
    public static class NewDoubleTestAInt2 implements Runnable {
	long jobSizePerThread;
	Double result;
	int prng_state;
	
	public NewDoubleTestAInt2(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    result = new Double(0.0);
	    prng_state = 42;
	    for (long i = 0L; i < jobSizePerThread; i++) {
		result = new Double(result.doubleValue() + 1.0);
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
	    }
	    System.out.println("result=" + result);
	    System.out.println("prng_state=" + prng_state);
	}
    }
    
    public static class NewDoubleTestAInt4 implements Runnable {
	long jobSizePerThread;
	Double result;
	int prng_state;
	
	public NewDoubleTestAInt4(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    result = new Double(0.0);
	    prng_state = 42;
	    for (long i = 0L; i < jobSizePerThread; i++) {
		result = new Double(result.doubleValue() + 1.0);
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
	    }
	    System.out.println("result=" + result);
	    System.out.println("prng_state=" + prng_state);
	}
    }
    
    public static class NewDoubleTestAInt8 implements Runnable {
	long jobSizePerThread;
	Double result;
	int prng_state;
	
	public NewDoubleTestAInt8(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    result = new Double(0.0);
	    prng_state = 42;
	    for (long i = 0L; i < jobSizePerThread; i++) {
		result = new Double(result.doubleValue() + 1.0);
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
	    }
	    System.out.println("result=" + result);
	    System.out.println("prng_state=" + prng_state);
	}
    }
    
    public static class NewDoubleTestAInt16 implements Runnable {
	long jobSizePerThread;
	Double result;
	int prng_state;
	
	public NewDoubleTestAInt16(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    result = new Double(0.0);
	    prng_state = 42;
	    for (long i = 0L; i < jobSizePerThread; i++) {
		result = new Double(result.doubleValue() + 1.0);
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
		prng_state = (3877 * prng_state + 29573) % 139968;
	    }
	    System.out.println("result=" + result);
	    System.out.println("prng_state=" + prng_state);
	}
    }
    
    public static class NewDoubleTestB implements Runnable {
	long jobSizePerThread;
	Double result;
	
	public NewDoubleTestB(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    result = new Double(0.0);
	    for (long i = 0L; i < jobSizePerThread; i++) {
		double old = result.doubleValue() + 1.0;
		result = new Double(old);
	    }
	    System.out.println(result);
	}
    }
    
    public static class NewDoubleTestC implements Runnable {
	long jobSizePerThread;
	Double result;
	
	public NewDoubleTestC(long jobSizePerThread) {
	    this.jobSizePerThread = jobSizePerThread;
	}
	
	public void run() {
	    for (long i = 0L; i < jobSizePerThread; i++) {
		result = new Double((double) i);
	    }
	    System.out.println(result);
	}
    }

    static String[] initTypeList() {
	String[] typeList = new String[11];
	typeList[ 0] = "int";
	typeList[ 1] = "long";
	typeList[ 2] = "double";
	typeList[ 3] = "newdoubleA";
	typeList[ 4] = "newdoubleAInt2";
	typeList[ 5] = "newdoubleAInt4";
	typeList[ 6] = "newdoubleAInt8";
	typeList[ 7] = "newdoubleAInt16";
	typeList[ 8] = "newdoubleB";
	typeList[ 9] = "newdoubleC";
	typeList[10] = "cachebuster1";
	return typeList;
    }

    static String typeListToStr(String[] typeList) {
	int i;
	String a = new String();
	for (i = 0; i < typeList.length; i++) {
	    if (i != 0) {
		a = a + ",";
	    }
	    a = a + typeList[i];
	}
	return a;
    }

    static boolean legalType(String s, String[] typeList) {
	int i;
	for (i = 0; i < typeList.length; i++) {
	    if (s.equals(typeList[i]))
		return true;
	}
	return false;
    }

    static Thread newThreadWithType (String s, int threadNum,
				     long jobSizePerThread) {
	CacheBuster1 c;
	if (s.equals("int"))
	    return new Thread(new IntTest(jobSizePerThread));
	else if (s.equals("long"))
	    return new Thread(new LongTest(jobSizePerThread));
	else if (s.equals("double"))
	    return new Thread(new DoubleTest(jobSizePerThread));
	else if (s.equals("newdoubleA"))
	    return new Thread(new NewDoubleTestA(jobSizePerThread));
	else if (s.equals("newdoubleAInt2"))
	    return new Thread(new NewDoubleTestAInt2(jobSizePerThread));
	else if (s.equals("newdoubleAInt4"))
	    return new Thread(new NewDoubleTestAInt4(jobSizePerThread));
	else if (s.equals("newdoubleAInt8"))
	    return new Thread(new NewDoubleTestAInt8(jobSizePerThread));
	else if (s.equals("newdoubleAInt16"))
	    return new Thread(new NewDoubleTestAInt16(jobSizePerThread));
	else if (s.equals("newdoubleB"))
	    return new Thread(new NewDoubleTestB(jobSizePerThread));
	else if (s.equals("newdoubleC"))
	    return new Thread(new NewDoubleTestC(jobSizePerThread));
	else if (s.equals("cachebuster1")) {
	    c = new CacheBuster1(threadNum, jobSizePerThread);
	    if (threadNum == 0) {
		CacheBuster1_data = c.initReadData();
	    }
	    c.setData(CacheBuster1_data);
	    return new Thread(c);
	} else
	    usage(1, null);
	return null;
    }

    static void doTest(String taskFnSpecifier, int numThreads,
		       long jobSizePerThread) {
	final int NTRIALS = 10;
	long[] runTime = new long[NTRIALS];

	for (int trial = 0; trial < NTRIALS; trial++) {
	    try {
		Thread[] tarray = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
		    tarray[i] =
			newThreadWithType(taskFnSpecifier, i, jobSizePerThread);
		}
		long startTime = System.nanoTime();
		for (int i = 0; i < numThreads; i++)
		    tarray[i].start();
		for (int i = 0; i < numThreads; i++)
		    tarray[i].join();
		runTime[trial] = System.nanoTime() - startTime;
	    }
	    catch (Exception e) { }
	}
	long minTime = Long.MAX_VALUE;
	long maxTime = 0;
	long totalTime = 0;
	for (int trial = 1; trial < NTRIALS; trial++) {  // Note: skip first one
	    minTime = Math.min(minTime, runTime[trial]);
	    maxTime = Math.max(maxTime, runTime[trial]);
	    totalTime += runTime[trial];
	}

	System.out.println();
	System.out.println(taskFnSpecifier + " ALL THREADS FINISHED.");
	System.out.print(taskFnSpecifier);
	System.out.format(" ELAPSED Times (msec): min=%.3f  max=%.3f  avg=%.3f\n",
			  ((double) minTime) / 1000000.0,
			  ((double) maxTime) / 1000000.0,
			  (((double) totalTime) / (NTRIALS-1)) / 1000000.0);
    }

    static long defaultJobSize = 1000000000L;
    
    static void usage(int exitCode, String[] typeList) {
	String programName = "ParallelTest";
	System.out.println("usage: " + programName + " type job-size num-threads");
	System.out.println("    type must be one of: " + typeListToStr(typeList));
  	System.out.println("    all other arguments must be integers >= 1");
  	System.out.println("    job-size is the total number of steps that will be performed by all threads");
  	System.out.println("        0 means to use the default number of steps: "
			   +defaultJobSize);
  	System.out.println("    num-threads must be >= 1, and is the number of threads to run in parallel.");
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
	    String[] typeList = initTypeList();
	    if (args.length != 3) {
		System.out.println("Expected 3 args but found " + args.length);
		usage(1, typeList);
	    }
	    String taskFnSpecifier = args[0];
	    if (!legalType(taskFnSpecifier, typeList)) {
		System.out.println("Type specified was " + taskFnSpecifier +
				   " but must be one of: " +
				   typeListToStr(typeList));
	    }

	    String arg = args[1];
	    long jobSize = 0;
	    try {
		jobSize = Long.valueOf(arg).longValue();
	    } catch (Exception e) {
		System.out.println("job-size specified was " + arg +
				   " but must be an integer");
		usage(1, typeList);
	    }
	    if (jobSize == 0) {
		jobSize = defaultJobSize;
	    }

	    arg = args[2];
	    int numThreads = 0;
	    try {
		numThreads = Integer.valueOf(arg).intValue();
	    } catch (Exception e) {
		System.out.println("num-threads specified was " + arg +
				   " but must be an integer");
		usage(1, typeList);
	    }
	    if (numThreads < 1) usage(1, typeList);

	    long jobSizePerThread = jobSize / numThreads;
	    if (jobSizePerThread == 0) {
		jobSizePerThread = 1;
	    }
	    System.out.println("Number of Threads: " + numThreads);
	    doTest(taskFnSpecifier, numThreads, jobSizePerThread);
	    System.exit(0);
	}
	catch(Exception e) {}
    }
}
