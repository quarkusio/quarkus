package io.quarkus.annotation.processor.generate_doc;

import java.util.List;

import io.quarkus.annotation.processor.Constants;

final class SummaryTableDocFormatter implements DocFormatter {
    private static final String TABLE_CLOSING_TAG = "\n|===";
    private static final String TABLE_ROW_FORMAT = "\n\n|<<%s, %s>>\n\n%s|%s %s\n|%s\n| %s";
    private static final String TABLE_HEADER_FORMAT = "== Summary\n%s\n[.configuration-reference, cols=\"65,.^17,.^13,^.^5\"]\n|===\n|Configuration property|Type|Default|Lifecycle";

    /**
     * Generate configuration keys in table format.
     * Generated table will contain a key column that points to the long descriptive format.
     *
     * @param configDocItems
     */
    @Override
    public String format(List<ConfigDocItem> configDocItems) {
        final String tableHeaders = String.format(TABLE_HEADER_FORMAT, Constants.CONFIG_PHASE_LEGEND);
        final StringBuilder generatedAsciiDoc = new StringBuilder(tableHeaders);

        for (ConfigDocItem configDocItem : configDocItems) {
            generatedAsciiDoc.append(configDocItem.accept(this));
        }

        generatedAsciiDoc.append(TABLE_CLOSING_TAG); // close table
        return generatedAsciiDoc.toString();
    }

    @Override
    public String format(ConfigDocKey configDocKey) {
        String typeContent = "";
        if (configDocKey.hasAcceptedValues()) {
            typeContent = DocGeneratorUtil.joinAcceptedValues(configDocKey.getAcceptedValues());
        } else if (configDocKey.hasType()) {
            typeContent = configDocKey.computeTypeSimpleName();
            final String javaDocLink = configDocKey.getJavaDocSiteLink();
            if (!javaDocLink.isEmpty()) {
                typeContent = String.format("link:%s[%s]\n", javaDocLink, typeContent);
            }
        }

        String doc = configDocKey.getConfigDoc();
        String firstLineDoc = "";
        if (doc != null && !doc.isEmpty()) {
            int firstDot = doc.indexOf('.');
            firstLineDoc = firstDot != -1 ? doc.substring(0, firstDot + 1) : doc.trim() + '.';
        }

        final String typeDetail = DocGeneratorUtil.getTypeFormatInformationNote(configDocKey);
        final String defaultValue = configDocKey.getDefaultValue();
        return String.format(TABLE_ROW_FORMAT,
                getAnchor(configDocKey), configDocKey.getKey(),
                firstLineDoc,
                typeContent, typeDetail,
                defaultValue.isEmpty() ? Constants.EMPTY : String.format("`%s`", defaultValue),
                configDocKey.getConfigPhase().getIllustration());
    }

    @Override
    public String format(ConfigDocSection configDocSection) {
        final StringBuilder generatedAsciiDoc = new StringBuilder();
        for (ConfigDocItem configDocItem : configDocSection.getConfigDocItems()) {
            generatedAsciiDoc.append(configDocItem.accept(this));
        }

        return generatedAsciiDoc.toString();
    }

}
