import os
import re

def replace_instances(directory):
    for filename in os.listdir(directory):
        if filename.endswith(".adoc"):
            new_content = []
            found_first_instance = False

            with open(os.path.join(directory, filename), 'r') as file:
                for line in file:
                    # Check if the line is a heading
                    if line.startswith('= '):
                        new_content.append(line)
                        continue

                    # Perform replacements on lines that are not headings
                    occurrences = [match.start() for match in re.finditer(r'\bQuarkus\b', line)]
                    if occurrences:
                        if not found_first_instance:
                            # Replace the first occurrence with ":firstinstance:"
                            line = line[:occurrences[0]] + ':firstinstance:' + line[occurrences[0] + len('Quarkus'):]
                            found_first_instance = True
                            occurrences.pop(0) # Remove first occurrence from the list

                        # Replace the rest of the occurrences with ":otherinstance:" starting from the last occurrence
                        for occurrence in reversed(occurrences):
                            line = line[:occurrence] + ':otherinstance:' + line[occurrence + len('Quarkus'):]

                    new_content.append(line)

            # Write the modified content back to the file
            with open(os.path.join(directory, filename), 'w') as file:
                file.writelines(new_content)

# Provide the path to the directory containing the AsciiDoc files
directory_path = './docs/src/main/asciidoc'
replace_instances(directory_path)
