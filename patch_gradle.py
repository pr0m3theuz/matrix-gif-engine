with open('build.gradle.kts', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    new_lines.append(line)
    if 'testImplementation(kotlin("test"))' in line:
        new_lines.append('\ttestImplementation("com.code-intelligence:jazzer-junit:0.24.0")\n')
        new_lines.append('\ttestImplementation("com.code-intelligence:jazzer-api:0.24.0")\n')

with open('build.gradle.kts', 'w') as f:
    f.writelines(new_lines)
