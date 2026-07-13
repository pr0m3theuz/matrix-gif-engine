package org.example.engine

import kotlin.collections.contains
import kotlin.collections.forEach
import kotlin.collections.plus
import kotlin.random.Random
import kotlinx.serialization.json.Json
import org.example.model.Bitboard
import org.example.model.Board
import org.example.model.ColumnInfo
import org.example.model.Coordinate
import org.example.model.LineOrientation
import org.example.model.Lines
import org.example.model.Node
import org.example.model.NodeConnections
import org.example.model.Piece
import org.example.model.PieceType
import org.example.model.Player
import org.example.model.PushDirection
import org.example.model.RetrievedCapturedPieceNode
import org.example.model.State
import org.example.model.assertPieceCount
import org.example.model.constructLines
import org.example.model.getLinesWithSpaces
import org.example.model.getNeighborFromPushDirection
import org.example.model.getNeighbours
import org.example.model.printHexGrid
import org.example.model.updateBoard

// TODO fix bug where the target nodes set includes the source node
fun getEligiblePotentialMoves(state: State): Map<Node, Set<Node?>> {
  // return Map<Node – the node containing a piece with potential
  // Set<Node?>> – set of eligible nodes that a potential can be placed on
  val eligibleNodes =
      state.board.nodes.filter {
        it.piece != null &&
            it.piece?.type !in setOf(PieceType.GIPF, PieceType.TAMSK) &&
            it.piece?.potential == true &&
            it.piece?.colorName == state.currentPlayer.name.name &&
            it.piece?.isNeutralized == false
      }

  check(eligibleNodes.none { it.piece?.type in setOf(PieceType.GIPF, PieceType.TAMSK) }) {
    val invalidPieces =
        eligibleNodes
            .mapNotNull { it.piece }
            .filter { it.type in setOf(PieceType.GIPF, PieceType.TAMSK) }
            .map { "${it.type} (${it.colorName})" }

    "Eligibility violation: Found prohibited basic pieces in the selection pool: $invalidPieces"
  }

  return eligibleNodes
      .associateWith { eligibleNode ->
        val eligibleNodesLines = state.lines.getLinesContainingNode(eligibleNode)

        when (eligibleNode.piece?.type) {
          // PieceType.TAMSK logic is handled by isTamskPieceAtCenter()
          PieceType.ZERTZ -> {
            /**
             * 1/ The ZÈRTZ-potential has the ability to jump over other pieces.
             *
             * 2/ Take the top piece from a ZÈRTZ-stack and use it to jump over one or more pieces.
             * It can jump over both your own and your opponent’s pieces.
             *
             * 3/ The potential must jump over at least one piece. If it jumps over more than one
             * piece, these must all be on the same line.
             *
             * 4/ A jump with a ZÈRTZ-potential always ends on the first vacant spot in the
             * direction of the jump; it cannot jump over empty spots.
             */
            /**
             * TODO
             * 1. What nodes is this piece on?
             * 2. What lines is this node in?
             * 3. Are the neighboring nodes occupied?
             * 4. Recursively find occupied nodes (node.piece != null) until a vacant node
             *    (node.piece == null) from each line
             * 5. These nodes are eligible for placing a ZERTZ Piece on
             */
            val result = run {
              val neighbours = eligibleNode.neighbors?.getNeighbours()
              val lines = eligibleNodesLines
              val verticalNodes =
                  findFirstNode(
                      currentNode = eligibleNode,
                      line = lines.verticalLines.first(),
                      orientation = LineOrientation.VERTICAL,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )
              val upwardRightNodes =
                  findFirstNode(
                      currentNode = eligibleNode,
                      line = lines.upwardRightLines.first(),
                      orientation = LineOrientation.UPWARD_RIGHT,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )
              val downwardRightNodes =
                  findFirstNode(
                      currentNode = eligibleNode,
                      line = lines.downwardRightLines.first(),
                      orientation = LineOrientation.DOWNWARD_RIGHT,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )

              val allNodes = verticalNodes + upwardRightNodes + downwardRightNodes

              // remove all nulls and neighbour nodes as the potential must jump over at least one
              // piece.
              allNodes
                  .filterNotNull()
                  .filter { it -> neighbours?.contains(it.coordinate) == false }
                  .toSet()
            }

            result
          }
          PieceType.YINSH -> {
            /**
             * 1/ The YINSH-potential has the ability to move along the lines on the board. It may
             * be moved onto an adjacent vacant spot or to any vacant spot that it can reach in a
             * straight line without jumping over pieces.
             */
            /**
             * TODO
             * 1. What nodes is this piece on?
             * 2. What lines is this node in?
             * 3. Are the neighboring nodes vacant?
             * 4. Recursively add vacant nodes (node.piece == null) until an occupied node is found
             *    (node.piece != null) from each line
             * 5. These nodes are eligible for placing a YINSH Piece on
             */
            val result = run {
              val lines = eligibleNodesLines
              val verticalNodes =
                  findAdjacentVacantNodes(
                      currentNode = eligibleNode,
                      line = lines.verticalLines.first(),
                      orientation = LineOrientation.VERTICAL,
                      isNextNodeVacant = false,
                      isNextNodeOccupied = true,
                  )
              val upwardRightNodes =
                  findAdjacentVacantNodes(
                      currentNode = eligibleNode,
                      line = lines.upwardRightLines.first(),
                      orientation = LineOrientation.UPWARD_RIGHT,
                      isNextNodeVacant = false,
                      isNextNodeOccupied = true,
                  )
              val downwardRightNodes =
                  findAdjacentVacantNodes(
                      currentNode = eligibleNode,
                      line = lines.downwardRightLines.first(),
                      orientation = LineOrientation.DOWNWARD_RIGHT,
                      isNextNodeVacant = false,
                      isNextNodeOccupied = true,
                  )

              (verticalNodes + upwardRightNodes + downwardRightNodes)
                  .filterNotNull()
                  .filter { it.coordinate != eligibleNode.coordinate }
                  .toSet()
            }

            result
          }
          PieceType.DVONN -> {
            /**
             * ============================================================================
             * MOVEMENT AND INTERACTION RULES: DVONN-POTENTIALS
             * ============================================================================
             * * 1. MOVEMENT & JUMPING RESTRICTIONS
             * - A DVONN-potential can ONLY jump onto a DVONN-potential of the OPPOSITE color.
             * - Valid targets include:
             * - A single DVONN-potential on the board.
             * - A DVONN-stack.
             * - A DVONN-potential currently sitting on top of a stack.
             * - Pathing:
             * - Can jump to an adjacent spot.
             * - Can move in a straight line over EMPTY spots (cannot jump over other pieces).
             * - Note: Unlike 'GIPF With Potentials', a DVONN-potential CANNOT jump onto a basic
             *   GIPF piece.
             * * 2. STACK MECHANICS
             * - Because players can jump onto opposing DVONN-potentials, stacks of alternating
             *   colors will form.
             * - Unlike a stack of 2 identical/same-color potentials, a multi-color DVONN-stack is
             *   NOT treated as a single entity.
             * - When a multi-color stack is part of a resolved row, ONLY the top piece is removed.
             * * 3. NEUTRALIZATION & OCCUPATION
             * - Target pieces are neutralized when jumped on, and remain neutralized while covered.
             * - The top piece's color controls/occupies that board spot.
             * - When the top piece is removed, the piece directly beneath it is liberated and
             *   returns to active play.
             * * 4. EDGE CASE: ROW-OF-4 RESOLUTION
             * - Removing a top DVONN-potential can un-neutralize the piece beneath it, potentially
             *   creating a new row-of-4.
             * - Removing an opponent's piece to reveal your own can complete your row-of-4.
             * - Removing your own piece to reveal an opponent's can complete their row-of-4.
             * - Turn Resolution Logic:
             * - A player's turn is NOT over if they still have a row that needs removal.
             * - Rows must be removed one at a time.
             * - Active player's turn ends ONLY when no more rows of their color remain.
             * - If an opponent begins their turn with an existing row of their color on the board
             *   (caused by the previous player's move), they MUST remove it before making a
             *   standard move.
             */
            /**
             * TODO
             * 1. What nodes is this piece on?
             * 2. What lines is this node in?
             * 3. Are the neighboring nodes vacant?
             * 4. Recursively find vacant nodes (node.piece == null) until an occupied node is found
             *    (node.piece != null) from each line
             * 5. Filter for nodes where node.piece?.type == PieceType.DVONN
             */
            val pieceType = PieceType.DVONN

            val result = run {
              val lines = eligibleNodesLines
              val verticalNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = lines.verticalLines.first(),
                          orientation = LineOrientation.VERTICAL,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name.name
                              }
                            }
                      }
                      .toSet()

              val upwardRightNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = lines.upwardRightLines.first(),
                          orientation = LineOrientation.UPWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name.name
                              }
                            }
                      }
                      .toSet()

              val downwardRightNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = lines.downwardRightLines.first(),
                          orientation = LineOrientation.DOWNWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name.name
                              }
                            }
                      }
                      .toSet()

              verticalNodes + upwardRightNodes + downwardRightNodes
            }

            result
          }
          PieceType.PUNCT -> {
            /**
             * The PÜNCT-potential is used just like the DVONN-potential, except that it can only
             * jump onto an opponent’s PÜNCT-potential. So their function is the same, but each
             * potential can only target potentials of its own type.
             */
            /**
             * TODO
             * 1. What nodes is this piece on?
             * 2. What lines is this node in?
             * 3. Are the neighboring nodes vacant?
             * 4. Recursively find vacant nodes (node.piece == null) until an occupied node is found
             *    (node.piece != null) from each line
             * 5. Filter for nodes where node.piece?.type == PieceType.PUNCT
             */
            val pieceType = PieceType.PUNCT

            val result = run {
              val lines = eligibleNodesLines
              val verticalNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = lines.verticalLines.first(),
                          orientation = LineOrientation.VERTICAL,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name.name
                              }
                            }
                      }
                      .toSet()

              val upwardRightNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = lines.upwardRightLines.first(),
                          orientation = LineOrientation.UPWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name.name
                              }
                            }
                      }
                      .toSet()

              val downwardRightNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = lines.downwardRightLines.first(),
                          orientation = LineOrientation.DOWNWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name.name
                              }
                            }
                      }
                      .toSet()

              verticalNodes + upwardRightNodes + downwardRightNodes
            }

            result
          }
          else -> {
            return mapOf()
          }
        }
      }
      .filter { (key, value) -> value.isNotEmpty() }
}

