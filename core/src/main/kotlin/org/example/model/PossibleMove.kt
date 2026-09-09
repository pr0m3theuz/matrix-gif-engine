package org.example.model

import kotlinx.serialization.Serializable
import org.example.toBitList
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray

enum class MoveType {
  AddPiece,
  UsePotential,
  RetrieveCapturePieces,
  UnusedTamskPotential,
}

enum class TurnPhase {
  ExtraMove,
  PlayerInputWindow,
  PieceRemoval,
}

enum class RetrieveCapture {
  RETRIEVE,
  CAPTURE,
}

@Serializable
data class PossibleMove(
    val piece: Piece? = null,
    val selectableDots: Set<NodeConnections> = emptySet(),
    val selectedBit: ULong = 0UL,
    val pushDirection: PushDirection? = null,
    val eligiblePotentialPieceNode: Node? = null,
    val eligiblePotentialTargetNodes: Set<Node> = emptySet(),
    val moveType: MoveType,
)

@Serializable
data class PossibleBitMove(
    val piece: Piece? = null,
    val sourceBit: ULong? = null,
    val targetBit: ULong? = null,
    val pieceType: PieceType? = null,
    val pieceColor: PlayerName? = null,
    val pushDirection: PushDirection? = null,
    val moveType: MoveType,
    val retrievedCapturedPiecesBit: List<RetrievedCapturedPieceBit> = emptyList(),
    val columnInfo: ColumnInfo? = null,
) {
  fun encode(): NDArray<Int, D1> {
    return when (moveType) {
      MoveType.AddPiece,
      MoveType.UsePotential -> {
        requireNotNull(targetBit) { "Target bit must not be null" }
        targetBit.toBitList()
      }
      MoveType.UnusedTamskPotential -> { boardCenterSpotMask.toBitList() }
      MoveType.RetrieveCapturePieces -> {
        retrievedCapturedPiecesBit
            .fold(0UL) { acc, bit ->
              acc or bit.bitmask
            }
            .toBitList()
      }
    }
  }
}

fun UInt.packPossibleBitMove(
  piece: UInt = 0u,
    sourceBit: ULong = 0xFFFFFFFFFFFFFFFFUL,
    targetBit: ULong,
    pushDirection: PushDirection? = null,
    moveType: MoveType,
    columnInfoIndex: Int = 31,
): UInt {
  // 7 bits allocated to safely store up to '64' when the mask is 0uL
  val target = targetBit.countTrailingZeroBits().toUInt()
  val source =
      if (sourceBit == 0xFFFFFFFFFFFFFFFFUL) 127u shl 7
      else sourceBit.countTrailingZeroBits().toUInt() shl 7

  val pushDir = (pushDirection?.ordinal?.toUInt() ?: 7u) shl 14
  val move = moveType.ordinal.toUInt() shl 17
  val index = columnInfoIndex.toUInt() shl 19
  val p = if (piece != 0u) piece else 0u

  return target or source or pushDir or move or index or p
}

