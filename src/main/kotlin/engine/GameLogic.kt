package org.example.engine

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
    val columnInfos: List<ColumnInfo> = emptyList(),
    val retrievedCapturedPiecesBit: List<RetrievedCapturedPieceBit> = emptyList(),
    val sourceBit: ULong? = null,
    val targetBit: ULong? = null,
    val pieceType: PieceType? = null,
    val pieceColor: PlayerName? = null,
    val pushDirection: PushDirection? = null,
    val moveType: MoveType,
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

fun playerTurn(state: State, turn: Int): State {

  var newState =
      state
          .deepCopy()
          .copy(
              collector = state.collector,
          )

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
    newState = playerMove(newState, turnPhase = TurnPhase.PieceRemoval, turn)

    // recombine player pieces
    newState.currentPlayer.combinePieces()
  }

  newState.assertPieceCount()

  // Handle Tamsk Potential
  while (isTamskPieceAtCenter(newState.board, newState.currentPlayer)) {
    newState = playerMove(newState, TurnPhase.ExtraMove, turn)
  }

  newState.assertPieceCount()

  newState = playerMove(newState, turnPhase = TurnPhase.PlayerInputWindow, turn)

  newState.assertPieceCount()

  // Handle Tamsk Potential
  while (isTamskPieceAtCenter(newState.board, newState.currentPlayer)) {
    newState = playerMove(newState, turnPhase = TurnPhase.ExtraMove, turn)
  }

  newState.assertPieceCount()

  // TODO While there are pieces to remove
  //  TODO Has a bug
  while (newState.bitboard.evaluateLinesForFourInARow(state.currentPlayer).isNotEmpty()) {
    newState = playerMove(newState, turnPhase = TurnPhase.PieceRemoval, turn)
    // recombine player pieces
    newState.currentPlayer.combinePieces()
  }

  newState.assertPieceCount()

//  newState.board.printHexGrid("END OF TURN")

  return newState
}

