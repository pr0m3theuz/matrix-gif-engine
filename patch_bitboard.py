import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# Fix undoUsePiecePotential White DVONN
content = re.sub(
r'''(PieceType\.DVONN -> \{\n\s*for \(i in 7 downTo 1\) \{\n\s*// place white on top of black on top of white\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == targetBit &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''PieceType.DVONN -> {
          for (i in 7 downTo 1) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whiteDVONNLayer[i] and targetBit) == targetBit &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix undoUsePiecePotential White DVONN (white on black)
content = re.sub(
r'''(// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(blackDVONNLayer\[i\] and targetBit\) == targetBit &&\n\s*\(blackDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and targetBit) == targetBit &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
