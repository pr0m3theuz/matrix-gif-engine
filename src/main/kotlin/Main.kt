package org.example

import kotlinx.serialization.json.Json
import org.example.engine.determineWinner
import org.example.engine.evaluateCapturedPieces
import org.example.engine.evaluatePiecesInReserve
import org.example.engine.getEligiblePotentialMoves
import org.example.engine.playerTurn
import org.example.model.Player
import org.example.model.State
import org.example.model.assertPieceCount
import org.example.model.initializeState
import org.example.model.printStateSummary

const val MAXIMUM_PIECES = 66
const val EXPECTED_TOTAL = MAXIMUM_PIECES / 2

fun main() {
  var gameState: State = initializeState()

  var turn = 0

  var playerWhoMadeTheLastMove: Player? = null

  // TODO implement function to evaluate if the current player's pieces has any valid moves left
  while (!evaluateCapturedPieces(gameState) || !evaluatePiecesInReserve(gameState)) {
    turn = turn.plus(1)
    println("Turn: $turn")
    gameState.printStateSummary()

    // TODO implement function to evaluate if the current player's pieces has any valid moves left
    if (
        (evaluateCapturedPieces(gameState) || evaluatePiecesInReserve(gameState)) &&
            getEligiblePotentialMoves(gameState).isEmpty()
    ) {
      break
    }

    gameState = playerTurn(gameState)

    gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)

    playerWhoMadeTheLastMove = gameState.currentPlayer
    // state new turn
    gameState = gameState.rotatePlayers()

    check(gameState.currentPlayer.name != gameState.nextPlayer.name) {
      "Turn rotation failure: The current player and the next player are both '${gameState.currentPlayer.name}'."
    }
  }

  gameState.printStateSummary()
  println("Player: ${determineWinner(gameState.currentPlayer, gameState.nextPlayer, playerWhoMadeTheLastMove)?.name} won")
	println("State: ${Json.encodeToString(gameState)}")

}

