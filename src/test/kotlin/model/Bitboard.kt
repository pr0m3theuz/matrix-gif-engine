@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import kotlin.test.assertEquals
import kotlin.text.toString
import kotlinx.serialization.json.Json
import org.example.ai.humanEvaluation.BestBitMove
import org.example.ai.mcts.selectMoveMCTS
import org.example.engine.MoveType
import org.example.engine.PossibleBitMove
import org.example.engine.TurnPhase
import org.example.model.Bitboard
import org.example.model.Board
import org.example.model.Coordinate
import org.example.model.LETTERS
import org.example.model.Node
import org.example.model.NodeNeighbors
import org.example.model.Piece
import org.example.model.PieceType
import org.example.model.Player
import org.example.model.PlayerName
import org.example.model.ROWS
import org.example.model.RetrievedCapturedPieceBit
import org.example.model.State
import org.example.model.assertPieceCount
import org.example.model.constructLines
import org.example.model.constructNodes
import org.example.model.convertBitboardToBoard
import org.example.model.convertBoardToBitboard
import org.example.model.createPlayerPiecesWithPotentialPowerset
import org.example.model.createRetrieveAndCapturePiecesList
import org.example.model.evaluateLinesForFourInARow
import org.example.model.getDvonnMoves
import org.example.model.getPunctMoves
import org.example.model.getsSelectedPiecesWithPotentialPowerset
import org.example.model.identifyAvailableMoves
import org.example.model.populateNeighbors
import org.example.model.removeSelectedPiecesToRemove
import org.example.model.usePiecePotential
import org.example.model.validatePieceRemoval
import org.example.toBitList
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

enum class PlayerName {
  WHITE,
  BLACK,
}

enum class PieceType {
  PUNCT,
  DVONN,
  GIPF,
  YINSH,
  TAMSK,
  ZERTZ,
}

enum class MoveType {
  RetrieveCapturePieces /* Add others if needed */
}

data class NodeDef(
  val coord: String,
  val pieceAbbr: String?,
  val isDot: Boolean,
  val isSpot: Boolean,
  val isCenter: Boolean = false,
  val above: String? = null,
  val below: String? = null,
  val ur: String? = null,
  val lr: String? = null,
  val ul: String? = null,
  val ll: String? = null,
  val bitmask: ULong = ULong.MAX_VALUE,
  val potential: Boolean? = null // Allows custom override in the map
) {
  fun toNode(): Node {
    return Node(
      coordinate = coord(coord),
      piece = pieceAbbr?.let {
        // Default to true unless overridden
        piece(it, potential ?: true)
      },
      isDot = isDot,
      isSpot = isSpot,
      isCenter = isCenter,
      neighbors = NodeNeighbors(
        above = above?.let { coord(it) },
        below = below?.let { coord(it) },
        upperRight = ur?.let { coord(it) },
        lowerRight = lr?.let { coord(it) },
        upperLeft = ul?.let { coord(it) },
        lowerLeft = ll?.let { coord(it) }
      ),
      bitmask = bitmask
    )
  }
}

fun coord(repr: String): Coordinate = Coordinate(repr[0], repr.substring(1).toInt())

fun piece(abbr: String, potential: Boolean = true): Piece {
  val color = if (abbr.startsWith("W")) PlayerName.WHITE else PlayerName.BLACK
  val type =
      when (abbr.substring(1)) {
        "P" -> PieceType.PUNCT
        "D" -> PieceType.DVONN
        "G" -> PieceType.GIPF
        "Y" -> PieceType.YINSH
        "T" -> PieceType.TAMSK
        "Z" -> PieceType.ZERTZ
        else -> throw IllegalArgumentException("Unknown piece type: $abbr")
      }
  // GIPF is non-potential by default in this state dump
  val isPotential = if (type == PieceType.GIPF) false else potential
  return Piece(abbr, isPotential, color.name, type)
}

class BitboardTest {

  var oldWhitePieces = 0b0111001010110101111111101001011110100001.toULong()
  var whitePieces = 0b0111001010110101111111101001011110100001.toULong()

  val verticalLineIndexArray =
      listOf(
          listOf(0, 1, 2, 3),
          listOf(4, 5, 6, 7, 8),
          listOf(9, 10, 11, 12, 13, 14),
          listOf(15, 16, 17, 18, 19, 20, 21),
          listOf(22, 23, 24, 25, 26, 27),
          listOf(28, 29, 30, 31, 32),
          listOf(33, 34, 35, 36),
          listOf(37, 38, 39),
      )

  val upwardRightLineIndexArray =
      listOf(
          listOf(0, 5, 11, 18, 25, 31, 36),
          listOf(1, 6, 12, 19, 26, 32),
          listOf(2, 7, 13, 20, 27),
          listOf(3, 8, 14, 21),
          listOf(4, 10, 17, 24, 30, 35, 39),
          listOf(9, 16, 23, 29, 34, 38),
          listOf(15, 22, 28, 33, 37),
      )

  val downwardRightLineIndexArray =
      listOf(
          listOf(0, 4, 9, 15),
          listOf(1, 5, 10, 16, 22),
          listOf(2, 6, 11, 17, 23, 28),
          listOf(3, 7, 12, 18, 24, 29, 33),
          listOf(8, 13, 19, 25, 30, 34, 37),
          listOf(14, 20, 26, 31, 35, 38),
          listOf(21, 27, 32, 36, 39),
      )

  data class ColumnInfo(
      val columnMask: ULong,
      val destinationMask: ULong,
      val shiftPairs: List<Pair<ULong, ULong>>, // (fromMask, toMask)
  ) {
    // Helper to format as binary with a prefix.
    // You could also use .padStart(64, '0') here if you want fixed widths.
    private fun ULong.toBin() = "0b" + this.toString(radix = 2)

    override fun toString(): String {
      // Format the list of pairs customly
      val formattedPairs =
          shiftPairs.joinToString(prefix = "[", postfix = "]") { (from, to) ->
            "(${from.toBin()} -> ${to.toBin()})"
          }

      return """
            ColumnInfo(
                columnMask = ${columnMask.toBin()},
                destinationMask = ${destinationMask.toBin()},
                shiftPairs = $formattedPairs
            )
        """
          .trimIndent()
    }
  }

  val columnInfos: List<ColumnInfo> = upwardRightLineIndexArray.map { arr ->
    ColumnInfo(
        columnMask = arr.fold(0UL) { acc, i -> acc or (1UL shl i) },
        destinationMask = 1UL shl arr.last(),
        shiftPairs =
            (0 until arr.size - 1).map { k ->
              (1UL shl arr[k]) to (1UL shl arr[k + 1])
            },
    )
  }

  // O(1) lookup: board index → ColumnInfo
  val indexToColumn: Map<Int, ColumnInfo> = buildMap {
    upwardRightLineIndexArray.forEachIndexed { i, arr ->
      arr.forEach { pos -> put(pos, columnInfos[i]) }
    }
  }

  @Test
  fun optimized() {
    val index = 5

    val col = indexToColumn[index] ?: error("Index $index not in any column")

    // region Shift Pieces Up
    // 1. Get the current state of ALL pieces to find the gaps
    val occupied = whitePieces // or blackPieces

    var updatedWhite = whitePieces
    //		var updatedBlack = blackPieces
    var gapFound = false

    // 2. Iterate sequentially. (Assume shiftPairs is ordered from insertion edge inward)
    for ((fromMask, toMask) in col.shiftPairs) {
      println("fromMask      : ${fromMask.toString(radix = 2).padStart(64, '0')}")
      println("toMask        : ${toMask.toString(radix = 2).padStart(64, '0')}")
      println("whitePieces   : ${whitePieces.toString(radix = 2).padStart(64, '0')}")
      println("updatedWhite  : ${updatedWhite.toString(radix = 2).padStart(64, '0')}")
      println()
      // Check the original board state to see if a piece is sitting here
      if ((occupied and fromMask) != 0UL) {

        // There is a piece! Shift it forward on the correct color's updated bitboard
        if ((whitePieces and fromMask) != 0UL) {
          updatedWhite = (updatedWhite and fromMask.inv()) or toMask
        }
        //				if ((blackPieces and fromMask) != 0UL)  {
        //					updatedBlack = (updatedBlack and fromMask.inv()) or toMask
        //				}

      } else {
        // We found a gap! The push chain reaction stops here.
        gapFound = true
        break
      }

      println("fromMask.inv(): ${fromMask.inv().toString(radix = 2).padStart(64, '0')}")
      println("updatedWhite  : ${updatedWhite.toString(radix = 2).padStart(64, '0')}")
      println()
    }

    // 3. The true legality check
    // If we shifted through the entire line and never found a gap, we must verify
    // that the absolute last spot was empty. If it wasn't, a piece falls off!
    if (!gapFound) {
      check(!gapFound) {
        "Cannot shift: the line is completely full, a piece would fall off the board"
      }
    }

    // 4. Finally, place the brand new piece at the insertion point
    // (assuming white is playing and inserting at the start of the line)
    var insertionMask = col.shiftPairs.first().first
    updatedWhite = updatedWhite or insertionMask

    println("whitePieces : ${whitePieces.toString(2)}")
    println("updatedWhite: ${updatedWhite.toString(2)}")

    assertTrue { updatedWhite.toString(2) == "0111001010110101111111101001111110100001" }
    // endregion

    // region Shift Pieces Down
    updatedWhite = whitePieces
    //		var updatedBlack = blackPieces
    gapFound = false

    // 2. Iterate sequentially. (Assume shiftPairs is ordered from insertion edge inward)
    for ((toMask, fromMask) in col.shiftPairs.reversed()) {
      println("fromMask      : ${fromMask.toString(radix = 2).padStart(64, '0')}")
      println("toMask        : ${toMask.toString(radix = 2).padStart(64, '0')}")
      println("whitePieces   : ${whitePieces.toString(radix = 2).padStart(64, '0')}")
      println("updatedWhite  : ${updatedWhite.toString(radix = 2).padStart(64, '0')}")
      println()
      // Check the original board state to see if a piece is sitting here
      if ((occupied and fromMask) != 0UL) {

        // There is a piece! Shift it forward on the correct color's updated bitboard
        if ((whitePieces and fromMask) != 0UL) {
          updatedWhite = (updatedWhite and fromMask.inv()) or toMask
        }
        //				if ((blackPieces and fromMask) != 0UL)  {
        //					updatedBlack = (updatedBlack and fromMask.inv()) or toMask
        //				}

      } else {
        // We found a gap! The push chain reaction stops here.
        gapFound = true
        break
      }

      println("fromMask.inv(): ${fromMask.inv().toString(radix = 2).padStart(64, '0')}")
      println("updatedWhite  : ${updatedWhite.toString(radix = 2).padStart(64, '0')}")
      println()
    }

    // 3. The true legality check
    // If we shifted through the entire line and never found a gap, we must verify
    // that the absolute last spot was empty. If it wasn't, a piece falls off!
    if (!gapFound) {
      check(!gapFound) {
        "Cannot shift: the line is completely full, a piece would fall off the board"
      }
    }

    // 4. Finally, place the brand new piece at the insertion point
    // (assuming white is playing and inserting at the start of the line)
    insertionMask = col.shiftPairs.asReversed().first().second
    updatedWhite = updatedWhite or insertionMask

    println("whitePieces : ${whitePieces.toString(2)}")
    println("updatedWhite: ${updatedWhite.toString(2)}")

    assertTrue { updatedWhite.toString(2) == "0111001010110111111111101001011110100001" }
    // endregion
  }

