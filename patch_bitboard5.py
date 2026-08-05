import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# I also need to check `usePiecePotential`. Did it remove from sourceBit?
content = re.sub(
r'''(PieceType\.ZERTZ -> \{\n\s*whiteZERTZ = safeAddAndCheck\(whiteZERTZ, "White ZERTZ", targetBit\)\n\s*whitePotentials = safeRemoveAndCheck\(whitePotentials, "White Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.ZERTZ -> {
          whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
          whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", sourceBit)
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''', content)

content = re.sub(
r'''(PieceType\.YINSH -> \{\n\s*whiteYINSH = safeAddAndCheck\(whiteYINSH, "White YINSH", targetBit\)\n\s*whitePotentials = safeRemoveAndCheck\(whitePotentials, "White Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.YINSH -> {
          whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH", targetBit)
          whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH", sourceBit)
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''', content)

# Black in usePiecePotential
content = re.sub(
r'''(PieceType\.ZERTZ -> \{\n\s*blackZERTZ = safeAddAndCheck\(blackZERTZ, "Black YINSH", targetBit\)\n\s*blackPotentials = safeRemoveAndCheck\(blackPotentials, "Black Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.ZERTZ -> {
          blackZERTZ = safeAddAndCheck(blackZERTZ, "Black ZERTZ", targetBit)
          blackZERTZ = safeRemoveAndCheck(blackZERTZ, "Black ZERTZ", sourceBit)
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''', content)

content = re.sub(
r'''(PieceType\.YINSH -> \{\n\s*blackYINSH = safeAddAndCheck\(blackYINSH, "Black YINSH", targetBit\)\n\s*blackPotentials = safeRemoveAndCheck\(blackPotentials, "Black Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.YINSH -> {
          blackYINSH = safeAddAndCheck(blackYINSH, "Black YINSH", targetBit)
          blackYINSH = safeRemoveAndCheck(blackYINSH, "Black YINSH", sourceBit)
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''', content)

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
