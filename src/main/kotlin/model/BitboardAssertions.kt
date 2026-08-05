@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import kotlinx.serialization.json.Json

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

fun Bitboard.assertPieceCount(
	EXPECTED_TOTAL: Int = 66 / 2,
	MAXIMUM_PIECES: Int = 66,
	currentPlayer: Player,
	nextPlayer: Player,
) {

	//  currentPlayer.piecesInReserve.sortBy { it.extractPieceType() }
	//  nextPlayer.piecesInReserve.sortBy { it.extractPieceType() }

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
	var boardBasics =
		blackGIPF.countOneBits() +
				blackZERTZ.countOneBits() +
				blackTAMSK.countOneBits() +
				blackYINSH.countOneBits()

	var boardStacks =
		blackDVONNLayer[0].countOneBits() +
				blackDVONNLayer[2].countOneBits() +
				blackDVONNLayer[4].countOneBits() +
				blackDVONNLayer[6].countOneBits() +
				blackPUNCTLayer[0].countOneBits() +
				blackPUNCTLayer[2].countOneBits() +
				blackPUNCTLayer[4].countOneBits() +
				blackPUNCTLayer[6].countOneBits() +
				whiteDVONNLayer[1].countOneBits() +
				whiteDVONNLayer[3].countOneBits() +
				whiteDVONNLayer[5].countOneBits() +
				whiteDVONNLayer[7].countOneBits() +
				whitePUNCTLayer[1].countOneBits() +
				whitePUNCTLayer[3].countOneBits() +
				whitePUNCTLayer[5].countOneBits() +
				whitePUNCTLayer[7].countOneBits()

	var boardPotentials = blackPotentials.countOneBits()

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

	if (totalBlackPieces > EXPECTED_TOTAL) {
		logger.info { "" + ("oooooooooooooo") }
	}

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
       │   ├── GIPF:  ${blackGIPF.countOneBits()}
       │   ├── ZERTZ: ${blackZERTZ.countOneBits()}
       │   ├── TAMSK: ${blackTAMSK.countOneBits()}
       │   └── YINSH: ${blackYINSH.countOneBits()}
       └── Stacks (Layered/hidden pieces)
           ├── DVONN (Black layers 0,2,4): ${blackDVONNLayer[0].countOneBits() + blackDVONNLayer[2].countOneBits() + blackDVONNLayer[4].countOneBits()}
           ├── DVONN (White layers 1,3,5): ${whiteDVONNLayer[1].countOneBits() + whiteDVONNLayer[3].countOneBits() + whiteDVONNLayer[5].countOneBits()}
           ├── PUNCT (Black layers 0,2,4): ${blackPUNCTLayer[0].countOneBits() + blackPUNCTLayer[2].countOneBits() + blackPUNCTLayer[4].countOneBits()}
           └── PUNCT (White layers 1,3,5): ${whitePUNCTLayer[1].countOneBits() + whitePUNCTLayer[3].countOneBits() + whitePUNCTLayer[5].countOneBits()}
           
    SUMMARY EVALUATION:
    - Player Total Weight: ${nextReservePotentials + nextReserveBasics + nextCapturedPotentials + nextCapturedBasics + currentReservePotentials + currentReserveBasics + currentCapturedPotentials + currentCapturedBasics}
    - Board Total Weight:  ${boardPotentials + boardBasics + boardStacks}
    - Bitboard State: ${Json.encodeToString<Bitboard>(this)}
    - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.WHITE) currentPlayer else nextPlayer)}
    - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.BLACK) currentPlayer else nextPlayer)}

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
	boardBasics =
		whiteGIPF.countOneBits() +
				whiteZERTZ.countOneBits() +
				whiteTAMSK.countOneBits() +
				whiteYINSH.countOneBits()

	boardStacks =
		whiteDVONNLayer[0].countOneBits() +
				whiteDVONNLayer[2].countOneBits() +
				whiteDVONNLayer[4].countOneBits() +
				whiteDVONNLayer[6].countOneBits() +
				whitePUNCTLayer[0].countOneBits() +
				whitePUNCTLayer[2].countOneBits() +
				whitePUNCTLayer[4].countOneBits() +
				whitePUNCTLayer[6].countOneBits() +
				blackDVONNLayer[1].countOneBits() +
				blackDVONNLayer[3].countOneBits() +
				blackDVONNLayer[5].countOneBits() +
				blackDVONNLayer[7].countOneBits() +
				blackPUNCTLayer[1].countOneBits() +
				blackPUNCTLayer[3].countOneBits() +
				blackPUNCTLayer[5].countOneBits() +
				blackPUNCTLayer[7].countOneBits()

	boardPotentials = whitePotentials.countOneBits()

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

	if (totalWhitePieces > EXPECTED_TOTAL) {
		logger.info { "" + ("oooooooooooooo") }
	}

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
      CRITICAL STATE CORRUPTION: Total White piece weight ($totalWhitePieces) != Expected ($EXPECTED_TOTAL)
      
      1. NEXT PLAYER (WHITE)
         ├── Reserve
         │   ├── Potentials: $nextReserveRaw (weighted: $nextReservePotentials)
         │   └── Basics:     $nextReserveBasics
         └── Captured (By Next Player)
             ├── Potentials: $nextCapturedRaw (weighted: $nextCapturedPotentials)
             └── Basics:     $nextCapturedBasics
             
      2. CURRENT PLAYER (BLACK/OTHER)
         ├── Reserve (White pieces held)
         │   ├── Potentials: $currentReserveRaw (weighted: $currentReservePotentials)
         │   └── Basics:     $currentReserveBasics
         └── Captured (White pieces captured)
             ├── Potentials: $currentCapturedRaw (weighted: $currentCapturedPotentials)
             └── Basics:     $currentCapturedBasics
             
      3. ACTIVE BOARD
         ├── Potentials (Weighted): $boardPotentials
         ├── Flat Basics (Single pieces on board)
         │   ├── GIPF:  ${whiteGIPF.countOneBits()}
         │   ├── ZERTZ: ${whiteZERTZ.countOneBits()}
         │   ├── TAMSK: ${whiteTAMSK.countOneBits()}
         │   └── YINSH: ${whiteYINSH.countOneBits()}
         └── Stacks (Layered/hidden pieces)
             ├── DVONN (White layers 0,2,4): ${whiteDVONNLayer[0].countOneBits() + whiteDVONNLayer[2].countOneBits() + whiteDVONNLayer[4].countOneBits()}
             ├── DVONN (Black layers 1,3,5): ${blackDVONNLayer[1].countOneBits() + blackDVONNLayer[3].countOneBits() + blackDVONNLayer[5].countOneBits()}
             ├── PUNCT (White layers 0,2,4): ${whitePUNCTLayer[0].countOneBits() + whitePUNCTLayer[2].countOneBits() + whitePUNCTLayer[4].countOneBits()}
             └── PUNCT (Black layers 1,3,5): ${blackPUNCTLayer[1].countOneBits() + blackPUNCTLayer[3].countOneBits() + blackPUNCTLayer[5].countOneBits()}
             
      SUMMARY EVALUATION:
      - Player Total Weight: ${nextReservePotentials + nextReserveBasics + nextCapturedPotentials + nextCapturedBasics + currentReservePotentials + currentReserveBasics + currentCapturedPotentials + currentCapturedBasics}
      - Board Total Weight:  ${boardPotentials + boardBasics + boardStacks}
      - Bitboard State: ${Json.encodeToString<Bitboard>(this)}
      - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.WHITE) currentPlayer else nextPlayer)}
      - Player: ${Json.encodeToString<Player>(if (currentPlayer.name == PlayerName.BLACK) currentPlayer else nextPlayer)}

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

