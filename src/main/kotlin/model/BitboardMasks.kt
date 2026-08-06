@file:OptIn(ExperimentalUnsignedTypes::class)

package org.example.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

val boardCenterSpotMask = 0b0000000000000000000001000000000000000000.toULong()

val bitDistanceWeights =
    0.rangeTo(39).associateWith { k ->
      when (abs(k - 18)) {
        0 -> 0
        1,
        6,
        7 -> 2
        2,
        5,
        8,
        11,
        12,
        13 -> 2
        3,
        4,
        9,
        10,
        14,
        15,
        16,
        17,
        18 -> 3
        19,
        20,
        21 -> 4
        else -> error("Unknown node $k")
      }
    }.values.toIntArray()

val verticalLineIndexArray =
    listOf(
        listOf(0, 1, 2, 3), // 0b0000000000000000000000000000000000001111
        listOf(4, 5, 6, 7, 8), // 0b0000000000000000000000000000000111110000
        listOf(9, 10, 11, 12, 13, 14), // 0b0000000000000000000000000111111000000000
        listOf(15, 16, 17, 18, 19, 20, 21), // 0b0000000000000000001111111000000000000000
        listOf(22, 23, 24, 25, 26, 27), // 0b0000000000001111110000000000000000000000
        listOf(28, 29, 30, 31, 32), // 0b0000000111110000000000000000000000000000
        listOf(33, 34, 35, 36), // 0b0001111000000000000000000000000000000000
        listOf(37, 38, 39), // 0b1110000000000000000000000000000000000000
    )

val upwardRightLineIndexArray =
    listOf(
        listOf(0, 5, 11, 18, 25, 31, 36), // 0b0001000010000010000001000000100000100001
        listOf(1, 6, 12, 19, 26, 32), // 0b0000000100000100000010000001000001000010
        listOf(2, 7, 13, 20, 27), // 0b0000000000001000000100000010000010000100
        listOf(3, 8, 14, 21), // 0b0000000000000000001000000100000100001000
        listOf(4, 10, 17, 24, 30, 35, 39), // 0b1000100001000001000000100000010000010000
        listOf(9, 16, 23, 29, 34, 38), // 0b0100010000100000100000010000001000000000
        listOf(15, 22, 28, 33, 37), // 0b0010001000010000010000001000000000000000
    )

val downwardRightLineIndexArray =
    listOf(
        listOf(0, 4, 9, 15), // 0b0000000000000000000000001000001000010001
        listOf(1, 5, 10, 16, 22), // 0b0000000000000000010000010000010000100010
        listOf(2, 6, 11, 17, 23, 28), // 0b0000000000010000100000100000100001000100
        listOf(3, 7, 12, 18, 24, 29, 33), // 0b0000001000100001000001000001000010001000
        listOf(8, 13, 19, 25, 30, 34, 37), // 0b0010010001000010000010000010000100000000
        listOf(14, 20, 26, 31, 35, 38), // 0b0100100010000100000100000100000000000000
        listOf(21, 27, 32, 36, 39), // 0b1001000100001000001000000000000000000000
    )

