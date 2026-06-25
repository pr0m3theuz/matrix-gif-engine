@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import kotlin.collections.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.engine.MoveType
import org.example.engine.PossibleBitMove

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
              (0 until arr.size - 1).map { k ->
                1UL shl arr[k]
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
                  (0 until arr.size - 1).map { k ->
                    1UL shl arr[k]
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
                  (0 until arr.size - 1).map { k ->
                    1UL shl arr[k]
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
    var whiteYINCH: ULong = 0UL,
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
    var blackYINCH: ULong = 0UL,
    var blackZERTZ: ULong = 0UL,
    var blackPotentials: ULong = 0UL,
) {

  // region From Gemini TODO
  val globalOccupancy: ULong
    get() {
      var occupied =
          whiteGIPF or
              whiteTAMSK or
              whiteYINCH or
              whiteZERTZ or
              whitePotentials or
              whiteNeutralized or
              blackGIPF or
              blackTAMSK or
              blackYINCH or
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
              whiteYINCH or
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
              blackYINCH or
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
    whiteYINCH = shiftBoard(whiteYINCH, fromMask, toMask)
    whiteZERTZ = shiftBoard(whiteZERTZ, fromMask, toMask)
    whitePotentials = shiftBoard(whitePotentials, fromMask, toMask)
    blackGIPF = shiftBoard(blackGIPF, fromMask, toMask)
    blackTAMSK = shiftBoard(blackTAMSK, fromMask, toMask)
    blackYINCH = shiftBoard(blackYINCH, fromMask, toMask)
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
    if (whiteYINCH != other.whiteYINCH) return false
    if (whiteZERTZ != other.whiteZERTZ) return false
    if (whitePotentials != other.whitePotentials) return false
    if (whiteNeutralized != other.whiteNeutralized) return false
    if (blackGIPF != other.blackGIPF) return false
    if (!blackDVONNLayer.contentEquals(other.blackDVONNLayer)) return false
    if (!blackPUNCTLayer.contentEquals(other.blackPUNCTLayer)) return false
    if (blackTAMSK != other.blackTAMSK) return false
    if (blackYINCH != other.blackYINCH) return false
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
    result = 31 * result + whiteYINCH.hashCode()
    result = 31 * result + whiteZERTZ.hashCode()
    result = 31 * result + whitePotentials.hashCode()
    result = 31 * result + whiteNeutralized.hashCode()
    result = 31 * result + blackGIPF.hashCode()
    result = 31 * result + blackDVONNLayer.contentHashCode()
    result = 31 * result + blackPUNCTLayer.contentHashCode()
    result = 31 * result + blackTAMSK.hashCode()
    result = 31 * result + blackYINCH.hashCode()
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
  //
  val occupiedSpots = globalOccupancy
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()
  var gapFound = false
  var vacantBitFound = 0UL

  // 1. DISCOVERY: Find out exactly which pieces are shifting
  for ((fromMask, toMask) in col.shiftPairs) {
    if ((occupiedSpots and fromMask) != 0UL) {
      movesToApply.add(fromMask to toMask)
    } else {
      gapFound = true
      vacantBitFound = fromMask
      break
    }
  }

  // 2. VALIDATION: Check if pieces fall off the board
  if (!gapFound) {
    check(!gapFound) {
      "Cannot shift: board is full"
    }
  }

  // 3. MODIFICATION: Apply the shifts in REVERSE to prevent teleportation bugs
  // mutates in place
  // move the last piece first
  movesToApply.asReversed().forEach { (fromMask, toMask) ->
    applyShiftToAll(fromMask, toMask)
  }

  return vacantBitFound
}

fun Bitboard.executePushDown(col: ColumnInfo): ULong {
  //
  val occupiedSpots = globalOccupancy
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()
  var gapFound = false
  var vacantBitFound = 0UL

  // 1. DISCOVERY: Find out exactly which pieces are shifting
  for ((toMask, fromMask) in col.shiftPairs.asReversed()) {
    if ((occupiedSpots and fromMask) != 0UL) {
      movesToApply.add(fromMask to toMask)
    } else {
      gapFound = true
      vacantBitFound = fromMask
      break
    }
  }

  // 2. VALIDATION: Check if pieces fall off the board
  if (!gapFound) {
    check(!gapFound) {
      "Cannot shift: board is full"
    }
  }

  // 3. MODIFICATION: Apply the shifts in REVERSE to prevent teleportation bugs
  movesToApply.asReversed().forEach { (fromMask, toMask) ->
    applyShiftToAll(fromMask, toMask)
  }

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
      whiteYINCH = whiteYINCH,
      whiteDVONNLayer = whiteDVONNLayer,
      whitePUNCTLayer = whitePUNCTLayer,
      whitePotentials = whitePotentials,
      blackGIPF = blackGIPF,
      blackTAMSK = blackTAMSK,
      blackYINCH = blackYINCH,
      blackZERTZ = blackZERTZ,
      blackDVONNLayer = blackDVONNLayer,
      blackPUNCTLayer = blackPUNCTLayer,
      blackPotentials = blackPotentials,
  )
}

fun Bitboard.convertBitboardToBoard(board: Board): Board {
  val nodes = mutableListOf<Node>()

  val newBoard = board.deepCopy()

  for (bit in 0..40) {
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
            else if ((whiteYINCH and bitmask) == bitmask) PieceType.YINSH
            else if ((whiteDVONNLayer[0] and bitmask) == bitmask) PieceType.DVONN
            else if ((whitePUNCTLayer[0] and bitmask) == bitmask) PieceType.PUNCT else null
          }

          PlayerName.BLACK -> {
            if ((blackGIPF and bitmask) == bitmask) PieceType.GIPF
            else if ((blackTAMSK and bitmask) == bitmask) PieceType.TAMSK
            else if ((blackZERTZ and bitmask) == bitmask) PieceType.ZERTZ
            else if ((blackYINCH and bitmask) == bitmask) PieceType.YINSH
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

  check((addAtIndex and openningSpotsLineMask) == addAtIndex) {
    val newPieceBinary = addAtIndex.toString(2).padStart(40, '0')
    val openingLineBinary = openningSpotsLineMask.toString(2).padStart(40, '0')

    "Bitmask Validation Failure: The new piece mask contains bits outside the allowed opening line mask.\n" +
        "New Piece Mask:   $newPieceBinary\n" +
        "Opening Line Mask: $openingLineBinary"
  }

  // TODO is addAtIndex occupied
  val isIndexOccupied = (globalOccupancy and addAtIndex) > 0UL
  var vacantBitFound = 0UL
  if (isIndexOccupied) {
    //    TODO("Implement Shift Pieces")
    when (pushDirection) {
      // push up based on bit order
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        vacantBitFound = executePushUp(col)
      }
      // push down based on bit order
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        vacantBitFound = executePushDown(col)
      }
    }
  }

  // TODO which bitboards to add piece
  when (piece.colorName) {
    PlayerName.WHITE.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          whiteGIPF = whiteGIPF or addAtIndex
        }

        PieceType.TAMSK -> {
          whiteTAMSK = whiteTAMSK or addAtIndex
          whitePotentials = whitePotentials or addAtIndex
        }

        PieceType.ZERTZ -> {
          whiteZERTZ = whiteZERTZ or addAtIndex
          whitePotentials = whitePotentials or addAtIndex
        }

        PieceType.DVONN -> {
          whiteDVONNLayer[0] = whiteDVONNLayer[0] or addAtIndex
          whitePotentials = whitePotentials or addAtIndex
        }

        PieceType.YINSH -> {
          whiteYINCH = whiteYINCH or addAtIndex
          whitePotentials = whitePotentials or addAtIndex
        }

        PieceType.PUNCT -> {
          whitePUNCTLayer[0] = whitePUNCTLayer[0] or addAtIndex
          whitePotentials = whitePotentials or addAtIndex
        }
      }
    }

    PlayerName.BLACK.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          blackGIPF = blackGIPF or addAtIndex
        }

        PieceType.TAMSK -> {
          blackTAMSK = blackTAMSK or addAtIndex
          blackPotentials = blackPotentials or addAtIndex
        }

        PieceType.ZERTZ -> {
          blackZERTZ = blackZERTZ or addAtIndex
          blackPotentials = blackPotentials or addAtIndex
        }

        PieceType.DVONN -> {
          blackDVONNLayer[0] = blackDVONNLayer[0] or addAtIndex
          blackPotentials = blackPotentials or addAtIndex
        }

        PieceType.YINSH -> {
          blackYINCH = blackYINCH or addAtIndex
          blackPotentials = blackPotentials or addAtIndex
        }

        PieceType.PUNCT -> {
          blackPUNCTLayer[0] = blackPUNCTLayer[0] or addAtIndex
          blackPotentials = blackPotentials or addAtIndex
        }
      }
    }
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

  // 1. Revert the bitboard additions
  // Note: Mirroring your use of `+`, we use `-` here.
  // Alternatively, you could use bitwise AND NOT: `whiteGIPF and removeAtIndex.inv()`
  when (piece.colorName) {
    PlayerName.WHITE.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          whiteGIPF = whiteGIPF and removeAtIndex.inv()
        }

        PieceType.TAMSK -> {
          whiteTAMSK = whiteTAMSK and removeAtIndex.inv()
          whitePotentials = whitePotentials and removeAtIndex.inv()
        }

        PieceType.ZERTZ -> {
          whiteZERTZ = whiteZERTZ and removeAtIndex.inv()
          whitePotentials = whitePotentials and removeAtIndex.inv()
        }

        PieceType.DVONN -> {
          whiteDVONNLayer[0] = whiteDVONNLayer[0] and removeAtIndex.inv()
          whitePotentials = whitePotentials and removeAtIndex.inv()
        }

        PieceType.YINSH -> {
          whiteYINCH = whiteYINCH and removeAtIndex.inv()
          whitePotentials = whitePotentials and removeAtIndex.inv()
        }

        PieceType.PUNCT -> {
          whitePUNCTLayer[0] = whitePUNCTLayer[0] and removeAtIndex.inv()
          whitePotentials = whitePotentials and removeAtIndex.inv()
        }
      }
    }

    PlayerName.BLACK.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          blackGIPF = blackGIPF and removeAtIndex.inv()
        }

        PieceType.TAMSK -> {
          blackTAMSK = blackTAMSK and removeAtIndex.inv()
          blackPotentials = blackPotentials and removeAtIndex.inv()
        }

        PieceType.ZERTZ -> {
          blackZERTZ = blackZERTZ and removeAtIndex.inv()
          blackPotentials = blackPotentials and removeAtIndex.inv()
        }

        PieceType.DVONN -> {
          blackDVONNLayer[0] = blackDVONNLayer[0] and removeAtIndex.inv()
          blackPotentials = blackPotentials and removeAtIndex.inv()
        }

        PieceType.YINSH -> {
          blackYINCH = blackYINCH and removeAtIndex.inv()
          blackPotentials = blackPotentials and removeAtIndex.inv()
        }

        PieceType.PUNCT -> {
          blackPUNCTLayer[0] = blackPUNCTLayer[0] and removeAtIndex.inv()
          blackPotentials = blackPotentials and removeAtIndex.inv()
        }
      }
    }
  }

  // 2. Revert the shifts (if a shift occurred during the add phase)
  if (wasIndexOccupied) {
    when (pushDirection) {
      // If the original move pushed UP, we must pull DOWN to undo
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        executePullDown(col, vacantBitFound)
      }

      // If the original move pushed DOWN, we must pull UP to undo
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        executePullUp(col, vacantBitFound)
      }
    }
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

          // TODO call addPieceToBitboard
          whiteTAMSK = whiteTAMSK or possibleBitMove.targetBit
          whitePotentials = whitePotentials and boardCenterSpotMask.inv()
        }

        PieceType.ZERTZ -> {
          whiteZERTZ = whiteZERTZ or possibleBitMove.targetBit
          whitePotentials = whitePotentials and possibleBitMove.sourceBit.inv()
        }

        PieceType.YINSH -> {
          whiteYINCH = whiteYINCH or possibleBitMove.targetBit
          whitePotentials = whitePotentials and possibleBitMove.sourceBit.inv()
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
              whiteDVONNLayer[i] = whiteDVONNLayer[i] or possibleBitMove.targetBit
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackDVONNLayer[i] = blackDVONNLayer[i] or possibleBitMove.targetBit
              break
            }
          }

          whitePotentials = whitePotentials and possibleBitMove.sourceBit.inv()
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
              whitePUNCTLayer[i] = whitePUNCTLayer[i] or possibleBitMove.targetBit
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackPUNCTLayer[i] = blackPUNCTLayer[i] or possibleBitMove.targetBit
              break
            }
          }

          whitePotentials = whitePotentials and possibleBitMove.sourceBit.inv()
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

          blackTAMSK = blackTAMSK or possibleBitMove.targetBit
          blackPotentials = blackPotentials and boardCenterSpotMask.inv()
        }

        PieceType.ZERTZ -> {
          blackZERTZ = blackZERTZ or possibleBitMove.targetBit
          blackPotentials = blackPotentials and possibleBitMove.sourceBit.inv()
        }

        PieceType.YINSH -> {
          blackYINCH = blackYINCH or possibleBitMove.targetBit
          blackPotentials = blackPotentials and possibleBitMove.sourceBit.inv()
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
              whiteDVONNLayer[i] = whiteDVONNLayer[i] or possibleBitMove.targetBit
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackDVONNLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackDVONNLayer[i] = blackDVONNLayer[i] or possibleBitMove.targetBit
              break
            }
          }

          blackPotentials = blackPotentials and possibleBitMove.sourceBit.inv()
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
              whitePUNCTLayer[i] = whitePUNCTLayer[i] or possibleBitMove.targetBit
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackPUNCTLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackPUNCTLayer[i] = blackPUNCTLayer[i] or possibleBitMove.targetBit
              break
            }
          }

          blackPotentials = blackPotentials and possibleBitMove.sourceBit.inv()
        }
      }
    }
  }
}

