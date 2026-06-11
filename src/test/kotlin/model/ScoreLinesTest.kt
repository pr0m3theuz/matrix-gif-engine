package model

import kotlinx.serialization.json.Json
import org.example.model.State
import org.example.model.printHexGrid
import org.junit.jupiter.api.Test
import java.io.File

class ScoreLinesTest {
  @Test fun scoreLines() {
	  val filename =
		  "/Users/darronporter/Downloads/Dissertation/code/src/test/kotlin/model/scoreLines_test.json"

	  val loadedState = Json.decodeFromString<State>(File(filename).bufferedReader().readText())

	  loadedState.board.printHexGrid()

	  org.example.model.scoreLines(loadedState.currentPlayer, loadedState.lines.verticalLines)

	  org.example.model.scoreLines(loadedState.currentPlayer, loadedState.lines.upwardRightLines)

	  org.example.model.scoreLines(loadedState.currentPlayer, loadedState.lines.downwardRightLines)

	}
}
