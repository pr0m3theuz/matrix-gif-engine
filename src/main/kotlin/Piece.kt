package org.example

import java.awt.Color

enum class PieceType() {
  GIPF,
  TAMSK,
  ZERTZ,
  DVONN,
  YINSH,
  PUNCT,
}

data class Piece(
    val abbreviation: String,
    var potential: Boolean,
    //	var position: Coordinate,
    val color: Color,
    val colorName: String,
    val type: PieceType,
		var stacked: Boolean = false, // neutralized // TODO Implement logic
) {
  fun usePiecePotential() {
    potential = false
  }

  fun hasNoPotential(): Boolean {
    return !potential
  }

  fun resetPotential() {
    potential = true
  }
}

fun createPlayerPieces(player: Player): List<Piece> {
  val pieces = mutableListOf<Piece>()

  val gipf =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('G'),
            potential = false,
            color = player.color,
            colorName = player.name,
            type = PieceType.GIPF,
        )
      }

  val tamsk =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('T'),
            potential = false,
            color = player.color,
            colorName = player.name,
            type = PieceType.TAMSK,
        )
      }

  val zertz =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('Z'),
            potential = false,
            color = player.color,
            colorName = player.name,
            type = PieceType.ZERTZ,
        )
      }

  val dvonn =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('D'),
            potential = false,
            color = player.color,
            colorName = player.name,
            type = PieceType.DVONN,
        )
      }

  val yinsh =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('Y'),
            potential = false,
            color = player.color,
            colorName = player.name,
            type = PieceType.YINSH,
        )
      }

  val punct =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('P'),
            potential = false,
            color = player.color,
            colorName = player.name,
            type = PieceType.PUNCT,
        )
      }

  pieces.addAll(gipf)
  pieces.addAll(tamsk)
  pieces.addAll(zertz)
  pieces.addAll(dvonn)
  pieces.addAll(yinsh)
  pieces.addAll(punct)

  return pieces
}
