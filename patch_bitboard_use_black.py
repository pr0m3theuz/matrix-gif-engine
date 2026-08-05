import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# I see my previous regex didn't quite match the "place black on top of white" because of a comment mismatch or because it was already changed by a previous script accidentally.
# Let's fix Black DVONN manually here

# Fix usePiecePotential Black DVONN (black on white)
content = re.sub(
r'''(// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white
            if (
                i % 2 == 1 &&
                    (whiteDVONNLayer[i] and targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)

content = re.sub(
r'''(// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(whitePUNCTLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whitePUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white
            if (
                i % 2 == 1 &&
                    (whitePUNCTLayer[i] and targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {''', content)


with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
