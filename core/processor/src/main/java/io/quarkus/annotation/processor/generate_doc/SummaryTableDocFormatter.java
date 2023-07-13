package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.Constants.CONFIG_PHASE_LEGEND;
import static io.quarkus.annotation.processor.Constants.NEW_LINE;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.toEnvVarName;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.quarkus.annotation.processor.Constants;

final class SummaryTableDocFormatter implements DocFormatter {
    private static final String TWO_NEW_LINES = "\n\n";
    private static final String TABLE_CLOSING_TAG = "\n|===";
    public static final String SEARCHABLE_TABLE_CLASS = ".searchable"; // a css class indicating if a table is searchable
    public static final String CONFIGURATION_TABLE_CLASS = ".configuration-reference";
    private static final String TABLE_ROW_FORMAT = "\n\na|%s [[%s]]`link:#%s[%s]`\n\n[.description]\n--\n%s\n--%s|%s %s\n|%s\n";
    private static final String SECTION_TITLE = "[[%s]]link:#%s[%s]";
    private static final String TABLE_HEADER_FORMAT = "[%s, cols=\"80,.^10,.^10\"]\n|===";
    private static final String TABLE_SECTION_ROW_FORMAT = "\n\nh|%s\n%s\nh|Type\nh|Default";
    private final boolean showEnvVars;

    private String anchorPrefix = "";

    public SummaryTableDocFormatter(boolean showEnvVars) {
        this.showEnvVars = showEnvVars;
    }

    public SummaryTableDocFormatter() {
        this(true);
    }

    /**
     * Generate configuration keys in table format with search engine activated or not.
     * Useful when we want to optionally activate or deactivate search engine
     */
    @Override
    public void format(Writer writer, String initialAnchorPrefix, boolean activateSearch,
            List<ConfigDocItem> configDocItems, boolean includeConfigPhaseLegend)
            throws IOException {
        if (includeConfigPhaseLegend) {
            writer.append("[.configuration-legend]").append(CONFIG_PHASE_LEGEND).append(NEW_LINE);
        }
        String searchableClass = activateSearch ? SEARCHABLE_TABLE_CLASS : Constants.EMPTY;
        String tableClasses = CONFIGURATION_TABLE_CLASS + searchableClass;
        writer.append(String.format(TABLE_HEADER_FORMAT, tableClasses));
        anchorPrefix = initialAnchorPrefix;

        // make sure that section-less configs get a legend
        if (configDocItems.isEmpty() || configDocItems.get(0).isConfigKey()) {
            String anchor = anchorPrefix + getAnchor("configuration");
            writer.append(String.format(TABLE_SECTION_ROW_FORMAT,
                    String.format(SECTION_TITLE, anchor, anchor, "Configuration property"),
                    Constants.EMPTY));
        }

        for (ConfigDocItem configDocItem : configDocItems) {
            if (configDocItem.isConfigSection() && configDocItem.getConfigDocSection().isShowSection()
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
            if (configDocKey.isEnum()) {
                typeContent = DocGeneratorUtil.joinEnumValues(configDocKey.getAcceptedValues());
            } else {
                typeContent = DocGeneratorUtil.joinAcceptedValues(configDocKey.getAcceptedValues());
            }
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

        if (showEnvVars) {
            // Convert a property name to an environment variable name and show it in the config description
            final String envVarExample = String.format("ifdef::add-copy-button-to-env-var[]\n" +
                    "Environment variable: env_var_with_copy_button:+++%1$s+++[]\n" +
                    "endif::add-copy-button-to-env-var[]\n" +
                    "ifndef::add-copy-button-to-env-var[]\n" +
                    "Environment variable: `+++%1$s+++`\n" +
                    "endif::add-copy-button-to-env-var[]", toEnvVarName(configDocKey.getKey()));
            if (configDocKey.getConfigDoc().isEmpty()) {
                doc = envVarExample;
            } else {
                // Add 2 new lines in order to show the environment variable on next line
                doc += TWO_NEW_LINES + envVarExample;
            }
        }

        final String typeDetail = DocGeneratorUtil.getTypeFormatInformationNote(configDocKey);
        final String defaultValue = configDocKey.getDefaultValue();
        // this is not strictly true, because we can have a required value with a default value, but
        // for documentation it will do
        String required = configDocKey.isOptional() || !defaultValue.isEmpty() ? ""
                : "required icon:exclamation-circle[title=Configuration property is required]";
        String key = configDocKey.getKey();
        String configKeyAnchor = configDocKey.isPassThroughMap() ? getAnchor(key + Constants.DASH + configDocKey.getDocMapKey())
                : getAnchor(key);
        String anchor = anchorPrefix + configKeyAnchor;

        StringBuilder keys = new StringBuilder();
        keys.append(
                String.format("%s [[%s]]`link:#%s[%s]`\n\n", configDocKey.getConfigPhase().getIllustration(), anchor, anchor,
                        key));
        for (String additionalKey : configDocKey.getAdditionalKeys()) {
            if (!additionalKey.equals(key)) {
                keys.append(String.format("`link:#%s[%s]`\n\n", anchor, additionalKey));
            }
        }

        writer.append(String.format("\n\na|%s\n[.description]\n--\n%s\n--%s|%s %s\n|%s\n",
                keys,
                // make sure nobody inserts a table cell separator here
                doc.replace("|", "\\|"),
                // if ConfigDocKey is enum, cell style operator must support block elements
                configDocKey.isEnum() ? " a" : Constants.EMPTY,
                typeContent, typeDetail,
                defaultValue.isEmpty() ? required
                        : String.format("`%s`", defaultValue.replace("|", "\\|")
                                .replace("`", "\\`"))));
    }

    @Override
    public void format(Writer writer, ConfigDocSection configDocSection) throws IOException {
        if (configDocSection.isShowSection()) {
            String anchor = anchorPrefix
                    + getAnchor(configDocSection.getName() + Constants.DASH + configDocSection.getSectionDetailsTitle());
            String sectionTitle = String.format(SECTION_TITLE, anchor, anchor, configDocSection.getSectionDetailsTitle());
            final String sectionRow = String.format(TABLE_SECTION_ROW_FORMAT, sectionTitle,
                    configDocSection.isOptional() ? "This configuration section is optional" : Constants.EMPTY);

            writer.append(sectionRow);
        }

        for (ConfigDocItem configDocItem : configDocSection.getConfigDocItems()) {
            configDocItem.accept(writer, this);
        }
    }

}
