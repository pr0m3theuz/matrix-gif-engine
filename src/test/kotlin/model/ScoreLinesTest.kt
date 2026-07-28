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

//    loadedState.board.printHexGrid("TEST")
//
//    org.example.model.scoreLines(loadedState.currentPlayer, loadedState.lines.verticalLines)
//
//    org.example.model.scoreLines(loadedState.currentPlayer, loadedState.lines.upwardRightLines)
//
//    org.example.model.scoreLines(loadedState.currentPlayer, loadedState.lines.downwardRightLines)
  }
}
