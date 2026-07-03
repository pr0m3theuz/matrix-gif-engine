@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.ai.humanEvaluation

import java.security.MessageDigest
import kotlin.collections.forEach
import kotlin.collections.mapNotNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.engine.MoveType
import org.example.engine.PossibleBitMove
import org.example.engine.PossibleMove
import org.example.engine.determineWinner
import org.example.engine.enforcePieceRemovalRules
import org.example.engine.identifyAvailableMoves
import org.example.engine.shiftPiece
import org.example.engine.usePiecePotential
import org.example.model.Bitboard
import org.example.model.Piece
import org.example.model.PieceType
import org.example.model.Player
import org.example.model.PlayerName
import org.example.model.State
import org.example.model.addPieceToBitboard
import org.example.model.assertPieceCount
import org.example.model.columnInfos
import org.example.model.constructLines
import org.example.model.createPlayerPiecesWithPotentialPowerset
import org.example.model.evaluateLinesForFourInARow
import org.example.model.getAvailablePushDirections
import org.example.model.getTamskMoves
import org.example.model.getsSelectedPiecesWithPotentialPowerset
import org.example.model.identifyAvailableMoves
import org.example.model.identifyPlayerPiecesWithPotential
import org.example.model.removeRetrieveAndCapturePiecesFromBitboard
import org.example.model.createRetrieveAndCapturePiecesList
import org.example.model.undoAddPieceToBitboard
import org.example.model.undoRetrieveAndCapturePieces
import org.example.model.undoTamskPotential
import org.example.model.undoUsePiecePotential
import org.example.model.usePiecePotential
import org.example.model.useTamskPotential

@Serializable
data class BestMove(
    val move: PossibleMove? = null,
    val score: Float = 0f,
)

data class BestBitMove(
    val move: PossibleBitMove? = null,
    val score: Float = 0f,
)

data class AlphaBetaScore(
    var move: PossibleMove? = null,
    var alpha: Float = Float.NEGATIVE_INFINITY,
    var beta: Float = Float.POSITIVE_INFINITY,
) {
  fun swapAlphaBeta(): AlphaBetaScore {
    return AlphaBetaScore(
        alpha = beta.unaryMinus(),
        beta = alpha.unaryMinus(),
    )
  }
}

