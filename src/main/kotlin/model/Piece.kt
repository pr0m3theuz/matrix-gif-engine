package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class PieceType() {
  GIPF,
  TAMSK,
  ZERTZ,
  DVONN,
  YINSH,
  PUNCT,
}

@Serializable
data class Piece(
	val abbreviation: String,
	var potential: Boolean,
    //	var position: Coordinate,
//	@Contextual val color: Color,
	val colorName: String,
	val type: PieceType,
	// TODO check that pieces alternate
	var isNeutralized: Boolean = false,
	var stackedPieces: MutableList<Piece> = mutableListOf(),
	) {
	fun deepCopy(): Piece {
		val string = Json.encodeToString(serializer(), this)
		return Json.decodeFromString(serializer(), string)
	}

  fun usePiecePotential(): Piece {
    potential = false
	  return Piece(
		  abbreviation = this.abbreviation,
		  potential = false,
//		  color = this.color,
		  colorName = this.colorName,
		  type = this.type,
	  )
  }

	fun pushPotential(newPiece: Piece) {
		val topPiece = stackedPieces.lastOrNull() ?: this
		require(topPiece.colorName != newPiece.colorName) {
			"Illegal Layering: Stacks must strictly alternate player colors!"
		}
		stackedPieces.add(newPiece)
		isNeutralized = true
	}

  fun hasNoPotential(): Boolean {
    return !potential
  }

	fun removePiece() {
		// TODO replace with Piece.removePiece()
		// TODO figure out how to remove piece from a node. either return the current piece (less the last stacked piece) or null
		//  how does these functions interact with each other
	}
}

fun createPlayerPieces(player: Player): List<Piece> {
  val pieces = mutableListOf<Piece>()

  val gipf =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('G'),
            potential = false,
//            color = player.color,
            colorName = player.name.name,
            type = PieceType.GIPF,
        )
      }

  val tamsk =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('T'),
            potential = true,
//            color = player.color,
            colorName = player.name.name,
            type = PieceType.TAMSK,
        )
      }

  val zertz =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('Z'),
            potential = true,
//            color = player.color,
            colorName = player.name.name,
            type = PieceType.ZERTZ,
        )
      }

  val dvonn =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('D'),
            potential = true,
//            color = player.color,
            colorName = player.name.name,
            type = PieceType.DVONN,
        )
      }

  val yinsh =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('Y'),
            potential = true,
//            color = player.color,
            colorName = player.name.name,
            type = PieceType.YINSH,
        )
      }

  val punct =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('P'),
            potential = true,
//            color = player.color,
            colorName = player.name.name,
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
