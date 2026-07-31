package org.example.model

fun convertBoardToBitboard(board: Board): Bitboard {
  var whiteGIPF: ULong = 0UL
  val whiteDVONNLayer: ULongArray = ULongArray(8)
  val whitePUNCTLayer: ULongArray = ULongArray(8)
  var whiteTAMSK: ULong = 0UL
  var whiteYINCH: ULong = 0UL
  var whiteZERTZ: ULong = 0UL
  var whitePotentials: ULong = 0UL

  var blackGIPF: ULong = 0UL
  val blackDVONNLayer: ULongArray = ULongArray(8)
  val blackPUNCTLayer: ULongArray = ULongArray(8)
  var blackTAMSK: ULong = 0UL
  var blackYINCH: ULong = 0UL
  var blackZERTZ: ULong = 0UL
  var blackPotentials: ULong = 0UL

  board.nodes
      .filter { node -> node.isSpot }
      .forEach { node: Node ->
        node.piece?.let { piece ->
          when (piece.colorName) {
            PlayerName.WHITE -> {
              when (piece.type) {
                PieceType.NULL -> {}
                PieceType.GIPF -> {
                  whiteGIPF = whiteGIPF or node.bitmask
                }

                PieceType.TAMSK -> {
                  whiteTAMSK = whiteTAMSK or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask
                }

                PieceType.ZERTZ -> {
                  whiteZERTZ = whiteZERTZ or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask
                }

                PieceType.YINSH -> {
                  whiteYINCH = whiteYINCH or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask
                }

                PieceType.DVONN -> {
                  whiteDVONNLayer[0] = whiteDVONNLayer[0] or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask

                  // stackedPieces start on layer[1] (the 2nd Dvonn layer)
                  piece.stackedPieces.forEachIndexed { stackIndex, piece ->
                    val layerIndex = stackIndex + 1
                    /* white pieces on even layers */
                    if (layerIndex % 2 == 0) {
                      check(piece.colorName == PlayerName.WHITE) { "" }
                    }
                    /* black pieces on odd layers */
                    if (layerIndex % 2 == 1) {
                      check(piece.colorName == PlayerName.BLACK) { "" }
                    }

                    whiteDVONNLayer[layerIndex] = whiteDVONNLayer[layerIndex] or node.bitmask
                  }
                }

                PieceType.PUNCT -> {
                  whitePUNCTLayer[0] = whitePUNCTLayer[0] or node.bitmask
                  if (piece.potential) whitePotentials = whitePotentials or node.bitmask

                  // stackedPieces start on layer[1] (the 2nd Dvonn layer)
                  piece.stackedPieces.forEachIndexed { stackIndex, piece ->
                    val layerIndex = stackIndex + 1
                    /* white pieces on even layers */
                    if (layerIndex % 2 == 0) {
                      check(piece.colorName == PlayerName.WHITE) { "" }
                    }
                    /* black pieces on odd layers */
                    if (layerIndex % 2 == 1) {
                      check(piece.colorName == PlayerName.BLACK) { "" }
                    }

                    whitePUNCTLayer[layerIndex] = whitePUNCTLayer[layerIndex] or node.bitmask
                  }
                }
              }
            }

            PlayerName.BLACK -> {
              when (piece.type) {
                PieceType.NULL -> {}
                PieceType.GIPF -> {
                  blackGIPF = blackGIPF or node.bitmask
                }

                PieceType.TAMSK -> {
                  blackTAMSK = blackTAMSK or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask
                }

                PieceType.ZERTZ -> {
                  blackZERTZ = blackZERTZ or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask
                }

                PieceType.YINSH -> {
                  blackYINCH = blackYINCH or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask
                }

                PieceType.DVONN -> {
                  blackDVONNLayer[0] = blackDVONNLayer[0] or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask

                  // stackedPieces start on layer[1] (the 2nd Dvonn layer)
                  piece.stackedPieces.forEachIndexed { stackIndex, piece ->
                    val layerIndex = stackIndex + 1
                    /* black pieces on even layers */
                    if (layerIndex % 2 == 0) {
                      check(piece.colorName == PlayerName.BLACK) { "" }
                    }
                    /* white pieces on odd layers */
                    if (layerIndex % 2 == 1) {
                      check(piece.colorName == PlayerName.WHITE) { "" }
                    }

                    blackDVONNLayer[layerIndex] = blackDVONNLayer[layerIndex] or node.bitmask
                  }
                }

                PieceType.PUNCT -> {
                  blackPUNCTLayer[0] = blackPUNCTLayer[0] or node.bitmask
                  if (piece.potential) blackPotentials = blackPotentials or node.bitmask

                  // stackedPieces start on layer[1] (the 2nd Dvonn layer)
                  piece.stackedPieces.forEachIndexed { stackIndex, piece ->
                    val layerIndex = stackIndex + 1
                    /* black pieces on even layers */
                    if (layerIndex % 2 == 0) {
                      check(piece.colorName == PlayerName.BLACK) { "" }
                    }
                    /* black pieces on odd layers */
                    if (layerIndex % 2 == 1) {
                      check(piece.colorName == PlayerName.WHITE) { "" }
                    }

                    blackPUNCTLayer[layerIndex] = blackPUNCTLayer[layerIndex] or node.bitmask
                  }
                }
              }
            }
          }
        }
      }

  return Bitboard(
      whiteGIPF = whiteGIPF,
      whiteTAMSK = whiteTAMSK,
      whiteZERTZ = whiteZERTZ,
      whiteYINSH = whiteYINCH,
      whiteDVONNLayer = whiteDVONNLayer,
      whitePUNCTLayer = whitePUNCTLayer,
      whitePotentials = whitePotentials,
      blackGIPF = blackGIPF,
      blackTAMSK = blackTAMSK,
      blackYINSH = blackYINCH,
      blackZERTZ = blackZERTZ,
      blackDVONNLayer = blackDVONNLayer,
      blackPUNCTLayer = blackPUNCTLayer,
      blackPotentials = blackPotentials,
  )
}