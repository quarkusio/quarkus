package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.annotation.processor.Constants;

final public class ConfigDocWriter {

    /**
     * Write all extension configuration in AsciiDoc format in `{root}/target/asciidoc/generated/config/` directory
     */
    public void writeAllExtensionConfigDocumentation(ConfigDocGeneratedOutput output)
            throws IOException {

        if (output.getConfigDocItems().isEmpty()) {
            return;
        }

        // Create single summary table
        final var configDocBuilder = new ConfigDocBuilder().addSummaryTable(output.getAnchorPrefix(), output.isSearchable(),
                output.getConfigDocItems(), output.getFileName(), true);

        generateDocumentation(output.getFileName(), configDocBuilder);
    }

    public void generateDocumentation(String fileName, ConfigDocBuilder configDocBuilder) throws IOException {
        generateDocumentation(
                // Resolve output file path
                Constants.GENERATED_DOCS_PATH.resolve(fileName),
                // Write all items
                configDocBuilder.build());
    }

    private void generateDocumentation(Path targetPath, ConfigDoc configDoc)
            throws IOException {
        try (Writer writer = Files.newBufferedWriter(targetPath)) {
            for (ConfigDoc.WriteItem writeItem : configDoc.getWriteItems()) {
                // Write documentation item, f.e. summary table
                writeItem.accept(writer);
            }
        }
    }

}
