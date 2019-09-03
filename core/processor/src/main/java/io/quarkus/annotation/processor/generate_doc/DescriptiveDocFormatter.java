package io.quarkus.annotation.processor.generate_doc;

import java.util.List;

class DescriptiveDocFormatter implements DocFormatter {
    private static final String ENTRY_END = "\n\n";
    private static final String DETAILS_TITLE = "\n== Details\n";
    private static final String DEFAULTS_VALUE_FORMAT = "Defaults to: `%s` +\n";
    private static final String BASIC_DESCRIPTION_FORMAT = "\n[[%s]]\n`%s`%s:: %s \n+\nType: `%s` +\n";

    /**
     * Generate configuration keys in descriptive format.
     * The key defines an anchor that used to link the description with the corresponding
     * key in the table of summary.
     */
    @Override
    public String format(List<ConfigItem> configItems) {
        StringBuilder generatedAsciiDoc = new StringBuilder(DETAILS_TITLE);
        for (ConfigItem configItem : configItems) {
            final String basicDescription = String.format(BASIC_DESCRIPTION_FORMAT, getAnchor(configItem), configItem.getKey(),
                    configItem.getConfigPhase().getIllustration(), configItem.getConfigDoc(),
                    configItem.computeTypeSimpleName());

            generatedAsciiDoc.append(basicDescription);
            if (!configItem.getDefaultValue().isEmpty()) {
                generatedAsciiDoc.append(String.format(DEFAULTS_VALUE_FORMAT, configItem.getDefaultValue()));
            }

            generatedAsciiDoc.append(ENTRY_END);
        }

        return generatedAsciiDoc.toString();
    }
}
