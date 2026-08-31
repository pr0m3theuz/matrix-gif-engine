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
TOTAL_GAMES=500
THREADS=22

# Base command string
CMD="$JAVA_BIN $JVM_OPTS -cp $JAR_PATH $MAIN_CLASS"

# ==========================================
# Matrix Parameters
# ==========================================
MM_DEPTHS=(1 3)

# Feature flags for Minimax (true/false)
QS_OPTIONS=(false true)  # Quiescence Search (-qs / -Mqs)
TT_OPTIONS=(false true)  # Transposition Table (-tt / -Mtt)
KM_OPTIONS=(false true)  # Killer Moves (-km / -Mkm)

# Total configurations: (Depths^2) * (8 White combos) * (8 Black combos)
TOTAL_RUNS=$((${#MM_DEPTHS[@]} * ${#MM_DEPTHS[@]} * ${#QS_OPTIONS[@]} * ${#TT_OPTIONS[@]} * ${#KM_OPTIONS[@]} * ${#QS_OPTIONS[@]} * ${#TT_OPTIONS[@]} * ${#KM_OPTIONS[@]}))
RUN_COUNT=1

echo "Starting Minimax vs Minimax Feature Matrix ($TOTAL_RUNS total configurations)..."

# ==========================================
# Helper to build Minimax CLI flags
# ==========================================
build_minimax_flags() {
  local prefix="$1"   # "m" for Agent 1, "M" for Agent 2
  local depth="$2"
  local qs="$3"
  local tt="$4"
  local km="$5"

  local flags="-${prefix} minimax -${prefix}dp $depth"
  [ "$qs" = true ] && flags="$flags -${prefix}qs"
  [ "$tt" = true ] && flags="$flags -${prefix}tt"
  [ "$km" = true ] && flags="$flags -${prefix}km"

  echo "$flags"
}

# ==========================================
# Helper to build readable label
# ==========================================
format_label() {
  local qs="$1"
  local tt="$2"
  local km="$3"
  local features=()

  [ "$qs" = true ] && features+=("QS")
  [ "$tt" = true ] && features+=("TT")
  [ "$km" = true ] && features+=("KM")

  if [ ${#features[@]} -eq 0 ]; then
    echo "Baseline"
  else
    IFS="+" ; echo "${features[*]}"
  fi
}

# ==========================================
# Matchups: Minimax (White) vs Minimax (Black)
# ==========================================
for d1 in "${MM_DEPTHS[@]}"; do
  for qs1 in "${QS_OPTIONS[@]}"; do
    for tt1 in "${TT_OPTIONS[@]}"; do
      for km1 in "${KM_OPTIONS[@]}"; do

        A1_FLAGS=$(build_minimax_flags "m" "$d1" "$qs1" "$tt1" "$km1")
        LABEL1=$(format_label "$qs1" "$tt1" "$km1")

        for d2 in "${MM_DEPTHS[@]}"; do
          for qs2 in "${QS_OPTIONS[@]}"; do
            for tt2 in "${TT_OPTIONS[@]}"; do
              for km2 in "${KM_OPTIONS[@]}"; do

                A2_FLAGS=$(build_minimax_flags "M" "$d2" "$qs2" "$tt2" "$km2")
                LABEL2=$(format_label "$qs2" "$tt2" "$km2")

                echo "----------------------------------------------------"
                echo "[$RUN_COUNT/$TOTAL_RUNS] White: Minimax (D$d1, $LABEL1) vs Black: Minimax (D$d2, $LABEL2)"
                echo "----------------------------------------------------"

                $CMD -b -g "$TOTAL_GAMES" -p "$THREADS" $A1_FLAGS $A2_FLAGS
                RUN_COUNT=$((RUN_COUNT + 1))

              done
            done
          done
        done

      done
    done
  done
done

echo "All Minimax vs Minimax matrix experiments completed successfully!"