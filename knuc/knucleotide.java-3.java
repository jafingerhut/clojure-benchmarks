/* The Computer Language Benchmarks Game
   http://shootout.alioth.debian.org/

   contributed by Matthieu Bentot
   based on the original by The Anh Tran
 */

import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.*;

public final class knucleotide {

    public static void main (String[] args) {
      try {
         byte source[]=readInput();

           String result[]=new String[7];
           AtomicInteger job=new AtomicInteger(6);

           Thread pool[]=new Thread[Runtime.getRuntime().availableProcessors()];
           for(int i=0;i<pool.length;i++) {
               pool[i]=new ProcessingThread(source, job, result);
               pool[i].start();
           }

           for(Thread t: pool) t.join();

           for(String s: result) System.out.println(s);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(-1);
      }
    }

    private static byte[] readInput() throws IOException {
        BufferedReader reader=new BufferedReader(new InputStreamReader(System.in, "US-ASCII"));

        String s;

        while((s=reader.readLine()) != null) {
            if (s.startsWith(">THREE")) break;
        }

        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        byte line[]=new byte[100];

        while((s=reader.readLine()) != null) {
           if (s.length()>line.length) line=new byte[s.length()];
           int i;
           for(i=0;i<s.length();i++) line[i]=(byte)s.charAt(i);
            baos.write(line, 0, i);
        }

        return baos.toByteArray();
    }

    private static final class ProcessingThread extends Thread {

      private final byte input[];

      private final AtomicInteger job;

      private final String result[];

      private ProcessingThread(byte source[], AtomicInteger job, String result[]) {
         this.input=source;
         this.job=job;
         this.result=result;
      }

      public void run() {
          int j;
          while((j=job.getAndDecrement()) >= 0) {
              switch (j) {
                  case 0:
                      result[j]=writeFreq(1);
                      break;
                  case 1:
                      result[j]=writeFreq(2);
                      break;
                  case 2:
                      result[j]=writeFreq("ggt");
                      break;
                  case 3:
                      result[j]=writeFreq("ggta");
                      break;
                  case 4:
                      result[j]=writeFreq("ggtatt");
                      break;
                  case 5:
                      result[j]=writeFreq("ggtattttaatt");
                      break;
                  case 6:
                      result[j]=writeFreq("ggtattttaatttatagt");
                      break;
                  default:
                      throw new RuntimeException("Invalid task");
              }
          }
      }

       private String writeFreq(int frameSize) {
           ArrayList<Key> result=new ArrayList<Key>(calculateFreq(input, frameSize).keySet());
           Collections.sort(result);

           float totalchar=input.length-frameSize+1;

           StringBuilder sb=new StringBuilder();

           for(Key k: result) {
              for(int i=0;i<k.key.length;i++) sb.append(Character.toUpperCase((char)k.key[i]));
               sb.append(String.format(" %.3f\n", (float)(k.count) * 100.0f / totalchar));
           }

           return sb.toString();
       }

       private String writeFreq(String specific) {
           Key k=new Key(specific.length());
           k.reHash(specific.getBytes(), 0);

           int count=calculateFreq(input, specific.length()).get(k).count;

           return String.format("%d\t%s", count, specific.toUpperCase());
       }

       private static HashMap<Key, Key> calculateFreq(byte input[], int frameSize) {
          HashMap<Key, Key> htb=new HashMap<Key, Key>();

           int end=input.length-frameSize+1;
           Key k=new Key(frameSize);

           for(int i=0;i<end;i++) {
               k.reHash(input, i);

               Key existing=htb.get(k);
               if (existing!=null) {
                  existing.count++;
               } else {
                   htb.put(k, k);
                   k=new Key(frameSize);
               }
           }

           return htb;
       }

   }

   static final class Key implements Comparable<Key> {

        public int hash, count=1;

        public final byte key[];

        public Key(int frame) {
            key=new byte[frame];
        }

        public void reHash(byte k[], int offset) {
           int hash=0;
            for (int i=0;i<key.length;i++) {
               byte b=k[offset+i];
                key[i]=b;
                hash=hash*31+b;
            }
            this.hash=hash;
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object obj) {
            return hash==((Key)obj).hash;
        }

        public int compareTo(Key o) {
            return o.count-count;
        }

    }

}
