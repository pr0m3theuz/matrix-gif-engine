@file:OptIn(ExperimentalUnsignedTypes::class)

package model

import kotlin.math.exp
import org.example.model.Bitboard
import org.example.model.neighbouringBitsBitmasks
import org.example.model.threeRunSubmasks
import org.junit.jupiter.api.Test

class ScoreLinesTest {
  @Test
  fun scoreRunsOfThree() {
    val bitboard: Bitboard =
        Bitboard(
            whiteGIPF = 8454145UL,
            whiteDVONNLayer = ulongArrayOf(549755813888UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(17039376UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 134217738UL,
            whiteYINSH = 9663676416UL,
            whiteZERTZ = 4328521728UL,
            whitePotentials = 558530560026UL,
            blackGIPF = 34361835520UL,
            blackDVONNLayer = ulongArrayOf(1280UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(2151677952UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 69323456512UL,
            blackYINSH = 137438953568UL,
            blackZERTZ = 268568576UL,
            blackPotentials = 71475137792UL,
        )

    var whiteClusters: Double = 0.0
    var blackClusters: Double = 0.0

    for ((index, mask) in threeRunSubmasks.withIndex()) {
      if ((bitboard.whitePieces and mask) == mask) {
        val whiteScore =
            exp((bitboard.whitePieces and mask).countOneBits().toDouble()) - 1.0
        whiteClusters += whiteScore
      }
      if ((bitboard.blackPieces and mask) == mask) {
        val blackScore =
            exp((bitboard.blackPieces and mask).countOneBits().toDouble()) - 1.0
        blackClusters += blackScore
      }
      //      println("White Run $index: $whiteClusters. score: $whiteScore")
      //      println("Black Run $index: $blackClusters. score: $blackScore")
    }

    println("White Clusters: $whiteClusters. White Pieces: ${bitboard.whitePieces.countOneBits()}")
    println("Black Clusters: $blackClusters. Black Pieces: ${bitboard.blackPieces.countOneBits()}")
  }

  @Test
  fun scoreClusters() {
    val bitboard: Bitboard =
        Bitboard(
            whiteGIPF = 8454145UL,
            whiteDVONNLayer = ulongArrayOf(549755813888UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whitePUNCTLayer = ulongArrayOf(17039376UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            whiteTAMSK = 134217738UL,
            whiteYINSH = 9663676416UL,
            whiteZERTZ = 4328521728UL,
            whitePotentials = 558530560026UL,
            blackGIPF = 34361835520UL,
            blackDVONNLayer = ulongArrayOf(1280UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackPUNCTLayer = ulongArrayOf(2151677952UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL, 0UL),
            blackTAMSK = 69323456512UL,
            blackYINSH = 137438953568UL,
            blackZERTZ = 268568576UL,
            blackPotentials = 71475137792UL,
        )

    var whiteClusters: Double = 0.0
    var blackClusters: Double = 0.0
    for (cluster in neighbouringBitsBitmasks.values) {
      val whiteScore =
          exp((bitboard.whitePieces and cluster).countOneBits().toDouble().div(cluster.countOneBits())) - 1.0
      val blackScore =
          exp((bitboard.blackPieces and cluster).countOneBits().toDouble().div(cluster.countOneBits())) - 1.0

      whiteClusters += whiteScore
      blackClusters += blackScore

      //      println("White Clusters: $whiteClusters. score: $whiteScore")
      //      println("Black Clusters: $blackClusters. score: $blackScore")
    }

    println("White Clusters: $whiteClusters. White Pieces: ${bitboard.whitePieces.countOneBits()}")
    println("Black Clusters: $blackClusters. Black Pieces: ${bitboard.blackPieces.countOneBits()}")
  }
}
