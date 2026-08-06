@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import net.jqwik.api.*
import org.example.ai.mcts.PackedMove
import org.example.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.util.Random

class BitboardPropertyTest {

	// --- 1. RANDOM EXTENSIONS (Mimicking Jazzer's FuzzedDataProvider) ---

	private fun Random.consumeLong(): Long = nextLong()
	private fun Random.consumeBoolean(): Boolean = nextBoolean()
	private fun Random.consumeInt(min: Int, max: Int): Int = nextInt((max - min) + 1) + min
	private fun Random.consumeInts(size: Int): IntArray = IntArray(size) { nextInt() }

	// --- 2. CORE DATA SHAPING LOGIC ---

	/**
	 * Drops active bits until the total number of 1-bits is less than or equal to [maxBits].
	 * Extremely fast, zero allocations, perfect for fuzzing/property testing.
	 */
	private fun limitActiveBits(mask: ULong, maxBits: Int): ULong {
		var current = mask
		while (current.countOneBits() > maxBits) {
			// Clears the lowest set bit
			current = current and (current - 1UL)
		}
		return current
	}

	/**
	 * Generates layered stacks where:
	 * 1. Layer N only has a bit set if Layer N-1 has that same bit set.
	 * 2. White and Black never occupy the exact same layer at the same index.
	 * 3. The total pieces across ALL layers strictly respect the max limits.
	 */
	private fun generateValidStacks(
		data: Random,
		baseWhite: ULong,
		baseBlack: ULong,
		maxWhite: Int,
		maxBlack: Int,
		maxLayers: Int = 8,
	): Pair<ULongArray, ULongArray> {
		val whiteLayer = ULongArray(maxLayers)
		val blackLayer = ULongArray(maxLayers)

		// Layer 0 starts with the guaranteed base constraints
		whiteLayer[0] = baseWhite
		blackLayer[0] = baseBlack

		var whiteCount = baseWhite.countOneBits()
		var blackCount = baseBlack.countOneBits()

		for (i in 1 until maxLayers) {
			val isEven = (i % 2 == 0)

			// --- whiteLayer stack (Base = White) ---
			// If i is odd, the piece is Black. If i is even, the piece is White.
			val wBudget = if (isEven) maxWhite - whiteCount else maxBlack - blackCount
			var nextWhite = whiteLayer[i - 1] and data.consumeLong().toULong()
			nextWhite = limitActiveBits(nextWhite, wBudget)
			whiteLayer[i] = nextWhite

			if (isEven) whiteCount += nextWhite.countOneBits()
			else blackCount += nextWhite.countOneBits()

			// --- blackLayer stack (Base = Black) ---
			// If i is odd, the piece is White. If i is even, the piece is Black.
			val bBudget = if (isEven) maxBlack - blackCount else maxWhite - whiteCount
			var nextBlack = blackLayer[i - 1] and data.consumeLong().toULong()
			nextBlack = limitActiveBits(nextBlack, bBudget)
			blackLayer[i] = nextBlack

			if (isEven) blackCount += nextBlack.countOneBits()
			else whiteCount += nextBlack.countOneBits()

			// If both stacks have stopped growing, terminate early
			if (nextWhite == 0UL && nextBlack == 0UL) break
		}

		return Pair(whiteLayer, blackLayer)
	}

	private val VALID_BOARD_MASK = 0xFFFFFFFFFFUL // Bits 0 to 39
	private val MAX_GIPF = 2
	private val MAX_TAMSK = 2
	private val MAX_ZERTZ = 2
	private val MAX_YINSH = 2
	private val MAX_DVONN = 2
	private val MAX_PUNCT = 2

