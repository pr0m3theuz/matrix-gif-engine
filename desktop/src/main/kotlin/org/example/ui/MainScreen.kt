@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalUnsignedTypes::class,
)

package org.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import org.example.ai.mcts.PackedMove
import org.example.model.*
import org.example.model.State
import org.hexworks.mixite.core.api.HexagonOrientation
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridLayout
import org.hexworks.mixite.core.api.contract.SatelliteData
import org.jetbrains.skia.Image

@Composable
fun MainScreen(
    window: ComposeWindow,
    uiState: MainUiState,
    onEvent: (event: MainUiEvent) -> Unit,
) {
  //        val context = LocalContext.current

  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val stateVertical = rememberScrollState(0)
  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("MATRX GIPF") },
            actions = {
              TooltipArea(
                  tooltip = {
                    // Composable tooltip content:
                    Surface(
                        modifier = Modifier.shadow(4.dp),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                      Text(
                          text = "Exit to Main Menu",
                          modifier = Modifier.padding(10.dp),
                      )
                    }
                  },
                  modifier = Modifier.padding(start = 40.dp),
                  delayMillis = 100, // In milliseconds
                  tooltipPlacement =
                      TooltipPlacement.CursorPoint(
                          alignment = Alignment.BottomStart,
                          offset =
                              DpOffset(
                                  (-8).dp,
                                  (8).dp, // Tooltip offset
                              ),
                      ),
              ) {
                IconButton(
                    onClick = {
                      onEvent(MainUiEvent.NavigateToMainMenu())
                    }
                ) {
                  Icon(Icons.AutoMirrored.Default.ExitToApp, null)
                }
              }
            },
        )
      },
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { paddingValues ->
    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
    ) {
      Box(
          modifier =
              Modifier.fillMaxSize()
                  .verticalScroll(stateVertical)
                  .padding(end = 12.dp, bottom = 12.dp)
      ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            //      verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
        ) {
          if (uiState.status == GameStatus.Init) {
            MainMenuScreen(onEvent)
          }

          if (uiState.status == GameStatus.Running && uiState.gameState != null) {
            GameScreen(uiState, uiState.gameState, onEvent)
          }
        }
      }
      VerticalScrollbar(
          modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
          adapter = rememberScrollbarAdapter(stateVertical),
      )
    }
  }
}

