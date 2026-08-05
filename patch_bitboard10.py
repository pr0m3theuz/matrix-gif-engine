import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# I see I added an extra bracket block inside PieceType.ZERTZ and YINSH in undoUsePiecePotential somehow?
# Wait, look at line 1170.
# PieceType.ZERTZ -> {
#   whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
#   whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ", sourceBit)
#   whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
#   for (j in 0..7) {  <--- This is because I used regex to replace whitePotentials = safeAddAndCheck(...), which caught the ONE inside PieceType.ZERTZ!!

# Oh I see! In `undoUsePiecePotential`, `whitePotentials = safeAddAndCheck` appears ONCE at the end of White, but it ALSO appeared inside PieceType.ZERTZ!
# I accidentally replaced the `whitePotentials = ...` inside PieceType.ZERTZ instead of the one at the end of White DVONN/PUNCT !!

# Let's completely revert BitboardOptimizedFunctions.kt to original and apply the patch properly!
