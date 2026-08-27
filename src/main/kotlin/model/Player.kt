@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.example.ai.humanEvaluation.*
import org.example.ai.mcts.PackedMove
import org.example.ai.mcts.selectMoveMCTS
import org.example.engine.ExperienceCollector
import org.example.engine.TranspositionTable
import kotlin.time.Duration.Companion.milliseconds

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

enum class PlayerName {
  WHITE,
  BLACK,
}

enum class Model {
  MINIMAX,
  MCTS,
  NEURAL_NETWORK,
}

fun getModel(model: String): Model {
  return when (model) {
    "MCTS",
    "mcts" -> Model.MCTS
    "MINIMAX",
    "minimax" -> Model.MINIMAX
    "nn",
    "NN",
    "NEURAL_NETWORK",
    "neural_network" -> Model.NEURAL_NETWORK
    else -> Model.MCTS
  }
}

enum class Strength(
    val difficulty: Int,
    val minimaxDepth: Int,
    val mctsRounds: IntRange,
    val duration: Duration,
) {
  RANDOM(
      difficulty = 0,
      minimaxDepth = 1,
      mctsRounds = 0..0,
      duration = 1.5.seconds,
  ),
  GREEDY(
      difficulty = 1,
      minimaxDepth = 1,
      mctsRounds = 0..0,
      duration = 1.5.seconds,
  ),
  EASY(
      difficulty = 2,
      minimaxDepth = 3,
      mctsRounds = 0..999,
      duration = 1.5.seconds,
  ),
  MEDIUM(
      difficulty = 3,
      minimaxDepth = 5,
      mctsRounds = 0..2499,
      duration = 5.seconds,
  ),
  HARD(
      difficulty = 4,
      minimaxDepth = 7,
      mctsRounds = 0..4999,
      duration = 10.seconds,
  ),
  NULL(
    difficulty = 0,
    minimaxDepth = 1,
    mctsRounds = 0..0,
    duration = Duration.ZERO,
  )
}

fun getStrength(strength: String): Strength {
  return when (strength) {
    "random",
    "RANDOM" -> Strength.RANDOM
    "greedy",
    "GREEDY" -> Strength.GREEDY
    "easy",
    "EASY" -> Strength.EASY
    "med",
    "medium",
    "MEDIUM" -> Strength.MEDIUM
    "hard",
    "HARD" -> Strength.HARD
    else -> Strength.EASY
  }
}

