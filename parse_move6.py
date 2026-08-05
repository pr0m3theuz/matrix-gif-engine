# move value was 251380616
val = 251380616
piece_type = (val >> 26) & 0b111
print(f"piece_type = {piece_type}")

color = (val >> 29) & 1
print(f"color = {color}")

# Wait, the piece color bit is 29, piece type is 26?
# In `PossibleMove.kt` line 242:
# // Color is bit 4 of the piece, which starts at 24 (so bit 29)
# wait, if color is bit 4 of the piece, and piece starts at 24? No, "starts at 24"?
