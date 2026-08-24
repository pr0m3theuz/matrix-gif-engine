package ai.mcts

import org.example.ai.mcts.selectMoveMCTS
import org.example.model.Player
import org.example.model.PlayerName
import org.example.model.State
import org.example.model.TurnPhase
import org.example.model.convertBoardToBitboard
import org.example.model.initializeState
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SelectMoveMCTSTest {
  @Test fun calculateUCTScore() {}

  @Test fun `select Move Using MCTS`() {
	  var state: State = initializeState(
		  Player(name = PlayerName.WHITE),
		  Player(name = PlayerName.BLACK),
		)

	  val bitboard = convertBoardToBitboard(state.board)

	  selectMoveMCTS(
		  bitboard = bitboard,
		  currentPlayer = state.currentPlayer,
		  nextPlayer = state.nextPlayer,
		  turnPhase = TurnPhase.PlayerInputWindow,
			rounds = 0..9999,
		  rng = Random(1)
	  )
	}

  @Test fun simulateRandomGame() {}

  @Test fun simulatePlayerTurn() {}

  @Test fun simulatePlayerMove() {}

  @Test fun simulatePieceRetrievalCapture() {}
}
