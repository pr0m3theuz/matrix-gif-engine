@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import kotlin.math.abs
import kotlin.math.exp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

// a line is full if it is equal to all bits being 1 else it is has at least 1 0 bit

val boardCenterSpotMask = 0b0000000000000000000001000000000000000000.toULong()
// val centralAreaBoardMask = 0b0000000001000011000011100001100000000000.toULong()

val bitDistanceWeights: Map<Int, Double> =
    0.rangeTo(39).associateWith { k ->
      when (abs(k - 18)) {
        0 -> exp(-0 / 1.5)
        1,
        6,
        7 -> exp(-1 / 1.5)
        2,
        5,
        8,
        11,
        12,
        13 -> exp(-2 / 1.5)
        3,
        4,
        9,
        10,
        14,
        15,
        16,
        17,
        18 -> exp(-3 / 1.5)
        19,
        20,
        21 -> exp(-4 / 1.5)
        else -> error("Unknown node $k")
      }
    }

val verticalLineIndexArray =
    listOf(
        listOf(0, 1, 2, 3), // 0b0000000000000000000000000000000000001111
        listOf(4, 5, 6, 7, 8), // 0b0000000000000000000000000000000111110000
        listOf(9, 10, 11, 12, 13, 14), // 0b0000000000000000000000000111111000000000
        listOf(15, 16, 17, 18, 19, 20, 21), // 0b0000000000000000001111111000000000000000
        listOf(22, 23, 24, 25, 26, 27), // 0b0000000000001111110000000000000000000000
        listOf(28, 29, 30, 31, 32), // 0b0000000111110000000000000000000000000000
        listOf(33, 34, 35, 36), // 0b0001111000000000000000000000000000000000
        listOf(37, 38, 39), // 0b1110000000000000000000000000000000000000
    )

val upwardRightLineIndexArray =
    listOf(
        listOf(0, 5, 11, 18, 25, 31, 36), // 0b0001000010000010000001000000100000100001
        listOf(1, 6, 12, 19, 26, 32), // 0b0000000100000100000010000001000001000010
        listOf(2, 7, 13, 20, 27), // 0b0000000000001000000100000010000010000100
        listOf(3, 8, 14, 21), // 0b0000000000000000001000000100000100001000
        listOf(4, 10, 17, 24, 30, 35, 39), // 0b1000100001000001000000100000010000010000
        listOf(9, 16, 23, 29, 34, 38), // 0b0100010000100000100000010000001000000000
        listOf(15, 22, 28, 33, 37), // 0b0010001000010000010000001000000000000000
    )

val downwardRightLineIndexArray =
    listOf(
        listOf(0, 4, 9, 15), // 0b0000000000000000000000001000001000010001
        listOf(1, 5, 10, 16, 22), // 0b0000000000000000010000010000010000100010
        listOf(2, 6, 11, 17, 23, 28), // 0b0000000000010000100000100000100001000100
        listOf(3, 7, 12, 18, 24, 29, 33), // 0b0000001000100001000001000001000010001000
        listOf(8, 13, 19, 25, 30, 34, 37), // 0b0010010001000010000010000010000100000000
        listOf(14, 20, 26, 31, 35, 38), // 0b0100100010000100000100000100000000000000
        listOf(21, 27, 32, 36, 39), // 0b1001000100001000001000000000000000000000
    )

val lineMasks =
    listOf(
        // verticalLineMasks
        0b0000000000000000000000000000000000001111
            .toULong(), // listOf(0, 1, 2, 3),                    //
        0b0000000000000000000000000000000111110000
            .toULong(), // listOf(4, 5, 6, 7, 8),                 //
        0b0000000000000000000000000111111000000000
            .toULong(), // listOf(9, 10, 11, 12, 13, 14),         //
        0b0000000000000000001111111000000000000000
            .toULong(), // listOf(15, 16, 17, 18, 19, 20, 21),    //
        0b0000000000001111110000000000000000000000
            .toULong(), // listOf(22, 23, 24, 25, 26, 27),        //
        0b0000000111110000000000000000000000000000
            .toULong(), // listOf(28, 29, 30, 31, 32),            //
        0b0001111000000000000000000000000000000000
            .toULong(), // listOf(33, 34, 35, 36),                //
        0b1110000000000000000000000000000000000000
            .toULong(), // listOf(37, 38, 39),                    //
        /// val upwardRightLineMasks =
        //	listOf(
        0b0001000010000010000001000000100000100001
            .toULong(), // listOf(0, 5, 11, 18, 25, 31, 36),      //
        0b0000000100000100000010000001000001000010
            .toULong(), // listOf(1, 6, 12, 19, 26, 32),          //
        0b0000000000001000000100000010000010000100
            .toULong(), // listOf(2, 7, 13, 20, 27),              //
        0b0000000000000000001000000100000100001000
            .toULong(), // listOf(3, 8, 14, 21),                  //
        0b1000100001000001000000100000010000010000
            .toULong(), // listOf(4, 10, 17, 24, 30, 35, 39),     //
        0b0100010000100000100000010000001000000000
            .toULong(), // listOf(9, 16, 23, 29, 34, 38),         //
        0b0010001000010000010000001000000000000000
            .toULong(), // listOf(15, 22, 28, 33, 37),            //
        // val downwardRightLineMasks =
        //	listOf(
        0b0000000000000000000000001000001000010001
            .toULong(), // listOf(0, 4, 9, 15),                   //
        0b0000000000000000010000010000010000100010
            .toULong(), // listOf(1, 5, 10, 16, 22),              //
        0b0000000000010000100000100000100001000100
            .toULong(), // listOf(2, 6, 11, 17, 23, 28),          //
        0b0000001000100001000001000001000010001000
            .toULong(), // listOf(3, 7, 12, 18, 24, 29, 33),      //
        0b0010010001000010000010000010000100000000
            .toULong(), // listOf(8, 13, 19, 25, 30, 34, 37),     //
        0b0100100010000100000100000100000000000000
            .toULong(), // listOf(14, 20, 26, 31, 35, 38),        //
        0b1001000100001000001000000000000000000000
            .toULong(), // listOf(21, 27, 32, 36, 39),            //
    )

val openningSpotsLineMask = 0b1111001100011000011000001100001100011111.toULong()

// val openningLineIndexArray =
//    listOf(
//        0,
//        1,
//        2,
//        3,
//        4,
//        8,
//        9,
//        14,
//        15,
//        21,
//        22,
//        27,
//        28,
//        32,
//        33,
//        36,
//        37,
//        38,
//        39,
//    )

@Serializable
data class ColumnInfo(
    val columnMask: ULong,
    //	val destinationMask: ULong,
    val positions: List<ULong>,
    val submasks: List<ULong>,
    val shiftPairs: List<Pair<ULong, ULong>>, // (fromMask, toMask)
    val lineOrientation: LineOrientation,
    val pushDirections: Pair<PushDirection, PushDirection>,
) {
  // Helper to format as binary with a prefix.
  private fun ULong.toBin() = "0b" + this.toString(radix = 2)

  override fun toString(): String {
    // Format the list of pairs customly
    val formattedPairs =
        shiftPairs.joinToString(prefix = "[", postfix = "]") { (from, to) ->
          "(${from.toBin()} -> ${to.toBin()})"
        }

    // destinationMask = ${destinationMask.toBin()},
    return """
            ColumnInfo(
                columnMask = ${columnMask.toBin()},
                shiftPairs = $formattedPairs
            )
        """
        .trimIndent()
  }
}

val columnInfos: List<ColumnInfo> =
    verticalLineIndexArray.map { arr ->
      ColumnInfo(
          columnMask = arr.fold(0UL) { acc, i -> acc or (1UL shl i) },
          //		destinationMask = 1UL shl arr.last(),
          positions =
              arr.indices.map { k ->
                1UL shl arr[k]
              },
          submasks =
              arr.indices
                  .map { k ->
                    1UL shl arr[k]
                  }
                  .windowed(4)
                  .map { sublist ->
                    sublist.fold(0UL) { acc, lng ->
                      acc or lng
                    }
                  },
          shiftPairs =
              (0 until arr.size - 1).map { k ->
                (1UL shl arr[k]) to (1UL shl arr[k + 1])
              },
          lineOrientation = LineOrientation.VERTICAL,
          pushDirections = Pair(PushDirection.UP, PushDirection.DOWN),
      )
    } +
        upwardRightLineIndexArray.map { arr ->
          ColumnInfo(
              columnMask = arr.fold(0UL) { acc, i -> acc or (1UL shl i) },
              //		destinationMask = 1UL shl arr.last(),

              positions =
                  arr.indices.map { k ->
                    1UL shl arr[k]
                  },
              submasks =
                  arr.indices
                      .map { k ->
                        1UL shl arr[k]
                      }
                      .windowed(4)
                      .map { sublist ->
                        sublist.fold(0UL) { acc, lng ->
                          acc or lng
                        }
                      },
              shiftPairs =
                  (0 until arr.size - 1).map { k ->
                    (1UL shl arr[k]) to (1UL shl arr[k + 1])
                  },
              lineOrientation = LineOrientation.UPWARD_RIGHT,
              pushDirections = Pair(PushDirection.UPPER_RIGHT, PushDirection.LOWER_LEFT),
          )
        } +
        downwardRightLineIndexArray.map { arr ->
          ColumnInfo(
              columnMask = arr.fold(0UL) { acc, i -> acc or (1UL shl i) },
              //		destinationMask = 1UL shl arr.last(),
              positions =
                  arr.indices.map { k ->
                    1UL shl arr[k]
                  },
              submasks =
                  arr.indices
                      .map { k ->
                        1UL shl arr[k]
                      }
                      .windowed(4)
                      .map { sublist ->
                        sublist.fold(0UL) { acc, lng ->
                          acc or lng
                        }
                      },
              shiftPairs =
                  (0 until arr.size - 1).map { k ->
                    (1UL shl arr[k]) to (1UL shl arr[k + 1])
                  },
              lineOrientation = LineOrientation.DOWNWARD_RIGHT,
              pushDirections = Pair(PushDirection.LOWER_RIGHT, PushDirection.UPPER_LEFT),
          )
        }