fun Bitboard.undoUsePiecePotential(possibleBitMove: PossibleBitMove) {
  // TODO filter for legal moves before calling this
  //  pieces can not be neutralized

  require(possibleBitMove.pieceType != null) { "No piece was selected!" }
  require(possibleBitMove.pieceColor != null) { "No piece was selected!" }
  require(possibleBitMove.sourceBit != null) { "No piece was selected!" }
  require(possibleBitMove.targetBit != null) { "No piece was selected!" }

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
          whiteZERTZ = whiteZERTZ and possibleBitMove.targetBit.inv()
          whitePotentials = whitePotentials or possibleBitMove.sourceBit
        }

        PieceType.YINSH -> {
          whiteYINCH = whiteYINCH and possibleBitMove.targetBit.inv()
          whitePotentials = whitePotentials or possibleBitMove.sourceBit
        }

        // TODO something about this seems wrong
        PieceType.DVONN -> {
          for (i in 5 downTo 1) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whiteDVONNLayer[i] and possibleBitMove.targetBit) == possibleBitMove.targetBit
            ) {
              whiteDVONNLayer[i] = whiteDVONNLayer[i] and possibleBitMove.targetBit.inv()
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              blackDVONNLayer[i] = blackDVONNLayer[i] and possibleBitMove.targetBit.inv()
              break
            }
          }

          whitePotentials = whitePotentials or possibleBitMove.sourceBit
        }

        PieceType.PUNCT -> {
          for (i in 5 downTo 1) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whitePUNCTLayer[i] and possibleBitMove.targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and possibleBitMove.targetBit) ==
                        possibleBitMove.targetBit
            ) {
              whitePUNCTLayer[i] = whitePUNCTLayer[i] and possibleBitMove.targetBit.inv()
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and possibleBitMove.targetBit) == possibleBitMove.targetBit
            ) {
              blackPUNCTLayer[i] = blackPUNCTLayer[i] and possibleBitMove.targetBit.inv()
              break
            }
          }

          whitePotentials = whitePotentials or possibleBitMove.sourceBit
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
          blackPotentials = blackPotentials or possibleBitMove.sourceBit
        }

        PieceType.YINSH -> {
          blackYINCH = blackYINCH and possibleBitMove.targetBit.inv()
          blackPotentials = blackPotentials or possibleBitMove.sourceBit
        }

        // TODO confirm if possibleBitMove.targetBit/Spot if is white
        // TODO something about this seems wrong
        PieceType.DVONN -> {

          for (i in 5 downTo 1) {
            // place white on top of black
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whiteDVONNLayer[i] and possibleBitMove.targetBit) == possibleBitMove.targetBit
            //							(whiteDVONNLayer[i - 1] and possibleBitMove.targetBit) ==
            // possibleBitMove.targetBit
            ) {
              whiteDVONNLayer[i] = whiteDVONNLayer[i] and possibleBitMove.targetBit.inv()
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackDVONNLayer[i] and possibleBitMove.targetBit) == possibleBitMove.targetBit
            ) {
              blackDVONNLayer[i] = blackDVONNLayer[i] and possibleBitMove.targetBit.inv()
              break
            }
          }

          blackPotentials = blackPotentials or possibleBitMove.sourceBit
        }

        PieceType.PUNCT -> {
          for (i in 5 downTo 1) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whitePUNCTLayer[i] and possibleBitMove.targetBit) == possibleBitMove.targetBit
            ) {
              whitePUNCTLayer[i] = whitePUNCTLayer[i] or possibleBitMove.targetBit
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackPUNCTLayer[i] and possibleBitMove.targetBit) == possibleBitMove.targetBit
            ) {
              blackPUNCTLayer[i] = blackPUNCTLayer[i] and possibleBitMove.targetBit.inv()
              break
            }
          }

          blackPotentials = blackPotentials or possibleBitMove.sourceBit
        }
      }
    }
  }
}

