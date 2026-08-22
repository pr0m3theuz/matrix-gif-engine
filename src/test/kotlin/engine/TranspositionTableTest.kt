package org.example.engine

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranspositionTableTest {

    @Test
    fun testTranspositionTableProbe() {
        val tt = TranspositionTable()
        val entry = TransitionTableEntry(key = 1L, move = 2u, score = 3, depth = 4, bound = Bound.EXACT, generation = 1)
        tt.save(entry, 1L, 3, Bound.EXACT, 4, null)

        val start = System.currentTimeMillis()
        var foundCount = 0
        for (i in 0L..1000000L) {
            val (found, res) = tt.probe(i)
            if (found) foundCount++
        }
        val end = System.currentTimeMillis()

        println("Probe time for 1,000,000 iterations: ${end - start} ms")
        assertTrue(foundCount > 0)
    }

    @Test
    fun testTranspositionTablePerformance2() {
        val tt = TranspositionTable()

        // When table is full, it will NOT iterate through the entire table anymore
        val startFill = System.currentTimeMillis()
        for (i in 0 until 100000) {
            val (f, e) = tt.probe((i + 1).toLong())
            tt.save(e, (i + 1).toLong(), 0, Bound.EXACT, 1, null)
        }
        val endFill = System.currentTimeMillis()
        println("Fill time for 100,000 items: ${endFill - startFill} ms")

        val startProbe = System.currentTimeMillis()
        for (i in 1..100000) {
            val (found, entry) = tt.probe(-1L)
        }
        val endProbe = System.currentTimeMillis()
        println("Probe time for 100,000 misses when filled: ${endProbe - startProbe} ms")
    }
}
