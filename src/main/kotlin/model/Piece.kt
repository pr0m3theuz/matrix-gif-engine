package org.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class PieceType() {
  NULL,
  GIPF,
  TAMSK,
  ZERTZ,
  YINSH,
  DVONN,
  PUNCT,
}

@Serializable
data class Piece(
    val abbreviation: String,
    var potential: Boolean,
    val colorName: PlayerName,
    val type: PieceType,
    var isNeutralized: Boolean = false,
    // TODO check that pieces alternate
    var stackedPieces: MutableList<Piece> = mutableListOf(),
) {
  fun deepCopy(): Piece {
    //		val string = Json.encodeToString(serializer(), this)
    //		return Json.decodeFromString(serializer(), string)
    return Piece(
        abbreviation = this.abbreviation,
        potential = this.potential,
        colorName = this.colorName,
        type = this.type,
        isNeutralized = this.isNeutralized,
        stackedPieces = this.stackedPieces.toMutableList(),
    )
  }

  fun pack(): UInt {
    val pieceType = type.ordinal.toUInt() shl 24

    val color = colorName.ordinal.toUInt() shl 28

    val potential =
        when (potential) {
          true -> 1u
          false -> 0u
        } shl 27

    val neutralized =
        when (isNeutralized) {
          true -> 1u
          false -> 0u
        } shl 29

    return pieceType or potential or color or neutralized
  }
}

