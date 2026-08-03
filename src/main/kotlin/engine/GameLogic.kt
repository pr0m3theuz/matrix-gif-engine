package org.example.engine

import io.github.oshai.kotlinlogging.KotlinLogging
import jdk.internal.foreign.abi.Binding
import kotlin.random.Random
import kotlinx.serialization.json.Json
import org.example.ai.mcts.PackedMove
import org.example.ai.mcts.encode
import org.example.model.*

private val logger = KotlinLogging.logger {}

fun playerTurn(state: State, turn: Int, rng: Random): State {

  var newState = state // .deepCopy(copyCollector = true)

  //  newState.board.printHexGrid("START OF TURN")

  /**
   * TODO Which one takes precedence at the beginning of a turn: (a) TAMSK extra move or (b) piece
   * removals A regular move and an extra move are considered one single turn, whether the extra
   * move is made after or before the regular move. The position of the pieces between the two moves
   * is regarded as an “interim” situation. This means that no pieces may be removed or captured in
   * between the regular move and the extra move. The same goes for situations where you succeed in
   * pushing a second or third TAMSK-stack onto the central spot during one and the same turn.
   */
  // TODO While there are pieces to remove
  //  TODO Has a bug
  while (newState.bitboard.evaluateLinesForFourInARow(state.currentPlayer).isNotEmpty()) {
    newState = playerMove(newState, turnPhase = TurnPhase.PieceRemoval, turn, rng)

    // recombine player pieces
    newState.currentPlayer.combinePieces()
  }

  newState.assertPieceCount()

  // Handle Tamsk Potential
  while (
      when (newState.currentPlayer.name) {
        PlayerName.WHITE ->
            newState.bitboard.whiteTAMSK and
                newState.bitboard.whitePotentials and
                boardCenterSpotMask
        PlayerName.BLACK ->
            newState.bitboard.blackTAMSK and
                newState.bitboard.blackPotentials and
                boardCenterSpotMask
      } == boardCenterSpotMask
  ) {
    newState = playerMove(newState, TurnPhase.ExtraMove, turn, rng)
  }

  newState.assertPieceCount()


    val availableMoves = mutableListOf<PackedMove>()
    newState.bitboard.identifyAvailableMoves(newState.currentPlayer, columnInfos, availableMoves)

    if (availableMoves.isNotEmpty()) {
        newState = playerMove(newState, turnPhase = TurnPhase.PlayerInputWindow, turn, rng)

        newState.assertPieceCount()
    }


  // Handle Tamsk Potential
  while (
      when (newState.currentPlayer.name) {
        PlayerName.WHITE ->
            newState.bitboard.whiteTAMSK and
                newState.bitboard.whitePotentials and
                boardCenterSpotMask
        PlayerName.BLACK ->
            newState.bitboard.blackTAMSK and
                newState.bitboard.blackPotentials and
                boardCenterSpotMask
      } == boardCenterSpotMask
  ) {
    newState = playerMove(newState, turnPhase = TurnPhase.ExtraMove, turn, rng)
  }

  newState.assertPieceCount()

  // TODO While there are pieces to remove
  //  TODO Has a bug
  while (newState.bitboard.evaluateLinesForFourInARow(state.currentPlayer).isNotEmpty()) {
    newState = playerMove(newState, turnPhase = TurnPhase.PieceRemoval, turn, rng)
    // recombine player pieces
    newState.currentPlayer.combinePieces()
  }

  newState.assertPieceCount()

  //  newState.board.printHexGrid("END OF TURN")

  return newState
}

