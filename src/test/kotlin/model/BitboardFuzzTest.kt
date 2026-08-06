@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.example.ai.mcts.PackedMove
import org.example.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue

class BitboardFuzzTest {

  /**
   * Drops active bits until the total number of 1-bits is less than or equal to [maxBits].
   * Extremely fast, zero allocations, perfect for fuzzing.
   */
  private fun limitActiveBits(mask: ULong, maxBits: Int): ULong {
    var current = mask
    while (current.countOneBits() > maxBits) {
      // Clears the lowest set bit
      current = current and (current - 1UL)
    }
    return current
  }

  /**
   * Generates layered stacks where:
   * 1. Layer N only has a bit set if Layer N-1 has that same bit set.
   * 2. White and Black never occupy the exact same layer at the same index.
   * 3. The total pieces across ALL layers strictly respect the max limits.
   */
  private fun generateValidStacks(
      data: FuzzedDataProvider,
      baseOccupancy: ULong,
      maxWhite: Int,
      maxBlack: Int,
      maxLayers: Int = 8,
  ): Pair<ULongArray, ULongArray> {
    val whiteLayer = ULongArray(maxLayers)
    val blackLayer = ULongArray(maxLayers)

    var whiteCount = 0
    var blackCount = 0
    var currentLayerOccupancy = baseOccupancy

    for (i in 0 until maxLayers) {
      if (currentLayerOccupancy == 0UL || (whiteCount >= maxWhite && blackCount >= maxBlack)) break

      // Fuzzer selects potential spots for White
      var whiteMask = currentLayerOccupancy and data.consumeLong().toULong()
      // Trim to ensure we don't exceed the remaining White budget
      whiteMask = limitActiveBits(whiteMask, maxWhite - whiteCount)

      // Black gets the remaining spots, but also must be trimmed to the Black budget
      var blackMask = currentLayerOccupancy and whiteMask.inv()
      blackMask = limitActiveBits(blackMask, maxBlack - blackCount)

      // Update arrays and budgets
      whiteLayer[i] = whiteMask
      blackLayer[i] = blackMask

      whiteCount += whiteMask.countOneBits()
      blackCount += blackMask.countOneBits()

      // The effective physical occupancy of THIS layer.
      // (If we trimmed bits above, those spots are now empty and cannot support a higher layer).
      val actualOccupancy = whiteMask or blackMask

      // Decide which stacks continue to the next layer
      val continueStackMask = data.consumeLong().toULong()
      currentLayerOccupancy = actualOccupancy and continueStackMask
    }

    return Pair(whiteLayer, blackLayer)
  }

  /** Helper function to extract bitboard state generation. */
  private val VALID_BOARD_MASK = 0xFFFFFFFFFFUL // Bits 0 to 39

  // Define your strict game limits here
  private val MAX_GIPF = 3
  private val MAX_TAMSK = 3
  private val MAX_ZERTZ = 3
  private val MAX_YINSH = 3
  private val MAX_DVONN = 3
  private val MAX_PUNCT = 3