fun findFirstNode(
    currentNode: Node,
    line: Set<Node>,
    orientation: LineOrientation,
    isNextNodeVacant: Boolean,
    isNextNodeOccupied: Boolean,
): Set<Node?> {
  when (orientation) {
    LineOrientation.VERTICAL -> {
      return setOf(
          getNextNode(
              node = currentNode,
              line = line,
              direction = PushDirection.UP,
              isNextNodeVacant = isNextNodeVacant,
              isNextNodeOccupied = isNextNodeOccupied,
          ),
          getNextNode(
              node = currentNode,
              line = line,
              direction = PushDirection.DOWN,
              isNextNodeVacant = isNextNodeVacant,
              isNextNodeOccupied = isNextNodeOccupied,
          ),
      )
    }
    LineOrientation.UPWARD_RIGHT -> {
      return setOf(
          getNextNode(
              node = currentNode,
              line = line,
              direction = PushDirection.UPPER_RIGHT,
              isNextNodeVacant = isNextNodeVacant,
              isNextNodeOccupied = isNextNodeOccupied,
          ),
          getNextNode(
              node = currentNode,
              line = line,
              direction = PushDirection.LOWER_LEFT,
              isNextNodeVacant = isNextNodeVacant,
              isNextNodeOccupied = isNextNodeOccupied,
          ),
      )
    }
    LineOrientation.DOWNWARD_RIGHT -> {
      return setOf(
          getNextNode(
              node = currentNode,
              line = line,
              direction = PushDirection.UPPER_LEFT,
              isNextNodeVacant = isNextNodeVacant,
              isNextNodeOccupied = isNextNodeOccupied,
          ),
          getNextNode(
              node = currentNode,
              line = line,
              direction = PushDirection.LOWER_RIGHT,
              isNextNodeVacant = isNextNodeVacant,
              isNextNodeOccupied = isNextNodeOccupied,
          ),
      )
    }
  }
}

