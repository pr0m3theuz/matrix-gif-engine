#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# ==========================================
# Configuration Variables
# ==========================================
JAVA_BIN="/home/peachyfox/Downloads/idea-IU-261.25134.95/jbr/bin/java"
JVM_OPTS="-Xmx20g"
JAR_PATH="/var/home/peachyfox/Downloads/comp_4031_dissertation/build/libs/code-1.0-SNAPSHOT-standalone.jar"
MAIN_CLASS="org.example.GenerateReproducibleGamesKt"

# Base command string
CMD="$JAVA_BIN $JVM_OPTS -cp $JAR_PATH $MAIN_CLASS"

# ==========================================
# Execution
# ==========================================

# echo "Starting Experiment 1: MCTS (random) vs Minimax (greedy)..."
# $CMD -b -g 500 -m mcts -ms random -M minimax -MS greedy

# echo "Starting Experiment 2: Minimax (greedy) vs MCTS (random)..."
# $CMD -b -g 500 -m minimax -ms greedy -M mcts -MS random

echo "Starting Experiment 3: MCTS (easy) vs Minimax (greedy)..."
$CMD -b -g 500 -m mcts -ms easy -M minimax -MS greedy

echo "Starting Experiment 4: Minimax (greedy) vs MCTS (easy)..."
$CMD -b -g 500 -m minimax -ms greedy -M mcts -MS easy

echo "All experiments completed successfully!"
