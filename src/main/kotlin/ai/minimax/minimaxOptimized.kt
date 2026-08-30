@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.ai.humanEvaluation

import kotlin.math.max
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.ai.mcts.PackedMove
import org.example.ai.minimax.qSearch
import org.example.ai.scoreBitboardState
import org.example.engine.Bound
import org.example.engine.TransitionTableEntry
import org.example.engine.TranspositionTable
import org.example.engine.getZobristHash
import org.example.model.*

val MAX_HISTORY: Int = 100000
const val INFINITY: Int = 2_000_000_000
const val MAX_Q_DEPTH: Int = 2

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

data class BestPackedMove(
    val move: PackedMove? = null,
    var score: Int = 0,
)

@Serializable
data class SearchInfo(
    var model: Model,
    var strength: String,
    var turnPhase: MutableList<TurnPhase> = mutableListOf(),
    var depths: MutableList<Double> = mutableListOf(),
    var nodesSearched: MutableList<Double> = mutableListOf(),
    val branchingCounts: MutableList<Double> = mutableListOf(),
    val totalActions: MutableList<Double> = mutableListOf(),
    val totalActionsSum: MutableList<Double> = mutableListOf(),
    var totalNodesEvaluated: Double = 0.0,
    var totalAvailableMovesEvaluated: Double = 0.0,
    var maxDepthReached: Double = 0.0,
) {
  val computedNodesEvaluated: Double
    get() =
        if (totalNodesEvaluated > 0.0) totalNodesEvaluated
        else if (nodesSearched.isNotEmpty()) nodesSearched.sum() else totalActions.size.toDouble()

  val computedAvailableMovesEvaluated: Double
    get() =
        if (totalAvailableMovesEvaluated > 0.0) totalAvailableMovesEvaluated else totalActions.sum()

  val computedMaxDepth: Double
    get() = if (maxDepthReached > 0.0) maxDepthReached else (depths.maxOrNull() ?: 0.0)

  val averageBranchingFactor: Double
    get() {
      val nodes = computedNodesEvaluated
      val moves = computedAvailableMovesEvaluated
      return if (nodes > 0.0) moves / nodes else 0.0
    }

  val effectiveBranchingFactor: Double
    get() {
      val nodes = computedNodesEvaluated
      val depth = computedMaxDepth
      return if (nodes > 0.0 && depth > 0.0) Math.pow(nodes, 1.0 / depth) else 0.0
    }
}

data class AlphaBetaScoreBitPacked(
    var move: PackedMove? = null,
    var alpha: Int = -INFINITY,
    var beta: Int = INFINITY,
) {
  fun swapAlphaBeta(): AlphaBetaScoreBitPacked {
    return AlphaBetaScoreBitPacked(
        alpha = -beta,
        beta = -alpha,
    )
  }

  fun deepCopy(): AlphaBetaScoreBitPacked {
    return AlphaBetaScoreBitPacked(
        alpha = alpha,
        beta = beta,
    )
  }
}

typealias SearchFunction =
    (
        maxDepth: Int,
        depth: Int,
        bitboard: Bitboard,
        currentPlayer: Player,
        opponentPlayer: Player,
        alphaBetaScore: AlphaBetaScoreBitPacked,
        rng: Random,
        turnPhase: TurnPhase?,
        isPVNode: Boolean,
        transpositionTable: TranspositionTable,
        endTime: Long,
        searchInfo: SearchInfo?,
    ) -> BestPackedMove

