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
#CMD="$JAVA_BIN -cp $JAR_PATH $MAIN_CLASS"

# ==========================================
# Matrix Parameters
# ==========================================
# Minimax Depths: EASY (3), MEDIUM (5), HARD (7)
TIME_DURATIONS=(100 300 500 1000 1500 2000)

RAVE_OPTIONS=(false) # true

# Depths * Iterations * RAVE on/off * 2 match directions
TOTAL_RUNS=$((${#MM_DEPTHS[@]} * ${#MCTS_ITERATIONS[@]} * ${#RAVE_OPTIONS[@]} * 2))
RUN_COUNT=1

echo "Starting Minimax vs MCTS Matrix ($TOTAL_RUNS total configurations)..."

# ==========================================
# Matchups: Minimax (White) vs MCTS (Black)
# ==========================================
for time in "${TIME_DURATIONS[@]}"; do
    for time2 in "${TIME_DURATIONS[@]}"; do

      A1_FLAGS="-m minimax -mtc -mtd $time"
      A2_FLAGS="-M mcts -Mtc -Mtd $time2"

      echo "----------------------------------------------------"
      echo "[$RUN_COUNT/$TOTAL_RUNS] Minimax (Time: $time ms) vs MCTS ($time2 ms, $RAVE_LABEL)"
      echo "----------------------------------------------------"

      $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" $A1_FLAGS $A2_FLAGS

      RUN_COUNT=$((RUN_COUNT + 1))
    done
done

# ==========================================
# Matchups: MCTS (White) vs Minimax (Black)
# ==========================================
  for time in "${TIME_DURATIONS[@]}"; do
    for time2 in "${TIME_DURATIONS[@]}"; do

      A1_FLAGS="-m mcts -mtc -mtd $time"

      A2_FLAGS="-M minimax -Mtc -Mtd $time2"

      echo "----------------------------------------------------"
      echo "[$RUN_COUNT/$TOTAL_RUNS] MCTS ($time ms, $RAVE_LABEL) vs Minimax ($time2 ms)"
      echo "----------------------------------------------------"

      $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" $A1_FLAGS $A2_FLAGS

      RUN_COUNT=$((RUN_COUNT + 1))
    done
  done

echo "All Minimax vs MCTS matrix experiments completed successfully!"