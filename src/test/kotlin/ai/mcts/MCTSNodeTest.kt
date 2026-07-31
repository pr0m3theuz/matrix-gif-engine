@file:OptIn(ExperimentalUnsignedTypes::class)

package ai.mcts

import org.example.ai.mcts.selectMoveMCTS
import org.example.model.Bitboard
import org.example.model.State
import org.example.model.TurnPhase
import org.example.model.convertBoardToBitboard
import org.example.model.initializeState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MCTSNodeTest {
  @Test fun addRandomChildNode() {
    // test node generation in the first 6 moves

    var state: State = initializeState()

    val bitboard = Bitboard()

    selectMoveMCTS(
      bitboard = bitboard,
      currentPlayer = state.currentPlayer,
      nextPlayer = state.nextPlayer,
      turnPhase = TurnPhase.PlayerInputWindow,
      rounds = 0..999,
      rng = Random(1)
    )
  }
}
