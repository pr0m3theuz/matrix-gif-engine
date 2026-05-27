package org.example

fun getEligiblePotentialMoves(state: State): Map<Node, Set<Node?>> {
  val eligibleNodes =
      state.board.nodes.filter {
        it.piece != null &&
            it.piece?.type != PieceType.GIPF &&
            it.piece?.potential == true &&
            it.piece?.colorName == state.currentPlayer.name &&
            !it.isNeutralized
      }

  return eligibleNodes.associateWith { eligibleNode ->
    val eligibleNodesLines = eligibleNodes.associateWith { eligibleNode ->
      state.lines.getLinesContainingNode(eligibleNode)
    }

    when (eligibleNode.piece?.type) {
      // PieceType.TAMSK logic is handled by isTamskPieceAtCenter()
      PieceType.ZERTZ -> {
        /**
         * 1/ The ZÈRTZ-potential has the ability to jump over other pieces. 2/ Take the top piece
         * from a ZÈRTZ-stack and use it to jump over one or more pieces. It can jump over both your
         * own and your opponent’s pieces. 3/ The potential must jump over at least one piece. If it
         * jumps over more than one piece, these must all be on the same line. 4/ A jump with a
         * ZÈRTZ-potential always ends on the first vacant spot in the direction of the jump; it
         * cannot jump over empty spots.
         */
        /**
         * TODO
         * 1. What nodes is this piece on?
         * 2. What lines is this node in?
         * 3. Are the neighboring nodes occupied?
         * 4. Recursively find occupied nodes (node.piece != null) until a vacant node (node.piece
         *    == null) from each line
         * 5. These nodes are eligible for placing a ZERTZ Piece on
         */

        val result =
            eligibleNodesLines.keys.associateWith { node ->
              val lines = eligibleNodesLines[node]!!
              val verticalNodes =
                  findFirstNode(
                      currentNode = node,
                      line = lines.verticalLines.first(),
                      orientation = LineOrientation.VERTICAL,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )
              val upwardRightNodes =
                  findFirstNode(
                      currentNode = node,
                      line = lines.upwardRightLines.first(),
                      orientation = LineOrientation.UPWARD_RIGHT,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )
              val downwardRightNodes =
                  findFirstNode(
                      currentNode = node,
                      line = lines.downwardRightLines.first(),
                      orientation = LineOrientation.DOWNWARD_RIGHT,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )

              verticalNodes + upwardRightNodes + downwardRightNodes
            }

        return result
      }
      PieceType.YINSH -> {
        /**
         * 1/ The YINSH-potential has the ability to move along the lines on the board. It may be
         * moved onto an adjacent vacant spot or to any vacant spot that it can reach in a straight
         * line without jumping over pieces.
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

        val result =
            eligibleNodesLines.keys.associateWith { node ->
              val lines = eligibleNodesLines[node]!!
              val verticalNodes =
                  findAdjacentVacantNodes(
                      currentNode = node,
                      line = lines.verticalLines.first(),
                      orientation = LineOrientation.VERTICAL,
                      isNextNodeVacant = false,
                      isNextNodeOccupied = false,
                  )
              val upwardRightNodes =
                  findAdjacentVacantNodes(
                      currentNode = node,
                      line = lines.upwardRightLines.first(),
                      orientation = LineOrientation.UPWARD_RIGHT,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )
              val downwardRightNodes =
                  findAdjacentVacantNodes(
                      currentNode = node,
                      line = lines.downwardRightLines.first(),
                      orientation = LineOrientation.DOWNWARD_RIGHT,
                      isNextNodeVacant = true,
                      isNextNodeOccupied = false,
                  )

              verticalNodes + upwardRightNodes + downwardRightNodes
            }

        return result
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
         * - Note: Unlike 'GIPF With Potentials', a DVONN-potential CANNOT jump onto a basic GIPF
         *   piece.
         * * 2. STACK MECHANICS
         * - Because players can jump onto opposing DVONN-potentials, stacks of alternating colors
         *   will form.
         * - Unlike a stack of 2 identical/same-color potentials, a multi-color DVONN-stack is NOT
         *   treated as a single entity.
         * - When a multi-color stack is part of a resolved row, ONLY the top piece is removed.
         * * 3. NEUTRALIZATION & OCCUPATION
         * - Target pieces are neutralized when jumped on, and remain neutralized while covered.
         * - The top piece's color controls/occupies that board spot.
         * - When the top piece is removed, the piece directly beneath it is liberated and returns
         *   to active play.
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
         *   (caused by the previous player's move), they MUST remove it before making a standard
         *   move.
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

        val result =
            eligibleNodesLines.keys.associateWith { node ->
              val lines = eligibleNodesLines[node]!!
              val verticalNodes =
                  findFirstNode(
                          currentNode = node,
                          line = lines.verticalLines.first(),
                          orientation = LineOrientation.VERTICAL,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filter {
                        it?.piece?.type == pieceType && it.piece?.colorName == state.nextPlayer.name
                      }
                      .toSet()

              val upwardRightNodes =
                  findFirstNode(
                          currentNode = node,
                          line = lines.upwardRightLines.first(),
                          orientation = LineOrientation.UPWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filter {
                        it?.piece?.type == pieceType && it.piece?.colorName == state.nextPlayer.name
                      }
                      .toSet()

              val downwardRightNodes =
                  findFirstNode(
                          currentNode = node,
                          line = lines.downwardRightLines.first(),
                          orientation = LineOrientation.DOWNWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filter {
                        it?.piece?.type == pieceType && it.piece?.colorName == state.nextPlayer.name
                      }
                      .toSet()

              verticalNodes + upwardRightNodes + downwardRightNodes
            }

        return result
      }
      PieceType.PUNCT -> {
        /**
         * The PÜNCT-potential is used just like the DVONN-potential, except that it can only jump
         * onto an opponent’s PÜNCT-potential. So their function is the same, but each potential can
         * only target potentials of its own type.
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

        val result =
            eligibleNodesLines.keys.associateWith { node ->
              val lines = eligibleNodesLines[node]!!
              val verticalNodes =
                  findFirstNode(
                          currentNode = node,
                          line = lines.verticalLines.first(),
                          orientation = LineOrientation.VERTICAL,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filter {
                        it?.piece?.type == pieceType && it.piece?.colorName == state.nextPlayer.name
                      }
                      .toSet()

              val upwardRightNodes =
                  findFirstNode(
                          currentNode = node,
                          line = lines.upwardRightLines.first(),
                          orientation = LineOrientation.UPWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filter {
                        it?.piece?.type == pieceType && it.piece?.colorName == state.nextPlayer.name
                      }
                      .toSet()

              val downwardRightNodes =
                  findFirstNode(
                          currentNode = node,
                          line = lines.downwardRightLines.first(),
                          orientation = LineOrientation.DOWNWARD_RIGHT,
                          isNextNodeVacant = false,
                          isNextNodeOccupied = true,
                      )
                      .filter {
                        it?.piece?.type == pieceType && it.piece?.colorName == state.nextPlayer.name
                      }
                      .toSet()

              verticalNodes + upwardRightNodes + downwardRightNodes
            }

        return result
      }
      else -> {
        return mapOf()
      }
    }
  }
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
          line.firstOrNull { node.neighbors?.above?.equals(it) ?: false }
        }
        PushDirection.DOWN -> {
          line.firstOrNull { node.neighbors?.below?.equals(it) ?: false }
        }

        PushDirection.UPPER_RIGHT -> {
          line.firstOrNull { node.neighbors?.upperRight?.equals(it) ?: false }
        }

        PushDirection.LOWER_RIGHT -> {
          line.firstOrNull { node.neighbors?.lowerRight?.equals(it) ?: false }
        }

        PushDirection.UPPER_LEFT -> {
          line.firstOrNull { node.neighbors?.upperLeft?.equals(it) ?: false }
        }

        PushDirection.LOWER_LEFT -> {
          line.firstOrNull { node.neighbors?.lowerLeft?.equals(it) ?: false }
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
          line.firstOrNull { node.neighbors?.above?.equals(it) ?: false }
        }
        PushDirection.DOWN -> {
          line.firstOrNull { node.neighbors?.below?.equals(it) ?: false }
        }

        PushDirection.UPPER_RIGHT -> {
          line.firstOrNull { node.neighbors?.upperRight?.equals(it) ?: false }
        }

        PushDirection.LOWER_RIGHT -> {
          line.firstOrNull { node.neighbors?.lowerRight?.equals(it) ?: false }
        }

        PushDirection.UPPER_LEFT -> {
          line.firstOrNull { node.neighbors?.upperLeft?.equals(it) ?: false }
        }

        PushDirection.LOWER_LEFT -> {
          line.firstOrNull { node.neighbors?.lowerLeft?.equals(it) ?: false }
        }
      }

  if (nextNode == null) return vacantNodes

  if ((nextNode.piece != null) == isNextNodeOccupied) {
    vacantNodes.add(node)
    return vacantNodes
  } else {
    vacantNodes.plus(node)
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
    currentNode.piece = null

    var newBoard = updateBoard(nextNode, board)
    newBoard = updateBoard(currentNode, board)

    println("[SHIFT] Grid configuration post-shift step:")
    newBoard.printHexGrid()

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
    newBoard = updateBoard(currentNode, board)
    return newBoard
  }

  return board
}

fun retrieveAndCapturePieces(
    player: Player,
    line: Set<Node>, // TODO Replace with Line
    removePiecesWithPotential: Boolean,
    // TODO Add intersectingNodes to chooseToRemovePiecesWithPotential()
): RetrievedCapturedPieces {
  var retrievedPieces = listOf<Piece>()
  var capturedPieces = listOf<Piece>()

  val nodesToRemovePieces = mutableSetOf<Node>()

  // TODO "Implement logic for handling nodes where pieces are stacked and remove the stacked pieces and leave the underlying pieces, i.e. node.piece"
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
	 * - Note: Unlike 'GIPF With Potentials', a DVONN-potential CANNOT jump onto a basic GIPF
	 *   piece.
	 *
	 * * 2. STACK MECHANICS
	 * - Because players can jump onto opposing DVONN-potentials, stacks of alternating colors
	 *   will form.
	 * - Unlike a stack of 2 identical/same-color potentials, a multi-color DVONN-stack is NOT
	 *   treated as a single entity.
	 * - When a multi-color stack is part of a resolved row, ONLY the top piece is removed.
	 *
	 * * 3. NEUTRALIZATION & OCCUPATION
	 * - Target pieces are neutralized when jumped on, and remain neutralized while covered.
	 * - The top piece's color controls/occupies that board spot.
	 * - When the top piece is removed, the piece directly beneath it is liberated and returns
	 *   to active play.
	 *
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
	 *   (caused by the previous player's move), they MUST remove it before making a standard
	 *   move.
	 */

  retrievedPieces =
      line
          .filter { node ->
	          if (node.isNeutralized) {
		          check(node.piece?.type == PieceType.DVONN || node.piece?.type == PieceType.PUNCT)
		          {
			          "Only Dvonn and Pünct pieces can be neutralized."
		          }

		          node.stackedPieces.last().colorName == player.name
	          } else {
		          node.piece?.colorName == player.name
	          }
          }
          .mapNotNull {
            nodesToRemovePieces.add(it)
            it.piece
          }

	// TODO Check if all pieces have their potentials then at least one piece has to be removed
	//	if (removePiecesWithPotential) true else node.piece!!.hasNoPotential()
	if (retrievedPieces.all { it.potential }) {
		TODO("Implement function to select which pieces to remove, chooseToRemovePiecesWithPotential()")
		// TODO remove pieces and nodes from retrievedPieces and nodesToRemovePieces that have not been selected
	}

  capturedPieces =
      line
          .filter { node ->
	          if (node.isNeutralized) {
		          check(node.piece?.type == PieceType.DVONN || node.piece?.type == PieceType.PUNCT)
		          {
			          "Only Dvonn and Pünct pieces can be neutralized."
		          }

		          node.stackedPieces.last().colorName != player.name
	          } else {
		          node.piece?.colorName != player.name
	          }
          }
	      .mapNotNull {
		      nodesToRemovePieces.add(it)
		      it.piece
	      }

  return RetrievedCapturedPieces(
      retrieved = retrievedPieces,
      captured = capturedPieces,
      nodes = nodesToRemovePieces.toList(),
  )
}
