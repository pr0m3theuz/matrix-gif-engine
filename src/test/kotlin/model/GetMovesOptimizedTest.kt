@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import kotlin.math.abs
import kotlin.test.assertTrue
import org.example.ai.mcts.PackedMove
import org.example.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

class GetMovesOptimizedTest {

  val rays =
      ulongArrayOf(
          70900812351UL,
          4366865519UL,
          412231887UL,
          9145962895UL,
          585206237169UL,
          70905040371UL,
          4639562230UL,
          9279123964UL,
          155728765432UL,
          292603166225UL,
          585210494514UL,
          71177764453UL,
          13506477770UL,
          155861942148UL,
          311455416072UL,
          146305679889UL,
          292611393058UL,
          585487060052UL,
          80048265385UL,
          160092369218UL,
          311590641796UL,
          622908653832UL,
          146561664034UL,
          293127522884UL,
          594580739224UL,
          226824562977UL,
          315945930818UL,
          623037653124UL,
          154363136068UL,
          308994708104UL,
          747106870544UL,
          386380613665UL,
          627000741954UL,
          267114549384UL,
          542819033856UL,
          956789048336UL,
          685217417249UL,
          989222969600UL,
          1016373264896UL,
          1070673822736UL,
      )

  //    get() {
  //      val rays = mutableMapOf<ULong, ULong>()
  //      columnInfos
  //          .flatMap { it.positions }
  //          .forEach { position ->
  //            columnInfos.forEach { info ->
  //              rays[position] =
  //                  rays.getOrDefault(position, 0UL) or
  //                      if (info.columnMask and position != 0UL) info.columnMask else 0UL
  //            }
  //          }
  //      return rays
  //    }

  fun getZertzJumpsInDirection(
      potentials: ULong,
      occupied: ULong,
      emptySquares: ULong,
      shift: (ULong) -> ULong = { 0UL },
  ): ULong {
    // 1. MUST JUMP OVER AT LEAST 1 PIECE
    // Shift the potential(s) 1 space. It must land on an occupied space to start a jump.
    var jumps = shift(potentials) and occupied
    var prop = occupied

    logger.info { "1. MUST JUMP OVER AT LEAST 1 PIECE" }
    logger.info { "jumps ${jumps.toString(2)}" }
    logger.info { "prop ${prop.toString(2)}" }

    // 2. KOGGE-STONE PROPAGATION
    // Smear the 'jumps' forward through contiguous occupied squares.

    // Iteration 1 (smear by 1)
    jumps = jumps or (shift(jumps) and prop)
    prop = prop and shift(prop)

    logger.info { "Iteration 1 (smear by 1)" }
    logger.info { "jumps ${jumps.toString(2)}" }
    logger.info { "prop ${prop.toString(2)}" }

    // Iteration 2 (smear by 2)
    var tempJumps = shift(shift(jumps))
    var tempProp = shift(shift(prop))
    jumps = jumps or (tempJumps and prop)
    prop = prop and tempProp

    logger.info { "Iteration 2 (smear by 2)" }
    logger.info { "jumps ${jumps.toString(2)}" }
    logger.info { "prop ${prop.toString(2)}" }

    // Iteration 3 (smear by 4)
    // A ZÈRTZ board maxes out around 7 spaces across. 1 + 1 + 2 + 4 = 8.
    // This is mathematically guaranteed to reach the end of any valid line.
    tempJumps = shift(shift(shift(shift(jumps))))
    jumps = jumps or (tempJumps and prop)

    logger.info { "Iteration 3 (smear by 4)" }
    logger.info { "jumps ${jumps.toString(2)}" }
    logger.info { "prop ${prop.toString(2)}" }

    // 3. LAND ON FIRST EMPTY SQUARE
    // Shift the smeared jumps one final time. The landing square MUST be empty.
    logger.info { "LAND ON FIRST EMPTY SQUARE" }
    logger.info { "lands on ${(shift(jumps) and emptySquares).toString(2)}" }
    return shift(jumps) and emptySquares
  }

