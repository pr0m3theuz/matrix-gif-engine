package org.example.ai.minimax

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import org.example.ai.humanEvaluation.AlphaBetaScoreBitPacked
import org.example.ai.humanEvaluation.BestPackedMove
import org.example.ai.humanEvaluation.SearchInfo
import org.example.ai.humanEvaluation.alphaBetaNgMxSearch
import org.example.ai.mcts.PackedMove
import org.example.ai.scoreBitboardState
import org.example.engine.Bound
import org.example.engine.TranspositionTable
import org.example.engine.getZobristHash
import org.example.model.Bitboard
import org.example.model.MoveType
import org.example.model.PieceType
import org.example.model.Player
import org.example.model.TurnPhase
import org.example.model.addPieceToBitboard
import org.example.model.assertPieceCount
import org.example.model.boardCenterSpotMask
import org.example.model.extractColumnInfo
import org.example.model.extractMoveType
import org.example.model.extractPiece
import org.example.model.extractPieceColor
import org.example.model.extractPieceType
import org.example.model.extractPushDirection
import org.example.model.extractSourceBit
import org.example.model.extractTargetBit
import org.example.model.generateCaptureMoves
import org.example.model.getTamskMoves
import org.example.model.identifyPiecesToRemove
import org.example.model.onlyPiece
import org.example.model.removeSelectedPieces
import org.example.model.removeUnusedTamskPotential
import org.example.model.undoAddPieceToBitboard
import org.example.model.undoRemoveUnusedTamskPotential
import org.example.model.undoRetrieveAndCapturePieces
import org.example.model.undoTamskPotential
import org.example.model.undoUsePiecePotential
import org.example.model.usePiecePotential
import org.example.model.useTamskPotential
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

