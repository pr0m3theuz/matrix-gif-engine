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

val RESULTS_DIR = "output/results"

fun recordGameResult(file: File, lock: String, gameId: Int, seed: Long, timestamp: Long, state: State, winner: Player?, turn: Int) {
	requireNotNull(winner)

	val gameResult = GameResult(
		gameId,
		seed,
		timestamp,
		turn,
		winner,
		state,
	)

	try {
		synchronized(lock) {
			file.appendText(Json.encodeToString(gameResult) + "\n")
		}
	} catch (serializationFailure: Exception) {
		// If the state itself can't serialize (e.g. mid-mutation), at least note that.
		file.appendText("Could not serialize gameState: ${serializationFailure.message}")
		logger.error { "Crash at turn $turn in game $gameId — state dumped to $file" }
	}
}