fun UInt.toPossibleBitMove(): PossibleBitMove {
  // 1. Unpack the piece FIRST so we can harvest its internal data
  val unpackedPiece = this.extractPiece()

  val sourceShift = (this shr 7 and 0b1111111.toUInt()).toInt()
  val targetShift = (this and 0b1111111.toUInt()).toInt()

  return PossibleBitMove(
      piece = unpackedPiece,

      // 2. Safely restore 0uL if the shift value was 64
      sourceBit = if (sourceShift == 127) null else (1uL shl sourceShift),
      targetBit = if (targetShift == 127) null else (1uL shl targetShift),

      // 3. Directly map the removed duplications from the unpacked piece
      pieceType = this.extractPieceType(),
      pieceColor = this.extractPieceColor(),
      pushDirection =
          when ((this shr 14 and 0b111.toUInt()).toInt()) {
            PushDirection.UP.ordinal -> PushDirection.UP
            PushDirection.DOWN.ordinal -> PushDirection.DOWN
            PushDirection.UPPER_RIGHT.ordinal -> PushDirection.UPPER_RIGHT
            PushDirection.LOWER_RIGHT.ordinal -> PushDirection.LOWER_RIGHT
            PushDirection.UPPER_LEFT.ordinal -> PushDirection.UPPER_LEFT
            PushDirection.LOWER_LEFT.ordinal -> PushDirection.LOWER_LEFT
            else -> null
          },
      moveType =
          when ((this shr 17 and 0b11.toUInt()).toInt()) {
            MoveType.AddPiece.ordinal -> MoveType.AddPiece
            MoveType.UsePotential.ordinal -> MoveType.UsePotential
            MoveType.RetrieveCapturePieces.ordinal -> MoveType.RetrieveCapturePieces
            MoveType.UnusedTamskPotential.ordinal -> MoveType.UnusedTamskPotential
            else -> error("Unknown move type for packed int: $this")
          },
      columnInfo =
          run {
            val index = (this shr 19 and 0b11111.toUInt()).toInt()
            if (index == 31) return@run null
            check(index in columnInfos.indices)
            return@run columnInfos[index]
          },
  )
}

fun UInt.extractPiece(): Piece? {
  // Piece is now packed at bit 24 and takes 6 bits
  val pieceByte = this and (0b111111.toUInt() shl 24)
  return extractPieceType()?.let {
	  Piece(
		  abbreviation = extractPieceColor().name.first().toString() + it.name.first().toString(),
		  potential = extractPotential(),
		  colorName = extractPieceColor(),
		  type = it,
		  isNeutralized = this.extractNeutralized(),
	  )
  }
}

fun UInt.onlyPiece(): UInt {
  // Piece is now packed at bit 24 and takes 6 bits
  return this and 62u.shl(24)
}

fun UInt.extractSourceBit(): ULong {
  // Source is at bit 7, taking 7 bits to safely handle 64 (empty)
  val shift = (this shr 7) and 0b1111111u
  return if (shift == 127u) ULong.MAX_VALUE else 1uL shl shift.toInt()
}

fun UInt.extractTargetBit(): ULong {
  // Target is at bit 0, taking 7 bits to safely handle 64 (empty)
  val shift = this and 0b1111111u
  return if (shift == 64u) 0uL else 1uL shl shift.toInt()
}

fun UInt.createPiece(pieceType: PieceType, pieceColor: PlayerName, potential: Boolean, isNeutralized: Boolean = false): UInt {
  val pieceType = pieceType.ordinal.toUInt() shl 26

  val color = pieceColor.ordinal.toUInt() shl 29

  val potential =
    when (potential) {
      true -> 1u
      false -> 0u
    } shl 25

  val neutralized =
    when (isNeutralized) {
      true -> 1u
      false -> 0u
    } shl 24

  return this or pieceType or potential or color or neutralized
}

fun UInt.setPieceType(pieceType: PieceType): UInt {
  // PieceType is bits 0-2 of the piece, which starts at 26 (so bits 26-26)
  return (this xor (0u shl 26)) or (pieceType.ordinal.toUInt() shl 26)
}

fun UInt.extractPieceType(): PieceType? {
  // PieceType is bits 0-2 of the piece, which starts at 26 (so bits 26-26)
  return when (((this shr 26) and 0b111u).toInt()) {
    PieceType.NULL.ordinal -> null
    PieceType.GIPF.ordinal -> PieceType.GIPF
    PieceType.TAMSK.ordinal -> PieceType.TAMSK
    PieceType.ZERTZ.ordinal -> PieceType.ZERTZ
    PieceType.YINSH.ordinal -> PieceType.YINSH
    PieceType.DVONN.ordinal -> PieceType.DVONN
    PieceType.PUNCT.ordinal -> PieceType.PUNCT
    else -> null
  }
}