  private fun generateBitboard(data: FuzzedDataProvider): Bitboard {
    var remainingBoard = VALID_BOARD_MASK

    // Helper to claim unstacked spots within constraints
    fun claimConstrained(maxWhite: Int, maxBlack: Int): Pair<ULong, ULong> {
      // Fuzzer claims random spots from what is left on the board
      val rawClaimed = remainingBoard and data.consumeLong().toULong()

      // Fuzzer decides which are White, bounded by White's limit
      val whiteMask = limitActiveBits(rawClaimed and data.consumeLong().toULong(), maxWhite)

      // Black gets the rest, bounded by Black's limit
      val blackMask = limitActiveBits(rawClaimed and whiteMask.inv(), maxBlack)

      val totalClaimed = whiteMask or blackMask
      remainingBoard = remainingBoard and totalClaimed.inv() // Remove from available pool

      return Pair(whiteMask, blackMask)
    }

    // 1. Generate standard pieces
    val (whiteGipf, blackGipf) = claimConstrained(MAX_GIPF, MAX_GIPF)
    val (whiteTamsk, blackTamsk) = claimConstrained(MAX_TAMSK, MAX_TAMSK)
    val (whiteZertz, blackZertz) = claimConstrained(MAX_ZERTZ, MAX_ZERTZ)
    val (whiteYinsh, blackYinsh) = claimConstrained(MAX_YINSH, MAX_YINSH)

    // 2. Claim base spots for stacked pieces (We limit the base layer to the total max,
    // though the stack generator will enforce the strict limit across all layers).
    val (dvonnBaseWhite, dvonnBaseBlack) = claimConstrained(MAX_DVONN, MAX_DVONN)
    val dvonnBaseOccupancy = dvonnBaseWhite or dvonnBaseBlack

    val (punctBaseWhite, punctBaseBlack) = claimConstrained(MAX_PUNCT, MAX_PUNCT)
    val punctBaseOccupancy = punctBaseWhite or punctBaseBlack

    // 3. Generate Stacks
    val (whiteDVONN, blackDVONN) =
        generateValidStacks(data, dvonnBaseOccupancy, MAX_DVONN, MAX_DVONN)
    val (whitePUNCT, blackPUNCT) =
        generateValidStacks(data, punctBaseOccupancy, MAX_PUNCT, MAX_PUNCT)

    // 4. Generate Potentials
    // Potentials must intersect with actual pieces the player owns.
    val globalWhiteOccupancy =
        whiteTamsk or whiteZertz or whiteYinsh or whiteDVONN[0] or whitePUNCT[0] // whiteGipf or
    val globalBlackOccupancy =
        blackTamsk or blackZertz or blackYinsh or blackDVONN[0] or blackPUNCT[0] // blackGipf or

    return Bitboard(
        whiteGIPF = whiteGipf,
        blackGIPF = blackGipf,
        whiteTAMSK = whiteTamsk,
        blackTAMSK = blackTamsk,
        whiteZERTZ = whiteZertz,
        blackZERTZ = blackZertz,
        whiteYINSH = whiteYinsh,
        blackYINSH = blackYinsh,
        whiteDVONNLayer = whiteDVONN,
        blackDVONNLayer = blackDVONN,
        whitePUNCTLayer = whitePUNCT,
        blackPUNCTLayer = blackPUNCT,

        // No need to limit count of potentials, as they are naturally limited by the piece counts
        // above
        whitePotentials = globalWhiteOccupancy and data.consumeLong().toULong(),
        blackPotentials = globalBlackOccupancy and data.consumeLong().toULong(),
    )
  }

  @FuzzTest
  fun fuzzAddAndUndoPiece(data: FuzzedDataProvider) {
    val bitboard = generateBitboard(data)

    val initBitboard = bitboard.deepCopy()

    val currentPlayer =
        Player(name = if (data.consumeBoolean()) PlayerName.WHITE else PlayerName.BLACK)

    val listSize = data.consumeInt(1, 15)

    val piecesInReserve = data.consumeInts(listSize).map {
      Piece(
        "",
        potential = data.consumeBoolean(),
        colorName = currentPlayer.name,
        type = PieceType.entries[data.consumeInt(1, 6)],
      ).pack()
    }

    currentPlayer.piecesInReserve.addAll(piecesInReserve)

    val movesBuffer = mutableListOf<PackedMove>()
    bitboard.generateMoves(currentPlayer = currentPlayer, TurnPhase.PlayerInputWindow, movesBuffer)

    movesBuffer.removeAll { (it as PackedMove.Single).value.extractMoveType() != MoveType.AddPiece }

    assumeTrue(movesBuffer.isNotEmpty())

    val move = (movesBuffer.first() as PackedMove.Single).value

    try {
      // Attempts to safely add a piece while respecting the opening spots and single bit mask
      // rules.
      val vacantBit = bitboard.addPieceToBitboard(move)

      // Fuzz the undo process with a random assumption of occupancy to test robust state checking.
      val wasOccupied = vacantBit != ULong.MAX_VALUE
      bitboard.undoAddPieceToBitboard(move, vacantBit, wasOccupied)

      assertEquals(initBitboard, bitboard) {
        "bitboard is not equal to initBitboard.\n${initBitboard}\n${bitboard}"
      }
    } catch (e: IllegalArgumentException) {
      // Expected when randomly generating distinct source and target bits fails.
      throw e
    } catch (e: IllegalStateException) {
      // Expected when layer invariants (e.g., overlapping DVONN layers) are broken by fuzzy data.
      throw e
    }
  }

