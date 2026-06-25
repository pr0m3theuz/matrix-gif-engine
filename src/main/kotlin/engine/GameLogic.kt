package org.example.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.ai.humanEvaluation.AlphaBetaScore
import org.example.ai.humanEvaluation.AlphaBetaScoreBit
import org.example.ai.humanEvaluation.alphabetaAddPieces
import org.example.ai.humanEvaluation.alphabetaBitboardAddPieces
import org.example.model.*
import kotlin.collections.HashMap

fun playerTurn(state: State): State {
	// TODO Replace with bitboard equivalent
  var newState = enforcePieceRemovalRules(state)

  // recombine player pieces
  newState.currentPlayer.combinePieces()

  state.assertPieceCount()

  // Handle Tamsk Potential
  if (isTamskPieceAtCenter(newState.board, newState.currentPlayer)) {
    newState = playerMove(newState) // ?: return null
  }

  newState.assertPieceCount()

  // TODO if gipf, gipf
  //  if tamsk, tamsk
  //  add piece or use potential
  newState = playerMove(newState) // ?: return null

  newState.assertPieceCount()

  newState.board.printHexGrid()

  // Handle Tamsk Potential
  if (isTamskPieceAtCenter(newState.board, newState.currentPlayer)) {
    newState = playerMove(newState) // ?: return null
  }

  newState.assertPieceCount()

  val occupiedDots = newState.board.nodes.filter { it.isDot && it.piece != null }

  check(!newState.board.nodes.any { it.isDot && it.piece != null }) {
    val dots = occupiedDots.map {
      "${it.coordinate.column}${it.coordinate.row} (${it.piece?.colorName} ${it.piece?.type?.name})"
    }

    "Invalid state transition: Outer perimeter dots must be empty at the end of a turn, " +
        "but found pieces remaining on: $dots"
  }

	// TODO Replace with bitboard equivalent
	newState = enforcePieceRemovalRules(newState)

  // recombine player pieces
  newState.currentPlayer.combinePieces()

  newState.assertPieceCount()

  newState.board.printHexGrid()

  return newState
}

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
)

enum class MoveType {
  AddPiece,
  UsePotential,
	RetrieveCapturePieces,
}

