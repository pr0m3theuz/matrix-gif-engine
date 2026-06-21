package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class PlayerName() {
	WHITE,
	BLACK
}

@Serializable
data class Player(
    val name: PlayerName,
    val abbreviation: String,
//    val color: Color,
    val piecesInReserve: MutableList<Piece> = mutableListOf(),
    val capturedPieces: MutableList<Piece> = mutableListOf(),
) {
	fun deepCopy(): Player {
		val string = Json.encodeToString(serializer(), this)
		return Json.decodeFromString(serializer(), string)
	}

  fun addPiecesToReserve(pieces: List<Piece>) {
    this.piecesInReserve.addAll(pieces)
  }

  fun addCapturedPieces(pieces: List<Piece>) {
    this.capturedPieces.addAll(pieces)
  }

  /** Play GIPF piece from the reserve and make a move */
  fun selectGIPFPiece(): Piece {
	  val piece =  this.piecesInReserve.first { it.type == PieceType.GIPF }
	  this.piecesInReserve.remove(piece)
    return piece
  }

  /** Select a piece from the reserve and make a move */
  fun selectPiece(piece: Piece): Piece {
    val initialSize = piecesInReserve.size

    val selectedPiece = this.piecesInReserve.firstOrNull  { it.type == piece.type && it.potential == piece.potential }
      check(selectedPiece != null) {
          "$piece was not found!"
      }
    this.piecesInReserve.remove(selectedPiece)

    val pieceCountDifference = initialSize - piecesInReserve.size
    println(
      "the delta was $pieceCountDifference (Initial: $initialSize, Current: ${piecesInReserve.size})"
    )

    check(pieceCountDifference == 1) {
      "Reserve decrement failure! Expected exactly 1 piece to be removed from the reserve, " +
          "but the delta was $pieceCountDifference (Initial: $initialSize, Current: ${piecesInReserve.size})."
    }

    return selectedPiece
  }

  fun getNumberOfPiecesInReserve(): Int {
    return this.piecesInReserve.size
  }

  fun recombinePieces() {
    val alreadyStackedPieces = this.piecesInReserve.filter { it.potential }
    val gipfPieces = this.piecesInReserve.filter { it.type == PieceType.GIPF }
    val pieceTypesThatCantStack =
        this.piecesInReserve
            .filter { !it.potential && it.type != PieceType.GIPF }
            .groupingBy { it.type }
            .eachCount()
            .filter { it.value < 2 }

    val unstackablePieces =
        pieceTypesThatCantStack
            .map { (key, pieces) ->
              this.piecesInReserve.filter { it.type == key && !it.potential }
            }
            .flatten()

    val unstackedPieces =
        this.piecesInReserve
            .filter { !it.potential && it.type != PieceType.GIPF }
            .groupingBy { it.type }
            .eachCount()
            .filter { it.value >= 2 }

    val newlyStackedPieces =
        unstackedPieces
            .map { (key, pieces) ->
              val mod = pieces.mod(2)
              val piecesToStack = pieces.div(2)

              val stackedPieces: MutableList<Piece> =
                  MutableList(piecesToStack) {
                    Piece(
                        abbreviation = this.abbreviation.plus(key.name.first()),
                        potential = true,
//                        color = this.color,
                        colorName = this.name.name,
                        type = key,
                    )
                  }

              if (mod == 1) {
                stackedPieces.add(
                    Piece(
                        abbreviation = this.abbreviation.plus(key.name.first()),
                        potential = false,
//                        color = this.color,
                        colorName = this.name.name,
                        type = key,
                    )
                )
              }

              stackedPieces
            }
            .flatten()

	  this.piecesInReserve.clear()
	  this.piecesInReserve.addAll(alreadyStackedPieces)
	  this.piecesInReserve.addAll(gipfPieces)
	  this.piecesInReserve.addAll(unstackablePieces)
	  this.piecesInReserve.addAll(newlyStackedPieces)
  }
}
