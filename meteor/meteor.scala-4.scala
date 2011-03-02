/* The Computer Language Shootout
   http://shootout.alioth.debian.org/
   contributed by Isaac Gouy
   updated for 2.8 by Rex Kerr
*/

// Most for-comprehension replaced by while loops
// BoardCells occupied by each Piece orientation are cached
// Piece orientations are cached

import scala.collection.mutable._

object meteor {
   def main(args: Array[String]) = {
      val solver = new Solver( args(0).toInt )
      solver.findSolutions
      solver.printSolutions
   }
}


final class Solver (n: Int) {
   private var countdown = n
   private var first: String = _
   private var last: String = _

   private val board = new Board()

   val pieces = Array.tabulate(10)(i => new Piece(i))

   val unplaced = new BitSet(pieces.length)

   { unplaced ++= (0 until pieces.length) }


   def findSolutions(): Unit = {
      if (countdown == 0) return

      if (unplaced.size > 0){
         val emptyCellIndex = board.firstEmptyCellIndex

         var k = 0
         while (k < pieces.length){
            if (unplaced.contains(k)){
               unplaced -= k

               var i = 0
               while (i < Piece.orientations){
                  val piece = pieces(k).nextOrientation

                  var j = 0
                  while (j < Piece.size){
                     if (board.add(j,emptyCellIndex,piece)) {

                        if (!shouldPrune) findSolutions

                        board.remove(piece)
                     }
                     j = j + 1
                  }
                  i = i + 1
               }
               unplaced += k
            }
            k = k + 1
         }
      }
      else {
         puzzleSolved
      }
   }

   private def puzzleSolved() = {
      val b = board.asString
      if (first == null){
         first = b; last = b
      } else {
         if (b < first){ first = b } else { if (b > last){ last = b } }
      }
      countdown = countdown - 1
   }

   private def shouldPrune(): Boolean = {
      board.unmark
      var i = 0
      while (i < board.cells.length){
         if (board.cells(i).contiguousEmptyCells % Piece.size != 0) return true
         i = i + 1
      }
      false
   }


   def printSolutions() = {

      def printBoard(s: String) = {
         var indent = false
         var i = 0
         while (i < s.length){
            if (indent) print(' ')
            var j = 0
            while (j < Board.cols){
               print(s.charAt(i)); print(' ')
               j = j + 1
               i = i + 1
            }
            print('\n')
            indent = !indent
         }
         print('\n')
      }

      print(n + " solutions found\n\n")
      printBoard(first)
      printBoard(last)
   }

/*
   def printPieces() =
      for (i <- Iterator.range(0,Board.pieces)) pieces(i).print
*/

}



// Board.scala
// import scala.collection.mutable._

object Board {
   val cols = 5
   val rows = 10
   val size = rows * cols
   val pieces = 10
   val noFit = new Array[BoardCell](0)
}

final class Board {
   val cells = boardCells()

   val cellsPieceWillFill = new Array[BoardCell](Piece.size)
   var cellCount = 0

   def unmark() = {
      var i = 0
      while (i < cells.length){
         cells(i).unmark
         i = i + 1
      }
   }

   def asString() =
      new String( cells map(
         c => if (c.piece == null) '-'.toByte
              else (c.piece.number + 48).toByte ))

   def firstEmptyCellIndex() = cells.findIndexOf(c => c.isEmpty)


   private val cache = Array.fill(
     Board.pieces,Piece.orientations,Piece.size,Board.size
   )(null: Array[BoardCell])

   def add(pieceIndex: Int, boardIndex: Int, p: Piece): Boolean = {
      var a = cache(p.number)(p.orientation)(pieceIndex)(boardIndex)

      cellCount = 0
      p.unmark

      if (a == null){
         find(p.cells(pieceIndex), cells(boardIndex))

         if (cellCount != Piece.size){
            cache(p.number)(p.orientation)(pieceIndex)(boardIndex) = Board.noFit
            return false
         }

         a = cellsPieceWillFill .filter(c => true)
         cache(p.number)(p.orientation)(pieceIndex)(boardIndex) = a
      }
      else {
         if (a == Board.noFit) return false
      }

      var i = 0
      while (i < a.length){
         if (!a(i).isEmpty) return false
         i = i + 1
      }

      i = 0
      while (i < a.length){
         a(i).piece = p
         i = i + 1
      }

      true
   }


   def remove(piece: Piece) = {
      var i = 0
      while (i < cells.length){
         if (cells(i).piece == piece) cells(i).empty
         i = i + 1
      }
   }


   private def find(p: PieceCell, b: BoardCell): Unit = {
      if (p != null && !p.marked && b != null){
         cellsPieceWillFill(cellCount) = b
         cellCount = cellCount + 1
         p.mark

         var i = 0
         while (i < Cell.sides){
            find(p.next(i), b.next(i))
            i = i + 1
         }
      }
   }