@Composable
private fun GameScreen(uiState: MainUiState, gameState: State, onEvent: (MainUiEvent) -> Unit) {
  val openGameOverDialog = remember { mutableStateOf(false) }

  if (uiState.winner != null) {
    openGameOverDialog.value = true
  }

  if (openGameOverDialog.value) {
    GameOverDialog(
        onDismissRequest = {
          openGameOverDialog.value = false
          onEvent(MainUiEvent.NavigateToMainMenu())
        },
        onConfirmation = {
          openGameOverDialog.value = false
          onEvent(MainUiEvent.ReplayGame())
        },
        dialogTitle = "Game Over!",
        dialogText =
            "${uiState.winner?.winner?.name} won by ${uiState.winner?.winCondition?.message}!",
    )
  }

  Card(
      modifier = Modifier.padding(16.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
      Column {
        Text(
            "Turn Count: ${uiState.turnCount}",
            style = MaterialTheme.typography.bodyLargeEmphasized,
        )
      }
      Column {
        Text(
            "Turn Phase: ${uiState.turnPhase.name}",
            style = MaterialTheme.typography.bodyLargeEmphasized,
        )
      }
      Column {
        Text(
            "Current Player: ${gameState.currentPlayer.name}",
            style = MaterialTheme.typography.bodyLargeEmphasized,
        )
      }
      Column {
        Text(
            "Opponent: ${gameState.nextPlayer.name}",
            style = MaterialTheme.typography.bodyLargeEmphasized,
        )
      }
    }
  }

  var selectedMove by remember { mutableStateOf<PackedMove?>(null) }

  val nodesToHighlight =
      selectedMove?.let {
        when (it) {
          is PackedMove.Multiple -> {
            it.values.map { move ->
              bitmaskNodes.getValue(move.extractTargetBit()).coordinate
            }
          }
          is PackedMove.Single -> {
            val temp =
                mutableListOf(
                    bitmaskNodes.getValue(it.value.extractTargetBit()).coordinate,
                )
            if (it.value.extractMoveType() == MoveType.UsePotential) {
              temp.add(bitmaskNodes.getValue(it.value.extractSourceBit()).coordinate)
            }
            temp.toList()
          }
        }
      } ?: emptyList()

  // todo draw the board
  DrawBoard(uiState, nodesToHighlight)

  Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
        modifier = Modifier.weight(0.5f, fill = false).padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Pieces(gameState.currentPlayer.piecesInReserve.mapNotNull { it.extractPiece() }, "Reserve")

      if (gameState.currentPlayer.capturedPieces.isNotEmpty()) {
        Pieces(gameState.currentPlayer.capturedPieces.mapNotNull { it.extractPiece() }, "Captured")
      }
    }

    Column(
        modifier = Modifier.weight(0.5f, fill = false).padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Pieces(gameState.nextPlayer.piecesInReserve.mapNotNull { it.extractPiece() }, "Reserve")

      if (gameState.nextPlayer.capturedPieces.isNotEmpty()) {
        Pieces(gameState.nextPlayer.capturedPieces.mapNotNull { it.extractPiece() }, "Captured")
      }
    }
  }

  if (uiState.turnPhase != TurnPhase.PieceRemoval) {
    val (addPieceMoves, usePotentialMoves) =
        uiState.availableMoves.partition {
          (it as PackedMove.Single).value.extractMoveType() == MoveType.AddPiece
        }

    if (addPieceMoves.isNotEmpty()) {
      AddPieceMoveOptions(
          addPieceMoves,
          updateSelectedMove = {
            selectedMove = it
          },
      )
    }

    if (usePotentialMoves.isNotEmpty()) {
      UsePotentialMoves(
          usePotentialMoves,
          updateSelectedMove = {
            selectedMove = it
          },
      )
    }
  }

  if (uiState.turnPhase == TurnPhase.PieceRemoval && uiState.availableMoves.isNotEmpty()) {
    selectedMove = PieceRemovalMoves(uiState)
  }

  if (uiState.gameState?.currentPlayer?.model != Model.HUMAN && uiState.winner == null) {
    onEvent(MainUiEvent.UseAI())
  }

  Button(
      enabled =
          uiState.availableMoves.contains(selectedMove) &&
              uiState.gameState?.currentPlayer?.model == Model.HUMAN,
      onClick = {
        if (uiState.availableMoves.contains(selectedMove)) {
          selectedMove?.let { onEvent(MainUiEvent.SelectMove(it)) }
        }
      },
  ) {
    Text("Confirm Move")
  }
}

@Composable
fun GameOverDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
) {

  AlertDialog(
      title = {
        Text(text = dialogTitle, style = MaterialTheme.typography.headlineMediumEmphasized)
      },
      text = {
        Text(text = dialogText, style = MaterialTheme.typography.bodyMedium)
      },
      onDismissRequest = {
        onDismissRequest()
      },
      confirmButton = {
        TextButton(
            onClick = {
              onConfirmation()
            }
        ) {
          Text("Replay")
        }
      },
      dismissButton = {
        TextButton(
            onClick = {
              onDismissRequest()
            }
        ) {
          Text("Go to Main Menu")
        }
      },
  )
}

