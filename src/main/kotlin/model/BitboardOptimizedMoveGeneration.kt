@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.example.ai.mcts.PackedMove

private val logger = logger {}

fun Bitboard.getTamskMoves(
	player: Player,
	movesBuffer: MutableList<PackedMove>, // Updated to UInt buffer
//	sortedColumns: List<ColumnInfo> = columnInfos, // Added to resolve the global colIndex
) {
	// --- 0. CONFIGURABLE DEBUGGING ---
	if (logger.isDebugEnabled()) {
		logger.trace { "--- GET TAMSK MOVES CALLED ---" }
		logger.trace { "Player: ${player.name}" }
	}

	// --- 1. EVALUATE CENTER SPOT ---
	// A valid TAMSK opening move requires the piece to be on the TAMSK board,
	// still marked as a Potential, and located exactly at the board center.
	val tamskPieceAtCenter =
		when (player.name) {
			PlayerName.WHITE -> whiteTAMSK and whitePotentials and boardCenterSpotMask
			PlayerName.BLACK -> blackTAMSK and blackPotentials and boardCenterSpotMask
		}

	// --- 2. VALIDATE AVAILABILITY ---
	// If the intersection doesn't perfectly match the center spot mask, no move exists.
	if (tamskPieceAtCenter != boardCenterSpotMask) {
		if (logger.isDebugEnabled()) {
			logger.trace {
				"No valid TAMSK piece found at center for ${player.name}. Returning null."
			}
			logger.trace { "--- GET TAMSK MOVES COMPLETED ---" }
		}
		return
	}

	// --- 3. PRE-RETURN SANITY CHECK ---
	// Double-check that our center spot mask hasn't been corrupted into a multi-bit mask.
	check(tamskPieceAtCenter.countOneBits() == 1) {
		"CRITICAL ERROR: Evaluated center spot mask must contain exactly one bit. Got: $tamskPieceAtCenter"
	}

	logger.trace { "Valid TAMSK move found for ${player.name} at center spot." }

	// --- 4. RETURN MOVE ---
	// Cache the vacantLines reference in case it's a computed property,
	// preventing multiple evaluations in the loop conditions.
	for (colIndex in columnInfos.indices) {
		val columnInfo = columnInfos[colIndex]
		if ((globalOccupancy and columnInfo.columnMask) == columnInfo.columnMask) continue

		// --- START OF LINE MOVE ---
		// Extract directly into primitives to avoid `Pair` object allocation
		val startTargetBit = columnInfo.positions.first()
		val startPushDirection = columnInfo.pushDirections.first

		movesBuffer.add(
			PackedMove.Single(
				0u.packPossibleBitMove(
					sourceBit = tamskPieceAtCenter,
					targetBit = startTargetBit,
					pushDirection = startPushDirection,
					moveType = MoveType.AddPiece,
					columnInfoIndex = columnInfo.index,
				)
					.setPieceType(
						pieceType = PieceType.TAMSK,
					)
					.setPieceColor(
						pieceColor = player.name,
					)
					.setPotential(potential = true)
			)
		)

		// --- END OF LINE MOVE ---
		// Extract directly into primitives to avoid `Pair` object allocation
		val endTargetBit = columnInfo.positions.last()
		val endPushDirection = columnInfo.pushDirections.second

		movesBuffer.add(
			PackedMove.Single(
				0u.packPossibleBitMove(
					sourceBit = tamskPieceAtCenter,
					targetBit = endTargetBit,
					pushDirection = endPushDirection,
					moveType = MoveType.AddPiece,
					columnInfoIndex = columnInfo.index,
				)
					.setPieceType(
						pieceType = PieceType.TAMSK,
					)
					.setPieceColor(
						pieceColor = player.name,
					)
					.setPotential(potential = true)
			)
		)
	}

	// add Unused TAMSK Potential Move to Moves Buffer
	  movesBuffer.add(
			PackedMove.Single(
				0u.packPossibleBitMove(
					sourceBit = tamskPieceAtCenter,
					targetBit = tamskPieceAtCenter,
					moveType = MoveType.UnusedTamskPotential,
				)
					.setPieceType(
						pieceType = PieceType.TAMSK,
					)
					.setPieceColor(
						pieceColor = player.name,
					)
					.setPotential(potential = true)
			)
		 )
}

