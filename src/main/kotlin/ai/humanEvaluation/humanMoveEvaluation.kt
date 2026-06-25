package org.example.ai.humanEvaluation

import java.security.MessageDigest
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle
import kotlin.collections.forEach
import kotlin.collections.mapNotNull
import kotlinx.serialization.Serializable
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
import org.example.model.retrieveAndCapturePieces
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

	// evaluate state & calculate score
	val possibleMoves = bitboard.identifyAvailableMoves(currentPlayer, columnInfos)

	if (possibleMoves.isEmpty()) {
		val winner = determineWinner(currentPlayer, opponentPlayer)
		winner?.let {
			return if (it.name == currentPlayer.name) {
				10000000
			} else if (winner.name == opponentPlayer.name) {
				-10000000
			} else {
				0 // draw
			}
		}
	}

		val countCapturedOpponentGIPFPieces =
			currentPlayer.capturedPieces.count { it.type == PieceType.GIPF }

		val countCapturedOpponentPieces =
			currentPlayer.capturedPieces
				.count { it.type != PieceType.GIPF && it.potential }
				.times(2) +
					currentPlayer.capturedPieces.count {
						it.type != PieceType.GIPF && !it.potential
					}

		val countCapturedGIPFPieces =
			opponentPlayer.capturedPieces.count { it.type == PieceType.GIPF }

		val countCapturedPieces =
			opponentPlayer.capturedPieces
				.count { it.type != PieceType.GIPF && it.potential }
				.times(2) +
					opponentPlayer.capturedPieces.count {
						it.type != PieceType.GIPF && !it.potential
					}

		val controllablePieces =
			currentPlayer.piecesInReserve.count {
				it.potential || it.type == PieceType.GIPF
			} + when(currentPlayer.name) {
				PlayerName.WHITE -> { bitboard.whitePieces.toInt() }
				PlayerName.BLACK -> { bitboard.blackPieces.toInt() }
			}

		val opponentControllablePieces =
			opponentPlayer.piecesInReserve.count {
				it.potential || it.type == PieceType.GIPF
			} + when(opponentPlayer.name) {
				PlayerName.WHITE -> { bitboard.whitePieces.toInt() }
				PlayerName.BLACK -> { bitboard.blackPieces.toInt() }
			}


		return countCapturedOpponentGIPFPieces.times(20000) +
					countCapturedOpponentPieces.times(100) +
					countCapturedGIPFPieces.times(-20000) +
					countCapturedPieces.times(-100) +
					controllablePieces.times(10) +
					opponentControllablePieces.times(-10)
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

  //  val bitboard = convertBoardToBitboard(state.board)

  // TODO Need to score piece removals
  // TODO enforce PieceRemovalRules & handle intersecting lines
  // TODO if linesWithFourInARow is empty, return/skip
  val bestPiecesToRetrieveCapture1= resolveBoardRemovals(
      currentPlayer = currentPlayer,
      opponentPlayer = opponentPlayer,
      bitboard = bitboard,
      depth = depth,
      alphaBetaScore = alphaBetaScore,
  )

	var newlyStackedPieces: List<Piece> = emptyList()

	bestPiecesToRetrieveCapture1.move?.let { move ->

		val retrievedPieces = move.retrievedCapturedPiecesBit.mapNotNull {
			it.retrievedPiece
		}
		val capturedPieces = move.retrievedCapturedPiecesBit.mapNotNull {
			it.capturedPiece
		}

		currentPlayer.addPiecesToReserve(retrievedPieces)
		currentPlayer.addCapturedPieces(capturedPieces)

		newlyStackedPieces = currentPlayer.combinePieces()

		// TODO Actually retrieveAndCapturePieces using move.retrievedCapturedPiecesBit list
		bitboard.removeRetrieveAndCapturePiecesFromBitboard(
			move.retrievedCapturedPiecesBit
		)
	}

  val possibleBitMoves = bitboard.identifyAvailableMoves(currentPlayer, columnInfos)

  if (possibleBitMoves.isEmpty() || depth == 0) {
    // score = evaluate s for original player
    // return [null, score]
    // TODO scoreBitboard()

	  // TODO undo retrieval and capture
	  // d. UNDO the piece removals to evaluate the next choice
	  bestPiecesToRetrieveCapture1.move?.retrievedCapturedPiecesBit?.mapNotNull {
		  it.retrievedPiece
	  }?.forEach { piece ->
		  currentPlayer.piecesInReserve.remove(piece)
	  }

	  bestPiecesToRetrieveCapture1.move?.retrievedCapturedPiecesBit?.mapNotNull {
		  it.capturedPiece
	  }?.forEach { piece ->
		  currentPlayer.capturedPieces.remove(piece)
	  }

	  bestPiecesToRetrieveCapture1.move?.retrievedCapturedPiecesBit?.let { bitboard.undoRetrieveAndCapturePieces(it) }

	  // TODO create scoreBitBoardState
	  return BestBitMove(score = scoreBitboardState(bitboard, currentPlayer, opponentPlayer).toFloat())
  }

  // how to see which move trigger retrieve and capture
  // how to make a move and then assess the state/
  possibleBitMoves.forEachIndexed { index, possibleBitMove ->
    //    val mutableState = state.deepCopy()

    when (possibleBitMove.moveType) {
      MoveType.AddPiece -> {
        // for each selectable dot, add piece, push piece, assess resulting state, score it
        require(possibleBitMove.piece != null || possibleBitMove.pieceType != PieceType.TAMSK) { "No piece was selected!" }

        val selectedPiece = possibleBitMove.piece?.let { currentPlayer.selectPiece(it) }

        possibleBitMove.columnInfos.forEachIndexed { index, columnInfo ->
          listOf(
                  columnInfo.positions.first() to columnInfo.pushDirections.first,
                  columnInfo.positions.last() to columnInfo.pushDirections.second,
              )
              .forEach { (addAtIndex, pushDirection) ->
                val vacantBitFound =
	                selectedPiece?.let {
		                bitboard.addPieceToBitboard(
			                addAtIndex = addAtIndex,
			                pushDirection = pushDirection,
			                col = columnInfo,
			                piece = it,
		                )
	                } ?: possibleBitMove.sourceBit?.let {
		                bitboard.useTamskPotential(
			                player = currentPlayer,
			                sourceIndex = it,
			                targetIndex = addAtIndex,
			                col = columnInfo,
			                pushDirection = pushDirection,
		                )
	                }

	              check(vacantBitFound != null) { "Invalid board state: No vacant bit found for piece deployment." }

	              // TODO Check if there is Tamsk Potential Move
								var tamskMoveScore: BestBitMove? = null
	              val isTamskPieceAtCenter = bitboard.getTamskMoves(currentPlayer)
	              if (isTamskPieceAtCenter != null) {
		              tamskMoveScore = BestBitMove(
			              move = possibleBitMove,
			              score =
				              alphabetaBitboardAddPieces(
					              depth = depth,
					              bitboard = bitboard,
					              currentPlayer = currentPlayer,
					              opponentPlayer = opponentPlayer,
					              //					              gameTree = gameTree,
					              alphaBetaScore = alphaBetaScore,
				              )
					              .score,
		              )
								}


                // TODO Need to score piece removals
                // TODO enforce PieceRemovalRules & handle intersecting lines
	              val bestPiecesToRetrieveCapture2= resolveBoardRemovals(
		              currentPlayer = currentPlayer,
		              opponentPlayer = opponentPlayer,
		              bitboard = bitboard,
		              depth = depth,
		              alphaBetaScore = alphaBetaScore,
	              )

	              var newlyStackedPieces: List<Piece> = emptyList()

	              bestPiecesToRetrieveCapture2.move?.let { move ->

		              val retrievedPieces = move.retrievedCapturedPiecesBit.mapNotNull {
			              it.retrievedPiece
		              }
		              val capturedPieces = move.retrievedCapturedPiecesBit.mapNotNull {
			              it.capturedPiece
		              }

		              currentPlayer.addPiecesToReserve(retrievedPieces)
		              currentPlayer.addCapturedPieces(capturedPieces)

		              // TODO uncombine pieces
		              newlyStackedPieces = currentPlayer.combinePieces()

		              // TODO Actually retrieveAndCapturePieces using move.retrievedCapturedPiecesBit list
		              bitboard.removeRetrieveAndCapturePiecesFromBitboard(
			              move.retrievedCapturedPiecesBit
		              )
	              }

	              // TODO call alphabetaBitboardAddPieces
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
                                .score + bestPiecesToRetrieveCapture2.score + (tamskMoveScore?.score ?: 0f),
                    )

	              // TODO Add selected piece back to player reserve
	              selectedPiece?.let { currentPlayer.piecesInReserve.add(it) }

                // TODO Undo Move
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

	              isTamskPieceAtCenter?.let {
									check(it.sourceBit != null) { "Action validation failed: Origin source bit cannot be null." }
									bitboard.undoTamskPotential(
										sourceIndex = it.sourceBit,
										player = currentPlayer,
										removeAtIndex = addAtIndex,
										vacantBitFound = vacantBitFound,
										pushDirection = pushDirection,
										col = columnInfo,
										wasIndexOccupied = vacantBitFound != 0UL,
									)
	              }


	              // d. UNDO the piece removals to evaluate the next choice
	              currentPlayer.uncombinePieces(newlyStackedPieces)

	              bestPiecesToRetrieveCapture2.move?.retrievedCapturedPiecesBit?.mapNotNull {
		              it.retrievedPiece
	              }?.forEach { piece ->
		              currentPlayer.piecesInReserve.remove(piece)
	              }

	              bestPiecesToRetrieveCapture2.move?.retrievedCapturedPiecesBit?.mapNotNull {
		              it.capturedPiece
	              }?.forEach { piece ->
		              currentPlayer.capturedPieces.remove(piece)
	              }

	              bestPiecesToRetrieveCapture2.move?.retrievedCapturedPiecesBit?.let { bitboard.undoRetrieveAndCapturePieces(it) }


                //	              val gameStateHash =
                //		              MessageDigest.getInstance("MD5")
                //			              .digest(state.toString().toByteArray())
                //			              .toHexString()

                if (move.score.unaryMinus() > alphaBetaScore.alpha) {
                  alphaBetaScore.alpha = move.score.unaryMinus()

                  alphaBetaScore.move = move.move
                  //		              gameTree[gameStateHash] = alphaBetaScore

                  println("line 371: ply $depth move $index: set best to: $alphaBetaScore")
                  println(
                      "line 373: ply $depth player: ${currentPlayer.name.name} index: $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
                  )
                }
                if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
                  println(
                      "line 378: ply $depth player: ${currentPlayer.name.name} move $index: return best: $alphaBetaScore"
                  )

	                // TODO Do I need undo bestPiecesToRetrieveCapture1 here?

                  return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
                }
              }
        }
      }

      MoveType.UsePotential -> {
        bitboard.usePiecePotential(possibleBitMove = possibleBitMove)

	      // TODO Handle TAMSK Potential
	      // TODO Check if there is Tamsk Potential Move
	      var tamskMoveScore: BestBitMove? = null
	      val isTamskPieceAtCenter = bitboard.getTamskMoves(currentPlayer)
	      if (isTamskPieceAtCenter != null) {
		      tamskMoveScore = BestBitMove(
			      move = possibleBitMove,
			      score =
				      alphabetaBitboardAddPieces(
					      depth = depth,
					      bitboard = bitboard,
					      currentPlayer = currentPlayer,
					      opponentPlayer = opponentPlayer,
					      //					              gameTree = gameTree,
					      alphaBetaScore = alphaBetaScore,
				      )
					      .score,
		      )
	      }

	      // TODO enforce PieceRemovalRules & handle intersecting lines
	      val bestPiecesToRetrieveCapture3= resolveBoardRemovals(
		      currentPlayer = currentPlayer,
		      opponentPlayer = opponentPlayer,
		      bitboard = bitboard,
		      depth = depth,
		      alphaBetaScore = alphaBetaScore,
	      )

	      var newlyStackedPieces: List<Piece> = emptyList()

	      bestPiecesToRetrieveCapture3.move?.let { move ->

		      val retrievedPieces = move.retrievedCapturedPiecesBit.mapNotNull {
			      it.retrievedPiece
		      }
		      val capturedPieces = move.retrievedCapturedPiecesBit.mapNotNull {
			      it.capturedPiece
		      }

		      currentPlayer.addPiecesToReserve(retrievedPieces)
		      currentPlayer.addCapturedPieces(capturedPieces)

		      newlyStackedPieces = currentPlayer.combinePieces()

		      // TODO Actually retrieveAndCapturePieces using move.retrievedCapturedPiecesBit list
		      bitboard.removeRetrieveAndCapturePiecesFromBitboard(
			      move.retrievedCapturedPiecesBit
		      )
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
                        .score + bestPiecesToRetrieveCapture3.score + (tamskMoveScore?.score ?: 0f),
            )

	      // TODO undo use piece potential
	      bitboard.undoUsePiecePotential(possibleBitMove)

				// d. UNDO the piece removals to evaluate the next choice
	      currentPlayer.uncombinePieces(newlyStackedPieces)

	      bestPiecesToRetrieveCapture3.move?.retrievedCapturedPiecesBit?.mapNotNull {
		      it.retrievedPiece
	      }?.forEach { piece ->
		      currentPlayer.piecesInReserve.remove(piece)
	      }

	      bestPiecesToRetrieveCapture3.move?.retrievedCapturedPiecesBit?.mapNotNull {
		      it.capturedPiece
	      }?.forEach { piece ->
		      currentPlayer.capturedPieces.remove(piece)
	      }

	      bestPiecesToRetrieveCapture3.move?.retrievedCapturedPiecesBit?.let { bitboard.undoRetrieveAndCapturePieces(it) }

        if (move.score.unaryMinus() > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = move.score.unaryMinus()
          alphaBetaScore.move = move.move
          //          gameTree[gameStateHash] = alphaBetaScore

          println("line 430: ply $depth move ${HtmlStyle.index}: set best to: $alphaBetaScore")
          println(
              "line 432: ply $depth player: ${currentPlayer.name.name} index: ${HtmlStyle.index}: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
          )
        }
        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
          println(
              "line 437: ply $depth player: ${currentPlayer.name.name} move $index: return best: $alphaBetaScore"
          )

	        // TODO Do I need undo bestPiecesToRetrieveCapture1 here?

          return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
        }
      }

	    MoveType.RetrieveCapturePieces -> {}
    }
  }

  println("line 446: ply $depth return best: $alphaBetaScore")
  println("line 447: ply $depth moves")

	// TODO undo retrieval and capture
	// TODO Actually retrieveAndCapturePieces using move.retrievedCapturedPiecesBit list
	currentPlayer.uncombinePieces(newlyStackedPieces)

	bestPiecesToRetrieveCapture1.move?.retrievedCapturedPiecesBit?.mapNotNull {
		it.retrievedPiece
	}?.forEach { piece ->
		currentPlayer.piecesInReserve.remove(piece)
	}

	bestPiecesToRetrieveCapture1.move?.retrievedCapturedPiecesBit?.mapNotNull {
		it.capturedPiece
	}?.forEach { piece ->
		currentPlayer.capturedPieces.remove(piece)
	}
	bestPiecesToRetrieveCapture1.move?.retrievedCapturedPiecesBit?.let {
		bitboard.removeRetrieveAndCapturePiecesFromBitboard(
			it
		)
	}

  return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
}