@Composable
private fun PieceRemovalMoves(uiState: MainUiState): PackedMove? {
  var selectedMove by remember { mutableStateOf<PackedMove?>(null) }

  Card(
      modifier = Modifier.padding(16.dp),
  ) {
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Retrieve/Capture Moves", style = MaterialTheme.typography.headlineSmallEmphasized)

      FlowRow(
          Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        uiState.availableMoves.forEach { move ->
          Row(
              verticalAlignment = Alignment.CenterVertically,
          ) {
            val nodes =
                (move as PackedMove.Multiple)
                    .values
                    .map { move ->
                      bitmaskNodes.getValue(move.extractTargetBit())
                    }
                    .distinct()

            val text = nodes.joinToString { it.coordinate.toString() }
            Checkbox(
                checked = selectedMove == move,
                onCheckedChange = {
                  selectedMove =
                      if (selectedMove == move) {
                        null
                      } else {
                        move
                      }
                },
            )
            Text(text)
          }
        }
      }
    }
  }
  return selectedMove
}

@Composable
private fun UsePotentialMoves(
    usePotentialMoves: List<PackedMove>,
    updateSelectedMove: (PackedMove?) -> Unit,
) {
  var selectedMove by remember { mutableStateOf<PackedMove?>(null) }

  Card(
      modifier = Modifier.padding(16.dp),
  ) {
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Use Potential Moves", style = MaterialTheme.typography.headlineSmallEmphasized)

      FlowRow(
          Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        usePotentialMoves.forEach { potential ->
          Row(
              verticalAlignment = Alignment.CenterVertically,
          ) {
            val move = (potential as PackedMove.Single).value
            val pieceType = move.extractPieceType()
            val sourceNode = bitmaskNodes.getValue(move.extractSourceBit())
            val targetNode = bitmaskNodes.getValue(move.extractTargetBit())

            val text = "${pieceType?.name}\n${sourceNode.coordinate} ➔ ${targetNode.coordinate}"

            Checkbox(
                checked = selectedMove == potential,
                onCheckedChange = {
                  selectedMove =
                      if (selectedMove == potential) {
                        null
                      } else {
                        potential
                      }

                  updateSelectedMove(selectedMove)
                },
            )
            Text(text)
          }
        }
      }
    }
  }
}

