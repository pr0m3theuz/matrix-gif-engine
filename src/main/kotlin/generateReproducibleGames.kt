@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalCoroutinesApi::class)

package org.example

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import kotlin.time.Clock.System.now
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.ParseException
import org.apache.commons.cli.help.HelpFormatter
import org.example.engine.*
import org.example.model.*

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

suspend fun main(args: Array<String>) = coroutineScope {
  File("output").mkdirs()
  File(FAILURES_DIR).mkdirs()

  val options = createCLIOptions()

  val parser: CommandLineParser = DefaultParser()
  val cmd: CommandLine =
      try {
        parser.parse(options, args)
      } catch (e: ParseException) {
        println("Error parsing arguments: ${e.message}")
        val formatter = HelpFormatter.builder().get()
        formatter.printHelp(
            "Command line syntax:",
            "Batch runner for MATRX GIPF with reproducible failures.",
            options,
            "",
            true,
        )
        return@coroutineScope
      }

  if (cmd.hasOption("help")) {
    val formatter = HelpFormatter.builder().get()
    formatter.printHelp(
        "Command line syntax:",
        "Batch runner for MATRX GIPF with reproducible failures.",
        options,
        "",
        true,
    )
    return@coroutineScope
  }

  val agentOne = parseAgentArgs(cmd, "Agent 1 (White)", "agent-1", "m")
  val agentTwo = parseAgentArgs(cmd, "Agent 2 (White)", "agent-2", "m")

  val startTimestamp = System.currentTimeMillis()
  val startIso = Instant.fromEpochMilliseconds(startTimestamp).toString()

  logger.info {
    """
      ============================== EXECUTION CONFIGURATION ==============================
      Start Time: $startIso ($startTimestamp ms)
      Mode:       ${if (cmd.hasOption("replay")) "Replay" else "Batch"}

      [Agent 1 (White)]
        Model:                ${agentOne.model}
        Strength:             ${agentOne.strength}
        Minimax Depth:        ${agentOne.depth}
        MCTS Iterations:      ${agentOne.iterations}
        Time Control:         ${agentOne.timeControl}
        Time Duration:        ${agentOne.timeDuration}
        MCTS RAVE:            ${agentOne.useRAVE}
        Progressive Widening: ${agentOne.enablePW}
        First Play Urgency:   ${agentOne.enableFPU}

      [Agent 2 (Black)]
        Model:                ${agentTwo.model}
        Strength:             ${agentTwo.strength}
        Minimax Depth:        ${agentTwo.depth}
        MCTS Iterations:      ${agentTwo.iterations}
        Time Control:         ${agentTwo.timeControl}
        Time Duration:        ${agentTwo.timeDuration}
        MCTS RAVE:            ${agentTwo.useRAVE}
        Progressive Widening: ${agentTwo.enablePW}
        First Play Urgency:   ${agentTwo.enableFPU}
      =====================================================================================
      """
        .trimIndent()
  }

  if (cmd.hasOption("replay")) {
    val gameId =
        cmd.getOptionValue("game-id").toIntOrNull() ?: error("Usage: replay <game-Id> [seed>")
    val seed = cmd.getOptionValue("seed")?.toLongOrNull() ?: error("Usage: replay <game-Id> [seed>")

    val verbose = cmd.getOptionValue("verbose").toBooleanStrictOrNull() ?: false

    logger.info { "Replaying game $gameId with seed $seed (verbose mode)" }

    playOneGame(
        gameId,
        seed,
        verbose = verbose,
        timestamp = 0L,
        agentOne,
        agentTwo,
    )

    logger.info {
      "Replay finished without throwing — if you were chasing a crash, " +
          "check whether all randomness in your game logic is seed-derived."
    }
  } else {
    // 3. Extract values with fallbacks
    val totalGames = cmd.getOptionValue("total-games")?.toIntOrNull() ?: 5000

    val cores = Runtime.getRuntime().availableProcessors()
    val parallelism =
        cmd.getOptionValue("parallelism")?.toIntOrNull() ?: (cores - 2).coerceAtLeast(1)

    // Base seed makes the ENTIRE run reproducible, not just individual games.
    // Print it so you can re-run the exact same batch later if needed.
    val baseSeed = cmd.getOptionValue("seed")?.toLongOrNull() ?: System.currentTimeMillis()

    runBatch(
        totalGames = totalGames,
        parallelism = parallelism,
        baseSeed = baseSeed,
        agentOne,
        agentTwo,
    )
  }
}

