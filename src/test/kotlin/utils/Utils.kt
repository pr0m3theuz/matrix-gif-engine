package utils

import kotlinx.serialization.json.Json
import org.example.model.columnInfos
import org.example.model.neighbouringBitsBitmasks
import org.example.model.openningSpotsLineMask
import org.example.model.threeRunSubmasks
import org.junit.jupiter.api.Test
import kotlin.collections.set

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

class Utils {
  @Test
  fun calculateNeighboringBits() {
    /*val a: List<MutableMap<ULong, MutableList<Pair<ULong, ULong>>>> = columnInfos.map { column ->
    	column.shiftPairs.groupByTo(mutableMapOf()) { it.first }
    }

    val b = mutableMapOf<ULong, MutableList<Pair<ULong, ULong>>>()

    columnInfos.forEach { column ->
    	column.shiftPairs
    			.groupByTo(mutableMapOf()) { it.first }
    			.forEach { (lng, pairs) ->
    				b[lng] = b[lng]?.plus(pairs)?.toMutableList() ?: pairs
    			}

    	column.shiftPairs
    		.groupByTo(mutableMapOf()) { it.second }
    		.forEach { (lng, pairs) ->
    			val pair = Pair(pairs.first().second, pairs.first().first)
    			b[lng] = b[lng]?.plus(pair)?.toMutableList() ?: mutableListOf(pair)
    		}
    }

    val c: MutableMap<ULong, ULong> = b.mapValuesTo(mutableMapOf()) {
    	it.value.sumOf { it.second }
    }*/

    val neighbouringBitsBitmasks: MutableMap<ULong, ULong> = mutableMapOf()

    for (column in columnInfos) {
      for ((a, b) in column.shiftPairs) {
        neighbouringBitsBitmasks[a] = neighbouringBitsBitmasks.getOrDefault(a, 0UL) or b
        neighbouringBitsBitmasks[b] = neighbouringBitsBitmasks.getOrDefault(b, 0UL) or a
      }
    }

    //    logger.info { "" + (Json.encodeToString(a)) }
    //    logger.info { "" + (Json.encodeToString(b)) }
    //    logger.info { "" + (Json.encodeToString(c)) }
    logger.info { "" + (Json.encodeToString(neighbouringBitsBitmasks)) }
  }

  @Test
  fun `count features`() {
    val TURNS_SINCE = 50 // 8
    val LIBERTIES = TURNS_SINCE + 8 // 6
    val LIBERTIES_AFTER = LIBERTIES + 6 // 6
    val RETRIVAL_SIZE = LIBERTIES_AFTER + 6 // 8
    val CAPTURE_SIZE = RETRIVAL_SIZE + 8 // 8
    val SELF_ATARI_SIZE = CAPTURE_SIZE + 8 // 8
    val CURRENT_PLAYER_COLOR = SELF_ATARI_SIZE + 1

    // features
    val features = CURRENT_PLAYER_COLOR + 1

    logger.info { "" + (features) }
    logger.info { "" }
  }

  @Test
  fun `run of three on the edge`() {
    val filteredList = threeRunSubmasks.filter { (it and openningSpotsLineMask) != 0UL }
    println(threeRunSubmasks.size)
    println()
    println(filteredList)
    println(filteredList.size)
    println(filteredList.fold(0UL) { a, b -> a or b })
  }

  @Test
  fun calculateRays() {
    val rays = mutableMapOf<ULong, ULong>()
    columnInfos
        .flatMap { it.positions }
        .forEach { position ->
          columnInfos.forEach { info ->
            rays[position] =
                rays.getOrDefault(position, 0UL) or
                    if (info.columnMask and position != 0UL) info.columnMask else 0UL
          }
        }
    logger.info { rays.toString() }

    val zertzRays = mutableMapOf<ULong, ULong>()
	  rays.forEach { (position, ray) ->
      zertzRays[position] =  (ray xor neighbouringBitsBitmasks.getOrDefault(position, 0UL)) or position
	  }
    logger.info { zertzRays.toString() }
  }

  @Test
  fun generateBits() {
    val linesThroughBit: List<MutableSet<ULong>> = List(40) { mutableSetOf() }
    val runsThroughBit: List<MutableSet<ULong>> = List(40) { mutableSetOf() }

    for (column in columnInfos) {
      for (bit in column.positions) {
        linesThroughBit[bit.countTrailingZeroBits()].add(column.columnMask)
      }
    }

    for (column in columnInfos) {
      for (bit in column.positions) {
        runsThroughBit[bit.countTrailingZeroBits()].addAll(column.submasks.filter { (bit and it) == bit })
      }
    }


    println()
    println(linesThroughBit)
    println()
    println(runsThroughBit)
    println(runsThroughBit.flatten().count())
  }


  @Test
  fun countRuns4() {
    println()
    println(
      columnInfos.flatMap { it.submasks }.count()
    )
  }
}
