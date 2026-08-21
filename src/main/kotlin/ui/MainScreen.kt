@file:OptIn(ExperimentalMaterialApi::class)

package org.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import org.example.ai.mcts.PackedMove
import org.example.model.*
import org.example.model.State
import org.hexworks.mixite.core.api.HexagonOrientation
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridLayout
import org.hexworks.mixite.core.api.contract.SatelliteData

@Composable
fun MainScreen(window: ComposeWindow, uiState: MainUiState, onEvent: (event: MainUiEvent) -> Unit) {
  //        val context = LocalContext.current

  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  Scaffold(
      modifier = Modifier.scrollable(rememberScrollState(), orientation = Orientation.Vertical),
      topBar = {
        TopAppBar(
            title = { Text("MATRX GIPF") }
            /*actions = {
            IconButton(onClick = { */
            /*TODO: Implement import logic */
            /* }) {
            		Icon(Icons.Filled.Add, contentDescription = "Import")
            	}
            }*/
        )
      },
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { paddingValues ->
    Surface(color = MaterialTheme.colorScheme.surface) {
      Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        //      verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
      ) {
        if (uiState.status == GameStatus.Init) {
          configurePlayers(onEvent)
        }

        if (uiState.status == GameStatus.Running && uiState.gameState != null) {
          gameScreen(uiState, uiState.gameState, onEvent)
        }
      }
    }

  }
}

