@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import org.example.ai.mcts.PackedMove
import org.example.ai.mcts.extractTargetBit
import org.example.toBitList
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ones
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.set
import kotlin.math.min

fun State.encodeState(): D2Array<Int> {
  // todo how does white pieces and black pieces compare to containing all the other boards as

  val STONE_COLOR = 0 // 0-45
  val EMPTY = 46 // 1
  val ONES = 47 // 1
  val ZEROS = 48 // 1
  val SENSIBLENESS = 49 // 1
  val TURNS_SINCE = 50 // 8
  val LIBERTIES = TURNS_SINCE + 7 // 6
  val LIBERTIES_AFTER = LIBERTIES + 7 // 6
  val RETRIVAL_SIZE = LIBERTIES_AFTER + 6 // 8
  val CAPTURE_SIZE = RETRIVAL_SIZE + 8 // 8
  val SELF_ATARI_SIZE = CAPTURE_SIZE + 8 // 8
  val CURRENT_PLAYER_COLOR = SELF_ATARI_SIZE + 8

  // features (95?) — TODO confirm total number of features
  val features = CURRENT_PLAYER_COLOR
  val nodeCount = 41
  // TODO add global node
  // column masks == adjacency matrix
  val ndArray = mk.zeros<Int>(features, nodeCount)

  when (currentPlayer.name) {
    PlayerName.WHITE -> {
      ndArray.set(0, bitboard.whitePieces.toBitList())
      ndArray.set(1, bitboard.whiteNeutralized.toBitList())
      ndArray.set(2, bitboard.whitePotentials.toBitList())
      ndArray.set(3, bitboard.whiteGIPF.toBitList())
      ndArray.set(4, bitboard.whiteTAMSK.toBitList())
      ndArray.set(5, bitboard.whiteYINSH.toBitList())
      ndArray.set(6, bitboard.whiteZERTZ.toBitList())

      // 7, 8, 9, 10, 11, 12, 13, 14
      bitboard.whiteDVONNLayer
          .map { it.toBitList() }
          .forEachIndexed { index, array ->
            ndArray.set(7 + index, array)
          }

      // 15, 16, 17, 18, 19, 20, 21, 22
      bitboard.whitePUNCTLayer
          .map { it.toBitList() }
          .forEachIndexed { index, array ->
            ndArray.set(15 + index, array)
          }

      ndArray.set(23, bitboard.blackPieces.toBitList())
      ndArray.set(24, bitboard.blackNeutralized.toBitList())
      ndArray.set(25, bitboard.blackGIPF.toBitList())
      ndArray.set(26, bitboard.blackPotentials.toBitList())
      ndArray.set(27, bitboard.blackZERTZ.toBitList())
      ndArray.set(28, bitboard.blackYINSH.toBitList())
      ndArray.set(29, bitboard.blackTAMSK.toBitList())

      // 30, 31, 32, 33, 34, 35, 36, 37
      bitboard.blackDVONNLayer
          .map { it.toBitList() }
          .forEachIndexed { index, array ->
            ndArray.set(30 + index, array)
          }

      // 38, 39, 40, 41, 42, 43, 44, 45
      bitboard.blackPUNCTLayer
          .map { it.toBitList() }
          .forEachIndexed { index, array ->
            ndArray.set(38 + index, array)
          }
    }
    PlayerName.BLACK -> {
      ndArray.set(0, bitboard.blackPieces.toBitList())
      ndArray.set(1, bitboard.blackNeutralized.toBitList())
      ndArray.set(2, bitboard.blackPotentials.toBitList())
      ndArray.set(3, bitboard.blackGIPF.toBitList())
      ndArray.set(4, bitboard.blackTAMSK.toBitList())
      ndArray.set(5, bitboard.blackYINSH.toBitList())
      ndArray.set(6, bitboard.blackZERTZ.toBitList())

      // 7, 8, 9, 10, 11, 12, 13, 14
      bitboard.blackDVONNLayer
          .map { it.toBitList() }
          .forEachIndexed { index, array ->
            ndArray.set(7 + index, array)
          }

      // 15, 16, 17, 18, 19, 20, 21, 22
      bitboard.blackPUNCTLayer
          .map { it.toBitList() }
          .forEachIndexed { index, array ->
            ndArray.set(15 + index, array)
          }

      ndArray.set(23, bitboard.whitePieces.toBitList())
      ndArray.set(24, bitboard.whiteNeutralized.toBitList())
      ndArray.set(25, bitboard.whiteGIPF.toBitList())
      ndArray.set(26, bitboard.whitePotentials.toBitList())
      ndArray.set(27, bitboard.whiteZERTZ.toBitList())
      ndArray.set(28, bitboard.whiteYINSH.toBitList())
      ndArray.set(29, bitboard.whiteTAMSK.toBitList())

      // 30, 31, 32, 33, 34, 35, 36, 37
      bitboard.whiteDVONNLayer
          .map { it.toBitList() }
          .forEachIndexed { index, array ->
            ndArray.set(30 + index, array)
          }

      // 38, 39, 40, 41, 42, 43, 44, 45
      bitboard.whitePUNCTLayer
          .map { it.toBitList() }
          .forEachIndexed { index, array ->
            ndArray.set(38 + index, array)
          }
    }
  }

  // empty spots
  ndArray.set(
      EMPTY,
      if (bitboard.globalOccupancy != 0UL && bitboard.globalOccupancy != 1099511627775UL) bitboard.globalOccupancy.inv().toBitList()
      else if (bitboard.globalOccupancy == 0UL) 1099511627775UL.toBitList()
      else 0UL.toBitList()
  )

  // ones
  ndArray.set(ONES, mk.ones<Int>(nodeCount))

  // zeros
  ndArray.set(ZEROS, mk.zeros<Int>(nodeCount))

  // Sensibleness (legal moves) — A move on this plane is 1 if the move is legal and doesn’t fill
  // the current player’s eyes, and 0 otherwise.
  // TODO + 46
  // todo use fold to accumulate source & target bitmasks? would stacks matter?
  // TODO need to change approach as I need to consider how each move affects the board & potentials
  //  ndArray[num_self_atari_stones][bit] = 1
  val availableMoves = mutableListOf<PackedMove>()
  bitboard.identifyAvailableMoves(
      currentPlayer,
      columnInfos = columnInfos,
      movesBuffer = availableMoves,
  )

  val flattenMoves =
      availableMoves
          .distinctBy { it.extractTargetBit() }
          .fold(0UL) { acc, move ->
            move.extractTargetBit()?.let { acc or it } ?: acc
          }
          .toBitList()

  ndArray.set(SENSIBLENESS, flattenMoves)

  // TODO Turns since — number of turns or actions since move was made — This set of eight binary
  // planes
  //  indicates how many moves ago a move was played.
  // multiple moves to the same bit will show up as just 1 move
  val previousTurnsMoves = ULongArray(8)
  // todo should previous move captures be add to previous move features
  val previousTurnCaptures = ULongArray(8)

  // last turn is first
  turnMoves.keys.toList().takeLast(8).reversed().forEachIndexed { index, turn ->
    val moves = turnMoves[turn] ?: error("Turn $turn moves not found!")

    moves.forEach { move ->
      when (move) {
        is PackedMove.Multiple -> {
          val bits =
              move.values.fold(0UL) { acc, bit ->
                acc or bit.extractTargetBit()
              }
          previousTurnCaptures[index] = previousTurnCaptures[index] or bits
        }
        is PackedMove.Single -> {
          requireNotNull(move.value.extractTargetBit())
          previousTurnsMoves[index] = previousTurnsMoves[index] or move.value.extractTargetBit()
        }
      }
    }
  }

  previousTurnsMoves
      .map { it.toBitList() }
      .forEachIndexed { index, array ->
        ndArray.set(TURNS_SINCE + index, array)
      }

  // Liberties — Number of liberties (empty adjacent points) + 6
  val liberitiesPlanes = ULongArray(7)

  neighbouringBitsBitmasks.forEach { (bit, mask) ->
    val liberties = mask.countOneBits() - (mask and bitboard.globalOccupancy).countOneBits()
    liberitiesPlanes[liberties] = liberitiesPlanes[liberties] or bit
  }

  //
  liberitiesPlanes
      .map { it.toBitList() }
      .forEachIndexed { index, array ->
        ndArray.set(LIBERTIES + index, array)
      }

  // Liberties after move / board after move + 6
  val postMoveLiberities = ULongArray(7)

  // TODO potentials are currently ignored
  val retrievalPlanes = ULongArray(8)
  // capture — How many opponent stones would this move capture?
  val capturePlanes = ULongArray(8)

  availableMoves
      .distinctBy { it.extractTargetBit() }
      .forEach { move ->
        val newState = this.deepCopy()
        val newBitboard = bitboard.deepCopy()

        when (move) {
          is PackedMove.Multiple -> {}
          is PackedMove.Single -> {
            when (move.value.extractMoveType()) {
              MoveType.UnusedTamskPotential -> {
                val unusedTAMSKPotential = newBitboard.removeUnusedTamskPotential(newState.currentPlayer)

                // add the unused TAMSK Potential to the opponent's captured pieces.
                newState.nextPlayer.capturedPieces.add(unusedTAMSKPotential)
              }
              MoveType.AddPiece -> {
                if (move.value.extractSourceBit() == boardCenterSpotMask) {
                  newBitboard.useTamskPotential(move.value)
                } else {
                  val selectedPiece =
                      move.value.onlyPiece().let { newState.currentPlayer.selectPiece(it) }

                  selectedPiece?.let {
                    newBitboard.addPieceToBitboard(move.value)
                  }
                }
              }
              MoveType.UsePotential -> {
                newBitboard.usePiecePotential(
                    move = move.value,
                )
              }
              MoveType.RetrieveCapturePieces -> {}
            }
          }
        }

        newBitboard.assertPieceCount(
            currentPlayer = newState.currentPlayer,
            nextPlayer = newState.nextPlayer,
        )

        neighbouringBitsBitmasks.forEach { (bit, mask) ->
          val liberties =
              mask.countOneBits() - (mask and newBitboard.globalOccupancy).countOneBits()
          postMoveLiberities[liberties] = postMoveLiberities[liberties] or bit
        }

        // todo does this even make sense in this context as you decide what to pieces to remove
        //  * select the max value or populate each plane from minimum to maximum of 8
        //  * neural network output which pieces to remove
        //  *
        val removePiecesPowerset = mutableListOf<PackedMove>()
        newBitboard.identifyAvailableMoves(currentPlayer, columnInfos, removePiecesPowerset)
        removePiecesPowerset.forEach { retrieveCapture ->
          when (retrieveCapture) {
            is PackedMove.Multiple -> {
              check(retrieveCapture.values.isNotEmpty()) {
                "There must be at least one column"
              }
              // newBitboard.deepCopy() or undo removal
              val retrievedCapturedPieces = mutableListOf<UInt>()

              newBitboard
                  .deepCopy()
                  .removeSelectedPieces(
                      player = newState.currentPlayer,
                      piecesToRemove = retrieveCapture.values.distinct(),
                      retrievedCapturedPieces,
                  )

              val (retrievedPieces, capturedPieces) =
                  retrievedCapturedPieces.partition {
                    it.extractRetrieveCapture() == RetrieveCapture.RETRIEVE
                  }

              val retrievedIndex = min(retrievedPieces.size, 7)
              val capturedIndex = min(capturedPieces.size, 7)

              retrievedPieces.forEach { piece ->
                retrievalPlanes[retrievedIndex] =
                    retrievalPlanes[retrievedIndex] or piece.extractTargetBit()
              }
              capturedPieces.forEach { piece ->
                capturePlanes[capturedIndex] =
                    capturePlanes[capturedIndex] or piece.extractTargetBit()
              }
            }
            is PackedMove.Single -> {}
          }
        }
      }

  postMoveLiberities
      .map { it.toBitList() }
      .forEachIndexed { index, array ->
        ndArray.set(LIBERTIES_AFTER + index, array)
      }

  // 50, 51, 52, 53, 54, 55, 56, 57
  retrievalPlanes
      .map { it.toBitList() }
      .forEachIndexed { index, array ->
        ndArray.set(RETRIVAL_SIZE + index, array)
      }

  // 58, 59, 60, 61, 62, 63, 64, 65
  capturePlanes
      .map { it.toBitList() }
      .forEachIndexed { index, array ->
        ndArray.set(CAPTURE_SIZE + index, array)
      }

  // Self-atari size — How many of own stones would be captured
  //  If this move was played, how many of your own stones
  //  would be put into atari and could be captured by the opponent
  //  in the next move?

  // TODO potentials are currently ignored

  //  val opponentRetrievalPlanes = ULongArray(8)
  val opponentCapturePlanes = ULongArray(8)

  val availableOpponentMoves = mutableListOf<PackedMove>()
  bitboard.identifyAvailableMoves(nextPlayer, movesBuffer = availableOpponentMoves)

  availableOpponentMoves
      .distinctBy { it.extractTargetBit() }
      .forEach { move ->
        // TODO new bitboard State
        //  ndArray[num_self_atari_stones][move.targetBit] = 1
        val newState = this.deepCopy()
        val newBitboard = bitboard.deepCopy()

        when (move) {
          is PackedMove.Multiple -> {}
          is PackedMove.Single -> {
            when (move.value.extractMoveType()) {
              MoveType.UnusedTamskPotential -> {
                val unusedTAMSKPotential = newBitboard.removeUnusedTamskPotential(newState.nextPlayer)

                // add the unused TAMSK Potential to the opponent's captured pieces.
                newState.currentPlayer.capturedPieces.add(unusedTAMSKPotential)
              }
              MoveType.AddPiece -> {
                if (move.value.extractSourceBit() == boardCenterSpotMask) {
                  newBitboard.useTamskPotential(move.value)
                } else {
                  val selectedPiece =
                      move.value.onlyPiece()?.let { newState.nextPlayer.selectPiece(it) }

                  selectedPiece?.let {
                    newBitboard.addPieceToBitboard(move.value)
                  }
                }
              }
              MoveType.UsePotential -> {
                newBitboard.usePiecePotential(
                    move = move.value,
                )
              }
              MoveType.RetrieveCapturePieces -> {}
            }
          }
        }

        newBitboard.assertPieceCount(
            currentPlayer = newState.nextPlayer,
            nextPlayer = newState.currentPlayer,
        )

        val opponentRetrievedCapturedPieces = mutableListOf<PackedMove>()
        newBitboard.identifyPiecesToRemove(newState.nextPlayer, opponentRetrievedCapturedPieces)
        opponentRetrievedCapturedPieces.forEach { retrieveCapture ->
          when (retrieveCapture) {
            is PackedMove.Multiple -> {
              // newBitboard.deepCopy() or undo removal
              val retrievedCapturedPieces = mutableListOf<UInt>()

              newBitboard
                  .deepCopy()
                  .removeSelectedPieces(
                      player = newState.nextPlayer,
                      piecesToRemove = retrieveCapture.values.distinct(),
                      movesBuffer = retrievedCapturedPieces,
                  )
              val capturedPieces =
                  retrievedCapturedPieces
                      .partition { it.extractRetrieveCapture() == RetrieveCapture.CAPTURE }
                      .second

              //      val retrievedIndex = min(retrievedPieces.size, 7)
              val capturedIndex = min(capturedPieces.size, 7)

              //      retrievalPlanes[retrievedIndex] = retrievalPlanes[retrievedIndex] or
              // addAtIndex
              capturedPieces.forEach { piece ->
                opponentCapturePlanes[capturedIndex] =
                    opponentCapturePlanes[capturedIndex] or piece.extractTargetBit()
              }
            }

            is PackedMove.Single -> {}
          }
        }
      }

  // 66, 67, 68, 69, 70, 71, 72, 73
  opponentCapturePlanes
      .map { it.toBitList() }
      .forEachIndexed { index, array ->
        ndArray.set(SELF_ATARI_SIZE + index, array)
      }

  // current player color
//  when (currentPlayer.name) {
//    PlayerName.WHITE -> {
//      ndArray.set(CURRENT_PLAYER_COLOR, mk.zeros<Int>(nodeCount))
//    }
//    PlayerName.BLACK -> {
//      ndArray.set(CURRENT_PLAYER_COLOR, mk.ones<Int>(nodeCount))
//    }
//  }

  return ndArray
}

