package org.example

import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.engine.determineWinner
import org.example.engine.evaluateCapturedPieces
import org.example.engine.playerTurn
import org.example.model.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.mutableListOf
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Batch runner with reproducible failures.
 *
 * Normal batch run:
 *   java -Xmx48g -jar app.jar run [totalGames] [parallelism] [baseSeed]
 *
 * Replay a single failed game (verbose, single-threaded):
 *   java -jar app.jar replay <gameId> <seed>
 */

private const val FAILURES_DIR = "output/failures"
private const val FAILURES_LOG = "output/failures.jsonl"

suspend fun main(args: Array<String>) = coroutineScope {
	File("output").mkdirs()
	File(FAILURES_DIR).mkdirs()

	when (args.getOrNull(0)) {
		"replay" -> {
			val gameId = args.getOrNull(1)?.toIntOrNull()
				?: error("Usage: replay <gameId> <seed>")
			val seed = args.getOrNull(2)?.toLongOrNull()
				?: error("Usage: replay <gameId> <seed>")
			println("Replaying game $gameId with seed $seed (verbose mode)")
			playOneGame(gameId, seed, verbose = true)
			println("Replay finished without throwing — if you were chasing a crash, " +
					"check whether all randomness in your game logic is seed-derived.")
		}
		else -> runBatch(args)
	}
}

private suspend fun runBatch(args: Array<String>) = coroutineScope {
	val totalGames = args.getOrNull(1)?.toIntOrNull() ?: 5000
	val cores = Runtime.getRuntime().availableProcessors()
	val parallelism = args.getOrNull(2)?.toIntOrNull() ?: (cores - 2).coerceAtLeast(1)
	// Base seed makes the ENTIRE run reproducible, not just individual games.
	// Print it so you can re-run the exact same batch later if needed.
	val baseSeed = args.getOrNull(3)?.toLongOrNull() ?: System.currentTimeMillis()

	println("Starting batch run: $totalGames games, parallelism=$parallelism, baseSeed=$baseSeed")
	println("(save baseSeed if you want to reproduce this exact batch later)")

	val dispatcher = Dispatchers.Default.limitedParallelism(parallelism)
	val completed = AtomicInteger(0)
	val failed = AtomicInteger(0)
	val startTime = System.currentTimeMillis()

	val elapsedMs = measureTimeMillis {
		val jobs = (1..totalGames).map { gameId ->
			async(dispatcher) {
				// Derive a per-game seed from the base seed + gameId so it's
				// deterministic across runs but distinct per game.
				val gameSeed = deriveSeed(baseSeed, gameId)
				try {
					playOneGame(gameId, gameSeed, verbose = false)
					val n = completed.incrementAndGet()
					if (n % 100 == 0) {
						val secs = (System.currentTimeMillis() - startTime) / 1000.0
						val rate = n / secs
						val etaSecs = ((totalGames - n) / rate).toLong()
						println("Completed $n/$totalGames (${"%.2f".format(rate)} games/sec, ETA ${etaSecs}s)")
					}
				} catch (e: Throwable) {
					failed.incrementAndGet()
					recordFailure(gameId, gameSeed, e)
				}
			}
		}
		jobs.awaitAll()
	}

	println("Done. ${completed.get()} succeeded, ${failed.get()} failed, ${elapsedMs / 1000.0}s elapsed.")
	if (failed.get() > 0) {
		println("Failures logged to $FAILURES_LOG — replay any of them with:")
		println("  java -jar app.jar replay <gameId> <seed>")
	}
}

/** Deterministic per-game seed. Same baseSeed + gameId always yields the same seed. */
private fun deriveSeed(baseSeed: Long, gameId: Int): Long {
	// Simple, stable mix — not cryptographic, just needs to avoid obvious collisions.
	return baseSeed xor (gameId.toLong() * -0x61c8864680b583ebL)
}

private fun recordFailure(gameId: Int, seed: Long, e: Throwable) {
	val timestamp = System.currentTimeMillis()

	// 1. Human-readable full stack trace, one file per failure for easy opening.
	val traceFile = File(FAILURES_DIR, "game_${gameId}_seed_${seed}.txt")
	traceFile.writeText(buildString {
		appendLine("gameId: $gameId")
		appendLine("seed: $seed")
		appendLine("timestamp: $timestamp")
		appendLine("exception: ${e::class.qualifiedName}: ${e.message}")
		appendLine()
		appendLine(e.stackTraceToString())
	})

	// 2. Structured JSONL entry — one line per failure, easy to grep/parse/aggregate.
	val entry = FailureRecord(
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

	System.err.println("Game $gameId FAILED (seed=$seed): ${e.message} — see $traceFile")
}

@kotlinx.serialization.Serializable
data class FailureRecord(
	val gameId: Int,
	val seed: Long,
	val timestamp: Long,
	val exceptionType: String,
	val message: String,
	val stackTrace: String,
)

/**
 * Plays one game. If [verbose] is true, prints full state summaries each turn —
 * use this only for single-game replay, never in a parallel batch (output would
 * interleave into garbage across threads).
 *
 * IMPORTANT: [seed] must be the ONLY source of randomness for this game. If
 * initializeState() / playerTurn() / your bitboard move selection pull from a
 * global Random() instead of this seed, replay will not reproduce the same game.
 * Thread a `Random(seed)` instance through those calls instead of relying on
 * ambient/global randomness.
 */
fun playOneGame(gameId: Int, seed: Long, verbose: Boolean) {
	val rng = Random(seed)

	// NOTE: adapt these calls to actually accept `rng` once your game logic
	// is updated to take an explicit Random parameter instead of a global one.
	var gameState: State = initializeState()
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
				println("Turn: $turn")
				gameState.printStateSummary()
			}

			gameState = playerTurn(gameState, turn, rng)
			gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)

			playerWhoMadeTheLastMove = gameState.currentPlayer
			gameState = gameState.rotatePlayers()

			if (gameState.turnMoves.size > 2) {
				val stalled = gameState.turnMoves.keys.toList().takeLast(2)
					.all { gameState.turnMoves[it].isNullOrEmpty() }
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

	val winner = determineWinner(
		gameState.currentPlayer,
		gameState.nextPlayer,
		playerWhoMadeTheLastMove,
		state = gameState,
		printStatement = verbose,
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

	gameState.collector?.saveCurrentEpisodes(agent = "mcts", games = gameId.toString())
}

private fun dumpCrashState(gameId: Int, seed: Long, turn: Int, gameState: State, e: Throwable) {
	val file = File(FAILURES_DIR, "game_${gameId}_seed_${seed}_state.json")
	try {
		file.writeText(Json.encodeToString(gameState))
	} catch (serializationFailure: Exception) {
		// If the state itself can't serialize (e.g. mid-mutation), at least note that.
		file.writeText("Could not serialize gameState: ${serializationFailure.message}")
	}
	System.err.println("Crash at turn $turn in game $gameId — state dumped to $file")
}