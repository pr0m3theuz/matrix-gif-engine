package org.example.ai.mcts

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.engine.determineWinner
import org.example.model.*
import org.example.toBitList
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

fun calculateUCTRAVEScore(
  parentRollouts: Double,
  childRollouts: Int,
  winPercentage: Float,
  temperature: Double = 1.5,
  useRAVE: Boolean = false,
  raveWins: Int = 0,
  raveCounts: Int = 0,
  raveK: Int = 3,
  bias: Double =
        10.0.pow(
            -7
        ), // see pg 3 for bias constant amounts https://www.ijcai.org/Proceedings/15/Papers/112.pdf
): Double {
  val exploration = sqrt(ln(parentRollouts).div(childRollouts.toDouble()))
  val ucb = winPercentage + temperature * exploration

  if (!useRAVE || raveCounts == 0) {
    return ucb
  }

  val betaState =  sqrt(raveK / ((3 * parentRollouts) + raveK))
    // raveCounts / (raveCounts + parentRollouts + bias * raveCounts + parentRollouts)

  val amaf = raveWins.toDouble() / raveCounts
  // val mean = winPercentage

  val uctRAVE = (((1.0 - betaState) * winPercentage) + (betaState * amaf))

  return uctRAVE + temperature * exploration
}

// TODO how to figure out when a turn ends

@Serializable
sealed class PackedMove {
  @Serializable data class Single(val value: UInt) : PackedMove()

  @Serializable data class Multiple(val values: List<UInt>) : PackedMove()
}

fun PackedMove.encode(): NDArray<Int, D1> {
  return when (this) {
    is PackedMove.Multiple -> {
      values
          .fold(0UL) { acc, bit ->
            acc or bit.extractTargetBit()
          }
          .toBitList()
    }
    is PackedMove.Single -> {
      val targetBit = value.extractTargetBit()
      requireNotNull(targetBit) { "Target bit must not be null" }
      targetBit.toBitList()
    }
  }
}

fun PackedMove.extractTargetBit(): ULong? {
  return when (this) {
    is PackedMove.Single -> {
      this.value.extractTargetBit()
    }
    is PackedMove.Multiple -> null
  }
}

