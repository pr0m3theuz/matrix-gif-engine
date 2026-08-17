package org.example.ai.mcts

import kotlin.math.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.ai.doActionGetTurnPhase
import org.example.ai.humanEvaluation.SearchInfo
import org.example.engine.determineWinner
import org.example.model.*
import org.example.toBitList
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

// TODO how to figure out when a turn ends

@Serializable
sealed class PackedMove {
  @Serializable data class Single(val value: UInt) : PackedMove()

  @Serializable
  data class Multiple(
      val values: List<UInt>,
  ) : PackedMove()

  var evaluation: Int = 0
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

/**
 * A move made during random-playout simulation, tagged with the player who made it.
 *
 * RAVE/AMAF (see [MCTSNode.updateRAVE]) credits a sibling node based on whether its move occurred
 * anywhere in the simulated playout. [PackedMove] equality is purely positional (bit-encoded board
 * target, no player identity), so without this tag a sibling belonging to one player could be
 * credited or blamed because the *opponent* happened to play an identically-encoded move elsewhere
 * in the same rollout. Tagging the mover lets us restrict the AMAF lookup to moves made by the same
 * player who would make the move at that node.
 */
data class SimulatedMove(val player: PlayerName, val move: PackedMove)

/**
 * All moves made along the in-tree path from the root down to [leaf], grouped by the player who
 * made each move (the player who was `currentPlayer` at the parent node, since that's whose move
 * list the child's [MCTSNode.move] was drawn from).
 *
 * RAVE/AMAF (Gelly & Silver, 2011) defines the "subtree" of a state as everything visited from that
 * state onward in a simulation - the remaining in-tree moves *and* the random-playout moves
 * - so both must be included when updating a node's AMAF statistics, not just the playout portion.
 */
private fun collectTreePathMovesByPlayer(
    leaf: MCTSNode
): MutableMap<PlayerName, MutableSet<PackedMove>> {
  val movesByPlayer = mutableMapOf<PlayerName, MutableSet<PackedMove>>()
  var node: MCTSNode? = leaf
  while (node?.parentNode != null) {
    val mover = node.parentNode.currentPlayer.name
    val move = node.move
    if (move != null) {
      movesByPlayer.getOrPut(mover) { mutableSetOf() }.add(move)
    }
    node = node.parentNode
  }
  return movesByPlayer
}

data class RaveStats(
    var raveCounts: Int = 0,
    var raveWins: MutableMap<PlayerName, Int> =
        mutableMapOf(PlayerName.BLACK to 0, PlayerName.WHITE to 0),
)

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
    val unvisitedMoves: MutableList<PackedMove>,
    val useRAVE: Boolean = false,
    val raveStats: Map<PackedMove, RaveStats>,
    var raveCounts: Int = 0,
    var raveWins: MutableMap<PlayerName, Int> =
      mutableMapOf(PlayerName.BLACK to 0, PlayerName.WHITE to 0),
    var parentQ: Float = 0.5f,
    var meanQ: Float = 0.5f, // from Facebook's ELF Go
    val progressiveWideningConstant: Double = 1.5,
    val progressiveWideningAlpha: Double = 0.4,
    val totalActions: Int,
    val evalScore: Int = 0,
    var expValue: Double = 0.0,
    var priorProbability: Double = 0.0,
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

  fun getEdgeQ(parentQ: Float): Float {
    return if (this.rolloutCounts > 0) {
      winCounts.getValue(currentPlayer.name).toFloat() / rolloutCounts
    } else {
      if (parentNode?.currentPlayer?.name == this.currentPlayer.name) parentQ else (1.0f - parentQ)
    }
  }

  fun getUnlockedActionCount(): Int {
    if (rolloutCounts <= 0) return 1

    val limit = progressiveWideningConstant * rolloutCounts.toDouble().pow(progressiveWideningAlpha)

    return limit.coerceIn(1.0, totalActions.toDouble()).roundToInt()
  }

