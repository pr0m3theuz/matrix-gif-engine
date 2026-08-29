@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.random.Random
import kotlinx.serialization.json.Json
import org.example.engine.*
import org.example.model.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

const val MAXIMUM_PIECES = 66
const val EXPECTED_TOTAL = MAXIMUM_PIECES / 2

fun main() {
  val rng = Random(-4076601381036510389)

  constructZobristHashKeysTable(Random(42))

//  var gameState: State =
//      initializeState(
//          Model.MINIMAX,
//          Strength.EASY,
//          Model.MCTS,
//          Strength.MEDIUM,
//          playerOneTimeControl = false,
//          playerTwoTimeControl = false,
//          playerOneEnableRAVE = false,
//          playerTwoEnableRAVE = false,
//      )

  var gameState: State =
    initializeState(
      whitePlayer = Player(
        name = PlayerName.WHITE,
        model = Model.MCTS,
        timeControl = false,
        timeDuration = 300.milliseconds,
        enableFPU = true,
        enablePW = true,
        iterations = 49,
        depth = 1,
      ),
      blackPlayer = Player(
        name = PlayerName.BLACK,
        model = Model.MINIMAX,
        timeControl = false,
        timeDuration = 300.milliseconds,
        enableFPU = true,
        enablePW = true,
        iterations = 0,
        depth = 3,
      ),
    )

  var turn = 0

  var playerWhoMadeTheLastMove: Player? = null

  while (
      !evaluateCapturedPieces(gameState) // ||
  //          gameState.bitboard.identifyAvailableMoves(gameState.currentPlayer).isNotEmpty()
  ) {
    turn = turn.plus(1)
    logger.info { "Turn: $turn" }
    //    gameState.printStateSummary()
    gameState.turnMoves[turn] = mutableListOf()
    gameState.turnDuration[turn] = mutableListOf()

    if (true) {
      gameState.turnSearchInfo[turn] = mutableListOf()
    }

    gameState = runBlocking {
      playerTurn(gameState, turn, rng, true)
    }


    gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)

    // break if there are no moves on the last 2 turns as per the rules
    if (gameState.turnMoves.size > 2) {
      gameState.turnMoves.keys
          .toList()
          .takeLast(1)
          .all { turns ->
            gameState.turnMoves[turns].isNullOrEmpty()
          }
          .let { if (it) break }
    }

    playerWhoMadeTheLastMove = gameState.currentPlayer
    gameState = gameState.rotatePlayers()

    check(gameState.currentPlayer.name != gameState.nextPlayer.name) {
      "Turn rotation failure: The current player and the next player are both '${gameState.currentPlayer.name}'."
    }
  }

  gameState.printStateSummary()
  logger.info { "State: ${Json.encodeToString(gameState)}" }

  val winner =
      determineWinner(
          gameState.currentPlayer,
          gameState.nextPlayer,
          playerWhoMadeTheLastMove,
          state = gameState,
          printStatement = true,
      )

  val allTurnStats =
      gameState.turnSearchInfo.mapNotNull { (t, infoLists) ->
        calculateTurnSearchStats(1, t, infoLists.flatten())
      }
  val avgAbf =
      if (allTurnStats.isNotEmpty()) allTurnStats.map { it.avgBranchingFactor }.average() else 0.0
  val avgEbf =
      if (allTurnStats.isNotEmpty()) allTurnStats.map { it.effBranchingFactor }.average() else 0.0

  println("Average Branching Factor: $avgAbf")
  println("Effective Branching Factor: $avgEbf")

//  File("output/results/search_stats.csv").appendText(recordSearchStatsCSV(1, 1, gameState))
//
//  File("output/results/raw_search_data.json")
//      .appendText(
//          recordRawSearchDataJson(
//              1,
//              gameState,
//          )
//      )

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

  logger.info { "Player: ${winner?.winner?.name} won by ${winner?.winCondition?.name}" }
  gameState.turnMoves.keys.toList().takeLast(3).forEach { turns ->
    logger.info { "Turn $turns: ${Json.encodeToString(gameState.turnMoves[turns])}" }
  }

  //  val lastTurnPlayer =
  //      gameState.turnMoves.keys
  //          .toList()
  //          .takeLast(3)
  //          .filter { gameState.turnMoves[it]?.isNotEmpty() == true }
  //          .let { turn ->
  //            gameState.turnMoves[turn.last()]
  //                ?.filterIsInstance<PackedMove.Single>()
  //                ?.first()
  //                ?.value
  //                ?.extractPieceColor()
  //          }
  //
  //  check(winner?.name == lastTurnPlayer) {
  //    "Terminal State Inconsistency: The declared winner '${winner?.name ?: "None"}' " +
  //        "does not match the player who executed the winning turn ('$lastTurnPlayer')."
  //  }

  //  gameState.collector?.saveCurrentEpisodes(agent = "mcts"/, games = 1.toString())

  // TODO
  // val combinedExperiences = gameState.currentPlayer.collector.toBuffer() +
  // gameState.nextPlayer.collector.toBuffer()
  // combinedExperiences.serialize(H5File(Path("experience_${}_${now().toString().replace(":",
  // "-")}.h5").absolutePathString(), HDF5Constants.H5F_ACC_TRUNC))
}