   private def boardCells() = {
      val a = Array.tabulate(Board.size)(i => new BoardCell(i))
      val m = (Board.size / Board.cols) - 1

      for (i <- 0 until a.length) {
         val row = i / Board.cols
         val isFirst = i % Board.cols == 0
         val isLast = (i+1) % Board.cols == 0
         val c = a(i)

         if (row % 2 == 1) {
            if (!isLast) c.next(Cell.NE) = a(i-(Board.cols-1))
            c.next(Cell.NW) = a(i-Board.cols)
            if (row != m) {
               if (!isLast) c.next(Cell.SE) = a(i+(Board.cols+1))
               c.next(Cell.SW) = a(i+Board.cols)
            }
         } else {
            if (row != 0) {
               if (!isFirst) c.next(Cell.NW) = a(i-(Board.cols+1))
               c.next(Cell.NE) = a(i-Board.cols)
            }
            if (row != m) {
               if (!isFirst) c.next(Cell.SW) = a(i+(Board.cols-1))
               c.next(Cell.SE) = a(i+Board.cols)
            }
         }
         if (!isFirst) c.next(Cell.W) = a(i-1)
         if (!isLast) c.next(Cell.E) = a(i+1)
      }
      a
   }


/*
// Printing all the board cells and their neighbours
// helps check that they are connected properly

   def printBoardCellsAndNeighbours() = {
      println("cell\tNW NE W  E  SW SE")
      for (i <- 0 until Board.size) {
         print(i + "\t")
         for (j <- 0 until Cell.sides) {
            val c = cells(i).next(j)
            if (c == null)
               print("-- ")
            else
               printf("{0,number,00} ")(c.number)
         }
         println("")
      }
      println("")
   }
*/

}




// Piece.scala

object Piece {
   val size = 5
   val rotations = Cell.sides
   val flips = 2
   val orientations = rotations * flips
}

final class Piece(_number: Int) {
   val number = _number

   def unmark() = {
      val c = cache(orientation)
      var i = 0
      while (i < c.length){
         c(i).unmark
         i = i + 1
      }
   }

   def cells = cache(orientation)

   private val cache = Array.tabulate(Piece.orientations)(pieceOrientation _)

   var orientation = 0

   def nextOrientation() = {
      orientation = (orientation + 1) % Piece.orientations
      this
   }


   private def pieceOrientation(k: Int) = {
      val cells = Array.fill(Piece.size)(new PieceCell())
      makePiece(number,cells)

      var i = 0
      while (i < k){
         if (i % Piece.rotations == 0)
            cells.foreach(_.flip)
         else
            cells.foreach(_.rotate)

         i = i + 1
      }
      cells
   }

   private def makePiece(number: Int, cells: Array[PieceCell]) = {
      number match {
         case 0 => make0(cells)
         case 1 => make1(cells)
         case 2 => make2(cells)
         case 3 => make3(cells)
         case 4 => make4(cells)
         case 5 => make5(cells)
         case 6 => make6(cells)
         case 7 => make7(cells)
         case 8 => make8(cells)
         case 9 => make9(cells)
      }
   }

   private def make0(a: Array[PieceCell]) = {
      a(0).next(Cell.E) = a(1)
      a(1).next(Cell.W) = a(0)
      a(1).next(Cell.E) = a(2)
      a(2).next(Cell.W) = a(1)
      a(2).next(Cell.E) = a(3)
      a(3).next(Cell.W) = a(2)
      a(3).next(Cell.SE) = a(4)
      a(4).next(Cell.NW) = a(3)
   }

   private def make1(a: Array[PieceCell]) = {
      a(0).next(Cell.SE) = a(1)
      a(1).next(Cell.NW) = a(0)
      a(1).next(Cell.SW) = a(2)
      a(2).next(Cell.NE) = a(1)
      a(2).next(Cell.W) = a(3)
      a(3).next(Cell.E) = a(2)
      a(3).next(Cell.SW) = a(4)
      a(4).next(Cell.NE) = a(3)
   }

   private def make2(a: Array[PieceCell]) = {
      a(0).next(Cell.W) = a(1)
      a(1).next(Cell.E) = a(0)
      a(1).next(Cell.SW) = a(2)
      a(2).next(Cell.NE) = a(1)
      a(2).next(Cell.SE) = a(3)
      a(3).next(Cell.NW) = a(2)
      a(3).next(Cell.SE) = a(4)
      a(4).next(Cell.NW) = a(3)
   }

   private def make3(a: Array[PieceCell]) = {
      a(0).next(Cell.SW) = a(1)
      a(1).next(Cell.NE) = a(0)
      a(1).next(Cell.W) = a(2)
      a(2).next(Cell.E) = a(1)
      a(1).next(Cell.SW) = a(3)
      a(3).next(Cell.NE) = a(1)
      a(2).next(Cell.SE) = a(3)
      a(3).next(Cell.NW) = a(2)
      a(3).next(Cell.SE) = a(4)
      a(4).next(Cell.NW) = a(3)
   }