fun playerMove(state: State, turnPhase: TurnPhase, turn: Int): State {

  val savedBoardState = state.board.deepCopy()
  val savedLines = state.lines.deepCopy()

  //	val gameTree: HashMap<String, AlphaBetaScore> = hashMapOf()

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
      )

  if (bestMove != null) {
    // println("Player Move: ${Json.encodeToString(bestMove)}")

    state.currentPlayer.collector.recordDecision(
        state.encodeState(),
        bestMove.encode(),
    )

    state.collector.recordDecision(
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

        val node = state.board.nodes.first { it.bitmask == bestMove.targetBit }

        node.piece = bestMove.piece?.let { state.currentPlayer.selectPiece(it) }

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

        if (bestMove.pieceType != PieceType.TAMSK) {
          requireNotNull(bestMove.piece) {
            "CRITICAL MOVE ERROR: bestMove.piece cannot be null. A valid move must have a piece."
          }

          bitboard.addPieceToBitboard(
              addAtIndex = bestMove.targetBit,
              pushDirection = bestMove.pushDirection,
              col = bestMove.columnInfos.first(),
              piece = bestMove.piece,
          )
        } else {
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

    val newBoard = bitboard.convertBitboardToBoard(savedBoardState)

    val newState =
        state
            .deepCopy()
            .copy(
                board = newBoard,
                bitboard = bitboard,
                lines = constructLines(newBoard.nodes),
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

fun enforcePieceRemovalRules(state: State): State {

  state.assertPieceCount()

  val newState = state.deepCopy()

  // TODO "Implement logic for instances where all of the current player's pieces have their
  // potential == true"
  /*
  TODO Confirm – Don't reset piece potential when captured/retrieved if it's potential == false &&
     that there's only one piece of that type that has been retrieved
  */
  /* TODO Implement logic for the below
     Additionally, if you're able to make a double stack of the same type you should,
      otherwise a single token will stay in your reserve but not be viable to be placed onto the board until a matching token is placed on top
  */

  // 5/ It does not matter which player causes a row-of-4: it
  // is always the color of the pieces that determines the
  // owner of the row. If you make a row in your own
  // color, you must (at least partially) remove it right
  // after completing your move. If you complete a row
  // with opposing pieces, the opponent must deal with
  // it before making their move. (See illustration 4:
  // White forces Black to remove the row of 5 black
  // pieces [at least partially] from the board.)

  // Dump Current State for debugging
  println("State: ${Json.encodeToString(newState)}")

  // TODO Check if the lines intersect and is a stack
  val linesWithFourPiecesInARow =
      newState.lines
          .toList()
          .map { line: MutableList<Set<Node>> ->
            evaluateLines(player = newState.currentPlayer, lines = line)
          }
          .filter { (hasFourPiecesInARow, _) -> hasFourPiecesInARow }

  if (linesWithFourPiecesInARow.isEmpty()) {
    return state
  }

  val retrievedCapturedPieces: List<RetrievedCapturedPieceNode> =
      if (linesWithFourPiecesInARow.size == 1) {
        linesWithFourPiecesInARow
            .map { (_, line) ->
              retrieveAndCapturePieces(
                  player = state.currentPlayer,
                  line = line,
                  removePiecesWithPotential =
                      chooseToRemovePiecesWithPotential(
                          line,
                          newState.currentPlayer,
                      ), // TODO Result not used
              )
            }
            .flatten()
            .distinctBy { it.node?.coordinate }
      } else {
        /**
         * 6/ It will occur that more than one row-of-4 of the same color are lined up at the same
         * time. If these rows do not intersect each other, then they are removed (at least
         * partially) following the standard procedure. If they do intersect, the player playing
         * that color may choose which row they will deal with first. If they remove the piece on
         * the intersecting spot, the second row is broken up and the remaining pieces of that row
         * stay on the board. If it is a stack and it is left on the intersecting spot, then the
         * second row is still intact, which means that it must also be dealt with. (See
         * illustration 5: Black may choose between fi rst dealing with the row of 4 pieces or the
         * row of 5 pieces. If the piece on the intersecting spot is a stack and Black removes it
         * from the board, then the other row in not complete anymore; if Black leaves the stack on
         * the board, they must also deal with that other row.)
         */
        val intersectingCoordinates: Set<Coordinate> =
            linesWithFourPiecesInARow
                .flatMap { it.second }
                .groupingBy { it.coordinate }
                .eachCount()
                .filter { (_, count: Int) -> count > 1 }
                .keys

        val intersectingLines: List<Set<Node>> =
            linesWithFourPiecesInARow
                .map { it.second }
                .filter { line -> line.any { node -> node.coordinate in intersectingCoordinates } }

        val nonIntersectingLines =
            linesWithFourPiecesInARow
                .map { it.second }
                .filter { line -> line !in intersectingLines }

        // TODO Add intersectingNodes to chooseToRemovePiecesWithPotential()
        // TODO create function that decides whether To Remove Pieces With Potential or not

        (intersectingLines.map { line ->
              retrieveAndCapturePieces(
                  player = state.currentPlayer,
                  line = line,
                  removePiecesWithPotential = false,
              )
            } +
                nonIntersectingLines.map { line ->
                  retrieveAndCapturePieces(
                      player = state.currentPlayer,
                      line = line,
                      removePiecesWithPotential = false,
                  )
                })
            .flatten()
            .distinctBy { it.node?.coordinate }
      }

  //
  val retrievedPieces = retrievedCapturedPieces.mapNotNull { it.retrievedPiece }
  //				.fold(initial = mutableListOf<Piece>()) { acc, line ->
  //        (acc + line.retrievedPiece).toMutableList()
  //      }
  val capturedPieces = retrievedCapturedPieces.mapNotNull { it.capturedPiece }

  //				.fold(initial = mutableListOf<Piece>()) { acc, line ->
  //        (acc + line.captured).toMutableList()
  //      }
  //  val retrievedCapturedNodes =
  //      retrievedCapturedPieces.fold(initial = mutableSetOf<Node>()) { acc, line ->
  //        acc.addAll(line.nodes)
  //        acc
  //      }

  newState.currentPlayer.addPiecesToReserve(retrievedPieces)
  newState.currentPlayer.addCapturedPieces(capturedPieces)

  val newPiecesInReserve = newState.currentPlayer.piecesInReserve.size
  val oldPiecesInReserve = state.currentPlayer.piecesInReserve.size

  if (newPiecesInReserve > oldPiecesInReserve) {
    check(newPiecesInReserve > oldPiecesInReserve) {
      "Invalid state transition: Current player's reserve pieces did not increase. " +
          "Before: $oldPiecesInReserve, After: $newPiecesInReserve" +
          "${
				retrievedPieces.forEach { piece ->
					piece.toString().plus("\n")
				}
			}"
    }
  }

  val newCapturedPieces = newState.currentPlayer.capturedPieces.size
  val oldCapturedPieces = state.currentPlayer.capturedPieces.size

  check(newCapturedPieces >= oldCapturedPieces) {
    "Invalid state transition: Current player's captured pieces count did not increase. " +
        "Before: $oldCapturedPieces, After: $newCapturedPieces"
  }

  val board = state.board.removePieces(retrievedCapturedPieces.distinctBy { it.node?.coordinate })

  val newNewState =
      State(
          currentPlayer = newState.currentPlayer,
          nextPlayer = newState.nextPlayer,
          board = board,
          bitboard = convertBoardToBitboard(board),
          lines = constructLines(board.nodes),
      )

  newNewState.assertPieceCount()

  return newNewState
}

fun chooseToRemovePiecesWithPotential(line: Set<Node>, player: Player): Boolean {
  return line.filter { it.piece?.colorName == player.name.name }.all { it.piece?.potential == true }
}

fun evaluateCapturedPieces(state: State): Boolean {
  return state.currentPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3 ||
      state.nextPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3
}

fun evaluatePiecesInReserve(state: State): Boolean {
  return identifyAvailableMoves(state).isEmpty() // for the currentPlayer
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
    when {
      it.identifyAvailableMoves(currentPlayer).isNotEmpty() &&
          it.identifyAvailableMoves(nextPlayer).isNotEmpty() -> null
      it.identifyAvailableMoves(currentPlayer).isNotEmpty() -> currentPlayer
      it.identifyAvailableMoves(nextPlayer).isNotEmpty() -> nextPlayer
      else -> null
    }
  }

  val stateHasAvailableMoves = state?.let { it ->
    when {
      identifyAvailableMoves(it).isNotEmpty() &&
          identifyNextPlayerAvailableMoves(it).isNotEmpty() -> null
      identifyAvailableMoves(it).isNotEmpty() -> currentPlayer
      identifyNextPlayerAvailableMoves(it).isNotEmpty() -> nextPlayer
      else -> null
    }
  }

  if (printStatement) {
    capturedGIPFPieces?.let { println("Captured GIPF Pieces: $it") }
    bitboardHasAvailableMoves?.let { println("Has Available Moves (Bitboard): $it") }
    stateHasAvailableMoves?.let { println("Has Available Moves (State): $it") }
    playerWhoMadeTheLastMove?.let { println("Player Made The Last Move: $it") }
  }

  // TODO should number of pieces captured be a win condition
  return capturedGIPFPieces
      ?: bitboardHasAvailableMoves
      ?: stateHasAvailableMoves
      ?: playerWhoMadeTheLastMove
}