fun playerMove(state: State, turnPhase: TurnPhase, turn: Int, rng: Random): State {

  val bitboard = state.bitboard.deepCopy()

  val packedMove: PackedMove? =
      state.currentPlayer.selectMove(
          turnPhase = turnPhase,
          bitboard = bitboard.deepCopy(),
          opponent = state.nextPlayer.deepCopy(),
          rng = rng,
      )

  //      when (turnPhase) {
  //        TurnPhase.PieceRemoval ->
  //            resolveBoardRemovals(
  //                    currentPlayer = state.currentPlayer.deepCopy(),
  //                    opponentPlayer = state.nextPlayer.deepCopy(),
  //                    bitboard = bitboard.deepCopy(),
  //                    depth = 3,
  //                    alphaBetaScore = AlphaBetaScoreBitPacked(),
  //                    rng = rng,
  //                )
  //                .move
  //        else ->
  //            alphaBetaPackedMove(
  //                    depth = 3,
  //                    bitboard = bitboard.deepCopy(),
  //                    currentPlayer = state.currentPlayer.deepCopy(),
  //                    opponentPlayer = state.nextPlayer.deepCopy(),
  //                    alphaBetaScore = AlphaBetaScoreBitPacked(),
  //                    rng = rng,
  //                )
  //                .move
  //      }

  // TODO use agent to selectMove
  // TODO handle bestMove when selectMoveMCTS returns null. it is a pass? how to record
  //    val packedMove: PackedMove? =
  //        selectMoveMCTS(
  //            bitboard = bitboard,
  //            currentPlayer = state.currentPlayer,
  //            nextPlayer = state.nextPlayer,
  //            turnPhase = turnPhase,
  //            rounds = 0..999,
  //            rng = rng,
  //        )

  if (packedMove != null) {
    // logger.info { "" + ("Player Move: ${Json.encodeToString(bestMove)}") }

    state.currentPlayer.collector?.recordDecision(
        state = state.encodeState(),
        action = packedMove.encode(),
        globalState = state.encodeGlobalState(turnPhase = turnPhase),
    )

    state.collector?.recordDecision(
        state = state.encodeState(),
        action = packedMove.encode(),
        globalState = state.encodeGlobalState(turnPhase = turnPhase),
    )

    state.turnMoves.getOrDefault(turn, mutableListOf()).add(packedMove)

    when (packedMove) {
      is PackedMove.Multiple -> {
        val retrievedCapturedPieces = mutableListOf<UInt>()
        bitboard.removeSelectedPiecesToRemove(
            player = state.currentPlayer,
            piecesToRemove = packedMove.values.distinct(),
            retrievedCapturedPieces,
        )

        state.currentPlayer.addRetrievedCapturedPieces(retrievedCapturedPieces)

        state.currentPlayer.combinePieces()
      }

      is PackedMove.Single -> {
        val bestMove = packedMove.value

        when (bestMove.extractMoveType()) {
          MoveType.AddPiece -> {
            val extractedPiece = bestMove.onlyPiece()

            if (bestMove.extractSourceBit() == boardCenterSpotMask) {
              require(bestMove.extractSourceBit() == boardCenterSpotMask) {
                "CRITICAL MOVE ERROR: bestMove.sourceBit cannot be null. A valid move must have an origin."
              }
              bitboard.useTamskPotential(bestMove)
            } else {
              //        val node = state.board.nodes.first { it.bitmask == bestMove.targetBit }
              //        node.piece = bestMove.piece?.let { state.currentPlayer.selectPiece(it) }

              val selectedPiece = extractedPiece.let { state.currentPlayer.selectPiece(it) }

              require(extractedPiece.extractPieceColor() == state.currentPlayer.name) {
                "Must be the current player's piece!"
              }

              // --- MOVE VALIDATION ---
              //            requireNotNull(bestMove.extractTargetBit()) {
              //              "CRITICAL MOVE ERROR: bestMove.targetBit cannot be null. A valid move
              // must have a destination."
              //            }
              //            requireNotNull(bestMove.extractPushDirection()) {
              //              "CRITICAL MOVE ERROR: bestMove.pushDirection cannot be null. A valid
              // move must define the resulting board shift."
              //            }
              //            requireNotNull(bestMove.extractColumnInfo()) {
              //              "CRITICAL MOVE ERROR: bestMove.columnInfos cannot be empty. No valid
              // board columns were provided for this move."
              //            }

              selectedPiece.let {
                bitboard.addPieceToBitboard(bestMove)
              }
            }
          }

          MoveType.UsePotential -> {
            requireNotNull(bestMove.extractSourceBit()) {
              "CRITICAL MOVE ERROR: Source bit cannot be null. A valid move must have an origin. $bestMove"
            }
            requireNotNull(bestMove.extractTargetBit()) {
              "CRITICAL MOVE ERROR: Target bit cannot be null. A valid move must have a target. $bestMove"
            }

            bitboard.usePiecePotential(
                move = bestMove,
                currentPlayer = state.currentPlayer,
                nextPlayer = state.nextPlayer,
            )
          }

          MoveType.RetrieveCapturePieces -> {}
        }
      }
    }

    bitboard.assertPieceCount(
        currentPlayer = state.currentPlayer,
        nextPlayer = state.nextPlayer,
    )

    val newBoard = bitboard.convertBitboardToBoard(state.board)

    val newState =
        State(
            currentPlayer = state.currentPlayer,
            nextPlayer = state.nextPlayer,
            board = newBoard,
            bitboard = bitboard,
            turnMoves = state.turnMoves,
            collector = state.collector,
        )

    newState.assertPieceCount(bitboard = bitboard)

    return newState
  } else {
    //    check(true) {"CRITICAL MOVE ERROR: No move made."}
  }

  return state
}

