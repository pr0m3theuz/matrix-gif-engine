@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlinx.serialization.json.Json

private val logger = logger {}

fun Bitboard.addPieceToBitboard(move: UInt): ULong {
  // --- 0. EXTRACT PROPERTIES DIRECTLY FROM UInt ---
  val addAtIndex = move.extractTargetBit()
  val pushDirection = move.extractPushDirection() ?: error("Missing pushDirection in move: $move")
  val col = move.extractColumnInfo()
  val pieceColor = move.extractPieceColor()
  val pieceType = move.extractPieceType() ?: error("Missing pieceType in move: $move")

  // --- 1. CONFIGURABLE DEBUGGING ---
  if (logger.isDebugEnabled()) {
    logger.trace { "--- ADD PIECE CALLED ---" }
    logger.trace { "Piece: $pieceColor $pieceType" }
    logger.trace {
      "AddAtIndex: 0b${addAtIndex.toString(2)} | PushDirection: $pushDirection | Col: $col"
    }
    logger.trace { "Board before adding piece:  0b${globalOccupancy.toString(2)}" }
  }

  // --- 2. PRE-CONDITION CHECKS (Input Validation) ---
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

  // --- 3. HANDLE SHIFTING (If occupied) ---
  val isIndexOccupied = (globalOccupancy and addAtIndex) != 0UL
  var vacantBitFound: ULong = ULong.MAX_VALUE

  if (isIndexOccupied) {

    logger.trace {
      "Index 0b(${addAtIndex.toString(2)}) is occupied. Executing shift ($pushDirection)."
    }

    when (pushDirection) {
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        vacantBitFound = executePushUp(col)
        logger.trace { "Executed PushUp. Vacant bit found: $vacantBitFound" }
      }
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        vacantBitFound = executePushDown(col)
        logger.trace { "Executed PushDown. Vacant bit found: $vacantBitFound" }
      }
    }

    // Ensure the shift actually found exactly one empty spot to push into
    check(vacantBitFound.countOneBits() == 1) {
      "SHIFT ERROR: Shift execution must return exactly one vacant bit. Got: $vacantBitFound"
    }
  } else {
    logger.trace { "Index is vacant. No shift required." }
  }

  // --- 4. LOCAL HELPER FOR SAFE BIT ADDITION ---
  fun safeAddAndCheck(board: ULong, boardName: String): ULong {
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board before restoring $boardName piece\n" +
                "addAtIndex: 0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
                "$boardName bitboard:  0b${board.toString(2).padStart(40, '0')}")
      }
    }
    val newBoard = board or addAtIndex
    check((newBoard and addAtIndex) == addAtIndex) {
      "ADD FAILED: Failed to add $pieceColor $pieceType to $boardName bitboard at index $addAtIndex."
    }
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board after restoring $boardName piece\n" +
                "addAtIndex: 0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
                "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}")
      }
    }
    return newBoard
  }

  // --- 5. APPLY TO BITBOARDS ---
  when (pieceColor) {
    PlayerName.WHITE -> {
      when (pieceType) {
        PieceType.NULL -> {}
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

    PlayerName.BLACK -> {
      when (pieceType) {
        PieceType.NULL -> {}
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
  }

  if (logger.isDebugEnabled()) {
    logger.trace { "--- ADD PIECE COMPLETE ---" }
    logger.trace { "Board after adding piece:  0b${globalOccupancy.toString(2)}" }
  }

  return vacantBitFound
}

fun Bitboard.undoAddPieceToBitboard(
    move: UInt,
    vacantBitFound: ULong,
    wasIndexOccupied: Boolean, // Required to know if we need to revert a shift
) {
  // --- 0. EXTRACT PROPERTIES DIRECTLY FROM UInt ---
  val removeAtIndex = move.extractTargetBit()
  val pushDirection = move.extractPushDirection() ?: error("Missing pushDirection in move: $move")
  val col = move.extractColumnInfo()
  val pieceColor = move.extractPieceColor()
  val pieceType = move.extractPieceType() ?: error("Missing pieceType in move: $move")

  // --- 1. CONFIGURABLE DEBUGGING ---
  if (logger.isDebugEnabled()) {
    logger.trace { "--- UNDO ADD PIECE CALLED ---" }
    logger.trace { "Piece: $pieceColor $pieceType" }
    logger.trace { "RemoveAtIndex: $removeAtIndex | VacantBitFound: $vacantBitFound" }
    logger.trace { "WasOccupied: $wasIndexOccupied | PushDirection: $pushDirection" }
    logger.trace { "Board before removing piece:  0b${globalOccupancy.toString(2)}" }
  }

  // --- 2. PRE-CONDITION CHECKS (Input Validation) ---
  require(removeAtIndex.countOneBits() == 1) {
    "CRITICAL ERROR: removeAtIndex must be exactly one bit. Got: $removeAtIndex"
  }
  if (wasIndexOccupied) {
    require(vacantBitFound.countOneBits() == 1 && wasIndexOccupied) {
      "CRITICAL ERROR: vacantBitFound must be exactly one bit if reverting a shift. Got: $vacantBitFound"
    }
  }
  check((globalOccupancy and removeAtIndex) != 0UL) {
    "STATE ERROR: Piece cannot be removed from unoccupied index in globalOccupancy."
  }

  // --- 3. LOCAL HELPER FOR SAFE BIT REMOVAL ---
  // This ensures the piece actually exists before removal, and is gone after.
  fun safeRemoveAndCheck(board: ULong, boardName: String): ULong {
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board before removing $boardName piece:\n" +
                "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
                "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}")
      }
    }
    check((board and removeAtIndex) != 0UL) {
      "UNDO FAILED: Expected $pieceColor $pieceType in $boardName bitboard at $removeAtIndex, but it was missing."
    }
    val newBoard = board and removeAtIndex.inv()
    check((newBoard and removeAtIndex) == 0UL) {
      "UNDO FAILED: Failed to remove $pieceColor $pieceType from $boardName bitboard."
    }
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board after removing $boardName piece\n" +
                "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
                "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}")
      }
    }
    logger.trace { "Successfully removed bit from $boardName." }
    return newBoard
  }

  // --- 4. REVERT THE BITBOARD ADDITIONS ---
  when (pieceColor) {
    PlayerName.WHITE -> {
      when (pieceType) {
        PieceType.NULL -> {}
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
    PlayerName.BLACK -> {
      when (pieceType) {
        PieceType.NULL -> {}
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
  }

  // --- 5. REVERT THE SHIFTS ---
  if (wasIndexOccupied) {
    logger.trace { "Reverting board shift. Original push: $pushDirection" }

    when (pushDirection) {
      // If the original move pushed UP, we must pull DOWN to undo
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        executePullDown(col, vacantBitFound)
        logger.trace { "Executed PullDown on col $col" }
      }

      // If the original move pushed DOWN, we must pull UP to undo
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        executePullUp(col, vacantBitFound)
        logger.trace { "Executed PullUp on col $col" }
      }
    }
  } else {
    logger.trace { "Index was not previously occupied. No shifts to revert." }
  }

  // --- 6. POST-CONDITION CHECKS ---
  if (logger.isDebugEnabled()) {
    logger.trace { "--- UNDO ADD PIECE COMPLETE ---" }
    logger.trace { "Board after removing piece:  0b${globalOccupancy.toString(2)}" }
  }
}

fun Bitboard.usePiecePotential(
    move: UInt,
) {
  // TODO filter for legal moves before calling this
  //  pieces can not be neutralized

  // --- 0. EXTRACT PROPERTIES DIRECTLY FROM UInt ---
  val pieceType = move.extractPieceType() ?: error("No piece type was selected in move: $move")
  val pieceColor = move.extractPieceColor()
  val sourceBit = move.extractSourceBit()
  val targetBit = move.extractTargetBit()

//  if (sourceBit == ULong.MAX_VALUE || targetBit == sourceBit || pieceType == PieceType.TAMSK || blackPotentials == 0UL || whitePotentials == 0UL) {
//    logger.trace { "Invalid source bit: $sourceBit." }
//    return
//  }
  require(sourceBit != targetBit) {
    "Movement violation: Origin and destination bit indexes must be different. Cannot use a piece potential on itself"
  }

  // --- 1. CONFIGURABLE DEBUGGING ---
  val playerPotentials = if (pieceColor == PlayerName.WHITE) whitePotentials else blackPotentials
  if (logger.isDebugEnabled()) {
    logger.trace { "--- USE POTENTIAL CALLED ---" }
    logger.trace { "Piece: $pieceColor $pieceType" }
    logger.trace {
      "Source Bit:                   0b${sourceBit.toString(2).padStart(40, '0')}"
    }
    logger.trace {
      "Target Bit:                   0b${targetBit.toString(2).padStart(40, '0')}"
    }
    logger.trace {
      "Player Potentials:            0b${playerPotentials.toString(2).padStart(40, '0')}"
    }
    logger.trace {
      "Board before using potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}"
    }
  }

  // --- 2. PRE-CONDITION CHECKS (Input Validation) ---
  require(sourceBit.countOneBits() == 1) {
    "CRITICAL ERROR: sourceBit must be exactly one bit. Got: $sourceBit"
  }
  require(targetBit.countOneBits() == 1) {
    "CRITICAL ERROR: targetBit must be exactly one bit. Got: $targetBit"
  }

  fun safeRemoveAndCheck(board: ULong, boardName: String, removeAtIndex: ULong): ULong {
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board before using $boardName potential\n" +
                "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
                "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}")
      }
    }
    check((board and removeAtIndex) != 0UL) {
      "UNDO FAILED: Expected $pieceColor $pieceType in $boardName bitboard at $removeAtIndex, but it was missing."
    }

    val newBoard = board and removeAtIndex.inv()

    check((newBoard and removeAtIndex) == 0UL) {
      "UNDO FAILED: Failed to use potential for $pieceColor $pieceType piece from $boardName bitboard."
    }
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board after using $boardName potential\n" +
                "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
                "new $boardName bitboard:      0b${newBoard.toString(2).padStart(40, '0')}")
      }
    }
    logger.trace { "Successfully removed bit from $boardName." }
    return newBoard
  }

  fun safeAddAndCheck(board: ULong, boardName: String, addAtIndex: ULong): ULong {
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board before adding piece at \n" +
                "addAtIndex:    0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
                "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}")
      }
    }
    val newBoard = board or addAtIndex
    check((newBoard and addAtIndex) == addAtIndex) {
      "ADD FAILED: Failed to add $pieceColor $pieceType piece to $boardName bitboard at index $addAtIndex."
    }
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board after adding piece at \n" +
                "addAtIndex:    0b${addAtIndex.toString(2).padStart(40, '0')}\n" +
                "new $boardName bitboard:      0b${newBoard.toString(2).padStart(40, '0')}")
      }
    }
    logger.trace { "Successfully added bit to $boardName." }
    return newBoard
  }

  // --- 3. APPLY TO BITBOARDS ---
  when (pieceColor) {
    PlayerName.WHITE -> {
      when (pieceType) {
        PieceType.NULL -> {}
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {}

        PieceType.ZERTZ -> {
          whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
        }

        PieceType.YINSH -> {
          whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH", targetBit)
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
        }

        // TODO something about this seems wrong
        PieceType.DVONN -> {
          for (i in 1..7) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whiteDVONNLayer[i] and targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeAddAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              blackDVONNLayer[i] =
                  safeAddAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", targetBit)
              break
            }
          }

          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
/**
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

          check(whiteDVONNLayer[2].countOneBits() != 1 || whiteDVONNLayer[0].countOneBits() >= 2) {
            "DVONN Layer 2 Invariant Broken: Black layer 2 has 1 active bit, " +
                "but base white layer 0 has ${whiteDVONNLayer[0].countOneBits()} bits (expected at least 2)."
          }

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
*/
        }

        PieceType.PUNCT -> {
          for (i in 1..7) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whitePUNCTLayer[i] and targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeAddAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeAddAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", targetBit)
              break
            }
          }

          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)

