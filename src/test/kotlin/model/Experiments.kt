@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import org.example.ai.mcts.PackedMove
import org.example.model.Bitboard
import org.example.model.MoveType
import org.example.model.columnInfos
import org.example.model.packPossibleBitMove
import org.junit.jupiter.api.Test

class Experiments {
	@Test
	fun generateCaptureMoves() {
		val linesThroughBit: List<MutableSet<ULong>> = List(40) { mutableSetOf() }

		for (column in columnInfos) {
			for (bit in column.positions) {
				linesThroughBit[bit.countTrailingZeroBits()].add(column.columnMask)
			}
		}

		val runsThroughBit: List<MutableSet<ULong>> = List(40) { mutableSetOf() }
		for (column in columnInfos) {
			for (bit in column.positions) {
				runsThroughBit[bit.countTrailingZeroBits()].addAll(column.submasks.filter { (bit and it) == bit })
			}
		}

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

		val whitePieces = initBitboard.whitePieces
		val blackPieces = initBitboard.blackPieces
		val occupiedBits = initBitboard.globalOccupancy

		val whiteCandidates = columnInfos.filter { it ->
			it.submasks.any { submask -> (submask and whitePieces).countOneBits() >= 3 }
		}

		val blackCandidates = columnInfos.filter { it ->
			it.submasks.any { submask -> (submask and blackPieces).countOneBits() >= 3 }
		}

		val whiteMovesBuffer = mutableListOf<PackedMove>()
		val blackMovesBuffer = mutableListOf<PackedMove>()

		for (colIndex in columnInfos.indices) {
			val columnInfo = columnInfos[colIndex]
			if ((occupiedBits and columnInfo.columnMask) == columnInfo.columnMask) continue

			if (columnInfo.submasks.all { submask -> (submask and whitePieces).countOneBits() < 3 }) continue

			// --- START OF LINE MOVE ---
			// Extract directly into primitives to avoid `Pair` object allocation
			val startTargetBit = columnInfo.positions[0]
			val startPushDirection = columnInfo.pushDirections.first

			if (runsThroughBit[startTargetBit.countTrailingZeroBits()].any { (it and whitePieces).countOneBits() >= 3 && (it and occupiedBits) != it }) {
				whiteMovesBuffer.add(
					PackedMove.Single(
						0u.packPossibleBitMove(
							columnInfoIndex = columnInfo.index,
							targetBit = startTargetBit,
							pushDirection = startPushDirection,
							moveType = MoveType.AddPiece,
						)
					)
				)
			}



			// --- END OF LINE MOVE ---
			// Extract directly into primitives to avoid `Pair` object allocation
			val endTargetBit = columnInfo.positions.last()
			val endPushDirection = columnInfo.pushDirections.second

			if (runsThroughBit[endTargetBit.countTrailingZeroBits()].any { (it and whitePieces).countOneBits() >= 3 && (it and occupiedBits) != it }) {
				whiteMovesBuffer.add(
					PackedMove.Single(
						0u.packPossibleBitMove(
							columnInfoIndex = columnInfo.index,
							targetBit = endTargetBit,
							pushDirection = endPushDirection,
							moveType = MoveType.AddPiece,
						)
					)
				)
			}
		}

		for (colIndex in columnInfos.indices) {
			val columnInfo = columnInfos[colIndex]
			if ((occupiedBits and columnInfo.columnMask) == columnInfo.columnMask) continue

			if (columnInfo.submasks.all { submask -> (submask and blackPieces).countOneBits() < 3 }) continue

			// --- START OF LINE MOVE ---
			// Extract directly into primitives to avoid `Pair` object allocation

			val startTargetBit = columnInfo.positions[0]
			val startPushDirection = columnInfo.pushDirections.first

			if (runsThroughBit[startTargetBit.countTrailingZeroBits()].any { (it and whitePieces).countOneBits() >= 3 && (it and occupiedBits) != it }) {
				blackMovesBuffer.add(
					PackedMove.Single(
						0u.packPossibleBitMove(
							columnInfoIndex = columnInfo.index,
							targetBit = startTargetBit,
							pushDirection = startPushDirection,
							moveType = MoveType.AddPiece,
						)
					)
				)
			}

			// --- END OF LINE MOVE ---
			// Extract directly into primitives to avoid `Pair` object allocation
			val endTargetBit = columnInfo.positions.last()
			val endPushDirection = columnInfo.pushDirections.second

			if (runsThroughBit[endTargetBit.countTrailingZeroBits()].any { (it and blackPieces).countOneBits() >= 3 && (it and occupiedBits) != it }) {
				blackMovesBuffer.add(
					PackedMove.Single(
						0u.packPossibleBitMove(
							columnInfoIndex = columnInfo.index,
							targetBit = endTargetBit,
							pushDirection = endPushDirection,
							moveType = MoveType.AddPiece,
						)
					)
				)
			}
		}
	}
}