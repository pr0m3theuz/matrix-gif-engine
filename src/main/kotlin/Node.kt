package org.example

import kotlin.math.abs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Node(
    val coordinate: Coordinate,
    var neighbors: NodeNeighbors? = null,
    val isDot: Boolean,
    val isSpot: Boolean, // play area
    val isCenter: Boolean,
    var piece: Piece? = null,
//    var isNeutralized: Boolean =
//        false, // neutralized // TODO Implement logic for stacking pieces and removing potential and
               // neutralizing pieces
    // TODO Implement logic // For DVONN & PÜNCT pieces, needs to be a mutableList of alternating
    // player pieces, use add() and removeLast(), this piece which can either be retrieved or
    // captured
    // TODO check that pieces alternate
//    var stackedPieces: MutableList<Piece> = mutableListOf(),
) {
  fun deepCopy(): Node {
    val string = Json.encodeToString(serializer(), this)
    return Json.decodeFromString(serializer(), string)
  }

  fun removePiece() {
    piece = null
  }
}

fun populateNeighbors(coordinate: Coordinate, nodes: Set<Node>): NodeNeighbors {
  // Dots are only connected the adjacent Spots
  val letterIndex = LETTERS.indexOf(coordinate.column)
  val centerLetterIndex = LETTERS.indexOf('E')
  val row = coordinate.row

  val columns =
      LETTERS.slice(
          letterIndex.minus(if (coordinate.column == LETTERS.first()) 0 else 1)..letterIndex.plus(
                  if (coordinate.column == LETTERS.last()) 0 else 1
              )
      )

  val above = Coordinate(column = coordinate.column, row = row.plus(1))
  val below = Coordinate(column = coordinate.column, row = row.minus(1))
  val upperRight =
      Coordinate(
          column = columns.last(),
          // A, B, C, D plus 1; E, F, G, H, I plus 0
          row = row.plus(if (LETTERS.indexOf(columns.last()) > centerLetterIndex) 0 else 1),
      )
  val lowerRight =
      Coordinate(
          column = columns.last(),
          // A, B, C, D minus 0; E, F, G, H, I minus 1
          row = row.minus(if (LETTERS.indexOf(columns.last()) > centerLetterIndex) 1 else 0),
      )
  val upperLeft =
      Coordinate(
          column = columns.first(),
          // A, B, C, D plus 0; E, F, G, H, I plus 1
          row = row.plus(if (LETTERS.indexOf(columns.first()) < centerLetterIndex) 0 else 1),
      )
  val lowerLeft =
      Coordinate(
          column = columns.first(),
          // A, B, C, D minus 1; E, F, G, H, I minus 0
          row = row.minus(if (LETTERS.indexOf(columns.first()) < centerLetterIndex) 1 else 0),
      )

  return NodeNeighbors(
      above = if (nodes.any { it.coordinate == above && it.isSpot }) above else null,
      below = if (nodes.any { it.coordinate == below && it.isSpot }) below else null,
      upperRight = if (nodes.any { it.coordinate == upperRight && it.isSpot }) upperRight else null,
      lowerRight = if (nodes.any { it.coordinate == lowerRight && it.isSpot }) lowerRight else null,
      upperLeft = if (nodes.any { it.coordinate == upperLeft && it.isSpot }) upperLeft else null,
      lowerLeft = if (nodes.any { it.coordinate == lowerLeft && it.isSpot }) lowerLeft else null,
  )
}

@Serializable
data class NodeNeighbors(
    val above: Coordinate?,
    val below: Coordinate?,
    val upperRight: Coordinate?,
    val lowerRight: Coordinate?,
    val upperLeft: Coordinate?,
    val lowerLeft: Coordinate?,
)

fun NodeNeighbors.getNeighbours(): List<Coordinate> {
  val list = mutableListOf<Coordinate>()
  this.above?.let { list.add(it) }
  this.below?.let { list.add(it) }
  this.upperRight?.let { list.add(it) }
  this.lowerRight?.let { list.add(it) }
  this.lowerLeft?.let { list.add(it) }
  this.upperLeft?.let { list.add(it) }

  return list
}

fun NodeNeighbors.getAvailablePushDirections(): List<PushDirection> {
  val list = mutableListOf<PushDirection>()
  this.above?.let { list.add(PushDirection.UP) }
  this.below?.let { list.add(PushDirection.DOWN) }
  this.upperRight?.let { list.add(PushDirection.UPPER_RIGHT) }
  this.lowerRight?.let { list.add(PushDirection.LOWER_RIGHT) }
  this.lowerLeft?.let { list.add(PushDirection.LOWER_LEFT) }
  this.upperLeft?.let { list.add(PushDirection.UPPER_LEFT) }

  return list
}

fun NodeNeighbors.getNeighborFromPushDirection(pushDirection: PushDirection): Coordinate? {
  return when (pushDirection) {
    PushDirection.UP -> this.above
    PushDirection.DOWN -> this.below
    PushDirection.UPPER_RIGHT -> this.upperRight
    PushDirection.LOWER_RIGHT -> this.lowerRight
    PushDirection.UPPER_LEFT -> this.upperLeft
    PushDirection.LOWER_LEFT -> this.lowerLeft
  }
}

fun NodeNeighbors.getPushDirectionFromNeighbor(neighbor: Coordinate): PushDirection? {
  return when (neighbor) {
    this.above -> PushDirection.UP
    this.below -> PushDirection.DOWN
    this.upperRight -> PushDirection.UPPER_RIGHT
    this.lowerRight -> PushDirection.LOWER_RIGHT
    this.upperLeft -> PushDirection.UPPER_LEFT
    this.lowerLeft -> PushDirection.LOWER_LEFT
    else -> {
      null
    }
  }
}

fun constructNodes(
    letters: String,
    rows: List<Int>,
    centerLetterIndex: Int,
    center: Coordinate,
): Set<Node> {
  val nodes = mutableSetOf<Node>()
  letters.forEach { letter ->
    // A B C D E F G H I J
    // 0 1 2 3 4 5 6 7 8 9
    // 9 - ABS(9-4) or 9 - ABS(0-4)
    val rowRange = 1..rows.last().minus(abs(letters.indexOf(letter).minus(centerLetterIndex)))

    rowRange.forEach { row ->
      val coordinate =
          Coordinate(
              column = letter,
              row = row,
          )
      val isDot =
          row == rowRange.first ||
              row == rowRange.last ||
              letter == letters.first() ||
              letter == letters.last()

      nodes.add(
          Node(
              coordinate = coordinate,
              neighbors = null,
              isDot = isDot,
              isSpot = !isDot,
              isCenter = coordinate == center,
              piece = null,
          )
      )
    }
  }

  return nodes
}

data class NodeConnections(
    val node: Node,
    val neighbours: MutableSet<Node> = mutableSetOf(),
)
