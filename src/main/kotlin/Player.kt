package org.example

import java.awt.Color

data class Player(
	val name: String,
	val abbreviation: String,
	val color: Color,
	val piecesInReserve: MutableList<Piece> = mutableListOf(),
	val capturedPieces: MutableList<Piece> = mutableListOf(),
)

fun Player.addPiecesToReserve(pieces: List<Piece>) {
	pieces.forEach { piece ->
		when (piece.type) {
			PieceType.TAMSK -> piece.potential = true
			PieceType.ZERTZ -> piece.potential = true
			PieceType.DVONN -> piece.potential = true
			PieceType.YINSH -> piece.potential = true
			PieceType.PUNCT -> piece.potential = true
			else -> {}
		}
	}

	this.piecesInReserve.addAll(pieces)
}

fun Player.addCapturedPieces(pieces: List<Piece>) {
	this.capturedPieces.addAll(pieces)
}

/** Play GIPF piece from the reserve and make a move */
fun Player.playGIPFPiece(piece: Piece): Piece? {
	return this.piecesInReserve.firstOrNull { it.type == PieceType.GIPF }
}

/** Select a piece from the reserve and make a move */
fun Player.selectPiece(pieceType: PieceType): Piece {
	val piece = this.piecesInReserve.first { it.type == pieceType}
	this.piecesInReserve.remove(piece)
	return piece
}

fun Player.getNumberOfPiecesInReserve(): Int {
	return this.piecesInReserve.size
}

