package utils

import ncsa.hdf.hdf5lib.HDF5Constants
import ncsa.hdf.`object`.h5.H5File
import org.example.engine.ExperienceBuffer
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.D3Array
import org.jetbrains.kotlinx.multik.ndarray.data.D4Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class ExperienceBufferTest {

	@TempDir
	lateinit var tempDir: Path

	private lateinit var filePath: String

	@BeforeEach
	fun setUp() {
		filePath = tempDir.resolve("experience_test.h5").absolutePathString()
	}

	@AfterEach
	fun tearDown() {
		// TempDir is auto-cleaned by JUnit, but explicit deletion guards against locked handles
		val f = Path.of(filePath)
		if (f.exists()) {
			f.toFile().delete()
		}
	}

	private fun newH5File(mode: Int = HDF5Constants.H5F_ACC_TRUNC): H5File {
		return H5File(filePath, mode)
	}

	private fun sampleBuffer(
		episodes: Int = 2,
		steps: Int = 3,
		rows: Int = 4,
		cols: Int = 5,
	): ExperienceBuffer {
		val statesFlat = IntArray(episodes * steps * rows * cols) { it }
		val actionsFlat = IntArray(episodes * steps * rows) { it * 2 }
		val rewardsFlat = IntArray(episodes * steps) { it - 10 }

		val states: D4Array<Int> = mk.ndarray(statesFlat, episodes, steps, rows, cols)
		val actions: D3Array<Int> = mk.ndarray(actionsFlat, episodes, steps, rows)
		val rewards: D2Array<Int> = mk.ndarray(rewardsFlat, episodes, steps)

		return ExperienceBuffer(states = states, actions = actions, rewards = rewards)
	}

	@Test
	fun `serialize creates file on disk`() {
		val buffer = sampleBuffer()
		val h5File = newH5File()

		buffer.serialize(h5File)

		assertTrue(Path.of(filePath).exists(), "H5 file should exist after serialize")
	}

	@Test
	fun `serialize creates experience group with expected datasets`() {
		val buffer = sampleBuffer()
		val h5File = newH5File()
		buffer.serialize(h5File)

		val readFile = newH5File(HDF5Constants.H5F_ACC_RDONLY)
		readFile.open()
		try {
			assertNotNull(readFile.get("/experience"), "experience group should exist")
			assertNotNull(readFile.get("/experience/states"), "states dataset should exist")
			assertNotNull(readFile.get("/experience/actions"), "actions dataset should exist")
			assertNotNull(readFile.get("/experience/rewards"), "rewards dataset should exist")
		} finally {
			readFile.close()
		}
	}

	@Test
	fun `round trip preserves shapes`() {
		val episodes = 2
		val steps = 3
		val rows = 4
		val cols = 5
		val original = sampleBuffer(episodes, steps, rows, cols)

		val writeFile = newH5File()
		original.serialize(writeFile)

		val readFile = newH5File(HDF5Constants.H5F_ACC_RDONLY)
		val loaded = original.loadExperience(readFile)

		assertArrayEquals(intArrayOf(episodes, steps, rows, cols), loaded.states.shape)
		assertArrayEquals(intArrayOf(episodes, steps, rows), loaded.actions.shape)
		assertArrayEquals(intArrayOf(episodes, steps), loaded.rewards.shape)
	}

	@Test
	fun `round trip preserves values`() {
		val original = sampleBuffer()

		val writeFile = newH5File()
		original.serialize(writeFile)

		val readFile = newH5File(HDF5Constants.H5F_ACC_RDONLY)
		val loaded = original.loadExperience(readFile)

		assertD4ArrayEquals(original.states, loaded.states)
		assertD3ArrayEquals(original.actions, loaded.actions)
		assertD2ArrayEquals(original.rewards, loaded.rewards)
	}

	@Test
	fun `round trip preserves negative values in rewards`() {
		// sampleBuffer already includes negative rewards via `it - 10`; assert explicitly
		val original = sampleBuffer(episodes = 1, steps = 5)

		val writeFile = newH5File()
		original.serialize(writeFile)

		val readFile = newH5File(HDF5Constants.H5F_ACC_RDONLY)
		val loaded = original.loadExperience(readFile)

		var hasNegative = false
		for (i in 0 until loaded.rewards.shape[0]) {
			for (j in 0 until loaded.rewards.shape[1]) {
				if (loaded.rewards[i, j] < 0) hasNegative = true
			}
		}
		assertTrue(hasNegative, "expected at least one negative reward to round-trip correctly")
		assertD2ArrayEquals(original.rewards, loaded.rewards)
	}

	@Test
	fun `single episode single step buffer round trips correctly`() {
		val original = sampleBuffer(episodes = 1, steps = 1, rows = 2, cols = 2)

		val writeFile = newH5File()
		original.serialize(writeFile)

		val readFile = newH5File(HDF5Constants.H5F_ACC_RDONLY)
		val loaded = original.loadExperience(readFile)

		assertD4ArrayEquals(original.states, loaded.states)
		assertD3ArrayEquals(original.actions, loaded.actions)
		assertD2ArrayEquals(original.rewards, loaded.rewards)
	}

	@Test
	fun `serialize closes file handle so it can be reopened`() {
		val buffer = sampleBuffer()
		val h5File = newH5File()
		buffer.serialize(h5File)

		// If serialize failed to close, this reopen would throw or deadlock depending on backend
		val reopened = newH5File(HDF5Constants.H5F_ACC_RDONLY)
		assertDoesNotThrow {
			reopened.open()
			reopened.close()
		}
	}

	// --- helpers ---

	private fun assertD4ArrayEquals(expected: D4Array<Int>, actual: D4Array<Int>) {
		assertArrayEquals(expected.shape, actual.shape, "D4Array shapes differ")
		val (d0, d1, d2, d3) = expected.shape
		for (i in 0 until d0) for (j in 0 until d1) for (k in 0 until d2) for (l in 0 until d3) {
			assertEquals(
				expected[i, j, k, l],
				actual[i, j, k, l],
				"Mismatch at [$i,$j,$k,$l]"
			)
		}
	}

	private fun assertD3ArrayEquals(expected: D3Array<Int>, actual: D3Array<Int>) {
		assertArrayEquals(expected.shape, actual.shape, "D4Array shapes differ")
		val (d0, d1, d2) = expected.shape
		for (i in 0 until d0) for (j in 0 until d1) for (k in 0 until d2) {
			assertEquals(
				expected[i, j, k],
				actual[i, j, k],
				"Mismatch at [$i,$j,$k]"
			)
		}
	}

	private fun assertD2ArrayEquals(expected: D2Array<Int>, actual: D2Array<Int>) {
		assertArrayEquals(expected.shape, actual.shape, "D2Array shapes differ")
		val (d0, d1) = expected.shape
		for (i in 0 until d0) for (j in 0 until d1) {
			assertEquals(expected[i, j], actual[i, j], "Mismatch at [$i,$j]")
		}
	}

//	private operator fun IntArray.component4(): Int = this[3]
}