  fun expandNextChild(rng: Random): MCTSNode {
    check(unvisitedMoves.isNotEmpty()) { "Cannot expand child from empty unvisitedMoves!" }

    // Prevent ln(0) errors if expanding on the very first rollout
    val parentRollouts = maxOf(1.0, this.rolloutCounts.toDouble())
    val temperature = 1.5

    var bestScore = -Double.MAX_VALUE
    var bestMoveIndex = 0

    for (i in unvisitedMoves.indices) {
      val move = unvisitedMoves[i]

      // Default exploitation value for unvisited moves (First Play Urgency)
      var exploitation = this.meanQ.toDouble()

      // 1. Evaluate Exploitation using RAVE (AMAF) if available
      if (this.useRAVE) {
        val stats = this.raveStats[move]
        if (stats != null && stats.raveCounts > 0) {
          exploitation = stats.raveWins.getValue(this.currentPlayer.name).toDouble() / stats.raveCounts
        }
      }

      // 2. Evaluate Exploration
      // Since child rollouts = 0, we use the parent-level exploration term
      // identical to your calculateUCTRAVEScore fallback logic.
      val exploration = sqrt(ln(parentRollouts))

      // 3. Calculate UCT Score
      val uctScore = exploitation + (temperature * exploration)

      if (uctScore > bestScore) {
        bestScore = uctScore
        bestMoveIndex = i
      }
    }

    val selectedPackedMove = unvisitedMoves[bestMoveIndex]

    val childNode = this.createChildNodeFromMove(selectedPackedMove, rng)

    return childNode
  }

  fun createChildNodeFromMove(selectedPackedMove: PackedMove, rng: Random): MCTSNode {
    val previousUnvisitedMoveSize = unvisitedMoves.size
    val previousChildrenNodesSize = childrenNodes.size
    // Child Node Properties
    val childCurrentPlayer = currentPlayer.liteDeepCopy()
    val childNextPlayer = nextPlayer.liteDeepCopy()
    val childBitboard = this.bitboard.deepCopy()

    // TODO apply move
    /**
     * TODO Does this move a TAMSK piece to the center? Does this move create 4 in a row for the
     * current player? if yes, remove pieces. Add retrieval/capture moves to
     * identifyAvailableMoves()
     */
    val turnCount =
        if (parentNode?.currentPlayer?.name == this.currentPlayer.name) {
          this.turnCount
        } else {
          this.turnCount.plus(1)
        }

    val turnPhase =
        doActionGetTurnPhase(
            selectedPackedMove,
            childBitboard,
            childCurrentPlayer,
            childNextPlayer,
        )

    // child Node Moves
    val (nodeCurrentPlayer, nodeNextPlayer, nodeTurnPhase, nodeMoves) =
        createChildState(
            childBitboard,
            childCurrentPlayer,
            childNextPlayer,
            turnPhase,
            previousTurnPhases.any { it == TurnPhase.PlayerInputWindow },
        )

    //    nodeMoves.sortByDescending { action ->
    //      scoreActions(
    //        selectedPackedMove = action,
    //        bitboard = childBitboard.deepCopy(),
    //        currentPlayer = nodeCurrentPlayer.liteDeepCopy(),
    //        nextPlayer = nodeNextPlayer.liteDeepCopy(),
    //        rng = rng,
    //      )
    //    }

    //    val scratchBitboard = childBitboard.deepCopy()
    //    val scratchCurrentPlayer = nodeCurrentPlayer.liteDeepCopy()
    //    val scratchNextPlayer = nodeNextPlayer.liteDeepCopy()
    //
    //// 2. Pre-calculate scores exactly ONCE per move (O(N) instead of O(N log N))
    //    for (i in nodeMoves.indices) {
    //      // 3. Reset scratch states (Implement these methods to overwrite data, NOT allocate!)
    //      // e.g., scratchBitboard.whitePieces = childBitboard.whitePieces
    //      scratchBitboard.copyFrom(childBitboard)
    //      scratchCurrentPlayer.copyFrom(nodeCurrentPlayer)
    //      scratchNextPlayer.copyFrom(nodeNextPlayer)
    //
    //      nodeMoves[i].evaluation = scoreActions(
    //        selectedPackedMove = nodeMoves[i],
    //        bitboard = scratchBitboard,
    //        currentPlayer = scratchCurrentPlayer,
    //        nextPlayer = scratchNextPlayer,
    //        rng = rng
    //      )
    //    }
    //
    //    for (i in 1 until nodeMoves.size) {
    //      val currentMove = nodeMoves[i]
    //      val currentScore = currentMove.evaluation
    //      var j = i - 1
    //
    //      // Shift elements that have a LOWER score to the right
    //      while (j >= 0 && nodeMoves[j].evaluation < currentScore) {
    //        nodeMoves[j + 1] = nodeMoves[j]
    //        j--
    //      }
    //      // Insert the current move at its correct sorted position
    //      nodeMoves[j + 1] = currentMove
    //    }



    // TODO Ascertain if turn phase works as expected. Create a test for different scenarios.
    val childNode =
        MCTSNode(
            bitboard = childBitboard,
            currentPlayer = nodeCurrentPlayer,
            nextPlayer = nodeNextPlayer,
            parentNode = this,
            move = selectedPackedMove,
            turnCount = turnCount,
            turnPhase = nodeTurnPhase,
            previousTurnPhases =
                if (nodeCurrentPlayer.name == this.currentPlayer.name)
                    previousTurnPhases.plus(this.turnPhase)
                else emptyList(),
            unvisitedMoves = nodeMoves,
            useRAVE = this.useRAVE,
            raveStats = nodeMoves.associateWith { RaveStats() },
            raveCounts = this.raveStats.getValue(selectedPackedMove).raveCounts,
            raveWins = this.raveStats.getValue(selectedPackedMove).raveWins.toMutableMap(),
            parentQ = this.meanQ,
            meanQ = this.meanQ,
            totalActions = nodeMoves.size,
            evalScore = selectedPackedMove.evaluation,
            progressiveWideningConstant = this.progressiveWideningConstant,
            progressiveWideningAlpha = this.progressiveWideningAlpha,
        )

    //    if (nodeMoves.isEmpty()) {
    //      logger.info { "Current node has no children!" }
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
    this.winCounts[winner.name] = this.winCounts.getValue(winner.name) + 1
    this.rolloutCounts += 1
  }