/*          val totalWhitePUNCTPiecesOutPlay =
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

          check(whitePUNCTLayer[2].countOneBits() != 1 || whitePUNCTLayer[0].countOneBits() >= 2) {
            "PUNCT Layer 2 Invariant Broken: Black layer 2 has 1 active bit, " +
                "but base white layer 0 has ${whitePUNCTLayer[0].countOneBits()} bits (expected at least 2)."
          }

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
          }*/
        }
      }
    }

    PlayerName.BLACK -> {
      when (pieceType) {
        PieceType.NULL -> {}
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {}

        PieceType.ZERTZ -> {
          blackZERTZ = safeAddAndCheck(blackZERTZ, "Black YINSH", targetBit)
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }

        PieceType.YINSH -> {
          blackYINSH = safeAddAndCheck(blackYINSH, "Black YINSH", targetBit)
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }

        // TODO confirm if targetBit/Spot if is white
        // TODO something about this seems wrong
        PieceType.DVONN -> {

          for (i in 1..7) {
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (whiteDVONNLayer[i] and targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeAddAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackDVONNLayer[i] and targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              blackDVONNLayer[i] =
                  safeAddAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", targetBit)
              break
            }
          }

          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
/*
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

          check(blackDVONNLayer[2].countOneBits() != 1 || blackDVONNLayer[0].countOneBits() >= 2) {
            "DVONN Layer 2 Invariant Broken: Black layer 2 has 1 active bit, " +
                "but base black layer 0 has ${blackDVONNLayer[0].countOneBits()} bits (expected at least 2)."
          }

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
              blackDVONNLayer[5].countOneBits()*/
        }

        PieceType.PUNCT -> {
          for (i in 1..7) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    (whitePUNCTLayer[i] and targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeAddAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackPUNCTLayer[i] and targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeAddAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", targetBit)
              break
            }
          }

          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)

          /*// TODO Missing PUNCT.
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

          check(blackPUNCTLayer[2].countOneBits() != 1 || blackPUNCTLayer[0].countOneBits() >= 2) {
            "PUNCT Layer 2 Invariant Broken: Black layer 2 has 1 active bit, " +
                "but base black layer 0 has ${blackPUNCTLayer[0].countOneBits()} bits (expected at least 2)."
          }

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
              blackPUNCTLayer[5].countOneBits()*/
        }
      }
    }
  }
  if (logger.isDebugEnabled()) {
    logger.trace { "--- USE POTENTIAL COMPLETE ---" }
    logger.trace {
      "Board after using potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}"
    }
  }

