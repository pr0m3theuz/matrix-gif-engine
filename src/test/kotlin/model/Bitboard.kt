@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import kotlin.test.assertEquals
import kotlin.text.toString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.ai.humanEvaluation.BestBitMove
import org.example.engine.MoveType
import org.example.engine.PossibleBitMove
import org.example.model.Bitboard
import org.example.model.Board
import org.example.model.Coordinate
import org.example.model.LETTERS
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
import org.example.model.createPlayerPiecesWithPotentialPowerset
import org.example.model.createRetrieveAndCapturePiecesList
import org.example.model.evaluateLinesForFourInARow
import org.example.model.getsSelectedPiecesWithPotentialPowerset
import org.example.model.identifyPlayerPiecesWithPotential
import org.example.model.populateNeighbors
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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

	  val blackBoardPieces = convertedBitboardBoard.nodes.filter { node -> node.piece?.colorName == PlayerName.BLACK.name }
	  val whiteBoardPieces = convertedBitboardBoard.nodes.filter { node -> node.piece?.colorName == PlayerName.WHITE.name }

	  println(Json.encodeToString(blackBoardPieces))
	  println(Json.encodeToString(whiteBoardPieces))

    val state =
	    State(
		    currentPlayer = currentPlayer,
		    nextPlayer = nextPlayer,
		    board = convertedBitboardBoard,
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
        testBitboard.createPlayerPiecesWithPotentialPowerset(
            testBitboard.identifyPlayerPiecesWithPotential(
                listOf(linesWithFourInARow.first()),
                currentPlayer,
            )
        )

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
}
