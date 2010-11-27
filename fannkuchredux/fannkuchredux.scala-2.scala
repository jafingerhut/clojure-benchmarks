/*
 * The Computer Language Benchmarks Game
 * http://shootout.alioth.debian.org/
 *
 * Scala version contributed by Rex Kerr
 * translated from Java version by Oleg Mazurov, June 2010
 * 
 */

object fannkuchredux {
  def fac(x: Int): Long = if (x < 2) 1L else x*fac(x-1)
  val F = (0 to 20).map(fac).toArray
  var chunk = 0L
  var ntasks = 0
  val taskId = new java.util.concurrent.atomic.AtomicInteger(0)
    
  class Fannkuch(n: Int) extends Thread {
    val p, pp, count = new Array[Int](n)
    var flips, cksum = 0

    def direct(idx: Long, i: Int) {
       if (i > 0) {
        val d = (idx / F(i)).toInt
        count(i) = d
        var j = 0
        while (j < d) { pp(j) = p(j); j += 1 }
        j = 0
        while (j+d <= i) { p(j) = p(j+d); j += 1 }
        while (j <= i) { p(j) = pp(j+d-i-1); j += 1 }
        direct(idx%F(i), i-1)
      }
    }
      
    def permute() {
      var first = p(1)
      p(1) = p(0)
      p(0) = first
      var i = 1
      count(i) += 1
      while (count(i) > i ) {
        count(i) = 0
        i += 1
        p(0) = p(1)
        val next = p(1)
        var j = 1
        while (j < i) { p(j) = p(j+1); j += 1 }
        p(i) = first
        first = next
        count(i) += 1
      }
    }

    def fcount() = {
      var flips = 1
      var first = p(0)
      if (p(first) != 0) {
        var i = 0
        while (i < n) { pp(i) = p(i); i += 1 }
        do {
          flips += 1
          var lo = 1
          var hi = first -1
          while (lo < hi) {
            val t = pp(lo)
            pp(lo) = pp(hi)
            pp(hi) = t
            lo += 1
            hi -= 1
          }
          val t = pp(first)
          pp(first) = first
          first = t
        } while (pp(first) != 0);
      }
      flips
    }
    
    def runTask(task: Int) {
      val lo = task*chunk
      val hi = F(n) min (lo+chunk)
      var j = 0
      while (j < p.length) { p(j) = j; j += 1 }
      direct(lo,p.length-1)
      var i = lo
      while (true) {
        if (p(0) != 0) {
          val f = fcount
          flips = Math.max(flips,f)
          cksum += (if ((i%2)==0) f else -f)
        }
        i += 1
        if (i == hi) return
        permute
      }
    }
    
    override def run() { while (true) {
      val task = taskId.getAndIncrement()
      if (task >= ntasks) return
      runTask(task)
    }}
  }
    
  def announce(n: Int, f: Int, ck: Int) {
    printf("%d\nPfannkuchen(%d) = %d\n",ck,n,f)
  }
  
  def main(args: Array[String]) {
    val n = (if (args.length > 0) args(0).toInt else 7)
    if (n < 0 || n > 20) announce(n,-1,-1)
    else if (n <= 1) announce(n,0,0)
    else {
      val nthreads = Runtime.getRuntime.availableProcessors
      def split(i: Long) = (F(n)+i-1)/i
      chunk = split(nthreads*50)
      ntasks = split(chunk).toInt
      val threads = Array.range(0,nthreads).map(_ => new Fannkuch(n))
      threads.foreach(_.start)
      threads.foreach(_.join)
      announce(n, (0/:threads)(_ max _.flips), (0/:threads)(_ + _.cksum))
    }
  }
}