  fun updateRAVE(winner: Player) {
    this.raveWins[winner.name] = this.raveWins.getValue(winner.name) + 1
    this.raveCounts += 1
  }

  fun winPercentage(player: Player): Float {
    if (rolloutCounts == 0) return 0f
    return winCounts.getValue(player.name).div(this.rolloutCounts.toFloat())
  }

  fun calculateUCTRAVEScore(
      childNode: MCTSNode,
      parentRollouts: Double,
      temperature: Double = 1.5,
      useRAVE: Boolean = false,
      raveK: Int = 3,
      fpu: Float = 0.0f,
      // see pg 3 for bias constant amounts https://www.ijcai.org/Proceedings/15/Papers/112.pdf
      bias: Double = 10.0.pow(-7),
  ): Double {

    // First Play Urgency: with zero rollouts, wins/rollouts is undefined (0/0) and the
    // exploration term blows up to Infinity/NaN. Rather than let that happen, fall back to the
    // caller-supplied baseline value (fpu, typically the parent's Q estimate). This applies
    // unconditionally, independent of useRAVE, since an unvisited node has no RAVE stats either.
    if (childNode.rolloutCounts <= 0) {
      //  return fpu.toDouble()
      val exploration = sqrt(ln(parentRollouts))
      return fpu + temperature * exploration
    }

    val wins = childNode.winCounts.getValue(this.currentPlayer.name)
    val rollouts = childNode.rolloutCounts.toDouble()
    val winPercentage = wins / rollouts

    val exploration = sqrt(ln(parentRollouts).div(rollouts))

    val ucb = winPercentage + temperature * exploration

    if (!useRAVE || childNode.raveCounts == 0) {
      return ucb
    }

    val p = childNode.rolloutCounts.toDouble()
    val pa = childNode.raveCounts.toDouble()
    val beta = pa / (raveCounts + p + bias * pa * p) // GRAVE
    // val beta = sqrt(raveK.toDouble() / ((3 * p) + raveK.toDouble())) // Silver & Gelly 2008
    // raveCounts / (raveCounts + parentRollouts + bias * raveCounts + parentRollouts)

    val sameMover = childNode.currentPlayer.name == this.currentPlayer.name
    val amaf =
        childNode.raveWins.getValue(this.currentPlayer.name).toDouble() / childNode.raveCounts
    //    val rawWP = if (sameMover) amaf else 1.0 - amaf
    // val mean = winPercentage

    val uctRAVE = (((1.0 - beta) * winPercentage) + (beta * amaf))

    return uctRAVE + temperature * exploration
  }