@Composable
private fun AddPieceMoveOptions(
    addPieceMoves: List<PackedMove>,
    updateSelectedMove: (PackedMove?) -> Unit,
) {
  var selectedMove by remember { mutableStateOf<PackedMove?>(null) }
  val pieceTypes =
      addPieceMoves
          .mapNotNull {
            (it as PackedMove.Single).value.extractPieceType()
          }
          .distinct()
          .sortedBy { it.ordinal }

  val pushDirections =
      addPieceMoves
          .mapNotNull {
            (it as PackedMove.Single).value.extractPushDirection()
          }
          .distinct()
          .sortedBy { it.ordinal }

  val targetPushDirections: MutableMap<ULong, MutableList<PushDirection?>> = mutableMapOf()

  for (it in addPieceMoves) {
    val move = (it as PackedMove.Single).value
    val list =
        targetPushDirections.getOrPut(
            move.extractTargetBit(),
            { mutableListOf<PushDirection?>() },
        )

    list.add(move.extractPushDirection())
  }

  val pushDirectionTargets: MutableMap<PushDirection, MutableList<ULong>> = mutableMapOf()

  for (it in addPieceMoves) {
    val move = (it as PackedMove.Single).value
    val pushDirection = move.extractPushDirection()
    if (pushDirection != null) {
      val list =
          pushDirectionTargets.getOrPut(
              pushDirection,
              { mutableListOf<ULong>() },
          )

      list.add(move.extractTargetBit())
    }
  }

  val targetNodes =
      addPieceMoves
          .map {
            (it as PackedMove.Single).value.extractTargetBit()
          }
          .distinct()
          .sorted()
          .map {
            bitmaskNodes.getValue(it)
          }

  var selectedPieceType by remember { mutableStateOf<PieceType>(pieceTypes.first()) }
  var selectedPushDirection by remember { mutableStateOf<PushDirection?>(null) }
  var selectedTargetNode by remember { mutableStateOf<Node?>(null) }

  Card(
      modifier = Modifier.padding(16.dp),
  ) {
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
          modifier = Modifier.padding(8.dp).fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text("Add Piece Moves", style = MaterialTheme.typography.headlineSmallEmphasized)

        Button(
            onClick = {
              selectedPieceType = PieceType.NULL
              selectedPushDirection = null
              selectedTargetNode = null
            }
        ) {
          Text("Reset")
        }
      }

      Row(
          horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        Card(
            modifier = Modifier.padding(end = 16.dp).weight(1f),
        ) {
          Column(Modifier.padding(8.dp)) {
            Text("Pieces")
            FlowRow(
                Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
              pieceTypes.forEach { pieceType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                      checked = pieceType == selectedPieceType,
                      onCheckedChange = { selectedPieceType = pieceType },
                  )
                  Text(pieceType.name)
                }
              }
            }
          }
        }
        Card(
            modifier = Modifier.padding(end = 16.dp).weight(1f),
        ) {
          Column(Modifier.padding(8.dp)) {
            Text("Push Directions")
            FlowRow(
                Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
              pushDirections.forEach { pushDirection ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                      enabled =
                          if (selectedTargetNode != null) {
                            targetPushDirections[selectedTargetNode?.bitmask]?.contains(
                                pushDirection
                            ) == true
                          } else if (pushDirection == selectedPushDirection) {
                            true
                          } else true,
                      checked = pushDirection == selectedPushDirection,
                      onCheckedChange = {
                        if (pushDirection == selectedPushDirection) {
                          selectedPushDirection = null
                        } else {
                          selectedPushDirection = pushDirection
                        }
                      },
                  )
                  Text(
                      when (pushDirection) {
                        PushDirection.UP -> "↑"
                        PushDirection.DOWN -> "↓"
                        PushDirection.UPPER_RIGHT -> "↗︎"
                        PushDirection.LOWER_RIGHT -> "↘︎"
                        PushDirection.UPPER_LEFT -> "↖︎"
                        PushDirection.LOWER_LEFT -> "↙︎"
                      } + " " + pushDirection.name.replace("_", " ")
                  )
                }
              }
            }
          }
        }
        Card(
            modifier = Modifier.weight(1f),
        ) {
          Column(Modifier.padding(8.dp)) {
            Text("Nodes")
            FlowRow(
                Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
              targetNodes.forEach { targetNode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                      enabled =
                          if (selectedPushDirection != null) {
                            pushDirectionTargets[selectedPushDirection]?.contains(
                                targetNode.bitmask
                            ) == true
                          } else if (targetNode == selectedTargetNode) {
                            true
                          } else true,
                      checked = targetNode == selectedTargetNode,
                      onCheckedChange = {
                        if (targetNode == selectedTargetNode) {
                          selectedTargetNode = null
                        } else {
                          selectedTargetNode = targetNode
                        }
                      },
                  )
                  Text(targetNode.coordinate.toString())
                }
              }
            }
          }
        }
      }
    }
  }

  if (
      selectedPieceType != PieceType.NULL &&
          selectedPushDirection != null &&
          selectedTargetNode != null
  ) {
    selectedMove = addPieceMoves.firstOrNull {
      val move = (it as PackedMove.Single).value
      move.extractPieceType() == selectedPieceType &&
          move.extractTargetBit() == selectedTargetNode!!.bitmask &&
          move.extractPushDirection() == selectedPushDirection
    }
    updateSelectedMove(selectedMove)
  }
}

