import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# For `usePiecePotential`, White DVONN
content = re.sub(
r'''(PieceType\.DVONN -> \{\n\s*for \(i in 1\.\.7\) \{\n\s*// place white on top of black on top of white\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == 0UL &&\n\s*\(blackDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*whiteDVONNLayer\[i\] =\n\s*safeAddAndCheck\(whiteDVONNLayer\[i\], "White DVONN \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(blackDVONNLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*blackDVONNLayer\[i\] =\n\s*safeAddAndCheck\(blackDVONNLayer\[i\], "Black DVONN \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*\}\n\n\s*whitePotentials = safeRemoveAndCheck\(whitePotentials, "White Potentials", sourceBit\))''',
r'''PieceType.DVONN -> {
          for (i in 1..7) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whiteDVONNLayer[i] and targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeAddAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              blackDVONNLayer[i] =
                  safeAddAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", targetBit)
              break
            }
          }

          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
          for (j in 7 downTo 0) {
              if ((whiteDVONNLayer[j] and sourceBit) == sourceBit) {
                  whiteDVONNLayer[j] = safeRemoveAndCheck(whiteDVONNLayer[j], "White DVONN [$j]", sourceBit)
                  break
              }
          }''', content)

# For `usePiecePotential`, White PUNCT
content = re.sub(
r'''(PieceType\.PUNCT -> \{\n\s*for \(i in 1\.\.7\) \{\n\s*// place white on top of black on top of white\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(whitePUNCTLayer\[i\] and targetBit\) == 0UL &&\n\s*\(blackPUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*whitePUNCTLayer\[i\] =\n\s*safeAddAndCheck\(whitePUNCTLayer\[i\], "White PUNCT \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(blackPUNCTLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whitePUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*blackPUNCTLayer\[i\] =\n\s*safeAddAndCheck\(blackPUNCTLayer\[i\], "Black PUNCT \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*\}\n\n\s*whitePotentials = safeRemoveAndCheck\(whitePotentials, "White Potentials", sourceBit\))''',
r'''PieceType.PUNCT -> {
          for (i in 1..7) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whitePUNCTLayer[i] and targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeAddAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackPUNCTLayer[i] and targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeAddAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", targetBit)
              break
            }
          }

          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
          for (j in 7 downTo 0) {
              if ((whitePUNCTLayer[j] and sourceBit) == sourceBit) {
                  whitePUNCTLayer[j] = safeRemoveAndCheck(whitePUNCTLayer[j], "White PUNCT [$j]", sourceBit)
                  break
              }
          }''', content)


# For `usePiecePotential`, Black DVONN
content = re.sub(
r'''(PieceType\.DVONN -> \{\n\n\s*for \(i in 1\.\.7\) \{\n\s*// place black on top of white\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == 0UL &&\n\s*\(blackDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*whiteDVONNLayer\[i\] =\n\s*safeAddAndCheck\(whiteDVONNLayer\[i\], "White DVONN \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*// place black on top of white on top of black\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(blackDVONNLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*blackDVONNLayer\[i\] =\n\s*safeAddAndCheck\(blackDVONNLayer\[i\], "Black DVONN \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*\}\n\n\s*blackPotentials = safeRemoveAndCheck\(blackPotentials, "Black Potentials", sourceBit\))''',
r'''PieceType.DVONN -> {

          for (i in 1..7) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    (whiteDVONNLayer[i] and targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeAddAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackDVONNLayer[i] and targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              blackDVONNLayer[i] =
                  safeAddAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", targetBit)
              break
            }
          }

          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
          for (j in 7 downTo 0) {
              if ((blackDVONNLayer[j] and sourceBit) == sourceBit) {
                  blackDVONNLayer[j] = safeRemoveAndCheck(blackDVONNLayer[j], "Black DVONN [$j]", sourceBit)
                  break
              }
          }''', content)


# For `usePiecePotential`, Black PUNCT
content = re.sub(
r'''(PieceType\.PUNCT -> \{\n\s*for \(i in 1\.\.7\) \{\n\s*// place black on top of white\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(whitePUNCTLayer\[i\] and targetBit\) == 0UL &&\n\s*\(blackPUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*whitePUNCTLayer\[i\] =\n\s*safeAddAndCheck\(whitePUNCTLayer\[i\], "White PUNCT \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*// place black on top of white on top of black\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(blackPUNCTLayer\[i\] and targetBit\) == 0UL &&\n\s*\(whitePUNCTLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*blackPUNCTLayer\[i\] =\n\s*safeAddAndCheck\(blackPUNCTLayer\[i\], "Black PUNCT \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*\}\n\n\s*blackPotentials = safeRemoveAndCheck\(blackPotentials, "Black Potentials", sourceBit\))''',
r'''PieceType.PUNCT -> {
          for (i in 1..7) {
            // place black on top of white
            if (
                i % 2 == 1 &&
                    (whitePUNCTLayer[i] and targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              whitePUNCTLayer[i] =
                  safeAddAndCheck(whitePUNCTLayer[i], "White PUNCT [$i]", targetBit)
              break
            }
            // place black on top of white on top of black
            if (
                i % 2 == 0 &&
                    (blackPUNCTLayer[i] and targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit
            ) {
              blackPUNCTLayer[i] =
                  safeAddAndCheck(blackPUNCTLayer[i], "Black PUNCT [$i]", targetBit)
              break
            }
          }

          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
          for (j in 7 downTo 0) {
              if ((blackPUNCTLayer[j] and sourceBit) == sourceBit) {
                  blackPUNCTLayer[j] = safeRemoveAndCheck(blackPUNCTLayer[j], "Black PUNCT [$j]", sourceBit)
                  break
              }
          }''', content)


with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