fun findAdjacentVacantNodes(
    currentNode: Node,
    line: Set<Node>,
    orientation: LineOrientation,
    isNextNodeVacant: Boolean,
    isNextNodeOccupied: Boolean,
): Set<Node?> {
  val vacantNodes: MutableSet<Node?> = mutableSetOf()

  when (orientation) {
    LineOrientation.VERTICAL -> {
      getAdjacentVacantNode(
          node = currentNode,
          line = line,
          direction = PushDirection.UP,
          isNextNodeVacant = isNextNodeVacant,
          isNextNodeOccupied = isNextNodeOccupied,
          vacantNodes = vacantNodes,
      )
      getAdjacentVacantNode(
          node = currentNode,
          line = line,
          direction = PushDirection.DOWN,
          isNextNodeVacant = isNextNodeVacant,
          isNextNodeOccupied = isNextNodeOccupied,
          vacantNodes = vacantNodes,
      )
    }
    LineOrientation.UPWARD_RIGHT -> {
      getAdjacentVacantNode(
          node = currentNode,
          line = line,
          direction = PushDirection.UPPER_RIGHT,
          isNextNodeVacant = isNextNodeVacant,
          isNextNodeOccupied = isNextNodeOccupied,
          vacantNodes = vacantNodes,
      )
      getAdjacentVacantNode(
          node = currentNode,
          line = line,
          direction = PushDirection.LOWER_LEFT,
          isNextNodeVacant = isNextNodeVacant,
          isNextNodeOccupied = isNextNodeOccupied,
          vacantNodes = vacantNodes,
      )
    }
    LineOrientation.DOWNWARD_RIGHT -> {
      getAdjacentVacantNode(
          node = currentNode,
          line = line,
          direction = PushDirection.UPPER_LEFT,
          isNextNodeVacant = isNextNodeVacant,
          isNextNodeOccupied = isNextNodeOccupied,
          vacantNodes = vacantNodes,
      )
      getAdjacentVacantNode(
          node = currentNode,
          line = line,
          direction = PushDirection.LOWER_RIGHT,
          isNextNodeVacant = isNextNodeVacant,
          isNextNodeOccupied = isNextNodeOccupied,
          vacantNodes = vacantNodes,
      )
    }
  }

  return vacantNodes
}

fun getNextNode(
    node: Node?,
    line: Set<Node>,
    direction: PushDirection,
    isNextNodeVacant: Boolean,
    isNextNodeOccupied: Boolean,
): Node? {
  if (node == null) return null

  val nextNode: Node? =
      when (direction) {
        PushDirection.UP -> {
          line.firstOrNull { it: Node -> node.neighbors?.above?.equals(it.coordinate) ?: false }
        }
        PushDirection.DOWN -> {
          line.firstOrNull { node.neighbors?.below?.equals(it.coordinate) ?: false }
        }

        PushDirection.UPPER_RIGHT -> {
          line.firstOrNull { node.neighbors?.upperRight?.equals(it.coordinate) ?: false }
        }

        PushDirection.LOWER_RIGHT -> {
          line.firstOrNull { node.neighbors?.lowerRight?.equals(it.coordinate) ?: false }
        }

        PushDirection.UPPER_LEFT -> {
          line.firstOrNull { node.neighbors?.upperLeft?.equals(it.coordinate) ?: false }
        }

        PushDirection.LOWER_LEFT -> {
          line.firstOrNull { node.neighbors?.lowerLeft?.equals(it.coordinate) ?: false }
        }
      }

  if (nextNode == null) return null

  if ((nextNode.piece == null) == isNextNodeVacant) {
    return nextNode
  } else if ((nextNode.piece != null) == isNextNodeOccupied) {
    return nextNode
  } else {
    return getNextNode(
        node = nextNode,
        line = line,
        direction = direction,
        isNextNodeVacant = isNextNodeVacant,
        isNextNodeOccupied = isNextNodeOccupied,
    )
  }
}

fun getAdjacentVacantNode(
    node: Node?,
    line: Set<Node>,
    direction: PushDirection,
    isNextNodeVacant: Boolean,
    isNextNodeOccupied: Boolean,
    vacantNodes: MutableSet<Node?> = mutableSetOf(),
): MutableSet<Node?> {
  if (node == null) return vacantNodes

  val nextNode: Node? =
      when (direction) {
        PushDirection.UP -> {
          line.firstOrNull { node.neighbors?.above?.equals(it.coordinate) ?: false }
        }
        PushDirection.DOWN -> {
          line.firstOrNull { node.neighbors?.below?.equals(it.coordinate) ?: false }
        }

        PushDirection.UPPER_RIGHT -> {
          line.firstOrNull { node.neighbors?.upperRight?.equals(it.coordinate) ?: false }
        }

        PushDirection.LOWER_RIGHT -> {
          line.firstOrNull { node.neighbors?.lowerRight?.equals(it.coordinate) ?: false }
        }

        PushDirection.UPPER_LEFT -> {
          line.firstOrNull { node.neighbors?.upperLeft?.equals(it.coordinate) ?: false }
        }

        PushDirection.LOWER_LEFT -> {
          line.firstOrNull { node.neighbors?.lowerLeft?.equals(it.coordinate) ?: false }
        }
      }

  if (nextNode == null) return vacantNodes

  if ((nextNode.piece != null) == isNextNodeOccupied) {
    vacantNodes.add(node)
    return vacantNodes
  } else {
    vacantNodes.add(nextNode)
    getAdjacentVacantNode(
        node = nextNode,
        line = line,
        direction = direction,
        isNextNodeVacant = isNextNodeVacant,
        isNextNodeOccupied = isNextNodeOccupied,
        vacantNodes = vacantNodes,
    )

    return vacantNodes
  }
}

