@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.ai

import kotlin.math.abs
import kotlin.random.Random
import org.example.ai.mcts.PackedMove
import org.example.engine.determineWinner
import org.example.model.*
import kotlin.math.min

/**
 * The evaluation function does not know which states are which, but it can return a single value
 * that estimates the proportion of states with each outcome. For example, suppose our experience
 * suggests that 82% of the states encountered in the two-pawns versus one-pawn category lead to a
 * win (utility +1); 2% to a loss (0), and 16% to a draw (1/2). Then a reasonable evaluation for
 * states in the category is the expected value: (0.82 × +1)+(0.02 × 0)+ (0.16 × 1/2) = 0.90. In
 * principle, the expected value can be determined for each category of states, resulting in an
 * evaluation function that works for any state.
 *
 * In practice, this kind of analysis requires too many categories and hence too much experience to
 * estimate all the probabilities. Instead, most evaluation functions compute separate numerical
 * contributions from each feature and then combine them to find the total value. For centuries,
 * chess players have developed ways of judging the value of a position using just this idea. For
 * example, introductory chess books give an approximate material value for each piece: each pawn is
 * worth 1, a knight or bishop is worth 3, a rook 5, and the queen 9. Other features such as “good
 * pawn structure” and “king safety” might be worth half a pawn, say. These feature values are then
 * simply added up to obtain the evaluation of the position. The weights should be normalized so
 * that the sum is always within the range of a loss (0) to a win (+1).
 *
 * We said that the evaluation function should be strongly correlated with the actual chances of
 * winning, but it need not be linearly correlated: if state s is twice as likely to win as state s'
 * we don’t require that EVAL(S) be twice EVAL(S’); all we require is that EVAL(S) > EVAL(S’).
 *
 * Adding up the values of features seems like a reasonable thing to do, but in fact it involves a
 * strong assumption: that the contribution of each feature is independent of the values of the
 * other features. For this reason, current programs for chess and other games also use nonlinear
 * combinations of features. For example, a pair of bishops might be worth more than twice the value
 * of a single bishop, and a bishop is worth more in the endgame than earlier—when the move number
 * feature is high or the number of remaining pieces feature is low.
 *
 * The evaluation function should be applied only to positions that are quiescent—that is, positions
 * in which there is no pending move (such as a capturing the queen) that would wildly swing the
 * evaluation. For nonquiescent positions the IS-CUTOFF returns false, and the search continues
 * until quiescent positions are reached. This extra quiescence search is sometimes restricted to
 * consider only certain types of moves, such as capture moves, that will quickly resolve the
 * uncertainties in the position.”
 *
 * Excerpt From Artificial Intelligence: A Modern Approach Stuart J. Russell & Peter Norvig This
 * material may be protected by copyright.
 */

// based on https://github.com/schuay/gf1/blob/master/ai_minimax.c
// and Analysis and Implementation of the game Gipf (GIPFTED)

