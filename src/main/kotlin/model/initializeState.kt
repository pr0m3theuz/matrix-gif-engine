@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

fun initializeState(
  playerOne: Player,
  playerTwo: Player
): State {
  val centerCoordinate = Coordinate(column = 'E', row = 5)

  // Create pieces
  val whitePieces = createPlayerPieces(playerOne)
  val blackPieces = createPlayerPieces(playerTwo)

  playerOne.piecesInReserve.addAll(whitePieces)
  playerTwo.piecesInReserve.addAll(blackPieces)

  val players = listOf<Player>(playerOne, playerTwo)

  // Create Nodes
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

  // Create board

  val board = Board(nodes = nodes, centerNodeCoordinate = centerCoordinate)

  // Create lines

  return State(
    currentPlayer = playerOne,
      nextPlayer = playerTwo,
      //		whitePlayer = whitePlayer,
      //		blackPlayer = blackPlayer,
      board = board,
      bitboard = Bitboard(),
      //      lines = constructLines(nodes = nodes),
  )
}

fun createPlayerPieces(player: Player): List<UInt> {
  val pieces = mutableListOf<UInt>()

  val gipf =
      List(3) {
        0u.createPiece(PieceType.GIPF, player.name, false)
        //        Piece(
        //            abbreviation = player.abbreviation.plus('G'),
        //            potential = false,
        ////            color = player.color,
        //            colorName = player.name,
        //            type = PieceType.GIPF,
        //        )
      }

  val tamsk =
      List(3) {
        0u.createPiece(PieceType.TAMSK, player.name, true)
        //        Piece(
        //            abbreviation = player.abbreviation.plus('T'),
        //            potential = true,
        ////            color = player.color,
        //            colorName = player.name,
        //            type = PieceType.TAMSK,
        //        )
      }

  val zertz =
      List(3) {
        0u.createPiece(PieceType.ZERTZ, player.name, true)
        //	      Piece(
        //            abbreviation = player.abbreviation.plus('Z'),
        //            potential = true,
        ////            color = player.color,
        //            colorName = player.name,
        //            type = PieceType.ZERTZ,
        //        )
      }

  val yinsh =
      List(3) {
        0u.createPiece(PieceType.YINSH, player.name, true)

        //	      Piece(
        //            abbreviation = player.abbreviation.plus('Y'),
        //            potential = true,
        ////            color = player.color,
        //            colorName = player.name,
        //            type = PieceType.YINSH,
        //        )
      }

  val dvonn =
      List(3) {
        0u.createPiece(PieceType.DVONN, player.name, true)

        //	      Piece(
        //            abbreviation = player.abbreviation.plus('D'),
        //            potential = true,
        ////            color = player.color,
        //            colorName = player.name,
        //            type = PieceType.DVONN,
        //        )
      }

  val punct =
      List(3) {
        0u.createPiece(PieceType.PUNCT, player.name, true)

        //	      Piece(
        //            abbreviation = player.abbreviation.plus('P'),
        //            potential = true,
        ////            color = player.color,
        //            colorName = player.name,
        //            type = PieceType.PUNCT,
        //        )
      }

  pieces.addAll(gipf)
  pieces.addAll(tamsk)
  pieces.addAll(zertz)
  pieces.addAll(yinsh)
  pieces.addAll(dvonn)
  pieces.addAll(punct)

  return pieces
}
