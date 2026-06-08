package org.example.ai.humanEvaluation

import kotlinx.serialization.Serializable
import org.example.engine.MoveType
import org.example.engine.PossibleMove
import org.example.engine.determineWinner
import org.example.engine.enforcePieceRemovalRules
import org.example.engine.identifyAvailableMoves
import org.example.engine.shiftPiece
import org.example.engine.usePiecePotential
import org.example.model.PieceType
import org.example.model.State
import org.example.model.assertPieceCount
import org.example.model.constructLines
import org.example.model.getAvailablePushDirections

@Serializable
data class BestMove(
    val move: PossibleMove? = null,
    val score: Int = 0,
)

// TODO change to Minimax
fun minimax(depth: Int = 12, state: State): BestMove {
  /**
   * TODO evaluate lines based on:
   * 1. proximity to 4 in a row for current player and next player
   * 2. capturing opponent's GIPF piece and losing a GIPF Piece
   * 3. capturing opponent's pieces and losing pieces ============================= GOAL
   *    ============================= You must try to either capture your opponent’s 3 GIPF pieces,
   *    or make your opponent run out of moves.
   */
  val possibleMoves = identifyAvailableMoves(state)

  if (possibleMoves.isEmpty() || depth == 0) {
    // score = evaluate s for original player
    // return [null, score]
    return BestMove(score = scoreState(state))
  }

  // how to see which move trigger retrieve and capture
  // how to make a move and then assess the state/
  val bestMoves =
      possibleMoves
          .map { possibleMove ->
            var moves: List<BestMove> = emptyList()
            when (possibleMove.moveType) {
              MoveType.AddPiece -> {
                // for each selectable dot, add piece, push piece, assess resulting state, score it
								require(possibleMove.piece != null) { "No piece was selected!" }

                moves =
                    possibleMove.selectableDots
                        .map { nodeConnections ->
                          // To deepcopy or not to deepcopy
                          val node = nodeConnections.node.deepCopy()

                          val selectedPiece = state.currentPlayer.selectPiece(possibleMove.piece)

	                        node.piece = selectedPiece

	                        if (node.piece == null) {
		                        check(node.piece != null) { "Piece ${node.piece} is null" }
	                        }

                          nodeConnections.node.neighbors!!.let {
                            it.getAvailablePushDirections().map { pushDirection ->
	                            if (node.piece == null) {
		                            check(node.piece != null) { "Piece ${node.piece} is null" }
	                            }

															val newBoard =
                                  shiftPiece(
                                      currentNode = node,
                                      moveDirection = pushDirection,
                                      board = state.board.deepCopy(),
                                      lines = state.lines.deepCopy(),
                                  )

                              val newState =
                                  state
                                      .deepCopy()
                                      .copy(
                                          board = newBoard,
                                          lines = constructLines(newBoard.nodes),
                                      )

                              newState.assertPieceCount()

                              BestMove(
                                  move =
                                      PossibleMove(
                                          piece = possibleMove.piece,
                                          selectableDots = setOf(nodeConnections),
                                          pushDirection = pushDirection,
                                          moveType = MoveType.AddPiece,
                                      ),
                                  score =
                                      minimax(
                                              depth = depth.minus(1),
                                              state =
                                                  newState.copy(
                                                      currentPlayer = newState.nextPlayer,
                                                      nextPlayer = newState.currentPlayer,
                                                  ),
                                          )
                                          .score,
                              )
                            }
                          }
                        }
                        .flatten()
              }

              MoveType.UsePotential -> {

                moves =
                    possibleMove.eligiblePotentialTargetNodes.mapNotNull {
                        eligiblePotentialTargetNode ->
                      possibleMove.eligiblePotentialPieceNode?.let {
                        val newState =
                            usePiecePotential(
                                node = it.deepCopy(),
                                eligibleNodesForPotential = setOf(eligiblePotentialTargetNode),
                                state = state,
                            )

                        newState.assertPieceCount()

                        BestMove(
                            move =
                                PossibleMove(
                                    eligiblePotentialPieceNode =
                                        possibleMove.eligiblePotentialPieceNode,
                                    eligiblePotentialTargetNodes =
                                        setOf(eligiblePotentialTargetNode),
                                    moveType = MoveType.UsePotential,
                                ),
                            score =
                                minimax(
                                        depth = depth.minus(1),
                                        state =
                                            newState.copy(
                                                currentPlayer = newState.nextPlayer,
                                                nextPlayer = newState.currentPlayer,
                                            ),
                                    )
                                    .score,
                        )
                      }
                    }
              }
            }

            moves
          }
          .flatten()

  return bestMoves.maxBy { it.score }
}

fun scoreState(state: State): Int {
  // evaluate state & calculate score
  val winner = determineWinner(state)
  winner?.let {
    return if (it.name == state.currentPlayer.name) {
      10000000
    } else if (winner.name == state.nextPlayer.name) {
      -10000000
    } else {
      0 // draw
    }
  }

  return enforcePieceRemovalRules(state).let { newState: State ->
    newState.currentPlayer.recombinePieces()

    newState.assertPieceCount()

    val countCapturedOpponentGIPFPieces =
        newState.currentPlayer.capturedPieces.count { it.type == PieceType.GIPF }

    val countCapturedOpponentPieces =
        newState.currentPlayer.capturedPieces
            .count { it.type != PieceType.GIPF && it.potential }
            .times(2) +
            newState.currentPlayer.capturedPieces.count {
              it.type != PieceType.GIPF && !it.potential
            }

    val countCapturedGIPFPieces =
        newState.nextPlayer.capturedPieces.count { it.type == PieceType.GIPF }

    val countCapturedPieces =
        newState.nextPlayer.capturedPieces
            .count { it.type != PieceType.GIPF && it.potential }
            .times(2) +
            newState.nextPlayer.capturedPieces.count {
              it.type != PieceType.GIPF && !it.potential
            }

    val controllablePieces =
        newState.currentPlayer.piecesInReserve.count {
          it.potential || it.type == PieceType.GIPF
        } +
            newState.board.nodes.count { node ->
              node.piece?.colorName == state.currentPlayer.name &&
                  node.piece?.isNeutralized == false
            } +
            newState.board.nodes.count { node ->
              node.piece?.isNeutralized == true &&
                  node.piece?.stackedPieces?.lastOrNull()?.colorName == newState.currentPlayer.name
            }

    val opponentControllablePieces =
        newState.nextPlayer.piecesInReserve.count {
          it.potential || it.type == PieceType.GIPF
        } +
            newState.board.nodes.count { node ->
              node.piece?.colorName == state.nextPlayer.name && node.piece?.isNeutralized == false
            } +
            newState.board.nodes.count { node ->
              node.piece?.isNeutralized == true &&
                  node.piece?.stackedPieces?.lastOrNull()?.colorName == newState.nextPlayer.name
            }

    val score =
        countCapturedOpponentGIPFPieces.times(20000) +
            countCapturedOpponentPieces.times(100) +
            countCapturedGIPFPieces.times(-20000) +
            countCapturedPieces.times(-100) +
            controllablePieces.times(10) +
            opponentControllablePieces.times(-10)

    score
  }
}

fun evaluateState() {}

fun evaluatePossibleMoves(possibleMoves: List<PossibleMove>, initialState: State) {}
