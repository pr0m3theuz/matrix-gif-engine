@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.cli.*
import org.apache.commons.cli.help.HelpFormatter
import org.example.engine.*
import org.example.model.*
import kotlin.time.Clock.System.now
import kotlin.time.Instant

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

/**
 * Batch runner with reproducible failures.
 *
 * Normal batch run: java -Xmx48g -jar app.jar run -b -g <TOTAL_GAMES> -p <PARALLELISM> -s <SEED> -m
 * <AGENT-1-MODEL> -ms <AGENT-1-DIFFICULTY> -M <AGENT-2-MODEL> -MS <AGENT 2-DIFFICULTY>
 *
 * Replay a single failed game (verbose, single-threaded): java -jar app.jar -r -G <GAME-ID> -s
 * <SEED> -v <VERBOSE> -m <AGENT-1-MODEL> -ms <AGENT-1-DIFFICULTY> -M <AGENT-2-MODEL> -MS
 * <AGENT-2-DIFFICULTY
 */

private const val FAILURES_DIR = "output/failures"
private const val FAILURES_LOG = "output/failures.jsonl"

class AgentOptions(name: String, prefix: String, shortPrefix: String) : OptionGroup(name) {
  val model by option("-$shortPrefix", "--$prefix-model", help = "AI Model for $name")
    .enum<Model> { it.name.lowercase() }
    .default(Model.NEURAL_NETWORK)

  val strength by option("-${shortPrefix}s", "--$prefix-strength", help = "Strength of $name")
    .enum<Strength> { it.name.lowercase() }
    .default(Strength.RANDOM)

  val timeControl by option("-${shortPrefix}tc", "--$prefix-time-control", help = "Enable time control for $name")
    .flag(default = false)

  val useRAVE by option("-${shortPrefix}rv", "--$prefix-mcts-rave", help = "Enable MCTS RAVE for $name")
    .flag(default = false)

  val enableFPU by option("-${shortPrefix}fpu", "--$prefix-fpu", help = "Enable First Play Urgency for $name")
    .flag(default = false)

  val enablePW by option("-${shortPrefix}pw", "--$prefix-progressive-widening", help = "Enable Progressive Widening for $name")
    .flag(default = false)

  val iterations by option("-${shortPrefix}i", "--$prefix-mcts-iterations", help = "MCTS iteration limit for $name")
    .int()
    .default(999)

  val depth by option("-${shortPrefix}dp", "--$prefix-depth", help = "Search depth for $name")
    .int()
    .default(3)

  fun toPlayer(playerName: PlayerName): Player =
    Player(
      name = playerName,
      model = model,
      strength = strength,
      timeControl = timeControl,
      useRAVE = useRAVE,
      enableFPU = enableFPU,
      enablePW = enablePW,
      iterations = iterations,
      depth = depth,
    )
}

class GameRunner : CliktCommand(name = "matrx-gipf") {
  override fun help(context: Context) = "Batch runner and replay tool for MATRX GIPF."

  private val batch by option("-b", "--batch", help = "Run in batch mode").flag(default = false)
  private val replay by option("-r", "--replay", help = "Replay a single failed game").flag(default = false)
  private val totalGames by option("-g", "--total-games", help = "Total number of games").int().default(500)
  private val gameId by option("-G", "--game-id", help = "ID of the game to replay").int()
  private val parallelism by option("-p", "--parallelism", help = "Number of parallel threads").int()
  private val seed by option("-s", "--seed", help = "Base seed for reproducibility").long()
  private val verbose by option("-v", "--verbose", help = "Print full state summaries each turn").flag(default = false)

  private val agent1 by AgentOptions("Agent 1 (White)", "agent-1", "m")
  private val agent2 by AgentOptions("Agent 2 (Black)", "agent-2", "M")

