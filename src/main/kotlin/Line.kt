package org.example

// TODO Needs to be reconstructed after each action
/**
 * Lines (Start Node, End Node): A-J 1-9? A1 - I5 B1 - B6 B1 - J4 C1 - C7 C1 - J3 D1 - D8 D1 - J2
 * E1 - E9 F1 - F8 F1 - A2 G1 - G7 G1 - A3 H1 - H6 H1 - A4 I1 - I5 I1 - A5 J1 - B6
 */
data class Lines(
    val verticalLines: MutableList<Set<Node>>,
    val upwardRightLines: MutableList<Set<Node>>,
    val downwardRightLines: MutableList<Set<Node>>,
) {
  fun getLinesContainingNode(node: Node): Lines {
    return Lines(
	    verticalLines = mutableListOf(this.verticalLines
		    .filter { nodes -> nodes.contains(node) }
		    .flatten()
		    .sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row })).toSet()),
	    upwardRightLines = mutableListOf(this.upwardRightLines
			    .filter { nodes -> nodes.contains(node) }
			    .flatten()
			    .sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row })).toSet()),
	    downwardRightLines = mutableListOf(this.downwardRightLines
		    .filter { nodes -> nodes.contains(node) }
		    .flatten()
		    .sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row })).toSet())
    )
  }
}

fun Lines.toList(): MutableList<MutableList<Set<Node>>> {
  return mutableListOf(
      this.verticalLines,
      this.upwardRightLines,
      this.downwardRightLines,
  )
}

val LETTERS = "ABCDEFGHIJ"
val ROWS = 1..9 // TODO as variable for board size

fun constructLines(nodes: Set<Node>): Lines {
  val rows = 2..8 // TODO as variable for board size
  val spots = nodes.filter { it.isSpot }.toSet()

  // construct vertical lines playable area
  val verticalLines: MutableList<Set<Node>> =
      LETTERS.slice(1 until LETTERS.length.minus(1))
          .map { column ->
            val lines = spots.filter { node -> node.coordinate.column == column }
            //            lines.subList(1, lines.size).toSet()
            lines.toSet()
          }
          .toMutableList()

  // construct upward lines
  val upwardLines: MutableList<Set<Node>> =
      spots
          .filter { it.neighbors?.lowerLeft == null }
          .map { node ->
            val upperRightNodes = mutableSetOf<Node>()
            val upperRightCoordinate = node.neighbors?.upperRight

            val result =
                getUpperRightNode(
                    upperRightCoordinate,
                    spots,
                    upperRightNodes,
                )

            // TODO is result == upperRightNodes

            setOf(node).plus(result)
          }
          .toMutableList()

  // construct downward lines
  val downwardLines: MutableList<Set<Node>> =
      spots
          .filter { it.neighbors?.upperLeft == null }
          .map { node ->
            val lowerRightNodes = mutableSetOf<Node>()
            val lowerRightCoordinate = node.neighbors?.lowerRight

            val result: MutableSet<Node> =
                getLowerRightNode(
                    coordinate = lowerRightCoordinate,
                    nodes = spots,
                    lowerRightNodes = lowerRightNodes,
                )

            // TODO is result == upperRightNodes

            setOf(node).plus(result)
          }
          .toMutableList()

  return Lines(
      verticalLines = verticalLines,
      upwardRightLines = upwardLines,
      downwardRightLines = downwardLines,
  )
}

fun getUpperRightNode(
    coordinate: Coordinate?,
    nodes: Set<Node>,
    upperRightNodes: MutableSet<Node>,
): MutableSet<Node> {
  val node = nodes.firstOrNull { node -> node.coordinate == coordinate }

  if (node == null) {
    return upperRightNodes
  }

  upperRightNodes.add(node)
  // TODO recursive stack overflow
  getUpperRightNode(node.neighbors?.upperRight, nodes, upperRightNodes)

  return upperRightNodes
}

fun getLowerRightNode(
    coordinate: Coordinate?,
    nodes: Set<Node>,
    lowerRightNodes: MutableSet<Node>,
): MutableSet<Node> {
  val node = nodes.firstOrNull { node -> node.coordinate == coordinate }

  if (node == null) {
    return lowerRightNodes
  }

  lowerRightNodes.add(node)

  getLowerRightNode(node.neighbors?.lowerRight, nodes, lowerRightNodes)

  return lowerRightNodes
}

fun evaluateLines(player: Player, lines: List<Set<Node>>): Pair<Boolean, Set<Node>> {
  lines.forEach { line ->
    val sublists = line.windowed(4)
    val hasFourPiecesInARow = sublists.any { sublist ->
      sublist.all { node -> node.piece?.color == player.color }
    }

    if (hasFourPiecesInARow) {
      return Pair(true, line)
    }
  }

  return Pair(false, setOf())
}

fun Lines.getLinesWithSpaces(): Lines {
  //  val lines = mutableListOf<Set<Node>>()
  //
  //  lines.addAll(this.verticalLines.filter { line -> line.any { it.piece == null } })
  //  lines.addAll(this.upwardRightLines.filter { line -> line.any { it.piece == null } })
  //  lines.addAll(this.downwardRightLines.filter { line -> line.any { it.piece == null } })

  return Lines(
      verticalLines =
          this.verticalLines.filter { line -> line.any { it.piece == null } }.toMutableList(),
      upwardRightLines =
          this.upwardRightLines.filter { line -> line.any { it.piece == null } }.toMutableList(),
      downwardRightLines =
          this.downwardRightLines.filter { line -> line.any { it.piece == null } }.toMutableList(),
  )
}