val neighbouringBitsBitmasks: MutableMap<ULong, ULong> = run {
  val neighbouringBitsBitmasks: MutableMap<ULong, ULong> = mutableMapOf()

  for (column in columnInfos) {
    for ((a, b) in column.shiftPairs) {
      neighbouringBitsBitmasks[a] = neighbouringBitsBitmasks.getOrDefault(a, 0UL) or b
      neighbouringBitsBitmasks[b] = neighbouringBitsBitmasks.getOrDefault(b, 0UL) or a
    }
  }

  return@run neighbouringBitsBitmasks
}

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class Bitboard(
    var whiteGIPF: ULong = 0UL,
    //
    /**
     * even indices are player pieces, odd layer are opponent pieces — 0 - white, 1 - black, 2 -
     * white, 3 - black, 4 - white, 5 = black
     */
    var whiteDVONNLayer: ULongArray = ULongArray(8),
    var whitePUNCTLayer: ULongArray = ULongArray(8),
    var whiteTAMSK: ULong = 0UL,
    var whiteYINSH: ULong = 0UL,
    var whiteZERTZ: ULong = 0UL,
    var whitePotentials: ULong = 0UL,
    var blackGIPF: ULong = 0UL,
    /**
     * even indices are player pieces, odd layer are opponent pieces — 0 - black, 1 - white, 2 -
     * black, 3 - white, 4 - black, 5 = white
     */
    var blackDVONNLayer: ULongArray = ULongArray(8),
    var blackPUNCTLayer: ULongArray = ULongArray(8),
    var blackTAMSK: ULong = 0UL,
    var blackYINSH: ULong = 0UL,
    var blackZERTZ: ULong = 0UL,
    var blackPotentials: ULong = 0UL,
) {
  fun deepCopy(): Bitboard {
    // TODO Optimize
    //    val string = Json.encodeToString(serializer(), this)
    //    return Json.decodeFromString(serializer(), string)
    return Bitboard(
        whiteGIPF = this.whiteGIPF,
        whiteDVONNLayer = this.whiteDVONNLayer.copyOf(),
        whitePUNCTLayer = this.whitePUNCTLayer.copyOf(),
        whiteTAMSK = this.whiteTAMSK,
        whiteYINSH = this.whiteYINSH,
        whiteZERTZ = this.whiteZERTZ,
        whitePotentials = this.whitePotentials,
        blackGIPF = this.blackGIPF,
        blackDVONNLayer = this.blackDVONNLayer.copyOf(),
        blackPUNCTLayer = this.blackPUNCTLayer.copyOf(),
        blackTAMSK = this.blackTAMSK,
        blackYINSH = this.blackYINSH,
        blackZERTZ = this.blackZERTZ,
        blackPotentials = this.blackPotentials,
    )
  }

  fun diff(oldBitboard: Bitboard): Bitboard {
    // --- 0. CONFIGURABLE DEBUGGING ---
    if (logger.isDebugEnabled()) {
      logger.info { "" + ("--- BITBOARD DIFF CALLED ---") }
      logger.info { "" + ("Original Bitboard State: ${Json.encodeToString(oldBitboard) }}") }
      logger.info { "" + ("Current Bitboard State: ${Json.encodeToString(this) }}") }
    }

    // --- 1. PRE-CONDITION WARNING ---
    require(this !== oldBitboard) {
      "LOGIC WARNING: You are diffing a bitboard against the exact same instance in memory. The result will be entirely empty."
    }

    // --- 2. LOCAL HELPER FOR ARRAY DIFFING ---
    // This elegantly replaces all four of your manual array initialization loops
    fun diffLayers(current: ULongArray, old: ULongArray, boardName: String): ULongArray {
      require(current.size == old.size) { "STATE ERROR: Layer sizes do not match for $boardName." }
      return ULongArray(current.size) { i ->
        current[i] xor old[i] // XOR captures exactly which bits flipped (added or removed)
      }
    }

    // --- 3. EXECUTE DIFF ---
    val diffBoard =
        Bitboard(
            whiteGIPF = this.whiteGIPF xor oldBitboard.whiteGIPF,
            whiteDVONNLayer =
                diffLayers(this.whiteDVONNLayer, oldBitboard.whiteDVONNLayer, "White DVONN"),
            whitePUNCTLayer =
                diffLayers(this.whitePUNCTLayer, oldBitboard.whitePUNCTLayer, "White PUNCT"),
            whiteTAMSK = this.whiteTAMSK xor oldBitboard.whiteTAMSK,
            whiteYINSH = this.whiteYINSH xor oldBitboard.whiteYINSH,
            whiteZERTZ = this.whiteZERTZ xor oldBitboard.whiteZERTZ,
            whitePotentials = this.whitePotentials xor oldBitboard.whitePotentials, // FIXED TYPO
            blackGIPF = this.blackGIPF xor oldBitboard.blackGIPF,
            blackDVONNLayer =
                diffLayers(this.blackDVONNLayer, oldBitboard.blackDVONNLayer, "Black DVONN"),
            blackPUNCTLayer =
                diffLayers(this.blackPUNCTLayer, oldBitboard.blackPUNCTLayer, "Black PUNCT"),
            blackTAMSK = this.blackTAMSK xor oldBitboard.blackTAMSK,
            blackYINSH = this.blackYINSH xor oldBitboard.blackYINSH,
            blackZERTZ = this.blackZERTZ xor oldBitboard.blackZERTZ,
            blackPotentials = this.blackPotentials xor oldBitboard.blackPotentials, // FIXED TYPO
        )

    if (logger.isDebugEnabled()) {
      // Optional: Count total changes to easily see if anything happened
      val totalChanges =
          diffBoard.whiteGIPF.countOneBits() +
              diffBoard.whiteTAMSK.countOneBits() +
              diffBoard.whiteYINSH.countOneBits() +
              diffBoard.whiteZERTZ.countOneBits() +
              diffBoard.whitePotentials.countOneBits() +
              diffBoard.whiteDVONNLayer.sumOf { it.countOneBits() } +
              diffBoard.whitePUNCTLayer.sumOf { it.countOneBits() } +
              diffBoard.blackGIPF.countOneBits() +
              diffBoard.blackTAMSK.countOneBits() +
              diffBoard.blackYINSH.countOneBits() +
              diffBoard.blackZERTZ.countOneBits() +
              diffBoard.blackPotentials.countOneBits() +
              diffBoard.blackDVONNLayer.sumOf { it.countOneBits() } +
              diffBoard.blackPUNCTLayer.sumOf { it.countOneBits() }

      logger.info { "" + ("Total changes: $totalChanges") }
      logger.info { "" + ("Diff complete. Total bits flipped across calculated layers.") }
      logger.info { "" + ("Diff Bitboard State: ${Json.encodeToString(oldBitboard) }}") }
      logger.info { "" + ("--- BITBOARD DIFF COMPLETED ---") }
    }

    check(this.globalOccupancy == oldBitboard.globalOccupancy) {
      buildString {
        appendLine(
            "CRITICAL STATE VALIDATION FAILED: The current bitboard does not perfectly match the initial bitboard."
        )
        appendLine("--- STATE DESYNC REPORT ---")

        // Local helper to cleanly format the mismatched bitmasks
        fun logDiff(boardName: String, diffBits: ULong) {
          if (diffBits != 0UL) {
            val count = diffBits.countOneBits()
            appendLine(
                "  -> [$boardName] Desync: $count bit(s) mismatched. Diff Mask: 0b${diffBits.toString(2).padStart(40, '0')}"
            )
          }
        }

        // White Boards
        logDiff("White GIPF", diffBoard.whiteGIPF)
        logDiff("White TAMSK", diffBoard.whiteTAMSK)
        logDiff("White YINSH", diffBoard.whiteYINSH)
        logDiff("White ZERTZ", diffBoard.whiteZERTZ)
        logDiff("White Potentials", diffBoard.whitePotentials)
        diffBoard.whiteDVONNLayer.forEachIndexed { i, layer ->
          logDiff("White DVONN Layer $i", layer)
        }
        diffBoard.whitePUNCTLayer.forEachIndexed { i, layer ->
          logDiff("White PUNCT Layer $i", layer)
        }

        // Black Boards
        logDiff("Black GIPF", diffBoard.blackGIPF)
        logDiff("Black TAMSK", diffBoard.blackTAMSK)
        logDiff("Black YINSH", diffBoard.blackYINSH)
        logDiff("Black ZERTZ", diffBoard.blackZERTZ)
        logDiff("Black Potentials", diffBoard.blackPotentials)
        diffBoard.blackDVONNLayer.forEachIndexed { i, layer ->
          logDiff("Black DVONN Layer $i", layer)
        }
        diffBoard.blackPUNCTLayer.forEachIndexed { i, layer ->
          logDiff("Black PUNCT Layer $i", layer)
        }

        appendLine("---------------------------")
        appendLine(
            "Hint: A '1' in the Diff Mask indicates a piece that exists on one board but is missing from the other."
        )
      }
    }

    return diffBoard
  }

  // region From Gemini TODO
  val globalOccupancy: ULong
    get() {
      var occupied =
          whiteGIPF or
              whiteTAMSK or
              whiteYINSH or
              whiteZERTZ or
              whitePotentials or
              whiteNeutralized or
              blackGIPF or
              blackTAMSK or
              blackYINSH or
              blackZERTZ or
              blackPotentials or
              blackNeutralized

      // Fold in the layered arrays
      whiteDVONNLayer.forEach { occupied = occupied or it }
      blackDVONNLayer.forEach { occupied = occupied or it }
      whitePUNCTLayer.forEach { occupied = occupied or it }
      blackPUNCTLayer.forEach { occupied = occupied or it }

      return occupied
    }

  val whitePieces: ULong
    get() {
      val occupied =
          whiteGIPF or
              whiteTAMSK or
              whiteYINSH or
              whiteZERTZ or
              getActiveWhiteDvonnPieces() or
              getActiveWhitePunctPieces() // or whitePotentials or whiteNeutralized // this is
      // redundant

      // TODO This is wrong
      //      whiteDVONNLayer.forEach { occupied = occupied or it }
      //      whitePUNCTLayer.forEach { occupied = occupied or it }

      return occupied
    }

  val blackPieces: ULong
    get() {
      val occupied =
          blackGIPF or
              blackTAMSK or
              blackYINSH or
              blackZERTZ or
              getActiveBlackDvonnPieces() or
              getActiveBlackPunctPieces()
      // or blackPotentials or blackNeutralized

      // TODO This is wrong
      //      blackDVONNLayer.forEach { occupied = occupied or it }
      //      blackPUNCTLayer.forEach { occupied = occupied or it }

      return occupied
    }

  val whiteNeutralized: ULong
    get() {
      var neutralized = 0UL

      whiteDVONNLayer.slice(1..7).forEach { neutralized = neutralized or it }
      whitePUNCTLayer.slice(1..7).forEach { neutralized = neutralized or it }

      return neutralized
    }

  val blackNeutralized: ULong
    get() {
      var neutralized = 0UL

      blackDVONNLayer.slice(1..7).forEach { neutralized = neutralized or it }
      blackPUNCTLayer.slice(1..7).forEach { neutralized = neutralized or it }

      return neutralized
    }

  // Helper to shift a single ULong board
  // TODO how do you stop the shift board? board == ULong
  // TODO does not handle inserting new pieces
  private fun shiftBoard(board: ULong, fromMask: ULong, toMask: ULong): ULong {
    // fromMask == current node
    // if (currentNode is filled) clear it
    return if ((board and fromMask) != 0UL) {
      // Clear the old position using AND, set the new position using OR
      (board and fromMask.inv()) or toMask
    } else {
      // Nothing was here, leave the board alone
      board
    }
  }

  // 2. Apply a shift to literally every board we track
  //
  fun applyShiftToAll(fromMask: ULong, toMask: ULong) {
    whiteGIPF = shiftBoard(whiteGIPF, fromMask, toMask)
    whiteTAMSK = shiftBoard(whiteTAMSK, fromMask, toMask)
    whiteYINSH = shiftBoard(whiteYINSH, fromMask, toMask)
    whiteZERTZ = shiftBoard(whiteZERTZ, fromMask, toMask)
    whitePotentials = shiftBoard(whitePotentials, fromMask, toMask)
    blackGIPF = shiftBoard(blackGIPF, fromMask, toMask)
    blackTAMSK = shiftBoard(blackTAMSK, fromMask, toMask)
    blackYINSH = shiftBoard(blackYINSH, fromMask, toMask)
    blackZERTZ = shiftBoard(blackZERTZ, fromMask, toMask)
    blackPotentials = shiftBoard(blackPotentials, fromMask, toMask)

    // Apply to layers
    for (i in whiteDVONNLayer.indices) {
      whiteDVONNLayer[i] = shiftBoard(whiteDVONNLayer[i], fromMask, toMask)
      whitePUNCTLayer[i] = shiftBoard(whitePUNCTLayer[i], fromMask, toMask)
      blackDVONNLayer[i] = shiftBoard(blackDVONNLayer[i], fromMask, toMask)
      blackPUNCTLayer[i] = shiftBoard(blackPUNCTLayer[i], fromMask, toMask)
    }
  }

  // endregion
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Bitboard

    if (whitePieces != other.whitePieces) return false
    if (blackPieces != other.blackPieces) return false
    if (whiteGIPF != other.whiteGIPF) return false
    if (!whiteDVONNLayer.contentEquals(other.whiteDVONNLayer)) return false
    if (!whitePUNCTLayer.contentEquals(other.whitePUNCTLayer)) return false
    if (whiteTAMSK != other.whiteTAMSK) return false
    if (whiteYINSH != other.whiteYINSH) return false
    if (whiteZERTZ != other.whiteZERTZ) return false
    if (whitePotentials != other.whitePotentials) return false
    if (whiteNeutralized != other.whiteNeutralized) return false
    if (blackGIPF != other.blackGIPF) return false
    if (!blackDVONNLayer.contentEquals(other.blackDVONNLayer)) return false
    if (!blackPUNCTLayer.contentEquals(other.blackPUNCTLayer)) return false
    if (blackTAMSK != other.blackTAMSK) return false
    if (blackYINSH != other.blackYINSH) return false
    if (blackZERTZ != other.blackZERTZ) return false
    if (blackPotentials != other.blackPotentials) return false
    if (blackNeutralized != other.blackNeutralized) return false
    if (globalOccupancy != other.globalOccupancy) return false

    return true
  }

  override fun hashCode(): Int {
    var result = whitePieces.hashCode()
    result = 31 * result + whiteGIPF.hashCode()
    result = 31 * result + whiteDVONNLayer.contentHashCode()
    result = 31 * result + whitePUNCTLayer.contentHashCode()
    result = 31 * result + whiteTAMSK.hashCode()
    result = 31 * result + whiteYINSH.hashCode()
    result = 31 * result + whiteZERTZ.hashCode()
    result = 31 * result + whitePotentials.hashCode()
    result = 31 * result + whiteNeutralized.hashCode()
    result = 31 * result + blackPieces.hashCode()
    result = 31 * result + blackGIPF.hashCode()
    result = 31 * result + blackDVONNLayer.contentHashCode()
    result = 31 * result + blackPUNCTLayer.contentHashCode()
    result = 31 * result + blackTAMSK.hashCode()
    result = 31 * result + blackYINSH.hashCode()
    result = 31 * result + blackZERTZ.hashCode()
    result = 31 * result + blackPotentials.hashCode()
    result = 31 * result + blackNeutralized.hashCode()
    result = 31 * result + globalOccupancy.hashCode()
    return result
  }

  fun restorePreviousBoardState(previousBoardState: Bitboard) {
    whiteGIPF = previousBoardState.whiteGIPF
    whiteDVONNLayer = previousBoardState.whiteDVONNLayer
    whiteYINSH = previousBoardState.whiteYINSH
    whiteZERTZ = previousBoardState.whiteZERTZ
    whiteDVONNLayer = previousBoardState.whiteDVONNLayer
    whitePUNCTLayer = previousBoardState.whitePUNCTLayer
    whitePotentials = previousBoardState.whitePotentials

    blackGIPF = previousBoardState.blackGIPF
    blackTAMSK = previousBoardState.blackTAMSK
    blackYINSH = previousBoardState.blackYINSH
    blackZERTZ = previousBoardState.blackZERTZ
    blackDVONNLayer = previousBoardState.blackDVONNLayer
    blackPUNCTLayer = previousBoardState.blackPUNCTLayer
    blackPotentials = previousBoardState.blackPotentials
  }
}

val Bitboard.vacantLines
  get() = lineMasks.mapNotNull { lineMask ->
    val column =
        // lines with vacancies should be less than numeric value of the mask
        if ((globalOccupancy and lineMask) < lineMask) {
          columnInfos.first { it.columnMask == lineMask }
        } else {
          // this lineMask has no vacancies
          null
        }
    column
  }

// TODO
// region From Gemini

fun Bitboard.executePushUp(col: ColumnInfo): ULong {
  // --- 0. CONFIGURABLE DEBUGGING ---
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- EXECUTE PUSH UP CALLED ---") }
    logger.info { "" + ("Column: $col") }
  }

  require(col.shiftPairs.isNotEmpty()) {
    "CRITICAL ERROR: Column shiftPairs cannot be empty."
  }

  val occupiedSpots = globalOccupancy
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()
  var gapFound = false
  var vacantBitFound = 0UL

  // --- 1. DISCOVERY: Find out exactly which pieces are shifting ---
                               logger.debug { "" + ("Starting discovery phase for Push Up...") }

  for ((fromMask, toMask) in col.shiftPairs) {
    // Strict bit validation: Ensure masks aren't corrupted
    check(fromMask.countOneBits() == 1 && toMask.countOneBits() == 1) {
      "STATE ERROR: Shift masks must contain exactly one bit. fromMask: $fromMask, toMask: $toMask"
    }

    if ((occupiedSpots and fromMask) != 0UL) {
      movesToApply.add(fromMask to toMask)
                                   logger.debug { "" + ("  -> Piece found at $fromMask. Queuing shift to $toMask.") }
    } else {
      gapFound = true
      vacantBitFound = fromMask
                                   logger.debug { "" + ("  -> Gap discovered at $fromMask. Ending discovery phase.") }
      break
    }

    if ((occupiedSpots and toMask) == 0UL) {
      gapFound = true
      vacantBitFound = toMask

          logger.debug { "" + ("  -> Gap discovered at ${toMask.toString(2)}. Ending discovery phase.") }
      break
    }
  }

  // --- 2. VALIDATION: Check if pieces fall off the board ---
  check(gapFound) {
    "ILLEGAL MOVE: Cannot shift column; board line is full and pieces would fall off."
  }

  check(vacantBitFound.countOneBits() == 1) {
    "STATE ERROR: Vacant bit found must be exactly one bit. Got: $vacantBitFound"
  }

  // --- 3. MODIFICATION: Apply the shifts in REVERSE ---
  // Mutates in place; moving the last piece first prevents teleportation bugs
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("Applying ${movesToApply.size} shifts in reverse order to prevent overwrite...") }
  }

  movesToApply.asReversed().forEach { (fromMask, toMask) ->
                                 logger.debug { "" + ("  -> Shifting piece from $fromMask to $toMask.") }
                                 logger.debug { "" + ("  -> board before shifting: 0b${globalOccupancy.toString(2)}") }
    applyShiftToAll(fromMask, toMask)
                                 logger.debug { "" + ("  -> board after shifting:  0b${globalOccupancy.toString(2)}") }
  }

                               logger.debug { "" + ("--- PUSH UP COMPLETE. Returning vacant bit: $vacantBitFound ---") }
  return vacantBitFound
}

fun Bitboard.executePushDown(col: ColumnInfo): ULong {
  // --- 0. CONFIGURABLE DEBUGGING ---
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- EXECUTE PUSH DOWN CALLED ---") }
    logger.info { "" + ("Column: $col") }
  }

  require(col.shiftPairs.isNotEmpty()) {
    "CRITICAL ERROR: Column shiftPairs cannot be empty."
  }

  val occupiedSpots = globalOccupancy
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()
  var gapFound = false
  var vacantBitFound = 0UL

  // --- 1. DISCOVERY: Find out exactly which pieces are shifting ---
                               logger.debug { "" + ("Starting discovery phase for Push Down...") }

  for ((toMask, fromMask) in col.shiftPairs.asReversed()) {
    // Strict bit validation: Ensure masks aren't corrupted
    check(fromMask.countOneBits() == 1 && toMask.countOneBits() == 1) {
      "STATE ERROR: Shift masks must contain exactly one bit. fromMask: $fromMask, toMask: $toMask"
    }

    if ((occupiedSpots and fromMask) != 0UL) {
      movesToApply.add(fromMask to toMask)

          logger.debug { "" + ("  -> Piece found at ${fromMask.toString(2)}. Queuing shift to ${toMask.toString(2)}.") }
    } else {
      gapFound = true
      vacantBitFound = fromMask

          logger.debug { "" + ("  -> Gap discovered at ${fromMask.toString(2)}. Ending discovery phase.") }
      break
    }

    if ((occupiedSpots and toMask) == 0UL) {
      gapFound = true
      vacantBitFound = toMask

          logger.debug { "" + ("  -> Gap discovered at ${toMask.toString(2)}. Ending discovery phase.") }
      break
    }
  }

  // --- 2. VALIDATION: Check if pieces fall off the board ---
  check(gapFound) {
    "ILLEGAL MOVE: Cannot shift column; board line is full and pieces would fall off."
  }

  check(vacantBitFound.countOneBits() == 1) {
    "STATE ERROR: Vacant bit found must be exactly one bit. Got: $vacantBitFound"
  }

  // --- 3. MODIFICATION: Apply the shifts in REVERSE ---
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("Applying ${movesToApply.size} shifts in reverse order to prevent overwrite...") }
  }

  movesToApply.asReversed().forEach { (fromMask, toMask) ->
                                 logger.debug { "" + ("  -> Shifting piece from $fromMask to $toMask.") }
                                 logger.debug { "" + ("  -> board before shifting: 0b${globalOccupancy.toString(2)}") }
    applyShiftToAll(fromMask, toMask)
                                 logger.debug { "" + ("  -> board after shifting:  0b${globalOccupancy.toString(2)}") }
  }

                               logger.debug { "" + ("--- PUSH DOWN COMPLETE. Returning vacant bit: $vacantBitFound ---") }
  return vacantBitFound
}

fun Bitboard.executePullDown(col: ColumnInfo, vacantBitFound: ULong) {
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()

  // 1. DISCOVERY: Find exactly which pieces need to be pulled back.
  // We iterate forward, stopping the moment the original push's source mask
  // matches the vacant bit we recorded.
  for ((fromMask, toMask) in col.shiftPairs) {
    if (fromMask == vacantBitFound) {
      // We reached the gap that stopped the original push.
      break
    }
    // Revert the direction: pull back from `toMask` to `fromMask`
    movesToApply.add(toMask to fromMask)
  }

  // 2. MODIFICATION: Apply the shifts safely
  // Apply in forward order to pull pieces into the gap at the edge.
  movesToApply.forEach { (currentMask, newMask) ->
    applyShiftToAll(currentMask, newMask)
  }
}

fun Bitboard.executePullUp(col: ColumnInfo, vacantBitFound: ULong) {
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()

  // 1. DISCOVERY: Find exactly which pieces need to be pulled back.
  // We iterate in reverse, stopping when the source mask matches the vacant bit.
  // Note: Your original push destructured as (toMask, fromMask), so we do the same.
  for ((toMask, fromMask) in col.shiftPairs.asReversed()) {
    if (fromMask == vacantBitFound) {
      // We reached the gap that stopped the original push.
      break
    }
    // Revert the direction: pull back from `toMask` to `fromMask`
    movesToApply.add(toMask to fromMask)
  }

  // 2. MODIFICATION: Apply the shifts safely
  movesToApply.forEach { (currentMask, newMask) ->
    applyShiftToAll(currentMask, newMask)
  }
}

