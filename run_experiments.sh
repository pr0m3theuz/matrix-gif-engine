#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# ==========================================
# Configuration Variables
# ==========================================
JAVA_BIN="java"
JVM_OPTS="-XX:InitialRAMPercentage=70.0 -XX:MaxRAMPercentage=90.0"
JAR_PATH="./build/libs/code-1.0-SNAPSHOT-standalone.jar"
MAIN_CLASS="org.example.GenerateReproducibleGamesKt"

# Run Settings
TOTAL_GAMES=550
THREADS=32

# Base command string
CMD="$JAVA_BIN $JVM_OPTS -cp $JAR_PATH $MAIN_CLASS"

# ==========================================
# Execution
# ==========================================

echo "Starting Run 1/12: MCTS (random) vs Minimax (greedy)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m mcts -mi 0 -M minimax -Mdp 1

echo "Starting Run 2/12: Minimax (greedy) vs MCTS (random)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m minimax -mdp 1 -M mcts -Mi 0

echo "Starting Run 3/12: MCTS (random) vs Minimax (easy)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m mcts -mi 0 -M minimax -Mdp 3

echo "Starting Run 4/12: Minimax (easy) vs MCTS (random)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m minimax -mdp 3 -M mcts -Mi 0

echo "Starting Run 5/12: MCTS (easy) vs Minimax (greedy)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m mcts -mi 999 -M minimax -Mdp 1

echo "Starting Run 6/12: Minimax (greedy) vs MCTS (easy)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m minimax -mdp 1 -M mcts -Mi 999

echo "Starting Run 7/12: Minimax (greedy) vs Minimax (easy)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m minimax -mdp 1 -M minimax -Mdp 3

echo "Starting Run 8/12: Minimax (easy) vs Minimax (greedy)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m minimax -mdp 3 -M minimax -Mdp 1

echo "Starting Run 9/12: MCTS (easy) vs MCTS (random)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m mcts -mi 999 -M mcts -Mi 0

echo "Starting Run 10/12: MCTS (random) vs MCTS (easy)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m mcts -mi 0 -M mcts -Mi 999

echo "Starting Run 11/12: MCTS (easy) vs Minimax (easy)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m mcts -mi 999 -M minimax -Mdp 3

echo "Starting Run 12/12: Minimax (easy) vs MCTS (easy)..."
$CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -m minimax -mdp 3 -M mcts -Mi 999

echo "All experiments completed successfully!"