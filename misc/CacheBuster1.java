public class CacheBuster1 implements Runnable {
    MyPRNG prng;
    long jobSize;
    int data[];
    int threadNum;
    
    static final int readDataSizeMBytes = 256;
    static final int readDataArraySize =
	((readDataSizeMBytes * (1 << 20)) / 4) - 1;

    public class MyPRNG {
	int m;

	//	public MyPRNG(int init1, int init2) {
	//	    m_w = init1;
	//	    m_z = init2;
	//	}
 
	//	int get_random()
	//	{
	//	    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
	//	    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
	//	    return (m_z << 16) + m_w;  /* 32-bit result */
	//	}

	public MyPRNG(int init1) {
	    m = init1;
	}
	
	int get_random()
	{
	    // According to the following web page, this is the PRNG
	    // in use in some version of glibc.
	    // http://en.wikipedia.org/wiki/Linear_congruential_generator
	    m = 1103515245 * m + 12345;
	    return (m & 0x7fffffff);
	}
    }
    
    public int[] initReadData() {
	MyPRNG initPRNG = new MyPRNG(1784);
	System.err.println("readDataSizeMBytes = " + readDataSizeMBytes);
	System.err.println("readDataArraySize = " + readDataArraySize);
	int readData[] = new int[readDataArraySize];
	int i;
	for (i = 0; i < readDataArraySize; i++) {
	    readData[i] = initPRNG.get_random();
	}
	return readData;
    }
    
    public CacheBuster1(int threadNum, long jobSize) {
	this.prng = new MyPRNG(threadNum);
	this.threadNum = threadNum;
	this.jobSize = jobSize;
    }
    
    public void setData(int data[]) {
	this.data = data;
    }
    
    public void run() {
	int result = 0;
	long negcount = 0;
	int idx;
	int dataSize = data.length;
	for (long i = 0L; i < jobSize; i++) {
	    idx = (prng.get_random() % dataSize);
	    //	    // Handle negative int's returned by get_random()
	    //	    if (idx < 0) {
	    //		idx += dataSize;
	    //		++negcount;
	    //	    }
	    result += data[idx];
	}
	System.out.println("Thread " + threadNum + " result = " + result +
			   " negcount = " + negcount);
    }
}