// this is slow/get called a lot
fun scoreBitboardState(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    rng: Random,
): Int {
  // TODO how to score control over the board/line
  // TODO how to score attacking positions, i.e. 4 in the row
  // TODO how to skip positions that don't improve the current player's position

  // evaluate state & calculate score
  val movesBuffer = mutableListOf<PackedMove>()
  // TODO replace with a more efficient function specifically for evaluation purposes
  //  assess playable pieces & vacant lines and use potentials
  var moves = bitboard.evaluateAvailableMoves(currentPlayer, columnInfos, movesBuffer)

  val countCapturedOpponentGIPFCount =
      currentPlayer.capturedPieces.count { it.extractPieceType() == PieceType.GIPF }
  val countCapturedOpponentGIPFScore = countCapturedOpponentGIPFCount.let { count ->
    1 shl count shl (count * 8)
  }

  val countCapturedGIPFCount =
      opponentPlayer.capturedPieces.count { it.extractPieceType() == PieceType.GIPF }
  val countCapturedGIPFPieces = countCapturedGIPFCount.let { count ->
    1 shl count shl (count * 8)
  }

  if (moves == 0 || countCapturedOpponentGIPFCount == 3 || countCapturedGIPFCount == 3) {
    val winner =
        determineWinner(currentPlayer, opponentPlayer, opponentPlayer, bitboard = bitboard)?.first
    winner?.let {
      return if (it.name == currentPlayer.name) {
        Int.MAX_VALUE - 100
      } else if (winner.name == opponentPlayer.name) {
        -(Int.MAX_VALUE - 100)
      } else {
        0 // draw
      }
    }
  }

  val countCapturedOpponentPieces =
      currentPlayer.capturedPieces
          .sumOf {
            if (it.extractPieceType() != PieceType.GIPF && it.extractPotential()) {
              2
            } else if (it.extractPieceType() != PieceType.GIPF && !it.extractPotential()) {
              1
            } else 0
          }
          .let { count ->
            1 shl minOf(count, 30)
          }

  val countCapturedPieces =
      opponentPlayer.capturedPieces
          .sumOf {
            if (it.extractPieceType() != PieceType.GIPF && it.extractPotential()) {
              2
            } else if (it.extractPieceType() != PieceType.GIPF && !it.extractPotential()) {
              1
            } else 0
          }
          .let { count ->
            1 shl minOf(count, 30)
          }

  // current player'S available moves
  //  val moves = mutableListOf<PossibleBitMove>()
  //  bitboard.identifyAvailableMoves(currentPlayer, columnInfos, moves)
  val maxMovesBase = 1 shl 30
  val availableMoves = maxMovesBase shr minOf(moves, 30)

  //      currentPlayer.piecesInReserve.count {
  //        it.extractPotential() || it.extractPieceType() == PieceType.GIPF
  //      }

  val currentPiecesInPlay =
      when (currentPlayer.name) {
        PlayerName.WHITE -> {
          bitboard.whitePieces
        }
        PlayerName.BLACK -> {
          bitboard.blackPieces
        }
      }.countOneBits()

  // opponent's available moves
  moves = bitboard.evaluateAvailableMoves(opponentPlayer, columnInfos, movesBuffer)
  val opponentAvailableMoves = maxMovesBase shr minOf(moves, 30)

  //  opponentPlayer.piecesInReserve.count {
  //        it.extractPotential() || it.extractPieceType() == PieceType.GIPF
  //      }

  val opponentPiecesInPlay =
      when (opponentPlayer.name) {
        PlayerName.WHITE -> {
          bitboard.whitePieces // how to treat pieces with potental
        }
        PlayerName.BLACK -> {
          bitboard.blackPieces
        }
      }.countOneBits()

  val gipfPiecesWeight = 0.33f
  val capturedPiecesWeight = 0.33f
  val piecesInReserve = 0.33f

  fun capturedValue(n: Double, opponent: Double): Double {
    return (n - opponent) * (n + opponent) * 10.0
  }

  fun scoreClusters(player: Player): Int {
    var sum = 0
    val pieces = if (player.name == PlayerName.WHITE) bitboard.whitePieces else bitboard.blackPieces

    for (cluster in clusterArray) {
      val k = (pieces and cluster).countOneBits()
      val n = cluster.countOneBits()

      sum += (((1 shl k) - 1) * 1024) / ((1 shl n) - 1)
    }
    return sum
  }

  fun scoreRunsOfThree(player: Player): Int {
    var sum = 0
    val pieces = if (player.name == PlayerName.WHITE) bitboard.whitePieces else bitboard.blackPieces

    for (mask in reducedThreeRunSubmasks) {
      if ((pieces and mask) == mask) {
        sum += 200
      }
    }
    return sum
  }

  fun centreControl(player: Player): Int {
    var weight = 0
    var pieces = if (player.name == PlayerName.WHITE) bitboard.whitePieces else bitboard.blackPieces

    while (pieces != 0UL) {
      val k = pieces.countTrailingZeroBits()
      weight += 128 shl bitDistanceWeights[abs(k - 18)]
      pieces = pieces and (pieces - 1UL)
    }

    return weight
  }

  fun tamskDistanceFromCentre(player: Player): Int {
    var weight = 0
    var pieces =
        if (player.name == PlayerName.WHITE) bitboard.whiteTAMSK and bitboard.whitePotentials
        else bitboard.blackTAMSK and bitboard.blackPotentials

    while (pieces != 0UL) {
      val k = pieces.countTrailingZeroBits()
      weight += 128 shl bitDistanceWeights[abs(k - 18)]
      pieces = pieces and (pieces - 1UL)
    }

    return weight
  }

  val currentValue =
      20 +
          countCapturedOpponentGIPFScore +
          countCapturedOpponentPieces +
          availableMoves +
          currentPiecesInPlay +
          centreControl(currentPlayer) +
          tamskDistanceFromCentre(currentPlayer) +
          scoreClusters(currentPlayer) +
          scoreRunsOfThree(currentPlayer)

  val opponentValue =
      -(20 +
          countCapturedGIPFPieces +
          countCapturedPieces +
          opponentAvailableMoves +
          opponentPiecesInPlay +
          centreControl(opponentPlayer) +
          tamskDistanceFromCentre(opponentPlayer) +
          scoreClusters(opponentPlayer) +
          scoreRunsOfThree(opponentPlayer))

  val noise = 0 // rng.nextInt(-50, 50)

  return (((currentValue + opponentValue) + noise) * 1000) /
      (currentValue + abs(opponentValue) + abs(noise))
}


