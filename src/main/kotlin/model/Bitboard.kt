@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import kotlin.collections.forEachIndexed
import kotlin.collections.map
import kotlin.io.println
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.engine.MoveType
import org.example.engine.PossibleBitMove
import kotlin.collections.windowed

// a line is full if it is equal to all bits being 1 else it is has at least 1 0 bit

val boardCenterSpotMask = 0b0000000000000000000001000000000000000000.toULong()

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

val openningLineIndexArray =
    listOf(
        0,
        1,
        2,
        3,
        4,
        8,
        9,
        14,
        15,
        21,
        22,
        27,
        28,
        32,
        33,
        36,
        37,
        38,
        39,
    )

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
              (0 until arr.size).map { k ->
                1UL shl arr[k]
              },
		      submasks =
			      (0 until arr.size).map { k ->
				      1UL shl arr[k]
			      }.windowed(4).map { sublist ->
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
                  (0 until arr.size).map { k ->
                    1UL shl arr[k]
                  },
	          submasks =
		          (0 until arr.size).map { k ->
			          1UL shl arr[k]
		          }.windowed(4).map { sublist ->
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
                  (0 until arr.size).map { k ->
                    1UL shl arr[k]
                  },
	          submasks =
		          (0 until arr.size).map { k ->
			          1UL shl arr[k]
		          }.windowed(4).map { sublist ->
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

@OptIn(ExperimentalUnsignedTypes::class)
@Serializable
data class Bitboard(
    var whiteGIPF: ULong = 0UL,
    //
    /**
     * even indices are player pieces, odd layer are opponent pieces — 0 - white, 1 - black, 2 -
     * white, 3 - black, 4 - white, 5 = black
     */
    var whiteDVONNLayer: ULongArray = ULongArray(6),
    var whitePUNCTLayer: ULongArray = ULongArray(6),
    var whiteTAMSK: ULong = 0UL,
    var whiteYINSH: ULong = 0UL,
    var whiteZERTZ: ULong = 0UL,
    var whitePotentials: ULong = 0UL,
    var blackGIPF: ULong = 0UL,
    /**
     * even indices are player pieces, odd layer are opponent pieces — 0 - black, 1 - white, 2 -
     * black, 3 - white, 4 - black, 5 = white
     */
    var blackDVONNLayer: ULongArray = ULongArray(6),
    var blackPUNCTLayer: ULongArray = ULongArray(6),
    var blackTAMSK: ULong = 0UL,
    var blackYINSH: ULong = 0UL,
    var blackZERTZ: ULong = 0UL,
    var blackPotentials: ULong = 0UL,
) {
  fun deepCopy(): Bitboard {
    // TODO Optimize
    val string = Json.encodeToString(serializer(), this)
    return Json.decodeFromString(serializer(), string)
  }

  fun diff(oldBitboard: Bitboard): Bitboard {
    // --- 0. CONFIGURABLE DEBUGGING ---
    val isDebugEnabled = false
    if (isDebugEnabled) {
      println("--- BITBOARD DIFF CALLED ---")
      println("Original Bitboard State: ${Json.encodeToString(oldBitboard) }}")
      println("Current Bitboard State: ${Json.encodeToString(this) }}")
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
    val diffBoard = Bitboard(
      whiteGIPF = this.whiteGIPF xor oldBitboard.whiteGIPF,
      whiteDVONNLayer = diffLayers(this.whiteDVONNLayer, oldBitboard.whiteDVONNLayer, "White DVONN"),
      whitePUNCTLayer = diffLayers(this.whitePUNCTLayer, oldBitboard.whitePUNCTLayer, "White PUNCT"),
      whiteTAMSK = this.whiteTAMSK xor oldBitboard.whiteTAMSK,
      whiteYINSH = this.whiteYINSH xor oldBitboard.whiteYINSH,
      whiteZERTZ = this.whiteZERTZ xor oldBitboard.whiteZERTZ,
      whitePotentials = this.whitePotentials xor oldBitboard.whitePotentials, // FIXED TYPO

      blackGIPF = this.blackGIPF xor oldBitboard.blackGIPF,
      blackDVONNLayer = diffLayers(this.blackDVONNLayer, oldBitboard.blackDVONNLayer, "Black DVONN"),
      blackPUNCTLayer = diffLayers(this.blackPUNCTLayer, oldBitboard.blackPUNCTLayer, "Black PUNCT"),
      blackTAMSK = this.blackTAMSK xor oldBitboard.blackTAMSK,
      blackYINSH = this.blackYINSH xor oldBitboard.blackYINSH,
      blackZERTZ = this.blackZERTZ xor oldBitboard.blackZERTZ,
      blackPotentials = this.blackPotentials xor oldBitboard.blackPotentials, // FIXED TYPO
    )

    if (isDebugEnabled) {
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

      println("Total changes: $totalChanges")
      println("Diff complete. Total bits flipped across calculated layers.")
      println("Diff Bitboard State: ${Json.encodeToString(oldBitboard) }}")
      println("--- BITBOARD DIFF COMPLETED ---")
    }

	  check(this.globalOccupancy == oldBitboard.globalOccupancy) {
		  buildString {
			  appendLine("CRITICAL STATE VALIDATION FAILED: The current bitboard does not perfectly match the initial bitboard.")
			  appendLine("--- STATE DESYNC REPORT ---")

			  // Local helper to cleanly format the mismatched bitmasks
			  fun logDiff(boardName: String, diffBits: ULong) {
				  if (diffBits != 0UL) {
					  val count = diffBits.countOneBits()
					  appendLine("  -> [$boardName] Desync: $count bit(s) mismatched. Diff Mask: 0b${diffBits.toString(2).padStart(40, '0')}")
				  }
			  }

			  // White Boards
			  logDiff("White GIPF", diffBoard.whiteGIPF)
			  logDiff("White TAMSK", diffBoard.whiteTAMSK)
			  logDiff("White YINSH", diffBoard.whiteYINSH)
			  logDiff("White ZERTZ", diffBoard.whiteZERTZ)
			  logDiff("White Potentials", diffBoard.whitePotentials)
			  diffBoard.whiteDVONNLayer.forEachIndexed { i, layer -> logDiff("White DVONN Layer $i", layer) }
			  diffBoard.whitePUNCTLayer.forEachIndexed { i, layer -> logDiff("White PUNCT Layer $i", layer) }

			  // Black Boards
			  logDiff("Black GIPF", diffBoard.blackGIPF)
			  logDiff("Black TAMSK", diffBoard.blackTAMSK)
			  logDiff("Black YINSH", diffBoard.blackYINSH)
			  logDiff("Black ZERTZ", diffBoard.blackZERTZ)
			  logDiff("Black Potentials", diffBoard.blackPotentials)
			  diffBoard.blackDVONNLayer.forEachIndexed { i, layer -> logDiff("Black DVONN Layer $i", layer) }
			  diffBoard.blackPUNCTLayer.forEachIndexed { i, layer -> logDiff("Black PUNCT Layer $i", layer) }

			  appendLine("---------------------------")
			  appendLine("Hint: A '1' in the Diff Mask indicates a piece that exists on one board but is missing from the other.")
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

      whiteDVONNLayer.slice(1..5).forEach { neutralized = neutralized or it }
      whitePUNCTLayer.slice(1..5).forEach { neutralized = neutralized or it }

      return neutralized
    }

  val blackNeutralized: ULong
    get() {
      var neutralized = 0UL

      blackDVONNLayer.slice(1..5).forEach { neutralized = neutralized or it }
      blackPUNCTLayer.slice(1..5).forEach { neutralized = neutralized or it }

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
    result = 31 * result + blackPieces.hashCode()
    result = 31 * result + whiteGIPF.hashCode()
    result = 31 * result + whiteDVONNLayer.contentHashCode()
    result = 31 * result + whitePUNCTLayer.contentHashCode()
    result = 31 * result + whiteTAMSK.hashCode()
    result = 31 * result + whiteYINSH.hashCode()
    result = 31 * result + whiteZERTZ.hashCode()
    result = 31 * result + whitePotentials.hashCode()
    result = 31 * result + whiteNeutralized.hashCode()
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
  val isDebugEnabled = false // Toggle for detailed trace logs
  if (isDebugEnabled) {
    println("--- EXECUTE PUSH UP CALLED ---")
    println("Column: $col")
  }

  require(col.shiftPairs.isNotEmpty()) {
    "CRITICAL ERROR: Column shiftPairs cannot be empty."
  }

  val occupiedSpots = globalOccupancy
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()
  var gapFound = false
  var vacantBitFound = 0UL

  // --- 1. DISCOVERY: Find out exactly which pieces are shifting ---
  if (isDebugEnabled) println("Starting discovery phase for Push Up...")

  for ((fromMask, toMask) in col.shiftPairs) {
    // Strict bit validation: Ensure masks aren't corrupted
    check(fromMask.countOneBits() == 1 && toMask.countOneBits() == 1) {
      "STATE ERROR: Shift masks must contain exactly one bit. fromMask: $fromMask, toMask: $toMask"
    }

    if ((occupiedSpots and fromMask) != 0UL) {
      movesToApply.add(fromMask to toMask)
      if (isDebugEnabled) println("  -> Piece found at $fromMask. Queuing shift to $toMask.")
    } else {
      gapFound = true
      vacantBitFound = fromMask
      if (isDebugEnabled) println("  -> Gap discovered at $fromMask. Ending discovery phase.")
      break
    }

    if ((occupiedSpots and toMask) == 0UL) {
      gapFound = true
      vacantBitFound = toMask
      if (isDebugEnabled)
          println("  -> Gap discovered at ${toMask.toString(2)}. Ending discovery phase.")
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
  if (isDebugEnabled) {
    println("Applying ${movesToApply.size} shifts in reverse order to prevent overwrite...")
  }

  movesToApply.asReversed().forEach { (fromMask, toMask) ->
    if (isDebugEnabled) println("  -> Shifting piece from $fromMask to $toMask.")
    if (isDebugEnabled) println("  -> board before shifting: 0b${globalOccupancy.toString(2)}")
    applyShiftToAll(fromMask, toMask)
    if (isDebugEnabled) println("  -> board after shifting:  0b${globalOccupancy.toString(2)}")
  }

  if (isDebugEnabled) println("--- PUSH UP COMPLETE. Returning vacant bit: $vacantBitFound ---")
  return vacantBitFound
}

fun Bitboard.executePushDown(col: ColumnInfo): ULong {
  // --- 0. CONFIGURABLE DEBUGGING ---
  val isDebugEnabled = false
  if (isDebugEnabled) {
    println("--- EXECUTE PUSH DOWN CALLED ---")
    println("Column: $col")
  }

  require(col.shiftPairs.isNotEmpty()) {
    "CRITICAL ERROR: Column shiftPairs cannot be empty."
  }

  val occupiedSpots = globalOccupancy
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()
  var gapFound = false
  var vacantBitFound = 0UL

  // --- 1. DISCOVERY: Find out exactly which pieces are shifting ---
  if (isDebugEnabled) println("Starting discovery phase for Push Down...")

  for ((toMask, fromMask) in col.shiftPairs.asReversed()) {
    // Strict bit validation: Ensure masks aren't corrupted
    check(fromMask.countOneBits() == 1 && toMask.countOneBits() == 1) {
      "STATE ERROR: Shift masks must contain exactly one bit. fromMask: $fromMask, toMask: $toMask"
    }

    if ((occupiedSpots and fromMask) != 0UL) {
      movesToApply.add(fromMask to toMask)
      if (isDebugEnabled)
          println(
              "  -> Piece found at ${fromMask.toString(2)}. Queuing shift to ${toMask.toString(2)}."
          )
    } else {
      gapFound = true
      vacantBitFound = fromMask
      if (isDebugEnabled)
          println("  -> Gap discovered at ${fromMask.toString(2)}. Ending discovery phase.")
      break
    }

    if ((occupiedSpots and toMask) == 0UL) {
      gapFound = true
      vacantBitFound = toMask
      if (isDebugEnabled)
          println("  -> Gap discovered at ${toMask.toString(2)}. Ending discovery phase.")
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
  if (isDebugEnabled) {
    println("Applying ${movesToApply.size} shifts in reverse order to prevent overwrite...")
  }

  movesToApply.asReversed().forEach { (fromMask, toMask) ->
    if (isDebugEnabled) println("  -> Shifting piece from $fromMask to $toMask.")
    if (isDebugEnabled) println("  -> board before shifting: 0b${globalOccupancy.toString(2)}")
    applyShiftToAll(fromMask, toMask)
    if (isDebugEnabled) println("  -> board after shifting:  0b${globalOccupancy.toString(2)}")
  }

  if (isDebugEnabled) println("--- PUSH DOWN COMPLETE. Returning vacant bit: $vacantBitFound ---")
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

  if (whiteGIPF == 9UL && blackGIPF == 4UL) {
    println("bit move found")
  }
}

// endregion

fun convertBoardToBitboard(board: Board): Bitboard {
  var whiteGIPF: ULong = 0UL
  val whiteDVONNLayer: ULongArray = ULongArray(6)
  val whitePUNCTLayer: ULongArray = ULongArray(6)
  var whiteTAMSK: ULong = 0UL
  var whiteYINCH: ULong = 0UL
  var whiteZERTZ: ULong = 0UL
  var whitePotentials: ULong = 0UL

  var blackGIPF: ULong = 0UL
  val blackDVONNLayer: ULongArray = ULongArray(6)
  val blackPUNCTLayer: ULongArray = ULongArray(6)
  var blackTAMSK: ULong = 0UL
  var blackYINCH: ULong = 0UL
  var blackZERTZ: ULong = 0UL
  var blackPotentials: ULong = 0UL

  board.nodes
      .filter { node -> node.isSpot }
      .forEach { node: Node ->
        node.piece?.let { piece ->
          when (piece.colorName) {
            PlayerName.WHITE.name -> {
              when (piece.type) {
                PieceType.GIPF -> {
                  whiteGIPF = whiteGIPF or node.bitmask
                }

                PieceType.TAMSK -> {
                  whiteTAMSK = whiteTAMSK or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask
                }

                PieceType.ZERTZ -> {
                  whiteZERTZ = whiteZERTZ or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask
                }

                PieceType.YINSH -> {
                  whiteYINCH = whiteYINCH or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask
                }

                PieceType.DVONN -> {
                  whiteDVONNLayer[0] = whiteDVONNLayer[0] or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask

                  // stackedPieces start on layer[1] (the 2nd Dvonn layer)
                  piece.stackedPieces.forEachIndexed { stackIndex, piece ->
                    val layerIndex = stackIndex + 1
                    /* white pieces on even layers */
                    if (layerIndex % 2 == 0) {
                      check(piece.colorName == PlayerName.WHITE.name) { "" }
                    }
                    /* black pieces on odd layers */
                    if (layerIndex % 2 == 1) {
                      check(piece.colorName == PlayerName.BLACK.name) { "" }
                    }

                    whiteDVONNLayer[layerIndex] = node.bitmask
                  }
                }

                PieceType.PUNCT -> {
                  whitePUNCTLayer[0] = whitePUNCTLayer[0] or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask

                  // stackedPieces start on layer[1] (the 2nd Dvonn layer)
                  piece.stackedPieces.forEachIndexed { stackIndex, piece ->
                    val layerIndex = stackIndex + 1
                    /* white pieces on even layers */
                    if (layerIndex % 2 == 0) {
                      check(piece.colorName == PlayerName.WHITE.name) { "" }
                    }
                    /* black pieces on odd layers */
                    if (layerIndex % 2 == 1) {
                      check(piece.colorName == PlayerName.BLACK.name) { "" }
                    }

                    whitePUNCTLayer[layerIndex] = node.bitmask
                  }
                }
              }
            }

            PlayerName.BLACK.name -> {
              when (piece.type) {
                PieceType.GIPF -> {
                  blackGIPF = blackGIPF or node.bitmask
                }

                PieceType.TAMSK -> {
                  blackTAMSK = blackTAMSK or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask
                }

                PieceType.ZERTZ -> {
                  blackZERTZ = blackZERTZ or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask
                }

                PieceType.YINSH -> {
                  blackYINCH = blackYINCH or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask
                }

                PieceType.DVONN -> {
                  blackDVONNLayer[0] = blackDVONNLayer[0] or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask

                  // stackedPieces start on layer[1] (the 2nd Dvonn layer)
                  piece.stackedPieces.forEachIndexed { stackIndex, piece ->
                    val layerIndex = stackIndex + 1
                    /* black pieces on even layers */
                    if (layerIndex % 2 == 0) {
                      check(piece.colorName == PlayerName.BLACK.name) { "" }
                    }
                    /* white pieces on odd layers */
                    if (layerIndex % 2 == 1) {
                      check(piece.colorName == PlayerName.WHITE.name) { "" }
                    }

                    blackDVONNLayer[layerIndex] = node.bitmask
                  }
                }

                PieceType.PUNCT -> {
                  blackPUNCTLayer[0] = blackPUNCTLayer[0] or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask

                  // stackedPieces start on layer[1] (the 2nd Dvonn layer)
                  piece.stackedPieces.forEachIndexed { stackIndex, piece ->
                    val layerIndex = stackIndex + 1
                    /* black pieces on even layers */
                    if (layerIndex % 2 == 0) {
                      check(piece.colorName == PlayerName.BLACK.name) { "" }
                    }
                    /* black pieces on odd layers */
                    if (layerIndex % 2 == 1) {
                      check(piece.colorName == PlayerName.WHITE.name) { "" }
                    }

                    blackPUNCTLayer[layerIndex] = node.bitmask
                  }
                }
              }
            }
          }
        }
      }

  return Bitboard(
      whiteGIPF = whiteGIPF,
      whiteTAMSK = whiteTAMSK,
      whiteZERTZ = whiteZERTZ,
      whiteYINSH = whiteYINCH,
      whiteDVONNLayer = whiteDVONNLayer,
      whitePUNCTLayer = whitePUNCTLayer,
      whitePotentials = whitePotentials,
      blackGIPF = blackGIPF,
      blackTAMSK = blackTAMSK,
      blackYINSH = blackYINCH,
      blackZERTZ = blackZERTZ,
      blackDVONNLayer = blackDVONNLayer,
      blackPUNCTLayer = blackPUNCTLayer,
      blackPotentials = blackPotentials,
  )
}

fun Bitboard.convertBitboardToBoard(board: Board): Board {
  val nodes = mutableListOf<Node>()

  val newBoard = board.deepCopy()

  for (bit in 0..39) {
    val bitmask = 1UL shl bit
    if ((globalOccupancy and bitmask) == 0UL) {
      // TODO does this update in place
      newBoard.nodes.first { it.bitmask == bitmask }.piece == null
      continue
    }

    // construct piece and place it on the matching node
    val color =
        if ((whitePieces and bitmask) == bitmask) {
          PlayerName.WHITE
        } else {
          //			if ((blackPieces and bitmask) == bitmask)
          PlayerName.BLACK
        }

    val isNeutralized =
        when (color) {
          PlayerName.WHITE -> {
            (whiteNeutralized and bitmask) == bitmask
          }

          PlayerName.BLACK -> {
            (blackNeutralized and bitmask) == bitmask
          }
        }

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
        when (color) {
          PlayerName.WHITE -> {
            if ((whiteGIPF and bitmask) == bitmask) PieceType.GIPF
            else if ((whiteTAMSK and bitmask) == bitmask) PieceType.TAMSK
            else if ((whiteZERTZ and bitmask) == bitmask) PieceType.ZERTZ
            else if ((whiteYINSH and bitmask) == bitmask) PieceType.YINSH
            else if ((whiteDVONNLayer[0] and bitmask) == bitmask) PieceType.DVONN
            else if ((whitePUNCTLayer[0] and bitmask) == bitmask) PieceType.PUNCT else null
          }

          PlayerName.BLACK -> {
            if ((blackGIPF and bitmask) == bitmask) PieceType.GIPF
            else if ((blackTAMSK and bitmask) == bitmask) PieceType.TAMSK
            else if ((blackZERTZ and bitmask) == bitmask) PieceType.ZERTZ
            else if ((blackYINSH and bitmask) == bitmask) PieceType.YINSH
            else if ((blackDVONNLayer[0] and bitmask) == bitmask) PieceType.DVONN
            else if ((blackPUNCTLayer[0] and bitmask) == bitmask) PieceType.PUNCT else null
          }
        }

    val stackedPieces: MutableList<Piece> =
        when (color) {
          PlayerName.WHITE -> {
            when (pieceType) {
              PieceType.GIPF,
              PieceType.TAMSK,
              PieceType.ZERTZ,
              PieceType.YINSH,
              null -> mutableListOf()

              PieceType.DVONN -> {
                whiteDVONNLayer
                    .mapIndexedNotNull { index, layer ->
                      if (index in 1..5 && (layer and bitmask) == bitmask) {
                        val pieceColor = if (index % 2 == 0) PlayerName.WHITE else PlayerName.BLACK
                        Piece(
                            abbreviation =
                                "${pieceColor.name.first()}${PieceType.DVONN.name.first()}",
                            potential = false,
                            colorName = pieceColor.name,
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
                      if (index in 1..5 && (layer and bitmask) == bitmask) {
                        val pieceColor = if (index % 2 == 0) PlayerName.WHITE else PlayerName.BLACK
                        Piece(
                            abbreviation =
                                "${pieceColor.name.first()}${PieceType.PUNCT.name.first()}",
                            potential = false,
                            colorName = pieceColor.name,
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
              PieceType.GIPF,
              PieceType.TAMSK,
              PieceType.ZERTZ,
              PieceType.YINSH,
              null -> mutableListOf()

              PieceType.DVONN -> {
                blackDVONNLayer
                    .mapIndexedNotNull { index, layer ->
                      if (index in 1..5 && (layer and bitmask) == bitmask) {
                        val pieceColor = if (index % 2 == 0) PlayerName.BLACK else PlayerName.WHITE
                        Piece(
                            abbreviation =
                                "${pieceColor.name.first()}${PieceType.DVONN.name.first()}",
                            potential = false,
                            colorName = pieceColor.name,
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
                      if (index in 1..5 && (layer and bitmask) == bitmask) {
                        val pieceColor = if (index % 2 == 0) PlayerName.BLACK else PlayerName.WHITE
                        Piece(
                            abbreviation =
                                "${pieceColor.name.first()}${PieceType.PUNCT.name.first()}",
                            potential = false,
                            colorName = pieceColor.name,
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

    val piece = pieceType?.let {
      Piece(
          abbreviation = "${color.name.first()}${pieceType.name.first()}",
          potential = hasPotential,
          colorName = color.name,
          type = it,
          isNeutralized = isNeutralized,
          stackedPieces = stackedPieces,
      )
    }
    // TODO does this update in place
    newBoard.nodes.first { it.bitmask == bitmask }.piece = piece
  }

  return newBoard
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
        //				println("shiftedPackedLine: ${(shiftedPackedLine shr idx).toString(radix =
        // 2).padStart(8, '0')}")
        //				println("mask             : ${mask.toString(radix = 2).padStart(8, '0')}")

        val value = if (((shiftedPackedLine shr idx) and mask) > 0.toUInt()) 1UL else 0UL

        updatedBitboard = updatedBitboard or (value shl i)

        //				println("value            : ${(value shl i).toString(radix = 2).padStart(64, '0')}")
        //				println("whitePieces      : ${whitePieces.toString(radix = 2).padStart(64, '0')}")
        //				println("updatedBitboard  : ${updatedBitboard.toString(radix = 2).padStart(64, '0')}")
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

fun Bitboard.addPieceToBitboard(
    addAtIndex: ULong,
    pushDirection: PushDirection,
    col: ColumnInfo,
    piece: Piece,
): ULong {
  // --- 0. CONFIGURABLE DEBUGGING ---
  val isDebugEnabled = false // Toggle this to true to see detailed trace logs
  if (isDebugEnabled) {
    println("--- ADD PIECE CALLED ---")
    println("Piece: ${piece.colorName} ${piece.type}")
    println("AddAtIndex: 0b${addAtIndex.toString(2)} | PushDirection: $pushDirection | Col: $col")
    println("Board before adding piece:  0b${globalOccupancy.toString(2)}")
  }

  // --- 1. PRE-CONDITION CHECKS (Input Validation) ---
  require(addAtIndex.countOneBits() == 1) {
    "CRITICAL ERROR: addAtIndex must be exactly one bit. Got: $addAtIndex"
  }

  check((addAtIndex and openningSpotsLineMask) == addAtIndex) {
    val newPieceBinary = addAtIndex.toString(2).padStart(40, '0')
    val openingLineBinary = openningSpotsLineMask.toString(2).padStart(40, '0')

    "Bitmask Validation Failure: The new piece mask contains bits outside the allowed opening line mask.\n" +
        "New Piece Mask:    $newPieceBinary\n" +
        "Opening Line Mask: $openingLineBinary"
  }

  // --- 2. HANDLE SHIFTING (If occupied) ---
  // Changed to `!= 0UL` as standard practice for checking if a specific bit intersects a mask
  val isIndexOccupied = (globalOccupancy and addAtIndex) != 0UL
  var vacantBitFound = 0UL

  if (isIndexOccupied) {
    if (isDebugEnabled)
        println(
            "Index 0b(${addAtIndex.toString(2)}) is occupied. Executing shift ($pushDirection)."
        )
    when (pushDirection) {
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        vacantBitFound = executePushUp(col)
        if (isDebugEnabled) println("Executed PushUp. Vacant bit found: $vacantBitFound")
      }
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        vacantBitFound = executePushDown(col)
        if (isDebugEnabled) println("Executed PushDown. Vacant bit found: $vacantBitFound")
      }
    }

    // Ensure the shift actually found exactly one empty spot to push into
    check(vacantBitFound.countOneBits() == 1) {
      "SHIFT ERROR: Shift execution must return exactly one vacant bit. Got: $vacantBitFound"
    }
  } else {
    if (isDebugEnabled) println("Index is vacant. No shift required.")
  }

  // --- 3. LOCAL HELPER FOR SAFE BIT ADDITION ---
  // This reduces the massive boilerplate in your original when-blocks
  // and guarantees post-condition verification for every single bitboard.
  fun safeAddAndCheck(board: ULong, boardName: String): ULong {
    if (isDebugEnabled) {
      println(
          "$boardName Board before restoring $boardName piece\n" +
              "addAtIndex: 0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
              "$boardName bitboard:  0b${board.toString(2).padStart(40, '0')}"
      )
    }
    val newBoard = board or addAtIndex
    check((newBoard and addAtIndex) == addAtIndex) {
      "ADD FAILED: Failed to add ${piece.colorName} ${piece.type} to $boardName bitboard at index $addAtIndex."
    }
    if (isDebugEnabled) {
      println(
          "$boardName Board after restoring $boardName piece\n" +
              "addAtIndex: 0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
              "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}"
      )
    }
    if (isDebugEnabled) println("Successfully added bit to $boardName.")
    return newBoard
  }

  // --- 4. APPLY TO BITBOARDS ---
  when (piece.colorName) {
    PlayerName.WHITE.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          whiteGIPF = safeAddAndCheck(whiteGIPF, "White GIPF")
        }
        PieceType.TAMSK -> {
          whiteTAMSK = safeAddAndCheck(whiteTAMSK, "White TAMSK")
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials")
        }
        PieceType.ZERTZ -> {
          whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ")
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials")
        }
        PieceType.DVONN -> {
          whiteDVONNLayer[0] = safeAddAndCheck(whiteDVONNLayer[0], "White DVONN[0]")
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials")
        }
        PieceType.YINSH -> {
          whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH")
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials")
        }
        PieceType.PUNCT -> {
          whitePUNCTLayer[0] = safeAddAndCheck(whitePUNCTLayer[0], "White PUNCT[0]")
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials")
        }
      }
    }

    PlayerName.BLACK.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          blackGIPF = safeAddAndCheck(blackGIPF, "Black GIPF")
        }
        PieceType.TAMSK -> {
          blackTAMSK = safeAddAndCheck(blackTAMSK, "Black TAMSK")
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials")
        }
        PieceType.ZERTZ -> {
          blackZERTZ = safeAddAndCheck(blackZERTZ, "Black ZERTZ")
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials")
        }
        PieceType.DVONN -> {
          blackDVONNLayer[0] = safeAddAndCheck(blackDVONNLayer[0], "Black DVONN[0]")
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials")
        }
        PieceType.YINSH -> {
          blackYINSH = safeAddAndCheck(blackYINSH, "Black YINSH")
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials")
        }
        PieceType.PUNCT -> {
          blackPUNCTLayer[0] = safeAddAndCheck(blackPUNCTLayer[0], "Black PUNCT[0]")
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials")
        }
      }
    }
    else -> error("UNKNOWN PLAYER: ${piece.colorName}")
  }

  if (isDebugEnabled) {
    println("--- ADD PIECE COMPLETE ---")
    println("Board after adding piece:  0b${globalOccupancy.toString(2)}")
  }

  return vacantBitFound
}

fun Bitboard.undoAddPieceToBitboard(
    removeAtIndex: ULong,
    vacantBitFound: ULong,
    pushDirection: PushDirection,
    col: ColumnInfo,
    piece: Piece,
    wasIndexOccupied: Boolean, // Required to know if we need to revert a shift
) {
  // --- 0. CONFIGURABLE DEBUGGING ---
  val isDebugEnabled = false // Toggle this to true to see detailed trace logs
  if (isDebugEnabled) {
    println("--- UNDO ADD PIECE CALLED ---")
    println("Piece: ${piece.colorName} ${piece.type}")
    println("RemoveAtIndex: $removeAtIndex | VacantBitFound: $vacantBitFound")
    println("WasOccupied: $wasIndexOccupied | PushDirection: $pushDirection")
    println("Board before removing piece:  0b${globalOccupancy.toString(2)}")
  }

  // --- 1. PRE-CONDITION CHECKS (Input Validation) ---
  require(removeAtIndex.countOneBits() == 1) {
    "CRITICAL ERROR: removeAtIndex must be exactly one bit. Got: $removeAtIndex"
  }
  if (wasIndexOccupied) {
    require(vacantBitFound.countOneBits() == 1) {
      "CRITICAL ERROR: vacantBitFound must be exactly one bit if reverting a shift. Got: $vacantBitFound"
    }
  }
  check((globalOccupancy and removeAtIndex) != 0UL) {
    "STATE ERROR: Piece cannot be removed from unoccupied index in globalOccupancy."
  }

  // Retained specific scenario debug hook from original code
  if (
      whiteGIPF != 10UL &&
          vacantBitFound == 2UL &&
          piece.type == PieceType.GIPF &&
          piece.colorName == PlayerName.WHITE.name &&
          removeAtIndex == 8UL &&
          wasIndexOccupied
  ) {
    println("DEBUG HOOK: Specific bit move scenario encountered before undo.")
  }

  // --- 2. LOCAL HELPER FOR SAFE BIT REMOVAL ---
  // This ensures the piece actually exists before removal, and is gone after.
  fun safeRemoveAndCheck(board: ULong, boardName: String): ULong {
    if (isDebugEnabled) {
      println(
          "$boardName Board before removing $boardName piece:\n" +
              "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
              "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}"
      )
    }
    check((board and removeAtIndex) != 0UL) {
      "UNDO FAILED: Expected ${piece.colorName} ${piece.type} in $boardName bitboard at $removeAtIndex, but it was missing."
    }
    val newBoard = board and removeAtIndex.inv()
    check((newBoard and removeAtIndex) == 0UL) {
      "UNDO FAILED: Failed to remove ${piece.colorName} ${piece.type} from $boardName bitboard."
    }
    if (isDebugEnabled) {
      println(
          "$boardName Board after removing $boardName piece\n" +
              "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
              "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}"
      )
    }
    if (isDebugEnabled) println("Successfully removed bit from $boardName.")
    return newBoard
  }

  // --- 3. REVERT THE BITBOARD ADDITIONS ---
  when (piece.colorName) {
    PlayerName.WHITE.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          whiteGIPF = safeRemoveAndCheck(whiteGIPF, "White GIPF")
        }
        PieceType.TAMSK -> {
          whiteTAMSK = safeRemoveAndCheck(whiteTAMSK, "White TAMSK")
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials")
        }
        PieceType.ZERTZ -> {
          whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ")
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials")
        }
        PieceType.DVONN -> {
          whiteDVONNLayer[0] = safeRemoveAndCheck(whiteDVONNLayer[0], "White DVONN[0]")
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials")
        }
        PieceType.YINSH -> {
          whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH")
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials")
        }
        PieceType.PUNCT -> {
          whitePUNCTLayer[0] = safeRemoveAndCheck(whitePUNCTLayer[0], "White PUNCT[0]")
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials")
        }
      }
    }
    PlayerName.BLACK.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          blackGIPF = safeRemoveAndCheck(blackGIPF, "Black GIPF")
        }
        PieceType.TAMSK -> {
          blackTAMSK = safeRemoveAndCheck(blackTAMSK, "Black TAMSK")
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials")
        }
        PieceType.ZERTZ -> {
          blackZERTZ = safeRemoveAndCheck(blackZERTZ, "Black ZERTZ")
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials")
        }
        PieceType.DVONN -> {
          blackDVONNLayer[0] = safeRemoveAndCheck(blackDVONNLayer[0], "Black DVONN[0]")
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials")
        }
        PieceType.YINSH -> {
          blackYINSH = safeRemoveAndCheck(blackYINSH, "Black YINSH")
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials")
        }
        PieceType.PUNCT -> {
          blackPUNCTLayer[0] = safeRemoveAndCheck(blackPUNCTLayer[0], "Black PUNCT[0]")
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials")
        }
      }
    }
    else -> error("UNKNOWN PLAYER: ${piece.colorName}")
  }

  // --- 4. REVERT THE SHIFTS ---
  if (wasIndexOccupied) {
    if (isDebugEnabled) println("Reverting board shift. Original push: $pushDirection")

    when (pushDirection) {
      // If the original move pushed UP, we must pull DOWN to undo
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        executePullDown(col, vacantBitFound)
        if (isDebugEnabled) println("Executed PullDown on col $col")
      }

      // If the original move pushed DOWN, we must pull UP to undo
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        executePullUp(col, vacantBitFound)
        if (isDebugEnabled) println("Executed PullUp on col $col")
      }
    }
  } else {
    if (isDebugEnabled) println("Index was not previously occupied. No shifts to revert.")
  }

  // --- 5. POST-CONDITION CHECKS ---
  if (isDebugEnabled) {
    println("--- UNDO ADD PIECE COMPLETE ---")
    println("Board after removing piece:  0b${globalOccupancy.toString(2)}")
  }
}

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

fun Bitboard.usePiecePotential(possibleBitMove: PossibleBitMove) {
  // TODO filter for legal moves before calling this
  //  pieces can not be neutralized

  require(possibleBitMove.pieceType != null) { "No piece was selected!" }
  require(possibleBitMove.pieceColor != null) { "No piece was selected!" }
  require(possibleBitMove.sourceBit != null) { "No piece was selected!" }
  require(possibleBitMove.targetBit != null) { "No piece was selected!" }

  // --- 0. CONFIGURABLE DEBUGGING ---
  val playerPotentials =
      if (possibleBitMove.pieceColor == PlayerName.WHITE) whitePotentials else blackPotentials
  val isDebugEnabled = false // Toggle this to true to see detailed trace logs
  if (isDebugEnabled) {
    println("--- USE POTENTIAL CALLED ---")
    println("Piece: ${possibleBitMove.pieceColor.name} ${possibleBitMove.pieceType}")
    println(
        "Source Bit:                   0b${possibleBitMove.sourceBit.toString(2).padStart(40, '0')}"
    )
    println(
        "Target Bit:                   0b${possibleBitMove.targetBit.toString(2).padStart(40, '0')}"
    )
    println("Player Potentials:            0b${playerPotentials.toString(2).padStart(40, '0')}")
    println("Board before using potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}")
  }

  // --- 1. PRE-CONDITION CHECKS (Input Validation) ---
  require(possibleBitMove.sourceBit.countOneBits() == 1) {
    "CRITICAL ERROR: sourceBit must be exactly one bit. Got: ${possibleBitMove.sourceBit}"
  }
  require(possibleBitMove.targetBit.countOneBits() == 1) {
    "CRITICAL ERROR: targetBit must be exactly one bit. Got: ${possibleBitMove.targetBit}"
  }

  fun safeRemoveAndCheck(board: ULong, boardName: String, removeAtIndex: ULong): ULong {
    if (isDebugEnabled) {
      println(
          "$boardName Board before using $boardName potential\n" +
              "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
              "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}"
      )
    }
    check((board and removeAtIndex) != 0UL) {
      "UNDO FAILED: Expected ${possibleBitMove.pieceColor.name} ${possibleBitMove.pieceType} in $boardName bitboard at $removeAtIndex, but it was missing."
    }
    val newBoard = board and removeAtIndex.inv()
    check((newBoard and removeAtIndex) == 0UL) {
      "UNDO FAILED: Failed to use potential for ${possibleBitMove.pieceColor.name} ${possibleBitMove.pieceType} piece from $boardName bitboard."
    }
    if (isDebugEnabled) {
      println(
          "$boardName Board after using $boardName potential\n" +
              "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
              "new $boardName bitboard:      0b${newBoard.toString(2).padStart(40, '0')}"
      )
    }
    if (isDebugEnabled) println("Successfully removed bit from $boardName.")
    return newBoard
  }

  fun safeAddAndCheck(board: ULong, boardName: String, addAtIndex: ULong): ULong {
    if (isDebugEnabled) {
      println(
          "$boardName Board before adding piece at \n" +
              "addAtIndex:    0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
              "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}"
      )
    }
    val newBoard = board or addAtIndex
    check((newBoard and addAtIndex) == addAtIndex) {
      "ADD FAILED: Failed to add ${possibleBitMove.pieceColor.name} ${possibleBitMove.pieceType} piece to $boardName bitboard at index $addAtIndex."
    }
    if (isDebugEnabled) {
      println(
          "$boardName Board after adding piece at \n" +
              "addAtIndex:    0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
              "new $boardName bitboard:      0b${newBoard.toString(2).padStart(40, '0')}"
      )
    }
    if (isDebugEnabled) println("Successfully added bit to $boardName.")
    return newBoard
  }

  // TODO which bitboards to add piece
  when (possibleBitMove.pieceColor.name) {
    PlayerName.WHITE.name -> {
      when (possibleBitMove.pieceType) {
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
          for (i in 1..5) {
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
        }

        PieceType.PUNCT -> {
          for (i in 1..5) {
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
        }
      }
    }

    PlayerName.BLACK.name -> {
      when (possibleBitMove.pieceType) {
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

          for (i in 1..5) {
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
        }

        PieceType.PUNCT -> {
          for (i in 1..5) {
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
        }
      }
    }
  }
  if (isDebugEnabled) {
    println("--- USE POTENTIAL COMPLETE ---")
    println("Board after using potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}")
  }
}

fun Bitboard.undoUsePiecePotential(possibleBitMove: PossibleBitMove) {
  // TODO filter for legal moves before calling this
  //  pieces can not be neutralized

  require(possibleBitMove.pieceType != null) { "No piece was selected!" }
  require(possibleBitMove.pieceColor != null) { "No piece was selected!" }
  require(possibleBitMove.sourceBit != null) { "No piece was selected!" }
  require(possibleBitMove.targetBit != null) { "No piece was selected!" }

  // --- 0. CONFIGURABLE DEBUGGING ---
  val playerPotentials =
      if (possibleBitMove.pieceColor == PlayerName.WHITE) whitePotentials else blackPotentials
  val isDebugEnabled = false // Toggle this to true to see detailed trace logs
  if (isDebugEnabled) {
    println("--- UNDO USE POTENTIAL CALLED ---")
    println("Piece: ${possibleBitMove.pieceColor.name} ${possibleBitMove.pieceType}")
    println(
        "Source Bit:                      0b${possibleBitMove.sourceBit.toString(2).padStart(40, '0')}"
    )
    println(
        "Target Bit:                      0b${possibleBitMove.targetBit.toString(2).padStart(40, '0')}"
    )
    println("Player Potentials:               0b${playerPotentials.toString(2).padStart(40, '0')}")
    println("Board before removing potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}")
  }

  // --- 1. PRE-CONDITION CHECKS (Input Validation) ---
  require(possibleBitMove.sourceBit.countOneBits() == 1) {
    "CRITICAL ERROR: sourceBit must be exactly one bit. Got: ${possibleBitMove.sourceBit}"
  }
  require(possibleBitMove.targetBit.countOneBits() == 1) {
    "CRITICAL ERROR: targetBit must be exactly one bit. Got: ${possibleBitMove.targetBit}"
  }

  fun safeRemoveAndCheck(board: ULong, boardName: String, removeAtIndex: ULong): ULong {
    if (isDebugEnabled) {
      println(
          "$boardName Board before removing $boardName piece:\n" +
              "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
              "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}"
      )
    }
    check((board and removeAtIndex) != 0UL) {
      "UNDO FAILED: Expected ${possibleBitMove.pieceColor.name} ${possibleBitMove.pieceType} in $boardName bitboard at $removeAtIndex, but it was missing."
    }
    val newBoard = board and removeAtIndex.inv()
    check((newBoard and removeAtIndex) == 0UL) {
      "UNDO FAILED: Failed to remove ${possibleBitMove.pieceColor.name} ${possibleBitMove.pieceType} piece from $boardName bitboard."
    }
    if (isDebugEnabled) {
      println(
          "$boardName Board after removing $boardName piece\n" +
              "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
              "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}"
      )
    }
    if (isDebugEnabled) println("Successfully removed bit from $boardName.")
    return newBoard
  }

  fun safeAddAndCheck(board: ULong, boardName: String, addAtIndex: ULong): ULong {
    if (isDebugEnabled) {
      println(
          "$boardName Board before restoring potential at \n0b${addAtIndex.toString(2).padStart(40, '0')}\n0b${board.toString(2).padStart(40, '0')}"
      )
    }
    val newBoard = board or addAtIndex
    check((newBoard and addAtIndex) == addAtIndex) {
      "ADD FAILED: Failed to restore potential for ${possibleBitMove.pieceColor.name} ${possibleBitMove.pieceType} to $boardName bitboard at index $addAtIndex."
    }
    if (isDebugEnabled) {
      println(
          "$boardName Board after restoring potential at \n0b${addAtIndex.toString(2).padStart(40, '0')}\n0b${newBoard.toString(2).padStart(40, '0')}"
      )
    }
    if (isDebugEnabled) println("Successfully added bit to $boardName.")
    return newBoard
  }

  // TODO which bitboards to add piece
  when (possibleBitMove.pieceColor.name) {
    PlayerName.WHITE.name -> {
      when (possibleBitMove.pieceType) {
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {
          //					 Use useTamskPotential()
          val isSourceValid =
              (possibleBitMove.sourceBit and whiteTAMSK and boardCenterSpotMask) ==
                  boardCenterSpotMask
          val isTargetValid =
              (possibleBitMove.targetBit and openningSpotsLineMask) == possibleBitMove.targetBit

          check(isSourceValid && isTargetValid) {
            buildString {
              appendLine("Illegal TAMSK Opening Move:")
              if (!isSourceValid) {
                appendLine(
                    "  -> Source Violation: Source index (0b${
										possibleBitMove.sourceBit.toString(2).padStart(40, '0')
									}) must be a White TAMSK piece located on the board center spot (0b${
										boardCenterSpotMask.toString(
											2
										).padStart(40, '0')
									})."
                )
              }
              if (!isTargetValid) {
                appendLine(
                    "  -> Target Violation: Target index (0b${
										possibleBitMove.targetBit.toString(2).padStart(40, '0')
									}) must be fully contained within the opening line mask (0b${
										openningSpotsLineMask.toString(
											2
										).padStart(40, '0')
									})."
                )
              }
            }
          }

          // TODO call undoAddPieceToBitboard
          whiteTAMSK = whiteTAMSK or possibleBitMove.targetBit
          whitePotentials = whitePotentials or boardCenterSpotMask
        }

        PieceType.ZERTZ -> {
          whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", possibleBitMove.targetBit)
          whitePotentials =
              safeAddAndCheck(whitePotentials, "White Potentials", possibleBitMove.sourceBit)
        }

        PieceType.YINSH -> {
          whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH", possibleBitMove.targetBit)
          whitePotentials =
              safeAddAndCheck(whitePotentials, "White Potentials", possibleBitMove.sourceBit)
        }

        // TODO something about this seems wrong
        PieceType.DVONN -> {
          for (i in 5 downTo 1) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whiteDVONNLayer[i] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit &&
                    (whiteDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN", possibleBitMove.targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit &&
                    (blackDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackDVONNLayer[i] =
                  safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN", possibleBitMove.targetBit)
              break
            }
          }

          whitePotentials =
              safeAddAndCheck(whitePotentials, "White Potentials", possibleBitMove.sourceBit)
        }

        PieceType.PUNCT -> {
          for (i in 5 downTo 1) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whitePUNCTLayer[i] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit &&
                    (whitePUNCTLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeRemoveAndCheck(whitePUNCTLayer[i], "White DVONN", possibleBitMove.targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit &&
                    (blackPUNCTLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeRemoveAndCheck(blackPUNCTLayer[i], "Black DVONN", possibleBitMove.targetBit)
              break
            }
          }

          whitePotentials =
              safeAddAndCheck(whitePotentials, "White Potentials", possibleBitMove.sourceBit)
        }
      }
    }

    PlayerName.BLACK.name -> {
      when (possibleBitMove.pieceType) {
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {
          val isSourceValid =
              (possibleBitMove.sourceBit and blackTAMSK and boardCenterSpotMask) ==
                  boardCenterSpotMask
          val isTargetValid =
              (possibleBitMove.targetBit and openningSpotsLineMask) == possibleBitMove.targetBit

          check(isSourceValid && isTargetValid) {
            buildString {
              appendLine("Illegal TAMSK Opening Move:")
              if (!isSourceValid) {
                appendLine(
                    "  -> Source Violation: Source index (0b${
										possibleBitMove.sourceBit.toString(2).padStart(40, '0')
									}) must be a Black TAMSK piece located on the board center spot (0b${
										boardCenterSpotMask.toString(
											2
										).padStart(40, '0')
									})."
                )
              }
              if (!isTargetValid) {
                appendLine(
                    "  -> Target Violation: Target index (0b${
										possibleBitMove.targetBit.toString(2).padStart(40, '0')
									}) must be fully contained within the opening line mask (0b${
										openningSpotsLineMask.toString(
											2
										).padStart(40, '0')
									})."
                )
              }
            }
          }

          // TODO undoAddPieceToBitboard
          blackTAMSK = blackTAMSK and possibleBitMove.targetBit.inv()
          blackPotentials = blackPotentials or boardCenterSpotMask
        }

        PieceType.ZERTZ -> {
          blackZERTZ = blackZERTZ and possibleBitMove.targetBit.inv()
          blackPotentials =
              safeAddAndCheck(blackPotentials, "Black Potentials", possibleBitMove.sourceBit)
        }

        PieceType.YINSH -> {
          blackYINSH = blackYINSH and possibleBitMove.targetBit.inv()
          blackPotentials =
              safeAddAndCheck(blackPotentials, "Black Potentials", possibleBitMove.sourceBit)
        }

        // TODO confirm if possibleBitMove.targetBit/Spot if is white
        // TODO something about this seems wrong
        PieceType.DVONN -> {

          for (i in 5 downTo 1) {
            // place white on top of black
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whiteDVONNLayer[i] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit &&
                    (whiteDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN", possibleBitMove.targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackDVONNLayer[i] and possibleBitMove.targetBit) == possibleBitMove.targetBit
            ) {
              blackDVONNLayer[i] =
                  safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN", possibleBitMove.targetBit)
              break
            }
          }

          blackPotentials =
              safeAddAndCheck(blackPotentials, "Black Potentials", possibleBitMove.sourceBit)
        }

        PieceType.PUNCT -> {
          for (i in 5 downTo 1) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whitePUNCTLayer[i] and possibleBitMove.targetBit) == possibleBitMove.targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeRemoveAndCheck(whitePUNCTLayer[i], "White PUNCT", possibleBitMove.targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackPUNCTLayer[i] and possibleBitMove.targetBit) == possibleBitMove.targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeRemoveAndCheck(blackPUNCTLayer[i], "Black PUNCT", possibleBitMove.targetBit)
              break
            }
          }

          blackPotentials =
              safeAddAndCheck(blackPotentials, "Black Potentials", possibleBitMove.sourceBit)
        }
      }
    }
  }

  if (isDebugEnabled) {
    println("--- UNDO USE POTENTIAL COMPLETE ---")
    println("Board after removing potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}")
  }
}

fun Bitboard.getTamskMoves(player: Player): PossibleBitMove? {
  // --- 0. CONFIGURABLE DEBUGGING ---
  val isDebugEnabled = false
  if (isDebugEnabled) {
    println("--- GET TAMSK MOVES CALLED ---")
    println("Player: ${player.name}")
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
    if (isDebugEnabled) {
      println("No valid TAMSK piece found at center for ${player.name}. Returning null.")
      println("--- GET TAMSK MOVES COMPLETED ---")
    }
    return null
  }

  // --- 3. PRE-RETURN SANITY CHECK ---
  // Double-check that our center spot mask hasn't been corrupted into a multi-bit mask.
  check(tamskPieceAtCenter.countOneBits() == 1) {
    "CRITICAL ERROR: Evaluated center spot mask must contain exactly one bit. Got: $tamskPieceAtCenter"
  }

  if (isDebugEnabled) println("Valid TAMSK move found for ${player.name} at center spot.")

  // --- 4. RETURN MOVE ---
  return PossibleBitMove(
      sourceBit = tamskPieceAtCenter,
      columnInfos = vacantLines,
      pieceType = PieceType.TAMSK,
      pieceColor = player.name,
      moveType = MoveType.AddPiece,
  )
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
  val isDebugEnabled = false
  if (isDebugEnabled) {
    println("--- USE TAMSK POTENTIAL CALLED ---")
    println("Player: ${player.name} | Source: $sourceIndex | Target: $targetIndex")
    println("PushDirection: $pushDirection | Col: $col")
    println("Player TAMSK:      0b${playerTAMSK.toString(2).padStart(40, '0')}")
    println("Player Potentials: 0b${playerPotentials.toString(2).padStart(40, '0')}")
    println("CenterMask:        0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
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
	    if (isDebugEnabled) {
		    println(
			    "White TAMSK Board before adding piece:       0b${whiteTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "White Potentials Board before adding piece:  0b${whitePotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }
      // Remove from Potentials board (using exact sourceIndex rather than broad mask)
      whitePotentials = whitePotentials and sourceIndex.inv()
      check((whitePotentials and sourceIndex) == 0UL) {
        "Failed to remove White Potential piece from source index."
      }
	    if (isDebugEnabled) {
		    println(
			    "White TAMSK Board after adding piece:        0b${whiteTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "White Potentials Board after adding piece:   0b${whitePotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }
    }

    PlayerName.BLACK -> {
	    if (isDebugEnabled) {
		    println(
			    "Black TAMSK Board before adding piece:       0b${blackTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "Black Potentials Board before adding piece:  0b${blackPotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }
      // Remove from Potentials board (using exact sourceIndex rather than broad mask)
      blackPotentials = blackPotentials and sourceIndex.inv()
      check((blackPotentials and sourceIndex) == 0UL) {
        "Failed to remove Black Potential piece from source index."
      }

	    if (isDebugEnabled) {
		    println(
			    " Black TAMSK Board after adding piece:        0b${blackTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "Black Potentials Board after adding piece:   0b${blackPotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }
    }
  }

  // --- 3. HANDLE SHIFTING ---
  // Changed `> 0UL` to `!= 0UL` (Standard bitwise evaluation)
  val isIndexOccupied = (globalOccupancy and targetIndex) != 0UL
  var vacantBitFound = 0UL

  if (isIndexOccupied) {
    if (isDebugEnabled) println("Target index is occupied. Executing shift ($pushDirection).")
    when (pushDirection) {
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        vacantBitFound = executePushUp(col)
        if (isDebugEnabled) println("Executed PushUp. Vacant bit found: $vacantBitFound")
      }
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        vacantBitFound = executePushDown(col)
        if (isDebugEnabled) println("Executed PushDown. Vacant bit found: $vacantBitFound")
      }
    }

    // Validate shift execution
    check(vacantBitFound.countOneBits() == 1) {
      "SHIFT ERROR: Shift execution must return exactly one vacant bit. Got: $vacantBitFound"
    }
  } else {
    if (isDebugEnabled) println("Target index is vacant. No shift required.")
  }

  // --- 4. APPLY TO BITBOARDS (With Post-Condition Checks) ---
  if (isDebugEnabled) println("Applying state changes for ${player.name}...")

  when (player.name) {
    PlayerName.WHITE -> {
	    if (isDebugEnabled) {
		    println(
			    "White TAMSK Board before adding piece:       0b${whiteTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "White Potentials Board before adding piece:  0b${whitePotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }
      // Add to TAMSK board
      whiteTAMSK = whiteTAMSK or targetIndex
      check((whiteTAMSK and targetIndex) != 0UL) {
        "Failed to add White TAMSK piece to target index."
      }
	    if (isDebugEnabled) {
		    println(
			    "White TAMSK Board after adding piece:        0b${whiteTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "White Potentials Board after adding piece:   0b${whitePotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }
    }

    PlayerName.BLACK -> {
	    if (isDebugEnabled) {
		    println(
			    "Black TAMSK Board before adding piece:       0b${blackTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "Black Potentials Board before adding piece:  0b${blackPotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }

      // Add to TAMSK board
      blackTAMSK = blackTAMSK or targetIndex
      check((blackTAMSK and targetIndex) != 0UL) {
        "Failed to add Black TAMSK piece to target index."
      }

	    if (isDebugEnabled) {
		    println(
			    "Black TAMSK Board after adding piece:        0b${blackTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "Black Potentials Board after adding piece:   0b${blackPotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }
    }
  }

  if (isDebugEnabled) {
    println("--- USE TAMSK POTENTIAL COMPLETE ---")
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
  val isDebugEnabled = false
  if (isDebugEnabled) {
    println("--- UNDO TAMSK POTENTIAL CALLED ---")
    println("Player: ${player.name} | Source: $sourceIndex | RemoveAt: $removeAtIndex")
    println("WasOccupied: $wasIndexOccupied | PushDirection: $pushDirection | Col: $col")
    println("Player TAMSK:      0b${playerTAMSK.toString(2).padStart(40, '0')}")
    println("Player Potentials: 0b${playerPotentials.toString(2).padStart(40, '0')}")
    println("CenterMask:        0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
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
  if (isDebugEnabled) println("Reverting bitboards for ${player.name}...")

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
    if (isDebugEnabled) println("Reverting board shift. Original push: $pushDirection")
    when (pushDirection) {
      // If the original move pushed UP, we must pull DOWN to undo
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        executePullDown(col, vacantBitFound)
        if (isDebugEnabled) println("Executed PullDown on col $col")
      }

      // If the original move pushed DOWN, we must pull UP to undo
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        executePullUp(col, vacantBitFound)
        if (isDebugEnabled) println("Executed PullUp on col $col")
      }
    }
  } else {
    if (isDebugEnabled) println("Index was not previously occupied. No shifts to revert.")
  }

  // --- RESTORE POTENTIAL AFTER SHIFTING ---
  if (isDebugEnabled) println("--- RESTORE POTENTIAL AFTER SHIFTING ---")
  when (player.name) {
    PlayerName.WHITE -> {
	    if (isDebugEnabled) {
		    println(
			    "White TAMSK Board before adding piece:       0b${whiteTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "White Potentials Board before adding piece:  0b${whitePotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }

      // Restore the potential piece
      whitePotentials = whitePotentials or sourceIndex
      check((whitePotentials and sourceIndex) != 0UL) {
        "Failed to restore White Potential piece to center."
      }
	    if (isDebugEnabled) {
		    println(
			    "White TAMSK Board after adding piece:        0b${whiteTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "White Potentials Board after adding piece:   0b${whitePotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }
    }

    PlayerName.BLACK -> {
	    if (isDebugEnabled) {
		    println(
			    "Black TAMSK Board before adding piece:       0b${blackTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "Potentials Board before adding piece:  0b${blackPotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }

      // Restore the potential piece
      blackPotentials = blackPotentials or sourceIndex
      check((blackPotentials and sourceIndex) != 0UL) {
        "Failed to restore Black Potential piece to center."
      }

	    if (isDebugEnabled) {
		    println(
			    "TAMSK Board after adding piece:        0b${blackTAMSK.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "Potentials Board after adding piece:   0b${blackPotentials.toString(2).padStart(40, '0')}"
		    )
		    println(
			    "CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
		    )
	    }
    }
  }

  if (isDebugEnabled) println("--- UNDO TAMSK POTENTIAL COMPLETE ---")

  return vacantBitFound
}

fun Bitboard.getZertzMoves(
    player: Player,
    columnInfos: List<ColumnInfo>,
): MutableList<PossibleBitMove> {
  val validMoves = mutableListOf<PossibleBitMove>()

  val zertzPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteZERTZ and whitePotentials
        }

        PlayerName.BLACK -> {
          blackZERTZ and blackPotentials
        }
      }

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
                validMoves.add(
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
                validMoves.add(
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
  return validMoves
}

fun Bitboard.getYinshMoves(
    player: Player,
    columnInfos: List<ColumnInfo>,
): MutableList<PossibleBitMove> {
  val validMoves = mutableListOf<PossibleBitMove>()

  val yinchPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteYINSH and whitePotentials
        }

        PlayerName.BLACK -> {
          blackYINSH and blackPotentials
        }
      }

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
                validMoves.add(
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
                validMoves.add(
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

  return validMoves
}

fun Bitboard.getDvonnMoves(
    player: Player,
    columnInfos: List<ColumnInfo>,
): MutableList<PossibleBitMove> {
  val validMoves = mutableListOf<PossibleBitMove>()

  val activeDvonnPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteDVONNLayer[0] and whitePotentials and whiteNeutralized.inv()
        }

        PlayerName.BLACK -> {
          blackDVONNLayer[0] and blackPotentials and blackNeutralized.inv()
        }
      }

  val layers = 5
  val targetDvonnPieces =
      when (player.name) {
        PlayerName.BLACK -> {
          var activeWhiteDvonn = 0UL
          var blockedDvonnMask = 0UL

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

          activeWhiteDvonn
        }

        PlayerName.WHITE -> {
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

          activeBlackDvonn
        }
      }

  columnInfos.forEach { col ->
    if (
        (col.columnMask and activeDvonnPieces > 0UL) && (col.columnMask and targetDvonnPieces > 0UL)
    ) {
      col.positions
          .filter {
            it and (col.columnMask and activeDvonnPieces) != 0UL // get position of yinch pieces
          }
          .forEach {
            val dvonnIndex = col.positions.indexOf(it)

            for (index in dvonnIndex.plus(1) until col.positions.size) {
              if ((col.positions[index] and targetDvonnPieces) > 0UL) {
                validMoves.add(
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
                validMoves.add(
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

  return validMoves
}

fun Bitboard.getPunctMoves(
    player: Player,
    columnInfos: List<ColumnInfo>,
): MutableList<PossibleBitMove> {
  val validMoves = mutableListOf<PossibleBitMove>()

  val activePunctPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whitePUNCTLayer[0] and whitePotentials and whiteNeutralized.inv()
        }

        PlayerName.BLACK -> {
          blackPUNCTLayer[0] and blackPotentials and blackNeutralized.inv()
        }
      }

  val layers = 5
  val targetPunctPieces =
      when (player.name) {
        PlayerName.BLACK -> {
          var activeWhitePunct = 0UL
          var blockedPunctMask = 0UL

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

          activeWhitePunct
        }

        PlayerName.WHITE -> {
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

          activeBlackPunct
        }
      }

  columnInfos.forEach { col ->
    if (
        (col.columnMask and activePunctPieces > 0UL) && (col.columnMask and targetPunctPieces > 0UL)
    ) {
      col.positions
          .filter {
            it and (col.columnMask and activePunctPieces) != 0UL // get position of punct pieces
          }
          .forEach {
            val punctIndex = col.positions.indexOf(it)

            for (index in punctIndex.plus(1) until col.positions.size) {
              if ((col.positions[index] and targetPunctPieces) > 0UL) {
                validMoves.add(
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
                validMoves.add(
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

  return validMoves
}

fun Bitboard.evaluateLinesForFourInARow(player: Player): List<ColumnInfo> {
  // --- 0. CONFIGURABLE DEBUGGING ---
  val isDebugEnabled = false
  if (isDebugEnabled) {
    println("--- EVALUATE LINES FOR FOUR IN A ROW CALLED ---")
    println("Player: ${player.name}")
    println("Bitboard State: ${Json.encodeToString<Bitboard>(this)}")
  }

  val playerPieces =
      when (player.name) {
        PlayerName.WHITE -> whitePieces
        PlayerName.BLACK -> blackPieces
      }

	var stooopid: String? = null
	val columns = columnInfos.filterIndexed { index, column ->
    if (isDebugEnabled) {
      println("player Active Pieces: 0b${playerPieces.toString(2).padStart(40, '0')}")
      println("Line $index Mask:         0b${column.columnMask.toString(2).padStart(40, '0')}")
    }

		val result = column.submasks.any { submask ->
      val fourInARow = (submask and playerPieces) == submask
      if (isDebugEnabled && fourInARow) {
        println("Sublist:              0b${submask.toString(2).padStart(40, '0')}")
        println("player Active Pieces: 0b${playerPieces.toString(2).padStart(40, '0')}")
        println("Result: $fourInARow")
      }
      fourInARow
		}

		if (isDebugEnabled) {
			if (result) println("--- FOUND FOUR IN A ROW IN LINE $index ---") else println("--- NO 4 IN A ROW IN LINE $index ---")
    }

//    if ((column.columnMask and playerPieces).countOneBits() < 4) {
//      if (isDebugEnabled) {
//        println("--- NO 4 IN A LINE $index ---")
//      }
//      false
//    } else {
//      if (isDebugEnabled) {
//        println(
//            "--- FOUND FOUR IN A LINE $index ---"
//        ) // \n0b${column.columnMask.toString(2).padStart(40, '0')}")
//      }
//
//	    println("column.positions.windowed(4).any: ${
//		    column.positions.windowed(4).any { sublist ->
//			    val sum = sublist.fold(0UL) { acc, lng ->
//				    acc or lng
//			    }
//			    (sum and playerPieces) == sum
//		    }
//			}")

	  result

  }

  if (isDebugEnabled) {
    println("--- EVALUATE LINES FOR FOUR IN A ROW COMPLETED ---")
  }

	return columns
}

fun Bitboard.identifyPlayerPiecesWithPotential(
    columnInfos: List<ColumnInfo>,
    player: Player,
): List<ULong> {
  require(columnInfos.isNotEmpty()) { "There must be at least one column" }

  // TODO handle intersecting lines, but do i?

  val playerPiecesWithPotential = columnInfos.map { column ->
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

  return playerPiecesWithPotential.flatten()
}

fun Bitboard.createPlayerPiecesWithPotentialPowerset(potentials: List<ULong>): List<List<ULong>> {
  // TODO causes stack overflow error, but an empty list is necessary as a player can leave the
  // stack in play
  // TODO Minimax/MCTS — what it would be like to remove at least one of these pieces if all pieces
  //   have potentials
  //   use line score heuristic and pieces in reserve
  // todo return of a list containing different combinations of bit positions
  return potentials
      .fold(initial = listOf(emptyList<ULong>())) { accumulator, item ->
        // For every item, take the current sublists (accumulator)
        // and add a new set of sublists where the item is appended
        accumulator + accumulator.map { it + item }
      }
      .filter { it.isNotEmpty() }
}

fun Bitboard.createRetrieveAndCapturePiecesList(
    columnInfos: List<ColumnInfo>,
    selectedPiecesWithPotentialPowerset: List<ULong>,
    player: Player,
): List<RetrievedCapturedPieceBit> {
  // TODO create pieces for retrieval and capturing
  // TODO return retrieved and captured pieces
  require(columnInfos.isNotEmpty()) { "There must be at least one column" }

  // --- 0. CONFIGURABLE DEBUGGING ---
  val isDebugEnabled = false
  if (isDebugEnabled) {
    println("--- CREATE RETRIEVE & CAPTURED PIECES CALLED ---")
  }

  val playerPiecesNotNeutralizedWithoutPotential =
      columnInfos
          .map { column ->
            column.positions.filter { bitmask ->
              val result =
                  when (player.name) {
                    PlayerName.WHITE -> {
                      // TODO FIX - includes neutralized pieces
                      // pieces without potential and not neutralized
	                    (whitePieces and bitmask) == bitmask &&
			                    (whitePotentials.inv() and bitmask) == bitmask &&
			                   (blackNeutralized.inv() and bitmask) == bitmask
                    }

                    PlayerName.BLACK -> {
											(blackPieces and bitmask) == bitmask &&
						              (blackPotentials.inv() and bitmask) == bitmask &&
                          (whiteNeutralized.inv() and bitmask) == bitmask
                    }
                  }
              result
            }
          }
          .flatten()

  val opponentPiecesAndNotNeutralized =
      columnInfos
          .map { column ->
            column.positions.filter { bitmask ->
              val result =
                  when (player.name) {
                    PlayerName.WHITE -> {
                      // opponent pieces and not neutralized
                      //	            (blackPotentials.inv() and bitmask) == bitmask &&
											(blackPieces and bitmask) == bitmask &&
                      (blackNeutralized.inv() and bitmask) == bitmask
                    }

                    PlayerName.BLACK -> {
                      //	            (whitePotentials.inv() and bitmask) == bitmask &&
	                    (whitePieces and bitmask) == bitmask &&
                      (whiteNeutralized.inv() and bitmask) == bitmask
                    }
                  }
              result
            }
          }
          .flatten()

  // stacked DVONN and PUNCT Pieces
  val neutralizedBitmasks =
      columnInfos
          .map { column ->
            column.positions.filter { bitmask ->
              // TODO THis
              (whiteNeutralized and bitmask) == bitmask || (blackNeutralized and bitmask) == bitmask
            }
          }
          .flatten()

  // --- 1. PRE-CONDITION CHECKS ---
	if (neutralizedBitmasks.isNotEmpty()) {
		check(
			neutralizedBitmasks.any {
				!playerPiecesNotNeutralizedWithoutPotential.contains(it)
			}
		) {
			"playerPiecesNotNeutralizedWithoutPotential should not contain any bits from neutralizedBitmasks"
		}
	}

  check(
      !selectedPiecesWithPotentialPowerset.any {
        playerPiecesNotNeutralizedWithoutPotential.contains(it)
      }
  ) {
    "playerPiecesNotNeutralizedWithoutPotential should not contain any bits from selectedPiecesWithPotentialPowerset"
  }

  // --- LOCAL HELPER: BASE LAYER REMOVAL ---
  fun safeRemoveAndCheck(board: ULong, boardName: String, bitmask: ULong, piece: Piece): ULong {
    if (isDebugEnabled) {
      println(
          "$boardName Board before removing $boardName piece:\n" +
              "removeAtIndex:           0b${bitmask.toString(2).padStart(40, '0')}\n" +
              "$boardName bitboard:     0b${board.toString(2).padStart(40, '0')}"
      )
    }
    val newBoard = board and bitmask.inv()
    check((newBoard and bitmask) == 0UL) {
      "Failed to remove ${piece.colorName} ${piece.type} from $boardName."
    }
    if (isDebugEnabled) {
      println(
          "$boardName Board after removing $boardName piece\n" +
              "removeAtIndex:            0b${bitmask.toString(2).padStart(40, '0')}\n" +
              "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}"
      )
    }
    if (isDebugEnabled) println("  -> Successfully removed from $boardName.")
    return newBoard
  }

  val neutralizedPieces = mutableListOf<RetrievedCapturedPieceBit>()
  neutralizedBitmasks.forEach { bitmask ->
    for (i in whiteDVONNLayer.indices.reversed()) {

      // 1. Check exactly which piece exists at this layer for this bitmask
      val hasWhiteDVONN = (whiteDVONNLayer[i] and bitmask) == bitmask
      val hasBlackDVONN = (blackDVONNLayer[i] and bitmask) == bitmask
      val hasWhitePUNCT = (whitePUNCTLayer[i] and bitmask) == bitmask
      val hasBlackPUNCT = (blackPUNCTLayer[i] and bitmask) == bitmask

      // If this specific coordinate/layer is empty, check the next layer down
      if (!hasWhiteDVONN && !hasBlackDVONN && !hasWhitePUNCT && !hasBlackPUNCT) {
        continue
      }

      // 2. Identify color and type
      val isEven = (i % 2 == 0)

      if (i > 0) {
        when {
          hasWhiteDVONN || hasBlackDVONN -> {
            // Determine base color based on which board it came from
            val baseColor = if (hasWhiteDVONN) PlayerName.WHITE else PlayerName.BLACK
            // Flip color if layer is odd
            val finalColor =
                if (isEven) baseColor
                else if (baseColor == PlayerName.WHITE) PlayerName.BLACK else PlayerName.WHITE

            val piece =
                Piece(
                    abbreviation = "${finalColor.name.first()}P",
                    potential = false,
                    colorName = finalColor.name,
                    type = PieceType.PUNCT,
                    isNeutralized = false,
                    stackedPieces = mutableListOf(),
                )

            neutralizedPieces.add(
                RetrievedCapturedPieceBit(
                    retrievedPiece = if (finalColor == player.name) piece else null,
                    capturedPiece = if (finalColor != player.name) piece else null,
                    bitmask = bitmask,
                    isNeutralized = true,
                )
            )

            blackDVONNLayer[i] =
                safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", bitmask, piece)
            whiteDVONNLayer[i] =
                safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", bitmask, piece)
            break
          }

          hasWhitePUNCT || hasBlackPUNCT -> {
            val baseColor = if (hasWhitePUNCT) PlayerName.WHITE else PlayerName.BLACK
            val finalColor =
                if (isEven) baseColor
                else if (baseColor == PlayerName.WHITE) PlayerName.BLACK else PlayerName.WHITE

            val piece =
                Piece(
                    abbreviation = "${finalColor.name.first()}P",
                    potential = false,
                    colorName = finalColor.name,
                    type = PieceType.PUNCT,
                    isNeutralized = false,
                    stackedPieces = mutableListOf(),
                )

            neutralizedPieces.add(
                RetrievedCapturedPieceBit(
	                  retrievedPiece = if (finalColor == player.name) piece else null,
	                  capturedPiece = if (finalColor != player.name) piece else null,
                    bitmask = bitmask,
                    isNeutralized = true,
                )
            )

            blackPUNCTLayer[i] =
                safeRemoveAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", bitmask, piece)
            whitePUNCTLayer[i] =
                safeRemoveAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", bitmask, piece)
            break
          }
        }
      }

      // Base layer logic (i == 0) handled by playerPiecesWithPotential

      //			} else {
      //				// Base layer logic (i == 0)
      //				val whiteHasPotential = (whitePotentials and bitmask) == bitmask
      //				val blackHasPotential = (blackPotentials and bitmask) == bitmask
      //
      //				when {
      //					hasWhiteDVONN -> neutralizedPieces.add(createBasePiece(PlayerName.WHITE,
      // PieceType.DVONN, whiteHasPotential))
      //					hasBlackDVONN -> neutralizedPieces.add(createBasePiece(PlayerName.BLACK,
      // PieceType.DVONN, blackHasPotential))
      //					hasWhitePUNCT -> neutralizedPieces.add(createBasePiece(PlayerName.WHITE,
      // PieceType.PUNCT, whiteHasPotential))
      //					hasBlackPUNCT -> neutralizedPieces.add(createBasePiece(PlayerName.BLACK,
      // PieceType.PUNCT, blackHasPotential))
      //				}
      //			}

      // 3. We found the top-most piece for this bitmask.
      // Break out of the layer loop so we don't grab blocked pieces underneath.
      break
    }
  }

  // TODO handle DVONN and PUNCT carefully. remove bit from top down to 0 layer

  // TODO set bits to zero. create function to clear bit.
  val pieces =
      playerPiecesNotNeutralizedWithoutPotential
          .plus(opponentPiecesAndNotNeutralized)
          .plus(selectedPiecesWithPotentialPowerset)
          .map { bitmask ->
            // construct piece and place it on the matching node
            val piece = createPiece(bitmask)

            if (piece != null) {
              when (piece.type) {
                PieceType.GIPF -> {
                  whiteGIPF = safeRemoveAndCheck(whiteGIPF, "White GIPF", bitmask, piece)
                  blackGIPF = safeRemoveAndCheck(blackGIPF, "Black GIPF", bitmask, piece)
                }

                PieceType.TAMSK -> {
                  whiteTAMSK = safeRemoveAndCheck(whiteTAMSK, "White TAMSK", bitmask, piece)
                  blackTAMSK = safeRemoveAndCheck(blackTAMSK, "Black TAMSK", bitmask, piece)
                }

                PieceType.ZERTZ -> {
                  whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", bitmask, piece)
                  blackZERTZ = safeRemoveAndCheck(blackZERTZ, "Black ZERTZ", bitmask, piece)
                }

                PieceType.DVONN -> {
                  if (piece.stackedPieces.isNotEmpty()) {
                    throw IllegalStateException("")
                  } else {
                    whiteDVONNLayer[0] =
                        safeRemoveAndCheck(whiteDVONNLayer[0], "White DVONN [0]", bitmask, piece)
                    blackDVONNLayer[0] =
                        safeRemoveAndCheck(blackDVONNLayer[0], "Black DVONN [0]", bitmask, piece)
                  }
                }

                PieceType.YINSH -> {
                  whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH", bitmask, piece)
                  blackYINSH = safeRemoveAndCheck(blackYINSH, "Black YINSH", bitmask, piece)
                }

                PieceType.PUNCT -> {
                  if (piece.stackedPieces.isNotEmpty()) {
                    throw IllegalStateException("")
                  } else {
                    whitePUNCTLayer[0] =
                        safeRemoveAndCheck(whitePUNCTLayer[0], "White PUNCT [0]", bitmask, piece)
                    blackPUNCTLayer[0] =
                        safeRemoveAndCheck(blackPUNCTLayer[0], "Black PUNCT [0]", bitmask, piece)
                  }
                }
              }

              if (piece.potential) {
                whitePotentials =
                    safeRemoveAndCheck(whitePotentials, "White Potentials", bitmask, piece)
                blackPotentials =
                    safeRemoveAndCheck(blackPotentials, "Black Potentials", bitmask, piece)
              }
            }
            RetrievedCapturedPieceBit(
                retrievedPiece = if (piece?.colorName == player.name.name) piece else null,
                capturedPiece = if (piece?.colorName != player.name.name) piece else null,
                bitmask = bitmask,
                isNeutralized = false,
            )
          }
          .plus(neutralizedPieces)
          .filter { it.retrievedPiece != null || it.capturedPiece != null }

  // TODO Check captured pieces aren't categorized as retrieved pieces and vice versa
  val targetColor = player.name.name

  check(pieces.mapNotNull { it.retrievedPiece }.all { it.colorName == targetColor }) {
    val invalidRetrieved =
        pieces
            .mapNotNull { it.retrievedPiece }
            .filter { it.colorName != targetColor }
            .map { "${it.type}(${it.colorName})" }
    "Scoring Violation: Player '$targetColor' attempted to retrieve opponent pieces: $invalidRetrieved"
  }

  check(pieces.mapNotNull { it.capturedPiece }.all { it.colorName != targetColor }) {
    val invalidCaptured =
        pieces
            .mapNotNull { it.capturedPiece }
            .filter { it.colorName == targetColor }
            .map { it.type }
    "Scoring Violation: Player '$targetColor' accidentally captured their own pieces: $invalidCaptured"
  }

  if (isDebugEnabled) {
    println("Bitboard State: ${Json.encodeToString<Bitboard>(this)}")
    println("Pieces retrieved & captured (count: ${pieces.size}):")
    pieces.forEachIndexed { index, piece ->
      println("Piece $index: $piece")
    }
    println("--- CREATE RETRIEVE & CAPTURED PIECES COMPLETED ---")
    println("====================================================")
  }

  return pieces
}

fun Bitboard.getsSelectedPiecesWithPotentialPowerset(
    selectedPiecesWithPotentialPowerset: List<ULong>,
    player: Player,
): List<RetrievedCapturedPieceBit> {
  return selectedPiecesWithPotentialPowerset.map { bitmask ->
    // construct piece and place it on the matching node
    val piece = createPiece(bitmask)

    val activePlayerColor = player.name.name

    check(piece?.colorName == activePlayerColor) {
      "Action Denied: Player '$activePlayerColor' cannot interact with this piece. " +
          "Found: ${piece?.type ?: "Empty Space"} belonging to '${piece?.colorName ?: "Nobody"}'." +
          "\n bitmask: 0b${bitmask.toString(2).padStart(40, '0')}"
    }

    check(piece.stackedPieces.isEmpty()) {
      val hiddenTypes = piece.stackedPieces.map { "${it.type}(${it.colorName})" }
      "Inconsistency failure: Expected an unstacked basic piece, but found a stack containing " +
          "${piece.stackedPieces.size} hidden pieces: $hiddenTypes."
    }

    check(piece.potential) {
      "State violation: Expected a 'Potential' piece structure, but found a basic, standard piece instead. " +
          "Piece Type: ${piece.type}, Color: ${piece.colorName}, Abbreviation: ${piece.abbreviation}."
    }

    when (piece.type) {
      PieceType.GIPF -> {
        whiteGIPF = whiteGIPF and bitmask.inv()
        blackGIPF = blackGIPF and bitmask.inv()
      }

      PieceType.TAMSK -> {
        whiteTAMSK = whiteTAMSK and bitmask.inv()
        blackTAMSK = blackTAMSK and bitmask.inv()
      }

      PieceType.ZERTZ -> {
        whiteZERTZ = whiteZERTZ and bitmask.inv()
        blackZERTZ = blackZERTZ and bitmask.inv()
      }

      PieceType.DVONN -> {
        if (piece.stackedPieces.isNotEmpty()) {
          throw IllegalStateException("")
        } else {
          whiteDVONNLayer[0] = whiteDVONNLayer[0] and bitmask.inv()
          blackDVONNLayer[0] = blackDVONNLayer[0] and bitmask.inv()
        }
      }

      PieceType.YINSH -> {
        whiteYINSH = whiteYINSH and bitmask.inv()
        blackYINSH = blackYINSH and bitmask.inv()
      }

      PieceType.PUNCT -> {
        if (piece.stackedPieces.isNotEmpty()) {
          throw IllegalStateException("")
        } else {
          whitePUNCTLayer[0] = whitePUNCTLayer[0] and bitmask.inv()
          blackPUNCTLayer[0] = blackPUNCTLayer[0] and bitmask.inv()
        }
      }
    }

    if (piece.potential) {
      whitePotentials = whitePotentials and bitmask.inv()
      blackPotentials = blackPotentials and bitmask.inv()
    }

    RetrievedCapturedPieceBit(
        retrievedPiece = if (piece.colorName == player.name.name) piece else null,
        capturedPiece = null,
        bitmask = bitmask,
        isNeutralized = false,
    )
  }
}

fun Bitboard.undoRetrieveAndCapturePieces(
    retrievedCapturedPiecesBits: List<RetrievedCapturedPieceBit>
) {
  // --- 0. CONFIGURABLE DEBUGGING ---
  val isDebugEnabled = false
  if (isDebugEnabled) {
    println("--- UNDO REMOVE/RETRIEVE CAPTURED PIECES CALLED ---")
    println("Bitboard State: ${Json.encodeToString<Bitboard>(this)}")
    println("Processing ${retrievedCapturedPiecesBits.size} pieces...")
    retrievedCapturedPiecesBits.forEachIndexed { index, piece ->
      println("piece $index: $piece")
    }
  }

  retrievedCapturedPiecesBits.forEachIndexed { index, data ->
    val (retrievedPiece, capturedPiece, bitmask, isNeutralized, keepRetrievedPieceInPlay) = data
    val piece = retrievedPiece ?: capturedPiece

    if (piece == null) {
      if (isDebugEnabled)
          println("Item $index: Both retrieved and captured pieces are null. Skipping.")
      return@forEachIndexed
    }

    if (isDebugEnabled) {
      println("Processing Piece $index: ${piece.colorName} ${piece.type} at $bitmask")
      println("  -> isNeutralized: $isNeutralized | Potential: ${piece.potential}")
    }

    // --- 1. PRE-CONDITION CHECKS ---
    require(bitmask.countOneBits() == 1) {
      "CRITICAL ERROR: Removal bitmask must be exactly one bit. Got: $bitmask"
    }

    fun safeAddAndCheck(board: ULong, boardName: String): ULong {
      if (isDebugEnabled) {
        println(
            "$boardName Board before restoring $boardName piece\n" +
                "addAtIndex: 0b${bitmask.toString(2).padStart(40, '0')}\n" +
                "$boardName bitboard:  0b${board.toString(2).padStart(40, '0')}"
        )
      }
      val newBoard = board or bitmask
      check(newBoard != board) {
        "ADD FAILED: Failed to add ${piece.colorName} ${piece.type} to $boardName bitboard at index $bitmask."
      }
      if (isDebugEnabled) {
        println(
            "$boardName Board after restoring $boardName piece\n" +
                "addAtIndex: 0b${bitmask.toString(2).padStart(40, '0')}\n" +
                "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}"
        )
      }
      if (isDebugEnabled) println("Successfully added bit to $boardName.")
      return newBoard
    }

    when (piece.colorName) {
      PlayerName.WHITE.name -> {
        when (piece.type) {
          PieceType.GIPF -> whiteGIPF = safeAddAndCheck(whiteGIPF, "White GIPF")
          PieceType.TAMSK -> whiteTAMSK = safeAddAndCheck(whiteTAMSK, "White TAMSK")
          PieceType.ZERTZ -> whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ")
          PieceType.YINSH -> whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH")
          else -> {} // DVONN and PUNCT handled below
        }
      }
      PlayerName.BLACK.name -> {
        when (piece.type) {
          PieceType.GIPF -> blackGIPF = safeAddAndCheck(blackGIPF, "Black GIPF")
          PieceType.TAMSK -> blackTAMSK = safeAddAndCheck(blackTAMSK, "Black TAMSK")
          PieceType.ZERTZ -> blackZERTZ = safeAddAndCheck(blackZERTZ, "Black ZERTZ")
          PieceType.YINSH -> blackYINSH = safeAddAndCheck(blackYINSH, "Black YINSH")
          else -> {} // DVONN and PUNCT handled below
        }
      }
    }

    if (isNeutralized) {
      var pieceRemoved = false
      for (i in 1..5) {
        if (pieceRemoved) break

        when (piece.colorName) {
          PlayerName.WHITE.name -> {
            when (piece.type) {
              PieceType.DVONN -> {
                // place white on top of black on top of white
                if (
                    i % 2 == 0 &&
                        (whiteDVONNLayer[i] and bitmask) == 0UL &&
                        (whiteDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  whiteDVONNLayer[i] = safeAddAndCheck(whiteDVONNLayer[i], "White DVONN[$i]")
                  pieceRemoved = true
                }
                // place white on top of black
                if (
                    i % 2 == 1 &&
                        (blackDVONNLayer[i] and bitmask) == 0UL &&
                        (blackDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackDVONNLayer[i] = safeAddAndCheck(blackDVONNLayer[i], "Black DVONN[$i]")
                  pieceRemoved = true
                }
              }

              PieceType.PUNCT -> {
                // place white on top of black on top of white
                if (
                    i % 2 == 0 &&
                        (whitePUNCTLayer[i] and bitmask) == 0UL &&
                        (whitePUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  whitePUNCTLayer[i] = safeAddAndCheck(whitePUNCTLayer[i], "White PUNCT[$i]")
                  pieceRemoved = true
                }
                // place white on top of black
                if (
                    i % 2 == 1 &&
                        (blackPUNCTLayer[i] and bitmask) == 0UL &&
                        (blackPUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackPUNCTLayer[i] = safeAddAndCheck(blackPUNCTLayer[i], "Black PUNCT[$i]")
                  pieceRemoved = true
                }
              }
              else -> {}
            }
          }

          PlayerName.BLACK.name -> {
            when (piece.type) {
              PieceType.DVONN -> {
                // place black on top of white
                if (
                    i % 2 == 1 &&
                        (whiteDVONNLayer[i] and bitmask) == 0UL &&
                        (whiteDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  whiteDVONNLayer[i] = safeAddAndCheck(whiteDVONNLayer[i], "White DVONN[$i]")
                  pieceRemoved = true
                }
                // place black on top of white on top of black
                if (
                    i % 2 == 0 &&
                        (blackDVONNLayer[i] and bitmask) == 0UL &&
                        (blackDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackDVONNLayer[i] = safeAddAndCheck(blackDVONNLayer[i], "Black DVONN[$i]")
                  pieceRemoved = true
                }
              }

              PieceType.PUNCT -> {
                // place black on top of white
                if (
                    i % 2 == 1 &&
                        (whitePUNCTLayer[i] and bitmask) == 0UL &&
                        (whitePUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  whitePUNCTLayer[i] = safeAddAndCheck(whitePUNCTLayer[i], "White PUNCT[$i]")
                  pieceRemoved = true
                }
                // place black on top of white on top of black
                if (
                    i % 2 == 0 &&
                        (blackPUNCTLayer[i] and bitmask) == 0UL &&
                        (blackPUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackPUNCTLayer[i] = safeAddAndCheck(blackPUNCTLayer[i], "Black PUNCT[$i]")
                  pieceRemoved = true
                }
              }

              else -> {}
            }
          }
        }
      }
    } else {
      when (piece.colorName) {
        PlayerName.WHITE.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              whiteDVONNLayer[0] = safeAddAndCheck(whiteDVONNLayer[0], "White DVONN[0]")
            }

            PieceType.PUNCT -> {
              whitePUNCTLayer[0] = safeAddAndCheck(whitePUNCTLayer[0], "White PUNCT[0]")
            }

            else -> {}
          }
        }

        PlayerName.BLACK.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              blackDVONNLayer[0] = safeAddAndCheck(blackDVONNLayer[0], "Black DVONN[0]")
            }

            PieceType.PUNCT -> {
              blackPUNCTLayer[0] = safeAddAndCheck(blackPUNCTLayer[0], "Black PUNCT[0]")
            }

            else -> {}
          }
        }
      }
    }

    if (piece.potential) {
      when (piece.colorName) {
        PlayerName.WHITE.name -> {
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials")
        }

        PlayerName.BLACK.name -> {
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials")
        }
      }
    }
  }

	if (isDebugEnabled) {
		println("Bitboard State: ${Json.encodeToString<Bitboard>(this)}")
		println("--- UNDO REMOVE/RETRIEVE CAPTURED PIECES COMPLETED ---")
		println("=============================================")
	}
}

fun Bitboard.removeRetrieveAndCapturePiecesFromBitboard(
    retrievedCapturedPiecesBits: List<RetrievedCapturedPieceBit>
) {

  // --- 0. CONFIGURABLE DEBUGGING ---
  val isDebugEnabled = false
  if (isDebugEnabled) {
    println("--- REMOVE/RETRIEVE CAPTURED PIECES CALLED ---")
    println("Bitboard State: ${Json.encodeToString<Bitboard>(this)}")
	  println(retrievedCapturedPiecesBits)
    println("Processing ${retrievedCapturedPiecesBits.size} pieces...")
    retrievedCapturedPiecesBits.forEachIndexed { index, piece ->
      println("piece $index: $piece")
    }
  }

  retrievedCapturedPiecesBits.forEachIndexed { index, data ->
    val (retrievedPiece, capturedPiece, bitmask, isNeutralized, keepRetrievedPieceInPlay) = data
    val piece = retrievedPiece ?: capturedPiece

    if (piece == null) {
      if (isDebugEnabled)
          println("Item $index: Both retrieved and captured pieces are null. Skipping.")
      return@forEachIndexed
    }

    if (isDebugEnabled) {
      println("Processing Piece $index: ${piece.colorName} ${piece.type} at $bitmask")
      println("  -> isNeutralized: $isNeutralized | Potential: ${piece.potential}")
    }

    // --- 1. PRE-CONDITION CHECKS ---
    require(bitmask.countOneBits() == 1) {
      "CRITICAL ERROR: Removal bitmask must be exactly one bit. Got: $bitmask"
    }

    // --- LOCAL HELPER: BASE LAYER REMOVAL ---
    fun safeRemoveAndCheck(board: ULong, boardName: String): ULong {
      if (isDebugEnabled) {
        println(
            "$boardName Board before removing $boardName piece:\n" +
                "removeAtIndex: 0b${bitmask.toString(2).padStart(40, '0')}\n" +
                "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}"
        )
      }

      val wasIndexOccupied = (board and bitmask) == bitmask

      check(wasIndexOccupied) {
        """
          STATE ERROR:  ${piece.colorName} ${piece.type} Piece cannot be removed from unoccupied index ($bitmask) in $boardName: $board.
          Bitboard State: ${Json.encodeToString<Bitboard>(this)}
          Processing ${retrievedCapturedPiecesBits.size} pieces...
          ${retrievedCapturedPiecesBits.forEachIndexed { index, piece ->
            println("piece $index: $piece")
          }}
        """.trimIndent()
      }

      val newBoard = board and bitmask.inv()
      check(newBoard != board) {
        """
          Failed to remove ${piece.colorName} ${piece.type} at position $bitmask from $boardName: $board
          Bitboard State: ${Json.encodeToString<Bitboard>(this)}
          Processing ${retrievedCapturedPiecesBits.size} pieces...
          ${retrievedCapturedPiecesBits.forEachIndexed { index, piece ->
          println("piece $index: $piece")
        }}
        """.trimIndent()
      }
      if (isDebugEnabled) {
        println(
            "$boardName Board after removing $boardName piece\n" +
                "removeAtIndex: 0b${bitmask.toString(2).padStart(40, '0')}\n" +
                "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}"
        )
      }
      if (isDebugEnabled) println("  -> Successfully removed from $boardName.")
      return newBoard
    }

    // --- 2. REMOVE FROM PRIMARY BOARDS ---
    when (piece.colorName) {
      PlayerName.WHITE.name -> {
        when (piece.type) {
          PieceType.GIPF -> whiteGIPF = safeRemoveAndCheck(whiteGIPF, "White GIPF")
          PieceType.TAMSK -> whiteTAMSK = safeRemoveAndCheck(whiteTAMSK, "White TAMSK")
          PieceType.ZERTZ -> whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ")
          PieceType.YINSH -> whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH")
          else -> {} // DVONN and PUNCT handled below
        }
      }
      PlayerName.BLACK.name -> {
        when (piece.type) {
          PieceType.GIPF -> blackGIPF = safeRemoveAndCheck(blackGIPF, "Black GIPF")
          PieceType.TAMSK -> blackTAMSK = safeRemoveAndCheck(blackTAMSK, "Black TAMSK")
          PieceType.ZERTZ -> blackZERTZ = safeRemoveAndCheck(blackZERTZ, "Black ZERTZ")
          PieceType.YINSH -> blackYINSH = safeRemoveAndCheck(blackYINSH, "Black YINSH")
          else -> {} // DVONN and PUNCT handled below
        }
      }
    }

    // --- 3. HANDLE DVONN / PUNCT STACKS ---
    if (isNeutralized) {
      var pieceRemoved = false
      for (i in 5 downTo 1) {
        if (pieceRemoved) break

        when (piece.colorName) {
          PlayerName.WHITE.name -> {
            when (piece.type) {
              PieceType.DVONN -> {
                // place white on top of black on top of white
                if (
                    i % 2 == 0 &&
                        (whiteDVONNLayer[i] and bitmask) == bitmask &&
                        (whiteDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  whiteDVONNLayer[i] = safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN[$i]")
                  pieceRemoved = true
                }
                // place white on top of black
                if (
                    i % 2 == 1 &&
                        (blackDVONNLayer[i] and bitmask) == bitmask &&
                        (blackDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackDVONNLayer[i] = safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN[$i]")
                  pieceRemoved = true
                }
              }

              PieceType.PUNCT -> {

                // place white on top of black on top of white
                if (
                    i % 2 == 0 &&
                        (whitePUNCTLayer[i] and bitmask) == bitmask &&
                        (whitePUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  whitePUNCTLayer[i] = safeRemoveAndCheck(whitePUNCTLayer[i], "White PUNCT[$i]")
                  pieceRemoved = true
                }
                // place white on top of black
                if (
                    i % 2 == 1 &&
                        (blackPUNCTLayer[i] and bitmask) == bitmask &&
                        (blackPUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackPUNCTLayer[i] = safeRemoveAndCheck(blackPUNCTLayer[i], "Black PUNCT[$i]")
                  pieceRemoved = true
                }
              }

              else -> {}
            }
          }

          PlayerName.BLACK.name -> {
            when (piece.type) {
              PieceType.DVONN -> {

                // place black on top of white
                if (
                    i % 2 == 1 &&
                        (whiteDVONNLayer[i] and bitmask) == bitmask &&
                        (whiteDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  whiteDVONNLayer[i] = safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN[$i]")
                  pieceRemoved = true
                }
                // place black on top of white on top of black
                if (
                    i % 2 == 0 &&
                        (blackDVONNLayer[i] and bitmask) == bitmask &&
                        (blackDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackDVONNLayer[i] = safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN[$i]")
                  pieceRemoved = true
                }
              }

              PieceType.PUNCT -> {
                // place black on top of white
                if (
                    i % 2 == 1 &&
                        (whitePUNCTLayer[i] and bitmask) == bitmask &&
                        (whitePUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  whitePUNCTLayer[i] = safeRemoveAndCheck(whitePUNCTLayer[i], "White PUNCT[$i]")
                  pieceRemoved = true
                }
                // place black on top of white on top of black
                if (
                    i % 2 == 0 &&
                        (blackPUNCTLayer[i] and bitmask) == bitmask &&
                        (blackPUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackPUNCTLayer[i] = safeRemoveAndCheck(blackPUNCTLayer[i], "Black PUNCT[$i]")
                  pieceRemoved = true
                }
              }

              else -> {}
            }
          }
        }
      }
    } else {
      when (piece.colorName) {
        PlayerName.WHITE.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              whiteDVONNLayer[0] = safeRemoveAndCheck(whiteDVONNLayer[0], "White DVONN[0]")
            }

            PieceType.PUNCT -> {
              whitePUNCTLayer[0] = safeRemoveAndCheck(whitePUNCTLayer[0], "White PUNCT[0]")
            }

            else -> {}
          }
        }

        PlayerName.BLACK.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              blackDVONNLayer[0] = safeRemoveAndCheck(blackDVONNLayer[0], "Black DVONN[0]")
            }

            PieceType.PUNCT -> {
              blackPUNCTLayer[0] = safeRemoveAndCheck(blackPUNCTLayer[0], "Black PUNCT[0]")
            }

            else -> {}
          }
        }
      }
    }

    if (piece.potential) {
      when (piece.colorName) {
        PlayerName.WHITE.name -> {
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials")
        }

        PlayerName.BLACK.name -> {
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials")
        }
      }
    }
  }


  if (isDebugEnabled) println("--- REMOVE/RETRIEVE COMPLETED ---")
}

private fun Bitboard.createPiece(bitmask: ULong): Piece? {
  val color =
      if ((whitePieces and bitmask) == bitmask) {
        PlayerName.WHITE
      } else {
        //			if ((blackPieces and bitmask) == bitmask)
        PlayerName.BLACK
      }

  val isNeutralized =
      when (color) {
        PlayerName.WHITE -> {
          (whiteNeutralized and bitmask) == bitmask
        }

        PlayerName.BLACK -> {
          (blackNeutralized and bitmask) == bitmask
        }
      }

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
      when (color) {
        PlayerName.WHITE -> {
          if ((whiteGIPF and bitmask) == bitmask) PieceType.GIPF
          else if ((whiteTAMSK and bitmask) == bitmask) PieceType.TAMSK
          else if ((whiteZERTZ and bitmask) == bitmask) PieceType.ZERTZ
          else if ((whiteYINSH and bitmask) == bitmask) PieceType.YINSH
          else if ((whiteDVONNLayer[0] and bitmask) == bitmask) PieceType.DVONN
          else if ((whitePUNCTLayer[0] and bitmask) == bitmask) PieceType.PUNCT else null
        }

        PlayerName.BLACK -> {
          if ((blackGIPF and bitmask) == bitmask) PieceType.GIPF
          else if ((blackTAMSK and bitmask) == bitmask) PieceType.TAMSK
          else if ((blackZERTZ and bitmask) == bitmask) PieceType.ZERTZ
          else if ((blackYINSH and bitmask) == bitmask) PieceType.YINSH
          else if ((blackDVONNLayer[0] and bitmask) == bitmask) PieceType.DVONN
          else if ((blackPUNCTLayer[0] and bitmask) == bitmask) PieceType.PUNCT else null
        }
      }

  val stackedPieces: MutableList<Piece> =
      when (color) {
        PlayerName.WHITE -> {
          when (pieceType) {
            PieceType.GIPF,
            PieceType.TAMSK,
            PieceType.ZERTZ,
            PieceType.YINSH,
            null -> mutableListOf()

            PieceType.DVONN -> {
              whiteDVONNLayer
                  .mapIndexedNotNull { index, layer ->
                    if (index in 1..5 && (layer and bitmask) == bitmask) {
                      val pieceColor = if (index % 2 == 0) PlayerName.WHITE else PlayerName.BLACK
                      Piece(
                          abbreviation =
                              "${pieceColor.name.first()}${PieceType.DVONN.name.first()}",
                          potential = false,
                          colorName = pieceColor.name,
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
                    if (index in 1..5 && (layer and bitmask) == bitmask) {
                      val pieceColor = if (index % 2 == 0) PlayerName.WHITE else PlayerName.BLACK
                      Piece(
                          abbreviation =
                              "${pieceColor.name.first()}${PieceType.PUNCT.name.first()}",
                          potential = false,
                          colorName = pieceColor.name,
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
            PieceType.GIPF,
            PieceType.TAMSK,
            PieceType.ZERTZ,
            PieceType.YINSH,
            null -> mutableListOf()

            PieceType.DVONN -> {
              blackDVONNLayer
                  .mapIndexedNotNull { index, layer ->
                    if (index in 1..5 && (layer and bitmask) == bitmask) {
                      val pieceColor = if (index % 2 == 0) PlayerName.BLACK else PlayerName.WHITE
                      Piece(
                          abbreviation =
                              "${pieceColor.name.first()}${PieceType.DVONN.name.first()}",
                          potential = false,
                          colorName = pieceColor.name,
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
                    if (index in 1..5 && (layer and bitmask) == bitmask) {
                      val pieceColor = if (index % 2 == 0) PlayerName.BLACK else PlayerName.WHITE
                      Piece(
                          abbreviation =
                              "${pieceColor.name.first()}${PieceType.PUNCT.name.first()}",
                          potential = false,
                          colorName = pieceColor.name,
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

  //
  return pieceType?.let {
    Piece(
        abbreviation = "${color.name.first()}${pieceType.name.first()}",
        potential = hasPotential,
        colorName = color.name,
        type = it,
        isNeutralized = isNeutralized,
        stackedPieces = stackedPieces,
    )
  }
}

// Helper function to keep the i == 0 block clean
private fun createBasePiece(
    player: PlayerName,
    pieceType: PieceType,
    hasPotential: Boolean,
): Piece {
  return Piece(
      abbreviation = "${player.name.first()}${pieceType.name.first()}",
      potential = hasPotential,
      colorName = player.name,
      type = pieceType,
      isNeutralized = false,
      stackedPieces = mutableListOf(),
  )
}

fun Bitboard.identifyAvailableMoves(
    currentPlayer: Player,
    columnInfos: List<ColumnInfo>,
): List<PossibleBitMove> {
  // TODO PieceType.TAMSK logic is handled by isTamskPieceAtCenter()
  val isTamskPieceAtCenter = getTamskMoves(currentPlayer)

  val gipfPiecesInReserve: List<Piece> =
      currentPlayer.piecesInReserve
          .filter { piece -> piece.type == PieceType.GIPF }
          .distinctBy { piece -> piece.type }

  val playableStackedPiecesInReserve: List<Piece> =
      currentPlayer.piecesInReserve
          .filter { piece ->
            piece.type != PieceType.GIPF && piece.potential
          }
          .distinctBy { piece -> piece.type }

  val eligibleMovesUsingPotential =
      getZertzMoves(currentPlayer, columnInfos) +
          getYinshMoves(currentPlayer, columnInfos) +
          getDvonnMoves(currentPlayer, columnInfos) +
          getPunctMoves(currentPlayer, columnInfos)

  // Build a list of all available moves
  val allAvailableMoves: MutableList<PossibleBitMove> = mutableListOf()

  playableStackedPiecesInReserve.forEach { piece ->
    allAvailableMoves.add(
        PossibleBitMove(
            piece = piece,
            columnInfos = vacantLines,
            moveType = MoveType.AddPiece,
        )
    )
  }

  if (gipfPiecesInReserve.isNotEmpty()) {
    return gipfPiecesInReserve.map { piece ->
      PossibleBitMove(
          piece = piece,
          columnInfos = vacantLines,
          moveType = MoveType.AddPiece,
          pieceType = PieceType.GIPF,
          pieceColor = currentPlayer.name,
      )
    }
  } else if (isTamskPieceAtCenter != null) {

    /**
     * TODO rewrite Check check(piece?.potential == false) { // val pieceCoords =
     * selectedNode.node.coordinate.let { "${it.column}${it.row}" } val currentPotential =
     * piece?.potential
     *
     * // "Invalid piece state at $pieceCoords: Expected piece potential to be spent (false), " // +
     * "but found potential status is: $currentPotential (Piece Type: ${piece?.type?.name}, Color:
     * ${piece?.colorName})" }
     */
    return listOf(isTamskPieceAtCenter)
  } else if (allAvailableMoves.isNotEmpty() || eligibleMovesUsingPotential.isNotEmpty()) {
    return allAvailableMoves + eligibleMovesUsingPotential
  } else {
    println("Player ${currentPlayer.name} has no available moves left!")
    return emptyList()
    //		emptyList<PossibleMove>()
  }
}

fun Bitboard.assertPieceCount(
    EXPECTED_TOTAL: Int = 66 / 2,
    MAXIMUM_PIECES: Int = 66,
    currentPlayer: Player,
    nextPlayer: Player,
) {

  currentPlayer.piecesInReserve.sortBy { it.type }
  nextPlayer.piecesInReserve.sortBy { it.type }

  // 1. Next Player's components
  var nextReservePotentials =
      nextPlayer.piecesInReserve.count { it.colorName == PlayerName.BLACK.name && it.potential } * 2
  var nextReserveBasics =
      nextPlayer.piecesInReserve.count { it.colorName == PlayerName.BLACK.name && !it.potential }
  var nextCapturedPotentials =
      nextPlayer.capturedPieces.count { it.colorName == PlayerName.BLACK.name && it.potential } * 2
  var nextCapturedBasics =
      nextPlayer.capturedPieces.count { it.colorName == PlayerName.BLACK.name && !it.potential }

  // 2. Current Player's components
  var currentReservePotentials =
      currentPlayer.piecesInReserve.count {
        it.colorName == PlayerName.BLACK.name && it.potential
      } * 2
  var currentReserveBasics =
      currentPlayer.piecesInReserve.count { it.colorName == PlayerName.BLACK.name && !it.potential }
  var currentCapturedPotentials =
      currentPlayer.capturedPieces.count { it.colorName == PlayerName.BLACK.name && it.potential } *
          2
  var currentCapturedBasics =
      currentPlayer.capturedPieces.count { it.colorName == PlayerName.BLACK.name && !it.potential }

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
          blackPUNCTLayer[0].countOneBits() +
          blackPUNCTLayer[2].countOneBits() +
          blackPUNCTLayer[4].countOneBits() +
          whiteDVONNLayer[1].countOneBits() +
          whiteDVONNLayer[3].countOneBits() +
          whiteDVONNLayer[5].countOneBits() +
          whitePUNCTLayer[1].countOneBits() +
          whitePUNCTLayer[3].countOneBits() +
          whitePUNCTLayer[5].countOneBits()

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
    println("oooooooooooooo")
  }

  check(totalBlackPieces == EXPECTED_TOTAL) {
    val nextReserveRaw = nextPlayer.piecesInReserve.count { it.colorName == PlayerName.BLACK.name && it.potential }
    val nextCapturedRaw = nextPlayer.capturedPieces.count { it.colorName == PlayerName.BLACK.name && it.potential }
    val currentReserveRaw = currentPlayer.piecesInReserve.count { it.colorName == PlayerName.BLACK.name && it.potential }
    val currentCapturedRaw = currentPlayer.capturedPieces.count { it.colorName == PlayerName.BLACK.name && it.potential }

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
    ======================================================================
    """.trimIndent()
  }

  // 1. Next Player's components
  nextReservePotentials =
      nextPlayer.piecesInReserve.count { it.colorName == PlayerName.WHITE.name && it.potential } * 2
  nextReserveBasics =
      nextPlayer.piecesInReserve.count { it.colorName == PlayerName.WHITE.name && !it.potential }
  nextCapturedPotentials =
      nextPlayer.capturedPieces.count { it.colorName == PlayerName.WHITE.name && it.potential } * 2
  nextCapturedBasics =
      nextPlayer.capturedPieces.count { it.colorName == PlayerName.WHITE.name && !it.potential }

  // 2. Current Player's components
  currentReservePotentials =
      currentPlayer.piecesInReserve.count {
        it.colorName == PlayerName.WHITE.name && it.potential
      } * 2
  currentReserveBasics =
      currentPlayer.piecesInReserve.count { it.colorName == PlayerName.WHITE.name && !it.potential }
  currentCapturedPotentials =
      currentPlayer.capturedPieces.count { it.colorName == PlayerName.WHITE.name && it.potential } *
          2
  currentCapturedBasics =
      currentPlayer.capturedPieces.count { it.colorName == PlayerName.WHITE.name && !it.potential }

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
          whitePUNCTLayer[0].countOneBits() +
          whitePUNCTLayer[2].countOneBits() +
          whitePUNCTLayer[4].countOneBits() +
          blackDVONNLayer[1].countOneBits() +
          blackDVONNLayer[3].countOneBits() +
          blackDVONNLayer[5].countOneBits() +
          blackPUNCTLayer[1].countOneBits() +
          blackPUNCTLayer[3].countOneBits() +
          blackPUNCTLayer[5].countOneBits()

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
    println("oooooooooooooo")
  }

  check(totalWhitePieces == EXPECTED_TOTAL) {
    val nextReserveRaw = nextPlayer.piecesInReserve.count { it.colorName == PlayerName.WHITE.name && it.potential }
    val nextCapturedRaw = nextPlayer.capturedPieces.count { it.colorName == PlayerName.WHITE.name && it.potential }
    val currentReserveRaw = currentPlayer.piecesInReserve.count { it.colorName == PlayerName.WHITE.name && it.potential }
    val currentCapturedRaw = currentPlayer.capturedPieces.count { it.colorName == PlayerName.WHITE.name && it.potential }

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

      ======================================================================
      """.trimIndent()
  }

  val totalPieces = totalBlackPieces + totalWhitePieces
  check(totalPieces == MAXIMUM_PIECES) {
    "Game Piece Desynchronization: Total pieces in play ($totalPieces) exceeds the maximum piece count ($MAXIMUM_PIECES). " +
        "Pieces have been illegally spawned or deleted." +
        "\nGame State: \n${Json.encodeToString(this)}"
  }
}
