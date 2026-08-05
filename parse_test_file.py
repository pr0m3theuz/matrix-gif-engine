import re

with open('src/test/kotlin/model/UndoUsePiecePotentialTest.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines[140:155]):
    print(f"{140+i}: {line.strip()}")
