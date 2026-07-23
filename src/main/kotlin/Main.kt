package org.example

import kotlinx.serialization.json.Json
import org.example.engine.determineWinner
import org.example.engine.evaluateCapturedPieces
import org.example.engine.getEligiblePotentialMoves
import org.example.engine.playerTurn
import org.example.model.Player
import org.example.model.State
import org.example.model.assertPieceCount
import org.example.model.convertBoardToBitboard
import org.example.model.identifyAvailableMoves
import org.example.model.initializeState
import org.example.model.printStateSummary
import org.jetbrains.kotlinx.multik.api.toNDArray
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray

const val MAXIMUM_PIECES = 66
const val EXPECTED_TOTAL = MAXIMUM_PIECES / 2

fun main() {
  var gameState: State = initializeState()

  var turn = 0

  var playerWhoMadeTheLastMove: Player? = null

  // TODO implement function to evaluate if the current player's pieces has any valid moves left
  while (
      !evaluateCapturedPieces(gameState) ||
          gameState.bitboard
              .identifyAvailableMoves(gameState.currentPlayer)
              .isNotEmpty()
  ) {
    turn = turn.plus(1)
    println("Turn: $turn")
    gameState.printStateSummary()

    // TODO implement function to evaluate if the current player's pieces has any valid moves left
    if (
        (evaluateCapturedPieces(gameState) ||
            gameState.bitboard
                .identifyAvailableMoves(gameState.currentPlayer)
                .isEmpty()) && getEligiblePotentialMoves(gameState).isEmpty()
    ) {
      break
    }

    gameState = playerTurn(gameState, turn)

    gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)

    playerWhoMadeTheLastMove = gameState.currentPlayer
    // state new turn
    gameState = gameState.rotatePlayers()

    check(gameState.currentPlayer.name != gameState.nextPlayer.name) {
      "Turn rotation failure: The current player and the next player are both '${gameState.currentPlayer.name}'."
    }
  }

  val winner =
      determineWinner(
          gameState.currentPlayer,
          gameState.nextPlayer,
          playerWhoMadeTheLastMove,
          state = gameState,
          printStatement = true,
      )

  println("Player: ${winner?.name} won")
  println("State: ${Json.encodeToString(gameState)}")
  gameState.printStateSummary()
}

fun ULong.toBitList(width: Int = 41): D1Array<Int> {
  return this.toLong()
    .toString(2)
    .padStart(width, '0')
    .map { (it - '0') }.toNDArray()
}