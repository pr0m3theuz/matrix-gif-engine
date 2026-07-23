package model

import java.io.File
import kotlinx.serialization.json.Json
import org.example.model.State
import org.example.model.columnInfos
import org.example.model.printHexGrid
import org.junit.jupiter.api.Test

class ScoreLinesTest {
  @Test
  fun scoreLines() {
    val filename =
        "/Users/darronporter/Downloads/Dissertation/code/src/test/kotlin/model/scoreLines_test.json"

    val loadedState = Json.decodeFromString<State>(File(filename).bufferedReader().readText())

    loadedState.board.printHexGrid("TEST")

    org.example.model.scoreLines(loadedState.currentPlayer, loadedState.lines.verticalLines)

    org.example.model.scoreLines(loadedState.currentPlayer, loadedState.lines.upwardRightLines)

    org.example.model.scoreLines(loadedState.currentPlayer, loadedState.lines.downwardRightLines)
  }

  @Test
  fun scoreLines_empty() {
    /*val a: List<MutableMap<ULong, MutableList<Pair<ULong, ULong>>>> = columnInfos.map { column ->
      column.shiftPairs.groupByTo(mutableMapOf()) { it.first }
    }

    val b = mutableMapOf<ULong, MutableList<Pair<ULong, ULong>>>()

    columnInfos.forEach { column ->
      column.shiftPairs
          .groupByTo(mutableMapOf()) { it.first }
          .forEach { (lng, pairs) ->
            b[lng] = b[lng]?.plus(pairs)?.toMutableList() ?: pairs
          }

      column.shiftPairs
        .groupByTo(mutableMapOf()) { it.second }
        .forEach { (lng, pairs) ->
          val pair = Pair(pairs.first().second, pairs.first().first)
          b[lng] = b[lng]?.plus(pair)?.toMutableList() ?: mutableListOf(pair)
        }
    }

    val c: MutableMap<ULong, ULong> = b.mapValuesTo(mutableMapOf()) {
      it.value.sumOf { it.second }
    }*/

    val neighbouringBitsBitmasks: MutableMap<ULong, ULong> = mutableMapOf()

    for (column in columnInfos) {
      for ((a, b) in column.shiftPairs) {
        neighbouringBitsBitmasks[a] = neighbouringBitsBitmasks.getOrDefault(a, 0UL) or b
        neighbouringBitsBitmasks[b] = neighbouringBitsBitmasks.getOrDefault(b, 0UL) or a
      }
    }

//    println(Json.encodeToString(a))
//    println(Json.encodeToString(b))
//    println(Json.encodeToString(c))
    println(Json.encodeToString(neighbouringBitsBitmasks))
  }

  @Test
  fun `count fatures`() {
    val TURNS_SINCE = 50 // 8
    val LIBERTIES = TURNS_SINCE + 8 // 6
    val LIBERTIES_AFTER = LIBERTIES + 6 // 6
    val RETRIVAL_SIZE = LIBERTIES_AFTER + 6 // 8
    val CAPTURE_SIZE = RETRIVAL_SIZE + 8 // 8
    val SELF_ATARI_SIZE = CAPTURE_SIZE + 8 // 8
    val CURRENT_PLAYER_COLOR = SELF_ATARI_SIZE + 1

    // features
    val features = CURRENT_PLAYER_COLOR + 1

    println(features)
    println()
  }
}
