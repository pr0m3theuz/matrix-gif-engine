package org.example.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.model.Player
import org.example.model.PlayerName
import org.example.model.State
import java.io.File
import java.io.Serial

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

@Serializable
data class GameResult(
	val gameId: Int,
	val seed: Long,
	val timestamp: Long,
	val turn: Int,
	val winner: Player,
	val gameState: State,
)

fun recordGameResult(gameId: Int, seed: Long, timestamp: Long, state: State, winner: Player?, turn: Int) {
	requireNotNull(winner)

	val RESULTS_DIR = "output/results"

	val whitePlayer = if (state.currentPlayer.name == PlayerName.WHITE) state.currentPlayer else state.nextPlayer
	val blackPlayer = if (state.currentPlayer.name == PlayerName.BLACK) state.currentPlayer else state.nextPlayer

	val filePrefix = "agent-1_${whitePlayer.model}_${whitePlayer.strength}_agent-2_${blackPlayer.model}_${blackPlayer.strength}"



	val gameResult = GameResult(
		gameId,
		seed,
		timestamp,
		turn,
		winner,
		state,
	)

	val file = File(RESULTS_DIR, "${filePrefix}_games_record.json")
	try {
		file.appendText(Json.encodeToString(gameResult))
	} catch (serializationFailure: Exception) {
		// If the state itself can't serialize (e.g. mid-mutation), at least note that.
		file.appendText("Could not serialize gameState: ${serializationFailure.message}")
	}
	logger.error { "Crash at turn $turn in game $gameId — state dumped to $file" }
}