val lineMasks =
    listOf(
        // verticalLineMasks
        0b0000000000000000000000000000000000001111
            .toULong(), // listOf(0, 1, 2, 3),                    //
        0b0000000000000000000000000000000111110000
            .toULong(), // listOf(4, 5, 6, 7, 8),                 //
        0b0000000000000000000000000111111000000000
            .toULong(), // listOf(9, 10, 11, 12, 13, 14),         //
        0b0000000000000000001111111000000000000000
            .toULong(), // listOf(15, 16, 17, 18, 19, 20, 21),    //
        0b0000000000001111110000000000000000000000
            .toULong(), // listOf(22, 23, 24, 25, 26, 27),        //
        0b0000000111110000000000000000000000000000
            .toULong(), // listOf(28, 29, 30, 31, 32),            //
        0b0001111000000000000000000000000000000000
            .toULong(), // listOf(33, 34, 35, 36),                //
        0b1110000000000000000000000000000000000000
            .toULong(), // listOf(37, 38, 39),                    //
        /// val upwardRightLineMasks =
        //	listOf(
        0b0001000010000010000001000000100000100001
            .toULong(), // listOf(0, 5, 11, 18, 25, 31, 36),      //
        0b0000000100000100000010000001000001000010
            .toULong(), // listOf(1, 6, 12, 19, 26, 32),          //
        0b0000000000001000000100000010000010000100
            .toULong(), // listOf(2, 7, 13, 20, 27),              //
        0b0000000000000000001000000100000100001000
            .toULong(), // listOf(3, 8, 14, 21),                  //
        0b1000100001000001000000100000010000010000
            .toULong(), // listOf(4, 10, 17, 24, 30, 35, 39),     //
        0b0100010000100000100000010000001000000000
            .toULong(), // listOf(9, 16, 23, 29, 34, 38),         //
        0b0010001000010000010000001000000000000000
            .toULong(), // listOf(15, 22, 28, 33, 37),            //
        // val downwardRightLineMasks =
        //	listOf(
        0b0000000000000000000000001000001000010001
            .toULong(), // listOf(0, 4, 9, 15),                   //
        0b0000000000000000010000010000010000100010
            .toULong(), // listOf(1, 5, 10, 16, 22),              //
        0b0000000000010000100000100000100001000100
            .toULong(), // listOf(2, 6, 11, 17, 23, 28),          //
        0b0000001000100001000001000001000010001000
            .toULong(), // listOf(3, 7, 12, 18, 24, 29, 33),      //
        0b0010010001000010000010000010000100000000
            .toULong(), // listOf(8, 13, 19, 25, 30, 34, 37),     //
        0b0100100010000100000100000100000000000000
            .toULong(), // listOf(14, 20, 26, 31, 35, 38),        //
        0b1001000100001000001000000000000000000000
            .toULong(), // listOf(21, 27, 32, 36, 39),            //
    )

val openningSpotsLineMask = 0b1111001100011000011000001100001100011111.toULong()

@Serializable
data class ColumnInfo(
    var index: Int = 0,
    val columnMask: ULong,
    //	val destinationMask: ULong,
    val positions: List<ULong>,
    val submasks: List<ULong>,
    val shiftPairs: List<Pair<ULong, ULong>>, // (fromMask, toMask)
    val lineOrientation: LineOrientation,
    val pushDirections: Pair<PushDirection, PushDirection>,
) {
  // Helper to format as binary with a prefix.
  private fun ULong.toBin() = "0b" + this.toString(radix = 2)

  override fun toString(): String {
    // Format the list of pairs customly
    val formattedPairs =
        shiftPairs.joinToString(prefix = "[", postfix = "]") { (from, to) ->
          "(${from.toBin()} -> ${to.toBin()})"
        }

    // destinationMask = ${destinationMask.toBin()},
    return """
            ColumnInfo(
                columnMask = ${columnMask.toBin()},
                shiftPairs = $formattedPairs
            )
        """
        .trimIndent()
  }
}