// negamax w/ alpha-beta pruning
fun alphaBetaNgMxSearch(
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
    enableQSearch: Boolean,
    collectSearchInfo: Boolean = false,
    enableTranspositionTable: Boolean,
    enableKillerMoves: Boolean,
): BestPackedMove {
  if (logger.isDebugEnabled()) {
    logger.info { "--- ALPHA-BETA CALLED ---" }
    logger.info { "currentPlayer: $currentPlayer" }
    logger.info { "opponentPlayer: $opponentPlayer" }
    logger.info { "depth: $depth" }
    logger.info { "Bitboard: ${Json.encodeToString(bitboard)}" }
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
  //  val initialHash = bitboard.getZobristHash(currentPlayer)
  //  val bitboard = convertBoardToBitboard(state.board)

  // TODO Need to score piece removals
  // TODO enforce PieceRemovalRules & handle intersecting lines
  // TODO if linesWithFourInARow is empty, return/skip

  /**
   * A regular move and an extra move are considered one single turn, whether the extra move is made
   * after or before the regular move. The position of the pieces between the two moves is regarded
   * as an “interim” situation. This means that no pieces may be removed or captured in between the
   * regular move and the extra move. The same goes for situations where you succeed in pushing a
   * second or third TAMSK-stack onto the central spot during one and the same turn.
   */
  val tamskPieceAtCenter = mutableListOf<PackedMove>()
  bitboard.getTamskMoves(currentPlayer, tamskPieceAtCenter)

  val (
      bestPiecesToRetrieveCapture1: List<UInt>,
      bestPiecesToRetrieveCapture1Score: Int,
      preMoveNewlyStackedPieces: List<UInt>) =
      if (tamskPieceAtCenter.isEmpty() && turnPhase == null) {
        bestPiecesToRemove(
            currentPlayer,
            opponentPlayer,
            bitboard,
            maxDepth,
            depth,
            alphaBetaScore.deepCopy(),
            "ALPHA-BETA MAIN BEGINNING",
            logger.isDebugEnabled(),
            rng,
            transpositionTable = transpositionTable,
            endTime = endTime,
            searchInfo = searchInfo,
            enableQSearch = enableQSearch,
            enableTranspositionTable = enableTranspositionTable,
            enableKillerMoves = enableKillerMoves,
        )
      } else {
        Triple(emptyList(), 0, emptyList())
      }

  val forcedRemovalScore =
      if (bestPiecesToRetrieveCapture1.isNotEmpty()) bestPiecesToRetrieveCapture1Score else 0

  fun rollbackInitialRemovals() {
    if (bestPiecesToRetrieveCapture1.isNotEmpty()) {
      currentPlayer.uncombinePieces(preMoveNewlyStackedPieces)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture1)

      bitboard.undoRetrieveAndCapturePieces(
          bestPiecesToRetrieveCapture1,
          "ALPHA-BETA MAIN BEGINNING @ depth $depth",
      )
    }

    bitboard.diff(initBitboard)

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
  }

  // region Get Moves
  val availableMoves = mutableListOf<PackedMove>()
  if (turnPhase != TurnPhase.ExtraMove) {
    bitboard.identifyAvailableMoves(currentPlayer, columnInfos, availableMoves)
  } else {
    availableMoves.addAll(tamskPieceAtCenter)
  }
  // endregion

  // region Quiescence Search
  val captureMoves = mutableListOf<PackedMove>()
  bitboard.generateCaptureMoves(currentPlayer, captureMoves)

  if (depth >= maxDepth && captureMoves.isNotEmpty() && enableQSearch) {
    val qMove =
        qSearch(
            turnPhase = TurnPhase.PlayerInputWindow,
            maxDepth = MAX_Q_DEPTH,
            depth = MAX_Q_DEPTH,
            bitboard = bitboard,
            currentPlayer = opponentPlayer,
            opponentPlayer = currentPlayer,
            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
            rng = rng,
            isPVNode = isPVNode,
            transpositionTable = transpositionTable,
            endTime = endTime,
            searchInfo = searchInfo,
            enableTranspositionTable = enableTranspositionTable,
            enableKillerMoves = enableKillerMoves,
        )

    rollbackInitialRemovals()

    return BestPackedMove(
        score = -qMove.score,
    )
  }
  // endregion

  // region Evaluate State
  if (
      availableMoves.isEmpty() ||
          currentPlayer.capturedPieces.count { it.extractPieceType() == PieceType.GIPF } == 3 ||
          opponentPlayer.capturedPieces.count { it.extractPieceType() == PieceType.GIPF } == 3 ||
          depth >= maxDepth ||
          System.currentTimeMillis() >= endTime
  ) {
    // score = evaluate s for original player
    // return [null, score]

    val score = scoreBitboardState(bitboard, currentPlayer, opponentPlayer, rng)

    // d. UNDO the piece removals to evaluate the next choice
    rollbackInitialRemovals()

    return BestPackedMove(
        score = score,
    )
  }

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
  // endregion

  if (searchInfo != null && collectSearchInfo) {
    while (searchInfo.nodesSearched.size <= depth) {
      searchInfo.nodesSearched.add(0.0)
    }
    searchInfo.nodesSearched[depth] += 1.0
    searchInfo.depths.add(depth.toDouble())
    if (System.currentTimeMillis() < endTime) {
      searchInfo.totalActions.add(availableMoves.size.toDouble())
    }
  }

  if (System.currentTimeMillis() >= endTime) {
    return BestPackedMove()
  }

  // region Transposition Table & Move Ordering
  val (ttFound, ttEntry) =
      transpositionTable.probe(
          hash = bitboard.getZobristHash(currentPlayer),
      )

  if (enableTranspositionTable) {
    val ttAlpha = alphaBetaScore.alpha // + forcedRemovalScore
    val ttBeta = alphaBetaScore.beta // + forcedRemovalScore

    if (!isPVNode && ttFound && ttEntry.depth >= depth) {
      if (ttEntry.bound == Bound.EXACT) {
        rollbackInitialRemovals()

        return BestPackedMove(
            score = ttEntry.score,
        )
      }
      if (ttEntry.bound == Bound.BETA && ttEntry.score >= ttBeta) {
        rollbackInitialRemovals()

        return BestPackedMove(
            score = ttEntry.score, // Fail-high
        )
      }
      if (ttEntry.bound == Bound.ALPHA && ttEntry.score <= ttAlpha) {
        rollbackInitialRemovals()

        return BestPackedMove(
            score = ttEntry.score, // Fail-low
        )
      }
    }
  }
  //
  var bound = Bound.ALPHA

  // how to see which move trigger retrieve and capture
  // how to make a move and then assess the state/
  availableMoves.shuffle(rng)

  //  val captureMoves = mutableListOf<PackedMove>()
  //  bitboard.generateCaptureMoves(currentPlayer, captureMoves)

  val pvMove = ttEntry.move != 0u && ttEntry.move.extractPieceColor() == currentPlayer.name

  val killerTableIndex = depth.coerceIn(0, currentPlayer.killerMoves[0].lastIndex)
  val captureTableIndex = depth.coerceIn(0, currentPlayer.captureMoves[0].lastIndex)

  val killer0 = currentPlayer.killerMoves[0][killerTableIndex]
  val killer1 = currentPlayer.killerMoves[1][killerTableIndex]
  val capture0 = currentPlayer.captureMoves[0][captureTableIndex]
  val capture1 = currentPlayer.captureMoves[1][captureTableIndex]

  val ttTargetBit = if (enableTranspositionTable && pvMove) ttEntry.move.extractTargetBit() else 0u // adjust type if needed
  val ttPushDirection = if (enableTranspositionTable && pvMove) ttEntry.move.extractPushDirection() else null
  val ttMoveType = if (enableTranspositionTable && pvMove) ttEntry.move.extractMoveType() else null

  availableMoves.sortByDescending {
    val move = (it as PackedMove.Single).value
    if (pvMove && move == ttEntry.move && enableTranspositionTable) {
      1000000
    } else if (move == killer0 && enableKillerMoves) {
      900000
    } else if (move == killer1 && enableKillerMoves) {
      800000
      //    } else if (captureMoves.isNotEmpty() && enableCaptureMoves) {
      //      if (
      //          captureMoves.any {
      //            val capture = (it as PackedMove.Single).value
      //            capture.extractTargetBit() == move.extractTargetBit() &&
      //                capture.extractPushDirection() == move.extractPushDirection() &&
      //                capture.extractMoveType() == move.extractMoveType()
      //          }
      //      ) {
      //        450000
      //      } else 0
    } else if (
        enableTranspositionTable && pvMove && // todo try target bit and push direction. works for add piece but not for use
            move.extractTargetBit() == ttTargetBit &&
            move.extractPushDirection() == ttPushDirection &&
            move.extractMoveType() == ttMoveType
    ) {
      500000

      //    } else if (move == capture0) {
      //      7
      //    } else if (move == capture1) {
      //      6
      //    } else if (
      //      // todo try target bit and push direction
      //      (move.extractTargetBit() == killer0.extractTargetBit() &&
      //      move.extractPushDirection() == killer0.extractPushDirection() &&
      //      move.extractMoveType() == killer0.extractMoveType())
      //    ) {
      //      4
      //    } else if (
      //    // todo try target bit and push direction
      //      (move.extractTargetBit() == killer1.extractTargetBit() &&
      //          move.extractPushDirection() == killer1.extractPushDirection() &&
      //          move.extractMoveType() == killer1.extractMoveType())
      //    ) {
      //      3
    } else {
      0
    }
  }

  // endregion

  // TODO trying to improve move ordering by searching the smaller subset of UsePotential moves
  // first
  //  try it got stuck on move 23 for at least a few minutes
  //  availableMoves.sortByDescending { (it as PackedMove.Single).value.extractMoveType().ordinal }

  outerLoop@ for ((index, packedMove) in availableMoves.withIndex()) {

    if (Thread.interrupted()) {
      break@outerLoop
    }

    //    val mutableState = state.deepCopy()
    if (logger.isDebugEnabled()) {
      logger.info { "Move: $index" }
      logger.info { "Bitboard State: ${Json.encodeToString(bitboard)}" }
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
                depth + 1,
                alphaBetaScore.deepCopy(),
                "ALPHA-BETA ADD PIECE",
                logger.isDebugEnabled(),
                rng,
                transpositionTable = transpositionTable,
                endTime = endTime,
                searchInfo = searchInfo,
                enableQSearch = enableQSearch,
                enableTranspositionTable = enableTranspositionTable,
                enableKillerMoves = enableKillerMoves,
            )

        bitboard.assertPieceCount(
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        if ((index > 5) && maxDepth >= 2) {
          if (
              lateMoveReductionSearch(
                  packedMove,
                  maxDepth,
                  depth,
                  bitboard,
                  opponentPlayer,
                  currentPlayer,
                  alphaBetaScore,
                  rng,
                  isPVNode,
                  moveValue,
                  ttEntry,
                  transpositionTable,
                  endTime,
                  searchInfo,
                  bestPiecesToRetrieveCapture4,
                  bestPiecesToRetrieveCapture4Score,
                  newlyStackedPieces,
                  unusedTAMSKPotential,
                  enableQSearch = enableQSearch,
                  enableTranspositionTable = enableTranspositionTable,
                  enableKillerMoves = enableKillerMoves,
              )
          )
              continue
        }

        val move =
            BestPackedMove(
                move = packedMove,
                score =
                    -alphaBetaNgMxSearch(
                            maxDepth = maxDepth,
                            depth = depth + 1,
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer,
                            opponentPlayer = currentPlayer,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            rng = rng,
                            isPVNode = isPVNode && moveValue == ttEntry.move,
                            transpositionTable = transpositionTable,
                            endTime = endTime,
                            searchInfo = searchInfo,
                            enableQSearch = enableQSearch,
                            enableTranspositionTable = enableTranspositionTable,
                            enableKillerMoves = enableKillerMoves,
                        )
                        .score,
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

        val compositeScore = move.score
        //            move.score +
        //                if (bestPiecesToRetrieveCapture4.isNotEmpty())
        // bestPiecesToRetrieveCapture4Score
        //                else 0

        if (compositeScore > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = compositeScore
          alphaBetaScore.move = move.move

          bound = Bound.EXACT
        }

        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
          if (enableKillerMoves) currentPlayer.updateKillerMoves(moveValue, depth)

          if (bestPiecesToRetrieveCapture4.isEmpty()) {
            // History heuristic only applies to quiet moves.
            // Add depth squared to emphasize high-depth cutoffs
            currentPlayer.updateHistoryMoves(moveValue, depth)
          }

          bound = Bound.BETA
          break@outerLoop
          //                  return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
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

        //                bitboard.diff(preMoveBitboardState)

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

        val postMoveBitboardState = bitboard.deepCopy()

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

        var extraTamskMoveVacantBitFound: ULong = ULong.MAX_VALUE
        var extraTamskMove: BestPackedMove? = null
        var unusedExtraTAMSKPotential: UInt? = null

        if (
            when (currentPlayer.name) {
              PlayerName.WHITE -> {
                bitboard.whiteTAMSK and bitboard.whitePotentials and boardCenterSpotMask
              }
              PlayerName.BLACK -> {
                bitboard.blackTAMSK and bitboard.blackPotentials and boardCenterSpotMask
              }
            } == boardCenterSpotMask
        ) {
          val (tamskMove1, vacantBitFound1, unusedExtraTAMSKPotential1) =
              applyTamskMove(
                  bitboard,
                  maxDepth,
                  depth,
                  currentPlayer,
                  opponentPlayer,
                  alphaBetaScore,
                  rng,
                  isPVNode,
                  moveValue,
                  ttEntry,
                  transpositionTable,
                  endTime,
                  searchInfo,
                  enableQSearch,
                  enableTranspositionTable = enableTranspositionTable,
                  enableKillerMoves = enableKillerMoves,
              )

          extraTamskMove = tamskMove1
          extraTamskMoveVacantBitFound = vacantBitFound1
          unusedExtraTAMSKPotential = unusedExtraTAMSKPotential1
        }

        val extraMoveBitboard = bitboard.deepCopy()

        // TODO Need to score piece removals
        // TODO enforce PieceRemovalRules & handle intersecting lines
        val (bestPiecesToRetrieveCapture2, bestPiecesToRetrieveCapture2Score, newlyStackedPieces) =
            bestPiecesToRemove(
                currentPlayer,
                opponentPlayer,
                bitboard,
                maxDepth,
                depth + 1,
                alphaBetaScore.deepCopy(),
                "ALPHA-BETA ADD PIECE",
                logger.isDebugEnabled(),
                rng,
                transpositionTable = transpositionTable,
                endTime = endTime,
                searchInfo = searchInfo,
                enableQSearch = enableQSearch,
                enableTranspositionTable = enableTranspositionTable,
                enableKillerMoves = enableKillerMoves,
            )

        bitboard.assertPieceCount(
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        //  TODO Late Move Reduction
        if ((index > 5) && maxDepth >= 2) {
          // todo reduced search
          val move =
              BestPackedMove(
                  move = packedMove,
                  score =
                      -alphaBetaNgMxSearch(
                              turnPhase = TurnPhase.PlayerInputWindow,
                              maxDepth = maxDepth,
                              depth = max(maxDepth, depth + 1),
                              bitboard = bitboard,
                              currentPlayer = opponentPlayer,
                              opponentPlayer = currentPlayer,
                              alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                              rng = rng,
                              isPVNode = isPVNode && moveValue == ttEntry.move,
                              transpositionTable = transpositionTable,
                              endTime = endTime,
                              searchInfo = searchInfo,
                              enableQSearch = enableQSearch,
                              enableTranspositionTable = enableTranspositionTable,
                              enableKillerMoves = enableKillerMoves,
                          )
                          .score,
              )

          val compositeScore = move.score
          //              move.score +
          //                  tamskMoveScore +
          //                  if (bestPiecesToRetrieveCapture2.isNotEmpty())
          // bestPiecesToRetrieveCapture2Score
          //                  else 0

          if (compositeScore < alphaBetaScore.alpha) {
            currentPlayer.uncombinePieces(newlyStackedPieces)

            currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture2)

            bitboard.undoRetrieveAndCapturePieces(
                bestPiecesToRetrieveCapture2,
                "ALPHA-BETA ADD PIECE @ depth $depth",
            )

            if (extraTamskMove?.move != null) {
              val extraTamskMoveValue = (extraTamskMove.move as PackedMove.Single).value

              if (
                  extraTamskMoveValue.extractSourceBit() == boardCenterSpotMask &&
                      extraTamskMoveValue.extractMoveType() == MoveType.AddPiece
              ) {
                bitboard.undoTamskPotential(
                    move = extraTamskMoveValue,
                    vacantBitFound = extraTamskMoveVacantBitFound,
                    wasIndexOccupied = extraTamskMoveVacantBitFound != ULong.MAX_VALUE,
                )
              }

              if (extraTamskMoveValue.extractMoveType() == MoveType.UnusedTamskPotential) {
                bitboard.undoRemoveUnusedTamskPotential(extraTamskMoveValue)
                unusedExtraTAMSKPotential?.let {
                  opponentPlayer.capturedPieces.remove(it)
                }
              }

              bitboard.diff(postMoveBitboardState)
            }

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

              // TODO Add selected piece back to player reserve
              moveValue.onlyPiece().let { currentPlayer.piecesInReserve.add(it) }
            }

            bitboard.assertPieceCount(
                currentPlayer = currentPlayer,
                nextPlayer = opponentPlayer,
            )

            continue
          }
        }

        val move =
            BestPackedMove(
                move = packedMove,
                score =
                    -alphaBetaNgMxSearch(
                            turnPhase = TurnPhase.PlayerInputWindow,
                            maxDepth = maxDepth,
                            depth = depth + 1,
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer,
                            opponentPlayer = currentPlayer,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            rng = rng,
                            isPVNode = isPVNode && moveValue == ttEntry.move,
                            transpositionTable = transpositionTable,
                            endTime = endTime,
                            searchInfo = searchInfo,
                            enableQSearch = enableQSearch,
                            enableTranspositionTable = enableTranspositionTable,
                            enableKillerMoves = enableKillerMoves,
                        )
                        .score,
            )

        if (logger.isDebugEnabled()) {
          logger.info { "--- ALPHA-BETA COMPLETED ---" }
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

        if (extraTamskMove?.move != null) {
          val extraTamskMoveValue = (extraTamskMove.move as PackedMove.Single).value

          if (
              extraTamskMoveValue.extractSourceBit() == boardCenterSpotMask &&
                  extraTamskMoveValue.extractMoveType() == MoveType.AddPiece
          ) {
            bitboard.undoTamskPotential(
                move = extraTamskMoveValue,
                vacantBitFound = extraTamskMoveVacantBitFound,
                wasIndexOccupied = extraTamskMoveVacantBitFound != ULong.MAX_VALUE,
            )
          }

          if (extraTamskMoveValue.extractMoveType() == MoveType.UnusedTamskPotential) {
            bitboard.undoRemoveUnusedTamskPotential(extraTamskMoveValue)
            unusedExtraTAMSKPotential?.let {
              opponentPlayer.capturedPieces.remove(it)
            }
          }

          bitboard.diff(postMoveBitboardState)
        }

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

        val compositeScore = move.score
        //            move.score +
        //                tamskMoveScore +
        //                if (bestPiecesToRetrieveCapture2.isNotEmpty())
        // bestPiecesToRetrieveCapture2Score
        //                else 0

        if (compositeScore > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = compositeScore

          alphaBetaScore.move = move.move

          bound = Bound.EXACT

          //          logger.info { "add piece: ply $depth index $index: set best to:
          // $alphaBetaScore" }
          //          logger.info {
          //            "add piece: ply $depth player: ${currentPlayer.name} index: $index: move:
          // $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
          //          }
        }
        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
          //          logger.info {
          //            "add piece: ply $depth player: ${currentPlayer.name} move $index: return
          // best: $alphaBetaScore"
          //          }

          if (enableKillerMoves) currentPlayer.updateKillerMoves(moveValue, depth)

          if (bestPiecesToRetrieveCapture2.isEmpty()) {
            // History heuristic only applies to quiet moves.
            // Add depth squared to emphasize high-depth cutoffs
            currentPlayer.updateCaptureMoves(moveValue, depth)
          }

          bound = Bound.BETA
          break@outerLoop
          //                  return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
        }
      }
      // endregion

      // region MoveType.UsePotential
      MoveType.UsePotential -> {
        val preUsePotentialBoardState = bitboard.deepCopy()

        bitboard.usePiecePotential(
            move = moveValue,
        )

        val postUsePotentialBitboardState = bitboard.deepCopy()

        // TODO Handle TAMSK Potential
        // TODO Check if there is Tamsk Potential Move
        var extraTamskMoveVacantBitFound: ULong = ULong.MAX_VALUE
        var extraTamskMove: BestPackedMove? = null
        var unusedExtraTAMSKPotential: UInt? = null

        if (
            when (currentPlayer.name) {
              PlayerName.WHITE -> {
                bitboard.whiteTAMSK and bitboard.whitePotentials and boardCenterSpotMask
              }
              PlayerName.BLACK -> {
                bitboard.blackTAMSK and bitboard.blackPotentials and boardCenterSpotMask
              }
            } == boardCenterSpotMask
        ) {
          val (tamskMove1, vacantBitFound1, unusedExtraTAMSKPotential1) =
              applyTamskMove(
                  bitboard,
                  maxDepth,
                  depth,
                  currentPlayer,
                  opponentPlayer,
                  alphaBetaScore,
                  rng,
                  isPVNode,
                  moveValue,
                  ttEntry,
                  transpositionTable,
                  endTime,
                  searchInfo,
                  enableQSearch,
                  enableTranspositionTable = enableTranspositionTable,
                  enableKillerMoves = enableKillerMoves,
              )

          extraTamskMove = tamskMove1
          extraTamskMoveVacantBitFound = vacantBitFound1
          unusedExtraTAMSKPotential = unusedExtraTAMSKPotential1
        }

        val extraMoveBitboard = bitboard.deepCopy()

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        // TODO enforce PieceRemovalRules & handle intersecting lines
        val (bestPiecesToRetrieveCapture3, bestPiecesToRetrieveCapture3Score, newlyStackedPieces) =
            bestPiecesToRemove(
                currentPlayer,
                opponentPlayer,
                bitboard,
                maxDepth,
                depth + 1,
                alphaBetaScore.deepCopy(),
                "ALPHA-BETA USE POTENTIAL",
                logger.isDebugEnabled(),
                rng = rng,
                transpositionTable = transpositionTable,
                endTime = endTime,
                searchInfo = searchInfo,
                enableQSearch = enableQSearch,
                enableTranspositionTable = enableTranspositionTable,
                enableKillerMoves = enableKillerMoves,
            )

        val beforeRecursionBitboardState = bitboard.deepCopy()

        if ((index > 5) && maxDepth >= 2) {
          // todo reduced search
          val move =
              BestPackedMove(
                  move = packedMove,
                  score =
                      -alphaBetaNgMxSearch(
                              turnPhase = TurnPhase.PlayerInputWindow,
                              maxDepth = maxDepth,
                              depth = max(maxDepth, depth + 1),
                              bitboard = bitboard,
                              currentPlayer = opponentPlayer,
                              opponentPlayer = currentPlayer,
                              alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                              rng = rng,
                              isPVNode = isPVNode && moveValue == ttEntry.move,
                              transpositionTable = transpositionTable,
                              endTime = endTime,
                              searchInfo = searchInfo,
                              enableQSearch = enableQSearch,
                              enableTranspositionTable = enableTranspositionTable,
                              enableKillerMoves = enableKillerMoves,
                          )
                          .score,
              )

          val compositeScore = move.score
          //              move.score +
          //                  tamskMoveScore +
          //                  if (bestPiecesToRetrieveCapture3.isNotEmpty())
          // bestPiecesToRetrieveCapture3Score
          //                  else 0

          if (compositeScore < alphaBetaScore.alpha) {

            currentPlayer.uncombinePieces(newlyStackedPieces)
            currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture3)

            bitboard.undoRetrieveAndCapturePieces(
                bestPiecesToRetrieveCapture3,
                "ALPHA-BETA USE POTENTIAL @ depth $depth",
            )

            if (extraTamskMove?.move != null) {
              val extraTamskMoveValue = (extraTamskMove.move as PackedMove.Single).value

              if (
                  extraTamskMoveValue.extractSourceBit() == boardCenterSpotMask &&
                      extraTamskMoveValue.extractMoveType() == MoveType.AddPiece
              ) {
                bitboard.undoTamskPotential(
                    move = extraTamskMoveValue,
                    vacantBitFound = extraTamskMoveVacantBitFound,
                    wasIndexOccupied = extraTamskMoveVacantBitFound != ULong.MAX_VALUE,
                )
              }

              if (extraTamskMoveValue.extractMoveType() == MoveType.UnusedTamskPotential) {
                bitboard.undoRemoveUnusedTamskPotential(extraTamskMoveValue)
                unusedExtraTAMSKPotential?.let {
                  opponentPlayer.capturedPieces.remove(it)
                }
              }

              bitboard.diff(postUsePotentialBitboardState)
            }

            // TODO Undo Move - should this be after undoing piece retrieval/capture
            bitboard.undoUsePiecePotential(moveValue)

            bitboard.assertPieceCount(
                currentPlayer = currentPlayer,
                nextPlayer = opponentPlayer,
            )

            continue
          }
        }

        val move =
            BestPackedMove(
                move = packedMove,
                score =
                    -alphaBetaNgMxSearch(
                            turnPhase = TurnPhase.PlayerInputWindow,
                            maxDepth = maxDepth,
                            depth = depth + 1,
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer,
                            opponentPlayer = currentPlayer,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            rng = rng,
                            isPVNode = isPVNode && moveValue == ttEntry.move,
                            transpositionTable = transpositionTable,
                            endTime = endTime,
                            searchInfo = searchInfo,
                            enableQSearch = enableQSearch,
                            enableTranspositionTable = enableTranspositionTable,
                            enableKillerMoves = enableKillerMoves,
                        )
                        .score,
            )

        if (logger.isDebugEnabled()) {
          logger.info { "--- ALPHA-BETA COMPLETED (POST USE POTENTIAL) ---" }
        }

        bitboard.diff(beforeRecursionBitboardState)

        //        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer =
        // opponentPlayer)

        // d. UNDO the piece removals to evaluate the next choice
        currentPlayer.uncombinePieces(newlyStackedPieces)

        currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture3)

        bitboard.undoRetrieveAndCapturePieces(
            bestPiecesToRetrieveCapture3,
            "ALPHA-BETA USE POTENTIAL @ depth $depth",
        )

        if (extraTamskMove?.move != null) {
          val tamskMoveValue = (extraTamskMove.move as PackedMove.Single).value
          if (
              tamskMoveValue.extractSourceBit() == boardCenterSpotMask &&
                  tamskMoveValue.extractMoveType() == MoveType.AddPiece
          ) {
            bitboard.undoTamskPotential(
                move = tamskMoveValue,
                vacantBitFound = extraTamskMoveVacantBitFound,
                wasIndexOccupied = extraTamskMoveVacantBitFound != ULong.MAX_VALUE,
            )
          }

          if (tamskMoveValue.extractMoveType() == MoveType.UnusedTamskPotential) {
            bitboard.undoRemoveUnusedTamskPotential(tamskMoveValue)
            unusedExtraTAMSKPotential?.let {
              opponentPlayer.capturedPieces.remove(it)
            }
          }

          bitboard.diff(postUsePotentialBitboardState)
        }

        bitboard.diff(postUsePotentialBitboardState)

        bitboard.assertPieceCount(
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        // TODO undo use piece potential
        bitboard.undoUsePiecePotential(moveValue)

        //        val bitboardCopy = bitboard.deepCopy()
        //        bitboardCopy.undoUsePiecePotential(moveValue)
        //        bitboard.undoUsePiecePotential(moveValue)

        //        bitboardCopy.diff(preMoveBitboardState)

        //        bitboard.restorePreviousBoardState(preUsePotentialBoardState)

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        val compositeScore = move.score
        //            move.score +
        //                tamskMoveScore +
        //                if (bestPiecesToRetrieveCapture3.isNotEmpty())
        // bestPiecesToRetrieveCapture3Score
        //                else 0

        if (compositeScore > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = compositeScore
          alphaBetaScore.move = move.move

          bound = Bound.EXACT
        }
        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
          if (enableKillerMoves) currentPlayer.updateKillerMoves(moveValue, depth)

          if (bestPiecesToRetrieveCapture3.isEmpty()) {
            // History heuristic only applies to quiet moves.
            // Add depth squared to emphasize high-depth cutoffs
            currentPlayer.updateCaptureMoves(moveValue, depth)
          }

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

  rollbackInitialRemovals()

  val singleMove = alphaBetaScore.move as? PackedMove.Single

  if (singleMove != null) {
    check(singleMove in availableMoves) {
      "AlphaBeta Search Invariant Violation: Best move extracted (${alphaBetaScore.move}) is not in the set of available moves. " +
          "Available move count: ${availableMoves.size}. Sample legal moves: ${availableMoves.take(3)}"
    }
  }

  alphaBetaScore.alpha += forcedRemovalScore

  if (enableTranspositionTable) {
    alphaBetaScore.move?.let {
      transpositionTable.save(
          entry = ttEntry,
          hash = bitboard.getZobristHash(currentPlayer),
          bound = bound,
          depth = depth,
          move = it,
          value = alphaBetaScore.alpha,
      )
    }
  }

  bitboard.diff(initBitboard)

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  // can't return null even if there are no good moves
  //  if (alphaBetaScore.move == null && depth == maxDepth) {
  //      alphaBetaScore.move = availableMoves.firstOrNull()
  //  }

  return BestPackedMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
}

private fun applyTamskMove(
    bitboard: Bitboard,
    maxDepth: Int,
    depth: Int,
    currentPlayer: Player,
    opponentPlayer: Player,
    alphaBetaScore: AlphaBetaScoreBitPacked,
    rng: Random,
    isPVNode: Boolean,
    moveValue: UInt,
    ttEntry: TransitionTableEntry,
    transpositionTable: TranspositionTable,
    endTime: Long,
    searchInfo: SearchInfo?,
    enableQSearch: Boolean,
    enableTranspositionTable: Boolean,
    enableKillerMoves: Boolean,
): Triple<BestPackedMove, ULong, UInt?> {
  val preTamskMoveBitboardState = bitboard.deepCopy()

  bitboard.diff(preTamskMoveBitboardState)

  val moves = mutableListOf<PackedMove>()
  bitboard.getTamskMoves(currentPlayer, moves)

  if (logger.isDebugEnabled()) {
    logger.info { "--- ALPHA-BETA USE-POTENTIAL CALLED (TAMSK) ---" }
  }

  var bestMove: PackedMove? = null
  var bestScore = -INFINITY

  movesLoop@ for ((index, packedMove) in moves.withIndex()) {

    val moveValue = (packedMove as PackedMove.Single).value

    var extraTamskMoveVacantBitFound: ULong = ULong.MAX_VALUE
    var unusedExtraTAMSKPotential: UInt? = null

    when (moveValue.extractMoveType()) {
      MoveType.UsePotential,
      MoveType.RetrieveCapturePieces -> {}
      MoveType.UnusedTamskPotential -> {
        if (moveValue.extractMoveType() == MoveType.UnusedTamskPotential) {
          unusedExtraTAMSKPotential = bitboard.removeUnusedTamskPotential(currentPlayer)

          // add the unused TAMSK Potential to the opponent's captured pieces.
          unusedExtraTAMSKPotential.let { opponentPlayer.capturedPieces.add(it) }
        }
      }
      MoveType.AddPiece -> {
        extraTamskMoveVacantBitFound = bitboard.useTamskPotential(moveValue)
      }
    }

    val move =
        alphaBetaNgMxSearch(
            maxDepth = maxDepth,
            depth = depth + 1,
            bitboard = bitboard,
            currentPlayer = currentPlayer,
            opponentPlayer = opponentPlayer,
            alphaBetaScore = alphaBetaScore.deepCopy(),
            rng = rng,
            isPVNode = isPVNode && moveValue == ttEntry.move,
            turnPhase = TurnPhase.ExtraMove,
            transpositionTable = transpositionTable,
            endTime = endTime,
            searchInfo = searchInfo,
            enableQSearch = enableQSearch,
            enableTranspositionTable = enableTranspositionTable,
            enableKillerMoves = enableKillerMoves,
        )

    bitboard.assertPieceCount(
        currentPlayer = currentPlayer,
        nextPlayer = opponentPlayer,
    )

    if (
        moveValue.extractSourceBit() == boardCenterSpotMask &&
            moveValue.extractMoveType() == MoveType.AddPiece
    ) {
      bitboard.undoTamskPotential(
          move = moveValue,
          vacantBitFound = extraTamskMoveVacantBitFound,
          wasIndexOccupied = extraTamskMoveVacantBitFound != ULong.MAX_VALUE,
      )
    }

    if (moveValue.extractMoveType() == MoveType.UnusedTamskPotential) {
      bitboard.undoRemoveUnusedTamskPotential(moveValue)
      unusedExtraTAMSKPotential?.let {
        opponentPlayer.capturedPieces.remove(it)
      }
    }

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    bitboard.diff(preTamskMoveBitboardState)

    if (move.score > alphaBetaScore.alpha) {
      alphaBetaScore.alpha = move.score
      bestScore = move.score
      bestMove = packedMove
    }

    if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
      if (logger.isDebugEnabled()) {
        logger.info { "Bitboard State: ${Json.encodeToString(bitboard)}" }
      }
      break@movesLoop
      //            return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
    }

    if (move.score > bestScore) {
      bestScore = move.score
      bestMove = packedMove
    }
  }

  if (logger.isDebugEnabled()) {
    logger.info { "--- ALPHA-BETA COMPLETED (TAMSK) ---" }
  }

  var vacantBitFound = ULong.MAX_VALUE
  var unusedTAMSKPotential: UInt? = null

  if (bestMove != null) {
    val moveValue = (bestMove as PackedMove.Single).value

    if (
        moveValue.extractSourceBit() == boardCenterSpotMask &&
            moveValue.extractMoveType() == MoveType.AddPiece
    ) {
      vacantBitFound = bitboard.useTamskPotential(moveValue)
    }

    if (moveValue.extractMoveType() == MoveType.UnusedTamskPotential) {
      unusedTAMSKPotential = bitboard.removeUnusedTamskPotential(currentPlayer)

      // add the unused TAMSK Potential to the opponent's captured pieces.
      opponentPlayer.capturedPieces.add(unusedTAMSKPotential)
    }
  }

  return Triple(
      BestPackedMove(
          move = bestMove,
          score = bestScore,
      ),
      vacantBitFound,
      unusedTAMSKPotential,
  )
}

