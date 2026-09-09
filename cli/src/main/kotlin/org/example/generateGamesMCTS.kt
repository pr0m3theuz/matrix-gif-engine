package org.example

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.example.engine.ExperienceCollector
import org.example.engine.determineWinner
import org.example.engine.evaluateCapturedPieces
import org.example.engine.playerTurn
import org.example.model.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.measureTimedValue

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

suspend fun main() = coroutineScope {
  val totalGames = 500
  val cores = Runtime.getRuntime().availableProcessors()
  val dispatcher = Dispatchers.Default.limitedParallelism((cores - 2).coerceAtLeast(1))
  val completed = AtomicInteger(0)
    val failed = AtomicInteger(0)
    val startTime = System.currentTimeMillis()

  val (jobs: List<Deferred<Any?>>, elapsedMs: Duration) = measureTimedValue {
      (1..totalGames).map { gameId ->
        async(dispatcher) {
          try {
            val collectors = playOneGame(gameId)
            val n = completed.incrementAndGet()
            if (n % 100 == 0) {
                val secs = (System.currentTimeMillis() - startTime) / 1000.0
                val rate = n / secs
                val etaSecs = ((totalGames - n) / rate).toLong()
                logger.info {
                    "Completed $n/$totalGames (${"%.2f".format(rate)} games/sec, ETA ${etaSecs}s)"
                }
            }
            collectors
          } catch (e: Exception) {
              failed.incrementAndGet()
              logger.info { "Game $gameId failed: ${e.message}" }
          }
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
          Path(
            "state_experience_mcts_${totalGames}_games_${
              now().toString().replace(
                ":",
                "-"
              )
            }.h5",
          )
    )

    logger.info {
        "Done. ${completed.get()} succeeded, ${failed.get()} failed, ${(System.currentTimeMillis() - startTime)/1000.0}s elapsed."
    }
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

    gameState = runBlocking {
      playerTurn(gameState, turn, rng)
    }


    gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)
      if (gameState.turnMoves.size > 2) {
          val stalled =
              gameState.turnMoves.keys.toList().takeLast(1).all {
                  gameState.turnMoves[it].isNullOrEmpty()
              }
          if (stalled) break
      }

    playerWhoMadeTheLastMove = gameState.currentPlayer
    gameState = gameState.rotatePlayers()

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
      )?.winner

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
