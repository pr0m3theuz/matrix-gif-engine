package org.example.model

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.example.ai.humanEvaluation.AlphaBetaScoreBitPacked
import org.example.ai.humanEvaluation.alphaBetaPackedMove
import org.example.ai.humanEvaluation.resolveBoardRemovals
import org.example.ai.mcts.PackedMove
import org.example.ai.mcts.selectMoveMCTS
import org.example.engine.ExperienceCollector

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
    "MCTS", "mcts" -> return Model.MCTS
    "MINIMAX", "minimax" -> return Model.MINIMAX
    "nn", "NN", "NEURAL_NETWORK", "neural_network" -> return Model.NEURAL_NETWORK
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
    duration = 1.5.toDuration(DurationUnit.SECONDS),
  ),
  GREEDY(
    difficulty = 1,
    minimaxDepth = 1,
    mctsRounds = 0..0,
    duration = 1.5.toDuration(DurationUnit.SECONDS),
  ),
  EASY(
    difficulty = 2,
    minimaxDepth = 3,
    mctsRounds = 0..999,
    duration = 1.5.toDuration(DurationUnit.SECONDS),
  ),
  MEDIUM(
    difficulty = 3,
    minimaxDepth = 5,
    mctsRounds = 0..2499,
    duration = 5.toDuration(DurationUnit.SECONDS),
  ),
  HARD(
    difficulty = 4,
    minimaxDepth = 7,
    mctsRounds = 0..4999,
    duration = 10.toDuration(DurationUnit.SECONDS),
  ),
}

fun getStrength(strength: String): Strength {
  return when (strength) {
    "random", "RANDOM" -> Strength.RANDOM
    "greedy", "GREEDY" -> Strength.GREEDY
    "easy", "EASY" -> Strength.EASY
    "med", "medium", "MEDIUM" -> Strength.MEDIUM
    "hard", "HARD" -> Strength.HARD
    else -> Strength.EASY
  }
}

