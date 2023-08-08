import os
import re
import json

def load_exclusions():
    with open('exclusion-list.json', 'r') as file:
        return json.load(file)

def replace_instances(directory, exclusions):
    output_directory = os.path.join(directory, 'output')
    os.makedirs(output_directory, exist_ok=True)  # Create the output directory if it doesn't exist

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

                    # Skip replacements for exclusion list
                    if any(exclusion in line for exclusion in exclusions):
                        new_content.append(line)
                        continue

                    # Perform replacements on lines that are not headings, inside comment blocks, or in the exclusion list
                    occurrences = [match.start() for match in re.finditer(r'\bQuarkus\b', line)]
                    if occurrences:
                        if not found_first_instance:
                            # Replace the first occurrence with "{product-name-first}"
                            line = line[:occurrences[0]] + '{product-name-first}' + line[occurrences[0] + len('Quarkus'):]
                            found_first_instance = True
                            occurrences.pop(0) # Remove first occurrence from the list

                        # Replace the rest of the occurrences with "{product-name}" starting from the last occurrence
                        for occurrence in reversed(occurrences):
                            line = line[:occurrence] + '{product-name}' + line[occurrence + len('Quarkus'):]

                    new_content.append(line)

            # Write the modified content back to the file in the output directory
            with open(os.path.join(output_directory, filename), 'w') as file:
                file.writelines(new_content)

# Provide the path to the directory containing the AsciiDoc files
directory_path = './docs/src/main/asciidoc'
exclusions = load_exclusions()
replace_instances(directory_path, exclusions)
