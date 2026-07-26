package org.example.ai.mcts

import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.engine.MoveType
import org.example.engine.PossibleBitMove
import org.example.engine.TurnPhase
import org.example.engine.determineWinner
import org.example.model.*

fun calculateUCTScore(
    parentRollouts: Double,
    childRollouts: Int,
    winPercentage: Float,
    temperature: Double = 1.5,
): Double {
  val exploration = sqrt(ln(parentRollouts).div(childRollouts.toDouble()))
  return winPercentage + temperature * exploration
}

// TODO how to figure out when a turn ends

@Serializable
data class MCTSNode(
    val bitboard: Bitboard,
    val currentPlayer: Player,
    val nextPlayer: Player,
    val parentNode: MCTSNode? = null,
    val move: PossibleBitMove? = null,
    val turnCount: Int = 0,
    val turnPhase: TurnPhase,
    val previousTurnPhases: List<TurnPhase> = emptyList(),
    val childrenNodes: MutableList<MCTSNode> = mutableListOf(),
    val winCounts: MutableMap<PlayerName, Int> =
        mutableMapOf(PlayerName.BLACK to 0, PlayerName.WHITE to 0),
    var rolloutCounts: Int = 0,
    val unvisitedMoves: MutableList<PossibleBitMove>,
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
    val selectedMove = unvisitedMoves.random(rng)

    // TODO apply move
    /**
     * TODO Does this move a TAMSK piece to the center? Does this move create 4 in a row for the
     * current player? if yes, remove pieces. Add retrieval/capture moves to
     * identifyAvailableMoves()
     */
    when (selectedMove.moveType) {
      MoveType.AddPiece -> {
        requireNotNull(selectedMove.targetBit)
        requireNotNull(selectedMove.pushDirection)

        val addAtIndex = selectedMove.targetBit
        val pushDirection = selectedMove.pushDirection
        val columnInfo = selectedMove.columnInfos.first()

        val selectedPiece = selectedMove.piece?.let { childCurrentPlayer.selectPiece(it) }

        selectedPiece?.let {
          childBitboard.addPieceToBitboard(
              addAtIndex = addAtIndex,
              pushDirection = pushDirection,
              col = columnInfo,
              piece = it,
          )
        }
            ?: selectedMove.sourceBit?.let {
              childBitboard.useTamskPotential(
                  player = childCurrentPlayer,
                  sourceIndex = it,
                  targetIndex = addAtIndex,
                  col = columnInfo,
                  pushDirection = pushDirection,
              )
            }
      }
      MoveType.UsePotential -> {
        childBitboard.usePiecePotential(
            possibleBitMove = selectedMove,
            currentPlayer = currentPlayer,
            nextPlayer = nextPlayer,
        )
      }
      MoveType.RetrieveCapturePieces -> {
        val retrievedCapturedPieces =
            childBitboard.removeSelectedPiecesToRemove(
                player = childCurrentPlayer,
                piecesToRemove = selectedMove.retrievedCapturedPiecesBit,
            )

        val retrievedPieces = retrievedCapturedPieces.mapNotNull {
          it.retrievedPiece
        }
        val capturedPieces = retrievedCapturedPieces.mapNotNull {
          it.capturedPiece
        }

        childCurrentPlayer.addPiecesToReserve(retrievedPieces)
        childCurrentPlayer.addCapturedPieces(capturedPieces)

        childCurrentPlayer.combinePieces()

        childBitboard.assertPieceCount(
            currentPlayer = childCurrentPlayer,
            nextPlayer = childNextPlayer,
        )
      }
    }

    // child Node Moves

    val currentPlayerTamskMoves = mutableListOf<PossibleBitMove>()
    childBitboard.getTamskMoves(childCurrentPlayer, currentPlayerTamskMoves)
    val currentPlayerRemovablePieces = childBitboard.identifyPiecesToRemove(childCurrentPlayer)
    val currentPlayerAvailableMoves = mutableListOf<PossibleBitMove>()
    childBitboard.identifyAvailableMoves(
        childCurrentPlayer,
        movesBuffer = currentPlayerAvailableMoves,
    )

    val nextPlayerAvailableMoves = mutableListOf<PossibleBitMove>()
    childBitboard.identifyAvailableMoves(childNextPlayer, movesBuffer = nextPlayerAvailableMoves)
    val nextPlayerTamskMoves = mutableListOf<PossibleBitMove>()
    childBitboard.getTamskMoves(childNextPlayer, nextPlayerTamskMoves)
    val nextPlayerRemovablePieces = childBitboard.identifyPiecesToRemove(childNextPlayer)

    data class ChildState(
        val currentPlayer: Player,
        val nextPlayer: Player,
        val turnPhase: TurnPhase,
        val moves: List<PossibleBitMove>,
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

          !turnHasHadNormalMove ->
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
            move = selectedMove,
            turnCount =
                if (nodeCurrentPlayer.name == this.currentPlayer.name) this.turnCount
                else this.turnCount.plus(1),
            turnPhase = nodeTurnPhase,
            previousTurnPhases =
                if (nodeCurrentPlayer.name == this.currentPlayer.name)
                    previousTurnPhases.plus(this.turnPhase)
                else emptyList(),
            unvisitedMoves = nodeMoves.toMutableList(),
        )

    this.childrenNodes.add(childNode)
    this.unvisitedMoves.remove(selectedMove)

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

  fun winPercentage(player: Player): Float {
    return winCounts[player.name]!!.div(this.rolloutCounts.toFloat())
  }

  fun selectChildNodeToExplore(): MCTSNode {
    val totalRollouts = childrenNodes.sumOf { it.rolloutCounts.toDouble() }
    var bestScore = -1.0
    var bestChildNode: MCTSNode? = null

    this.childrenNodes.forEach { childNode ->
      val score =
          calculateUCTScore(
              parentRollouts = totalRollouts,
              childRollouts = childNode.rolloutCounts,
              winPercentage = childNode.winPercentage(nextPlayer),
              temperature = 1.5,
          )

      if (score > bestScore) {
        bestScore = score
        bestChildNode = childNode
      }
    }

    return bestChildNode!!
  }
}