fun Bitboard.getTamskMoves(player: Player): PossibleBitMove? {
  val tamskPieceAtCenter =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteTAMSK and boardCenterSpotMask and whitePotentials
        }

        PlayerName.BLACK -> {
          blackTAMSK and boardCenterSpotMask and blackPotentials
        }
      }

  // no valid moves were found
  if (tamskPieceAtCenter == 0UL) return null

  return PossibleBitMove(
      sourceBit = tamskPieceAtCenter,
      columnInfos = vacantLines,
      pieceType = PieceType.TAMSK,
      pieceColor = player.name,
      moveType = MoveType.AddPiece,
  )
} // TODO figure out where to place Tamsk piece (col.positions.first() or col.positions.last()) and

// direction to push (start: Push Up, end: push down)

fun Bitboard.useTamskPotential(
    player: Player,
    sourceIndex: ULong,
    targetIndex: ULong,
    col: ColumnInfo,
    pushDirection: PushDirection,
): ULong {
  val isSourceValid = (sourceIndex and boardCenterSpotMask) == boardCenterSpotMask
  val isTargetValid = (targetIndex and openningSpotsLineMask) == targetIndex

  check(isSourceValid && isTargetValid) {
    buildString {
      appendLine("Illegal TAMSK Opening Move:")
      if (!isSourceValid) {
        appendLine(
            "  -> Source Violation: Source index (0b${
                        sourceIndex.toString(2).padStart(40, '0')
                    }) must be a White TAMSK piece located on the board center spot (0b${
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

  val isIndexOccupied = (globalOccupancy and targetIndex) > 0UL
  var vacantBitFound = 0UL
  if (isIndexOccupied) {
    when (pushDirection) {
      // push up based on bit order
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        vacantBitFound = executePushUp(col)
      }
      // push down based on bit order
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        vacantBitFound = executePushDown(col)
      }
    }
  }

  when (player.name) {
    PlayerName.WHITE -> {
      whiteTAMSK = whiteTAMSK or targetIndex
      whitePotentials = whitePotentials and boardCenterSpotMask.inv()
    }

    PlayerName.BLACK -> {
      blackTAMSK = blackTAMSK or targetIndex
      blackPotentials = blackPotentials and boardCenterSpotMask.inv()
    }
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
  val isSourceValid = (sourceIndex and boardCenterSpotMask) == boardCenterSpotMask
  val isTargetValid = (removeAtIndex and openningSpotsLineMask) == removeAtIndex

  check(isSourceValid && isTargetValid) {
    buildString {
      appendLine("Illegal TAMSK Opening Move:")
      if (!isSourceValid) {
        appendLine(
            "  -> Source Violation: Source index (0b${
						sourceIndex.toString(2).padStart(40, '0')
					}) must be a White TAMSK piece located on the board center spot (0b${
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

  when (player.name) {
    PlayerName.WHITE -> {
      whiteTAMSK = whiteTAMSK and removeAtIndex.inv()
      whitePotentials = whitePotentials or sourceIndex
    }

    PlayerName.BLACK -> {
      blackTAMSK = blackTAMSK and removeAtIndex.inv()
      blackPotentials = blackPotentials or sourceIndex
    }
  }

  if (wasIndexOccupied) {
    when (pushDirection) {
      // If the original move pushed UP, we must pull DOWN to undo
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        executePullDown(col, vacantBitFound)
      }

      // If the original move pushed DOWN, we must pull UP to undo
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        executePullUp(col, vacantBitFound)
      }
    }
  }

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

fun Bitboard.getYinchMoves(
    player: Player,
    columnInfos: List<ColumnInfo>,
): MutableList<PossibleBitMove> {
  val validMoves = mutableListOf<PossibleBitMove>()

  val yinchPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteYINCH and whitePotentials
        }

        PlayerName.BLACK -> {
          blackYINCH and blackPotentials
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

        else -> {
          // no valid moves were found
          return validMoves
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
  val playerPieces =
      when (player.name) {
        PlayerName.WHITE -> whitePieces
        PlayerName.BLACK -> blackPieces
      }

  return columnInfos.filter { column ->
    if ((column.columnMask and playerPieces).countOneBits() < 4) {
      false
    } else {
      column.positions.windowed(4).any { sublist ->
        sublist.all { position -> (position and playerPieces) == position }
      }
    }
  }
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
              (whitePotentials and whiteNeutralized.inv() and bitmask) == bitmask
            }

            PlayerName.BLACK -> {
              (blackPotentials and blackNeutralized.inv() and bitmask) == bitmask
            }
          }
      result
    }
  }

  return playerPiecesWithPotential.flatten()
}

fun Bitboard.createPlayerPiecesWithPotentialPowerset(potentials: List<ULong>): List<List<ULong>> {
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

fun Bitboard.retrieveAndCapturePieces(
    columnInfos: List<ColumnInfo>,
    selectedPiecesWithPotentialPowerset: List<ULong>,
    player: Player,
): List<RetrievedCapturedPieceBit> {
  // TODO create pieces for retrieval and capturing
  // TODO return retrieved and captured pieces
  require(columnInfos.isNotEmpty()) { "There must be at least one column" }

  val playerPiecesWithoutPotential = columnInfos.map { column ->
    column.positions.filter { bitmask ->
      val result =
          when (player.name) {
            PlayerName.WHITE -> {
              // pieces without potential and not neutralized
              (whitePotentials.inv() and whiteNeutralized.inv() and bitmask) == bitmask
            }

            PlayerName.BLACK -> {
              (blackPotentials.inv() and blackNeutralized.inv() and bitmask) == bitmask
            }
          }
      result
    }
  }

  val opponentPiecesAndNotNeutralized = columnInfos.map { column ->
    column.positions.filter { bitmask ->
      val result =
          when (player.name) {
            PlayerName.WHITE -> {
              // opponent pieces and not neutralized
              (blackPieces and blackNeutralized.inv() and bitmask) == bitmask
            }

            PlayerName.BLACK -> {
              (whitePieces and whiteNeutralized.inv() and bitmask) == bitmask
            }
          }
      result
    }
  }

  // stacked DVONN and PUNCT Pieces
  val neutralizedBitmasks =
      columnInfos
          .map { column ->
            column.positions.filter { bitmask ->
              (whiteNeutralized and blackNeutralized and bitmask) == bitmask
            }
          }
          .flatten()

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
                    retrievedPiece = if (baseColor == finalColor) piece else null,
                    capturedPiece = if (baseColor != finalColor) piece else null,
                    bitmask = bitmask,
                    isNeutralized = true,
                )
            )

            blackDVONNLayer[i] = blackDVONNLayer[i] and bitmask.inv()
            whiteDVONNLayer[i] = whiteDVONNLayer[i] and bitmask.inv()
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
                    retrievedPiece = if (baseColor == finalColor) piece else null,
                    capturedPiece = if (baseColor != finalColor) piece else null,
                    bitmask = bitmask,
                    isNeutralized = true,
                )
            )

            blackPUNCTLayer[i] = blackPUNCTLayer[i] and bitmask.inv()
            whitePUNCTLayer[i] = whitePUNCTLayer[i] and bitmask.inv()
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
  return playerPiecesWithoutPotential
      .plus(opponentPiecesAndNotNeutralized)
      .flatten()
      .plus(selectedPiecesWithPotentialPowerset)
      .map { bitmask ->
        // construct piece and place it on the matching node
        var piece = createPiece(bitmask)

        if (piece != null) {
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
              whiteYINCH = whiteYINCH and bitmask.inv()
              blackYINCH = blackYINCH and bitmask.inv()
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
        }

        if (piece != null && piece.potential) {
          whitePotentials = whitePotentials and bitmask.inv()
          blackPotentials = blackPotentials and bitmask.inv()
        }

        RetrievedCapturedPieceBit(
            retrievedPiece = if (piece?.colorName == player.name.name) piece else null,
            capturedPiece = if (piece?.colorName != player.name.name) piece else null,
            bitmask = bitmask,
            isNeutralized = false,
        )
      }
      .plus(neutralizedPieces)
}

