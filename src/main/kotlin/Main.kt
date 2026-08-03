package org.example

import kotlinx.serialization.json.Json
import org.example.ai.mcts.PackedMove
import org.example.engine.determineWinner
import org.example.engine.evaluateCapturedPieces
import org.example.engine.playerTurn
import org.example.model.*
import kotlin.random.Random

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

const val MAXIMUM_PIECES = 66
const val EXPECTED_TOTAL = MAXIMUM_PIECES / 2

fun main() {
  val rng = Random(4968145030332927181)
  var gameState: State = initializeState(
      Model.MCTS,
      Strength.EASY,
      Model.MINIMAX,
      Strength.GREEDY,
  )

  var turn = 0

  var playerWhoMadeTheLastMove: Player? = null

  // TODO implement function to evaluate if the current player's pieces has any valid moves left
  while (
      !evaluateCapturedPieces(gameState) // ||
  //          gameState.bitboard.identifyAvailableMoves(gameState.currentPlayer).isNotEmpty()
  ) {
    turn = turn.plus(1)
    logger.info { "Turn: $turn" }
    //    gameState.printStateSummary()
    gameState.turnMoves[turn] = mutableListOf()

    // TODO implement function to evaluate if the current player's pieces has any valid moves left
    if (
        (evaluateCapturedPieces(gameState)) // ||
    //            gameState.bitboard.identifyAvailableMoves(gameState.currentPlayer).isEmpty()) &&
    //            getEligiblePotentialMoves(gameState).isEmpty()
    ) {
      break
    }

    gameState = playerTurn(gameState, turn, rng)

    gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)

    playerWhoMadeTheLastMove = gameState.currentPlayer
    // state new turn
    gameState = gameState.rotatePlayers()

    // break if there are no moves on the last 2 turns as per the rules
    if (gameState.turnMoves.size > 2) {
      gameState.turnMoves.keys
          .toList()
          .takeLast(2)
          .all { turns ->
            gameState.turnMoves[turns].isNullOrEmpty()
          }
          .let { if (it) break }
    }

    check(gameState.currentPlayer.name != gameState.nextPlayer.name) {
      "Turn rotation failure: The current player and the next player are both '${gameState.currentPlayer.name}'."
    }
  }

  gameState.printStateSummary()
  logger.info { "" + ("State: ${Json.encodeToString(gameState)}") }

  val winner =
      determineWinner(
          gameState.currentPlayer,
          gameState.nextPlayer,
          playerWhoMadeTheLastMove,
          state = gameState,
          printStatement = true,
      )

//  when (winner?.name) {
//    gameState.currentPlayer.name -> {
//      gameState.currentPlayer.collector?.endEpisode(1)
//      gameState.nextPlayer.collector?.endEpisode(-1)
//    }
//    gameState.nextPlayer.name -> {
//      gameState.nextPlayer.collector?.endEpisode(1)
//      gameState.currentPlayer.collector?.endEpisode(-1)
//    }
//    else -> {}
//  }

  logger.info { "" + ("Player: ${winner?.name} won") }
  gameState.turnMoves.keys.toList().takeLast(3).forEach { turns ->
    logger.info { "" + ("Turn $turns: ${Json.encodeToString(gameState.turnMoves[turns])}") }
  }

  val lastTurnPlayer =
      gameState.turnMoves.keys
          .toList()
          .takeLast(3)
          .filter { gameState.turnMoves[it]?.isNotEmpty() == true }
          .let { turn ->
            gameState.turnMoves[turn.last()]
                ?.filterIsInstance<PackedMove.Single>()
                ?.first()
                ?.value
                ?.extractPieceColor()
          }

  check(winner?.name == lastTurnPlayer) {
    "Terminal State Inconsistency: The declared winner '${winner?.name ?: "None"}' " +
        "does not match the player who executed the winning turn ('$lastTurnPlayer')."
  }

//  gameState.collector?.saveCurrentEpisodes(agent = "mcts"/, games = 1.toString())

  // TODO
  // val combinedExperiences = gameState.currentPlayer.collector.toBuffer() +
  // gameState.nextPlayer.collector.toBuffer()
  // combinedExperiences.serialize(H5File(Path("experience_${}_${now().toString().replace(":",
  // "-")}.h5").absolutePathString(), HDF5Constants.H5F_ACC_TRUNC))
}
