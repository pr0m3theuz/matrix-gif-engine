# If moveValue = 251380616u, the bit string is:
# 251380616
# In binary:
val = 251380616
print(bin(val))
# 0b1110 1111 1011 1100 0011 1000 1000
# bits 0-6: targetBit shift = 8 -> targetBit = 256
# bits 7-13: sourceBit shift = 7 -> sourceBit = 128
# bits 26-28: pieceType = (val >> 26) & 7 = 3 (ZERTZ)
# Look at preMoveBitboardState and postUsePotentialBitboardState!
# preMoveBitboardState:
# whitePUNCTLayer[0] = 128u
# whitePotentials = 128u
# blackPUNCTLayer[0] = 256u
# postUsePotentialBitboardState:
# whitePotentials = 0u
# blackPUNCTLayer[0] = 256u, blackPUNCTLayer[1] = 256u !!
# Wait!
# If whitePUNCTLayer[0] = 128 (sourceBit)
# and blackPUNCTLayer[0] = 256 (targetBit)
# and the piece is White PUNCT moving from 128 to 256
# Then PieceType SHOULD BE PUNCT (6), NOT ZERTZ (3) !
# Why is pieceType 3?
# Wait! Look at line 188 of `PossibleMove.kt`
# Maybe `PossibleMove.kt` is wrong, or the `UndoUsePiecePotentialTest.kt` generated a wrong test case statically?
# If `251380616` is a generated move, maybe `extractPieceType` used to be different?
# The `UndoUsePiecePotentialTest` was manually written or generated with a bug?
