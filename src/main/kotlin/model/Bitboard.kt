@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

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

fun Bitboard.vacantLines(): List<ColumnInfo> {
  return lineMasks.mapNotNull { lineMask ->
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
}

// TODO
// region From Gemini

fun Bitboard.executePushUp(col: ColumnInfo) {
  //
  val occupiedSpots = globalOccupancy
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()
  var gapFound = false

  // 1. DISCOVERY: Find out exactly which pieces are shifting
  for ((fromMask, toMask) in col.shiftPairs) {
    if ((occupiedSpots and fromMask) != 0UL) {
      movesToApply.add(fromMask to toMask)
    } else {
      gapFound = true
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
}

fun Bitboard.executePushDown(col: ColumnInfo) {
  //
  val occupiedSpots = globalOccupancy
  val movesToApply = mutableListOf<Pair<ULong, ULong>>()
  var gapFound = false

  // 1. DISCOVERY: Find out exactly which pieces are shifting
  for ((toMask, fromMask) in col.shiftPairs.asReversed()) {
    if ((occupiedSpots and fromMask) != 0UL) {
      movesToApply.add(fromMask to toMask)
    } else {
      gapFound = true
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
    addAtIndex: Int,
    pushDirection: PushDirection,
    col: ColumnInfo,
    piece: Piece,
) {
  require(addAtIndex in openningLineIndexArray) {
    "Invalid opening line placement! Index $addAtIndex is not a valid selection. " +
        "Eligible indices: ${openningLineIndexArray.joinToString()}"
  }

  val newPieceMask = 1UL shl addAtIndex

  check((newPieceMask and openningSpotsLineMask) == newPieceMask) {
    val newPieceBinary = newPieceMask.toString(2).padStart(40, '0')
    val openingLineBinary = openningSpotsLineMask.toString(2).padStart(40, '0')

    "Bitmask Validation Failure: The new piece mask contains bits outside the allowed opening line mask.\n" +
        "New Piece Mask:   $newPieceBinary\n" +
        "Opening Line Mask: $openingLineBinary"
  }

  // TODO is addAtIndex occupied
  val isIndexOccupied = (globalOccupancy and newPieceMask) > 0UL

  if (isIndexOccupied) {
    //    TODO("Implement Shift Pieces")
    when (pushDirection) {
      // push up based on bit order
      PushDirection.UP -> {
        executePushUp(col)
      }

      PushDirection.UPPER_RIGHT -> {
        executePushUp(col)
      }

      PushDirection.LOWER_RIGHT -> {
        executePushUp(col)
      }
      // push down based on bit order
      PushDirection.DOWN -> {
        executePushDown(col)
      }

      PushDirection.UPPER_LEFT -> {
        executePushDown(col)
      }

      PushDirection.LOWER_LEFT -> {
        executePushDown(col)
      }
    }
  }

  // TODO which bitboards to add piece
  when (piece.colorName) {
    PlayerName.WHITE.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          whiteGIPF = whiteGIPF + newPieceMask
        }

        PieceType.TAMSK -> {
          whiteTAMSK = whiteTAMSK + newPieceMask
          whitePotentials = whitePotentials + newPieceMask
        }

        PieceType.ZERTZ -> {
          whiteZERTZ = whiteZERTZ + newPieceMask
          whitePotentials = whitePotentials + newPieceMask
        }

        PieceType.DVONN -> {
          whiteDVONNLayer[0] = whiteDVONNLayer[0] + newPieceMask
          whitePotentials = whitePotentials + newPieceMask
        }

        PieceType.YINSH -> {
          whiteYINCH = whiteYINCH + newPieceMask
          whitePotentials = whitePotentials + newPieceMask
        }

        PieceType.PUNCT -> {
          whitePUNCTLayer[0] = whitePUNCTLayer[0] + newPieceMask
          whitePotentials = whitePotentials + newPieceMask
        }
      }
    }

    PlayerName.BLACK.name -> {
      when (piece.type) {
        PieceType.GIPF -> {
          blackGIPF = blackGIPF + newPieceMask
        }

        PieceType.TAMSK -> {
          blackTAMSK = blackTAMSK + newPieceMask
          blackPotentials = blackPotentials + newPieceMask
        }

        PieceType.ZERTZ -> {
          blackZERTZ = blackZERTZ + newPieceMask
          blackPotentials = blackPotentials + newPieceMask
        }

        PieceType.DVONN -> {
          blackDVONNLayer[0] = blackDVONNLayer[0] + newPieceMask
          blackPotentials = blackPotentials + newPieceMask
        }

        PieceType.YINSH -> {
          blackYINCH = blackYINCH + newPieceMask
          blackPotentials = blackPotentials + newPieceMask
        }

        PieceType.PUNCT -> {
          blackPUNCTLayer[0] = blackPUNCTLayer[0] + newPieceMask
          blackPotentials = blackPotentials + newPieceMask
        }
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

fun Bitboard.usePiecePotential(piece: Piece, sourceIndex: ULong, targetIndex: ULong) {
  // TODO filter for legal moves before calling this
  //  pieces can not be neutralized

  // TODO which bitboards to add piece
  when (piece.colorName) {
    PlayerName.WHITE.name -> {
      when (piece.type) {
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {
          val isSourceValid =
              (sourceIndex and whiteTAMSK and boardCenterSpotMask) == boardCenterSpotMask
          val isTargetValid = (targetIndex and openningSpotsLineMask) == targetIndex

          check(isSourceValid && isTargetValid) {
            buildString {
              appendLine("Illegal TAMSK Opening Move:")
              if (!isSourceValid) {
                appendLine(
                    "  -> Source Violation: Source index (0b${
                                        sourceIndex.toString(2).padStart(40, '0')
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
                                        targetIndex.toString(2).padStart(40, '0')
                                    }) must be fully contained within the opening line mask (0b${
                                        openningSpotsLineMask.toString(
                                            2
                                        ).padStart(40, '0')
                                    })."
                )
              }
            }
          }

          whiteTAMSK = whiteTAMSK or targetIndex
          whitePotentials = whitePotentials xor boardCenterSpotMask
        }

        PieceType.ZERTZ -> {
          whiteZERTZ = whiteZERTZ or targetIndex
          whitePotentials = whitePotentials xor sourceIndex
        }

        PieceType.YINSH -> {
          whiteYINCH = whiteYINCH or targetIndex
          whitePotentials = whitePotentials xor sourceIndex
        }

        PieceType.DVONN -> {
          for (i in 1..5) {
            // place black on top of white
            if (i % 2 == 0 && whiteDVONNLayer[i] == 0UL && whiteDVONNLayer[i - 1] == targetIndex) {
              whiteDVONNLayer[i] = whiteDVONNLayer[i] or targetIndex
              break
            }
            // place black on top of white on top of black
            if (i % 2 == 1 && blackDVONNLayer[i] == 0UL && blackDVONNLayer[i - 1] == targetIndex) {
              blackDVONNLayer[i] = blackDVONNLayer[i] or targetIndex
              break
            }
          }

          whitePotentials = whitePotentials xor sourceIndex
        }

        PieceType.PUNCT -> {
          for (i in 1..5) {
            // place black on top of white
            if (
                i % 2 == 0 &&
                    whitePUNCTLayer[i] == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetIndex) == targetIndex
            ) {
              whitePUNCTLayer[i] = whitePUNCTLayer[i] or targetIndex
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 1 &&
                    blackPUNCTLayer[i] == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetIndex) == targetIndex
            ) {
              blackPUNCTLayer[i] = blackPUNCTLayer[i] or targetIndex
              break
            }
          }

          whitePotentials = whitePotentials xor sourceIndex
        }
      }
    }

    PlayerName.BLACK.name -> {
      when (piece.type) {
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {
          val isSourceValid =
              (sourceIndex and blackTAMSK and boardCenterSpotMask) == boardCenterSpotMask
          val isTargetValid = (targetIndex and openningSpotsLineMask) == targetIndex

          check(isSourceValid && isTargetValid) {
            buildString {
              appendLine("Illegal TAMSK Opening Move:")
              if (!isSourceValid) {
                appendLine(
                    "  -> Source Violation: Source index (0b${
                                        sourceIndex.toString(2).padStart(40, '0')
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
                                        targetIndex.toString(2).padStart(40, '0')
                                    }) must be fully contained within the opening line mask (0b${
                                        openningSpotsLineMask.toString(
                                            2
                                        ).padStart(40, '0')
                                    })."
                )
              }
            }
          }

          blackTAMSK = blackTAMSK or targetIndex
          blackPotentials = blackPotentials xor boardCenterSpotMask
        }

        PieceType.ZERTZ -> {
          blackZERTZ = blackZERTZ or targetIndex
          blackPotentials = blackPotentials xor sourceIndex
        }

        PieceType.YINSH -> {
          blackYINCH = blackYINCH or targetIndex
          blackPotentials = blackPotentials xor sourceIndex
        }

        // TODO confirm if targetIndex/Spot if is white
        PieceType.DVONN -> {

          for (i in 1..5) {
            // place black on top of white
            if (i % 2 == 1 && whiteDVONNLayer[i] == 0UL && whiteDVONNLayer[i - 1] == targetIndex) {
              whiteDVONNLayer[i] = whiteDVONNLayer[i] or targetIndex
              break
            }
            // place black on top of white on top of black
            if (i % 2 == 0 && blackDVONNLayer[i] == 0UL && blackDVONNLayer[i - 1] == targetIndex) {
              blackDVONNLayer[i] = blackDVONNLayer[i] or targetIndex
              break
            }
          }

          blackPotentials = blackPotentials xor sourceIndex
        }

        PieceType.PUNCT -> {
          for (i in 1..5) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    whitePUNCTLayer[i] == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetIndex) == targetIndex
            ) {
              whitePUNCTLayer[i] = whitePUNCTLayer[i] or targetIndex
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    blackPUNCTLayer[i] == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetIndex) == targetIndex
            ) {
              blackPUNCTLayer[i] = blackPUNCTLayer[i] or targetIndex
              break
            }
          }

          blackPotentials = blackPotentials xor sourceIndex
        }
      }
    }
  }
}