fun pushPiece(
    currentNode: Node,
    moveDirection: PushDirection,
    coordinateToMoveTo: Coordinate,
    validCoordinates: List<Coordinate>,
    board: Board,
): Board {
  //  TODO("Implement this function to complete the task")

  if (!validCoordinates.contains(coordinateToMoveTo)) {
    return board
  }

  val newNode = board.nodes.first { it.coordinate == coordinateToMoveTo }

  if (newNode.piece == null) {
    newNode.piece = currentNode.piece
    currentNode.piece = null

    var newBoard = updateBoard(newNode, board)
    newBoard = updateBoard(currentNode, board)
    return newBoard
  } else {
    TODO("Shift Pieces")
  }

  return board
}

// fun shiftPiece(currentNode: Node, moveDirection: PushDirection, board: Board, lines: Lines):
// Board {
//	println("Shifting Pieces")
//  val nextNode =
//      board.nodes.firstOrNull { node ->
//        node.coordinate == currentNode.neighbors?.getNeighborFromPushDirection(moveDirection)
//      }
//
//  if (nextNode == null) {
//    return board
//  }
//
//  val line =
//      when (moveDirection) {
//        PushDirection.UP -> {
//          lines.verticalLines.filter { nodes ->
//	          nodes.any {node -> node.coordinate == currentNode.coordinate || node.coordinate ==
// nextNode.coordinate }
//					}
//        }
//        PushDirection.DOWN -> {
//          lines.verticalLines.filter { nodes ->
//	          nodes.any {node -> node.coordinate == currentNode.coordinate || node.coordinate ==
// nextNode.coordinate }
//					}
//        }
//        PushDirection.UPPER_RIGHT -> {
//          lines.upwardRightLines.filter { nodes ->
//	          nodes.any {node -> node.coordinate == currentNode.coordinate || node.coordinate ==
// nextNode.coordinate }
//          }
//        }
//        PushDirection.LOWER_RIGHT -> {
//          lines.downwardRightLines.filter { nodes ->
//            nodes.any {node -> node.coordinate == currentNode.coordinate || node.coordinate ==
// nextNode.coordinate }
//          }
//        }
//        PushDirection.UPPER_LEFT -> {
//          lines.downwardRightLines.filter { nodes ->
//	          nodes.any {node -> node.coordinate == currentNode.coordinate || node.coordinate ==
// nextNode.coordinate }
//          }
//        }
//        PushDirection.LOWER_LEFT -> {
//          lines.upwardRightLines.filter { nodes ->
//	          nodes.any {node -> node.coordinate == currentNode.coordinate || node.coordinate ==
// nextNode.coordinate }
//          }
//        }
//      }
//
//  check(line.size == 1) { "Line size ${line.size} not equal to 1" }
//
//  // Is there empty space in this line else return board
//  if (line.first().count { it.piece == null } == 0) {
//    return board
//  }
//
//  if (nextNode.piece == null) {
//    nextNode.piece = currentNode.piece
//    currentNode.piece = null
//
//    var newBoard = updateBoard(nextNode, board)
//    newBoard = updateBoard(currentNode, board)
//
//	  newBoard.printHexGrid()
//
//    return newBoard
//  } else {
//    return shiftPiece(
//        currentNode = nextNode,
//        moveDirection = moveDirection,
//        board = board,
//        lines = lines,
//    )
//  }
// }

fun shiftPiece(currentNode: Node, moveDirection: PushDirection, board: Board, lines: Lines): Board {
  val currentCoordStr = "${currentNode.coordinate.column}${currentNode.coordinate.row}"
  val pieceDesc = currentNode.piece?.let { "${it.colorName} ${it.type.name}" } ?: "EMPTY"

  if (pieceDesc == "EMPTY") {
    require(currentNode.piece != null) { "Piece ${currentNode.piece} is null" }
  }

  println("[SHIFT] Processing node $currentCoordStr ($pieceDesc) pushing $moveDirection")

  val targetCoordinate = currentNode.neighbors?.getNeighborFromPushDirection(moveDirection)
  val nextNode = board.nodes.firstOrNull { node -> node.coordinate == targetCoordinate }

  if (nextNode == null) {
    println(
        "[SHIFT INFO] Shift halted: No neighbor found for $currentCoordStr in direction $moveDirection (Edge of track reached)."
    )
    return board
  }

  val nextCoordStr = "${nextNode.coordinate.column}${nextNode.coordinate.row}"

  val line =
      when (moveDirection) {
        PushDirection.UP,
        PushDirection.DOWN -> {
          lines.verticalLines.filter { nodes ->
            nodes.any { node ->
              node.coordinate == currentNode.coordinate || node.coordinate == nextNode.coordinate
            }
          }
        }
        PushDirection.UPPER_RIGHT,
        PushDirection.LOWER_LEFT -> {
          lines.upwardRightLines.filter { nodes ->
            nodes.any { node ->
              node.coordinate == currentNode.coordinate || node.coordinate == nextNode.coordinate
            }
          }
        }
        PushDirection.LOWER_RIGHT,
        PushDirection.UPPER_LEFT -> {
          lines.downwardRightLines.filter { nodes ->
            nodes.any { node ->
              node.coordinate == currentNode.coordinate || node.coordinate == nextNode.coordinate
            }
          }
        }
      }

  // State Validation
  check(line.size == 1) {
    "Invalid Board Topology: Expected exactly 1 lines instance linking $currentCoordStr and $nextCoordStr for direction $moveDirection, but found ${line.size}."
  }

  // Is there empty space in this line else return board
  if (line.first().count { it.piece == null } == 0) {
    println(
        "[SHIFT WARN] Move blocked: Line tracking through $currentCoordStr is completely full. No pieces shifted."
    )
    return board
  }

  if (nextNode.piece == null) {
    println(
        "[SHIFT ACTION] Empty space found at $nextCoordStr. Sliding piece from $currentCoordStr -> $nextCoordStr."
    )

    nextNode.piece = currentNode.piece

    var newBoard = updateBoard(nextNode, board)

    val targetNextNodeCoordinate = nextNode.coordinate
    check(newBoard.nodes.first { it.coordinate == targetNextNodeCoordinate }.piece != null) {
      "State corruption: Node at coordinate $targetNextNodeCoordinate was expected to contain a piece, but it is empty."
    }

    currentNode.piece = null
    newBoard = updateBoard(currentNode, newBoard)

    val sourceCoordinate = currentNode.coordinate
    check(newBoard.nodes.first { it.coordinate == sourceCoordinate }.piece == null) {
      "State corruption: Node at coordinate $sourceCoordinate was expected to be empty, but it still contains a piece."
    }

    println("[SHIFT] Grid configuration post-shift step:")
    newBoard.printHexGrid("POST-SHIFT")

    return newBoard
  } else {
    val nextPieceDesc = "${nextNode.piece?.colorName} ${nextNode.piece?.type?.name}"
    println(
        "[SHIFT CASCADE] Collision at $nextCoordStr ($nextPieceDesc). Initiating recursive push..."
    )

    val newBoard =
        shiftPiece(
            currentNode = nextNode,
            moveDirection = moveDirection,
            board = board,
            lines = lines,
        )

    return shiftPiece(
        currentNode = currentNode,
        moveDirection = moveDirection,
        board = newBoard,
        lines = lines,
    )
  }
}