data class AlphaBetaScoreBit(
    var move: PossibleBitMove? = null,
    var alpha: Float = Float.NEGATIVE_INFINITY,
    var beta: Float = Float.POSITIVE_INFINITY,
) {
  fun swapAlphaBeta(): AlphaBetaScoreBit {
    return AlphaBetaScoreBit(
        alpha = beta.unaryMinus(),
        beta = alpha.unaryMinus(),
    )
  }
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

              MoveType.RetrieveCapturePieces -> {}
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
    val winner = determineWinner(state.currentPlayer, state.nextPlayer)
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
    newState.currentPlayer.combinePieces()

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
              node.piece?.colorName == state.currentPlayer.name.name &&
                  node.piece?.isNeutralized == false
            } +
            newState.board.nodes.count { node ->
              node.piece?.isNeutralized == true &&
                  node.piece?.stackedPieces?.lastOrNull()?.colorName ==
                      newState.currentPlayer.name.name
            }

    val opponentControllablePieces =
        newState.nextPlayer.piecesInReserve.count {
          it.potential || it.type == PieceType.GIPF
        } +
            newState.board.nodes.count { node ->
              node.piece?.colorName == state.nextPlayer.name.name &&
                  node.piece?.isNeutralized == false
            } +
            newState.board.nodes.count { node ->
              node.piece?.isNeutralized == true &&
                  node.piece?.stackedPieces?.lastOrNull()?.colorName ==
                      newState.nextPlayer.name.name
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

fun scoreBitboardState(bitboard: Bitboard, currentPlayer: Player, opponentPlayer: Player): Int {
  // TODO how to score control over the board/line
  // TODO how to score attacking positions, i.e. 4 in the row
  // TODO how to skip positions that don't improve the current player's position

	/**
	 * The evaluation function does not know which states are which, but it can return a single value that estimates the proportion of states with each outcome.
	 * For example, suppose our experience suggests that 82% of the states encountered in the two-pawns versus one-pawn category lead to a win (utility +1); 2% to a loss (0), and 16% to a draw (1/2).
	 * Then a reasonable evaluation for states in the category is the expected value: (0.82 × +1)+(0.02 × 0)+ (0.16 × 1/2) = 0.90.
	 * In principle, the expected value can be determined for each category of states, resulting in an evaluation function that works for any state.
	 *
	 * In practice, this kind of analysis requires too many categories and hence too much experience to estimate all the probabilities.
	 * Instead, most evaluation functions compute separate numerical contributions from each feature and then combine them to find the total value.
	 * For centuries, chess players have developed ways of judging the value of a position using just this idea.
	 * For example, introductory chess books give an approximate material value for each piece: each pawn is worth 1, a knight or bishop is worth 3, a rook 5, and the queen 9.
	 * Other features such as “good pawn structure” and “king safety” might be worth half a pawn, say. These feature values are then simply added up to obtain the evaluation of the position.
	 * The weights should be normalized so that the sum is always within the range of a loss (0) to a win (+1).
	 *
	 * We said that the evaluation function should be strongly correlated with the actual chances of winning, but it need not be linearly correlated:
	 * if state s is twice as likely to win as state s' we don’t require that EVAL(S) be twice EVAL(S’); all we require is that EVAL(S) > EVAL(S’).
	 *
	 * Adding up the values of features seems like a reasonable thing to do, but in fact it involves a strong assumption:
	 * that the contribution of each feature is independent of the values of the other features.
	 * For this reason, current programs for chess and other games also use nonlinear combinations of features.
	 * For example, a pair of bishops might be worth more than twice the value of a single bishop,
	 * and a bishop is worth more in the endgame than earlier—when the move number feature is high or the number of remaining pieces feature is low.
	 *
	 * The evaluation function should be applied only to positions that are quiescent—that is,
	 * positions in which there is no pending move (such as a capturing the queen) that would wildly swing the evaluation.
	 * For nonquiescent positions the IS-CUTOFF returns false, and the search continues until quiescent positions are reached.
	 * This extra quiescence search is sometimes restricted to consider only certain types of moves, such as capture moves,
	 * that will quickly resolve the uncertainties in the position.”
	 *
	 * Excerpt From
	 * Artificial Intelligence: A Modern Approach
	 * Stuart J. Russell & Peter Norvig
	 * This material may be protected by copyright.
	 * */

  // evaluate state & calculate score
  val possibleMoves = bitboard.identifyAvailableMoves(currentPlayer, columnInfos)

  if (possibleMoves.isEmpty()) {
    val winner = determineWinner(currentPlayer, opponentPlayer)
    winner?.let {
      return if (it.name == currentPlayer.name) {
        1
      } else if (winner.name == opponentPlayer.name) {
        -1
      } else {
        0 // draw
      }
    }
  }

  val countCapturedOpponentGIPFPieces =
      currentPlayer.capturedPieces.count { it.type == PieceType.GIPF }

  val countCapturedOpponentPieces =
      currentPlayer.capturedPieces.count { it.type != PieceType.GIPF && it.potential }.times(2) +
          currentPlayer.capturedPieces.count {
            it.type != PieceType.GIPF && !it.potential
          }

  val countCapturedGIPFPieces = opponentPlayer.capturedPieces.count { it.type == PieceType.GIPF }

  val countCapturedPieces =
      opponentPlayer.capturedPieces.count { it.type != PieceType.GIPF && it.potential }.times(2) +
          opponentPlayer.capturedPieces.count {
            it.type != PieceType.GIPF && !it.potential
          }

  val controllablePieces =
      currentPlayer.piecesInReserve.count {
        it.potential || it.type == PieceType.GIPF
      } +
          when (currentPlayer.name) {
            PlayerName.WHITE -> {
              bitboard.whitePieces.toInt()
            }
            PlayerName.BLACK -> {
              bitboard.blackPieces.toInt()
            }
          }

  val opponentControllablePieces =
      opponentPlayer.piecesInReserve.count {
        it.potential || it.type == PieceType.GIPF
      } +
          when (opponentPlayer.name) {
            PlayerName.WHITE -> {
              bitboard.whitePieces.toInt() // how to treat pieces with potental
            }
            PlayerName.BLACK -> {
              bitboard.blackPieces.toInt()
            }
          }

	val gipfPiecesWeight = 0.33f
	val capturedPiecesWeight = 0.33f
	val piecesInReserve = 0.33f

  return (countCapturedOpponentGIPFPieces.times(gipfPiecesWeight) +
      countCapturedOpponentPieces.times(capturedPiecesWeight) +
      countCapturedGIPFPieces.times(-gipfPiecesWeight) +
      countCapturedPieces.times(-capturedPiecesWeight) +
      controllablePieces.times(piecesInReserve) +
      opponentControllablePieces.times(-piecesInReserve)).toInt()
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

      MoveType.RetrieveCapturePieces -> {}
    }
  }

  println("line 446: ply $depth return best: $alphaBetaScore")
  println("line 447: ply $depth moves")

  return BestMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
}

