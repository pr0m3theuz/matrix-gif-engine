@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.ai

import kotlin.time.Duration.Companion.seconds
import org.example.engine.TranspositionTable
import org.example.model.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DoActionGetTurnPhaseTest {
  @Test
  fun createChildState() {

    val childBitboard =
        Bitboard(
            whiteGIPF = 37_748_737uL,
            whiteDVONNLayer = ulongArrayOf(0uL, 0uL, 0uL, 0uL, 0uL, 0uL, 0uL, 0uL),
            whitePUNCTLayer = ulongArrayOf(8_606_711_808uL, 0uL, 0uL, 0uL, 0uL, 0uL, 0uL, 0uL),
            whiteTAMSK = 343_597_383_680uL,
            whiteYINSH = 256uL,
            whiteZERTZ = 136_314_880uL,
            whitePotentials = 352_340_410_624uL,
            blackGIPF = 0uL,
            blackDVONNLayer = ulongArrayOf(8_421_376uL, 0uL, 0uL, 0uL, 0uL, 0uL, 0uL, 0uL),
            blackPUNCTLayer = ulongArrayOf(0uL, 0uL, 0uL, 0uL, 0uL, 0uL, 0uL, 0uL),
            blackTAMSK = 17_179_869_184uL,
            blackYINSH = 520uL,
            blackZERTZ = 22uL,
            blackPotentials = 17_188_291_102uL,
        )

    val childCurrentPlayer =
        Player(
            name = PlayerName.BLACK, // or PlayerColor.BLACK / PlayerName.BLACK
            model = Model.MCTS,
            strength = Strength.RANDOM,
            piecesInReserve =
                mutableListOf(
                    704643072u,
                    838860800u,
                    905969664u,
                    973078528u,
                    973078528u,
                    973078528u,
                    704643072u,
                    603979776u,
                    603979776u,
                    603979776u,
                ),
            capturedPieces = mutableListOf(),
            timeControl = false,
            timeDuration = 1.seconds,
            useRAVE = false,
            enableFPU = false,
            enablePW = true,
            iterations = 999,
            depth = 1,
            collector = null,
            transpositionTable = TranspositionTable(),
            killerMoves = mutableListOf(),
            captureMoves = mutableListOf(),
            historyTable = mutableListOf(),
        )

    val childNextPlayer =
        Player(
            name = PlayerName.WHITE, // or PlayerColor.WHITE / PlayerName.WHITE
            model = Model.MCTS,
            strength = Strength.RANDOM,
            piecesInReserve =
                mutableListOf(
                    167772160u,
                    234881024u,
                    301989888u,
                    301989888u,
                    369098752u,
                    369098752u,
                    369098752u,
                    436207616u,
                ),
            capturedPieces = mutableListOf(),
            timeControl = false,
            timeDuration = 1.seconds,
            useRAVE = false,
            enableFPU = false,
            enablePW = true,
            iterations = 0,
            depth = 1,
            collector = null,
            transpositionTable = TranspositionTable(),
            killerMoves = mutableListOf(),
            captureMoves = mutableListOf(),
            historyTable = mutableListOf(),
        )

    val turnHasHadNormalMove = false
    val turnPhase = TurnPhase.PieceRemoval
    val previousTurnPhases = emptyList<TurnPhase>() + turnPhase
    val (nodeCurrentPlayer, nodeNextPlayer, nodeTurnPhase, nodeMoves) =
        org.example.ai.mcts.createChildState(
            childBitboard,
            childCurrentPlayer,
            childNextPlayer,
            turnPhase,
            previousTurnPhases.any { it == TurnPhase.PlayerInputWindow },
        )

    assertTrue(nodeCurrentPlayer == childCurrentPlayer)
    assertTrue(nodeNextPlayer == childNextPlayer)
    assertTrue(nodeTurnPhase == TurnPhase.PlayerInputWindow)
  }
}