  @Test
  fun `test executePushUp`() {
    val index = 5

    val col = indexToColumn[index] ?: error("Index $index not in any column")

    fun shiftBoard(board: ULong, fromMask: ULong, toMask: ULong): ULong {
      // fromMask == current node
      // if (currentNode is filled) clear it
      println("fromMask      : ${fromMask.toString(radix = 2).padStart(64, '0')}")
      println("toMask        : ${toMask.toString(radix = 2).padStart(64, '0')}")
      println("board         : ${board.toString(radix = 2).padStart(64, '0')}")
      println()
      return if ((board and fromMask) != 0UL) {
        // Clear the old position using AND, set the new position using OR
        (board and fromMask.inv()) or toMask
      } else {
        // Nothing was here, leave the board alone
        board
      }
    }

    val occupiedSpots = 0b0111001010110101111111101001011110100001.toULong()
    var modifiedBoard = 0b0111001010110101111111101001011110100001.toULong()
    val movesToApply = mutableListOf<Pair<ULong, ULong>>()
    var gapFound = false

    // 1. DISCOVERY: Find out exactly which pieces are shifting
    for ((fromMask, toMask) in col.shiftPairs) {
      if ((occupiedSpots and fromMask) != 0UL) {
        movesToApply.add(fromMask to toMask)
      } else {
        gapFound = true
        break
      }
    }

    // 2. VALIDATION: Check if pieces fall off the board
    if (!gapFound) {
      check(!gapFound) {
        "Cannot shift: board is full"
      }
    }

    // 3. MODIFICATION: Apply the shifts in REVERSE to prevent teleportation bugs
    // mutates in place
    // move the last piece first
    movesToApply.asReversed().forEach { (fromMask, toMask) ->
      modifiedBoard = shiftBoard(modifiedBoard, fromMask, toMask)
    }

    // 4. Finally, insert the new piece at the start of the line
    // e.g., boardState.whitePieces = boardState.whitePieces or col.shiftPairs.first().first
    val insertionMask = col.shiftPairs.first().first
    modifiedBoard = modifiedBoard or insertionMask

    println("occupiedSpots : ${occupiedSpots.toString(2)}")
    println("modifiedBoard : ${modifiedBoard.toString(2)}")
    println()
    assertEquals("111001010110101111111101001111110100001", modifiedBoard.toString(2))
  }

  @Test
  fun `test executePushDown`() {
    val index = 5

    val col = indexToColumn[index] ?: error("Index $index not in any column")

    fun shiftBoard(board: ULong, fromMask: ULong, toMask: ULong): ULong {
      // fromMask == current node
      // if (currentNode is filled) clear it
      println("fromMask      : ${fromMask.toString(radix = 2).padStart(64, '0')}")
      println("toMask        : ${toMask.toString(radix = 2).padStart(64, '0')}")
      println("board         : ${board.toString(radix = 2).padStart(64, '0')}")
      println()
      return if ((board and fromMask) != 0UL) {
        // Clear the old position using AND, set the new position using OR
        (board and fromMask.inv()) or toMask
      } else {
        // Nothing was here, leave the board alone
        board
      }
    }

    val occupiedSpots = 0b0111001010110101111111101001011110100001.toULong()
    var modifiedBoard = 0b0111001010110101111111101001011110100001.toULong()
    val movesToApply = mutableListOf<Pair<ULong, ULong>>()
    var gapFound = false

    // 1. DISCOVERY: Find out exactly which pieces are shifting
    for ((toMask, fromMask) in col.shiftPairs.asReversed()) {
      if ((occupiedSpots and fromMask) != 0UL) {
        movesToApply.add(fromMask to toMask)
      } else {
        gapFound = true
        break
      }
    }

    // 2. VALIDATION: Check if pieces fall off the board
    if (!gapFound) {
      check(!gapFound) {
        "Cannot shift: board is full"
      }
    }

    // 3. MODIFICATION: Apply the shifts in REVERSE to prevent teleportation bugs
    movesToApply.asReversed().forEach { (fromMask, toMask) ->
      modifiedBoard = shiftBoard(modifiedBoard, fromMask, toMask)
    }

    // 4. Finally, insert the new piece at the start of the line
    // e.g., boardState.whitePieces = boardState.whitePieces or col.shiftPairs.first().first
    val insertionMask = col.shiftPairs.asReversed().first().second
    modifiedBoard = modifiedBoard or insertionMask

    println("occupiedSpots : ${occupiedSpots.toString(2)}")
    println("modifiedBoard : ${modifiedBoard.toString(2)}")
    println()
    assertEquals("111001010110111111111101001011110100001", modifiedBoard.toString(2))
  }

  fun `adding a Piece to the Bitboard`() {}