// endregion

fun Bitboard.convertBitboardToBoard(board: Board): Board {
  val updatedNodes =
      board.nodes
          .map { node ->
            val bitmask = node.bitmask

            // 1. Boundary nodes (dots) have bitmask = 18446744073709551615UL. Leave them alone.
            if (bitmask == 18446744073709551615UL) {
              node // No modification needed
            } else {
              // 2. Active board spots
              if ((globalOccupancy and bitmask) == 0UL) {
                node.copy(piece = null) // Safe non-mutating copy
              } else {

                // construct piece and place it on the matching node
                // 2. Determine base-piece color (which memory array owns the physical stack
                // structure)
                require(bitmask.countOneBits() == 1) {
                  "CRITICAL ERROR: bitmask must be exactly one bit to determine color. Got: $bitmask"
                }

                // --- 2. AGGREGATE MASKS ---
                // Combine all piece locations for a given color into one flat mask
                val whiteAggregate =
                    whiteGIPF or
                        whiteTAMSK or
                        whiteZERTZ or
                        whiteYINSH or
                        whiteDVONNLayer[0] or
                        whitePUNCTLayer[0]

                val blackAggregate =
                    blackGIPF or
                        blackTAMSK or
                        blackZERTZ or
                        blackYINSH or
                        blackDVONNLayer[0] or
                        blackPUNCTLayer[0]

                // --- 3. EVALUATE ---
                val color =
                    when {
                      (whiteAggregate and bitmask) != 0UL -> PlayerName.WHITE
                      (blackAggregate and bitmask) != 0UL -> PlayerName.BLACK
                      else -> null
                    }

                requireNotNull(color)

                val isNeutralized =
                    (whiteNeutralized and bitmask) == bitmask ||
                        (blackNeutralized and bitmask) == bitmask

                val hasPotential =
                    when (color) {
                      PlayerName.WHITE -> {
                        (whitePotentials and bitmask) == bitmask
                      }

                      PlayerName.BLACK -> {
                        (blackPotentials and bitmask) == bitmask
                      }
                    }

                val pieceType: PieceType? =
                    when (bitmask) {
                      (whiteGIPF and bitmask),
                      (blackGIPF and bitmask) -> PieceType.GIPF
                      (whiteTAMSK and bitmask),
                      (blackTAMSK and bitmask) -> PieceType.TAMSK
                      (whiteZERTZ and bitmask),
                      (blackZERTZ and bitmask) -> PieceType.ZERTZ
                      (whiteYINSH and bitmask),
                      (blackYINSH and bitmask) -> PieceType.YINSH
                      (whiteDVONNLayer[0] and bitmask),
                      (blackDVONNLayer[0] and bitmask) -> PieceType.DVONN
                      (whitePUNCTLayer[0] and bitmask),
                      (blackPUNCTLayer[0] and bitmask) -> PieceType.PUNCT
                      else -> null
                    }

                requireNotNull(pieceType)

                val stackedPieces: MutableList<Piece> =
                    when (color) {
                      PlayerName.WHITE -> {
                        when (pieceType) {
                          PieceType.NULL,
                          PieceType.GIPF,
                          PieceType.TAMSK,
                          PieceType.ZERTZ,
                          PieceType.YINSH -> {
                            mutableListOf()
                          }
                          PieceType.DVONN -> {
                            whiteDVONNLayer
                                .mapIndexedNotNull { index, layer ->
                                  if (index in 1..7 && (layer and bitmask) == bitmask) {
                                    val pieceColor =
                                        if (index % 2 == 0) PlayerName.WHITE else PlayerName.BLACK
                                    Piece(
                                        abbreviation =
                                            "${pieceColor.name.first()}${PieceType.DVONN.name.first()}",
                                        potential = false,
                                        colorName = pieceColor,
                                        type = PieceType.DVONN,
                                        isNeutralized = false,
                                        stackedPieces = mutableListOf(),
                                    )
                                  } else null
                                }
                                .toMutableList()
                          }

                          PieceType.PUNCT -> {
                            whitePUNCTLayer
                                .mapIndexedNotNull { index, layer ->
                                  if (index in 1..7 && (layer and bitmask) == bitmask) {
                                    val pieceColor =
                                        if (index % 2 == 0) PlayerName.WHITE else PlayerName.BLACK
                                    Piece(
                                        abbreviation =
                                            "${pieceColor.name.first()}${PieceType.PUNCT.name.first()}",
                                        potential = false,
                                        colorName = pieceColor,
                                        type = PieceType.PUNCT,
                                        isNeutralized = false,
                                        stackedPieces = mutableListOf(),
                                    )
                                  } else null
                                }
                                .toMutableList()
                          }
                        }
                      }

                      PlayerName.BLACK -> {
                        when (pieceType) {
                          PieceType.NULL,
                          PieceType.GIPF,
                          PieceType.TAMSK,
                          PieceType.ZERTZ,
                          PieceType.YINSH,
                          -> {
                            mutableListOf()
                          }
                          PieceType.DVONN -> {
                            blackDVONNLayer
                                .mapIndexedNotNull { index, layer ->
                                  if (index in 1..7 && (layer and bitmask) == bitmask) {
                                    val pieceColor =
                                        if (index % 2 == 0) PlayerName.BLACK else PlayerName.WHITE
                                    Piece(
                                        abbreviation =
                                            "${pieceColor.name.first()}${PieceType.DVONN.name.first()}",
                                        potential = false,
                                        colorName = pieceColor,
                                        type = PieceType.DVONN,
                                        isNeutralized = false,
                                        stackedPieces = mutableListOf(),
                                    )
                                  } else null
                                }
                                .toMutableList()
                          }

                          PieceType.PUNCT -> {
                            blackPUNCTLayer
                                .mapIndexedNotNull { index, layer ->
                                  if (index in 1..7 && (layer and bitmask) == bitmask) {
                                    val pieceColor =
                                        if (index % 2 == 0) PlayerName.BLACK else PlayerName.WHITE
                                    Piece(
                                        abbreviation =
                                            "${pieceColor.name.first()}${PieceType.PUNCT.name.first()}",
                                        potential = false,
                                        colorName = pieceColor,
                                        type = PieceType.PUNCT,
                                        isNeutralized = false,
                                        stackedPieces = mutableListOf(),
                                    )
                                  } else null
                                }
                                .toMutableList()
                          }
                        }
                      }
                    }

                val piece =
                    Piece(
                        abbreviation = "${color.name.first()}${pieceType.name.first()}",
                        potential = hasPotential,
                        colorName = color,
                        type = pieceType,
                        isNeutralized = isNeutralized,
                        stackedPieces = stackedPieces,
                    )

                // TODO does this update in place
                node.copy(piece = piece)
              }
            }
          }
          .toSet()

  return board.copy(nodes = updatedNodes)
}

/*fun Bitboard.shiftPieces(index: Int, pushDirection: PushDirection) {
  require(index in openningLineIndexArray) {
    "Invalid opening line placement! Index $index is not a valid selection. " +
        "Eligible indices: ${openningLineIndexArray.joinToString()}"
  }

  // TODO check if line has empty spots

  when (pushDirection) {
    PushDirection.UP -> {
      var updatedBitboard = whitePieces
      var packedLine = 0UL
      val indexArray = verticalLineIndexArray.first { it.contains(index) }
      indexArray.forEach { i ->
        val mask = 1UL shl i
        val value = if ((whitePieces and mask) > 0UL) 1UL else 0UL
        packedLine = (packedLine shl 1) + value

        // clear bit
        updatedBitboard = updatedBitboard and (value shl i).inv()
      }
      check(packedLine.countTrailingZeroBits() > 0) { "" }

      val shiftedPackedLine = packedLine shr 1

      // update bitboards
      indexArray.reversed().forEachIndexed { idx, i ->
        val mask = 1UL
        //				logger.info { "" + ("shiftedPackedLine: ${(shiftedPackedLine shr idx).toString(radix =
        // 2).padStart(8, '0')}") }
        //				logger.info { "" + ("mask             : ${mask.toString(radix = 2).padStart(8, '0')}") }

        val value = if (((shiftedPackedLine shr idx) and mask) > 0.toUInt()) 1UL else 0UL

        updatedBitboard = updatedBitboard or (value shl i)

        //				logger.info { "" + ("value            : ${(value shl i).toString(radix = 2).padStart(64, '0')}") }
        //				logger.info { "" + ("whitePieces      : ${whitePieces.toString(radix = 2).padStart(64, '0')}") }
        //				logger.info { "" + ("updatedBitboard  : ${updatedBitboard.toString(radix = 2).padStart(64, '0')}") }
      }
    }
    PushDirection.DOWN -> {
      val indexArray = verticalLineIndexArray.first { it.contains(index) }
    }
    PushDirection.UPPER_RIGHT -> {
      val indexArray = upwardRightLineIndexArray.first { it.contains(index) }
    }
    PushDirection.LOWER_RIGHT -> {
      val indexArray = upwardRightLineIndexArray.first { it.contains(index) }
    }
    PushDirection.UPPER_LEFT -> {
      val indexArray = downwardRightLineIndexArray.first { it.contains(index) }
    }
    PushDirection.LOWER_LEFT -> {
      val indexArray = downwardRightLineIndexArray.first { it.contains(index) }
    }
  }
}*/

/*fun Bitboard.movePieceBit(fromIndex: Int, toIndex: Int) {
	// region From Gemini
	val fromMask = 1UL shl fromIndex
	val toMask = 1UL shl toIndex
	val clearFrom = fromMask.inv()

	// 1. Identify what is currently sitting at 'fromIndex'
	val isWhite = (whitePieces and fromMask) != 0UL
	val isBlack = (blackPieces and fromMask) != 0UL
	val isStack = (stackPieces and fromMask) != 0UL
	val isGipf  = (gipfPieces  and fromMask) != 0UL

	// 2. Clear the 'fromIndex' across all boards
	whitePieces = whitePieces and clearFrom
	blackPieces = blackPieces and clearFrom
	stackPieces = stackPieces and clearFrom
	gipfPieces  = gipfPieces  and clearFrom

	// 3. Set the 'toIndex' on the matching boards
	if (isWhite) whitePieces = whitePieces or toMask
	if (isBlack) blackPieces = blackPieces or toMask
	if (isStack) stackPieces = stackPieces or toMask
	if (isGipf)  gipfPieces  = gipfPieces  or toMask

	// endregion
}*/

fun Bitboard.getActiveWhiteDvonnPieces(): ULong {
  // TODO("Implement")
  val layers = 5 // whiteDVONNLayer.size.minus(1)
  // region From Gemini
  var activeWhiteDvonn = 0UL
  var blockedDvonnMask = 0UL // Tracks all pieces we've seen so far from the top down

  // Iterate from the highest possible layer down to the board (Layer 0)
  for (i in layers downTo 0) {
    // 1. Grab the pieces on this layer, but ONLY if they aren't blocked by a higher layer
    // if layer [i] is even it is the color of the base layer
    // if layer [i] is odd it is the opposing color
    var visibleWhiteThisLayer = 0UL
    var visibleBlackThisLayer = 0UL

    if (i.mod(2) == 0) {
      // even layer
      visibleWhiteThisLayer = whiteDVONNLayer[i] and blockedDvonnMask.inv()
      visibleBlackThisLayer = blackDVONNLayer[i] and blockedDvonnMask.inv()
    } else {
      // odd layer
      visibleWhiteThisLayer = blackDVONNLayer[i] and blockedDvonnMask.inv()
      visibleBlackThisLayer = whiteDVONNLayer[i] and blockedDvonnMask.inv()
    }

    // 2. Add these newly discovered visible pieces to our final surface bitboards
    activeWhiteDvonn = activeWhiteDvonn or visibleWhiteThisLayer

    // 3. Update the blocked mask for the next layer down
    blockedDvonnMask = blockedDvonnMask or visibleWhiteThisLayer or visibleBlackThisLayer
  }

  return activeWhiteDvonn
}

fun Bitboard.getActiveBlackDvonnPieces(): ULong {
  // TODO("Implement")
  val layers = 5 // whiteDVONNLayer.size.minus(1)
  // region From Gemini
  var activeBlackDvonn = 0UL
  var blockedDvonnMask = 0UL // Tracks all pieces we've seen so far from the top down

  // Iterate from the highest possible layer down to the board (Layer 0)
  for (i in layers downTo 0) {
    // 1. Grab the pieces on this layer, but ONLY if they aren't blocked by a higher layer
    // if layer [i] is even it is the color of the base layer
    // if layer [i] is odd it is the opposing color
    var visibleWhiteThisLayer = 0UL
    var visibleBlackThisLayer = 0UL

    if (i.mod(2) == 0) {
      // even layer
      visibleWhiteThisLayer = whiteDVONNLayer[i] and blockedDvonnMask.inv()
      visibleBlackThisLayer = blackDVONNLayer[i] and blockedDvonnMask.inv()
    } else {
      // odd layer
      visibleWhiteThisLayer = blackDVONNLayer[i] and blockedDvonnMask.inv()
      visibleBlackThisLayer = whiteDVONNLayer[i] and blockedDvonnMask.inv()
    }

    // 2. Add these newly discovered visible pieces to our final surface bitboards
    activeBlackDvonn = activeBlackDvonn or visibleBlackThisLayer

    // 3. Update the blocked mask for the next layer down
    blockedDvonnMask = blockedDvonnMask or visibleWhiteThisLayer or visibleBlackThisLayer
  }
  // endregion
  return activeBlackDvonn
}

fun Bitboard.getActiveWhitePunctPieces(): ULong {
  // TODO("Implement")
  val layers = 5 // whiteDVONNLayer.size.minus(1)
  // region From Gemini
  var activeWhitePunct = 0UL
  var blockedPunctMask = 0UL // Tracks all pieces we've seen so far from the top down

  // Iterate from the highest possible layer down to the board (Layer 0)
  for (i in layers downTo 0) {
    // 1. Grab the pieces on this layer, but ONLY if they aren't blocked by a higher layer
    // if layer [i] is even it is the color of the base layer
    // if layer [i] is odd it is the opposing color
    var visibleWhiteThisLayer = 0UL
    var visibleBlackThisLayer = 0UL
    if (i.mod(2) == 0) {
      // even layer
      visibleWhiteThisLayer = whitePUNCTLayer[i] and blockedPunctMask.inv()
      visibleBlackThisLayer = blackPUNCTLayer[i] and blockedPunctMask.inv()
    } else {
      // odd layer
      visibleWhiteThisLayer = blackPUNCTLayer[i] and blockedPunctMask.inv()
      visibleBlackThisLayer = whitePUNCTLayer[i] and blockedPunctMask.inv()
    }

    // 2. Add these newly discovered visible pieces to our final surface bitboards
    activeWhitePunct = activeWhitePunct or visibleWhiteThisLayer

    // 3. Update the blocked mask for the next layer down
    blockedPunctMask = blockedPunctMask or visibleWhiteThisLayer or visibleBlackThisLayer
  }
  // endregion
  return activeWhitePunct
}

fun Bitboard.getActiveBlackPunctPieces(): ULong {
  // TODO("Implement")
  val layers = 5 // whiteDVONNLayer.size.minus(1)
  // region From Gemini
  var activeBlackPunct = 0UL
  var blockedPunctMask = 0UL // Tracks all pieces we've seen so far from the top down

  // Iterate from the highest possible layer down to the board (Layer 0)
  for (i in layers downTo 0) {
    // 1. Grab the pieces on this layer, but ONLY if they aren't blocked by a higher layer
    // if layer [i] is even it is the color of the base layer
    // if layer [i] is odd it is the opposing color
    var visibleWhiteThisLayer = 0UL
    var visibleBlackThisLayer = 0UL
    if (i.mod(2) == 0) {
      // even layer
      visibleWhiteThisLayer = whitePUNCTLayer[i] and blockedPunctMask.inv()
      visibleBlackThisLayer = blackPUNCTLayer[i] and blockedPunctMask.inv()
    } else {
      // odd layer
      visibleWhiteThisLayer = blackPUNCTLayer[i] and blockedPunctMask.inv()
      visibleBlackThisLayer = whitePUNCTLayer[i] and blockedPunctMask.inv()
    }

    // 2. Add these newly discovered visible pieces to our final surface bitboards
    activeBlackPunct = activeBlackPunct or visibleBlackThisLayer

    // 3. Update the blocked mask for the next layer down
    blockedPunctMask = blockedPunctMask or visibleWhiteThisLayer or visibleBlackThisLayer
  }
  // endregion
  return activeBlackPunct
}

