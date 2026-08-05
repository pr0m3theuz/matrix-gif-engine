@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import org.example.ai.humanEvaluation.BestPackedMove
import org.example.ai.mcts.PackedMove
import org.example.model.Bitboard
import org.example.model.Player
import org.example.model.PlayerName
import org.example.model.undoUsePiecePotential
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UndoUsePiecePotentialTest {

  @Test
  fun undoUsePiecePotential() {
    val bitboard =
        Bitboard(
            whiteGIPF = 97u,
            whiteDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whitePUNCTLayer = ulongArrayOf(384u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whiteTAMSK = 0u,
            whiteYINSH = 0u,
            whiteZERTZ = 0u,
            whitePotentials = 128u,
            blackGIPF = 203423744u,
            blackDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackPUNCTLayer = ulongArrayOf(256u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackTAMSK = 0u,
            blackYINSH = 0u,
            blackZERTZ = 0u,
            blackPotentials = 256u,
        )
    val currentPlayer =
        Player(
            name = PlayerName.WHITE,
            piecesInReserve =
                mutableListOf(
                    201326592u,
                    201326592u,
                    201326592u,
                    184549376u,
                    184549376u,
                    184549376u,
                    218103808u,
                    218103808u,
                    218103808u,
                    234881024u,
                    167772160u,
                    167772160u,
                    167772160u,
                    234881024u,
                ),
        )
    val depth = 1u
    val opponentPlayer =
        Player(
            name = PlayerName.BLACK,
            piecesInReserve =
                mutableListOf(
                    436207616u,
                    436207616u,
                    436207616u,
                    452984832u,
                    452984832u,
                    452984832u,
                    469762048u,
                    469762048u,
                    469762048u,
                    486539264u,
                    486539264u,
                    486539264u,
                    503316480u,
                    503316480u,
                ),
        )
    val initBitboard =
        Bitboard(
            whiteGIPF = 97u,
            whiteDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whitePUNCTLayer = ulongArrayOf(128u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whiteTAMSK = 0u,
            whiteYINSH = 0u,
            whiteZERTZ = 0u,
            whitePotentials = 128u,
            blackGIPF = 203423744u,
            blackDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackPUNCTLayer = ulongArrayOf(256u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackTAMSK = 0u,
            blackYINSH = 0u,
            blackZERTZ = 0u,
            blackPotentials = 256u,
        )
    val availableMoves = 221
    val index = 220
    val packedMove = PackedMove.Single(value = 452707208u)
    val preMoveBitboardState =
        Bitboard(
            whiteGIPF = 97u,
            whiteDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whitePUNCTLayer = ulongArrayOf(128u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whiteTAMSK = 0u,
            whiteYINSH = 0u,
            whiteZERTZ = 0u,
            whitePotentials = 128u,
            blackGIPF = 203423744u,
            blackDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackPUNCTLayer = ulongArrayOf(256u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackTAMSK = 0u,
            blackYINSH = 0u,
            blackZERTZ = 0u,
            blackPotentials = 256u,
        )
    val moveValue = 452707208u
    val postUsePotentialBitboardState =
        Bitboard(
            whiteGIPF = 97u,
            whiteDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whitePUNCTLayer = ulongArrayOf(128u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whiteTAMSK = 0u,
            whiteYINSH = 0u,
            whiteZERTZ = 0u,
            whitePotentials = 0u,
            blackGIPF = 203423744u,
            blackDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackPUNCTLayer = ulongArrayOf(256u, 256u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackTAMSK = 0u,
            blackYINSH = 0u,
            blackZERTZ = 0u,
            blackPotentials = 256u,
        )

    val move = BestPackedMove(move = PackedMove.Single(value = 452707208u), score = 55.811718f)

	  val modifiedBitboard =
        Bitboard(
            whiteGIPF = 97u,
            whiteDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whitePUNCTLayer = ulongArrayOf(128u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whiteTAMSK = 0u,
            whiteYINSH = 0u,
            whiteZERTZ = 0u,
            whitePotentials = 0u,
            blackGIPF = 203423744u,
            blackDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackPUNCTLayer = ulongArrayOf(256u, 256u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackTAMSK = 0u,
            blackYINSH = 0u,
            blackZERTZ = 0u,
            blackPotentials = 256u,
        )
	  modifiedBitboard.undoUsePiecePotential(moveValue)

      assertEquals(preMoveBitboardState, modifiedBitboard)
  }
}
