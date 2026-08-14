package org.example

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.cli.*
import org.apache.commons.cli.help.HelpFormatter
import org.example.engine.*
import org.example.model.*
import kotlin.time.Clock.System.now

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

// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m mcts -ms random -M minimax -MS greedy
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m minimax -ms greedy -M mcts -MS random
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m mcts -ms random -M minimax -MS easy
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m minimax -ms easy -M mcts -MS random
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m mcts -ms easy -M minimax -MS greedy
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m minimax -ms greedy -M mcts -MS easy
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m minimax -ms easy -M minimax -MS
// greedy
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m minimax -ms greedy -M minimax -MS
// easy
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m mcts -ms easy -M mcts -MS random
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m mcts -ms random -M mcts -MS easy
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m mcts -ms easy -M minimax -MS easy
// /home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java -Xmx20g -jar
// /var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar
// -m org.example.GenerateReproducibleGamesKt run -b -g 500 -m minimax -ms easy -M mcts -MS easy

// -b -g 500 -m mcts -ms random -M minimax -MS greedy
// -b -g 500 -m minimax -ms greedy -M mcts -MS random
// -b -g 500 -m mcts -ms random -M minimax -MS easy
// -b -g 500 -m minimax -ms easy -M mcts -MS random
// -b -g 500 -m mcts -ms easy -M minimax -MS greedy
// -b -g 500 -m minimax -ms greedy -M mcts -MS easy
// -b -g 500 -m minimax -ms easy -M minimax -MS greedy
// -b -g 500 -m minimax -ms greedy -M minimax -MS easy
// -b -g 500 -m mcts -ms easy -M mcts -MS random
// -b -g 500 -m mcts -ms random -M mcts -MS easy
// -b -g 500 -m mcts -ms easy -M minimax -MS easy
// -b -g 500 -m minimax -ms easy -M mcts -MS easy

// -b -g 500 -m mcts -ms random -M minimax -MS easy
// -b -g 500 -m minimax -ms easy -M mcts -MS random
// -b -g 500 -m minimax -ms easy -M minimax -MS greedy
// -b -g 500 -m minimax -ms greedy -M minimax -MS easy
// -b -g 500 -m mcts -ms easy -M minimax -MS easy
// -b -g 500 -m minimax -ms easy -M mcts -MS easy

private const val FAILURES_DIR = "output/failures"
private const val FAILURES_LOG = "output/failures.jsonl"

suspend fun main(args: Array<String>) = coroutineScope {
  File("output").mkdirs()
  File(FAILURES_DIR).mkdirs()

  val options =
      Options().apply {
        addOption(
            Option.builder("b")
                .longOpt("batch")
                .hasArg(false)
                //                .argName("batch")
                .desc(
                    "Normal batch run: java -Xmx48g -jar app.jar run -b -g <TOTAL_GAMES> -p <PARALLELISM> -s <SEED>" +
                        " -m <AGENT-1-MODEL> -ms <AGENT-1-STRENGTH> -M <AGENT-2-MODEL> -MS <AGENT-2-DIFFICULTY>"
                )
                .get()
        )
        addOption(
            Option.builder("r")
                .longOpt("replay")
                .hasArg(false)
                //                .argName("REPLAY")
                .desc(
                    "Replay a single failed game (verbose, single-threaded): java -jar app.jar -r -G <GAME-ID> -s <SEED> -v <VERBOSE>" +
                        " -m <AGENT-1-MODEL> -ms <AGENT-1-DIFFICULTY> -M <AGENT-2-MODEL> -MS <AGENT-2-DIFFICULTY>"
                )
                .get()
        )
        addOption(
            Option.builder("g")
                .longOpt("total-games")
                .hasArg()
                .argName("COUNT")
                .desc("Total number of games (default: 5000)")
                .get()
        )
        addOption(
            Option.builder("G")
                .longOpt("game-id")
                .hasArg()
                .argName("GAME-ID")
                .desc("ID of the game to replay")
                .get()
        )
        addOption(
            Option.builder("p")
                .longOpt("parallelism")
                .hasArg()
                .argName("THREADS")
                .desc("Number of parallel threads (default: cores - 2)")
                .get()
        )
        addOption(
            Option.builder("s")
                .longOpt("seed")
                .hasArg()
                .argName("SEED")
                .desc("Base seed for run reproducibility (default: current system time)")
                .get()
        )
        addOption(
            Option.builder("m")
                .longOpt("agent-1-model")
                .hasArg(true)
                .argName("AGENT-1-MODEL")
                .desc("AI Model of Agent 1 (white) (default: Monte Carlo Tree Search)")
                .get()
        )
        addOption(
            Option.builder("ms")
                .longOpt("agent-1-strength")
                .hasArg(true)
                .argName("AGENT-1-STRENGTH")
                .desc("Strength of Agent 1 (white) (default: easy)")
                .get()
        )
        addOption(
            Option.builder("mtc")
                .longOpt("agent-1-time-control")
                .hasArg(false)
                .argName("AGENT-1-TIME-CONTROL")
                .desc("Time Given to Agent 1 search for a move (white)")
                .get()
        )
        addOption(
            Option.builder("mrv")
                .longOpt("agent-1-mcts-rave")
                .hasArg(false)
                //            .argName("AGENT-1-MCTS-RAVE")
                .desc("Enable MCTS RAVE for Agent 1 (white) (default: false)")
                .get()
        )
        addOption(
            Option.builder("M")
                .longOpt("agent-2-model")
                .hasArg(true)
                .argName("AGENT-2-MODEL")
                .desc("AI Model of Agent 2 (black) (default: Monte Carlo Tree Search)")
                .get()
        )
        addOption(
            Option.builder("MS")
                .longOpt("agent-2-strength")
                .hasArg(true)
                .argName("AGENT-2-STRENGTH")
                .desc("Strength of Agent 2 (white) (default: easy)")
                .get()
        )
        addOption(
            Option.builder("MTC")
                .longOpt("agent-2-time-control")
                .hasArg(false)
                //            .argName("AGENT-2-TIME-CONTROL")
                .desc("Time Given to Agent 2 search for a move(black)")
                .get()
        )
        addOption(
            Option.builder("MRV")
                .longOpt("agent-2-mcts-rave")
                .hasArg(false)
                //            .argName("AGENT-2-MCTS-RAVE")
                .desc("Enable MCTS RAVE for Agent 2 (black)")
                .get()
        )
        addOption(
            Option.builder("v")
                .longOpt("verbose")
                .hasArg()
                .argName("VERBOSE")
                .desc(
                    "If verbose is true, prints full state summaries each turn — use this only for" +
                        " single-game replay (default: false)"
                )
                .get()
        )
        addOption("h", "help", false, "Print this help message")
      }

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

  val playerOneModel = getModel(cmd.getOptionValue("agent-1-model"))
  val playerOneStrength = getStrength(cmd.getOptionValue("agent-1-strength"))

  val playerTwoModel = getModel(cmd.getOptionValue("agent-2-model"))
  val playerTwoStrength = getStrength(cmd.getOptionValue("agent-2-strength"))

  val playerOneTimeControl = cmd.hasOption("player-one-time-control")
  val playerOneEnableRAVE = cmd.hasOption("player-one-mcts-rave")

  val playerTwoTimeControl = cmd.hasOption("player-two-time-control")
  val playerTwoEnableRAVE = cmd.hasOption("player-two-mcts-rave")

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
        playerOneModel = playerOneModel,
        playerOneStrength = playerOneStrength,
        playerTwoModel = playerTwoModel,
        playerTwoStrength = playerTwoStrength,
        playerOneTimeControl = playerOneTimeControl,
        playerTwoTimeControl = playerTwoTimeControl,
        playerOneEnableRAVE = playerOneEnableRAVE,
        playerTwoEnableRAVE = playerTwoEnableRAVE,
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
        playerOneModel = playerOneModel,
        playerOneStrength = playerOneStrength,
        playerTwoModel = playerTwoModel,
        playerTwoStrength = playerTwoStrength,
        playerOneTimeControl = playerOneTimeControl,
        playerTwoTimeControl = playerTwoTimeControl,
        playerOneEnableRAVE = playerOneEnableRAVE,
        playerTwoEnableRAVE = playerTwoEnableRAVE,
    )
  }
}

