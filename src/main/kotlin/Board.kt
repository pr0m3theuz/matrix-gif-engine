package org.example

import kotlin.collections.sortedWith
import kotlin.math.abs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Board(
    val nodes: Set<Node>,
    val centerNodeCoordinate: Coordinate = Coordinate(column = 'E', row = 5),
) {
  fun deepCopy(): Board {
    val string = Json.encodeToString(serializer(), this)
    return Json.decodeFromString(serializer(), string)
  }
}

fun addNewPiece(piece: Piece, coordinate: Coordinate, board: Board): Board {
  val dots = board.nodes.filter { it.isDot }
  val coordinates = dots.map { it.coordinate }

  if (coordinate !in coordinates) {
    return board
  }

  val node = dots.first { it.coordinate == coordinate }

  // TODO Is there vacant space in the connected lines?

  val adjacentCoordinates = node.neighbors?.getNeighbours() ?: emptyList()

  node.piece = piece

  val newBoard = updateBoard(node, board)

  return newBoard
}

fun updateBoard(node: Node, board: Board): Board {

  val nodes = board.deepCopy().nodes.toMutableSet()
  val nodeToRemove = board.nodes.first { it.coordinate == node.coordinate }
  nodes.remove(nodeToRemove)
  nodes.add(node)

  return Board(
      nodes = nodes.sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row })).toSet(),
      centerNodeCoordinate = board.centerNodeCoordinate,
  )
}

// TODO FIX Bug where nodes containing stacked pieces are being replaced instead of stack pieces
// being removed
fun Board.removePieces(nodes: List<RetrievedCapturedPieceNode>): Board {
  val oldActivePiecesCount =
      this.nodes.count { it.piece != null } +
          this.nodes.sumOf { it.piece?.stackedPieces?.size ?: 0 }
  val newNodes = this.deepCopy().nodes.toMutableList()

  val updatedNodes =
      nodes
          .filter { !it.keepRetrievedPieceInPlay }
          .map { node ->
            val updatedNode = newNodes.first { it.coordinate == node.node?.coordinate }

	          if (node.isStackedPieceNode) {
              updatedNode.piece?.stackedPieces?.removeLast()
		          if (updatedNode.piece?.stackedPieces?.isEmpty() == true) {
								updatedNode.piece?.isNeutralized = false
							}

            } else {
              updatedNode.piece = null
            }
            updatedNode
          }

  updatedNodes.forEach { updatedNode ->
    newNodes.remove(newNodes.find { it.coordinate == updatedNode.coordinate }!!)
  }

  // TODO update check
  //  check(nodes.all { it.piece == null }) { "Pieces have not been removed from all nodes." }

  newNodes.addAll(updatedNodes)

  check(this.nodes.size == newNodes.size)

  val newActivePiecesCount =
      newNodes.count { it.piece != null } + newNodes.sumOf { it.piece?.stackedPieces?.size ?: 0 }

  val piecesRemoved = oldActivePiecesCount - newActivePiecesCount
  check(piecesRemoved == nodes.size) {
    "Active piece count mismatch after removal operation! " +
        "Expected to remove ${nodes.size} pieces (nodes processed), but active piece delta was $piecesRemoved " +
        "(Before: $oldActivePiecesCount, After: $newActivePiecesCount)."
  }

  if (newActivePiecesCount == oldActivePiecesCount) {
    check(false) {
      "Invalid state transition: Expected the number of empty board nodes to increase, " +
          "but it did not. Empty nodes before: $oldActivePiecesCount, after: $newActivePiecesCount."
    }
  }

	val hasInvalidNeutralizedPiece = newNodes.any {
		it.piece?.isNeutralized == true && it.piece?.stackedPieces?.isEmpty() == true
	}

	check(!hasInvalidNeutralizedPiece) {
		"Sanity check failed: A piece cannot be neutralized if its stack is empty."
	}

	check(newNodes.filter { it.piece?.isNeutralized == true }.all { it.piece?.stackedPieces?.isNotEmpty() == true }) {}

  return Board(
      nodes =
          newNodes.sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row })).toSet(),
      centerNodeCoordinate = this.centerNodeCoordinate,
  )
}

fun Board.printHexGrid() {

  val columns = 'A'..'J'

  val validRowsByColumn =
      mapOf(
          'A' to 1..5,
          'B' to 1..6,
          'C' to 1..7,
          'D' to 1..8,
          'E' to 1..9,
          'F' to 1..8,
          'G' to 1..7,
          'H' to 1..6,
          'I' to 1..5,
          'J' to 1..4,
      )

  println("=================== GIPF BOARD ===================")
  println(" |1|1|1|1|1|1||2||3||4|5|6|7|8|9|")
  for (rowLetter in columns) {
    val rowStringBuilder = StringBuilder()

    // Dynamic indentation to align the hexagonal grid visually
    // Rows further from the center row 'E' shift inward
    val distanceFromCenter = abs(rowLetter - 'E')
    val indent = if (distanceFromCenter > 0) distanceFromCenter else distanceFromCenter * 2
    rowStringBuilder.append("|".repeat(indent))

    val validColumns = validRowsByColumn[rowLetter] ?: 1..9

    for (colNum in 1..9) {
      if (colNum in validColumns) {
        // Find if there's a node at this Coordinate configuration
        // Note: assuming Coordinate(column = Char, row = Int)
        // If you flipped your Coordinate class properties as well, update these accessors
        // accordingly!
        val node =
            this.nodes.find { it.coordinate.column == rowLetter && it.coordinate.row == colNum }

        if (node != null) {
          val symbol =
              when {
                node.piece != null -> node.piece?.abbreviation ?: "??"
                node.isCenter -> "➕"
                node.isSpot -> "✖️"
                node.isDot -> "⚫"
                else -> "?"
              }
          rowStringBuilder.append("|$symbol|")
        } else {
          rowStringBuilder.append("    ")
        }
      } else {
        rowStringBuilder.append("    ")
      }
    }

    if (rowStringBuilder.toString().trim().isNotEmpty()) {
      // Print the Row Letter on the left side
      println("$rowLetter$rowStringBuilder")
    }
  }

  // Print column number footer coordinates
  println(" |1|1|1|1|1|1||2||3||4|5|6|7|8|9|")
  println("==================================================")
}