fun playerMove(state: State): State {

  val savedBoardState = state.board.deepCopy()
  val savedLines = state.lines.deepCopy()

//	val gameTree: HashMap<String, AlphaBetaScore> = hashMapOf()

	val bitboard = convertBoardToBitboard(state.board)

	val bestMove = alphabetaBitboardAddPieces(
		depth = 1,
		bitboard = bitboard,
		currentPlayer = state.currentPlayer,
		opponentPlayer = state.nextPlayer,
		alphaBetaScore = AlphaBetaScoreBit()
	).move

	when (bestMove?.moveType) {
		MoveType.AddPiece -> {

			val node = state.board.nodes.first { it.bitmask == bestMove.targetBit }

			node.piece = bestMove.piece?.let { state.currentPlayer.selectPiece(it) }

			require(
				bestMove.targetBit != null &&
				bestMove.pushDirection != null &&
				bestMove.columnInfos.isNotEmpty() &&
				bestMove.piece != null
			)

			if(bestMove.sourceBit == null) {
				bitboard.addPieceToBitboard(
					addAtIndex = bestMove.targetBit,
					pushDirection = bestMove.pushDirection,
					col = bestMove.columnInfos.first(),
					piece = bestMove.piece
				)
			} else {
				bitboard.useTamskPotential(
					player = state.currentPlayer,
					sourceIndex = bestMove.sourceBit,
					targetIndex = bestMove.targetBit,
					col = bestMove.columnInfos.first(),
					pushDirection = bestMove.pushDirection,
				)
			}

			bitboard.assertPieceCount(currentPlayer = state.currentPlayer, nextPlayer = state.nextPlayer)

			// Move piece in the selected spot(node) based on selected push direction
			val newBoard =
				bitboard.convertBitboardToBoard(savedBoardState)

			val newState =
				state
					.deepCopy()
					.copy(
						board = newBoard,
						lines = constructLines(newBoard.nodes),
					)

			val occupiedDots = newState.board.nodes.filter { it.isDot && it.piece != null }

			if (newState.board.nodes.any { it.isDot && it.piece != null }) {
				check(!newState.board.nodes.any { it.isDot && it.piece != null }) {
					val dots = occupiedDots.map {
						"${it.coordinate.column}${it.coordinate.row} (${it.piece?.colorName} ${it.piece?.type?.name})"
					}

					"Invalid state transition: Outer perimeter dots must be empty at the end of a turn, " +
							"but found pieces remaining on: $dots"
				}
			}

			newState.assertPieceCount()

			return newState
		}
		MoveType.UsePotential -> {
			require(bestMove.sourceBit != null && bestMove.targetBit != null) {}

			bitboard.usePiecePotential(possibleBitMove = bestMove)

			// Move piece in the selected spot(node) based on selected push direction
			val newBoard =
				bitboard.convertBitboardToBoard(savedBoardState)

			val newState =
				state
					.deepCopy()
					.copy(
						board = newBoard,
						lines = constructLines(newBoard.nodes),
					)

			val occupiedDots = newState.board.nodes.filter { it.isDot && it.piece != null }

			if (newState.board.nodes.any { it.isDot && it.piece != null }) {
				check(!newState.board.nodes.any { it.isDot && it.piece != null }) {
					val dots = occupiedDots.map {
						"${it.coordinate.column}${it.coordinate.row} (${it.piece?.colorName} ${it.piece?.type?.name})"
					}

					"Invalid state transition: Outer perimeter dots must be empty at the end of a turn, " +
							"but found pieces remaining on: $dots"
				}
			}

			newState.assertPieceCount()

			return newState
		}
		MoveType.RetrieveCapturePieces -> { }
		null -> {
			return state
		}
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
		        6/ It will occur that more than one row-of-4 of the same
		        color are lined up at the same time. If these rows do
		        not intersect each other, then they are removed (at
		        least partially) following the standard procedure.
		        If they do intersect, the player playing that color
		        may choose which row they will deal with first. If
		        they remove the piece on the intersecting spot, the
		        second row is broken up and the remaining pieces of
		        that row stay on the board. If it is a stack and it is left
		        on the intersecting spot, then the second row is still
		        intact, which means that it must also be dealt with.
		        (See illustration 5: Black may choose between fi rst
		        dealing with the row of 4 pieces or the row of 5
		        pieces. If the piece on the intersecting spot is a
		        stack and Black removes it from the board, then
		        the other row in not complete anymore; if Black
		        leaves the stack on the board, they must also deal
		        with that other row.)
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
          lines = constructLines(board.nodes),
      )

  newNewState.assertPieceCount()

  return newNewState
}

fun chooseToRemovePiecesWithPotential(line: Set<Node>, player: Player): Boolean {
  return line.filter { it.piece?.colorName == player.name.name }.all { it.piece?.potential == true }
}

fun evaluateCapturedPieces(state: State): Boolean {
  return state.currentPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3
}

fun evaluatePiecesInReserve(state: State): Boolean {
  return state.currentPlayer.piecesInReserve.none { piece ->
    piece.potential || piece.type == PieceType.GIPF
  }
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

fun determineWinner(currentPlayer: Player, nextPlayer: Player): Player? {
	// TODO refactor

  return if (
      currentPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3 ||
          currentPlayer.piecesInReserve.any { piece ->
            piece.potential || piece.type == PieceType.GIPF
          }
  ) {
    currentPlayer
  } else if (
      nextPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3 ||
          nextPlayer.piecesInReserve.any { piece ->
            piece.potential || piece.type == PieceType.GIPF
          }
  ) {
    nextPlayer
  } else {
    null
  }
}
