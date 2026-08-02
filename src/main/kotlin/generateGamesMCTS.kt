package org.example

import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import ncsa.hdf.hdf5lib.HDF5Constants
import ncsa.hdf.`object`.h5.H5File
import org.example.engine.ExperienceCollector
import org.example.engine.determineWinner
import org.example.engine.evaluateCapturedPieces
import org.example.engine.playerTurn
import org.example.model.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.time.Clock.System.now

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

suspend fun main() = coroutineScope {
  val totalGames = 2
  val cores = Runtime.getRuntime().availableProcessors()
  val dispatcher = Dispatchers.Default.limitedParallelism(cores)
  val completed = AtomicInteger(0)

  val jobs =
      (1..totalGames).map { gameId ->
        async(dispatcher) {
          try {
            val collectors = playOneGame(gameId)
            val n = completed.incrementAndGet()
            if (n % 50 == 0) logger.info { "Completed $n/$totalGames games" }
            collectors
          } catch (e: Exception) {
            logger.info { "" + ("Game $gameId failed: ${e.message}") }
          }
        }
      }

  val experiences = jobs.awaitAll()

  val stateBuffer =
      experiences
          .filterIsInstance<ExperienceCollector>()
          .reduce { acc, buffer -> acc + buffer }
          .toBuffer()

//  val agentOneExperiences = experiences.filterIsInstance<List<ExperienceCollector>>().map { it[1] }
//  val agentTwoExperiences = experiences.filterIsInstance<List<ExperienceCollector>>().map { it[2] }

  // .reduce { acc, buffer -> acc + buffer }
  //
    stateBuffer.serialize(
      H5File(
        Path(
          "/training_data",
          "state_experience_mcts_${totalGames}_games_${
            now().toString().replace(
              ":",
              "-"
            )
          }.h5",
        )
          .absolutePathString(),
        HDF5Constants.H5F_ACC_TRUNC,
      )
    )

  println(completed.get())
  logger.info { "" + ("All games finished.") }
}

fun playOneGame(gameId: Int): ExperienceCollector? {
  val rng = Random(1)
  var gameState: State =
      initializeState(
          playerOneModel = Model.MCTS,
          playerTwoModel = Model.MCTS,
      )
  var turn = 0
  var playerWhoMadeTheLastMove: Player? = null

  while (!evaluateCapturedPieces(gameState)) {
    turn++
    gameState.turnMoves[turn] = mutableListOf()

    if (evaluateCapturedPieces(gameState)) break

    gameState = playerTurn(gameState, turn, rng)
    gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)

    playerWhoMadeTheLastMove = gameState.currentPlayer
    gameState = gameState.rotatePlayers()

    if (gameState.turnMoves.size > 2) {
      val stalled =
          gameState.turnMoves.keys.toList().takeLast(2).all {
            gameState.turnMoves[it].isNullOrEmpty()
          }
      if (stalled) break
    }

    check(gameState.currentPlayer.name != gameState.nextPlayer.name) {
      "Turn rotation failure in game $gameId"
    }
  }

  val winner =
      determineWinner(
          gameState.currentPlayer,
          gameState.nextPlayer,
          playerWhoMadeTheLastMove,
          state = gameState,
          printStatement = false, // turn this off in batch runs
      )

  when (winner?.name) {
    gameState.currentPlayer.name -> {
      gameState.currentPlayer.collector?.endEpisode(1)
      gameState.nextPlayer.collector?.endEpisode(-1)
    }
    gameState.nextPlayer.name -> {
      gameState.nextPlayer.collector?.endEpisode(1)
      gameState.currentPlayer.collector?.endEpisode(-1)
    }
    else -> {}
  }

  // IMPORTANT: unique identifier per game so parallel saves don't clobber each other
//  gameState.collector?.saveCurrentEpisodes(agent = "mcts", games = gameId.toString())
  gameState.collector?.endEpisode(0)
  return gameState.collector

//  return listOf<ExperienceCollector?>(
//      gameState.collector,
//      gameState.currentPlayer.collector,
//      gameState.nextPlayer.collector,
//  )
}
