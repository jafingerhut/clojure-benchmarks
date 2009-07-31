/*
   The Computer Language Benchmarks Game
   http://shootout.alioth.debian.org/
   contributed by The Anh Tran
 */


import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.*;


public class regexdna
{
    // source data is duplicated into 2 arrays
    static ArrayList<StringBuilder> source_as_segments = new ArrayList<StringBuilder>();;
    static ArrayList<StringBuilder> source_as_lines = new ArrayList<StringBuilder>();;

    // read data from stdin to StringBuilder
    // return initial data size
    private static int ReadInput(StringBuilder sb)
    {
        try
        {
            BufferedReader reader = new BufferedReader (new InputStreamReader (System.in, "US-ASCII"));

            char[] buf = new char[64 *1024];
            int read = 0, total = 0;

            while ((read = reader.read (buf)) != -1)
            {
                total += read;
                sb.append (buf, 0, read);
            }

            return total;
        }
        catch (IOException ie)
        {
            ie.printStackTrace ();
        }

        return 0;
    }

    // strip header and newline
    // duplicate each data line into 2 arrays
    private static int StripHeader(StringBuilder sb)
    {
        Pattern pat = Pattern.compile("(>.*\n)|\n");
        Matcher mt = pat.matcher(sb);   // scan all data

        StringBuilder desti = null;
        StringBuffer tmp = new StringBuffer();

        while (mt.find())
        {
            mt.appendReplacement(tmp, "");

            if (mt.start(1) >= 0)   // this is header line
            {
                desti = new StringBuilder();    // alloc new dna sequence
                source_as_segments.add(desti);
            }

            desti.append(tmp);  // append this line to current dna sequence
            source_as_lines.add(new StringBuilder(tmp));    // also append this line to 2nd array

            // reset buffer len, re-use in next match
            tmp.setLength(0);
        }

        int strip_len = 0;
        for (StringBuilder b : source_as_segments)
            strip_len += b.length();

        return strip_len;
    }

    private static void CountMatch()
    {
        final String[] patterns =
        {   "agggtaaa|tttaccct" ,
            "[cgt]gggtaaa|tttaccc[acg]",
            "a[act]ggtaaa|tttacc[agt]t",
            "ag[act]gtaaa|tttac[agt]ct",
            "agg[act]taaa|ttta[agt]cct",
            "aggg[acg]aaa|ttt[cgt]ccct",
            "agggt[cgt]aa|tt[acg]accct",
            "agggta[cgt]a|t[acg]taccct",
            "agggtaa[cgt]|[acg]ttaccct"
        };

        final AtomicIntegerArray results = new AtomicIntegerArray(patterns.length);
        final AtomicIntegerArray tasks = new AtomicIntegerArray(patterns.length);

        Thread[] pool = new Thread[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < pool.length; i++)
        {
            pool[i] = new Thread()
            {
                public void run()
                {
                    // for each search pattern
                    for (int pt = 0; pt < patterns.length; pt++)
                    {
                        Pattern expression = Pattern.compile(patterns[pt]);

                        int total_seg = source_as_segments.size();
                        int seq;
                        Matcher mt = expression.matcher("");

                        // fetch not-yet-processed sequence
                        while ((seq = tasks.getAndIncrement(pt)) < total_seg)
                        {
                            mt.reset(source_as_segments.get(seq));

                            while (mt.find())
                                results.incrementAndGet(pt);
                        }
                    }
                }
            };
            pool[i].start();
        }

        // wait for result
        for (Thread t : pool)
        {
            try
            {
                t.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        for (int i = 0; i< patterns.length; i++)
        {
            System.out.format("%s %d\n", patterns[i], results.get(i));
        }
    }

    private static int Replace()
    {
        final String[] pat_search =
        {
            "W", "Y", "K", "M",
            "S", "R", "B", "D",
            "V", "H", "N"
        };
        final String[] pat_replace =
        {
            "(a|t)", "(c|t)", "(g|t)", "(a|c)",
            "(c|g)", "(a|g)", "(c|g|t)", "(a|g|t)",
            "(a|c|g)", "(a|c|t)", "(a|c|g|t)"
        };

        final AtomicIntegerArray tasks = new AtomicIntegerArray(pat_search.length);
        final AtomicIntegerArray result = new AtomicIntegerArray(pat_search.length);

        Thread[] pool = new Thread[Runtime.getRuntime().availableProcessors()];
        final CyclicBarrier barrier = new CyclicBarrier(pool.length);

        for (int i = 0; i < pool.length; i++)
        {
            pool[i] = new Thread()
            {
                public void run()
                {
                    StringBuffer des_buf = new StringBuffer();

                    for (int pt = 0; pt < pat_search.length; pt++)
                    {
                        Pattern pattern = Pattern.compile(pat_search[pt]);
                        Matcher m = pattern.matcher("");

                        int total_line = source_as_lines.size();
                        int line;

                        while ((line = tasks.getAndIncrement(pt)) < total_line)
                        {
                            StringBuilder src_buf = source_as_lines.get(line);
                            m.reset(src_buf);
                            boolean change = false;

                            while (m.find())
                            {
                                m.appendReplacement(des_buf, pat_replace[pt]);
                                change = true;
                            }

                            if (change)
                            {
                                m.appendTail(des_buf);
                                src_buf.setLength(0);
                                src_buf.append(des_buf);
                            }

                            if (pt == (pat_search.length -1))
                                result.addAndGet(pt, src_buf.length());

                            des_buf.setLength(0);
                        }

                        try
                        {
                            barrier.await();
                        }
                        catch (Exception ie)
                        {
                            ie.printStackTrace();
                        }
                    }
                }
            };

            pool[i].start();
        }

        for (Thread t : pool)
        {
            try
            {
                t.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        int replace_len = 0;
        for (int i = 0; i < result.length(); i++)
            replace_len += result.get(i);
        return replace_len;
    }

    public static void main (String[] args)
    {
        StringBuilder sb = new StringBuilder ();
        int init_len = ReadInput(sb);

        int strip_len = StripHeader(sb);
        sb = null;

        CountMatch();
        source_as_segments = null;

        int replace_len = Replace();
        source_as_lines = null;

        System.out.format("\n%d\n%d\n%d\n", init_len, strip_len, replace_len);
    }
}
