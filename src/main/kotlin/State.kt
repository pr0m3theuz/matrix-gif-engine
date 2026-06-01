package org.example

import java.awt.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class State(
    val currentPlayer: Player,
    val nextPlayer: Player,
    //	val whitePlayer: Player,
    //	val blackPlayer: Player,
    val board: Board,
    val lines: Lines,
) {
  fun deepCopy(): State {
    val string = Json.encodeToString(serializer(), this)
    return Json.decodeFromString(serializer(), string)
  }

  // TODO Create update state function/Create next state function
  fun updateState(board: Board, state: State): State {
		state.assertPieceCount()

    return State(
        currentPlayer = state.nextPlayer,
        nextPlayer = state.currentPlayer,
        //		whitePlayer = state.whitePlayer,
        //		blackPlayer = state.blackPlayer,
        board = board,
        lines = constructLines(board.nodes),
    )
  }

  fun rotatePlayers(): State {
    return this.deepCopy().copy(
        currentPlayer = this.nextPlayer,
        nextPlayer = this.currentPlayer,
			)

  }
}

fun State.printStateSummary() {
  val sb = StringBuilder()

  sb.appendLine("=================== GAME STATE ===================")

  // 1. Players Section
  sb.appendLine("Active Players:")
  sb.appendLine("  • CURRENT: ${currentPlayer.name}")
  sb.appendLine("    - Reserve:  ${currentPlayer.piecesInReserve.size} pieces")
  sb.appendLine("    - Captured: ${currentPlayer.capturedPieces.size} pieces")

  sb.appendLine("  • NEXT:    ${nextPlayer.name}")
  sb.appendLine("    - Reserve:  ${nextPlayer.piecesInReserve.size} pieces")
  sb.appendLine("    - Captured: ${nextPlayer.capturedPieces.size} pieces")

  sb.appendLine("--------------------------------------------------")

  val occupiedNodes = board.nodes.filter { it.piece != null }
  sb.appendLine("Board State (${occupiedNodes.size} pieces total):")

  if (occupiedNodes.isEmpty()) {
    sb.appendLine("  (Board is completely empty)")
  } else {
    occupiedNodes.sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row })).forEach {
        node ->
      val piece = node.piece!!
      val pieceType = piece.type.name
      val pieceColor = piece.colorName
      val potentialStr =
          when (piece.potential) {
            true -> " [Has Potential]"
            false -> " [Spent Potential]"
          }

      sb.appendLine(
          "  • [${node.coordinate.column}${node.coordinate.row}] -> $pieceColor $pieceType$potentialStr"
      )
    }
  }

  print(sb.toString())
}

fun initializeState(): State {
  val centerCoordinate = Coordinate(column = 'E', row = 5)

  // Create players
  val whitePlayer =
      Player(
          name = "White",
          abbreviation = "W",
//          color = Color.WHITE,
      )

  val blackPlayer =
      Player(
          name = "Black",
          abbreviation = "B",
//          color = Color.BLACK,
      )

  // Create pieces
  val whitePieces = createPlayerPieces(whitePlayer)
  val blackPieces = createPlayerPieces(blackPlayer)

  whitePlayer.piecesInReserve.addAll(whitePieces)
  blackPlayer.piecesInReserve.addAll(blackPieces)

  val players = listOf<Player>(whitePlayer, blackPlayer)

  // Create Nodes
  val nodes =
      constructNodes(
          letters = LETTERS,
          rows = ROWS.toList(),
          centerLetterIndex = LETTERS.indexOf(centerCoordinate.column),
          center = centerCoordinate,
      )

  // Populate node neighbours
  nodes.forEach { node ->
    node.neighbors =
        populateNeighbors(
            coordinate = node.coordinate,
            nodes = nodes,
        )
  }

  // Create board

  val board = Board(nodes = nodes, centerNodeCoordinate = centerCoordinate)

  // Create lines

  return State(
      currentPlayer = whitePlayer,
      nextPlayer = blackPlayer,
      //		whitePlayer = whitePlayer,
      //		blackPlayer = blackPlayer,
      board = board,
      lines = constructLines(nodes = nodes),
  )
}