  fun getZertzTargets(
      boardMask: ULong,
      potentials: ULong,
      occupied: ULong,
      emptySquares: ULong,
      movesBuffer: MutableList<PackedMove>,
  ) {
    //    val sourceTargets: MutableList<Pair<ULong, ULong>> = mutableListOf()

    var tempPotentials = potentials
    while (tempPotentials != 0UL) {
      val potential = 1UL shl tempPotentials.countTrailingZeroBits()
      val potentialBitPosition = tempPotentials.countTrailingZeroBits()
      val potentialMask = rays[potentialBitPosition]
      val adjacentBits = clusterArray[potentialBitPosition]

      val targets = emptySquares and potentialMask and adjacentBits.inv()

      var tempAdjBits = adjacentBits
      var tempTargets = targets
      while (tempAdjBits != 0UL) {
        val adjacentBitPosition = tempAdjBits.countTrailingZeroBits()
        val adjacentBit = tempAdjBits.takeLowestOneBit()

        val distanceBetweenBits = abs(potentialBitPosition - adjacentBitPosition)

        val jumpOne =
            if (adjacentBitPosition < potentialBitPosition) {
              (potential shr distanceBetweenBits) and occupied != 0UL
            } else {
              (potential shl distanceBetweenBits) and occupied != 0UL
            }

        // for vertical rays
        val upperBound =
            (openningSpotsLineMask and (adjacentBit - 1UL).inv() and boardMask).takeLowestOneBit()
        val lowerBound =
            (openningSpotsLineMask and (adjacentBit - 1UL) and boardMask).takeHighestOneBit()

        val inBoundBits = ((upperBound shl 1) - 1UL) and (lowerBound - 1UL).inv()

        val validTargets =
            if (abs(adjacentBitPosition - potentialBitPosition) == 1) {
              // lower bound < tempTargets < upper bound & not and adjacent bit
              tempTargets and inBoundBits
            } else if (adjacentBitPosition < potentialBitPosition) {
              tempTargets and adjacentBit - 1UL
            } else {
              tempTargets and
                  (adjacentBit or (adjacentBit - 1UL).inv()) and
                  rays[adjacentBitPosition]
            }

        val tempValidTargets = validTargets // and (adjacentBits.inv())
        if (jumpOne && tempValidTargets != 0UL) {
          val target =
              if (adjacentBitPosition < potentialBitPosition) {
                validTargets.takeHighestOneBit()
              } else {
                validTargets.takeLowestOneBit()
              }
          //          sourceTargets.add(Pair(potential, target))
          movesBuffer.add(
              PackedMove.Single(
                  0u.packPossibleBitMove(
                          sourceBit = potential,
                          targetBit = target,
                          moveType = MoveType.UsePotential,
                      )
                      .setPieceType(
                          pieceType = PieceType.ZERTZ,
                      )
                      .setPieceColor(
                          pieceColor = PlayerName.BLACK,
                      )
                      .setPotential(potential = true)
              )
          )

          tempTargets = tempTargets xor target
        }
        tempAdjBits = tempAdjBits xor adjacentBit
      }

      tempPotentials = tempPotentials xor potential
    }

    //    return sourceTargets
  }