fun Bitboard.getTamskMoves(player: Player): List<Pair<ULong, Any>> {
  val validMoves = mutableListOf<Pair<ULong, ColumnInfo>>()

  val tamskPiece =
      when (player.name) {
        PlayerName.WHITE -> {
          whiteTAMSK and boardCenterSpotMask
        }

        PlayerName.BLACK -> {
          blackTAMSK and boardCenterSpotMask
        }
      }

  // no valid moves were found
  if (tamskPiece == 0UL) return validMoves

  return lineMasks
      .filter { mask ->
        (mask and globalOccupancy) < mask
      }
      .map { mask ->
        tamskPiece to columnInfos.first { it.columnMask == mask }
      }
} // TODO figure out where to place Tamsk piece (col.positions.first() or col.positions.last()) and

// direction to push (start: Push Up, end: push down)

fun Bitboard.useTamskPotential(player: Player, sourceIndex: ULong, targetIndex: ULong) {
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

  when (player.name) {
    PlayerName.WHITE -> {
      whiteTAMSK = whiteTAMSK or targetIndex
      whitePotentials = whitePotentials xor boardCenterSpotMask
    }

    PlayerName.BLACK -> {
      blackTAMSK = blackTAMSK or targetIndex
      blackPotentials = blackPotentials xor boardCenterSpotMask
    }
  }
}

