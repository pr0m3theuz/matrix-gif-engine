package org.example.ai.minimax

import org.example.ai.mcts.PackedMove
import org.example.model.*

fun determineNextPhase(
  bitboard: Bitboard,
  currentPlayer: Player,
  move: PackedMove,
  turnPhase: TurnPhase,
  normalMoveMade: Boolean,
): Pair<TurnPhase, Boolean> {   // (nextPhase, nextNormalMoveMade)

  val stillNormalMoveMade = normalMoveMade || turnPhase == TurnPhase.PlayerInputWindow
      // A PieceRemoval move (RetrieveCapturePieces) is itself a terminal action for this
      // sub-decision — after resolving it, check whether more rows are still outstanding
      // (rule E.6/E.7: rows must be removed one at a time), otherwise fall through to
      // the TAMSK/turn-end check below.
      if (bitboard.evaluateLinesForFourInARow(currentPlayer).isNotEmpty()) {
        return TurnPhase.PieceRemoval to stillNormalMoveMade
      }
      // No pending row. Check for an unused, freshly-earned TAMSK extra move.
      // TODO verify the "already used this turn" bookkeeping — the old code relied on the
      // call site to notice `isTamskPieceAtCenter.isNotEmpty()` once, right after the move
      // that pushed the stack there. If getTamskMoves() would also fire on a TAMSK stack that
      // was placed on E5 in an EARLIER turn (it shouldn't be able to — H.1.1 says it's lost if
      // unused the same turn — but this needs a real flag/consumed-marker on the bitboard, not
      // just "is a TAMSK stack currently sitting on E5").
      if (turnPhase != TurnPhase.ExtraMove) {
        if (
          when (currentPlayer.name) {
            PlayerName.WHITE -> {
              bitboard.whiteTAMSK and bitboard.whitePotentials and boardCenterSpotMask
            }
            PlayerName.BLACK -> {
              bitboard.blackTAMSK and bitboard.blackPotentials and boardCenterSpotMask
            }
          } == boardCenterSpotMask
        ) {
          return TurnPhase.ExtraMove to stillNormalMoveMade
        }
      }


  // No pending rows, no pending TAMSK. Either the normal move is still owed, or the
  // turn is genuinely over.
  return if (!stillNormalMoveMade) {
    TurnPhase.PlayerInputWindow to false   // go make the normal move
  } else {
    TurnPhase.PlayerInputWindow to true    // signals turn-end to the caller
  }
}

/**
 * Applies `move` to the bitboard and returns whatever state undoMove() will need to reverse it.
 * This is where the old MoveType.AddPiece / MoveType.UsePotential / MoveType.RetrieveCapturePieces
 * / MoveType.UnusedTamskPotential branches collapse into one dispatch, reusing exactly the same
 * underlying bitboard mutators qSearch already called.
 *
 * TODO: this needs the real move-application code moved out of the old qSearch's giant `when` block
 *   (lines ~160-471 in the original file) — addPieceToBitboard/useTamskPotential/
 *   usePiecePotential/removeSelectedPieces — rather than being reimplemented here. Sketching the
 *   shape only; wire up the real calls + capture whatever each one returns (vacantBitFound,
 *   retrievedCapturedPieces, newlyStackedPieces, etc.) into UndoInfo.
 */
sealed class UndoInfo {
  data class AddPiece(val vacantBitFound: ULong, val wasCenterTamsk: Boolean) : UndoInfo()

  data class UsePotential(val postMoveState: Bitboard) : UndoInfo()

  data class RemovePieces(
      val retrievedCapturedPieces: List<UInt>,
      val newlyStackedPieces: List<UInt>,
  ) : UndoInfo()

  data class UnusedTamsk(val unusedTAMSKPotential: UInt) : UndoInfo()
}

fun applyMove(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    move: PackedMove,
): UndoInfo {
  require(move is PackedMove.Single || move is PackedMove.Multiple)

  return when (move) {
    is PackedMove.Single -> {
      val moveValue = move.value
      when (moveValue.extractMoveType()) {
        MoveType.UnusedTamskPotential -> {
          val unusedTAMSKPotential = bitboard.removeUnusedTamskPotential(currentPlayer)
          opponentPlayer.capturedPieces.add(unusedTAMSKPotential)
          UndoInfo.UnusedTamsk(unusedTAMSKPotential = unusedTAMSKPotential)
        }

        MoveType.AddPiece -> {
          val vacantBitFound =
              if (moveValue.extractSourceBit() == boardCenterSpotMask) {
                bitboard.useTamskPotential(moveValue)
              } else {
                currentPlayer.selectPiece(moveValue.onlyPiece())
                bitboard.addPieceToBitboard(moveValue)
              }

          UndoInfo.AddPiece(
              vacantBitFound = vacantBitFound,
              wasCenterTamsk = moveValue.extractSourceBit() == boardCenterSpotMask,
          )
        }

        MoveType.UsePotential -> {
          val pre = bitboard.deepCopy()
          bitboard.usePiecePotential(move = moveValue)
          UndoInfo.UsePotential(postMoveState = pre)
        }

        MoveType.RetrieveCapturePieces ->
            error("RetrieveCapturePieces must arrive as PackedMove.Multiple")
      }
    }

    is PackedMove.Multiple -> {
      val retrieved = mutableListOf<UInt>()
      bitboard.removeSelectedPieces(
          player = currentPlayer,
          piecesToRemove = move.values.distinct(),
          movesBuffer = retrieved,
          caller = "qSearch flattened PieceRemoval",
      )
      currentPlayer.addRetrievedCapturedPieces(retrieved)
      val newlyStacked = currentPlayer.combinePieces()
      UndoInfo.RemovePieces(retrievedCapturedPieces = retrieved, newlyStackedPieces = newlyStacked)
    }
  }
}

fun undoMove(
    bitboard: Bitboard,
    currentPlayer: Player,
    opponentPlayer: Player,
    move: PackedMove,
    undoInfo: UndoInfo,
) {
  when (undoInfo) {
    is UndoInfo.AddPiece -> {
      require(move is PackedMove.Single)
      val moveValue = move.value
      if (undoInfo.wasCenterTamsk) {
        bitboard.undoTamskPotential(
            move = moveValue,
            vacantBitFound = undoInfo.vacantBitFound,
            wasIndexOccupied = undoInfo.vacantBitFound != ULong.MAX_VALUE,
        )
      } else {
        bitboard.undoAddPieceToBitboard(
            move = moveValue,
            vacantBitFound = undoInfo.vacantBitFound,
            wasIndexOccupied = undoInfo.vacantBitFound != ULong.MAX_VALUE,
        )
        moveValue.onlyPiece().let { currentPlayer.piecesInReserve.add(it) }
      }
    }
    is UndoInfo.UsePotential -> {
      require(move is PackedMove.Single)
      bitboard.undoUsePiecePotential(move.value)
    }
    is UndoInfo.RemovePieces -> {
      currentPlayer.uncombinePieces(undoInfo.newlyStackedPieces)
      currentPlayer.removeRetrievedCapturedPieces(undoInfo.retrievedCapturedPieces)
      bitboard.undoRetrieveAndCapturePieces(
          undoInfo.retrievedCapturedPieces,
          "qSearch flattened PieceRemoval undo",
      )
    }
    is UndoInfo.UnusedTamsk -> {
      require(move is PackedMove.Single)
      bitboard.undoRemoveUnusedTamskPotential(move.value)
      opponentPlayer.capturedPieces.remove(undoInfo.unusedTAMSKPotential)
      // captured in UndoInfo.UnusedTamsk, currently a TODO() above.
    }
  }
}
