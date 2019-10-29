package io.quarkus.annotation.processor.generate_doc;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.quarkus.annotation.processor.Constants;

final class SummaryTableDocFormatter implements DocFormatter {
    private static final String TABLE_CLOSING_TAG = "\n|===";
    private static final String TABLE_ROW_FORMAT = "\n\na|%s [[%s]]`link:#%s[%s]`\n\n[.description]\n--\n%s\n--|%s %s\n|%s\n";
    private static final String TABLE_SECTION_ROW_FORMAT = "\n\nh|[[%s]]link:#%s[%s]\nh|Type\nh|Default";
    private static final String TABLE_HEADER_FORMAT = "[.configuration-legend]%s\n[.configuration-reference, cols=\"80,.^10,.^10\"]\n|===";
    //    private static final String MORE_INFO_ABOUT_SECTION_FORMAT = "link:#%s[icon:plus-circle[], title=More information about %s]";

    private String anchorPrefix = "";

    /**
     * Generate configuration keys in table format.
     * Generated table will contain a key column that points to the long descriptive format.
     *
     * @param configDocItems
     */
    @Override
    public void format(Writer writer, String initialAnchorPrefix, List<ConfigDocItem> configDocItems) throws IOException {
        final String tableHeaders = String.format(TABLE_HEADER_FORMAT, Constants.CONFIG_PHASE_LEGEND);
        writer.append(tableHeaders);
        anchorPrefix = initialAnchorPrefix;

        // make sure that section-less configs get a legend
        if (configDocItems.isEmpty() || configDocItems.get(0).isConfigKey()) {
            String anchor = anchorPrefix + getAnchor("configuration");
            writer.append(String.format(TABLE_SECTION_ROW_FORMAT, anchor, anchor, "Configuration property"));
        }

        for (ConfigDocItem configDocItem : configDocItems) {
            if (configDocItem.isConfigSection()
                    && configDocItem.getConfigDocSection().getAnchorPrefix() != null) {
                anchorPrefix = configDocItem.getConfigDocSection().getAnchorPrefix() + "_";
            }
            configDocItem.accept(writer, this);
        }

        writer.append(TABLE_CLOSING_TAG); // close table
    }

    @Override
    public void format(Writer writer, ConfigDocKey configDocKey) throws IOException {
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
        if (configDocKey.isList()) {
            typeContent = "list of " + typeContent;
        }

        String doc = configDocKey.getConfigDoc();

        final String typeDetail = DocGeneratorUtil.getTypeFormatInformationNote(configDocKey);
        final String defaultValue = configDocKey.getDefaultValue();
        // this is not strictly true, because we can have a required value with a default value, but
        // for documentation it will do
        String required = configDocKey.isOptional() || !defaultValue.isEmpty() ? ""
                : "required icon:exclamation-circle[title=Configuration property is required]";
        String anchor = anchorPrefix + getAnchor(configDocKey.getKey());
        writer.append(String.format(TABLE_ROW_FORMAT,
                configDocKey.getConfigPhase().getIllustration(),
                anchor,
                anchor,
                configDocKey.getKey(),
                // make sure nobody inserts a table cell separator here
                doc.replace("|", "\\|"),
                typeContent, typeDetail,
                defaultValue.isEmpty() ? required : String.format("`%s`", defaultValue)));
    }

    @Override
    public void format(Writer writer, ConfigDocSection configDocSection) throws IOException {
        //        final String moreInfoAboutSection = String.format(MORE_INFO_ABOUT_SECTION_FORMAT, getAnchor(configDocSection.getName()),
        //                configDocSection.getSectionDetailsTitle());
        //        final String moreInfoAboutSection = configDocSection.getSectionDetailsTitle();
        String anchor = anchorPrefix + getAnchor(configDocSection.getSectionDetailsTitle());
        final String sectionRow = String.format(TABLE_SECTION_ROW_FORMAT, anchor, anchor,
                configDocSection.getSectionDetailsTitle());
        writer.append(sectionRow);

        for (ConfigDocItem configDocItem : configDocSection.getConfigDocItems()) {
            configDocItem.accept(writer, this);
        }
    }

}
