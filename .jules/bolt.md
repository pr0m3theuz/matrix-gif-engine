## 2024-05-18 - [O(1) Transposition Table Lookup]
 **Learning:** The Transposition Table in this project was previously using a completely unoptimized O(N) linear search over a large 1-million item array in its probe function. For an engine with deep MCTS and minimax tree search where TT probes occur millions of times, this kind of loop is an engine-killing bottleneck.
 **Action:** Always ensure large internal caches or tables (like TT) in Kotlin use O(1) mathematical lookup (like bitwise AND mask for power-of-two arrays) instead of linear scan. Ensure `hash.toInt() and mask` is used.
