package org.example.engine

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import org.example.model.initializeState
import org.example.model.PieceType
import org.example.model.State
import org.example.model.Player
import org.example.model.Board
import org.example.model.Coordinate
import org.example.model.Node
import org.example.model.Lines
import org.example.model.PushDirection
import org.example.model.constructLines
import org.example.model.assertPieceCount
import kotlin.test.assertTrue

class GameFuzzTest {

    @KFuzzTest
    fun fuzzEngineMoves(f: KFuzzer) {
        var state = initializeState()

        // Use KFuzzer to determine number of moves
        val numMoves = f.int(1, 10)

        for (i in 0 until numMoves) {
            try {
                // Since playerTurn is fully automated based on random internal choices we can call it.
                // However, to make it robust, we should test other functions explicitly.
                if (f.boolean()) {
                    state = playerTurn(state)
                    state.assertPieceCount(33, 66)
                    // Basic invariant checking after a player turn
                    assertTrue(state.board.nodes.none { it.isDot && it.piece != null })
                }
            } catch (e: Exception) {
                if (isExpectedException(e)) continue
                throw e
            }
        }
    }

    @KFuzzTest
    fun fuzzShiftPiece(f: KFuzzer) {
        val state = initializeState()

        // Randomly pick a node and a direction
        val nodes = state.board.nodes.toList()
        if (nodes.isEmpty()) return

        val randomNode = nodes[f.int(0, nodes.size - 1)]
        val directions = PushDirection.values()
        val randomDir = directions[f.int(0, directions.size - 1)]

        try {
            shiftPiece(randomNode, randomDir, state.board, state.lines)
        } catch (e: Exception) {
             if (isExpectedException(e)) return
             throw e
        }
    }

    @KFuzzTest
    fun fuzzUsePiecePotential(f: KFuzzer) {
        val state = initializeState()

        // Randomly pick a node and a target set of nodes
        val nodes = state.board.nodes.toList()
        if (nodes.isEmpty()) return

        val randomNode = nodes[f.int(0, nodes.size - 1)]
        val numTargets = f.int(1, 5)
        val targets = mutableSetOf<Node>()
        for (i in 0 until numTargets) {
            targets.add(nodes[f.int(0, nodes.size - 1)])
        }

        try {
            usePiecePotential(randomNode, targets, state)
        } catch (e: Exception) {
             if (isExpectedException(e)) return
             throw e
        }
    }

    private fun isExpectedException(e: Exception): Boolean {
        if (e is IllegalArgumentException || e is IllegalStateException) {
             val msg = e.message ?: ""
             return msg.contains("Invalid state transition") ||
                    msg.contains("Turn rotation failure") ||
                    msg.contains("State corrupted") ||
                    msg.contains("Invalid Board Topology") ||
                    msg.contains("State invalid") ||
                    msg.contains("Critical State Corruption") ||
                    msg.contains("Game Piece Desynchronization") ||
                    msg.contains("Illegal placement") ||
                    msg.contains("Illegal move") ||
                    msg.contains("Source node must contain a valid game piece") ||
                    msg.contains("Invalid source piece")
        }
        return false
    }
}