   private def make4(a: Array[PieceCell]) = {
      a(0).next(Cell.SE) = a(1)
      a(1).next(Cell.NW) = a(0)
      a(1).next(Cell.SW) = a(2)
      a(2).next(Cell.NE) = a(1)
      a(1).next(Cell.E) = a(3)
      a(3).next(Cell.W) = a(1)
      a(3).next(Cell.SE) = a(4)
      a(4).next(Cell.NW) = a(3)
   }

   private def make5(a: Array[PieceCell]) = {
      a(0).next(Cell.SW) = a(1)
      a(1).next(Cell.NE) = a(0)
      a(0).next(Cell.SE) = a(2)
      a(2).next(Cell.NW) = a(0)
      a(1).next(Cell.SE) = a(3)
      a(3).next(Cell.NW) = a(1)
      a(2).next(Cell.SW) = a(3)
      a(3).next(Cell.NE) = a(2)
      a(3).next(Cell.SW) = a(4)
      a(4).next(Cell.NE) = a(3)
   }

   private def make6(a: Array[PieceCell]) = {
      a(0).next(Cell.SW) = a(1)
      a(1).next(Cell.NE) = a(0)
      a(2).next(Cell.SE) = a(1)
      a(1).next(Cell.NW) = a(2)
      a(1).next(Cell.SE) = a(3)
      a(3).next(Cell.NW) = a(1)
      a(3).next(Cell.SW) = a(4)
      a(4).next(Cell.NE) = a(3)
   }

   private def make7(a: Array[PieceCell]) = {
      a(0).next(Cell.SE) = a(1)
      a(1).next(Cell.NW) = a(0)
      a(0).next(Cell.SW) = a(2)
      a(2).next(Cell.NE) = a(0)
      a(2).next(Cell.SW) = a(3)
      a(3).next(Cell.NE) = a(2)
      a(3).next(Cell.SE) = a(4)
      a(4).next(Cell.NW) = a(3)
   }

   private def make8(a: Array[PieceCell]) = {
      a(0).next(Cell.E) = a(1)
      a(1).next(Cell.W) = a(0)
      a(1).next(Cell.E) = a(2)
      a(2).next(Cell.W) = a(1)
      a(2).next(Cell.NE) = a(3)
      a(3).next(Cell.SW) = a(2)
      a(3).next(Cell.E) = a(4)
      a(4).next(Cell.W) = a(3)
   }

   private def make9(a: Array[PieceCell]) = {
      a(0).next(Cell.E) = a(1)
      a(1).next(Cell.W) = a(0)
      a(1).next(Cell.E) = a(2)
      a(2).next(Cell.W) = a(1)
      a(2).next(Cell.NE) = a(3)
      a(3).next(Cell.SW) = a(2)
      a(2).next(Cell.E) = a(4)
      a(4).next(Cell.W) = a(2)
      a(4).next(Cell.NW) = a(3)
      a(3).next(Cell.SE) = a(4)
   }

/*
   def printMe() = {
      println("Piece # " + number)
      println("cell\tNW NE W  E  SW SE")
      for (i <- Iterator.range(0,Piece.size)){
         print(i + "\t")
         for (j <- Iterator.range(0,Cell.sides)){
            val c = cells(i).next(j)
            if (c == null)
               print("-- ")
            else
               for (k <- Iterator.range(0,Piece.size)){
                  if (cells(k) == c) printf(" {0,number,0} ")(k)
               }
         }
         println("")
      }
      println("")
   }
*/
}





// Cell.scala

object Cell {
   val NW = 0; val NE = 1
   val W  = 2; val E  = 3
   val SW = 4; val SE = 5

   val sides = 6
}

abstract class Cell {
   var marked = false

   def mark() = marked = true
   def unmark() = marked = false
}




// BoardCell.scala

final class BoardCell(val number: Int) extends Cell {
   val next = new Array[BoardCell](Cell.sides)
   var piece: Piece = _

   def isEmpty() = piece == null
   def empty() = piece = null

   def contiguousEmptyCells(): Int = {
      if (!marked && isEmpty){
         mark
         var count = 1

         var i = 0
         while (i < next.length){
            if (next(i) != null && next(i).isEmpty)
               count = count + next(i).contiguousEmptyCells
            i = i + 1
         }

         count } else { 0 }
   }
}




// PieceCell.scala

final class PieceCell extends Cell {
   val next = new Array[PieceCell](Cell.sides)

   def flip = {
      var swap = next(Cell.NE)
      next(Cell.NE) = next(Cell.NW)
      next(Cell.NW) = swap

      swap = next(Cell.E)
      next(Cell.E) = next(Cell.W)
      next(Cell.W) = swap

      swap = next(Cell.SE)
      next(Cell.SE) = next(Cell.SW)
      next(Cell.SW) = swap
   }

   def rotate = {
      var swap = next(Cell.E)
      next(Cell.E) = next(Cell.NE)
      next(Cell.NE) = next(Cell.NW)
      next(Cell.NW) = next(Cell.W)
      next(Cell.W) = next(Cell.SW)
      next(Cell.SW) = next(Cell.SE)
      next(Cell.SE) = swap
   }
}