private fun lateMoveReductionSearch(
    packedMove: PackedMove.Single,
    maxDepth: Int,
    depth: Int,
    bitboard: Bitboard,
    opponentPlayer: Player,
    currentPlayer: Player,
    alphaBetaScore: AlphaBetaScoreBitPacked,
    rng: Random,
    isPVNode: Boolean,
    moveValue: UInt,
    ttEntry: TransitionTableEntry,
    transpositionTable: TranspositionTable,
    endTime: Long,
    searchInfo: SearchInfo?,
    bestPiecesToRetrieveCapture4: List<UInt>,
    bestPiecesToRetrieveCapture4Score: Int,
    newlyStackedPieces: List<UInt>,
    unusedTAMSKPotential: UInt,
    enableQSearch: Boolean,
    enableTranspositionTable: Boolean,
    enableKillerMoves: Boolean,
): Boolean {
  val move =
      BestPackedMove(
          move = packedMove,
          score =
              -alphaBetaNgMxSearch(
                      maxDepth = maxDepth,
                      depth = max(maxDepth, depth + 1),
                      bitboard = bitboard,
                      currentPlayer = opponentPlayer,
                      opponentPlayer = currentPlayer,
                      alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                      rng = rng,
                      isPVNode = isPVNode && moveValue == ttEntry.move,
                      transpositionTable = transpositionTable,
                      endTime = endTime,
                      searchInfo = searchInfo,
                      enableQSearch = enableQSearch,
                      enableTranspositionTable = enableTranspositionTable,
                      enableKillerMoves = enableKillerMoves,
                  )
                  .score,
      )

  val compositeScore = move.score
  //    move.score +
  //        if (bestPiecesToRetrieveCapture4.isNotEmpty()) bestPiecesToRetrieveCapture4Score
  //        else 0

  if (compositeScore < alphaBetaScore.alpha) {

    currentPlayer.uncombinePieces(newlyStackedPieces)
    currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture4)

    bitboard.undoRetrieveAndCapturePieces(
        bestPiecesToRetrieveCapture4,
        "ALPHA-BETA UNUSED TAMSK POTENTIAL @ depth $depth",
    )

    // TODO Undo Move - should this be after undoing piece retrieval/capture
    bitboard.undoRemoveUnusedTamskPotential(moveValue)
    opponentPlayer.capturedPieces.remove(unusedTAMSKPotential)

    bitboard.assertPieceCount(
        currentPlayer = currentPlayer,
        nextPlayer = opponentPlayer,
    )

    return true
  }
  return false
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
    enableQSearch: Boolean,
    enableTranspositionTable: Boolean,
    enableKillerMoves: Boolean,
): Triple<List<UInt>, Int, List<UInt>> {
  val initBitboard = bitboard.deepCopy()

  if (System.currentTimeMillis() >= endTime) {
    return Triple(emptyList(), 0, emptyList())
  }

  val resolveBoardRemovals =
      resolveBoardRemovals(
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
          enableQSearch = enableQSearch,
          enableTranspositionTable = enableTranspositionTable,
          enableKillerMoves = enableKillerMoves,
      )

  bitboard.diff(initBitboard)

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  if (logger.isDebugEnabled()) {
    logger.info { "--- RESOLVE BOARD REMOVALS COMPLETED ---" }
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

fun resolveBoardRemovals(
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
    enableQSearch: Boolean,
    enableTranspositionTable: Boolean,
    enableKillerMoves: Boolean,
    collectSearchInfo: Boolean = false,
): BestPackedMove {
  if (logger.isDebugEnabled()) {
    logger.info { "--- RESOLVE BOARD REMOVALS CALLED ---" }
    logger.info { "Bitboard: ${Json.encodeToString(bitboard)}" }
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

    if (searchInfo != null && collectSearchInfo) {
      searchInfo.turnPhase.add(TurnPhase.PieceRemoval)
      while (searchInfo.nodesSearched.size <= depth) {
        searchInfo.nodesSearched.add(0.0)
      }
      searchInfo.nodesSearched[depth] += 1.0
      searchInfo.depths.add(depth.toDouble())
      if (System.currentTimeMillis() < endTime) {
        searchInfo.totalActions.add(removePiecesPowerset.size.toDouble())
      }
    }

    removalLoop@ for ((index, removePieces) in removePiecesPowerset.withIndex()) {
      // 1. Generate your powerset of choices for these lines
      // 2. Loop through each choice in the powerset:
      // a. Apply the piece removals to the board

      if (System.currentTimeMillis() >= endTime) {
        break@removalLoop
      }

      /**
       * TODO causes stack overflow error, but an empty list is necessary as a player can leave the
       * stack in play TODO Minimax/MCTS — what it would be like to remove at least one of these
       * pieces if all pieces have potentials use line score heuristic and pieces in reserve TODO
       * return of a list containing different combinations of bit positions
       */
      require(removePieces is PackedMove.Multiple)

      val retrievedCapturedPieces = mutableListOf<UInt>()

      if (logger.isDebugEnabled()) {
        logger.debug { "--- resolveBoardRemovals called removeSelectedPiecesToRemove() ---" }
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

      if (logger.isDebugEnabled()) {
        logger.info { "--- RESOLVE BOARD REMOVALS COMPLETED ---" }
      }

      //      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      // c. Update alpha/beta scores
      val move =
          BestPackedMove(
              move = removePieces,
              score =
                  -alphaBetaNgMxSearch(
                          turnPhase = TurnPhase.PlayerInputWindow,
                          maxDepth = maxDepth,
                          depth = depth,
                          bitboard = bitboard,
                          currentPlayer = opponentPlayer,
                          opponentPlayer = currentPlayer,
                          alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                          rng = rng,
                          transpositionTable = transpositionTable,
                          endTime = endTime,
                          searchInfo = searchInfo,
                          enableQSearch = enableQSearch,
                          enableTranspositionTable = enableTranspositionTable,
                          enableKillerMoves = enableKillerMoves,
                      )
                      .score,
          )

      if (logger.isDebugEnabled()) {
        logger.info { "--- ALPHA-BETA COMPLETED (RESOLVE BOARD REMOVALS POWERSET) ---" }
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
      if (move.score > alphaBetaScore.alpha) {
        alphaBetaScore.alpha = move.score

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
          logger.info { "Bitboard State: ${Json.encodeToString(bitboard)}" }
        }
        break@removalLoop
        //            return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
      }
    }
    // endregion

    bitboard.diff(initBitboard)

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    if (logger.isDebugEnabled()) {
      logger.info { "Bitboard State (NO REMOVALS): ${Json.encodeToString(bitboard)}" }
    }
    // TODO

    return if (alphaBetaScore.move != null) {
      BestPackedMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
    } else {
      // if no move is found take the most pieces
      BestPackedMove(removePiecesPowerset.first(), score = alphaBetaScore.alpha)
    }
  }

  return BestPackedMove(null, score = alphaBetaScore.alpha)
}
