#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# ==========================================
# Configuration Variables
# ==========================================
JAVA_BIN="java"
JVM_OPTS="-Xmx31g"
JAR_PATH="./build/libs/code-1.0-SNAPSHOT-standalone.jar"
MAIN_CLASS="org.example.GenerateReproducibleGamesKt"

# Base command string
CMD="$JAVA_BIN $JVM_OPTS -cp $JAR_PATH $MAIN_CLASS"

# ==========================================
# Execution
# ==========================================

#echo "Starting Run 1/16: MCTS (random) vs Minimax (greedy)..."
#$CMD -b -g 500 -p 16 -m mcts -ms random -M minimax -MS greedy
#
#echo "Starting Run 2/16: Minimax (greedy) vs MCTS (random)..."
#$CMD -b -g 500 -p 16 -m minimax -ms greedy -M mcts -MS random
#
#echo "Starting Run 3/16: MCTS (random) vs Minimax (easy)..."
#$CMD -b -g 500 -p 16 -m mcts -ms random -M minimax -MS easy
#
#echo "Starting Run 4/16: Minimax (easy) vs MCTS (random)..."
#$CMD -b -g 500 -p 16 -m minimax -ms easy -M mcts -MS random
#
#echo "Starting Run 5/16: MCTS (easy) vs Minimax (greedy)..."
#$CMD -b -g 500 -p 16 -m mcts -ms easy -M minimax -MS greedy
#
#echo "Starting Run 6/16: Minimax (greedy) vs MCTS (easy)..."
#$CMD -b -g 500 -p 16 -m minimax -ms greedy -M mcts -MS easy
#
#echo "Starting Run 7/16: Minimax (easy) vs Minimax (greedy)..."
#$CMD -b -g 500 -p 16 -m minimax -ms easy -M minimax -MS greedy
#
#echo "Starting Run 9/16: MCTS (easy) vs MCTS (random)..."
#$CMD -b -g 500 -p 16 -m mcts -ms easy -M mcts -MS random
#
#echo "Starting Run 10/16: MCTS (random) vs MCTS (easy)..."
#$CMD -b -g 500 -p 16 -m mcts -ms random -M mcts -MS easy

echo "Starting Run 11/16: MCTS (easy) vs Minimax (easy)..."
$CMD -b -g 2 -p 16 -m mcts -ms easy -M minimax -MS easy

echo "Starting Run 12/16: Minimax (easy) vs MCTS (easy)..."
$CMD -b -g 500 -p 16 -m minimax -ms easy -M mcts -MS easy

echo "All experiments completed successfully!"