  /*  fun selectChildNodeToExplore(): MCTSNode {
    val totalRollouts = this.rolloutCounts.toDouble()

    val selectableNodes = this.getUnlockedActionCount()

    childrenNodes.sortByDescending { it.evalScore }

    val maxEvalScore = childrenNodes.maxOf { it.evalScore }
    var sumExp = 0.0

    for (action in childrenNodes) {
      val expval = exp((action.evalScore - maxEvalScore).toDouble() / 1.0)
      action.expValue = expval
      sumExp += expval
    }

    // 4. Normalize to probabilities
    for (action in childrenNodes) {
      action.priorProbability = action.expValue / sumExp
    }

    var bestScore = -Double.MAX_VALUE
    var bestChildNode: MCTSNode? = null

    for (index in 0.until(selectableNodes)) {
      val score =
          calculateUCTRAVEScore(
              childNode = childrenNodes[index],
              parentRollouts = totalRollouts,
              temperature = 1.5,
              useRAVE = this.useRAVE,
              fpu = childrenNodes[index].getEdgeQ(this.meanQ),
          )

      if (score > bestScore) {
        bestScore = score
        bestChildNode = childrenNodes[index]
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
  }*/

  fun updateMeanQ() {
    var totalVisitedQ = 0.0f
    var totalVisitedCount = 0.0f

    for (childNode in this.childrenNodes) {
      if (childNode.rolloutCounts > 0) {
        totalVisitedQ += childNode.winCounts.getValue(this.currentPlayer.name)
        totalVisitedCount += childNode.rolloutCounts
      }
    }

    this.meanQ = (this.parentQ + totalVisitedQ) / (1 + totalVisitedCount)
  }

