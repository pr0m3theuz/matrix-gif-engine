import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# Fix `undoRetrieveAndCapturePieces` parity
content = content.replace(
'''(whiteDVONNLayer[i] and bitmask) == 0UL &&
									(whiteDVONNLayer[i - 1] and bitmask) == bitmask''',
'''(whiteDVONNLayer[i] and bitmask) == 0UL &&
									(blackDVONNLayer[i - 1] and bitmask) == bitmask''')

content = content.replace(
'''(blackDVONNLayer[i] and bitmask) == 0UL &&
									(blackDVONNLayer[i - 1] and bitmask) == bitmask''',
'''(blackDVONNLayer[i] and bitmask) == 0UL &&
									(whiteDVONNLayer[i - 1] and bitmask) == bitmask''')

content = content.replace(
'''(whitePUNCTLayer[i] and bitmask) == 0UL &&
									(whitePUNCTLayer[i - 1] and bitmask) == bitmask''',
'''(whitePUNCTLayer[i] and bitmask) == 0UL &&
									(blackPUNCTLayer[i - 1] and bitmask) == bitmask''')

content = content.replace(
'''(blackPUNCTLayer[i] and bitmask) == 0UL &&
									(blackPUNCTLayer[i - 1] and bitmask) == bitmask''',
'''(blackPUNCTLayer[i] and bitmask) == 0UL &&
									(whitePUNCTLayer[i - 1] and bitmask) == bitmask''')


# Fix `usePiecePotential` AND `undoUsePiecePotential` parity
content = content.replace(
'''(whiteDVONNLayer[i] and targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit''',
'''(whiteDVONNLayer[i] and targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit''')

content = content.replace(
'''(blackDVONNLayer[i] and targetBit) == 0UL &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit''',
'''(blackDVONNLayer[i] and targetBit) == 0UL &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit''')

content = content.replace(
'''(whitePUNCTLayer[i] and targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit''',
'''(whitePUNCTLayer[i] and targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit''')

content = content.replace(
'''(blackPUNCTLayer[i] and targetBit) == 0UL &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit''',
'''(blackPUNCTLayer[i] and targetBit) == 0UL &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit''')

content = content.replace(
'''(whiteDVONNLayer[i] and targetBit) == targetBit &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit''',
'''(whiteDVONNLayer[i] and targetBit) == targetBit &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit''')

content = content.replace(
'''(blackDVONNLayer[i] and targetBit) == targetBit &&
                    (blackDVONNLayer[i - 1] and targetBit) == targetBit''',
'''(blackDVONNLayer[i] and targetBit) == targetBit &&
                    (whiteDVONNLayer[i - 1] and targetBit) == targetBit''')

content = content.replace(
'''(whitePUNCTLayer[i] and targetBit) == targetBit &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit''',
'''(whitePUNCTLayer[i] and targetBit) == targetBit &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit''')

content = content.replace(
'''(blackPUNCTLayer[i] and targetBit) == targetBit &&
                    (blackPUNCTLayer[i - 1] and targetBit) == targetBit''',
'''(blackPUNCTLayer[i] and targetBit) == targetBit &&
                    (whitePUNCTLayer[i - 1] and targetBit) == targetBit''')


# Fix `usePiecePotential` removing piece from sourceBit
# ZERTZ
content = content.replace(
'''PieceType.ZERTZ -> {
          whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''',
'''PieceType.ZERTZ -> {
          whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
          whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", sourceBit)
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''')

content = content.replace(
'''PieceType.ZERTZ -> {
          blackZERTZ = safeAddAndCheck(blackZERTZ, "Black ZERTZ", targetBit)
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''',
'''PieceType.ZERTZ -> {
          blackZERTZ = safeAddAndCheck(blackZERTZ, "Black ZERTZ", targetBit)
          blackZERTZ = safeRemoveAndCheck(blackZERTZ, "Black ZERTZ", sourceBit)
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''')

# YINSH
content = content.replace(
'''PieceType.YINSH -> {
          whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH", targetBit)
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''',
'''PieceType.YINSH -> {
          whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH", targetBit)
          whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH", sourceBit)
          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''')

content = content.replace(
'''PieceType.YINSH -> {
          blackYINSH = safeAddAndCheck(blackYINSH, "Black YINSH", targetBit)
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''',
'''PieceType.YINSH -> {
          blackYINSH = safeAddAndCheck(blackYINSH, "Black YINSH", targetBit)
          blackYINSH = safeRemoveAndCheck(blackYINSH, "Black YINSH", sourceBit)
          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''')

# DVONN/PUNCT white removing from sourceBit
content = content.replace(
'''        PieceType.DVONN -> {
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

          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)''',
'''        PieceType.DVONN -> {
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
              if (j % 2 == 0) {
                  if ((whiteDVONNLayer[j] and sourceBit) == sourceBit) {
                      whiteDVONNLayer[j] = safeRemoveAndCheck(whiteDVONNLayer[j], "White DVONN [$j]", sourceBit)
                      break
                  }
              } else {
                  if ((blackDVONNLayer[j] and sourceBit) == sourceBit) {
                      blackDVONNLayer[j] = safeRemoveAndCheck(blackDVONNLayer[j], "Black DVONN [$j]", sourceBit)
                      break
                  }
              }
          }''')

