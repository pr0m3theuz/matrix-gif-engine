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
# High sample size recommended for statistically significant win rate measurements
TOTAL_GAMES=1000
THREADS=32

# Base command string
CMD="$JAVA_BIN $JVM_OPTS -cp $JAR_PATH $MAIN_CLASS"

# ==========================================
# Mirror Match Configurations (Symmetric Agents)
# ==========================================
# 1. Random / 0-iteration baseline
# 2. Minimax baseline (Depth 1, 3)
# 3. Minimax with full heuristics (QS + TT + KM)
# 4. MCTS baseline (100, 500 iterations)
declare -a CONFIGS=(
  "-m minimax -mdp 1 -M minimax -Mdp 1|Minimax (Depth 1) Mirror"
#  "-m minimax -mdp 3 -M minimax -Mdp 3|Minimax (Depth 3) Mirror"
  "-m minimax -mdp 3 -mqs -mtt -mkm -M minimax -Mdp 3 -Mqs -Mtt -Mkm|Minimax (Depth 3 + QS + TT + KM) Mirror"
  "-m mcts -mi 0 -M mcts -Mi 0|MCTS (Random) Mirror"
  "-m mcts -mi 99 -M mcts -Mi 99|MCTS (100 Iters) Mirror"
  "-m mcts -mi 499 -M mcts -Mi 499|MCTS (500 Iters) Mirror"
  "-m mcts -mi 999 -M mcts -Mi 999|MCTS (1000 Iters) Mirror"
)

TOTAL_RUNS=${#CONFIGS[@]}
RUN_COUNT=1

echo "============================================================"
echo "Starting White First-Move Advantage Experiment"
echo "Total Configurations: $TOTAL_RUNS | Games per Config: $TOTAL_GAMES"
echo "============================================================"

for entry in "${CONFIGS[@]}"; do
  IFS="|" read -r FLAGS LABEL <<< "$entry"

  echo ""
  echo "----------------------------------------------------"
  echo "[$RUN_COUNT/$TOTAL_RUNS] Testing: $LABEL"
  echo "----------------------------------------------------"

  # Run identical agents against each other with search statistics enabled
  $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -s "$SEED" -css $FLAGS

  RUN_COUNT=$((RUN_COUNT + 1))
done

echo ""
echo "All first-move advantage mirror experiments completed successfully!"