fun Bitboard.getsSelectedPiecesWithPotentialPowerset(
    selectedPiecesWithPotentialPowerset: List<ULong>,
    player: Player,
): List<RetrievedCapturedPieceBit> {
  return selectedPiecesWithPotentialPowerset.map { bitmask ->
    // construct piece and place it on the matching node
    var piece = createPiece(bitmask)

    if (piece != null) {
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
          whiteYINCH = whiteYINCH and bitmask.inv()
          blackYINCH = blackYINCH and bitmask.inv()
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
    }

    if (piece != null && piece.potential) {
      whitePotentials = whitePotentials and bitmask.inv()
      blackPotentials = blackPotentials and bitmask.inv()
    }

    check(piece?.colorName == player.name.name)

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

  retrievedCapturedPiecesBits.forEach {
      (retrievedPiece, capturedPiece, bitmask, isNeutralized, keepRetrievedPieceInPlay) ->
    val piece = retrievedPiece ?: capturedPiece

    when (piece?.colorName) {
      PlayerName.WHITE.name -> {
        when (piece.type) {
          PieceType.GIPF -> {
            whiteGIPF = whiteGIPF or bitmask
          }

          PieceType.TAMSK -> {
            whiteTAMSK = whiteTAMSK or bitmask
          }

          PieceType.ZERTZ -> {
            whiteZERTZ = whiteZERTZ or bitmask
          }

          PieceType.YINSH -> {
            whiteYINCH = whiteYINCH or bitmask
          }

          else -> {}
        }
      }

      PlayerName.BLACK.name -> {
        when (piece.type) {
          PieceType.GIPF -> {
            blackGIPF = blackGIPF or bitmask
          }

          PieceType.TAMSK -> {
            blackTAMSK = blackTAMSK or bitmask
          }

          PieceType.ZERTZ -> {
            blackZERTZ = blackZERTZ or bitmask
          }

          PieceType.YINSH -> {
            blackYINCH = blackYINCH or bitmask
          }

          else -> {}
        }
      }
    }

    if (isNeutralized) {
      when (piece?.colorName) {
        PlayerName.WHITE.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              for (i in 1..5) {
                // place white on top of black on top of white
                if (
                    i % 2 == 0 &&
                        (whiteDVONNLayer[i] and bitmask) == 0UL &&
                        (whiteDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  whiteDVONNLayer[i] = whiteDVONNLayer[i] or bitmask
                  break
                }
                // place white on top of black
                if (
                    i % 2 == 1 &&
                        (blackDVONNLayer[i] and bitmask) == 0UL &&
                        (blackDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackDVONNLayer[i] = blackDVONNLayer[i] or bitmask
                  break
                }
              }
            }

            PieceType.PUNCT -> {
              for (i in 1..5) {
                // place white on top of black on top of white
                if (
                    i % 2 == 0 &&
                        (whitePUNCTLayer[i] and bitmask) == 0UL &&
                        (whitePUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  whitePUNCTLayer[i] = whitePUNCTLayer[i] or bitmask
                  break
                }
                // place white on top of black
                if (
                    i % 2 == 1 &&
                        (blackPUNCTLayer[i] and bitmask) == 0UL &&
                        (blackPUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackPUNCTLayer[i] = blackPUNCTLayer[i] or bitmask
                  break
                }
              }
            }

            else -> {}
          }
        }

        PlayerName.BLACK.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              for (i in 1..5) {
                // place black on top of white
                if (
                    i % 2 == 1 &&
                        (whiteDVONNLayer[i] and bitmask) == 0UL &&
                        (whiteDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  whiteDVONNLayer[i] = whiteDVONNLayer[i] or bitmask
                  break
                }
                // place black on top of white on top of black
                if (
                    i % 2 == 0 &&
                        (blackDVONNLayer[i] and bitmask) == 0UL &&
                        (blackDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackDVONNLayer[i] = blackDVONNLayer[i] or bitmask
                  break
                }
              }
            }

            PieceType.PUNCT -> {
              for (i in 1..5) {
                // place black on top of white
                if (
                    i % 2 == 1 &&
                        (whitePUNCTLayer[i] and bitmask) == 0UL &&
                        (whitePUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  whitePUNCTLayer[i] = whitePUNCTLayer[i] or bitmask
                  break
                }
                // place black on top of white on top of black
                if (
                    i % 2 == 0 &&
                        (blackPUNCTLayer[i] and bitmask) == 0UL &&
                        (blackPUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackPUNCTLayer[i] = blackPUNCTLayer[i] or bitmask
                  break
                }
              }
            }

            else -> {}
          }
        }
      }
    } else {
      when (piece?.colorName) {
        PlayerName.WHITE.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              whiteDVONNLayer[0] = whiteDVONNLayer[0] or bitmask
            }

            PieceType.PUNCT -> {
              whitePUNCTLayer[0] = whitePUNCTLayer[0] or bitmask
            }

            else -> {}
          }
        }

        PlayerName.BLACK.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              blackDVONNLayer[0] = blackDVONNLayer[0] or bitmask
            }

            PieceType.PUNCT -> {
              blackPUNCTLayer[0] = blackPUNCTLayer[0] or bitmask
            }

            else -> {}
          }
        }
      }
    }

    if (piece?.potential == true) {
      when (piece.colorName) {
        PlayerName.WHITE.name -> {
          whitePotentials = whitePotentials or bitmask
        }

        PlayerName.BLACK.name -> {
          blackPotentials = blackPotentials or bitmask
        }
      }
    }
  }
}