fun Bitboard.getZertzMoves(
	player: Player,
	columnInfos: List<ColumnInfo>,
	movesBuffer: MutableList<PackedMove>,
) {
	val zertzPieces =
		when (player.name) {
			PlayerName.WHITE -> whiteZERTZ and whitePotentials
			PlayerName.BLACK -> blackZERTZ and blackPotentials
		}

	if (zertzPieces == 0UL) return

	for (col in columnInfos) {
		val colZertzPieces = col.columnMask and zertzPieces

		// Skip this column entirely if it has no ZERTZ potentials
		if (colZertzPieces == 0UL) continue

		// Primitive loop: Eliminates .filter, .forEach, and .indexOf allocations
		for (zertzIndex in col.positions.indices) {
			val sourceBit = col.positions[zertzIndex]

			// Check if this specific position holds a valid ZERTZ potential piece
			if ((sourceBit and colZertzPieces) != 0UL) {

				// --- 1. Check positions ABOVE (higher index) ---
				for (index in (zertzIndex + 1) until col.positions.size) {
					val targetBit = col.positions[index]
					val isOccupied = (targetBit and globalOccupancy) != 0UL

					if (isOccupied) {
						// Spot is occupied: continue jumping over the piece
						continue
					} else {
						// Spot is empty: Did we jump over at least one piece?
						if (index - zertzIndex > 1) {
							movesBuffer.add(
								PackedMove.Single(
									0u.packPossibleBitMove(
										sourceBit = sourceBit,
										targetBit = targetBit,
										moveType = MoveType.UsePotential,
									)
										.setPieceType(
											pieceType = PieceType.ZERTZ,
										)
										.setPieceColor(
											pieceColor = player.name,
										)
										.setPotential(potential = true)
								)
							)
						}
						// Stop looking further up this column once we hit the first empty spot
						break
					}
				}

				// --- 2. Check positions BELOW (lower index) ---
				for (index in (zertzIndex - 1) downTo 0) {
					val targetBit = col.positions[index]
					val isOccupied = (targetBit and globalOccupancy) != 0UL

					if (isOccupied) {
						// Spot is occupied: continue jumping over the piece
						continue
					} else {
						// Spot is empty: Did we jump over at least one piece?
						if (zertzIndex - index > 1) {
							movesBuffer.add(
								PackedMove.Single(
									0u.packPossibleBitMove(
										sourceBit = sourceBit,
										targetBit = targetBit,
										moveType = MoveType.UsePotential,
									)
										.setPieceType(
											pieceType = PieceType.ZERTZ,
										)
										.setPieceColor(
											pieceColor = player.name,
										)
										.setPotential(potential = true)
								)
							)
						}
						// Stop looking further down this column once we hit the first empty spot
						break
					}
				}
			}
		}
	}
}

fun Bitboard.getYinshMoves(
	player: Player,
	columnInfos: List<ColumnInfo>,
	movesBuffer: MutableList<PackedMove>, // Updated to UInt buffer
) {
	// Fixed minor typo in variable name: yinchPieces -> yinshPieces
	val yinshPieces =
		when (player.name) {
			PlayerName.WHITE -> whiteYINSH and whitePotentials
			PlayerName.BLACK -> blackYINSH and blackPotentials
		}

	if (yinshPieces == 0UL) return

	for (col in columnInfos) {
		val colYinshPieces = col.columnMask and yinshPieces

		// Skip this column entirely if it has no YINSH potentials
		if (colYinshPieces == 0UL) continue

		// Primitive loop: Eliminates .filter, .forEach, and .indexOf allocations
		for (yinshIndex in col.positions.indices) {
			val sourceBit = col.positions[yinshIndex]

			// Check if this specific position holds a valid YINSH potential piece
			if ((sourceBit and colYinshPieces) != 0UL) {

				// --- 1. Check positions ABOVE (higher index) ---
				for (index in (yinshIndex + 1) until col.positions.size) {
					val targetBit = col.positions[index]
					val isOccupied = (targetBit and globalOccupancy) != 0UL

					if (!isOccupied) {
						// Spot is empty: add as a valid slide destination
						movesBuffer.add(
							PackedMove.Single(
								0u.packPossibleBitMove(
									sourceBit = sourceBit,
									targetBit = targetBit,
									moveType = MoveType.UsePotential,
								)
									.setPieceType(
										pieceType = PieceType.YINSH,
									)
									.setPieceColor(
										pieceColor = player.name,
									)
									.setPotential(potential = true)
							)
						)
					} else {
						// Spot is occupied: YINSH sliding stops at the first piece
						break
					}
				}

				// --- 2. Check positions BELOW (lower index) ---
				for (index in (yinshIndex - 1) downTo 0) {
					val targetBit = col.positions[index]
					val isOccupied = (targetBit and globalOccupancy) != 0UL

					if (!isOccupied) {
						// Spot is empty: add as a valid slide destination
						movesBuffer.add(
							PackedMove.Single(
								0u.packPossibleBitMove(
									sourceBit = sourceBit,
									targetBit = targetBit,
									moveType = MoveType.UsePotential,
								)
									.setPieceType(
										pieceType = PieceType.YINSH,
									)
									.setPieceColor(
										pieceColor = player.name,
									)
									.setPotential(potential = true)
							)
						)
					} else {
						// Spot is occupied: YINSH sliding stops at the first piece
						break
					}
				}
			}
		}
	}
}

