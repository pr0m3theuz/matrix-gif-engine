package utils

import org.example.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class BitPackingTests {

  @Test
  fun `test pack and unpack round-trip with standard values`() {
    // Arrange
    val sourceBit = 1UL shl 39
    val targetBit = 1UL shl 39
    val pieceType = PieceType.PUNCT
    val pieceColor = PlayerName.BLACK
    val pushDirection = PushDirection.LOWER_LEFT
    val moveType = MoveType.RetrieveCapturePieces
    val columnInfoIndex = columnInfos.lastIndex

    // Act
    val packedMove =
        0u.packPossibleBitMove(
                piece = 0u,
                sourceBit = sourceBit,
                targetBit = targetBit,
                pushDirection = pushDirection,
                moveType = moveType,
                columnInfoIndex = columnInfoIndex,
            )
            .setPieceType(pieceType)
            .setPieceColor(pieceColor)

    val unpackedMove = packedMove.toPossibleBitMove()

    // Assert
    assertEquals(sourceBit, unpackedMove.sourceBit, "Source bit mismatch")
    assertEquals(targetBit, unpackedMove.targetBit, "Target bit mismatch")
    assertEquals(pieceType, unpackedMove.pieceType, "Piece type mismatch")
    assertEquals(pieceColor, unpackedMove.pieceColor, "Piece color mismatch")
    assertEquals(pushDirection, unpackedMove.pushDirection, "Push direction mismatch")
    assertEquals(moveType, unpackedMove.moveType, "Move type mismatch")
    assertEquals(
        columnInfos[columnInfoIndex],
        unpackedMove.columnInfo,
        "Column info mismatch",
    )
  }

  @Test
  fun `test pack and unpack round-trip with boundary values`() {
    // Arrange
    // Use boundary indices (0 and 63) for 64-bit ULong representation
    val sourceBit = 1UL shl 39
    val targetBit = 1UL shl 39
    val pieceType = PieceType.PUNCT
    val pieceColor = PlayerName.BLACK
    val pushDirection = PushDirection.LOWER_LEFT
    val moveType = MoveType.RetrieveCapturePieces
    val columnInfoIndex = columnInfos.lastIndex // Max 5-bit index

    // Act
    val packedMove =
        0u.packPossibleBitMove(
                piece = 0u,
                sourceBit = sourceBit,
                targetBit = targetBit,
                pushDirection = pushDirection,
                moveType = moveType,
                columnInfoIndex = columnInfoIndex,
            )
            .setPieceType(pieceType)
            .setPieceColor(pieceColor)

    val unpackedMove = packedMove.toPossibleBitMove()

    // Assert
    assertEquals(sourceBit, unpackedMove.sourceBit, "Boundary Source bit mismatch")
    assertEquals(targetBit, unpackedMove.targetBit, "Boundary Target bit mismatch")
    assertEquals(pieceType, unpackedMove.pieceType)
    assertEquals(pieceColor, unpackedMove.pieceColor)
    assertEquals(pushDirection, unpackedMove.pushDirection)
    assertEquals(moveType, unpackedMove.moveType)
    assertEquals(columnInfos[columnInfoIndex], unpackedMove.columnInfo)
  }

  @Test
  fun `test packing logic outputs expected bit structure`() {
    // Checking against a manually calculated ULong bitmask
    val sourceIndex = 4 // trailing zeros = 4
    val targetIndex = 2 // trailing zeros = 2
    val pieceType = PieceType.ZERTZ // ordinal 2
    val pieceColor = PlayerName.BLACK // ordinal 1
    val direction = PushDirection.DOWN // ordinal 1
    val moveType = MoveType.AddPiece // ordinal 0
    val columnIndex = 3

    val packed =
        0u.packPossibleBitMove(
                piece = 0u,
                sourceBit = 1UL shl sourceIndex,
                targetBit = 1UL shl targetIndex,
                pushDirection = direction,
                moveType = moveType,
                columnInfoIndex = columnIndex,
            )
            .setPieceType(pieceType)
            .setPieceColor(pieceColor)

    // Expected bits based on the new layout:
    // target     (bits 0-5):   2 (0b000010)                 -> 2
    // source     (bits 6-11):  4 (0b000100 shl 6)           -> 256
    // pieceType  (bits 12-14): 2 (0b010 shl 12)             -> 8192
    // pieceColor (bit 15):     1 (0b1 shl 15)               -> 32768
    // direction  (bits 16-18): 1 (0b001 shl 16)             -> 65536
    // moveType   (bits 19-20): 0 (0b00 shl 19)              -> 0
    // column     (bits 21-25): 3 (0b00011 shl 21)           -> 6291456

    val expected: UInt = 2u or 256u or 8192u or 32768u or 65536u or 0u or 6291456u

    assertEquals(expected, packed, "Manual bit calculation mismatch")
  }

  @Test
  fun `Piece pack and unpack round trip`() {
    val piece =
        Piece(
            abbreviation = "BP",
            potential = true,
            colorName = PlayerName.BLACK,
            type = PieceType.PUNCT,
        )

    val packedPiece = piece.pack()
    val unpackedPiece = packedPiece.extractPiece()

    assertEquals(piece.type, unpackedPiece?.type)
    assertEquals(piece.potential, unpackedPiece?.potential)
    assertEquals(piece.colorName, unpackedPiece?.colorName)
    assertEquals(piece.abbreviation, unpackedPiece?.abbreviation)
  }

  @Test
  fun `Piece manual bit structure calculation`() {
    val piece = Piece("WZ", false, PlayerName.WHITE, PieceType.ZERTZ)

    // ZERTZ (ordinal 2) -> 2
    // Potential False -> 0
    // WHITE (ordinal 0) -> 0
    // Expected byte: 2
    assertEquals(2.toUByte(), piece.pack())

    val piece2 = Piece("BY", true, PlayerName.BLACK, PieceType.YINSH)
    // YINSH (ordinal 4) -> 4
    // Potential True (bit 3) -> 8
    // BLACK (ordinal 1, bit 4) -> 16
    // Expected byte: 4 + 8 + 16 = 28
    assertEquals(28.toUByte(), piece2.pack())
  }

  @Test
  fun `BitMove pack and unpack round trip with a packed Piece`() {
    val piece = Piece("WT", true, PlayerName.WHITE, PieceType.TAMSK)

    val sourceBit = 1UL shl 42
    val targetBit = 1UL shl 10
    val pieceType = PieceType.TAMSK
    val pieceColor = PlayerName.WHITE
    val pushDirection = PushDirection.LOWER_RIGHT
    val moveType = MoveType.AddPiece
    val columnInfoIndex = 14

    val packedMove =
        0u.packPossibleBitMove(
                piece = piece.pack(),
                sourceBit = sourceBit,
                targetBit = targetBit,
                pushDirection = pushDirection,
                moveType = moveType,
                columnInfoIndex = columnInfoIndex,
            )
//            .setPieceType(pieceType)
//            .setPieceColor(pieceColor)

    val unpackedMove = packedMove.toPossibleBitMove()

    assertEquals(sourceBit, unpackedMove.sourceBit)
    assertEquals(targetBit, unpackedMove.targetBit)
    assertEquals(pieceType, unpackedMove.pieceType)
    assertEquals(pieceColor, unpackedMove.pieceColor)
    assertEquals(pushDirection, unpackedMove.pushDirection)
    assertEquals(moveType, unpackedMove.moveType)
    assertEquals(columnInfos[columnInfoIndex], unpackedMove.columnInfo)

    // Assert the nested piece unpacked correctly
    assertNotNull(unpackedMove.piece)
    assertEquals(piece.type, unpackedMove.piece.type)
    assertEquals(piece.potential, unpackedMove.piece.potential)
    assertEquals(piece.colorName, unpackedMove.piece.colorName)
  }

  @Test
  fun `BitMove manual bit calculation to ensure exact UInt usage`() {
    // Arrange
    val pieceByte: UInt = 28u shl 24 // Represents BLACK, potential=true, YINSH
    val sourceIndex = 4
    val targetIndex = 2
    val pieceType = PieceType.ZERTZ // ordinal 2
    val pieceColor = PlayerName.BLACK // ordinal 1
    val direction = PushDirection.DOWN // ordinal 1
    val moveType = MoveType.UsePotential // ordinal 1
    val columnIndex = 3

    val packed =
        0u.packPossibleBitMove(
                piece = pieceByte,
                sourceBit = 1UL shl sourceIndex,
                targetBit = 1UL shl targetIndex,
                pushDirection = direction,
                moveType = moveType,
                columnInfoIndex = columnIndex,
            )
            .setPieceType(pieceType)
            .setPieceColor(pieceColor)

    // Expected bits based on 32-bit layout:
    // target     (bits 0-5):   2                     -> 2
    // source     (bits 6-11):  4 (4 shl 6)           -> 256
    // pieceType  (bits 12-14): 2 (2 shl 12)          -> 8192
    // pieceColor (bit 15):     1 (1 shl 15)          -> 32768
    // direction  (bits 16-18): 1 (1 shl 16)          -> 65536
    // moveType   (bits 19-20): 1 (1 shl 19)          -> 524288
    // column     (bits 21-25): 3 (3 shl 21)          -> 6291456
    // piece      (bits 26-31): 28 (28 shl 26)        -> 1879048192

    val expected: UInt =
        2u or 256u or 8192u or 32768u or 65536u or 524288u or 6291456u or 1879048192u

    assertEquals(expected, packed, "The bitwise structure failed to match the expected UInt map")
  }

  @Test
  fun unpackingUInts() {
    val packedValue: UInt = 8192u
    val possibleBitMove = packedValue.toPossibleBitMove()
    val unpackedPiece = packedValue.extractPiece()
    val expectedPiece =
        Piece(
            abbreviation = "WG",
            potential = false,
            colorName = PlayerName.WHITE,
            type = PieceType.GIPF,
            isNeutralized = false,
        )

    assertEquals(expectedPiece, unpackedPiece)
  }

  @Test
  fun whitePunct() {
    val packedValue: UInt = 503040657u
    val expectedValue = 234605201u
    val expectedPiece = expectedValue.extractPiece()
    val expectedColor = expectedValue.extractPieceColor()

    val reconstructedValue =
        0u.packPossibleBitMove(
                sourceBit = 2097152UL,
                targetBit = 131072UL,
                moveType = MoveType.UsePotential,
            )
            .setPieceType(
                pieceType = PieceType.PUNCT,
            )
            .setPieceColor(
                pieceColor = PlayerName.WHITE,
            )
            .setPotential(potential = true)

    val updatedPackedValue =
        packedValue.setPieceColor(PlayerName.WHITE).setPotential(potential = true)

    assertEquals(PlayerName.WHITE, expectedColor)
    assertEquals(PlayerName.WHITE, updatedPackedValue.extractPieceColor())
    assertEquals(PlayerName.WHITE, reconstructedValue.extractPieceColor())
    //    assertEquals(PlayerName.WHITE, packedValue.extractPieceColor())
  }

  @Test
  fun `source should be 0xFFFFFFFFFFFFFFFFUL, but isn't`() {
    val actualValue = 439894055u
    val expectedvalue = 439910311u

    val reconstructedValue =
        0u.packPossibleBitMove(
            piece =
                Piece(
                        abbreviation = "BZ",
                        potential = true,
                        colorName = PlayerName.BLACK,
                        type = PieceType.ZERTZ,
                        isNeutralized = false,
                    )
                    .pack(),
            columnInfoIndex = 7,
            targetBit = 549755813888UL,
            pushDirection = PushDirection.DOWN,
            moveType = MoveType.AddPiece,
        )

    assertEquals(expectedvalue, reconstructedValue)
  }
}