// @Serializable
data class MCTSNode(
    val bitboard: Bitboard,
    val currentPlayer: Player,
    val nextPlayer: Player,
    val parentNode: MCTSNode? = null,
    val move: PackedMove? = null,
    var turnCount: Int = 0,
    val turnPhase: TurnPhase,
    val previousTurnPhases: List<TurnPhase> = emptyList(),
    val childrenNodes: MutableList<MCTSNode> = mutableListOf(),
    val winCounts: MutableMap<PlayerName, Int> =
        mutableMapOf(PlayerName.BLACK to 0, PlayerName.WHITE to 0),
    var rolloutCounts: Int = 0,
    val unvisitedMoves: MutableList<PackedMove> = mutableListOf(),
    val useRAVE: Boolean = false,
    var raveCounts: Int = 0,
    var raveWins: Int = 0,
) {
  override fun toString(): String {
    // Break the cycle: Do NOT include parentNode or childrenNodes here
    return "MCTSNode{" +
        "move=" +
        move +
        ", rolloutCounts=" +
        rolloutCounts +
        ", winCounts=" +
        winCounts +
        ", unvisitedMoves=" +
        unvisitedMoves.size +
        '}'
  }

  fun addRandomChildNode(rng: Random): MCTSNode {
    val previousUnvisitedMoveSize = unvisitedMoves.size
    val previousChildrenNodesSize = childrenNodes.size
    // Child Node Properties
    val childCurrentPlayer = currentPlayer.deepCopy()
    val childNextPlayer = nextPlayer.deepCopy()
    val childBitboard = this.bitboard.deepCopy()
    val selectedPackedMove = unvisitedMoves.random(rng)

    // TODO apply move
    /**
     * TODO Does this move a TAMSK piece to the center? Does this move create 4 in a row for the
     * current player? if yes, remove pieces. Add retrieval/capture moves to
     * identifyAvailableMoves()
     */
    this.turnCount =
        if (parentNode?.currentPlayer?.name == this.currentPlayer.name) {
          this.turnCount
        } else {
          this.turnCount.plus(1)
        }

    var turnPhase: TurnPhase? = null
    when (selectedPackedMove) {
      is PackedMove.Multiple -> {
        val retrievedCapturedPieces = mutableListOf<UInt>()

        childBitboard.removeSelectedPieces(
            player = childCurrentPlayer,
            piecesToRemove = selectedPackedMove.values.distinct(),
            movesBuffer = retrievedCapturedPieces,
        )

        turnPhase = TurnPhase.PieceRemoval

        //        val retrievedPieces = retrievedCapturedPieces.mapNotNull {
        //          it.retrievedPiece
        //        }
        //        val capturedPieces = retrievedCapturedPieces.mapNotNull {
        //          it.capturedPiece
        //        }

        childCurrentPlayer.addRetrievedCapturedPieces(retrievedCapturedPieces)

        childCurrentPlayer.combinePieces()

        childBitboard.assertPieceCount(
            currentPlayer = childCurrentPlayer,
            nextPlayer = childNextPlayer,
        )
      }
      is PackedMove.Single -> {
        val selectedMove = selectedPackedMove.value
        when (selectedMove.extractMoveType()) {
          MoveType.AddPiece -> {
            //            requireNotNull(selectedMove.extractTargetBit())
            //            requireNotNull(selectedMove.ex)
            //            requireNotNull(selectedMove.columnInfo)
            //
            //            val addAtIndex = selectedMove.targetBit
            //            val pushDirection = selectedMove.pushDirection
            //            val columnInfo = selectedMove.columnInfo
            if (selectedMove.extractSourceBit() == boardCenterSpotMask) {
              childBitboard.useTamskPotential(selectedMove)

              turnPhase = TurnPhase.ExtraMove
            } else {
              turnPhase = TurnPhase.PlayerInputWindow

              val selectedPiece =
                  selectedMove.onlyPiece().let { childCurrentPlayer.selectPiece(it) }

              selectedPiece?.let {
                childBitboard.addPieceToBitboard(
                    move = selectedMove,
                )
              }
            }
          }
          MoveType.UsePotential -> {
            val selectedMove = selectedPackedMove.value
            childBitboard.usePiecePotential(
                move = selectedMove,
            )

            turnPhase = TurnPhase.PlayerInputWindow
          }
          MoveType.RetrieveCapturePieces -> {}
        }
      }
    }

    // child Node Moves

    val currentPlayerTamskMoves = mutableListOf<PackedMove>()
    childBitboard.getTamskMoves(childCurrentPlayer, currentPlayerTamskMoves)

    val currentPlayerRemovablePieces = mutableListOf<PackedMove>()
    childBitboard.identifyPiecesToRemove(childCurrentPlayer, currentPlayerRemovablePieces)

    val currentPlayerAvailableMoves = mutableListOf<PackedMove>()
    childBitboard.identifyAvailableMoves(
        childCurrentPlayer,
        movesBuffer = currentPlayerAvailableMoves,
    )

    val nextPlayerAvailableMoves = mutableListOf<PackedMove>()
    childBitboard.identifyAvailableMoves(childNextPlayer, movesBuffer = nextPlayerAvailableMoves)

    val nextPlayerTamskMoves = mutableListOf<PackedMove>()
    childBitboard.getTamskMoves(childNextPlayer, nextPlayerTamskMoves)

    val nextPlayerRemovablePieces = mutableListOf<PackedMove>()
    childBitboard.identifyPiecesToRemove(childNextPlayer, nextPlayerRemovablePieces)

    data class ChildState(
        val currentPlayer: Player,
        val nextPlayer: Player,
        val turnPhase: TurnPhase,
        val moves: List<PackedMove>,
    )

    val turnHasHadNormalMove = previousTurnPhases.any { it == TurnPhase.PlayerInputWindow }

    val (nodeCurrentPlayer, nodeNextPlayer, nodeTurnPhase, nodeMoves) =
        when {
          currentPlayerTamskMoves.isNotEmpty() ->
              ChildState(
                  childCurrentPlayer,
                  childNextPlayer,
                  TurnPhase.ExtraMove,
                  currentPlayerTamskMoves,
              )

          currentPlayerRemovablePieces.isNotEmpty() && turnHasHadNormalMove ->
              ChildState(
                  childCurrentPlayer,
                  childNextPlayer,
                  TurnPhase.PieceRemoval,
                  currentPlayerRemovablePieces,
              )

          !turnHasHadNormalMove && turnPhase != TurnPhase.PlayerInputWindow ->
              ChildState(
                  childCurrentPlayer,
                  childNextPlayer,
                  TurnPhase.PlayerInputWindow,
                  currentPlayerAvailableMoves,
              )

          // TODO Don't change prematurely
          nextPlayerRemovablePieces.isNotEmpty() ->
              ChildState(
                  childNextPlayer,
                  childCurrentPlayer,
                  TurnPhase.PieceRemoval,
                  nextPlayerRemovablePieces,
              )

          nextPlayerTamskMoves.isNotEmpty() ->
              ChildState(
                  childNextPlayer,
                  childCurrentPlayer,
                  TurnPhase.ExtraMove,
                  nextPlayerTamskMoves,
              )

          nextPlayerAvailableMoves.isNotEmpty() ->
              ChildState(
                  childNextPlayer,
                  childCurrentPlayer,
                  TurnPhase.PlayerInputWindow,
                  nextPlayerAvailableMoves,
              )

          else -> {
            //        error("No valid turn phase transition found") // Game is over
            ChildState(
                childNextPlayer,
                childCurrentPlayer,
                TurnPhase.PlayerInputWindow,
                emptyList(),
            )
          }
        }

    // TODO Ascertain if turn phase works as expected. Create a test for different scenarios.
    val childNode =
        MCTSNode(
            bitboard = childBitboard,
            currentPlayer = nodeCurrentPlayer,
            nextPlayer = nodeNextPlayer,
            parentNode = this,
            move = selectedPackedMove,
            turnCount = this.turnCount,
            turnPhase = nodeTurnPhase,
            previousTurnPhases =
                if (nodeCurrentPlayer.name == this.currentPlayer.name)
                    previousTurnPhases.plus(this.turnPhase)
                else emptyList(),
            unvisitedMoves = nodeMoves.toMutableList(),
            useRAVE = this.useRAVE,
        )

    //    if (nodeMoves.isEmpty()) {
    //      logger.info { "" + ("Current node has no children!") }
    //    }

    this.childrenNodes.add(childNode)
    this.unvisitedMoves.remove(selectedPackedMove)

    val unvisitedMovesDifference = previousUnvisitedMoveSize - unvisitedMoves.size
    val childrenNodesDifference = childrenNodes.size - previousChildrenNodesSize

    check(unvisitedMovesDifference == 1) {
      "UnvisitedMoves decrement failure! Expected exactly 1 move to be removed from unvisitedMoves, " +
          "but the delta was $unvisitedMovesDifference (Initial: $previousUnvisitedMoveSize, Current: ${unvisitedMoves.size})."
    }

    check(childrenNodesDifference == 1) {
      "Reserve decrement failure! Expected exactly 1 piece to be removed from the reserve, " +
          "but the delta was $childrenNodesDifference (Initial: $previousChildrenNodesSize, Current: ${childrenNodes.size})."
    }

    return childNode
  }

  fun recordWin(winner: Player) {
    this.winCounts[winner.name] = this.winCounts[winner.name]!! + 1
    this.rolloutCounts += 1
  }

  fun updateRAVE(winner: Player) {
    this.raveWins += if (this.currentPlayer == winner) 1 else 0
    this.raveCounts += 1
  }

  fun winPercentage(player: Player): Float {
    if (rolloutCounts == 0) return 0f
    return winCounts[player.name]!!.div(this.rolloutCounts.toFloat())
  }

  fun selectChildNodeToExplore(): MCTSNode {
    val totalRollouts = childrenNodes.sumOf { it.rolloutCounts.toDouble() }

    //
    val childNodePlayer = childrenNodes.first().currentPlayer

    var bestScore = -1.0
    var bestChildNode: MCTSNode? = null

    for (childNode in this.childrenNodes) {
      val score =
          calculateUCTRAVEScore(
              parentRollouts = totalRollouts,
              childRollouts = childNode.rolloutCounts,
              winPercentage = childNode.winPercentage(childNodePlayer),
              temperature = 1.5,
              useRAVE = this.useRAVE,
          )

      if (score > bestScore) {
        bestScore = score
        bestChildNode = childNode
      }
    }

    if (this.childrenNodes.size == 1) {
      bestChildNode = this.childrenNodes.first()
    }

    requireNotNull(bestChildNode) {
      "Search Strategy Failure: Unable to select a best child node from parent node " +
          "(visitCount=${totalRollouts}, children=${this.childrenNodes.size}). " +
          "Ensure tree expansion and rollout selection policies are correctly handling non-terminal states."
    }

    return bestChildNode
  }
}

