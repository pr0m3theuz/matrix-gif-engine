package utils

import kotlinx.serialization.json.Json
import org.example.model.columnInfos
import org.junit.jupiter.api.Test

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

}