private suspend fun runBatch(
    totalGames: Int,
    parallelism: Int,
    baseSeed: Long,
    agentOne: AgentConfig,
    agentTwo: AgentConfig,
) = coroutineScope {
  logger.info {
    "Starting batch run: $totalGames games, parallelism=$parallelism, baseSeed=$baseSeed"
  }
  logger.info { "(save baseSeed if you want to reproduce this exact batch later)" }

  val filePrefix =
      "agent-1_${agentOne.model}_${
        if (agentOne.strength != Strength.NULL) agentOne.strength else {
        "iterations-" + agentOne.iterations + "-depth-" + agentOne.depth
      }}_agent-2_${agentTwo.model}_${ if (agentTwo.strength != Strength.NULL) agentTwo.strength else {
        "iterations-" + agentTwo.iterations + "-depth-" + agentTwo.depth
      }}"

  val timestamp = now().toString().replace(":", "-")

  val file = File(RESULTS_DIR, "${filePrefix}_games_record_$timestamp.jsonl")
  val lock = file.path

  val searchStatsFile = File(RESULTS_DIR, "${filePrefix}_search_stats_$timestamp.csv")

  if (!file.exists()) {
    file.createNewFile()
  }

  if (!searchStatsFile.exists()) {
    searchStatsFile.createNewFile()
  }

  searchStatsFile.appendText("seed,turn,model,depth,avg_branching_factor,eff_branching_factor")

  val gameResultsData = Channel<String>(capacity = 64)
  val searchResultsData = Channel<String>(capacity = 64)

  val gameResultsWriter =
      launch(Dispatchers.IO) {
        file.bufferedWriter().use { writer ->
          for (line in gameResultsData) {
            writer.write(line)
          }
        }
      }

  val searchResultsWriter =
      launch(Dispatchers.IO) {
        searchStatsFile.bufferedWriter().use { writer ->
          for (line in searchResultsData) {
            writer.write(line)
          }
        }
      }

  val dispatcher = Dispatchers.Default.limitedParallelism(parallelism)
  val completed = AtomicInteger(0)
  val failed = AtomicInteger(0)
  val startTime = System.currentTimeMillis()

  val allowedTime = AtomicLong(0)
  val previousCompletionTime = AtomicLong(0)

  constructZobristHashKeysTable(Random(42))

  val elapsedMs = measureTimeMillis {
    val jobs =
        (1..totalGames).map { gameId ->
          async(dispatcher) {
            // Derive a per-game seed from the base seed + gameId so it's
            // deterministic across runs but distinct per game.

            val gameSeed = deriveSeed(baseSeed, gameId)
            try {
              val result =
                  playOneGame(
                      file = file,
                      fileLock = lock,
                      gameId = gameId,
                      seed = gameSeed,
                      verbose = false,
                      timestamp = startTime,
                      agentOne,
                      agentTwo,
                  )

              gameResultsData.send(result.first())
              searchResultsData.send(result.last())

              val n = completed.incrementAndGet()
              if (n % 10 == 0) {
                previousCompletionTime.set(System.currentTimeMillis())
                val secs = (System.currentTimeMillis() - startTime) / 1000.0
                val rate = n / secs

                val etaSecs = ((totalGames - n) / rate).toLong()
                allowedTime.set(etaSecs / 2)
                logger.info {
                  "Completed $n/$totalGames (${"%.2f".format(rate)} games/sec, ETA ${etaSecs}s)"
                }
              }

              if (
                  allowedTime.get() > 5.seconds.inWholeMilliseconds &&
                    System.nanoTime() >
                          previousCompletionTime.get().plus(allowedTime.get())
              ) {
                exitProcess(1)
              }
            } catch (e: Throwable) {
              failed.incrementAndGet()
              recordFailure(gameId, gameSeed, e)
            }
          }
        }
    jobs.awaitAll()
  }

  gameResultsData.close()
  searchResultsData.close()

  gameResultsWriter.join()
  searchResultsWriter.join()

  logger.info {
    "Done. ${completed.get()} succeeded, ${failed.get()} failed, ${elapsedMs / 1000.0}s elapsed."
  }
  if (failed.get() > 0) {
    logger.info { "Failures logged to $FAILURES_LOG — replay any of them with:" }
    //    logger.info {
    //      "  java -jar app.jar -replay -G [game-id] -s [seed] -m $playerOneModel -ms
    // $playerTwoStrength -M $playerTwoModel -MS $playerTwoStrength"
    //    }
  }

  coroutineContext.cancelChildren()
}

