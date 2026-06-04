package org.example

import org.example.engine.determineWinner
import org.example.engine.evaluateCapturedPieces
import org.example.engine.evaluatePiecesInReserve
import org.example.engine.getEligiblePotentialMoves
import org.example.engine.playerTurn
import org.example.model.State
import org.example.model.assertPieceCount
import org.example.model.initializeState
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.IntStream

class GameSimulationTest {

    @Test
    fun `simulate 10000 games concurrently`() {
        val gamesCompleted = AtomicInteger(0)

        IntStream.range(0, 10000).parallel().forEach { i ->
            try {
                var gameState: State = initializeState()
                var turn = 0

                while (!evaluateCapturedPieces(gameState) || !evaluatePiecesInReserve(gameState)) {
                    turn++

                    // Prevent infinite loops in random game simulations
                    if (turn > 5000) {
                        break
                    }

                    if ((evaluateCapturedPieces(gameState) || evaluatePiecesInReserve(gameState)) &&
                        getEligiblePotentialMoves(gameState).isEmpty()) {
                        break
                    }

                    try {
                        gameState = playerTurn(gameState)
                    } catch (e: IllegalStateException) {
                        if (e.message == "Player has no available moves left!") {
                            // Natural end of game when no moves are left
                            break
                        } else {
                            throw e
                        }
                    }

                    gameState.assertPieceCount(EXPECTED_TOTAL, MAXIMUM_PIECES)
                    gameState = gameState.rotatePlayers()

                    check(gameState.currentPlayer.name != gameState.nextPlayer.name) {
                        "Turn rotation failure: The current player and the next player are both '${gameState.currentPlayer.name}'."
                    }
                }

                determineWinner(gameState)
                gamesCompleted.incrementAndGet()
            } catch (e: Exception) {
                System.err.println("Game $i failed with exception: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }

        println("Successfully completed ${gamesCompleted.get()} games concurrently.")
        assert(gamesCompleted.get() == 10000) { "Not all 10000 games completed successfully." }
    }
}
