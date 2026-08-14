package org.example.ai.minimax

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.random.Random
import kotlinx.serialization.json.Json
import org.example.ai.humanEvaluation.AlphaBetaScoreBitPacked
import org.example.ai.humanEvaluation.BestPackedMove
import org.example.ai.humanEvaluation.SearchInfo
import org.example.ai.mcts.PackedMove
import org.example.ai.scoreBitboardState
import org.example.engine.Bound
import org.example.engine.TranspositionTable
import org.example.engine.getZobristHash
import org.example.model.*

private val logger = KotlinLogging.logger {}

fun qSearch(
    maxDepth: Int,
    depth: Int = 3,
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    alphaBetaScore: AlphaBetaScoreBitPacked,
    rng: Random,
    turnPhase: TurnPhase,
    normalMoveMade: Boolean = false,
    isPVNode: Boolean = true,
    transpositionTable: TranspositionTable,
    endTime: Long = Long.MAX_VALUE,
    searchInfo: SearchInfo? = null,
): BestPackedMove {

  val initBitboard = bitboard.deepCopy()

  searchInfo?.nodesSearched++

  if (depth <= 0 || System.currentTimeMillis() >= endTime) {
    // score = evaluate s for original player
    // return [null, score]

    val score = scoreBitboardState(bitboard, currentPlayer, opponentPlayer, rng)

    //		// d. UNDO the piece removals to evaluate the next choice
    //		if (bestPiecesToRetrieveCapture1.isNotEmpty()) {
    //			currentPlayer.uncombinePieces(preMoveNewlyStackedPieces)
    //
    //			bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
    //
    //			currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture1)
    //
    //			bitboard.undoRetrieveAndCapturePieces(
    //				bestPiecesToRetrieveCapture1,
    //				"ALPHA-BETA MAIN BEGINNING @ depth $depth",
    //			)
    //		}
    //
    //		bitboard.diff(initBitboard)
    //
    //		bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    return BestPackedMove(
        score = score,
    )
  }

  // region Transposition Table & Move Ordering
  val (ttFound, ttEntry) =
      transpositionTable.probe(
          hash = bitboard.getZobristHash(currentPlayer),
      )

  if (!isPVNode && ttFound) {
    if (ttEntry.bound == Bound.EXACT) {

      return BestPackedMove(
          score = ttEntry.score,
      )
    }
    if (ttEntry.bound == Bound.BETA && ttEntry.score >= alphaBetaScore.beta) {

      return BestPackedMove(
          score = ttEntry.score, // Fail-high
      )
    }
    if (ttEntry.bound == Bound.ALPHA && ttEntry.score >= alphaBetaScore.alpha) {

      return BestPackedMove(
          score = ttEntry.score, // Fail-low
      )
    }
  }

  val standPat = scoreBitboardState(bitboard, currentPlayer, opponentPlayer, rng)

  if (standPat >= alphaBetaScore.beta) {
    return BestPackedMove(score = alphaBetaScore.beta)
  }

  if (standPat > alphaBetaScore.alpha) {
    alphaBetaScore.alpha = standPat
  }

  val movesBuffer = mutableListOf<PackedMove>()
  if (turnPhase == TurnPhase.PlayerInputWindow) {
    bitboard.generateCaptureMoves(currentPlayer, movesBuffer)
  } else {
    bitboard.generateMoves(currentPlayer, turnPhase = turnPhase, movesBuffer)
  }

  val pvMove = ttEntry.move != 0u && ttEntry.move.extractPieceColor() == currentPlayer.name

  // endregion

  var bound = Bound.ALPHA

  // apply moves
  outerLoop@ for ((index, move) in movesBuffer.withIndex()) {
    //    val mutableState = state.deepCopy()
    if (logger.isDebugEnabled()) {
      logger.info { "Move: $index" }
      logger.info { "" + ("Bitboard State: ${Json.encodeToString(bitboard)}") }
    }

    val preMoveBitboardState = bitboard.deepCopy()

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    if (logger.isDebugEnabled()) {
      logger.trace { "Phase: $turnPhase, move $index: $move" }
    }

    val undoInfo = applyMove(bitboard, currentPlayer, opponentPlayer, move)

    val (nextPhase, nextNormalMoveMade) =
        determineNextPhase(bitboard, currentPlayer, move, turnPhase, normalMoveMade)

    val moveScore =
        BestPackedMove(
            move = move,
            score =
                when {
                  nextPhase != TurnPhase.PlayerInputWindow -> {
                    // PieceRemoval or ExtraMove: same player, same window, same depth, same
                    // normalMoveMade
                    qSearch(
                            maxDepth = maxDepth,
                            depth = depth,
                            bitboard = bitboard,
                            currentPlayer = currentPlayer,
                            opponentPlayer = opponentPlayer,
                            alphaBetaScore = alphaBetaScore.deepCopy(),
                            rng = rng,
                            turnPhase = nextPhase,
                            normalMoveMade = nextNormalMoveMade,
                            isPVNode = isPVNode,
                            transpositionTable = transpositionTable,
                            endTime = endTime,
                            searchInfo = searchInfo,
                        )
                        .score
                  }
                  !nextNormalMoveMade -> {
                    // Normal move still owed this turn: same player, same window — this is the
                    // pre-move loops finishing (rows/TAMSK the opponent left you), not a ply
                    // boundary.
                    qSearch(
                            maxDepth = maxDepth,
                            depth = depth,
                            bitboard = bitboard,
                            currentPlayer = currentPlayer,
                            opponentPlayer = opponentPlayer,
                            alphaBetaScore = alphaBetaScore,
                            rng = rng,
                            turnPhase = TurnPhase.PlayerInputWindow,
                            normalMoveMade = false,
                            isPVNode = isPVNode,
                            transpositionTable = transpositionTable,
                            endTime = endTime,
                            searchInfo = searchInfo,
                        )
                        .score
                  }
                  else -> {
                    // Turn genuinely complete: real ply boundary.
                    //  val moveValue = (move as PackedMove.Single).value

                    -qSearch(
                            maxDepth = maxDepth,
                            depth = depth - 1,
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer,
                            opponentPlayer = currentPlayer,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            rng = rng,
                            turnPhase = TurnPhase.PlayerInputWindow,
                            normalMoveMade = false,
                            isPVNode = isPVNode,
                            transpositionTable = transpositionTable,
                            endTime = endTime,
                            searchInfo = searchInfo,
                        )
                        .score
                  }
                }
        )

    undoMove(bitboard, currentPlayer, opponentPlayer, move, undoInfo)

    if (moveScore.score > alphaBetaScore.alpha) {
      alphaBetaScore.alpha = moveScore.score - alphaBetaScore.alpha
      alphaBetaScore.move = moveScore.move
      bound = Bound.EXACT
    }
    if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
      bound = Bound.BETA
      break@outerLoop
    }
  }

  if (turnPhase == TurnPhase.PlayerInputWindow) {
    transpositionTable.save(
        entry = ttEntry,
        hash = bitboard.getZobristHash(currentPlayer),
        bound = bound,
        depth = maxDepth - depth,
        move = alphaBetaScore.move,
        value = alphaBetaScore.alpha,
    )
  }

  bitboard.diff(initBitboard)
  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  return BestPackedMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
}

