package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.example.engine.ExperienceCollector

enum class PlayerName {
  WHITE,
  BLACK,
}

@Serializable
data class Player(
    val name: PlayerName,
    val abbreviation: String,
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
        abbreviation = this.abbreviation,
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

    //    println(
    //        "the delta was $pieceCountDifference (Initial: $initialSize, Current:
    // ${piecesInReserve.size})"
    //    )

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
    val isDebugEnabled = false
    if (isDebugEnabled) {
      println("--- COMBINE PIECES CALLED ---")
      println("Reserve size before combining: ${this.piecesInReserve.size}")
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
      if (isDebugEnabled) println("No combinable pieces found. Exiting.")
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

      if (isDebugEnabled) {
        println(
            "  -> Processing Type: $type | Total: $count | Forming $pairs pairs, $remainder leftover."
        )
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

    if (isDebugEnabled) {
      println("Successfully generated ${newlyStackedPotentials.size} new potential pieces.")
      println("Reserve size after combining: ${this.piecesInReserve.size}")
      println("--- COMBINE PIECES COMPLETED ---")
    }

    // FIXED: Only return the actual newly formed stacks, omitting the leftover singletons
    return newlyStackedPotentials
  }

  fun uncombinePieces(newlyStackedPieces: List<UInt>) {
    // --- 0. CONFIGURABLE DEBUGGING ---
    val isDebugEnabled = false
    if (isDebugEnabled) {
      println("--- UNCOMBINE PIECES CALLED ---")
      println("Newly stacked pieces count: ${newlyStackedPieces.size}")
    }

    val preReservePotentials =
        piecesInReserve.count {
          it.extractPotential()
        } * 2
    val preReserveBasics = piecesInReserve.count { !it.extractPotential() }

    // Early exit for cleaner control flow
    if (newlyStackedPieces.isEmpty()) {
      if (isDebugEnabled) println("No newly stacked pieces to process. Exiting.")
      return
    }

    // --- 1. FILTER & VALIDATE ---
    // Extract only the pieces that are currently marked as potentials
//    val piecesToUncombine = newlyStackedPieces.filter { it.extractPotential() }

    if (newlyStackedPieces.isEmpty()) {
      if (isDebugEnabled) println("None of the newly stacked pieces are 'potential'. Exiting.")
      return
    }

    if (isDebugEnabled) {
      println("Found ${newlyStackedPieces.size} potential piece(s) to uncombine.")
      println("Reserve size before uncombining: ${this.piecesInReserve.size}")
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
    newlyStackedPieces.forEach { piece ->
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

    if (isDebugEnabled) {
      println("Successfully generated and added ${unstackedPieces.size} regular pieces.")
      println("Reserve size after uncombining: ${this.piecesInReserve.size}")
      println("Pre Uncombine Potential Pieces: $preReservePotentials")
      println("Pre Uncombine Basic Pieces: $preReserveBasics")
      println("Post Uncombine Potential Pieces: $postReservePotentials")
      println("Post Uncombine Basic Pieces: $postReserveBasics")
      println("--- UNCOMBINE PIECES COMPLETED ---")
    }
  }
}