fun resolveBoardRemovals(
    currentPlayer: Player,
    opponentPlayer: Player,
    bitboard: Bitboard,
    depth: Int,
    alphaBetaScore: AlphaBetaScoreBit,
): BestBitMove { // or BestMove, depending on your return type

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
          val retrievedCapturedPieces =
              bitboard.retrieveAndCapturePieces(
                  columnInfos = linesWithFourInARow,
                  selectedPiecesWithPotentialPowerset = emptyList(),
                  player = currentPlayer,
              )

          val piecesWithPotentialPowerset =
              bitboard.getsSelectedPiecesWithPotentialPowerset(
                  playerPiecesWithPotentialToRemove,
                  currentPlayer,
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

          val allRetrievedCapturedPieces =
              bestPiecesToRemove.move?.retrievedCapturedPiecesBit?.plus(retrievedCapturedPieces)
                  ?: retrievedCapturedPieces

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

          // d. UNDO the piece removals to evaluate the next choice
	        currentPlayer.uncombinePieces(newlyStackedPieces)

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

	        // c. Update alpha/beta scores
          if (move.score.unaryMinus() > alphaBetaScore.alpha) {
            alphaBetaScore.alpha = move.score.unaryMinus()

            alphaBetaScore.move = move.move
            //								              gameTree[gameStateHash] = alphaBetaScore

            println("line 371: ply $depth move $index: set best to: $alphaBetaScore")
            println(
                "line 373: ply $depth player: ${currentPlayer.name.name} index: $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
            )
          }
          if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
            println(
                "line 378: ply $depth player: ${currentPlayer.name.name} move $index: return best: $alphaBetaScore"
            )
            return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
          }
        }
      } else {
        val retrievedCapturedPieces =
            bitboard.retrieveAndCapturePieces(
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

        currentPlayer.addPiecesToReserve(retrievedPieces)
        currentPlayer.addCapturedPieces(capturedPieces)

	      val newlyStackedPieces = currentPlayer.combinePieces()

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

	      // d. UNDO the piece removals to evaluate the next choice
	      currentPlayer.uncombinePieces(newlyStackedPieces)

	      for (piece in retrievedPieces) {
          currentPlayer.piecesInReserve.remove(piece)
        }

        for (piece in capturedPieces) {
          currentPlayer.capturedPieces.remove(piece)
        }

	      bitboard.undoRetrieveAndCapturePieces(retrievedCapturedPieces)

	      // c. Update alpha/beta scores
        if (move.score.unaryMinus() > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = move.score.unaryMinus()

          alphaBetaScore.move = move.move
          //								              gameTree[gameStateHash] = alphaBetaScore

          println("line 371: ply $depth move $index: set best to: $alphaBetaScore")
          println(
              "line 373: ply $depth player: ${currentPlayer.name.name} index: $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
          )
        }
        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
          println(
              "line 378: ply $depth player: ${currentPlayer.name.name} move $index: return best: $alphaBetaScore"
          )
          return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
        }
      }
    }
  }

  return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
}