fun Bitboard.getDvonnMoves(
	player: Player,
	columnInfos: List<ColumnInfo>,
	movesBuffer: MutableList<PackedMove>, // Updated to UInt buffer
) {
	val activeDvonnPieces =
		when (player.name) {
			PlayerName.WHITE -> whiteDVONNLayer[0] and whitePotentials and whiteNeutralized.inv()
			PlayerName.BLACK -> blackDVONNLayer[0] and blackPotentials and blackNeutralized.inv()
		}

	if (activeDvonnPieces == 0UL) return

	val targetDvonnPieces =
		when (player.name) {
			PlayerName.BLACK -> {
				var activeWhiteDvonn = 0UL
				var blockedDvonnMask = 0UL

				// Use downTo to avoid the Iterator allocation of .indices.reversed()
				for (i in (whiteDVONNLayer.size - 1) downTo 0) {
					val isEvenLayer = i % 2 == 0
					val visibleWhiteThisLayer =
						if (isEvenLayer) {
							whiteDVONNLayer[i] and blockedDvonnMask.inv()
						} else {
							blackDVONNLayer[i] and blockedDvonnMask.inv()
						}
					val visibleBlackThisLayer =
						if (isEvenLayer) {
							blackDVONNLayer[i] and blockedDvonnMask.inv()
						} else {
							whiteDVONNLayer[i] and blockedDvonnMask.inv()
						}

					activeWhiteDvonn = activeWhiteDvonn or visibleWhiteThisLayer
					blockedDvonnMask = blockedDvonnMask or visibleWhiteThisLayer or visibleBlackThisLayer
				}
				activeWhiteDvonn
			}

			PlayerName.WHITE -> {
				var activeBlackDvonn = 0UL
				var blockedDvonnMask = 0UL

				for (i in (whiteDVONNLayer.size - 1) downTo 0) {
					val isEvenLayer = i % 2 == 0
					val visibleWhiteThisLayer =
						if (isEvenLayer) {
							whiteDVONNLayer[i] and blockedDvonnMask.inv()
						} else {
							blackDVONNLayer[i] and blockedDvonnMask.inv()
						}
					val visibleBlackThisLayer =
						if (isEvenLayer) {
							blackDVONNLayer[i] and blockedDvonnMask.inv()
						} else {
							whiteDVONNLayer[i] and blockedDvonnMask.inv()
						}

					activeBlackDvonn = activeBlackDvonn or visibleBlackThisLayer
					blockedDvonnMask = blockedDvonnMask or visibleWhiteThisLayer or visibleBlackThisLayer
				}
				activeBlackDvonn
			}
		}

	for (col in columnInfos) {
		val columnActiveDvonnPieces = col.columnMask and activeDvonnPieces
		val columnTargetDvonnPieces = col.columnMask and targetDvonnPieces

		if (columnActiveDvonnPieces == 0UL || columnTargetDvonnPieces == 0UL) continue

		// Primitive loop: eliminates .indexOf() scaling issues
		for (dvonnIndex in col.positions.indices) {
			val sourceBit = col.positions[dvonnIndex]

			// Check if this specific position holds a valid active piece
			if ((sourceBit and columnActiveDvonnPieces) != 0UL) {

				// --- 1. Check positions ABOVE (higher index) ---
				for (index in (dvonnIndex + 1) until col.positions.size) {
					val targetBit = col.positions[index]
					val isOccupied = (targetBit and globalOccupancy) != 0UL

					if (!isOccupied) {
						continue // Path is empty, keep sliding up
					}

					// We hit a piece. Is it our target?
					if ((targetBit and columnTargetDvonnPieces) != 0UL) {
						movesBuffer.add(
							PackedMove.Single(
								0u.packPossibleBitMove(
									sourceBit = sourceBit,
									targetBit = targetBit,
									moveType = MoveType.UsePotential,
								)
									.setPieceType(
										pieceType = PieceType.DVONN,
									)
									.setPieceColor(
										pieceColor = player.name,
									)
									.setPotential(potential = true)
							)
						)
					}
					// Stop looking up regardless, because a piece blocks the path
					break
				}

				// --- 2. Check positions BELOW (lower index) ---
				for (index in (dvonnIndex - 1) downTo 0) {
					val targetBit = col.positions[index]
					val isOccupied = (targetBit and globalOccupancy) != 0UL

					if (!isOccupied) {
						continue // Path is empty, keep sliding down
					}

					// We hit a piece. Is it our target?
					if ((targetBit and columnTargetDvonnPieces) != 0UL) {
						movesBuffer.add(
							PackedMove.Single(
								0u.packPossibleBitMove(
									sourceBit = sourceBit,
									targetBit = targetBit,
									moveType = MoveType.UsePotential,
								)
									.setPieceType(
										pieceType = PieceType.DVONN,
									)
									.setPieceColor(
										pieceColor = player.name,
									)
									.setPotential(potential = true)
							)
						)
					}
					// Stop looking down regardless, because a piece blocks the path
					break
				}
			}
		}
	}
}