@Composable
private fun gameScreen(uiState: MainUiState, gameState: State, onEvent: (MainUiEvent) -> Unit) {
  Card(
      modifier = Modifier.padding(16.dp),
      elevation = 4.dp,
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
            "Opponent Player: ${gameState.nextPlayer.name}",
            style = MaterialTheme.typography.bodyLargeEmphasized,
        )
      }
    }
  }

  var selectedMove by remember { mutableStateOf<PackedMove?>(null) }

  // todo draw the board
  DrawBoard(uiState)

  if (uiState.turnPhase != TurnPhase.PieceRemoval) {
    val (addPieceMoves, usePotentialMoves) =
        uiState.availableMoves.partition {
          (it as PackedMove.Single).value.extractMoveType() == MoveType.AddPiece
        }

    if (addPieceMoves.isNotEmpty()) {
      selectedMove = AddPieceMoveOptions(addPieceMoves, selectedMove)
    }

    if (usePotentialMoves.isNotEmpty()) {
      selectedMove = UsePotentialMoves(usePotentialMoves)
    }
  }

  if (uiState.turnPhase == TurnPhase.PieceRemoval && uiState.availableMoves.isNotEmpty()) {
    selectedMove = PieceRemovalMoves(uiState)
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
private fun PieceRemovalMoves(uiState: MainUiState): PackedMove? {
  var selectedMove by remember { mutableStateOf<PackedMove?>(null) }

  Card(
      modifier = Modifier.padding(16.dp),
      elevation = 8.dp,
  ) {
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Retrieve/Capture Moves", style = MaterialTheme.typography.headlineSmallEmphasized)

      LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 192.dp)) {
        items(uiState.availableMoves.size) { index ->
          Row(
              verticalAlignment = Alignment.CenterVertically,
          ) {
            val move =
                (uiState.availableMoves[index] as PackedMove.Multiple).values.map { move ->
                  bitmaskNodes.getValue(move.extractTargetBit())
                }

            val text = move.joinToString { it.coordinate.toString() }
            Checkbox(
                checked = selectedMove == uiState.availableMoves[index],
                onCheckedChange = {
                  selectedMove = if (selectedMove == uiState.availableMoves[index]) {
                    null
                  } else {
                    uiState.availableMoves[index]
                  }
                }
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
): PackedMove? {
  var selectedMove by remember { mutableStateOf<PackedMove?>(null) }

  Card(
      modifier = Modifier.padding(16.dp),
      elevation = 8.dp,
  ) {
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Use Potential Moves", style = MaterialTheme.typography.headlineSmallEmphasized)

      LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 192.dp)) {
        items(usePotentialMoves.size) { index ->
          Row(
              verticalAlignment = Alignment.CenterVertically,
          ) {
            val move = (usePotentialMoves[index] as PackedMove.Single).value
            val pieceType = move.extractPieceType()
            val sourceNode = bitmaskNodes.getValue(move.extractSourceBit())
            val targetNode = bitmaskNodes.getValue(move.extractTargetBit())

            val text = "$pieceType $sourceNode -> $targetNode"
            Checkbox(
                checked = selectedMove == usePotentialMoves[index],
                onCheckedChange = {
                  selectedMove = if (selectedMove == usePotentialMoves[index]) {
                    null
                  } else {
                    usePotentialMoves[index]
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
private fun AddPieceMoveOptions(
    addPieceMoves: List<PackedMove>,
    selectedMove: PackedMove?,
): PackedMove? {
  var selectedMove1 = selectedMove
  val pieceTypes =
      addPieceMoves
          .mapNotNull {
            (it as PackedMove.Single).value.extractPieceType()
          }
          .distinct()

  val pushDirections =
      addPieceMoves
          .mapNotNull {
            (it as PackedMove.Single).value.extractPushDirection()
          }
          .distinct()

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
      elevation = 8.dp,
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
            elevation = 4.dp,
        ) {
          Column(Modifier.padding(8.dp)) {
            Text("Pieces")
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 128.dp)) {
              items(pieceTypes.size) { pieceType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                      checked = pieceTypes[pieceType] == selectedPieceType,
                      onCheckedChange = { selectedPieceType = pieceTypes[pieceType] },
                  )
                  Text(pieceTypes[pieceType].name)
                }
              }
            }
          }
        }
        Card(
            modifier = Modifier.padding(end = 16.dp).weight(1f),
            elevation = 4.dp,
        ) {
          Column(Modifier.padding(8.dp)) {
            Text("Push Directions")
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 192.dp)) {
              items(pushDirections.size) { pushDirection ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                      enabled =
                          if (selectedTargetNode != null) {
                            targetPushDirections[selectedTargetNode?.bitmask]?.contains(
                                pushDirections[pushDirection]
                            ) == true
                          } else if (pushDirections[pushDirection] == selectedPushDirection) {
                            true
                          } else true,
                      checked = pushDirections[pushDirection] == selectedPushDirection,
                      onCheckedChange = {
                        if (pushDirections[pushDirection] == selectedPushDirection) {
                          selectedPushDirection = null
                        } else {
                          selectedPushDirection = pushDirections[pushDirection]
                        }
                      },
                  )
                  Text(pushDirections[pushDirection].name)
                }
              }
            }
          }
        }
        Card(
            modifier = Modifier.weight(1f),
            elevation = 4.dp,
        ) {
          Column(Modifier.padding(8.dp)) {
            Text("Nodes")
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 96.dp)) {
              items(targetNodes.size) { targetNode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                      enabled =
                          if (selectedPushDirection != null) {
                            pushDirectionTargets[selectedPushDirection]?.contains(
                                targetNodes[targetNode].bitmask
                            ) == true
                          } else if (targetNodes[targetNode] == selectedTargetNode) {
                            true
                          } else true,
                      checked = targetNodes[targetNode] == selectedTargetNode,
                      onCheckedChange = {
                        if (targetNodes[targetNode] == selectedTargetNode) {
                          selectedTargetNode = null
                        } else {
                          selectedTargetNode = targetNodes[targetNode]
                        }
                      },
                  )
                  Text(targetNodes[targetNode].coordinate.toString())
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
    selectedMove1 = addPieceMoves.firstOrNull {
      val move = (it as PackedMove.Single).value
      move.extractPieceType() == selectedPieceType &&
          move.extractTargetBit() == selectedTargetNode!!.bitmask &&
          move.extractPushDirection() == selectedPushDirection
    }
    return selectedMove1
  }

  return null
}

@Composable
private fun configurePlayers(onEvent: (MainUiEvent) -> Unit) {
  var playerOneModelExpanded by remember { mutableStateOf(false) }
  var playerTwoModelExpanded by remember { mutableStateOf(false) }

  var playeronemodel by remember { mutableStateOf(Model.HUMAN) }
  var playerOneiterations by remember { mutableStateOf(0) }
  var playerOnedepth by remember { mutableStateOf(1) }
  var playerOnetimeControl by remember { mutableStateOf(false) }
  var playerOneuseRAVE by remember { mutableStateOf(false) }
  var playerOneenableFPU by remember { mutableStateOf(true) }
  var playerOneenablePW by remember { mutableStateOf(true) }

  var playerTwomodel by remember { mutableStateOf(Model.MCTS) }
  var playerTwoiterations by remember { mutableStateOf(0) }
  var playerTwodepth by remember { mutableStateOf(1) }
  var playerTwotimeControl by remember { mutableStateOf(false) }
  var playerTwouseRAVE by remember { mutableStateOf(false) }
  var playerTwoenableFPU by remember { mutableStateOf(true) }
  var playerTwoenablePW by remember { mutableStateOf(true) }

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
        elevation = 4.dp,
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
                value = playeronemodel.name,
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
                  onClick = { playeronemodel = model },
              )
            }
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
                label = { Text("MCTS Iterations") },
                value = playerOneiterations.toString(),
                onValueChange = { playerOneiterations = it.toInt().coerceIn(0..1000) },
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
                label = { Text("Minimax Search Depth") },
                value = playerOnedepth.toString(),
                onValueChange = { playerOnedepth = it.toInt().coerceIn(0..5) },
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
                checked = playerOnetimeControl,
                onCheckedChange = {
                  playerOnetimeControl = it
                },
                thumbContent =
                    if (playerOnetimeControl) {
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
                checked = playerOneuseRAVE,
                onCheckedChange = {
                  playerOneuseRAVE = it
                },
                thumbContent =
                    if (playerOneuseRAVE) {
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
            Text("First Play Urgency")
            Switch(
                checked = playerOneenableFPU,
                onCheckedChange = {
                  playerOneenableFPU = it
                },
                thumbContent =
                    if (playerOneenableFPU) {
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
                checked = playerOneenablePW,
                onCheckedChange = {
                  playerOneenablePW = it
                },
                thumbContent =
                    if (playerOneenablePW) {
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
        elevation = 4.dp,
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
                value = playerTwomodel.name,
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
                  onClick = { playerTwomodel = model },
              )
            }
          }
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
                label = { Text("MCTS Iterations") },
                value = playerTwoiterations.toString(),
                onValueChange = { playerTwoiterations = it.toInt().coerceIn(0..1000) },
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
                label = { Text("Minimax Search Depth") },
                value = playerTwodepth.toString(),
                onValueChange = { playerTwodepth = it.toInt().coerceIn(0..5) },
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
                checked = playerTwotimeControl,
                onCheckedChange = {
                  playerTwotimeControl = it
                },
                thumbContent =
                    if (playerTwotimeControl) {
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
                checked = playerTwouseRAVE,
                onCheckedChange = {
                  playerTwouseRAVE = it
                },
                thumbContent =
                    if (playerTwouseRAVE) {
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
            Text("First Play Urgency")
            Switch(
                checked = playerTwoenableFPU,
                onCheckedChange = {
                  playerTwoenableFPU = it
                },
                thumbContent =
                    if (playerTwoenableFPU) {
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
                checked = playerTwoenablePW,
                onCheckedChange = {
                  playerTwoenablePW = it
                },
                thumbContent =
                    if (playerTwoenablePW) {
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
                      model = playeronemodel,
                      timeControl = playerTwotimeControl,
                      useRAVE = playerTwouseRAVE,
                      enableFPU = playerTwoenableFPU,
                      enablePW = playerTwoenablePW,
                      iterations = playerTwoiterations,
                      depth = playerTwodepth,
                  ),
                  Player(
                      name = PlayerName.BLACK,
                      model = playerTwomodel,
                      timeControl = playerTwotimeControl,
                      useRAVE = playerTwouseRAVE,
                      enableFPU = playerTwoenableFPU,
                      enablePW = playerTwoenablePW,
                      iterations = playerTwoiterations,
                      depth = playerTwodepth,
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
// https://www.redblobgames.com/grids/hexagons/
fun DrawBoard(uiState: MainUiState) {
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

  Row {
    Card(
      modifier = Modifier.padding(16.dp).weight(0.3f),
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
    }

    Card(
      modifier = Modifier.padding(16.dp).weight(0.7f),
      elevation = 6.dp,
    ) {
      val textMeasurer = rememberTextMeasurer()

      val textStyle =
        TextStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = MaterialTheme.typography.headlineSmallEmphasized.fontSize.times(2),
          fontWeight = FontWeight.Bold,
          brush =
            Brush.linearGradient(
              colors = IBMColorBlindPalette.colors,
            ),
        )

      Canvas(modifier = Modifier.height(900.dp).width(900.dp)) {
        for ((index, hexagon) in hexagons.withIndex()) {
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
              node.piece?.let { piece: Piece ->
                drawCircle(
                  color = Color.DarkGray.copy(alpha = 0.5f),
                  center =
                    Offset(
                      hexagon.centerX.toFloat(),
                      hexagon.centerY.toFloat(),
                    )
                      .minus(canvasOffset),
                  radius =
                    (hexagon.centerX - hexagon.points[0].coordinateX).toFloat().times(0.6f),
                )
                drawText(
                  textMeasurer = textMeasurer,
                  topLeft =
                    Offset(
                      hexagon.centerX.toFloat(),
                      hexagon.centerY.toFloat(),
                    )
                      .minus(canvasOffset)
                      .minus(Offset(30f, 30f)),
                  style = textStyle,
                  text =
                    buildAnnotatedString {
                      withStyle(ParagraphStyle(textAlign = TextAlign.Start)) {
                        append(
                          when (piece.type) {
                            PieceType.DVONN,
                            PieceType.PUNCT -> {
                              if (piece.isNeutralized) {
                                piece.stackedPieces.last().abbreviation
                              } else {
                                piece.abbreviation.also {
                                  if (piece.potential) {
                                    it.uppercase()
                                  } else {
                                    it.lowercase()
                                  }
                                }
                              }
                            }
                            else -> {
                              piece.abbreviation.also {
                                if (piece.potential) {
                                  it.uppercase()
                                } else {
                                  it.lowercase()
                                }
                              }
                            }
                          }
                        )
                      }
                    },
                )
              }
            }
        }

        /*for (hexagon in grid.hexagons) {
					val neighbors =
							grid.getNeighborsOf(hexagon).map {
								Offset(
										it.centerX.toFloat(),
										it.centerY.toFloat(),
								)
							}

					for ((index, neighbor) in neighbors.withIndex()) {
						drawLine(
								color = Color.LightGray,
								start =
										Offset(
												hexagon.centerX.toFloat(),
												hexagon.centerY.toFloat(),
										),
								end = neighbor,
								strokeWidth = 1f,
						)
					}

					drawText(
							textMeasurer = textMeasurer,
							topLeft =
									Offset(
											hexagon.centerX.toFloat(),
											hexagon.centerY.toFloat(),
									),
							text =
									buildAnnotatedString {
										withStyle(ParagraphStyle(textAlign = TextAlign.Start)) {
											append("x:${hexagon.gridX},z:${hexagon.gridZ},y:${hexagon.gridY}")
										}
									},
					)
				}*/
      }
    }
  }

}
