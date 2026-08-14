@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.ai.humanEvaluation

import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.ai.mcts.PackedMove
import org.example.ai.minimax.applyMove
import org.example.ai.minimax.determineNextPhase
import org.example.ai.minimax.qSearch
import org.example.ai.minimax.undoMove
import org.example.ai.scoreBitboardState
import org.example.engine.Bound
import org.example.engine.TranspositionTable
import org.example.engine.getZobristHash
import org.example.model.*

val MAX_HISTORY: Int = 100000
private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

data class BestPackedMove(
    val move: PackedMove? = null,
    var score: Int = 0,
)

@Serializable
data class SearchInfo(
    var depth: Int,
    var nodesSearched: Int = 0,
)

data class AlphaBetaScoreBitPacked(
    var move: PackedMove? = null,
    var alpha: Int = Int.MIN_VALUE,
    var beta: Int = Int.MAX_VALUE,
) {
  fun swapAlphaBeta(): AlphaBetaScoreBitPacked {
    return AlphaBetaScoreBitPacked(
        alpha = beta.unaryMinus(),
        beta = alpha.unaryMinus(),
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
    turnPhase: TurnPhase,
    normalMoveMade: Boolean,
    isPVNode: Boolean = true,
    transpositionTable: TranspositionTable,
    endTime: Long = Long.MAX_VALUE,
    searchInfo: SearchInfo? = null,
): BestPackedMove {
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- ALPHA-BETA CALLED ---") }
    logger.info { "currentPlayer: $currentPlayer" }
    logger.info { "opponentPlayer: $opponentPlayer" }
    logger.info { "depth: $depth" }
    logger.info { "" + ("Bitboard: ${Json.encodeToString(bitboard)}") }
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

  searchInfo?.nodesSearched++

  if (System.currentTimeMillis() >= endTime) {
    val score = scoreBitboardState(bitboard, currentPlayer, opponentPlayer, rng)
    return BestPackedMove(
        score = score,
    )
  }

  // region Get Moves
  val availableMoves = mutableListOf<PackedMove>()
  bitboard.generateMoves(currentPlayer, turnPhase, availableMoves)
  // endregion

  // region Quiescence Search
  val captureMoves = mutableListOf<PackedMove>()
  bitboard.generateCaptureMoves(currentPlayer, captureMoves)

  if (depth <= 0 && captureMoves.isNotEmpty() && maxDepth > 1) {
    val qMove =
        qSearch(
            maxDepth = maxDepth,
            depth = maxDepth,
            bitboard = bitboard,
            currentPlayer = opponentPlayer,
            opponentPlayer = currentPlayer,
            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
            rng = rng,
            turnPhase = turnPhase,
            normalMoveMade = normalMoveMade,
            isPVNode = isPVNode,
            transpositionTable = transpositionTable,
            endTime = endTime,
            searchInfo = searchInfo,
        )

    bitboard.diff(initBitboard)

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    return BestPackedMove(
        score = qMove.score,
    )
  }
  // endregion

  // region Evaluate State
  if (availableMoves.isEmpty() || depth <= 0 || System.currentTimeMillis() >= endTime) {
    // score = evaluate s for original player
    // return [null, score]

    val score = scoreBitboardState(bitboard, currentPlayer, opponentPlayer, rng)

    return BestPackedMove(
        score = score,
    )
  }

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
  // endregion

  // region Transposition Table & Move Ordering
  val (ttFound, ttEntry) =
      transpositionTable.probe(
          hash = bitboard.getZobristHash(currentPlayer),
      )

  if (!isPVNode && ttFound && ttEntry.depth >= (maxDepth - depth)) {
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

  //
  var bound = Bound.ALPHA

  // how to see which move trigger retrieve and capture
  // how to make a move and then assess the state/
  availableMoves.shuffle(rng)

  //  val captureMoves = mutableListOf<PackedMove>()
  //  bitboard.generateCaptureMoves(currentPlayer, captureMoves)

  val pvMove = ttEntry.move != 0u && ttEntry.move.extractPieceColor() == currentPlayer.name

  availableMoves.sortByDescending { action ->
    when (action) {
      is PackedMove.Multiple -> {
        action.values.size
      }
      is PackedMove.Single -> {
        val move = action.value
        val killer0 = currentPlayer.killerMoves[0][depth]
        val killer1 = currentPlayer.killerMoves[1][depth]
        val capture0 = currentPlayer.captureMoves[0][depth]
        val capture1 = currentPlayer.captureMoves[1][depth]

        if (pvMove && move == ttEntry.move) {
          1000000
        } else if (move == killer0) {
          900000
        } else if (move == killer1) {
          800000
        } else if (captureMoves.isNotEmpty()) {
          if (
              captureMoves.any {
                (it as PackedMove.Single).value.extractTargetBit() == move.extractTargetBit()
              } &&
                  captureMoves.any {
                    (it as PackedMove.Single).value.extractPushDirection() ==
                        move.extractPushDirection()
                  } &&
                  captureMoves.any {
                    (it as PackedMove.Single).value.extractMoveType() == move.extractMoveType()
                  }
          ) {
            450000
          } else 0
        } else if (
            pvMove && // todo try target bit and push direction. works for add piece but not for use
                // potential
                ((move.extractTargetBit() == ttEntry.move.extractTargetBit() &&
                    move.extractPushDirection() == ttEntry.move.extractPushDirection() &&
                    move.extractMoveType() == ttEntry.move.extractMoveType()) // ||
                //                (move.extractPieceType() == ttEntry.move.extractPieceType() &&
                //                    move.extractMoveType() == ttEntry.move.extractMoveType())
                )
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
    }
  }

  // endregion

  outerLoop@ for ((index, move) in availableMoves.withIndex()) {
    //    val mutableState = state.deepCopy()
    if (logger.isDebugEnabled()) {
      logger.info { "Move: $index" }
      logger.info { "" + ("Bitboard State: ${Json.encodeToString(bitboard)}") }
    }

    val preMoveBitboardState = bitboard.deepCopy()

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

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
                    alphaBetaNgMxSearch(
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
                    val moveValue = (move as PackedMove.Single).value

                    alphaBetaNgMxSearch(
                            maxDepth = maxDepth,
                            depth = depth,
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer,
                            opponentPlayer = currentPlayer,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            rng = rng,
                            turnPhase = nextPhase,
                            normalMoveMade = nextNormalMoveMade,
                            isPVNode = isPVNode && moveValue == ttEntry.move,
                            transpositionTable = transpositionTable,
                            endTime = endTime,
                            searchInfo = searchInfo,
                        )
                        .score
                  }
                  else -> {
                    // Turn genuinely complete: real ply boundary.
                    -alphaBetaNgMxSearch(
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
                },
        )

    undoMove(bitboard, currentPlayer, opponentPlayer, move, undoInfo)

    bitboard.diff(initBitboard)

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

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

  val singleMove = alphaBetaScore.move as? PackedMove.Single

  if (singleMove != null) {
    check(singleMove in availableMoves) {
      "AlphaBeta Search Invariant Violation: Best move extracted (${alphaBetaScore.move}) is not in the set of available moves. " +
          "Available move count: ${availableMoves.size}. Sample legal moves: ${availableMoves.take(3)}"
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

fun resolveBoardRemovals(
    maxDepth: Int,
    currentPlayer: Player,
    opponentPlayer: Player,
    bitboard: Bitboard,
    depth: Int,
    alphaBetaScore: AlphaBetaScoreBitPacked,
    rng: Random,
    turnPhase: TurnPhase = TurnPhase.PieceRemoval,
    normalMoveMade: Boolean,
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
                  alphaBetaNgMxSearch(
                          maxDepth = maxDepth,
                          depth = depth.minus(1),
                          bitboard = bitboard,
                          currentPlayer = opponentPlayer,
                          opponentPlayer = currentPlayer,
                          alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                          rng = rng,
                          turnPhase = turnPhase,
                          normalMoveMade = normalMoveMade,
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
