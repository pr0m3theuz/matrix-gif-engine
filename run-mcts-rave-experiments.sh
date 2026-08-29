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
# Strengths mapped to durations:
# EASY (1.5s), MEDIUM (5.0s), HARD (10.0s)
STRENGTHS=("easy")
RAVE_MODES=("standard" "rave")

TOTAL_RUNS=$((${#STRENGTHS[@]} * ${#STRENGTHS[@]} * ${#RAVE_MODES[@]} * ${#RAVE_MODES[@]}))
RUN_COUNT=1

# ==========================================
# Execution Loop
# ==========================================
echo "Starting matrix run ($TOTAL_RUNS total configurations)..."

for s1 in "${STRENGTHS[@]}"; do
  for s2 in "${STRENGTHS[@]}"; do
    for r1 in "${RAVE_MODES[@]}"; do
      for r2 in "${RAVE_MODES[@]}"; do

        # Configure Agent 1 flags (White)
        A1_FLAGS="-m mcts -ms $s1 -mtc"
        if [ "$r1" == "rave" ]; then
          A1_FLAGS="$A1_FLAGS -mrv"
        fi

        # Configure Agent 2 flags (Black)
        A2_FLAGS="-M mcts -MS $s2 -MTC"
        if [ "$r2" == "rave" ]; then
          A2_FLAGS="$A2_FLAGS -MRV"
        fi

        echo "----------------------------------------------------"
        echo "[$RUN_COUNT/$TOTAL_RUNS] Agent 1 (MCTS, $s1, $r1) vs Agent 2 (MCTS, $s2, $r2)"
        echo "----------------------------------------------------"

        $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" $A1_FLAGS $A2_FLAGS

        RUN_COUNT=$((RUN_COUNT + 1))
      done
    done
  done
done

echo "All time-control matrix experiments completed successfully!"