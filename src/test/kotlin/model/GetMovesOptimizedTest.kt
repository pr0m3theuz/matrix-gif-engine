@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import kotlin.test.assertEquals
import org.example.ai.mcts.PackedMove
import org.example.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

class GetMovesOptimizedTest {
  @Test
  fun getTamskMoves() {
    val bitboard =
        Bitboard(
            whiteGIPF = 551970406400UL,
            whiteDVONNLayer = ulongArrayOf(8388612UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(4194304UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 4295229568UL,
            whiteYINSH = 2576UL,
            whiteZERTZ = 274877939713UL,
            whitePotentials = 12616341UL,
            blackGIPF = 1048648UL,
            blackDVONNLayer = ulongArrayOf(148480UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(524288UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 42949672962UL,
            blackYINSH = 65536UL,
            blackZERTZ = 137457827840UL,
            blackPotentials = 180391461890UL,
        )

    val currentPlayer =
        Player(
            name = PlayerName.WHITE,
            abbreviation = "W",
        )
    val movesBuffer: MutableList<PackedMove> = mutableListOf()
    bitboard.getTamskMoves(currentPlayer, movesBuffer)

    assertTrue(movesBuffer.isEmpty())
  }

  @Test
  fun getZertzMoves() {
    val bitboard =
        Bitboard(
            whiteGIPF = 542720UL,
            whiteDVONNLayer = ulongArrayOf(4378853376u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whitePUNCTLayer = ulongArrayOf(2097282u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whiteTAMSK = 137443148288UL,
            whiteYINSH = 17448304896UL,
            whiteZERTZ = 551903559680UL,
            whitePotentials = 711175963522UL,
            blackGIPF = 1073751040UL,
            blackDVONNLayer = ulongArrayOf(9437188u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackPUNCTLayer = ulongArrayOf(65576u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackTAMSK = 42949672976UL,
            blackYINSH = 671088641UL,
            blackZERTZ = 274911494144UL,
            blackPotentials = 318541758525UL,
        )

    val currrentPlayer = Player(name = PlayerName.BLACK, abbreviation = "B")

    val movesBuffer = mutableListOf<PackedMove>()
    bitboard.getZertzMoves(
        currrentPlayer,
        columnInfos,
        movesBuffer,
    )

    val expectedPiece =
        Piece(
            abbreviation = "BZ",
            potential = true,
            colorName = PlayerName.BLACK,
            type = PieceType.ZERTZ,
            isNeutralized = false,
        )

    val expectedPackedPiece = expectedPiece.pack()

    val nullPiece = 63.toUByte()

    val expectedMoves =
        listOf<PossibleBitMove>(
            PossibleBitMove(
                piece = expectedPiece,
                sourceBit = 32768UL,
                targetBit = 131072UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.BLACK,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = expectedPiece,
                sourceBit = 33554432UL,
                targetBit = 68719476736UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.BLACK,
                moveType = MoveType.UsePotential,
            ),
        )

    assertEquals(
        expectedMoves.first(),
        (movesBuffer.first() as PackedMove.Single).value.toPossibleBitMove(),
    )

    logger.info { "" + (movesBuffer) }
  }

  @Test fun getYinshMoves() {}

  @Test fun getDvonnMoves() {}

  @Test fun getPunctMoves() {}

  @Test fun identifyAvailableMoves() {}

  @Test fun identifyPiecesToRemove() {}

  @Test
  fun `get DVONN Moves when Blocked`() {
    /**
     * TODO The DVONN potential has the ability to jump atop any of the opponent's DVONN pieces
     * (individual pieces or stacks) as long as the both DVONN pieces are on the same line and there
     * are no other pieces in between them.
     */
    // 1. Initialize Active Bitboard (with size-8 arrays)
    val bitboard =
      Bitboard(
        whiteGIPF = 2UL,
        whiteDVONNLayer = ulongArrayOf(1UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        whitePUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        whiteTAMSK = 0UL,
        whiteYINSH = 0UL,
        whiteZERTZ = 0UL,
        whitePotentials = 1UL,
        blackGIPF = 0UL,
        blackDVONNLayer = ulongArrayOf(8UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        blackPUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        blackTAMSK = 0UL,
        blackYINSH = 0UL,
        blackZERTZ = 0UL,
        blackPotentials = 8UL,
      )

    // 2. Initialize Init Bitboard (slightly different whitePotentials and whiteDVONNLayer)
    val initBitboard =
      Bitboard(
        whiteGIPF = 0UL,
        whiteDVONNLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        whitePUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        whiteTAMSK = 0UL,
        whiteYINSH = 0UL,
        whiteZERTZ = 0UL,
        whitePotentials = 0UL,
        blackGIPF = 0UL,
        blackDVONNLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        blackPUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        blackTAMSK = 0UL,
        blackYINSH = 0UL,
        blackZERTZ = 0UL,
        blackPotentials = 0UL,
      )

    // 3. Initialize Current and Opponent Players
    val currentPlayer =
      Player(
        name = PlayerName.WHITE,
        abbreviation = "W",
        piecesInReserve = mutableListOf(piece("WY", potential = false).pack()),
      )

    val opponentPlayer =
      Player(
        name = PlayerName.BLACK,
        abbreviation = "B",
        piecesInReserve = mutableListOf(),
      )

    val dvonnMoves = mutableListOf<PackedMove>()
    bitboard.getDvonnMoves(
      player = currentPlayer,
      columnInfos = org.example.model.columnInfos,
      dvonnMoves,
    )

    Assertions.assertTrue(dvonnMoves.isEmpty())
  }
}
