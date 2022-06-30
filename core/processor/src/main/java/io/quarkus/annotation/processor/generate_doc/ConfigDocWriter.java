package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.Constants.SUMMARY_TABLE_ID_VARIABLE;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.annotation.processor.Constants;

final public class ConfigDocWriter {
    private final DocFormatter summaryTableDocFormatter = new SummaryTableDocFormatter();
    private static final String DECLARE_VAR = "\n:%s: %s\n";

    /**
     * Write all extension configuration in AsciiDoc format in `{root}/target/asciidoc/generated/config/` directory
     */
    public void writeAllExtensionConfigDocumentation(ConfigDocGeneratedOutput output)
            throws IOException {
        generateDocumentation(Constants.GENERATED_DOCS_PATH.resolve(output.getFileName()), output.getAnchorPrefix(),
                output.isSearchable(), output.getConfigDocItems(), output.getFileName());
    }

    /**
     * Generate documentation in a summary table and descriptive format
     *
     */
    private void generateDocumentation(Path targetPath, String initialAnchorPrefix, boolean activateSearch,
            List<ConfigDocItem> configDocItems, String fileName)
            throws IOException {
        if (configDocItems.isEmpty()) {
            return;
        }

        try (Writer writer = Files.newBufferedWriter(targetPath)) {

            // Create var with unique value for each summary table that will make DURATION_FORMAT_NOTE (see below) unique
            var fileNameWithoutExtension = fileName.substring(0, fileName.length() - Constants.ADOC_EXTENSION.length());
            writer.append(String.format(DECLARE_VAR, SUMMARY_TABLE_ID_VARIABLE, fileNameWithoutExtension));

            summaryTableDocFormatter.format(writer, initialAnchorPrefix, activateSearch, configDocItems);

            boolean hasDuration = false, hasMemory = false;
            for (ConfigDocItem item : configDocItems) {
                if (item.hasDurationInformationNote()) {
                    hasDuration = true;
                }

                if (item.hasMemoryInformationNote()) {
                    hasMemory = true;
                }
            }

            if (hasDuration) {
                writer.append(Constants.DURATION_FORMAT_NOTE);
            }

            if (hasMemory) {
                writer.append(Constants.MEMORY_SIZE_FORMAT_NOTE);
            }
        }
    }
}
