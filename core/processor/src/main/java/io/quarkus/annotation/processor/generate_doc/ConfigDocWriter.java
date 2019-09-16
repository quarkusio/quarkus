package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.annotation.processor.Constants;

final public class ConfigDocWriter {
    private static final String DOC_DOUBLE_NEWLINE_SEPARATOR = "\n\n";
    private final DocFormatter descriptiveDocFormatter = new DescriptiveDocFormatter();
    private final DocFormatter summaryTableDocFormatter = new SummaryTableDocFormatter();

    /**
     * Write extension configuration AsciiDoc format in `{root}/target/asciidoc/generated/config/`
     */
    public void writeExtensionConfigDocumentation(Map<String, List<ConfigDocItem>> extensionsConfigurations)
            throws IOException {
        for (Map.Entry<String, List<ConfigDocItem>> entry : extensionsConfigurations.entrySet()) {
            final List<ConfigDocItem> configDocItems = entry.getValue();
            final String extensionFileName = entry.getKey();

            sort(configDocItems);

            final String doc = generateDocumentation(configDocItems);
            Files.write(Constants.GENERATED_DOCS_PATH.resolve(extensionFileName), doc.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Sort docs keys. The sorted list will contain the properties in the following order
     * - 1. Map config items as last elements of the generated docs.
     * - 2. Build time properties will come first.
     * - 3. Otherwise respect source code declaration order.
     * - 4. Elements within a configuration section will appear at the end of the generated doc while preserving described in
     * 1-4.
     */
    private static void sort(List<ConfigDocItem> configDocItems) {
        Collections.sort(configDocItems);
        for (ConfigDocItem configDocItem : configDocItems) {
            if (configDocItem.isConfigSection()) {
                sort(configDocItem.getConfigDocSection().getConfigDocItems());
            }
        }
    }

    /**
     * Generate documentation in a summary table and descriptive format
     *
     * @param configDocItems
     * @return - generated output
     */
    private String generateDocumentation(List<ConfigDocItem> configDocItems) {
        final StringBuilder doc = new StringBuilder(summaryTableDocFormatter.format(configDocItems));
        doc.append(DOC_DOUBLE_NEWLINE_SEPARATOR);
        doc.append(descriptiveDocFormatter.format(configDocItems));
        final String generatedDoc = doc.toString();

        if (generatedDoc.contains(Constants.DURATION_INFORMATION)) {
            doc.append(Constants.DURATION_FORMAT_NOTE);
        }

        if (generatedDoc.contains(Constants.MEMORY_SIZE_INFORMATION)) {
            doc.append(Constants.MEMORY_SIZE_FORMAT_NOTE);
        }

        return doc.toString();
    }
}