@Composable
private fun MainMenuScreen(onEvent: (MainUiEvent) -> Unit) {
  var playerOneModelExpanded by remember { mutableStateOf(false) }
  var playerTwoModelExpanded by remember { mutableStateOf(false) }

  var playerOneModel by remember { mutableStateOf(Model.HUMAN) }
  var playerOneIterations by remember { mutableStateOf(0) }
  var playerOneDepth by remember { mutableStateOf(1) }
  var playerOneTimeControl by remember { mutableStateOf(false) }
  var playerOneUseRAVE by remember { mutableStateOf(false) }
  var playerOneEnableFPU by remember { mutableStateOf(false) }
  var playerOneEnablePW by remember { mutableStateOf(true) }

  var playerTwoModel by remember { mutableStateOf(Model.MCTS) }
  var playerTwoIterations by remember { mutableStateOf(0) }
  var playerTwoDepth by remember { mutableStateOf(1) }
  var playerTwoTimeControl by remember { mutableStateOf(false) }
  var playerTwoUseRAVE by remember { mutableStateOf(false) }
  var playerTwoEnableFPU by remember { mutableStateOf(false) }
  var playerTwoEnablePW by remember { mutableStateOf(true) }

  Row {
    Text("Select Players", style = MaterialTheme.typography.headlineMediumEmphasized)
  }

  Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Card(
        modifier = Modifier.weight(0.5f),
    ) {
      Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row {
          Text(
              "White (Plays First)",
              style = MaterialTheme.typography.headlineSmallEmphasized,
          )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // verticalArrangement = Arrangement.SpaceAround,
        ) {
          Row(modifier = Modifier.fillMaxWidth()) { Text("Options") }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
                enabled = true,
                label = { Text("Model") },
                value = playerOneModel.name,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                  IconButton(onClick = { playerOneModelExpanded = !playerOneModelExpanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "More options")
                  }
                },
            )
            DropdownMenu(
                expanded = playerOneModelExpanded,
                onDismissRequest = { playerOneModelExpanded = false },
            ) {
              for (model in Model.entries) DropdownMenuItem(
                  text = { Text(model.name) },
                  onClick = { playerOneModel = model },
              )
            }
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
                label = { Text("MCTS Iterations (0-99999)") },
                value = playerOneIterations.toString(),
                onValueChange = {
                  val input = it.trim()
                  if (input.isNotEmpty() || input.isNotBlank()) {
                    playerOneIterations = input.trim().toInt().coerceIn(0..99999)
                  }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
                label = { Text("Minimax Search Depth (0-5)") },
                value = playerOneDepth.toString(),
                onValueChange = {
                  val input = it.trim()
                  if (input.isNotEmpty() || input.isNotBlank()) {
                    playerOneDepth = input.toInt().coerceIn(0..5)
                  }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Time Control")
            Switch(
                checked = playerOneTimeControl,
                onCheckedChange = {
                  playerOneTimeControl = it
                },
                thumbContent =
                    if (playerOneTimeControl) {
                      {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                      }
                    } else {
                      null
                    },
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Use RAVE")
            Switch(
                checked = playerOneUseRAVE,
                onCheckedChange = {
                  playerOneUseRAVE = it
                },
                thumbContent =
                    if (playerOneUseRAVE) {
                      {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                      }
                    } else {
                      null
                    },
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Mean First Play Urgency")
            Switch(
                checked = playerOneEnableFPU,
                onCheckedChange = {
                  playerOneEnableFPU = it
                },
                thumbContent =
                    if (playerOneEnableFPU) {
                      {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                      }
                    } else {
                      null
                    },
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Progressive Widening")
            Switch(
                checked = playerOneEnablePW,
                onCheckedChange = {
                  playerOneEnablePW = it
                },
                thumbContent =
                    if (playerOneEnablePW) {
                      {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                      }
                    } else {
                      null
                    },
            )
          }
        }
      }
    }

    Card(
        modifier = Modifier.weight(0.5f),
    ) {
      Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
            "Black (Plays Second)",
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
        ) {
          Row(modifier = Modifier.fillMaxWidth()) { Text("Options") }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            OutlinedTextField(
                enabled = true,
                label = { Text("Model") },
                value = playerTwoModel.name,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                  IconButton(onClick = { playerTwoModelExpanded = !playerTwoModelExpanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "More options")
                  }
                },
            )

            DropdownMenu(
                expanded = playerTwoModelExpanded,
                onDismissRequest = { playerTwoModelExpanded = false },
            ) {
              for (model in Model.entries) DropdownMenuItem(
                  text = { Text(model.name) },
                  onClick = { playerTwoModel = model },
              )
            }
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
                label = { Text("MCTS Iterations (0-99999)") },
                value = playerTwoIterations.toString(),
                onValueChange = {
                  val input = it.trim()
                  if (input.isNotEmpty() || input.isNotBlank()) {
                    playerTwoIterations = input.trim().toInt().coerceIn(0..99999)
                  }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
                label = { Text("Minimax Search Depth (0-5)") },
                value = playerTwoDepth.toString(),
                onValueChange = {
                  val input = it.trim()
                  if (input.isNotEmpty() || input.isNotBlank()) {
                    playerTwoDepth = input.toInt().coerceIn(0..5)
                  }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Time Control")
            Switch(
                checked = playerTwoTimeControl,
                onCheckedChange = {
                  playerTwoTimeControl = it
                },
                thumbContent =
                    if (playerTwoTimeControl) {
                      {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                      }
                    } else {
                      null
                    },
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Use RAVE")
            Switch(
                checked = playerTwoUseRAVE,
                onCheckedChange = {
                  playerTwoUseRAVE = it
                },
                thumbContent =
                    if (playerTwoUseRAVE) {
                      {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                      }
                    } else {
                      null
                    },
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Mean First Play Urgency")
            Switch(
                checked = playerTwoEnableFPU,
                onCheckedChange = {
                  playerTwoEnableFPU = it
                },
                thumbContent =
                    if (playerTwoEnableFPU) {
                      {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                      }
                    } else {
                      null
                    },
            )
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Progressive Widening")
            Switch(
                checked = playerTwoEnablePW,
                onCheckedChange = {
                  playerTwoEnablePW = it
                },
                thumbContent =
                    if (playerTwoEnablePW) {
                      {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                      }
                    } else {
                      null
                    },
            )
          }
        }
      }
    }
  }

  Row {
    Button(
        enabled = true,
        onClick = {
          onEvent(
              MainUiEvent.StartNewGame(
                  Player(
                      name = PlayerName.WHITE,
                      model = playerOneModel,
                      timeControl = playerOneTimeControl,
                      useRAVE = playerOneUseRAVE,
                      enableFPU = playerOneEnableFPU,
                      enablePW = playerOneEnablePW,
                      iterations = playerOneIterations,
                      depth = playerOneDepth,
                  ),
                  Player(
                      name = PlayerName.BLACK,
                      model = playerTwoModel,
                      timeControl = playerTwoTimeControl,
                      useRAVE = playerTwoUseRAVE,
                      enableFPU = playerTwoEnableFPU,
                      enablePW = playerTwoEnablePW,
                      iterations = playerTwoIterations,
                      depth = playerTwoDepth,
                  ),
              )
          )
        },
    ) {
      Text("Start a New Game!")
    }
  }
}

