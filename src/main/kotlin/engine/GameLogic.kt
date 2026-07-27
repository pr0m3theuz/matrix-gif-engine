package org.example.engine

import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.ai.mcts.selectMoveMCTS
import org.example.model.*
import org.example.toBitList
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray

/**
 * TODO create a two functions:
 * 1. Add new pieces to the board
 * 2. Using piece potentials
 */

// TODO Create  LIST OF ALL POSSIBLE MOVES

@Serializable
data class PossibleMove(
    val piece: Piece? = null,
    val selectableDots: Set<NodeConnections> = emptySet(),
    val selectedBit: ULong = 0UL,
    val pushDirection: PushDirection? = null,
    val eligiblePotentialPieceNode: Node? = null,
    val eligiblePotentialTargetNodes: Set<Node> = emptySet(),
    val moveType: MoveType,
)

@Serializable
data class PossibleBitMove(
    val piece: Piece? = null,
    val sourceBit: ULong? = null,
    val targetBit: ULong? = null,
    val pieceType: PieceType? = null,
    val pieceColor: PlayerName? = null,
    val pushDirection: PushDirection? = null,
    val moveType: MoveType,
    val retrievedCapturedPiecesBit: List<RetrievedCapturedPieceBit> = emptyList(),
    val columnInfos: List<ColumnInfo> = emptyList(),
) {
  fun encode(): NDArray<Int, D1> {
    return when (moveType) {
      MoveType.AddPiece,
      MoveType.UsePotential -> {
        requireNotNull(targetBit) { "Target bit must not be null" }
        targetBit.toBitList()
      }
      MoveType.RetrieveCapturePieces -> {
        retrievedCapturedPiecesBit
            .fold(0UL) { acc, bit ->
              acc or bit.bitmask
            }
            .toBitList()
      }
    }
  }
}

enum class MoveType {
  AddPiece,
  UsePotential,
  RetrieveCapturePieces,
}

enum class TurnPhase {
  ExtraMove,
  PlayerInputWindow,
  PieceRemoval,
}

enum class TurnLifecycle {
  Initialization, // Engine handles upkeep, updates active player index
  PreExecutionCheck, // Engine evaluates board for 4-in-a-row BEFORE input
  InputWindow, // Engine accepts Player Actions (Free, Normal, Potential)
  PostExecutionCheck, // Engine evaluates board for 4-in-a-row AFTER input
  Termination, // Engine serializes state changes, prepares for next turn switch
}

