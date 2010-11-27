/* The Computer Language Benchmarks Game
   http://shootout.alioth.debian.org/
   contributed by Isaac Gouy
   modified by Meiko Rachimow
   updated for 2.8 by Rex Kerr
*/

object spectralnorm {
  def main(args: Array[String]) = {
    val n = (if (args.length>0) args(0).toInt else 100)
    printf("%.09f\n", (new SpectralNorm(n)).approximate())
  }
}

class SpectralNorm(n: Int) {

  // Ordinary and transposed versions of infinite matrix
  val A = (i: Int, j: Int) => 1.0/((i+j)*(i+j+1)/2 +i+1)
  val At = (j: Int, i: Int) => 1.0/((i+j)*(i+j+1)/2 +i+1)

  // Matrix multiplication w <- M*v
  def mult(v: Array[Double], w: Array[Double], M: (Int,Int)=> Double ) {
    var i = 0
    while (i < n) {
     var s = 0.0
     var j = 0
     while (j < n) { s += M(i,j)*v(j); j += 1 }
     w(i) =  s
     i += 1
    }
  }

  def approximate() = {
    val u,v,w = Array.fill(n)(1.0)

    var i = 0
    while (i < 10) {
      // Multiply by matrix & transpose
      mult(u,w,A)
      mult(w,v,At)
      mult(v,w,A)
      mult(w,u,At)
      i += 1
    }

    var vbv,vv = 0.0
    i = 0
    while (i < n) {
      vbv += u(i)*v(i)
      vv += v(i)*v(i)
      i += 1
    }

    math.sqrt(vbv/vv)
  }
}
