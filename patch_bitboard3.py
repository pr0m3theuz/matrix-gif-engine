import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# Fix Black DVONN in undoUsePiecePotential (which was partially botched in the last script)
# Wait, black DVONN layer:
# (black,white,black,white,black,white,black,white) -> No, that's what we originally talked about but it applies to blackDVONNLayer
# Let's check how the user defined it.
# whiteDVONN/PUNCTLayer(white,black,white,black,white,black,white,black)
# blackDVONN/PUNCTLayer(black,white,black,white,black,white,black,white)

# So if a Black piece is added, it is placed on:
# If i=1 (white layer): place black on top of white -> whiteDVONNLayer[1] (WAIT NO! The white layer [1] is BLACK! No, whiteDVONNLayer[1] is black)
# Let's fix Black DVONN `undoUsePiecePotential` to check the right lower layer.
content = re.sub(
r'''(// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*// TODO find top layer and remove bit\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == targetBit &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{)''',
r'''// place black on top of white
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whiteDVONNLayer[i] and targetBit) == targetBit &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {''', content)


with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