@Serializable
data class Player(
    val name: PlayerName,
    @EncodeDefault val model: Model = Model.NEURAL_NETWORK,
    @EncodeDefault val strength: Strength = Strength.RANDOM,
    val piecesInReserve: MutableList<UInt> = mutableListOf(),
    val capturedPieces: MutableList<UInt> = mutableListOf(),
    @EncodeDefault val timeControl: Boolean = false,
    @EncodeDefault val timeDuration: Duration = 1000.milliseconds,
    @EncodeDefault val useRAVE: Boolean = false,
    @EncodeDefault val enableFPU: Boolean = false,
    @EncodeDefault val enablePW: Boolean = false,
    @EncodeDefault val iterations: Int = 0,
    @EncodeDefault val depth: Int = 0,
    @Transient val collector: ExperienceCollector? = ExperienceCollector(),
    @Transient val transpositionTable: TranspositionTable = TranspositionTable(),
    @Transient val killerMoves: List<UIntArray> = List(2) { UIntArray(64) },
    @Transient val captureMoves: List<UIntArray> = List(2) { UIntArray(64) },
    @Transient val historyTable: List<IntArray> = List(7) { IntArray(40) },
) {
  fun deepCopy(copyCollector: Boolean = false): Player {
    //    val string = Json.encodeToString(serializer(), this)
    //    return Json.decodeFromString(serializer(), string)

    return Player(
        name = this.name,
        model = this.model,
        strength = this.strength,
        piecesInReserve = this.piecesInReserve.toMutableList(),
        capturedPieces = this.capturedPieces.toMutableList(),
        timeControl = this.timeControl,
        useRAVE = this.useRAVE,
        timeDuration = this.timeDuration,
        enableFPU = this.enableFPU,
        enablePW = this.enablePW,
        iterations = this.iterations,
        depth = this.depth,
        collector = if (copyCollector) this.collector else null,
        transpositionTable = this.transpositionTable,
        killerMoves = this.killerMoves,
        captureMoves = this.captureMoves,
        historyTable = this.historyTable,
    )
  }

  fun liteDeepCopy(copyCollector: Boolean = false): Player {
    return Player(
        name = this.name,
        model = this.model,
        strength = this.strength,
        piecesInReserve = this.piecesInReserve.toMutableList(),
        capturedPieces = this.capturedPieces.toMutableList(),
        timeControl = this.timeControl,
        timeDuration = this.timeDuration,
        useRAVE = this.useRAVE,
        enableFPU = this.enableFPU,
        enablePW = this.enablePW,
        iterations = this.iterations,
        depth = this.depth,
        collector = if (copyCollector) this.collector else null,
        transpositionTable = this.transpositionTable,
        killerMoves = emptyList(),
        captureMoves = emptyList(),
        historyTable = emptyList(),
    )
  }

  fun copyFrom(source: Player) {
    // Note: We skip 'val' properties like name, model, strength
    // because they are immutable and assumed identical between the two player instances.

    // 1. Copy MutableLists WITHOUT triggering Iterator allocations
    this.piecesInReserve.clear()
    for (i in 0 until source.piecesInReserve.size) {
      this.piecesInReserve.add(source.piecesInReserve[i])
    }

    this.capturedPieces.clear()
    for (i in 0 until source.capturedPieces.size) {
      this.capturedPieces.add(source.capturedPieces[i])
    }

    // 2. Copy nested Arrays (Zero allocations)
    //  for (i in this.killerMoves.indices) {
    //    source.killerMoves[i].copyInto(this.killerMoves[i])
    //  }
    //  for (i in this.captureMoves.indices) {
    //    source.captureMoves[i].copyInto(this.captureMoves[i])
    //  }
    //  for (i in this.historyTable.indices) {
    //    source.historyTable[i].copyInto(this.historyTable[i])
    //  }
  }

  fun addRetrievedCapturedPieces(pieces: List<UInt>) {
    val preRemovalReserve = piecesInReserve.toList()
    val preCapturedPieces = capturedPieces.toList()

    var removedReserve = 0
    var removedCapturedPiece = 0

    for (piece in pieces) {
      if (piece.extractPieceColor() == this.name) {
        this.piecesInReserve.add(piece.onlyPiece())
        removedReserve++
      }
      if (piece.extractPieceColor() != this.name) {
        this.capturedPieces.add(piece.onlyPiece())
        removedCapturedPiece++
      }
    }

    val capturedSum = removedReserve + removedCapturedPiece

    check(capturedSum == pieces.size) {
      "Piece Accounting Mismatch: Sum of captured piece counters ($capturedSum) does not match actual active piece list size (${pieces.size}). " +
          "Tracked pieces count: $capturedSum | Actual remaining pieces in list: ${pieces.size}."
    }
  }

  fun removeRetrievedCapturedPieces(pieces: List<UInt>) {
    val preRemovalReserve = piecesInReserve.toList()
    val preCapturedPieces = capturedPieces.toList()

    var removedReserve = 0
    var removedCapturedPiece = 0

    for (piece in pieces) {
      if (piece.extractPieceColor() == this.name) {
        this.piecesInReserve.remove(piece.onlyPiece())
        removedReserve++
      }
      if (piece.extractPieceColor() != this.name) {
        this.capturedPieces.remove(piece.onlyPiece())
        removedCapturedPiece++
      }
    }

    val capturedSum = removedReserve + removedCapturedPiece

    check(capturedSum == pieces.size) {
      "Piece Accounting Mismatch: Sum of captured piece counters ($capturedSum) does not match actual active piece list size (${pieces.size}). " +
          "Tracked pieces count: $capturedSum | Actual remaining pieces in list: ${pieces.size}."
    }
  }

  /** Select a piece from the reserve and make a move */
  fun selectPiece(piece: UInt): UInt {
    val initialSize = piecesInReserve.size

    val selectedPiece =
        this.piecesInReserve.firstOrNull {
          it == piece.onlyPiece()
        }
    check(selectedPiece != null) {
      "$piece was not found!"
    }

    this.piecesInReserve.remove(selectedPiece)

    val pieceCountDifference = initialSize - piecesInReserve.size

    //    logger.info { "" + (//        "the delta was $pieceCountDifference (Initial: $initialSize,
    // Current:
    // ${piecesInReserve.size})"
    // ) }

    check(pieceCountDifference == 1) {
      "Reserve decrement failure! Expected exactly 1 piece to be removed from the reserve, " +
          "but the delta was $pieceCountDifference (Initial: $initialSize, Current: ${piecesInReserve.size})."
    }

    return selectedPiece
  }

  fun getNumberOfPiecesInReserve(): Int {
    return this.piecesInReserve.size
  }

  fun combinePieces(): List<UInt> {
    // --- 0. CONFIGURABLE DEBUGGING ---
    if (logger.isDebugEnabled()) {
      logger.info { "--- COMBINE PIECES CALLED ---" }
      logger.info { "Reserve size before combining: ${this.piecesInReserve.size}" }
    }

    // --- 1. SINGLE-PASS PARTITIONING ---
    // Instead of filtering 5 times, we split the reserve once.
    // piecesToKeep = GIPF pieces OR already stacked pieces
    // piecesToCombine = Regular pieces eligible for stacking
    val (piecesToKeep: List<UInt>, piecesToCombine: List<UInt>) =
        this.piecesInReserve.partition {
          it.extractPotential() || it.extractPieceType() == PieceType.GIPF
        }

    if (piecesToCombine.isEmpty()) {
      logger.debug { "No combinable pieces found. Exiting." }
      return emptyList()
    }

    // --- 2. GROUP AND PROCESS ---
    val newlyStackedPotentials = mutableListOf<UInt>()
    val leftoverPieces = mutableListOf<UInt>()

    // Group the combinable pieces by their specific type
    val groupedCombinable = piecesToCombine.groupBy { it.extractPieceType() }

    for ((type, pieces) in groupedCombinable) {
      val count = pieces.size
      val pairs = count / 2
      val remainder = count % 2

      if (logger.isDebugEnabled()) {
        logger.info {
          "" +
              ("  -> Processing Type: $type | Total: $count | Forming $pairs pairs, $remainder leftover.")
        }
      }

      // Create the newly stacked (Potential) pieces
      type?.let { pieceType ->
        repeat(pairs) {
          newlyStackedPotentials.add(
              0u.createPiece(pieceType, this.name, true)
              // Piece(
              //     abbreviation = this.abbreviation + pieceType.name.first(),
              //     potential = true,
              //     colorName = this.name,
              //     type = pieceType,
              // )
          )
        }

        // Create the leftover single piece (if there was an odd number)
        if (remainder == 1) {
          leftoverPieces.add(
              0u.createPiece(pieceType, this.name, false)
              // Piece(
              //     abbreviation = this.abbreviation + pieceType.name.first(),
              //     potential = false,
              //     colorName = this.name,
              //     type = pieceType,
              // )
          )
        }
      }
    }

    // --- 3. STATE VALIDATION (Conservation of Mass) ---
    // A crucial check: The total number of "atomic" pieces shouldn't change.
    // A potential piece counts as 2, a regular piece counts as 1.
    val atomicCountBefore = this.piecesInReserve.sumOf { if (it.extractPotential()) 2 else 1 }
    val atomicCountAfter =
        piecesToKeep.sumOf { if (it.extractPotential()) 2 else 1 } +
            (newlyStackedPotentials.size * 2) +
            leftoverPieces.size

    check(atomicCountBefore == atomicCountAfter) {
      "CRITICAL STATE ERROR: Piece conservation violated. Atomic pieces before: $atomicCountBefore | After: $atomicCountAfter"
    }

    // --- 4. MUTATE STATE ---
    this.piecesInReserve.clear()
    this.piecesInReserve.addAll(piecesToKeep)
    this.piecesInReserve.addAll(leftoverPieces)
    this.piecesInReserve.addAll(newlyStackedPotentials)

    if (logger.isDebugEnabled()) {
      logger.info {
        "Successfully generated ${newlyStackedPotentials.size} new potential pieces."
      }
      logger.info { "Reserve size after combining: ${this.piecesInReserve.size}" }
      logger.info { "--- COMBINE PIECES COMPLETED ---" }
    }

    // FIXED: Only return the actual newly formed stacks, omitting the leftover singletons
    return newlyStackedPotentials
  }

  fun uncombinePieces(newlyStackedPieces: List<UInt>) {
    // --- 0. CONFIGURABLE DEBUGGING ---
    if (logger.isDebugEnabled()) {
      logger.info { "--- UNCOMBINE PIECES CALLED ---" }
      logger.info { "Newly stacked pieces count: ${newlyStackedPieces.size}" }
    }

    val preReservePotentials =
        piecesInReserve.count {
          it.extractPotential()
        } * 2
    val preReserveBasics = piecesInReserve.count { !it.extractPotential() }

    // Early exit for cleaner control flow
    if (newlyStackedPieces.isEmpty()) {
      logger.debug { "No newly stacked pieces to process. Exiting." }
      return
    }

    // --- 1. FILTER & VALIDATE ---
    // Extract only the pieces that are currently marked as potentials
    //    val piecesToUncombine = newlyStackedPieces.filter { it.extractPotential() }

    //    if (newlyStackedPieces.isEmpty()) {
    //      logger.debug { "None of the newly stacked pieces are 'potential'. Exiting." }
    //      return
    //    }

    if (logger.isDebugEnabled()) {
      logger.info { "Found ${newlyStackedPieces.size} potential piece(s) to uncombine." }
      logger.info { "Reserve size before uncombining: ${this.piecesInReserve.size}" }
    }

    // --- 2. GENERATE UNCOMBINED PIECES ---
    // OPTIMIZATION: flatMap cleanly replaces map { MutableList(...) }.flatten()
    val unstackedPieces: List<UInt> = newlyStackedPieces.flatMap { piece ->
      requireNotNull(piece.extractPieceType()) { "Piece Type cannot be null. $piece." }
      listOfNotNull(
          piece.extractPieceType()?.let {
            0u.createPiece(it, piece.extractPieceColor(), potential = false)
          },
          piece.extractPieceType()?.let {
            0u.createPiece(it, piece.extractPieceColor(), potential = false)
          },
      )
    }

    // --- 3. MUTATE STATE (With Post-Condition Checks) ---

    // Safely remove the combined potentials
    for (piece in newlyStackedPieces) {
      val wasRemoved = this.piecesInReserve.remove(piece)
      check(wasRemoved) {
        "STATE ERROR: Cannot uncombine piece; ${piece.extractPieceColor()} ${piece.extractPieceType()} (Potential) was not found in the reserve."
      }
    }

    // Safely add the unstacked pieces back
    val wereAdded = this.piecesInReserve.addAll(unstackedPieces)
    check(wereAdded || unstackedPieces.isEmpty()) {
      "STATE ERROR: Failed to add the newly unstacked pieces to the reserve."
    }

    val postReservePotentials =
        piecesInReserve.count {
          it.extractPotential()
        } * 2
    val postReserveBasics = piecesInReserve.count { !it.extractPotential() }

    if (logger.isDebugEnabled()) {
      logger.info {
        "Successfully generated and added ${unstackedPieces.size} regular pieces."
      }
      logger.info { "Reserve size after uncombining: ${this.piecesInReserve.size}" }
      logger.info { "Pre Uncombine Potential Pieces: $preReservePotentials" }
      logger.info { "Pre Uncombine Basic Pieces: $preReserveBasics" }
      logger.info { "Post Uncombine Potential Pieces: $postReservePotentials" }
      logger.info { "Post Uncombine Basic Pieces: $postReserveBasics" }
      logger.info { "--- UNCOMBINE PIECES COMPLETED ---" }
    }
  }
}

