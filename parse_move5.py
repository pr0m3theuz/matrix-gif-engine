move_val = 251380616

source_shift = (move_val >> 7) & 0b1111111
target_shift = move_val & 0b1111111
piece_type = (move_val >> 26) & 0b111

print(f"Move value: {move_val}")
print(f"Source shift: {source_shift} -> source bit: {1 << source_shift}")
print(f"Target shift: {target_shift} -> target bit: {1 << target_shift}")
print(f"Piece Type Enum Ordinal: {piece_type}")