  fun getYinshTargets(
    boardMask: ULong,
    potentials: ULong,
    occupied: ULong,
    emptySquares: ULong,
    movesBuffer: MutableList<PackedMove>,
  ) {
    //    val sourceTargets: MutableList<Pair<ULong, ULong>> = mutableListOf()

    var tempPotentials = potentials
    while (tempPotentials != 0UL) {
      val potential = 1UL shl tempPotentials.countTrailingZeroBits()
      val potentialBitPosition = tempPotentials.countTrailingZeroBits()
      val potentialMask = rays[potentialBitPosition]
      val adjacentBits = clusterArray[potentialBitPosition]

      val targets = (emptySquares and potentialMask) or (emptySquares and adjacentBits)

      var tempAdjBits = adjacentBits
      var tempTargets = targets

      while (tempAdjBits != 0UL) {
        val adjacentBitPosition = tempAdjBits.countTrailingZeroBits()
        val adjacentBit = tempAdjBits.takeLowestOneBit()

        val distanceBetweenBits = abs(potentialBitPosition - adjacentBitPosition)

        val emptyAdjacent =
          if (adjacentBitPosition < potentialBitPosition) {
            (potential shr distanceBetweenBits) and occupied == 0UL
          } else {
            (potential shl distanceBetweenBits) and occupied == 0UL
          }

        if (!emptyAdjacent) continue

        // for vertical rays
//        val upperBound =
//          (openningSpotsLineMask and (adjacentBit - 1UL).inv() and boardMask).takeLowestOneBit()
//        val lowerBound =
//          (openningSpotsLineMask and (adjacentBit - 1UL) and boardMask).takeHighestOneBit()

//        val inBoundBits = ((upperBound shl 1) - 1UL) and (lowerBound - 1UL).inv()

        val validTargets =
//          if (abs(adjacentBitPosition - potentialBitPosition) == 1) {
//            // lower bound < tempTargets < upper bound & not and adjacent bit
//            tempTargets and inBoundBits
//          } else
            if (adjacentBitPosition < potentialBitPosition) {
            tempTargets and adjacentBit - 1UL and rays[adjacentBitPosition] and adjacentBits.inv()
          } else {
            tempTargets and
                (adjacentBit or (adjacentBit - 1UL).inv()) and
                rays[adjacentBitPosition] and adjacentBits.inv()
          }

        // todo try tempTargets and rays[adjacentBitPosition]

        val tempValidTargets = validTargets // and (adjacentBits.inv())
        if (tempValidTargets != 0UL) {
          val target =
            if (adjacentBitPosition < potentialBitPosition) {
              validTargets.takeHighestOneBit()
            } else {
              validTargets.takeLowestOneBit()
            }
          //          sourceTargets.add(Pair(potential, target))
          movesBuffer.add(
            PackedMove.Single(
              0u.packPossibleBitMove(
                sourceBit = potential,
                targetBit = target,
                moveType = MoveType.UsePotential,
              )
                .setPieceType(
                  pieceType = PieceType.YINSH,
                )
                .setPieceColor(
                  pieceColor = PlayerName.WHITE,
                )
                .setPotential(potential = true)
            )
          )

          tempTargets = tempTargets xor target
        }

        movesBuffer.add(
          PackedMove.Single(
            0u.packPossibleBitMove(
              sourceBit = potential,
              targetBit = adjacentBit,
              moveType = MoveType.UsePotential,
            )
              .setPieceType(
                pieceType = PieceType.YINSH,
              )
              .setPieceColor(
                pieceColor = PlayerName.WHITE,
              )
              .setPotential(potential = true)
          )
        )

        tempAdjBits = tempAdjBits xor adjacentBit
      }

      tempPotentials = tempPotentials xor potential
    }

    //    return sourceTargets
  }


  val shiftForward: (ULong) -> ULong = { it shl 1 }