  override fun run() = runBlocking {
    File("output").mkdirs()
    File(FAILURES_DIR).mkdirs()
    File(RESULTS_DIR).mkdirs()

    val playerOne = agent1.toPlayer(PlayerName.WHITE)
    val playerTwo = agent2.toPlayer(PlayerName.BLACK)

    val startTimestamp = System.currentTimeMillis()
    val startIso = Instant.fromEpochMilliseconds(startTimestamp).toString()

    logger.info {
      """
      ============================== EXECUTION CONFIGURATION ==============================
      Start Time: $startIso ($startTimestamp ms)
      Mode:       ${if (replay) "Replay" else "Batch"}

      [Agent 1 (White)]
        Model:                ${playerOne.model}
        Strength:             ${playerOne.strength}
        Time Control:         ${playerOne.timeControl}
        MCTS RAVE:            ${playerOne.useRAVE}
        MCTS Iterations:      ${playerOne.iterations}
        Depth:                ${playerOne.depth}
        Progressive Widening: ${playerOne.enablePW}
        First Play Urgency:   ${playerOne.enableFPU}

      [Agent 2 (Black)]
        Model:                ${playerTwo.model}
        Strength:             ${playerTwo.strength}
        Time Control:         ${playerTwo.timeControl}
        MCTS RAVE:            ${playerTwo.useRAVE}
        MCTS Iterations:      ${playerTwo.iterations}
        Depth:                ${playerTwo.depth}
        Progressive Widening: ${playerTwo.enablePW}
        First Play Urgency:   ${playerTwo.enableFPU}
      =====================================================================================
      """.trimIndent()
    }

    val totalExecutionMs = measureTimeMillis {
      if (replay) {
        val targetGameId = gameId ?: error("Error: --game-id (-G) is required when running in replay mode.")
        val targetSeed = seed ?: error("Error: --seed (-s) is required when running in replay mode.")

        logger.info { "Replaying game $targetGameId with seed $targetSeed (verbose: $verbose)" }

        playOneGame(
          gameId = targetGameId,
          seed = targetSeed,
          verbose = verbose,
          timestamp = startTimestamp,
          playerOne = playerOne,
          playerTwo = playerTwo,
        )

        logger.info {
          "Replay finished without throwing — if you were chasing a crash, " +
          "check whether all randomness in your game logic is seed-derived."
        }
      } else {
        val cores = Runtime.getRuntime().availableProcessors()
        val actualParallelism = parallelism ?: (cores - 2).coerceAtLeast(1)
        val baseSeed = seed ?: startTimestamp

        runBatch(
          totalGames = totalGames,
          parallelism = actualParallelism,
          baseSeed = baseSeed,
          playerOne = playerOne,
          playerTwo = playerTwo,
        )
      }
    }

    val finishIso = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString()
    logger.info {
      """
      ================================ RUN SUMMARY ================================
      Start Time:     $startIso
      End Time:       $finishIso
      Total Duration: ${totalExecutionMs}ms (${"%.2f".format(totalExecutionMs / 1000.0)}s)
      =============================================================================
      """.trimIndent()
    }
  }
}

fun main(args: Array<String>) = GameRunner().main(args)

