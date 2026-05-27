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
  fun usePiecePotential(): Piece {
    potential = false
	  return Piece(
		  abbreviation = this.abbreviation,
		  potential = false,
		  color = this.color,
		  colorName = this.colorName,
		  type = this.type,
		  stacked = this.stacked
	  )
  }

  fun hasNoPotential(): Boolean {
    return !potential
  }

  fun resetPotential() {
		TODO("Implement logic to handle piece stacking when there is a two of a kind for each piece type")
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
            potential = true,
            color = player.color,
            colorName = player.name,
            type = PieceType.TAMSK,
        )
      }

  val zertz =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('Z'),
            potential = true,
            color = player.color,
            colorName = player.name,
            type = PieceType.ZERTZ,
        )
      }

  val dvonn =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('D'),
            potential = true,
            color = player.color,
            colorName = player.name,
            type = PieceType.DVONN,
        )
      }

  val yinsh =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('Y'),
            potential = true,
            color = player.color,
            colorName = player.name,
            type = PieceType.YINSH,
        )
      }

  val punct =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('P'),
            potential = true,
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
