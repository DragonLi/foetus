package foetus

object lexical {
  import calls._

  // finds a lexical ordering
  def lexicalOrder(recCalls: List[List[Relation]]): Option[List[Int]] = recCalls match {
      case Nil | Nil :: _ =>
        Some(List())
      case call :: _ =>
        combine(call.indices.toList).find { is => recCalls.forall { call => isDecreasing(is.map(call(_))) }}
    }

  private def combine(in: List[Int]) =
    for {
      len <- 1 to in.length
      comb <- in.combinations(len)
      combPerm <- comb.permutations
    } yield combPerm

  private def isDecreasing(xs: List[Relation]): Boolean = xs match {
    case Nil | RelUnknown :: _ => false
    case RelLess :: xs1 => true
    case RelEqual :: xs1 => isDecreasing(xs1)
  }
}