package org.example.ui

import androidx.lifecycle.ViewModel
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.example.ai.doActionGetTurnPhase
import org.example.ai.mcts.PackedMove
import org.example.ai.mcts.createChildState
import org.example.engine.Winner
import org.example.engine.determineWinner
import org.example.engine.evaluateCapturedPieces
import org.example.engine.playerMove
import org.example.model.*

enum class GameStatus {
  Init,
  Running,
  Completed,
}

data class MainUiState(
    val status: GameStatus = GameStatus.Init,
    val gameState: State? = null,
    val turnCount: Int = 0,
    val turnPhase: TurnPhase = TurnPhase.PlayerInputWindow,
    val availableMoves: List<PackedMove> = emptyList(),
    val winner: Winner? = null,
    val previousTurnPhases: List<TurnPhase> = emptyList(),
    val playerWhoMadeTheLastMove: Player? = null,
)

sealed class MainUiEvent {

  data class StartNewGame(
      val whitePlayer: Player,
      val blackPlayer: Player,
  ) : MainUiEvent()

  data class SelectMove(val move: PackedMove) : MainUiEvent()

  data class ReplayGame(val a: Boolean = true) : MainUiEvent()

  data class NavigateToMainMenu(val a: Boolean = true) : MainUiEvent()
}

class MainViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(MainUiState())
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

  fun onEvent(event: MainUiEvent) {
    when (event) {
      is MainUiEvent.NavigateToMainMenu -> {
        _uiState.update {
          MainUiState()
        }
      }
      is MainUiEvent.StartNewGame -> {
        _uiState.update {
          it.copy(
              status = GameStatus.Running,
              gameState =
                  initializeState(
                      event.whitePlayer,
                      event.blackPlayer,
                  ),
          )
        }

        initGame()
      }
      is MainUiEvent.SelectMove -> {
        applyMove(event.move)
      }
      is MainUiEvent.ReplayGame -> {
        val whitePlayer =
            if (_uiState.value.gameState?.currentPlayer?.name == PlayerName.WHITE) {
              _uiState.value.gameState?.currentPlayer
            } else _uiState.value.gameState?.nextPlayer

        val blackPlayer =
            if (_uiState.value.gameState?.currentPlayer?.name == PlayerName.BLACK) {
              _uiState.value.gameState?.currentPlayer
            } else _uiState.value.gameState?.nextPlayer

        requireNotNull(whitePlayer)
        requireNotNull(blackPlayer)

        _uiState.update {
          it.copy(
              status = GameStatus.Running,
              gameState =
                  initializeState(
                      whitePlayer.copy(
                          piecesInReserve = mutableListOf(),
                          capturedPieces = mutableListOf(),
                      ),
                      blackPlayer.copy(
                          piecesInReserve = mutableListOf(),
                          capturedPieces = mutableListOf(),
                      ),
                  ),
            turnCount = 0,
            turnPhase = TurnPhase.PlayerInputWindow,
            availableMoves = emptyList(),
            winner = null,
            previousTurnPhases = emptyList(),
            playerWhoMadeTheLastMove = null,
          )
        }

        initGame()
      }
    }
  }

  val rng = Random(42)
  var playerWhoMadeTheLastMove: Player? = null

  private fun initGame() {
    val state = _uiState.value.gameState?.deepCopy()

    requireNotNull(state)

    val availableMoves: MutableList<PackedMove> = mutableListOf()

    state.bitboard.generateMoves(state.currentPlayer, TurnPhase.PlayerInputWindow, availableMoves)

    _uiState.update {
      it.copy(
          turnCount = 1,
          turnPhase = TurnPhase.PlayerInputWindow,
          availableMoves = availableMoves,
      )
    }
  }

  private fun playerTurn(state: State, turn: Int, rng: Random) {
    var newState = state // .deepCopy(copyCollector = true)

    if (newState.bitboard.evaluateLinesForFourInARow(state.currentPlayer).isNotEmpty()) {
      val availableMoves: MutableList<PackedMove> = mutableListOf()

      newState.bitboard.generateMoves(
          newState.currentPlayer,
          TurnPhase.PieceRemoval,
          availableMoves,
      )

      _uiState.update {
        it.copy(
            turnPhase = TurnPhase.PieceRemoval,
            availableMoves = availableMoves,
        )
      }

      newState = playerMove(newState, turnPhase = TurnPhase.PieceRemoval, turn, rng)

      // recombine player pieces
      newState.currentPlayer.combinePieces()

      _uiState.update {
        it.copy(
            gameState = newState,
            playerWhoMadeTheLastMove = newState.currentPlayer,
        )
      }

      if (evaluateCapturedPieces(newState)) return
    }

    newState.assertPieceCount()

    // Handle Tamsk Potential
    if (
        when (newState.currentPlayer.name) {
          PlayerName.WHITE -> {
            newState.bitboard.whiteTAMSK and
                newState.bitboard.whitePotentials and
                boardCenterSpotMask
          }
          PlayerName.BLACK -> {
            newState.bitboard.blackTAMSK and
                newState.bitboard.blackPotentials and
                boardCenterSpotMask
          }
        } == boardCenterSpotMask
    ) {
      val availableMoves: MutableList<PackedMove> = mutableListOf()

      newState.bitboard.generateMoves(newState.currentPlayer, TurnPhase.ExtraMove, availableMoves)

      _uiState.update {
        it.copy(turnPhase = TurnPhase.ExtraMove)
      }
      newState = playerMove(newState, TurnPhase.ExtraMove, turn, rng)

      _uiState.update {
        it.copy(
            gameState = newState,
            playerWhoMadeTheLastMove = newState.currentPlayer,
        )
      }
    }

    newState.assertPieceCount()

    val availableMoves = mutableListOf<PackedMove>()
    newState.bitboard.identifyAvailableMoves(newState.currentPlayer, columnInfos, availableMoves)

    if (availableMoves.isNotEmpty()) {
      _uiState.update {
        it.copy(
            turnPhase = TurnPhase.PlayerInputWindow,
            availableMoves = availableMoves,
        )
      }

      newState =
          if (newState.currentPlayer.model != Model.HUMAN) {
            playerMove(newState, turnPhase = TurnPhase.PlayerInputWindow, turn, rng)
          } else {
            newState
          }

      newState.assertPieceCount()

      _uiState.update {
        it.copy(
            gameState = newState,
            playerWhoMadeTheLastMove = newState.currentPlayer,
        )
      }
    }

    // Handle Tamsk Potential
    if (
        when (newState.currentPlayer.name) {
          PlayerName.WHITE -> {
            newState.bitboard.whiteTAMSK and
                newState.bitboard.whitePotentials and
                boardCenterSpotMask
          }
          PlayerName.BLACK -> {
            newState.bitboard.blackTAMSK and
                newState.bitboard.blackPotentials and
                boardCenterSpotMask
          }
        } == boardCenterSpotMask
    ) {
      val availableMoves: MutableList<PackedMove> = mutableListOf()
      newState.bitboard.generateMoves(newState.currentPlayer, TurnPhase.ExtraMove, availableMoves)

      _uiState.update {
        it.copy(
            turnPhase = TurnPhase.ExtraMove,
            availableMoves = availableMoves,
        )
      }

      newState = playerMove(newState, turnPhase = TurnPhase.ExtraMove, turn, rng)

      _uiState.update {
        it.copy(gameState = newState)
      }
    }

    newState.assertPieceCount()

    // TODO While there are pieces to remove
    //  TODO Has a bug? what bug?
    if (newState.bitboard.evaluateLinesForFourInARow(state.currentPlayer).isNotEmpty()) {
      val availableMoves: MutableList<PackedMove> = mutableListOf()
      newState.bitboard.generateMoves(newState.currentPlayer, TurnPhase.ExtraMove, availableMoves)

      _uiState.update {
        it.copy(
            turnPhase = TurnPhase.PieceRemoval,
            availableMoves = availableMoves,
        )
      }

      newState = playerMove(newState, turnPhase = TurnPhase.PieceRemoval, turn, rng)
      // recombine player pieces
      newState.currentPlayer.combinePieces()

      _uiState.update {
        it.copy(
            gameState = newState,
            playerWhoMadeTheLastMove = newState.currentPlayer,
        )
      }
    }

    newState.assertPieceCount()

    //  newState.board.printHexGrid("END OF TURN")

    val (nodeCurrentPlayer, nodeNextPlayer, nodeTurnPhase, nodeMoves) =
        createChildState(
            childBitboard = newState.bitboard,
            childCurrentPlayer = state.currentPlayer,
            childNextPlayer = state.nextPlayer,
            turnPhase = TurnPhase.PieceRemoval,
            turnHasHadNormalMove = true,
        )

    val newBoard = newState.bitboard.convertBitboardToBoard(state.board)

    _uiState.update {
      it.copy(
          gameState =
              State(
                  currentPlayer = nodeCurrentPlayer,
                  nextPlayer = nodeNextPlayer,
                  board = newBoard,
                  bitboard = newState.bitboard,
                  turnMoves = state.turnMoves,
                  turnDuration = state.turnDuration,
                  turnSearchInfo = state.turnSearchInfo,
                  collector = state.collector,
              ),
          turnPhase = nodeTurnPhase,
          turnCount = _uiState.value.turnCount + 1,
          availableMoves = nodeMoves,
      )
    }
  }

  private fun applyMove(packedMove: PackedMove) {
    val state = _uiState.value.gameState?.deepCopy()

    requireNotNull(state)

    val bitboard = state.bitboard.deepCopy()

    val turnPhase =
        doActionGetTurnPhase(
            packedMove,
            bitboard,
            state.currentPlayer,
            state.nextPlayer,
        )

    state.turnMoves.getOrDefault(_uiState.value.turnCount, mutableListOf()).add(packedMove)

    _uiState.update {
      it.copy(previousTurnPhases = _uiState.value.previousTurnPhases + _uiState.value.turnPhase)
    }

    // child Node Moves
    val (nodeCurrentPlayer, nodeNextPlayer, nodeTurnPhase, nodeMoves) =
        createChildState(
            bitboard,
            state.currentPlayer,
            state.nextPlayer,
            turnPhase,
            _uiState.value.previousTurnPhases.any { it == TurnPhase.PlayerInputWindow },
        )

    bitboard.assertPieceCount(
        currentPlayer = state.currentPlayer,
        nextPlayer = state.nextPlayer,
    )

    val newBoard = bitboard.convertBitboardToBoard(state.board)

    val newState =
        State(
            currentPlayer = nodeCurrentPlayer,
            nextPlayer = nodeNextPlayer,
            board = newBoard,
            bitboard = bitboard,
            turnMoves = state.turnMoves,
            turnDuration = state.turnDuration,
            turnSearchInfo = state.turnSearchInfo,
            collector = state.collector,
        )

    newState.assertPieceCount(bitboard = bitboard)

    _uiState.update {
      it.copy(
          gameState = newState,
          turnPhase = nodeTurnPhase,
          availableMoves = nodeMoves,
      )
    }

    if (evaluateCapturedPieces(newState)) {
      val winner =
          determineWinner(
              currentPlayer = nodeCurrentPlayer,
              nextPlayer = nodeNextPlayer,
              playerWhoMadeTheLastMove = state.currentPlayer,
              bitboard = bitboard,
              state = newState,
              true,
          )

      _uiState.update {
        it.copy(
            winner = winner,
            status = GameStatus.Completed,
        )
      }

      return
    }

    // todo check is nodesMoves is empty declare winner
    if (nodeMoves.isEmpty()) {
      val winner =
          determineWinner(
              currentPlayer = nodeCurrentPlayer,
              nextPlayer = nodeNextPlayer,
              playerWhoMadeTheLastMove = _uiState.value.playerWhoMadeTheLastMove,
              bitboard = bitboard,
              state = newState,
              true,
          )

      _uiState.update {
        it.copy(
            winner = winner,
            status = GameStatus.Completed,
        )
      }
      return
    }

    if (nodeCurrentPlayer.model != Model.HUMAN) {
      _uiState.update {
        it.copy(
            turnCount = _uiState.value.turnCount + 1,
        )
      }

      playerTurn(
          newState,
          _uiState.value.turnCount,
          rng,
      )

      if (evaluateCapturedPieces(newState) || _uiState.value.availableMoves.isEmpty()) {
        val winner =
            determineWinner(
                currentPlayer = nodeCurrentPlayer,
                nextPlayer = nodeNextPlayer,
                playerWhoMadeTheLastMove = _uiState.value.playerWhoMadeTheLastMove,
                bitboard = bitboard,
                state = newState,
                true,
            )

        _uiState.update {
          it.copy(
              winner = winner,
              status = GameStatus.Completed,
          )
        }
        return
      }
    }
  }
}
