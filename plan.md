1.  **Bitboard diff optimization**
    *   Currently, `Bitboard.diff` executes a lot of manual property access and logic. Let's see if we can optimize it by avoiding copies. Wait, there is already `diffLayers` using array allocation for each call.
    *   Let's check `Bitboard.kt` lines 86-93 where `diffLayers` is defined. It creates `ULongArray(current.size)`. During MCTS/Minimax this allocation might be extremely frequent.
    *   Alternatively, `globalOccupancy` property (line 217) uses bitwise operations. It combines `whiteDVONNLayer[0]` and `whitePUNCTLayer[0]` etc. It is already optimized compared to iterating through the layers.
    *   Let's look at how bit iteration works. I see `countTrailingZeroBits()` is used in `getDvonnMoves` and `getPunctMoves` to iterate through occupied indices.
