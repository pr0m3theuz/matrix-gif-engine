@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import org.example.ai.humanEvaluation.SearchInfo
import org.example.ai.mcts.PackedMove
import org.example.engine.ExperienceCollector
import kotlin.time.Duration

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

@Serializable
data class State(
  val currentPlayer: Player,
  val nextPlayer: Player,
  @Transient val board: Board = Board(emptySet()),
  val bitboard: Bitboard,
  @Transient val lines: Lines? = null,
  val turnMoves: MutableMap<Int, MutableList<PackedMove>> = mutableMapOf(), // moves per turn
  val turnDuration: MutableMap<Int, MutableList<Long>> = mutableMapOf(), // moves per turn
  val turnSearchInfo: MutableMap<Int, MutableList<MutableList<SearchInfo>>> = mutableMapOf(),
  @Transient val collector: ExperienceCollector? = ExperienceCollector(),
) {
  fun deepCopy(copyCollector: Boolean = false): State {
    //    val string = Json.encodeToString(serializer(), this)
    //    return Json.decodeFromString(serializer(), string)

    return State(
        currentPlayer = currentPlayer.deepCopy(copyCollector = true),
        nextPlayer = nextPlayer.deepCopy(copyCollector = true),
        board = this.board,
        bitboard = bitboard.deepCopy(),
        //	    lines = this.lines,
        turnMoves = this.turnMoves.toMutableMap(),
        turnDuration = this.turnDuration.toMutableMap(),
      turnSearchInfo = this.turnSearchInfo.toMutableMap(),
        collector = if (copyCollector) this.collector else null,
    )
  }

  fun rotatePlayers(): State {
    return State(
        currentPlayer = this.nextPlayer,
        nextPlayer = this.currentPlayer,
        board = this.board,
        bitboard = bitboard,
        turnMoves = this.turnMoves,
        turnDuration = this.turnDuration,
      turnSearchInfo = this.turnSearchInfo,
        collector = this.collector,
    )
  }
}

fun State.printStateSummary() {
  val sb = StringBuilder()

  sb.appendLine("=================== GAME STATE ===================")

  // 1. Players Section
  sb.appendLine("Active Players:")
  sb.appendLine("  • CURRENT: ${currentPlayer.name}")
  sb.appendLine(
      "    - Reserve:  ${currentPlayer.piecesInReserve.count {it.extractPotential() || it.extractPieceType() == PieceType.GIPF}} pieces"
  )
  sb.appendLine("    - Captured: ${currentPlayer.capturedPieces.size} pieces")

  sb.appendLine("  • NEXT:    ${nextPlayer.name}")
  sb.appendLine(
      "    - Reserve:  ${nextPlayer.piecesInReserve.count {it.extractPotential() || it.extractPieceType() == PieceType.GIPF}} pieces"
  )
  sb.appendLine("    - Captured: ${nextPlayer.capturedPieces.size} pieces")

  sb.appendLine("--------------------------------------------------")

  val occupiedNodes = board.nodes.filter { it.piece != null }
  sb.appendLine("Board State (${occupiedNodes.size} pieces total):")

  if (occupiedNodes.isEmpty()) {
    sb.appendLine("  (Board is completely empty)")
  } else {
    occupiedNodes.sortedWith(compareBy({ it.coordinate.column }, { it.coordinate.row })).forEach {
        node ->
      val piece = node.piece!!
      val pieceType = piece.type
      val pieceColor = piece.colorName
      val potentialStr =
          when (piece.potential) {
            true -> " [Has Potential]"
            false -> " [Spent Potential]"
          }

      sb.appendLine(
          "  • [${node.coordinate.column}${node.coordinate.row}] -> $pieceColor $pieceType$potentialStr"
      )
    }
  }

  logger.info { sb.toString() }
}

