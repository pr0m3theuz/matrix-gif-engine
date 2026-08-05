import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# Fix usePiecePotential White DVONN (white on black on white)
content = re.sub(
r'''(// place white on top of black on top of white\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whiteDVONNLayer[i] and targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix usePiecePotential White DVONN (white on black)
content = re.sub(
r'''(// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(blackDVONNLayer\[i\] and targetBit\) == 0UL &&\n\s*\(blackDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix usePiecePotential White PUNCT (white on black on white)
content = re.sub(
r'''(// place white on top of black on top of white\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(whitePUNCTLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whitePUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whitePUNCTLayer[i] and targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix usePiecePotential White PUNCT (white on black)
content = re.sub(
r'''(// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(blackPUNCTLayer\[i\] and targetBit\) == 0UL &&\n\s*\(blackPUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {''', content)


# Fix usePiecePotential Black DVONN (black on white)
content = re.sub(
r'''(// place black on top of white\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white
            if (
                i % 2 == 1 &&
                    (whiteDVONNLayer[i] and targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix usePiecePotential Black DVONN (black on white on black)
content = re.sub(
r'''(// place black on top of white on top of black\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(blackDVONNLayer\[i\] and targetBit\) == 0UL &&\n\s*\(blackDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackDVONNLayer[i] and targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix usePiecePotential Black PUNCT (black on white)
content = re.sub(
r'''(// place black on top of white\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(whitePUNCTLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whitePUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white
            if (
                i % 2 == 1 &&
                    (whitePUNCTLayer[i] and targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

# Fix usePiecePotential Black PUNCT (black on white on black)
content = re.sub(
r'''(// place black on top of white on top of black\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(blackPUNCTLayer\[i\] and targetBit\) == 0UL &&\n\s*\(blackPUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackPUNCTLayer[i] and targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {''', content)


with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