  @Test
  fun getTamskMoves() {
    val bitboard =
        Bitboard(
            whiteGIPF = 551970406400UL,
            whiteDVONNLayer = ulongArrayOf(8388612UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(4194304UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 4295229568UL,
            whiteYINSH = 2576UL,
            whiteZERTZ = 274877939713UL,
            whitePotentials = 12616341UL,
            blackGIPF = 1048648UL,
            blackDVONNLayer = ulongArrayOf(148480UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(524288UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 42949672962UL,
            blackYINSH = 65536UL,
            blackZERTZ = 137457827840UL,
            blackPotentials = 180391461890UL,
        )

    val currentPlayer =
        Player(
            name = PlayerName.WHITE,
        )
    val movesBuffer: MutableList<PackedMove> = mutableListOf()
    bitboard.getTamskMoves(currentPlayer, movesBuffer)

    assertTrue(movesBuffer.isEmpty())
  }

  @Test
  fun getZertzMoves() {
    val bitboard =
        Bitboard(
            whiteGIPF = 542720UL,
            whiteDVONNLayer = ulongArrayOf(4378853376u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whitePUNCTLayer = ulongArrayOf(2097282u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            whiteTAMSK = 137443148288UL,
            whiteYINSH = 17448304896UL,
            whiteZERTZ = 551903559680UL,
            whitePotentials = 711175963522UL,
            blackGIPF = 1073751040UL,
            blackDVONNLayer = ulongArrayOf(9437188u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackPUNCTLayer = ulongArrayOf(65576u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
            blackTAMSK = 42949672976UL,
            blackYINSH = 671088641UL,
            blackZERTZ = 274911494144UL,
            blackPotentials = 318541758525UL,
        )

    val currrentPlayer = Player(name = PlayerName.BLACK)

    val movesBuffer = mutableListOf<PackedMove>()
    bitboard.getZertzMoves(
        currrentPlayer,
        columnInfos,
        movesBuffer,
    )

    val expectedPiece =
        Piece(
            abbreviation = "BZ",
            potential = true,
            colorName = PlayerName.BLACK,
            type = PieceType.ZERTZ,
            isNeutralized = false,
        )

    val expectedMoves =
        listOf<PossibleBitMove>(
            PossibleBitMove(
                piece = expectedPiece,
                sourceBit = 32768UL,
                targetBit = 131072UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.BLACK,
                moveType = MoveType.UsePotential,
            ),
            PossibleBitMove(
                piece = expectedPiece,
                sourceBit = 33554432UL,
                targetBit = 68719476736UL,
                pieceType = PieceType.ZERTZ,
                pieceColor = PlayerName.BLACK,
                moveType = MoveType.UsePotential,
            ),
        )

    assertEquals(
        expectedMoves.first(),
        (movesBuffer.first() as PackedMove.Single).value.toPossibleBitMove(),
    )

    logger.info { movesBuffer }
  }

  @Test
  fun getZertzMovesTwo() {
    repeat(100000) {
      val bitboard =
          Bitboard(
              whiteGIPF = 893352538030.toULong(),
              blackZERTZ = 274911494144.toULong(),
              blackPotentials = 274911494144.toULong(),
          )

      val currrentPlayer = Player(name = PlayerName.BLACK)

      val movesBuffer = mutableListOf<PackedMove>()
      bitboard.getZertzMoves(
          currrentPlayer,
          columnInfos,
          movesBuffer,
      )

      val expectedPiece =
          Piece(
              abbreviation = "BZ",
              potential = true,
              colorName = PlayerName.BLACK,
              type = PieceType.ZERTZ,
              isNeutralized = false,
          )

      /*      val expectedMoves =
      mutableListOf<PossibleBitMove>(
        PossibleBitMove(
          piece = expectedPiece,
          sourceBit = 32768UL,
          targetBit = 16UL,
          pieceType = PieceType.ZERTZ,
          pieceColor = PlayerName.BLACK,
          moveType = MoveType.UsePotential,
        ),
        PossibleBitMove(
          piece = expectedPiece,
          sourceBit = 32768UL,
          targetBit = 131072UL,
          pieceType = PieceType.ZERTZ,
          pieceColor = PlayerName.BLACK,
          moveType = MoveType.UsePotential,
        ),
        PossibleBitMove(
          piece = expectedPiece,
          sourceBit = 32768UL,
          targetBit = 137438953472UL,
          pieceType = PieceType.ZERTZ,
          pieceColor = PlayerName.BLACK,
          moveType = MoveType.UsePotential,
        ),
        PossibleBitMove(
          piece = expectedPiece,
          sourceBit = 33554432UL,
          targetBit = 1UL,
          pieceType = PieceType.ZERTZ,
          pieceColor = PlayerName.BLACK,
          moveType = MoveType.UsePotential,
        ),
        PossibleBitMove(
          piece = expectedPiece,
          sourceBit = 33554432UL,
          targetBit = 68719476736UL,
          pieceType = PieceType.ZERTZ,
          pieceColor = PlayerName.BLACK,
          moveType = MoveType.UsePotential,
        ),
        PossibleBitMove(
          piece = expectedPiece,
          sourceBit = 33554432UL,
          targetBit = 137438953472UL,
          pieceType = PieceType.ZERTZ,
          pieceColor = PlayerName.BLACK,
          moveType = MoveType.UsePotential,
        ),
      )*/

      //    val sortedMoves =
      //        movesBuffer.map { (it as PackedMove.Single).value.toPossibleBitMove()
      // }.toMutableList()
      //
      //    sortedMoves.sortWith(compareBy<PossibleBitMove> { it.sourceBit }.thenBy { it.targetBit
      // })
      //
      //    assertEquals(
      //        expectedMoves.first(),
      //        sortedMoves.first(),
      //    )
      //    assertEquals(
      //        expectedMoves.last(),
      //        sortedMoves.last(),
      //    )
      //
      //    assertEquals(
      //        expectedMoves.size,
      //        movesBuffer.size,
      //    )
      //
      //    logger.info { movesBuffer }
    }
  }

  @Test
  fun `test simple jump over exactly one piece`() {
    // Visual Line: [Empty] [Occupied] [Potential]
    // Bit Index:      2         1          0
    val potentials = 0b001UL
    val occupied = 0b011UL // Bits 0 and 1 are occupied
    val empty = 0b100UL // Bit 2 is empty

    val landing = getZertzJumpsInDirection(potentials, occupied, empty, shiftForward)

    // Must land exactly on the empty square (bit 2)
    assertEquals(0b100UL, landing, "Should jump over one piece to the empty space")
  }

  @Test
  fun `test processes multiple potentials simultaneously`() {
    // We have TWO potentials on the board in different places.
    // Line: [E] [O] [P2] [E] [O] [O] [P1]
    // Idx:   6   5   4    3   2   1   0
    val potentials = 0b0010001UL // P2 is bit 4, P1 is bit 0
    val occupied = 0b0110011UL
    val empty = 0b1001100UL // Bits 3 and 6 are empty

    val landing = getZertzJumpsInDirection(potentials, occupied, empty, shiftForward)

    // P1 should jump over bits 1,2 and land on 3.
    // P2 should jump over bit 5 and land on 6.
    // Expected landing bits: 6 and 3 (0b1001000)
    assertEquals(0b1000100UL, landing, "Should calculate jumps for multiple pieces at once")
  }

  @Test
  fun `getZertzMoves using bit tricks`() {
    repeat(100000) {
      val bitboard =
          Bitboard(
              blackZERTZ = 274911494144UL,
              blackPotentials = 318541758525UL,
          )

      val currrentPlayer = Player(name = PlayerName.BLACK)

      val movesBuffer = mutableListOf<PackedMove>()
      getZertzTargets(
          boardMask = 1UL.shl(40).minus(1UL),
          potentials = 274911494144.toULong(),
          occupied = 893352538030.toULong(),
          emptySquares = (893352538030.toULong().inv() and 1UL.shl(40).minus(1UL)),
          movesBuffer = movesBuffer,
      )

      val expectedSourceTargets =
          "[(32768, 16), (32768, 131072), (32768, 137438953472), (33554432, 1), (33554432, 137438953472), (33554432, 68719476736)]"

//      val results = movesBuffer.map { (it as PackedMove.Single).value.extractSourceBit() to it.value.extractTargetBit() }.toString()

//      movesBuffer.associateBy { (it as PackedMove.Single).value.extractSourceBit() to it.value.extractTargetBit() }
//            assertEquals(expectedSourceTargets, results)
    }
  }

  @Test
  fun getYinshMoves() {
    val bitboard = Bitboard(
        whiteGIPF = 66080u,
        whiteDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
        whitePUNCTLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
        whiteTAMSK = 4194304u,
        whiteYINSH = 16384u,
        whiteZERTZ = 0u,
        whitePotentials = 4210688u,
        blackGIPF = 554050813952u,
        blackDVONNLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
        blackPUNCTLayer = ulongArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u),
        blackTAMSK = 0u,
        blackYINSH = 5u,
        blackZERTZ = 0u,
        blackPotentials = 0u,
    )

    val copy = bitboard.deepCopy()

    val move = 318490376u

    copy.usePiecePotential(move)
    print(move)
  }

  @Test
  fun `getYinshMoves using bit tricks`() {
    val bitboard = Bitboard(
      whiteGIPF = 1044086047519.toULong(),
      whiteYINSH = 262144UL,
      whitePotentials = 262144UL,
    )

    val copy = bitboard.deepCopy()

    val movesBuffer = mutableListOf<PackedMove>()
    val boardMask = 1UL.shl(40).minus(1UL)

    getYinshTargets(
      boardMask,
      potentials = 262144.toULong(),
      occupied = bitboard.globalOccupancy,
      emptySquares = (bitboard.globalOccupancy.inv() and 1UL.shl(40).minus(1UL)),
      movesBuffer = movesBuffer,
    )

    val move = 318490376u

    copy.usePiecePotential(move)
    print(move)
  }

  @Test fun getDvonnMoves() {}

  @Test fun getPunctMoves() {}

  @Test fun identifyAvailableMoves() {}

  @Test fun identifyPiecesToRemove() {}

  @Test
  fun `get DVONN Moves when Blocked`() {
    /**
     * TODO The DVONN potential has the ability to jump atop any of the opponent's DVONN pieces
     * (individual pieces or stacks) as long as the both DVONN pieces are on the same line and there
     * are no other pieces in between them.
     */
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
    val initBitboard =
        Bitboard(
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
            piecesInReserve = mutableListOf(piece("WY", potential = false).pack()),
        )

    val opponentPlayer =
        Player(
            name = PlayerName.BLACK,
            piecesInReserve = mutableListOf(),
        )

    val dvonnMoves = mutableListOf<PackedMove>()
    bitboard.getDvonnMoves(
        player = currentPlayer,
        columnInfos = columnInfos,
        dvonnMoves,
    )

    Assertions.assertTrue(dvonnMoves.isEmpty())
  }

  @Test
  fun `piece removal intersecting basics`() {
    val bitboard: Bitboard = Bitboard(
      whiteGIPF = 33311UL,
    )

    val whitePlayer = Player(
      name = PlayerName.WHITE,
    )

    val moves = mutableListOf<PackedMove>()
    bitboard.identifyPiecesToRemove(whitePlayer, moves)

    println(moves)

    val expected = listOf<PackedMove>(
      PackedMove.Multiple(values = listOf(
        16646016u, 16646017u, 16646018u, 16646019u
      )),
      PackedMove.Multiple(values = listOf(
        16646016u, 16646020u, 16646025u, 16646031u
      ))
    )

    assertEquals(expected, moves)
  }

  @Test
  fun `piece removal intersecting potential`() {
    val bitboard: Bitboard = Bitboard(
      whiteGIPF = 33310UL, // 32794UL
      whiteTAMSK = 1UL, // 517ul
      whitePotentials = 1UL // 517ul
    )

    val whitePlayer = Player(
      name = PlayerName.WHITE,
    )

    val moves = mutableListOf<PackedMove>()
    bitboard.identifyPiecesToRemove(whitePlayer, moves)

    println(moves)

    val expected = listOf<PackedMove>(
      PackedMove.Multiple(values = listOf(
        16646016u, 16646017u, 16646018u, 16646019u, 16646020u, 16646025u, 16646031u
      )),
      PackedMove.Multiple(values = listOf(
        16646017u, 16646018u, 16646019u, 16646020u, 16646025u, 16646031u
      )),
      PackedMove.Multiple(values = listOf(
        16646016u, 16646017u, 16646018u, 16646019u
      )),
      PackedMove.Multiple(values = listOf(
        16646016u, 16646020u, 16646025u, 16646031u
      )),
      PackedMove.Multiple(values = listOf(
        16646017u, 16646018u, 16646019u
      )),
      PackedMove.Multiple(values = listOf(
        16646020u, 16646025u, 16646031u
      ))
    )

    assertEquals(expected, moves)
  }

  @Test
  fun `multiple potentials and piece removal intersecting potential`() {
    val bitboard: Bitboard = Bitboard(
      whiteGIPF = 32794UL, // 32794UL
      whiteTAMSK = 517UL, // 517UL
      whitePotentials = 517UL // 517UL
    )

    val whitePlayer = Player(
      name = PlayerName.WHITE,
    )

    val moves = mutableListOf<PackedMove>()
    bitboard.identifyPiecesToRemove(whitePlayer, moves)

    println(moves)

    val expected = listOf<PackedMove>(
      PackedMove.Multiple(listOf(16646016u, 16646017u, 16646019u, 16646020u, 16646031u)),
      PackedMove.Multiple(listOf(16646016u, 16646018u, 16646017u, 16646019u, 16646020u, 16646031u)),
      PackedMove.Multiple(listOf(16646016u, 16646025u, 16646017u, 16646019u, 16646020u, 16646031u)),
      PackedMove.Multiple(listOf(16646016u, 16646018u, 16646025u, 16646017u, 16646019u, 16646020u, 16646031u)),
      PackedMove.Multiple(listOf(16646017u, 16646019u, 16646020u, 16646031u)),
      PackedMove.Multiple(listOf(16646018u, 16646017u, 16646019u, 16646020u, 16646031u)),
      PackedMove.Multiple(listOf(16646025u, 16646017u, 16646019u, 16646020u, 16646031u)),
      PackedMove.Multiple(listOf(16646018u, 16646025u, 16646017u, 16646019u, 16646020u, 16646031u)),
      PackedMove.Multiple(listOf(16646016u, 16646018u, 16646017u, 16646019u)),
      PackedMove.Multiple(listOf(16646016u, 16646025u, 16646020u, 16646031u)),
      PackedMove.Multiple(listOf(16646017u, 16646019u)),
      PackedMove.Multiple(listOf(16646020u, 16646031u))
    )



    assertEquals(expected, moves)
  }

  @Test
  fun `multiple potentials and piece removal no intersecting potential`() {
    val bitboard: Bitboard = Bitboard(
      whiteGIPF = 32795UL, // 32794UL
      whiteTAMSK = 516UL, // 517UL
      whitePotentials = 516UL // 517UL
    )

    val whitePlayer = Player(
      name = PlayerName.WHITE,
    )

    val moves = mutableListOf<PackedMove>()
    bitboard.identifyPiecesToRemove(whitePlayer, moves)

    println(moves)

    val expected = listOf<PackedMove>(
      PackedMove.Multiple(values = listOf(
        16646018u, 16646016u, 16646017u, 16646019u
      )),
      PackedMove.Multiple(values = listOf(
        16646025u, 16646016u, 16646020u, 16646031u
      )),
       PackedMove.Multiple(values = listOf(
        16646016u, 16646017u, 16646019u
      )),
      PackedMove.Multiple(values = listOf(
        16646016u, 16646020u, 16646031u
      ))
    )

    assertEquals(expected, moves)
  }
}