fun usePotentialMovePiece(
    currentNode: Node,
    coordinateToMoveTo: Coordinate,
    validCoordinates: List<Coordinate>,
    board: Board,
): Board {
  if (!validCoordinates.contains(coordinateToMoveTo)) {
    return board
  }

  val newNode = board.nodes.first { it.coordinate == coordinateToMoveTo }

  if (newNode.piece == null) {
    newNode.piece = currentNode.piece?.usePiecePotential()

    var newBoard = updateBoard(newNode, board)
    newBoard = updateBoard(currentNode, newBoard)
    return newBoard
  }

  return board
}

fun retrieveAndCapturePieces(
    player: Player,
    line: Set<Node>, // TODO Replace with Line
    removePiecesWithPotential: Boolean,
    // TODO Add intersectingNodes to chooseToRemovePiecesWithPotential()
): List<RetrievedCapturedPieceNode> {
  var retrievedPieces = listOf<RetrievedCapturedPieceNode>()
  var capturedPieces = listOf<RetrievedCapturedPieceNode>()

  // TODO "Implement logic for handling nodes where pieces are stacked and remove the stacked pieces
  // and leave the underlying pieces, i.e. node.piece"
  /**
   * ============================================================================
   * MOVEMENT AND INTERACTION RULES: DVONN & PUNCT POTENTIALS
   * ============================================================================
   * * 1. MOVEMENT & JUMPING RESTRICTIONS
   * - A DVONN-potential can ONLY jump onto a DVONN-potential of the OPPOSITE color.
   * - Valid targets include:
   * - A single DVONN-potential on the board.
   * - A DVONN-stack.
   * - A DVONN-potential currently sitting on top of a stack.
   * - Pathing:
   * - Can jump to an adjacent spot.
   * - Can move in a straight line over EMPTY spots (cannot jump over other pieces).
   * - Note: Unlike 'GIPF With Potentials', a DVONN-potential CANNOT jump onto a basic GIPF piece.
   *
   * * 2. STACK MECHANICS
   * - Because players can jump onto opposing DVONN-potentials, stacks of alternating colors will
   *   form.
   * - Unlike a stack of 2 identical/same-color potentials, a multi-color DVONN-stack is NOT treated
   *   as a single entity.
   * - When a multi-color stack is part of a resolved row, ONLY the top piece is removed.
   *
   * * 3. NEUTRALIZATION & OCCUPATION
   * - Target pieces are neutralized when jumped on, and remain neutralized while covered.
   * - The top piece's color controls/occupies that board spot.
   * - When the top piece is removed, the piece directly beneath it is liberated and returns to
   *   active play.
   *
   * * 4. EDGE CASE: ROW-OF-4 RESOLUTION
   * - Removing a top DVONN-potential can un-neutralize the piece beneath it, potentially creating a
   *   new row-of-4.
   * - Removing an opponent's piece to reveal your own can complete your row-of-4.
   * - Removing your own piece to reveal an opponent's can complete their row-of-4.
   * - Turn Resolution Logic:
   * - A player's turn is NOT over if they still have a row that needs removal.
   * - Rows must be removed one at a time.
   * - Active player's turn ends ONLY when no more rows of their color remain.
   * - If an opponent begins their turn with an existing row of their color on the board (caused by
   *   the previous player's move), they MUST remove it before making a standard move.
   */
  val validTypes = setOf(PieceType.DVONN, PieceType.PUNCT)

  retrievedPieces =
      line
          .filter { node ->
            if (node.piece == null) {
              false
            } else if (node.piece?.isNeutralized == true) {
              check(node.piece?.type in validTypes) {
                "Expected DVONN or PUNCT piece, but found: ${node.piece?.type ?: "Empty Node/No Piece"}"
              }
              node.piece?.stackedPieces?.last()?.colorName == player.name.name
            } else {
              node.piece?.colorName == player.name.name
            }
          }
          .map { node ->
            // TODO replace with Piece.removePiece()
            // TODO figure out how to remove piece from a node. either return the current piece
            // (less the last stacked piece) or null
            //  how does these functions interact with each other
            if (node.piece?.isNeutralized == true) {
              check(node.piece?.type in validTypes) {
                "Expected DVONN or PUNCT piece, but found: ${node.piece?.type ?: "Empty Node/No Piece"}"
              }

              RetrievedCapturedPieceNode(
                  retrievedPiece = node.piece?.stackedPieces?.last(),
                  node = node,
                  isStackedPieceNode = true,
              )
            } else {
              RetrievedCapturedPieceNode(
                  retrievedPiece = node.piece,
                  node = node,
                  isStackedPieceNode = false,
              )
            }
          }
          .toMutableList()

  check(retrievedPieces.all { piece -> piece.retrievedPiece?.colorName == player.name.name }) {
    val invalidColors =
        retrievedPieces.map { it.retrievedPiece?.colorName }.filter { it != player.name.name }.distinct()

    "Sanity check failed: Player '${player.name}' cannot retrieve pieces belonging to: $invalidColors"
  }

  // TODO Check if all pieces have their potentials then at least one piece has to be removed
  //	if (removePiecesWithPotential) true else node.piece!!.hasNoPotential()
  if (retrievedPieces.all { it.retrievedPiece?.potential == true }) {
    //		TODO("Implement function to select which pieces to remove,
    // chooseToRemovePiecesWithPotential()")
    // TODO remove pieces and nodes from retrievedPieces and nodesToRemovePieces that have not been
    // selected
    // TODO Replace below with better logic
    var selectedPiecesToKeepInPlay = retrievedPieces.mapNotNull { piece ->
      if (Random.nextInt(from = 0, until = 1) == 1) {
        piece
      } else {
        null
      }
    }

    // if all pieces were selected to keep in play, select a piece at random
    if (selectedPiecesToKeepInPlay.size == retrievedPieces.size) {
      selectedPiecesToKeepInPlay = listOf(retrievedPieces.random())
    }

    retrievedPieces.removeAll(selectedPiecesToKeepInPlay)
  }

  capturedPieces =
      line
          .filter { node ->
            if (node.piece == null) {
              false
            } else if (node.piece?.isNeutralized == true) {
              check(node.piece?.type in validTypes) {
                "Expected DVONN or PUNCT piece, but found: ${node.piece?.type ?: "Empty Node/No Piece"}"
              }

              node.piece?.stackedPieces?.last()?.colorName != player.name.name
            } else {
              node.piece?.colorName != player.name.name
            }
          }
          .map { node ->
            if (node.piece?.isNeutralized == true) {
              check(node.piece?.type in validTypes) {
                "Expected DVONN or PUNCT piece, but found: ${node.piece?.type ?: "Empty Node/No Piece"}"
              }

              RetrievedCapturedPieceNode(
                  capturedPiece = node.piece?.stackedPieces?.last(),
                  node = node,
                  isStackedPieceNode = true,
              )
            } else {
              RetrievedCapturedPieceNode(
                  capturedPiece = node.piece,
                  node = node,
                  isStackedPieceNode = false,
              )
            }
          }

  return retrievedPieces + capturedPieces
}