fun Bitboard.usePiecePotential(
    possibleBitMove: PossibleBitMove,
    currentPlayer: Player,
    nextPlayer: Player,
) {
  // TODO filter for legal moves before calling this
  //  pieces can not be neutralized

  require(possibleBitMove.pieceType != null) { "No piece was selected!" }
  require(possibleBitMove.pieceColor != null) { "No piece was selected!" }
  require(possibleBitMove.sourceBit != null) { "No piece was selected!" }
  require(possibleBitMove.targetBit != null) { "No piece was selected!" }
  require(possibleBitMove.sourceBit != possibleBitMove.targetBit) {
    "Movement violation: Origin and destination bit indexes must be distinct. Cannot use a piece potential on itself"
  }

  // --- 0. CONFIGURABLE DEBUGGING ---
  val playerPotentials =
      if (possibleBitMove.pieceColor == PlayerName.WHITE) whitePotentials else blackPotentials
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- USE POTENTIAL CALLED ---") }
    logger.info { "" + ("Piece: ${possibleBitMove.pieceColor} ${possibleBitMove.pieceType}") }
    logger.info { "" + ("Source Bit:                   0b${possibleBitMove.sourceBit.toString(2).padStart(40, '0')}") }
    logger.info { "" + ("Target Bit:                   0b${possibleBitMove.targetBit.toString(2).padStart(40, '0')}") }
    logger.info { "" + ("Player Potentials:            0b${playerPotentials.toString(2).padStart(40, '0')}") }
    logger.info { "" + ("Board before using potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}") }
  }

  // --- 1. PRE-CONDITION CHECKS (Input Validation) ---
  require(possibleBitMove.sourceBit.countOneBits() == 1) {
    "CRITICAL ERROR: sourceBit must be exactly one bit. Got: ${possibleBitMove.sourceBit}"
  }
  require(possibleBitMove.targetBit.countOneBits() == 1) {
    "CRITICAL ERROR: targetBit must be exactly one bit. Got: ${possibleBitMove.targetBit}"
  }

  fun safeRemoveAndCheck(board: ULong, boardName: String, removeAtIndex: ULong): ULong {
    if (logger.isDebugEnabled()) {
      logger.info { "" + ("$boardName Board before using $boardName potential\n" +
              "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
              "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}") }
    }
    check((board and removeAtIndex) != 0UL) {
      "UNDO FAILED: Expected ${possibleBitMove.pieceColor} ${possibleBitMove.pieceType} in $boardName bitboard at $removeAtIndex, but it was missing."
    }

    val newBoard = board and removeAtIndex.inv()

    check((newBoard and removeAtIndex) == 0UL) {
      "UNDO FAILED: Failed to use potential for ${possibleBitMove.pieceColor} ${possibleBitMove.pieceType} piece from $boardName bitboard."
    }
    if (logger.isDebugEnabled()) {
      logger.info { "" + ("$boardName Board after using $boardName potential\n" +
              "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
              "new $boardName bitboard:      0b${newBoard.toString(2).padStart(40, '0')}") }
    }
                                 logger.debug { "" + ("Successfully removed bit from $boardName.") }
    return newBoard
  }

  fun safeAddAndCheck(board: ULong, boardName: String, addAtIndex: ULong): ULong {
    if (logger.isDebugEnabled()) {
      logger.info { "" + ("$boardName Board before adding piece at \n" +
              "addAtIndex:    0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
              "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}") }
    }
    val newBoard = board or addAtIndex
    check((newBoard and addAtIndex) == addAtIndex) {
      "ADD FAILED: Failed to add ${possibleBitMove.pieceColor} ${possibleBitMove.pieceType} piece to $boardName bitboard at index $addAtIndex."
    }
    if (logger.isDebugEnabled()) {
      logger.info { "" + ("$boardName Board after adding piece at \n" +
              "addAtIndex:    0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
              "new $boardName bitboard:      0b${newBoard.toString(2).padStart(40, '0')}") }
    }
                                 logger.debug { "" + ("Successfully added bit to $boardName.") }
    return newBoard
  }

  // TODO which bitboards to add piece
  when (possibleBitMove.pieceColor) {
    PlayerName.WHITE -> {
      when (possibleBitMove.pieceType) {
        PieceType.NULL -> {}
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {}

        PieceType.ZERTZ -> {
          whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ", possibleBitMove.targetBit)
          whitePotentials =
              safeRemoveAndCheck(whitePotentials, "White Potentials", possibleBitMove.sourceBit)
        }

        PieceType.YINSH -> {
          whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH", possibleBitMove.targetBit)
          whitePotentials =
              safeRemoveAndCheck(whitePotentials, "White Potentials", possibleBitMove.sourceBit)
        }

        // TODO something about this seems wrong
        PieceType.DVONN -> {
          for (i in 1..7) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whiteDVONNLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeAddAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", possibleBitMove.targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackDVONNLayer[i] =
                  safeAddAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", possibleBitMove.targetBit)
              break
            }
          }

          whitePotentials =
              safeRemoveAndCheck(whitePotentials, "White Potentials", possibleBitMove.sourceBit)

          val totalWhiteDVONNPiecesOutPlay =
              currentPlayer.piecesInReserve
                  .filter {
                    it.extractPieceType() == PieceType.DVONN &&
                        it.extractPieceColor() == PlayerName.WHITE
                  }
                  .sumOf { if (it.extractPotential()) 2 else 1 } +
                  currentPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.piecesInReserve
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 }

          val totalBlackDVONNPiecesOutPlay =
              currentPlayer.piecesInReserve
                  .filter {
                    it.extractPieceType() == PieceType.DVONN &&
                        it.extractPieceColor() == PlayerName.BLACK
                  }
                  .sumOf { if (it.extractPotential()) 2 else 1 } +
                  currentPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.piecesInReserve
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 }

          val totalWhiteDVONNPiecesInPlay =
              whiteDVONNLayer[0].countOneBits() +
                  (whiteDVONNLayer[0] and whitePotentials).countOneBits() +
                  whiteDVONNLayer[2].countOneBits() +
                  whiteDVONNLayer[4].countOneBits() +
                  whiteDVONNLayer[6].countOneBits() +
                  blackDVONNLayer[1].countOneBits() +
                  blackDVONNLayer[3].countOneBits() +
                  blackDVONNLayer[5].countOneBits() +
                  blackDVONNLayer[7].countOneBits()

          val totalBlackDVONNPiecesInPlay =
              blackDVONNLayer[0].countOneBits() +
                  (blackDVONNLayer[0] and blackPotentials).countOneBits() +
                  blackDVONNLayer[2].countOneBits() +
                  blackDVONNLayer[4].countOneBits() +
                  blackDVONNLayer[6].countOneBits() +
                  whiteDVONNLayer[1].countOneBits() +
                  whiteDVONNLayer[3].countOneBits() +
                  whiteDVONNLayer[5].countOneBits() +
                  whiteDVONNLayer[7].countOneBits()

          check((totalWhiteDVONNPiecesOutPlay + totalWhiteDVONNPiecesInPlay) == 6) {}

          check((totalBlackDVONNPiecesOutPlay + totalBlackDVONNPiecesInPlay) == 6) {}

          //          check(whiteDVONNLayer[4].countOneBits() != 1 ||
          // (whiteDVONNLayer[0].countOneBits() + totalWhiteDVONNPiecesOutPlay) in 3..4) {
          //            "DVONN Layer 4 Invariant Broken: Black layer 4 has 1 active bit, " +
          //                "but base white layer 0 has ${whiteDVONNLayer[0].countOneBits()} bits
          // (expected 3)."
          //          }

          check(whiteDVONNLayer[2].countOneBits() != 1 || whiteDVONNLayer[0].countOneBits() >= 2) {
            "DVONN Layer 2 Invariant Broken: Black layer 2 has 1 active bit, " +
                "but base white layer 0 has ${whiteDVONNLayer[0].countOneBits()} bits (expected at least 2)."
          }

          //          check(whiteDVONNLayer[5].countOneBits() != 1 ||
          // (blackDVONNLayer[0].countOneBits() + totalBlackDVONNPiecesOutPlay) == 3) {
          //            "DVONN Layer 5 Invariant Broken: Black layer 5 has 1 active bit, " +
          //                "but base black layer 0 has ${blackDVONNLayer[0].countOneBits()} bits
          // (expected 3)."
          //          }

          check(
              whiteDVONNLayer[3].countOneBits() != 1 ||
                  (blackDVONNLayer[0].countOneBits() + totalBlackDVONNPiecesOutPlay) >= 2
          ) {
            "DVONN Layer 3 Invariant Broken: Black layer 3 has 1 active bit, " +
                "but base black layer 0 has ${blackDVONNLayer[0].countOneBits()} bits (expected at least 2)."
          }

          check(
              whiteDVONNLayer[1].countOneBits() != 1 ||
                  (blackDVONNLayer[0].countOneBits() + totalBlackDVONNPiecesOutPlay) >= 1
          ) {
            "DVONN Layer 1 Invariant Broken: Black layer 1 has 1 active bit, " +
                "but base black layer 0 has ${blackDVONNLayer[0].countOneBits()} bits (expected at least 1)."
          }
        }

        PieceType.PUNCT -> {
          for (i in 1..7) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whitePUNCTLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeAddAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", possibleBitMove.targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeAddAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", possibleBitMove.targetBit)
              break
            }
          }

          whitePotentials =
              safeRemoveAndCheck(whitePotentials, "White Potentials", possibleBitMove.sourceBit)

          val totalWhitePUNCTPiecesOutPlay =
              currentPlayer.piecesInReserve
                  .filter {
                    it.extractPieceType() == PieceType.PUNCT &&
                        it.extractPieceColor() == PlayerName.WHITE
                  }
                  .sumOf { if (it.extractPotential()) 2 else 1 } +
                  currentPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.piecesInReserve
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 }

          val totalBlackPUNCTPiecesOutPlay =
              currentPlayer.piecesInReserve
                  .filter {
                    it.extractPieceType() == PieceType.PUNCT &&
                        it.extractPieceColor() == PlayerName.BLACK
                  }
                  .sumOf { if (it.extractPotential()) 2 else 1 } +
                  currentPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.piecesInReserve
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 }

          val totalWhitePUNCTPiecesInPlay =
              whitePUNCTLayer[0].countOneBits() +
                  (whitePUNCTLayer[0] and whitePotentials).countOneBits() +
                  whitePUNCTLayer[2].countOneBits() +
                  whitePUNCTLayer[4].countOneBits() +
                  whitePUNCTLayer[6].countOneBits() +
                  blackPUNCTLayer[1].countOneBits() +
                  blackPUNCTLayer[3].countOneBits() +
                  blackPUNCTLayer[5].countOneBits() +
                  blackPUNCTLayer[7].countOneBits()

          val totalBlackPUNCTPiecesInPlay =
              blackPUNCTLayer[0].countOneBits() +
                  (blackPUNCTLayer[0] and blackPotentials).countOneBits() +
                  blackPUNCTLayer[2].countOneBits() +
                  blackPUNCTLayer[4].countOneBits() +
                  blackPUNCTLayer[6].countOneBits() +
                  whitePUNCTLayer[1].countOneBits() +
                  whitePUNCTLayer[3].countOneBits() +
                  whitePUNCTLayer[5].countOneBits() +
                  whitePUNCTLayer[7].countOneBits()

          check((totalWhitePUNCTPiecesOutPlay + totalWhitePUNCTPiecesInPlay) == 6) {}

          check((totalBlackPUNCTPiecesOutPlay + totalBlackPUNCTPiecesInPlay) == 6) {}

          //          check(whitePUNCTLayer[4].countOneBits() != 1 ||
          // (whitePUNCTLayer[0].countOneBits() + totalWhitePUNCTPiecesOutPlay) in 3..4) {
          //            "PUNCT Layer 4 Invariant Broken: Black layer 4 has 1 active bit, " +
          //                "but base white layer 0 has ${whitePUNCTLayer[0].countOneBits()} bits
          // (expected 3 or 4)."
          //          }

          check(whitePUNCTLayer[2].countOneBits() != 1 || whitePUNCTLayer[0].countOneBits() >= 2) {
            "PUNCT Layer 2 Invariant Broken: Black layer 2 has 1 active bit, " +
                "but base white layer 0 has ${whitePUNCTLayer[0].countOneBits()} bits (expected at least 2)."
          }

          //          check(whitePUNCTLayer[5].countOneBits() != 1 ||
          // (blackPUNCTLayer[0].countOneBits() + totalBlackPUNCTPiecesOutPlay) == 3) {
          //            "PUNCT Layer 5 Invariant Broken: Black layer 5 has 1 active bit, " +
          //                "but base black layer 0 has ${blackPUNCTLayer[0].countOneBits()} bits
          // (expected 3)."
          //          }

          check(
              whitePUNCTLayer[3].countOneBits() != 1 ||
                  (blackPUNCTLayer[0].countOneBits() + totalBlackPUNCTPiecesOutPlay) >= 2
          ) {
            "PUNCT Layer 3 Invariant Broken: Black layer 3 has 1 active bit, " +
                "but base black layer 0 has ${blackPUNCTLayer[0].countOneBits()} bits (expected at least 2)."
          }

          check(
              whitePUNCTLayer[1].countOneBits() != 1 ||
                  (blackPUNCTLayer[0].countOneBits() + totalBlackPUNCTPiecesOutPlay) >= 1
          ) {
            "PUNCT Layer 1 Invariant Broken: Black layer 1 has 1 active bit, " +
                "but base black layer 0 has ${blackPUNCTLayer[0].countOneBits()} bits (expected at least 1)."
          }
        }
      }
    }

    PlayerName.BLACK -> {
      when (possibleBitMove.pieceType) {
        PieceType.NULL -> {}
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {}

        PieceType.ZERTZ -> {
          blackZERTZ = safeAddAndCheck(blackZERTZ, "Black YINSH", possibleBitMove.targetBit)
          blackPotentials =
              safeRemoveAndCheck(blackPotentials, "Black Potentials", possibleBitMove.sourceBit)
        }

        PieceType.YINSH -> {
          blackYINSH = safeAddAndCheck(blackYINSH, "Black YINSH", possibleBitMove.targetBit)
          blackPotentials =
              safeRemoveAndCheck(blackPotentials, "Black Potentials", possibleBitMove.sourceBit)
        }

        // TODO confirm if possibleBitMove.targetBit/Spot if is white
        // TODO something about this seems wrong
        PieceType.DVONN -> {

          for (i in 1..7) {
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (whiteDVONNLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeAddAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", possibleBitMove.targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackDVONNLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackDVONNLayer[i] =
                  safeAddAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", possibleBitMove.targetBit)
              break
            }
          }

          blackPotentials =
              safeRemoveAndCheck(blackPotentials, "Black Potentials", possibleBitMove.sourceBit)

          // TODO Missing PUNCT.
          val totalBlackDVONNPiecesOutPlay =
              currentPlayer.piecesInReserve
                  .filter {
                    it.extractPieceType() == PieceType.DVONN &&
                        it.extractPieceColor() == PlayerName.BLACK
                  }
                  .sumOf { if (it.extractPotential()) 2 else 1 } +
                  currentPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.piecesInReserve
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 }

          val totalWhiteDVONNPiecesOutPlay =
              currentPlayer.piecesInReserve
                  .filter {
                    it.extractPieceType() == PieceType.DVONN &&
                        it.extractPieceColor() == PlayerName.WHITE
                  }
                  .sumOf { if (it.extractPotential()) 2 else 1 } +
                  currentPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.piecesInReserve
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.DVONN &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 }

          val totalBlackDVONNPiecesInPlay =
              blackDVONNLayer[0].countOneBits() +
                  (blackDVONNLayer[0] and blackPotentials).countOneBits() +
                  blackDVONNLayer[2].countOneBits() +
                  blackDVONNLayer[4].countOneBits() +
                  blackDVONNLayer[6].countOneBits() +
                  whiteDVONNLayer[1].countOneBits() +
                  whiteDVONNLayer[3].countOneBits() +
                  whiteDVONNLayer[5].countOneBits() +
                  whiteDVONNLayer[7].countOneBits()

          val totalWhiteDVONNPiecesInPlay =
              whiteDVONNLayer[0].countOneBits() +
                  (whiteDVONNLayer[0] and whitePotentials).countOneBits() +
                  whiteDVONNLayer[2].countOneBits() +
                  whiteDVONNLayer[4].countOneBits() +
                  whiteDVONNLayer[6].countOneBits() +
                  blackDVONNLayer[1].countOneBits() +
                  blackDVONNLayer[3].countOneBits() +
                  blackDVONNLayer[5].countOneBits() +
                  blackDVONNLayer[7].countOneBits()

          check((totalBlackDVONNPiecesOutPlay + totalBlackDVONNPiecesInPlay) == 6) {}

          check((totalWhiteDVONNPiecesOutPlay + totalWhiteDVONNPiecesInPlay) == 6) {}

          //          check(blackDVONNLayer[4].countOneBits() != 1 ||
          // (blackDVONNLayer[0].countOneBits() + totalBlackDVONNPiecesOutPlay) in 3..4) {
          //            "DVONN Layer 4 Invariant Broken: Black layer 4 has 1 active bit, " +
          //                "but base black layer 0 has ${blackDVONNLayer[0].countOneBits()} bits
          // (expected 3)."
          //          }

          check(blackDVONNLayer[2].countOneBits() != 1 || blackDVONNLayer[0].countOneBits() >= 2) {
            "DVONN Layer 2 Invariant Broken: Black layer 2 has 1 active bit, " +
                "but base black layer 0 has ${blackDVONNLayer[0].countOneBits()} bits (expected at least 2)."
          }

          //          check(blackDVONNLayer[5].countOneBits() != 1 ||
          // (whiteDVONNLayer[0].countOneBits() == 3 + totalWhiteDVONNPiecesOutPlay)) {
          //            "DVONN Layer 5 Invariant Broken: Black layer 5 has 1 active bit, " +
          //                "but base white layer 0 has ${whiteDVONNLayer[0].countOneBits()} bits
          // (expected 3)."
          //          }

          check(
              blackDVONNLayer[3].countOneBits() != 1 ||
                  (whiteDVONNLayer[0].countOneBits() + totalWhiteDVONNPiecesOutPlay) >= 2
          ) {
            "DVONN Layer 3 Invariant Broken: Black layer 3 has 1 active bit, " +
                "but base white layer 0 has ${whiteDVONNLayer[0].countOneBits()} bits (expected at least 2)."
          }

          check(
              blackDVONNLayer[1].countOneBits() != 1 ||
                  (whiteDVONNLayer[0].countOneBits() + totalWhiteDVONNPiecesOutPlay) >= 1
          ) {
            "DVONN Layer 1 Invariant Broken: Black layer 1 has 1 active bit, " +
                "but base white layer 0 has ${whiteDVONNLayer[0].countOneBits()} bits (expected at least 1)."
          }

          whiteDVONNLayer[0].countOneBits()
          (whiteDVONNLayer[0] and whitePotentials).countOneBits()
          blackDVONNLayer[1].countOneBits() +
              blackDVONNLayer[3].countOneBits() +
              blackDVONNLayer[5].countOneBits()
        }

        PieceType.PUNCT -> {
          for (i in 1..7) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    (whitePUNCTLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeAddAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", possibleBitMove.targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackPUNCTLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeAddAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", possibleBitMove.targetBit)
              break
            }
          }

          blackPotentials =
              safeRemoveAndCheck(blackPotentials, "Black Potentials", possibleBitMove.sourceBit)

          // TODO Missing PUNCT.
          val totalBlackPUNCTPiecesOutPlay =
              currentPlayer.piecesInReserve
                  .filter {
                    it.extractPieceType() == PieceType.PUNCT &&
                        it.extractPieceColor() == PlayerName.BLACK
                  }
                  .sumOf { if (it.extractPotential()) 2 else 1 } +
                  currentPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.piecesInReserve
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.BLACK
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 }

          val totalWhitePUNCTPiecesOutPlay =
              currentPlayer.piecesInReserve
                  .filter {
                    it.extractPieceType() == PieceType.PUNCT &&
                        it.extractPieceColor() == PlayerName.WHITE
                  }
                  .sumOf { if (it.extractPotential()) 2 else 1 } +
                  currentPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.piecesInReserve
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 } +
                  nextPlayer.capturedPieces
                      .filter {
                        it.extractPieceType() == PieceType.PUNCT &&
                            it.extractPieceColor() == PlayerName.WHITE
                      }
                      .sumOf { if (it.extractPotential()) 2 else 1 }

          val totalBlackPUNCTPiecesInPlay =
              blackPUNCTLayer[0].countOneBits() +
                  (blackPUNCTLayer[0] and blackPotentials).countOneBits() +
                  blackPUNCTLayer[2].countOneBits() +
                  blackPUNCTLayer[4].countOneBits() +
                  blackPUNCTLayer[6].countOneBits() +
                  whitePUNCTLayer[1].countOneBits() +
                  whitePUNCTLayer[3].countOneBits() +
                  whitePUNCTLayer[5].countOneBits() +
                  whitePUNCTLayer[7].countOneBits()

          val totalWhitePUNCTPiecesInPlay =
              whitePUNCTLayer[0].countOneBits() +
                  (whitePUNCTLayer[0] and whitePotentials).countOneBits() +
                  whitePUNCTLayer[2].countOneBits() +
                  whitePUNCTLayer[4].countOneBits() +
                  whitePUNCTLayer[6].countOneBits() +
                  blackPUNCTLayer[1].countOneBits() +
                  blackPUNCTLayer[3].countOneBits() +
                  blackPUNCTLayer[5].countOneBits() +
                  blackPUNCTLayer[7].countOneBits()

          check((totalBlackPUNCTPiecesOutPlay + totalBlackPUNCTPiecesInPlay) == 6) {}

          check((totalWhitePUNCTPiecesOutPlay + totalWhitePUNCTPiecesInPlay) == 6) {}

          //          check(blackPUNCTLayer[4].countOneBits() != 1 ||
          // (blackPUNCTLayer[0].countOneBits() + totalBlackPUNCTPiecesOutPlay) in 3..4) {
          //            "PUNCT Layer 4 Invariant Broken: Black layer 4 has 1 active bit, " +
          //                "but base black layer 0 has ${blackPUNCTLayer[0].countOneBits()} bits
          // (expected 3)."
          //          }

          check(blackPUNCTLayer[2].countOneBits() != 1 || blackPUNCTLayer[0].countOneBits() >= 2) {
            "PUNCT Layer 2 Invariant Broken: Black layer 2 has 1 active bit, " +
                "but base black layer 0 has ${blackPUNCTLayer[0].countOneBits()} bits (expected at least 2)."
          }

          //          check(blackPUNCTLayer[5].countOneBits() != 1 ||
          // (whitePUNCTLayer[0].countOneBits() + totalWhitePUNCTPiecesOutPlay) == 3) {
          //            "PUNCT Layer 5 Invariant Broken: Black layer 5 has 1 active bit, " +
          //                "but base white layer 0 has ${whitePUNCTLayer[0].countOneBits()} bits
          // (expected 3)."
          //          }

          check(
              blackPUNCTLayer[3].countOneBits() != 1 ||
                  (whitePUNCTLayer[0].countOneBits() + totalWhitePUNCTPiecesOutPlay) >= 2
          ) {
            "PUNCT Layer 3 Invariant Broken: Black layer 3 has 1 active bit, " +
                "but base white layer 0 has ${whitePUNCTLayer[0].countOneBits()} bits (expected at least 2)."
          }

          check(
              blackPUNCTLayer[1].countOneBits() != 1 ||
                  (whitePUNCTLayer[0].countOneBits() + totalWhitePUNCTPiecesOutPlay) >= 1
          ) {
            "PUNCT Layer 1 Invariant Broken: Black layer 1 has 1 active bit, " +
                "but base white layer 0 has ${whitePUNCTLayer[0].countOneBits()} bits (expected at least 1)."
          }

          blackPUNCTLayer[1].countOneBits() +
              blackPUNCTLayer[3].countOneBits() +
              blackPUNCTLayer[5].countOneBits()
        }
      }
    }
  }
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- USE POTENTIAL COMPLETE ---") }
    logger.info { "" + ("Board after using potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}") }
  }
}