	private fun generateBitboard(data: Random): Bitboard {
		var remainingBoard = VALID_BOARD_MASK

		fun claimConstrained(maxWhite: Int, maxBlack: Int): Pair<ULong, ULong> {
			val rawClaimed = remainingBoard and data.consumeLong().toULong()
			val whiteMask = limitActiveBits(rawClaimed and data.consumeLong().toULong(), maxWhite)
			val blackMask = limitActiveBits(rawClaimed and whiteMask.inv(), maxBlack)

			val totalClaimed = whiteMask or blackMask
			remainingBoard = remainingBoard and totalClaimed.inv()

			return Pair(whiteMask, blackMask)
		}

		val (whiteGipf, blackGipf) = claimConstrained(MAX_GIPF, MAX_GIPF)
		val (whiteTamsk, blackTamsk) = claimConstrained(MAX_TAMSK, MAX_TAMSK)
		val (whiteZertz, blackZertz) = claimConstrained(MAX_ZERTZ, MAX_ZERTZ)
		val (whiteYinsh, blackYinsh) = claimConstrained(MAX_YINSH, MAX_YINSH)

		val (dvonnBaseWhite, dvonnBaseBlack) = claimConstrained(MAX_DVONN, MAX_DVONN)
		val (punctBaseWhite, punctBaseBlack) = claimConstrained(MAX_PUNCT, MAX_PUNCT)

		val (whiteDVONN, blackDVONN) =
			generateValidStacks(data, dvonnBaseWhite, dvonnBaseBlack, MAX_DVONN, MAX_DVONN)
		val (whitePUNCT, blackPUNCT) =
			generateValidStacks(data, punctBaseWhite, punctBaseBlack, MAX_PUNCT, MAX_PUNCT)

		val globalWhiteOccupancy =
			whiteTamsk or whiteZertz or whiteYinsh or whiteDVONN[0] or whitePUNCT[0]
		val globalBlackOccupancy =
			blackTamsk or blackZertz or blackYinsh or blackDVONN[0] or blackPUNCT[0]

		return Bitboard(
			whiteGIPF = whiteGipf,
			blackGIPF = blackGipf,
			whiteTAMSK = whiteTamsk,
			blackTAMSK = blackTamsk,
			whiteZERTZ = whiteZertz,
			blackZERTZ = blackZertz,
			whiteYINSH = whiteYinsh,
			blackYINSH = blackYinsh,
			whiteDVONNLayer = whiteDVONN,
			blackDVONNLayer = blackDVONN,
			whitePUNCTLayer = whitePUNCT,
			blackPUNCTLayer = blackPUNCT,
			whitePotentials = globalWhiteOccupancy and data.consumeLong().toULong(),
			blackPotentials = globalBlackOccupancy and data.consumeLong().toULong(),
		)
	}

	// --- 3. JQWIK DATA PROVIDERS & PARAMETER CLASSES ---

	data class AddPieceParams(
		val bitboard: Bitboard,
		val index: Int,
		val first: Boolean,
		val pieceTypeInt: Int
	)

	@Provide
	fun addPieceProvider(): Arbitrary<AddPieceParams> = Arbitraries.randomValue { random ->
		AddPieceParams(
			bitboard = generateBitboard(random),
			index = random.consumeInt(0, 21),
			first = random.consumeBoolean(),
			pieceTypeInt = random.consumeInt(1, 6)
		)
	}

	data class UsePiecePotentialParams(
		val bitboard: Bitboard,
		val pieceTypeInt: Int,
		val targetBitShift: Int,
		val sourceBitShift: Int
	)

	@Provide
	fun usePiecePotentialProvider(): Arbitrary<UsePiecePotentialParams> = Arbitraries.randomValue { random ->
		UsePiecePotentialParams(
			bitboard = generateBitboard(random),
			pieceTypeInt = random.consumeInt(3, 6),
			targetBitShift = random.consumeInt(0, 39),
			sourceBitShift = random.consumeInt(0, 39)
		)
	}

	data class TamskPotentialParams(
		val bitboard: Bitboard,
		val isWhite: Boolean
	)