fun Bitboard.getPunctMoves(
	player: Player,
	columnInfos: List<ColumnInfo> = org.example.model.columnInfos,
	movesBuffer: MutableList<PackedMove>, // Updated to UInt buffer
) {
	val activePunctPieces =
		when (player.name) {
			PlayerName.WHITE -> whitePUNCTLayer[0] and whitePotentials and whiteNeutralized.inv()
			PlayerName.BLACK -> blackPUNCTLayer[0] and blackPotentials and blackNeutralized.inv()
		}

	if (activePunctPieces == 0UL) return

	val targetPunctPieces =
		when (player.name) {
			PlayerName.BLACK -> {
				var activeWhitePunct = 0UL
				var blockedPunctMask = 0UL

				// Use downTo to avoid the Iterator allocation of .indices.reversed()
				for (i in (whitePUNCTLayer.size - 1) downTo 0) {
					val isEvenLayer = i % 2 == 0
					val visibleWhiteThisLayer =
						if (isEvenLayer) {
							whitePUNCTLayer[i] and blockedPunctMask.inv()
						} else {
							blackPUNCTLayer[i] and blockedPunctMask.inv()
						}
					val visibleBlackThisLayer =
						if (isEvenLayer) {
							blackPUNCTLayer[i] and blockedPunctMask.inv()
						} else {
							whitePUNCTLayer[i] and blockedPunctMask.inv()
						}

					activeWhitePunct = activeWhitePunct or visibleWhiteThisLayer
					blockedPunctMask = blockedPunctMask or visibleWhiteThisLayer or visibleBlackThisLayer
				}
				activeWhitePunct
			}

			PlayerName.WHITE -> {
				var activeBlackPunct = 0UL
				var blockedPunctMask = 0UL

				// Use downTo to avoid the Iterator allocation of .indices.reversed()
				for (i in (whitePUNCTLayer.size - 1) downTo 0) {
					val isEvenLayer = i % 2 == 0
					val visibleWhiteThisLayer =
						if (isEvenLayer) {
							whitePUNCTLayer[i] and blockedPunctMask.inv()
						} else {
							blackPUNCTLayer[i] and blockedPunctMask.inv()
						}
					val visibleBlackThisLayer =
						if (isEvenLayer) {
							blackPUNCTLayer[i] and blockedPunctMask.inv()
						} else {
							whitePUNCTLayer[i] and blockedPunctMask.inv()
						}

					activeBlackPunct = activeBlackPunct or visibleBlackThisLayer
					blockedPunctMask = blockedPunctMask or visibleWhiteThisLayer or visibleBlackThisLayer
				}
				activeBlackPunct
			}
		}

	for (col in columnInfos) {
		val columnActivePunctPieces = col.columnMask and activePunctPieces
		val columnTargetPunctPieces = col.columnMask and targetPunctPieces

		if (columnActivePunctPieces == 0UL || columnTargetPunctPieces == 0UL) continue

		// Primitive loop: eliminates .indexOf() scaling issues
		for (punctIndex in col.positions.indices) {
			val sourceBit = col.positions[punctIndex]

			// Check if this specific position holds a valid active piece
			if ((sourceBit and columnActivePunctPieces) != 0UL) {

				// --- 1. Check positions ABOVE (higher index) ---
				for (index in (punctIndex + 1) until col.positions.size) {
					val targetBit = col.positions[index]
					val isOccupied = (targetBit and globalOccupancy) != 0UL

					if (!isOccupied) {
						continue // Path is empty, keep sliding up
					}

					// We hit a piece. Is it our target?
					if ((targetBit and columnTargetPunctPieces) != 0UL) {
						check(sourceBit != targetBit) {
							"Invalid board state transition: Cannot execute move where source index ($sourceBit) matches target index ($targetBit)."
						}
						movesBuffer.add(
							PackedMove.Single(
								0u.packPossibleBitMove(
									sourceBit = sourceBit,
									targetBit = targetBit,
									moveType = MoveType.UsePotential,
								)
									.setPieceType(
										pieceType = PieceType.PUNCT,
									)
									.setPieceColor(
										pieceColor = player.name,
									)
									.setPotential(potential = true)
							)
						)
					}
					// Stop looking up regardless, because a piece blocks the path
					break
				}

				// --- 2. Check positions BELOW (lower index) ---
				for (index in (punctIndex - 1) downTo 0) {
					val targetBit = col.positions[index]
					val isOccupied = (targetBit and globalOccupancy) != 0UL

					if (!isOccupied) {
						continue // Path is empty, keep sliding down
					}

					// We hit a piece. Is it our target?
					if ((targetBit and columnTargetPunctPieces) != 0UL) {
						movesBuffer.add(
							PackedMove.Single(
								0u.packPossibleBitMove(
									sourceBit = sourceBit,
									targetBit = targetBit,
									moveType = MoveType.UsePotential,
								)
									.setPieceType(
										pieceType = PieceType.PUNCT,
									)
									.setPieceColor(
										pieceColor = player.name,
									)
									.setPotential(potential = true)
							)
						)
					}
					// Stop looking down regardless, because a piece blocks the path
					break
				}
			}
		}
	}
}

