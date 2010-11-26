/* The Computer Language Benchmarks Game
 * http://shootout.alioth.debian.org/
 * original contributed by Kenneth Jonsson
 */

import scala.actors.Actor
import scala.actors.Actor._

class Worker(size: Int) extends Actor {
    private val bytesPerRow = (size + 7) >> 3
    private val maxIterations = 50
    private val limitSquared = 4.0

    // Calculate all pixes for one row [-i..i], the real-part
    // coordinate is constant throughout this method
    private def calcRow(rowNum: Int): (Actor, Int, Array[Byte]) = {
	var rowBitmap = new Array[Byte](bytesPerRow)
	var column = 0
	val ci = 2.0 * rowNum / size - 1.0

	while (column < size) {
	    val cr = 2.0 * column / size - 1.5
	    var zr, tr, zi, ti = 0.0
            var iterations = 0

            do {
		zi = 2 * zr * zi + ci
		zr = tr - ti + cr
		ti = zi * zi
		tr = zr * zr
		iterations += 1
            } while (tr + ti <= limitSquared && iterations < maxIterations)

	    if (tr + ti <= limitSquared)
		rowBitmap(column >> 3) = (rowBitmap(column >> 3)
					  | (0x80 >> (column & 7))).toByte

            column += 1
	}
	return (self, rowNum, rowBitmap)
    }

    def act() {
	while (true) {
	    receive {
		case rowNum: Int =>
		    reply(calcRow(rowNum))
		case "EXIT" =>
		    exit()
	    }
	}
    }
}

class MandelbrotCoordinator(size: Int) extends Actor {

    private var nextRowNum = 0
    private var rowsRemaining = size
    private var bitmap = new Array[Array[Byte]](size)

    private def calcNextRow(worker: Actor) {
	if (nextRowNum == size)
	    // All rows has been dispatched, tell the worker to exit
	    worker ! "EXIT"
	else {
	    worker ! nextRowNum
	    nextRowNum += 1
	}
    }

    def act() {
	for (i <- 1 to Runtime.getRuntime().availableProcessors()) {
	    val worker = new Worker(size)
	    // Keep two rows in flight per worker to avoid any worker
	    // idle time, probably not neccessary on a quad-core
	    // machine but might help at higher core count...
	    calcNextRow(worker)
	    calcNextRow(worker)
	    worker.start
	}

	while (true) {
	    receive {
		case (sender: Actor, rowNum: Int, rowBitmap: Array[Byte]) =>
		    calcNextRow(sender)
		    bitmap(rowNum) = rowBitmap
		    rowsRemaining -= 1
		    if (rowsRemaining == 0) {
			// The image is finished, write it to stdout and exit
			println("P4\n" + size + " " + size)
			bitmap.foreach(row => System.out.write(row, 0, row.length))
			exit()
		    }
	    }
	}
    }
}

object mandelbrot {
    def main(args: Array[String]) {
	val coordinator = new MandelbrotCoordinator(args(0).toInt)
	coordinator.start
    }
}
