@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.ai.humanEvaluation

import kotlin.random.Random
import kotlinx.serialization.json.Json
import org.example.ai.mcts.PackedMove
import org.example.ai.scoreBitboardState
import org.example.engine.Bound
import org.example.engine.getZobristHash
import org.example.engine.transpositionTable
import org.example.model.*

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

data class BestPackedMove(
    val move: PackedMove? = null,
    var score: Float = 0f,
)

data class AlphaBetaScoreBitPacked(
    var move: PackedMove? = null,
    var alpha: Float = Float.NEGATIVE_INFINITY,
    var beta: Float = Float.POSITIVE_INFINITY,
) {
  fun swapAlphaBeta(): AlphaBetaScoreBitPacked {
    return AlphaBetaScoreBitPacked(
        alpha = beta.unaryMinus(),
        beta = alpha.unaryMinus(),
    )
  }
}

// TODO Implement Move Ordering
fun alphaBetaPackedMove(
    depth: Int = 3,
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    alphaBetaScore: AlphaBetaScoreBitPacked,
    rng: Random,
    turnPhase: TurnPhase? = null,
    isPVNode: Boolean = true,
): BestPackedMove {
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- ALPHA-BETA CALLED ---") }
    logger.info { "" + ("currentPlayer: ${currentPlayer}") }
    logger.info { "" + ("opponentPlayer: $opponentPlayer") }
    logger.info { "" + ("depth: ${depth}") }
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
      bestPiecesToRetrieveCapture1Score: Float,
      preMoveNewlyStackedPieces: List<UInt>) =
      if (tamskPieceAtCenter.isEmpty() && turnPhase == null) {
        bestPiecesToRemove(
            currentPlayer,
            opponentPlayer,
            bitboard,
            depth,
            AlphaBetaScoreBitPacked(),
            initBitboard,
            logger.isDebugEnabled(),
            rng,
        )
      } else {
        Triple(emptyList(), 0f, emptyList())
      }

  val availableMoves = mutableListOf<PackedMove>()
  if (turnPhase != TurnPhase.ExtraMove) {
    bitboard.identifyAvailableMoves(currentPlayer, columnInfos, availableMoves)
  } else {
    availableMoves.addAll(tamskPieceAtCenter)
  }

  if ((availableMoves.isEmpty()) || depth <= 0) {
    // score = evaluate s for original player
    // return [null, score]

    // d. UNDO the piece removals to evaluate the next choice
    if (bestPiecesToRetrieveCapture1.isNotEmpty()) {
      currentPlayer.uncombinePieces(preMoveNewlyStackedPieces)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture1)

      bitboard.undoRetrieveAndCapturePieces(bestPiecesToRetrieveCapture1)
    }

    bitboard.diff(initBitboard)

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    val score = scoreBitboardState(bitboard, currentPlayer, opponentPlayer, rng).toFloat()

    return BestPackedMove(
        score = score,
    )
  }

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  val (ttFound, ttEntry) =
      transpositionTable.probe(
          hash = bitboard.getZobristHash(currentPlayer),
      )

  if (!isPVNode && ttFound && ttEntry.depth <= depth) {
    if (ttEntry.bound == Bound.EXACT) {
      return BestPackedMove(
          score = ttEntry.score.toFloat(),
      )
    }
    if (ttEntry.bound == Bound.BETA && ttEntry.score >= alphaBetaScore.beta.toInt()) {
      return BestPackedMove(
          score = ttEntry.score.toFloat(), // Fail-high
      )
    }
    if (ttEntry.bound == Bound.ALPHA && ttEntry.score >= alphaBetaScore.alpha.toInt()) {
      return BestPackedMove(
          score = ttEntry.score.toFloat(), // Fail-low
      )
    }
  }

  //
  var bound = Bound.ALPHA

  // how to see which move trigger retrieve and capture
  // how to make a move and then assess the state/
  availableMoves.shuffle(rng)

  if (ttEntry.move != 0u && ttEntry.move.extractPieceColor() == currentPlayer.name) {
    availableMoves.sortByDescending {
      if (
          ((it as PackedMove.Single).value.extractPieceType() == ttEntry.move.extractPieceType() &&
              it.value.extractMoveType() == ttEntry.move.extractMoveType()) ||
              it.value == ttEntry.move
      )
          1
      else 0
    }
  }
  // TODO trying to improve move ordering by searching the smaller subset of UsePotential moves
  // first
  //  try it got stuck on move 23 for at least a few minutes
  //  availableMoves.sortByDescending { (it as PackedMove.Single).value.extractMoveType().ordinal }

  outerLoop@ for ((index, packedMove) in availableMoves.withIndex()) {
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
      // region MoveType.AddPiece
      MoveType.AddPiece -> {
        // for each selectable dot, add piece, push piece, assess resulting state, score it
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

              selectedPiece?.let {
                bitboard.addPieceToBitboard(moveValue)
              }
            }

        check(vacantBitFound != null) {
          "Invalid board state: No vacant bit found for piece deployment."
        }

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

        var tamskMoveScore = 0f
        val isTamskPieceAtCenter = mutableListOf<PackedMove>()
        bitboard.getTamskMoves(currentPlayer, isTamskPieceAtCenter)
        if (isTamskPieceAtCenter.isNotEmpty()) {
          val preTamskMoveBitboardState = bitboard.deepCopy()

          bitboard.diff(preTamskMoveBitboardState)

          if (logger.isDebugEnabled()) {
            logger.info { "" + ("--- ALPHA-BETA CALLED (TAMSK) ---") }
          }
          tamskMoveScore =
              alphaBetaPackedMove(
                      depth = depth,
                      bitboard = bitboard,
                      currentPlayer = currentPlayer,
                      opponentPlayer = opponentPlayer,
                      // gameTree = gameTree,
                      alphaBetaScore = AlphaBetaScoreBitPacked(),
                      rng = rng,
                      isPVNode = isPVNode && packedMove == availableMoves[0],
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
                depth,
                AlphaBetaScoreBitPacked(),
                postMoveBitboardState,
                logger.isDebugEnabled(),
                rng,
            )

        bitboard.assertPieceCount(
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        val move =
            BestPackedMove(
                move = packedMove,
                score =
                    alphaBetaPackedMove(
                            depth = depth.minus(1),
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer.deepCopy(),
                            opponentPlayer = currentPlayer.deepCopy(),
                            //					              gameTree = gameTree,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            rng = rng,
                            isPVNode = isPVNode && packedMove == availableMoves[0],
                        )
                        .score +
                        tamskMoveScore +
                        if (bestPiecesToRetrieveCapture2.isNotEmpty())
                            bestPiecesToRetrieveCapture2Score
                        else 0f,
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

        //        bitboard.undoRetrieveAndCapturePieces(bestPiecesToRetrieveCapture2)

        //        bitboard.diff(postMoveBitboardState)

        //        bitboard.assertPieceCount(
        //            currentPlayer = currentPlayer,
        //            nextPlayer = opponentPlayer,
        //        )

        //	              val gameStateHash =
        //		              MessageDigest.getInstance("MD5")
        //			              .digest(state.toString().toByteArray())
        //			              .toHexString()

        // TODO Undo Move - should this be after undoing piece retrieval/capture

        if (moveValue.extractSourceBit() == boardCenterSpotMask) {
          //          bitboard.undoTamskPotential(
          //              move = moveValue,
          //              vacantBitFound = vacantBitFound,
          //              wasIndexOccupied = vacantBitFound != 0UL,
          //          )
        } else {

          //          bitboard.undoAddPieceToBitboard(
          //              move = moveValue,
          //              vacantBitFound = vacantBitFound,
          //              wasIndexOccupied = vacantBitFound != 0UL,
          //          )

          //          bitboard.diff(preMoveBitboardState)

          // TODO Add selected piece back to player reserve
          moveValue.onlyPiece().let { currentPlayer.piecesInReserve.add(it) }
        }

        //        bitboard.diff(preMoveBitboardState)

        preMoveBitboardState.assertPieceCount(
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        bitboard.restorePreviousBoardState(preMoveBitboardState)

        bitboard.assertPieceCount(
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        if (move.score.unaryMinus() > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = move.score.unaryMinus()

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
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        val postUsePotentialBitboardState = bitboard.deepCopy()

        // TODO Handle TAMSK Potential
        // TODO Check if there is Tamsk Potential Move
        var tamskMoveScore = 0f
        val isTamskPieceAtCenter = mutableListOf<PackedMove>()
        bitboard.getTamskMoves(currentPlayer, isTamskPieceAtCenter)
        if (isTamskPieceAtCenter.isNotEmpty()) {
          val preTamskMoveBitboardState = bitboard.deepCopy()

          bitboard.diff(preTamskMoveBitboardState)

          if (logger.isDebugEnabled()) {
            logger.info { "" + ("--- ALPHA-BETA CALLED (TAMSK) ---") }
          }
          tamskMoveScore =
              alphaBetaPackedMove(
                      depth = depth,
                      bitboard = bitboard,
                      currentPlayer = currentPlayer,
                      opponentPlayer = opponentPlayer,
                      //					              gameTree = gameTree,
                      alphaBetaScore = AlphaBetaScoreBitPacked(),
                      rng = rng,
                      isPVNode = isPVNode && packedMove == availableMoves[0],
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
                depth,
                AlphaBetaScoreBitPacked(),
                postUsePotentialBitboardState,
                logger.isDebugEnabled(),
                rng = rng,
            )

        val move =
            BestPackedMove(
                move = packedMove,
                score =
                    alphaBetaPackedMove(
                            depth = depth.minus(1),
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer.deepCopy(),
                            opponentPlayer = currentPlayer.deepCopy(),
                            //					              gameTree = gameTree,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            rng = rng,
                            isPVNode = isPVNode && packedMove == availableMoves[0],
                        )
                        .score +
                        tamskMoveScore +
                        if (bestPiecesToRetrieveCapture3.isNotEmpty())
                            bestPiecesToRetrieveCapture3Score
                        else 0f,
            )

        if (logger.isDebugEnabled()) {
          logger.info { "" + ("--- ALPHA-BETA COMPLETED (POST USE POTENTIAL) ---") }
        }

//        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        // d. UNDO the piece removals to evaluate the next choice
        currentPlayer.uncombinePieces(newlyStackedPieces)

        currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture3)

        //        bitboard.undoRetrieveAndCapturePieces(bestPiecesToRetrieveCapture3)

        //        bitboard.diff(postUsePotentialBitboardState)

        //        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer =
        // opponentPlayer)

        // TODO undo use piece potential

        //        val bitboardCopy = bitboard.deepCopy()
        //        bitboardCopy.undoUsePiecePotential(moveValue)
        //        bitboard.undoUsePiecePotential(moveValue)

        //        bitboardCopy.diff(preMoveBitboardState)

        bitboard.restorePreviousBoardState(preUsePotentialBoardState)

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        if (move.score.unaryMinus() > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = move.score.unaryMinus()
          alphaBetaScore.move = move.move

          bound = Bound.EXACT

          //          logger.info { "use potential: ply $depth index $index: set best to:
          // $alphaBetaScore" }
          //          logger.info {
          //            "use potential: ply $depth player: ${currentPlayer.name} index: $index:
          // move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
          //          }
        }
        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
          //          logger.info {
          //            "use potential: ply $depth player: ${currentPlayer.name} move $index: return
          // best: $alphaBetaScore"
          //          }
          bound = Bound.BETA
          break@outerLoop
          //          require(alphaBetaScore.move != null)
          //          return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
        }
      }
      // endregion
      // region MoveType.RetrieveCapturePieces
      MoveType.RetrieveCapturePieces -> {}

    // endregion
    }
  }

  //  logger.info { "" + ("line 446: ply $depth return best: $alphaBetaScore") }
  //  logger.info { "" + ("line 447: ply $depth moves") }

  // TODO undo retrieval and capture
  // TODO Actually retrieveAndCapturePieces using move.retrievedCapturedPiecesBit list
  // region Retrieve & Capture Pieces clean up
  if (bestPiecesToRetrieveCapture1.isNotEmpty()) {
    currentPlayer.uncombinePieces(preMoveNewlyStackedPieces)

    //    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture1)

    bitboard.undoRetrieveAndCapturePieces(bestPiecesToRetrieveCapture1)
  }

  bitboard.diff(initBitboard)

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
  // endregion
  // TODO figure out
  // --- POST-SEARCH VALIDATION ---
  /*		check(possibleBitMoves.contains(alphaBetaScore.move)) {
  	buildString {
  		appendLine("CRITICAL AI ERROR: Alpha-Beta search returned an illegal or ungenerated move.")
  		appendLine("--- ILLEGAL MOVE REPORT ---")

  		val badMove = alphaBetaScore.move
  		if (badMove == null) {
  			appendLine("  -> Move is NULL. (Did the search exhaust without finding any valid leaf?)")
  		} else {
  			appendLine("  -> Piece: ${badMove.pieceColor} ${badMove.pieceType}")
  			appendLine("  -> Move Type: ${badMove.moveType}")

  			// Format the bits safely if they exist
  			val sourceStr = badMove.sourceBit?.toString(2)?.padStart(40, '0') ?: "NULL"
  			val targetStr = badMove.targetBit?.toString(2)?.padStart(40, '0') ?: "NULL"

  			appendLine("  -> Source Mask: 0b$sourceStr")
  			appendLine("  -> Target Mask: 0b$targetStr")
  			appendLine("  -> Push Dir:    ${badMove.pushDirection}")
  		}

  		appendLine("---------------------------")
  		appendLine("Context: The generator provided ${possibleBitMoves.size} legal moves for this ply.")
  		appendLine("Hint: Check your leaf node evaluation, transposition table reads, or ensure that placeholder moves (like empty initial best moves) are not bleeding up the search tree.")
  	}
  }*/

  val singleMove = alphaBetaScore.move as? PackedMove.Single

  if (singleMove != null) {
    check(singleMove in availableMoves) {
      "AlphaBeta Search Invariant Violation: Best move extracted (${alphaBetaScore.move}) is not in the set of available moves. " +
          "Available move count: ${availableMoves.size}. Sample legal moves: ${availableMoves.take(3)}"
    }
  }

  val score =
      alphaBetaScore.alpha +
          if (bestPiecesToRetrieveCapture1.isNotEmpty()) bestPiecesToRetrieveCapture1Score else 0f

  alphaBetaScore.move?.let {
    transpositionTable.save(
        entry = ttEntry,
        hash = bitboard.getZobristHash(currentPlayer),
        bound = bound,
        depth = depth,
        move = it,
        value = score.toInt(),
    )
  }

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  return BestPackedMove(alphaBetaScore.move, score = score)
}

private fun bestPiecesToRemove(
    currentPlayer: Player,
    opponentPlayer: Player,
    bitboard: Bitboard,
    depth: Int,
    alphaBetaScore: AlphaBetaScoreBitPacked,
    postUsePotentialBitboardState: Bitboard,
    isDebugEnabled: Boolean,
    rng: Random,
): Triple<List<UInt>, Float, List<UInt>> {
  val resolveBoardRemovals =
      resolveBoardRemovals(
          currentPlayer = currentPlayer.deepCopy(),
          opponentPlayer = opponentPlayer.deepCopy(),
          bitboard = bitboard.deepCopy(),
          depth = depth,
          alphaBetaScore = alphaBetaScore.copy(move = null),
          rng = rng,
      )

  //  bitboard.diff(postUsePotentialBitboardState)

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

      bitboard.removeSelectedPiecesToRemove(
          player = currentPlayer,
          piecesToRemove = removePieces.values.distinct(),
          movesBuffer = move,
      )

      currentPlayer.addRetrievedCapturedPieces(move)

      // TODO uncombine pieces
      newlyStackedPieces = currentPlayer.combinePieces()

      // TODO Actually retrieveAndCapturePieces using retrievedCapturedPiecesBit
      // list
      //      bitboard.removeRetrieveAndCapturePiecesFromBitboard(move)

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
    currentPlayer: Player,
    opponentPlayer: Player,
    bitboard: Bitboard,
    depth: Int,
    alphaBetaScore: AlphaBetaScoreBitPacked,
    rng: Random,
): BestPackedMove {
  if (logger.isDebugEnabled()) {
    logger.info { "" + ("--- RESOLVE BOARD REMOVALS CALLED ---") }
    logger.info { "" + ("Bitboard: ${Json.encodeToString(bitboard)}") }
    logger.info { "" + ("currentPlayer: ${currentPlayer}") }
    logger.info { "" + ("opponentPlayer: ${opponentPlayer}") }
    logger.info { "" + ("depth: ${depth}") }
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
      val playerPiecesWithPotentialToRemove = removePieces.values.distinct()

      val retrievedCapturedPieces = mutableListOf<UInt>()
      bitboard.removeSelectedPiecesToRemove(
          player = currentPlayer,
          piecesToRemove = playerPiecesWithPotentialToRemove,
          movesBuffer = retrievedCapturedPieces,
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

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      if (logger.isDebugEnabled()) {
        logger.info { "" + ("--- RESOLVE BOARD REMOVALS COMPLETED ---") }
      }

      //      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      // c. Update alpha/beta scores
      val move =
          BestPackedMove(
              move = removePieces,
              score =
                  alphaBetaPackedMove(
                          depth = depth.minus(1),
                          // state = state,
                          bitboard = bitboard.deepCopy(),
                          currentPlayer = opponentPlayer.deepCopy(),
                          opponentPlayer = currentPlayer.deepCopy(),
                          // gameTree = gameTree,
                          alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                          rng = rng,
                      )
                      .score,
          )

      if (logger.isDebugEnabled()) {
        logger.info { "" + ("--- ALPHA-BETA COMPLETED (RESOLVE BOARD REMOVALS POWERSET) ---") }
      }

      //      bitboard.diff(postPieceRemovalBitboardState)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      // d. UNDO the piece removals to evaluate the next choice
      currentPlayer.uncombinePieces(newlyStackedPieces)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      currentPlayer.removeRetrievedCapturedPieces(retrievedCapturedPieces)

      bitboard.undoRetrieveAndCapturePieces(retrievedCapturedPieces)
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

  //  bitboard.diff(initBitboard)

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  if (logger.isDebugEnabled()) {
    logger.info { "" + ("Bitboard State (NO REMOVALS): ${Json.encodeToString(bitboard)}") }
  }
  // TODO
  return BestPackedMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
}
