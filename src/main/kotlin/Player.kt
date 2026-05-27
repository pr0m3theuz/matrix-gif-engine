package org.example

import java.awt.Color

data class Player(
    val name: String,
    val abbreviation: String,
    val color: Color,
    val piecesInReserve: MutableList<Piece> = mutableListOf(),
    val capturedPieces: MutableList<Piece> = mutableListOf(),
) {

  fun addPiecesToReserve(pieces: List<Piece>) {
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

  fun addCapturedPieces(pieces: List<Piece>) {
    this.capturedPieces.addAll(pieces)
  }

  /** Play GIPF piece from the reserve and make a move */
  fun playGIPFPiece(piece: Piece): Piece? {
    return this.piecesInReserve.firstOrNull { it.type == PieceType.GIPF }
  }

  /** Select a piece from the reserve and make a move */
  fun selectPiece(pieceType: PieceType): Piece {
    val piece = this.piecesInReserve.first { it.type == pieceType }
    this.piecesInReserve.remove(piece)
    return piece
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
                        color = this.color,
                        colorName = this.name,
                        type = key,
                    )
                  }

              if (mod == 1) {
                stackedPieces.add(
                    Piece(
                        abbreviation = this.abbreviation.plus(key.name.first()),
                        potential = false,
                        color = this.color,
                        colorName = this.name,
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