fun Bitboard.removeRetrieveAndCapturePiecesFromBitboard(
    retrievedCapturedPiecesBits: List<RetrievedCapturedPieceBit>
) {

  retrievedCapturedPiecesBits.forEach {
      (retrievedPiece, capturedPiece, bitmask, isNeutralized, keepRetrievedPieceInPlay) ->
    val piece = retrievedPiece ?: capturedPiece

    when (piece?.colorName) {
      PlayerName.WHITE.name -> {
        when (piece.type) {
          PieceType.GIPF -> {
            whiteGIPF = whiteGIPF and bitmask.inv()
          }

          PieceType.TAMSK -> {
            whiteTAMSK = whiteTAMSK and bitmask.inv()
          }

          PieceType.ZERTZ -> {
            whiteZERTZ = whiteZERTZ and bitmask.inv()
          }

          PieceType.YINSH -> {
            whiteYINCH = whiteYINCH and bitmask.inv()
          }

          else -> {}
        }
      }

      PlayerName.BLACK.name -> {
        when (piece.type) {
          PieceType.GIPF -> {
            blackGIPF = blackGIPF and bitmask.inv()
          }

          PieceType.TAMSK -> {
            blackTAMSK = blackTAMSK and bitmask.inv()
          }

          PieceType.ZERTZ -> {
            blackZERTZ = blackZERTZ and bitmask.inv()
          }

          PieceType.YINSH -> {
            blackYINCH = blackYINCH and bitmask.inv()
          }

          else -> {}
        }
      }
    }

    if (isNeutralized) {
      when (piece?.colorName) {
        PlayerName.WHITE.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              for (i in 5 downTo 1) {
                // place white on top of black on top of white
                if (
                    i % 2 == 0 &&
                        (whiteDVONNLayer[i] and bitmask) == 0UL &&
                        (whiteDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  whiteDVONNLayer[i] = whiteDVONNLayer[i] and bitmask.inv()
                  break
                }
                // place white on top of black
                if (
                    i % 2 == 1 &&
                        (blackDVONNLayer[i] and bitmask) == 0UL &&
                        (blackDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackDVONNLayer[i] = blackDVONNLayer[i] and bitmask.inv()
                  break
                }
              }
            }

            PieceType.PUNCT -> {
              for (i in 5 downTo 1) {
                // place white on top of black on top of white
                if (
                    i % 2 == 0 &&
                        (whitePUNCTLayer[i] and bitmask) == 0UL &&
                        (whitePUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  whitePUNCTLayer[i] = whitePUNCTLayer[i] and bitmask.inv()
                  break
                }
                // place white on top of black
                if (
                    i % 2 == 1 &&
                        (blackPUNCTLayer[i] and bitmask) == 0UL &&
                        (blackPUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackPUNCTLayer[i] = blackPUNCTLayer[i] and bitmask.inv()
                  break
                }
              }
            }

            else -> {}
          }
        }

        PlayerName.BLACK.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              for (i in 5 downTo 1) {
                // place black on top of white
                if (
                    i % 2 == 1 &&
                        (whiteDVONNLayer[i] and bitmask) == 0UL &&
                        (whiteDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  whiteDVONNLayer[i] = whiteDVONNLayer[i] and bitmask.inv()
                  break
                }
                // place black on top of white on top of black
                if (
                    i % 2 == 0 &&
                        (blackDVONNLayer[i] and bitmask) == 0UL &&
                        (blackDVONNLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackDVONNLayer[i] = blackDVONNLayer[i] and bitmask.inv()
                  break
                }
              }
            }

            PieceType.PUNCT -> {
              for (i in 5 downTo 1) {
                // place black on top of white
                if (
                    i % 2 == 1 &&
                        (whitePUNCTLayer[i] and bitmask) == 0UL &&
                        (whitePUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  whitePUNCTLayer[i] = whitePUNCTLayer[i] and bitmask.inv()
                  break
                }
                // place black on top of white on top of black
                if (
                    i % 2 == 0 &&
                        (blackPUNCTLayer[i] and bitmask) == 0UL &&
                        (blackPUNCTLayer[i - 1] and bitmask) == bitmask
                ) {
                  blackPUNCTLayer[i] = blackPUNCTLayer[i] and bitmask.inv()
                }
              }
            }

            else -> {}
          }
        }
      }
    } else {
      when (piece?.colorName) {
        PlayerName.WHITE.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              whiteDVONNLayer[0] = whiteDVONNLayer[0] and bitmask.inv()
            }

            PieceType.PUNCT -> {
              whitePUNCTLayer[0] = whitePUNCTLayer[0] and bitmask.inv()
            }

            else -> {}
          }
        }

        PlayerName.BLACK.name -> {
          when (piece.type) {
            PieceType.DVONN -> {
              blackDVONNLayer[0] = blackDVONNLayer[0] and bitmask.inv()
            }

            PieceType.PUNCT -> {
              blackPUNCTLayer[0] = blackPUNCTLayer[0] and bitmask.inv()
            }

            else -> {}
          }
        }
      }
    }

    if (piece?.potential == true) {
      when (piece.colorName) {
        PlayerName.WHITE.name -> {
          whitePotentials = whitePotentials and bitmask.inv()
        }

        PlayerName.BLACK.name -> {
          blackPotentials = blackPotentials and bitmask.inv()
        }
      }
    }
  }
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
          else if ((whiteYINCH and bitmask) == bitmask) PieceType.YINSH
          else if ((whiteDVONNLayer[0] and bitmask) == bitmask) PieceType.DVONN
          else if ((whitePUNCTLayer[0] and bitmask) == bitmask) PieceType.PUNCT else null
        }

        PlayerName.BLACK -> {
          if ((blackGIPF and bitmask) == bitmask) PieceType.GIPF
          else if ((blackTAMSK and bitmask) == bitmask) PieceType.TAMSK
          else if ((blackZERTZ and bitmask) == bitmask) PieceType.ZERTZ
          else if ((blackYINCH and bitmask) == bitmask) PieceType.YINSH
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
          getYinchMoves(currentPlayer, columnInfos) +
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
  } else if (allAvailableMoves.isNotEmpty()) {
    return allAvailableMoves + eligibleMovesUsingPotential
  } else {
    //		emptyList<PossibleMove>()
    throw IllegalStateException("Player ${currentPlayer.name} has no available moves left!")
  }
}