val columnInfos: List<ColumnInfo> =
  (verticalLineIndexArray.map { arr ->
      ColumnInfo(
          columnMask = arr.fold(0UL) { acc, i -> acc or (1UL shl i) },
          //		destinationMask = 1UL shl arr.last(),
          positions =
              arr.indices.map { k ->
                1UL shl arr[k]
              },
          submasks =
              arr.indices
                  .map { k ->
                    1UL shl arr[k]
                  }
                  .windowed(4)
                  .map { sublist ->
                    sublist.fold(0UL) { acc, lng ->
                      acc or lng
                    }
                  },
          shiftPairs =
              (0 until arr.size - 1).map { k ->
                (1UL shl arr[k]) to (1UL shl arr[k + 1])
              },
          lineOrientation = LineOrientation.VERTICAL,
          pushDirections = Pair(PushDirection.UP, PushDirection.DOWN),
      )
    } +
        upwardRightLineIndexArray.map { arr ->
          ColumnInfo(
              columnMask = arr.fold(0UL) { acc, i -> acc or (1UL shl i) },
              //		destinationMask = 1UL shl arr.last(),

              positions =
                  arr.indices.map { k ->
                    1UL shl arr[k]
                  },
              submasks =
                  arr.indices
                      .map { k ->
                        1UL shl arr[k]
                      }
                      .windowed(4)
                      .map { sublist ->
                        sublist.fold(0UL) { acc, lng ->
                          acc or lng
                        }
                      },
              shiftPairs =
                  (0 until arr.size - 1).map { k ->
                    (1UL shl arr[k]) to (1UL shl arr[k + 1])
                  },
              lineOrientation = LineOrientation.UPWARD_RIGHT,
              pushDirections = Pair(PushDirection.UPPER_RIGHT, PushDirection.LOWER_LEFT),
          )
        } +
        downwardRightLineIndexArray.map { arr ->
          ColumnInfo(
              columnMask = arr.fold(0UL) { acc, i -> acc or (1UL shl i) },
              //		destinationMask = 1UL shl arr.last(),
              positions =
                  arr.indices.map { k ->
                    1UL shl arr[k]
                  },
              submasks =
                  arr.indices
                      .map { k ->
                        1UL shl arr[k]
                      }
                      .windowed(4)
                      .map { sublist ->
                        sublist.fold(0UL) { acc, lng ->
                          acc or lng
                        }
                      },
              shiftPairs =
                  (0 until arr.size - 1).map { k ->
                    (1UL shl arr[k]) to (1UL shl arr[k + 1])
                  },
              lineOrientation = LineOrientation.DOWNWARD_RIGHT,
              pushDirections = Pair(PushDirection.LOWER_RIGHT, PushDirection.UPPER_LEFT),
          )
        }).mapIndexed { index, info -> info.copy(index = index) }

val neighbouringBitsBitmasks: Map<ULong, ULong> = calculateNeighbouringBitmasks()

fun calculateNeighbouringBitmasks(): Map<ULong, ULong> {

  val neighbouringBitsBitmasks: MutableMap<ULong, ULong> = mutableMapOf()

  for (column in columnInfos) {
    for ((a, b) in column.shiftPairs) {
      neighbouringBitsBitmasks[a] = neighbouringBitsBitmasks.getOrDefault(a, 0UL) or b
      neighbouringBitsBitmasks[b] = neighbouringBitsBitmasks.getOrDefault(b, 0UL) or a
    }
  }

  return neighbouringBitsBitmasks.toMap()
}

val clusterArray = neighbouringBitsBitmasks.values.toULongArray()

val threeRunSubmasks: List<ULong> = columnInfos.flatMap { (_, _, positions, _, _, _, _) ->
  positions.windowed(3).map { sublist ->
    sublist.fold(0UL) { acc, lng ->
      acc or lng
    }
  }
}

val reducedThreeRunSubmasks: List<ULong> =
    listOf(
        112UL,
        448UL,
        3584UL,
        28672UL,
        229376UL,
        3670016UL,
        29360128UL,
        234881024UL,
        1879048192UL,
        7516192768UL,
        60129542144UL,
        120259084288UL,
        2081UL,
        70900514816UL,
        4162UL,
        4362600448UL,
        8324UL,
        135274496UL,
        16648UL,
        2113792UL,
        132112UL,
        585189294080UL,
        8454656UL,
        292594647040UL,
        272662528UL,
        146297323520UL,
        529UL,
        33296UL,
        1058UL,
        4260864UL,
        2116UL,
        276955136UL,
        4232UL,
        9143582720UL,
        532736UL,
        155692564480UL,
        68173824UL,
        311385128960UL,
        4431282176UL,
        622770257920UL,
    )