content = content.replace(
'''        PieceType.PUNCT -> {
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

          whitePotentials = safeRemoveAndCheck(whitePotentials, "White Potentials", sourceBit)''',
'''        PieceType.PUNCT -> {
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
              if (j % 2 == 0) {
                  if ((whitePUNCTLayer[j] and sourceBit) == sourceBit) {
                      whitePUNCTLayer[j] = safeRemoveAndCheck(whitePUNCTLayer[j], "White PUNCT [$j]", sourceBit)
                      break
                  }
              } else {
                  if ((blackPUNCTLayer[j] and sourceBit) == sourceBit) {
                      blackPUNCTLayer[j] = safeRemoveAndCheck(blackPUNCTLayer[j], "Black PUNCT [$j]", sourceBit)
                      break
                  }
              }
          }''')


# DVONN/PUNCT black removing from sourceBit
content = content.replace(
'''        PieceType.DVONN -> {

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

          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)''',
'''        PieceType.DVONN -> {

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
              if (j % 2 == 0) {
                  if ((blackDVONNLayer[j] and sourceBit) == sourceBit) {
                      blackDVONNLayer[j] = safeRemoveAndCheck(blackDVONNLayer[j], "Black DVONN [$j]", sourceBit)
                      break
                  }
              } else {
                  if ((whiteDVONNLayer[j] and sourceBit) == sourceBit) {
                      whiteDVONNLayer[j] = safeRemoveAndCheck(whiteDVONNLayer[j], "White DVONN [$j]", sourceBit)
                      break
                  }
              }
          }''')


content = content.replace(
'''        PieceType.PUNCT -> {
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

          blackPotentials = safeRemoveAndCheck(blackPotentials, "Black Potentials", sourceBit)''',
'''        PieceType.PUNCT -> {
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
              if (j % 2 == 0) {
                  if ((blackPUNCTLayer[j] and sourceBit) == sourceBit) {
                      blackPUNCTLayer[j] = safeRemoveAndCheck(blackPUNCTLayer[j], "Black PUNCT [$j]", sourceBit)
                      break
                  }
              } else {
                  if ((whitePUNCTLayer[j] and sourceBit) == sourceBit) {
                      whitePUNCTLayer[j] = safeRemoveAndCheck(whitePUNCTLayer[j], "White PUNCT [$j]", sourceBit)
                      break
                  }
              }
          }''')



# UNDO Phase: restoring piece to sourceBit
# ZERTZ
content = content.replace(
'''PieceType.ZERTZ -> {
          whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''',
'''PieceType.ZERTZ -> {
          whiteZERTZ = safeRemoveAndCheck(whiteZERTZ, "White ZERTZ", targetBit)
          whiteZERTZ = safeAddAndCheck(whiteZERTZ, "White ZERTZ", sourceBit)
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''')

content = content.replace(
'''PieceType.ZERTZ -> {
          blackZERTZ = blackZERTZ and targetBit.inv()
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''',
'''PieceType.ZERTZ -> {
          blackZERTZ = safeRemoveAndCheck(blackZERTZ, "Black ZERTZ", targetBit)
          blackZERTZ = safeAddAndCheck(blackZERTZ, "Black ZERTZ", sourceBit)
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''')

# YINSH
content = content.replace(
'''PieceType.YINSH -> {
          whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH", targetBit)
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''',
'''PieceType.YINSH -> {
          whiteYINSH = safeRemoveAndCheck(whiteYINSH, "White YINSH", targetBit)
          whiteYINSH = safeAddAndCheck(whiteYINSH, "White YINSH", sourceBit)
          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)
        }''')

content = content.replace(
'''PieceType.YINSH -> {
          blackYINSH = blackYINSH and targetBit.inv()
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''',
'''PieceType.YINSH -> {
          blackYINSH = safeRemoveAndCheck(blackYINSH, "Black YINSH", targetBit)
          blackYINSH = safeAddAndCheck(blackYINSH, "Black YINSH", sourceBit)
          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)
        }''')


# DVONN/PUNCT white restoring to sourceBit
content = content.replace(
'''        PieceType.DVONN -> {
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

          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)''',
'''        PieceType.DVONN -> {
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
          }''')

content = content.replace(
'''        PieceType.PUNCT -> {
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

          whitePotentials = safeAddAndCheck(whitePotentials, "White Potentials", sourceBit)''',
'''        PieceType.PUNCT -> {
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
          }''')


# DVONN/PUNCT black restoring to sourceBit
content = content.replace(
'''        PieceType.DVONN -> {

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

          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)''',
'''        PieceType.DVONN -> {

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
          }''')

content = content.replace(
'''        PieceType.PUNCT -> {
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

          blackPotentials = safeAddAndCheck(blackPotentials, "Black Potentials", sourceBit)''',
'''        PieceType.PUNCT -> {
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
          }''')

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'w') as f:
    f.write(content)
