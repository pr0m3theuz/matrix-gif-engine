import java.io.File
import kotlin.io.readText
import kotlinx.serialization.json.Json
import org.example.Coordinate
import org.example.LineOrientation
import org.example.Node
import org.example.PieceType
import org.example.State
import org.example.constructLines
import org.example.evaluateLines
import org.example.initializeState
import org.example.printHexGrid
import org.example.toList
import org.example.updateBoard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MovesTest {

  @Test
  fun getEligiblePotentialMoves() {
    var state = initializeState()
    val piece = state.currentPlayer.selectPiece(PieceType.YINSH)

    val node = state.board.nodes.first { it.coordinate == state.board.centerNodeCoordinate }
    node.piece = piece

    val board =
        updateBoard(
            node = node,
            board = state.board,
        )

    state = state.updateState(board, state)
    state.board.printHexGrid()

    val eligibleMovesUsingPotential = org.example.getEligiblePotentialMoves(state)

    assert(eligibleMovesUsingPotential.containsKey(node))
    assert(eligibleMovesUsingPotential[node]?.isNotEmpty() == true)
  }

  @Test
  fun findFirstNode() {
    var state = initializeState()
    val piece = state.currentPlayer.selectPiece(PieceType.ZERTZ)

    val node = state.board.nodes.first { it.coordinate == state.board.centerNodeCoordinate }
    node.piece = piece

    val board =
        updateBoard(
            node = node,
            board = state.board,
        )

    state = state.updateState(board, state)
    state.board.printHexGrid()

    val lines = state.lines.getLinesContainingNode(node)

    val verticalNodes =
        org.example.findFirstNode(
            currentNode = node,
            line = lines.verticalLines.first(),
            orientation = LineOrientation.VERTICAL,
            isNextNodeVacant = false,
            isNextNodeOccupied = true,
        )
    val upwardRightNodes =
        org.example.findFirstNode(
            currentNode = node,
            line = lines.upwardRightLines.first(),
            orientation = LineOrientation.UPWARD_RIGHT,
            isNextNodeVacant = false,
            isNextNodeOccupied = true,
        )
    val downwardRightNodes =
        org.example.findFirstNode(
            currentNode = node,
            line = lines.downwardRightLines.first(),
            orientation = LineOrientation.DOWNWARD_RIGHT,
            isNextNodeVacant = false,
            isNextNodeOccupied = true,
        )

    assert(verticalNodes.size == lines.verticalLines.first().count { it.piece == null })
    assert(upwardRightNodes.size == lines.upwardRightLines.first().count { it.piece == null })
    assert(downwardRightNodes.size == lines.downwardRightLines.first().count { it.piece == null })
  }

  @Test
  fun findAdjacentVacantNodes() {
    var state = initializeState()
    val piece = state.currentPlayer.selectPiece(PieceType.YINSH)

    val node = state.board.nodes.first { it.coordinate == state.board.centerNodeCoordinate }
    node.piece = piece

    val board =
        updateBoard(
            node = node,
            board = state.board,
        )

    state = state.updateState(board, state)
    state.board.printHexGrid()

    val lines = state.lines.getLinesContainingNode(node)

    val verticalNodes =
        org.example.findAdjacentVacantNodes(
            currentNode = node,
            line = lines.verticalLines.first(),
            orientation = LineOrientation.VERTICAL,
            isNextNodeVacant = false,
            isNextNodeOccupied = true,
        )
    val upwardRightNodes =
        org.example.findAdjacentVacantNodes(
            currentNode = node,
            line = lines.upwardRightLines.first(),
            orientation = LineOrientation.UPWARD_RIGHT,
            isNextNodeVacant = false,
            isNextNodeOccupied = true,
        )
    val downwardRightNodes =
        org.example.findAdjacentVacantNodes(
            currentNode = node,
            line = lines.downwardRightLines.first(),
            orientation = LineOrientation.DOWNWARD_RIGHT,
            isNextNodeVacant = false,
            isNextNodeOccupied = true,
        )

    assert(verticalNodes.size == lines.verticalLines.first().count { it.piece == null })
    assert(upwardRightNodes.size == lines.upwardRightLines.first().count { it.piece == null })
    assert(downwardRightNodes.size == lines.downwardRightLines.first().count { it.piece == null })
  }

  @Test fun getNextNode() {}

  @Test
  fun getAdjacentVacantNode() {
    var state = initializeState()
    val piece = state.currentPlayer.selectPiece(PieceType.YINSH)

    val node = state.board.nodes.first { it.coordinate == state.board.centerNodeCoordinate }
    node.piece = piece

    val board =
        updateBoard(
            node = node,
            board = state.board,
        )

    state = state.updateState(board, state)
    state.board.printHexGrid()

    val lines = state.lines.getLinesContainingNode(node)
  }

  @Test fun pushPiece() {}

  @Test fun shiftPiece() {}

  @Test fun usePotentialMovePiece() {}

  @Test fun retrieveAndCapturePieces() {}

  @Test
  fun usePiecePotential() {
    val filename =
        "/Users/darronporter/Downloads/Dissertation/code/src/test/kotlin/usePiecePotential_bug.json"

    val loadedState = Json.decodeFromString<State>(File(filename).bufferedReader().readText())

    loadedState.board.printHexGrid()

    val pieceType = PieceType.DVONN
    val node = loadedState.board.nodes.first { it.coordinate == Coordinate('E', 5) }

    val lines = constructLines(loadedState.board.nodes).getLinesContainingNode(node)

    val verticalNodes =
        org.example
            .findFirstNode(
                currentNode = node,
                line = lines.verticalLines.first(),
                orientation = LineOrientation.VERTICAL,
                isNextNodeVacant = false,
                isNextNodeOccupied = true,
            )
            .filter {
              it?.piece?.type == pieceType &&
                  (it.piece?.colorName != loadedState.currentPlayer.name ||
		                  it.piece?.stackedPieces?.lastOrNull()?.colorName == loadedState.nextPlayer.name)
            }
            .toSet()
    val upwardRightNodes =
        org.example
            .findFirstNode(
                currentNode = node,
                line = lines.upwardRightLines.first(),
                orientation = LineOrientation.UPWARD_RIGHT,
                isNextNodeVacant = false,
                isNextNodeOccupied = true,
            )
            .filter {
              it?.piece?.type == pieceType &&
                  (it.piece?.colorName != loadedState.currentPlayer.name ||
		                  it.piece?.stackedPieces?.lastOrNull()?.colorName == loadedState.nextPlayer.name)
            }
            .toSet()
    val downwardRightNodes =
        org.example
            .findFirstNode(
                currentNode = node,
                line = lines.downwardRightLines.first(),
                orientation = LineOrientation.DOWNWARD_RIGHT,
                isNextNodeVacant = false,
                isNextNodeOccupied = true,
            )
            .filter {
              it?.piece?.type == pieceType &&
                  (it.piece?.colorName != loadedState.currentPlayer.name ||
                      it.piece?.stackedPieces?.lastOrNull()?.colorName == loadedState.nextPlayer.name)
            }
            .toSet()

    // TODO add logic for stacked pieces

    val eligibleNodesForPotential =
        (verticalNodes + upwardRightNodes + downwardRightNodes).filterNotNull().toSet()

    val piece = node.piece!!

    val newState: State =
        when (piece.type) {
          PieceType.GIPF -> {
            loadedState
          }
          PieceType.TAMSK -> {
            loadedState
          }

          PieceType.ZERTZ -> {
            val potential = piece.usePiecePotential()
            val selectedNodeToPlacePotential: Node = eligibleNodesForPotential.random()

            check(!potential.potential && !piece.potential) {
              "State invalid: source piece and potential cannot have active potentials. " +
                  "State -> Potential: ${potential.potential}, Piece: ${piece.potential}" +
                  "\nCurrent Node: ${Json.encodeToString(node)}" +
                  "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
                  "\n\nState: ${Json.encodeToString(loadedState)}"
            }

            selectedNodeToPlacePotential.piece = potential

            var newBoard = updateBoard(node, loadedState.board)
            newBoard = updateBoard(selectedNodeToPlacePotential, newBoard)

            loadedState.copy(board = newBoard, lines = constructLines(newBoard.nodes))
          }

          PieceType.YINSH -> {
            val potential = piece.usePiecePotential()

            val selectedNodeToPlacePotential: Node = eligibleNodesForPotential.random()

            check(!potential.potential && !piece.potential) {
              "State invalid: source piece and potential cannot have active potentials. " +
                  "State -> Potential: ${potential.potential}, Piece: ${piece.potential}" +
                  "\nCurrent Node: ${Json.encodeToString(node)}" +
                  "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
                  "\n\nState: ${Json.encodeToString(loadedState)}"
            }

            selectedNodeToPlacePotential.piece = potential

            var newBoard = updateBoard(node, loadedState.board)
            newBoard = updateBoard(selectedNodeToPlacePotential, newBoard)

            loadedState.copy(board = newBoard, lines = constructLines(newBoard.nodes))
          }

          PieceType.DVONN -> {
            val potential = piece.usePiecePotential()
            val selectedNodeToPlacePotential: Node = eligibleNodesForPotential.random()

            check(!potential.potential && !piece.potential) {
              "State invalid: source piece and potential cannot have active potentials. " +
                  "State -> Potential: ${potential.potential}, Piece: ${piece.potential}" +
                  "\nCurrent Node: $node" +
                  "\n\nState: ${Json.encodeToString(loadedState)}"
            }

            if (potential.type != selectedNodeToPlacePotential.piece?.type) {
              check(selectedNodeToPlacePotential.piece?.colorName != potential.colorName) {
                "Illegal placement: Cannot target a piece of a different type (${potential.type})." +
                    "\nCurrent Node: ${Json.encodeToString(node)}" +
                    "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
                    "\n\nState: ${Json.encodeToString(loadedState)}"
              }
            }

            if (selectedNodeToPlacePotential.piece?.isNeutralized == false) {
              check(selectedNodeToPlacePotential.piece?.colorName != potential.colorName) {
                "Illegal placement: Cannot target a piece of your own color (${potential.colorName})." +
                    "\nCurrent Node: ${Json.encodeToString(node)}" +
                    "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
                    "\n\nState: ${Json.encodeToString(loadedState)}"
              }
            }

	          selectedNodeToPlacePotential.piece?.stackedPieces?.let {
		          if (it.size > 1) {
			          val topPotential = it.last()
			          
			          check(topPotential.colorName != potential.colorName) {
				          "Illegal move: A ${potential.type.name}-potential cannot jump onto another potential of the same color (${potential.colorName})." +
						          "\nCurrent Node: ${Json.encodeToString(node)}" +
						          "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
						          "\n\nState: ${Json.encodeToString(loadedState)}"
			          }
		          }
	          }

            selectedNodeToPlacePotential.piece?.stackedPieces?.add(potential)
            selectedNodeToPlacePotential.piece?.isNeutralized = true

            var newBoard = updateBoard(node, loadedState.board)
            newBoard = updateBoard(selectedNodeToPlacePotential, newBoard)

            loadedState.copy(board = newBoard, lines = constructLines(newBoard.nodes))
          }

          PieceType.PUNCT -> {
            val potential = piece.usePiecePotential()
            val selectedNodeToPlacePotential: Node = eligibleNodesForPotential.random()

            check(!potential.potential && !piece.potential) {
              "State invalid: source piece and potential cannot have active potentials. " +
                  "State -> Potential: ${potential.potential}, Piece: ${piece.potential}" +
                  "\n\nCurrent Node: $node" +
                  "\n\nState: ${Json.encodeToString(loadedState)}"
            }

            if (potential.type != selectedNodeToPlacePotential.piece?.type) {
              check(selectedNodeToPlacePotential.piece?.colorName != potential.colorName) {
                "Illegal placement: Cannot target a piece of a different type (${potential.type})." +
                    "\nCurrent Node: ${Json.encodeToString(node)}" +
                    "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
                    "\n\nState: ${Json.encodeToString(loadedState)}"
              }
            }

	          if (selectedNodeToPlacePotential.piece?.isNeutralized == false) {
		          check(selectedNodeToPlacePotential.piece?.colorName != potential.colorName) {
			          "Illegal placement: Cannot target a piece of your own color (${potential.colorName})." +
					          "\nCurrent Node: ${Json.encodeToString(node)}" +
					          "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
					          "\n\nState: ${Json.encodeToString(loadedState)}"
		          }
	          }

	          selectedNodeToPlacePotential.piece?.stackedPieces?.let {
		          if (it.size > 1) {
			          val topPotential = it.last()

			          check(topPotential.colorName != potential.colorName) {
				          "Illegal move: A ${potential.type.name}-potential cannot jump onto another potential of the same color (${potential.colorName})." +
						          "\nCurrent Node: ${Json.encodeToString(node)}" +
						          "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
						          "\n\nState: ${Json.encodeToString(loadedState)}"
			          }
		          }
	          }

	          selectedNodeToPlacePotential.piece?.stackedPieces?.add(potential)
	          selectedNodeToPlacePotential.piece?.isNeutralized = true


	          var newBoard = updateBoard(node, loadedState.board)
            newBoard = updateBoard(selectedNodeToPlacePotential, newBoard)

            loadedState.copy(board = newBoard, lines = constructLines(newBoard.nodes))
          }
        }

    val updatedNode = newState.board.nodes.first { it.coordinate == node.coordinate }

	  newState.lines
		  .toList()
		  .map { line -> evaluateLines(player = newState.currentPlayer, lines = line) }
		  .filter { (hasFourPiecesInARow, _) -> hasFourPiecesInARow }

    if (updatedNode.piece?.isNeutralized == true) {
      check(node.piece?.type in setOf(PieceType.DVONN, PieceType.PUNCT)) {
        "Only Dvonn and Pünct pieces can be neutralized. \n Node: ${Json.encodeToString<Node>(node)}"
      }
      node.piece?.stackedPieces?.lastIndex?.let { node.piece?.stackedPieces[it] }?.colorName == loadedState.currentPlayer.name
    } else {
      node.piece?.colorName == loadedState.currentPlayer.name
    }
  }

  @Test
  fun `testing different states`() {

    val filename =
        "/Users/darronporter/Downloads/Dissertation/code/src/test/kotlin/saved_state.json"

    val loadedState = Json.decodeFromString<State>(File(filename).bufferedReader().readText())

    loadedState.board.printHexGrid()

    val pieceType = PieceType.DVONN
    val node = loadedState.board.nodes.first { it.coordinate == Coordinate('B', 3) }

    val lines = constructLines(loadedState.board.nodes).getLinesContainingNode(node)

    val verticalNodes =
        org.example
            .findFirstNode(
                currentNode = node,
                line = lines.verticalLines.first(),
                orientation = LineOrientation.VERTICAL,
                isNextNodeVacant = false,
                isNextNodeOccupied = true,
            )
            .filter {
              it?.piece?.type == pieceType &&
                  (it.piece?.colorName != loadedState.currentPlayer.name ||
                      it.piece?.stackedPieces?.lastOrNull()?.colorName == loadedState.nextPlayer.name)
            }
            .toSet()
    val upwardRightNodes =
        org.example
            .findFirstNode(
                currentNode = node,
                line = lines.upwardRightLines.first(),
                orientation = LineOrientation.UPWARD_RIGHT,
                isNextNodeVacant = false,
                isNextNodeOccupied = true,
            )
            .filter {
              it?.piece?.type == pieceType &&
                  (it.piece?.colorName != loadedState.currentPlayer.name ||
                      it.piece?.stackedPieces?.lastOrNull()?.colorName == loadedState.nextPlayer.name)
            }
            .toSet()
    val downwardRightNodes =
        org.example
            .findFirstNode(
                currentNode = node,
                line = lines.downwardRightLines.first(),
                orientation = LineOrientation.DOWNWARD_RIGHT,
                isNextNodeVacant = false,
                isNextNodeOccupied = true,
            )
            .filter {
              it?.piece?.type == pieceType &&
                  (it.piece?.colorName != loadedState.currentPlayer.name ||
                      it.piece?.stackedPieces?.lastOrNull()?.colorName == loadedState.nextPlayer.name)
            }
            .toSet()

    // TODO add logic for stacked pieces

    val eligiblePotentialTargetNodes = verticalNodes + upwardRightNodes + downwardRightNodes
  }
}