fun Bitboard.getTamskMoves(player: Player, movesBuffer: MutableList<PossibleBitMove>) {
  // --- 0. CONFIGURABLE DEBUGGING ---
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- GET TAMSK MOVES CALLED ---") }
    logger.info { "" + ("Player: ${player.name}") }
  }

  // --- 1. EVALUATE CENTER SPOT ---
  // A valid TAMSK opening move requires the piece to be on the TAMSK board,
  // still marked as a Potential, and located exactly at the board center.
  val tamskPieceAtCenter =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteTAMSK and whitePotentials and boardCenterSpotMask
        }
        PlayerName.BLACK -> {
          blackTAMSK and blackPotentials and boardCenterSpotMask
        }
      }

  // --- 2. VALIDATE AVAILABILITY ---
  // If the intersection doesn't perfectly match the center spot mask, no move exists.
  if (tamskPieceAtCenter != boardCenterSpotMask) {
    if (logger.isDebugEnabled()) {
      logger.info { "" + ("No valid TAMSK piece found at center for ${player.name}. Returning null.") }
      logger.info { "" + ("--- GET TAMSK MOVES COMPLETED ---") }
    }
    return
  }

  // --- 3. PRE-RETURN SANITY CHECK ---
  // Double-check that our center spot mask hasn't been corrupted into a multi-bit mask.
  check(tamskPieceAtCenter.countOneBits() == 1) {
    "CRITICAL ERROR: Evaluated center spot mask must contain exactly one bit. Got: $tamskPieceAtCenter"
  }

                               logger.debug { "" + ("Valid TAMSK move found for ${player.name} at center spot.") }

  // --- 4. RETURN MOVE ---
  vacantLines.forEach { columnInfo ->
    val start = columnInfo.positions.first() to columnInfo.pushDirections.first
    val end = columnInfo.positions.last() to columnInfo.pushDirections.second

    movesBuffer.add(
        PossibleBitMove(
            sourceBit = tamskPieceAtCenter,
            pieceType = PieceType.TAMSK,
            pieceColor = player.name,
            columnInfo = columnInfo,
            targetBit = start.first,
            pushDirection = start.second,
            moveType = MoveType.AddPiece,
        )
    )

    movesBuffer.add(
        PossibleBitMove(
            sourceBit = tamskPieceAtCenter,
            pieceType = PieceType.TAMSK,
            pieceColor = player.name,
            columnInfo = columnInfo,
            targetBit = end.first,
            pushDirection = end.second,
            moveType = MoveType.AddPiece,
        )
    )
  }
}

// direction to push (start: Push Up, end: push down)