fun usePiecePotential(node: Node, eligibleNodesForPotential: Set<Node>, state: State): State {
  require(node.piece != null) { "Source node must contain a valid game piece." }

  check(node.piece!!.type !in setOf(PieceType.GIPF, PieceType.TAMSK)) {
    "Invalid source piece: ${node.piece?.type ?: "Node is empty"}. The current piece cannot be a GIPF or TAMSK piece." +
        "\n\nCurrent Node: $node" +
        "\n\nState: ${Json.encodeToString(state)}"
  }

  var newState = state.deepCopy()

  val piece = node.piece!!
  val potential = piece.usePiecePotential()

  var selectedNodeToPlacePotential: Node? = null

  check(!potential.potential && !piece.potential) {
    "State invalid: source piece and potential cannot have active potentials. " +
        "State -> Potential: ${potential.potential}, Piece: ${piece.potential}" +
        "\nCurrent Node: ${Json.encodeToString(node)}" +
        "\n\nState: ${Json.encodeToString(state)}"
  }

  when (piece.type) {
    PieceType.GIPF -> {
      TODO("Should have been handled before this function was called")
    }
    PieceType.TAMSK -> {
      TODO("Should have been handled before this function was called")
    }

    PieceType.ZERTZ -> {
      val candidateNodes = eligibleNodesForPotential.filter { it.piece == null }

      if (candidateNodes.isEmpty()) {
        check(candidateNodes.isNotEmpty()) { "Nodes are empty." }
      }

      selectedNodeToPlacePotential = candidateNodes.random()

      check(selectedNodeToPlacePotential.piece == null) {
        "Illegal placement: Cannot target an occupied node." +
            "\nCurrent Node: ${Json.encodeToString(node)}" +
            "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
            "\n\nState: ${Json.encodeToString(state)}"
      }

      selectedNodeToPlacePotential.piece = potential
    }

    PieceType.YINSH -> {
      val candidateNodes = eligibleNodesForPotential.filter { it.piece == null }

      if (candidateNodes.isEmpty()) {
        // TODO
        check(candidateNodes.isNotEmpty()) { "Nodes are empty." }
      }

      selectedNodeToPlacePotential = candidateNodes.random()

      check(selectedNodeToPlacePotential.piece == null) {
        "Illegal placement: Cannot target an occupied node." +
            "\nCurrent Node: ${Json.encodeToString(node)}" +
            "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
            "\n\nState: ${Json.encodeToString(state)}"
      }

      selectedNodeToPlacePotential.piece = potential
    }

    PieceType.DVONN -> {
      val candidateNodes = eligibleNodesForPotential.filter { it.piece?.type == PieceType.DVONN }

      if (candidateNodes.isEmpty()) {
        check(candidateNodes.isNotEmpty()) { "Nodes are empty." }
      }

      selectedNodeToPlacePotential = candidateNodes.random()

      // TODO replace with Piece.pushPotential() Function
      if (potential.type != selectedNodeToPlacePotential.piece?.type) {
        check(potential.type != selectedNodeToPlacePotential.piece?.type) {
          "Illegal placement: Cannot target a piece of a different type (${potential.type})." +
              "\nCurrent Node: ${Json.encodeToString(node)}" +
              "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
              "\n\nState: ${Json.encodeToString(state)}"
        }
      }

      if (selectedNodeToPlacePotential.piece?.isNeutralized == false) {
        check(selectedNodeToPlacePotential.piece?.colorName != potential.colorName) {
          "Illegal placement: Cannot target a piece of your own color (${potential.colorName})." +
              "\nCurrent Node: ${Json.encodeToString(node)}" +
              "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
              "\n\nState: ${Json.encodeToString(state)}"
        }
      }

      selectedNodeToPlacePotential.piece?.stackedPieces?.let {
        if (it.size > 1) {
          val topPotential = it.last()

          check(topPotential.colorName != potential.colorName) {
            "Illegal move: A ${potential.type.name}-potential cannot jump onto another potential of the same color (${potential.colorName})." +
                "\nCurrent Node: ${Json.encodeToString(node)}" +
                "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
                "\n\nState: ${Json.encodeToString(state)}"
          }
        }
      }

      // TODO replace with Piece.pushPotential() Function
      selectedNodeToPlacePotential.piece?.stackedPieces?.add(potential)
      selectedNodeToPlacePotential.piece?.isNeutralized = true
    }

    PieceType.PUNCT -> {
      val candidateNodes = eligibleNodesForPotential.filter { it.piece?.type == PieceType.PUNCT }

      if (candidateNodes.isEmpty()) {
        check(candidateNodes.isNotEmpty()) { "Nodes are empty." }
      }

      selectedNodeToPlacePotential = candidateNodes.random()

      // TODO replace with Piece.pushPotential() Function
      if (potential.type != selectedNodeToPlacePotential.piece?.type) {
        check(potential.type != selectedNodeToPlacePotential.piece?.type) {
          "Illegal placement: Cannot target a piece of a different type (${potential.type})." +
              "\nCurrent Node: ${Json.encodeToString(node)}" +
              "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
              "\n\nState: ${Json.encodeToString(state)}"
        }
      }

      if (selectedNodeToPlacePotential.piece?.isNeutralized == false) {
        check(selectedNodeToPlacePotential.piece?.colorName != potential.colorName) {
          "Illegal placement: Cannot target a piece of your own color (${potential.colorName})." +
              "\nCurrent Node: ${Json.encodeToString(node)}" +
              "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
              "\n\nState: ${Json.encodeToString(state)}"
        }
      }

      selectedNodeToPlacePotential.piece?.stackedPieces?.let {
        if (it.size > 1) {
          val topPotential = it.last()

          check(topPotential.colorName != potential.colorName) {
            "Illegal move: A ${potential.type.name}-potential cannot jump onto another potential of the same color (${potential.colorName})." +
                "\nCurrent Node: ${Json.encodeToString(node)}" +
                "\nSelected Node To Place Potential: ${Json.encodeToString(selectedNodeToPlacePotential)}" +
                "\n\nState: ${Json.encodeToString(state)}"
          }
        }
      }

      // TODO replace with Piece.pushPotential() Function
      selectedNodeToPlacePotential.piece?.stackedPieces?.add(potential)
      selectedNodeToPlacePotential.piece?.isNeutralized = true
    }
  }

  var newBoard = updateBoard(node, newState.board)
  newBoard = updateBoard(selectedNodeToPlacePotential, newBoard)

  newState = newState.copy(board = newBoard, lines = constructLines(newBoard.nodes))

  newState.assertPieceCount()

  return newState
}