fun Bitboard.assertPieceCount(
    EXPECTED_TOTAL: Int = 66 / 2,
    MAXIMUM_PIECES: Int = 66,
    currentPlayer: Player,
    nextPlayer: Player,
) {
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
          blackYINCH.countOneBits()

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

  check(totalBlackPieces == EXPECTED_TOTAL) {
    """
    Critical State Corruption: Total Black pieces ($totalBlackPieces) does not match expected maximum ($EXPECTED_TOTAL).
    Breakdown:
    - Next Player Reserve: Potentials=${nextReservePotentials / 2} (weighted=$nextReservePotentials), Basics=$nextReserveBasics
    - Next Player Captured: Potentials=${nextCapturedPotentials / 2} (weighted=$nextCapturedPotentials), Basics=$nextCapturedBasics
    - Current Player Reserve: Potentials=${currentReservePotentials / 2} (weighted=$currentReservePotentials), Basics=$currentReserveBasics
    - Current Player Captured: Potentials=${currentCapturedPotentials / 2} (weighted=$currentCapturedPotentials), Basics=$currentCapturedBasics
    - Active Board: Potentials=${boardPotentials / 2} (weighted=$boardPotentials), Basics=$boardBasics, Hidden in Stacks=$boardStacks
    """
        .trimIndent()
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
          whiteYINCH.countOneBits()

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

  check(totalWhitePieces == EXPECTED_TOTAL) {
    """
    Critical State Corruption: Total White pieces ($totalWhitePieces) does not match expected maximum ($EXPECTED_TOTAL).
    Breakdown:
    - Next Player Reserve: Potentials=${nextReservePotentials / 2} (weighted=$nextReservePotentials), Basics=$nextReserveBasics
    - Next Player Captured: Potentials=${nextCapturedPotentials / 2} (weighted=$nextCapturedPotentials), Basics=$nextCapturedBasics
    - Current Player Reserve: Potentials=${currentReservePotentials / 2} (weighted=$currentReservePotentials), Basics=$currentReserveBasics
    - Current Player Captured: Potentials=${currentCapturedPotentials / 2} (weighted=$currentCapturedPotentials), Basics=$currentCapturedBasics
    - Active Board: Potentials=${boardPotentials / 2} (weighted=$boardPotentials), Basics=$boardBasics, Hidden in Stacks=$boardStacks
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