fun selectMoveMCTS(
    bitboard: Bitboard,
    currentPlayer: Player,
    nextPlayer: Player,
    rounds: IntRange = 0..9999,
    turnPhase: TurnPhase,
    rng: Random,
    duration: Duration = Duration.ZERO,
    useRAVE: Boolean = false,
): PackedMove? {
  val availableMoves: MutableList<PackedMove> = mutableListOf()

  bitboard.generateMoves(currentPlayer, turnPhase, availableMoves)

  if (availableMoves.isEmpty()) return null

  if (rounds.last() == 0) {
    return availableMoves.random(rng)
  }

  val rootMCTSNode =
      MCTSNode(
          bitboard = bitboard,
          currentPlayer = currentPlayer,
          nextPlayer = nextPlayer,
          turnPhase = turnPhase,
          unvisitedMoves = availableMoves.toMutableList(),
          useRAVE = useRAVE,
      )

  if (rootMCTSNode.unvisitedMoves.isEmpty()) {
    return null
  }

  if (duration == Duration.ZERO) {
    repeat(rounds.count()) {
      var currentNode: MCTSNode? = rootMCTSNode

      while (
          currentNode?.unvisitedMoves?.isEmpty() == true &&
              currentNode.childrenNodes.isNotEmpty() &&
              !evaluateCapturedPieces(currentNode.currentPlayer) // &&
      // !evaluatePiecesInReserve(currentNode.currentPlayer)
      ) {
        //      if (currentNode.unvisitedMoves.isEmpty() && currentNode.childrenNodes.isEmpty()) {
        //        logger.info { "" + ("Current node has no children!}") }
        //      }

        currentNode = currentNode.selectChildNodeToExplore()
      }

      checkNotNull(currentNode) {
        "Search Tree Traversal Failure: Encountered a null node during evaluation loop. " +
            "Verify tree expansion bounds and parent-child link validity."
      }

      //    if (currentNode.unvisitedMoves.isEmpty() && currentNode.childrenNodes.isEmpty()) {
      //      logger.info { "" + ("Current node has no children!") }
      //    }

      if (currentNode.unvisitedMoves.isNotEmpty()) currentNode = currentNode.addRandomChildNode(rng)

      val simulationActions: MutableList<PackedMove> = mutableListOf()

      val winner =
          simulateRandomGame(
              currentNode.bitboard.deepCopy(),
              currentNode.currentPlayer.deepCopy(),
              currentNode.nextPlayer.deepCopy(),
              rng = rng,
              simulationActions = simulationActions,
          )

      while (currentNode != null && winner != null) {
        currentNode.recordWin(winner)
        currentNode = currentNode.parentNode
      }
    }
  } else {
    val startTime = System.nanoTime()
    val endTime = startTime + duration.inWholeNanoseconds

    while (System.nanoTime() < endTime) {
      var currentNode: MCTSNode? = rootMCTSNode

      while (
          currentNode?.unvisitedMoves?.isEmpty() == true &&
              currentNode.childrenNodes.isNotEmpty() &&
              !evaluateCapturedPieces(currentNode.currentPlayer)
      ) {
        currentNode = currentNode.selectChildNodeToExplore()
      }

      checkNotNull(currentNode) {
        "Search Tree Traversal Failure: Encountered a null node during evaluation loop. " +
            "Verify tree expansion bounds and parent-child link validity."
      }

      if (currentNode.unvisitedMoves.isNotEmpty()) currentNode = currentNode.addRandomChildNode(rng)

      // https://www.ijcai.org/Proceedings/15/Papers/112.pdf
      // https://github.com/hiive/hiivelabs-zertz-mcts/blob/12537a6be44e99f8273c9f81587191526f358a0e/src/mcts.rs
      val simulationActions: MutableList<PackedMove> = mutableListOf()

      val winner =
          simulateRandomGame(
              currentNode.bitboard.deepCopy(),
              currentNode.currentPlayer.deepCopy(),
              currentNode.nextPlayer.deepCopy(),
              rng = rng,
              simulationActions,
          )

      while (currentNode != null && winner != null) {
        currentNode.recordWin(winner)
        currentNode = currentNode.parentNode

        // Check if this sibling's action appears in the simulation
        if (currentNode != null && currentNode.childrenNodes.isNotEmpty()) {
          for (siblingNode in currentNode.childrenNodes) {
            if (siblingNode.move in simulationActions) {
              siblingNode.updateRAVE(winner)
            }
          }
        }
      }
    }
  }

  var bestMove: PackedMove? = null
  var bestPercentage = -1f
  for (child in rootMCTSNode.childrenNodes) {
    val winPercentage = child.winPercentage(nextPlayer)
    if (winPercentage > bestPercentage) {
      bestPercentage = winPercentage
      bestMove = child.move
    }
  }

  // TODO What to do when no best move is found?
  return bestMove ?: availableMoves.random(rng)
}