	@Provide
	fun tamskPotentialProvider(): Arbitrary<TamskPotentialParams> = Arbitraries.randomValue { random ->
		val bitboard = generateBitboard(random)

		bitboard.whiteTAMSK = bitboard.whiteTAMSK xor (bitboard.whiteTAMSK and boardCenterSpotMask)
		bitboard.blackTAMSK = bitboard.blackTAMSK xor (bitboard.blackTAMSK and boardCenterSpotMask)

		bitboard.whiteGIPF = bitboard.whiteGIPF xor (bitboard.whiteGIPF and boardCenterSpotMask)
		bitboard.blackGIPF = bitboard.blackGIPF xor (bitboard.blackGIPF and boardCenterSpotMask)

		bitboard.whiteZERTZ = bitboard.whiteZERTZ xor (bitboard.whiteZERTZ and boardCenterSpotMask)
		bitboard.blackZERTZ = bitboard.blackZERTZ xor (bitboard.blackZERTZ and boardCenterSpotMask)

		bitboard.whiteYINSH = bitboard.whiteYINSH xor (bitboard.whiteYINSH and boardCenterSpotMask)
		bitboard.blackYINSH = bitboard.blackYINSH xor (bitboard.blackYINSH and boardCenterSpotMask)

// Potentials
		bitboard.whitePotentials = bitboard.whitePotentials xor (bitboard.whitePotentials and boardCenterSpotMask)
		bitboard.blackPotentials = bitboard.blackPotentials xor (bitboard.blackPotentials and boardCenterSpotMask)

// Layered bitboards (DVONN and PUNCT)
		for (i in 0..7) {
			bitboard.whiteDVONNLayer[i] = bitboard.whiteDVONNLayer[i] xor (bitboard.whiteDVONNLayer[i] and boardCenterSpotMask)
			bitboard.blackDVONNLayer[i] = bitboard.blackDVONNLayer[i] xor (bitboard.blackDVONNLayer[i] and boardCenterSpotMask)

			bitboard.whitePUNCTLayer[i] = bitboard.whitePUNCTLayer[i] xor (bitboard.whitePUNCTLayer[i] and boardCenterSpotMask)
			bitboard.blackPUNCTLayer[i] = bitboard.blackPUNCTLayer[i] xor (bitboard.blackPUNCTLayer[i] and boardCenterSpotMask)
		}

		TamskPotentialParams(
			bitboard = bitboard,
			isWhite = random.consumeBoolean(),
		)
	}

	data class RetrieveCaptureParams(
		val bitboard: Bitboard,
		val isWhite: Boolean,
	)

	@Provide
	fun retrieveCaptureProvider(): Arbitrary<RetrieveCaptureParams> = Arbitraries.randomValue { random ->
		RetrieveCaptureParams(
			bitboard = generateBitboard(random),
			isWhite = random.consumeBoolean(),
		)
	}

	data class PlayerParams(
		val isWhite: Boolean,
		val piecesInReserve: List<UInt>,
	)

	@Provide
	fun playerProvider(): Arbitrary<PlayerParams> = Arbitraries.randomValue { random ->
		val listSize = random.consumeInt(3, 15)
		val playerBoolean = random.consumeBoolean()

		val piecesInReserve = random.consumeInts(listSize).map {
			Piece(
					"",
					potential = random.consumeBoolean(),
					colorName = if (playerBoolean) PlayerName.WHITE else PlayerName.BLACK,
					type = PieceType.entries[random.consumeInt(2, 6)],
				).pack()
		}

		PlayerParams(
			isWhite = playerBoolean,
			piecesInReserve = piecesInReserve,
		)
	}

	// --- 4. PROPERTY TESTS ---

	@Property
	fun fuzzAddAndUndoPiece(@ForAll("addPieceProvider") params: AddPieceParams, @ForAll("playerProvider") playerParams: PlayerParams) {
		val currentPlayer = Player(name = if (playerParams.isWhite) PlayerName.WHITE else PlayerName.BLACK)
		val bitboard = params.bitboard


		val initBitboard = bitboard.deepCopy()

		val piecesInReserve = playerParams.piecesInReserve

		currentPlayer.piecesInReserve.addAll(piecesInReserve.toList())

		val movesBuffer = mutableListOf<PackedMove>()
		bitboard.generateMoves(currentPlayer = currentPlayer, TurnPhase.PlayerInputWindow, movesBuffer)

		movesBuffer.removeAll { (it as PackedMove.Single).value.extractMoveType() != MoveType.AddPiece }

		assumeTrue(movesBuffer.isNotEmpty())

		val move = (movesBuffer.first() as PackedMove.Single).value

		try {
			val selectedPiece = move.onlyPiece().let { currentPlayer.selectPiece(it) }
			val vacantBit = bitboard.addPieceToBitboard(move)
			val wasOccupied = vacantBit != ULong.MAX_VALUE

			move.onlyPiece().let { currentPlayer.piecesInReserve.add(it) }
			bitboard.undoAddPieceToBitboard(move, vacantBit, wasOccupied)

			assertEquals(initBitboard, bitboard) {
				"bitboard is not equal to initBitboard.\n${initBitboard}\n${bitboard}"
			}
			assertEquals(piecesInReserve, currentPlayer.piecesInReserve)

		} catch (e: IllegalArgumentException) {
			throw e
		} catch (e: IllegalStateException) {
			throw e
		}
	}