fun identifyAvailableMoves(state: State): List<PossibleMove> {
  // Does player have GIPF pieces in reserve?
  val gipfPiecesInReserve: List<Piece> =
      state.currentPlayer.piecesInReserve
          .filter { piece -> piece.type == PieceType.GIPF }
          .distinctBy { piece -> piece.type }

  val playableStackedPiecesInReserve: List<Piece> =
      state.currentPlayer.piecesInReserve
          .filter { piece ->
            piece.type != PieceType.GIPF && piece.potential
          }
          .distinctBy { piece -> piece.type }

  // TODO work on finding eligiblePotentialTargetNodes
  val eligibleMovesUsingPotential: Map<Node, Set<Node?>> = getEligiblePotentialMoves(state)

  // TODO Current Player has no moves left
  if (
      playableStackedPiecesInReserve.isEmpty() &&
          gipfPiecesInReserve.isEmpty() &&
          eligibleMovesUsingPotential.isEmpty()
  ) {
    return emptyList()
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
  // TODO can this be simplified
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

  if (selectableDots.size < populatedNodes.size) {
    println(
        "Warning: Selectable dots pool (${selectableDots.size}) is smaller than populated nodes (${populatedNodes.size})."
    )
  }

  val numberOfPiecesBefore = state.currentPlayer.getNumberOfPiecesInReserve()

  // Build a list of all available moves
  var allAvailableMoves: MutableList<PossibleMove> = mutableListOf()

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

  allAvailableMoves =
      allAvailableMoves
          .filter { possibleMove -> possibleMove.selectableDots.isNotEmpty() }
          .toMutableList()

  if (gipfPiecesInReserve.isNotEmpty()) {
    return gipfPiecesInReserve.map { piece ->
      PossibleMove(piece = piece, selectableDots = selectableDots, moveType = MoveType.AddPiece)
    }
  } else if (isTamskPieceAtCenter(state.board, state.currentPlayer)) {
    // TODO use potential
    // TODO put piece on a selectable dot shift piece

    val piece: Piece? =
        state.board.nodes
            .first {
              it.coordinate.column == state.board.centerNodeCoordinate.column &&
                  it.coordinate.row == state.board.centerNodeCoordinate.row
            }
            .piece

    val selectedNode = selectDot(selectableDots)

    check(piece?.potential == false) {
      //      val pieceCoords = selectedNode.node.coordinate.let { "${it.column}${it.row}" }
      val currentPotential = piece?.potential

      //      "Invalid piece state at $pieceCoords: Expected piece potential to be spent (false), "
      // +
      "but found potential status is: $currentPotential (Piece Type: ${piece?.type?.name}, Color: ${piece?.colorName})"
    }

    return listOf<PossibleMove>(
        PossibleMove(
            piece = piece.usePiecePotential(),
            selectableDots = selectableDots,
            moveType = MoveType.AddPiece,
        )
    )
  } else if (allAvailableMoves.isNotEmpty()) {
    return allAvailableMoves
  } else {
    return emptyList<PossibleMove>()
//    throw IllegalStateException("Player ${state.currentPlayer.name} has no available moves left!")
  }
}

fun identifyNextPlayerAvailableMoves(state: State): List<PossibleMove> {
  // Does player have GIPF pieces in reserve?
  val gipfPiecesInReserve: List<Piece> =
    state.nextPlayer.piecesInReserve
      .filter { piece -> piece.type == PieceType.GIPF }
      .distinctBy { piece -> piece.type }

  val playableStackedPiecesInReserve: List<Piece> =
    state.nextPlayer.piecesInReserve
      .filter { piece ->
        piece.type != PieceType.GIPF && piece.potential
      }
      .distinctBy { piece -> piece.type }

  // TODO work on finding eligiblePotentialTargetNodes
  val eligibleMovesUsingPotential: Map<Node, Set<Node?>> = getEligiblePotentialMoves(state)

  // TODO Current Player has no moves left
  if (
    playableStackedPiecesInReserve.isEmpty() &&
    gipfPiecesInReserve.isEmpty() &&
    eligibleMovesUsingPotential.isEmpty()
  ) {
    return emptyList()
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
  // TODO can this be simplified
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

  if (selectableDots.size < populatedNodes.size) {
    println(
      "Warning: Selectable dots pool (${selectableDots.size}) is smaller than populated nodes (${populatedNodes.size})."
    )
  }

  val numberOfPiecesBefore = state.nextPlayer.getNumberOfPiecesInReserve()

  // Build a list of all available moves
  var allAvailableMoves: MutableList<PossibleMove> = mutableListOf()

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

  allAvailableMoves =
    allAvailableMoves
      .filter { possibleMove -> possibleMove.selectableDots.isNotEmpty() }
      .toMutableList()

  if (gipfPiecesInReserve.isNotEmpty()) {
    return gipfPiecesInReserve.map { piece ->
      PossibleMove(piece = piece, selectableDots = selectableDots, moveType = MoveType.AddPiece)
    }
  } else if (isTamskPieceAtCenter(state.board, state.nextPlayer)) {
    // TODO use potential
    // TODO put piece on a selectable dot shift piece

    val piece: Piece? =
      state.board.nodes
        .first {
          it.coordinate.column == state.board.centerNodeCoordinate.column &&
              it.coordinate.row == state.board.centerNodeCoordinate.row
        }
        .piece

    val selectedNode = selectDot(selectableDots)

    check(piece?.potential == false) {
      //      val pieceCoords = selectedNode.node.coordinate.let { "${it.column}${it.row}" }
      val currentPotential = piece?.potential

      //      "Invalid piece state at $pieceCoords: Expected piece potential to be spent (false), "
      // +
      "but found potential status is: $currentPotential (Piece Type: ${piece?.type?.name}, Color: ${piece?.colorName})"
    }

    return listOf<PossibleMove>(
      PossibleMove(
        piece = piece.usePiecePotential(),
        selectableDots = selectableDots,
        moveType = MoveType.AddPiece,
      )
    )
  } else if (allAvailableMoves.isNotEmpty()) {
    return allAvailableMoves
  } else {
    //		emptyList<PossibleMove>()
    throw IllegalStateException("Player ${state.nextPlayer.name} has no available moves left!")
  }
}

fun identifyAvailableBitboardMoves(state: State, bitboard: Bitboard, columnInfo: List<ColumnInfo>): List<PossibleMove> {
	// Does player have GIPF pieces in reserve?
	val gipfPiecesInReserve: List<Piece> =
		state.currentPlayer.piecesInReserve
			.filter { piece -> piece.type == PieceType.GIPF }
			.distinctBy { piece -> piece.type }

	val playableStackedPiecesInReserve: List<Piece> =
		state.currentPlayer.piecesInReserve
			.filter { piece ->
				piece.type != PieceType.GIPF && piece.potential
			}
			.distinctBy { piece -> piece.type }

	// TODO work on finding eligiblePotentialTargetNodes
	val eligibleMovesUsingPotential: Map<Node, Set<Node?>> = getEligiblePotentialMoves(state)

	// TODO Current Player has no moves left
	if (
		playableStackedPiecesInReserve.isEmpty() &&
		gipfPiecesInReserve.isEmpty() &&
		eligibleMovesUsingPotential.isEmpty()
	) {
		return emptyList()
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
	// TODO can this be simplified
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

	if (selectableDots.size < populatedNodes.size) {
		println(
			"Warning: Selectable dots pool (${selectableDots.size}) is smaller than populated nodes (${populatedNodes.size})."
		)
	}

	val numberOfPiecesBefore = state.currentPlayer.getNumberOfPiecesInReserve()

	// Build a list of all available moves
	var allAvailableMoves: MutableList<PossibleMove> = mutableListOf()

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

	allAvailableMoves =
		allAvailableMoves
			.filter { possibleMove -> possibleMove.selectableDots.isNotEmpty() }
			.toMutableList()

	if (gipfPiecesInReserve.isNotEmpty()) {
		return gipfPiecesInReserve.map { piece ->
			PossibleMove(piece = piece, selectableDots = selectableDots, moveType = MoveType.AddPiece)
		}
	} else if (isTamskPieceAtCenter(state.board, state.currentPlayer)) {
		// TODO use potential
		// TODO put piece on a selectable dot shift piece

		val piece: Piece? =
			state.board.nodes
				.first {
					it.coordinate.column == state.board.centerNodeCoordinate.column &&
							it.coordinate.row == state.board.centerNodeCoordinate.row
				}
				.piece

		val selectedNode = selectDot(selectableDots)

		check(piece?.potential == false) {
			//      val pieceCoords = selectedNode.node.coordinate.let { "${it.column}${it.row}" }
			val currentPotential = piece?.potential

			//      "Invalid piece state at $pieceCoords: Expected piece potential to be spent (false), "
			// +
			"but found potential status is: $currentPotential (Piece Type: ${piece?.type?.name}, Color: ${piece?.colorName})"
		}

		return listOf<PossibleMove>(
			PossibleMove(
				piece = piece.usePiecePotential(),
				selectableDots = selectableDots,
				moveType = MoveType.AddPiece,
			)
		)
	} else if (allAvailableMoves.isNotEmpty()) {
		return allAvailableMoves
	} else {
		//		emptyList<PossibleMove>()
		throw IllegalStateException("Player ${state.currentPlayer.name} has no available moves left!")
	}
}