@Composable
fun DrawBoard(uiState: MainUiState, selectedMoveCoordinates: List<Coordinate>) {
  val HEIGHT = 11
  val offsetAmount = 100f
  val canvasOffset = Offset(offsetAmount, offsetAmount.minus(30f))

  val builder: HexagonalGridBuilder<SatelliteData> =
      HexagonalGridBuilder<SatelliteData>()
          .setGridHeight(HEIGHT)
          .setGridWidth(HEIGHT)
          .setGridLayout(HexagonalGridLayout.HEXAGONAL)
          .setOrientation(HexagonOrientation.FLAT_TOP)
          .setRadius(60.0)

  val grid = builder.build()

  grid.getHexagonsByOffsetRange(2, 5, 2, -1)

  val hexagons =
      bitmaskToCubeCoordinate.values.map {
        grid.getByCubeCoordinate(it).get()
      }

  val spots = uiState.gameState?.board?.nodes?.filter { it.isSpot }

  val hexagonSpots = hexagons.associateWith { hexagon ->
    val bitmaskIndex = bitmaskToCubeCoordinate.values.indexOf(hexagon.cubeCoordinate)
    val bitmask = bitmaskToCubeCoordinate.keys.toList()[bitmaskIndex]
    spots?.first { it.bitmask == bitmask }
  }

  Row {
    /*    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(0.25f),
        elevation = 2.dp,
    ) {
      Column(
          modifier = Modifier.padding(16.dp).fillMaxWidth(),
      ) {
        val json = Json { prettyPrint = true }
        Text(
            text = "Bitborad",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            text = json.encodeToString(uiState.gameState?.bitboard),
            style = MaterialTheme.typography.bodySmall,
        )
      }
    }*/

    Card(
        modifier = Modifier.padding(16.dp).widthIn(max = 1000.dp),
    ) {
      val textMeasurer = rememberTextMeasurer()

      val textStyle =
          TextStyle(
              fontFamily = FontFamily.Monospace,
              fontSize = MaterialTheme.typography.headlineSmallEmphasized.fontSize.times(1),
              fontWeight = FontWeight.Bold,
              brush =
                  Brush.linearGradient(
                      colors = IBMColorBlindPalette.colors,
                  ),
          )

      var tooltipText by remember { mutableStateOf("") }

      TooltipArea(
          tooltip = {
            // Composable tooltip content:
            if (tooltipText.isNotEmpty()) {
              Surface(
                  modifier = Modifier.shadow(4.dp),
                  color = MaterialTheme.colorScheme.background,
                  shape = RoundedCornerShape(4.dp),
              ) {
                Text(
                    text = tooltipText,
                    modifier = Modifier.padding(10.dp),
                )
              }
            }
          },
          modifier = Modifier.padding(start = 40.dp),
          delayMillis = 100, // In milliseconds
          tooltipPlacement =
              TooltipPlacement.CursorPoint(
                  alignment = Alignment.TopEnd,
                  offset =
                      DpOffset(
                          (8).dp,
                          (-8).dp, // Tooltip offset
                      ),
              ),
      ) {
        Canvas(
            modifier =
                Modifier.height(900.dp).width(900.dp).onPointerEvent(PointerEventType.Move) {
                  val position = it.changes.first().position
                  val hexagon = hexagons.firstOrNull {
                    val radius = abs(it.centerX - it.points[0].coordinateX).toFloat().times(0.8f)

                    position.x.plus(100f) in it.centerX.minus(radius)..it.centerX.plus(radius) &&
                        position.y.plus(70f) in it.centerY.minus(radius)..it.centerY.plus(radius)
                  }

                  tooltipText =
                      hexagon?.let { hex ->
                        val node = hexagonSpots.getValue(hex)
                        "Spot: " +
                            node?.coordinate.toString() +
                            if (node?.piece != null) {
                              "\n" +
                                  "Piece: " +
                                  node.piece?.abbreviation +
                                  "\n" +
                                  "Potential: " +
                                  node.piece?.potential.toString().uppercase() +
                                  if (node.piece?.isNeutralized == true) {
                                    "\n" +
                                        "Neutralized: " +
                                        node.piece?.isNeutralized.toString().uppercase() +
                                        "\n" +
                                        "Active Piece: " +
                                        node.piece?.stackedPieces?.last()?.abbreviation
                                  } else ""
                            } else ""
                      } ?: ""
                }
        ) {
          for (hexagon in hexagons) {
            val neighbors =
                grid.getNeighborsOf(hexagon).map {
                  Offset(
                      it.centerX.toFloat(),
                      it.centerY.toFloat(),
                  )
                }

            for (neighbor in neighbors) {
              drawLine(
                  color = Color.Black,
                  start =
                      Offset(
                              hexagon.centerX.toFloat(),
                              hexagon.centerY.toFloat(),
                          )
                          .minus(canvasOffset),
                  end = neighbor.minus(canvasOffset),
                  strokeWidth = 8f,
              )
            }
          }
          for ((index, hexagon) in hexagons.withIndex()) {
            spots
                ?.first {
                  it.bitmask == bitmaskToCubeCoordinate.keys.toList()[index]
                }
                ?.let { node ->
                  if (node.coordinate in selectedMoveCoordinates) {
                    drawCircle(
                        brush =
                            Brush.linearGradient(
                                colors = IBMColorBlindPalette.colors.map { it.copy(alpha = 0.6f) },
                                end = Offset(size.width / 4f, 0f),
                                tileMode = TileMode.Mirror,
                            ),
                        center =
                            Offset(
                                    hexagon.centerX.toFloat(),
                                    hexagon.centerY.toFloat(),
                                )
                                .minus(canvasOffset),
                        radius =
                            (hexagon.centerX - hexagon.points[0].coordinateX).toFloat().times(0.8f),
                    )
                  }

                  node.piece?.let { piece: Piece ->
                    val topPiece =
                        when (piece.type) {
                          PieceType.DVONN,
                          PieceType.PUNCT -> {
                            if (piece.isNeutralized) {
                              piece.stackedPieces.last()
                            } else piece
                          }
                          else -> piece
                        }

                    val image = getPieceImage(topPiece)

                    image?.let {
                      drawImage(
                          image,
                          IntOffset.Zero,
                          IntSize(it.width, it.height),
                          dstOffset =
                              IntOffset(
                                      hexagon.centerX.toInt(),
                                      hexagon.centerY.toInt(),
                                  )
                                  .minus(
                                      IntOffset(
                                          100 + it.width.times(0.1).toInt(),
                                          70 + it.height.times(0.1).toInt(),
                                      )
                                  ),
                          dstSize =
                              IntSize(
                                  it.width.times(0.2f).roundToInt(),
                                  it.height.times(0.2f).roundToInt(),
                              ),
                      )
                    }
                  }
                }
          }
        }
      }
    }
  }
}