@Serializable
data class Player(
    val name: PlayerName,
    val model: Model = Model.MCTS,
    val strength: Strength = Strength.RANDOM,
    //    val color: Color,
    val piecesInReserve: MutableList<UInt> = mutableListOf(),
    val capturedPieces: MutableList<UInt> = mutableListOf(),
    @Transient val collector: ExperienceCollector? = ExperienceCollector(),
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
        collector = if (copyCollector) this.collector else null,
    )
  }

  fun addRetrievedCapturedPieces(pieces: List<UInt>) {
    for (piece in pieces) {
      if (piece.extractPieceColor() == this.name) {
        this.piecesInReserve.add(piece.onlyPiece())
      }
      if (piece.extractPieceColor() != this.name) {
        this.capturedPieces.add(piece.onlyPiece())
      }
    }
  }

  fun removeRetrievedCapturedPieces(pieces: List<UInt>) {
    for (piece in pieces) {
      if (piece.extractPieceColor() == this.name) {
        this.piecesInReserve.remove(piece.onlyPiece())
      }
      if (piece.extractPieceColor() != this.name) {
        this.capturedPieces.remove(piece.onlyPiece())
      }
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
      logger.info { "" + ("--- COMBINE PIECES CALLED ---") }
      logger.info { "" + ("Reserve size before combining: ${this.piecesInReserve.size}") }
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
      logger.debug { "" + ("No combinable pieces found. Exiting.") }
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
        "" + ("Successfully generated ${newlyStackedPotentials.size} new potential pieces.")
      }
      logger.info { "" + ("Reserve size after combining: ${this.piecesInReserve.size}") }
      logger.info { "" + ("--- COMBINE PIECES COMPLETED ---") }
    }

    // FIXED: Only return the actual newly formed stacks, omitting the leftover singletons
    return newlyStackedPotentials
  }

  fun uncombinePieces(newlyStackedPieces: List<UInt>) {
    // --- 0. CONFIGURABLE DEBUGGING ---
    if (logger.isDebugEnabled()) {
      logger.info { "" + ("--- UNCOMBINE PIECES CALLED ---") }
      logger.info { "" + ("Newly stacked pieces count: ${newlyStackedPieces.size}") }
    }

    val preReservePotentials =
        piecesInReserve.count {
          it.extractPotential()
        } * 2
    val preReserveBasics = piecesInReserve.count { !it.extractPotential() }

    // Early exit for cleaner control flow
    if (newlyStackedPieces.isEmpty()) {
      logger.debug { "" + ("No newly stacked pieces to process. Exiting.") }
      return
    }

    // --- 1. FILTER & VALIDATE ---
    // Extract only the pieces that are currently marked as potentials
    //    val piecesToUncombine = newlyStackedPieces.filter { it.extractPotential() }

    if (newlyStackedPieces.isEmpty()) {
      logger.debug { "" + ("None of the newly stacked pieces are 'potential'. Exiting.") }
      return
    }

    if (logger.isDebugEnabled()) {
      logger.info { "" + ("Found ${newlyStackedPieces.size} potential piece(s) to uncombine.") }
      logger.info { "" + ("Reserve size before uncombining: ${this.piecesInReserve.size}") }
    }

    // --- 2. GENERATE UNCOMBINED PIECES ---
    // OPTIMIZATION: flatMap cleanly replaces map { MutableList(...) }.flatten()
    val unstackedPieces: List<UInt> = newlyStackedPieces.flatMap { piece ->
      requireNotNull(piece.extractPieceType()) { "Piece Type cannot be null. $piece." }
      listOf(
              piece.extractPieceType()?.let {
                0u.createPiece(it, piece.extractPieceColor(), potential = false)
              },
              piece.extractPieceType()?.let {
                0u.createPiece(it, piece.extractPieceColor(), potential = false)
              },
          )
          .filterNotNull()
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
        "" + ("Successfully generated and added ${unstackedPieces.size} regular pieces.")
      }
      logger.info { "" + ("Reserve size after uncombining: ${this.piecesInReserve.size}") }
      logger.info { "" + ("Pre Uncombine Potential Pieces: $preReservePotentials") }
      logger.info { "" + ("Pre Uncombine Basic Pieces: $preReserveBasics") }
      logger.info { "" + ("Post Uncombine Potential Pieces: $postReservePotentials") }
      logger.info { "" + ("Post Uncombine Basic Pieces: $postReserveBasics") }
      logger.info { "" + ("--- UNCOMBINE PIECES COMPLETED ---") }
    }
  }
}


fun Player.selectMove(
    turnPhase: TurnPhase,
    bitboard: Bitboard,
    opponent: Player,
    rng: Random,
): PackedMove? {
  return when (model) {
    Model.MINIMAX -> {
      when (turnPhase) {
        TurnPhase.PlayerInputWindow,
        TurnPhase.ExtraMove -> {
          alphaBetaPackedMove(
                  bitboard = bitboard.deepCopy(),
                  currentPlayer = this.deepCopy(),
                  opponentPlayer = opponent.deepCopy(),
                  alphaBetaScore = AlphaBetaScoreBitPacked(),
                  rng = rng,
                  depth = strength.minimaxDepth,
              turnPhase = turnPhase,
              )
              .move
        }
        TurnPhase.PieceRemoval -> {
          resolveBoardRemovals(
                  currentPlayer = this.deepCopy(),
                  opponentPlayer = opponent.deepCopy(),
                  bitboard = bitboard.deepCopy(),
                  alphaBetaScore = AlphaBetaScoreBitPacked(),
                  rng = rng,
                  depth = strength.minimaxDepth,
              )
              .move
        }
      }
    }
    Model.MCTS -> {
      selectMoveMCTS(
          bitboard = bitboard.deepCopy(),
          currentPlayer = this.deepCopy(),
          nextPlayer = opponent.deepCopy(),
          turnPhase = turnPhase,
          rounds = strength.mctsRounds,
          rng = rng,
      )
    }
    Model.NEURAL_NETWORK -> {
      TODO("Implement")
    }
  }
}