/*
private fun bestPiecesToRemove(
	currentPlayer: Player,
	opponentPlayer: Player,
	bitboard: Bitboard,
	maxDepth: Int,
	depth: Int,
	alphaBetaScore: AlphaBetaScoreBitPacked,
	caller: String,
	isDebugEnabled: Boolean,
	rng: Random,
	transpositionTable: TranspositionTable,
	endTime: Long = Long.MAX_VALUE,
	searchInfo: SearchInfo? = null,
): Triple<List<UInt>, Int, List<UInt>> {
	val initBitboard = bitboard.deepCopy()

	if (System.currentTimeMillis() >= endTime) {
		return Triple(emptyList(), Int.MIN_VALUE, emptyList())
	}

	val resolveBoardRemovals =
		resolveBoardRemovalsQS(
			maxDepth = maxDepth,
			currentPlayer = currentPlayer,
			opponentPlayer = opponentPlayer,
			bitboard = bitboard,
			depth = depth,
			alphaBetaScore = alphaBetaScore.copy(move = null),
			rng = rng,
			"$caller bestPiecesToRemove()",
			transpositionTable = transpositionTable,
			endTime = endTime,
			searchInfo = searchInfo,
		)

	bitboard.diff(initBitboard)

	bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

	if (logger.isDebugEnabled()) {
		logger.info { "" + ("--- RESOLVE BOARD REMOVALS COMPLETED ---") }
	}
	var newlyStackedPieces: List<UInt> = emptyList()

	if (
		resolveBoardRemovals.move != null &&
		(resolveBoardRemovals.move as PackedMove.Multiple).values.isNotEmpty()
	) {
		resolveBoardRemovals.move.let { removePieces ->
			val move = mutableListOf<UInt>()

			bitboard.removeSelectedPieces(
				player = currentPlayer,
				piecesToRemove = removePieces.values.distinct(),
				movesBuffer = move,
				caller = "$caller bestPiecesToRemove() @ depth $depth",
			)

			currentPlayer.addRetrievedCapturedPieces(move)

			// TODO uncombine pieces
			newlyStackedPieces = currentPlayer.combinePieces()

			// TODO Actually retrieveAndCapturePieces using retrievedCapturedPiecesBit
			// list
			//      bitboard.undoRetrieveAndCapturePieces(move)

			bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

			return Triple(
				move,
				resolveBoardRemovals.score,
				newlyStackedPieces,
			)
		}
	} else {
		return Triple(emptyList(), resolveBoardRemovals.score, emptyList())
	}
}

private fun resolveBoardRemovalsQS(
	maxDepth: Int,
	currentPlayer: Player,
	opponentPlayer: Player,
	bitboard: Bitboard,
	depth: Int,
	alphaBetaScore: AlphaBetaScoreBitPacked,
	rng: Random,
	caller: String = "",
	transpositionTable: TranspositionTable,
	endTime: Long = Long.MAX_VALUE,
	searchInfo: SearchInfo? = null,
): BestPackedMove {
	if (logger.isDebugEnabled()) {
		logger.info { "" + ("--- RESOLVE BOARD REMOVALS CALLED ---") }
		logger.info { "" + ("Bitboard: ${Json.encodeToString(bitboard)}") }
		logger.info { "currentPlayer: $currentPlayer" }
		logger.info { "opponentPlayer: $opponentPlayer" }
		logger.info { "depth: $depth" }
	}

	if (System.currentTimeMillis() >= endTime) {
		return BestPackedMove()
	}

	bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

	val initBitboard = bitboard.deepCopy()

	val removePiecesPowerset: MutableList<PackedMove> = mutableListOf<PackedMove>()
	bitboard.identifyPiecesToRemove(currentPlayer, removePiecesPowerset)

	// region Retrieve/Capture Pieces with Player Potential Powerset
	removePiecesPowerset.sortByDescending {
		when (it) {
			is PackedMove.Multiple -> {
				it.values.size
			}
			is PackedMove.Single -> {
				0
			}
		}
	}
	if (removePiecesPowerset.isNotEmpty()) {
		removalLoop@ for ((index, removePieces) in removePiecesPowerset.withIndex()) {
			// 1. Generate your powerset of choices for these lines
			// 2. Loop through each choice in the powerset:
			// a. Apply the piece removals to the board

			*/
