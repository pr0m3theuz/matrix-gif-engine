package org.example

fun main() {
  var gameState: State = initializeState()

  val initPiecesCount =
      gameState.currentPlayer.piecesInReserve.size + gameState.nextPlayer.piecesInReserve.size

  gameState.printStateSummary()
	// TODO implement function to evaluate if the current player's pieces has any valid moves left
  while (!evaluateCapturedPieces(gameState) || !evaluatePiecesInReserve(gameState)) {

    gameState = playerTurn(gameState)

	  // TODO implement function to evaluate if the current player's pieces has any valid moves left
    if (evaluateCapturedPieces(gameState) || evaluatePiecesInReserve(gameState)) {
      break
    }

    gameState.printStateSummary()

    // TODO Check total pieces count == piecesCount
    val totalPieces =
        gameState.nextPlayer.piecesInReserve.size +
            gameState.nextPlayer.capturedPieces.size +
            gameState.currentPlayer.piecesInReserve.size +
            gameState.currentPlayer.capturedPieces.size +
            gameState.board.nodes.count { it.piece != null }

	  // TODO Confirm if this handles used potentials
    check(totalPieces == initPiecesCount) {
      "Game Piece Desynchronization: Total pieces in play ($totalPieces) does not match the initial piece count ($initPiecesCount). Pieces have been illegally spawned or deleted."
    }
    // state new turn
    gameState = gameState.rotatePlayers()
  }
  // TODO Print winner
}