private suspend fun runBatch(
  totalGames: Int,
  parallelism: Int,
  baseSeed: Long,
  playerOne: Player,
  playerTwo: Player,
) = coroutineScope {
  logger.info {
    "Starting batch run: $totalGames games, parallelism=$parallelism, baseSeed=$baseSeed"
  }
  logger.info { "(save baseSeed if you want to reproduce this exact batch later)" }

  val filePrefix = "agent-1_${playerOne.model}_${playerOne.strength}_agent-2_${playerTwo.model}_${playerTwo.strength}"
  val file = File(RESULTS_DIR, "${filePrefix}_games_record_${now().toString().replace(":", "-")}.jsonl")
  val lock = file.path

  if (!file.exists()) {
    file.createNewFile()
  }

  val dispatcher = Dispatchers.Default.limitedParallelism(parallelism)
  val completed = AtomicInteger(0)
  val failed = AtomicInteger(0)
  val startTime = System.currentTimeMillis()

  val elapsedMs = measureTimeMillis {
    val jobs =
        (1..totalGames).map { gameId ->
          async(dispatcher) {
            // Derive a per-game seed from the base seed + gameId so it's
            // deterministic across runs but distinct per game.
            val gameSeed = deriveSeed(baseSeed, gameId)
            try {
              playOneGame(
                file = file,
                fileLock = lock,
                gameId = gameId,
                seed = gameSeed,
                verbose = false,
                timestamp = startTime,
                playerOne = playerOne,
                playerTwo = playerTwo,
              )

              val n = completed.incrementAndGet()
              if (n % 10 == 0) {
                val secs = (System.currentTimeMillis() - startTime) / 1000.0
                val rate = n / secs
                val etaSecs = ((totalGames - n) / rate).toLong()
                logger.info {
                  "Completed $n/$totalGames (${"%.2f".format(rate)} games/sec, ETA ${etaSecs}s)"
                }
              }
            } catch (e: Throwable) {
              failed.incrementAndGet()
              recordFailure(gameId, gameSeed, e)
            }
          }
        }
    jobs.awaitAll()
  }

  logger.info {
    "Done. ${completed.get()} succeeded, ${failed.get()} failed, ${elapsedMs / 1000.0}s elapsed."
  }
  if (failed.get() > 0) {
    logger.info { "Failures logged to $FAILURES_LOG — replay any of them with:" }
//    logger.info {
//      "  java -jar app.jar -replay -G [game-id] -s [seed] -m $playerOneModel -ms $playerTwoStrength -M $playerTwoModel -MS $playerTwoStrength"
//    }
  }
}

/** Deterministic per-game seed. Same baseSeed + gameId always yields the same seed. */
private fun deriveSeed(baseSeed: Long, gameId: Int): Long {
  // Simple, stable mix — not cryptographic, just needs to avoid obvious collisions.
  return baseSeed xor (gameId.toLong() * -0x61c8864680b583ebL)
}

private fun recordFailure(gameId: Int, seed: Long, e: Throwable) {
  val timestamp = System.currentTimeMillis()

  val traceFile = File(FAILURES_DIR, "game_${gameId}_seed_${seed}.txt")
  traceFile.writeText(
      buildString {
        appendLine("gameId: $gameId")
        appendLine("seed: $seed")
        appendLine("timestamp: $timestamp")
        appendLine("exception: ${e::class.qualifiedName}: ${e.message}")
        appendLine()
        appendLine(e.stackTraceToString())
      }
  )

  val entry =
      FailureRecord(
          gameId = gameId,
          seed = seed,
          timestamp = timestamp,
          exceptionType = e::class.qualifiedName ?: "Unknown",
          message = e.message ?: "",
          stackTrace = e.stackTraceToString(),
      )
  synchronized(FAILURES_LOG) {
    File(FAILURES_LOG).appendText(Json.encodeToString(entry) + "\n")
  }

  logger.error { "Game $gameId FAILED (seed=$seed): ${e.message} — see $traceFile" }
}

@Serializable
data class FailureRecord(
    val gameId: Int,
    val seed: Long,
    val timestamp: Long,
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
)

private const val MAX_TURNS_THRESHOLD = 500

/**
 * Plays one game. If [verbose] is true, prints full state summaries each turn — use this only for
 * single-game replay, never in a parallel batch (output would interleave into garbage across
 * threads).
 *
 * IMPORTANT: [seed] must be the ONLY source of randomness for this game. If initializeState() /
 * playerTurn() / your bitboard move selection pull from a global Random() instead of this seed,
 * replay will not reproduce the same game. Thread a `Random(seed)` instance through those calls
 * instead of relying on ambient/global randomness.
 */
