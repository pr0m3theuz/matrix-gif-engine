@file:OptIn(ExperimentalUnsignedTypes::class)

package ai.mcts

import kotlin.random.Random
import org.example.ai.mcts.selectMoveMCTS
import org.example.model.*
import org.junit.jupiter.api.Test

class MCTSNodeTest {
  @Test
  fun addRandomChildNode() {
    // test node generation in the first 6 moves

    var state: State =
        initializeState(
            Player(name = PlayerName.WHITE),
            Player(name = PlayerName.BLACK),
        )

    val bitboard = Bitboard()

    selectMoveMCTS(
        bitboard = bitboard,
        currentPlayer = state.currentPlayer,
        nextPlayer = state.nextPlayer,
        turnPhase = TurnPhase.PlayerInputWindow,
        rounds = 0..999,
        rng = Random(1),
    )
  }
}
