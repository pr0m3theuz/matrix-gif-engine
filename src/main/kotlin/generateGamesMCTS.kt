package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.example.engine.evaluateCapturedPieces
import org.example.engine.*
import org.example.model.*
import org.example.model.initializeState
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.mutableListOf
import kotlin.random.Random

suspend fun main() = coroutineScope {
	val totalGames = 2
	val cores = Runtime.getRuntime().availableProcessors()
	val dispatcher = Dispatchers.Default.limitedParallelism(cores)
	val completed = AtomicInteger(0)

	val jobs = (1..totalGames).map { gameId ->
		async(dispatcher) {
			try {
				playOneGame(gameId)
				val n = completed.incrementAndGet()
				if (n % 50 == 0) println("Completed $n/$totalGames games")
			} catch (e: Exception) {
				println("Game $gameId failed: ${e.message}")
			}
		}
	}
	jobs.awaitAll()
	println("All games finished.")
}

fun playOneGame(gameId: Int) {
	val rng = Random(1)
	var gameState: State = initializeState()
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
			val stalled = gameState.turnMoves.keys.toList().takeLast(2)
				.all { gameState.turnMoves[it].isNullOrEmpty() }
			if (stalled) break
		}

		check(gameState.currentPlayer.name != gameState.nextPlayer.name) {
			"Turn rotation failure in game $gameId"
		}
	}

	val winner = determineWinner(
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
	gameState.collector?.saveCurrentEpisodes(agent = "mcts", games = gameId.toString())
}