@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.ai

import org.example.ai.mcts.PackedMove
import org.example.model.Bitboard
import org.example.model.ColumnInfo
import org.example.model.PieceType
import org.example.model.Player
import org.example.model.PlayerName
import org.example.model.boardCenterSpotMask
import org.example.model.extractPieceType
import org.example.model.extractPotential
import org.example.model.getDvonnMoves
import org.example.model.getPunctMoves
import org.example.model.getYinshMoves
import org.example.model.getZertzMoves

fun Bitboard.evaluateAvailableMoves(
	currentPlayer: Player,
	columnInfos: List<ColumnInfo> = org.example.model.columnInfos,
	movesBuffer: MutableList<PackedMove>,
): Int {
  movesBuffer.clear()

  val seenTypes = mutableSetOf<PieceType>()
  var playablePiecesInReserve = 0

  for (piece in currentPlayer.piecesInReserve) {
    val type = piece.extractPieceType()
    if (type != null) {
      if (type == PieceType.GIPF && seenTypes.add(type)) {
        playablePiecesInReserve += 1
	      break
      }
	    if (piece.extractPotential() && seenTypes.add(type)) {
        playablePiecesInReserve += 1
	      break
      }
    }
  }

  val occupiedBits = globalOccupancy

	var availableMoves = 0
	for (colIndex in columnInfos.indices) {
		val columnInfo = columnInfos[colIndex]
		if ((occupiedBits and columnInfo.columnMask) == columnInfo.columnMask) continue

		// TODO replace with math
		//   for each column + 2
		availableMoves += 2
	}

  // TODO PieceType.TAMSK logic is handled by isTamskPieceAtCenter()
	if (when (currentPlayer.name) {
			PlayerName.WHITE -> whiteTAMSK and whitePotentials and boardCenterSpotMask
			PlayerName.BLACK -> blackTAMSK and blackPotentials and boardCenterSpotMask
		} == boardCenterSpotMask) {
		return availableMoves
	}



  /**
   * TODO rewrite Check check(piece?.extractPotential() == false) { // val pieceCoords =
   * selectedNode.node.coordinate.let { "${it.column}${it.row}" } val currentPotential =
   * piece?.extractPotential()
   *
   * // "Invalid piece state at $pieceCoords: Expected piece potential to be spent (false), " // +
   * "but found potential status is: $currentPotential (Piece Type:
   * ${piece?.extractPieceType()?.name}, Color: ${piece?.extractPieceColor()})" }
   */

  when (currentPlayer.name) {
	  PlayerName.WHITE -> {
      if (blackZERTZ != 0UL) {
        getZertzMoves(currentPlayer, columnInfos, movesBuffer)
      }
      if (blackYINSH != 0UL) {
        getYinshMoves(currentPlayer, columnInfos, movesBuffer)
      }
      if (blackDVONNLayer[0] != 0UL) {
        getDvonnMoves(currentPlayer, columnInfos, movesBuffer)
      }
      if (blackPUNCTLayer[0] != 0UL) {
        getPunctMoves(currentPlayer, columnInfos, movesBuffer)
      }
    }
	  PlayerName.BLACK -> {
      if (blackZERTZ != 0UL) {
        getZertzMoves(currentPlayer, columnInfos, movesBuffer)
      }
       if (blackYINSH != 0UL) {
         getYinshMoves(currentPlayer, columnInfos, movesBuffer)
      }
       if (blackDVONNLayer[0] != 0UL) {
         getDvonnMoves(currentPlayer, columnInfos, movesBuffer)
      }
       if (blackPUNCTLayer[0] != 0UL) {
         getPunctMoves(currentPlayer, columnInfos, movesBuffer)
      }
    }
  }

	return playablePiecesInReserve * availableMoves + movesBuffer.size
}