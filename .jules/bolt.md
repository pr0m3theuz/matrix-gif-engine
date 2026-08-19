## 2024-06-25 - Avoid Re-constructing Bitboards for Empty Checks

**Learning:** In bitboard programming, there is a common anti-pattern where a loop uses `countTrailingZeroBits()` and `bitboard and (bitboard - 1UL)` to iterate over all set bits and recreate the exact same bitmask by ORing `(1UL shl bitIndex)`. If the goal is simply to verify that a bitboard is empty (e.g. `occupiedIndices == 0UL`), it's significantly faster to evaluate the boolean condition `bitboard == 0UL` directly, skipping the loop entirely.

**Action:** Whenever identifying available moves along a path (e.g. for DVONN or PUNCT potentials), avoid looping over blocking pieces just to check if the path is empty. Instead, test the bitwise AND of the occupancy and path mask directly against `0UL`.
