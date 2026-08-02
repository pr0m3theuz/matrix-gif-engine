@file:OptIn(ExperimentalUnsignedTypes::class)

package ai.minimax

import org.example.ai.humanEvaluation.AlphaBetaScoreBitPacked
import org.example.ai.mcts.PackedMove
import org.example.model.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ResolveBoardRemovalsTest {
  @Test
  fun resolveBoardRemovals() {
    val alphaBetaScore =
        AlphaBetaScoreBitPacked(move = null, alpha = 2.967696f, beta = Float.POSITIVE_INFINITY)
    val bitboard =
        Bitboard(
            whiteGIPF = 0u,
            whiteDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whitePUNCTLayer = ulongArrayOf(262144u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whiteTAMSK = 0u,
            whiteYINSH = 2097152u,
            whiteZERTZ = 134217728u,
            whitePotentials = 136577024u,
            blackGIPF = 33554432u,
            blackDVONNLayer = ulongArrayOf(1u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackPUNCTLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackTAMSK = 67108864u,
            blackYINSH = 524288u,
            blackZERTZ = 0u,
            blackPotentials = 68681729u,
        )
    val currentPlayer =
        Player(
            name = PlayerName.WHITE,
            piecesInReserve =
                mutableListOf(
                    234881024u,
                    33554432u,
                    201326592u,
                    218103808u,
                    218103808u,
                    218103808u,
                    184549376u,
                    184549376u,
                ),
            capturedPieces =
                mutableListOf(436207616u, 436207616u, 285212672u, 285212672u, 503316480u),
        )
    val depth = 1
    val opponentPlayer =
        Player(
            name = PlayerName.BLACK,
            piecesInReserve =
                mutableListOf(
                    452984832u,
                    452984832u,
                    452984832u,
                    503316480u,
                    469762048u,
                    469762048u,
                    486539264u,
                    486539264u,
                ),
            capturedPieces = mutableListOf(167772160u, 33554432u, 167772160u, 16777216u, 16777216u),
        )
    val initBitboard =
        Bitboard(
            whiteGIPF = 16777216u,
            whiteDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whitePUNCTLayer = ulongArrayOf(1318912u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whiteTAMSK = 0u,
            whiteYINSH = 2101376u,
            whiteZERTZ = 134217728u,
            whitePotentials = 136577024u,
            blackGIPF = 33554432u,
            blackDVONNLayer = ulongArrayOf(1u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackPUNCTLayer = ulongArrayOf(1048576u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackTAMSK = 67108864u,
            blackYINSH = 524288u,
            blackZERTZ = 0u,
            blackPotentials = 68681729u,
        )
    val removePiecesPowerset =
        listOf(
            PackedMove.Multiple(
                values =
                    listOf(
                        16646023u,
                        16646029u,
                        16646036u,
                        16646023u,
                        16646028u,
                        16646040u,
                        1090387860u,
                    )
            ),
            PackedMove.Multiple(
                values =
                    listOf(
                        16646043u,
                        16646023u,
                        16646029u,
                        16646036u,
                        16646023u,
                        16646028u,
                        16646040u,
                        1090387860u,
                    )
            ),
            PackedMove.Multiple(
                values =
                    listOf(
                        16646034u,
                        16646023u,
                        16646029u,
                        16646036u,
                        16646023u,
                        16646028u,
                        16646040u,
                        1090387860u,
                    )
            ),
            PackedMove.Multiple(
                values =
                    listOf(
                        16646043u,
                        16646034u,
                        16646023u,
                        16646029u,
                        16646036u,
                        16646023u,
                        16646028u,
                        16646040u,
                        1090387860u,
                    )
            ),
        )

	  val removePiecesPowersetRe = mutableListOf<PackedMove>()
	  initBitboard.identifyPiecesToRemove(currentPlayer, removePiecesPowersetRe)

	  // region Retrieve/Capture Pieces with Player Potential Powerset
	  if (removePiecesPowersetRe.isNotEmpty()) {
		  removePiecesPowersetRe.forEachIndexed { index, removePieces ->
			  // 1. Generate your powerset of choices for these lines
			  // 2. Loop through each choice in the powerset:
			  // a. Apply the piece removals to the board

			  /**
			   * TODO causes stack overflow error, but an empty list is necessary as a player can leave the
			   * stack in play TODO Minimax/MCTS — what it would be like to remove at least one of these
			   * pieces if all pieces have potentials use line score heuristic and pieces in reserve TODO
			   * return of a list containing different combinations of bit positions
			   */
			  require(removePieces is PackedMove.Multiple)
			  val playerPiecesWithPotentialToRemove = removePieces.values

			  val retrievedCapturedPieces = mutableListOf<UInt>()
			  initBitboard.removeSelectedPiecesToRemove(
				  player = currentPlayer,
				  piecesToRemove = playerPiecesWithPotentialToRemove,
				  movesBuffer = retrievedCapturedPieces,
			  )
		  }
	  }
  }

    @Test
    fun `neutralized piece no removed`() {
        val bitboard = Bitboard(
            whiteGIPF = 385UL,
            whiteDVONNLayer = ulongArrayOf(4294967296UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 2214592512UL,
            whiteYINSH = 0UL,
            whiteZERTZ = 0UL,
            whitePotentials = 6509559808UL,
            blackGIPF = 34078720UL,
            blackDVONNLayer = ulongArrayOf(4294967296UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 0UL,
            blackYINSH = 0UL,
            blackZERTZ = 0UL,
            blackPotentials = 0UL
        )

        val initBitboard = Bitboard(
            whiteGIPF = 385UL,
            whiteDVONNLayer = ulongArrayOf(4294967296UL, 4294967296UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 2214592512UL,
            whiteYINSH = 0UL,
            whiteZERTZ = 0UL,
            whitePotentials = 6509559808UL,
            blackGIPF = 36175872UL,
            blackDVONNLayer = ulongArrayOf(68719476736UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 134217728UL,
            blackYINSH = 0UL,
            blackZERTZ = 0UL,
            blackPotentials = 134217728UL
        )

// --- Players ---

        val currentPlayer = Player(
            name = PlayerName.BLACK, // adjust enum class name if needed
            model = Model.MINIMAX,
            strength = Strength.EASY,
            piecesInReserve = mutableListOf(
                771751936u, 771751936u, 771751936u, 838860800u, 838860800u,
                838860800u, 973078528u, 973078528u, 973078528u, 905969664u,
                704643072u, 704643072u, 905969664u
            ),
        )

        val opponentPlayer = Player(
            name = PlayerName.WHITE, // adjust enum class name if needed
            model = Model.MINIMAX,
            strength = Strength.EASY,
            piecesInReserve = mutableListOf(
                436207616u, 436207616u, 436207616u, 234881024u, 234881024u,
                234881024u, 167772160u, 301989888u, 301989888u, 301989888u,
                369098752u, 369098752u
            ),
        )

// --- Other Parameters ---

        val depth = 1

        val rng = kotlin.random.Random(0) // seeded RNG for deterministic test execution

        val removePiecesPowerset = listOf(
            PackedMove.Multiple(values = listOf(16646037u, 16646052u, 33423264u)),
            PackedMove.Multiple(values = listOf(16646043u, 16646037u, 16646052u, 33423264u))
        )

// --- Test ---
        val newBitboard = initBitboard.deepCopy()

        val removePiecesPowersetRe = mutableListOf<PackedMove>()
        newBitboard.identifyPiecesToRemove(currentPlayer, removePiecesPowersetRe)

        // region Retrieve/Capture Pieces with Player Potential Powerset
        if (removePiecesPowersetRe.isNotEmpty()) {
            removePiecesPowersetRe.forEachIndexed { index, removePieces ->
                // 1. Generate your powerset of choices for these lines
                // 2. Loop through each choice in the powerset:
                // a. Apply the piece removals to the board

                /**
                 * TODO causes stack overflow error, but an empty list is necessary as a player can leave the
                 * stack in play TODO Minimax/MCTS — what it would be like to remove at least one of these
                 * pieces if all pieces have potentials use line score heuristic and pieces in reserve TODO
                 * return of a list containing different combinations of bit positions
                 */
                require(removePieces is PackedMove.Multiple)
                val playerPiecesWithPotentialToRemove = removePieces.values

                val retrievedCapturedPieces = mutableListOf<UInt>()
                newBitboard.removeSelectedPiecesToRemove(
                    player = currentPlayer,
                    piecesToRemove = playerPiecesWithPotentialToRemove,
                    movesBuffer = retrievedCapturedPieces,
                )

                currentPlayer.addRetrievedCapturedPieces(retrievedCapturedPieces)

                val newlyStackedPieces = currentPlayer.combinePieces()

                val postPieceRemovalBitboardState = newBitboard.deepCopy()
                newBitboard.diff(postPieceRemovalBitboardState)

                newBitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

                newBitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

                // d. UNDO the piece removals to evaluate the next choice
                currentPlayer.uncombinePieces(newlyStackedPieces)

                newBitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

                currentPlayer.removeRetrievedCapturedPieces(retrievedCapturedPieces)

                newBitboard.undoRetrieveAndCapturePieces(retrievedCapturedPieces)
                //      bitboard.undoRetrieveAndCapturePieces(piecesWithPotentialPowerset)

                newBitboard.diff(initBitboard)

                newBitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
            }
        }

        assertEquals(initBitboard, newBitboard)
    }
}
