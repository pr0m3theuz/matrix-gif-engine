package org.example

import kotlinx.serialization.json.Json

const val MAXIMUM_PIECES = 66
const val EXPECTED_TOTAL = MAXIMUM_PIECES / 2

fun main() {
  var gameState: State = initializeState()

  var turn = 0

  // TODO implement function to evaluate if the current player's pieces has any valid moves left
  while (!evaluateCapturedPieces(gameState) || !evaluatePiecesInReserve(gameState)) {
    turn = turn.plus(1)
    println("Turn: $turn")
    gameState.printStateSummary()

    gameState = playerTurn(gameState)

    // TODO implement function to evaluate if the current player's pieces has any valid moves left
    if (
        (evaluateCapturedPieces(gameState) || evaluatePiecesInReserve(gameState)) &&
            getEligiblePotentialMoves(gameState).isEmpty()
    ) {
      break
    }

    gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)
    // state new turn
    gameState = gameState.rotatePlayers()

    check(gameState.currentPlayer.name != gameState.nextPlayer.name) {
      "Turn rotation failure: The current player and the next player are both '${gameState.currentPlayer.name}'."
    }
  }
  // TODO Print winner
  println(determineWinner(gameState)?.name)
  gameState.printStateSummary()
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