fun Bitboard.identifyAvailableMoves(
	currentPlayer: Player,
	columnInfos: List<ColumnInfo> = org.example.model.columnInfos,
	movesBuffer: MutableList<PackedMove>,
) {
	movesBuffer.clear()

	val gipfPieceInReserve =
		currentPlayer.piecesInReserve.firstOrNull { piece -> piece.extractPieceType() == PieceType.GIPF }

//  val playableStackedPiecesInReserve: List<UInt> =
//      currentPlayer.piecesInReserve
//          .filter { piece ->
//            piece.extractPieceType() != PieceType.GIPF && piece.extractPotential()
//          }
//          .distinctBy { piece -> piece.extractPieceType() }

	val seenTypes = mutableSetOf<PieceType>()
	val playableStackedPiecesInReserve = mutableListOf<UInt>()

	for (piece in currentPlayer.piecesInReserve) {
		val type = piece.extractPieceType()
		if (type != null) {
			if (type != PieceType.GIPF && piece.extractPotential() && seenTypes.add(type)) {
				playableStackedPiecesInReserve.add(piece)
			}
		}
	}

	// GIPF Pieces
	val occupiedBits = globalOccupancy
	if (gipfPieceInReserve != null) {
		for (colIndex in columnInfos.indices) {
			val columnInfo = columnInfos[colIndex]
			if ((occupiedBits and columnInfo.columnMask) == columnInfo.columnMask) continue

			// --- START OF LINE MOVE ---
			// Extract directly into primitives to avoid `Pair` object allocation
			val startTargetBit = columnInfo.positions[0]
			val startPushDirection = columnInfo.pushDirections.first

			movesBuffer.add(
				PackedMove.Single(
					0u.packPossibleBitMove(
						piece = gipfPieceInReserve,
						columnInfoIndex = columnInfo.index,
						targetBit = startTargetBit,
						pushDirection = startPushDirection,
						moveType = MoveType.AddPiece,
					)
				)
			)

			// --- END OF LINE MOVE ---
			// Extract directly into primitives to avoid `Pair` object allocation
			val endTargetBit = columnInfo.positions.last()
			val endPushDirection = columnInfo.pushDirections.second

			movesBuffer.add(
				PackedMove.Single(
					0u.packPossibleBitMove(
						piece = gipfPieceInReserve,
						columnInfoIndex = columnInfo.index,
						targetBit = endTargetBit,
						pushDirection = endPushDirection,
						moveType = MoveType.AddPiece,
					)
				)
			)
		}
		return
	}

	// TODO PieceType.TAMSK logic is handled by isTamskPieceAtCenter()
	getTamskMoves(currentPlayer, movesBuffer)
	if (movesBuffer.isNotEmpty()) {
		//    require(movesBuffer.all { it.sourceBit != null }) {
		//      val invalidIndex = movesBuffer.indexOfFirst { it.sourceBit == null }
		//      "Move Buffer Contamination: Found uninitialized or invalid sourceBit at index
		// $invalidIndex " +
		//          "out of ${movesBuffer.size} total buffered moves. Buffer contents: $movesBuffer"
		//    }
		return
	}

	/**
	 * TODO rewrite Check check(piece?.extractPotential() == false) { // val pieceCoords =
	 * selectedNode.node.coordinate.let { "${it.column}${it.row}" } val currentPotential =
	 * piece?.extractPotential()
	 *
	 * // "Invalid piece state at $pieceCoords: Expected piece potential to be spent (false), " // +
	 * "but found potential status is: $currentPotential (Piece Type:
	 * ${piece?.extractPieceType()?.name}, Color: ${piece?.extractPieceColor()})" }
	 */
	for (piece in playableStackedPiecesInReserve) {
		for (colIndex in columnInfos.indices) {
			val columnInfo = columnInfos[colIndex]
			if ((occupiedBits and columnInfo.columnMask) == columnInfo.columnMask) continue

			// --- START OF LINE MOVE ---
			// Extract directly into primitives to avoid `Pair` object allocation
			val startTargetBit = columnInfo.positions[0]
			val startPushDirection = columnInfo.pushDirections.first

			movesBuffer.add(
				PackedMove.Single(
					0u.packPossibleBitMove(
						piece = piece,
						columnInfoIndex = colIndex,
						targetBit = startTargetBit,
						pushDirection = startPushDirection,
						moveType = MoveType.AddPiece,
					)
				)
			)

			// --- END OF LINE MOVE ---
			// Extract directly into primitives to avoid `Pair` object allocation
			val endTargetBit = columnInfo.positions.last()
			val endPushDirection = columnInfo.pushDirections.second

			movesBuffer.add(
				PackedMove.Single(
					0u.packPossibleBitMove(
						piece = piece,
						columnInfoIndex = colIndex,
						targetBit = endTargetBit,
						pushDirection = endPushDirection,
						moveType = MoveType.AddPiece,
					)
				)
			)
		}
	}

	when (currentPlayer.name) {
		PlayerName.WHITE -> {
			if (blackZERTZ != 0UL) {
				getZertzMoves(currentPlayer, columnInfos, movesBuffer)
			}
			if (blackYINSH != 0UL) {
				getYinshMoves(currentPlayer, columnInfos, movesBuffer)
			}
			if (blackDVONNLayer[0] != 0UL) {
				getDvonnMoves(currentPlayer, columnInfos, movesBuffer)
			}
			if (blackPUNCTLayer[0] != 0UL) {
				getPunctMoves(currentPlayer, columnInfos, movesBuffer)
			}
		}
		PlayerName.BLACK -> {
			if (blackZERTZ != 0UL) {
				getZertzMoves(currentPlayer, columnInfos, movesBuffer)
			}
			if (blackYINSH != 0UL) {
				getYinshMoves(currentPlayer, columnInfos, movesBuffer)
			}
			if (blackDVONNLayer[0] != 0UL) {
				getDvonnMoves(currentPlayer, columnInfos, movesBuffer)
			}
			if (blackPUNCTLayer[0] != 0UL) {
				getPunctMoves(currentPlayer, columnInfos, movesBuffer)
			}
		}
	}
}

