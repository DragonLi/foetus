package foetus

import ast._
import calls._

object ordering {

  val debug = false

  def log(msg: String) {
    if (debug) println(msg)
  }

  private object Elem {
    val zero: Relation = RelUnknown
    val one: Relation = RelEqual
    def add(r1: Relation, r2: Relation): Relation = (r1, r2) match {
      case (RelLess, _) => RelLess
      case (_, RelLess) => RelLess
      case (RelEqual, _) => RelEqual
      case (RelUnknown, RelEqual) => RelEqual
      case (RelUnknown, RelUnknown) => RelUnknown
    }
    def mult(r1: Relation, r2: Relation): Relation = (r1, r2) match {
      case (RelUnknown, _) => RelUnknown
      case (_, RelUnknown) => RelUnknown
      case (RelLess, _) => RelLess
      case (_, RelLess) => RelLess
      case (RelEqual, RelEqual) => RelEqual
    }
  }

  object CallMatrixOps {
    type Row = List[Relation]

    def transpose(m: CallMatrix): CallMatrix = m match {
      case Nil => Nil
      case Nil :: _ => Nil
      case rows => rows.map(_.head) :: transpose(rows.map(_.tail))
    }

    private def mult0(rows: CallMatrix, cols: CallMatrix): CallMatrix =
      rows.map { row => cols.map { col => (row, col).zipped.map(Elem.mult).foldLeft(Elem.zero)(Elem.add) } }

    def mult(m1: CallMatrix, m2: CallMatrix): CallMatrix = {
      mult0(m1, transpose(m2))
    }

    def diag(m: CallMatrix) =
      for (i <- m.indices.toList) yield m(i)(i)
  }

  /**
   *
   * @param defs - definitions to order
   * @return a list of pairs (f -> Option[Order])
   */
  def orderDefs(defs: List[Def]): List[(String, Option[List[Int]])] = {
    val graph: CallGraph = callGraph(defs)
    val callers: List[String] = defs.map(_.name)
    val completeGraph = saturate(graph)
    callers.map { f =>
      val matrices: List[CallMatrix] = circles(completeGraph, f)
      val paramOrders: List[List[Relation]] =
        matrices.map(CallMatrixOps.diag)
      val order = lexical.lexicalOrder(paramOrders)
      order match {
        case None =>
          log(s"$f FAILS termination check")
        case Some(Nil) =>
          log(s"$f PASSES termination check")
        case Some(ord) =>
          log(s"$f PASSES termination check (by lexical order ${ord.mkString(", ")})")
      }
      f -> order
    }
  }

  // transitive closure of a graph
  private def saturate(calls: CallGraph): CallGraph = {
    var total, total1 = calls
    do {
      total = total1
      total1 = (total ++ compose(total, calls)).distinct
    } while (total != total1)
    total1
  }

  private def compose(cg1: CallGraph, cg2: CallGraph): CallGraph =
    for {
      Call(caller1, callee1, callMat1) <- cg1
      Call(caller2, callee2, callMat2) <- cg2 if callee1 == caller2
    } yield Call(caller1, callee2, CallMatrixOps.mult(callMat2, callMat1))

  private def circles(graph: CallGraph, f: String): List[CallMatrix] =
    for (Call(`f`, `f`, m) <- graph) yield m
}
