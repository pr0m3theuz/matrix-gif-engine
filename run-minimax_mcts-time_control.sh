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
TOTAL_GAMES=100
THREADS=22

# Base command string
CMD="$JAVA_BIN $JVM_OPTS -cp $JAR_PATH $MAIN_CLASS"

# ==========================================
# Matrix Parameters
# ==========================================
# Strengths spanning:
# EASY (1.5s, Depth 3 / ~999 rounds)
# MEDIUM (5.0s, Depth 5 / ~2499 rounds)
# HARD (10.0s, Depth 7 / ~4999 rounds)
STRENGTHS=("easy")
RAVE_OPTIONS=(false true)

# 3 strengths * 3 strengths * 2 (RAVE on/off) * 2 match directions = 36 runs
TOTAL_RUNS=$((${#STRENGTHS[@]} * ${#STRENGTHS[@]} * ${#RAVE_OPTIONS[@]} * 2))
RUN_COUNT=1

echo "Starting Minimax vs MCTS Time Control Matrix ($TOTAL_RUNS total configurations)..."

# ==========================================
# Matchups: Minimax (White) vs MCTS (Black)
# ==========================================
for mm_strength in "${STRENGTHS[@]}"; do
  for mcts_strength in "${STRENGTHS[@]}"; do
    for rave in "${RAVE_OPTIONS[@]}"; do

      A1_FLAGS="-m minimax -ms $mm_strength -mtc"
      A2_FLAGS="-M mcts -MS $mcts_strength -MTC"
      
      if [ "$rave" = true ]; then
        A2_FLAGS="$A2_FLAGS -MRV"
        RAVE_LABEL="RAVE"
      else
        RAVE_LABEL="Standard"
      fi

      echo "----------------------------------------------------"
      echo "[$RUN_COUNT/$TOTAL_RUNS] Minimax ($mm_strength) vs MCTS ($mcts_strength, $RAVE_LABEL)"
      echo "----------------------------------------------------"

      $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" $A1_FLAGS $A2_FLAGS
      RUN_COUNT=$((RUN_COUNT + 1))
    done
  done
done

# ==========================================
# Matchups: MCTS (White) vs Minimax (Black)
# ==========================================
for mcts_strength in "${STRENGTHS[@]}"; do
  for mm_strength in "${STRENGTHS[@]}"; do
    for rave in "${RAVE_OPTIONS[@]}"; do

      A1_FLAGS="-m mcts -ms $mcts_strength -mtc"
      if [ "$rave" = true ]; then
        A1_FLAGS="$A1_FLAGS -mrv"
        RAVE_LABEL="RAVE"
      else
        RAVE_LABEL="Standard"
      fi

      A2_FLAGS="-M minimax -MS $mm_strength -MTC"

      echo "----------------------------------------------------"
      echo "[$RUN_COUNT/$TOTAL_RUNS] MCTS ($mcts_strength, $RAVE_LABEL) vs Minimax ($mm_strength)"
      echo "----------------------------------------------------"

      $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" $A1_FLAGS $A2_FLAGS
      RUN_COUNT=$((RUN_COUNT + 1))
    done
  done
done

echo "All Minimax vs MCTS matrix experiments completed successfully!"