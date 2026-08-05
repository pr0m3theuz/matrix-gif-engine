import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# Fix DVONN and PUNCT layer removals for `usePiecePotential` and `undoUsePiecePotential`
# When moving a piece potential, the piece MUST be removed from its source layer during use, and restored during undo!

# Let's see if usePiecePotential for White DVONN actually removes it from its sourceBit.
content = re.sub(
r'''(whitePotentials = safeRemoveAndCheck\(whitePotentials, "White Potentials", sourceBit\))''',
r'''whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
          // also remove from source
          if (pieceType == PieceType.DVONN) {
             for (j in 7 downTo 0) {
                if ((whiteDVONNLayer[j] and sourceBit) == sourceBit) {
                    whiteDVONNLayer[j] = safeRemoveAndCheck(whiteDVONNLayer[j], "White DVONN [$j]", sourceBit)
                    break
                }
             }
          } else if (pieceType == PieceType.PUNCT) {
             for (j in 7 downTo 0) {
                if ((whitePUNCTLayer[j] and sourceBit) == sourceBit) {
                    whitePUNCTLayer[j] = safeRemoveAndCheck(whitePUNCTLayer[j], "White PUNCT [$j]", sourceBit)
                    break
                }
             }
          }''', content, count=1) # only doing the first occurrence, I'll do this better.
