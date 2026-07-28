package org.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class PieceType() {
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

	fun pack(): UByte {
		val pieceType = type.ordinal.toUInt()

		val color = colorName.ordinal.toUInt() shl 4

		val potential = when (potential) {
			true -> 1u
			false -> 0u
		} shl 3


		val neutralized = when (isNeutralized) {
			true -> 1u
			false -> 0u
		} shl 5
		
		return (pieceType or potential or color or neutralized).toUByte()
	}
}

fun UByte.unpackPiece(): Piece? {
	val pieceType = when ((this and 0b111.toUByte()).toInt()) {
		PieceType.GIPF.ordinal -> PieceType.GIPF
		PieceType.TAMSK.ordinal -> PieceType.TAMSK
		PieceType.ZERTZ.ordinal -> PieceType.ZERTZ
		PieceType.YINSH.ordinal -> PieceType.YINSH
		PieceType.DVONN.ordinal -> PieceType.DVONN
		PieceType.PUNCT.ordinal -> PieceType.PUNCT
		else -> return null
	}

	val potential = when (((this and 0b1000.toUByte()).toUInt() shr 3).toInt()) {
		0 -> false
		1 -> true
		else -> false
	}

	val colorName = when (((this and 0b10000.toUByte()).toUInt() shr 4).toInt()) {
		PlayerName.WHITE.ordinal -> PlayerName.WHITE
		PlayerName.BLACK.ordinal -> PlayerName.BLACK
		else -> error("Invalid color name $this")
	}

	val neutralized = when (((this and 0b100000.toUByte()).toUInt() shr 5).toInt()) {
		0 -> false
		1 -> true
		else -> false
	}

	return Piece(
		abbreviation = "${colorName.name.first()}${pieceType.name.first()}",
		colorName = colorName,
		type = pieceType,
		potential = potential,
		isNeutralized = neutralized,
	)
}

fun createPlayerPieces(player: Player): List<Piece> {
  val pieces = mutableListOf<Piece>()

  val gipf =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('G'),
            potential = false,
//            color = player.color,
            colorName = player.name,
            type = PieceType.GIPF,
        )
      }

  val tamsk =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('T'),
            potential = true,
//            color = player.color,
            colorName = player.name,
            type = PieceType.TAMSK,
        )
      }

  val zertz =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('Z'),
            potential = true,
//            color = player.color,
            colorName = player.name,
            type = PieceType.ZERTZ,
        )
      }

  val dvonn =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('D'),
            potential = true,
//            color = player.color,
            colorName = player.name,
            type = PieceType.DVONN,
        )
      }

  val yinsh =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('Y'),
            potential = true,
//            color = player.color,
            colorName = player.name,
            type = PieceType.YINSH,
        )
      }

  val punct =
      List(3) {
        Piece(
            abbreviation = player.abbreviation.plus('P'),
            potential = true,
//            color = player.color,
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