/**
 * TODO causes stack overflow error, but an empty list is necessary as a player can leave the stack
 * in play TODO Minimax/MCTS — what it would be like to remove at least one of these pieces if all
 * pieces have potentials use line score heuristic and pieces in reserve TODO return of a list
 * containing different combinations of bit positions
 *//*

   			require(removePieces is PackedMove.Multiple)

   			val retrievedCapturedPieces = mutableListOf<UInt>()

   			if (logger.isDebugEnabled()) {
   				logger.debug { "" + ("--- resolveBoardRemovals called removeSelectedPiecesToRemove() ---") }
   			}

   			bitboard.removeSelectedPieces(
   				player = currentPlayer,
   				piecesToRemove = removePieces.values.distinct(),
   				movesBuffer = retrievedCapturedPieces,
   				caller = "$caller resolveBoardRemovals()",
   			)

   			currentPlayer.addRetrievedCapturedPieces(retrievedCapturedPieces)

   			val newlyStackedPieces = currentPlayer.combinePieces()

   			val postPieceRemovalBitboardState = bitboard.deepCopy()
   			bitboard.diff(postPieceRemovalBitboardState)

   			bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

   			// b. Recurse! Call resolveBoardRemovals() again for the SAME player
   			//    to handle any chain reactions caused by the removal
   			//      val bestPiecesToRemove =
   			//          resolveBoardRemovals(
   			//              currentPlayer = currentPlayer,
   			//              opponentPlayer = opponentPlayer,
   			//              bitboard = bitboard,
   			//              depth = depth,
   			//              alphaBetaScore = alphaBetaScore.copy(move = null),
   			//          )

   			//      bitboard.diff(postPieceRemovalBitboardState)

   			if (logger.isDebugEnabled()) {
   				logger.info { "" + ("--- RESOLVE BOARD REMOVALS COMPLETED ---") }
   			}

   			//      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

   			// c. Update alpha/beta scores
   			val move =
   				BestPackedMove(
   					move = removePieces,
   					score =
   						qSearch(
   							maxDepth = maxDepth,
   							depth = depth.minus(1),
   							bitboard = bitboard,
   							currentPlayer = opponentPlayer,
   							opponentPlayer = currentPlayer,
   							alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
   							rng = rng,
   							transpositionTable = transpositionTable,
   							endTime = endTime,
   							searchInfo = searchInfo,
   						)
   							.score,
   				)

   			if (logger.isDebugEnabled()) {
   				logger.info { "" + ("--- ALPHA-BETA COMPLETED (RESOLVE BOARD REMOVALS POWERSET) ---") }
   			}

   			//      bitboard.diff(postPieceRemovalBitboardState)

   			//      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

   			// d. UNDO the piece removals to evaluate the next choice
   			currentPlayer.uncombinePieces(newlyStackedPieces)

   			//      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

   			currentPlayer.removeRetrievedCapturedPieces(retrievedCapturedPieces)

   			bitboard.undoRetrieveAndCapturePieces(
   				retrievedCapturedPieces,
   				"$caller resolveBoardRemovals() @ depth $depth",
   			)
   			//      bitboard.undoRetrieveAndCapturePieces(piecesWithPotentialPowerset)

   			//      bitboard.diff(initBitboard)

   			bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

   			// c. Update alpha/beta scores
   			if (move.score.unaryMinus() > alphaBetaScore.alpha) {
   				alphaBetaScore.alpha = move.score.unaryMinus()

   				alphaBetaScore.move = move.move
   				//								              gameTree[gameStateHash] = alphaBetaScore

   				//            logger.info { "" + ("line 371: ply $depth move $index: set best to:
   				// $alphaBetaScore") }
   				//            logger.info { "" + (//                "line 373: ply $depth player:
   				// ${currentPlayer.name} index:
   				// $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
   				// ) }
   			}

   			if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
   				//            logger.info { "" + (//                "line 378: ply $depth player:
   				// ${currentPlayer.name} move $index:
   				// return best: $alphaBetaScore"
   				// ) }

   				if (logger.isDebugEnabled()) {
   					logger.info { "" + ("Bitboard State: ${Json.encodeToString(bitboard)}") }
   				}
   				break@removalLoop
   				//            return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
   			}
   		}
   	}
   	// endregion

   	bitboard.diff(initBitboard)

   	bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

   	if (logger.isDebugEnabled()) {
   		logger.info { "" + ("Bitboard State (NO REMOVALS): ${Json.encodeToString(bitboard)}") }
   	}
   	// TODO
   	return BestPackedMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
   }
   */