fun selectDot(
    selectableDots: Set<NodeConnections>,
    //    linesWithSpace: List<Set<Node>>,
): NodeConnections {
  // TODO Iterable each selectable dot
  // TODO This is random
  return selectableDots.random()
}

fun selectPushDirection(
    availablePushDirections: List<PushDirection>,
): PushDirection {
  // TODO Iterable each selectable dot
  // TODO This is random
  return availablePushDirections.random()
}

fun selectPieceFromReserve(pieces: List<Piece>): Piece? {
  if (pieces.isEmpty()) return null
  return pieces.random()
}

fun chooseToRemovePiecesWithPotential(line: Set<Node>, player: Player): Boolean {
  return line.filter { it.piece?.colorName == player.name }.all { it.piece?.potential == true }
}

fun evaluateCapturedPieces(state: State): Boolean {
  return state.currentPlayer.capturedPieces.count { piece ->
    piece.extractPieceType() == PieceType.GIPF
  } == 3 ||
      state.nextPlayer.capturedPieces.count { piece ->
        piece.extractPieceType() == PieceType.GIPF
      } == 3
}

fun determineWinner(
    currentPlayer: Player,
    nextPlayer: Player,
    playerWhoMadeTheLastMove: Player?,
    bitboard: Bitboard? = null,
    state: State? = null,
    printStatement: Boolean = false,
): Player? {
  // TODO refactor & test

  val capturedGIPFPieces =
      when {
        currentPlayer.capturedPieces.count { piece ->
          piece.extractPieceType() == PieceType.GIPF
        } == 3 &&
            nextPlayer.capturedPieces.count { piece ->
              piece.extractPieceType() == PieceType.GIPF
            } == 3 -> null
        currentPlayer.capturedPieces.count { piece ->
          piece.extractPieceType() == PieceType.GIPF
        } == 3 -> currentPlayer
        nextPlayer.capturedPieces.count { piece -> piece.extractPieceType() == PieceType.GIPF } ==
            3 -> nextPlayer
        else -> null
      }

  val bitboardHasAvailableMoves = bitboard?.let { it ->
    val currentPlayerMoves = mutableListOf<PackedMove>()
    val nextPlayerMoves = mutableListOf<PackedMove>()

    it.identifyAvailableMoves(currentPlayer, movesBuffer = currentPlayerMoves)
    it.identifyAvailableMoves(nextPlayer, movesBuffer = nextPlayerMoves)

    when {
      currentPlayerMoves.isNotEmpty() && nextPlayerMoves.isNotEmpty() -> null
      currentPlayerMoves.isNotEmpty() -> currentPlayer
      nextPlayerMoves.isNotEmpty() -> nextPlayer
      else -> null
    }
  }

  if (printStatement) {
    capturedGIPFPieces?.let {
      logger.info { "" + ("Captured GIPF Pieces: ${Json.encodeToString(it)}") }
    }
    bitboardHasAvailableMoves?.let {
      logger.info { "" + ("Has Available Moves (Bitboard): ${Json.encodeToString(it)}") }
    }
    playerWhoMadeTheLastMove?.let {
      logger.info { "" + ("Player Made The Last Move: ${Json.encodeToString(it)}") }
    }
  }

  // TODO should number of pieces captured be a win condition
  return capturedGIPFPieces ?: bitboardHasAvailableMoves ?: playerWhoMadeTheLastMove
}