/** Deterministic per-game seed. Same baseSeed + gameId always yields the same seed. */
private fun deriveSeed(baseSeed: Long, gameId: Int): Long {
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

private const val MAX_TURNS_THRESHOLD = 999

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
    agentOne: AgentConfig,
    agentTwo: AgentConfig,
): List<String> {
  val rng = Random(seed)

  // NOTE: adapt these calls to actually accept `rng` once your game logic
  // is updated to take an explicit Random parameter instead of a global one.
  var gameState: State =
      initializeState(
          whitePlayer =
              Player(
                  name = PlayerName.WHITE,
                  model = agentOne.model,
                  strength = agentOne.strength,
                  timeControl = agentOne.timeControl,
                  useRAVE = agentOne.useRAVE,
                  enableFPU = agentOne.enableFPU,
                  enablePW = agentOne.enablePW,
                  iterations = agentOne.iterations,
                  depth = agentOne.depth,
              ),
          blackPlayer =
              Player(
                  name = PlayerName.BLACK,
                  model = agentTwo.model,
                  strength = agentTwo.strength,
                  timeControl = agentTwo.timeControl,
                  useRAVE = agentTwo.useRAVE,
                  enableFPU = agentTwo.enableFPU,
                  enablePW = agentTwo.enablePW,
                  iterations = agentTwo.iterations,
                  depth = agentTwo.depth,
              ),
      )

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

  return recordGameResult(file, fileLock, gameId, seed, timestamp, gameState, winner, turn)
  //  gameState.collector?.saveCurrentEpisodes(agent = "mcts", games = gameId.toString())
}

fun playOneGame(
    gameId: Int,
    seed: Long,
    verbose: Boolean,
    timestamp: Long,
    agentOne: AgentConfig,
    agentTwo: AgentConfig,
) {
  val rng = Random(seed)

  // NOTE: adapt these calls to actually accept `rng` once your game logic
  // is updated to take an explicit Random parameter instead of a global one.
  var gameState: State =
      initializeState(
          whitePlayer =
              Player(
                  name = PlayerName.WHITE,
                  model = agentOne.model,
                  strength = agentOne.strength,
                  timeControl = agentOne.timeControl,
                  useRAVE = agentOne.useRAVE,
                  enableFPU = agentOne.enableFPU,
                  enablePW = agentOne.enablePW,
                  iterations = agentOne.iterations,
                  depth = agentOne.depth,
              ),
          blackPlayer =
              Player(
                  name = PlayerName.BLACK,
                  model = agentTwo.model,
                  strength = agentTwo.strength,
                  timeControl = agentTwo.timeControl,
                  useRAVE = agentTwo.useRAVE,
                  enableFPU = agentTwo.enableFPU,
                  enablePW = agentTwo.enablePW,
                  iterations = agentTwo.iterations,
                  depth = agentTwo.depth,
              ),
      )

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
