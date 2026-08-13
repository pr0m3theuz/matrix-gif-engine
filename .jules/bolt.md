## 2024-05-19 - Bitboard getDvonnMoves and getPunctMoves redundant iteration
**Learning:** `getDvonnMoves` and `getPunctMoves` calculated `occupiedIndices` by iterating through set bits in `blockingPieces` and setting them individually in `occupiedIndices`.
```kotlin
    var occupiedIndices = 0UL
    var blockingPieces = occupiedColumnSpots and pathMask
    while (blockingPieces != 0UL) {
      val bitIndex = blockingPieces.countTrailingZeroBits()
      occupiedIndices = occupiedIndices or (1UL shl bitIndex)
      blockingPieces = blockingPieces and (blockingPieces - 1UL)
    }
```
This is mathematically equivalent to simply saying `occupiedIndices = occupiedColumnSpots and pathMask`. The while loop is entirely redundant and wastes CPU cycles iterating through bits just to reconstruct the identical bitmask. This happens on the hot path of move generation.

**Action:** Remove the `while (blockingPieces != 0UL)` loop and replace it with `val occupiedIndices = occupiedColumnSpots and pathMask` in both `getDvonnMoves` and `getPunctMoves`.