fun qSearch(
	maxDepth: Int,
	depth: Int = 3,
	bitboard: Bitboard,
	currentPlayer: Player,
	opponentPlayer: Player,
	alphaBetaScore: AlphaBetaScoreBitPacked,
	rng: Random,
	turnPhase: TurnPhase? = null,
	isPVNode: Boolean = true,
	transpositionTable: TranspositionTable,
	endTime: Long = Long.MAX_VALUE,
	searchInfo: SearchInfo? = null,
): BestPackedMove {

	val initBitboard = bitboard.deepCopy()

	searchInfo?.nodesSearched++

	if (
		depth <= 0 ||
		System.currentTimeMillis() >= endTime
	) {
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

	val captureMoves = mutableListOf<PackedMove>()
	bitboard.generateCaptureMoves(currentPlayer, captureMoves)

	val pvMove = ttEntry.move != 0u && ttEntry.move.extractPieceColor() == currentPlayer.name

	// endregion

	var bound = Bound.ALPHA

	// apply moves
	outerLoop@ for ((index, packedMove) in captureMoves.withIndex()) {
		//    val mutableState = state.deepCopy()
		if (logger.isDebugEnabled()) {
			logger.info { "Move: $index" }
			logger.info { "" + ("Bitboard State: ${Json.encodeToString(bitboard)}") }
		}

		val preMoveBitboardState = bitboard.deepCopy()

		bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

		require(packedMove is PackedMove.Single)
		val moveValue = packedMove.value

		when (moveValue.extractMoveType()) {
			// region Unused Tamsk Potential
			MoveType.UnusedTamskPotential -> {
				val unusedTAMSKPotential = bitboard.removeUnusedTamskPotential(currentPlayer)

				// add the unused TAMSK Potential to the opponent's captured pieces.
				opponentPlayer.capturedPieces.add(unusedTAMSKPotential)

				val (bestPiecesToRetrieveCapture4, bestPiecesToRetrieveCapture4Score, newlyStackedPieces) =
					bestPiecesToRemove(
						currentPlayer,
						opponentPlayer,
						bitboard,
						maxDepth,
						depth.minus(1),
						AlphaBetaScoreBitPacked(),
						"ALPHA-BETA ADD PIECE",
						logger.isDebugEnabled(),
						rng,
						transpositionTable = transpositionTable,
						endTime = endTime,
						searchInfo = searchInfo,
					)

				bitboard.assertPieceCount(
					currentPlayer = currentPlayer,
					nextPlayer = opponentPlayer,
				)

				val move =
					BestPackedMove(
						move = packedMove,
						score =
							qSearch(
								maxDepth = maxDepth,
								depth = depth.minus(1),
								bitboard = bitboard,
								currentPlayer = opponentPlayer,
								opponentPlayer = currentPlayer,
								alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
								rng = rng,
								isPVNode = isPVNode && moveValue == ttEntry.move,
								transpositionTable = transpositionTable,
								endTime = endTime,
								searchInfo = searchInfo,
							)
								.score
					)

				currentPlayer.uncombinePieces(newlyStackedPieces)

				currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture4)

				bitboard.undoRetrieveAndCapturePieces(
					bestPiecesToRetrieveCapture4,
					"ALPHA-BETA UNUSED TAMSK POTENTIAL @ depth $depth",
				)

				bitboard.assertPieceCount(
					currentPlayer = currentPlayer,
					nextPlayer = opponentPlayer,
				)

				// Undo removeUnusedTamskPotential
				bitboard.undoRemoveUnusedTamskPotential(moveValue)
				opponentPlayer.capturedPieces.remove(unusedTAMSKPotential)

				bitboard.assertPieceCount(
					currentPlayer = currentPlayer,
					nextPlayer = opponentPlayer,
				)

				val compositeScore = move.score.unaryMinus() +
						if (bestPiecesToRetrieveCapture4.isNotEmpty())
							bestPiecesToRetrieveCapture4Score
						else 0

				if (compositeScore > alphaBetaScore.alpha) {
					alphaBetaScore.alpha = compositeScore
					alphaBetaScore.move = move.move
					bound = Bound.EXACT
				}
				if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
					bound = Bound.BETA
					break@outerLoop
				}
			}
			// endregion
			// region MoveType.AddPiece
			MoveType.AddPiece -> {
				require(
					moveValue.extractPiece() != null || moveValue.extractPieceType() == PieceType.TAMSK
				) {
					"No piece was selected!\nPossible move: $moveValue"
				}

				//        bitboard.diff(preMoveBitboardState)

				bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

				val addAtIndex = moveValue.extractTargetBit()
				val pushDirection = moveValue.extractPushDirection()
				val columnInfo = moveValue.extractColumnInfo()

				requireNotNull(addAtIndex)
				requireNotNull(pushDirection)
				requireNotNull(columnInfo)

				if (logger.isDebugEnabled()) {
					logger.info {
						"" +
								("""
									Move: $index
									Piece: ${moveValue.extractPiece()}
									Piece Type: ${moveValue.extractPieceType()}
									Push direction: $pushDirection
									Add At Index: $addAtIndex
									Pre Move Bitboard State: ${Json.encodeToString(bitboard)}
								"""
									.trimIndent())
					}
				}

				bitboard.diff(preMoveBitboardState)

				bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

				val vacantBitFound =
					if (moveValue.extractSourceBit() == boardCenterSpotMask) {
						bitboard.useTamskPotential(moveValue)
					} else {
						val selectedPiece = moveValue.onlyPiece().let { currentPlayer.selectPiece(it) }

						bitboard.addPieceToBitboard(moveValue)
					}

				//        check(vacantBitFound != null) {
				//          "Invalid board state: No vacant bit found for piece deployment."
				//        }

//				val postMoveBitboardState = bitboard.deepCopy()

				if (logger.isDebugEnabled()) {
					logger.info {
						"" +
								("""
									Move: $index
									Piece: ${moveValue.extractPiece()}
									Piece Type: ${moveValue.extractPieceType()}
									Push direction: $pushDirection
									Add At Index: $addAtIndex
									Post Move Bitboard State: ${Json.encodeToString(bitboard)}
								"""
									.trimIndent())
					}
				}

				bitboard.assertPieceCount(
					currentPlayer = currentPlayer,
					nextPlayer = opponentPlayer,
				)

				var tamskMoveScore = 0
				val isTamskPieceAtCenter = mutableListOf<PackedMove>()
				bitboard.getTamskMoves(currentPlayer, isTamskPieceAtCenter)
				if (isTamskPieceAtCenter.isNotEmpty()) {
					val preTamskMoveBitboardState = bitboard.deepCopy()

					if (logger.isDebugEnabled()) {
						logger.info { "" + ("--- ALPHA-BETA CALLED (TAMSK) ---") }
					}
					tamskMoveScore =
						qSearch(
							maxDepth = maxDepth,
							depth = depth.minus(1),
							bitboard = bitboard,
							currentPlayer = currentPlayer,
							opponentPlayer = opponentPlayer,
							alphaBetaScore = AlphaBetaScoreBitPacked(),
							rng = rng,
							isPVNode = isPVNode && moveValue == ttEntry.move,
							turnPhase = TurnPhase.ExtraMove,
							transpositionTable = transpositionTable,
							endTime = endTime,
							searchInfo = searchInfo,
						)
							.score

					bitboard.diff(preTamskMoveBitboardState)

					bitboard.assertPieceCount(
						currentPlayer = currentPlayer,
						nextPlayer = opponentPlayer,
					)

					if (logger.isDebugEnabled()) {
						logger.info { "" + ("--- ALPHA-BETA COMPLETED (TAMSK) ---") }
					}
				}

				// TODO Need to score piece removals
				// TODO enforce PieceRemovalRules & handle intersecting lines
				val (bestPiecesToRetrieveCapture2, bestPiecesToRetrieveCapture2Score, newlyStackedPieces) =
					bestPiecesToRemove(
						currentPlayer,
						opponentPlayer,
						bitboard,
						maxDepth,
						depth.minus(1),
						AlphaBetaScoreBitPacked(),
						"ALPHA-BETA ADD PIECE",
						logger.isDebugEnabled(),
						rng,
						transpositionTable = transpositionTable,
						endTime = endTime,
						searchInfo = searchInfo
					)

				bitboard.assertPieceCount(
					currentPlayer = currentPlayer,
					nextPlayer = opponentPlayer,
				)

				val move =
					BestPackedMove(
						move = packedMove,
						score =
							qSearch(
								maxDepth = maxDepth,
								depth = depth.minus(1),
								bitboard = bitboard,
								currentPlayer = opponentPlayer,
								opponentPlayer = currentPlayer,
								alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
								rng = rng,
								isPVNode = isPVNode && moveValue == ttEntry.move,
								transpositionTable = transpositionTable,
								endTime = endTime,
								searchInfo = searchInfo,
							)
								.score
					)

				if (logger.isDebugEnabled()) {
					logger.info { "" + ("--- ALPHA-BETA COMPLETED ---") }
				}

				// d. UNDO the piece removals to evaluate the next choice

				currentPlayer.uncombinePieces(newlyStackedPieces)

				currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture2)

				//        bitboard.assertPieceCount(
				//            currentPlayer = currentPlayer,
				//            nextPlayer = opponentPlayer,
				//        )

				bitboard.undoRetrieveAndCapturePieces(
					bestPiecesToRetrieveCapture2,
					"ALPHA-BETA ADD PIECE @ depth $depth",
				)

				//        bitboard.diff(postMoveBitboardState)

				bitboard.assertPieceCount(
					currentPlayer = currentPlayer,
					nextPlayer = opponentPlayer,
				)

				// TODO Undo Move - should this be after undoing piece retrieval/capture

				if (moveValue.extractSourceBit() == boardCenterSpotMask) {

					bitboard.undoTamskPotential(
						move = moveValue,
						vacantBitFound = vacantBitFound,
						wasIndexOccupied = vacantBitFound != ULong.MAX_VALUE,
					)

					bitboard.diff(preMoveBitboardState)
				} else {

					bitboard.undoAddPieceToBitboard(
						move = moveValue,
						vacantBitFound = vacantBitFound,
						wasIndexOccupied = vacantBitFound != ULong.MAX_VALUE,
					)

					bitboard.diff(preMoveBitboardState)

					// TODO Add selected piece back to player reserve
					moveValue.onlyPiece().let { currentPlayer.piecesInReserve.add(it) }
				}

				//        bitboard.restorePreviousBoardState(preMoveBitboardState)

				bitboard.assertPieceCount(
					currentPlayer = currentPlayer,
					nextPlayer = opponentPlayer,
				)

				val compositeScore = move.score.unaryMinus() + tamskMoveScore +
						if (bestPiecesToRetrieveCapture2.isNotEmpty())
							bestPiecesToRetrieveCapture2Score
						else 0

				if (compositeScore > alphaBetaScore.alpha) {
					alphaBetaScore.alpha = compositeScore
					alphaBetaScore.move = move.move

					bound = Bound.EXACT
				}
				if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
					bound = Bound.BETA
					break@outerLoop
				}
			}
			// endregion

			// region MoveType.UsePotential
			MoveType.UsePotential -> {
//				val preUsePotentialBoardState = bitboard.deepCopy()

				bitboard.usePiecePotential(
					move = moveValue,
				)

				val postUsePotentialBitboardState = bitboard.deepCopy()

				// TODO Handle TAMSK Potential
				// TODO Check if there is Tamsk Potential Move
				var tamskMoveScore = 0
				val isTamskPieceAtCenter = mutableListOf<PackedMove>()
				bitboard.getTamskMoves(currentPlayer, isTamskPieceAtCenter)
				if (isTamskPieceAtCenter.isNotEmpty()) {
					val preTamskMoveBitboardState = bitboard.deepCopy()

					bitboard.diff(preTamskMoveBitboardState)

					if (logger.isDebugEnabled()) {
						logger.info { "" + ("--- ALPHA-BETA USE-POTENTIAL CALLED (TAMSK) ---") }
					}
					tamskMoveScore =
						qSearch(
							maxDepth = maxDepth,
							depth = depth.minus(1),
							bitboard = bitboard,
							currentPlayer = currentPlayer,
							opponentPlayer = opponentPlayer,
							alphaBetaScore = AlphaBetaScoreBitPacked(),
							rng = rng,
							isPVNode = isPVNode && moveValue == ttEntry.move,
							turnPhase = TurnPhase.ExtraMove,
							transpositionTable = transpositionTable,
							endTime = endTime,
							searchInfo = searchInfo,
						)
							.score

					bitboard.diff(preTamskMoveBitboardState)

					bitboard.assertPieceCount(
						currentPlayer = currentPlayer,
						nextPlayer = opponentPlayer,
					)

					if (logger.isDebugEnabled()) {
						logger.info { "" + ("--- ALPHA-BETA COMPLETED (TAMSK) ---") }
					}
				}

				bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

				// TODO enforce PieceRemovalRules & handle intersecting lines
				val (bestPiecesToRetrieveCapture3, bestPiecesToRetrieveCapture3Score, newlyStackedPieces) =
					bestPiecesToRemove(
						currentPlayer,
						opponentPlayer,
						bitboard,
						maxDepth,
						depth.minus(1),
						AlphaBetaScoreBitPacked(),
						"ALPHA-BETA USE POTENTIAL",
						logger.isDebugEnabled(),
						rng = rng,
						transpositionTable = transpositionTable,
						endTime = endTime,
						searchInfo = searchInfo
					)

				val beforeRecursionBitboardState = bitboard.deepCopy()

				val move =
					BestPackedMove(
						move = packedMove,
						score =
							qSearch(
								maxDepth = maxDepth,
								depth = depth.minus(1),
								bitboard = bitboard,
								currentPlayer = opponentPlayer,
								opponentPlayer = currentPlayer,
								alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
								rng = rng,
								isPVNode = isPVNode && moveValue == ttEntry.move,
								transpositionTable = transpositionTable,
								endTime = endTime,
								searchInfo = searchInfo,
							)
								.score
					)

				if (logger.isDebugEnabled()) {
					logger.info { "" + ("--- ALPHA-BETA COMPLETED (POST USE POTENTIAL) ---") }
				}

				bitboard.diff(beforeRecursionBitboardState)

				// d. UNDO the piece removals to evaluate the next choice
				currentPlayer.uncombinePieces(newlyStackedPieces)

				currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture3)

				bitboard.undoRetrieveAndCapturePieces(
					bestPiecesToRetrieveCapture3,
					"ALPHA-BETA USE POTENTIAL @ depth $depth",
				)

				bitboard.diff(postUsePotentialBitboardState)

				bitboard.assertPieceCount(
					currentPlayer = currentPlayer,
					nextPlayer = opponentPlayer,
				)

				// TODO undo use piece potential
				bitboard.undoUsePiecePotential(moveValue)

				bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)


				val compositeScore = move.score.unaryMinus() + tamskMoveScore +
						if (bestPiecesToRetrieveCapture3.isNotEmpty())
							bestPiecesToRetrieveCapture3Score
						else 0

				if (compositeScore > alphaBetaScore.alpha) {
					alphaBetaScore.alpha = compositeScore
					alphaBetaScore.move = move.move

					bound = Bound.EXACT
				}
				if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
					bound = Bound.BETA
					break@outerLoop
				}
			}
			// endregion
			// region MoveType.RetrieveCapturePieces
			MoveType.RetrieveCapturePieces -> {}

			// endregion
		}
	}

	alphaBetaScore.move?.let {
		transpositionTable.save(
			entry = ttEntry,
			hash = bitboard.getZobristHash(currentPlayer),
			bound = bound,
			depth = maxDepth - depth,
			move = it,
			value = alphaBetaScore.alpha,
		)
	}

	bitboard.diff(initBitboard)

	bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

	return BestPackedMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
}

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

			/**
			 * TODO causes stack overflow error, but an empty list is necessary as a player can leave the
			 * stack in play TODO Minimax/MCTS — what it would be like to remove at least one of these
			 * pieces if all pieces have potentials use line score heuristic and pieces in reserve TODO
			 * return of a list containing different combinations of bit positions
			 */
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
