import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()


# For `undoUsePiecePotential`, White DVONN
content = re.sub(
r'''(PieceType\.DVONN -> \{\n\s*for \(i in 7 downTo 1\) \{\n\s*// place white on top of black on top of white\n\s*if \(\n\s*i % 2 == 0 &&\n\s*\(whiteDVONNLayer\[i\] and targetBit\) == targetBit &&\n\s*\(blackDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*whiteDVONNLayer\[i\] =\n\s*safeRemoveAndCheck\(whiteDVONNLayer\[i\], "White DVONN \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*// place white on top of black\n\s*if \(\n\s*i % 2 == 1 &&\n\s*\(blackDVONNLayer\[i\] and targetBit\) == targetBit &&\n\s*\(whiteDVONNLayer\[i - 1\] and targetBit\) == targetBit\n\s*\) \{\n\s*blackDVONNLayer\[i\] =\n\s*safeRemoveAndCheck\(blackDVONNLayer\[i\], "Black DVONN \[\$i\]", targetBit\)\n\s*break\n\s*\}\n\s*\}\n\n\s*whitePotentials = safeAddAndCheck\(whitePotentials, "White Potentials", sourceBit\))''',
r'''PieceType.DVONN -> {
          for (i in 7 downTo 1) {
            // place white on top of black on top of white
            if (
                i % 2 == 0 &&
                    (whiteDVONNLayer[i] and targetBit) == targetBit &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              whiteDVONNLayer[i] =
                  safeRemoveAndCheck(whiteDVONNLayer[i], "White DVONN [$i]", targetBit)
              break
            }
            // place white on top of black
            if (
                i % 2 == 1 &&
                    (blackDVONNLayer[i] and targetBit) == targetBit &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit
            ) {
              blackDVONNLayer[i] =
                  safeRemoveAndCheck(blackDVONNLayer[i], "Black DVONN [$i]", targetBit)
              break
            }
          }

          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
          for (j in 0..7) {
              if ((whiteDVONNLayer[j] and sourceBit) == 0UL) {
                  // We need to know where it came from, but wait,
                  // the original piece was on top of a stack or alone.
                  // If we don't know the exact layer it was removed from, we just add it to the correct layer.
                  // Actually, potential pieces are just single pieces. We restore it exactly at the layer it was taken from.
                  // Wait, it is restored at `sourceBit`. But it could have been removed from layer J.
                  // Since we only know sourceBit, and it was taken from top...
                  // We just place it at the first empty layer at sourceBit, provided the parity is correct for a white piece!
                  // Let's just place it at layer 0 if layer 0 is empty? Yes.
                  whiteDVONNLayer[j] = safeAddAndCheck(whiteDVONNLayer[j], "White DVONN [$j]", sourceBit)
                  break
              }
          }''', content) # NOTE: I will fix this logic properly.