fun Bitboard.getZertzMoves(
    player: Player,
    columnInfos: List<ColumnInfo>,
): MutableList<Pair<ULong, ULong>> {
  val validMoves = mutableListOf<Pair<ULong, ULong>>()

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

            for (index in zertzIndex.plus(1) until col.positions.size) {
              if ((col.positions[index] and globalOccupancy) > 0UL) {
                continue
              } else if (
                  (col.positions[index] and globalOccupancy) == 0UL && index.minus(zertzIndex) > 1
              ) {
                validMoves.add(col.positions[zertzIndex] to col.positions[index])
                break
              } else {
                break
              }
            }

            for (index in zertzIndex.minus(1) downTo 0) {

              if ((col.positions[index] and globalOccupancy) > 0UL) {
                continue
              } else if (
                  (col.positions[index] and globalOccupancy) == 0UL && zertzIndex.minus(index) > 1
              ) {
                validMoves.add(col.positions[zertzIndex] to col.positions[index])
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
): MutableList<Pair<ULong, ULong>> {
  val validMoves = mutableListOf<Pair<ULong, ULong>>()

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
                validMoves.add(col.positions[yinchIndex] to col.positions[index])
              } else if ((col.positions[index] and globalOccupancy) > 0UL) {
                break
              }
            }

            for (index in yinchIndex.minus(1) downTo 0) {
              if ((col.positions[index] and globalOccupancy) == 0UL) {
                validMoves.add(col.positions[yinchIndex] to col.positions[index])
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
): MutableList<Pair<ULong, ULong>> {
  val validMoves = mutableListOf<Pair<ULong, ULong>>()

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

        else -> {
          // no valid moves were found
          return validMoves
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
                validMoves.add(col.positions[dvonnIndex] to col.positions[index])
              }
            }

            for (index in dvonnIndex.minus(1) downTo 0) {
              if ((col.positions[index] and targetDvonnPieces) > 0UL) {
                validMoves.add(col.positions[dvonnIndex] to col.positions[index])
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
): MutableList<Pair<ULong, ULong>> {
  val validMoves = mutableListOf<Pair<ULong, ULong>>()

  val activePunctPieces =
      when (player.name) {
        PlayerName.WHITE -> {
          whitePUNCTLayer[0] and whitePotentials and whiteNeutralized.inv()
        }

        PlayerName.BLACK -> {
          blackPUNCTLayer[0] and blackPotentials and blackNeutralized.inv()
        }

        else -> {
          // no valid moves were found
          return validMoves
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

        else -> {
          // no valid moves were found
          return validMoves
        }
      }

  columnInfos.forEach { col ->
    if (
        (col.columnMask and activePunctPieces > 0UL) && (col.columnMask and targetPunctPieces > 0UL)
    ) {
      col.positions
          .filter {
            it and (col.columnMask and activePunctPieces) != 0UL // get position of yinch pieces
          }
          .forEach {
            val dvonnIndex = col.positions.indexOf(it)

            for (index in dvonnIndex.plus(1) until col.positions.size) {
              if ((col.positions[index] and targetPunctPieces) > 0UL) {
                validMoves.add(col.positions[dvonnIndex] to col.positions[index])
              }
            }

            for (index in dvonnIndex.minus(1) downTo 0) {
              if ((col.positions[index] and targetPunctPieces) > 0UL) {
                validMoves.add(col.positions[dvonnIndex] to col.positions[index])
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
  return potentials.fold(initial = listOf(emptyList<ULong>())) { accumulator, item ->
    // For every item, take the current sublists (accumulator)
    // and add a new set of sublists where the item is appended
    accumulator + accumulator.map { it + item }
  }.filter { it.isNotEmpty() }
}



fun Bitboard.retrieveAndCapturePieces(columnInfos: List<ColumnInfo>, selectedPiecesWithPotentialPowerset: List<ULong>, player: Player) {
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

  val neutralizedPieces = mutableListOf<Piece>()
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

            neutralizedPieces.add(
                Piece(
                    abbreviation = "${finalColor.name.first()}D",
                    potential = false,
                    colorName = finalColor.name,
                    type = PieceType.DVONN,
                    isNeutralized = false,
                    stackedPieces = mutableListOf(),
                )
            )

            blackDVONNLayer[i] = blackDVONNLayer[i] xor bitmask
            whiteDVONNLayer[i] = whiteDVONNLayer[i] xor bitmask
          }

          hasWhitePUNCT || hasBlackPUNCT -> {
            val baseColor = if (hasWhitePUNCT) PlayerName.WHITE else PlayerName.BLACK
            val finalColor =
                if (isEven) baseColor
                else if (baseColor == PlayerName.WHITE) PlayerName.BLACK else PlayerName.WHITE

            neutralizedPieces.add(
                Piece(
                    abbreviation = "${finalColor.name.first()}P",
                    potential = false,
                    colorName = finalColor.name,
                    type = PieceType.PUNCT,
                    isNeutralized = false,
                    stackedPieces = mutableListOf(),
                )
            )

            blackPUNCTLayer[i] = blackPUNCTLayer[i] xor bitmask
            whitePUNCTLayer[i] = whitePUNCTLayer[i] xor bitmask
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

  val pieces =
      // TODO set bits to zero. create function to clear bit.
      playerPiecesWithoutPotential
          .plus(opponentPiecesAndNotNeutralized)
          .flatten()
	        .plus(selectedPiecesWithPotentialPowerset)
          .mapNotNull { bitmask ->
            // construct piece and place it on the matching node
            var piece = createPiece(bitmask)

            piece =
                if (piece != null) {
                  when (piece.type) {
                    PieceType.GIPF -> {
                      whiteGIPF = whiteGIPF xor bitmask
                      blackGIPF = blackGIPF xor bitmask
                      piece
                    }
                    PieceType.TAMSK -> {
                      whiteTAMSK = whiteTAMSK xor bitmask
                      blackTAMSK = blackTAMSK xor bitmask
                      piece
                    }
                    PieceType.ZERTZ -> {
                      whiteZERTZ = whiteZERTZ xor bitmask
                      blackZERTZ = blackZERTZ xor bitmask
                      piece
                    }
                    PieceType.DVONN -> {
                      if (piece.stackedPieces.isNotEmpty()) {
                        piece.stackedPieces.removeLast()
                      } else {
                        whiteDVONNLayer[0] = whiteDVONNLayer[0] xor bitmask
                        blackDVONNLayer[0] = blackDVONNLayer[0] xor bitmask
                        piece
                      }
                    }
                    PieceType.YINSH -> {
                      whiteYINCH = whiteYINCH xor bitmask
                      blackYINCH = blackYINCH xor bitmask
                      piece
                    }
                    PieceType.PUNCT -> {
                      if (piece.stackedPieces.isNotEmpty()) {
                        piece.stackedPieces.removeLast()
                      } else {
                        whitePUNCTLayer[0] = whitePUNCTLayer[0] xor bitmask
                        blackPUNCTLayer[0] = blackPUNCTLayer[0] xor bitmask
                        piece
                      }
                    }
                  }
                } else {
                  null
                }

	          if (piece != null && piece.potential) {
							whitePotentials = whitePotentials xor bitmask
		          blackPotentials = blackPotentials xor bitmask
	          }

            piece
          }
          .plus(neutralizedPieces)

  val retrievedCapturedPieces =
      RetrievedCapturedPieces(
          retrieved = pieces.filter { it.colorName == player.name.name },
          captured = pieces.filter { it.colorName != player.name.name },
          nodes = emptyList(),
      )
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