fun selectMoveMCTS(
    bitboard: Bitboard,
    currentPlayer: Player,
    nextPlayer: Player,
    rounds: IntRange = 0..9999,
    turnPhase: TurnPhase,
    rng: Random,
): PossibleBitMove? {
  val availableMoves = bitboard.generateMoves(currentPlayer, turnPhase)

  if (availableMoves.isEmpty()) return null

  val rootMCTSNode =
      MCTSNode(
          bitboard = bitboard,
          currentPlayer = currentPlayer,
          nextPlayer = nextPlayer,
          turnPhase = turnPhase,
          unvisitedMoves = availableMoves.toMutableList(),
      )

  if (rootMCTSNode.unvisitedMoves.isEmpty()) {
    return null
  }

  repeat(rounds.count()) {
    var currentNode: MCTSNode? = rootMCTSNode

    while (
        currentNode?.unvisitedMoves?.isEmpty() == true &&
            !evaluateCapturedPieces(currentNode.currentPlayer) &&
            !evaluatePiecesInReserve(currentNode.currentPlayer)
    ) {
      currentNode = currentNode.selectChildNodeToExplore()
    }

    checkNotNull(currentNode)

    if (currentNode.unvisitedMoves.isNotEmpty()) currentNode = currentNode.addRandomChildNode(rng)

    val winner =
        simulateRandomGame(
            currentNode.bitboard.deepCopy(),
            currentNode.currentPlayer.deepCopy(),
            currentNode.nextPlayer.deepCopy(),
          rng = rng,
        )

    while (currentNode != null && winner != null) {
      currentNode.recordWin(winner)
      currentNode = currentNode.parentNode
    }
  }

  var bestMove: PossibleBitMove? = null
  var bestPercentage = -1f
  rootMCTSNode.childrenNodes.forEach { child ->
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
  return player.capturedPieces.count { piece -> piece.type == PieceType.GIPF } == 3
}

private fun evaluatePiecesInReserve(player: Player): Boolean {
  return player.piecesInReserve.none { piece ->
    piece.potential || piece.type == PieceType.GIPF
  }
}

fun simulateRandomGame(
    bitboard: Bitboard,
    currentPlayer: Player,
    nextPlayer: Player,
    rng: Random,
): Player? {

  var playerWhoMadeTheLastMove: Player? = null

  var activePlayer = currentPlayer
  var opponentPlayer = nextPlayer

  val availableMoves = mutableListOf<PossibleBitMove>()
  bitboard.identifyAvailableMoves(activePlayer, movesBuffer = availableMoves)

  while (!evaluateCapturedPieces(activePlayer) || availableMoves.isNotEmpty()) {
    if (availableMoves.isEmpty()) break

    simulatePlayerTurn(
        bitboard = bitboard,
        currentPlayer = activePlayer,
        opponentPlayer = opponentPlayer,
        rng = rng,
    )

    playerWhoMadeTheLastMove = activePlayer
    // end of turn, rotate players
    val tempPlayer = opponentPlayer
    opponentPlayer = activePlayer
    activePlayer = tempPlayer

    availableMoves.clear()
    bitboard.identifyAvailableMoves(activePlayer, movesBuffer = availableMoves)
  }

  val winner = determineWinner(activePlayer, opponentPlayer, playerWhoMadeTheLastMove)
  //  println("Player: ${winner?.name} won")
  return winner
}

fun simulatePlayerTurn(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    rng: Random,
) {
  val isDebugEnabled = false // Toggle this to true to see detailed trace logs
  if (isDebugEnabled) {
    //		println("--- ALPHA-BETA CALLED ---")
    println("currentPlayer: $currentPlayer")
    println("opponentPlayer: $opponentPlayer")
    println("Bitboard: ${Json.encodeToString(bitboard)}")
  }

  // TODO given a list of moves, select a random move

  simulatePieceRetrievalCapture(
      bitboard,
      currentPlayer,
      opponentPlayer,
      rng,
  )

  val tamskMoves = mutableListOf<PossibleBitMove>()
  bitboard.getTamskMoves(currentPlayer, tamskMoves)

  if (tamskMoves.isNotEmpty()) {
    simulatePlayerMove(
        bitboard,
        currentPlayer,
        opponentPlayer,
        rng,
    )
  }

  simulatePlayerMove(
      bitboard,
      currentPlayer,
      opponentPlayer,
      rng = rng,
  )

  tamskMoves.clear()
  bitboard.getTamskMoves(currentPlayer, tamskMoves)

  if (tamskMoves.isNotEmpty()) {
    simulatePlayerMove(
        bitboard,
        currentPlayer,
        opponentPlayer,
        rng = rng,
    )
  }

  simulatePieceRetrievalCapture(
      bitboard,
      currentPlayer,
      opponentPlayer,
      rng,
  )

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
}

fun simulatePlayerMove(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    rng: Random,
) {
//  val initbitboard = bitboard.deepCopy()
  val possibleBitMoves = mutableListOf<PossibleBitMove>()
  bitboard.identifyAvailableMoves(currentPlayer, columnInfos, possibleBitMoves)

  // TODO change to depth <= 0
  if (possibleBitMoves.isEmpty()) {
    return
  }

  val randomPossibleBitMove = possibleBitMoves.random(rng)

  when (randomPossibleBitMove.moveType) {
    MoveType.AddPiece -> {
      requireNotNull(randomPossibleBitMove.targetBit)
      requireNotNull(randomPossibleBitMove.pushDirection)

      val addAtIndex = randomPossibleBitMove.targetBit
      val pushDirection = randomPossibleBitMove.pushDirection
      val columnInfo = randomPossibleBitMove.columnInfos.first()

      val selectedPiece = randomPossibleBitMove.piece?.let { currentPlayer.selectPiece(it) }

      selectedPiece?.let {
        bitboard.addPieceToBitboard(
            addAtIndex = addAtIndex,
            pushDirection = pushDirection,
            col = columnInfo,
            piece = it,
        )
      }
          ?: randomPossibleBitMove.sourceBit?.let {
            bitboard.useTamskPotential(
                player = currentPlayer,
                sourceIndex = it,
                targetIndex = addAtIndex,
                col = columnInfo,
                pushDirection = pushDirection,
            )
          }
    }
    MoveType.UsePotential -> {
      bitboard.usePiecePotential(
          possibleBitMove = randomPossibleBitMove,
          currentPlayer = currentPlayer,
          nextPlayer = opponentPlayer,
      )
    }
    MoveType.RetrieveCapturePieces -> {}
  }

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
}

fun simulatePieceRetrievalCapture(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    rng: Random,
) {
  val removePiecesPowerset =
      bitboard.identifyPiecesToRemove(currentPlayer).filter {
        it.retrievedCapturedPiecesBit.isNotEmpty()
      }

  if (removePiecesPowerset.isNotEmpty()) {
    val selectedPieceToRemove = removePiecesPowerset.random(rng)

    check(selectedPieceToRemove.retrievedCapturedPiecesBit.isNotEmpty()) {
      "There must be at least one column"
    }

    val retrievedCapturedPieces =
        bitboard.removeSelectedPiecesToRemove(
            player = currentPlayer,
            piecesToRemove = selectedPieceToRemove.retrievedCapturedPiecesBit,
        )

    val retrievedPieces = retrievedCapturedPieces.mapNotNull {
      it.retrievedPiece
    }
    val capturedPieces = retrievedCapturedPieces.mapNotNull {
      it.capturedPiece
    }

    currentPlayer.addPiecesToReserve(retrievedPieces)
    currentPlayer.addCapturedPieces(capturedPieces)

    currentPlayer.combinePieces()

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
  }

  bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
}
