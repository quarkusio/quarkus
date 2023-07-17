package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.Constants.SUMMARY_TABLE_ID_VARIABLE;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.annotation.processor.Constants;

/**
 * {@link ConfigDoc} builder
 */
class ConfigDocBuilder {

    /**
     * Declare AsciiDoc variable
     */
    private static final String DECLARE_VAR = "\n:%s: %s\n";
    private final DocFormatter summaryTableDocFormatter;
    protected final List<ConfigDoc.WriteItem> writeItems = new ArrayList<>();

    public ConfigDocBuilder() {
        summaryTableDocFormatter = new SummaryTableDocFormatter();
    }

    protected ConfigDocBuilder(boolean showEnvVars) {
        summaryTableDocFormatter = new SummaryTableDocFormatter(showEnvVars);
    }

    /**
     * Add documentation in a summary table and descriptive format
     */
    public final ConfigDocBuilder addSummaryTable(String initialAnchorPrefix, boolean activateSearch,
            List<ConfigDocItem> configDocItems, String fileName,
            boolean includeConfigPhaseLegend) {

        writeItems.add(writer -> {

            // Create var with unique value for each summary table that will make DURATION_FORMAT_NOTE (see below) unique
            var fileNameWithoutExtension = fileName.substring(0, fileName.length() - Constants.ADOC_EXTENSION.length());
            writer.append(String.format(DECLARE_VAR, SUMMARY_TABLE_ID_VARIABLE, fileNameWithoutExtension));

            summaryTableDocFormatter.format(writer, initialAnchorPrefix, activateSearch, configDocItems,
                    includeConfigPhaseLegend);

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
        });
        return this;
    }

    public boolean hasWriteItems() {
        return !writeItems.isEmpty();
    }

    /**
     * Passed strings are appended to the file
     */
    public final ConfigDocBuilder write(String... strings) {
        requireNonNull(strings);
        writeItems.add(writer -> {
            for (String str : strings) {
                writer.append(str);
            }
        });
        return this;
    }

    public final ConfigDoc build() {
        final List<ConfigDoc.WriteItem> docItemsCopy = List.copyOf(writeItems);
        return () -> docItemsCopy;
    }

}