fun playOneGame(
    file: File,
    fileLock: String,
    gameId: Int,
    seed: Long,
    verbose: Boolean,
    timestamp: Long,
    playerOne: Player,
    playerTwo: Player,
) {
  val rng = Random(seed)

  var gameState: State = initializeState(playerOne = playerOne, playerTwo = playerTwo)
  var turn = 0
  var playerWhoMadeTheLastMove: Player? = null

  // On failure we dump the move history and last-known state, giving you the
  // exact sequence leading up to the crash independent of whether seeding
  // was perfectly deterministic elsewhere in the codebase.
  try {
    while (!evaluateCapturedPieces(gameState)) {
      turn++
      gameState.turnMoves[turn] = mutableListOf()
      gameState.turnDuration[turn] = mutableListOf()
      gameState.turnSearchInfo[turn] = mutableListOf()

      if (evaluateCapturedPieces(gameState)) break

      if (verbose) {
        logger.info { "Turn: $turn" }
        gameState.printStateSummary()
      }

      gameState = playerTurn(gameState, turn, rng)
      gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)


      if (gameState.turnMoves.size > 2) {
        gameState.turnMoves.keys
          .toList()
          .takeLast(1)
          .any { turns ->
            gameState.turnMoves[turns].isNullOrEmpty()
          }
          .let { if (it) break }
      }

      check(value = turn <= MAX_TURNS_THRESHOLD) {
        "Safety guard triggered: Exceeded maximum turn threshold of $MAX_TURNS_THRESHOLD. Turns: $turn"
      }

      playerWhoMadeTheLastMove = gameState.currentPlayer
      gameState = gameState.rotatePlayers()

      check(gameState.currentPlayer.name != gameState.nextPlayer.name) {
        "Turn rotation failure in game $gameId"
      }
    }
  } catch (e: Throwable) {
    dumpCrashState(gameId, seed, turn, gameState, e)
    throw e
  }

  val winner =
      determineWinner(
          gameState.currentPlayer,
          gameState.nextPlayer,
          playerWhoMadeTheLastMove,
          state = gameState,
          printStatement = verbose,
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

  recordGameResult(file, fileLock, gameId, seed, timestamp, gameState, winner, turn)
  //  gameState.collector?.saveCurrentEpisodes(agent = "mcts", games = gameId.toString())
}

fun playOneGame(
    gameId: Int,
    seed: Long,
    verbose: Boolean,
    timestamp: Long,
    playerOne: Player,
    playerTwo: Player,
) {
  val rng = Random(seed)

  // NOTE: adapt these calls to actually accept `rng` once your game logic
  // is updated to take an explicit Random parameter instead of a global one.
  var gameState: State = initializeState(playerOne = playerOne, playerTwo = playerTwo)
  var turn = 0
  var playerWhoMadeTheLastMove: Player? = null

  // On failure we dump the move history and last-known state, giving you the
  // exact sequence leading up to the crash independent of whether seeding
  // was perfectly deterministic elsewhere in the codebase.
  try {
    while (!evaluateCapturedPieces(gameState)) {
      turn++
      gameState.turnMoves[turn] = mutableListOf()

      if (evaluateCapturedPieces(gameState)) break

      if (verbose) {
        logger.info { "Turn: $turn" }
        gameState.printStateSummary()
      }

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
  } catch (e: Throwable) {
    dumpCrashState(gameId, seed, turn, gameState, e)
    throw e
  }

  determineWinner(
      gameState.currentPlayer,
      gameState.nextPlayer,
      playerWhoMadeTheLastMove,
      state = gameState,
      printStatement = verbose,
  )
}

private fun dumpCrashState(gameId: Int, seed: Long, turn: Int, gameState: State, e: Throwable) {
  val file = File(FAILURES_DIR, "game_${gameId}_seed_${seed}_state.json")
  try {
    file.writeText(Json.encodeToString(gameState))
  } catch (serializationFailure: Exception) {
    // If the state itself can't serialize (e.g. mid-mutation), at least note that.
    file.writeText("Could not serialize gameState: ${serializationFailure.message}")
  }
  logger.error { "Crash at turn $turn in game $gameId — state dumped to $file" }
}
