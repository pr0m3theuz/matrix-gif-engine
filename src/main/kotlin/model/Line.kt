package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lines (Start Node, End Node): A-J 1-9? A1 - I5 B1 - B6 B1 - J4 C1 - C7 C1 - J3 D1 - D8 D1 - J2
 * E1 - E9 F1 - F8 F1 - A2 G1 - G7 G1 - A3 H1 - H6 H1 - A4 I1 - I5 I1 - A5 J1 - B6
 */
@Serializable
data class Lines(
    val verticalLines: MutableList<Set<Node>>,
    val upwardRightLines: MutableList<Set<Node>>,
    val downwardRightLines: MutableList<Set<Node>>,
) {
  fun getLinesContainingNode(node: Node): Lines {
    return Lines(
        verticalLines =
            mutableListOf(
                this.verticalLines
                    .filter { nodes -> nodes.contains(node) }
                    .flatten()
                    .sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row }))
                    .toSet()
            ),
        upwardRightLines =
            mutableListOf(
                this.upwardRightLines
                    .filter { nodes -> nodes.contains(node) }
                    .flatten()
                    .sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row }))
                    .toSet()
            ),
        downwardRightLines =
            mutableListOf(
                this.downwardRightLines
                    .filter { nodes -> nodes.contains(node) }
                    .flatten()
                    .sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row }))
                    .toSet()
            ),
    )
  }

  fun deepCopy(): Lines {
    val string = Json.encodeToString(serializer(), this)
    return Json.decodeFromString(serializer(), string)
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

  /**
   * ============================================================================
   * MOVEMENT AND INTERACTION RULES: DVONN & PUNCT POTENTIALS
   * ============================================================================
   * * 1. MOVEMENT & JUMPING RESTRICTIONS
   * - A DVONN-potential can ONLY jump onto a DVONN-potential of the OPPOSITE color.
   * - Valid targets include:
   * - A single DVONN-potential on the board.
   * - A DVONN-stack.
   * - A DVONN-potential currently sitting on top of a stack.
   * - Pathing:
   * - Can jump to an adjacent spot.
   * - Can move in a straight line over EMPTY spots (cannot jump over other pieces).
   * - Note: Unlike 'GIPF With Potentials', a DVONN-potential CANNOT jump onto a basic GIPF piece.
   *
   * * 2. STACK MECHANICS
   * - Because players can jump onto opposing DVONN-potentials, stacks of alternating colors will
   *   form.
   * - Unlike a stack of 2 identical/same-color potentials, a multi-color DVONN-stack is NOT treated
   *   as a single entity.
   * - When a multi-color stack is part of a resolved row, ONLY the top piece is removed.
   *
   * * 3. NEUTRALIZATION & OCCUPATION
   * - Target pieces are neutralized when jumped on, and remain neutralized while covered.
   * - The top piece's color controls/occupies that board spot.
   * - When the top piece is removed, the piece directly beneath it is liberated and returns to
   *   active play.
   *
   * * 4. EDGE CASE: ROW-OF-4 RESOLUTION
   * - Removing a top DVONN-potential can un-neutralize the piece beneath it, potentially creating a
   *   new row-of-4.
   * - Removing an opponent's piece to reveal your own can complete your row-of-4.
   * - Removing your own piece to reveal an opponent's can complete their row-of-4.
   * - Turn Resolution Logic:
   * - A player's turn is NOT over if they still have a row that needs removal.
   * - Rows must be removed one at a time.
   * - Active player's turn ends ONLY when no more rows of their color remain.
   * - If an opponent begins their turn with an existing row of their color on the board (caused by
   *   the previous player's move), they MUST remove it before making a standard move.
   */
  lines.forEach { line ->
    val sublists = line.windowed(4).filter { nodes -> nodes.all { it.piece != null } }
    val hasFourPiecesInARow = sublists.any { sublist ->
      sublist.all { node ->
        if (node.piece?.isNeutralized == true) {
          check(node.piece?.type in setOf(PieceType.DVONN, PieceType.PUNCT)) {
            "Only Dvonn and Pünct pieces can be neutralized. \n Node: ${Json.encodeToString<Node>(node)}"
          }
          node.piece?.stackedPieces?.last()?.colorName == player.name
        } else {
          node.piece?.colorName == player.name
        }
      }
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

data class LineScore(
    val line: Set<Node>,
    // TODO status: winning, losing, draw
    // TODO who is winning
    val status: String,
    val score: Float,
)

fun scoreLines(player: Player, lines: List<Set<Node>>) {
	/**
	* TODO heuristics: features are piece mobility, line-control, potential energy of perimeter
  * pieces, number of playable pieces in reserve, capturing opponent gipf and other pieces, central node
  * occupation is only important for Tamsk pieces.¬
  * What specific mathematical features (e.g., piece mobility, line-control, central node
  * occupation, potential energy of perimeter pieces) are you encoding into your static heuristic
  * evaluation function for the Alpha-Beta agent, and what methodology will you use to tune the
  * weights of these features so that the baseline agent is legitimately competitive?
  */

  /**
   * ============================================================================
   * MOVEMENT AND INTERACTION RULES: DVONN & PUNCT POTENTIALS
   * ============================================================================
   * * 1. MOVEMENT & JUMPING RESTRICTIONS
   * - A DVONN-potential can ONLY jump onto a DVONN-potential of the OPPOSITE color.
   * - Valid targets include:
   * - A single DVONN-potential on the board.
   * - A DVONN-stack.
   * - A DVONN-potential currently sitting on top of a stack.
   * - Pathing:
   * - Can jump to an adjacent spot.
   * - Can move in a straight line over EMPTY spots (cannot jump over other pieces).
   * - Note: Unlike 'GIPF With Potentials', a DVONN-potential CANNOT jump onto a basic GIPF piece.
   *
   * * 2. STACK MECHANICS
   * - Because players can jump onto opposing DVONN-potentials, stacks of alternating colors will
   *   form.
   * - Unlike a stack of 2 identical/same-color potentials, a multi-color DVONN-stack is NOT treated
   *   as a single entity.
   * - When a multi-color stack is part of a resolved row, ONLY the top piece is removed.
   *
   * * 3. NEUTRALIZATION & OCCUPATION
   * - Target pieces are neutralized when jumped on, and remain neutralized while covered.
   * - The top piece's color controls/occupies that board spot.
   * - When the top piece is removed, the piece directly beneath it is liberated and returns to
   *   active play.
   *
   * * 4. EDGE CASE: ROW-OF-4 RESOLUTION
   * - Removing a top DVONN-potential can un-neutralize the piece beneath it, potentially creating a
   *   new row-of-4.
   * - Removing an opponent's piece to reveal your own can complete your row-of-4.
   * - Removing your own piece to reveal an opponent's can complete their row-of-4.
   * - Turn Resolution Logic:
   * - A player's turn is NOT over if they still have a row that needs removal.
   * - Rows must be removed one at a time.
   * - Active player's turn ends ONLY when no more rows of their color remain.
   * - If an opponent begins their turn with an existing row of their color on the board (caused by
   *   the previous player's move), they MUST remove it before making a standard move.
   */
  lines.map { line ->
    val currentPlayerPieces = mutableListOf(mutableListOf<Node>())
    val opponentPlayerPieces = mutableListOf(mutableListOf<Node>())

    val result =
        line
            .sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row }))
            .fold(initial = mutableListOf<Node>() to mutableListOf<List<Node>>()) {
                (currentList, allLists),
                currentNode: Node ->
              if (currentList.isEmpty()) {
                mutableListOf(currentNode) to allLists
              } else {
                if (currentList.last().piece?.colorName == currentNode.piece?.colorName) {
                  currentList.apply { add(currentNode) } to allLists
                } else if (currentList.last().piece == currentNode.piece) {
                  currentList.apply { add(currentNode) } to allLists
                } else {
                  mutableListOf(currentNode) to allLists.apply { add(currentList) }
                }
              }
            }
            .let { it.second.apply { add(it.first) } }

    // TODO who is dominating this line
    result.sortByDescending { it.size }

    val grouped = result.groupBy { it.first().piece?.colorName }

    println(grouped.maxBy { it.value.size })

    result
  }
}