fun playerTurn(state: State, turn: Int, rng: Random): State {

  var newState = state //.deepCopy(copyCollector = true)

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
  while (isTamskPieceAtCenter(newState.board, newState.currentPlayer)) {
    newState = playerMove(newState, TurnPhase.ExtraMove, turn, rng)
  }

  newState.assertPieceCount()

  newState = playerMove(newState, turnPhase = TurnPhase.PlayerInputWindow, turn, rng)

  newState.assertPieceCount()

  // Handle Tamsk Potential
  while (isTamskPieceAtCenter(newState.board, newState.currentPlayer)) {
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

  /* var bestMove =
  alphabetaBitboardAddPieces(
          depth = 3,
          bitboard = bitboard,
          currentPlayer = state.currentPlayer,
          opponentPlayer = state.nextPlayer,
          alphaBetaScore = AlphaBetaScoreBit(),
      )
      .move*/

  // TODO use agent to selectMove
  // TODO handle bestMove when selectMoveMCTS returns null. it is a pass? how to record
  val bestMove: PossibleBitMove? =
      selectMoveMCTS(
          bitboard = bitboard,
          currentPlayer = state.currentPlayer,
          nextPlayer = state.nextPlayer,
          turnPhase = turnPhase,
          rounds = 0..999,
          rng = rng,
      )

  if (bestMove != null) {
    // println("Player Move: ${Json.encodeToString(bestMove)}")

    state.currentPlayer.collector?.recordDecision(
        state.encodeState(),
        bestMove.encode(),
    )

    state.collector?.recordDecision(
        state.encodeState(),
        bestMove.encode(),
    )

    when (bestMove.moveType) {
      MoveType.AddPiece -> {

        state.turnMoves.getOrDefault(turn, mutableListOf()).add(bestMove)

        require(
            bestMove.piece?.colorName == state.currentPlayer.name.name ||
                bestMove.pieceColor == state.currentPlayer.name
        ) {
          "Must be the current player's piece!"
        }

        //        val node = state.board.nodes.first { it.bitmask == bestMove.targetBit }

        //        node.piece = bestMove.piece?.let { state.currentPlayer.selectPiece(it) }
        val selectedPiece = bestMove.piece?.let { state.currentPlayer.selectPiece(it) }

        // --- MOVE VALIDATION ---
        requireNotNull(bestMove.targetBit) {
          "CRITICAL MOVE ERROR: bestMove.targetBit cannot be null. A valid move must have a destination."
        }
        requireNotNull(bestMove.pushDirection) {
          "CRITICAL MOVE ERROR: bestMove.pushDirection cannot be null. A valid move must define the resulting board shift."
        }
        require(bestMove.columnInfos.isNotEmpty()) {
          "CRITICAL MOVE ERROR: bestMove.columnInfos cannot be empty. No valid board columns were provided for this move."
        }

        when (bestMove.sourceBit) {
          null -> {
            requireNotNull(bestMove.piece) {
              "CRITICAL MOVE ERROR: bestMove.piece cannot be null. A valid move must have a piece."
            }

            bitboard.addPieceToBitboard(
                addAtIndex = bestMove.targetBit,
                pushDirection = bestMove.pushDirection,
                col = bestMove.columnInfos.first(),
                piece = bestMove.piece,
            )
          }

          boardCenterSpotMask -> {
            requireNotNull(bestMove.sourceBit) {
              "CRITICAL MOVE ERROR: bestMove.sourceBit cannot be null. A valid move must have an origin."
            }

            bitboard.useTamskPotential(
                player = state.currentPlayer,
                sourceIndex = bestMove.sourceBit,
                targetIndex = bestMove.targetBit,
                col = bestMove.columnInfos.first(),
                pushDirection = bestMove.pushDirection,
            )
          }
        }
      }

      MoveType.UsePotential -> {
        requireNotNull(bestMove.sourceBit) {
          "CRITICAL MOVE ERROR: Source bit cannot be null. A valid move must have an origin. $bestMove"
        }
        requireNotNull(bestMove.targetBit) {
          "CRITICAL MOVE ERROR: Target bit cannot be null. A valid move must have a target. $bestMove"
        }

        state.turnMoves.getOrDefault(turn, mutableListOf()).add(bestMove)

        bitboard.usePiecePotential(
            possibleBitMove = bestMove,
            currentPlayer = state.currentPlayer,
            nextPlayer = state.nextPlayer,
        )
      }

      MoveType.RetrieveCapturePieces -> {
        val retrievedCapturedPieces =
            bitboard.removeSelectedPiecesToRemove(
                player = state.currentPlayer,
                piecesToRemove = bestMove.retrievedCapturedPiecesBit,
            )

        state.turnMoves
            .getOrDefault(turn, mutableListOf())
            .add(bestMove.copy(retrievedCapturedPiecesBit = retrievedCapturedPieces))

        val retrievedPieces = retrievedCapturedPieces.mapNotNull {
          it.retrievedPiece
        }
        val capturedPieces = retrievedCapturedPieces.mapNotNull {
          it.capturedPiece
        }

        state.currentPlayer.addPiecesToReserve(retrievedPieces)
        state.currentPlayer.addCapturedPieces(capturedPieces)

        state.currentPlayer.combinePieces()
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
  return line.filter { it.piece?.colorName == player.name.name }.all { it.piece?.potential == true }
}

fun evaluateCapturedPieces(state: State): Boolean {
  return state.currentPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3 ||
      state.nextPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3
}

fun isTamskPieceAtCenter(
    board: Board,
    player: Player,
): Boolean {
  val piece: Piece? =
      board.nodes
          .first {
            it.coordinate.column == board.centerNodeCoordinate.column &&
                it.coordinate.row == board.centerNodeCoordinate.row
          }
          .piece

  if (piece == null) return false

  // Is the piece a TAMSK piece && the current player's piece && potential == true
  return piece.type == PieceType.TAMSK && piece.colorName == player.name.name && piece.potential
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
        currentPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3 &&
            nextPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3 -> null
        currentPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3 ->
            currentPlayer
        nextPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3 -> nextPlayer
        else -> null
      }

  val bitboardHasAvailableMoves = bitboard?.let { it ->
    val currentPlayerMoves = mutableListOf<PossibleBitMove>()
    val nextPlayerMoves = mutableListOf<PossibleBitMove>()

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
    capturedGIPFPieces?.let { println("Captured GIPF Pieces: ${Json.encodeToString(it)}") }
    bitboardHasAvailableMoves?.let { println("Has Available Moves (Bitboard): ${Json.encodeToString(it)}") }
    playerWhoMadeTheLastMove?.let { println("Player Made The Last Move: ${Json.encodeToString(it)}") }
  }

  // TODO should number of pieces captured be a win condition
  return capturedGIPFPieces ?: bitboardHasAvailableMoves ?: playerWhoMadeTheLastMove
}
