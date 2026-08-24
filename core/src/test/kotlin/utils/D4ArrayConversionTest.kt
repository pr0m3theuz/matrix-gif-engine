package utils

import org.example.engine.toD4Array
import org.example.engine.toPaddedD2Array
import org.example.engine.toPaddedD4Array
import org.jetbrains.kotlinx.multik.api.d2arrayIndices
import org.jetbrains.kotlinx.multik.api.io.writeNPZ
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.time.Clock.System.now

class D4ArrayConversionTest {

	@Test
	fun `toD4Array preserves shape and element values across all 4 dimensions`() {
		// Distinct dimensions to ensure no axis/index transposition
		val d1 = 2
		val d2 = 3
		val d3 = 4
		val d4 = 5

		// Populate with unique positional values (e.g. 1234 for i=1, j=2, k=3, l=4)
		val rawList: List<List<D2Array<Int>>> = List(d1) { i ->
			List(d2) { j ->
				mk.d2arrayIndices(d3, d4) { k, l ->
					i * 1000 + j * 100 + k * 10 + l
				}
			}
		}

		val result = rawList.toD4Array()

//		mk.writeNPZ(
//			Path("test_${now()}.npz"), result, result, result
//		)

		// 1. Verify exact 4D shape
		assertArrayEquals(intArrayOf(d1, d2, d3, d4), result.shape)

		// 2. Verify element mapping accuracy
		for (i in 0 until d1) {
			for (j in 0 until d2) {
				for (k in 0 until d3) {
					for (l in 0 until d4) {
						val expectedValue = i * 1000 + j * 100 + k * 10 + l
						assertEquals(
							expected = expectedValue,
							actual = result[i, j, k, l],
							message = "Mismatch at index [$i, $j, $k, l]"
						)
					}
				}
			}
		}
	}

	@Test
	fun `toD4Array converts minimal 1x1x1x1 input correctly`() {
		val expectedValue = 99
		val rawList = listOf(
			listOf(
				mk.d2arrayIndices(1, 1) { _, _ -> expectedValue }
			)
		)

		val result = rawList.toD4Array()

		assertArrayEquals(intArrayOf(1, 1, 1, 1), result.shape)
		assertEquals(expectedValue, result[0, 0, 0, 0])
	}

	@Test
	fun `toD4Array throws IndexOutOfBoundsException when outer list is empty`() {
		val emptyOuterList = emptyList<List<D2Array<Int>>>()

		assertThrows<NoSuchElementException> {
			emptyOuterList.toD4Array()
		}
	}

	@Test
	fun `toD4Array throws IndexOutOfBoundsException when inner list is empty`() {
		val emptyInnerList = listOf(emptyList<D2Array<Int>>())

		assertThrows<IndexOutOfBoundsException> {
			emptyInnerList.toD4Array()
		}
	}
}