fun alphabetaBitboardAddPieces(
    depth: Int = 3,
    //    state: State,
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    //    gameTree: HashMap<String, AlphaBetaScore>,
    alphaBetaScore: AlphaBetaScoreBit,
): BestBitMove {
	val isDebugEnabled = false // Toggle this to true to see detailed trace logs
	if (isDebugEnabled) {
		println("--- ALPHA-BETA CALLED ---")
		println("currentPlayer: ${currentPlayer}")
		println("opponentPlayer: $opponentPlayer")
		println("depth: ${depth}")
		println("Bitboard: ${Json.encodeToString(bitboard)}")
	}
  /**
   * TODO evaluate lines based on:
   * 1. proximity to 4 in a row for current player and next player
   * 2. capturing opponent's GIPF piece and losing a GIPF Piece
   * 3. capturing opponent's pieces and losing pieces ============================= GOAL
   *    ============================= You must try to either capture your opponent’s 3 GIPF pieces,
   *    or make your opponent run out of moves.
   */

  // TODO create check for bitboard pieces
  //  state.assertPieceCount()
  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  val initBitboard = bitboard.deepCopy()
  //  val bitboard = convertBoardToBitboard(state.board)


  // TODO Need to score piece removals
  // TODO enforce PieceRemovalRules & handle intersecting lines
  // TODO if linesWithFourInARow is empty, return/skip

	/**
	 * A regular move and an extra move are considered
	 * one single turn, whether the extra move is made
	 * aft er or before the regular move. The position of
	 * the pieces between the two moves is regarded as an
	 * “interim” situation. This means that no pieces may
	 * be removed or captured in between the regular
	 * move and the extra move. The same goes for
	 * situations where you succeed in pushing a second
	 * or third TAMSK-stack onto the central spot during
	 * one and the same turn.
	 */
	val isTamskPieceAtCenter = bitboard.getTamskMoves(currentPlayer)

	var preMoveNewlyStackedPieces: List<Piece> = emptyList()

	val bestPiecesToRetrieveCapture1= if (isTamskPieceAtCenter ==  null) {
		val bestPiecesToRetrieveCapture1 =
			resolveBoardRemovals(
				currentPlayer = currentPlayer,
				opponentPlayer = opponentPlayer,
				bitboard = bitboard,
				depth = depth,
				alphaBetaScore = alphaBetaScore,
			)

		bitboard.diff(initBitboard)
		bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

		println("--- RESOLVE BOARD REMOVALS COMPLETED (PRE-MOVE) ---")

		bestPiecesToRetrieveCapture1.move?.let { move ->
			val retrievedPieces =
				move.retrievedCapturedPiecesBit.mapNotNull {
					it.retrievedPiece
				}
			val capturedPieces =
				move.retrievedCapturedPiecesBit.mapNotNull {
					it.capturedPiece
				}

			currentPlayer.addPiecesToReserve(retrievedPieces)
			currentPlayer.addCapturedPieces(capturedPieces)

			preMoveNewlyStackedPieces = currentPlayer.combinePieces()

			bitboard.diff(initBitboard)

			// TODO Actually retrieveAndCapturePieces using move.retrievedCapturedPiecesBit list
			bitboard.removeRetrieveAndCapturePiecesFromBitboard(move.retrievedCapturedPiecesBit)

			bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

			bestPiecesToRetrieveCapture1
		}
	} else {
		null
	}

  val possibleBitMoves = bitboard.identifyAvailableMoves(currentPlayer, columnInfos)

	// TODO change to depth <= 0
  if (possibleBitMoves.isEmpty() || depth <= 0) {
    // score = evaluate s for original player
    // return [null, score]
    // TODO scoreBitboard()

    // TODO undo retrieval and capture
    // d. UNDO the piece removals to evaluate the next choice
	  if (bestPiecesToRetrieveCapture1 != null) {
	    currentPlayer.uncombinePieces(preMoveNewlyStackedPieces)

	    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

		  bestPiecesToRetrieveCapture1.move
			  ?.retrievedCapturedPiecesBit
			  ?.mapNotNull {
				  it.retrievedPiece
			  }
			  ?.forEach { piece ->
				  currentPlayer.piecesInReserve.remove(piece)
			  }

		  bestPiecesToRetrieveCapture1.move
			  ?.retrievedCapturedPiecesBit
			  ?.mapNotNull {
				  it.capturedPiece
			  }
			  ?.forEach { piece ->
				  currentPlayer.capturedPieces.remove(piece)
			  }

		  bestPiecesToRetrieveCapture1.move?.retrievedCapturedPiecesBit?.let {
			  bitboard.undoRetrieveAndCapturePieces(it)
		  }
		}

    bitboard.diff(initBitboard)

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    // TODO create scoreBitBoardState
    return BestBitMove(
        score = scoreBitboardState(bitboard, currentPlayer, opponentPlayer).toFloat()
    )
  }

  // how to see which move trigger retrieve and capture
  // how to make a move and then assess the state/
  possibleBitMoves.forEachIndexed { index, possibleBitMove ->
    //    val mutableState = state.deepCopy()
		if (isDebugEnabled) {
			println("Move: $index")
			println("Bitboard State: ${Json.encodeToString(bitboard)}")
		}

    val preMoveBitboardState = bitboard.deepCopy()

    when (possibleBitMove.moveType) {
      MoveType.AddPiece -> {
        // for each selectable dot, add piece, push piece, assess resulting state, score it
        require(possibleBitMove.piece != null || possibleBitMove.pieceType == PieceType.TAMSK) {
          "No piece was selected!\nPossible move: ${possibleBitMove}"
        }

	      bitboard.diff(preMoveBitboardState)

	      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        possibleBitMove.columnInfos.forEachIndexed { index, columnInfo ->
          listOf(
                  columnInfo.positions.first() to columnInfo.pushDirections.first,
                  columnInfo.positions.last() to columnInfo.pushDirections.second,
              )
              .forEach { (addAtIndex, pushDirection) ->
								println("""
									Move: $index
									Piece: ${possibleBitMove.piece}
									Piece Type: ${possibleBitMove.pieceType}
									Push direction: $pushDirection
									Add At Index: $addAtIndex
									Pre Move Bitboard State: ${Json.encodeToString(bitboard)}
								""".trimIndent())

	              bitboard.diff(preMoveBitboardState)

	              bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)


	              val selectedPiece = possibleBitMove.piece?.let { currentPlayer.selectPiece(it) }

                val vacantBitFound =
                    selectedPiece?.let {
                      bitboard.addPieceToBitboard(
                          addAtIndex = addAtIndex,
                          pushDirection = pushDirection,
                          col = columnInfo,
                          piece = it,
                      )
                    }
                        ?: possibleBitMove.sourceBit?.let {
                          bitboard.useTamskPotential(
                              player = currentPlayer,
                              sourceIndex = it,
                              targetIndex = addAtIndex,
                              col = columnInfo,
                              pushDirection = pushDirection,
                          )
                        }

                check(vacantBitFound != null) {
                  "Invalid board state: No vacant bit found for piece deployment."
                }

	              val postMoveBitboardState = bitboard.deepCopy()

	              println("""
									Move: $index
									Piece: ${possibleBitMove.piece}
									Piece Type: ${possibleBitMove.pieceType}
									Push direction: $pushDirection
									Add At Index: $addAtIndex
									Post Move Bitboard State: ${Json.encodeToString(bitboard)}
								""".trimIndent())

	              bitboard.assertPieceCount(
                    currentPlayer = currentPlayer,
                    nextPlayer = opponentPlayer,
                )

                // TODO Check if there is Tamsk Potential Move
                var tamskMoveScore: Float = 0f
                val isTamskPieceAtCenter = bitboard.getTamskMoves(currentPlayer)
                if (isTamskPieceAtCenter != null) {
									val preTamskMoveBitboardState = bitboard.deepCopy()

	                bitboard.diff(preTamskMoveBitboardState)

	                if (isDebugEnabled) {
		                println("--- ALPHA-BETA CALLED (TAMSK) ---")
	                }
                  tamskMoveScore =
                      alphabetaBitboardAddPieces(
                              depth = depth,
                              bitboard = bitboard,
                              currentPlayer = currentPlayer,
                              opponentPlayer = opponentPlayer,
                              //					              gameTree = gameTree,
                              alphaBetaScore = alphaBetaScore,
                          )
                          .score

	                bitboard.diff(preTamskMoveBitboardState)

	                bitboard.assertPieceCount(
		                currentPlayer = currentPlayer,
		                nextPlayer = opponentPlayer,
	                )

	                if (isDebugEnabled) {
		                println("--- ALPHA-BETA COMPLETED (TAMSK) ---")
	                }
								}

                // TODO Need to score piece removals
                // TODO enforce PieceRemovalRules & handle intersecting lines
                val bestPiecesToRetrieveCapture2 =
                    resolveBoardRemovals(
                        currentPlayer = currentPlayer,
                        opponentPlayer = opponentPlayer,
                        bitboard = bitboard,
                        depth = depth,
                        alphaBetaScore = alphaBetaScore,
                    )

	              bitboard.diff(postMoveBitboardState)

	              bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

	              println("--- RESOLVE BOARD REMOVALS COMPLETED ---")

                var newlyStackedPieces: List<Piece> = emptyList()

                if (
                    bestPiecesToRetrieveCapture2.move?.retrievedCapturedPiecesBit?.isNotEmpty() ==
                        true
                ) {
                  bestPiecesToRetrieveCapture2.move.retrievedCapturedPiecesBit.let {
                      retrievedCapturedPiecesBit ->
                    val retrievedPieces = retrievedCapturedPiecesBit.mapNotNull {
                      it.retrievedPiece
                    }
                    val capturedPieces = retrievedCapturedPiecesBit.mapNotNull {
                      it.capturedPiece
                    }

                    currentPlayer.addPiecesToReserve(retrievedPieces)
                    currentPlayer.addCapturedPieces(capturedPieces)

                    // TODO uncombine pieces
                    newlyStackedPieces = currentPlayer.combinePieces()

                    // TODO Actually retrieveAndCapturePieces using retrievedCapturedPiecesBit
                    // list
                    bitboard.removeRetrieveAndCapturePiecesFromBitboard(retrievedCapturedPiecesBit)

	                  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
									}
                }

                // TODO call alphabetaBitboardAddPieces
                val move =
                    BestBitMove(
                        move =
                            PossibleBitMove(
                                sourceBit = possibleBitMove.sourceBit,
                                targetBit = addAtIndex,
                                pushDirection = pushDirection,
                                columnInfos = listOf(columnInfo),
                                piece = possibleBitMove.piece,
                                moveType = possibleBitMove.moveType,
                            ),
                        score =
                            alphabetaBitboardAddPieces(
                                    depth = depth.minus(1),
                                    bitboard = bitboard,
                                    currentPlayer = opponentPlayer,
                                    opponentPlayer = currentPlayer,
                                    //					              gameTree = gameTree,
                                    alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                                )
                                .score +
                                tamskMoveScore +
                                if (bestPiecesToRetrieveCapture2.move != null)
                                    bestPiecesToRetrieveCapture2.score
                                else 0f,
                    )

	              if (isDebugEnabled) {
		              println("--- ALPHA-BETA COMPLETED ---")
	              }

                bitboard.assertPieceCount(
                    currentPlayer = currentPlayer,
                    nextPlayer = opponentPlayer,
                )

                // d. UNDO the piece removals to evaluate the next choice

                currentPlayer.uncombinePieces(newlyStackedPieces)

                bitboard.assertPieceCount(
                    currentPlayer = currentPlayer,
                    nextPlayer = opponentPlayer,
                )

                bestPiecesToRetrieveCapture2.move
                    ?.retrievedCapturedPiecesBit
                    ?.mapNotNull {
                      it.retrievedPiece
                    }
                    ?.forEach { piece ->
                      currentPlayer.piecesInReserve.remove(piece)
                    }

                bestPiecesToRetrieveCapture2.move
                    ?.retrievedCapturedPiecesBit
                    ?.mapNotNull {
                      it.capturedPiece
                    }
                    ?.forEach { piece ->
                      currentPlayer.capturedPieces.remove(piece)
                    }

                bestPiecesToRetrieveCapture2.move?.retrievedCapturedPiecesBit?.let {
                  bitboard.undoRetrieveAndCapturePieces(it)
                }

                bitboard.diff(postMoveBitboardState)

                bitboard.assertPieceCount(
                    currentPlayer = currentPlayer,
                    nextPlayer = opponentPlayer,
                )

                //	              val gameStateHash =
                //		              MessageDigest.getInstance("MD5")
                //			              .digest(state.toString().toByteArray())
                //			              .toHexString()

	              // TODO Undo Move - should this be after undoing piece retrieval/capture
	              selectedPiece?.let {
		              bitboard.undoAddPieceToBitboard(
			              removeAtIndex = addAtIndex,
			              vacantBitFound = vacantBitFound,
			              pushDirection = pushDirection,
			              col = columnInfo,
			              piece = it,
			              wasIndexOccupied = vacantBitFound != 0UL,
		              )
	              }

	              possibleBitMove.sourceBit?.let {
		              bitboard.undoTamskPotential(
			              sourceIndex = it,
			              player = currentPlayer,
			              removeAtIndex = addAtIndex,
			              vacantBitFound = vacantBitFound,
			              pushDirection = pushDirection,
			              col = columnInfo,
			              wasIndexOccupied = vacantBitFound != 0UL
		              )
	              }

	              bitboard.diff(preMoveBitboardState)

	              // TODO Add selected piece back to player reserve
	              selectedPiece?.let { currentPlayer.piecesInReserve.add(it) }

                bitboard.diff(preMoveBitboardState)

	              bitboard.assertPieceCount(
		              currentPlayer = currentPlayer,
		              nextPlayer = opponentPlayer,
	              )

                if (move.score.unaryMinus() > alphaBetaScore.alpha) {
                  alphaBetaScore.alpha = move.score.unaryMinus()

                  alphaBetaScore.move = move.move
                  //		              gameTree[gameStateHash] = alphaBetaScore

//                  println("line 371: ply $depth move $index: set best to: $alphaBetaScore")
//                  println(
//                      "line 373: ply $depth player: ${currentPlayer.name.name} index: $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
//                  )
                }
                if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
//                  println(
//                      "line 378: ply $depth player: ${currentPlayer.name.name} move $index: return best: $alphaBetaScore"
//                  )

                  // TODO Do I need undo bestPiecesToRetrieveCapture1 here?

                  return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
                }
              }
        }
      }

      MoveType.UsePotential -> {
        bitboard.usePiecePotential(possibleBitMove = possibleBitMove)

	      val postUsePotentialBitboardState = bitboard.deepCopy()

        // TODO Handle TAMSK Potential
        // TODO Check if there is Tamsk Potential Move
        var tamskMoveScore: Float = 0f
        val isTamskPieceAtCenter = bitboard.getTamskMoves(currentPlayer)
        if (isTamskPieceAtCenter != null) {
	        val preTamskMoveBitboardState = bitboard.deepCopy()

	        bitboard.diff(preTamskMoveBitboardState)

	        if (isDebugEnabled) {
		        println("--- ALPHA-BETA CALLED (TAMSK) ---")
	        }
	        tamskMoveScore =
		        alphabetaBitboardAddPieces(
			        depth = depth,
			        bitboard = bitboard,
			        currentPlayer = currentPlayer,
			        opponentPlayer = opponentPlayer,
			        //					              gameTree = gameTree,
			        alphaBetaScore = alphaBetaScore,
		        )
			        .score

	        bitboard.diff(preTamskMoveBitboardState)

	        bitboard.assertPieceCount(
		        currentPlayer = currentPlayer,
		        nextPlayer = opponentPlayer,
	        )

	        if (isDebugEnabled) {
		        println("--- ALPHA-BETA COMPLETED (TAMSK) ---")
	        }
				}

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)



        // TODO enforce PieceRemovalRules & handle intersecting lines
        val bestPiecesToRetrieveCapture3 =
            resolveBoardRemovals(
                currentPlayer = currentPlayer,
                opponentPlayer = opponentPlayer,
                bitboard = bitboard,
                depth = depth,
                alphaBetaScore = alphaBetaScore,
            )

	      println("--- RESOLVE BOARD REMOVALS COMPLETED (POST USE POTENTIAL) ---")

        bitboard.diff(postUsePotentialBitboardState)
        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)


        var newlyStackedPieces: List<Piece> = emptyList()

        bestPiecesToRetrieveCapture3.move?.let { move ->
          val retrievedPieces =
              move.retrievedCapturedPiecesBit.mapNotNull {
                it.retrievedPiece
              }
          val capturedPieces =
              move.retrievedCapturedPiecesBit.mapNotNull {
                it.capturedPiece
              }

          currentPlayer.addPiecesToReserve(retrievedPieces)
          currentPlayer.addCapturedPieces(capturedPieces)

          newlyStackedPieces = currentPlayer.combinePieces()

          // TODO Actually retrieveAndCapturePieces using move.retrievedCapturedPiecesBit list
          bitboard.removeRetrieveAndCapturePiecesFromBitboard(move.retrievedCapturedPiecesBit)

          bitboard.diff(postUsePotentialBitboardState)
          bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
        }


        val move =
            BestBitMove(
                move = possibleBitMove,
                score =
                    alphabetaBitboardAddPieces(
                            depth = depth.minus(1),
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer,
                            opponentPlayer = currentPlayer,
                            //					              gameTree = gameTree,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                        )
                        .score +
                        tamskMoveScore +
                        if (bestPiecesToRetrieveCapture3.move != null)
                            bestPiecesToRetrieveCapture3.score
                        else 0f,
            )

	      if (isDebugEnabled) {
		      println("--- ALPHA-BETA COMPLETED (POST USE POTENTIAL) ---")
	      }

	      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        // d. UNDO the piece removals to evaluate the next choice
        currentPlayer.uncombinePieces(newlyStackedPieces)

        bestPiecesToRetrieveCapture3.move
            ?.retrievedCapturedPiecesBit
            ?.mapNotNull {
              it.retrievedPiece
            }
            ?.forEach { piece ->
              currentPlayer.piecesInReserve.remove(piece)
            }

        bestPiecesToRetrieveCapture3.move
            ?.retrievedCapturedPiecesBit
            ?.mapNotNull {
              it.capturedPiece
            }
            ?.forEach { piece ->
              currentPlayer.capturedPieces.remove(piece)
            }

        bestPiecesToRetrieveCapture3.move?.retrievedCapturedPiecesBit?.let {
          bitboard.undoRetrieveAndCapturePieces(it)
        }

        bitboard.diff(postUsePotentialBitboardState)

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        // TODO undo use piece potential
        bitboard.undoUsePiecePotential(possibleBitMove)

        bitboard.diff(preMoveBitboardState)
        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        if (move.score.unaryMinus() > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = move.score.unaryMinus()
          alphaBetaScore.move = move.move
          //          gameTree[gameStateHash] = alphaBetaScore

//          println("line 430: ply $depth move ${index}: set best to: $alphaBetaScore")
//          println(
//              "line 432: ply $depth player: ${currentPlayer.name.name} index: ${index}: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
//          )
        }
        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
//          println(
//              "line 437: ply $depth player: ${currentPlayer.name.name} move $index: return best: $alphaBetaScore"
//          )

          // TODO Do I need undo bestPiecesToRetrieveCapture1 here?

          return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
        }
      }

      MoveType.RetrieveCapturePieces -> {}
    }
  }

