package io.quarkus.annotation.processor.generate_doc;

import java.util.List;

final class DescriptiveDocFormatter implements DocFormatter {
    private static final String ENTRY_END = "\n--\n\n***\n";
    private static final String DETAILS_TITLE = "\n== Details\n";
    private static final String TYPE_DESCRIPTION_FORMAT = "\n\nType: `%s` %s";
    private static final String DEFAULTS_VALUE_FORMAT = "\n\nDefaults to: `%s`";
    private static final String BASIC_DESCRIPTION_FORMAT = "\n[[%s]]\n`%s` %s::\n+\n--\n%s";
    private static final String ACCEPTED_VALUES_DESCRIPTION_FORMAT = "\n\nAccepted values: %s";

    /**
     * Generate configuration keys in descriptive format.
     * The key defines an anchor that used to link the description with the corresponding
     * key in the table of summary.
     *
     * @param configDocItems
     */
    @Override
    public String format(List<ConfigDocItem> configDocItems) {
        final StringBuilder generatedAsciiDoc = new StringBuilder(DETAILS_TITLE);
        for (ConfigDocItem configDocItem : configDocItems) {
            generatedAsciiDoc.append(configDocItem.accept(this));
        }

        return generatedAsciiDoc.toString();
    }

    @Override
    public String format(ConfigDocKey configDocKey) {
        final StringBuilder configItemDoc = new StringBuilder();

        final String basicDescription = String.format(BASIC_DESCRIPTION_FORMAT, getAnchor(configDocKey), configDocKey.getKey(),
                configDocKey.getConfigPhase().getIllustration(), configDocKey.getConfigDoc());
        configItemDoc.append(basicDescription);

        if (configDocKey.hasAcceptedValues()) {
            configItemDoc.append(String.format(ACCEPTED_VALUES_DESCRIPTION_FORMAT,
                    DocGeneratorUtil.joinAcceptedValues(configDocKey.getAcceptedValues())));
        } else if (configDocKey.hasType()) {
            configItemDoc.append(String.format(TYPE_DESCRIPTION_FORMAT, configDocKey.computeTypeSimpleName(),
                    DocGeneratorUtil.getTypeFormatInformationNote(configDocKey)));
        }

        if (!configDocKey.getDefaultValue().isEmpty()) {
            configItemDoc.append(String.format(DEFAULTS_VALUE_FORMAT, configDocKey.getDefaultValue()));
        }

        configItemDoc.append(ENTRY_END);

        return configItemDoc.toString();
    }

    @Override
    public String format(ConfigDocSection configDocSection) {
        final StringBuilder generatedAsciiDoc = new StringBuilder(configDocSection.getSectionDetails());
        generatedAsciiDoc.append("\n\n");
        for (ConfigDocItem configDocItem : configDocSection.getConfigDocItems()) {
            generatedAsciiDoc.append(configDocItem.accept(this));
        }

        return generatedAsciiDoc.toString();
    }
}