fun Bitboard.generateMoves(
	currentPlayer: Player,
	turnPhase: TurnPhase,
	movesBuffer: MutableList<PackedMove>,
) {
	when (turnPhase) {
		TurnPhase.PieceRemoval -> {
			identifyPiecesToRemove(currentPlayer, removalsBuffer = movesBuffer)
		}
		TurnPhase.ExtraMove -> {
			getTamskMoves(currentPlayer, movesBuffer)
		}
		TurnPhase.PlayerInputWindow -> {
			identifyAvailableMoves(currentPlayer, movesBuffer = movesBuffer)
		}
	}
}

fun Bitboard.createPlayerPiecesWithPotentialPowerset(
	player: Player,
	columnInfos: List<ColumnInfo> = emptyList(),
	positions: List<ULong> = emptyList(),
	buffer: MutableList<List<UInt>> = mutableListOf(),
) {
	require(!(columnInfos.isEmpty() && positions.isEmpty())) { "There must be at least one column" }

	// TODO handle intersecting lines, but do i?
	val playerPotentials =
		when (player.name) {
			PlayerName.WHITE -> {
				// pieces with potential and not neutralized
				whitePotentials and whiteNeutralized.inv()
			}

			PlayerName.BLACK -> {
				blackPotentials and blackNeutralized.inv()
			}
		}

	val powerset: MutableList<ULong> = mutableListOf()

	for (column in columnInfos.indices) {
		val potentialMask = columnInfos[column].columnMask and playerPotentials

		var subset = potentialMask and playerPotentials
		while (subset != 0UL) {
			powerset.add(subset)

			subset = (subset - 1UL) and potentialMask
		}
	}

	for (position in positions) {
		val potentialMask = position and playerPotentials
		var subset = potentialMask
		while (subset != 0UL) {
			powerset.add(subset and potentialMask)

			subset = (subset - 1UL) and potentialMask
		}
	}

	for (subset in powerset) {
		while (subset != 0UL) {
			0u.packPossibleBitMove(
				targetBit = 1UL shl subset.countTrailingZeroBits(),
				moveType = MoveType.RetrieveCapturePieces,
			)
		}
	}

	/**
	 * TODO causes stack overflow error, but an empty list is necessary as a player can leave the
	 * stack in play TODO Minimax/MCTS — what it would be like to remove at least one of these pieces
	 * if all pieces have potentials use line score heuristic and pieces in reserve TODO return of a
	 * list containing different combinations of bit positions
	 */
}