//  println("line 446: ply $depth return best: $alphaBetaScore")
//  println("line 447: ply $depth moves")

  // TODO undo retrieval and capture
  // TODO Actually retrieveAndCapturePieces using move.retrievedCapturedPiecesBit list
	if (bestPiecesToRetrieveCapture1 != null) {
		currentPlayer.uncombinePieces(preMoveNewlyStackedPieces)

		bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

		bestPiecesToRetrieveCapture1.move
			?.retrievedCapturedPiecesBit
			?.mapNotNull {
				it.retrievedPiece
			}
			?.forEach { piece ->
				currentPlayer.piecesInReserve.remove(piece)
			}

		bestPiecesToRetrieveCapture1.move
			?.retrievedCapturedPiecesBit
			?.mapNotNull {
				it.capturedPiece
			}
			?.forEach { piece ->
				currentPlayer.capturedPieces.remove(piece)
			}
		bestPiecesToRetrieveCapture1.move?.retrievedCapturedPiecesBit?.let {
			bitboard.removeRetrieveAndCapturePiecesFromBitboard(it)
		}
	}

  bitboard.diff(initBitboard)

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  return BestBitMove(null, score = alphaBetaScore.alpha)
}

fun resolveBoardRemovals(
    currentPlayer: Player,
    opponentPlayer: Player,
    bitboard: Bitboard,
    depth: Int,
    alphaBetaScore: AlphaBetaScoreBit,
): BestBitMove { // or BestMove, depending on your return type
	val isDebugEnabled = false // Toggle this to true to see detailed trace logs
	if (isDebugEnabled) {
		println("--- RESOLVE BOARD REMOVALS CALLED ---")
    println("Bitboard: ${Json.encodeToString(bitboard)}")
		println("currentPlayer: ${currentPlayer}")
		println("opponentPlayer: ${opponentPlayer}")
		println("depth: ${depth}")
	}

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  val initBitboard = bitboard.deepCopy()

  val linesWithFourInARow = bitboard.evaluateLinesForFourInARow(currentPlayer)

  if (linesWithFourInARow.isNotEmpty()) {
    // 1. Generate your powerset of choices for these lines
    // 2. Loop through each choice in the powerset:
    // a. Apply the piece removals to the board

    linesWithFourInARow.forEachIndexed { index, line ->
      val playerPiecesWithPotentialPowerset =
          bitboard.createPlayerPiecesWithPotentialPowerset(
              bitboard.identifyPlayerPiecesWithPotential(listOf(line), currentPlayer)
          )

      if (playerPiecesWithPotentialPowerset.isNotEmpty()) {
        playerPiecesWithPotentialPowerset.forEachIndexed { index, playerPiecesWithPotentialToRemove
          ->
	        val piecesWithPotentialPowerset =
		        bitboard.getsSelectedPiecesWithPotentialPowerset(
			        playerPiecesWithPotentialToRemove,
			        currentPlayer,
		        )

	        val retrievedCapturedPieces =
              bitboard.createRetrieveAndCapturePiecesList(
                  columnInfos = linesWithFourInARow,
                  selectedPiecesWithPotentialPowerset = emptyList(),
                  player = currentPlayer,
              )


          check(
              piecesWithPotentialPowerset.all {
                it.retrievedPiece?.colorName == currentPlayer.name.name
              }
          )

          val retrievedPieces = retrievedCapturedPieces.mapNotNull {
            it.retrievedPiece
          }
          val capturedPieces = retrievedCapturedPieces.mapNotNull {
            it.capturedPiece
          }

          currentPlayer.addPiecesToReserve(retrievedPieces)
          currentPlayer.addCapturedPieces(capturedPieces)

          currentPlayer.addPiecesToReserve(
              piecesWithPotentialPowerset.mapNotNull { it.retrievedPiece }
          )

          val newlyStackedPieces = currentPlayer.combinePieces()

	        val bitboardCopy = bitboard.deepCopy()
	        bitboard.diff(bitboardCopy)

          bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

          // b. Recurse! Call resolveBoardRemovals() again for the SAME player
          //    to handle any chain reactions caused by the removal
          val bestPiecesToRemove =
              resolveBoardRemovals(
                  currentPlayer,
                  opponentPlayer,
                  bitboard,
                  depth,
                  alphaBetaScore,
              )

	        bitboard.diff(bitboardCopy)

	        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)


	        println("--- RESOLVE BOARD REMOVALS COMPLETED ---")

          val allRetrievedCapturedPieces =
              bestPiecesToRemove.move?.retrievedCapturedPiecesBit?.plus(retrievedCapturedPieces.plus(piecesWithPotentialPowerset))
                  ?: (retrievedCapturedPieces + piecesWithPotentialPowerset)


	        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

	        // c. Update alpha/beta scores
          val move =
              BestBitMove(
                  move =
                      PossibleBitMove(
                          retrievedCapturedPiecesBit = allRetrievedCapturedPieces,
                          moveType = MoveType.RetrieveCapturePieces,
                      ),
                  score =
                      alphabetaBitboardAddPieces(
                              depth = depth.minus(1),
                              // state = state,
                              bitboard = bitboard,
                              currentPlayer = opponentPlayer,
                              opponentPlayer = currentPlayer,
                              // gameTree = gameTree,
                              alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                          )
                          .score,
              )

	        if (isDebugEnabled) {
		        println("--- ALPHA-BETA COMPLETED (RESOLVE BOARD REMOVALS POWERSET) ---")
	        }

	        bitboard.diff(bitboardCopy)

	        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)


          // d. UNDO the piece removals to evaluate the next choice
          currentPlayer.uncombinePieces(newlyStackedPieces)

          bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

          for (piece in retrievedPieces) {
            currentPlayer.piecesInReserve.remove(piece)
          }

          for (piece in capturedPieces) {
            currentPlayer.capturedPieces.remove(piece)
          }

          piecesWithPotentialPowerset
              .mapNotNull { it.retrievedPiece }
              .forEach {
                currentPlayer.piecesInReserve.remove(it)
              }

          bitboard.undoRetrieveAndCapturePieces(retrievedCapturedPieces)
          bitboard.undoRetrieveAndCapturePieces(piecesWithPotentialPowerset)

	        bitboard.diff(initBitboard)

	        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

          // c. Update alpha/beta scores
          if (move.score.unaryMinus() > alphaBetaScore.alpha) {
            alphaBetaScore.alpha = move.score.unaryMinus()

            alphaBetaScore.move = move.move
            //								              gameTree[gameStateHash] = alphaBetaScore

//            println("line 371: ply $depth move $index: set best to: $alphaBetaScore")
//            println(
//                "line 373: ply $depth player: ${currentPlayer.name.name} index: $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
//            )
          }

          if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
//            println(
//                "line 378: ply $depth player: ${currentPlayer.name.name} move $index: return best: $alphaBetaScore"
//            )

						println("Bitboard State: ${Json.encodeToString(bitboard)}")

            return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
          }
        }
      } else {
        val retrievedCapturedPieces =
            bitboard.createRetrieveAndCapturePiecesList(
                columnInfos = linesWithFourInARow,
                selectedPiecesWithPotentialPowerset = emptyList(),
                player = currentPlayer,
            )

        val retrievedPieces = retrievedCapturedPieces.mapNotNull {
          it.retrievedPiece
        }
        val capturedPieces = retrievedCapturedPieces.mapNotNull {
          it.capturedPiece
        }

	      // TODO Assumes retreived and captured pieces are correctly categorized

        currentPlayer.addPiecesToReserve(retrievedPieces)
        currentPlayer.addCapturedPieces(capturedPieces)

        val newlyStackedPieces = currentPlayer.combinePieces()

	      val bitboardCopy = bitboard.deepCopy()

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        val move =
            BestBitMove(
                move =
                    PossibleBitMove(
                        retrievedCapturedPiecesBit = retrievedCapturedPieces,
                        moveType = MoveType.RetrieveCapturePieces,
                    ),
                score =
                    alphabetaBitboardAddPieces(
                            depth = depth.minus(1),
                            // state = state,
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer,
                            opponentPlayer = currentPlayer,
                            // gameTree = gameTree,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                        )
                        .score,
            )

	      if (isDebugEnabled) {
		      println("--- ALPHA-BETA COMPLETED (RESOLVE BOARD REMOVALS) ---")
	      }

	      bitboard.diff(bitboardCopy)

	      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        // d. UNDO the piece removals to evaluate the next choice
        currentPlayer.uncombinePieces(newlyStackedPieces)

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        for (piece in retrievedPieces) {
          currentPlayer.piecesInReserve.remove(piece)
        }

        for (piece in capturedPieces) {
          currentPlayer.capturedPieces.remove(piece)
        }

        bitboard.undoRetrieveAndCapturePieces(retrievedCapturedPieces)

	      bitboard.diff(initBitboard)

	      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        // c. Update alpha/beta scores
        if (move.score.unaryMinus() > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = move.score.unaryMinus()

          alphaBetaScore.move = move.move
          //								              gameTree[gameStateHash] = alphaBetaScore

//          println("line 371: ply $depth move $index: set best to: $alphaBetaScore")
//          println(
//              "line 373: ply $depth player: ${currentPlayer.name.name} index: $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
//          )
        }
        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
//          println(
//              "line 378: ply $depth player: ${currentPlayer.name.name} move $index: return best: $alphaBetaScore"
//          )
	        println("Bitboard State: ${Json.encodeToString(bitboard)}")
          return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
        }
      }
    }
  }

	bitboard.diff(initBitboard)

	bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
	println("Bitboard State (NO REMOVALS): ${Json.encodeToString(bitboard)}")
  return BestBitMove(null, score = alphaBetaScore.alpha)
}
