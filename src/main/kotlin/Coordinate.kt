package org.example

data class Coordinate(
	val column: Char,
	val row: Int,
) {
//	fun equals(other: Coordinate): Boolean {
//		return column == other.column && row == other.row
//	}
}

enum class PushDirection() {
	UP,
	DOWN,
	UPPER_RIGHT,
	LOWER_RIGHT,
	UPPER_LEFT,
	LOWER_LEFT,
}

data class RetrievedCapturedPieces(
	val retrieved: List<Piece>,
	val captured: List<Piece>,
	val nodes: List<Node>,
)