@Composable
fun Pieces(pieces: List<Piece>, title: String) {

  Card(
      modifier = Modifier.padding(16.dp).fillMaxWidth(),
  ) {
    Column(
        Modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(title, style = MaterialTheme.typography.headlineSmallEmphasized)
      Row(
          Modifier,
      ) {
        FlowRow(
            Modifier.padding(20.dp).wrapContentHeight(align = Alignment.Top),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          pieces
              .distinctBy { it.type }
              .forEach { piece ->
                val count = pieces.sumOf { if (it.type == piece.type) it.count() else 0 }

                if (count > 0) {
                  TooltipArea(
                      tooltip = {
                        // Composable tooltip content:
                        Surface(
                            modifier = Modifier.shadow(4.dp),
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                          Text(
                              text = piece.type.name,
                              modifier = Modifier.padding(10.dp),
                          )
                        }
                      },
                      modifier = Modifier.padding(start = 40.dp),
                      delayMillis = 100, // In milliseconds
                      tooltipPlacement =
                          TooltipPlacement.CursorPoint(
                              alignment = Alignment.TopEnd,
                              offset =
                                  DpOffset(
                                      (8).dp,
                                      (-8).dp, // Tooltip offset
                                  ),
                          ),
                  ) {
                    BadgedBox(
                        badge = {
                          Badge(
                          ) {
                            Text("$count", style = MaterialTheme.typography.headlineSmallEmphasized)
                          }
                        }
                    ) {
                      Image(
                          modifier = Modifier.size(64.dp),
                          bitmap = getPieceImage(piece),
                          contentDescription = null,
                      )
                    }
                  }
                }
              }
        }
      }
    }
  }
}

private fun getPieceImage(piece: Piece): ImageBitmap {

  val dir =
      "/Users/darronporter/Downloads/Dissertation/code-cli-desktop/core/src/main/resources/images"

  return Image.makeFromEncoded(
          File("$dir/${piece.colorName.name.lowercase()}_${piece.type.name.lowercase()}.png")
              .readBytes()
      )
      .toComposeImageBitmap()
}
