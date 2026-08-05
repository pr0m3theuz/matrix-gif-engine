import re

with open('src/test/kotlin/model/UndoUsePiecePotentialTest.kt', 'r') as f:
    content = f.read()

# I am going to replace 251380616u with a valid PUNCT move!
# 251380616 = 0b1110_1111_1011_1100_0011_1000_1000
# Target shift: 8 (bits 0-6)
# Source shift: 7 (bits 7-13)
# Piece Type: 6 (PUNCT) (bits 26-28)
# Color: 0 (White) (bit 29)
# Move Type: ?
# 0b00_110_xxx...
# Let's generate a valid UInt:
# Target: 8
# Source: 7 << 7 = 896
# PieceType: 6 << 26 = 402653184
# Color: 0 << 29 = 0
# Wait, let's keep all other bits the same except piece type.
# Piece type 3 was 3 << 26 = 201326592
# Difference is (6 - 3) << 26 = 3 << 26 = 201326592
# So 251380616 + 201326592 = 452707208

print("New valid move value:", 251380616 + 201326592)