// OPTIMIZATION 1: Move this OUTSIDE the function (e.g., at the file level or in a Companion Object).
// This completely removes the severe GC penalty of allocating a new list every evaluation call.
private val sharedMovesBuffer = ThreadLocal.withInitial { ArrayList<PackedMove>(64) }

fun fasterEvaluation(
  bitboard: Bitboard,
  currentPlayer: Player,
  opponentPlayer: Player,
  rng: Random,
): Int {

  // 1. Resolve player sides once (eliminates repeated branches)
  val isWhite = currentPlayer.name == PlayerName.WHITE
  val myPieces = if (isWhite) bitboard.whitePieces else bitboard.blackPieces
  val oppPieces = if (isWhite) bitboard.blackPieces else bitboard.whitePieces
  val myPotentials = if (isWhite) bitboard.whitePotentials else bitboard.blackPotentials
  val oppPotentials = if (isWhite) bitboard.blackPotentials else bitboard.whitePotentials
  val myTAMSK = if (isWhite) bitboard.whiteTAMSK else bitboard.blackTAMSK
  val oppTAMSK = if (isWhite) bitboard.blackTAMSK else bitboard.whiteTAMSK

  // 2. Tally captured pieces WITHOUT allocations (no .count, no .sumOf, no Iterators)
  var myCapturedOppGipfCount = 0
  var myCapturedPoints = 0
  val myCaptured = currentPlayer.capturedPieces
  for (i in myCaptured.indices) {
    val p = myCaptured[i]
    if (p.extractPieceType() == PieceType.GIPF) {
      myCapturedOppGipfCount++
    } else {
      myCapturedPoints += if (p.extractPotential()) 2 else 1
    }
  }

  var oppCapturedMyGipfCount = 0
  var oppCapturedPoints = 0
  val oppCaptured = opponentPlayer.capturedPieces
  for (i in oppCaptured.indices) {
    val p = oppCaptured[i]
    if (p.extractPieceType() == PieceType.GIPF) {
      oppCapturedMyGipfCount++
    } else {
      oppCapturedPoints += if (p.extractPotential()) 2 else 1
    }
  }

  // 3. Move Generation (Reusing buffer to prevent GC pauses)
  val movesBuffer = sharedMovesBuffer.get()
  movesBuffer.clear()

//  val myMovesCount = bitboard.evaluateAvailableMoves(currentPlayer, columnInfos, movesBuffer)

  // 4. Terminal State Check
  if ( myCapturedOppGipfCount == 3 || oppCapturedMyGipfCount == 3) {
    val winner = determineWinner(currentPlayer, opponentPlayer, opponentPlayer, bitboard = bitboard)?.first
    if (winner != null) {
      return if (winner.name == currentPlayer.name) {
        Int.MAX_VALUE - 100
      } else if (winner.name == opponentPlayer.name) {
        -(Int.MAX_VALUE - 100)
      } else {
        0 // draw
      }
    }
  }

  movesBuffer.clear()
//  val oppMovesCount = bitboard.evaluateAvailableMoves(opponentPlayer, columnInfos, movesBuffer)

  // 5. Precalculate Scores (Simplified bitwise math)
  val maxMovesBase = 1 shl 30
//  val myAvailableMovesScore = maxMovesBase shr min(myMovesCount, 30)
//  val oppAvailableMovesScore = maxMovesBase shr min(oppMovesCount, 30)

  val myCapturedOppGipfScore = 1 shl (myCapturedOppGipfCount * 9)
  val oppCapturedMyGipfScore = 1 shl (oppCapturedMyGipfCount * 9)
  val myCapturedPiecesScore = 1 shl min(myCapturedPoints, 30)
  val oppCapturedPiecesScore = 1 shl min(oppCapturedPoints, 30)

  // 6. Inline calculations for Centre Control & Tamsk
  // Uses inline absolute value calculation (diff/absDiff) to skip Math.abs overhead.
  var myCentreWeight = 0
  var myTamskWeight = 0
  var oppCentreWeight = 0
  var oppTamskWeight = 0

  var temp = myPieces
  while (temp != 0UL) {
    val k = temp.countTrailingZeroBits()
    val diff = k - 18
    myCentreWeight += 128 shl bitDistanceWeights[if (diff < 0) -diff else diff]
    temp = temp and (temp - 1UL)
  }

  temp = myTAMSK and myPotentials
  while (temp != 0UL) {
    val k = temp.countTrailingZeroBits()
    val diff = k - 18
    myTamskWeight += 128 shl bitDistanceWeights[if (diff < 0) -diff else diff]
    temp = temp and (temp - 1UL)
  }

  temp = oppPieces
  while (temp != 0UL) {
    val k = temp.countTrailingZeroBits()
    val diff = k - 18
    oppCentreWeight += 128 shl bitDistanceWeights[if (diff < 0) -diff else diff]
    temp = temp and (temp - 1UL)
  }

  temp = oppTAMSK and oppPotentials
  while (temp != 0UL) {
    val k = temp.countTrailingZeroBits()
    val diff = k - 18
    oppTamskWeight += 128 shl bitDistanceWeights[if (diff < 0) -diff else diff]
    temp = temp and (temp - 1UL)
  }

  // 7. Inline Clusters & Runs
  var myClusters = 0
  var oppClusters = 0
  for (i in clusterArray.indices) {
    val cluster = clusterArray[i]
    val denom = (1 shl cluster.countOneBits()) - 1
    if (denom != 0) {
      val myK = (myPieces and cluster).countOneBits()
      if (myK > 0) myClusters += (((1 shl myK) - 1) * 1024) / denom

      val oppK = (oppPieces and cluster).countOneBits()
      if (oppK > 0) oppClusters += (((1 shl oppK) - 1) * 1024) / denom
    }
  }

  var myRuns = 0
  var oppRuns = 0
  for (i in reducedThreeRunSubmasks.indices) {
    val mask = reducedThreeRunSubmasks[i]
    if ((myPieces and mask) == mask) myRuns += 200
    if ((oppPieces and mask) == mask) oppRuns += 200
  }

  // 8. Final Calculation
  val myTotal = 20 +
      myCapturedOppGipfScore +
      myCapturedPiecesScore +
//      myAvailableMovesScore +
      myPieces.countOneBits() +
      myCentreWeight +
      myTamskWeight +
      myClusters +
      myRuns

  val oppTotal = 20 +
      oppCapturedMyGipfScore +
      oppCapturedPiecesScore +
//      oppAvailableMovesScore +
      oppPieces.countOneBits() +
      oppCentreWeight +
      oppTamskWeight +
      oppClusters +
      oppRuns

  // Returns equivalent of ((currentValue + opponentValue) * 1000) / (currentValue + abs(opponentValue))
  return ((myTotal - oppTotal) * 1000) / (myTotal + oppTotal)
}

