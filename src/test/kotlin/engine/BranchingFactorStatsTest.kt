package org.example.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.example.ai.humanEvaluation.SearchInfo
import org.example.model.Model
import org.example.model.Strength
import org.example.model.initializeState

class BranchingFactorStatsTest {

  @Test
  fun `test SearchInfo branching factors calculation`() {
    val searchInfo = SearchInfo(
        model = Model.MINIMAX,
        strength = Strength.EASY.name,
        totalNodesEvaluated = 100.0,
        totalAvailableMovesEvaluated = 1500.0,
        maxDepthReached = 4.0,
    )

    assertEquals(15.0, searchInfo.averageBranchingFactor, 0.0001)
    // 100^(1/4) = 3.16227766
    assertEquals(3.162277, searchInfo.effectiveBranchingFactor, 0.0001)
  }

  @Test
  fun `test SearchInfo branching factors edge cases`() {
    val emptySearchInfo = SearchInfo(
        model = Model.MINIMAX,
        strength = Strength.RANDOM.name,
    )

    assertEquals(0.0, emptySearchInfo.averageBranchingFactor, 0.0001)
    assertEquals(0.0, emptySearchInfo.effectiveBranchingFactor, 0.0001)

    val zeroDepthSearchInfo = SearchInfo(
        model = Model.MCTS,
        strength = Strength.EASY.name,
        totalNodesEvaluated = 50.0,
        totalAvailableMovesEvaluated = 200.0,
        maxDepthReached = 0.0,
    )

    assertEquals(4.0, zeroDepthSearchInfo.averageBranchingFactor, 0.0001)
    assertEquals(0.0, zeroDepthSearchInfo.effectiveBranchingFactor, 0.0001)
  }

  @Test
  fun `test calculateTurnSearchStats aggregation`() {
    val info1 = SearchInfo(
        model = Model.MINIMAX,
        strength = Strength.MEDIUM.name,
        totalNodesEvaluated = 10.0,
        totalAvailableMovesEvaluated = 100.0,
        maxDepthReached = 2.0,
    )
    val info2 = SearchInfo(
        model = Model.MINIMAX,
        strength = Strength.MEDIUM.name,
        totalNodesEvaluated = 90.0,
        totalAvailableMovesEvaluated = 900.0,
        maxDepthReached = 4.0,
    )

    val stats = calculateTurnSearchStats(
        gameId = 1,
        turn = 3,
        searchInfos = listOf(info1, info2),
    )

    assertEquals(1, stats?.gameId)
    assertEquals(3, stats?.turn)
    assertEquals("MINIMAX", stats?.model)
    assertEquals(4.0, stats?.depth)
    // Total nodes = 100, Total moves = 1000 => ABF = 10.0
    assertEquals(10.0, stats?.avgBranchingFactor)
    // EBF = 100^(1/4) = 3.16227766
    assertEquals(3.162277, stats?.effBranchingFactor ?: 0.0, 0.0001)
  }

  @Test
  fun `test recordSearchStatsCSV file output format`() {
    val tempFile = File.createTempFile("search_stats_test", ".csv")
    tempFile.deleteOnExit()

    val state = initializeState()
    val searchInfo = SearchInfo(
        model = Model.MCTS,
        strength = Strength.EASY.name,
        totalNodesEvaluated = 64.0,
        totalAvailableMovesEvaluated = 320.0,
        maxDepthReached = 3.0,
    )
    state.turnSearchInfo[1] = mutableListOf(mutableListOf(searchInfo))

//    recordSearchStatsCSV(
//        csvFile = tempFile,
//        lock = "testLock",
//        gameId = 42,
//        state = state,
//    )

    assertTrue(tempFile.exists())
    val lines = tempFile.readLines()
    assertEquals(2, lines.size)
    assertEquals("gameId,turn,model,depth,avg_branching_factor,eff_branching_factor", lines[0])

    val row = lines[1].split(",")
    assertEquals("42", row[0]) // gameId
    assertEquals("1", row[1])  // turn
    assertEquals("MCTS", row[2]) // model
    assertEquals("3.0", row[3]) // depth
    assertEquals("5.0", row[4]) // avg_branching_factor (320 / 64)
    // 64^(1/3) = 4.0
    assertEquals(4.0, row[5].toDouble(), 0.0001) // eff_branching_factor
  }

  @Test
  fun `test recordRawSearchDataJson file output format`() {
    val tempFile = File.createTempFile("raw_search_data_test", ".json")
    tempFile.deleteOnExit()

    val state = initializeState()
    val searchInfo = SearchInfo(
        model = Model.MCTS,
        strength = Strength.EASY.name,
        totalNodesEvaluated = 64.0,
        totalAvailableMovesEvaluated = 320.0,
        maxDepthReached = 3.0,
    )
    state.turnSearchInfo[1] = mutableListOf(mutableListOf(searchInfo))

//    recordRawSearchDataJson(
//        jsonFile = tempFile,
//        lock = "testLock",
//        gameId = 100,
//        state = state,
//    )

    assertTrue(tempFile.exists())
    val content = tempFile.readText()
    assertTrue(content.contains("\"gameId\": 100"))
    assertTrue(content.contains("\"model\": \"MCTS\""))
  }
}