fun Bitboard.useTamskPotential(
    player: Player,
    sourceIndex: ULong,
    targetIndex: ULong,
    col: ColumnInfo,
    pushDirection: PushDirection,
): ULong {
  // --- 0. CONFIGURABLE DEBUGGING ---
  // NEW: Verify the player actually has a piece at the source index before moving it
  val playerPotentials = if (player.name == PlayerName.WHITE) whitePotentials else blackPotentials
  val playerTAMSK = if (player.name == PlayerName.WHITE) whiteTAMSK else blackTAMSK
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- USE TAMSK POTENTIAL CALLED ---") }
    logger.info { "" + ("Player: ${player.name} | Source: $sourceIndex | Target: $targetIndex") }
    logger.info { "" + ("PushDirection: $pushDirection | Col: $col") }
    logger.info { "" + ("Player TAMSK:      0b${playerTAMSK.toString(2).padStart(40, '0')}") }
    logger.info { "" + ("Player Potentials: 0b${playerPotentials.toString(2).padStart(40, '0')}") }
    logger.info { "" + ("CenterMask:        0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
  }

  // --- 1. PRE-CONDITION CHECKS (Input Validation) ---
  require(sourceIndex.countOneBits() == 1) {
    "CRITICAL ERROR: sourceIndex must be exactly one bit. Got: $sourceIndex"
  }
  require(targetIndex.countOneBits() == 1) {
    "CRITICAL ERROR: targetIndex must be exactly one bit. Got: $targetIndex"
  }

  // --- 2. RULE VALIDATION ---
  val isSourceValid = (sourceIndex and boardCenterSpotMask) != 0UL
  val isTargetValid = (targetIndex and openningSpotsLineMask) != 0UL

  check(isSourceValid && isTargetValid) {
    buildString {
      appendLine("ILLEGAL TAMSK OPENING MOVE:")
      if (!isSourceValid) {
        appendLine(
            "  -> Source Violation: Source index (0b${
						sourceIndex.toString(2).padStart(40, '0')
					}) must intersect the board center spot mask (0b${
						boardCenterSpotMask.toString(2).padStart(40, '0')
					})."
        )
      }
      if (!isTargetValid) {
        appendLine(
            "  -> Target Violation: Target index (0b${
						targetIndex.toString(2).padStart(40, '0')
					}) must be fully contained within the opening line mask (0b${
						openningSpotsLineMask.toString(2).padStart(40, '0')
					})."
        )
      }
    }
  }

  check((playerPotentials and sourceIndex) == boardCenterSpotMask) {
    "STATE ERROR: ${player.name} does not have a Potential piece at the specified source index."
  }

  // --- REMOVE POTENTIAL BEFORE SHIFTING ---
  when (player.name) {
    PlayerName.WHITE -> {
      if (logger.isDebugEnabled()) {
        logger.info { "" + ("White TAMSK Board before adding piece:       0b${whiteTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("White Potentials Board before adding piece:  0b${whitePotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }
      // Remove from Potentials board (using exact sourceIndex rather than broad mask)
      whitePotentials = whitePotentials and sourceIndex.inv()
      check((whitePotentials and sourceIndex) == 0UL) {
        "Failed to remove White Potential piece from source index."
      }
      if (logger.isDebugEnabled()) {
        logger.info { "" + ("White TAMSK Board after adding piece:        0b${whiteTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("White Potentials Board after adding piece:   0b${whitePotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }
    }

    PlayerName.BLACK -> {
      if (logger.isDebugEnabled()) {
        logger.info { "" + ("Black TAMSK Board before adding piece:       0b${blackTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("Black Potentials Board before adding piece:  0b${blackPotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }
      // Remove from Potentials board (using exact sourceIndex rather than broad mask)
      blackPotentials = blackPotentials and sourceIndex.inv()
      check((blackPotentials and sourceIndex) == 0UL) {
        "Failed to remove Black Potential piece from source index."
      }

      if (logger.isDebugEnabled()) {
        logger.info { "" + (" Black TAMSK Board after adding piece:        0b${blackTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("Black Potentials Board after adding piece:   0b${blackPotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }
    }
  }

  // --- 3. HANDLE SHIFTING ---
  // Changed `> 0UL` to `!= 0UL` (Standard bitwise evaluation)
  val isIndexOccupied = (globalOccupancy and targetIndex) != 0UL
  var vacantBitFound = 0UL

  if (isIndexOccupied) {
                                 logger.debug { "" + ("Target index is occupied. Executing shift ($pushDirection).") }
    when (pushDirection) {
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        vacantBitFound = executePushUp(col)
                                     logger.debug { "" + ("Executed PushUp. Vacant bit found: $vacantBitFound") }
      }
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        vacantBitFound = executePushDown(col)
                                     logger.debug { "" + ("Executed PushDown. Vacant bit found: $vacantBitFound") }
      }
    }

    // Validate shift execution
    check(vacantBitFound.countOneBits() == 1) {
      "SHIFT ERROR: Shift execution must return exactly one vacant bit. Got: $vacantBitFound"
    }
  } else {
                                 logger.debug { "" + ("Target index is vacant. No shift required.") }
  }

  // --- 4. APPLY TO BITBOARDS (With Post-Condition Checks) ---
                               logger.debug { "" + ("Applying state changes for ${player.name}...") }

  when (player.name) {
    PlayerName.WHITE -> {
      if (logger.isDebugEnabled()) {
        logger.info { "" + ("White TAMSK Board before adding piece:       0b${whiteTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("White Potentials Board before adding piece:  0b${whitePotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }
      // Add to TAMSK board
      whiteTAMSK = whiteTAMSK or targetIndex
      check((whiteTAMSK and targetIndex) != 0UL) {
        "Failed to add White TAMSK piece to target index."
      }
      if (logger.isDebugEnabled()) {
        logger.info { "" + ("White TAMSK Board after adding piece:        0b${whiteTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("White Potentials Board after adding piece:   0b${whitePotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }
    }

    PlayerName.BLACK -> {
      if (logger.isDebugEnabled()) {
        logger.info { "" + ("Black TAMSK Board before adding piece:       0b${blackTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("Black Potentials Board before adding piece:  0b${blackPotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }

      // Add to TAMSK board
      blackTAMSK = blackTAMSK or targetIndex
      check((blackTAMSK and targetIndex) != 0UL) {
        "Failed to add Black TAMSK piece to target index."
      }

      if (logger.isDebugEnabled()) {
        logger.info { "" + ("Black TAMSK Board after adding piece:        0b${blackTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("Black Potentials Board after adding piece:   0b${blackPotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }
    }
  }

  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- USE TAMSK POTENTIAL COMPLETE ---") }
  }

  return vacantBitFound
}

fun Bitboard.undoTamskPotential(
    sourceIndex: ULong,
    removeAtIndex: ULong,
    vacantBitFound: ULong,
    pushDirection: PushDirection,
    col: ColumnInfo,
    player: Player,
    wasIndexOccupied: Boolean,
): ULong {
  // --- 0. CONFIGURABLE DEBUGGING ---
  val playerPotentials = if (player.name == PlayerName.WHITE) whitePotentials else blackPotentials
  val playerTAMSK = if (player.name == PlayerName.WHITE) whiteTAMSK else blackTAMSK
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- UNDO TAMSK POTENTIAL CALLED ---") }
    logger.info { "" + ("Player: ${player.name} | Source: $sourceIndex | RemoveAt: $removeAtIndex") }
    logger.info { "" + ("WasOccupied: $wasIndexOccupied | PushDirection: $pushDirection | Col: $col") }
    logger.info { "" + ("Player TAMSK:      0b${playerTAMSK.toString(2).padStart(40, '0')}") }
    logger.info { "" + ("Player Potentials: 0b${playerPotentials.toString(2).padStart(40, '0')}") }
    logger.info { "" + ("CenterMask:        0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
  }

  // --- 1. PRE-CONDITION CHECKS (Input Validation) ---
  require(sourceIndex.countOneBits() == 1) {
    "CRITICAL ERROR: sourceIndex must be exactly one bit. Got: $sourceIndex"
  }
  require(removeAtIndex.countOneBits() == 1) {
    "CRITICAL ERROR: removeAtIndex must be exactly one bit. Got: $removeAtIndex"
  }
  if (wasIndexOccupied) {
    require(vacantBitFound.countOneBits() == 1) {
      "CRITICAL ERROR: vacantBitFound must be exactly one bit if reverting a shift. Got: $vacantBitFound"
    }
  }

  // --- 2. RULE VALIDATION ---
  val isSourceValid = (sourceIndex and boardCenterSpotMask) != 0UL
  val isTargetValid = (removeAtIndex and openningSpotsLineMask) != 0UL

  check(isSourceValid && isTargetValid) {
    buildString {
      appendLine("ILLEGAL TAMSK UNDO ATTEMPT:")
      if (!isSourceValid) {
        appendLine(
            "  -> Source Violation: Source index (0b${
						sourceIndex.toString(2).padStart(40, '0')
					}) must intersect the board center spot mask (0b${
						boardCenterSpotMask.toString(2).padStart(40, '0')
					})."
        )
      }
      if (!isTargetValid) {
        appendLine(
            "  -> Target Violation: Target index (0b${
						removeAtIndex.toString(2).padStart(40, '0')
					}) must be fully contained within the opening line mask (0b${
						openningSpotsLineMask.toString(2).padStart(40, '0')
					})."
        )
      }
    }
  }

  // --- 3. REVERT STATE MUTATIONS (With Pre- and Post-Condition Checks) ---
                               logger.debug { "" + ("Reverting bitboards for ${player.name}...") }

  when (player.name) {
    PlayerName.WHITE -> {
      // Verify piece is actually there before wiping it
      check((whiteTAMSK and removeAtIndex) != 0UL) {
        "STATE ERROR: Cannot undo; White TAMSK piece missing at target index $removeAtIndex."
      }
      whiteTAMSK = whiteTAMSK and removeAtIndex.inv()
      check((whiteTAMSK and removeAtIndex) == 0UL) {
        "Failed to remove White TAMSK piece from board."
      }
    }

    PlayerName.BLACK -> {
      // Verify piece is actually there before wiping it
      check((blackTAMSK and removeAtIndex) != 0UL) {
        "STATE ERROR: Cannot undo; Black TAMSK piece missing at target index $removeAtIndex."
      }
      blackTAMSK = blackTAMSK and removeAtIndex.inv()
      check((blackTAMSK and removeAtIndex) == 0UL) {
        "Failed to remove Black TAMSK piece from board."
      }
    }
  }

  // --- 4. REVERT SHIFTING ---
  if (wasIndexOccupied) {
                                 logger.debug { "" + ("Reverting board shift. Original push: $pushDirection") }
    when (pushDirection) {
      // If the original move pushed UP, we must pull DOWN to undo
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        executePullDown(col, vacantBitFound)
                                     logger.debug { "" + ("Executed PullDown on col $col") }
      }

      // If the original move pushed DOWN, we must pull UP to undo
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        executePullUp(col, vacantBitFound)
                                     logger.debug { "" + ("Executed PullUp on col $col") }
      }
    }
  } else {
                                 logger.debug { "" + ("Index was not previously occupied. No shifts to revert.") }
  }

  // --- RESTORE POTENTIAL AFTER SHIFTING ---
                               logger.debug { "" + ("--- RESTORE POTENTIAL AFTER SHIFTING ---") }
  when (player.name) {
    PlayerName.WHITE -> {
      if (logger.isDebugEnabled()) {
        logger.info { "" + ("White TAMSK Board before adding piece:       0b${whiteTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("White Potentials Board before adding piece:  0b${whitePotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }

      // Restore the potential piece
      whitePotentials = whitePotentials or sourceIndex
      check((whitePotentials and sourceIndex) != 0UL) {
        "Failed to restore White Potential piece to center."
      }
      if (logger.isDebugEnabled()) {
        logger.info { "" + ("White TAMSK Board after adding piece:        0b${whiteTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("White Potentials Board after adding piece:   0b${whitePotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }
    }

    PlayerName.BLACK -> {
      if (logger.isDebugEnabled()) {
        logger.info { "" + ("Black TAMSK Board before adding piece:       0b${blackTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("Potentials Board before adding piece:  0b${blackPotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }

      // Restore the potential piece
      blackPotentials = blackPotentials or sourceIndex
      check((blackPotentials and sourceIndex) != 0UL) {
        "Failed to restore Black Potential piece to center."
      }

      if (logger.isDebugEnabled()) {
        logger.info { "" + ("TAMSK Board after adding piece:        0b${blackTAMSK.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("Potentials Board after adding piece:   0b${blackPotentials.toString(2).padStart(40, '0')}") }
        logger.info { "" + ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}") }
      }
    }
  }

                               logger.debug { "" + ("--- UNDO TAMSK POTENTIAL COMPLETE ---") }

  return vacantBitFound
}

fun Bitboard.getZertzMoves(
    player: Player,
    columnInfos: List<ColumnInfo>,
    movesBuffer: MutableList<PossibleBitMove>,
) {
  val zertzPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteZERTZ and whitePotentials
        }

        PlayerName.BLACK -> {
          blackZERTZ and blackPotentials
        }
      }

  if (zertzPieces == 0UL) return

  columnInfos.forEach { col ->
    if (col.columnMask and zertzPieces > 0UL) {
      col.positions
          .filter {
            it and (col.columnMask and zertzPieces) != 0UL // get position of zertz pieces
          }
          .forEach {
            val zertzIndex = col.positions.indexOf(it)
            // positions above
            for (index in zertzIndex.plus(1) until col.positions.size) {
              if ((col.positions[index] and globalOccupancy) > 0UL) {
                continue
              } else if (
                  (col.positions[index] and globalOccupancy) == 0UL && index.minus(zertzIndex) > 1
              ) {
                movesBuffer.add(
                    PossibleBitMove(
                        sourceBit = col.positions[zertzIndex],
                        targetBit = col.positions[index],
                        pieceType = PieceType.ZERTZ,
                        pieceColor = player.name,
                        moveType = MoveType.UsePotential,
                    )
                )
                break
              } else {
                break
              }
            }

            // positions below
            for (index in zertzIndex.minus(1) downTo 0) {

              if ((col.positions[index] and globalOccupancy) > 0UL) {
                continue
              } else if (
                  (col.positions[index] and globalOccupancy) == 0UL && zertzIndex.minus(index) > 1
              ) {
                movesBuffer.add(
                    PossibleBitMove(
                        sourceBit = col.positions[zertzIndex],
                        targetBit = col.positions[index],
                        pieceType = PieceType.ZERTZ,
                        pieceColor = player.name,
                        moveType = MoveType.UsePotential,
                    )
                )
                break
              } else {
                break
              }
            }
          }
    }
  }
}

fun Bitboard.getYinshMoves(
    player: Player,
    columnInfos: List<ColumnInfo>,
    movesBuffer: MutableList<PossibleBitMove>,
) {
  val yinchPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteYINSH and whitePotentials
        }

        PlayerName.BLACK -> {
          blackYINSH and blackPotentials
        }
      }

  if (yinchPieces == 0UL) return

  columnInfos.forEach { col ->
    if (col.columnMask and yinchPieces > 0UL) {
      col.positions
          .filter {
            it and (col.columnMask and yinchPieces) != 0UL // get position of yinch pieces
          }
          .forEach {
            val yinchIndex = col.positions.indexOf(it)

            for (index in yinchIndex.plus(1) until col.positions.size) {
              if ((col.positions[index] and globalOccupancy) == 0UL) {
                movesBuffer.add(
                    PossibleBitMove(
                        sourceBit = col.positions[yinchIndex],
                        targetBit = col.positions[index],
                        pieceType = PieceType.YINSH,
                        pieceColor = player.name,
                        moveType = MoveType.UsePotential,
                    )
                )
              } else if ((col.positions[index] and globalOccupancy) > 0UL) {
                break
              }
            }

            for (index in yinchIndex.minus(1) downTo 0) {
              if ((col.positions[index] and globalOccupancy) == 0UL) {
                movesBuffer.add(
                    PossibleBitMove(
                        sourceBit = col.positions[yinchIndex],
                        targetBit = col.positions[index],
                        pieceType = PieceType.YINSH,
                        pieceColor = player.name,
                        moveType = MoveType.UsePotential,
                    )
                )
              } else if ((col.positions[index] and globalOccupancy) > 0UL) {
                break
              }
            }
          }
    }
  }
}

fun Bitboard.getDvonnMoves(
    player: Player,
    columnInfos: List<ColumnInfo>,
    movesBuffer: MutableList<PossibleBitMove>,
) {
  val activeDvonnPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteDVONNLayer[0] and whitePotentials and whiteNeutralized.inv()
        }

        PlayerName.BLACK -> {
          blackDVONNLayer[0] and blackPotentials and blackNeutralized.inv()
        }
      }

  if (activeDvonnPieces == 0UL) return // emptyList()

  val targetDvonnPieces =
      when (player.name) {
        PlayerName.BLACK -> {
          var activeWhiteDvonn = 0UL
          var blockedDvonnMask = 0UL

          for (i in whiteDVONNLayer.indices.reversed()) {
            // 1. Grab the pieces on this layer, but ONLY if they aren't blocked by a higher layer
            // if layer [i] is even it is the color of the base layer
            // if layer [i] is odd it is the opposing color
            var visibleWhiteThisLayer = 0UL
            var visibleBlackThisLayer = 0UL

            if (i.mod(2) == 0) {
              // even layer
              visibleWhiteThisLayer = whiteDVONNLayer[i] and blockedDvonnMask.inv()
              visibleBlackThisLayer = blackDVONNLayer[i] and blockedDvonnMask.inv()
            } else {
              // odd layer
              visibleWhiteThisLayer = blackDVONNLayer[i] and blockedDvonnMask.inv()
              visibleBlackThisLayer = whiteDVONNLayer[i] and blockedDvonnMask.inv()
            }

            // 2. Add these newly discovered visible pieces to our final surface bitboards
            activeWhiteDvonn = activeWhiteDvonn or visibleWhiteThisLayer

            // 3. Update the blocked mask for the next layer down
            blockedDvonnMask = blockedDvonnMask or visibleWhiteThisLayer or visibleBlackThisLayer
          }

          activeWhiteDvonn
        }

        PlayerName.WHITE -> {
          var activeBlackDvonn = 0UL
          var blockedDvonnMask = 0UL // Tracks all pieces we've seen so far from the top down

          // Iterate from the highest possible layer down to the board (Layer 0)
          for (i in whiteDVONNLayer.indices.reversed()) {
            // 1. Grab the pieces on this layer, but ONLY if they aren't blocked by a higher layer
            // if layer [i] is even it is the color of the base layer
            // if layer [i] is odd it is the opposing color
            var visibleWhiteThisLayer = 0UL
            var visibleBlackThisLayer = 0UL

            if (i.mod(2) == 0) {
              // even layer
              visibleWhiteThisLayer = whiteDVONNLayer[i] and blockedDvonnMask.inv()
              visibleBlackThisLayer = blackDVONNLayer[i] and blockedDvonnMask.inv()
            } else {
              // odd layer
              visibleWhiteThisLayer = blackDVONNLayer[i] and blockedDvonnMask.inv()
              visibleBlackThisLayer = whiteDVONNLayer[i] and blockedDvonnMask.inv()
            }

            // 2. Add these newly discovered visible pieces to our final surface bitboards
            activeBlackDvonn = activeBlackDvonn or visibleBlackThisLayer

            // 3. Update the blocked mask for the next layer down
            blockedDvonnMask = blockedDvonnMask or visibleWhiteThisLayer or visibleBlackThisLayer
          }

          activeBlackDvonn
        }
      }

  for (col in columnInfos) {
    val occupiedColumnSpots = col.columnMask and globalOccupancy
    val columnActiveDvonnPieces = col.columnMask and activeDvonnPieces
    val columnTargetDvonnPieces = col.columnMask and targetDvonnPieces

    if (columnActiveDvonnPieces == 0UL || columnTargetDvonnPieces == 0UL) continue

    val highBit = maxOf(columnActiveDvonnPieces, columnTargetDvonnPieces)
    val lowBit = minOf(columnActiveDvonnPieces, columnTargetDvonnPieces)

    // 2. Generate a mask of bits strictly between the high and low bits
    val pathMask = (highBit - 1UL) xor ((lowBit shl 1) - 1UL)

    // 3. Find intersecting occupied spots
    var occupiedIndices = 0UL
    var blockingPieces = occupiedColumnSpots and pathMask
    while (blockingPieces != 0UL) {
      // numberOfTrailingZeros gives you the index of the lowest set bit (e.g., 17)
      val bitIndex = blockingPieces.countTrailingZeroBits()
      occupiedIndices = occupiedIndices or (1UL shl bitIndex)

      // Clear the lowest set bit so we can find the next one (e.g., 24)
      blockingPieces = blockingPieces and (blockingPieces - 1UL)
    }

    if (
        (columnActiveDvonnPieces > 0UL) &&
            (columnTargetDvonnPieces > 0UL) &&
            occupiedIndices == 0UL // should be the same as OR right?
    ) {
      col.positions
          .filter {
            it and columnActiveDvonnPieces != 0UL // get position of yinch pieces
          }
          .forEach {
            val dvonnIndex = col.positions.indexOf(it)

            for (index in dvonnIndex.plus(1) until col.positions.size) {
              if ((col.positions[index] and targetDvonnPieces) > 0UL) {
                movesBuffer.add(
                    PossibleBitMove(
                        sourceBit = col.positions[dvonnIndex],
                        targetBit = col.positions[index],
                        pieceType = PieceType.DVONN,
                        pieceColor = player.name,
                        moveType = MoveType.UsePotential,
                    )
                )
              }
            }

            for (index in dvonnIndex.minus(1) downTo 0) {
              if ((col.positions[index] and targetDvonnPieces) > 0UL) {
                movesBuffer.add(
                    PossibleBitMove(
                        sourceBit = col.positions[dvonnIndex],
                        targetBit = col.positions[index],
                        pieceType = PieceType.DVONN,
                        pieceColor = player.name,
                        moveType = MoveType.UsePotential,
                    )
                )
              }
            }
          }
    }
  }
}

fun Bitboard.getPunctMoves(
    player: Player,
    columnInfos: List<ColumnInfo> = org.example.model.columnInfos,
    movesBuffer: MutableList<PossibleBitMove>,
) {

  val activePunctPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whitePUNCTLayer[0] and whitePotentials and whiteNeutralized.inv()
        }

        PlayerName.BLACK -> {
          blackPUNCTLayer[0] and blackPotentials and blackNeutralized.inv()
        }
      }

  if (activePunctPieces == 0UL) return // emptyList()

  val targetPunctPieces =
      when (player.name) {
        PlayerName.BLACK -> {
          var activeWhitePunct = 0UL
          var blockedPunctMask = 0UL

          for (i in whitePUNCTLayer.indices.reversed()) {
            // 1. Grab the pieces on this layer, but ONLY if they aren't blocked by a higher layer
            // if layer [i] is even it is the color of the base layer
            // if layer [i] is odd it is the opposing color
            var visibleWhiteThisLayer = 0UL
            var visibleBlackThisLayer = 0UL

            if (i.mod(2) == 0) {
              // even layer
              visibleWhiteThisLayer = whitePUNCTLayer[i] and blockedPunctMask.inv()
              visibleBlackThisLayer = blackPUNCTLayer[i] and blockedPunctMask.inv()
            } else {
              // odd layer
              visibleWhiteThisLayer = blackPUNCTLayer[i] and blockedPunctMask.inv()
              visibleBlackThisLayer = whitePUNCTLayer[i] and blockedPunctMask.inv()
            }

            // 2. Add these newly discovered visible pieces to our final surface bitboards
            activeWhitePunct = activeWhitePunct or visibleWhiteThisLayer

            // 3. Update the blocked mask for the next layer down
            blockedPunctMask = blockedPunctMask or visibleWhiteThisLayer or visibleBlackThisLayer
          }

          activeWhitePunct
        }

        PlayerName.WHITE -> {
          var activeBlackPunct = 0UL
          var blockedPunctMask = 0UL // Tracks all pieces we've seen so far from the top down

          // Iterate from the highest possible layer down to the board (Layer 0)
          for (i in whitePUNCTLayer.indices.reversed()) {
            // 1. Grab the pieces on this layer, but ONLY if they aren't blocked by a higher layer
            // if layer [i] is even it is the color of the base layer
            // if layer [i] is odd it is the opposing color
            var visibleWhiteThisLayer = 0UL
            var visibleBlackThisLayer = 0UL

            if (i.mod(2) == 0) {
              // even layer
              visibleWhiteThisLayer = whitePUNCTLayer[i] and blockedPunctMask.inv()
              visibleBlackThisLayer = blackPUNCTLayer[i] and blockedPunctMask.inv()
            } else {
              // odd layer
              visibleWhiteThisLayer = blackPUNCTLayer[i] and blockedPunctMask.inv()
              visibleBlackThisLayer = whitePUNCTLayer[i] and blockedPunctMask.inv()
            }

            // 2. Add these newly discovered visible pieces to our final surface bitboards
            activeBlackPunct = activeBlackPunct or visibleBlackThisLayer

            // 3. Update the blocked mask for the next layer down
            blockedPunctMask = blockedPunctMask or visibleWhiteThisLayer or visibleBlackThisLayer
          }

          activeBlackPunct
        }
      }

  for (col in columnInfos) {
    val occupiedColumnSpots = col.columnMask and globalOccupancy
    val columnActivePunctPieces = col.columnMask and activePunctPieces
    val columnTargetPunctPieces = col.columnMask and targetPunctPieces

    if (columnActivePunctPieces == 0UL || columnTargetPunctPieces == 0UL) continue

    val highBit = maxOf(columnActivePunctPieces, columnTargetPunctPieces)
    val lowBit = minOf(columnActivePunctPieces, columnTargetPunctPieces)

    // 2. Generate a mask of bits strictly between the high and low bits
    val pathMask = (highBit - 1UL) xor ((lowBit shl 1) - 1UL)

    // 3. Find intersecting occupied spots
    var occupiedIndices = 0UL
    var blockingPieces = occupiedColumnSpots and pathMask
    while (blockingPieces != 0UL) {
      // numberOfTrailingZeros gives you the index of the lowest set bit (e.g., 17)
      val bitIndex = blockingPieces.countTrailingZeroBits()
      occupiedIndices = occupiedIndices or (1UL shl bitIndex)

      // Clear the lowest set bit so we can find the next one (e.g., 24)
      blockingPieces = blockingPieces and (blockingPieces - 1UL)
    }

    if (
        (columnActivePunctPieces > 0UL) &&
            (columnTargetPunctPieces > 0UL) &&
            occupiedIndices == 0UL // should be the same as OR right?
    ) {
      col.positions
          .filter {
            it and columnActivePunctPieces != 0UL // get position of punct pieces
          }
          .forEach {
            val punctIndex = col.positions.indexOf(it)

            for (index in punctIndex.plus(1) until col.positions.size) {
              if ((col.positions[index] and targetPunctPieces) > 0UL) {
                movesBuffer.add(
                    PossibleBitMove(
                        sourceBit = col.positions[punctIndex],
                        targetBit = col.positions[index],
                        pieceType = PieceType.PUNCT,
                        pieceColor = player.name,
                        moveType = MoveType.UsePotential,
                    )
                )
              }
            }

            for (index in punctIndex.minus(1) downTo 0) {
              if ((col.positions[index] and targetPunctPieces) > 0UL) {
                movesBuffer.add(
                    PossibleBitMove(
                        sourceBit = col.positions[punctIndex],
                        targetBit = col.positions[index],
                        pieceType = PieceType.PUNCT,
                        pieceColor = player.name,
                        moveType = MoveType.UsePotential,
                    )
                )
              }
            }
          }
    }
  }
}

fun Bitboard.evaluateLinesForFourInARow(player: Player): List<ColumnInfo> {
  // --- 0. CONFIGURABLE DEBUGGING ---
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- EVALUATE LINES FOR FOUR IN A ROW CALLED ---") }
    logger.info { "" + ("Player: ${player.name}") }
    logger.info { "" + ("Bitboard State: ${Json.encodeToString<Bitboard>(this)}") }
  }

  val playerPieces =
      when (player.name) {
        PlayerName.WHITE -> whitePieces
        PlayerName.BLACK -> blackPieces
      }

  val columns =
      columnInfos
          .filter { it.positions.size >= 4 }
          .filterIndexed { index, column ->
            if (logger.isDebugEnabled()) {
              logger.info { "" + ("player Active Pieces: 0b${playerPieces.toString(2).padStart(40, '0')}") }
              logger.info { "" + ("Line $index Mask:         0b${column.columnMask.toString(2).padStart(40, '0')}") }
            }

            val result =
                column.submasks.any { submask ->
                  val fourInARow = (submask and playerPieces) == submask
                  if (logger.isDebugEnabled() && fourInARow) {
                    logger.info { "" + ("Sublist:              0b${submask.toString(2).padStart(40, '0')}") }
                    logger.info { "" + ("player Active Pieces: 0b${playerPieces.toString(2).padStart(40, '0')}") }
                    logger.info { "" + ("Result: $fourInARow") }
                  }
                  fourInARow
                }

            if (logger.isDebugEnabled()) {
              if (result) logger.info { "" + ("--- FOUND FOUR IN A ROW IN LINE $index ---") }
              else logger.info { "" + ("--- NO 4 IN A ROW IN LINE $index ---") }
            }

            //    if ((column.columnMask and playerPieces).countOneBits() < 4) {
            //      if (logger.isDebugEnabled()) {
            //        logger.info { "" + ("--- NO 4 IN A LINE $index ---") }
            //      }
            //      false
            //    } else {
            //      if (logger.isDebugEnabled()) {
            //        logger.info { "" + (//            "--- FOUND FOUR IN A LINE $index ---"
            //) } // \n0b${column.columnMask.toString(2).padStart(40, '0')}")
            //      }
            //
            //	    logger.info { "" + ("column.positions.windowed(4).any: ${
            //		    column.positions.windowed(4).any { sublist ->
            //			    val sum = sublist.fold(0UL) { acc, lng ->
            //				    acc or lng
            //			    }
            //			    (sum and playerPieces) == sum
            //		    }
            //			}") }

            result
          }

  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- EVALUATE LINES FOR FOUR IN A ROW COMPLETED ---") }
  }

  return columns
}

fun Bitboard.createPlayerPiecesWithPotentialPowerset(
    player: Player,
    columnInfos: List<ColumnInfo> = emptyList(),
    positions: List<ULong> = emptyList(),
): List<List<ULong>> {
  require(!(columnInfos.isEmpty() && positions.isEmpty())) { "There must be at least one column" }

  // TODO handle intersecting lines, but do i?

  val playerPiecesWithPotential =
      if (columnInfos.isNotEmpty()) {
        columnInfos.flatMap { column ->
          column.positions.filter { bitmask ->
            val result =
                when (player.name) {
                  PlayerName.WHITE -> {
                    // pieces with potential and not neutralized
                    (whitePotentials and bitmask) == bitmask &&
                        (whiteNeutralized.inv() and bitmask) == bitmask
                  }

                  PlayerName.BLACK -> {
                    (blackPotentials and bitmask) == bitmask &&
                        (blackNeutralized.inv() and bitmask) == bitmask
                  }
                }
            result
          }
        }
      } else {
        positions.filter { bitmask ->
          val result =
              when (player.name) {
                PlayerName.WHITE -> {
                  // pieces with potential and not neutralized
                  (whitePotentials and bitmask) == bitmask &&
                      (whiteNeutralized.inv() and bitmask) == bitmask
                }

                PlayerName.BLACK -> {
                  (blackPotentials and bitmask) == bitmask &&
                      (blackNeutralized.inv() and bitmask) == bitmask
                }
              }
          result
        }
      }

  /**
   * TODO causes stack overflow error, but an empty list is necessary as a player can leave the
   * stack in play TODO Minimax/MCTS — what it would be like to remove at least one of these pieces
   * if all pieces have potentials use line score heuristic and pieces in reserve TODO return of a
   * list containing different combinations of bit positions
   */
  return playerPiecesWithPotential.fold(initial = listOf(emptyList<ULong>())) { accumulator, item ->
    // For every item, take the current sublists (accumulator)
    // and add a new set of sublists where the item is appended
    accumulator +
        accumulator.map {
          // only add unique/distinct items
          if (!it.contains(item)) {
            (it + item)
          } else {
            it
          }
        }
  }
}

fun Bitboard.identifyAvailableMoves(
    currentPlayer: Player,
    columnInfos: List<ColumnInfo> = org.example.model.columnInfos,
    movesBuffer: MutableList<PossibleBitMove>,
) {
  movesBuffer.clear()

  val gipfPieceInReserve =
      currentPlayer.piecesInReserve
          .firstOrNull { piece -> piece.extractPieceType() == PieceType.GIPF }
          ?.extractPiece()

  val playableStackedPiecesInReserve: List<Piece> =
      currentPlayer.piecesInReserve
          .filter { piece ->
            piece.extractPieceType() != PieceType.GIPF && piece.extractPotential()
          }
          .distinctBy { piece -> piece.extractPieceType() }
          .mapNotNull { it.extractPiece() }

  if (gipfPieceInReserve != null) {
    vacantLines.forEach { columnInfo ->
      val start = columnInfo.positions.first() to columnInfo.pushDirections.first
      val end = columnInfo.positions.last() to columnInfo.pushDirections.second

      movesBuffer.add(
          PossibleBitMove(
              piece = gipfPieceInReserve,
              pieceType = PieceType.GIPF,
              pieceColor = currentPlayer.name,
              columnInfo = columnInfo,
              targetBit = start.first,
              pushDirection = start.second,
              moveType = MoveType.AddPiece,
          )
      )
      movesBuffer.add(
          PossibleBitMove(
              piece = gipfPieceInReserve,
              pieceType = PieceType.GIPF,
              pieceColor = currentPlayer.name,
              columnInfo = columnInfo,
              targetBit = end.first,
              pushDirection = end.second,
              moveType = MoveType.AddPiece,
          )
      )
    }
    return
  }

  // TODO PieceType.TAMSK logic is handled by isTamskPieceAtCenter()
  getTamskMoves(currentPlayer, movesBuffer)
  if (movesBuffer.isNotEmpty()) {
    //    require(movesBuffer.all { it.sourceBit != null }) {
    //      val invalidIndex = movesBuffer.indexOfFirst { it.sourceBit == null }
    //      "Move Buffer Contamination: Found uninitialized or invalid sourceBit at index
    // $invalidIndex " +
    //          "out of ${movesBuffer.size} total buffered moves. Buffer contents: $movesBuffer"
    //    }
    return
  }

  /**
   * TODO rewrite Check check(piece?.extractPotential() == false) { // val pieceCoords =
   * selectedNode.node.coordinate.let { "${it.column}${it.row}" } val currentPotential =
   * piece?.extractPotential()
   *
   * // "Invalid piece state at $pieceCoords: Expected piece potential to be spent (false), " // +
   * "but found potential status is: $currentPotential (Piece Type:
   * ${piece?.extractPieceType()?.name}, Color: ${piece?.extractPieceColor()})" }
   */
  playableStackedPiecesInReserve.forEach { piece ->
    vacantLines.forEach { columnInfo ->
      val start = columnInfo.positions.first() to columnInfo.pushDirections.first
      val end = columnInfo.positions.last() to columnInfo.pushDirections.second

      movesBuffer.add(
          PossibleBitMove(
              piece = piece,
              pieceType = piece.type,
              pieceColor = currentPlayer.name,
              columnInfo = columnInfo,
              targetBit = start.first,
              pushDirection = start.second,
              moveType = MoveType.AddPiece,
          )
      )

      movesBuffer.add(
          PossibleBitMove(
              piece = piece,
              pieceType = piece.type,
              pieceColor = currentPlayer.name,
              columnInfo = columnInfo,
              targetBit = end.first,
              pushDirection = end.second,
              moveType = MoveType.AddPiece,
          )
      )
    }
  }

  getZertzMoves(currentPlayer, columnInfos, movesBuffer)
  getYinshMoves(currentPlayer, columnInfos, movesBuffer)
  getDvonnMoves(currentPlayer, columnInfos, movesBuffer)
  getPunctMoves(currentPlayer, columnInfos, movesBuffer)
}

fun Bitboard.identifyPiecesToRemove(player: Player): List<PossibleBitMove> {
  val linesWithFourInARow = evaluateLinesForFourInARow(player)

  // TODO If one or more pieces (regardless of color)
  //  extend a row-of-4 without interruption, these pieces
  //  are considered to be part of the row and may also
  //  be removed.
  //  * see getDVONNmoves() for inverse
  //  * write tests

  // filter for fully occupied submasks and sum extensions
  // fullyPopulatedSubmasksPositions
  val fullyPopulatedPositions = linesWithFourInARow.flatMap { (_, positions, submasks, _, _, _) ->
    val occupiedBits =
        submasks
            .filter { submask -> (submask and globalOccupancy) == submask }
            .fold(0UL) { acc, mask ->
              acc or mask
            }

    val occupiedPositions = positions.filter { it and occupiedBits != 0UL }

    //        occupiedBits to occupiedPositions
    occupiedPositions
  }

  //  val fullyPopulatedPositions = fullyPopulatedSubmasksPositions.values.flatten()

  if (linesWithFourInARow.isNotEmpty()) {
    // TODO Ensure Rules are followed for stack piece retrieval
    val playerPiecesWithPotential =
        createPlayerPiecesWithPotentialPowerset(player, positions = fullyPopulatedPositions)

    val playerPiecesNotNeutralizedWithoutPotential = fullyPopulatedPositions.filter { bitmask ->
      val result =
          when (player.name) {
            PlayerName.WHITE -> {
              // TODO FIX - includes neutralized pieces
              // pieces without potential and not neutralized
              (whitePieces and bitmask) == bitmask &&
                  (whitePotentials.inv() and bitmask) == bitmask &&
                  (whiteNeutralized.inv() and bitmask) == bitmask &&
                  (blackNeutralized.inv() and bitmask) ==
                      bitmask // for white pieces stacked on neutralized black pieces
            }

            PlayerName.BLACK -> {
              (blackPieces and bitmask) == bitmask &&
                  (blackPotentials.inv() and bitmask) == bitmask &&
                  (blackNeutralized.inv() and bitmask) == bitmask &&
                  (whiteNeutralized.inv() and bitmask) ==
                      bitmask // for black pieces stacked on neutralized black pieces
            }
          }
      result
    }

    /**
     * TODO Note: An opponent’s stack of 2 potentials may also be left on the board. Create powerset
     * for this
     */
    val opponentPiecesAndNotNeutralized = fullyPopulatedPositions.filter { bitmask ->
      val result =
          when (player.name) {
            PlayerName.WHITE -> {
              // opponent pieces and not neutralized
              //	            (blackPotentials.inv() and bitmask) == bitmask &&
              (blackPieces and bitmask) == bitmask &&
                  (blackNeutralized.inv() and bitmask) == bitmask &&
                  (whiteNeutralized.inv() and bitmask) == bitmask
            }

            PlayerName.BLACK -> {
              //	            (whitePotentials.inv() and bitmask) == bitmask &&
              (whitePieces and bitmask) == bitmask &&
                  (whiteNeutralized.inv() and bitmask) == bitmask &&
                  (blackNeutralized.inv() and bitmask) == bitmask
            }
          }
      result
    }

    // stacked DVONN and PUNCT Pieces
    val neutralizedBitmasks =
        fullyPopulatedPositions
            .filter { bitmask ->
              (whiteNeutralized and bitmask) == bitmask || (blackNeutralized and bitmask) == bitmask
            }
            .distinct()

    // --- 1. PRE-CONDITION CHECKS ---
    if (neutralizedBitmasks.isNotEmpty()) {
      check(
          neutralizedBitmasks.any {
            !playerPiecesNotNeutralizedWithoutPotential.contains(it)
          }
      ) {
        "playerPiecesNotNeutralizedWithoutPotential should not contain any bits from neutralizedBitmasks"
      }

      check(
          neutralizedBitmasks.any {
            !opponentPiecesAndNotNeutralized.contains(it)
          }
      ) {
        "opponentPiecesAndNotNeutralized should not contain any bits from neutralizedBitmasks"
      }
    }

    if (
        playerPiecesNotNeutralizedWithoutPotential.isNotEmpty() &&
            playerPiecesWithPotential.isNotEmpty()
    ) {
      check(
          playerPiecesNotNeutralizedWithoutPotential.any { bitmask ->
            playerPiecesWithPotential.any { pieces ->
              !pieces.contains(bitmask)
            }
          }
      ) {
        "playerPiecesNotNeutralizedWithoutPotential should not contain any bits from playerPiecesWithPotential"
      }
    }

    val removePiecesPowerset =
        playerPiecesWithPotential
            .map { potentialPieces ->
              PossibleBitMove(
                  retrievedCapturedPiecesBit =
                      ((potentialPieces +
                                  playerPiecesNotNeutralizedWithoutPotential +
                                  opponentPiecesAndNotNeutralized)
                              .distinct()
                              .map { bitmask ->
                                RetrievedCapturedPieceBit(
                                    bitmask = bitmask,
                                    isNeutralized = false,
                                )
                              } +
                              neutralizedBitmasks.map { bitmask ->
                                RetrievedCapturedPieceBit(
                                    bitmask = bitmask,
                                    isNeutralized = true,
                                )
                              })
                          .also { it ->
                            val bitmaskOccurances = it.groupBy { it.bitmask }
                            check(bitmaskOccurances.all { it.value.size == 1 }) {
                              "$bitmaskOccurances"
                            }
                          },
                  moveType = MoveType.RetrieveCapturePieces,
              )
            }
            .filter { it.retrievedCapturedPiecesBit.isNotEmpty() }

    //    check(removePiecesPowerset.size == playerPiecesWithPotential.size)

    //    check(removePiecesPowerset.any { it.retrievedCapturedPiecesBit.isEmpty() }) {
    //      "Should not have an empty list of pieces to remove."
    //    }

    return removePiecesPowerset
  }

  return emptyList()
}

fun Bitboard.assertPieceCount(
    EXPECTED_TOTAL: Int = 66 / 2,
    MAXIMUM_PIECES: Int = 66,
    currentPlayer: Player,
    nextPlayer: Player,
) {

  //  currentPlayer.piecesInReserve.sortBy { it.extractPieceType() }
  //  nextPlayer.piecesInReserve.sortBy { it.extractPieceType() }

  // 1. Next Player's components
  var nextReservePotentials =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
      } * 2
  var nextReserveBasics =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.BLACK && !it.extractPotential()
      }
  var nextCapturedPotentials =
      nextPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
      } * 2
  var nextCapturedBasics =
      nextPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.BLACK && !it.extractPotential()
      }

  // 2. Current Player's components
  var currentReservePotentials =
      currentPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
      } * 2
  var currentReserveBasics =
      currentPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.BLACK && !it.extractPotential()
      }
  var currentCapturedPotentials =
      currentPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
      } * 2
  var currentCapturedBasics =
      currentPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.BLACK && !it.extractPotential()
      }

  // 3. Board components
  var boardBasics =
      blackGIPF.countOneBits() +
          blackZERTZ.countOneBits() +
          blackTAMSK.countOneBits() +
          blackYINSH.countOneBits()

  var boardStacks =
      blackDVONNLayer[0].countOneBits() +
          blackDVONNLayer[2].countOneBits() +
          blackDVONNLayer[4].countOneBits() +
          blackDVONNLayer[6].countOneBits() +
          blackPUNCTLayer[0].countOneBits() +
          blackPUNCTLayer[2].countOneBits() +
          blackPUNCTLayer[4].countOneBits() +
          blackPUNCTLayer[6].countOneBits() +
          whiteDVONNLayer[1].countOneBits() +
          whiteDVONNLayer[3].countOneBits() +
          whiteDVONNLayer[5].countOneBits() +
          whiteDVONNLayer[7].countOneBits() +
          whitePUNCTLayer[1].countOneBits() +
          whitePUNCTLayer[3].countOneBits() +
          whitePUNCTLayer[5].countOneBits() +
          whitePUNCTLayer[7].countOneBits()

  var boardPotentials = blackPotentials.countOneBits()

  val totalBlackPieces =
      nextReservePotentials +
          nextReserveBasics +
          nextCapturedPotentials +
          nextCapturedBasics +
          currentReservePotentials +
          currentReserveBasics +
          currentCapturedPotentials +
          currentCapturedBasics +
          boardPotentials +
          boardBasics +
          boardStacks

  if (totalBlackPieces > EXPECTED_TOTAL) {
    logger.info { "" + ("oooooooooooooo") }
  }

  check(totalBlackPieces == EXPECTED_TOTAL) {
    val nextReserveRaw =
        nextPlayer.piecesInReserve.count {
          it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
        }
    val nextCapturedRaw =
        nextPlayer.capturedPieces.count {
          it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
        }
    val currentReserveRaw =
        currentPlayer.piecesInReserve.count {
          it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
        }
    val currentCapturedRaw =
        currentPlayer.capturedPieces.count {
          it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
        }

    """
    CRITICAL STATE CORRUPTION: Total Black piece weight ($totalBlackPieces) != Expected ($EXPECTED_TOTAL)
    
    1. NEXT PLAYER
       ├── Reserve (Black pieces held)
       │   ├── Potentials: $nextReserveRaw (weighted: $nextReservePotentials)
       │   └── Basics:     $nextReserveBasics
       └── Captured (By Next Player)
           ├── Potentials: $nextCapturedRaw (weighted: $nextCapturedPotentials)
           └── Basics:     $nextCapturedBasics
           
    2. CURRENT PLAYER
       ├── Reserve (Black pieces held)
       │   ├── Potentials: $currentReserveRaw (weighted: $currentReservePotentials)
       │   └── Basics:     $currentReserveBasics
       └── Captured (By Current Player)
           ├── Potentials: $currentCapturedRaw (weighted: $currentCapturedPotentials)
           └── Basics:     $currentCapturedBasics
           
    3. ACTIVE BOARD
       ├── Potentials (Weighted): $boardPotentials
       ├── Flat Basics (Single pieces on board)
       │   ├── GIPF:  ${blackGIPF.countOneBits()}
       │   ├── ZERTZ: ${blackZERTZ.countOneBits()}
       │   ├── TAMSK: ${blackTAMSK.countOneBits()}
       │   └── YINSH: ${blackYINSH.countOneBits()}
       └── Stacks (Layered/hidden pieces)
           ├── DVONN (Black layers 0,2,4): ${blackDVONNLayer[0].countOneBits() + blackDVONNLayer[2].countOneBits() + blackDVONNLayer[4].countOneBits()}
           ├── DVONN (White layers 1,3,5): ${whiteDVONNLayer[1].countOneBits() + whiteDVONNLayer[3].countOneBits() + whiteDVONNLayer[5].countOneBits()}
           ├── PUNCT (Black layers 0,2,4): ${blackPUNCTLayer[0].countOneBits() + blackPUNCTLayer[2].countOneBits() + blackPUNCTLayer[4].countOneBits()}
           └── PUNCT (White layers 1,3,5): ${whitePUNCTLayer[1].countOneBits() + whitePUNCTLayer[3].countOneBits() + whitePUNCTLayer[5].countOneBits()}
           
    SUMMARY EVALUATION:
    - Player Total Weight: ${nextReservePotentials + nextReserveBasics + nextCapturedPotentials + nextCapturedBasics + currentReservePotentials + currentReserveBasics + currentCapturedPotentials + currentCapturedBasics}
    - Board Total Weight:  ${boardPotentials + boardBasics + boardStacks}
    - Bitboard State: ${Json.encodeToString<Bitboard>(this)}
    - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.WHITE) currentPlayer else nextPlayer)}
    - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.BLACK) currentPlayer else nextPlayer)}

    ======================================================================
    """
        .trimIndent()
  }

  // 1. Next Player's components
  nextReservePotentials =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
      } * 2
  nextReserveBasics =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.WHITE && !it.extractPotential()
      }
  nextCapturedPotentials =
      nextPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
      } * 2
  nextCapturedBasics =
      nextPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.WHITE && !it.extractPotential()
      }

  // 2. Current Player's components
  currentReservePotentials =
      currentPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
      } * 2
  currentReserveBasics =
      currentPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.WHITE && !it.extractPotential()
      }
  currentCapturedPotentials =
      currentPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
      } * 2
  currentCapturedBasics =
      currentPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.WHITE && !it.extractPotential()
      }

  // 3. Board components
  boardBasics =
      whiteGIPF.countOneBits() +
          whiteZERTZ.countOneBits() +
          whiteTAMSK.countOneBits() +
          whiteYINSH.countOneBits()

  boardStacks =
      whiteDVONNLayer[0].countOneBits() +
          whiteDVONNLayer[2].countOneBits() +
          whiteDVONNLayer[4].countOneBits() +
          whiteDVONNLayer[6].countOneBits() +
          whitePUNCTLayer[0].countOneBits() +
          whitePUNCTLayer[2].countOneBits() +
          whitePUNCTLayer[4].countOneBits() +
          whitePUNCTLayer[6].countOneBits() +
          blackDVONNLayer[1].countOneBits() +
          blackDVONNLayer[3].countOneBits() +
          blackDVONNLayer[5].countOneBits() +
          blackDVONNLayer[7].countOneBits() +
          blackPUNCTLayer[1].countOneBits() +
          blackPUNCTLayer[3].countOneBits() +
          blackPUNCTLayer[5].countOneBits() +
          blackPUNCTLayer[7].countOneBits()

  boardPotentials = whitePotentials.countOneBits()

  val totalWhitePieces =
      nextReservePotentials +
          nextReserveBasics +
          nextCapturedPotentials +
          nextCapturedBasics +
          currentReservePotentials +
          currentReserveBasics +
          currentCapturedPotentials +
          currentCapturedBasics +
          boardPotentials +
          boardBasics +
          boardStacks

  if (totalWhitePieces > EXPECTED_TOTAL) {
    logger.info { "" + ("oooooooooooooo") }
  }

  check(totalWhitePieces == EXPECTED_TOTAL) {
    val nextReserveRaw =
        nextPlayer.piecesInReserve.count {
          it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
        }
    val nextCapturedRaw =
        nextPlayer.capturedPieces.count {
          it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
        }
    val currentReserveRaw =
        currentPlayer.piecesInReserve.count {
          it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
        }
    val currentCapturedRaw =
        currentPlayer.capturedPieces.count {
          it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
        }

    """
      CRITICAL STATE CORRUPTION: Total White piece weight ($totalWhitePieces) != Expected ($EXPECTED_TOTAL)
      
      1. NEXT PLAYER (WHITE)
         ├── Reserve
         │   ├── Potentials: $nextReserveRaw (weighted: $nextReservePotentials)
         │   └── Basics:     $nextReserveBasics
         └── Captured (By Next Player)
             ├── Potentials: $nextCapturedRaw (weighted: $nextCapturedPotentials)
             └── Basics:     $nextCapturedBasics
             
      2. CURRENT PLAYER (BLACK/OTHER)
         ├── Reserve (White pieces held)
         │   ├── Potentials: $currentReserveRaw (weighted: $currentReservePotentials)
         │   └── Basics:     $currentReserveBasics
         └── Captured (White pieces captured)
             ├── Potentials: $currentCapturedRaw (weighted: $currentCapturedPotentials)
             └── Basics:     $currentCapturedBasics
             
      3. ACTIVE BOARD
         ├── Potentials (Weighted): $boardPotentials
         ├── Flat Basics (Single pieces on board)
         │   ├── GIPF:  ${whiteGIPF.countOneBits()}
         │   ├── ZERTZ: ${whiteZERTZ.countOneBits()}
         │   ├── TAMSK: ${whiteTAMSK.countOneBits()}
         │   └── YINSH: ${whiteYINSH.countOneBits()}
         └── Stacks (Layered/hidden pieces)
             ├── DVONN (White layers 0,2,4): ${whiteDVONNLayer[0].countOneBits() + whiteDVONNLayer[2].countOneBits() + whiteDVONNLayer[4].countOneBits()}
             ├── DVONN (Black layers 1,3,5): ${blackDVONNLayer[1].countOneBits() + blackDVONNLayer[3].countOneBits() + blackDVONNLayer[5].countOneBits()}
             ├── PUNCT (White layers 0,2,4): ${whitePUNCTLayer[0].countOneBits() + whitePUNCTLayer[2].countOneBits() + whitePUNCTLayer[4].countOneBits()}
             └── PUNCT (Black layers 1,3,5): ${blackPUNCTLayer[1].countOneBits() + blackPUNCTLayer[3].countOneBits() + blackPUNCTLayer[5].countOneBits()}
             
      SUMMARY EVALUATION:
      - Player Total Weight: ${nextReservePotentials + nextReserveBasics + nextCapturedPotentials + nextCapturedBasics + currentReservePotentials + currentReserveBasics + currentCapturedPotentials + currentCapturedBasics}
      - Board Total Weight:  ${boardPotentials + boardBasics + boardStacks}
      - Bitboard State: ${Json.encodeToString<Bitboard>(this)}
      - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.WHITE) currentPlayer else nextPlayer)}
      - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.BLACK) currentPlayer else nextPlayer)}

      ======================================================================
      """
        .trimIndent()
  }

  val totalPieces = totalBlackPieces + totalWhitePieces
  check(totalPieces == MAXIMUM_PIECES) {
    "Game Piece Desynchronization: Total pieces in play ($totalPieces) exceeds the maximum piece count ($MAXIMUM_PIECES). " +
        "Pieces have been illegally spawned or deleted." +
        "\nGame State: \n${Json.encodeToString(this)}"
  }
}