fun Player.updateKillerMoves(move: UInt, depth: Int) {
  val firstKiller = killerMoves[0][depth]

  if (firstKiller != move) {
    killerMoves[1][depth] = firstKiller
    killerMoves[0][depth] = move
  }
}

fun Player.updateCaptureMoves(move: UInt, depth: Int) {
  val firstCapture = captureMoves[0][depth]

  if (firstCapture != move || firstCapture.extractTargetBit() != move.extractTargetBit()) {
    captureMoves[1][depth] = firstCapture
    captureMoves[0][depth] = move
  }
}

fun Player.updateHistoryMoves(move: UInt, depth: Int) {
  val pieceIndex = move.extractPieceType()?.ordinal
  val nodeIndex = move.extractTargetBit().countTrailingZeroBits()

  if (pieceIndex == null) return

  historyTable[pieceIndex][nodeIndex] += (depth * depth)

  // 3. Decay the table if it risks dominating the scores
  if (historyTable[pieceIndex][nodeIndex] > MAX_HISTORY) {
    historyTable[pieceIndex][nodeIndex] = historyTable[pieceIndex][nodeIndex] shr 1
  }
}

suspend fun Player.selectMove(
    turnPhase: TurnPhase,
    bitboard: Bitboard,
    currentPlayer: Player,
    opponent: Player,
    rng: Random,
    searchInfos: MutableList<SearchInfo> = mutableListOf(),
): PackedMove? {
  return when (model) {
    Model.MINIMAX -> {
      when (turnPhase) {
        TurnPhase.PlayerInputWindow,
        TurnPhase.ExtraMove -> {

          if (timeControl) {
            transpositionTable.newSearch()
            var remainingTime = timeDuration
            var startingDepth = 1
            var bestMove: PackedMove? = null

            val startTime = System.currentTimeMillis()
            val endTime = startTime + timeDuration.inWholeMilliseconds

            withTimeoutOrNull(timeDuration) {
              runInterruptible {
                while (!Thread.interrupted()) {
                  val searchInfo =
                    SearchInfo(
                      model = Model.MINIMAX,
                      strength = timeDuration.toString(),
                    )
                  searchInfo.turnPhase.add(turnPhase)

                  if (Thread.interrupted()) {
                    break
                  }

                  val (move, elapsed) =
                    measureTimedValue {
                        alphaBetaNgMxSearch(
                          maxDepth = startingDepth,
                          bitboard = bitboard.deepCopy(),
                          currentPlayer = currentPlayer.deepCopy(),
                          opponentPlayer = opponent.deepCopy(),
                          alphaBetaScore = AlphaBetaScoreBitPacked(),
                          rng = rng,
                          depth = 0,
                          turnPhase = turnPhase,
                          transpositionTable = transpositionTable,
                          endTime = endTime,
                          searchInfo = searchInfo,
                        )
                          .move
                      }

                  searchInfos.add(searchInfo)
                  remainingTime -= elapsed
                  startingDepth += 1
                  bestMove = move
                  // if there is not enough time remaining break
//                  if (remainingTime < elapsed) break
                }
              }
            }


            bestMove
          } else {
            transpositionTable.newSearch()

            var startingDepth = 1
            var bestMove: PackedMove? = null

            while (startingDepth <= depth) {
              val searchInfo =
                  SearchInfo(
                      model = Model.MINIMAX,
                      strength = depth.toString(),
                  )
              searchInfo.turnPhase.add(turnPhase)

              val move =
                  alphaBetaNgMxSearch(
                          maxDepth = startingDepth,
                          bitboard = bitboard.deepCopy(),
                          currentPlayer = currentPlayer.deepCopy(),
                          opponentPlayer = opponent.deepCopy(),
                          alphaBetaScore = AlphaBetaScoreBitPacked(),
                          rng = rng,
                          depth = 0,
                          turnPhase = turnPhase,
                          transpositionTable = transpositionTable,
                          searchInfo = searchInfo,
                      )
                      .move

              searchInfos.add(searchInfo)
              bestMove = move
              startingDepth += 1
            }

            bestMove
          }
        }
        TurnPhase.PieceRemoval -> {
          if (timeControl) {
            transpositionTable.newSearch()
            //            var remainingTime = strength.duration
            var startingDepth = 1
            var bestMove: PackedMove? = null

            val startTime = System.currentTimeMillis()
            val endTime = startTime + timeDuration.inWholeMilliseconds

            withTimeoutOrNull(timeDuration) {
              runInterruptible {
                while (
                  System.currentTimeMillis() < endTime /*|| startingDepth <= strength.minimaxDepth*/
                ) {
                  val searchInfo =
                    SearchInfo(
                      model = Model.MINIMAX,
                      strength = timeDuration.toString(),
                    )
                  searchInfo.turnPhase.add(turnPhase)

                  val (move, elapsed) =
                    measureTimedValue {
                      resolveBoardRemovals(
                        maxDepth = startingDepth,
                        currentPlayer = currentPlayer.deepCopy(),
                        opponentPlayer = opponent.deepCopy(),
                        bitboard = bitboard.deepCopy(),
                        alphaBetaScore = AlphaBetaScoreBitPacked(),
                        rng = rng,
                        depth = 0,
                        caller = "PLAYER $name selectMove() @ ${startingDepth}",
                        transpositionTable = transpositionTable,
                        endTime = endTime,
                        searchInfo = searchInfo,
                      )
                        .move
                    }

                  searchInfos.add(searchInfo)
                  //              remainingTime -= elapsed
                  bestMove = move
                  startingDepth += 1
                  // if there is not enough time remaining break
                  //              if (remainingTime < elapsed) break
                }
              }
            }

            bestMove
          } else {
            transpositionTable.newSearch()

            var startingDepth = 1
            var bestMove: PackedMove? = null

            while (startingDepth <= depth) {
              val searchInfo =
                  SearchInfo(
                      model = Model.MINIMAX,
                      strength = depth.toString(),
                  )
              searchInfo.turnPhase.add(turnPhase)

              val move =
                  resolveBoardRemovals(
                          maxDepth = startingDepth,
                          currentPlayer = currentPlayer.deepCopy(),
                          opponentPlayer = opponent.deepCopy(),
                          bitboard = bitboard.deepCopy(),
                          alphaBetaScore = AlphaBetaScoreBitPacked(),
                          rng = rng,
                          depth = 0,
                          caller = "PLAYER $name selectMove() @ ${startingDepth}",
                          transpositionTable = transpositionTable,
                          searchInfo = searchInfo,
                      )
                      .move

              searchInfos.add(searchInfo)
              bestMove = move
              startingDepth += 1
            }

            bestMove
          }
        }
      }
    }
    Model.MCTS -> {
      selectMoveMCTS(
          bitboard = bitboard.deepCopy(),
          currentPlayer = currentPlayer.liteDeepCopy(),
          nextPlayer = opponent.liteDeepCopy(),
          turnPhase = turnPhase,
          rounds = 0..iterations,
          rng = rng,
          duration = if (timeControl) timeDuration else Duration.ZERO,
          useRAVE = this.useRAVE,

          searchInfos = searchInfos,
      )
    }
    Model.NEURAL_NETWORK -> {
      TODO("Implement")
    }
  }
}