  fun selectOrExpandChild(rng: Random): MCTSNode {
    val allowedChildrenLimit = getUnlockedActionCount()

    // 1. LAZY EXPANSION: If unlocked slot is available, instantiate a new child
    if (childrenNodes.size < allowedChildrenLimit && unvisitedMoves.isNotEmpty()) {
      return expandNextChild(rng)
    }

    // update prior probabilities
    childrenNodes.sortByDescending { it.evalScore }

    val maxEvalScore = childrenNodes.maxOf { it.evalScore }
    var sumExp = 0.0

    for (action in childrenNodes) {
      val expval = exp((action.evalScore - maxEvalScore).toDouble() / 1.0)
      action.expValue = expval
      sumExp += expval
    }

    // 4. Normalize to probabilities
    for (action in childrenNodes) {
      action.priorProbability = action.expValue / sumExp
    }

    // 2. PUCT SELECTION: Otherwise, pick the best existing child via PUCT / RAVE
    val totalRollouts = this.rolloutCounts.toDouble()
    var bestScore = -Double.MAX_VALUE
    var bestChildNode: MCTSNode? = null

    for (child in childrenNodes) {
      val score =
          calculateUCTRAVEScore(
              childNode = child,
              parentRollouts = totalRollouts,
              temperature = 1.5,
              useRAVE = this.useRAVE,
              fpu = child.getEdgeQ(this.meanQ),
          )

      if (score > bestScore) {
        bestScore = score
        bestChildNode = child
      }
    }

    // Fallback if no children exist yet (should rarely happen unless unvisitedMoves was empty)
    return bestChildNode ?: expandNextChild(rng)
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
  searchInfos: MutableList<SearchInfo>? = null,
): PackedMove? {
  val availableMoves: MutableList<PackedMove> = mutableListOf()

  bitboard.generateMoves(currentPlayer, turnPhase, availableMoves)

  if (availableMoves.isEmpty()) return null

  if (rounds.last() == 0) {
    return availableMoves.random(rng)
  }

  //  availableMoves.sortByDescending { action ->
  //    scoreActions(
  //      selectedPackedMove = action,
  //      bitboard = bitboard.deepCopy(),
  //      currentPlayer = currentPlayer.liteDeepCopy(),
  //      nextPlayer = nextPlayer.liteDeepCopy(),
  //      rng = rng,
  //    )
  //  }

  //  val scratchBitboard = bitboard.deepCopy()
  //  val scratchCurrentPlayer = currentPlayer.liteDeepCopy()
  //  val scratchNextPlayer = nextPlayer.liteDeepCopy()
  //
  //// 2. Pre-calculate scores exactly ONCE per move (O(N) instead of O(N log N))
  //  for (i in availableMoves.indices) {
  //    // 3. Reset scratch states (Implement these methods to overwrite data, NOT allocate!)
  //    // e.g., scratchBitboard.whitePieces = childBitboard.whitePieces
  //    scratchBitboard.copyFrom(bitboard)
  //    scratchCurrentPlayer.copyFrom(currentPlayer)
  //    scratchNextPlayer.copyFrom(nextPlayer)
  //
  //    availableMoves[i].evaluation = scoreActions(
  //      selectedPackedMove = availableMoves[i],
  //      bitboard = scratchBitboard,
  //      currentPlayer = scratchCurrentPlayer,
  //      nextPlayer = scratchNextPlayer,
  //      rng = rng
  //    )
  //  }
  //
  //  for (i in 1 until availableMoves.size) {
  //    val currentMove = availableMoves[i]
  //    val currentScore = currentMove.evaluation
  //    var j = i - 1
  //
  //    // Shift elements that have a LOWER score to the right
  //    while (j >= 0 && availableMoves[j].evaluation < currentScore) {
  //      availableMoves[j + 1] = availableMoves[j]
  //      j--
  //    }
  //    // Insert the current move at its correct sorted position
  //    availableMoves[j + 1] = currentMove
  //  }

  val rootMCTSNode =
      MCTSNode(
          bitboard = bitboard,
          currentPlayer = currentPlayer,
          nextPlayer = nextPlayer,
          turnPhase = turnPhase,
          unvisitedMoves = availableMoves.toMutableList(),
          useRAVE = useRAVE,
          raveStats = availableMoves.associateWith { RaveStats() },
          totalActions = availableMoves.size,
      )

  if (rootMCTSNode.unvisitedMoves.isEmpty()) {
    return null
  }

  if (duration == Duration.ZERO) {
    repeat(rounds.count()) {
      var currentNode: MCTSNode? = rootMCTSNode

      while (
          (currentNode?.unvisitedMoves?.isNotEmpty() == true ||
              currentNode?.childrenNodes?.isNotEmpty() == true) &&
              !evaluateCapturedPieces(currentNode.currentPlayer) // &&
      ) {

        currentNode = currentNode.selectOrExpandChild(rng)

        if (currentNode.rolloutCounts == 0) {
          break
        }
      }

      checkNotNull(currentNode) {
        "Search Tree Traversal Failure: Encountered a null node during evaluation loop. " +
            "Verify tree expansion bounds and parent-child link validity."
      }

      //      if (currentNode.unvisitedMoves.isNotEmpty()) currentNode =
      // currentNode.expandNextChild(rng)

      val simulationActions: MutableList<SimulatedMove> = mutableListOf()

      val winner =
          simulateRandomGame(
              currentNode.bitboard.deepCopy(),
              currentNode.currentPlayer.liteDeepCopy(),
              currentNode.nextPlayer.liteDeepCopy(),
              rng = rng,
              simulationActions = simulationActions,
          )

      // Group moves by the player who made them so AMAF lookups below never cross-credit a
      // sibling using a move the *other* player happened to play during the rollout. Includes
      // both the in-tree path moves and the random-playout moves, per the RAVE definition.
      val simulationActionsByPlayer: Map<PlayerName, Set<PackedMove>> =
          collectTreePathMovesByPlayer(currentNode).apply {
            for (simulatedMove in simulationActions) {
              getOrPut(simulatedMove.player) { mutableSetOf() }.add(simulatedMove.move)
            }
          }

      while (currentNode != null && winner != null) {
        currentNode.recordWin(winner)
        currentNode.updateMeanQ()

        val parentNode = currentNode.parentNode

        // Check if actions appear in the simulation for the PARENT node's context
        if (parentNode != null) {
          val movesByParentMover =
              simulationActionsByPlayer.getValue(parentNode.currentPlayer.name)

          // Check if this sibling's action appears in the simulation
          for (siblingNode in parentNode.childrenNodes) {
            if (siblingNode.move in movesByParentMover) {
              siblingNode.updateRAVE(winner)
            }
          }

          // 2. NEW: Update unvisited moves in the parent's raveStats map
          if (parentNode.unvisitedMoves.isNotEmpty()) {
            for (unvisitedMove in parentNode.unvisitedMoves) {
              if (unvisitedMove in movesByParentMover) {
                val stats = parentNode.raveStats.getValue(unvisitedMove)
                stats.raveCounts += 1
                stats.raveWins[winner.name] = stats.raveWins.getValue(winner.name) + 1
              }
            }
          }
        }

        currentNode = parentNode
      }
    }
  } else {
    val startTime = System.currentTimeMillis()
    val endTime = startTime + duration.inWholeMilliseconds

    while (System.currentTimeMillis() < endTime) {
      var currentNode: MCTSNode? = rootMCTSNode

      while (
          (currentNode?.unvisitedMoves?.isNotEmpty() == true ||
              currentNode?.childrenNodes?.isNotEmpty() == true) &&
              !evaluateCapturedPieces(currentNode.currentPlayer) // &&
      ) {

        currentNode = currentNode.selectOrExpandChild(rng)

        if (currentNode.rolloutCounts == 0) {
          break
        }
      }

      checkNotNull(currentNode) {
        "Search Tree Traversal Failure: Encountered a null node during evaluation loop. " +
            "Verify tree expansion bounds and parent-child link validity."
      }

      //      if (currentNode.unvisitedMoves.isNotEmpty()) currentNode =
      // currentNode.expandNextChild(rng)

      // https://www.ijcai.org/Proceedings/15/Papers/112.pdf
      // https://github.com/hiive/hiivelabs-zertz-mcts/blob/12537a6be44e99f8273c9f81587191526f358a0e/src/mcts.rs
      val simulationActions: MutableList<SimulatedMove> = mutableListOf()

      val winner =
          simulateRandomGame(
              currentNode.bitboard.deepCopy(),
              currentNode.currentPlayer.liteDeepCopy(),
              currentNode.nextPlayer.liteDeepCopy(),
              rng = rng,
              simulationActions,
              endTime,
          )

      // Group moves by the player who made them so AMAF lookups below never cross-credit a
      // sibling using a move the *other* player happened to play during the rollout. Includes
      // both the in-tree path moves and the random-playout moves, per the RAVE definition.
      val simulationActionsByPlayer: Map<PlayerName, Set<PackedMove>> =
          collectTreePathMovesByPlayer(currentNode).apply {
            for (simulatedMove in simulationActions) {
              getOrPut(simulatedMove.player) { mutableSetOf() }.add(simulatedMove.move)
            }
          }

      while (currentNode != null && winner != null) {
        currentNode.recordWin(winner)
        currentNode.updateMeanQ()
        currentNode = currentNode.parentNode

        // Check if this sibling's action appears in the simulation
        if (currentNode != null && currentNode.childrenNodes.isNotEmpty()) {
          // todo update unvisitedActions

          for (siblingNode in currentNode.childrenNodes) {
            val movesByThisSiblingsMover =
                simulationActionsByPlayer.getValue(siblingNode.currentPlayer.name)
            if (siblingNode.move in movesByThisSiblingsMover) {
              siblingNode.updateRAVE(winner)
            }
          }
        }
      }
    }
  }

  fun collectMctsStats(root_node: MCTSNode, minVisits: Int = 1): SearchInfo {
    val branchingCounts: MutableList<Int> = mutableListOf()
    val turnPhases: MutableList<TurnPhase> = mutableListOf()
    val totalActions: MutableList<Int> = mutableListOf()
    val depth: MutableList<Int> = mutableListOf()

    fun traverse (node: MCTSNode, currentDepth: Int) {
      depth.add(currentDepth)
      turnPhases.add(node.turnPhase)
      totalActions.add(node.totalActions)
      // Filter children with meaningful search volume
      val activeChildren = node.childrenNodes.filter { it.rolloutCounts >= minVisits }

      if (activeChildren.isNotEmpty()) {
        branchingCounts.add(activeChildren.size)
        for (child in activeChildren) {
          traverse(child, currentDepth + 1)
        }
      }
    }

    traverse(root_node, 0)

    return SearchInfo(
      model = Model.MCTS,
      turnPhase = turnPhases,
      branchingCounts = branchingCounts,
      totalActions = totalActions,
      depths = depth,
    )
  }

  searchInfos?.add(
    collectMctsStats(rootMCTSNode)
  )

  var bestMove: PackedMove? = null
  var bestPercentage = -1f
  for (child in rootMCTSNode.childrenNodes) {
    val winPercentage = child.winPercentage(currentPlayer)
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
    simulationActions: MutableList<SimulatedMove>,
    endTime: Long = Long.MAX_VALUE,
): Player? {

  var playerWhoMadeTheLastMove: Player? = null

  // TODO if > 15 pieces and GIPF pieces in reserve rotate player

  var activePlayer = currentPlayer
  var opponentPlayer = nextPlayer

  val availableMoves = mutableListOf<PackedMove>()
  bitboard.identifyAvailableMoves(activePlayer, movesBuffer = availableMoves)

  //  val opponentMoves = mutableListOf<PossibleBitMove>()
  //  bitboard.identifyAvailableMoves(opponentPlayer, movesBuffer = opponentMoves)

  while (!evaluateCapturedPieces(activePlayer) && availableMoves.isNotEmpty()) {

    if (System.currentTimeMillis() >= endTime) {
      return null
    }

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
          ?.winner
  //  logger.info { "Player: ${winner?.name} won" }
  return winner
}

fun simulatePlayerTurn(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    rng: Random,
    simulationActions: MutableList<SimulatedMove>,
) {
  if (logger.isDebugEnabled()) {
    //		logger.info { "--- ALPHA-BETA CALLED ---" }
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

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

  while (
      when (currentPlayer.name) {
        PlayerName.WHITE -> bitboard.whiteTAMSK and bitboard.whitePotentials and boardCenterSpotMask
        PlayerName.BLACK -> bitboard.blackTAMSK and bitboard.blackPotentials and boardCenterSpotMask
      } == boardCenterSpotMask
  ) {
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

  while (
      when (currentPlayer.name) {
        PlayerName.WHITE -> bitboard.whiteTAMSK and bitboard.whitePotentials and boardCenterSpotMask
        PlayerName.BLACK -> bitboard.blackTAMSK and bitboard.blackPotentials and boardCenterSpotMask
      } == boardCenterSpotMask
  ) {
    simulatePlayerMove(
        bitboard,
        currentPlayer,
        opponentPlayer,
        rng = rng,
        simulationActions,
    )
  }

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
    simulationActions: MutableList<SimulatedMove>,
) {
  //  val initbitboard = bitboard.deepCopy()
  val possibleBitMoves = mutableListOf<PackedMove>()
  bitboard.identifyAvailableMoves(currentPlayer, columnInfos, possibleBitMoves)

  // TODO change to depth <= 0
  if (possibleBitMoves.isEmpty()) {
    return
  }

  val randomPackedMove = possibleBitMoves.random(rng)

  simulationActions.add(SimulatedMove(currentPlayer.name, randomPackedMove))

  when (randomPackedMove) {
    is PackedMove.Multiple -> {}
    is PackedMove.Single -> {
      when (randomPackedMove.value.extractMoveType()) {
        MoveType.AddPiece -> {
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
        MoveType.UnusedTamskPotential -> {
          val unusedTAMSKPotential = bitboard.removeUnusedTamskPotential(currentPlayer)

          // add the unused TAMSK Potential to the opponent's captured pieces.
          opponentPlayer.capturedPieces.add(unusedTAMSKPotential)
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
    simulationActions: MutableList<SimulatedMove>,
) {
  val removePiecesPowerset = mutableListOf<PackedMove>()
  bitboard.identifyPiecesToRemove(currentPlayer, removePiecesPowerset)

  if (removePiecesPowerset.isNotEmpty()) {
    val selectedPieceToRemove = removePiecesPowerset.random(rng)

    simulationActions.add(SimulatedMove(currentPlayer.name, selectedPieceToRemove))

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