fun Bitboard.validatePieceRemoval(removedPieces: List<RetrievedCapturedPieceBit>) {
	// Does not handle stack pieces
	fun safeRemoveAndCheck(board: ULong, boardName: String, bitmask: ULong, piece: Piece): Boolean {
		return (board and bitmask) == 0UL
	}

	val map = removedPieces.associateWith { removedPiece ->
		val bitmask = removedPiece.bitmask
		val piece = removedPiece.retrievedPiece ?: removedPiece.capturedPiece

		val hasBeenRemoved =
			when (piece?.colorName) {
				PlayerName.WHITE -> {
					when (piece.type) {
						PieceType.NULL -> {
							true
						}
						PieceType.GIPF -> {
							safeRemoveAndCheck(whiteGIPF, "White GIPF", bitmask, piece)
						}

						PieceType.TAMSK -> {
							safeRemoveAndCheck(whiteTAMSK, "White TAMSK", bitmask, piece)
						}

						PieceType.ZERTZ -> {
							safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", bitmask, piece)
						}

						PieceType.DVONN -> {
							if (piece.stackedPieces.isNotEmpty()) {
								throw IllegalStateException("")
							} else {
								safeRemoveAndCheck(whiteDVONNLayer[0], "White DVONN [0]", bitmask, piece)
							}
						}

						PieceType.YINSH -> {
							safeRemoveAndCheck(whiteYINSH, "White YINSH", bitmask, piece)
						}

						PieceType.PUNCT -> {
							if (piece.stackedPieces.isNotEmpty()) {
								throw IllegalStateException("")
							} else {
								safeRemoveAndCheck(whitePUNCTLayer[0], "White PUNCT [0]", bitmask, piece)
							}
						}
					}
				}
				PlayerName.BLACK -> {
					when (piece.type) {
						PieceType.NULL -> {
							true
						}
						PieceType.GIPF -> {
							safeRemoveAndCheck(blackGIPF, "Black GIPF", bitmask, piece)
						}

						PieceType.TAMSK -> {
							safeRemoveAndCheck(blackTAMSK, "Black TAMSK", bitmask, piece)
						}

						PieceType.ZERTZ -> {
							safeRemoveAndCheck(blackZERTZ, "Black ZERTZ", bitmask, piece)
						}

						PieceType.DVONN -> {
							if (piece.stackedPieces.isNotEmpty()) {
								throw IllegalStateException("")
							} else {
								safeRemoveAndCheck(blackDVONNLayer[0], "Black DVONN [0]", bitmask, piece)
							}
						}

						PieceType.YINSH -> {
							safeRemoveAndCheck(blackYINSH, "Black YINSH", bitmask, piece)
						}

						PieceType.PUNCT -> {
							if (piece.stackedPieces.isNotEmpty()) {
								throw IllegalStateException("")
							} else {
								safeRemoveAndCheck(blackPUNCTLayer[0], "Black PUNCT [0]", bitmask, piece)
							}
						}
					}
				}
				else -> false
			}

		hasBeenRemoved
	}

	check(map.all { it.value }) {
		buildString {
			map.filter { !it.value }
				.forEach { (bit, _) ->
					appendLine("$bit was not removed")
				}
		}
	}
}

fun Bitboard.validateBoardConversion(convertedBoard: Board) {}