	@Property
	fun fuzzUseAndUndoPiecePotential(@ForAll("usePiecePotentialProvider") params: UsePiecePotentialParams, @ForAll("playerProvider") playerParams: PlayerParams) {
		val bitboard = params.bitboard
		val initBitboard = bitboard.deepCopy()

		val currentPlayer = Player(name = if (playerParams.isWhite) PlayerName.WHITE else PlayerName.BLACK)
		val nextPlayer = Player(name = if (currentPlayer.name == PlayerName.WHITE) PlayerName.BLACK else PlayerName.WHITE)

		val movesBuffer = mutableListOf<PackedMove>()
		bitboard.generateMoves(currentPlayer = currentPlayer, TurnPhase.PlayerInputWindow, movesBuffer)

		movesBuffer.removeAll { (it as PackedMove.Single).value.extractMoveType() != MoveType.UsePotential }

		assumeTrue(movesBuffer.isNotEmpty())

		val move = (movesBuffer.first() as PackedMove.Single).value

		try {
			bitboard.usePiecePotential(move, currentPlayer, nextPlayer)
			bitboard.undoUsePiecePotential(move)

			assertEquals(initBitboard, bitboard) {
				"bitboard is not equal to initBitboard.\n${initBitboard}\n${bitboard}"
			}
		} catch (e: IllegalArgumentException) {
			throw e
		} catch (e: IllegalStateException) {
			throw e
		}
	}

	@Property
	fun fuzzUseAndUndoTamskPotential(@ForAll("tamskPotentialProvider") params: TamskPotentialParams) {
		val currentPlayer = Player(name = if (params.isWhite) PlayerName.WHITE else PlayerName.BLACK)

		val bitboard = params.bitboard

		when (currentPlayer.name) {
			PlayerName.WHITE -> {
				bitboard.whiteTAMSK = bitboard.whiteTAMSK or boardCenterSpotMask
				bitboard.whitePotentials = bitboard.whitePotentials or boardCenterSpotMask
			}
			PlayerName.BLACK -> {
				bitboard.blackTAMSK = bitboard.blackTAMSK or boardCenterSpotMask
				bitboard.blackPotentials = bitboard.blackPotentials or boardCenterSpotMask
			}
		}

		val initBitboard = bitboard.deepCopy()

		val movesBuffer = mutableListOf<PackedMove>()
		bitboard.getTamskMoves(currentPlayer, movesBuffer)

		assumeTrue(movesBuffer.isNotEmpty())

		val move = (movesBuffer.first() as PackedMove.Single).value

		try {
			val vacantBit = bitboard.useTamskPotential(move)
			bitboard.undoTamskPotential(move, vacantBit, vacantBit != 0UL)

			assertEquals(initBitboard, bitboard) {
				"bitboard is not equal to initBitboard.\n${initBitboard}\n${bitboard}"
			}
		} catch (e: IllegalArgumentException) {
			throw e
		} catch (e: IllegalStateException) {
			throw e
		}
	}

	@Property
	fun fuzzRetrieveAndUndoCapturePieces(@ForAll("retrieveCaptureProvider") params: RetrieveCaptureParams) {
		val bitboard = params.bitboard
		val initBitboard = bitboard.deepCopy()
		val player = Player(name = if (params.isWhite) PlayerName.WHITE else PlayerName.BLACK)
		val initPlayer = player.deepCopy()

		val movesBuffer = mutableListOf<PackedMove>()
		bitboard.identifyPiecesToRemove(player, movesBuffer)

		assumeTrue(movesBuffer.isNotEmpty())

		val move = (movesBuffer.first() as PackedMove.Multiple).values.distinct()

		val removedPieces = mutableListOf<UInt>()

		try {
			bitboard.removeSelectedPieces(player, move, removedPieces)
			player.addRetrievedCapturedPieces(removedPieces)

			val newlyStackedPieces = player.combinePieces()

			player.uncombinePieces(newlyStackedPieces)
			player.removeRetrievedCapturedPieces(removedPieces)

			bitboard.undoRetrieveAndCapturePieces(removedPieces)

			assertEquals(initBitboard, bitboard) {
				"bitboard is not equal to initBitboard.\nInit Bitbaord: ${initBitboard}\nActual Bitboard: ${bitboard}"
			}

			assertEquals(initPlayer, player)

		} catch (e: IllegalArgumentException) {
			throw e
		} catch (e: IllegalStateException) {
			throw e
		}
	}
}