/*  for (i in 0..7) {
    check((whiteDVONNLayer[i] and blackDVONNLayer[i]).countOneBits() == 0) {
      val overlapMask = whiteDVONNLayer[i] and blackDVONNLayer[i]
      "DVONN Layer Overlap Corrupted at index $i: White and Black pieces occupy identical coordinates. " +
          "Overlap Mask: 0x${overlapMask.toString(2)} (White: 0x${whiteDVONNLayer[i].toString(2)}, Black: 0x${blackDVONNLayer[i].toString(2)})"
    }
    check((whitePUNCTLayer[i] and blackPUNCTLayer[i]).countOneBits() == 0) {
      val overlapMask = whitePUNCTLayer[i] and blackPUNCTLayer[i]
      "PUNCT Layer Overlap Corrupted at index $i: White and Black pieces occupy identical coordinates. " +
          "Overlap Mask: 0x${overlapMask.toString(2)} (White: 0x${whitePUNCTLayer[i].toString(2)}, Black: 0x${blackPUNCTLayer[i].toString(2)})"
    }
  }*/
}

fun Bitboard.undoUsePiecePotential(move: UInt) {
  // TODO filter for legal moves before calling this
  //  pieces can not be neutralized

  // --- 0. EXTRACT PROPERTIES DIRECTLY FROM UInt ---
  val pieceType = move.extractPieceType() ?: error("No piece type was selected in move: $move")
  val pieceColor = move.extractPieceColor()
  val sourceBit = move.extractSourceBit()
  val targetBit = move.extractTargetBit()

  if (sourceBit == ULong.MAX_VALUE) {
    logger.trace { "Invalid source bit: $sourceBit." }
    return
  }

  // --- 1. CONFIGURABLE DEBUGGING ---
  val playerPotentials = if (pieceColor == PlayerName.WHITE) whitePotentials else blackPotentials
  if (logger.isDebugEnabled()) {
    logger.trace { "--- UNDO USE POTENTIAL CALLED ---" }
    logger.trace { "Piece: $pieceColor $pieceType" }
    logger.trace {
      "Source Bit:                      0b${sourceBit.toString(2).padStart(40, '0')}"
    }
    logger.trace {
      "Target Bit:                      0b${targetBit.toString(2).padStart(40, '0')}"
    }
    logger.trace {
      "Player Potentials:               0b${playerPotentials.toString(2).padStart(40, '0')}"
    }
    logger.trace {
      "Board before removing potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}"
    }
  }

  // --- 2. PRE-CONDITION CHECKS (Input Validation) ---
  require(sourceBit.countOneBits() == 1) {
    "CRITICAL ERROR: sourceBit must be exactly one bit. Got: $sourceBit"
  }
  require(targetBit.countOneBits() == 1) {
    "CRITICAL ERROR: targetBit must be exactly one bit. Got: $targetBit"
  }

  fun safeRemoveAndCheck(board: ULong, boardName: String, removeAtIndex: ULong): ULong {
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board before removing $boardName piece:\n" +
                "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
                "$boardName bitboard:      0b${board.toString(2).padStart(40, '0')}")
      }
    }

    check((board and removeAtIndex) != 0UL) {
      "UNDO FAILED: Expected $pieceColor $pieceType in $boardName bitboard at $removeAtIndex, but it was missing."
    }

    val newBoard = board and removeAtIndex.inv()

    check((newBoard and removeAtIndex) == 0UL) {
      "UNDO FAILED: Failed to remove $pieceColor $pieceType piece from $boardName bitboard."
    }

    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board after removing $boardName piece\n" +
                "removeAtIndex: 0b${removeAtIndex.toString(2).padStart(40, '0')}\n" +
                "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}")
      }
    }
    logger.trace { "Successfully removed bit from $boardName." }

    return newBoard
  }

  fun safeAddAndCheck(board: ULong, boardName: String, addAtIndex: ULong): ULong {
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board before restoring potential at \n0b${addAtIndex.toString(2).padStart(40, '0')}\n0b${board.toString(2).padStart(40, '0')}")
      }
    }
    val newBoard = board or addAtIndex
    check((newBoard and addAtIndex) == addAtIndex) {
      "ADD FAILED: Failed to restore potential for $pieceColor $pieceType to $boardName bitboard at index $addAtIndex."
    }
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board after restoring potential at \n0b${addAtIndex.toString(2).padStart(40, '0')}\n0b${newBoard.toString(2).padStart(40, '0')}")
      }
    }
    logger.trace { "Successfully added bit to $boardName." }
    return newBoard
  }

  // --- 3. APPLY TO BITBOARDS ---
  when (pieceColor) {
    PlayerName.WHITE -> {
      when (pieceType) {
        PieceType.NULL -> {}
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {
          //       Use useTamskPotential()
/*          val isSourceValid =
              (sourceBit and whiteTAMSK and boardCenterSpotMask) == boardCenterSpotMask
          val isTargetValid = (targetBit and openningSpotsLineMask) == targetBit

          check(isSourceValid && isTargetValid) {
            buildString {
              appendLine("Illegal TAMSK Opening Move:")
              if (!isSourceValid) {
                appendLine(
                    "  -> Source Violation: Source index (0b${
										sourceBit.toString(2).padStart(40, '0')
									}) must be a White TAMSK piece located on the board center spot (0b${
										boardCenterSpotMask.toString(2).padStart(40, '0')
									})."
                )
              }
              if (!isTargetValid) {
                appendLine(
                    "  -> Target Violation: Target index (0b${
										targetBit.toString(2).padStart(40, '0')
									}) must be fully contained within the opening line mask (0b${
										openningSpotsLineMask.toString(2).padStart(40, '0')
									})."
                )
              }
            }
          }

          // TODO call undoAddPieceToBitboard
          whiteTAMSK = whiteTAMSK or targetBit
          whitePotentials = whitePotentials or boardCenterSpotMask*/
        }

        PieceType.ZERTZ -> {
          whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }

        PieceType.YINSH -> {
          whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH", targetBit)
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }

        // TODO something about this seems wrong
        PieceType.DVONN -> {
          for (i in 7 downTo 1) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whiteDVONNLayer[i] and targetBit) == targetBit &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and targetBit) == targetBit &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              blackDVONNLayer[i] =
                  safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", targetBit)
              break
            }
          }

          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }

        PieceType.PUNCT -> {
          for (i in 7 downTo 1) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whitePUNCTLayer[i] and targetBit) == targetBit &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeRemoveAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and targetBit) == targetBit &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeRemoveAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", targetBit)
              break
            }
          }

          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }
      }
    }

    PlayerName.BLACK -> {
      when (pieceType) {
        PieceType.NULL -> {}
        PieceType.GIPF -> {}
        PieceType.TAMSK -> {
/*          val isSourceValid =
              (sourceBit and blackTAMSK and boardCenterSpotMask) == boardCenterSpotMask
          val isTargetValid = (targetBit and openningSpotsLineMask) == targetBit

          check(isSourceValid && isTargetValid) {
            buildString {
              appendLine("Illegal TAMSK Opening Move:")
              if (!isSourceValid) {
                appendLine(
                    "  -> Source Violation: Source index (0b${
										sourceBit.toString(2).padStart(40, '0')
									}) must be a Black TAMSK piece located on the board center spot (0b${
										boardCenterSpotMask.toString(2).padStart(40, '0')
									})."
                )
              }
              if (!isTargetValid) {
                appendLine(
                    "  -> Target Violation: Target index (0b${
										targetBit.toString(2).padStart(40, '0')
									}) must be fully contained within the opening line mask (0b${
										openningSpotsLineMask.toString(2).padStart(40, '0')
									})."
                )
              }
            }
          }

          // TODO undoAddPieceToBitboard
          blackTAMSK = blackTAMSK and targetBit.inv()
          blackPotentials = blackPotentials or boardCenterSpotMask*/
        }

        PieceType.ZERTZ -> {
          blackZERTZ = blackZERTZ and targetBit.inv()
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }

        PieceType.YINSH -> {
          blackYINSH = blackYINSH and targetBit.inv()
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }

        // TODO confirm if possibleBitMove.targetBit/Spot if is white
        // TODO something about this seems wrong
        PieceType.DVONN -> {

          for (i in 7 downTo 1) {
            // place white on top of black
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whiteDVONNLayer[i] and targetBit) == targetBit &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackDVONNLayer[i] and targetBit) == targetBit &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              blackDVONNLayer[i] =
                  safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", targetBit)
              break
            }
          }

          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }

        PieceType.PUNCT -> {
          for (i in 7 downTo 1) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whitePUNCTLayer[i] and targetBit) == targetBit &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeRemoveAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackPUNCTLayer[i] and targetBit) == targetBit &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeRemoveAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", targetBit)
              break
            }
          }

          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }
      }
    }
  }

  if (logger.isDebugEnabled()) {
    logger.trace { "--- UNDO USE POTENTIAL COMPLETE ---" }
    logger.trace {
      "Board after removing potential: 0b${globalOccupancy.toString(2).padStart(40, '0')}"
    }
  }
}

