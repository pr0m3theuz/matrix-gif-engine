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
THREADS=22
SEED=42

# Base command string
CMD="$JAVA_BIN $JVM_OPTS -cp $JAR_PATH $MAIN_CLASS"

# ==========================================
# Matrix Parameters
# ==========================================
MCTS_ITERATIONS=(99 499 999)

# MCTS Feature Flags (true/false)
RAVE_OPTIONS=(false true)  # RAVE (-rv / -Mrv)
FPU_OPTIONS=(false true)   # First Play Urgency (-fpu / -Mfpu)
PW_OPTIONS=(false true)    # Progressive Widening (-pw / -Mpw)

# Total configurations: (Iterations) * (8 White combos) * (8 Black combos)
TOTAL_RUNS=$((${#MCTS_ITERATIONS[@]} * ${#RAVE_OPTIONS[@]} * ${#FPU_OPTIONS[@]} * ${#PW_OPTIONS[@]} * ${#RAVE_OPTIONS[@]} * ${#FPU_OPTIONS[@]} * ${#PW_OPTIONS[@]}))
RUN_COUNT=1

echo "Starting MCTS vs MCTS Enhancement Matrix ($TOTAL_RUNS total configurations)..."

# ==========================================
# Helper to build MCTS CLI flags
# ==========================================
build_mcts_flags() {
  local prefix="$1"   # "m" for Agent 1, "M" for Agent 2
  local iters="$2"
  local rave="$3"
  local fpu="$4"
  local pw="$5"

  local flags="-${prefix} mcts -${prefix}i $iters"
  [ "$rave" = true ] && flags="$flags -${prefix}rv"
  [ "$fpu" = true ] && flags="$flags -${prefix}fpu"
  [ "$pw" = true ] && flags="$flags -${prefix}pw"

  echo "$flags"
}

# ==========================================
# Helper to build readable label
# ==========================================
format_label() {
  local rave="$1"
  local fpu="$2"
  local pw="$3"
  local features=()

  [ "$rave" = true ] && features+=("RAVE")
  [ "$fpu" = true ] && features+=("FPU")
  [ "$pw" = true ] && features+=("PW")

  if [ ${#features[@]} -eq 0 ]; then
    echo "Standard"
  else
    IFS="+" ; echo "${features[*]}"
  fi
}

# ==========================================
# Matchups: MCTS (White) vs MCTS (Black)
# ==========================================
for iters in "${MCTS_ITERATIONS[@]}"; do
  for rave1 in "${RAVE_OPTIONS[@]}"; do
    for fpu1 in "${FPU_OPTIONS[@]}"; do
      for pw1 in "${PW_OPTIONS[@]}"; do

        A1_FLAGS=$(build_mcts_flags "m" "$iters" "$rave1" "$fpu1" "$pw1")
        LABEL1=$(format_label "$rave1" "$fpu1" "$pw1")

        for rave2 in "${RAVE_OPTIONS[@]}"; do
          for fpu2 in "${FPU_OPTIONS[@]}"; do
            for pw2 in "${PW_OPTIONS[@]}"; do

              A2_FLAGS=$(build_mcts_flags "M" "$iters" "$rave2" "$fpu2" "$pw2")
              LABEL2=$(format_label "$rave2" "$fpu2" "$pw2")

              echo "----------------------------------------------------"
              echo "[$RUN_COUNT/$TOTAL_RUNS] ($iters iters) White: MCTS ($LABEL1) vs Black: MCTS ($LABEL2)"
              echo "----------------------------------------------------"

              $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" -s "$SEED" -css $A1_FLAGS $A2_FLAGS
              RUN_COUNT=$((RUN_COUNT + 1))

            done
          done
        done

      done
    done
  done
done

echo "All MCTS vs MCTS enhancement matrix experiments completed successfully!"