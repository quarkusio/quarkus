package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.annotation.processor.Constants;

final public class ConfigDocWriter {
    private final DocFormatter summaryTableDocFormatter = new SummaryTableDocFormatter();

    /**
     * Write all extension configuration in AsciiDoc format in `{root}/target/asciidoc/generated/config/` directory
     */
    public void writeAllExtensionConfigDocumentation(ConfigDocGeneratedOutput output)
            throws IOException {
        generateDocumentation(Constants.GENERATED_DOCS_PATH.resolve(output.getFileName()), output.getAnchorPrefix(),
                output.isSearchable(), output.getConfigDocItems());
    }

    /**
     * Generate documentation in a summary table and descriptive format
     *
     * @param targetPath
     * @param configDocItems
     * @throws IOException
     */
    private void generateDocumentation(Path targetPath, String initialAnchorPrefix, boolean activateSearch,
            List<ConfigDocItem> configDocItems)
            throws IOException {
        try (Writer writer = Files.newBufferedWriter(targetPath)) {
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
