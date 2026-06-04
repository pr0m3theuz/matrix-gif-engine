package org.example.engine

import kotlinx.serialization.json.Json
import org.example.model.assertPieceCount
import org.example.model.getLinesWithSpaces
import org.example.model.getNeighbours
import org.example.model.getPushDirectionFromNeighbor
import org.example.model.Board
import org.example.model.Coordinate
import org.example.model.Lines
import org.example.model.Node
import org.example.model.NodeConnections
import org.example.model.Piece
import org.example.model.PieceType
import org.example.model.Player
import org.example.model.PushDirection
import org.example.model.RetrievedCapturedPieceNode
import org.example.model.State
import org.example.model.constructLines
import org.example.model.evaluateLines
import org.example.model.printHexGrid
import org.example.model.removePieces
import org.example.model.toList
import kotlin.collections.mutableSetOf
import kotlin.collections.none
import kotlin.system.exitProcess

fun playerTurn(state: State): State {
  var newState = enforcePieceRemovalRules(state,)

  // recombine player pieces
  newState.currentPlayer.recombinePieces()

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

  newState = enforcePieceRemovalRules(newState,)

  // recombine player pieces
  newState.currentPlayer.recombinePieces()

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

data class PossibleMove(
	val piece: Piece? = null,
	val selectableDots: Set<NodeConnections> = emptySet(),
	val eligiblePotentialPieceNode: Node? = null,
	val eligiblePotentialTargetNodes: Set<Node> = emptySet(),
	val moveType: MoveType,
)

enum class MoveType {
  AddPiece,
  UsePotential,
}

fun playerMove(state: State): State {
  // Does player have GIPF pieces in reserve?
  val gipfPiecesInReserve: Int =
      state.currentPlayer.piecesInReserve.count { piece -> piece.type == PieceType.GIPF }

  val playableStackedPiecesInReserve: List<Piece> =
      state.currentPlayer.piecesInReserve.filter { piece ->
        piece.type != PieceType.GIPF && piece.potential
      }

	// TODO work on finding eligiblePotentialTargetNodes
	val eligibleMovesUsingPotential: Map<Node, Set<Node?>> =
		getEligiblePotentialMoves(state)

  // TODO Current Player has no moves left
  if (
      playableStackedPiecesInReserve.isEmpty() &&
          gipfPiecesInReserve == 0 &&
          eligibleMovesUsingPotential.isEmpty()
  ) {
    return state
  }

  val dots = state.board.nodes.filter { it.isDot }

  // TODO rename variable to be more descriptive
  val populatedNodes: MutableSet<NodeConnections> = mutableSetOf()

  val linesWithSpace: Lines = state.lines.getLinesWithSpaces()

  val allLines =
      linesWithSpace.verticalLines +
          linesWithSpace.upwardRightLines +
          linesWithSpace.downwardRightLines

  check(allLines.all { nodes -> nodes.any { node -> node.piece == null } }) {
    val jammedLines =
        allLines
            .filter { nodes -> nodes.none { node -> node.piece == null } }
            .map { nodes ->
              nodes.joinToString(", ", prefix = "[", postfix = "]") {
                "${it.coordinate.column}${it.coordinate.row}"
              }
            }

    "Invalid state transition: A piece shift was attempted on a blocked axis. " +
        "The following target lines have no empty spaces remaining: $jammedLines"
  }

  // populate populatedNodes
  dots.forEach { dot ->
    val nodeConnections = NodeConnections(node = dot)

    when {
      (dot.neighbors?.above != null || dot.neighbors?.below != null) -> {
        linesWithSpace.verticalLines.forEach { line ->
          val ends: List<Coordinate> = listOf(line.first().coordinate, line.last().coordinate)
          val origins: List<Node> = listOf(line.first(), line.last())

          if (ends.contains(dot.neighbors!!.above) || ends.contains(dot.neighbors!!.below)) {
            ends.forEach { coordinate ->
              val result = dot.neighbors!!.getNeighbours().contains(coordinate)
              if (result) {
                // selectableDots.plus(dot)
                // TODO Add node to list of nodes to push piece on
                val node = origins.first { node -> node.coordinate == coordinate }

                nodeConnections.neighbours.add(node)
              }
            }
          }
        }
      }

      dot.neighbors?.upperRight != null || dot.neighbors?.lowerLeft != null -> {
        linesWithSpace.upwardRightLines.forEach { line ->
          val ends: List<Coordinate> = listOf(line.first().coordinate, line.last().coordinate)
          val origins: List<Node> = listOf(line.first(), line.last())

          if (
              ends.contains(dot.neighbors!!.upperRight) || ends.contains(dot.neighbors!!.lowerLeft)
          ) {
            ends.forEach { coordinate ->
              val result = dot.neighbors!!.getNeighbours().contains(coordinate)
              if (result) {
                // selectableDots.plus(dot)
                // TODO Add node to list of nodes to push piece on
                val node = origins.first { node -> node.coordinate == coordinate }

                nodeConnections.neighbours.add(node)
              }
            }
          }
        }
      }

      dot.neighbors?.lowerRight != null || dot.neighbors?.upperLeft != null -> {
        linesWithSpace.downwardRightLines.forEach { line ->
          val ends: List<Coordinate> = listOf(line.first().coordinate, line.last().coordinate)
          val origins: List<Node> = listOf(line.first(), line.last())

          if (
              ends.contains(dot.neighbors!!.lowerRight) || ends.contains(dot.neighbors!!.upperLeft)
          ) {
            ends.forEach { coordinate ->
              val result = dot.neighbors!!.getNeighbours().contains(coordinate)
              if (result) {
                // selectableDots.plus(dot)
                // TODO Add node to list of nodes to push piece on
                val node = origins.first { node -> node.coordinate == coordinate }

                nodeConnections.neighbours.add(node)
              }
            }
          }
        }
      }
    }

    populatedNodes.add(nodeConnections)
  }

  val selectableDots: Set<NodeConnections> =
      populatedNodes.filter { it.neighbours.isNotEmpty() }.toSet()

  val numberOfPiecesBefore = state.currentPlayer.getNumberOfPiecesInReserve()

  // Build a list of all available moves
  val allAvailableMoves: MutableList<PossibleMove> = mutableListOf()

  playableStackedPiecesInReserve.forEach { piece ->
    allAvailableMoves.add(
        PossibleMove(piece = piece, selectableDots = selectableDots, moveType = MoveType.AddPiece)
    )
  }

  eligibleMovesUsingPotential
      .filter { (key, value) -> value.filterNotNull().isNotEmpty() }
      .forEach { (node, eligiblePotentialTargetNodes): Map.Entry<Node, Set<Node?>> ->
        allAvailableMoves.add(
            PossibleMove(
                eligiblePotentialPieceNode = node,
                eligiblePotentialTargetNodes = eligiblePotentialTargetNodes.filterNotNull().toSet(),
                moveType = MoveType.UsePotential,
            )
        )
      }

  val selectedDot =
      // the Order of playing GIPF and TAMSK potential is invariant
      if (gipfPiecesInReserve > 0) {
        val selectedNode = selectDot(selectableDots)

        selectedNode.node.piece = state.currentPlayer.selectGIPFPiece()

        val piecesPlaced = numberOfPiecesBefore - state.currentPlayer.getNumberOfPiecesInReserve()

        check(piecesPlaced == 1) {
          "Player state corrupted: Expected 1 piece to be removed from reserve, but found $piecesPlaced."
        }

        selectedNode
      } else if (isTamskPieceAtCenter(state.board, state.currentPlayer)) {
        // TODO use potential
        // TODO put piece on a selectable dot shift piece
        val selectedNode = selectDot(selectableDots)

        val piece: Piece? =
            state.board.nodes
                .first {
                  it.coordinate.column == state.board.centerNodeCoordinate.column &&
                      it.coordinate.row == state.board.centerNodeCoordinate.row
                }
                .piece

        selectedNode.node.piece = piece?.usePiecePotential()

        check(piece?.potential == false) {
          val pieceCoords = selectedNode.node.coordinate.let { "${it.column}${it.row}" }
          val currentPotential = piece?.potential

          "Invalid piece state at $pieceCoords: Expected piece potential to be spent (false), " +
              "but found potential status is: $currentPotential (Piece Type: ${piece?.type?.name}, Color: ${piece?.colorName})"
        }

        selectedNode
      } else if (allAvailableMoves.isNotEmpty()) {
        // TODO select piece from otherPiecesInReserve or use a piece's potential if there are any

        val randomMove = allAvailableMoves.random()


        when (randomMove.moveType) {
          MoveType.AddPiece -> {
            val selectedNode = randomMove.selectableDots.random() // selectDot(selectableDots)

            val selectedPiece = randomMove.piece //selectPieceFromReserve(playableStackedPiecesInReserve)

            require(selectedPiece != null) { "No piece was selected!" }

            selectedNode.node.piece = state.currentPlayer.selectPiece(selectedPiece.type)
            selectedNode
          }
	        // TODO work on finding eligiblePotentialTargetNodes
          MoveType.UsePotential -> {
            require(randomMove.eligiblePotentialPieceNode != null)
            val newState = usePiecePotential(
	            node = randomMove.eligiblePotentialPieceNode.deepCopy(),
	            eligibleNodesForPotential = randomMove.eligiblePotentialTargetNodes,
	            state = state,
            )

            return newState
          }
        }
      } else {
				println("${state.currentPlayer.name} player has no available moves left!")
	      // TODO exitProcess or return state
//	      return null
				exitProcess(status = 0)
      }

  // Directions to the piece can be pushed ir
  val availablePushDirections =
      selectedDot.neighbours.mapNotNull { neighbour ->
        selectedDot.node.neighbors!!.getPushDirectionFromNeighbor(neighbour.coordinate)
      }

  if (availablePushDirections.isEmpty()) {
    check(availablePushDirections.isEmpty()) {
      "Invalid game state: Expected no valid moves to be remaining, but found available push directions: $availablePushDirections"
    }
  }

  val selectedPushDirection = selectPushDirection(availablePushDirections)

  val savedBoardState = state.board.deepCopy()
  val savedLines = state.lines.deepCopy()

  // Move piece in the selected spot(node) based on selected push direction
  val newBoard =
	  shiftPiece(
		  currentNode = selectedDot.node,
		  moveDirection = selectedPushDirection,
		  board = savedBoardState,
		  lines = savedLines,
	  )

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

	newState.assertPieceCount()


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
          .map { line -> evaluateLines(player = newState.currentPlayer, lines = line) }
          .filter { (hasFourPiecesInARow, _) -> hasFourPiecesInARow }

  if (linesWithFourPiecesInARow.isEmpty()) {
    return state
  }

  val retrievedCapturedPieces: List<RetrievedCapturedPieceNode> =
      if (linesWithFourPiecesInARow.size == 1) {
        linesWithFourPiecesInARow.map { (_, line) ->
	        retrieveAndCapturePieces(
		        player = state.currentPlayer,
		        line = line,
		        removePiecesWithPotential =
			        chooseToRemovePiecesWithPotential(line, newState.currentPlayer), // Result not used
	        )
        }.flatten().distinctBy { it.node?.coordinate }
      } else {
        /*
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
            }).flatten().distinctBy { it.node?.coordinate }
      }

	//
  val retrievedPieces =
      retrievedCapturedPieces.mapNotNull { it.retrievedPiece }
//				.fold(initial = mutableListOf<Piece>()) { acc, line ->
//        (acc + line.retrievedPiece).toMutableList()
//      }
  val capturedPieces =
      retrievedCapturedPieces.mapNotNull { it.capturedPiece }

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

  val newNewState = State(
	  currentPlayer = newState.currentPlayer,
	  nextPlayer = newState.nextPlayer,
	  board = board,
	  lines = constructLines(board.nodes),
  )

	newNewState.assertPieceCount()

	return newNewState
}

fun chooseToRemovePiecesWithPotential(line: Set<Node>, player: Player): Boolean {
  return line.filter { it.piece?.colorName == player.name }.all { it.piece?.potential == true }
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
  return piece.type == PieceType.TAMSK && piece.colorName == player.name && piece.potential
}

fun determineWinner(state: State): Player? {
  return if (
      state.currentPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3 ||
          state.currentPlayer.piecesInReserve.any { piece ->
	      piece.potential || piece.type == PieceType.GIPF
      }
  ) {
    state.currentPlayer
  } else if (
      state.nextPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF }  == 3 || state.nextPlayer.piecesInReserve.any { piece ->
	      piece.potential || piece.type == PieceType.GIPF
      }
  ) {
    state.nextPlayer
  } else {
    null
  }
}