fun State.encodeGlobalState(turnPhase: TurnPhase): D1Array<Float> {
  // TODO total pieces equal 3 and 6
  //  move player turn feature here
  //  encode turn phase
  //  needs to be a tensor

  val ndArray = mk.zeros<Float>(26)

  fun countPieces(pieces: List<UInt>, type: PieceType): Float {
    val max = if (type == PieceType.GIPF) 3.0f else 6.0f
    val count = pieces.sumOf { piece ->
      if (piece.extractPieceType() == type) {
        if (piece.extractPotential()) 2L else 1L
      } else {
        0L
      }
    }
    return count.toFloat() / max
  }

  val pieceTypes = listOf(
    PieceType.GIPF, PieceType.TAMSK, PieceType.ZERTZ,
    PieceType.YINSH, PieceType.DVONN, PieceType.PUNCT
  )

  pieceTypes.forEachIndexed { i, type ->
    // --- Reserves (Relative Encoding) ---
    // Indices 0-5: Current Player's reserve
    ndArray[i] = countPieces(currentPlayer.piecesInReserve, type)
    // Indices 6-11: Opponent's reserve
    ndArray[i + 6] = countPieces(nextPlayer.piecesInReserve, type)

    // --- Captures (Relative Encoding) ---
    // Indices 12-17: Current Player's captures
    ndArray[i + 12] = countPieces(currentPlayer.capturedPieces, type)
    // Indices 18-23: Opponent's captures
    ndArray[i + 18] = countPieces(nextPlayer.capturedPieces,  type)
  }

  ndArray[24] = when (turnPhase) {
    TurnPhase.ExtraMove -> 0.0f
    TurnPhase.PlayerInputWindow -> 0.5f
    TurnPhase.PieceRemoval -> 1.0f
  }

  // current player color
  ndArray[25] = if (currentPlayer.name == PlayerName.WHITE) 0.0f else 1.0f

  return ndArray
}