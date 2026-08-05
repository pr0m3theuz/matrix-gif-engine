import re

with open('src/test/kotlin/model/UndoUsePiecePotentialTest.kt', 'r') as f:
    content = f.read()

content = content.replace("251380616u", "452707208u")

with open('src/test/kotlin/model/UndoUsePiecePotentialTest.kt', 'w') as f:
    f.write(content)
