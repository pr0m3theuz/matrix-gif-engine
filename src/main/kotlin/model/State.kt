package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class State(
	val currentPlayer: Player,
	val nextPlayer: Player,
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

fun State.assertPieceCount(EXPECTED_TOTAL: Int = 66/2, MAXIMUM_PIECES: Int = 66) {
	// TODO Check total pieces count == piecesCount
	val totalPieces =
		this.nextPlayer.piecesInReserve.size +
				this.nextPlayer.capturedPieces.size +
				this.currentPlayer.piecesInReserve.size +
				this.currentPlayer.capturedPieces.size +
				this.board.nodes.count { it.piece != null } +
				this.board.nodes.sumOf { it.piece?.stackedPieces?.size ?: 0 }

	val nextPlayer = this.nextPlayer
	val currentPlayer = this.currentPlayer
	val board = this.board

	// 1. Next Player's components
	var nextReservePotentials =
		nextPlayer.piecesInReserve.count { it.colorName == "Black" && it.potential } * 2
	var nextReserveBasics =
		nextPlayer.piecesInReserve.count { it.colorName == "Black" && !it.potential }
	var nextCapturedPotentials =
		nextPlayer.capturedPieces.count { it.colorName == "Black" && it.potential } * 2
	var nextCapturedBasics =
		nextPlayer.capturedPieces.count { it.colorName == "Black" && !it.potential }

	// 2. Current Player's components
	var currentReservePotentials =
		currentPlayer.piecesInReserve.count { it.colorName == "Black" && it.potential } * 2
	var currentReserveBasics =
		currentPlayer.piecesInReserve.count { it.colorName == "Black" && !it.potential }
	var currentCapturedPotentials =
		currentPlayer.capturedPieces.count { it.colorName == "Black" && it.potential } * 2
	var currentCapturedBasics =
		currentPlayer.capturedPieces.count { it.colorName == "Black" && !it.potential }

	// 3. Board components
	var boardPotentials =
		board.nodes.count { it.piece?.colorName == "Black" && it.piece?.potential == true } * 2
	var boardBasics =
		board.nodes.count { it.piece?.colorName == "Black" && it.piece?.potential == false }
	var boardStacks =
		board.nodes.sumOf { it.piece?.stackedPieces?.count { p -> p.colorName == "Black" } ?: 0 }

	val totalBlackPieces =
		nextReservePotentials +
				nextReserveBasics +
				nextCapturedPotentials +
				nextCapturedBasics +
				currentReservePotentials +
				currentReserveBasics +
				currentCapturedPotentials +
				currentCapturedBasics +
				boardPotentials +
				boardBasics +
				boardStacks

	check(totalBlackPieces == EXPECTED_TOTAL) {
		"""
    Critical State Corruption: Total Black pieces ($totalBlackPieces) does not match expected maximum ($EXPECTED_TOTAL).
    Breakdown:
    - Next Player Reserve: Potentials=${nextReservePotentials / 2} (weighted=$nextReservePotentials), Basics=$nextReserveBasics
    - Next Player Captured: Potentials=${nextCapturedPotentials / 2} (weighted=$nextCapturedPotentials), Basics=$nextCapturedBasics
    - Current Player Reserve: Potentials=${currentReservePotentials / 2} (weighted=$currentReservePotentials), Basics=$currentReserveBasics
    - Current Player Captured: Potentials=${currentCapturedPotentials / 2} (weighted=$currentCapturedPotentials), Basics=$currentCapturedBasics
    - Active Board: Potentials=${boardPotentials / 2} (weighted=$boardPotentials), Basics=$boardBasics, Hidden in Stacks=$boardStacks
    """
			.trimIndent()
	}

	// 1. Next Player's components
	nextReservePotentials =
		nextPlayer.piecesInReserve.count { it.colorName == "White" && it.potential } * 2
	nextReserveBasics =
		nextPlayer.piecesInReserve.count { it.colorName == "White" && !it.potential }
	nextCapturedPotentials =
		nextPlayer.capturedPieces.count { it.colorName == "White" && it.potential } * 2
	nextCapturedBasics =
		nextPlayer.capturedPieces.count { it.colorName == "White" && !it.potential }

	// 2. Current Player's components
	currentReservePotentials =
		currentPlayer.piecesInReserve.count { it.colorName == "White" && it.potential } * 2
	currentReserveBasics =
		currentPlayer.piecesInReserve.count { it.colorName == "White" && !it.potential }
	currentCapturedPotentials =
		currentPlayer.capturedPieces.count { it.colorName == "White" && it.potential } * 2
	currentCapturedBasics =
		currentPlayer.capturedPieces.count { it.colorName == "White" && !it.potential }

	// 3. Board components
	boardPotentials =
		board.nodes.count { it.piece?.colorName == "White" && it.piece?.potential == true } * 2
	boardBasics =
		board.nodes.count { it.piece?.colorName == "White" && it.piece?.potential == false }
	boardStacks =
		board.nodes.sumOf { it.piece?.stackedPieces?.count { p -> p.colorName == "White" } ?: 0 }

	val totalWhitePieces =
		nextReservePotentials +
				nextReserveBasics +
				nextCapturedPotentials +
				nextCapturedBasics +
				currentReservePotentials +
				currentReserveBasics +
				currentCapturedPotentials +
				currentCapturedBasics +
				boardPotentials +
				boardBasics +
				boardStacks

	check(totalWhitePieces == EXPECTED_TOTAL) {
		"""
    Critical State Corruption: Total White pieces ($totalWhitePieces) does not match expected maximum ($EXPECTED_TOTAL).
    Breakdown:
    - Next Player Reserve: Potentials=${nextReservePotentials / 2} (weighted=$nextReservePotentials), Basics=$nextReserveBasics
    - Next Player Captured: Potentials=${nextCapturedPotentials / 2} (weighted=$nextCapturedPotentials), Basics=$nextCapturedBasics
    - Current Player Reserve: Potentials=${currentReservePotentials / 2} (weighted=$currentReservePotentials), Basics=$currentReserveBasics
    - Current Player Captured: Potentials=${currentCapturedPotentials / 2} (weighted=$currentCapturedPotentials), Basics=$currentCapturedBasics
    - Active Board: Potentials=${boardPotentials / 2} (weighted=$boardPotentials), Basics=$boardBasics, Hidden in Stacks=$boardStacks
    """
			.trimIndent()
	}

	// TODO Confirm if this handles used potentials
	check(totalPieces <= MAXIMUM_PIECES) {
		"Game Piece Desynchronization: Total pieces in play ($totalPieces) exceeds the maximum piece count ($MAXIMUM_PIECES). " +
				"Pieces have been illegally spawned or deleted." +
				"\nGame State: \n${Json.encodeToString(this)}"
	}
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