  @Test
  fun `convert Bitboard To Board`() {
    val bitboard =
        Bitboard(
            whiteGIPF = 275951648772UL,
            whiteTAMSK = 13421772800UL,
            whiteYINSH = 8388608UL,
            whiteZERTZ = 68719478784UL,
            whitePotentials = 82149640192UL,
            blackGIPF = 51539869696UL,
            blackTAMSK = 2228224UL,
            blackYINSH = 0UL,
            blackZERTZ = 16777280UL,
            blackPotentials = 2569142336UL,
        )

    bitboard.whitePUNCTLayer[0] = 33554432UL
    bitboard.blackDVONNLayer[0] = 2415919104UL
    bitboard.blackPUNCTLayer[0] = 134217728UL
    bitboard.blackPUNCTLayer[1] = 134217728UL

    val currentPlayer =
        Player(
            name = PlayerName.WHITE,
            abbreviation = "W",
            piecesInReserve =
                mutableListOf(
                    Piece(
                        abbreviation = "WZ",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.ZERTZ,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WD",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.DVONN,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WD",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.DVONN,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WD",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.DVONN,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WY",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WY",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WP",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WP",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                ),
        )

    val nextPlayer =
        Player(
            name = PlayerName.BLACK,
            abbreviation = "B",
            piecesInReserve =
                mutableListOf(
                    Piece(
                        abbreviation = "BT",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.TAMSK,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BZ",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.ZERTZ,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BD",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.DVONN,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BY",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BY",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BY",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BP",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BP",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                ),
        )

    val centerCoordinate = Coordinate(column = 'E', row = 5)

    val nodes =
        constructNodes(
            letters = LETTERS,
            rows = ROWS.toList(),
            centerLetterIndex = LETTERS.indexOf(centerCoordinate.column),
            center = centerCoordinate,
        )

    // Populate node neighbours
    nodes.forEach { node ->
      node.neighbors =
          populateNeighbors(
              coordinate = node.coordinate,
              nodes = nodes,
          )
    }

    bitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = nextPlayer)

    val board = Board(nodes = nodes, centerNodeCoordinate = centerCoordinate)

    val convertedBitboardBoard = bitboard.convertBitboardToBoard(board)

    val blackBoardPieces =
        convertedBitboardBoard.nodes.filter { node ->
          node.piece?.colorName == PlayerName.BLACK.name
        }
    val whiteBoardPieces =
        convertedBitboardBoard.nodes.filter { node ->
          node.piece?.colorName == PlayerName.WHITE.name
        }

    println(Json.encodeToString(blackBoardPieces))
    println(Json.encodeToString(whiteBoardPieces))

    val state =
        State(
            currentPlayer = currentPlayer,
            nextPlayer = nextPlayer,
            board = convertedBitboardBoard,
            bitboard = bitboard,
            lines = constructLines(nodes = convertedBitboardBoard.nodes),
        )

    state.assertPieceCount()
  }

  @Test
  fun `remove Retrieve And Capture Pieces From Bitboard`() {
    val bitboard =
        Bitboard(
            whiteGIPF = 17213423616UL,
            whiteTAMSK = 8589934592UL,
            whiteYINSH = 65568UL,
            whiteZERTZ = 2147745792UL,
            whitePotentials = 285615652896UL,
            blackGIPF = 34896609280UL,
            blackTAMSK = 73022832640UL,
            blackYINSH = 0UL,
            blackZERTZ = 0UL,
            blackPotentials = 73039740928UL,
        )

    val currentPlayer =
        Player(
            name = PlayerName.BLACK,
            abbreviation = "B",
            piecesInReserve =
                mutableListOf(
                    Piece(
                        abbreviation = "BG",
                        potential = false,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.GIPF,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BZ",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.ZERTZ,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BZ",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.ZERTZ,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BZ",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.ZERTZ,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BD",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.DVONN,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BY",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BY",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BY",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BP",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "BP",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                ),
            capturedPieces =
                mutableListOf(
                    Piece(
                        abbreviation = "WG",
                        potential = false,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.GIPF,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WP",
                        potential = false,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                ),
        )

    val opponentPlayer =
        Player(
            name = PlayerName.WHITE,
            abbreviation = "W",
            piecesInReserve =
                mutableListOf(
                    Piece(
                        abbreviation = "WT",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.TAMSK,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WT",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.TAMSK,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WZ",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.ZERTZ,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WD",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.DVONN,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WD",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.DVONN,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WY",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WP",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WP",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                ),
        )

    val initBitboard =
        Bitboard(
            whiteGIPF = 17213423620UL,
            whiteTAMSK = 8589934592UL,
            whiteYINSH = 65568UL,
            whiteZERTZ = 2147745792UL,
            whitePotentials = 285615652896UL,
            blackGIPF = 34896611328UL,
            blackTAMSK = 73022832640UL,
            blackYINSH = 64UL,
            blackZERTZ = 0UL,
            blackPotentials = 73308176448UL,
        )

    initBitboard.whiteDVONNLayer[0] = 274877906944UL
    initBitboard.whitePUNCTLayer[0] = 1073741824UL

    initBitboard.blackDVONNLayer[0] = 16908288UL
    initBitboard.blackPUNCTLayer[0] = 268435456UL
    initBitboard.blackPUNCTLayer[1] = 268435456UL

    bitboard.whiteDVONNLayer[0] = 274877906944UL
    bitboard.whitePUNCTLayer[0] = 1073741824UL
    bitboard.blackDVONNLayer[0] = 16908288UL
    bitboard.blackPUNCTLayer[1] = 268435456UL

    val expectedBitboard =
        Bitboard(
            whiteGIPF = 17213423620UL,
            whiteTAMSK = 8589934592UL,
            whiteYINSH = 65568UL,
            whiteZERTZ = 2147745792UL,
            whitePotentials = 285615652896UL,
            blackGIPF = 34896611328UL,
            blackTAMSK = 73022832640UL,
            blackYINSH = 64UL,
            blackZERTZ = 0UL,
            blackPotentials = 73308176448UL,
        )

    expectedBitboard.whiteDVONNLayer[0] = 274877906944UL
    expectedBitboard.whitePUNCTLayer[0] = 1073741824UL

    expectedBitboard.blackDVONNLayer[0] = 16908288UL
    expectedBitboard.blackPUNCTLayer[0] = 268435456UL

    val testBitboard = initBitboard.deepCopy()

    val linesWithFourInARow = testBitboard.evaluateLinesForFourInARow(currentPlayer)

    println(linesWithFourInARow)

    val playerPiecesWithPotentialPowerset =
        testBitboard
            .createPlayerPiecesWithPotentialPowerset(
                listOf(linesWithFourInARow.first()),
                currentPlayer,
            )
            .filter { it.isNotEmpty() }

    println(playerPiecesWithPotentialPowerset)

    val piecesWithPotentialPowerset =
        testBitboard.getsSelectedPiecesWithPotentialPowerset(
            playerPiecesWithPotentialPowerset.first(),
            currentPlayer,
        )

    val retrievedCapturedPieces =
        testBitboard
            .createRetrieveAndCapturePiecesList(
                columnInfos = linesWithFourInARow,
                selectedPiecesWithPotentialPowerset = emptyList(),
                player = currentPlayer,
            )
            .plus(piecesWithPotentialPowerset)

    listOf(
        RetrievedCapturedPieceBit(
            retrievedPiece =
                Piece(
                    abbreviation = "BG",
                    potential = false,
                    colorName = PlayerName.BLACK.name,
                    type = PieceType.GIPF,
                    isNeutralized = false,
                ),
            capturedPiece = null,
            bitmask = 2048UL,
            isNeutralized = false,
            keepRetrievedPieceInPlay = false,
        ),
        RetrievedCapturedPieceBit(
            retrievedPiece = null,
            capturedPiece =
                Piece(
                    abbreviation = "WG",
                    potential = false,
                    colorName = PlayerName.WHITE.name,
                    type = PieceType.GIPF,
                    isNeutralized = false,
                ),
            bitmask = 4UL,
            isNeutralized = false,
            keepRetrievedPieceInPlay = false,
        ),
        RetrievedCapturedPieceBit(
            retrievedPiece =
                Piece(
                    abbreviation = "BP",
                    potential = true,
                    colorName = PlayerName.BLACK.name,
                    type = PieceType.PUNCT,
                    isNeutralized = false,
                ),
            capturedPiece = null,
            bitmask = 268435456UL,
            isNeutralized = false,
            keepRetrievedPieceInPlay = false,
        ),
        RetrievedCapturedPieceBit(
            retrievedPiece = null,
            capturedPiece =
                Piece(
                    abbreviation = "WP",
                    potential = false,
                    colorName = PlayerName.WHITE.name,
                    type = PieceType.PUNCT,
                    isNeutralized = false,
                ),
            bitmask = 268435456UL,
            isNeutralized = true,
            keepRetrievedPieceInPlay = false,
        ),
        RetrievedCapturedPieceBit(
            retrievedPiece =
                Piece(
                    abbreviation = "BY",
                    potential = true,
                    colorName = PlayerName.BLACK.name,
                    type = PieceType.YINSH,
                    isNeutralized = false,
                ),
            capturedPiece = null,
            bitmask = 64UL,
            isNeutralized = false,
            keepRetrievedPieceInPlay = false,
        ),
    )

    val expectedRetrievedCapturedPiecesBit =
        listOf(
            RetrievedCapturedPieceBit(
                retrievedPiece =
                    Piece(
                        abbreviation = "BG",
                        potential = false,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.GIPF,
                        isNeutralized = false,
                    ),
                capturedPiece = null,
                bitmask = 2048UL,
                isNeutralized = false,
                keepRetrievedPieceInPlay = false,
            ),
            RetrievedCapturedPieceBit(
                retrievedPiece = null,
                capturedPiece =
                    Piece(
                        abbreviation = "WG",
                        potential = false,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.GIPF,
                        isNeutralized = false,
                    ),
                bitmask = 4UL,
                isNeutralized = false,
                keepRetrievedPieceInPlay = false,
            ),
            RetrievedCapturedPieceBit(
                retrievedPiece = null,
                capturedPiece =
                    Piece(
                        abbreviation = "WP",
                        potential = false,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                bitmask = 268435456UL,
                isNeutralized = true,
                keepRetrievedPieceInPlay = false,
            ),
            RetrievedCapturedPieceBit(
                retrievedPiece =
                    Piece(
                        abbreviation = "BY",
                        potential = true,
                        colorName = PlayerName.BLACK.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                capturedPiece = null,
                bitmask = 64UL,
                isNeutralized = false,
                keepRetrievedPieceInPlay = false,
            ),
        )

    assertEquals(expectedRetrievedCapturedPiecesBit, retrievedCapturedPieces)

    BestBitMove(
        score = 5.286691f,
        move =
            PossibleBitMove(
                piece = null,
                retrievedCapturedPiecesBit =
                    mutableListOf(
                        RetrievedCapturedPieceBit(
                            retrievedPiece =
                                Piece(
                                    abbreviation = "BG",
                                    potential = false,
                                    colorName = PlayerName.BLACK.name,
                                    type = PieceType.GIPF,
                                    isNeutralized = false,
                                ),
                            capturedPiece = null,
                            bitmask = 2048UL,
                            isNeutralized = false,
                            keepRetrievedPieceInPlay = false,
                        ),
                        RetrievedCapturedPieceBit(
                            retrievedPiece = null,
                            capturedPiece =
                                Piece(
                                    abbreviation = "WG",
                                    potential = false,
                                    colorName = PlayerName.WHITE.name,
                                    type = PieceType.GIPF,
                                    isNeutralized = false,
                                ),
                            bitmask = 4UL,
                            isNeutralized = false,
                            keepRetrievedPieceInPlay = false,
                        ),
                        RetrievedCapturedPieceBit(
                            retrievedPiece =
                                Piece(
                                    abbreviation = "BP",
                                    potential = true,
                                    colorName = PlayerName.BLACK.name,
                                    type = PieceType.PUNCT,
                                    isNeutralized = false,
                                ),
                            capturedPiece = null,
                            bitmask = 268435456UL,
                            isNeutralized = false,
                            keepRetrievedPieceInPlay = false,
                        ),
                        RetrievedCapturedPieceBit(
                            retrievedPiece = null,
                            capturedPiece =
                                Piece(
                                    abbreviation = "WP",
                                    potential = false,
                                    colorName = PlayerName.WHITE.name,
                                    type = PieceType.PUNCT,
                                    isNeutralized = false,
                                ),
                            bitmask = 268435456UL,
                            isNeutralized = true,
                            keepRetrievedPieceInPlay = false,
                        ),
                        RetrievedCapturedPieceBit(
                            retrievedPiece =
                                Piece(
                                    abbreviation = "BY",
                                    potential = true,
                                    colorName = PlayerName.BLACK.name,
                                    type = PieceType.YINSH,
                                    isNeutralized = false,
                                ),
                            capturedPiece = null,
                            bitmask = 64UL,
                            isNeutralized = false,
                            keepRetrievedPieceInPlay = false,
                        ),
                    ),
                sourceBit = null,
                targetBit = null,
                pieceType = null,
                pieceColor = null,
                pushDirection = null,
                moveType = MoveType.RetrieveCapturePieces,
            ),
    )

    mutableListOf(
        Piece(
            abbreviation = "BG",
            potential = false,
            colorName = PlayerName.BLACK.name,
            type = PieceType.GIPF,
            isNeutralized = false,
        ),
        Piece(
            abbreviation = "BP",
            potential = true,
            colorName = PlayerName.BLACK.name,
            type = PieceType.PUNCT,
            isNeutralized = false,
        ),
        Piece(
            abbreviation = "BY",
            potential = true,
            colorName = PlayerName.BLACK.name,
            type = PieceType.YINSH,
            isNeutralized = false,
        ),
    )
    mutableListOf(
        Piece(
            abbreviation = "WG",
            potential = false,
            colorName = PlayerName.WHITE.name,
            type = PieceType.GIPF,
            isNeutralized = false,
        ),
        Piece(
            abbreviation = "WP",
            potential = false,
            colorName = PlayerName.WHITE.name,
            type = PieceType.PUNCT,
            isNeutralized = false,
        ),
    )

    testBitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    println(Json.encodeToString(testBitboard))
  }

  @Test
  fun `convert Bitboard To Board 3`() {
    // 1. Re-create the 65 Nodes
    val nodeDefinitions =
        listOf(
            // Column A
            NodeDef("A1", null, isDot = true, isSpot = false, ur = "B2"),
            NodeDef("A2", null, isDot = true, isSpot = false, ur = "B3", lr = "B2"),
            NodeDef("A3", null, isDot = true, isSpot = false, ur = "B4", lr = "B3"),
            NodeDef("A4", null, isDot = true, isSpot = false, ur = "B5", lr = "B4"),
            NodeDef("A5", null, isDot = true, isSpot = false, lr = "B5"),

            // Column B
            NodeDef("B1", null, isDot = true, isSpot = false, above = "B2", ur = "C2"),
            NodeDef(
                "B2",
                null,
                isDot = false,
                isSpot = true,
                above = "B3",
                ur = "C3",
                lr = "C2",
                bitmask = 1UL,
            ),
            NodeDef(
                "B3",
                "WP",
                isDot = false,
                isSpot = true,
                above = "B4",
                below = "B2",
                ur = "C4",
                lr = "C3",
                bitmask = 2UL,
            ),
            NodeDef(
                "B4",
                null,
                isDot = false,
                isSpot = true,
                above = "B5",
                below = "B3",
                ur = "C5",
                lr = "C4",
                bitmask = 4UL,
            ),
            NodeDef(
                "B5",
                "BD",
                isDot = false,
                isSpot = true,
                below = "B4",
                ur = "C6",
                lr = "C5",
                bitmask = 8UL,
            ),
            NodeDef("B6", null, isDot = true, isSpot = false, below = "B5", lr = "C6"),

            // Column C
            NodeDef("C1", null, isDot = true, isSpot = false, above = "C2", ur = "D2"),
            NodeDef(
                "C2",
                "WG",
                isDot = false,
                isSpot = true,
                above = "C3",
                ur = "D3",
                lr = "D2",
                ul = "B2",
                bitmask = 16UL,
            ),
            NodeDef(
                "C3",
                null,
                isDot = false,
                isSpot = true,
                above = "C4",
                below = "C2",
                ur = "D4",
                lr = "D3",
                ul = "B3",
                ll = "B2",
                bitmask = 32UL,
            ),
            NodeDef(
                "C4",
                "WY",
                isDot = false,
                isSpot = true,
                above = "C5",
                below = "C3",
                ur = "D5",
                lr = "D4",
                ul = "B4",
                ll = "B3",
                bitmask = 64UL,
            ),
            NodeDef(
                "C5",
                null,
                isDot = false,
                isSpot = true,
                above = "C6",
                below = "C4",
                ur = "D6",
                lr = "D5",
                ul = "B5",
                ll = "B4",
                bitmask = 128UL,
            ),
            NodeDef(
                "C6",
                "BD",
                isDot = false,
                isSpot = true,
                below = "C5",
                ur = "D7",
                lr = "D6",
                ll = "B5",
                bitmask = 256UL,
            ),
            NodeDef("C7", null, isDot = true, isSpot = false, below = "C6", lr = "D7"),

            // Column D
            NodeDef("D1", null, isDot = true, isSpot = false, above = "D2", ur = "E2"),
            NodeDef(
                "D2",
                null,
                isDot = false,
                isSpot = true,
                above = "D3",
                ur = "E3",
                lr = "E2",
                ul = "C2",
                bitmask = 512UL,
            ),
            NodeDef(
                "D3",
                null,
                isDot = false,
                isSpot = true,
                above = "D4",
                below = "D2",
                ur = "E4",
                lr = "E3",
                ul = "C3",
                ll = "C2",
                bitmask = 1024UL,
            ),
            NodeDef(
                "D4",
                null,
                isDot = false,
                isSpot = true,
                above = "D5",
                below = "D3",
                ur = "E5",
                lr = "E4",
                ul = "C4",
                ll = "C3",
                bitmask = 2048UL,
            ),
            NodeDef(
                "D5",
                "WT",
                isDot = false,
                isSpot = true,
                above = "D6",
                below = "D4",
                ur = "E6",
                lr = "E5",
                ul = "C5",
                ll = "C4",
                bitmask = 4096UL,
            ),
            NodeDef(
                "D6",
                null,
                isDot = false,
                isSpot = true,
                above = "D7",
                below = "D5",
                ur = "E7",
                lr = "E6",
                ul = "C6",
                ll = "C5",
                bitmask = 8192UL,
            ),
            NodeDef(
                "D7",
                "WT",
                isDot = false,
                isSpot = true,
                below = "D6",
                ur = "E8",
                lr = "E7",
                ll = "C6",
                bitmask = 16384UL,
            ),
            NodeDef("D8", null, isDot = true, isSpot = false, below = "D7", lr = "E8"),

            // Column E
            NodeDef("E1", null, isDot = true, isSpot = false, above = "E2"),
            NodeDef(
                "E2",
                "WD",
                isDot = false,
                isSpot = true,
                above = "E3",
                ur = "F2",
                ul = "D2",
                bitmask = 32768UL,
            ),
            NodeDef(
                "E3",
                "WG",
                isDot = false,
                isSpot = true,
                above = "E4",
                below = "E2",
                ur = "F3",
                lr = "F2",
                ul = "D3",
                ll = "D2",
                bitmask = 65536UL,
            ),
            NodeDef(
                "E4",
                null,
                isDot = false,
                isSpot = true,
                above = "E5",
                below = "E3",
                ur = "F4",
                lr = "F3",
                ul = "D4",
                ll = "D3",
                bitmask = 131072UL,
            ),
            NodeDef(
                "E5",
                null,
                isDot = false,
                isSpot = true,
                isCenter = true,
                above = "E6",
                below = "E4",
                ur = "F5",
                lr = "F4",
                ul = "D5",
                ll = "D4",
                bitmask = 262144UL,
            ),
            NodeDef(
                "E6",
                "WT",
                isDot = false,
                isSpot = true,
                above = "E7",
                below = "E5",
                ur = "F6",
                lr = "F5",
                ul = "D6",
                ll = "D5",
                bitmask = 524288UL,
            ),
            NodeDef(
                "E7",
                "WG",
                isDot = false,
                isSpot = true,
                above = "E8",
                below = "E6",
                ur = "F7",
                lr = "F6",
                ul = "D7",
                ll = "D6",
                bitmask = 1048576UL,
            ),
            NodeDef(
                "E8",
                "BP",
                isDot = false,
                isSpot = true,
                below = "E7",
                lr = "F7",
                ll = "D7",
                bitmask = 2097152UL,
            ),
            NodeDef("E9", null, isDot = true, isSpot = false, below = "E8"),

            // Column F
            NodeDef("F1", null, isDot = true, isSpot = false, above = "F2", ul = "E2"),
            NodeDef(
                "F2",
                "BG",
                isDot = false,
                isSpot = true,
                above = "F3",
                ur = "G2",
                ul = "E3",
                ll = "E2",
                bitmask = 4194304UL,
            ),
            NodeDef(
                "F3",
                null,
                isDot = false,
                isSpot = true,
                above = "F4",
                below = "F2",
                ur = "G3",
                lr = "G2",
                ul = "E4",
                ll = "E3",
                bitmask = 8388608UL,
            ),
            NodeDef(
                "F4",
                null,
                isDot = false,
                isSpot = true,
                above = "F5",
                below = "F3",
                ur = "G4",
                lr = "G3",
                ul = "E5",
                ll = "E4",
                bitmask = 16777216UL,
            ),
            NodeDef(
                "F5",
                null,
                isDot = false,
                isSpot = true,
                above = "F6",
                below = "F4",
                ur = "G5",
                lr = "G4",
                ul = "E6",
                ll = "E5",
                bitmask = 33554432UL,
            ),
            NodeDef(
                "F6",
                null,
                isDot = false,
                isSpot = true,
                above = "F7",
                below = "F5",
                ur = "G6",
                lr = "G5",
                ul = "E7",
                ll = "E6",
                bitmask = 67108864UL,
            ),
            NodeDef(
                "F7",
                "BG",
                isDot = false,
                isSpot = true,
                below = "F6",
                lr = "G6",
                ul = "E8",
                ll = "E7",
                bitmask = 134217728UL,
            ),
            NodeDef("F8", null, isDot = true, isSpot = false, below = "F7", ll = "E8"),

            // Column G
            NodeDef("G1", null, isDot = true, isSpot = false, above = "G2", ul = "F2"),
            NodeDef(
                "G2",
                null,
                isDot = false,
                isSpot = true,
                above = "G3",
                ur = "H2",
                ul = "F3",
                ll = "F2",
                bitmask = 268435456UL,
            ),
            NodeDef(
                "G3",
                null,
                isDot = false,
                isSpot = true,
                above = "G4",
                below = "G2",
                ur = "H3",
                lr = "H2",
                ul = "F4",
                ll = "F3",
                bitmask = 536870912UL,
            ),
            NodeDef(
                "G4",
                null,
                isDot = false,
                isSpot = true,
                above = "G5",
                below = "G3",
                ur = "H4",
                lr = "H3",
                ul = "F5",
                ll = "F4",
                bitmask = 1073741824UL,
            ),
            NodeDef(
                "G5",
                null,
                isDot = false,
                isSpot = true,
                above = "G6",
                below = "G4",
                ur = "H5",
                lr = "H4",
                ul = "F6",
                ll = "F5",
                bitmask = 2147483648UL,
            ),
            NodeDef(
                "G6",
                null,
                isDot = false,
                isSpot = true,
                below = "G5",
                lr = "H5",
                ul = "F7",
                ll = "F6",
                bitmask = 4294967296UL,
            ),
            NodeDef("G7", null, isDot = true, isSpot = false, below = "G6", ll = "F7"),

            // Column H
            NodeDef("H1", null, isDot = true, isSpot = false, above = "H2", ul = "G2"),
            NodeDef(
                "H2",
                "BD",
                isDot = false,
                isSpot = true,
                above = "H3",
                ur = "I2",
                ul = "G3",
                ll = "G2",
                bitmask = 8589934592UL,
            ),
            NodeDef(
                "H3",
                "BT",
                isDot = false,
                isSpot = true,
                above = "H4",
                below = "H2",
                ur = "I3",
                lr = "I2",
                ul = "G4",
                ll = "G3",
                bitmask = 17179869184UL,
            ),
            NodeDef(
                "H4",
                "BG",
                isDot = false,
                isSpot = true,
                above = "H5",
                below = "H3",
                ur = "I4",
                lr = "I3",
                ul = "G5",
                ll = "G4",
                bitmask = 34359738368UL,
            ),
            NodeDef(
                "H5",
                "WD",
                isDot = false,
                isSpot = true,
                below = "H4",
                lr = "I4",
                ul = "G6",
                ll = "G5",
                bitmask = 68719476736UL,
            ),
            NodeDef("H6", null, isDot = true, isSpot = false, below = "H5", ll = "G6"),

            // Column I
            NodeDef("I1", null, isDot = true, isSpot = false, above = "I2", ul = "H2"),
            NodeDef(
                "I2",
                "BY",
                isDot = false,
                isSpot = true,
                above = "I3",
                ul = "H3",
                ll = "H2",
                bitmask = 137438953472UL,
            ),
            NodeDef(
                "I3",
                "BZ",
                isDot = false,
                isSpot = true,
                above = "I4",
                ul = "H4",
                ll = "H3",
                bitmask = 274877906944UL,
            ),
            NodeDef(
                "I4",
                "WD",
                isDot = false,
                isSpot = true,
                below = "I3",
                ul = "H5",
                ll = "H4",
                bitmask = 549755813888UL,
            ),
            NodeDef("I5", null, isDot = true, isSpot = false, below = "I4", ll = "H5"),

            // Column J
            NodeDef("J1", null, isDot = true, isSpot = false, ul = "I2"),
            NodeDef("J2", null, isDot = true, isSpot = false, ul = "I3", ll = "I2"),
            NodeDef("J3", null, isDot = true, isSpot = false, ul = "I4", ll = "I3"),
            NodeDef("J4", null, isDot = true, isSpot = false, ll = "I4"),
        )

    val nodesList = LinkedHashSet(nodeDefinitions.map { it.toNode() })

    // 2. Initialize Boards
    val board = Board(nodes = nodesList, centerNodeCoordinate = coord("E5"))
    val initBoard = Board(nodes = nodesList, centerNodeCoordinate = coord("E5"))

    val initBlackBoardPieces =
        initBoard.nodes.filter { node ->
          node.piece?.colorName == PlayerName.BLACK.name
        }

    val initWhiteBoardPieces =
        initBoard.nodes.filter { node ->
          node.piece?.colorName == PlayerName.WHITE.name
        }

    val initBitboard = convertBoardToBitboard(initBoard)

    val modifiableBitboard = convertBoardToBitboard(initBoard)

    // 3. Initialize Reserves and Players
    val blackReserve =
        mutableListOf(
            piece("BT"),
            piece("BT"),
            piece("BZ"),
            piece("BZ"),
            piece("BY"),
            piece("BY"),
            piece("BP"),
            piece("BP"),
        )

    val whiteReserve =
        mutableListOf(
            piece("WT"),
            piece("WT"),
            piece("WZ"),
            piece("WZ"),
            piece("WZ"),
            piece("WY"),
            piece("WY"),
            piece("WY"),
            piece("WP"),
            piece("WP"),
            piece("WP"),
        )

    val nextPlayer =
        Player(name = PlayerName.BLACK, abbreviation = "B", piecesInReserve = blackReserve)
    val currentPlayer =
        Player(name = PlayerName.WHITE, abbreviation = "W", piecesInReserve = whiteReserve)

    // 4. Initialize Bitboard
    val finalBitboard =
        Bitboard(
            whiteGIPF = 1114128UL,
            whiteDVONNLayer = ulongArrayOf(618475323392UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 16384UL,
            whiteYINSH = 0UL,
            whiteZERTZ = 0UL,
            whitePotentials = 618475339776UL,
            blackGIPF = 34498150400UL,
            blackDVONNLayer = ulongArrayOf(8589934856UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(2097152UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 17179869184UL,
            blackYINSH = 137438953472UL,
            blackZERTZ = 274877906944UL,
            blackPotentials = 438088761608UL,
            //      globalOccupancy = 1091063365912UL,
            //      whitePieces = 618476453904UL,
            //      blackPieces = 472586912008UL,
            //      whiteNeutralized = 0UL,
            //      blackNeutralized = 0UL
        )

    finalBitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = nextPlayer)

    // 5. Initialize Best Move
    val retrievedCapturedPiecesBit =
        listOf(
            RetrievedCapturedPieceBit(bitmask = 2UL),
            RetrievedCapturedPieceBit(bitmask = 64UL),
            RetrievedCapturedPieceBit(bitmask = 4096UL),
            RetrievedCapturedPieceBit(bitmask = 524288UL),
        )

    val bestMove =
        PossibleBitMove(
            retrievedCapturedPiecesBit = retrievedCapturedPiecesBit,
            moveType = MoveType.RetrieveCapturePieces,
        )

    val retrievedCapturedPieces =
        modifiableBitboard.removeSelectedPiecesToRemove(
            player = currentPlayer,
            piecesToRemove = bestMove.retrievedCapturedPiecesBit,
        )

    val centerCoordinate = Coordinate(column = 'E', row = 5)

    val nodes =
        constructNodes(
            letters = LETTERS,
            rows = ROWS.toList(),
            centerLetterIndex = LETTERS.indexOf(centerCoordinate.column),
            center = centerCoordinate,
        )

    // Populate node neighbours
    nodes.forEach { node ->
      node.neighbors =
          populateNeighbors(
              coordinate = node.coordinate,
              nodes = nodes,
          )
    }

    modifiableBitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = nextPlayer)

    modifiableBitboard.validatePieceRemoval(retrievedCapturedPieces)

    val convertedBitboardBoard = modifiableBitboard.convertBitboardToBoard(board)

    val blackBoardPieces =
        convertedBitboardBoard.nodes.filter { node ->
          node.piece?.colorName == PlayerName.BLACK.name
        }

    val whiteBoardPieces =
        convertedBitboardBoard.nodes.filter { node ->
          node.piece?.colorName == PlayerName.WHITE.name
        }

    println(Json.encodeToString(blackBoardPieces))
    println(Json.encodeToString(whiteBoardPieces))

    val state =
        State(
            currentPlayer = currentPlayer,
            nextPlayer = nextPlayer,
            board = convertedBitboardBoard,
          bitboard = modifiableBitboard,
            lines = constructLines(nodes = convertedBitboardBoard.nodes),
        )

    state.assertPieceCount()
  }

  @Test
  fun `get PUNCT Moves`() {
    val bitboard =
        Bitboard(
            whiteGIPF = 137472507908UL,
            whiteTAMSK = 0UL,
            whiteYINSH = 4194336UL,
            whiteZERTZ = 134217728UL,
            whitePotentials = 8745140520UL,
            blackGIPF = 268435584UL,
            blackTAMSK = 8388608UL,
            blackYINSH = 0UL,
            blackZERTZ = 2163712UL,
            blackPotentials = 10552851UL,
        )

    bitboard.whiteDVONNLayer[0] = 16793608UL
    bitboard.whitePUNCTLayer[0] = 68UL
    bitboard.whitePUNCTLayer[1] = 64UL
    bitboard.whitePUNCTLayer[2] = 64UL
    bitboard.whitePUNCTLayer[3] = 64UL
    bitboard.whitePUNCTLayer[4] = 64UL
    bitboard.whitePUNCTLayer[5] = 64UL
    bitboard.blackDVONNLayer[0] = 19UL
    bitboard.blackPUNCTLayer[0] = 512UL

    val whitePlayer =
        Player(
            name = PlayerName.WHITE,
            abbreviation = "W",
            piecesInReserve =
                mutableListOf(
                    Piece(
                        abbreviation = "WT",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.TAMSK,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WT",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.TAMSK,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WT",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.TAMSK,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WZ",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.ZERTZ,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WZ",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.ZERTZ,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WY",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.YINSH,
                        isNeutralized = false,
                    ),
                    Piece(
                        abbreviation = "WP",
                        potential = true,
                        colorName = PlayerName.WHITE.name,
                        type = PieceType.PUNCT,
                        isNeutralized = false,
                    ),
                ),
        )

    bitboard.getPunctMoves(whitePlayer, org.example.model.columnInfos)
  }

  @Test
  fun `get PUNCT Moves 2`() {
    // 1. Initialize the updated Bitboard
    val bitboard =
        Bitboard(
            whiteGIPF = 2147483776UL,
            whiteDVONNLayer = ulongArrayOf(68732059680UL, 32UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(16908288UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 549822922752UL,
            whiteYINSH = 283467841536UL,
            whiteZERTZ = 268435536UL,
            whitePotentials = 833563394048UL,
            blackGIPF = 38654705664UL,
            blackDVONNLayer = ulongArrayOf(262656UL, 262144UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer =
                ulongArrayOf(
                    138512695296UL,
                    1073741824UL,
                    1073741824UL,
                    1073741824UL,
                    1073741824UL,
                    0UL,
                ),
            blackTAMSK = 17179869184UL,
            blackYINSH = 32768UL,
            blackZERTZ = 537395202UL,
            blackPotentials = 18253611522UL,
        )

    // 2. Initialize Current and Opponent Players
    val currentPlayer =
        Player(
            name = PlayerName.BLACK,
            abbreviation = "B",
            piecesInReserve =
                mutableListOf(
                    piece("BZ", potential = false),
                    piece("BY", potential = false),
                ),
            capturedPieces =
                mutableListOf(
                    piece("WZ", potential = true),
                    piece("WT", potential = true),
                    piece("WG", potential = false),
                    piece("WY", potential = false),
                ),
        )

    val opponentPlayer =
        Player(
            name = PlayerName.WHITE,
            abbreviation = "W",
            piecesInReserve =
                mutableListOf(
                    piece("WY", potential = false),
                    piece("WP", potential = false),
                ),
            capturedPieces =
                mutableListOf(
                    piece("BT", potential = true),
                    piece("BG", potential = false),
                    piece("BT", potential = true),
                    piece("BY", potential = true),
                    piece("BY", potential = true),
                    piece("BD", potential = false),
                    piece("BZ", potential = false),
                    piece("BD", potential = false),
                ),
        )

    // 3. Initialize Possible Moves list
    val possibleBitMoves =
        listOf(
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 2UL,
                targetBit = 4096UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.BLACK,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 2UL,
                targetBit = 1024UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.BLACK,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 512UL,
                targetBit = 8388608UL,
                pieceType = PieceType.DVONN,
                pieceColor = PlayerName.BLACK,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 137438953472UL,
                targetBit = 1073741824UL,
                pieceType = PieceType.PUNCT,
                pieceColor = PlayerName.BLACK,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
        )

    // 4. Assign the randomly selected PossibleBitMove
    val randomPossibleBitMove = possibleBitMoves[3]

    val punctMoves = bitboard.getPunctMoves(currentPlayer)

    assert(punctMoves.isNotEmpty())
  }

  @Test
  fun `get PUNCT Moves 3`() {
    // 1. Initialize Active Bitboard
    val bitboard =
        Bitboard(
            whiteGIPF = 10UL,
            whiteDVONNLayer = ulongArrayOf(134217728UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(268443648UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 131204UL,
            whiteYINSH = 4194560UL,
            whiteZERTZ = 1104UL,
            whitePotentials = 134357460UL,
            blackGIPF = 2097152UL,
            blackDVONNLayer = ulongArrayOf(536870912UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer =
                ulongArrayOf(
                    687194767360UL,
                    137438953472UL,
                    137438953472UL,
                    137438953472UL,
                    137438953472UL,
                    137438953472UL,
                    0UL,
                    0UL
                ),
            blackTAMSK = 4294967328UL,
            blackYINSH = 18432UL,
            blackZERTZ = 17858560UL,
            blackPotentials = 142287618080UL,
        )

    // 2. Initialize Init Bitboard (slightly different blackPotentials field)
    val initBitboard =
        Bitboard(
            whiteGIPF = 10UL,
            whiteDVONNLayer = ulongArrayOf(134217728UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(268443648UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 131204UL,
            whiteYINSH = 4194560UL,
            whiteZERTZ = 1104UL,
            whitePotentials = 134357460UL,
            blackGIPF = 2097152UL,
            blackDVONNLayer = ulongArrayOf(536870912UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer =
                ulongArrayOf(
                    687194767360UL,
                    137438953472UL,
                    137438953472UL,
                    137438953472UL,
                    137438953472UL,
                    137438953472UL,
                    0UL,
                    0UL
                ),
            blackTAMSK = 4294967328UL,
            blackYINSH = 18432UL,
            blackZERTZ = 17858560UL,
            blackPotentials = 692043431968UL, // Notice difference from bitboard
        )

    // 3. Initialize Current and Opponent Players
    val currentPlayer =
        Player(
            name = PlayerName.BLACK,
            abbreviation = "B",
            piecesInReserve =
                mutableListOf(
                    piece("BZ", potential = false),
                    piece("BY", potential = false),
                ),
            capturedPieces =
                mutableListOf(
                    piece("WY", potential = true),
                    piece("WD", potential = true),
                    piece("WG", potential = false),
                ),
        )

    val opponentPlayer =
        Player(
            name = PlayerName.WHITE,
            abbreviation = "W",
            piecesInReserve =
                mutableListOf(
                    piece("WZ", potential = false),
                    piece("WD", potential = true),
                    piece("WY", potential = false),
                ),
            capturedPieces =
                mutableListOf(
                    piece("BY", potential = true),
                    piece("BD", potential = true),
                    piece("BG", potential = false),
                    piece("BG", potential = false),
                    piece("BD", potential = true),
                    piece("BT", potential = true), // Inferred TAMSK from 'TAMS'
                ),
        )

    // 4. Initialize Possible Moves
    val possibleBitMoves =
        listOf(
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 32768UL,
                targetBit = 8589934592UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.BLACK,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 16777216UL,
                targetBit = 8589934592UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.BLACK,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 549755813888UL,
                targetBit = 137438953472UL,
                pieceType = PieceType.PUNCT,
                pieceColor = PlayerName.BLACK,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
        )

    // 5. Assign Selected Random Move
    val randomPossibleBitMove = possibleBitMoves[2]

    val punctMoves = initBitboard.getPunctMoves(currentPlayer)

    assert(punctMoves.isNotEmpty())
  }

  @Test
  fun `use potential`() {
    // 1. Initialize Active Bitboard (with size-8 arrays)
    val bitboard =
        Bitboard(
            whiteGIPF = 131072UL,
            whiteDVONNLayer = ulongArrayOf(67108928UL, 64UL, 64UL, 64UL, 64UL, 64UL, 64UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(4311744512UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 65664UL,
            whiteYINSH = 38400UL,
            whiteZERTZ = 551936851976UL,
            whitePotentials = 556215108288UL,
            blackGIPF = 0UL,
            blackDVONNLayer = ulongArrayOf(256UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(268959746UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 1UL,
            blackYINSH = 26306674692UL,
            blackZERTZ = 26640UL,
            blackPotentials = 268967959UL,
        )

    // 2. Initialize Init Bitboard (slightly different whitePotentials and whiteDVONNLayer)
    val initBitboard =
        Bitboard(
            whiteGIPF = 131072UL,
            whiteDVONNLayer = ulongArrayOf(67108928UL, 64UL, 64UL, 64UL, 64UL, 64UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(4311744512UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 65664UL,
            whiteYINSH = 38400UL,
            whiteZERTZ = 551936851976UL,
            whitePotentials = 556282217152UL, // Notice difference from bitboard
            blackGIPF = 0UL,
            blackDVONNLayer = ulongArrayOf(256UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(268959746UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 1UL,
            blackYINSH = 26306674692UL,
            blackZERTZ = 26640UL,
            blackPotentials = 268967959UL,
        )

    // 3. Initialize Current and Opponent Players
    val currentPlayer =
        Player(
            name = PlayerName.WHITE,
            abbreviation = "W",
            piecesInReserve = mutableListOf(piece("WY", potential = false)),
            capturedPieces =
                mutableListOf(
                    piece("BG", potential = false),
                    piece("BG", potential = false),
                    piece("BG", potential = false),
                    piece("BT", potential = true),
                    piece("BY", potential = false),
                    piece("BT", potential = true),
                    piece("BD", potential = true),
                ),
        )

    val opponentPlayer =
        Player(
            name = PlayerName.BLACK,
            abbreviation = "B",
            piecesInReserve = mutableListOf(),
            capturedPieces =
                mutableListOf(
                    piece("WT", potential = true),
                    piece("WG", potential = false),
                    piece("WP", potential = true),
                    piece("WG", potential = false),
                ),
        )

    // 4. Initialize Possible Moves
    val possibleBitMoves =
        listOf(
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 2147483648UL,
                targetBit = 262144UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.WHITE,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 2147483648UL,
                targetBit = 1048576UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.WHITE,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 67108864UL,
                targetBit = 64UL,
                pieceType = PieceType.DVONN,
                pieceColor = PlayerName.WHITE,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 4294967296UL,
                targetBit = 268435456UL,
                pieceType = PieceType.PUNCT,
                pieceColor = PlayerName.WHITE,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 4294967296UL,
                targetBit = 524288UL,
                pieceType = PieceType.PUNCT,
                pieceColor = PlayerName.WHITE,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = null,
                columnInfos = emptyList(),
                retrievedCapturedPiecesBit = emptyList(),
                sourceBit = 4294967296UL,
                targetBit = 2UL,
                pieceType = PieceType.PUNCT,
                pieceColor = PlayerName.WHITE,
                pushDirection = null,
                moveType = MoveType.UsePotential,
            ),
        )

    // 5. Assign Selected Random Move
    val randomPossibleBitMove = possibleBitMoves[2]

    initBitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)

    val modifiableBitboard = initBitboard.deepCopy()

    modifiableBitboard.usePiecePotential(
        randomPossibleBitMove,
        currentPlayer,
        nextPlayer = opponentPlayer,
    )

    modifiableBitboard.assertPieceCount(currentPlayer = currentPlayer, nextPlayer = opponentPlayer)
  }

  @Test
  fun `remove Selected Pieces`() {

  }

   @Test
   fun `get DVONN Moves`(){
  /**
   * TODO The DVONN potential has the ability to jump atop any of the opponent's DVONN pieces (individual pieces or stacks)
   *  as long as the both DVONN pieces are on the same line and there are no other pieces in between them.
   * */
  // 1. Initialize Active Bitboard (with size-8 arrays)
  val bitboard =
    Bitboard(
      whiteGIPF = 0UL,
      whiteDVONNLayer = ulongArrayOf(1UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      whitePUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      whiteTAMSK = 0UL,
      whiteYINSH = 0UL,
      whiteZERTZ = 0UL,
      whitePotentials = 1UL,
      blackGIPF = 0UL,
      blackDVONNLayer = ulongArrayOf(8UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      blackPUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      blackTAMSK = 0UL,
      blackYINSH = 0UL,
      blackZERTZ = 0UL,
      blackPotentials = 8UL,
    )

     // 2. Initialize Init Bitboard (slightly different whitePotentials and whiteDVONNLayer)
     val initBitboard = Bitboard(
       whiteGIPF = 0UL,
       whiteDVONNLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
       whitePUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
       whiteTAMSK = 0UL,
       whiteYINSH = 0UL,
       whiteZERTZ = 0UL,
       whitePotentials = 0UL,
       blackGIPF = 0UL,
       blackDVONNLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
       blackPUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
       blackTAMSK = 0UL,
       blackYINSH = 0UL,
       blackZERTZ = 0UL,
       blackPotentials = 0UL,
     )


     // 3. Initialize Current and Opponent Players
     val currentPlayer =
       Player(
         name = PlayerName.WHITE,
         abbreviation = "W",
         piecesInReserve = mutableListOf(piece("WY", potential = false)),
       )

     val opponentPlayer =
       Player(
         name = PlayerName.BLACK,
         abbreviation = "B",
         piecesInReserve = mutableListOf(),
       )

     val dvonnMoves = bitboard.getDvonnMoves(
	     player = currentPlayer,
       columnInfos = org.example.model.columnInfos
     )

     assertTrue(dvonnMoves.isNotEmpty())
   }

  @Test
  fun `get DVONN Moves when Blocked`(){
    /**
     * TODO The DVONN potential has the ability to jump atop any of the opponent's DVONN pieces (individual pieces or stacks)
     *  as long as the both DVONN pieces are on the same line and there are no other pieces in between them.
     * */
    // 1. Initialize Active Bitboard (with size-8 arrays)
    val bitboard =
      Bitboard(
        whiteGIPF = 2UL,
        whiteDVONNLayer = ulongArrayOf(1UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        whitePUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        whiteTAMSK = 0UL,
        whiteYINSH = 0UL,
        whiteZERTZ = 0UL,
        whitePotentials = 1UL,
        blackGIPF = 0UL,
        blackDVONNLayer = ulongArrayOf(8UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        blackPUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
        blackTAMSK = 0UL,
        blackYINSH = 0UL,
        blackZERTZ = 0UL,
        blackPotentials = 8UL,
      )

    // 2. Initialize Init Bitboard (slightly different whitePotentials and whiteDVONNLayer)
    val initBitboard = Bitboard(
      whiteGIPF = 0UL,
      whiteDVONNLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      whitePUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      whiteTAMSK = 0UL,
      whiteYINSH = 0UL,
      whiteZERTZ = 0UL,
      whitePotentials = 0UL,
      blackGIPF = 0UL,
      blackDVONNLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      blackPUNCTLayer = ulongArrayOf(0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      blackTAMSK = 0UL,
      blackYINSH = 0UL,
      blackZERTZ = 0UL,
      blackPotentials = 0UL,
    )


    // 3. Initialize Current and Opponent Players
    val currentPlayer =
      Player(
        name = PlayerName.WHITE,
        abbreviation = "W",
        piecesInReserve = mutableListOf(piece("WY", potential = false)),
      )

    val opponentPlayer =
      Player(
        name = PlayerName.BLACK,
        abbreviation = "B",
        piecesInReserve = mutableListOf(),
      )

    val dvonnMoves = bitboard.getDvonnMoves(
      player = currentPlayer,
      columnInfos = org.example.model.columnInfos
    )

    assertTrue(dvonnMoves.isEmpty())
  }

  @Test
  fun `getEligiblePotentialMoves`(){
    // 1. Re-create the 65 Nodes with the exact piece configurations of the dump
    val nodeDefinitions = listOf(
      // Column A
      NodeDef("A1", null, isDot = true, isSpot = false, ur = "B2"),
      NodeDef("A2", null, isDot = true, isSpot = false, ur = "B3", lr = "B2"),
      NodeDef("A3", null, isDot = true, isSpot = false, ur = "B4", lr = "B3"),
      NodeDef("A4", null, isDot = true, isSpot = false, ur = "B5", lr = "B4"),
      NodeDef("A5", null, isDot = true, isSpot = false, lr = "B5"),

      // Column B
      NodeDef("B1", null, isDot = true, isSpot = false, above = "B2", ur = "C2"),
      NodeDef("B2", "BY", isDot = false, isSpot = true, above = "B3", ur = "C3", lr = "C2", bitmask = 1UL),
      NodeDef("B3", "WZ", isDot = false, isSpot = true, above = "B4", below = "B2", ur = "C4", lr = "C3", bitmask = 2UL),
      NodeDef("B4", "BT", isDot = false, isSpot = true, above = "B5", below = "B3", ur = "C5", lr = "C4", bitmask = 4UL),
      NodeDef("B5", null, isDot = false, isSpot = true, below = "B4", ur = "C6", lr = "C5", bitmask = 8UL),
      NodeDef("B6", null, isDot = true, isSpot = false, below = "B5", lr = "C6"),

      // Column C
      NodeDef("C1", null, isDot = true, isSpot = false, above = "C2", ur = "D2"),
      NodeDef("C2", "BY", isDot = false, isSpot = true, above = "C3", ur = "D3", lr = "D2", ul = "B2", bitmask = 16UL),
      NodeDef("C3", "BP", isDot = false, isSpot = true, above = "C4", below = "C2", ur = "D4", lr = "D3", ul = "B3", ll = "B2", bitmask = 32UL),
      NodeDef("C4", null, isDot = false, isSpot = true, above = "C5", below = "C3", ur = "D5", lr = "D4", ul = "B4", ll = "B3", bitmask = 64UL),
      NodeDef("C5", null, isDot = false, isSpot = true, above = "C6", below = "C4", ur = "D6", lr = "D5", ul = "B5", ll = "B4", bitmask = 128UL),
      NodeDef("C6", null, isDot = false, isSpot = true, below = "C5", ur = "D7", lr = "D6", ll = "B5", bitmask = 256UL),
      NodeDef("C7", null, isDot = true, isSpot = false, below = "C6", lr = "D7"),

      // Column D
      NodeDef("D1", null, isDot = true, isSpot = false, above = "D2", ur = "E2"),
      NodeDef("D2", "WZ", isDot = false, isSpot = true, above = "D3", ur = "E3", lr = "E2", ul = "C2", bitmask = 512UL),
      NodeDef("D3", "BP", isDot = false, isSpot = true, above = "D4", below = "D2", ur = "E4", lr = "E3", ul = "C3", ll = "C2", bitmask = 1024UL),
      NodeDef("D4", "WT", isDot = false, isSpot = true, above = "D5", below = "D3", ur = "E5", lr = "E4", ul = "C4", ll = "C3", bitmask = 2048UL),
      NodeDef("D5", "WT", isDot = false, isSpot = true, above = "D6", below = "D4", ur = "E6", lr = "E5", ul = "C5", ll = "C4", bitmask = 4096UL),
      NodeDef("D6", "BT", isDot = false, isSpot = true, above = "D7", below = "D5", ur = "E7", lr = "E6", ul = "C6", ll = "C5", bitmask = 8192UL),
      NodeDef("D7", "WD", isDot = false, isSpot = true, below = "D6", ur = "E8", lr = "E7", ll = "C6", bitmask = 16384UL),
      NodeDef("D8", null, isDot = true, isSpot = false, below = "D7", lr = "E8"),

      // Column E
      NodeDef("E1", null, isDot = true, isSpot = false, above = "E2"),
      NodeDef("E2", "WT", isDot = false, isSpot = true, above = "E3", ur = "F2", ul = "D2", bitmask = 32768UL),
      NodeDef("E3", "BZ", isDot = false, isSpot = true, above = "E4", below = "E2", ur = "F3", lr = "F2", ul = "D3", ll = "D2", bitmask = 65536UL),
      NodeDef("E4", "WG", isDot = false, isSpot = true, potential = false, above = "E5", below = "E3", ur = "F4", lr = "F3", ul = "D4", ll = "D3", bitmask = 131072UL),
      NodeDef("E5", null, isDot = false, isSpot = true, isCenter = true, above = "E6", below = "E4", ur = "F5", lr = "F4", ul = "D5", ll = "D4", bitmask = 262144UL),
      NodeDef("E6", "BZ", isDot = false, isSpot = true, potential = false, above = "E7", below = "E5", ur = "F6", lr = "F5", ul = "D6", ll = "D5", bitmask = 524288UL),
      NodeDef("E7", null, isDot = false, isSpot = true, above = "E8", below = "E6", ur = "F7", lr = "F6", ul = "D7", ll = "D6", bitmask = 1048576UL),
      NodeDef("E8", "WP", isDot = false, isSpot = true, below = "E7", lr = "F7", ll = "D7", bitmask = 2097152UL),
      NodeDef("E9", null, isDot = true, isSpot = false, below = "E8"),

      // Column F
      NodeDef("F1", null, isDot = true, isSpot = false, above = "F2", ul = "E2"),
      NodeDef("F2", "WG", isDot = false, isSpot = true, potential = false, above = "F3", ur = "G2", ul = "E3", ll = "E2", bitmask = 4194304UL),
      NodeDef("F3", "BY", isDot = false, isSpot = true, above = "F4", ur = "G3", lr = "G2", ul = "E4", ll = "E3", bitmask = 8388608UL),
      NodeDef("F4", "BG", isDot = false, isSpot = true, potential = false, above = "F5", ur = "G4", lr = "G3", ul = "E5", ll = "E4", bitmask = 16777216UL),
      NodeDef("F5", "WY", isDot = false, isSpot = true, potential = false, above = "F6", ur = "G5", lr = "G4", ul = "E6", ll = "E5", bitmask = 33554432UL),
      NodeDef("F6", "BT", isDot = false, isSpot = true, above = "F7", ur = "G6", lr = "G5", ul = "E7", ll = "E6", bitmask = 67108864UL),
      NodeDef("F7", "BP", isDot = false, isSpot = true, below = "F6", lr = "G6", ul = "E8", ll = "E7", bitmask = 134217728UL),
      NodeDef("F8", null, isDot = true, isSpot = false, below = "F7", ll = "E8"),

      // Column G
      NodeDef("G1", null, isDot = true, isSpot = false, above = "G2", ul = "F2"),
      NodeDef("G2", "BD", isDot = false, isSpot = true, above = "G3", ur = "H2", ul = "F3", ll = "F2", bitmask = 268435456UL),
      NodeDef("G3", "WY", isDot = false, isSpot = true, potential = false, above = "G4", ur = "H3", lr = "H2", ul = "F4", ll = "F3", bitmask = 536870912UL),
      NodeDef("G4", "WP", isDot = false, isSpot = true, above = "G5", ur = "H4", lr = "H3", ul = "F5", ll = "F4", bitmask = 1073741824UL),
      NodeDef("G5", null, isDot = false, isSpot = true, above = "G6", ur = "H5", lr = "H4", ul = "F6", ll = "F5", bitmask = 2147483648UL),
      NodeDef("G6", null, isDot = false, isSpot = true, below = "G5", lr = "H5", ul = "F7", ll = "F6", bitmask = 4294967296UL),
      NodeDef("G7", null, isDot = true, isSpot = false, below = "G6", ll = "F7"),

      // Column H
      NodeDef("H1", null, isDot = true, isSpot = false, above = "H2", ul = "G2"),
      NodeDef("H2", "WY", isDot = false, isSpot = true, potential = false, above = "H3", ur = "I2", ul = "G3", ll = "G2", bitmask = 8589934592UL),
      NodeDef("H3", "BZ", isDot = false, isSpot = true, potential = false, above = "H4", ur = "I3", lr = "I2", ul = "G4", ll = "G3", bitmask = 17179869184UL),
      NodeDef("H4", null, isDot = false, isSpot = true, above = "H5", ur = "I4", lr = "I3", ul = "G5", ll = "G4", bitmask = 34359738368UL),
      NodeDef("H5", "WD", isDot = false, isSpot = true, below = "H4", lr = "I4", ul = "G6", ll = "G5", bitmask = 68719476736UL),
      NodeDef("H6", null, isDot = true, isSpot = false, below = "H5", ll = "G6"),

      // Column I
      NodeDef("I1", null, isDot = true, isSpot = false, above = "I2", ul = "H2"),
      NodeDef("I2", "BD", isDot = false, isSpot = true, above = "I3", ul = "H3", ll = "H2", bitmask = 137438953472UL),
      NodeDef("I3", "WZ", isDot = false, isSpot = true, above = "I4", ul = "H4", ll = "H3", bitmask = 274877906944UL),
      NodeDef("I4", "WD", isDot = false, isSpot = true, below = "I3", ul = "H5", ll = "H4", bitmask = 549755813888UL),
      NodeDef("I5", null, isDot = true, isSpot = false, below = "I4", ll = "H5"),

      // Column J
      NodeDef("J1", null, isDot = true, isSpot = false, ul = "I2"),
      NodeDef("J2", null, isDot = true, isSpot = false, ul = "I3", ll = "I2"),
      NodeDef("J3", null, isDot = true, isSpot = false, ul = "I4", ll = "I3"),
      NodeDef("J4", null, isDot = true, isSpot = false, ll = "I4")
    )

    val nodesList = LinkedHashSet(nodeDefinitions.map { it.toNode() })
    val board = Board(nodes = nodesList, centerNodeCoordinate = coord("E5"))

    // 2. Initialize Players (Current is WHITE, Next is BLACK)
    val currentPlayer = Player(
      name = PlayerName.WHITE,
      abbreviation = "W",
      piecesInReserve = mutableListOf(
        piece("WY", potential = false)
      ),
      capturedPieces = mutableListOf(
        piece("BD", potential = true),
        piece("BG", potential = false),
        piece("BZ", potential = true),
        piece("BG", potential = false)
      )
    )

    val nextPlayer = Player(
      name = PlayerName.BLACK,
      abbreviation = "B",
      piecesInReserve = mutableListOf(),
      capturedPieces = mutableListOf(
        piece("WG", potential = false),
        piece("WY", potential = true),
        piece("WP", potential = true)
      )
    )

    val playerWhoMadeTheLastMove = nextPlayer // BLACK
    var turn = 604

    val bitboard = convertBoardToBitboard(board)

    // 3. Initialize Game State
    val gameState = State(
      currentPlayer = currentPlayer,
      nextPlayer = nextPlayer,
      board = board,
      bitboard = bitboard,
      lines = constructLines(board.nodes)
    )

    // 4. Initialize result map mapping Node context to Collections SingletonSet/LinkedHashSet
    val b3Node = nodesList.first { it.coordinate == coord("B5") }
    val e8Node = nodesList.first { it.coordinate == coord("F7") }

    val result = linkedMapOf(
      b3Node to setOf(b3Node),
      e8Node to setOf(e8Node)
    )

    val eligiblePotentialMoves = org.example.engine.getEligiblePotentialMoves(gameState)



    val bitboardAvailableMoves = bitboard.identifyAvailableMoves(
	    currentPlayer = currentPlayer,
	    columnInfos = org.example.model.columnInfos
    )

    assert(bitboardAvailableMoves.size == eligiblePotentialMoves.size)
  }

  @Test
  fun `stuck in while loop 2`(){
    // 1. Re-create the 65 Nodes with the exact piece configurations of the dump
    val nodeDefinitions = listOf(
      // Column A
      NodeDef("A1", null, isDot = true, isSpot = false, ur = "B2"),
      NodeDef("A2", null, isDot = true, isSpot = false, ur = "B3", lr = "B2"),
      NodeDef("A3", null, isDot = true, isSpot = false, ur = "B4", lr = "B3"),
      NodeDef("A4", null, isDot = true, isSpot = false, ur = "B5", lr = "B4"),
      NodeDef("A5", null, isDot = true, isSpot = false, lr = "B5"),

      // Column B
      NodeDef("B1", null, isDot = true, isSpot = false, above = "B2", ur = "C2"),
      NodeDef("B2", "BY", isDot = false, isSpot = true, above = "B3", ur = "C3", lr = "C2", bitmask = 1UL),
      NodeDef("B3", "WZ", isDot = false, isSpot = true, above = "B4", below = "B2", ur = "C4", lr = "C3", bitmask = 2UL),
      NodeDef("B4", "BT", isDot = false, isSpot = true, above = "B5", below = "B3", ur = "C5", lr = "C4", bitmask = 4UL),
      NodeDef("B5", null, isDot = false, isSpot = true, below = "B4", ur = "C6", lr = "C5", bitmask = 8UL),
      NodeDef("B6", null, isDot = true, isSpot = false, below = "B5", lr = "C6"),

      // Column C
      NodeDef("C1", null, isDot = true, isSpot = false, above = "C2", ur = "D2"),
      NodeDef("C2", "BY", isDot = false, isSpot = true, above = "C3", ur = "D3", lr = "D2", ul = "B2", bitmask = 16UL),
      NodeDef("C3", "BP", isDot = false, isSpot = true, above = "C4", below = "C2", ur = "D4", lr = "D3", ul = "B3", ll = "B2", bitmask = 32UL),
      NodeDef("C4", null, isDot = false, isSpot = true, above = "C5", below = "C3", ur = "D5", lr = "D4", ul = "B4", ll = "B3", bitmask = 64UL),
      NodeDef("C5", null, isDot = false, isSpot = true, above = "C6", below = "C4", ur = "D6", lr = "D5", ul = "B5", ll = "B4", bitmask = 128UL),
      NodeDef("C6", null, isDot = false, isSpot = true, below = "C5", ur = "D7", lr = "D6", ll = "B5", bitmask = 256UL),
      NodeDef("C7", null, isDot = true, isSpot = false, below = "C6", lr = "D7"),

      // Column D
      NodeDef("D1", null, isDot = true, isSpot = false, above = "D2", ur = "E2"),
      NodeDef("D2", "WZ", isDot = false, isSpot = true, above = "D3", ur = "E3", lr = "E2", ul = "C2", bitmask = 512UL),
      NodeDef("D3", "BP", isDot = false, isSpot = true, above = "D4", below = "D2", ur = "E4", lr = "E3", ul = "C3", ll = "C2", bitmask = 1024UL),
      NodeDef("D4", "WT", isDot = false, isSpot = true, above = "D5", below = "D3", ur = "E5", lr = "E4", ul = "C4", ll = "C3", bitmask = 2048UL),
      NodeDef("D5", "WT", isDot = false, isSpot = true, above = "D6", below = "D4", ur = "E6", lr = "E5", ul = "C5", ll = "C4", bitmask = 4096UL),
      NodeDef("D6", "BT", isDot = false, isSpot = true, above = "D7", below = "D5", ur = "E7", lr = "E6", ul = "C6", ll = "C5", bitmask = 8192UL),
      NodeDef("D7", "WD", isDot = false, isSpot = true, below = "D6", ur = "E8", lr = "E7", ll = "C6", bitmask = 16384UL),
      NodeDef("D8", null, isDot = true, isSpot = false, below = "D7", lr = "E8"),

      // Column E
      NodeDef("E1", null, isDot = true, isSpot = false, above = "E2"),
      NodeDef("E2", "WT", isDot = false, isSpot = true, above = "E3", ur = "F2", ul = "D2", bitmask = 32768UL),
      NodeDef("E3", "BZ", isDot = false, isSpot = true, above = "E4", below = "E2", ur = "F3", lr = "F2", ul = "D3", ll = "D2", bitmask = 65536UL),
      NodeDef("E4", "WG", isDot = false, isSpot = true, potential = false, above = "E5", below = "E3", ur = "F4", lr = "F3", ul = "D4", ll = "D3", bitmask = 131072UL),
      NodeDef("E5", null, isDot = false, isSpot = true, isCenter = true, above = "E6", below = "E4", ur = "F5", lr = "F4", ul = "D5", ll = "D4", bitmask = 262144UL),
      NodeDef("E6", "BZ", isDot = false, isSpot = true, potential = false, above = "E7", below = "E5", ur = "F6", lr = "F5", ul = "D6", ll = "D5", bitmask = 524288UL),
      NodeDef("E7", null, isDot = false, isSpot = true, above = "E8", below = "E6", ur = "F7", lr = "F6", ul = "D7", ll = "D6", bitmask = 1048576UL),
      NodeDef("E8", "WP", isDot = false, isSpot = true, below = "E7", lr = "F7", ll = "D7", bitmask = 2097152UL),
      NodeDef("E9", null, isDot = true, isSpot = false, below = "E8"),

      // Column F
      NodeDef("F1", null, isDot = true, isSpot = false, above = "F2", ul = "E2"),
      NodeDef("F2", "WG", isDot = false, isSpot = true, potential = false, above = "F3", ur = "G2", ul = "E3", ll = "E2", bitmask = 4194304UL),
      NodeDef("F3", "BY", isDot = false, isSpot = true, above = "F4", ur = "G3", lr = "G2", ul = "E4", ll = "E3", bitmask = 8388608UL),
      NodeDef("F4", "BG", isDot = false, isSpot = true, potential = false, above = "F5", ur = "G4", lr = "G3", ul = "E5", ll = "E4", bitmask = 16777216UL),
      NodeDef("F5", "WY", isDot = false, isSpot = true, potential = false, above = "F6", ur = "G5", lr = "G4", ul = "E6", ll = "E5", bitmask = 33554432UL),
      NodeDef("F6", "BT", isDot = false, isSpot = true, above = "F7", ur = "G6", lr = "G5", ul = "E7", ll = "E6", bitmask = 67108864UL),
      NodeDef("F7", "BP", isDot = false, isSpot = true, below = "F6", lr = "G6", ul = "E8", ll = "E7", bitmask = 134217728UL),
      NodeDef("F8", null, isDot = true, isSpot = false, below = "F7", ll = "E8"),

      // Column G
      NodeDef("G1", null, isDot = true, isSpot = false, above = "G2", ul = "F2"),
      NodeDef("G2", "BD", isDot = false, isSpot = true, above = "G3", ur = "H2", ul = "F3", ll = "F2", bitmask = 268435456UL),
      NodeDef("G3", "WY", isDot = false, isSpot = true, potential = false, above = "G4", ur = "H3", lr = "H2", ul = "F4", ll = "F3", bitmask = 536870912UL),
      NodeDef("G4", "WP", isDot = false, isSpot = true, above = "G5", ur = "H4", lr = "H3", ul = "F5", ll = "F4", bitmask = 1073741824UL),
      NodeDef("G5", null, isDot = false, isSpot = true, above = "G6", ur = "H5", lr = "H4", ul = "F6", ll = "F5", bitmask = 2147483648UL),
      NodeDef("G6", null, isDot = false, isSpot = true, below = "G5", lr = "H5", ul = "F7", ll = "F6", bitmask = 4294967296UL),
      NodeDef("G7", null, isDot = true, isSpot = false, below = "G6", ll = "F7"),

      // Column H
      NodeDef("H1", null, isDot = true, isSpot = false, above = "H2", ul = "G2"),
      NodeDef("H2", "WY", isDot = false, isSpot = true, potential = false, above = "H3", ur = "I2", ul = "G3", ll = "G2", bitmask = 8589934592UL),
      NodeDef("H3", "BZ", isDot = false, isSpot = true, potential = false, above = "H4", ur = "I3", lr = "I2", ul = "G4", ll = "G3", bitmask = 17179869184UL),
      NodeDef("H4", null, isDot = false, isSpot = true, above = "H5", ur = "I4", lr = "I3", ul = "G5", ll = "G4", bitmask = 34359738368UL),
      NodeDef("H5", "WD", isDot = false, isSpot = true, below = "H4", lr = "I4", ul = "G6", ll = "G5", bitmask = 68719476736UL),
      NodeDef("H6", null, isDot = true, isSpot = false, below = "H5", ll = "G6"),

      // Column I
      NodeDef("I1", null, isDot = true, isSpot = false, above = "I2", ul = "H2"),
      NodeDef("I2", "BD", isDot = false, isSpot = true, above = "I3", ul = "H3", ll = "H2", bitmask = 137438953472UL),
      NodeDef("I3", "WZ", isDot = false, isSpot = true, above = "I4", ul = "H4", ll = "H3", bitmask = 274877906944UL),
      NodeDef("I4", "WD", isDot = false, isSpot = true, below = "I3", ul = "H5", ll = "H4", bitmask = 549755813888UL),
      NodeDef("I5", null, isDot = true, isSpot = false, below = "I4", ll = "H5"),

      // Column J
      NodeDef("J1", null, isDot = true, isSpot = false, ul = "I2"),
      NodeDef("J2", null, isDot = true, isSpot = false, ul = "I3", ll = "I2"),
      NodeDef("J3", null, isDot = true, isSpot = false, ul = "I4", ll = "I3"),
      NodeDef("J4", null, isDot = true, isSpot = false, ll = "I4")
    )

    val nodesList = LinkedHashSet(nodeDefinitions.map { it.toNode() })
    val board = Board(nodes = nodesList, centerNodeCoordinate = coord("E5"))

    // 2. Initialize Players (Current is WHITE, Next is BLACK)
    val currentPlayer = Player(
      name = PlayerName.WHITE,
      abbreviation = "W",
      piecesInReserve = mutableListOf(
        piece("WY", potential = false)
      ),
      capturedPieces = mutableListOf(
        piece("BD", potential = true),
        piece("BG", potential = false),
        piece("BZ", potential = true),
        piece("BG", potential = false)
      )
    )

    val nextPlayer = Player(
      name = PlayerName.BLACK,
      abbreviation = "B",
      piecesInReserve = mutableListOf(),
      capturedPieces = mutableListOf(
        piece("WG", potential = false),
        piece("WY", potential = true),
        piece("WP", potential = true)
      )
    )

    val playerWhoMadeTheLastMove = nextPlayer // BLACK
    var turn = 604

    val bitboard = convertBoardToBitboard(board)

    // 3. Initialize Game State
    val gameState = State(
      currentPlayer = currentPlayer,
      nextPlayer = nextPlayer,
      board = board,
      bitboard = bitboard,
      lines = constructLines(board.nodes)
    )

    // 4. Initialize result map mapping Node context to Collections SingletonSet/LinkedHashSet
    val b3Node = nodesList.first { it.coordinate == coord("B5") }
    val e8Node = nodesList.first { it.coordinate == coord("F7") }

    val result = linkedMapOf(
      b3Node to setOf(b3Node),
      e8Node to setOf(e8Node)
    )

    val eligiblePotentialMoves = org.example.engine.getEligiblePotentialMoves(gameState)


    val bitboardAvailableMoves = bitboard.identifyAvailableMoves(
      currentPlayer = currentPlayer,
      columnInfos = org.example.model.columnInfos
    )

    assert(bitboardAvailableMoves.size == eligiblePotentialMoves.size)

    val bestMove =
      selectMoveMCTS(
        bitboard = bitboard,
        currentPlayer = currentPlayer,
        nextPlayer = nextPlayer,
        turnPhase = TurnPhase.PlayerInputWindow,
        rounds = 0..999
      )
  }

  @Test
  fun `stuck in a while loop`(){
    // 1. Re-create the 65 Nodes with the exact piece configurations of the dump
    val nodeDefinitions = listOf(
      // Column A
      NodeDef("A1", null, isDot = true, isSpot = false, ur = "B2"),
      NodeDef("A2", null, isDot = true, isSpot = false, ur = "B3", lr = "B2"),
      NodeDef("A3", null, isDot = true, isSpot = false, ur = "B4", lr = "B3"),
      NodeDef("A4", null, isDot = true, isSpot = false, ur = "B5", lr = "B4"),
      NodeDef("A5", null, isDot = true, isSpot = false, lr = "B5"),

      // Column B
      NodeDef("B1", null, isDot = true, isSpot = false, above = "B2", ur = "C2"),
      NodeDef("B2", "WG", isDot = false, isSpot = true, potential = false, above = "B3", ur = "C3", lr = "C2", bitmask = 1UL),
      NodeDef("B3", "WT", isDot = false, isSpot = true, above = "B4", below = "B2", ur = "C4", lr = "C3", bitmask = 2UL),
      NodeDef("B4", null, isDot = false, isSpot = true, above = "B5", below = "B3", ur = "C5", lr = "C4", bitmask = 4UL),
      NodeDef("B5", "WT", isDot = false, isSpot = true, below = "B4", ur = "C6", lr = "C5", bitmask = 8UL),
      NodeDef("B6", null, isDot = true, isSpot = false, below = "B5", lr = "C6"),

      // Column C
      NodeDef("C1", null, isDot = true, isSpot = false, above = "C2", ur = "D2"),
      NodeDef("C2", "WP", isDot = false, isSpot = true, above = "C3", ur = "D3", lr = "D2", ul = "B2", bitmask = 16UL),
      NodeDef("C3", "BY", isDot = false, isSpot = true, potential = false, above = "C4", below = "C2", ur = "D4", lr = "D3", ul = "B3", ll = "B2", bitmask = 32UL),
      NodeDef("C4", "BY", isDot = false, isSpot = true, potential = false, above = "C5", below = "C3", ur = "D5", lr = "D4", ul = "B4", ll = "B3", bitmask = 64UL),
      NodeDef("C5", null, isDot = false, isSpot = true, above = "C6", below = "C4", ur = "D6", lr = "D5", ul = "B5", ll = "B4", bitmask = 128UL),
      NodeDef("C6", "BD", isDot = false, isSpot = true, below = "C5", ur = "D7", lr = "D6", ll = "B5", bitmask = 256UL),
      NodeDef("C7", null, isDot = true, isSpot = false, below = "C6", lr = "D7"),

      // Column D
      NodeDef("D1", null, isDot = true, isSpot = false, above = "D2", ur = "E2"),
      NodeDef("D2", null, isDot = false, isSpot = true, above = "D3", ur = "E3", lr = "E2", ul = "C2", bitmask = 512UL),
      NodeDef("D3", "BD", isDot = false, isSpot = true, above = "D4", below = "D2", ur = "E4", lr = "E3", ul = "C3", ll = "C2", bitmask = 1024UL),
      NodeDef("D4", "BZ", isDot = false, isSpot = true, above = "D5", below = "D3", ur = "E5", lr = "E4", ul = "C4", ll = "C3", bitmask = 2048UL),
      NodeDef("D5", null, isDot = false, isSpot = true, above = "D6", below = "D4", ur = "E6", lr = "E5", ul = "C5", ll = "C4", bitmask = 4096UL),
      NodeDef("D6", null, isDot = false, isSpot = true, above = "D7", below = "D5", ur = "E7", lr = "E6", ul = "C6", ll = "C5", bitmask = 8192UL),
      NodeDef("D7", null, isDot = false, isSpot = true, below = "D6", ur = "E8", lr = "E7", ll = "C6", bitmask = 16384UL),
      NodeDef("D8", null, isDot = true, isSpot = false, below = "D7", lr = "E8"),

      // Column E
      NodeDef("E1", null, isDot = true, isSpot = false, above = "E2"),
      NodeDef("E2", null, isDot = false, isSpot = true, above = "E3", ur = "F2", ul = "D2", bitmask = 32768UL),
      NodeDef("E3", "WG", isDot = false, isSpot = true, potential = false, above = "E4", below = "E2", ur = "F3", lr = "F2", ul = "D3", ll = "D2", bitmask = 65536UL),
      NodeDef("E4", "BZ", isDot = false, isSpot = true, potential = false, above = "E5", below = "E3", ur = "F4", lr = "F3", ul = "D4", ll = "D3", bitmask = 131072UL),
      NodeDef("E5", "WP", isDot = false, isSpot = true, isCenter = true, above = "E6", below = "E4", ur = "F5", lr = "F4", ul = "D5", ll = "D4", bitmask = 262144UL),
      NodeDef("E6", null, isDot = false, isSpot = true, above = "E7", below = "E5", ur = "F6", lr = "F5", ul = "D6", ll = "D5", bitmask = 524288UL),
      NodeDef("E7", null, isDot = false, isSpot = true, above = "E8", below = "E6", ur = "F7", lr = "F6", ul = "D7", ll = "D6", bitmask = 1048576UL),
      NodeDef("E8", "BG", isDot = false, isSpot = true, potential = false, below = "E7", lr = "F7", ll = "D7", bitmask = 2097152UL),
      NodeDef("E9", null, isDot = true, isSpot = false, below = "E8"),

      // Column F
      NodeDef("F1", null, isDot = true, isSpot = false, above = "F2", ul = "E2"),
      NodeDef("F2", "BP", isDot = false, isSpot = true, above = "F3", ur = "G2", ul = "E3", ll = "E2", bitmask = 4194304UL),
      NodeDef("F3", "WG", isDot = false, isSpot = true, potential = false, above = "F4", ur = "G3", lr = "G2", ul = "E4", ll = "E3", bitmask = 8388608UL),
      NodeDef("F4", "WP", isDot = false, isSpot = true, above = "F5", ur = "G4", lr = "G3", ul = "E5", ll = "E4", bitmask = 16777216UL),
      NodeDef("F5", "WZ", isDot = false, isSpot = true, above = "F6", ur = "G5", lr = "G4", ul = "E6", ll = "E5", bitmask = 33554432UL),
      NodeDef("F6", "BT", isDot = false, isSpot = true, above = "F7", ur = "G6", lr = "G5", ul = "E7", ll = "E6", bitmask = 67108864UL),
      NodeDef("F7", "WT", isDot = false, isSpot = true, below = "F6", lr = "G6", ul = "E8", ll = "E7", bitmask = 134217728UL),
      NodeDef("F8", null, isDot = true, isSpot = false, below = "F7", ll = "E8"),

      // Column G
      NodeDef("G1", null, isDot = true, isSpot = false, above = "G2", ul = "F2"),
      NodeDef("G2", "BZ", isDot = false, isSpot = true, potential = false, above = "G3", ur = "H2", ul = "F3", ll = "F2", bitmask = 268435456UL),
      NodeDef("G3", "BT", isDot = false, isSpot = true, above = "G4", ur = "H3", lr = "H2", ul = "F4", ll = "F3", bitmask = 536870912UL),
      NodeDef("G4", "WY", isDot = false, isSpot = true, potential = false, above = "G5", ur = "H4", lr = "H3", ul = "F5", ll = "F4", bitmask = 1073741824UL),
      NodeDef("G5", "BP", isDot = false, isSpot = true, above = "G6", ur = "H5", lr = "H4", ul = "F6", ll = "F5", bitmask = 2147483648UL),
      NodeDef("G6", "WZ", isDot = false, isSpot = true, potential = false, below = "G5", lr = "H5", ul = "F7", ll = "F6", bitmask = 4294967296UL),
      NodeDef("G7", null, isDot = true, isSpot = false, below = "G6", ll = "F7"),

      // Column H
      NodeDef("H1", null, isDot = true, isSpot = false, above = "H2", ul = "G2"),
      NodeDef("H2", "WY", isDot = false, isSpot = true, above = "H3", ur = "I2", ul = "G3", ll = "G2", bitmask = 8589934592UL),
      NodeDef("H3", null, isDot = false, isSpot = true, above = "H4", ur = "I3", lr = "I2", ul = "G4", ll = "G3", bitmask = 17179869184UL),
      NodeDef("H4", "BG", isDot = false, isSpot = true, potential = false, above = "H5", ur = "I4", lr = "I3", ul = "G5", ll = "G4", bitmask = 34359738368UL),
      NodeDef("H5", "BT", isDot = false, isSpot = true, below = "H4", lr = "I4", ul = "G6", ll = "G5", bitmask = 68719476736UL),
      NodeDef("H6", null, isDot = true, isSpot = false, below = "H5", ll = "G6"),

      // Column I
      NodeDef("I1", null, isDot = true, isSpot = false, above = "I2", ul = "H2"),
      NodeDef("I2", "BY", isDot = false, isSpot = true, potential = false, above = "I3", ul = "H3", ll = "H2", bitmask = 137438953472UL),
      NodeDef("I3", null, isDot = false, isSpot = true, above = "I4", ul = "H4", ll = "H3", bitmask = 274877906944UL),
      NodeDef("I4", "WD", isDot = false, isSpot = true, below = "I3", ul = "H5", ll = "H4", bitmask = 549755813888UL),
      NodeDef("I5", null, isDot = true, isSpot = false, below = "I4", ll = "H5"),

      // Column J
      NodeDef("J1", null, isDot = true, isSpot = false, ul = "I2"),
      NodeDef("J2", null, isDot = true, isSpot = false, ul = "I3", ll = "I2"),
      NodeDef("J3", null, isDot = true, isSpot = false, ul = "I4", ll = "I3"),
      NodeDef("J4", null, isDot = true, isSpot = false, ll = "I4")
    )

    val nodesList = LinkedHashSet(nodeDefinitions.map { it.toNode() })
    val board = Board(nodes = nodesList, centerNodeCoordinate = coord("E5"))
    val savedBoardState = Board(nodes = LinkedHashSet(nodesList), centerNodeCoordinate = coord("E5"))

// 2. Initialize Players (Current is WHITE, Next is BLACK)
    val currentPlayer = Player(
      name = PlayerName.WHITE,
      abbreviation = "W",
      piecesInReserve = mutableListOf(
        piece("WY", potential = false)
      ),
      capturedPieces = mutableListOf(
        piece("BD", potential = true),
        piece("BY", potential = true),
        piece("BG", potential = false),
        piece("BZ", potential = true),
        piece("BP", potential = true)
      )
    )

    val nextPlayer = Player(
      name = PlayerName.BLACK,
      abbreviation = "B",
      piecesInReserve = mutableListOf(
        piece("BY", potential = false)
      ),
      capturedPieces = mutableListOf(
        piece("WD", potential = true),
        piece("WY", potential = true),
        piece("WD", potential = true),
        piece("WZ", potential = true),
        piece("WZ", potential = false)
      )
    )

    val playerWhoMadeTheLastMove = nextPlayer // BLACK

// 5. Initialize Bitboard
    val bitboard = Bitboard(
      whiteGIPF = 8454145UL,
      whiteDVONNLayer = ulongArrayOf(549755813888UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      whitePUNCTLayer = ulongArrayOf(17039376UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      whiteTAMSK = 134217738UL,
      whiteYINSH = 9663676416UL,
      whiteZERTZ = 4328521728UL,
      whitePotentials = 558530560026UL,
      blackGIPF = 34361835520UL,
      blackDVONNLayer = ulongArrayOf(1280UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      blackPUNCTLayer = ulongArrayOf(2151677952UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
      blackTAMSK = 69323456512UL,
      blackYINSH = 137438953568UL,
      blackZERTZ = 268568576UL,
      blackPotentials = 71475137792UL,
//      globalOccupancy = 807452216699UL,
//      whitePieces = 563907723291UL,
//      blackPieces = 243544493408UL,
//      whiteNeutralized = 0UL,
//      blackNeutralized = 0UL
    )

    // 3. Initialize State
    val state = State(
      currentPlayer = currentPlayer,
      nextPlayer = nextPlayer,
      board = board,
      bitboard = bitboard,
      lines = constructLines(board.nodes)
    )

    val eligiblePotentialMoves = org.example.engine.getEligiblePotentialMoves(state)

    val bitboardAvailableMoves = bitboard.identifyAvailableMoves(
      currentPlayer = currentPlayer,
      columnInfos = org.example.model.columnInfos
    )

    assert(bitboardAvailableMoves.size == eligiblePotentialMoves.size)

    val bestMove =
      selectMoveMCTS(
        bitboard = bitboard,
        currentPlayer = currentPlayer,
        nextPlayer = nextPlayer,
        turnPhase = TurnPhase.PlayerInputWindow,
        rounds = 0..999
      )
  }

  @Test
  fun `encodeBitboard`() {
    val whiteGIPF = 8454145UL

    val array = whiteGIPF.toString(2).padStart(40, '0').encodeToByteArray().map {
      it.mod(48.toByte())
    }


    val ver = whiteGIPF.toBitList()

    println(whiteGIPF.toString(2).padStart(40, '0'))
    println(ver)
    println(array)

    val ndArray =
      mk.zeros<Int>(47, 40)

    ndArray.set(0, whiteGIPF.toBitList())

    println("ndArray:")
    println(ndArray)

  }
}