fun Bitboard.validatePieceRemoval(removedPieces: List<RetrievedCapturedPieceBit>) {
  // Does not handle stack pieces
  fun safeRemoveAndCheck(board: ULong, boardName: String, bitmask: ULong, piece: Piece): Boolean {
    return (board and bitmask) == 0UL
  }

  val map = removedPieces.associateWith { removedPiece ->
    val bitmask = removedPiece.bitmask
    val piece = removedPiece.retrievedPiece ?: removedPiece.capturedPiece

    val hasBeenRemoved =
        when (piece?.colorName) {
          PlayerName.WHITE -> {
            when (piece.type) {
              PieceType.NULL -> {
                true
              }
              PieceType.GIPF -> {
                safeRemoveAndCheck(whiteGIPF, "White GIPF", bitmask, piece)
              }

              PieceType.TAMSK -> {
                safeRemoveAndCheck(whiteTAMSK, "White TAMSK", bitmask, piece)
              }

              PieceType.ZERTZ -> {
                safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", bitmask, piece)
              }

              PieceType.DVONN -> {
                if (piece.stackedPieces.isNotEmpty()) {
                  throw IllegalStateException("")
                } else {
                  safeRemoveAndCheck(whiteDVONNLayer[0], "White DVONN [0]", bitmask, piece)
                }
              }

              PieceType.YINSH -> {
                safeRemoveAndCheck(whiteYINSH, "White YINSH", bitmask, piece)
              }

              PieceType.PUNCT -> {
                if (piece.stackedPieces.isNotEmpty()) {
                  throw IllegalStateException("")
                } else {
                  safeRemoveAndCheck(whitePUNCTLayer[0], "White PUNCT [0]", bitmask, piece)
                }
              }
            }
          }
          PlayerName.BLACK -> {
            when (piece.type) {
              PieceType.NULL -> {
                true
              }
              PieceType.GIPF -> {
                safeRemoveAndCheck(blackGIPF, "Black GIPF", bitmask, piece)
              }

              PieceType.TAMSK -> {
                safeRemoveAndCheck(blackTAMSK, "Black TAMSK", bitmask, piece)
              }

              PieceType.ZERTZ -> {
                safeRemoveAndCheck(blackZERTZ, "Black ZERTZ", bitmask, piece)
              }

              PieceType.DVONN -> {
                if (piece.stackedPieces.isNotEmpty()) {
                  throw IllegalStateException("")
                } else {
                  safeRemoveAndCheck(blackDVONNLayer[0], "Black DVONN [0]", bitmask, piece)
                }
              }

              PieceType.YINSH -> {
                safeRemoveAndCheck(blackYINSH, "Black YINSH", bitmask, piece)
              }

              PieceType.PUNCT -> {
                if (piece.stackedPieces.isNotEmpty()) {
                  throw IllegalStateException("")
                } else {
                  safeRemoveAndCheck(blackPUNCTLayer[0], "Black PUNCT [0]", bitmask, piece)
                }
              }
            }
          }
          else -> false
        }

    hasBeenRemoved
  }

  check(map.all { it.value }) {
    buildString {
      map.filter { !it.value }
          .forEach { (bit, _) ->
            appendLine("$bit was not removed")
          }
    }
  }
}

fun Bitboard.validateBoardConversion(convertedBoard: Board) {}