private fun evaluateCapturedPieces(player: Player): Boolean {
  return player.capturedPieces.count { piece -> piece.extractPieceType() == PieceType.GIPF } == 3
}

private fun evaluatePiecesInReserve(player: Player): Boolean {
  return player.piecesInReserve.none { piece ->
    piece.extractPotential() || piece.extractPieceType() == PieceType.GIPF
  }
}

fun simulateRandomGame(
    bitboard: Bitboard,
    currentPlayer: Player,
    nextPlayer: Player,
    rng: Random,
    simulationActions: MutableList<PackedMove>,
): Player? {

  var playerWhoMadeTheLastMove: Player? = null

  // TODO if > 15 pieces and GIPF pieces in reserve rotate player

  var activePlayer = currentPlayer
  var opponentPlayer = nextPlayer

  val availableMoves = mutableListOf<PackedMove>()
  bitboard.identifyAvailableMoves(activePlayer, movesBuffer = availableMoves)

  //  val opponentMoves = mutableListOf<PossibleBitMove>()
  //  bitboard.identifyAvailableMoves(opponentPlayer, movesBuffer = opponentMoves)

  while (!evaluateCapturedPieces(activePlayer) || availableMoves.isNotEmpty()) {
    if (availableMoves.isEmpty()) break

    simulatePlayerTurn(
        bitboard = bitboard,
        currentPlayer = activePlayer,
        opponentPlayer = opponentPlayer,
        rng = rng,
        simulationActions,
    )

    // TODO fix early game turns. white gets an extra move
    //    check(
    //        abs(activePlayer.piecesInReserve.count { piece -> piece.type == PieceType.GIPF } -
    //            opponentPlayer.piecesInReserve.count { piece -> piece.type == PieceType.GIPF }) <
    // 2
    //    )

    playerWhoMadeTheLastMove = activePlayer
    // end of turn, rotate players
    val tempPlayer = opponentPlayer
    opponentPlayer = activePlayer
    activePlayer = tempPlayer

    availableMoves.clear()
    bitboard.identifyAvailableMoves(activePlayer, movesBuffer = availableMoves)
  }

  val winner =
      determineWinner(
              activePlayer,
              opponentPlayer,
              playerWhoMadeTheLastMove ?: nextPlayer,
          )
          ?.first
  //  logger.info { "" + ("Player: ${winner?.name} won") }
  return winner
}