fun State.assertPieceCount(
    EXPECTED_TOTAL: Int = 66 / 2,
    MAXIMUM_PIECES: Int = 66,
    bitboard: Bitboard? = null,
) {

  val nextPlayer = this.nextPlayer
  val currentPlayer = this.currentPlayer
  val board = this.board

  // 1. Next Player's components
  var nextReservePotentials =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
      } * 2
  var nextReserveBasics =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.BLACK && !it.extractPotential()
      }
  var nextCapturedPotentials =
      nextPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
      } * 2
  var nextCapturedBasics =
      nextPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.BLACK && !it.extractPotential()
      }

  // 2. Current Player's components
  var currentReservePotentials =
      currentPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
      } * 2
  var currentReserveBasics =
      currentPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.BLACK && !it.extractPotential()
      }
  var currentCapturedPotentials =
      currentPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
      } * 2
  var currentCapturedBasics =
      currentPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.BLACK && !it.extractPotential()
      }

  // 3. Board components
  var boardPotentials =
      board.nodes.count {
        it.piece?.colorName == PlayerName.BLACK && it.piece?.potential == true
      } * 2
  var boardBasics =
      board.nodes.count {
        it.piece?.colorName == PlayerName.BLACK && it.piece?.potential == false
      }
  var boardStacks =
      board.nodes.sumOf {
        it.piece?.stackedPieces?.count { p -> p.colorName == PlayerName.BLACK } ?: 0
      }

  var gipfPieces =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.BLACK && it.extractPieceType() == PieceType.GIPF
      } +
          nextPlayer.capturedPieces.count {
            it.extractPieceColor() == PlayerName.BLACK && it.extractPieceType() == PieceType.GIPF
          } +
          currentPlayer.piecesInReserve.count {
            it.extractPieceColor() == PlayerName.BLACK && it.extractPieceType() == PieceType.GIPF
          } +
          currentPlayer.capturedPieces.count {
            it.extractPieceColor() == PlayerName.BLACK && it.extractPieceType() == PieceType.GIPF
          } +
          board.nodes.count {
            it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.GIPF
          }

  var zertzPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.BLACK && it.extractPieceType() == PieceType.ZERTZ
        ) {
          if (it.extractPotential() == true) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.ZERTZ
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.ZERTZ
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.ZERTZ
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.ZERTZ) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  var tamskPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.BLACK && it.extractPieceType() == PieceType.TAMSK
        ) {
          if (it.extractPotential() == true) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.TAMSK
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.TAMSK
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.TAMSK
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.TAMSK) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  var yinshPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.BLACK && it.extractPieceType() == PieceType.YINSH
        ) {
          if (it.extractPotential() == true) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.YINSH
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.YINSH
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.YINSH
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.YINSH) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  var dvonnPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.BLACK && it.extractPieceType() == PieceType.DVONN
        ) {
          if (it.extractPotential() == true) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.DVONN
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.DVONN
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.DVONN
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.DVONN) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  var punctPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.BLACK && it.extractPieceType() == PieceType.PUNCT
        ) {
          if (it.extractPotential() == true) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.PUNCT
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.PUNCT
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.BLACK &&
                    it.extractPieceType() == PieceType.PUNCT
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.PUNCT) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  val totalBlackPieces =
      nextReservePotentials +
          nextReserveBasics +
          nextCapturedPotentials +
          nextCapturedBasics +
          currentReservePotentials +
          currentReserveBasics +
          currentCapturedPotentials +
          currentCapturedBasics +
          boardPotentials +
          boardBasics +
          boardStacks

  check(totalBlackPieces == EXPECTED_TOTAL) {
    val nextReserveRaw =
        nextPlayer.piecesInReserve.count {
          it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
        }
    val nextCapturedRaw =
        nextPlayer.capturedPieces.count {
          it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
        }
    val currentReserveRaw =
        currentPlayer.piecesInReserve.count {
          it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
        }
    val currentCapturedRaw =
        currentPlayer.capturedPieces.count {
          it.extractPieceColor() == PlayerName.BLACK && it.extractPotential()
        }

    """
    CRITICAL STATE CORRUPTION: Total Black piece weight ($totalBlackPieces) != Expected ($EXPECTED_TOTAL)
    
    1. NEXT PLAYER
       ├── Reserve (Black pieces held)
       │   ├── Potentials: $nextReserveRaw (weighted: $nextReservePotentials)
       │   └── Basics:     $nextReserveBasics
       └── Captured (By Next Player)
           ├── Potentials: $nextCapturedRaw (weighted: $nextCapturedPotentials)
           └── Basics:     $nextCapturedBasics
           
    2. CURRENT PLAYER
       ├── Reserve (Black pieces held)
       │   ├── Potentials: $currentReserveRaw (weighted: $currentReservePotentials)
       │   └── Basics:     $currentReserveBasics
       └── Captured (By Current Player)
           ├── Potentials: $currentCapturedRaw (weighted: $currentCapturedPotentials)
           └── Basics:     $currentCapturedBasics
           
    3. ACTIVE BOARD
       ├── Potentials (Weighted): $boardPotentials
       ├── Flat Basics (Single pieces on board)
       │   ├── GIPF:  ${board.nodes.filter { it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.GIPF }}
       │   ├── ZERTZ: ${board.nodes.filter { it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.ZERTZ }}
       │   ├── TAMSK: ${board.nodes.filter { it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.TAMSK }}
       │   └── YINSH: ${board.nodes.filter { it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.YINSH }}
       └── Stacks (Layered/hidden pieces)
           ├── DVONN (Black layers 0,2,4): ${board.nodes.filter { it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.DVONN }}
           ├── PUNCT (Black layers 0,2,4): ${board.nodes.filter { it.piece?.colorName == PlayerName.BLACK && it.piece?.type == PieceType.PUNCT }}
           
    SUMMARY EVALUATION:
    - Player Total Weight: ${nextReservePotentials + nextReserveBasics + nextCapturedPotentials + nextCapturedBasics + currentReservePotentials + currentReserveBasics + currentCapturedPotentials + currentCapturedBasics}
    - Board Total Weight:  ${boardPotentials + boardBasics + boardStacks}
    ${bitboard?.let { "- Bitboard State: " + Json.encodeToString<Bitboard>(it)}}
    - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.WHITE) currentPlayer else nextPlayer)}
    ======================================================================
    """
        .trimIndent()
  }

  // 1. Next Player's components
  nextReservePotentials =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
      } * 2
  nextReserveBasics =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.WHITE && !it.extractPotential()
      }
  nextCapturedPotentials =
      nextPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
      } * 2
  nextCapturedBasics =
      nextPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.WHITE && !it.extractPotential()
      }

  // 2. Current Player's components
  currentReservePotentials =
      currentPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
      } * 2
  currentReserveBasics =
      currentPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.WHITE && !it.extractPotential()
      }
  currentCapturedPotentials =
      currentPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
      } * 2
  currentCapturedBasics =
      currentPlayer.capturedPieces.count {
        it.extractPieceColor() == PlayerName.WHITE && !it.extractPotential()
      }

  // 3. Board components
  boardPotentials =
      board.nodes.count {
        it.piece?.colorName == PlayerName.WHITE && it.piece?.potential == true
      } * 2
  boardBasics =
      board.nodes.count {
        it.piece?.colorName == PlayerName.WHITE && it.piece?.potential == false
      }
  boardStacks =
      board.nodes.sumOf {
        it.piece?.stackedPieces?.count { p -> p.colorName == PlayerName.WHITE } ?: 0
      }

  gipfPieces =
      nextPlayer.piecesInReserve.count {
        it.extractPieceColor() == PlayerName.WHITE && it.extractPieceType() == PieceType.GIPF
      } +
          nextPlayer.capturedPieces.count {
            it.extractPieceColor() == PlayerName.WHITE && it.extractPieceType() == PieceType.GIPF
          } +
          currentPlayer.piecesInReserve.count {
            it.extractPieceColor() == PlayerName.WHITE && it.extractPieceType() == PieceType.GIPF
          } +
          currentPlayer.capturedPieces.count {
            it.extractPieceColor() == PlayerName.WHITE && it.extractPieceType() == PieceType.GIPF
          } +
          board.nodes.count {
            it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.GIPF
          }

  zertzPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.WHITE && it.extractPieceType() == PieceType.ZERTZ
        ) {
          if (it.extractPotential() == true) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.ZERTZ
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.ZERTZ
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.ZERTZ
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.ZERTZ) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  tamskPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.WHITE && it.extractPieceType() == PieceType.TAMSK
        ) {
          if (it.extractPotential() == true) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.TAMSK
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.TAMSK
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.TAMSK
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.TAMSK) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  yinshPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.WHITE && it.extractPieceType() == PieceType.YINSH
        ) {
          if (it.extractPotential() == true) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.YINSH
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.YINSH
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.YINSH
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.YINSH) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  dvonnPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.WHITE && it.extractPieceType() == PieceType.DVONN
        ) {
          if (it.extractPotential() == true) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.DVONN
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.DVONN
            ) {
              if (it.extractPotential()) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.DVONN
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.DVONN) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  punctPieces =
      nextPlayer.piecesInReserve.sumOf {
        if (
            it.extractPieceColor() == PlayerName.WHITE && it.extractPieceType() == PieceType.PUNCT
        ) {
          if (it.extractPotential()) 2 else 1
        } else 0
      } +
          nextPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.PUNCT
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.piecesInReserve.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.PUNCT
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          currentPlayer.capturedPieces.sumOf {
            if (
                it.extractPieceColor() == PlayerName.WHITE &&
                    it.extractPieceType() == PieceType.PUNCT
            ) {
              if (it.extractPotential() == true) 2 else 1
            } else 0
          } +
          board.nodes.sumOf {
            if (it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.PUNCT) {
              if (it.piece?.potential == true) 2 else 1
            } else 0
          }

  val totalWhitePieces =
      nextReservePotentials +
          nextReserveBasics +
          nextCapturedPotentials +
          nextCapturedBasics +
          currentReservePotentials +
          currentReserveBasics +
          currentCapturedPotentials +
          currentCapturedBasics +
          boardPotentials +
          boardBasics +
          boardStacks

  check(totalWhitePieces == EXPECTED_TOTAL) {
    val nextReserveRaw =
        nextPlayer.piecesInReserve.count {
          it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
        }
    val nextCapturedRaw =
        nextPlayer.capturedPieces.count {
          it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
        }
    val currentReserveRaw =
        currentPlayer.piecesInReserve.count {
          it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
        }
    val currentCapturedRaw =
        currentPlayer.capturedPieces.count {
          it.extractPieceColor() == PlayerName.WHITE && it.extractPotential()
        }

    """
 CRITICAL STATE CORRUPTION: Total White piece weight (${totalWhitePieces}) != Expected ($EXPECTED_TOTAL)
    
    1. NEXT PLAYER
       ├── Reserve (White pieces held)
       │   ├── Potentials: $nextReserveRaw (weighted: $nextReservePotentials)
       │   └── Basics:     $nextReserveBasics
       └── Captured (By Next Player)
           ├── Potentials: $nextCapturedRaw (weighted: $nextCapturedPotentials)
           └── Basics:     $nextCapturedBasics
           
    2. CURRENT PLAYER
       ├── Reserve (White pieces held)
       │   ├── Potentials: $currentReserveRaw (weighted: $currentReservePotentials)
       │   └── Basics:     $currentReserveBasics
       └── Captured (By Current Player)
           ├── Potentials: $currentCapturedRaw (weighted: $currentCapturedPotentials)
           └── Basics:     $currentCapturedBasics
           
    3. ACTIVE BOARD
       ├── Potentials (Weighted): $boardPotentials
       ├── Flat Basics (Single pieces on board)
       │   ├── GIPF:  ${board.nodes.filter { it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.GIPF }}
       │   ├── ZERTZ: ${board.nodes.filter { it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.ZERTZ }}
       │   ├── TAMSK: ${board.nodes.filter { it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.TAMSK }}
       │   └── YINSH: ${board.nodes.filter { it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.YINSH }}
       └── Stacks (Layered/hidden pieces)
           ├── DVONN (White layers 0,2,4): ${board.nodes.filter { it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.DVONN }}
           ├── PUNCT (White layers 0,2,4): ${board.nodes.filter { it.piece?.colorName == PlayerName.WHITE && it.piece?.type == PieceType.PUNCT }}
           
    SUMMARY EVALUATION:
    - Player Total Weight: ${nextReservePotentials + nextReserveBasics + nextCapturedPotentials + nextCapturedBasics + currentReservePotentials + currentReserveBasics + currentCapturedPotentials + currentCapturedBasics}
    - Board Total Weight:  ${boardPotentials + boardBasics + boardStacks}
    ${bitboard?.let { "- Bitboard State: " + Json.encodeToString<Bitboard>(it) }}
    - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.WHITE) currentPlayer else nextPlayer)}
    ======================================================================
    """
        .trimIndent()
  }

  val totalPieces = totalBlackPieces + totalWhitePieces
  check(totalPieces == MAXIMUM_PIECES) {
    "Game Piece Desynchronization: Total pieces in play ($totalPieces) exceeds the maximum piece count ($MAXIMUM_PIECES). " +
        "Pieces have been illegally spawned or deleted." +
        "\nGame State: \n${Json.encodeToString(this)}"
  }
}

