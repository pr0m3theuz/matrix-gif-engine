import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# Fix ZERTZ safeRemoveAndCheck missing the sourceBit restore in undoUsePiecePotential
# The test failed at: java.lang.IllegalStateException: UNDO FAILED: Expected WHITE ZERTZ in White ZERTZ bitboard at 256, but it was missing.
# Wait, for undoUsePiecePotential, we are REMOVING from the targetBit, and ADDING to the sourceBit, right?

# Look at undoUsePiecePotential -> PieceType.ZERTZ
# PieceType.ZERTZ -> {
#   whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
#   whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
# }
# Wait, if we used a potential, we MOVED it from sourceBit to targetBit.
# So to UNDO, we remove it from targetBit and ADD it to sourceBit for BOTH the ZERTZ and the Potentials bitboards!
content = re.sub(
r'''(PieceType\.ZERTZ -> \{\n\s*whiteZERTZ = safeRemoveAndCheck\(whiteZERTZ, "White ZERTZ", targetBit\)\n\s*whitePotentials = safeAddAndCheck\(whitePotentials, "White Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.ZERTZ -> {
          whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
          whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ", sourceBit)
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''', content)

content = re.sub(
r'''(PieceType\.YINSH -> \{\n\s*whiteYINSH = safeRemoveAndCheck\(whiteYINSH, "White YINSH", targetBit\)\n\s*whitePotentials = safeAddAndCheck\(whitePotentials, "White Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.YINSH -> {
          whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH", targetBit)
          whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH", sourceBit)
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''', content)

# And for black in undoUsePiecePotential
content = re.sub(
r'''(PieceType\.ZERTZ -> \{\n\s*blackZERTZ = blackZERTZ and targetBit.inv\(\)\n\s*blackPotentials = safeAddAndCheck\(blackPotentials, "Black Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.ZERTZ -> {
          blackZERTZ = safeRemoveAndCheck(blackZERTZ, "Black ZERTZ", targetBit)
          blackZERTZ = safeAddAndCheck(blackZERTZ, "Black ZERTZ", sourceBit)
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''', content)

content = re.sub(
r'''(PieceType\.YINSH -> \{\n\s*blackYINSH = blackYINSH and targetBit.inv\(\)\n\s*blackPotentials = safeAddAndCheck\(blackPotentials, "Black Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.YINSH -> {
          blackYINSH = safeRemoveAndCheck(blackYINSH, "Black YINSH", targetBit)
          blackYINSH = safeAddAndCheck(blackYINSH, "Black YINSH", sourceBit)
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''', content)

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
