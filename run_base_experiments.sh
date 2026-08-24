#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# ==========================================
# Configuration Variables
# ==========================================
JAVA_BIN="java"
JVM_OPTS="-Xmx87g"
JAR_PATH="./build/libs/code-1.0-SNAPSHOT-standalone.jar"
MAIN_CLASS="org.example.GenerateReproducibleGamesKt"

# Run Settings
TOTAL_GAMES=500
THREADS=22

# Base command string
CMD="$JAVA_BIN $JVM_OPTS -cp $JAR_PATH $MAIN_CLASS"

# ==========================================
# Matrix Parameters
# ==========================================
# Minimax Depths: EASY (3), MEDIUM (5), HARD (7)
MM_DEPTHS=(1 3)

# MCTS Iterations: EASY (~999)
MCTS_ITERATIONS=(0 25 50 100 250 500 1000)

RAVE_OPTIONS=(false true)

# Depths * Iterations * RAVE on/off * 2 match directions
TOTAL_RUNS=$((${#MM_DEPTHS[@]} * ${#MCTS_ITERATIONS[@]} * ${#RAVE_OPTIONS[@]} * 2))
RUN_COUNT=1

echo "Starting Minimax vs MCTS Matrix ($TOTAL_RUNS total configurations)..."

# ==========================================
# Matchups: Minimax (White) vs MCTS (Black)
# ==========================================
for depth in "${MM_DEPTHS[@]}"; do
  for iters in "${MCTS_ITERATIONS[@]}"; do
    for rave in "${RAVE_OPTIONS[@]}"; do

      A1_FLAGS="-m minimax -mdp $depth"
      A2_FLAGS="-M mcts -MI $iters"

      if [ "$rave" = true ]; then
        A2_FLAGS="$A2_FLAGS -MRV"
        RAVE_LABEL="RAVE"
      else
        RAVE_LABEL="Standard"
      fi

      echo "----------------------------------------------------"
      echo "[$RUN_COUNT/$TOTAL_RUNS] Minimax (Depth $depth) vs MCTS ($iters iters, $RAVE_LABEL)"
      echo "----------------------------------------------------"

      $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" $A1_FLAGS $A2_FLAGS
      RUN_COUNT=$((RUN_COUNT + 1))
    done
  done
done

# ==========================================
# Matchups: MCTS (White) vs Minimax (Black)
# ==========================================
for iters in "${MCTS_ITERATIONS[@]}"; do
  for depth in "${MM_DEPTHS[@]}"; do
    for rave in "${RAVE_OPTIONS[@]}"; do

      A1_FLAGS="-m mcts -mi $iters"
      if [ "$rave" = true ]; then
        A1_FLAGS="$A1_FLAGS -mrv"
        RAVE_LABEL="RAVE"
      else
        RAVE_LABEL="Standard"
      fi

      A2_FLAGS="-M minimax -Mdp $depth"

      echo "----------------------------------------------------"
      echo "[$RUN_COUNT/$TOTAL_RUNS] MCTS ($iters iters, $RAVE_LABEL) vs Minimax (Depth $depth)"
      echo "----------------------------------------------------"

      $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" $A1_FLAGS $A2_FLAGS
      RUN_COUNT=$((RUN_COUNT + 1))
    done
  done
done

echo "All Minimax vs MCTS matrix experiments completed successfully!"