fun simulatePlayerTurn(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    rng: Random,
    simulationActions: MutableList<PackedMove>,
) {
  if (logger.isDebugEnabled()) {
    //		logger.info { "" + ("--- ALPHA-BETA CALLED ---") }
    logger.info { "currentPlayer: $currentPlayer" }
    logger.info { "opponentPlayer: $opponentPlayer" }
    logger.info { "Bitboard: ${Json.encodeToString(bitboard)}" }
  }

  // TODO given a list of moves, select a random move

  simulatePieceRetrievalCapture(
      bitboard,
      currentPlayer,
      opponentPlayer,
      rng,
      simulationActions,
  )

  val tamskMoves = mutableListOf<PackedMove>()
  bitboard.getTamskMoves(currentPlayer, tamskMoves)

  if (tamskMoves.isNotEmpty()) {
    simulatePlayerMove(
        bitboard,
        currentPlayer,
        opponentPlayer,
        rng,
        simulationActions,
    )
  }

  simulatePlayerMove(
      bitboard,
      currentPlayer,
      opponentPlayer,
      rng = rng,
      simulationActions,
  )

  tamskMoves.clear()
  bitboard.getTamskMoves(currentPlayer, tamskMoves)

  if (tamskMoves.isNotEmpty()) {
    simulatePlayerMove(
        bitboard,
        currentPlayer,
        opponentPlayer,
        rng = rng,
        simulationActions,
    )
  }

  // TODO come back to this for UnusedTamskPotential when there are no vacancies on the board
  //  while (
  //    when (currentPlayer.name) {
  //      PlayerName.WHITE ->
  //        bitboard.whiteTAMSK and
  //            bitboard.whitePotentials and
  //            boardCenterSpotMask
  //      PlayerName.BLACK ->
  //        bitboard.blackTAMSK and
  //            bitboard.blackPotentials and
  //            boardCenterSpotMask
  //    } == boardCenterSpotMask
  //  ) {
  //    simulatePlayerMove(
  //      bitboard,
  //      currentPlayer,
  //      opponentPlayer,
  //      rng = rng,
  //      simulationActions
  //    )
  //  }

  simulatePieceRetrievalCapture(
      bitboard,
      currentPlayer,
      opponentPlayer,
      rng,
      simulationActions,
  )

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
}