  @FuzzTest
  fun fuzzUseAndUndoPiecePotential(data: FuzzedDataProvider) {
    val bitboard = generateBitboard(data)
    val initBitboard = bitboard.deepCopy()

    val currentPlayer =
        Player(name = if (data.consumeBoolean()) PlayerName.WHITE else PlayerName.BLACK)
    val nextPlayer =
        Player(
            name =
                if (currentPlayer.name == PlayerName.WHITE) PlayerName.BLACK else PlayerName.WHITE
        )

    val movesBuffer = mutableListOf<PackedMove>()
    bitboard.generateMoves(currentPlayer = currentPlayer, TurnPhase.PlayerInputWindow, movesBuffer)

    movesBuffer.removeAll { (it as PackedMove.Single).value.extractMoveType() != MoveType.AddPiece }

    assumeTrue(movesBuffer.isNotEmpty())

    val move = (movesBuffer.first() as PackedMove.Single).value

    try {
      bitboard.usePiecePotential(move, currentPlayer, nextPlayer)
      bitboard.undoUsePiecePotential(move)

      assertEquals(initBitboard, bitboard) {
        "bitboard is not equal to initBitboard.\n${initBitboard}\n${bitboard}"
      }
    } catch (e: IllegalArgumentException) {
      // Expected when randomly generating distinct source and target bits fails.
      throw e
    } catch (e: IllegalStateException) {
      // Expected when layer invariants (e.g., overlapping DVONN layers) are broken by fuzzy data.
      throw e
    }
  }

  @FuzzTest
  fun fuzzUseAndUndoTamskPotential(data: FuzzedDataProvider) {
    val bitboard = generateBitboard(data)
    val initBitboard = bitboard.deepCopy()

    val currentPlayer =
        Player(name = if (data.consumeBoolean()) PlayerName.WHITE else PlayerName.BLACK)

    val movesBuffer = mutableListOf<PackedMove>()
    bitboard.getTamskMoves(currentPlayer, movesBuffer)

    assumeTrue(movesBuffer.isNotEmpty())

    val move = (movesBuffer.first() as PackedMove.Single).value

    try {
      val vacantBit = bitboard.useTamskPotential(move)
      val wasOccupied = data.consumeBoolean()

      bitboard.undoTamskPotential(move, vacantBit, wasOccupied)

      assertEquals(initBitboard, bitboard) {
        "bitboard is not equal to initBitboard.\n${initBitboard}\n${bitboard}"
      }
    } catch (e: IllegalArgumentException) {
      // Expected
      throw e
    } catch (e: IllegalStateException) {
      // Expected when testing validates that the fuzzed move isn't hitting the correct center spot
      // mask.
      throw e
    }
  }

  @FuzzTest
  fun fuzzRetrieveAndUndoCapturePieces(data: FuzzedDataProvider) {
    val bitboard = generateBitboard(data)

    val initBitboard = bitboard.deepCopy()
    val player = Player(name = if (data.consumeBoolean()) PlayerName.WHITE else PlayerName.BLACK)

    val movesBuffer = mutableListOf<PackedMove>()
    bitboard.identifyPiecesToRemove(player, movesBuffer)

    assumeTrue(movesBuffer.isNotEmpty())

    val move = (movesBuffer.first() as PackedMove.Multiple).values.distinct()

    val removedPieces = mutableListOf<UInt>()

    try {
      // Generates the mutable list buffer output while pulling items off the bitboard
      bitboard.removeSelectedPieces(player, move, removedPieces)

      // Reverts mutations relying on the tracked operations in movesBuffer
      bitboard.undoRetrieveAndCapturePieces(removedPieces)
      assertEquals(initBitboard, bitboard) {
        "bitboard is not equal to initBitboard.\n${initBitboard}\n${bitboard}"
      }
    } catch (e: IllegalArgumentException) {
      throw e
    } catch (e: IllegalStateException) {
      throw e
    }
  }
}
