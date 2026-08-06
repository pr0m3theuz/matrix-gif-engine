@file:OptIn(ExperimentalUnsignedTypes::class)

package ai.minimax

import kotlin.test.assertEquals
import org.example.ai.humanEvaluation.AlphaBetaScoreBitPacked
import org.example.ai.humanEvaluation.BestPackedMove
import org.example.ai.humanEvaluation.alphaBetaPackedMove
import org.example.ai.mcts.PackedMove
import org.example.model.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ResolveBoardRemovalsTest {
  @Test
  fun resolveBoardRemovals() {
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
         * TODO causes stack overflow error, but an empty list is necessary as a player can leave
         * the stack in play TODO Minimax/MCTS — what it would be like to remove at least one of
         * these pieces if all pieces have potentials use line score heuristic and pieces in reserve
         * TODO return of a list containing different combinations of bit positions
         */
        require(removePieces is PackedMove.Multiple)
        val playerPiecesWithPotentialToRemove = removePieces.values

        val retrievedCapturedPieces = mutableListOf<UInt>()
        initBitboard.removeSelectedPieces(
            player = currentPlayer,
            piecesToRemove = playerPiecesWithPotentialToRemove,
            movesBuffer = retrievedCapturedPieces,
        )
      }
    }
  }

  @Test
  fun `neutralized piece no removed`() {
    val bitboard =
        Bitboard(
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
            blackPotentials = 0UL,
        )

    val initBitboard =
        Bitboard(
            whiteGIPF = 385UL,
            whiteDVONNLayer =
                ulongArrayOf(4294967296UL, 4294967296UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
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
            blackPotentials = 134217728UL,
        )

    // --- Players ---

    val currentPlayer =
        Player(
            name = PlayerName.BLACK, // adjust enum class name if needed
            model = Model.MINIMAX,
            strength = Strength.EASY,
            piecesInReserve =
                mutableListOf(
                    771751936u,
                    771751936u,
                    771751936u,
                    838860800u,
                    838860800u,
                    838860800u,
                    973078528u,
                    973078528u,
                    973078528u,
                    905969664u,
                    704643072u,
                    704643072u,
                    905969664u,
                ),
        )

    val opponentPlayer =
        Player(
            name = PlayerName.WHITE, // adjust enum class name if needed
            model = Model.MINIMAX,
            strength = Strength.EASY,
            piecesInReserve =
                mutableListOf(
                    436207616u,
                    436207616u,
                    436207616u,
                    234881024u,
                    234881024u,
                    234881024u,
                    167772160u,
                    301989888u,
                    301989888u,
                    301989888u,
                    369098752u,
                    369098752u,
                ),
        )

    // --- Other Parameters ---

    val depth = 1

    val rng = kotlin.random.Random(1) // seeded RNG for deterministic test execution

    val removePiecesPowerset =
        listOf(
            PackedMove.Multiple(values = listOf(16646037u, 16646052u, 33423264u)),
            PackedMove.Multiple(values = listOf(16646043u, 16646037u, 16646052u, 33423264u)),
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
         * TODO causes stack overflow error, but an empty list is necessary as a player can leave
         * the stack in play TODO Minimax/MCTS — what it would be like to remove at least one of
         * these pieces if all pieces have potentials use line score heuristic and pieces in reserve
         * TODO return of a list containing different combinations of bit positions
         */
        require(removePieces is PackedMove.Multiple)
        val playerPiecesWithPotentialToRemove = removePieces.values

        val retrievedCapturedPieces = mutableListOf<UInt>()
        newBitboard.removeSelectedPieces(
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

  @Test
  fun `null piece`() {
    // 1. Initialize Active and Initial Bitboards
    val bitboard =
        Bitboard(
            whiteGIPF = 67108881UL,
            whiteDVONNLayer = ulongArrayOf(256UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(16384UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 4204544UL,
            whiteYINSH = 4098UL,
            whiteZERTZ = 512UL,
            whitePotentials = 4225794UL,
            blackGIPF = 838860800UL,
            blackDVONNLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(274877907968UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 8590458880UL,
            blackYINSH = 32864UL,
            blackZERTZ = 0UL,
            blackPotentials = 283468399712UL,
        )

    val initBitboard =
        Bitboard(
            whiteGIPF = 67108881UL,
            whiteDVONNLayer = ulongArrayOf(256UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(16384UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 4204544UL,
            whiteYINSH = 4098UL,
            whiteZERTZ = 512UL,
            whitePotentials = 4225794UL,
            blackGIPF = 838860800UL,
            blackDVONNLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(274877907968UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 8590458880UL,
            blackYINSH = 32864UL,
            blackZERTZ = 0UL,
            blackPotentials = 283468399712UL,
        )

    // 2. Initialize Players (Using MINIMAX/EASY properties and packed integer lists)
    val currentPlayer =
        Player(
            name = PlayerName.WHITE,
            model = Model.MINIMAX,
            strength = Strength.EASY,
            piecesInReserve =
                mutableListOf(
                    436207616u,
                    369098752u,
                    369098752u,
                    234881024u,
                    301989888u,
                    436207616u,
                    234881024u,
                ),
            capturedPieces = mutableListOf(704643072u),
        )

    val opponentPlayer =
        Player(
            name = PlayerName.BLACK,
            model = Model.MINIMAX,
            strength = Strength.EASY,
            piecesInReserve =
                mutableListOf(
                    905969664u,
                    905969664u,
                    771751936u,
                    905969664u,
                    771751936u,
                    771751936u,
                    973078528u,
                ),
            capturedPieces = mutableListOf(),
        )

    // 3. Search and Search state parameters
    val depth = 3
    val maxDepth = 3
    val index = 28

    // 4. Initialize Kotlin XorWowRandom with any seed (matching class type)
    val rng = kotlin.random.Random(1)

    // 5. Initialize Powersets List
    val removePiecesPowerset =
        listOf(
            PackedMove.Multiple(
                listOf(16646025u, 16646027u, 16646028u, 16646029u, 16646030u, 1090387850u)
            ),
            PackedMove.Multiple(listOf(16646025u, 16646027u, 16646028u, 16646029u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646027u, 16646028u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646027u, 16646029u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646028u, 16646029u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646027u, 16646028u, 16646029u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646027u, 16646028u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646027u, 16646029u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646028u, 16646029u, 1090387850u)),
            PackedMove.Multiple(listOf(16646027u, 16646028u, 16646029u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646027u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646028u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646027u, 16646028u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646029u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646027u, 16646029u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646028u, 16646029u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646027u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646028u, 1090387850u)),
            PackedMove.Multiple(listOf(16646027u, 16646028u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646029u, 1090387850u)),
            PackedMove.Multiple(listOf(16646027u, 16646029u, 1090387850u)),
            PackedMove.Multiple(listOf(16646028u, 16646029u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646027u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646028u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646029u, 16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(16646025u, 1090387850u)),
            PackedMove.Multiple(listOf(16646027u, 1090387850u)),
            PackedMove.Multiple(listOf(16646028u, 1090387850u)), // matches removePieces
            PackedMove.Multiple(listOf(16646029u, 1090387850u)),
            PackedMove.Multiple(listOf(16646030u, 1090387850u)),
            PackedMove.Multiple(listOf(1090387850u)),
        )

      val alphaBetaScore = AlphaBetaScoreBitPacked(
          move = PackedMove.Multiple(listOf(16646025u, 16646027u, 16646028u, 1090387850u)),
          alpha = 0,
          beta = 2147483647
      )

    removalLoop@ for ((index, removePieces) in removePiecesPowerset.withIndex()) {
      require(removePieces is PackedMove.Multiple)
      val piecesToRemove = removePieces.values.distinct()

      val retrievedCapturedPieces = mutableListOf<UInt>()
      bitboard.removeSelectedPieces(
          player = currentPlayer,
          piecesToRemove = piecesToRemove,
          movesBuffer = retrievedCapturedPieces,
      )

      currentPlayer.addRetrievedCapturedPieces(retrievedCapturedPieces)

      val newlyStackedPieces = currentPlayer.combinePieces()

      val postPieceRemovalBitboardState = bitboard.deepCopy()
      bitboard.diff(postPieceRemovalBitboardState)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      val move =
          BestPackedMove(
              move = removePieces,
              score =
                  alphaBetaPackedMove(
                          maxDepth = maxDepth,
                          depth = depth.minus(1),
                          // state = state,
                          bitboard = bitboard.deepCopy(),
                          currentPlayer = opponentPlayer,
                          opponentPlayer = currentPlayer,
                          // gameTree = gameTree,
                          alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                          rng = rng,
                      )
                      .score,
          )

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      // d. UNDO the piece removals to evaluate the next choice
      currentPlayer.uncombinePieces(newlyStackedPieces)

      //      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      currentPlayer.removeRetrievedCapturedPieces(retrievedCapturedPieces)
      bitboard.undoRetrieveAndCapturePieces(retrievedCapturedPieces)

      bitboard.diff(initBitboard)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
    }

    assertEquals(initBitboard, bitboard)
  }

  @Test
  @DisplayName("Verify search state initialization, bitboard properties, and move values")
  fun testSearchStateInitialization() {
    // ==========================================
    // 1. Initialize Active Bitboard
    // ==========================================
    val bitboard =
        Bitboard(
            whiteGIPF = 67108881UL,
            whiteDVONNLayer = ulongArrayOf(8192UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(16384UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 4196352UL,
            whiteYINSH = 2UL,
            whiteZERTZ = 0UL,
            whitePotentials = 4220930UL,
            blackGIPF = 838860800UL,
            blackDVONNLayer = ulongArrayOf(256UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(274877906944UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 8590458880UL,
            blackYINSH = 32864UL,
            blackZERTZ = 0UL,
            blackPotentials = 283468398944UL,
        )

    // ==========================================
    // 2. Initialize Pre-move Bitboard State
    // ==========================================
    val initBitboard =
        Bitboard(
            whiteGIPF = 67108881UL,
            whiteDVONNLayer = ulongArrayOf(256UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(16384UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 4196352UL,
            whiteYINSH = 4098UL,
            whiteZERTZ = 0UL,
            whitePotentials = 4217090UL,
            blackGIPF = 838860800UL,
            blackDVONNLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(274877906944UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 8590458880UL,
            blackYINSH = 32864UL,
            blackZERTZ = 0UL,
            blackPotentials = 283468398688UL,
        )

    val preMoveBitboardState = initBitboard

    // ==========================================
    // 3. Initialize Post-move / Recursion State
    // ==========================================
    val postMoveBitboardState =
        Bitboard(
            whiteGIPF = 67108881UL,
            whiteDVONNLayer = ulongArrayOf(8192UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(16384UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 4196352UL,
            whiteYINSH = 4098UL,
            whiteZERTZ = 0UL,
            whitePotentials = 4225026UL,
            blackGIPF = 838860800UL,
            blackDVONNLayer = ulongArrayOf(256UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(274877906944UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 8590458880UL,
            blackYINSH = 32864UL,
            blackZERTZ = 0UL,
            blackPotentials = 283468398944UL,
        )

    val beforeRecursionBitboardState = postMoveBitboardState

    // ==========================================
    // 4. Initialize Players
    // ==========================================
    val currentPlayer =
        Player(
            name = PlayerName.BLACK,
            model = Model.MINIMAX,
            strength = Strength.EASY,
            piecesInReserve =
                mutableListOf(771751936u, 771751936u, 771751936u, 973078528u, 905969664u, 905969664u),
        )

    val opponentPlayer =
        Player(
            name = PlayerName.WHITE,
            model = Model.MINIMAX,
            strength = Strength.EASY,
            piecesInReserve =
                mutableListOf(
                    234881024u,
                    234881024u,
                    234881024u,
                    369098752u,
                    436207616u,
                    167772160u,
                    301989888u,
                    369098752u,
                    436207616u,
                    301989888u,
                ),
            capturedPieces = mutableListOf(704643072u, 973078528u),
        )

    // ==========================================
    // 5. Search Context & Parameters
    // ==========================================
    val depth = 2
    val isPVNode = true
    val maxDepth = 3
    val rng = kotlin.random.Random(0)
    val turnPhase: Any? = null

    // ==========================================
    // 6. Available Moves List
    // ==========================================
    val availableMoves =
        listOf(
            PackedMove.Single(914423681u),
            PackedMove.Single(906002307u),
            PackedMove.Single(913358735u),
            PackedMove.Single(906526600u),
            PackedMove.Single(909148068u),
            PackedMove.Single(906510212u),
            PackedMove.Single(915472259u),
            PackedMove.Single(907575189u),
            PackedMove.Single(915996552u),
            PackedMove.Single(907050894u),
            PackedMove.Single(917045141u),
            PackedMove.Single(908083094u),
            PackedMove.Single(913915791u),
            PackedMove.Single(911835029u),
            PackedMove.Single(911310747u),
            PackedMove.Single(910262180u),
            PackedMove.Single(914947970u),
            PackedMove.Single(916537254u),
            PackedMove.Single(912310148u),
            PackedMove.Single(908099483u),
            PackedMove.Single(908623776u),
            PackedMove.Single(917061543u),
            PackedMove.Single(914964380u),
            PackedMove.Single(916520846u),
            PackedMove.Single(907034505u),
            PackedMove.Single(905985920u),
            PackedMove.Single(909131681u),
            PackedMove.Single(909672359u),
            PackedMove.Single(916012965u),
            PackedMove.Single(912359335u),
            PackedMove.Single(908607388u),
            PackedMove.Single(911785859u),
            PackedMove.Single(910786464u),
            PackedMove.Single(909655973u),
            PackedMove.Single(914440086u),
            PackedMove.Single(910212992u),
            PackedMove.Single(907558799u),
            PackedMove.Single(913407909u),
            PackedMove.Single(911261570u),
            PackedMove.Single(910737281u),
            PackedMove.Single(913899392u),
            PackedMove.Single(915488673u),
            PackedMove.Single(912834441u),
            PackedMove.Single(912883622u),
            PackedMove.Single(983105416u),
            PackedMove.Single(976240545u),
            PackedMove.Single(777568131u),
            PackedMove.Single(978370434u),
            PackedMove.Single(977846145u),
            PackedMove.Single(782303118u),
            PackedMove.Single(855361417u),
            PackedMove.Single(980516773u),
            PackedMove.Single(977321856u),
            PackedMove.Single(983646118u),
            PackedMove.Single(773341071u),
            PackedMove.Single(775454631u),
            PackedMove.Single(778665894u),
            PackedMove.Single(981532545u),
            PackedMove.Single(981008256u),
            PackedMove.Single(977371044u),
            PackedMove.Single(982073244u),
            PackedMove.Single(855360144u),
            PackedMove.Single(979468199u),
            PackedMove.Single(982581123u),
            PackedMove.Single(778141607u),
            PackedMove.Single(974143369u),
            PackedMove.Single(772833166u),
            PackedMove.Single(855360263u),
            PackedMove.Single(775438245u),
            PackedMove.Single(774406048u),
            PackedMove.Single(975716252u),
            PackedMove.Single(979419012u),
            PackedMove.Single(855361424u),
            PackedMove.Single(773357461u),
            PackedMove.Single(774930340u),
            PackedMove.Single(781254531u),
            PackedMove.Single(779141007u),
            PackedMove.Single(855361425u),
            PackedMove.Single(983121829u),
            PackedMove.Single(775995264u),
            PackedMove.Single(855361426u),
            PackedMove.Single(780730242u),
            PackedMove.Single(984154005u),
            PackedMove.Single(781270945u),
            PackedMove.Single(975208347u),
            PackedMove.Single(978894723u),
            PackedMove.Single(774913953u),
            PackedMove.Single(780746652u),
            PackedMove.Single(771784579u),
            PackedMove.Single(782319526u),
            PackedMove.Single(782843815u),
            PackedMove.Single(771768192u),
            PackedMove.Single(982056834u),
            PackedMove.Single(772816777u),
            PackedMove.Single(974684053u),
            PackedMove.Single(780205953u),
            PackedMove.Single(777617301u),
            PackedMove.Single(975732640u),
            PackedMove.Single(980467599u),
            PackedMove.Single(979992486u),
        ) 
    // ==========================================
    // 7. Transition Table Context
    // ==========================================
    val index = 8

    // ==========================================
    // 8. PackedMove & Column Information
    // ==========================================
    val packedMove = availableMoves[index] as PackedMove.Single
    val moveValue = 915996552u

    val vacantBitFound = 8192

    val move =
        BestPackedMove(
            move = PackedMove.Single(915996552u),
            score = 96,
        )

    // ==========================================
    // ASSERTIONS
    // ==========================================

    // Assert Bitboards
    assertEquals(bitboard.whiteGIPF, 67108881UL)
    assertEquals(bitboard.blackPotentials, 283468398944UL)
    assertEquals(preMoveBitboardState.whiteYINSH, 4098UL)
    assertEquals(beforeRecursionBitboardState.whitePotentials, 4225026UL)

    // Assert Players
    assertEquals(PlayerName.BLACK, currentPlayer.name)
    assertEquals(6, currentPlayer.piecesInReserve.size)
    assertTrue(currentPlayer.capturedPieces.isEmpty())

    assertEquals(PlayerName.WHITE, opponentPlayer.name)
    assertEquals(10, opponentPlayer.piecesInReserve.size)
    assertEquals(2, opponentPlayer.capturedPieces.size)


      val bitboardForTesting = bitboard.deepCopy()

    alphaBetaPackedMove(
      maxDepth = maxDepth,
      depth = depth.minus(1),
      bitboard = bitboardForTesting,
      currentPlayer = opponentPlayer,
      opponentPlayer = currentPlayer,
      //					              gameTree = gameTree,
      alphaBetaScore = AlphaBetaScoreBitPacked(),
      rng = rng,
      isPVNode = isPVNode && packedMove == availableMoves[0],
    )

      bitboardForTesting.diff(bitboard)
  }
}
