## 2024-08-07 - Replaced Object Allocations with Bitwise Math in Move Generation
**Learning:** During move generation (a hot loop for Minimax/MCTS), instantiating `mutableSetOf<PieceType>()` and calling `Set.add` causes significant object allocation and hash calculation overhead that can easily be optimized out.
**Action:** Always favor primitive types and bitwise operations (`Int` bitmasks) over object collections (`Set`/`List`) for small, enumerable states within the inner loops of the game engine (like `Bitboard.identifyAvailableMoves`).