fun doActionGetTurnPhase(
  selectedPackedMove: PackedMove,
  childBitboard: Bitboard,
  childCurrentPlayer: Player,
  childNextPlayer: Player
): TurnPhase? {
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

        MoveType.UnusedTamskPotential -> {
          val unusedTAMSKPotential = childBitboard.removeUnusedTamskPotential(childCurrentPlayer)

          // add the unused TAMSK Potential to the opponent's captured pieces.
          childNextPlayer.capturedPieces.add(unusedTAMSKPotential)
        }

        MoveType.RetrieveCapturePieces -> {}
      }
    }
  }
  return turnPhase
}

fun scoreActions(
  selectedPackedMove: PackedMove,
  bitboard: Bitboard,
  currentPlayer: Player,
  nextPlayer: Player,
  rng: Random,
): Int {
  when (selectedPackedMove) {
    is PackedMove.Multiple -> {
      val retrievedCapturedPieces = mutableListOf<UInt>()

      bitboard.removeSelectedPieces(
        player = currentPlayer,
        piecesToRemove = selectedPackedMove.values.distinct(),
        movesBuffer = retrievedCapturedPieces,
      )

      currentPlayer.addRetrievedCapturedPieces(retrievedCapturedPieces)

      currentPlayer.combinePieces()

      bitboard.assertPieceCount(
        currentPlayer = currentPlayer,
        nextPlayer = nextPlayer,
      )

      return scoreBitboardState(
        bitboard = bitboard,
        currentPlayer = currentPlayer,
        opponentPlayer = nextPlayer,
        rng = rng,
      )
    }

    is PackedMove.Single -> {
      val selectedMove = selectedPackedMove.value
      when (selectedMove.extractMoveType()) {
        MoveType.AddPiece -> {
          if (selectedMove.extractSourceBit() == boardCenterSpotMask) {
            bitboard.useTamskPotential(selectedMove)
          } else {
            val selectedPiece =
              selectedMove.onlyPiece().let { currentPlayer.selectPiece(it) }

            selectedPiece?.let {
              bitboard.addPieceToBitboard(
                move = selectedMove,
              )
            }
          }

          return fasterEvaluation(
            bitboard = bitboard,
            currentPlayer = currentPlayer,
            opponentPlayer = nextPlayer,
            rng = rng,
          )
        }

        MoveType.UsePotential -> {
          val selectedMove = selectedPackedMove.value
          bitboard.usePiecePotential(
            move = selectedMove,
          )

          return scoreBitboardState(
            bitboard = bitboard,
            currentPlayer = currentPlayer,
            opponentPlayer = nextPlayer,
            rng = rng,
          )
        }

        MoveType.UnusedTamskPotential -> {
          val unusedTAMSKPotential = bitboard.removeUnusedTamskPotential(currentPlayer)

          // add the unused TAMSK Potential to the opponent's captured pieces.
          nextPlayer.capturedPieces.add(unusedTAMSKPotential)

          return scoreBitboardState(
            bitboard = bitboard,
            currentPlayer = currentPlayer,
            opponentPlayer = nextPlayer,
            rng = rng,
          )
        }

        MoveType.RetrieveCapturePieces -> {}
      }
    }
  }

  return 0
}