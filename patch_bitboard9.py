import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# Instead of doing loops, wait! A piece that used its potential was a piece from the board.
# Was it part of a stack? Yes, it could be on top of a stack.
# When a piece moves, its layer is `j`. But wait, in `usePiecePotential`, `sourceBit` is just a bit.
# We don't have the layer encoded in the move value `UInt`.
# So to properly UNDO a usePiecePotential, we just put it back on the board at `sourceBit`.
# BUT wait! `sourceBit` is just a coordinate. We must put it back on TOP of the stack at `sourceBit`!
# Let's fix this cleanly.

# White DVONN undo UsePiecePotential
content = re.sub(
r'''(whitePotentials = safeAddAndCheck\(whitePotentials, "White Potentials", sourceBit\)\n\s*\})''',
r'''whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
          for (j in 0..7) {
              if (j % 2 == 0) {
                  if ((whiteDVONNLayer[j] and sourceBit) == 0UL) {
                      whiteDVONNLayer[j] = safeAddAndCheck(whiteDVONNLayer[j], "White DVONN [$j]", sourceBit)
                      break
                  }
              } else {
                  if ((blackDVONNLayer[j] and sourceBit) == 0UL) {
                      blackDVONNLayer[j] = safeAddAndCheck(blackDVONNLayer[j], "Black DVONN [$j]", sourceBit)
                      break
                  }
              }
          }
        }''', content, count=1)

# White PUNCT undo UsePiecePotential
content = re.sub(
r'''(PieceType\.PUNCT -> \{\n\s*for \(i in 7 downTo 1\) \{\n\s*// place white on top of black on top of white\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(whitePUNCTLayer\[i\] and targetBit\) == targetBit &&\n\s*\(blackPUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*whitePUNCTLayer\[i\] =\n\s*safeRemoveAndCheck\(whitePUNCTLayer\[i\], "White PUNCT \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(blackPUNCTLayer\[i\] and targetBit\) == targetBit &&\n\s*\(whitePUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*blackPUNCTLayer\[i\] =\n\s*safeRemoveAndCheck\(blackPUNCTLayer\[i\], "Black PUNCT \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*\}\n\n\s*whitePotentials = safeAddAndCheck\(whitePotentials, "White Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.PUNCT -> {
          for (i in 7 downTo 1) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whitePUNCTLayer[i] and targetBit) == targetBit &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeRemoveAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and targetBit) == targetBit &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeRemoveAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", targetBit)
              break
            }
          }

          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
          for (j in 0..7) {
              if (j % 2 == 0) {
                  if ((whitePUNCTLayer[j] and sourceBit) == 0UL) {
                      whitePUNCTLayer[j] = safeAddAndCheck(whitePUNCTLayer[j], "White PUNCT [$j]", sourceBit)
                      break
                  }
              } else {
                  if ((blackPUNCTLayer[j] and sourceBit) == 0UL) {
                      blackPUNCTLayer[j] = safeAddAndCheck(blackPUNCTLayer[j], "Black PUNCT [$j]", sourceBit)
                      break
                  }
              }
          }
        }''', content)


# Black DVONN undo UsePiecePotential
content = re.sub(
r'''(PieceType\.DVONN -> \{\n\n\s*for \(i in 7 downTo 1\) \{\n\s*// place black on top of white\n\s*if \(\n\s*i % 2 == 1 &&\n\s*// TODO find top layer and remove bit\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == targetBit &&\n\s*\(blackDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*whiteDVONNLayer\[i\] =\n\s*safeRemoveAndCheck\(whiteDVONNLayer\[i\], "White DVONN \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*// place black on top of white on top of black\n\s*if \(\n\s*i % 2 == 0 &&\n\s*// TODO find top layer and remove bit\n\s*\(blackDVONNLayer\[i\] and targetBit\) == targetBit &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*blackDVONNLayer\[i\] =\n\s*safeRemoveAndCheck\(blackDVONNLayer\[i\], "Black DVONN \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*\}\n\n\s*blackPotentials = safeAddAndCheck\(blackPotentials, "Black Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.DVONN -> {

          for (i in 7 downTo 1) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whiteDVONNLayer[i] and targetBit) == targetBit &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackDVONNLayer[i] and targetBit) == targetBit &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              blackDVONNLayer[i] =
                  safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", targetBit)
              break
            }
          }

          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
          for (j in 0..7) {
              if (j % 2 == 0) {
                  if ((blackDVONNLayer[j] and sourceBit) == 0UL) {
                      blackDVONNLayer[j] = safeAddAndCheck(blackDVONNLayer[j], "Black DVONN [$j]", sourceBit)
                      break
                  }
              } else {
                  if ((whiteDVONNLayer[j] and sourceBit) == 0UL) {
                      whiteDVONNLayer[j] = safeAddAndCheck(whiteDVONNLayer[j], "White DVONN [$j]", sourceBit)
                      break
                  }
              }
          }
        }''', content)


# Black PUNCT undo UsePiecePotential
content = re.sub(
r'''(PieceType\.PUNCT -> \{\n\s*for \(i in 7 downTo 1\) \{\n\s*// place black on top of white\n\s*if \(\n\s*i % 2 == 1 &&\n\s*// TODO find top layer and remove bit\n\s*\(whitePUNCTLayer\[i\] and targetBit\) == targetBit &&\n\s*\(blackPUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*whitePUNCTLayer\[i\] =\n\s*safeRemoveAndCheck\(whitePUNCTLayer\[i\], "White PUNCT \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*// place black on top of white on top of black\n\s*if \(\n\s*i % 2 == 0 &&\n\s*// TODO find top layer and remove bit\n\s*\(blackPUNCTLayer\[i\] and targetBit\) == targetBit &&\n\s*\(whitePUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*blackPUNCTLayer\[i\] =\n\s*safeRemoveAndCheck\(blackPUNCTLayer\[i\], "Black PUNCT \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*\}\n\n\s*blackPotentials = safeAddAndCheck\(blackPotentials, "Black Potentials", sourceBit\)\n\s*\})''',
r'''PieceType.PUNCT -> {
          for (i in 7 downTo 1) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    // TODO find top layer and remove bit
                    (whitePUNCTLayer[i] and targetBit) == targetBit &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeRemoveAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    // TODO find top layer and remove bit
                    (blackPUNCTLayer[i] and targetBit) == targetBit &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeRemoveAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", targetBit)
              break
            }
          }

          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
          for (j in 0..7) {
              if (j % 2 == 0) {
                  if ((blackPUNCTLayer[j] and sourceBit) == 0UL) {
                      blackPUNCTLayer[j] = safeAddAndCheck(blackPUNCTLayer[j], "Black PUNCT [$j]", sourceBit)
                      break
                  }
              } else {
                  if ((whitePUNCTLayer[j] and sourceBit) == 0UL) {
                      whitePUNCTLayer[j] = safeAddAndCheck(whitePUNCTLayer[j], "White PUNCT [$j]", sourceBit)
                      break
                  }
              }
          }
        }''', content)


with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