private suspend fun runBatch(
    totalGames: Int,
    parallelism: Int,
    baseSeed: Long,
    playerOneModel: Model,
    playerOneStrength: Strength,
    playerTwoModel: Model,
    playerTwoStrength: Strength,
    playerOneTimeControl: Boolean,
    playerTwoTimeControl: Boolean,
    playerOneEnableRAVE: Boolean,
    playerTwoEnableRAVE: Boolean,
) = coroutineScope {
  logger.info {
    "Starting batch run: $totalGames games, parallelism=$parallelism, baseSeed=$baseSeed"
  }
  logger.info { "(save baseSeed if you want to reproduce this exact batch later)" }

  val filePrefix =
      "agent-1_${playerOneModel}_${playerOneStrength}_agent-2_${playerTwoModel}_${playerTwoStrength}"
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
                  playerOneModel = playerOneModel,
                  playerOneStrength = playerOneStrength,
                  playerTwoModel = playerTwoModel,
                  playerTwoStrength = playerTwoStrength,
                  playerOneTimeControl = playerOneTimeControl,
                  playerTwoTimeControl = playerTwoTimeControl,
                  playerOneEnableRAVE = playerOneEnableRAVE,
                  playerTwoEnableRAVE = playerTwoEnableRAVE,
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
    logger.info {
      "  java -jar app.jar -replay -G [game-id] -s [seed] -m $playerOneModel -ms $playerTwoStrength -M $playerTwoModel -MS $playerTwoStrength"
    }
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
    playerOneModel: Model,
    playerOneStrength: Strength,
    playerTwoModel: Model,
    playerTwoStrength: Strength,
    playerOneTimeControl: Boolean = false,
    playerTwoTimeControl: Boolean = false,
    playerOneEnableRAVE: Boolean = false,
    playerTwoEnableRAVE: Boolean = false,
) {
  val rng = Random(seed)

  // NOTE: adapt these calls to actually accept `rng` once your game logic
  // is updated to take an explicit Random parameter instead of a global one.
  var gameState: State =
      initializeState(
          playerOneModel,
          playerOneStrength,
          playerTwoModel,
          playerTwoStrength,
          playerOneTimeControl,
          playerTwoTimeControl,
          playerOneEnableRAVE,
          playerTwoEnableRAVE,
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
    playerOneModel: Model,
    playerOneStrength: Strength,
    playerTwoModel: Model,
    playerTwoStrength: Strength,
    playerOneTimeControl: Boolean,
    playerTwoTimeControl: Boolean,
    playerOneEnableRAVE: Boolean,
    playerTwoEnableRAVE: Boolean,
) {
  val rng = Random(seed)

  // NOTE: adapt these calls to actually accept `rng` once your game logic
  // is updated to take an explicit Random parameter instead of a global one.
  var gameState: State =
      initializeState(
          playerOneModel,
          playerOneStrength,
          playerTwoModel,
          playerTwoStrength,
          playerOneTimeControl,
          playerTwoTimeControl,
          playerOneEnableRAVE,
          playerTwoEnableRAVE,
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
