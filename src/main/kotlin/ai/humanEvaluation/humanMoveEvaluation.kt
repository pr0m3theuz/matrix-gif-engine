package org.example.ai.humanEvaluation

import java.security.MessageDigest
import kotlin.collections.forEach
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
    val score: Float = 0f,
)

data class AlphaBetaScore(
    var move: PossibleMove? = null,
    var alpha: Float = Float.NEGATIVE_INFINITY,
    var beta: Float = Float.POSITIVE_INFINITY,
)

fun AlphaBetaScore.swapAlphaBeta(): AlphaBetaScore {
  return AlphaBetaScore(
      alpha = beta.unaryMinus(),
      beta = alpha.unaryMinus(),
  )
}

// TODO change to Minimax
fun minimaxAddPieces(depth: Int = 7, state: State): BestMove {
  /**
   * TODO evaluate lines based on:
   * 1. proximity to 4 in a row for current player and next player
   * 2. capturing opponent's GIPF piece and losing a GIPF Piece
   * 3. capturing opponent's pieces and losing pieces ============================= GOAL
   *    ============================= You must try to either capture your opponent’s 3 GIPF pieces,
   *    or make your opponent run out of moves.
   */
  state.assertPieceCount()

  val possibleMoves = identifyAvailableMoves(state)

  if (possibleMoves.isEmpty() || depth == 0) {
    // score = evaluate s for original player
    // return [null, score]
    return BestMove(score = scoreState(state).toFloat())
  }

  // how to see which move trigger retrieve and capture
  // how to make a move and then assess the state/
  val bestMoves =
      possibleMoves
          .map { possibleMove ->
            val mutableState = state.deepCopy()

            var moves: List<BestMove> = emptyList()
            when (possibleMove.moveType) {
              MoveType.AddPiece -> {
                // for each selectable dot, add piece, push piece, assess resulting state, score it
                require(possibleMove.piece != null) { "No piece was selected!" }

                val selectedPiece = mutableState.currentPlayer.selectPiece(possibleMove.piece)

                moves =
                    possibleMove.selectableDots
                        .map { nodeConnections ->
                          nodeConnections.node.neighbors!!.let {
                            it.getAvailablePushDirections().map { pushDirection ->
                              // To deepcopy or not to deepcopy
                              val node = nodeConnections.node.deepCopy()

                              node.piece = selectedPiece

                              if (node.piece == null) {
                                check(node.piece != null) { "Piece ${node.piece} is null" }
                              }

                              val newBoard =
                                  shiftPiece(
                                      currentNode = node,
                                      moveDirection = pushDirection,
                                      board = mutableState.board.deepCopy(),
                                      lines = mutableState.lines.deepCopy(),
                                  )

                              var newState =
                                  mutableState
                                      .deepCopy()
                                      .copy(
                                          board = newBoard,
                                          lines = constructLines(newBoard.nodes),
                                      )

                              newState.assertPieceCount()

                              newState = enforcePieceRemovalRules(newState)

                              BestMove(
                                  move =
                                      PossibleMove(
                                          piece = possibleMove.piece,
                                          selectableDots = setOf(nodeConnections),
                                          pushDirection = pushDirection,
                                          moveType = MoveType.AddPiece,
                                      ),
                                  score =
                                      minimaxAddPieces(
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
                        var newState =
                            usePiecePotential(
                                node = it.deepCopy(),
                                eligibleNodesForPotential = setOf(eligiblePotentialTargetNode),
                                state = mutableState,
                            )

                        newState.assertPieceCount()

                        newState = enforcePieceRemovalRules(newState)

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
                                minimaxAddPieces(
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
	// TODO how to score control over the board/line
	// TODO how to score attacking positions, i.e. 4 in the row
	// TODO how to skip positions that don't improve the current player's position

  // evaluate state & calculate score
  val possibleMoves = identifyAvailableMoves(state)


  if (possibleMoves.isEmpty()) {
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

// TODO implement minimaxRemovePieces
fun minimaxRemovePieces(state: State) {}

fun evaluateState() {}

fun evaluatePossibleMoves(possibleMoves: List<PossibleMove>, initialState: State) {}

fun alphabetaAddPieces(
    depth: Int = 3,
    state: State,
    gameTree: HashMap<String, AlphaBetaScore>,
    alphaBetaScore: AlphaBetaScore,
): BestMove {
  /**
   * TODO evaluate lines based on:
   * 1. proximity to 4 in a row for current player and next player
   * 2. capturing opponent's GIPF piece and losing a GIPF Piece
   * 3. capturing opponent's pieces and losing pieces ============================= GOAL
   *    ============================= You must try to either capture your opponent’s 3 GIPF pieces,
   *    or make your opponent run out of moves.
   */
  state.assertPieceCount()

  val possibleMoves = identifyAvailableMoves(state)

  if (possibleMoves.isEmpty() || depth == 0) {
    // score = evaluate s for original player
    // return [null, score]
    return BestMove(score = scoreState(state).toFloat())
  }

  // how to see which move trigger retrieve and capture
  // how to make a move and then assess the state/
  possibleMoves.forEach { possibleMove ->
    val mutableState = state.deepCopy()

    when (possibleMove.moveType) {
      MoveType.AddPiece -> {
        // for each selectable dot, add piece, push piece, assess resulting state, score it
        require(possibleMove.piece != null) { "No piece was selected!" }

        val selectedPiece = mutableState.currentPlayer.selectPiece(possibleMove.piece)

        possibleMove.selectableDots.forEachIndexed { index, nodeConnections ->
          nodeConnections.node.neighbors!!.let {
            it.getAvailablePushDirections().forEach { pushDirection ->
              // To deepcopy or not to deepcopy
              val node = nodeConnections.node.deepCopy()

              node.piece = selectedPiece

              if (node.piece == null) {
                check(node.piece != null) { "Piece ${node.piece} is null" }
              }

              val newBoard =
                  shiftPiece(
                      currentNode = node,
                      moveDirection = pushDirection,
                      board = mutableState.board.deepCopy(),
                      lines = mutableState.lines.deepCopy(),
                  )

              var newState =
                  mutableState
                      .deepCopy()
                      .copy(
                          board = newBoard,
                          lines = constructLines(newBoard.nodes),
                      )

              newState.assertPieceCount()

              newState = enforcePieceRemovalRules(newState)

              val gameStateHash =
                  MessageDigest.getInstance("MD5")
                      .digest(newState.toString().toByteArray())
                      .toHexString()

	            println("line 342: gameStateHash: $gameStateHash")

              val move =
                  BestMove(
                      move =
                          PossibleMove(
                              piece = possibleMove.piece,
                              selectableDots = setOf(nodeConnections),
                              pushDirection = pushDirection,
                              moveType = MoveType.AddPiece,
                          ),
                      score =
                          alphabetaAddPieces(
                                  depth = depth.minus(1),
                                  state =
                                      newState.copy(
                                          currentPlayer = newState.nextPlayer,
                                          nextPlayer = newState.currentPlayer,
                                      ),
                                  gameTree = gameTree,
                                  alphaBetaScore.swapAlphaBeta(),
                              )
                              .score,
                  )

              if (move.score.unaryMinus() > alphaBetaScore.alpha) {
                alphaBetaScore.alpha = move.score.unaryMinus()

                alphaBetaScore.move = move.move
                gameTree[gameStateHash] = alphaBetaScore

	              println("line 371: ply $depth move $index: set best to: $alphaBetaScore")
	              println(
		              "line 373: ply $depth player: ${newState.currentPlayer} index: $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
	              )
              }
              if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
	              println(
		              "line 378: ply $depth player: ${newState.currentPlayer} move $index: return best: $alphaBetaScore"
	              )
                return BestMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
              }
            }
          }
        }
      }

      MoveType.UsePotential -> {

        possibleMove.eligiblePotentialTargetNodes.forEachIndexed {
            index,
            eligiblePotentialTargetNode ->
          possibleMove.eligiblePotentialPieceNode?.let {
            var newState =
                usePiecePotential(
                    node = it.deepCopy(),
                    eligibleNodesForPotential = setOf(eligiblePotentialTargetNode),
                    state = mutableState,
                )

            newState.assertPieceCount()

            newState = enforcePieceRemovalRules(newState)

            val gameStateHash =
                MessageDigest.getInstance("MD5")
                    .digest(newState.toString().toByteArray())
                    .toHexString()

            println("line 409: gameStateHash: $gameStateHash")

            val move =
                BestMove(
                    move =
                        PossibleMove(
                            eligiblePotentialPieceNode = possibleMove.eligiblePotentialPieceNode,
                            eligiblePotentialTargetNodes = setOf(eligiblePotentialTargetNode),
                            moveType = MoveType.UsePotential,
                        ),
                    score =
                        alphabetaAddPieces(
                                depth = depth.minus(1),
                                state =
                                    newState.copy(
                                        currentPlayer = newState.nextPlayer,
                                        nextPlayer = newState.currentPlayer,
                                    ),
                                gameTree = gameTree,
                                alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            )
                            .score,
                )

            if (move.score.unaryMinus() > alphaBetaScore.alpha) {
              alphaBetaScore.alpha = move.score.unaryMinus()
              alphaBetaScore.move = move.move
              gameTree[gameStateHash] = alphaBetaScore

              println("line 430: ply $depth move $index: set best to: $alphaBetaScore")
              println(
                  "line 432: ply $depth player: ${newState.currentPlayer} index: $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
              )
            }
            if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
              println(
                  "line 437: ply $depth player: ${newState.currentPlayer} move $index: return best: $alphaBetaScore"
              )
              return BestMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
            }
          }
        }
      }
    }
  }

  println("line 446: ply $depth return best: $alphaBetaScore")
  println("line 447: ply $depth moves")

  return BestMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
}
