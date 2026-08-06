## 2024-05-24 - Transposition Table was missing direct indexing
**Learning:** Found a critical performance bottleneck in `src/main/kotlin/engine/ZobristHashing.kt` where the `TranspositionTable.probe` method was performing an O(N) linear scan over the entire hash table (size 1,048,576) instead of an O(1) direct lookup. This effectively crippled minimax performance.
**Action:** Always verify that Transposition Tables use direct indexing (`table[hash and mask]`) for constant time lookups instead of scanning.
