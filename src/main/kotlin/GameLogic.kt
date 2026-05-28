package org.example

import kotlin.collections.mutableSetOf

/**
 * TODO create a two functions:
 *  1. Add new pieces to the board
 *  2. Using piece potentials
*/
fun playerMove(state: State): State {
  // Does player have GIPF pieces in reserve?
  val gipfPiecesInReserve: Int =
      state.currentPlayer.piecesInReserve.count { piece -> piece.type == PieceType.GIPF }

  val playableStackedPiecesInReserve: List<Piece> =
      state.currentPlayer.piecesInReserve.filter { piece -> piece.type != PieceType.GIPF && piece.potential}

	val eligibleMoves = getEligiblePotentialMoves(state)

  // TODO Current Player has no moves left
  if (playableStackedPiecesInReserve.isEmpty() && gipfPiecesInReserve == 0 && eligibleMoves.isEmpty()) {
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

  val selectableDots = populatedNodes.filter { it.neighbours.isNotEmpty() }.toSet()

  val numberOfPiecesBefore = state.currentPlayer.getNumberOfPiecesInReserve()

  val selectedDot =
			// the Order of playing GIPF and TAMSK potential is invariant
      if (gipfPiecesInReserve > 0) {
        val selectedNode = selectDot(selectableDots)

        selectedNode.node.piece = state.currentPlayer.selectPiece(PieceType.GIPF)

        val piecesPlaced = numberOfPiecesBefore - state.currentPlayer.getNumberOfPiecesInReserve()

        check(piecesPlaced == 1) {
          "Player state corrupted: Expected 1 piece to be removed from reserve, but found $piecesPlaced."
        }

        selectedNode
      } else
				if (isTamskPieceAtCenter(state.board, state.currentPlayer)) {
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
      }
				else /*if (otherPiecesInReserve.isNotEmpty())*/ {
        // TODO select piece from otherPiecesInReserve or use a piece's potential if there are any
        val selectedNode = selectDot(selectableDots)

        val selectedPiece = selectPieceFromReserve(playableStackedPiecesInReserve)

        require(selectedPiece != null) { "No piece was selected!" }

        selectedNode.node.piece = state.currentPlayer.selectPiece(selectedPiece.type)
        selectedNode
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

  val newState =
      updateBoard(
          board =
	          // Move piece in the selected spot(node) based on selected push direction
	          shiftPiece(
                  currentNode = selectedDot.node,
                  moveDirection = selectedPushDirection,
                  board = state.board,
                  lines = state.lines,
              ),
          state = state,
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

data class NodeConnections(
    val node: Node,
    val neighbours: MutableSet<Node> = mutableSetOf(),
)

fun enforcePieceRemovalRules(state: State): State {
  val newState =
      State(
          currentPlayer = state.currentPlayer,
          nextPlayer = state.nextPlayer,
          board = state.board,
          lines = state.lines,
      )

  // TODO "Implement logic for instances where all of the current player's pieces have their potential == true"
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

  // TODO Check if the lines intersect and is a stack
  val linesWithFourPiecesInARow =
      newState.lines
          .toList()
          .map { line -> evaluateLines(player = newState.currentPlayer, lines = line) }
          .filter { (hasFourPiecesInARow, _) -> hasFourPiecesInARow }

  if (linesWithFourPiecesInARow.isEmpty()) {
    return state
  }

  val retrievedCapturedPieces: List<RetrievedCapturedPieces> =
      if (linesWithFourPiecesInARow.size == 1) {
        linesWithFourPiecesInARow.map { (_, line) ->
          retrieveAndCapturePieces(
              player = state.currentPlayer,
              line = line,
              removePiecesWithPotential =
                  chooseToRemovePiecesWithPotential(line, newState.currentPlayer),
          )
        }
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

        intersectingLines.map { line ->
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
            }
      }

  val retrievedPieces =
      retrievedCapturedPieces.fold(initial = mutableListOf<Piece>()) { acc, line ->
        (acc + line.retrieved).toMutableList()
      }
  val capturedPieces =
      retrievedCapturedPieces.fold(initial = mutableListOf<Piece>()) { acc, line ->
        (acc + line.captured).toMutableList()
      }
  val retrievedCapturedNodes =
      retrievedCapturedPieces.fold(initial = mutableSetOf<Node>()) { acc, line ->
        acc.addAll(line.nodes)
        acc
      }

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

  val board = state.board.removePieces(retrievedCapturedNodes)

  return State(
      currentPlayer = newState.currentPlayer,
      nextPlayer = newState.currentPlayer,
      board = board,
      lines = constructLines(board.nodes),
  )
}

fun chooseToRemovePiecesWithPotential(line: Set<Node>, player: Player): Boolean {
  return line.filter { it.piece?.color == player.color }.all { it.piece?.potential != null }
}

fun evaluateCapturedPieces(state: State): Boolean {
  return state.currentPlayer.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3
}

fun evaluatePiecesInReserve(state: State): Boolean {
  return state.currentPlayer.piecesInReserve.none { piece -> piece.potential || piece.type == PieceType.GIPF }
}



fun playerTurn(state: State): State {
  var newState = enforcePieceRemovalRules(state)

	// recombine player pieces
	newState.currentPlayer.recombinePieces()

	// Handle Tamsk Potential
  if (isTamskPieceAtCenter(newState.board, newState.currentPlayer)) {
    newState = playerMove(newState)
  }

  newState = playerMove(newState)

  newState.board.printHexGrid()

	// Handle Tamsk Potential
  if (isTamskPieceAtCenter(newState.board, newState.currentPlayer)) {
    newState = playerMove(newState)
  }

  val occupiedDots = newState.board.nodes.filter { it.isDot && it.piece != null }

  check(!newState.board.nodes.any { it.isDot && it.piece != null }) {
    val dots = occupiedDots.map {
      "${it.coordinate.column}${it.coordinate.row} (${it.piece?.colorName} ${it.piece?.type?.name})"
    }

    "Invalid state transition: Outer perimeter dots must be empty at the end of a turn, " +
        "but found pieces remaining on: $dots"
  }

  newState = enforcePieceRemovalRules(newState)

	// recombine player pieces
	newState.currentPlayer.recombinePieces()

  newState.board.printHexGrid()

  return newState
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