fun Bitboard.useTamskPotential(move: UInt): ULong {
  // --- 0. EXTRACT PROPERTIES DIRECTLY FROM UInt ---
  val pieceColor = move.extractPieceColor()
  val sourceIndex = move.extractSourceBit()
  val targetIndex = move.extractTargetBit()
  val col = move.extractColumnInfo()
  val pushDirection = move.extractPushDirection() ?: error("Missing push direction in move: $move")

//  if (sourceIndex != boardCenterSpotMask || targetIndex == sourceIndex || move.extractPieceType() != PieceType.TAMSK || blackPotentials == 0UL || whitePotentials == 0UL) {
//    logger.trace { "Invalid source bit: $sourceIndex." }
//    return ULong.MAX_VALUE
//  }

  // --- 1. CONFIGURABLE DEBUGGING ---
  val playerPotentials = if (pieceColor == PlayerName.WHITE) whitePotentials else blackPotentials
  val playerTAMSK = if (pieceColor == PlayerName.WHITE) whiteTAMSK else blackTAMSK
  if (logger.isDebugEnabled()) {
    logger.trace { "--- USE TAMSK POTENTIAL CALLED ---" }
    logger.trace { "Player: $pieceColor | Source: $sourceIndex | Target: $targetIndex" }
    logger.trace { "PushDirection: $pushDirection | Col: $col" }
    logger.trace { "Player TAMSK:      0b${playerTAMSK.toString(2).padStart(40, '0')}" }
    logger.trace { "Player Potentials: 0b${playerPotentials.toString(2).padStart(40, '0')}" }
    logger.trace {
      "CenterMask:        0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
    }
  }

  // --- 2. PRE-CONDITION CHECKS (Input Validation) ---
  require(sourceIndex.countOneBits() == 1) {
    "CRITICAL ERROR: sourceIndex must be exactly one bit. Got: $sourceIndex"
  }
  require(targetIndex.countOneBits() == 1) {
    "CRITICAL ERROR: targetIndex must be exactly one bit. Got: $targetIndex"
  }

  // --- 3. RULE VALIDATION ---
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
    "STATE ERROR: $pieceColor does not have a Potential piece at the specified source index."
  }

  // --- 4. REMOVE POTENTIAL BEFORE SHIFTING ---
  when (pieceColor) {
    PlayerName.WHITE -> {
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("White TAMSK Board before adding piece:       0b${whiteTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("White Potentials Board before adding piece:  0b${whitePotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
      // Remove from Potentials board (using exact sourceIndex rather than broad mask)
      whitePotentials = whitePotentials and sourceIndex.inv()
      check((whitePotentials and sourceIndex) == 0UL) {
        "Failed to remove White Potential piece from source index."
      }
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("White TAMSK Board after adding piece:        0b${whiteTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("White Potentials Board after adding piece:   0b${whitePotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
    }

    PlayerName.BLACK -> {
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("Black TAMSK Board before adding piece:       0b${blackTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("Black Potentials Board before adding piece:  0b${blackPotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
      // Remove from Potentials board (using exact sourceIndex rather than broad mask)
      blackPotentials = blackPotentials and sourceIndex.inv()
      check((blackPotentials and sourceIndex) == 0UL) {
        "Failed to remove Black Potential piece from source index."
      }

      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              (" Black TAMSK Board after adding piece:        0b${blackTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("Black Potentials Board after adding piece:   0b${blackPotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
    }
  }

  // --- 5. HANDLE SHIFTING ---
  // Changed `> 0UL` to `!= 0UL` (Standard bitwise evaluation)
  val isIndexOccupied = (globalOccupancy and targetIndex) != 0UL
  var vacantBitFound = ULong.MAX_VALUE

  if (isIndexOccupied) {
    logger.trace { "Target index is occupied. Executing shift ($pushDirection)." }
    when (pushDirection) {
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        vacantBitFound = executePushUp(col)
        logger.trace { "Executed PushUp. Vacant bit found: $vacantBitFound" }
      }
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        vacantBitFound = executePushDown(col)
        logger.trace { "Executed PushDown. Vacant bit found: $vacantBitFound" }
      }
    }

    // Validate shift execution
    check(vacantBitFound.countOneBits() == 1) {
      "SHIFT ERROR: Shift execution must return exactly one vacant bit. Got: $vacantBitFound"
    }
  } else {
    logger.trace { "Target index is vacant. No shift required." }
  }

  // --- 6. APPLY TO BITBOARDS (With Post-Condition Checks) ---
  logger.trace { "Applying state changes for $pieceColor..." }

  when (pieceColor) {
    PlayerName.WHITE -> {
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("White TAMSK Board before adding piece:       0b${whiteTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("White Potentials Board before adding piece:  0b${whitePotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
      // Add to TAMSK board
      whiteTAMSK = whiteTAMSK or targetIndex
      check((whiteTAMSK and targetIndex) != 0UL) {
        "Failed to add White TAMSK piece to target index."
      }
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("White TAMSK Board after adding piece:        0b${whiteTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("White Potentials Board after adding piece:   0b${whitePotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
    }

    PlayerName.BLACK -> {
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("Black TAMSK Board before adding piece:       0b${blackTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("Black Potentials Board before adding piece:  0b${blackPotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }

      // Add to TAMSK board
      blackTAMSK = blackTAMSK or targetIndex
      check((blackTAMSK and targetIndex) != 0UL) {
        "Failed to add Black TAMSK piece to target index."
      }

      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("Black TAMSK Board after adding piece:        0b${blackTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("Black Potentials Board after adding piece:   0b${blackPotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
    }
  }

  if (logger.isDebugEnabled()) {
    logger.trace { "--- USE TAMSK POTENTIAL COMPLETE ---" }
  }

  return vacantBitFound
}

fun Bitboard.removeUnusedTamskPotential(currentPlayer: Player): UInt {

  //  if (sourceIndex != boardCenterSpotMask || targetIndex == sourceIndex ||
  // move.extractPieceType() != PieceType.TAMSK || blackPotentials == 0UL || whitePotentials == 0UL)
  // {
  //    logger.trace { "Invalid source bit: $sourceIndex." }
  //    return ULong.MAX_VALUE
  //  }

  // --- 1. CONFIGURABLE DEBUGGING ---
  val playerPotentials =
      if (currentPlayer.name == PlayerName.WHITE) whitePotentials else blackPotentials
  val playerTAMSK = if (currentPlayer.name == PlayerName.WHITE) whiteTAMSK else blackTAMSK
  if (logger.isDebugEnabled()) {
    logger.trace { "--- REMOVE UNUSED TAMSK POTENTIAL CALLED ---" }
    logger.trace { "Player: $currentPlayer.name" }
    logger.trace { "Player TAMSK:      0b${playerTAMSK.toString(2).padStart(40, '0')}" }
    logger.trace { "Player Potentials: 0b${playerPotentials.toString(2).padStart(40, '0')}" }
    logger.trace {
      "CenterMask:        0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
    }
  }

  // --- 3. RULE VALIDATION ---
  val isPotentialsValid = (playerPotentials and boardCenterSpotMask) == boardCenterSpotMask
  val isTamskValid = (playerTAMSK and boardCenterSpotMask) == boardCenterSpotMask

  check(isPotentialsValid && isTamskValid) {
    buildString {
      appendLine("ILLEGAL TAMSK OPENING MOVE:")
      if (!isPotentialsValid) {
        appendLine(
            "  -> Source Violation: Source index (0b${
            playerPotentials.toString(2).padStart(40, '0')
          }) must intersect the board center spot mask (0b${
            boardCenterSpotMask.toString(2).padStart(40, '0')
          })."
        )
      }
      if (!isTamskValid) {
        appendLine(
            "  -> Target Violation: Target index (0b${
            playerTAMSK.toString(2).padStart(40, '0')
          }) must be fully contained within the opening line mask (0b${
            boardCenterSpotMask.toString(2).padStart(40, '0')
          })."
        )
      }
    }
  }

  check(isPotentialsValid) {
    "STATE ERROR: $isPotentialsValid does not have a Potential piece at the board center spot."
  }

  // --- 4. REMOVE UNUSED POTENTIAL ---
  when (currentPlayer.name) {
    PlayerName.WHITE -> {
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("White TAMSK Board before removing TAMSK potential:       0b${whiteTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("White Potentials Board before removing TAMSK potential:  0b${whitePotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
      // Remove from Potentials board (using exact sourceIndex rather than broad mask)
      whitePotentials = whitePotentials and boardCenterSpotMask.inv()

      check((whitePotentials and boardCenterSpotMask) == 0UL) {
        "Failed to remove White Potential piece from board center spott."
      }
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("White TAMSK Board after removing TAMSK potential:        0b${whiteTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("White Potentials Board after removing TAMSK potential:   0b${whitePotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
    }

    PlayerName.BLACK -> {
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("Black TAMSK Board before removing TAMSK potential:       0b${blackTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("Black Potentials Board before removing TAMSK potential:  0b${blackPotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }

      // Remove from Potentials board (using exact sourceIndex rather than broad mask)
      blackPotentials = blackPotentials and boardCenterSpotMask.inv()

      check((blackPotentials and boardCenterSpotMask) == 0UL) {
        "Failed to remove Black Potential piece from board center spot."
      }

      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              (" Black TAMSK Board after removing TAMSK potential:        0b${blackTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("Black Potentials Board after removing TAMSK potential:   0b${blackPotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
    }
  }

  if (logger.isDebugEnabled()) {
    logger.trace { "--- REMOVE UNUSED TAMSK POTENTIAL COMPLETE ---" }
  }

  return 0u.createPiece(
    pieceType = PieceType.TAMSK,
    pieceColor = currentPlayer.name,
    potential = false,
  )
}

fun Bitboard.undoTamskPotential(
    move: UInt,
    vacantBitFound: ULong,
    wasIndexOccupied: Boolean,
) {
  // --- 0. EXTRACT PROPERTIES DIRECTLY FROM UInt ---
  val pieceColor = move.extractPieceColor()
  val sourceIndex = move.extractSourceBit()
  val removeAtIndex = move.extractTargetBit()
  val col = move.extractColumnInfo()
  val pushDirection = move.extractPushDirection() ?: error("Missing push direction in move: $move")

//  if (sourceIndex != boardCenterSpotMask || removeAtIndex == sourceIndex || move.extractPieceType() != PieceType.TAMSK || blackTAMSK == 0UL || whiteTAMSK == 0UL) {
//    logger.trace { "Invalid source bit: $sourceIndex." }
//    return ULong.MAX_VALUE
//  }

  // --- 1. CONFIGURABLE DEBUGGING ---
  val playerPotentials = if (pieceColor == PlayerName.WHITE) whitePotentials else blackPotentials
  val playerTAMSK = if (pieceColor == PlayerName.WHITE) whiteTAMSK else blackTAMSK
  if (logger.isDebugEnabled()) {
    logger.trace { "--- UNDO TAMSK POTENTIAL CALLED ---" }
    logger.trace { "Player: $pieceColor | Source: $sourceIndex | RemoveAt: $removeAtIndex" }
    logger.trace {
	    "WasOccupied: $wasIndexOccupied | PushDirection: $pushDirection | Col: $col"
    }
    logger.trace { "Player TAMSK:      0b${playerTAMSK.toString(2).padStart(40, '0')}" }
    logger.trace { "Player Potentials: 0b${playerPotentials.toString(2).padStart(40, '0')}" }
    logger.trace {
      "CenterMask:        0b${boardCenterSpotMask.toString(2).padStart(40, '0')}"
    }
  }

  // --- 2. PRE-CONDITION CHECKS (Input Validation) ---
  require(sourceIndex.countOneBits() == 1) {
    "CRITICAL ERROR: sourceIndex must be exactly one bit. Got: $sourceIndex"
  }
  require(removeAtIndex.countOneBits() == 1) {
    "CRITICAL ERROR: removeAtIndex must be exactly one bit. Got: $removeAtIndex"
  }
  if (wasIndexOccupied) {
    require(vacantBitFound.countOneBits() == 1 && wasIndexOccupied) {
      "CRITICAL ERROR: vacantBitFound must be exactly one bit if reverting a shift. Got: $vacantBitFound"
    }
  }

  // --- 3. RULE VALIDATION ---
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

  // --- 4. REVERT STATE MUTATIONS (With Pre- and Post-Condition Checks) ---
  logger.trace { "Reverting bitboards for $pieceColor..." }

  when (pieceColor) {
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

  // --- 5. REVERT SHIFTING ---
  if (wasIndexOccupied) {
    logger.trace { "Reverting board shift. Original push: $pushDirection" }
    when (pushDirection) {
      // If the original move pushed UP, we must pull DOWN to undo
      PushDirection.UP,
      PushDirection.UPPER_RIGHT,
      PushDirection.LOWER_RIGHT -> {
        executePullDown(col, vacantBitFound)
        logger.trace { "Executed PullDown on col $col" }
      }

      // If the original move pushed DOWN, we must pull UP to undo
      PushDirection.DOWN,
      PushDirection.UPPER_LEFT,
      PushDirection.LOWER_LEFT -> {
        executePullUp(col, vacantBitFound)
        logger.trace { "Executed PullUp on col $col" }
      }
    }
  } else {
    logger.trace { "Index was not previously occupied. No shifts to revert." }
  }

  // --- 6. RESTORE POTENTIAL AFTER SHIFTING ---
  logger.trace { "--- RESTORE POTENTIAL AFTER SHIFTING ---" }
  when (pieceColor) {
    PlayerName.WHITE -> {
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("White TAMSK Board before adding piece:       0b${whiteTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("White Potentials Board before adding piece:  0b${whitePotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }

      // Restore the potential piece
      whitePotentials = whitePotentials or sourceIndex
      check((whitePotentials and sourceIndex) != 0UL) {
        "Failed to restore White Potential piece to center."
      }
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("White TAMSK Board after adding piece:        0b${whiteTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("White Potentials Board after adding piece:   0b${whitePotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
    }

    PlayerName.BLACK -> {
      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("Black TAMSK Board before adding piece:       0b${blackTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("Potentials Board before adding piece:  0b${blackPotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }

      // Restore the potential piece
      blackPotentials = blackPotentials or sourceIndex
      check((blackPotentials and sourceIndex) != 0UL) {
        "Failed to restore Black Potential piece to center."
      }

      if (logger.isDebugEnabled()) {
        logger.trace {
          "" +
              ("TAMSK Board after adding piece:        0b${blackTAMSK.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("Potentials Board after adding piece:   0b${blackPotentials.toString(2).padStart(40, '0')}")
        }
        logger.trace {
          "" +
              ("CenterMask:                            0b${boardCenterSpotMask.toString(2).padStart(40, '0')}")
        }
      }
    }
  }

  logger.trace { "--- UNDO TAMSK POTENTIAL COMPLETE ---" }
}



fun Bitboard.removeSelectedPieces(
    player: Player,
    piecesToRemove: List<UInt>,
    movesBuffer: MutableList<UInt>,
    caller: String = ""
) {
  if (piecesToRemove.isEmpty()) {
    logger.trace { "Pieces to Remove was empty. Returned early." }
    return }

  require(piecesToRemove.isNotEmpty()) { "There must be at least one piece to remove." }

  // --- 0. CONFIGURABLE DEBUGGING ---
  if (logger.isDebugEnabled()) {
    logger.trace { "--- ${caller.uppercase()} CALLED --> CREATE RETRIEVE & CAPTURED PIECES CALLED ---" }
    logger.trace { "Bitboard State: ${Json.encodeToString<Bitboard>(this)}" }
    logger.trace { "Processing ${piecesToRemove.size} pieces..." }
    for ((index, piece) in piecesToRemove.withIndex()) {
//      if (piecesToRemove.size == 1 && piece == 16646028u && index == 0 && caller.uppercase() == "ALPHA-BETA MAIN BEGINNING BESTPIECESTOREMOVE() @ DEPTH 1") {
//        println()
//      }
      logger.trace { "piece $index: $piece" }
    }
  }

  val (ordinaryBitmasks, neutralizedBitmasks) =
      piecesToRemove.distinct().partition { !it.extractNeutralized() }

  // --- LOCAL HELPER: BASE LAYER REMOVAL ---
  fun safeRemoveAndCheck(board: ULong, boardName: String, bitmask: ULong, piece: UInt): ULong {
    if (board and bitmask == 0UL ) return board
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board before removing $boardName piece:\n" +
                "removeAtIndex:           0b${bitmask.toString(2).padStart(40, '0')}\n" +
                "$boardName bitboard:     0b${board.toString(2).padStart(40, '0')}")
      }
    }
    val newBoard = board and bitmask.inv()

    val extractPieceColor = piece.extractPieceColor()
    val extractPieceType = piece.extractPieceType()

    check((newBoard and bitmask) == 0UL) {
      "Failed to remove $extractPieceColor $extractPieceType from $boardName."
    }
    if (logger.isDebugEnabled()) {
      logger.trace {
        "" +
            ("$boardName Board after removing $boardName piece\n" +
                "removeAtIndex:            0b${bitmask.toString(2).padStart(40, '0')}\n" +
                "new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}")
      }
    }
    logger.trace { "  -> Successfully removed from $boardName." }
    return newBoard
  }

  //  val neutralizedPieces = mutableListOf<UInt>()
	for (it in neutralizedBitmasks) {
		val bitmask = it.extractTargetBit()
		stackLoop@ for (i in 7 downTo 1) {

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

			when {
				hasWhiteDVONN || hasBlackDVONN -> {
					// Determine base color based on which board it came from
					val baseColor = if (hasWhiteDVONN) PlayerName.WHITE else PlayerName.BLACK
					// Flip color if layer is odd
					val finalColor =
						if (isEven) baseColor
						else if (baseColor == PlayerName.WHITE) PlayerName.BLACK else PlayerName.WHITE

					//          val piece =
					//            Piece(
					//              abbreviation = "${finalColor.name.first()}D",
					//              potential = false,
					//              colorName = finalColor,
					//              type = PieceType.DVONN,
					//              isNeutralized = false,
					//              stackedPieces = mutableListOf(),
					//            )

					//          neutralizedPieces.add(
					//            RetrievedCapturedPieceBit(
					//              retrievedPiece = if (finalColor == player.name) piece else null,
					//              capturedPiece = if (finalColor != player.name) piece else null,
					//              bitmask = bitmask,
					//              isNeutralized = true,
					//            )
					//          )

					val piece =
						0u.createPiece(
							pieceType = PieceType.DVONN,
							pieceColor = finalColor,
							potential = false,
							isNeutralized = true,
						)

					movesBuffer.add(
						0u.packPossibleBitMove(
							piece = piece,
							targetBit = bitmask,
							moveType = MoveType.RetrieveCapturePieces,
						)
							.setRetrieveCapture(
								if (finalColor == player.name) RetrieveCapture.RETRIEVE
								else RetrieveCapture.CAPTURE
							)
					)

					blackDVONNLayer[i] =
						safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", bitmask, piece)
					whiteDVONNLayer[i] =
						safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", bitmask, piece)

					check((whiteDVONNLayer[i] and blackDVONNLayer[i]).countOneBits() == 0 ) {
						val overlapMask = whiteDVONNLayer[i] and blackDVONNLayer[i]
						"DVONN Layer Overlap Corrupted at index $i: White and Black pieces occupy identical coordinates. " +
								"Overlap Mask: 0x${overlapMask.toString(2)} (White: 0x${whiteDVONNLayer[i].toString(2)}, Black: 0x${blackDVONNLayer[i].toString(2)})"
					}

					break@stackLoop
				}

				hasWhitePUNCT || hasBlackPUNCT -> {
					val baseColor = if (hasWhitePUNCT) PlayerName.WHITE else PlayerName.BLACK
					val finalColor =
						if (isEven) baseColor
						else if (baseColor == PlayerName.WHITE) PlayerName.BLACK else PlayerName.WHITE

					//          val piece =
					//            Piece(
					//              abbreviation = "${finalColor.name.first()}P",
					//              potential = false,
					//              colorName = finalColor,
					//              type = PieceType.PUNCT,
					//              isNeutralized = false,
					//              stackedPieces = mutableListOf(),
					//            )
					//
					//          neutralizedPieces.add(
					//            RetrievedCapturedPieceBit(
					//              retrievedPiece = if (finalColor == player.name) piece else null,
					//              capturedPiece = if (finalColor != player.name) piece else null,
					//              bitmask = bitmask,
					//              isNeutralized = true,
					//            )
					//          )

					val piece =
						0u.createPiece(
							pieceType = PieceType.PUNCT,
							pieceColor = finalColor,
							potential = false,
							isNeutralized = true,
						)

					movesBuffer.add(
						0u.packPossibleBitMove(
							piece = piece,
							targetBit = bitmask,
							moveType = MoveType.RetrieveCapturePieces,
						)
							.setRetrieveCapture(
								if (finalColor == player.name) RetrieveCapture.RETRIEVE
								else RetrieveCapture.CAPTURE
							)
					)

					blackPUNCTLayer[i] =
						safeRemoveAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", bitmask, piece)
					whitePUNCTLayer[i] =
						safeRemoveAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", bitmask, piece)

					check((whitePUNCTLayer[i] and blackPUNCTLayer[i]).countOneBits() == 0 ) {
						val overlapMask = whitePUNCTLayer[i] and blackPUNCTLayer[i]
						"PUNCT Layer Overlap Corrupted at index $i: White and Black pieces occupy identical coordinates. " +
								"Overlap Mask: 0x${overlapMask.toString(2)} (White: 0x${whitePUNCTLayer[i].toString(2)}, Black: 0x${blackPUNCTLayer[i].toString(2)})"
					}

					break@stackLoop
				}
			}
		}
	}

  //  val pieces: List<UInt> =
	for (packedMove in ordinaryBitmasks) {
		val bitmask = packedMove.extractTargetBit()
		// construct piece and place it on the matching node
		val piece = addPieceToPackedMove(bitmask)

		when (piece?.extractPieceType()) {
			PieceType.NULL -> {}
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

      PieceType.YINSH -> {
        whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH", bitmask, piece)
        blackYINSH = safeRemoveAndCheck(blackYINSH, "Black YINSH", bitmask, piece)
      }

			PieceType.DVONN -> {
				if (
					(bitmask and whiteNeutralized) == bitmask || (bitmask and blackNeutralized) == bitmask
				) {
					throw IllegalStateException("")
				} else {
					whiteDVONNLayer[0] =
						safeRemoveAndCheck(whiteDVONNLayer[0], "White DVONN [0]", bitmask, piece)
					blackDVONNLayer[0] =
						safeRemoveAndCheck(blackDVONNLayer[0], "Black DVONN [0]", bitmask, piece)

					check((whiteDVONNLayer[0] and blackDVONNLayer[0]).countOneBits() == 0 ) {
						val overlapMask = whiteDVONNLayer[0] and blackDVONNLayer[0]
						"DVONN Layer Overlap Corrupted at index 0: White and Black pieces occupy identical coordinates. " +
								"Overlap Mask: 0x${overlapMask.toString(2)} (White: 0x${whiteDVONNLayer[0].toString(2)}, Black: 0x${blackDVONNLayer[0].toString(2)})"
					}
				}
			}

			PieceType.PUNCT -> {
				if (
					(bitmask and whiteNeutralized) == bitmask || (bitmask and blackNeutralized) == bitmask
				) {
					throw IllegalStateException("")
				} else {
					whitePUNCTLayer[0] =
						safeRemoveAndCheck(whitePUNCTLayer[0], "White PUNCT [0]", bitmask, piece)
					blackPUNCTLayer[0] =
						safeRemoveAndCheck(blackPUNCTLayer[0], "Black PUNCT [0]", bitmask, piece)

					check((whitePUNCTLayer[0] and blackPUNCTLayer[0]).countOneBits() == 0 ) {
						val overlapMask = whitePUNCTLayer[0] and blackPUNCTLayer[0]
						"PUNCT Layer Overlap Corrupted at index 0: White and Black pieces occupy identical coordinates. " +
								"Overlap Mask: 0x${overlapMask.toString(2)} (White: 0x${whitePUNCTLayer[0].toString(2)}, Black: 0x${blackPUNCTLayer[0].toString(2)})"
					}
				}
			}

			else ->
				throw IllegalStateException("Invalid Piece Type or Piece ($piece) is null. $packedMove")
		}

		if (piece.extractPotential()) {
			whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", bitmask, piece)
			blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", bitmask, piece)
		}

		//            RetrievedCapturedPieceBit(
		//                retrievedPiece = if (packedMove?.extractPieceColor() == player.name)
		// packedMove else
		// null,
		//                capturedPiece = if (packedMove?.extractPieceColor() != player.name) packedMove
		// else
		// null,
		//                bitmask = bitmask,
		//                isNeutralized = false,
		//            )

		movesBuffer.add(
			0u.packPossibleBitMove(
				piece = piece,
				targetBit = bitmask,
				moveType = MoveType.RetrieveCapturePieces,
			)
				.setRetrieveCapture(packedMove.extractRetrieveCapture())
		)
	}
  //          .plus(neutralizedPieces)
  //          .filter { it.retrievedPiece != null || it.capturedPiece != null }

  // TODO Check captured pieces aren't categorized as retrieved pieces and vice versa

  check(
      movesBuffer
          .filter { it.extractRetrieveCapture() == RetrieveCapture.RETRIEVE }
          .all { it.extractPieceColor() == player.name }
  ) {
    val invalidRetrieved =
        movesBuffer
            .filter {
              it.extractRetrieveCapture() == RetrieveCapture.RETRIEVE &&
                  it.extractPieceColor() != player.name
            }
            .map { "${it.extractPieceType()}(${it.extractPieceColor()})" }
    "Scoring Violation: Player '${player.name}' attempted to retrieve opponent pieces: $invalidRetrieved"
  }

  check(
      movesBuffer
          .filter { it.extractRetrieveCapture() == RetrieveCapture.CAPTURE }
          .all { it.extractPieceColor() != player.name }
  ) {
    val invalidCaptured =
        movesBuffer
            .filter {
              it.extractRetrieveCapture() == RetrieveCapture.CAPTURE &&
                  it.extractPieceColor() == player.name
            }
            .map { "${it.extractPieceType()}(${it.extractPieceColor()})" }
    "Scoring Violation: Player '${player.name}' accidentally captured their own pieces: $invalidCaptured"
  }

  check(piecesToRemove.size == movesBuffer.size) {
    "State Mutation Desynchronization: Mismatch between targeted piece removals (${piecesToRemove.size}) " +
        "and buffered move operations (${movesBuffer.size}). " +
        "Removals: $piecesToRemove | Moves: $movesBuffer"
  }

  if (logger.isDebugEnabled()) {
    logger.trace { "Bitboard State: ${Json.encodeToString<Bitboard>(this)}" }
    logger.trace { "Pieces retrieved & captured (count: ${movesBuffer.size}):" }
	  for ((index, piece) in movesBuffer.withIndex()) {
		  logger.trace { "Piece $index: $piece" }
	  }
    logger.trace { "--- CREATE RETRIEVE & CAPTURED PIECES COMPLETED ---" }
    logger.trace { "====================================================" }
  }

  //  return pieces
}

fun Bitboard.addPieceToPackedMove(bitmask: ULong): UInt? {

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
          when (bitmask) {
            (whiteGIPF and bitmask) -> PieceType.GIPF
            (whiteTAMSK and bitmask) -> PieceType.TAMSK
            (whiteZERTZ and bitmask) -> PieceType.ZERTZ
            (whiteYINSH and bitmask) -> PieceType.YINSH
            (whiteDVONNLayer[0] and bitmask) -> PieceType.DVONN
            (whitePUNCTLayer[0] and bitmask) -> PieceType.PUNCT
            else -> null
          }
        }

        PlayerName.BLACK -> {
          when (bitmask) {
            (blackGIPF and bitmask) -> PieceType.GIPF
            (blackTAMSK and bitmask) -> PieceType.TAMSK
            (blackZERTZ and bitmask) -> PieceType.ZERTZ
            (blackYINSH and bitmask) -> PieceType.YINSH
            (blackDVONNLayer[0] and bitmask) -> PieceType.DVONN
            (blackPUNCTLayer[0] and bitmask) -> PieceType.PUNCT
            else -> null
          }
        }
      }

  /*  val stackedPieces: MutableList<Piece> =
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
              if (index in 1..7 && (layer and bitmask) == bitmask) {
                val pieceColor = if (index % 2 == 0) PlayerName.WHITE else PlayerName.BLACK
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
                val pieceColor = if (index % 2 == 0) PlayerName.WHITE else PlayerName.BLACK
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
        PieceType.GIPF,
        PieceType.TAMSK,
        PieceType.ZERTZ,
        PieceType.YINSH,
        null -> mutableListOf()

        PieceType.DVONN -> {
          blackDVONNLayer
            .mapIndexedNotNull { index, layer ->
              if (index in 1..7 && (layer and bitmask) == bitmask) {
                val pieceColor = if (index % 2 == 0) PlayerName.BLACK else PlayerName.WHITE
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
                val pieceColor = if (index % 2 == 0) PlayerName.BLACK else PlayerName.WHITE
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
  }*/

  //
  //  return pieceType?.let {
  //    Piece(
  //      abbreviation = "${color.name.first()}${pieceType.name.first()}",
  //      potential = hasPotential,
  //      colorName = color,
  //      type = it,
  //      isNeutralized = isNeutralized,
  //      stackedPieces = stackedPieces,
  //    )
  //  }

  return pieceType?.let {
    0u.createPiece(
        pieceType = it,
        pieceColor = color,
        potential = hasPotential,
        isNeutralized = isNeutralized,
    )
  }
}

fun Bitboard.undoRetrieveAndCapturePieces(retrievedCapturedPieces: List<UInt>, caller: String = "") {
  // --- 0. CONFIGURABLE DEBUGGING ---
  if (logger.isDebugEnabled()) {
    logger.trace { "--- ${caller.uppercase()} CALLED --> UNDO REMOVE/RETRIEVE CAPTURED PIECES CALLED ---" }
    logger.trace { "Bitboard State: ${Json.encodeToString<Bitboard>(this)}" }
    logger.trace { "Processing ${retrievedCapturedPieces.size} pieces..." }
	  for ((index, piece) in retrievedCapturedPieces.withIndex()) {
		  logger.trace { "piece $index: $piece" }
	  }
  }

	for ((index, data) in retrievedCapturedPieces.withIndex()) {
		val bitmask = data.extractTargetBit()
		val isNeutralized = data.extractNeutralized()
		val piece = data.onlyPiece()

		if (piece.extractPieceType() == null) {
			logger.trace { "Item $index: Both retrieved and captured pieces are null. Skipping." }
			continue
		}

		if (logger.isDebugEnabled()) {
			logger.trace {
				"" +
						("Processing Piece $index: ${piece.extractPieceColor()} ${piece.extractPieceType()} at $bitmask")
			}
			logger.trace {
				"  -> isNeutralized: $isNeutralized | Potential: ${piece.extractPotential()}"
			}
		}

		// --- 1. PRE-CONDITION CHECKS ---
		require(bitmask.countOneBits() == 1) {
			"CRITICAL ERROR: Removal bitmask must be exactly one bit. Got: $bitmask"
		}

		fun safeAddAndCheck(board: ULong, boardName: String): ULong {
			if (logger.isDebugEnabled()) {
				logger.trace {
					"" +
							("$boardName Board before restoring $boardName piece\n" +
									"addAtIndex: 0b${bitmask.toString(2).padStart(40, '0')}\n" +
									"$boardName bitboard:  0b${board.toString(2).padStart(40, '0')}")
				}
			}
			val newBoard = board or bitmask
			check(newBoard != board) {
				"ADD FAILED: Failed to add ${piece.extractPieceColor()} ${piece.extractPieceType()} to $boardName bitboard at index $bitmask."
			}
			if (logger.isDebugEnabled()) {
				logger.trace {
					"" +
							("$boardName Board after restoring $boardName piece\n" +
									"addAtIndex: 0b${bitmask.toString(2).padStart(40, '0')}\n" +
									"new $boardName bitboard:  0b${newBoard.toString(2).padStart(40, '0')}")
				}
			}
			logger.trace { "Successfully added bit to $boardName." }
			return newBoard
		}

		when (piece.extractPieceColor()) {
			PlayerName.WHITE -> {
				when (piece.extractPieceType()) {
					PieceType.GIPF -> whiteGIPF = safeAddAndCheck(whiteGIPF, "White GIPF")
					PieceType.TAMSK -> whiteTAMSK = safeAddAndCheck(whiteTAMSK, "White TAMSK")
					PieceType.ZERTZ -> whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ")
					PieceType.YINSH -> whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH")
					else -> {} // DVONN and PUNCT handled below
				}
			}

			PlayerName.BLACK -> {
				when (piece.extractPieceType()) {
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
			for (i in 1..7) {
				if (pieceRemoved) break

				when (piece.extractPieceColor()) {
					PlayerName.WHITE -> {
						when (piece.extractPieceType()) {
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

					PlayerName.BLACK -> {
						when (piece.extractPieceType()) {
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
			when (piece.extractPieceColor()) {
				PlayerName.WHITE -> {
					when (piece.extractPieceType()) {
						PieceType.DVONN -> {
							whiteDVONNLayer[0] = safeAddAndCheck(whiteDVONNLayer[0], "White DVONN[0]")
						}

						PieceType.PUNCT -> {
							whitePUNCTLayer[0] = safeAddAndCheck(whitePUNCTLayer[0], "White PUNCT[0]")
						}

						else -> {}
					}
				}

				PlayerName.BLACK -> {
					when (piece.extractPieceType()) {
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

		if (piece.extractPotential()) {
			when (piece.extractPieceColor()) {
				PlayerName.WHITE -> {
					whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials")
				}

				PlayerName.BLACK -> {
					blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials")
				}
			}
		}
	}

  for (i in 0..7) {
    check((whiteDVONNLayer[i] and blackDVONNLayer[i]).countOneBits() == 0) {
      val overlapMask = whiteDVONNLayer[i] and blackDVONNLayer[i]
      "DVONN Layer Overlap Corrupted at index $i: White and Black pieces occupy identical coordinates. " +
          "Overlap Mask: 0x${overlapMask.toString(2)} (White: 0x${whiteDVONNLayer[i].toString(2)}, " +
          "Black: 0x${blackDVONNLayer[i].toString(2)})"
    }
    check((whitePUNCTLayer[i] and blackPUNCTLayer[i]).countOneBits() == 0) {
      val overlapMask = whitePUNCTLayer[i] and blackPUNCTLayer[i]
      "PUNCT Layer Overlap Corrupted at index $i: White and Black pieces occupy identical coordinates. " +
          "Overlap Mask: 0x${overlapMask.toString(2)} (White: 0x${whitePUNCTLayer[i].toString(2)}, " +
          "Black: 0x${blackPUNCTLayer[i].toString(2)})"
    }
  }

  if (logger.isDebugEnabled()) {
    logger.trace { "Bitboard State: ${Json.encodeToString<Bitboard>(this)}" }
    logger.trace { "--- UNDO REMOVE/RETRIEVE CAPTURED PIECES COMPLETED ---" }
    logger.trace { "=============================================" }
  }
}
