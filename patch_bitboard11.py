import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# I messed up lines 1218+ and 1377+ in `undoUsePiecePotential` for White ZERTZ
# I accidentally added the DVONN logic `for (j in 0..7)` into `PieceType.ZERTZ ->`

content = re.sub(
r'''(PieceType\.ZERTZ -> \{\n\s*whiteZERTZ = safeRemoveAndCheck\(whiteZERTZ, "White ZERTZ", targetBit\)\n\s*whiteZERTZ = safeAddAndCheck\(whiteZERTZ, "White ZERTZ", sourceBit\)\n\s*whitePotentials = safeAddAndCheck\(whitePotentials, "White Potentials", sourceBit\)\n\s*for \(j in 0\.\.7\) \{\n\s*if \(j % 2 == 0\) \{\n\s*if \(\(whiteDVONNLayer\[j\] and sourceBit\) == 0UL\) \{\n\s*whiteDVONNLayer\[j\] = safeAddAndCheck\(whiteDVONNLayer\[j\], "White DVONN \[\$j\]", sourceBit\)\n\s*break\n\s*\}\n\s*\} else \{\n\s*if \(\(blackDVONNLayer\[j\] and sourceBit\) == 0UL\) \{\n\s*blackDVONNLayer\[j\] = safeAddAndCheck\(blackDVONNLayer\[j\], "Black DVONN \[\$j\]", sourceBit\)\n\s*break\n\s*\}\n\s*\}\n\s*\}\n\s*\})''',
r'''PieceType.ZERTZ -> {
          whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
          whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ", sourceBit)
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''', content)

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
