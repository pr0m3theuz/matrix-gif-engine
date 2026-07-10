package ai.mcts

import org.example.ai.mcts.selectMoveMCTS
import org.example.model.State
import org.example.model.convertBoardToBitboard
import org.example.model.initializeState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SelectMoveMCTSTest {
  @Test fun calculateUCTScore() {}

  @Test fun `select Move Using MCTS`() {
	  var state: State = initializeState()

	  val bitboard = convertBoardToBitboard(state.board)

	  selectMoveMCTS(
		  bitboard = bitboard,
		  currentPlayer = state.currentPlayer,
		  nextPlayer = state.nextPlayer,
			rounds = 0..9999
	  )
	}

  @Test fun simulateRandomGame() {}

  @Test fun simulatePlayerTurn() {}

  @Test fun simulatePlayerMove() {}

  @Test fun simulatePieceRetrievalCapture() {}
}
