package org.example.ai.mcts

import org.example.model.*

data class ChildState(
	val currentPlayer: Player,
	val nextPlayer: Player,
	val turnPhase: TurnPhase,
	val moves: MutableList<PackedMove>,
)

fun createChildState(
	childBitboard: Bitboard,
	childCurrentPlayer: Player,
	childNextPlayer: Player,
	turnPhase: TurnPhase?,
	turnHasHadNormalMove: Boolean,
): ChildState {
	val currentPlayerTamskMoves = mutableListOf<PackedMove>()
	childBitboard.getTamskMoves(childCurrentPlayer, currentPlayerTamskMoves)
	if (currentPlayerTamskMoves.isNotEmpty()) {
		return ChildState(childCurrentPlayer, childNextPlayer, TurnPhase.ExtraMove, currentPlayerTamskMoves)
	}

	val currentPlayerRemovablePieces = mutableListOf<PackedMove>()
	childBitboard.identifyPiecesToRemove(childCurrentPlayer, currentPlayerRemovablePieces)
	if (currentPlayerRemovablePieces.isNotEmpty()) {
		return ChildState(childCurrentPlayer, childNextPlayer, TurnPhase.PieceRemoval, currentPlayerRemovablePieces)
	}

	if (!turnHasHadNormalMove/* && turnPhase != TurnPhase.PlayerInputWindow */) {
		val currentPlayerAvailableMoves = mutableListOf<PackedMove>()
		childBitboard.identifyAvailableMoves(childCurrentPlayer, movesBuffer = currentPlayerAvailableMoves)
		return ChildState(childCurrentPlayer, childNextPlayer, TurnPhase.PlayerInputWindow, currentPlayerAvailableMoves)
	}

	val nextPlayerRemovablePieces = mutableListOf<PackedMove>()
	childBitboard.identifyPiecesToRemove(childNextPlayer, nextPlayerRemovablePieces)
	if (nextPlayerRemovablePieces.isNotEmpty()) {
		return ChildState(childNextPlayer, childCurrentPlayer, TurnPhase.PieceRemoval, nextPlayerRemovablePieces)
	}

	val nextPlayerTamskMoves = mutableListOf<PackedMove>()
	childBitboard.getTamskMoves(childNextPlayer, nextPlayerTamskMoves)
	if (nextPlayerTamskMoves.isNotEmpty()) {
		return ChildState(childNextPlayer, childCurrentPlayer, TurnPhase.ExtraMove, nextPlayerTamskMoves)
	}

	val nextPlayerAvailableMoves = mutableListOf<PackedMove>()
	childBitboard.identifyAvailableMoves(childNextPlayer, movesBuffer = nextPlayerAvailableMoves)
	return ChildState(childNextPlayer, childCurrentPlayer, TurnPhase.PlayerInputWindow, nextPlayerAvailableMoves)
}