fun simulatePlayerMove(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    rng: Random,
    simulationActions: MutableList<PackedMove>,
) {
  //  val initbitboard = bitboard.deepCopy()
  val possibleBitMoves = mutableListOf<PackedMove>()
  bitboard.identifyAvailableMoves(currentPlayer, columnInfos, possibleBitMoves)

  // TODO change to depth <= 0
  if (possibleBitMoves.isEmpty()) {
    return
  }

  val randomPackedMove = possibleBitMoves.random(rng)

  simulationActions.add(randomPackedMove)

  when (randomPackedMove) {
    is PackedMove.Multiple -> {}
    is PackedMove.Single -> {
      when (randomPackedMove.value.extractMoveType()) {
        MoveType.AddPiece -> {
          //          val addAtIndex = randomPossibleBitMove.extractTargetBit()
          //          val pushDirection = randomPossibleBitMove.extractPushDirection()
          //          val columnInfo = randomPossibleBitMove.extractColumnInfo()
          //
          //          requireNotNull(randomPossibleBitMove.targetBit)
          //          requireNotNull(randomPossibleBitMove.pushDirection)
          //          requireNotNull(randomPossibleBitMove.columnInfo)
          if (randomPackedMove.value.extractSourceBit() == boardCenterSpotMask) {
            bitboard.useTamskPotential(randomPackedMove.value)
          } else {
            val selectedPiece =
                randomPackedMove.value.onlyPiece().let { currentPlayer.selectPiece(it) }

            selectedPiece?.let {
              bitboard.addPieceToBitboard(randomPackedMove.value)
            }
          }
        }
        MoveType.UsePotential -> {
          bitboard.usePiecePotential(
              move = randomPackedMove.value,
          )
        }
        MoveType.RetrieveCapturePieces -> {}
      }
    }
  }

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
}

fun simulatePieceRetrievalCapture(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    rng: Random,
    simulationActions: MutableList<PackedMove>,
) {
  val removePiecesPowerset = mutableListOf<PackedMove>()
  bitboard.identifyPiecesToRemove(currentPlayer, removePiecesPowerset)

  if (removePiecesPowerset.isNotEmpty()) {
    val selectedPieceToRemove = removePiecesPowerset.random(rng)

    simulationActions.add(selectedPieceToRemove)

    when (selectedPieceToRemove) {
      is PackedMove.Multiple -> {
        check(selectedPieceToRemove.values.isNotEmpty()) {
          "There must be at least one column"
        }

        // can i modify selectedPieceToRemove.values directly
        val retrievedCapturedPieces = mutableListOf<UInt>()
        bitboard.removeSelectedPieces(
            player = currentPlayer,
            piecesToRemove = selectedPieceToRemove.values.distinct(),
            retrievedCapturedPieces,
        )

        currentPlayer.addRetrievedCapturedPieces(retrievedCapturedPieces)

        currentPlayer.combinePieces()
      }
      is PackedMove.Single -> {}
    }

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
  }

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
}