fun Bitboard.identifyPiecesToRemove(player: Player, removalsBuffer: MutableList<PackedMove>) {
	val linesWithFourInARow = evaluateLinesForFourInARow(player)

	// TODO If one or more pieces (regardless of color)
	//  extend a row-of-4 without interruption, these pieces
	//  are considered to be part of the row and may also
	//  be removed.
	//  * see getDVONNmoves() for inverse
	//  * write tests

	// filter for fully occupied submasks and sum extensions
	// fullyPopulatedSubmasksPositions
	val fullyPopulatedPositions = linesWithFourInARow.flatMap { (_, _, positions, submasks, _, _, _) ->
		val occupiedBits =
			submasks
				.filter { submask -> (submask and globalOccupancy) == submask }
				.fold(0UL) { acc, mask ->
					acc or mask
				}

		val occupiedPositions = positions.filter { it and occupiedBits != 0UL }

		//        occupiedBits to occupiedPositions
		occupiedPositions
	}

	//  val fullyPopulatedPositions = fullyPopulatedSubmasksPositions.values.flatten()

	if (linesWithFourInARow.isNotEmpty()) {
		// TODO Ensure Rules are followed for stack piece retrieval
		val playerPiecesWithPotential =
			createPlayerPiecesWithPotentialPowerset(
				player,
				positions = fullyPopulatedPositions,
			)

		val playerPiecesNotNeutralizedWithoutPotential = fullyPopulatedPositions.filter { bitmask ->
			val result =
				when (player.name) {
					PlayerName.WHITE -> {
						// TODO FIX - includes neutralized pieces
						// pieces without potential and not neutralized
						(whitePieces and bitmask) == bitmask &&
								(whitePotentials.inv() and bitmask) == bitmask &&
								(whiteNeutralized.inv() and bitmask) == bitmask &&
								(blackNeutralized.inv() and bitmask) ==
								bitmask // for white pieces stacked on neutralized black pieces
					}

					PlayerName.BLACK -> {
						(blackPieces and bitmask) == bitmask &&
								(blackPotentials.inv() and bitmask) == bitmask &&
								(blackNeutralized.inv() and bitmask) == bitmask &&
								(whiteNeutralized.inv() and bitmask) ==
								bitmask // for black pieces stacked on neutralized black pieces
					}
				}
			result
		}

		/**
		 * TODO Note: An opponent’s stack of 2 potentials may also be left on the board. Create powerset
		 * for this
		 */
		val opponentPiecesAndNotNeutralized = fullyPopulatedPositions.filter { bitmask ->
			val result =
				when (player.name) {
					PlayerName.WHITE -> {
						// opponent pieces and not neutralized
						//	            (blackPotentials.inv() and bitmask) == bitmask &&
						(blackPieces and bitmask) == bitmask &&
								(blackNeutralized.inv() and bitmask) == bitmask &&
								(whiteNeutralized.inv() and bitmask) == bitmask
					}

					PlayerName.BLACK -> {
						//	            (whitePotentials.inv() and bitmask) == bitmask &&
						(whitePieces and bitmask) == bitmask &&
								(whiteNeutralized.inv() and bitmask) == bitmask &&
								(blackNeutralized.inv() and bitmask) == bitmask
					}
				}
			result
		}

		// stacked DVONN and PUNCT Pieces
		val neutralizedBitmasks =
			fullyPopulatedPositions
				.filter { bitmask ->
					(whiteNeutralized and bitmask) == bitmask || (blackNeutralized and bitmask) == bitmask
				}
				.distinct()

		// --- 1. PRE-CONDITION CHECKS ---
		if (neutralizedBitmasks.isNotEmpty()) {
			check(
				neutralizedBitmasks.any {
					!playerPiecesNotNeutralizedWithoutPotential.contains(it)
				}
			) {
				"playerPiecesNotNeutralizedWithoutPotential should not contain any bits from neutralizedBitmasks"
			}

			check(
				neutralizedBitmasks.any {
					!opponentPiecesAndNotNeutralized.contains(it)
				}
			) {
				"opponentPiecesAndNotNeutralized should not contain any bits from neutralizedBitmasks"
			}
		}

		if (
			playerPiecesNotNeutralizedWithoutPotential.isNotEmpty() &&
			playerPiecesWithPotential.isNotEmpty()
		) {
			check(
				playerPiecesNotNeutralizedWithoutPotential.any { bitmask ->
					playerPiecesWithPotential.any { pieces ->
						!pieces.contains(bitmask)
					}
				}
			) {
				"playerPiecesNotNeutralizedWithoutPotential should not contain any bits from playerPiecesWithPotential"
			}
		}

		val forcedRemovals = mutableListOf<UInt>()

		for (it in playerPiecesNotNeutralizedWithoutPotential) {
			forcedRemovals.add(
				0u.packPossibleBitMove(
					targetBit = it,
					moveType = MoveType.RetrieveCapturePieces,
				)
					.setRetrieveCapture(RetrieveCapture.RETRIEVE)
			)
		}
		for (it in opponentPiecesAndNotNeutralized) {
			// could have potentials
			forcedRemovals.add(
				0u.packPossibleBitMove(
					targetBit = it,
					moveType = MoveType.RetrieveCapturePieces,
				)
					.setRetrieveCapture(RetrieveCapture.CAPTURE)
			)
		}
		for (bitmask in neutralizedBitmasks) {
			forcedRemovals.add(
				0u.packPossibleBitMove(
					targetBit = bitmask,
					moveType = MoveType.RetrieveCapturePieces,
				)
					.setNeutralized(true)
			)
		}

		for (subset in playerPiecesWithPotential) {
			removalsBuffer.add(
				PackedMove.Multiple(
					subset.map { bitmask ->
						0u.packPossibleBitMove(
							targetBit = bitmask,
							moveType = MoveType.RetrieveCapturePieces,
						)
							.setRetrieveCapture(RetrieveCapture.RETRIEVE)
					} + forcedRemovals
				)
			)
		}

		// TODO confirm there is never an empty list
		if (playerPiecesWithPotential.isEmpty()) {
			removalsBuffer.add(PackedMove.Multiple(forcedRemovals))
		}

		removalsBuffer.removeIf { (it as PackedMove.Multiple).values.isEmpty() }
	}
}