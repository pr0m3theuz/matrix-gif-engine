import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# Fix undoUsePiecePotential White PUNCT
content = re.sub(
r'''(// place white on top of black on top of white\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(whitePUNCTLayer\[i\] and targetBit\) == targetBit &&\n\s*\(whitePUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whitePUNCTLayer[i] and targetBit) == targetBit &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix undoUsePiecePotential White PUNCT (white on black)
content = re.sub(
r'''(// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(blackPUNCTLayer\[i\] and targetBit\) == targetBit &&\n\s*\(blackPUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and targetBit) == targetBit &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix undoUsePiecePotential Black DVONN
content = re.sub(
r'''(// place black on top of white\n\s*if \(\n\s*i % 2 == 1 &&\n\s*// TODO find top layer and remove bit\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == targetBit &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whiteDVONNLayer[i] and targetBit) == targetBit &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix undoUsePiecePotential Black DVONN (black on white on black)
content = re.sub(
r'''(// place black on top of white on top of black\n\s*if \(\n\s*i % 2 == 0 &&\n\s*// TODO find top layer and remove bit\n\s*\(blackDVONNLayer\[i\] and targetBit\) == targetBit &&\n\s*\(blackDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackDVONNLayer[i] and targetBit) == targetBit &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)


# Fix undoUsePiecePotential Black PUNCT
content = re.sub(
r'''(// place black on top of white\n\s*if \(\n\s*i % 2 == 1 &&\n\s*// TODO find top layer and remove bit\n\s*\(whitePUNCTLayer\[i\] and targetBit\) == targetBit &&\n\s*\(whitePUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whitePUNCTLayer[i] and targetBit) == targetBit &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix undoUsePiecePotential Black PUNCT (black on white on black)
content = re.sub(
r'''(// place black on top of white on top of black\n\s*if \(\n\s*i % 2 == 0 &&\n\s*// TODO find top layer and remove bit\n\s*\(blackPUNCTLayer\[i\] and targetBit\) == targetBit &&\n\s*\(blackPUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackPUNCTLayer[i] and targetBit) == targetBit &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
