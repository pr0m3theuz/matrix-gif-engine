## 2024-05-24 - Kotlin list copies
**Learning:** Calling `toList().toMutableList()` is an anti-pattern as it does an O(N) allocation + copy twice instead of once. `toMutableList()` does the allocation and copy natively.
**Action:** Search for and eliminate `toList().toMutableList()` chains.
