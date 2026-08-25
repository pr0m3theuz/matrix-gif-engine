package org.example.engine

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.ai.humanEvaluation.SearchInfo
import org.example.model.Player
import org.example.model.State
import kotlinx.coroutines.channels.Channel

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

@Serializable
data class GameResult(
    val gameId: Int,
    val seed: Long,
    val timestamp: Long,
    val turn: Int,
    val winner: Winner,
    val gameState: State,
)

data class TurnSearchStats(
    val gameId: Int,
    val turn: Int,
    val model: String,
    val depth: Double,
    val avgBranchingFactor: Double,
    val effBranchingFactor: Double,
)

@Serializable
data class RawGameSearchData(
    val gameId: Int,
    val turnSearchInfo: Map<Int, List<List<SearchInfo>>>,
)

val RESULTS_DIR = "output/results"

fun calculateTurnSearchStats(gameId: Int, turn: Int, searchInfos: List<SearchInfo>): TurnSearchStats? {
  if (searchInfos.isEmpty()) return null
  val model = searchInfos.first().model.name
  val totalNodes = searchInfos.sumOf { it.computedNodesEvaluated }
  val totalAvailableMoves = searchInfos.sumOf { it.computedAvailableMovesEvaluated }
  val maxDepth = searchInfos.maxOfOrNull { it.computedMaxDepth } ?: 0.0

  val avgBranchingFactor = if (totalNodes > 0.0) totalAvailableMoves / totalNodes else 0.0
  val effBranchingFactor = if (totalNodes > 0.0 && maxDepth > 0.0) Math.pow(totalNodes, 1.0 / maxDepth) else 0.0

  return TurnSearchStats(
      gameId = gameId,
      turn = turn,
      model = model,
      depth = maxDepth,
      avgBranchingFactor = avgBranchingFactor,
      effBranchingFactor = effBranchingFactor,
  )
}

fun recordSearchStatsCSV(
//    csvFile: File,
//    lock: String,
    seed: Long,
    gameId: Int,
    state: State,
): String {

  return buildString {
    for ((turn, infoLists) in state.turnSearchInfo.entries.sortedBy { it.key }) {
      val searchInfos = infoLists.flatten()
      val stats = calculateTurnSearchStats(gameId, turn, searchInfos)
      if (stats != null) {
        appendLine("${seed},${stats.turn},${stats.model},${stats.depth},${stats.avgBranchingFactor},${stats.effBranchingFactor}")
      }
    }
  }

//  try {
//    synchronized(lock) {
//      if (csvFile.parentFile != null && !csvFile.parentFile.exists()) {
//        csvFile.parentFile.mkdirs()
//      }
//      val isNew = !csvFile.exists() || csvFile.length() == 0L
//      csvFile.appendText(
//          buildString {
//            if (isNew) {
//              appendLine("seed,turn,model,depth,avg_branching_factor,eff_branching_factor")
//            }
//            for ((turn, infoLists) in state.turnSearchInfo.entries.sortedBy { it.key }) {
//              val searchInfos = infoLists.flatten()
//              val stats = calculateTurnSearchStats(gameId, turn, searchInfos)
//              if (stats != null) {
//                appendLine("${seed},${stats.turn},${stats.model},${stats.depth},${stats.avgBranchingFactor},${stats.effBranchingFactor}")
//              }
//            }
//          }
//      )
//    }
//  } catch (e: Exception) {
//    logger.error { "Failed to log search stats CSV for game $gameId: ${e.message}" }
//  }
}

fun recordRawSearchDataJson(
//    jsonFile: File,
//    lock: String,
    gameId: Int,
    state: State,
): String {
  val rawData = RawGameSearchData(
    gameId = gameId,
    turnSearchInfo = state.turnSearchInfo,
  )

  val jsonPretty = Json { prettyPrint = false }

  return jsonPretty.encodeToString(rawData)
}

fun recordGameResult(
  file: File,
  lock: String,
  gameId: Int,
  seed: Long,
  timestamp: Long,
  state: State,
  winner: Winner?,
  turn: Int,
): List<String> {
  requireNotNull(winner)

  val gameResult =
      GameResult(
          gameId,
          seed,
          timestamp,
          turn,
          winner,
          state,
      )

  val gameResults = Json.encodeToString(gameResult) + "\n"

//  try {
//    synchronized(lock) {
//      return file.appendText(Json.encodeToString(gameResult) + "\n")
//    }
//  } catch (serializationFailure: Exception) {
//     If the state itself can't serialize (e.g. mid-mutation), at least note that.
//    file.appendText("Could not serialize gameState: ${serializationFailure.message}")
//    logger.error { "Crash at turn $turn in game $gameId — state dumped to $file" }
//  }

//  val rawJsonFilePath = if (file.path.endsWith(".jsonl")) {
//    file.path.substringBeforeLast(".jsonl") + "_raw_search_data.json"
//  } else {
//    file.path + "_raw_search_data.json"
//  }

//  recordRawSearchDataJson(File(rawJsonFilePath), lock, gameId, state)

  val searchData = recordSearchStatsCSV( seed, gameId, state)

  return listOf(gameResults,
    searchData
    )
}
