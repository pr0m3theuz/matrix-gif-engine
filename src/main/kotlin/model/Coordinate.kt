package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Coordinate(
	val column: Char,
	val row: Int,
) {
//	fun equals(other: Coordinate): Boolean {
//		return column == other.column && row == other.row
//	}

	fun deepCopy(): Coordinate {
		val string = Json.encodeToString(serializer(), this)
		return Json.decodeFromString(serializer(), string)
	}
}

@Serializable
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


@Serializable
data class RetrievedCapturedPieceNode(
	val retrievedPiece: Piece? = null,
	val capturedPiece: Piece? = null,
	val node: Node? = null,
	val isStackedPieceNode: Boolean = false,
	val keepRetrievedPieceInPlay: Boolean = false,
)

@Serializable
data class RetrievedCapturedPieceBit(
	val retrievedPiece: Piece? = null,
	val capturedPiece: Piece? = null,
	val bitmask: ULong,
	val isNeutralized: Boolean = false,
	val keepRetrievedPieceInPlay: Boolean = false,
)
