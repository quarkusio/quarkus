import os
import re

def replace_instances(directory):
    for filename in os.listdir(directory):
        if filename.endswith(".adoc"):
            with open(os.path.join(directory, filename), 'r') as file:
                content = file.read()
                occurrences = [match.start() for match in re.finditer(r'\bQuarkus\b', content)]
                if occurrences:
                    # Replace the rest of the occurrences with ":otherinstance:" starting from the last occurrence
                    for occurrence in reversed(occurrences[1:]):
                        content = content[:occurrence] + ':otherinstance:' + content[occurrence + len('Quarkus'):]

                    # Replace the first occurrence with ":firstinstance:"
                    occurrence = occurrences[0]
                    content = content[:occurrence] + ':firstinstance:' + content[occurrence + len('Quarkus'):]

            # Write the modified content back to the file
            with open(os.path.join(directory, filename), 'w') as file:
                file.write(content)

# Provide the path to the directory containing the AsciiDoc files
directory_path = './docs/src/main/asciidoc'
replace_instances(directory_path)
