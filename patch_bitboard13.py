import re

with open('src/main/kotlin/model/BitboardOptimizedFunctions.kt', 'r') as f:
    content = f.read()

# I see it in parse_move8.py output:
# preMoveBitboardState:
# whitePUNCTLayer[0] = 128u
# whitePotentials = 128u
# blackPUNCTLayer[0] = 256u
# postUsePotentialBitboardState:
# whitePotentials = 0u
# blackPUNCTLayer[0] = 256u, blackPUNCTLayer[1] = 256u
#
# Wait! In the test, PieceType.PUNCT was originally 6!
# BUT I REPLACED 251380616u with 452707208u.
# Let's see if there is another occurrence of 251380616u in the test.
