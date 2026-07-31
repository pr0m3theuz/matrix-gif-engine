@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.ai.humanEvaluation

import kotlinx.serialization.json.Json
import org.example.ai.mcts.PackedMove
import org.example.ai.scoreBitboardState
import org.example.model.*
import kotlin.random.Random

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

data class BestPackedMove(
    val move: PackedMove? = null,
    val score: Float = 0f,
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

fun alphaBetaPackedMove(
    depth: Int = 3,
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    alphaBetaScore: AlphaBetaScoreBitPacked,
    rng: Random,
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
  val isTamskPieceAtCenter = mutableListOf<PackedMove>()
  bitboard.getTamskMoves(currentPlayer, isTamskPieceAtCenter)

  val (bestPiecesToRetrieveCapture1, bestPiecesToRetrieveCapture1Score, preMoveNewlyStackedPieces) =
      if (isTamskPieceAtCenter.isEmpty()) {
        bestPiecesToRemove(
            currentPlayer,
            opponentPlayer,
            bitboard,
            depth,
            alphaBetaScore,
            initBitboard,
            logger.isDebugEnabled(),
            rng,
        )
      } else {
        Triple(emptyList(), 0f, emptyList())
      }

  val availableMoves = mutableListOf<PackedMove>()
  bitboard.identifyAvailableMoves(currentPlayer, columnInfos, availableMoves)

  if (availableMoves.isEmpty() || depth <= 0) {
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

    return BestPackedMove(
        score = scoreBitboardState(bitboard, currentPlayer, opponentPlayer, rng).toFloat()
    )
  }

  // how to see which move trigger retrieve and capture
  // how to make a move and then assess the state/
  outerLoop@ for ((index, packedMove) in availableMoves.withIndex()) {
    //    val mutableState = state.deepCopy()
    if (logger.isDebugEnabled()) {
      logger.info { "" + ("Move: $index") }
      logger.info { "" + ("Bitboard State: ${Json.encodeToString(bitboard)}") }
    }

    val preMoveBitboardState = bitboard.deepCopy()

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

        bitboard.diff(preMoveBitboardState)

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        //        moveValue.columnInfos.forEachIndexed { index, columnInfo ->
        //          listOf(
        //                  columnInfo.positions.first() to columnInfo.pushDirections.first,
        //                  columnInfo.positions.last() to columnInfo.pushDirections.second,
        //              )
        //              .forEach { (addAtIndex, pushDirection) ->

        val addAtIndex = moveValue.extractTargetBit()
        val pushDirection = moveValue.extractPushDirection()
        val columnInfo = moveValue.extractColumnInfo()

        requireNotNull(addAtIndex)
        requireNotNull(pushDirection)
        requireNotNull(columnInfo)

        if (logger.isDebugEnabled()) {
          logger.info { "" + ("""
									Move: $index
									Piece: ${moveValue.extractPiece()}
									Piece Type: ${moveValue.extractPieceType()}
									Push direction: $pushDirection
									Add At Index: $addAtIndex
									Pre Move Bitboard State: ${Json.encodeToString(bitboard)}
								"""
                  .trimIndent()) }
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
          logger.info { "" + ("""
									Move: $index
									Piece: ${moveValue.extractPiece()}
									Piece Type: ${moveValue.extractPieceType()}
									Push direction: $pushDirection
									Add At Index: $addAtIndex
									Post Move Bitboard State: ${Json.encodeToString(bitboard)}
								"""
                  .trimIndent()) }
        }

        bitboard.assertPieceCount(
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        var tamskMoveScore: Float = 0f
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
                      alphaBetaScore = alphaBetaScore.copy(move = null),
                      rng = rng,
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
                alphaBetaScore,
                postMoveBitboardState,
                logger.isDebugEnabled(),
                rng,
            )

        val move =
            BestPackedMove(
                move = packedMove,
                score =
                    alphaBetaPackedMove(
                            depth = depth.minus(1),
                            bitboard = bitboard,
                            currentPlayer = opponentPlayer,
                            opponentPlayer = currentPlayer,
                            //					              gameTree = gameTree,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            rng = rng,
                        )
                        .score +
                        tamskMoveScore +
                        if (bestPiecesToRetrieveCapture2.isNotEmpty())
                            bestPiecesToRetrieveCapture2Score
                        else 0f,
            )

        if (moveValue == 20463525u && depth == 3) {
          logger.info { "" + ("WTF!") }
        }

        if (logger.isDebugEnabled()) {
          logger.info { "" + ("--- ALPHA-BETA COMPLETED ---") }
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

        currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture2)

        bitboard.undoRetrieveAndCapturePieces(bestPiecesToRetrieveCapture2)

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

        if (moveValue.extractSourceBit() == boardCenterSpotMask) {
          bitboard.undoTamskPotential(
              move = moveValue,
              vacantBitFound = vacantBitFound,
              wasIndexOccupied = vacantBitFound != 0UL,
          )
        } else {

          bitboard.undoAddPieceToBitboard(
              move = moveValue,
              vacantBitFound = vacantBitFound,
              wasIndexOccupied = vacantBitFound != 0UL,
          )

          bitboard.diff(preMoveBitboardState)

          // TODO Add selected piece back to player reserve
          moveValue.onlyPiece().let { currentPlayer.piecesInReserve.add(it) }
        }

        bitboard.diff(preMoveBitboardState)

        bitboard.assertPieceCount(
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        if (move.score.unaryMinus() > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = move.score.unaryMinus()

          alphaBetaScore.move = move.move
          //		              gameTree[gameStateHash] = alphaBetaScore

          //                  logger.info { "" + ("line 371: ply $depth move $index: set best to:
          // $alphaBetaScore") }
          //                  logger.info { "" + (//                      "line 373: ply $depth player: ${currentPlayer.name} index:
          // $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
          //) }
        }
        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
          //                  logger.info { "" + (//                      "line 378: ply $depth player: ${currentPlayer.name} move
          // $index: return best: $alphaBetaScore"
          //) }

          break@outerLoop
          //                  return BestBitMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
        }
      }
      // endregion

      // region MoveType.UsePotential
      MoveType.UsePotential -> {
        bitboard.usePiecePotential(
            move = moveValue,
            currentPlayer = currentPlayer,
            nextPlayer = opponentPlayer,
        )

        val postUsePotentialBitboardState = bitboard.deepCopy()

        // TODO Handle TAMSK Potential
        // TODO Check if there is Tamsk Potential Move
        var tamskMoveScore: Float = 0f
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
                      alphaBetaScore = alphaBetaScore.copy(move = null),
                      rng = rng,
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
                alphaBetaScore,
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
                            currentPlayer = opponentPlayer,
                            opponentPlayer = currentPlayer,
                            //					              gameTree = gameTree,
                            alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                            rng = rng,
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

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        // d. UNDO the piece removals to evaluate the next choice
        currentPlayer.uncombinePieces(newlyStackedPieces)

        currentPlayer.removeRetrievedCapturedPieces(bestPiecesToRetrieveCapture3)

        bitboard.undoRetrieveAndCapturePieces(bestPiecesToRetrieveCapture3)

        bitboard.diff(postUsePotentialBitboardState)

        bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        // TODO undo use piece potential
        bitboard.undoUsePiecePotential(moveValue)

        bitboard.diff(preMoveBitboardState)
        bitboard.deepCopy().assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

        if (move.score.unaryMinus() > alphaBetaScore.alpha) {
          alphaBetaScore.alpha = move.score.unaryMinus()
          alphaBetaScore.move = move.move
          //          gameTree[gameStateHash] = alphaBetaScore

          //          logger.info { "" + ("line 430: ply $depth move ${index}: set best to: $alphaBetaScore") }
          //          logger.info { "" + (//              "line 432: ply $depth player: ${currentPlayer.name} index: ${index}:
          // move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
          //) }
        }
        if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
          //          logger.info { "" + (//              "line 437: ply $depth player: ${currentPlayer.name} move $index:
          // return best: $alphaBetaScore"
          //) }
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

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

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

  if (alphaBetaScore.move?.let { (it as PackedMove.Single).value == 20463525u} == true && depth == 3) {
    logger.info { "" + ("WTF!") }
  }

  return BestPackedMove(alphaBetaScore.move, score = alphaBetaScore.alpha)
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
          currentPlayer = currentPlayer,
          opponentPlayer = opponentPlayer,
          bitboard = bitboard,
          depth = depth,
          alphaBetaScore = alphaBetaScore.copy(move = null),
          rng = rng,
      )

  bitboard.diff(postUsePotentialBitboardState)

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
          piecesToRemove = removePieces.values,
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

  val removePiecesPowerset = mutableListOf<PackedMove>()
  bitboard.identifyPiecesToRemove(currentPlayer, removePiecesPowerset)

  // region Retrieve/Capture Pieces with Player Potential Powerset
  if (removePiecesPowerset.isNotEmpty()) {
    removePiecesPowerset.forEachIndexed { index, removePieces ->
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
      val playerPiecesWithPotentialToRemove = removePieces.values

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

      bitboard.diff(postPieceRemovalBitboardState)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      if (logger.isDebugEnabled()) {
        logger.info { "" + ("--- RESOLVE BOARD REMOVALS COMPLETED ---") }
      }

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      // c. Update alpha/beta scores
      val move =
          BestPackedMove(
              move = removePieces,
              score =
                  alphaBetaPackedMove(
                          depth = depth.minus(1),
                          // state = state,
                          bitboard = bitboard,
                          currentPlayer = opponentPlayer,
                          opponentPlayer = currentPlayer,
                          // gameTree = gameTree,
                          alphaBetaScore = alphaBetaScore.swapAlphaBeta(),
                          rng = rng,
                      )
                      .score,
          )

      if (logger.isDebugEnabled()) {
        logger.info { "" + ("--- ALPHA-BETA COMPLETED (RESOLVE BOARD REMOVALS POWERSET) ---") }
      }

      bitboard.diff(postPieceRemovalBitboardState)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      // d. UNDO the piece removals to evaluate the next choice
      currentPlayer.uncombinePieces(newlyStackedPieces)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      currentPlayer.removeRetrievedCapturedPieces(retrievedCapturedPieces)

      bitboard.undoRetrieveAndCapturePieces(retrievedCapturedPieces)
      //      bitboard.undoRetrieveAndCapturePieces(piecesWithPotentialPowerset)

      bitboard.diff(initBitboard)

      bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

      // c. Update alpha/beta scores
      if (move.score.unaryMinus() > alphaBetaScore.alpha) {
        alphaBetaScore.alpha = move.score.unaryMinus()

        alphaBetaScore.move = move.move
        //								              gameTree[gameStateHash] = alphaBetaScore

        //            logger.info { "" + ("line 371: ply $depth move $index: set best to: $alphaBetaScore") }
        //            logger.info { "" + (//                "line 373: ply $depth player: ${currentPlayer.name} index:
        // $index: move: $move, score: ${move.score},  alphaBetaScore: $alphaBetaScore"
        //) }
      }

      if (alphaBetaScore.alpha >= alphaBetaScore.beta) {
        //            logger.info { "" + (//                "line 378: ply $depth player: ${currentPlayer.name} move $index:
        // return best: $alphaBetaScore"
        //) }

        if (logger.isDebugEnabled()) {
          logger.info { "" + ("Bitboard State: ${Json.encodeToString(bitboard)}") }
        }
        //            break@removalLoop
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
