package org.example.engine

import kotlin.collections.contains
import kotlin.collections.plus
import org.example.model.Board
import org.example.model.LineOrientation
import org.example.model.Lines
import org.example.model.Node
import org.example.model.PieceType
import org.example.model.PushDirection
import org.example.model.State
import org.example.model.getNeighborFromPushDirection
import org.example.model.getNeighbours
import org.example.model.updateBoard

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

// TODO fix bug where the target nodes set includes the source node
fun getEligiblePotentialMoves(state: State): Map<Node, Set<Node?>> {
  // return Map<Node – the node containing a piece with potential
  // Set<Node?>> – set of eligible nodes that a potential can be placed on
  val eligibleNodes =
      state.board.nodes.filter {
        it.piece != null &&
            it.piece?.type !in setOf(PieceType.GIPF, PieceType.TAMSK) &&
            it.piece?.potential == true &&
            it.piece?.colorName == state.currentPlayer.name &&
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
        requireNotNull(state.lines)
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
              val verticalNodes =
                  findFirstNode(
                      currentNode = eligibleNode,
                      line = eligibleNodesLines.verticalLines.first(),
                      orientation = LineOrientation.VERTICAL,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )
              val upwardRightNodes =
                  findFirstNode(
                      currentNode = eligibleNode,
                      line = eligibleNodesLines.upwardRightLines.first(),
                      orientation = LineOrientation.UPWARD_RIGHT,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )
              val downwardRightNodes =
                  findFirstNode(
                      currentNode = eligibleNode,
                      line = eligibleNodesLines.downwardRightLines.first(),
                      orientation = LineOrientation.DOWNWARD_RIGHT,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )

              val allNodes = verticalNodes + upwardRightNodes + downwardRightNodes

              // remove all nulls and neighbor nodes as the potential must jump over at least one
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
              val verticalNodes =
                  findAdjacentVacantNodes(
                      currentNode = eligibleNode,
                      line = eligibleNodesLines.verticalLines.first(),
                      orientation = LineOrientation.VERTICAL,
                      isNextNodeVacant = false,
                      isNextNodeOccupied = true,
                  )
              val upwardRightNodes =
                  findAdjacentVacantNodes(
                      currentNode = eligibleNode,
                      line = eligibleNodesLines.upwardRightLines.first(),
                      orientation = LineOrientation.UPWARD_RIGHT,
                      isNextNodeVacant = false,
                      isNextNodeOccupied = true,
                  )
              val downwardRightNodes =
                  findAdjacentVacantNodes(
                      currentNode = eligibleNode,
                      line = eligibleNodesLines.downwardRightLines.first(),
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
             * - Unlike a stack of 2 identical/same-color potentials, a multicolor DVONN-stack is
             *   NOT treated as a single entity.
             * - When a multicolor stack is part of a resolved row, ONLY the top piece is removed.
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
              val verticalNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = eligibleNodesLines.verticalLines.first(),
                          orientation = LineOrientation.VERTICAL,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name
                              }
                            }
                      }
                      .toSet()

              val upwardRightNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = eligibleNodesLines.upwardRightLines.first(),
                          orientation = LineOrientation.UPWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name
                              }
                            }
                      }
                      .toSet()

              val downwardRightNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = eligibleNodesLines.downwardRightLines.first(),
                          orientation = LineOrientation.DOWNWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name
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
              val verticalNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = eligibleNodesLines.verticalLines.first(),
                          orientation = LineOrientation.VERTICAL,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name
                              }
                            }
                      }
                      .toSet()

              val upwardRightNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = eligibleNodesLines.upwardRightLines.first(),
                          orientation = LineOrientation.UPWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name
                              }
                            }
                      }
                      .toSet()

              val downwardRightNodes =
                  findFirstNode(
                          currentNode = eligibleNode,
                          line = eligibleNodesLines.downwardRightLines.first(),
                          orientation = LineOrientation.DOWNWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filterNotNull()
                      .filter {
                        it.piece?.type == pieceType &&
                            run {
                              if (it.piece?.isNeutralized == false) {
                                it.piece?.colorName != state.currentPlayer.name
                              } else {
                                it.piece?.stackedPieces?.lastOrNull()?.colorName ==
                                    state.nextPlayer.name
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
      .filter { (_, value) -> value.isNotEmpty() }
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

  return if ((nextNode.piece == null) == isNextNodeVacant) {
    nextNode
  } else if ((nextNode.piece != null) == isNextNodeOccupied) {
    nextNode
  } else {
    getNextNode(
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


fun shiftPiece(currentNode: Node, moveDirection: PushDirection, board: Board, lines: Lines): Board {
  val currentCoordStr = "${currentNode.coordinate.column}${currentNode.coordinate.row}"
  val pieceDesc = currentNode.piece?.let { "${it.colorName} ${it.type.name}" } ?: "EMPTY"

  if (pieceDesc == "EMPTY") {
    require(currentNode.piece != null) { "Piece ${currentNode.piece} is null" }
  }

  logger.info { "[SHIFT] Processing node $currentCoordStr ($pieceDesc) pushing $moveDirection" }

  val targetCoordinate = currentNode.neighbors?.getNeighborFromPushDirection(moveDirection)
  val nextNode = board.nodes.firstOrNull { node -> node.coordinate == targetCoordinate }

  if (nextNode == null) {
    logger.info { "[SHIFT INFO] Shift halted: No neighbor found for $currentCoordStr in direction $moveDirection (Edge of track reached)." }
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
    logger.info { "[SHIFT WARN] Move blocked: Line tracking through $currentCoordStr is completely full. No pieces shifted." }
    return board
  }

  if (nextNode.piece == null) {
    logger.info { "[SHIFT ACTION] Empty space found at $nextCoordStr. Sliding piece from $currentCoordStr -> $nextCoordStr." }

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

    logger.info { "" + ("[SHIFT] Grid configuration post-shift step:") }
//    newBoard.printHexGrid("POST-SHIFT")

    return newBoard
  } else {
    val nextPieceDesc = "${nextNode.piece?.colorName} ${nextNode.piece?.type?.name}"
    logger.info { "[SHIFT CASCADE] Collision at $nextCoordStr ($nextPieceDesc). Initiating recursive push..." }

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

