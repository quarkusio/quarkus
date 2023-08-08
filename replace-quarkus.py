import os
import re

def replace_instances(directory):
    for filename in os.listdir(directory):
        # Skip filenames that contain the substring "attributes.adoc"
        if 'attributes.adoc' in filename:
            continue

        if filename.endswith(".adoc"):
            new_content = []
            found_first_instance = False
            inside_comment_block = False

            with open(os.path.join(directory, filename), 'r') as file:
                for line in file:
                    # Check if the line starts or ends a comment block
                    if line.strip() == '////':
                        inside_comment_block = not inside_comment_block
                        new_content.append(line)
                        continue

                    # Skip lines inside a comment block
                    if inside_comment_block:
                        new_content.append(line)
                        continue

                    # Check if the line is a heading (starting with one or more '=' followed by a space)
                    if re.match(r'^=+\s', line):
                        new_content.append(line)
                        continue

                    # Perform replacements on lines that are not headings or inside comment blocks
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