fun UInt.extractPushDirection(): PushDirection? {
  // PushDirection is at bit 14, taking 3 bits
  return when (((this shr 14) and 0b111u).toInt()) {
    PushDirection.UP.ordinal -> PushDirection.UP
    PushDirection.DOWN.ordinal -> PushDirection.DOWN
    PushDirection.UPPER_RIGHT.ordinal -> PushDirection.UPPER_RIGHT
    PushDirection.LOWER_RIGHT.ordinal -> PushDirection.LOWER_RIGHT
    PushDirection.UPPER_LEFT.ordinal -> PushDirection.UPPER_LEFT
    PushDirection.LOWER_LEFT.ordinal -> PushDirection.LOWER_LEFT
    else -> null
  }
}

fun UInt.extractMoveType(): MoveType {
  // MoveType is at bit 17, taking 2 bits
  return when (((this shr 17) and 0b11u).toInt()) {
    MoveType.AddPiece.ordinal -> MoveType.AddPiece
    MoveType.UsePotential.ordinal -> MoveType.UsePotential
    MoveType.RetrieveCapturePieces.ordinal -> MoveType.RetrieveCapturePieces
    MoveType.UnusedTamskPotential.ordinal -> MoveType.UnusedTamskPotential
    else -> error("Unknown move type: $this")
  }
}

fun UInt.extractColumnInfo(): ColumnInfo {
  // Index is at bit 19, taking 5 bits
  val index = ((this shr 19) and 0b11111u).toInt()
  check(index in columnInfos.indices) { "Column index $index out of bounds" }
  return columnInfos[index]
}

fun UInt.setPieceColor(pieceColor: PlayerName): UInt {
  // Color is bit 4 of the piece, which starts at 24 (so bit 29)
  return when (pieceColor) {
    PlayerName.WHITE -> this and (1u shl 29).inv()
    PlayerName.BLACK -> {
      (this xor (1u shl 29)) or (pieceColor.ordinal.toUInt() shl 29)
    }
  }
}

fun UInt.extractPieceColor(): PlayerName {
  // Color is bit 4 of the piece, which starts at 24 (so bit 29)
  return when (((this shr 29) and 1u).toInt()) {
    PlayerName.WHITE.ordinal -> PlayerName.WHITE
    PlayerName.BLACK.ordinal -> PlayerName.BLACK
    else -> error("Unknown piece color type for packed int: $this")
  }
}

fun UInt.setPotential(potential: Boolean): UInt {
  // Potential is bit 3 of the piece, starting at bit 24 (so bit 25)
  return if (potential) {
    (this xor (1u shl 25)) or (1u shl 25)
  } else {
    this and (1u shl 25).inv() // Correctly clears the bit to 0
  }
}

fun UInt.extractPotential(): Boolean {
  return ((this shr 25) and 1u) == 1u
}

fun UInt.setNeutralized(neutralized: Boolean): UInt {
  // Neutralized is bit 5 of the piece, starting at bit 24 (so bit 24)
  return if (neutralized) {
    (this xor (1u shl 24)) or (1u shl 24)
  } else {
    this and (1u shl 24).inv() // Correctly clears the bit to 0
  }
}

fun UInt.extractNeutralized(): Boolean {
  return ((this shr 24) and 1u) == 1u
}

fun UInt.setRetrieveCapture(retrieveCapture: RetrieveCapture): UInt {
  // RetrieveCapture is bit 6 of the piece, starting at bit 24 (so bit 30)
  return when (retrieveCapture ) {
	  RetrieveCapture.RETRIEVE -> {
      this and (1u shl 30).inv() // Correctly clears the bit to 0
    }
	  RetrieveCapture.CAPTURE -> {
      (this xor (1u shl 30)) or (1u shl 30)
    }
  }
}

fun UInt.extractRetrieveCapture(): RetrieveCapture {
  return when (((this shr 30) and 1u).toInt()) {
    RetrieveCapture.RETRIEVE.ordinal -> RetrieveCapture.RETRIEVE
    RetrieveCapture.CAPTURE.ordinal -> RetrieveCapture.CAPTURE
    else -> error("Unknown retrieval: $this")
  }
}
